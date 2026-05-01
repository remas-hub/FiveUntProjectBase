/**
 * هون بتسجّل دخول أو تنشئ حساب جديد، تختار صورة للبروفايل، والكل متصل بـ Appwrite (جدول الطلاب والتخزين).
 */
package com.example.RemasProject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.bumptech.glide.Glide;
import com.example.RemasProject.Hellper.UiInsetsHelper;
import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.Hellper.LearningSettingsPrefs;
import com.example.RemasProject.model.Student;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

public class LoginActivity extends AppCompatActivity {
    
    private EditText etName, etEmail, etPassword;
    private Button btnLogin;
    private TextView tvSwitchMode, tvTitle;
    private ImageView ivProfilePicture;
    private LinearLayout layoutRegister, layoutProfilePicture;
    private ProgressBar progressBar;
    private boolean isLoginMode = true;
    
    private DALAppWriteConnection dal;
    private SharedPreferences prefs;
    private String selectedImageUrl = "";
    
    // Activity Result Launchers
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        UiInsetsHelper.enableEdgeToEdge(this);
        View root = findViewById(android.R.id.content);
        if (root instanceof android.view.ViewGroup && ((android.view.ViewGroup) root).getChildCount() > 0) {
            View contentRoot = ((android.view.ViewGroup) root).getChildAt(0);
            UiInsetsHelper.applySafePadding(contentRoot);
        }
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.login_status_bar));
        new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);

        // لو الطالب سبق وسجّل دخول على نفس الجهاز، studentId محفوظ — ما نعرض نموذج الدخول مرة ثانية
        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        String savedStudentId = prefs.getString("studentId", null);
        if (savedStudentId != null) {
            // الانتقال مباشرة للشاشة الرئيسية
            startMainActivity();
            return;
        }
        
        dal = new DALAppWriteConnection(this);
        
        initializeImagePickers();
        initializeViews();
        setupListeners();
        animateViews();
    }
    
    private void initializeImagePickers() {
        // Launcher للكاميرا
        cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    
                    if (imageBitmap != null) {
                        uploadImageToServer(imageBitmap);
                    }
                }
            }
        );
        
        // Launcher للمعرض
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    
                    if (imageUri != null) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                            uploadImageToServer(bitmap);
                        } catch (Exception e) {
                            showToast("خطأ في قراءة الصورة: " + e.getMessage());
                        }
                    }
                }
            }
        );
    }
    
    private void initializeViews() {
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvSwitchMode = findViewById(R.id.tvSwitchMode);
        tvTitle = findViewById(R.id.tvTitle);
        layoutRegister = findViewById(R.id.layoutRegister);
        layoutProfilePicture = findViewById(R.id.layoutProfilePicture);
        ivProfilePicture = findViewById(R.id.ivProfilePicture);
        progressBar = findViewById(R.id.progressBar);
        
        // إخفاء حقول التسجيل في البداية
        layoutRegister.setVisibility(View.GONE);
        etName.setVisibility(View.GONE);
        layoutProfilePicture.setVisibility(View.GONE);
    }
    
    private void setupListeners() {
        btnLogin.setOnClickListener(v -> {
            if (isLoginMode) {
                performLogin();
            } else {
                performRegister();
            }
        });
        
        tvSwitchMode.setOnClickListener(v -> switchMode());
        
        // اختيار الصورة الشخصية
        layoutProfilePicture.setOnClickListener(v -> showImagePickerDialog());
        ivProfilePicture.setOnClickListener(v -> showImagePickerDialog());
    }
    
    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر صورة شخصية");
        
        String[] options = {"📷 التقاط صورة", "🖼️ اختيار من المعرض"};
        
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // فتح الكاميرا
                Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cameraLauncher.launch(cameraIntent);
            } else {
                // فتح المعرض
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                galleryLauncher.launch(galleryIntent);
            }
        });
        
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }
    
    private void uploadImageToServer(Bitmap bitmap) {
        showToast("جاري رفع الصورة...");
        
        new Thread(() -> {
            try {
                // تحويل Bitmap إلى byte array
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] imageBytes = stream.toByteArray();
                
                // تسمية الملف
                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
                
                // رفع الملف
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> result = 
                    dal.uploadFile(imageBytes, fileName, "image/jpeg", null);
                
                runOnUiThread(() -> {
                    if (result.success && result.data != null) {
                        selectedImageUrl = result.data.fileUrl;
                        Glide.with(LoginActivity.this)
                                .load(bitmap)
                                .circleCrop()
                                .into(ivProfilePicture);
                        ivProfilePicture.setVisibility(View.VISIBLE);
                        showToast("تم رفع الصورة بنجاح!");
                    } else {
                        showToast("فشل رفع الصورة: "
                                + (result.message != null ? result.message : ""));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showToast("حدث خطأ: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void switchMode() {
        isLoginMode = !isLoginMode;
        
        if (isLoginMode) {
            // وضع تسجيل الدخول
            tvTitle.setText("تسجيل الدخول");
            btnLogin.setText("دخول");
            tvSwitchMode.setText("ليس لديك حساب؟ سجل الآن");
            animateViewOut(layoutRegister, () -> {
                layoutRegister.setVisibility(View.GONE);
                etName.setVisibility(View.GONE);
                layoutProfilePicture.setVisibility(View.GONE);
            });
        } else {
            // وضع التسجيل
            tvTitle.setText("إنشاء حساب جديد");
            btnLogin.setText("تسجيل");
            tvSwitchMode.setText("لديك حساب؟ سجل دخول");
            etName.setVisibility(View.VISIBLE);
            layoutRegister.setVisibility(View.VISIBLE);
            layoutProfilePicture.setVisibility(View.VISIBLE);
            animateViewIn(layoutRegister);
            animateViewIn(layoutProfilePicture);
        }
    }
    
    private void performLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("يرجى إدخال البريد الإلكتروني وكلمة المرور");
            return;
        }
        
        showProgress(true);
        
        new Thread(() -> {
            try {
                android.util.Log.d("LOGIN", "بدء تسجيل الدخول للبريد: " + email);
                
                // ما في استعلام بالإيميل فقط من الـ API الجاهز؛ منجيب كل الطلاب ونطابق محلياً (مناسب لحجم بيانات صغير)
                // البحث عن الطالب في قاعدة البيانات
                DALAppWriteConnection.OperationResult<ArrayList<Student>> result = 
                    dal.getData("students", null, Student.class);
                
                runOnUiThread(() -> {
                    showProgress(false);
                    
                    android.util.Log.d("LOGIN", "نتيجة getData - success: " + result.success);
                    android.util.Log.d("LOGIN", "نتيجة getData - message: " + result.message);
                    
                    if (result.success) {
                        if (result.data == null) {
                            android.util.Log.d("LOGIN", "result.data is null");
                            showToast("لا توجد بيانات. يرجى التسجيل أولاً");
                            return;
                        }
                        
                        android.util.Log.d("LOGIN", "عدد الطلاب: " + result.data.size());
                        
                        if (result.data.isEmpty()) {
                            android.util.Log.d("LOGIN", "القائمة فارغة - لا يوجد طلاب مسجلين");
                            showToast("لا يوجد حساب بهذا البريد. يرجى التسجيل أولاً");
                            return;
                        }
                        
                        Student foundStudent = null;
                        for (Student student : result.data) {
                            String studentEmail = student.getEmail();
                            String studentPassword = student.getPassword();
                            
                            android.util.Log.d("LOGIN", "فحص طالب - Email: " + studentEmail + ", Password: " + 
                                (studentPassword != null ? "***" : "null"));
                            
                            if (studentEmail != null && studentEmail.equals(email) && 
                                studentPassword != null && studentPassword.equals(password)) {
                                foundStudent = student;
                                android.util.Log.d("LOGIN", "تم العثور على الطالب: " + student.getName());
                                break;
                            }
                        }
                        
                        if (foundStudent != null) {
                            // التحقق من وجود ID
                            if (foundStudent.getId() == null || foundStudent.getId().isEmpty()) {
                                android.util.Log.e("LOGIN", "الطالب لا يحتوي على ID");
                                showToast("خطأ في بيانات المستخدم. يرجى المحاولة مرة أخرى");
                                return;
                            }
                            
                            // حفظ معلومات تسجيل الدخول
                            saveLoginInfo(foundStudent);
                            showToast("مرحباً " + foundStudent.getName() + "! 🎉");
                            
                            android.util.Log.d("LOGIN", "تم حفظ معلومات تسجيل الدخول، الانتقال للشاشة الرئيسية");
                            
                            // الانتقال للشاشة الرئيسية
                            new Handler().postDelayed(() -> {
                                android.util.Log.d("LOGIN", "بدء startMainActivity");
                                startMainActivity();
                            }, 500);
                        } else {
                            android.util.Log.d("LOGIN", "لم يتم العثور على طالب مطابق");
                            showToast("البريد الإلكتروني أو كلمة المرور غير صحيحة");
                        }
                    } else {
                        android.util.Log.e("LOGIN", "فشل جلب البيانات: " + (result.message != null ? result.message : "خطأ غير معروف"));
                        showToast("خطأ في الاتصال: " + (result.message != null ? result.message : "حاول مرة أخرى"));
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("LOGIN", "خطأ في performLogin: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    showProgress(false);
                    showToast("حدث خطأ: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void performRegister() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            showToast("يرجى إدخال جميع البيانات");
            return;
        }
        
        if (password.length() < 4) {
            showToast("كلمة المرور يجب أن تكون 4 أحرف على الأقل");
            return;
        }
        
        showProgress(true);
        
        new Thread(() -> {
            try {
                // نفس أسلوب الدخول: جلب القائمة ثم التحقق من عدم تكرار الإيميل قبل الإنشاء
                // التحقق من وجود البريد الإلكتروني
                DALAppWriteConnection.OperationResult<ArrayList<Student>> checkResult = 
                    dal.getData("students", null, Student.class);
                
                if (checkResult.success && checkResult.data != null) {
                    for (Student student : checkResult.data) {
                        if (student.getEmail().equals(email)) {
                            runOnUiThread(() -> {
                                showProgress(false);
                                showToast("هذا البريد الإلكتروني مستخدم بالفعل");
                            });
                            return;
                        }
                    }
                }
                
                // إنشاء طالب جديد
                Student newStudent = new Student(name, email, password);
                newStudent.setProfileImageUrl(selectedImageUrl.isEmpty() ? "" : selectedImageUrl);
                if (LearningSettingsPrefs.isAdminTeacherEmail(email)) {
                    newStudent.setIsTeacher(true);
                }
                
                DALAppWriteConnection.OperationResult<ArrayList<Student>> saveResult = 
                    dal.saveData(newStudent, "students", null);
                
                runOnUiThread(() -> {
                    showProgress(false);
                    
                    if (saveResult.success && saveResult.data != null && !saveResult.data.isEmpty()) {
                        Student savedStudent = saveResult.data.get(0);
                        
                        // إنشاء سجل تقدم للطالب
                        createStudentProgress(savedStudent);
                        
                        // حفظ معلومات تسجيل الدخول
                        saveLoginInfo(savedStudent);
                        showToast("تم إنشاء الحساب بنجاح! 🎉");
                        
                        // الانتقال للشاشة الرئيسية
                        new Handler().postDelayed(() -> startMainActivity(), 500);
                    } else {
                        showToast("فشل إنشاء الحساب: " + (saveResult.message != null ? saveResult.message : "خطأ غير معروف"));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    showProgress(false);
                    showToast("حدث خطأ: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void createStudentProgress(Student student) {
        // مستند تقدم فارغ يُربط بمعرف الطالب؛ باقي الشاشات تتحدث عليه لاحقاً
        new Thread(() -> {
            try {
                com.example.RemasProject.model.StudentProgress progress = 
                    new com.example.RemasProject.model.StudentProgress(
                        student.getId(), student.getName());
                dal.saveData(progress, "student_progress", null);
            } catch (Exception e) {
                // تجاهل الأخطاء في إنشاء التقدم
            }
        }).start();
    }
    
    private void saveLoginInfo(Student student) {
        try {
            // القيم دي بتقرأها EnglishLearningActivity والـ DAL عبر نفس اسم ملف الـ prefs
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("studentId", student.getId());
            editor.putString("studentName", student.getName());
            editor.putString("studentEmail", student.getEmail());
            boolean teacherAccess = LearningSettingsPrefs.effectiveIsTeacher(
                    student.isTeacher(), student.getEmail());
            editor.putBoolean(LearningSettingsPrefs.KEY_IS_TEACHER, teacherAccess);
            if (student.getProfileImageUrl() != null) {
                editor.putString("studentProfileImage", student.getProfileImageUrl());
            }
            boolean saved = editor.commit();
            android.util.Log.d("LOGIN", "تم حفظ معلومات تسجيل الدخول: " + saved);
            android.util.Log.d("LOGIN", "studentId: " + student.getId());
            android.util.Log.d("LOGIN", "studentName: " + student.getName());
        } catch (Exception e) {
            android.util.Log.e("LOGIN", "خطأ في حفظ معلومات تسجيل الدخول: " + e.getMessage());
        }
    }
    
    private void startMainActivity() {
        try {
            android.util.Log.d("LOGIN", "بدء startMainActivity");
            
            // التحقق من حفظ البيانات
            String savedId = prefs.getString("studentId", null);
            android.util.Log.d("LOGIN", "savedId من SharedPreferences: " + savedId);
            
            Intent intent = new Intent(LoginActivity.this, EnglishLearningActivity.class);
            // يمسح رجوع المستخدم لشاشات الدخول القديمة من زر الرجوع في النظام
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            
            android.util.Log.d("LOGIN", "تم بدء EnglishLearningActivity");
            
            finish();
            
            android.util.Log.d("LOGIN", "تم إنهاء LoginActivity");
        } catch (Exception e) {
            android.util.Log.e("LOGIN", "خطأ في startMainActivity: " + e.getMessage(), e);
            showToast("خطأ في الانتقال: " + e.getMessage());
        }
    }
    
    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!show);
    }
    
    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    private void animateViews() {
        // أنيميشن للعنوان
        ObjectAnimator titleAnim = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        titleAnim.setDuration(800);
        titleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        titleAnim.start();
        
        // أنيميشن للحقول
        animateViewIn(etEmail);
        new Handler().postDelayed(() -> animateViewIn(etPassword), 100);
        new Handler().postDelayed(() -> animateViewIn(btnLogin), 200);
        new Handler().postDelayed(() -> animateViewIn(tvSwitchMode), 300);
    }
    
    private void animateViewIn(View view) {
        view.setAlpha(0f);
        view.setTranslationY(50f);
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(500)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
    }
    
    private void animateViewOut(View view, Runnable onComplete) {
        view.animate()
            .alpha(0f)
            .translationY(-50f)
            .setDuration(300)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    if (onComplete != null) onComplete.run();
                }
            })
            .start();
    }
}
