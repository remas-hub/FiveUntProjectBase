package com.example.RemasProject;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.model.StudentProgress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LettersActivity extends AppCompatActivity {
    
    private ArabicTextToSpeech arabicTTS;
    private GridLayout gridLayout;
    private TextView tvToolbarTitle;
    private ImageButton btnBack;
    private String[] letters = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", 
                                "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", 
                                "U", "V", "W", "X", "Y", "Z"};
    
    // ترجمة الحروف للعربية
    private String[] arabicHelp = {
        "أي", "بي", "سي", "دي", "إي", "إف", "جي", "إتش", "آي", "جاي",
        "كي", "إل", "إم", "إن", "أو", "بي", "كيو", "آر", "إس", "تي",
        "يو", "في", "دبليو", "إكس", "واي", "زد"
    };
    
    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;
    private StudentProgress currentProgress;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letters);
        
        initializeToolbar();
        setupSystemBars();
        
        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        dal = new DALAppWriteConnection(this);
        
        gridLayout = findViewById(R.id.gridLetters);
        
        // تهيئة TextToSpeech
        arabicTTS = new ArabicTextToSpeech(this, status -> {
            // جاهز للاستخدام
        });
        
        // تحميل التقدم
        loadProgress();
        
        // إنشاء أزرار الحروف
        createLetterButtons();
    }
    
    private void initializeToolbar() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        
        tvToolbarTitle.setText("تعلم الحروف");
        
        btnBack.setOnClickListener(v -> onBackPressed());
    }
    
    private void loadProgress() {
        new Thread(() -> {
            try {
                DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> result = 
                    dal.getData("student_progress", null, StudentProgress.class);
                
                if (result.success && result.data != null && studentId != null) {
                    for (StudentProgress p : result.data) {
                        if (p.getStudentId().equals(studentId)) {
                            currentProgress = p;
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                // تجاهل الأخطاء
            }
        }).start();
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
    
    private void createLetterButtons() {
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int padding = 16;
        int columns = 4;
        int buttonSize = (screenWidth - (padding * (columns + 1))) / columns;
        
        // ألوان جذابة للأطفال
        int[] colors = {
            0xFFFF6B6B, 0xFFFFD93D, 0xFF6BCF7F, 0xFF4ECDC4,
            0xFF45B7D1, 0xFF96CEB4, 0xFFFFEAA7, 0xFFDDA15E,
            0xFFFF8C94, 0xFFA8E6CF, 0xFFFFD3A5, 0xFFC7CEEA,
            0xFFFFB6C1, 0xFFB4E7CE, 0xFFFFE4B5, 0xFFD4A5A5,
            0xFFFFCCCB, 0xFFB5EAD7, 0xFFFFE5B4, 0xFFC8B6FF,
            0xFFFFB3BA, 0xFFBAFFC9, 0xFFFFF4A3, 0xFFD4A5FF,
            0xFFFF9AA2, 0xFF9AFFC7
        };
        
        for (int i = 0; i < letters.length; i++) {
            String letter = letters[i];
            Button btn = new Button(this);
            btn.setText(letter);
            btn.setTextSize(28);
            btn.setAllCaps(false);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // تصميم الأزرار
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = buttonSize;
            params.height = buttonSize;
            params.setMargins(padding / 2, padding / 2, padding / 2, padding / 2);
            btn.setLayoutParams(params);
            
            // ألوان جذابة
            int colorIndex = i % colors.length;
            btn.setBackgroundColor(colors[colorIndex]);
            btn.setTextColor(0xFF000000);
            
            // التحقق من الحروف المتعلمة
            if (currentProgress != null && 
                currentProgress.getLettersLearned().containsKey(letter) &&
                currentProgress.getLettersLearned().get(letter)) {
                // إضافة علامة ✓ للحروف المتعلمة
                btn.setText(letter + " ✓");
            }
            
            // عند الضغط على الحرف
            final String letterToSpeak = letter;
            final int index = i;
            btn.setOnClickListener(v -> {
                animateButtonClick(btn, () -> {
                    speakLetter(letterToSpeak, index);
                    markLetterLearned(letterToSpeak);
                });
            });
            
            gridLayout.addView(btn);
            
            // أنيميشن للأزرار
            animateButtonIn(btn, i * 50);
        }
    }
    
    private void animateButtonIn(Button button, long delay) {
        new Handler().postDelayed(() -> {
            button.setAlpha(0f);
            button.setScaleX(0f);
            button.setScaleY(0f);
            button.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(400)
                .setInterpolator(new BounceInterpolator())
                .start();
        }, delay);
    }
    
    private void animateButtonClick(Button button, Runnable onComplete) {
        button.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction(() -> {
                button.animate()
                    .scaleX(1.1f)
                    .scaleY(1.1f)
                    .setDuration(150)
                    .withEndAction(() -> {
                        button.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .setInterpolator(new BounceInterpolator())
                            .withEndAction(onComplete)
                            .start();
                    })
                    .start();
            })
            .start();
    }
    
    private void speakLetter(String letter, int index) {
        if (arabicTTS != null) {
            // نطق الحرف بالإنجليزية
            arabicTTS.speakEnglish(letter);
            
            // نطق المساعدة بالعربية
            if (index >= 0 && index < arabicHelp.length) {
                String arabicText = "الحرف " + letter + " يلفظ " + arabicHelp[index];
                new Handler().postDelayed(() -> {
                    arabicTTS.speakArabic(arabicText);
                }, 800);
            }
        }
    }
    
    private void markLetterLearned(String letter) {
        if (studentId == null) return;
        
        new Thread(() -> {
            try {
                // تحميل التقدم الحالي
                if (currentProgress == null) {
                    DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> result = 
                        dal.getData("student_progress", null, StudentProgress.class);
                    
                    if (result.success && result.data != null) {
                        for (StudentProgress p : result.data) {
                            if (p.getStudentId().equals(studentId)) {
                                currentProgress = p;
                                break;
                            }
                        }
                    }
                    
                    // إنشاء تقدم جديد إذا لم يوجد
                    if (currentProgress == null) {
                        String studentName = prefs.getString("studentName", "طالب");
                        currentProgress = new StudentProgress(studentId, studentName);
                    }
                }
                
                // تحديث التقدم
                currentProgress.markLetterLearned(letter);
                
                // حفظ في قاعدة البيانات
                dal.saveData(currentProgress, "student_progress", null);
                
                runOnUiThread(() -> {
                    Toast.makeText(this, "ممتاز! تعلمت الحرف " + letter + " 🎉", 
                        Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                // تجاهل الأخطاء
            }
        }).start();
    }
    
    private void setupSystemBars() {
        View toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
                int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
                
                ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
                if (params != null) {
                    params.topMargin = topInset;
                    v.setLayoutParams(params);
                }
                
                return insets;
            });
        }
    }
    
    @Override
    protected void onDestroy() {
        if (arabicTTS != null) {
            arabicTTS.shutdown();
        }
        super.onDestroy();
    }
}
