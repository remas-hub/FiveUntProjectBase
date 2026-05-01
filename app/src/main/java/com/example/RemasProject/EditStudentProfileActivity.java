/**
 * تعديل اسم الطالب وصورته — رفع للتخزين، تحديث السجل على السيرفر، والرجوع لما تخلص.
 */
package com.example.RemasProject;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.RemasProject.Hellper.AppwriteImageLoader;
import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.Hellper.UiInsetsHelper;
import com.example.RemasProject.model.Student;

import java.io.ByteArrayOutputStream;

/**
 * تعديل اسم الطالب وصورته من الشاشة الرئيسية.
 */
public class EditStudentProfileActivity extends AppCompatActivity {

    private static final String PREFS = "EnglishLearning";

    private EditText etName;
    private ImageView ivProfile;
    private Button btnSave;
    private ProgressBar progressBar;
    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;
    private String selectedImageUrl = "";

    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_student_profile);

        UiInsetsHelper.enableEdgeToEdge(this);
        View content = findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            UiInsetsHelper.applySafePadding(((ViewGroup) content).getChildAt(0));
        }

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        dal = new DALAppWriteConnection(this);
        studentId = prefs.getString("studentId", null);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarTitle.setText("تعديل الاسم والصورة");
        btnBack.setOnClickListener(v -> onBackPressed());

        etName = findViewById(R.id.etStudentNameEdit);
        ivProfile = findViewById(R.id.ivProfileEdit);
        btnSave = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressSaveProfile);

        etName.setText(prefs.getString("studentName", ""));
        selectedImageUrl = prefs.getString("studentProfileImage", "");
        AppwriteImageLoader.loadCircleProfile(this, ivProfile, selectedImageUrl,
                android.R.drawable.ic_menu_myplaces);

        registerImageLaunchers();
        ivProfile.setOnClickListener(v -> showImagePickerDialog());

        btnSave.setOnClickListener(v -> saveProfile());
    }

    private void registerImageLaunchers() {
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Bundle extras = result.getData().getExtras();
                    Bitmap imageBitmap = extras != null ? (Bitmap) extras.get("data") : null;
                    if (imageBitmap != null) {
                        uploadImageToServer(imageBitmap);
                    }
                });

        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() != RESULT_OK || result.getData() == null) {
                        return;
                    }
                    Uri imageUri = result.getData().getData();
                    if (imageUri == null) {
                        return;
                    }
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                        uploadImageToServer(bitmap);
                    } catch (Exception e) {
                        toast("خطأ في قراءة الصورة: " + e.getMessage());
                    }
                });
    }

    private void showImagePickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("اختر صورة شخصية");
        builder.setItems(new String[]{"التقاط صورة", "اختيار من المعرض"}, (dialog, which) -> {
            if (which == 0) {
                cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
            } else {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK);
                galleryIntent.setType("image/*");
                galleryLauncher.launch(galleryIntent);
            }
        });
        builder.setNegativeButton("إلغاء", null);
        builder.show();
    }

    private void uploadImageToServer(Bitmap bitmap) {
        toast("جاري رفع الصورة…");
        new Thread(() -> {
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream);
                byte[] imageBytes = stream.toByteArray();
                String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
                DALAppWriteConnection.OperationResult<DALAppWriteConnection.FileInfo> result =
                        dal.uploadFile(imageBytes, fileName, "image/jpeg", null);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (result.success && result.data != null) {
                        selectedImageUrl = result.data.fileUrl;
                        Glide.with(EditStudentProfileActivity.this).load(bitmap).circleCrop().into(ivProfile);
                        toast("تم رفع الصورة");
                    } else {
                        toast("فشل رفع الصورة: " + (result.message != null ? result.message : ""));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> toast("حدث خطأ: " + e.getMessage()));
            }
        }).start();
    }

    private void saveProfile() {
        String name = etName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            toast("يرجى إدخال الاسم");
            return;
        }

        setBusy(true);

        new Thread(() -> {
            boolean remoteOk = false;
            if (studentId != null && !studentId.isEmpty()) {
                // جلب المستند الحالي ثم PATCH حتى لا نفقد حقول ما ظهرت بالنموذج (مثل isTeacher)
                DALAppWriteConnection.OperationResult<Student> fetch =
                        dal.getDataById("students", studentId, null, Student.class);
                if (fetch.success && fetch.data != null) {
                    Student s = fetch.data;
                    s.setName(name);
                    if (selectedImageUrl != null && !selectedImageUrl.isEmpty()) {
                        s.setProfileImageUrl(selectedImageUrl);
                    }
                    DALAppWriteConnection.OperationResult<Student> upd =
                            dal.updateData(s, "students", studentId, null);
                    remoteOk = upd.success;
                }
            }

            // الاسم والصورة للعرض الفوري على الرئيسية حتى لو فشل التحديث على السيرفر
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString("studentName", name);
            if (selectedImageUrl != null && !selectedImageUrl.isEmpty()) {
                ed.putString("studentProfileImage", selectedImageUrl);
            }
            ed.commit();

            boolean finalRemoteOk = remoteOk;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                setBusy(false);
                if (studentId == null || studentId.isEmpty()) {
                    toast("تم حفظ الاسم محلياً (سجّل الدخول لمزامنة السحابة)");
                } else if (finalRemoteOk) {
                    toast("تم حفظ التعديلات");
                } else {
                    toast("تم الحفظ محلياً؛ تعذّر تحديث السحابة حالياً");
                }
                finish();
            });
        }).start();
    }

    private void setBusy(boolean busy) {
        progressBar.setVisibility(busy ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!busy);
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }
}
