package com.example.RemasProject;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
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
import java.util.Locale;
import java.util.Random;

public class PracticeActivity extends AppCompatActivity {
    
    private ArabicTextToSpeech arabicTTS;
    private TextView tvQuestion, tvScore, tvToolbarTitle;
    private ImageButton btnBack;
    private Button btnOption1, btnOption2, btnOption3, btnOption4;
    private LinearLayout optionsLayout;
    
    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;
    private StudentProgress currentProgress;
    
    private String[] words = {"Apple", "Ball", "Cat", "Dog", "Elephant", "Fish", 
                             "Giraffe", "House", "Ice", "Juice", "Key", "Lion"};
    
    // ترجمة الكلمات للعربية
    private String[] arabicTranslations = {
        "تفاحة", "كرة", "قطة", "كلب", "فيل", "سمكة",
        "زرافة", "منزل", "ثلج", "عصير", "مفتاح", "أسد"
    };
    
    private String currentCorrectWord;
    private int score = 0;
    private int totalQuestions = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);
        
        initializeToolbar();
        setupSystemBars();
        
        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        dal = new DALAppWriteConnection(this);
        
        tvQuestion = findViewById(R.id.tvQuestion);
        tvScore = findViewById(R.id.tvScore);
        btnOption1 = findViewById(R.id.btnOption1);
        btnOption2 = findViewById(R.id.btnOption2);
        btnOption3 = findViewById(R.id.btnOption3);
        btnOption4 = findViewById(R.id.btnOption4);
        optionsLayout = findViewById(R.id.optionsLayout);
        
        // تهيئة TextToSpeech
        arabicTTS = new ArabicTextToSpeech(this, status -> {
            // جاهز للاستخدام
        });
        
        // تحميل التقدم
        loadProgress();
        
        // بدء اللعبة
        generateNewQuestion();
        
        // أنيميشن للعناصر
        animateViews();
        
        // إعداد مستمعي الأزرار
        btnOption1.setOnClickListener(v -> {
            animateButtonClick(btnOption1, () -> checkAnswer(btnOption1.getText().toString()));
        });
        btnOption2.setOnClickListener(v -> {
            animateButtonClick(btnOption2, () -> checkAnswer(btnOption2.getText().toString()));
        });
        btnOption3.setOnClickListener(v -> {
            animateButtonClick(btnOption3, () -> checkAnswer(btnOption3.getText().toString()));
        });
        btnOption4.setOnClickListener(v -> {
            animateButtonClick(btnOption4, () -> checkAnswer(btnOption4.getText().toString()));
        });
    }
    
    private void initializeToolbar() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        
        tvToolbarTitle.setText("لعبة الممارسة");
        
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
    
    private void animateViews() {
        // أنيميشن للسؤال
        tvQuestion.setAlpha(0f);
        tvQuestion.setTranslationY(-50f);
        tvQuestion.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        // أنيميشن للأزرار
        animateButtonIn(btnOption1, 200);
        animateButtonIn(btnOption2, 300);
        animateButtonIn(btnOption3, 400);
        animateButtonIn(btnOption4, 500);
    }
    
    private void animateButtonIn(Button button, long delay) {
        new Handler().postDelayed(() -> {
            button.setAlpha(0f);
            button.setTranslationY(50f);
            button.animate()
                .alpha(1f)
                .translationY(0f)
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
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100)
                    .setInterpolator(new BounceInterpolator())
                    .withEndAction(onComplete)
                    .start();
            })
            .start();
    }
    
    private void generateNewQuestion() {
        Random random = new Random();
        
        // اختيار كلمة صحيحة عشوائية
        int wordIndex = random.nextInt(words.length);
        currentCorrectWord = words[wordIndex];
        
        // تحديث السؤال
        tvQuestion.setText("اختر الكلمة: " + currentCorrectWord);
        
        // نطق الكلمة بالإنجليزية
        if (arabicTTS != null) {
            arabicTTS.speakEnglish("Find: " + currentCorrectWord);
        }
        
        // إنشاء خيارات (3 خاطئة + 1 صحيحة)
        String[] options = new String[4];
        options[0] = currentCorrectWord;
        
        // إضافة كلمات خاطئة
        for (int i = 1; i < 4; i++) {
            String wrongWord;
            do {
                wrongWord = words[random.nextInt(words.length)];
            } while (wrongWord.equals(currentCorrectWord) || contains(options, wrongWord));
            options[i] = wrongWord;
        }
        
        // خلط الخيارات
        shuffleArray(options);
        
        // عرض الخيارات مع أنيميشن
        animateOptionsChange();
        btnOption1.setText(options[0]);
        btnOption2.setText(options[1]);
        btnOption3.setText(options[2]);
        btnOption4.setText(options[3]);
        
        // تحديث العداد
        totalQuestions++;
        updateScore();
    }
    
    private void animateOptionsChange() {
        Button[] buttons = {btnOption1, btnOption2, btnOption3, btnOption4};
        for (Button btn : buttons) {
            btn.setAlpha(0f);
            btn.setScaleX(0.8f);
            btn.setScaleY(0.8f);
            btn.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }
    }
    
    private void checkAnswer(String selectedAnswer) {
        boolean isCorrect = selectedAnswer.equals(currentCorrectWord);
        
        if (isCorrect) {
            score++;
            Toast.makeText(this, "صحيح! 🎉", Toast.LENGTH_SHORT).show();
            
            // نطق بالإنجليزية
            if (arabicTTS != null) {
                arabicTTS.speakEnglish("Correct! " + currentCorrectWord);
            }
            
            // أنيميشن للزر الصحيح
            Button correctButton = getButtonByText(selectedAnswer);
            if (correctButton != null) {
                animateCorrectAnswer(correctButton);
            }
        } else {
            Toast.makeText(this, "حاول مرة أخرى!", Toast.LENGTH_SHORT).show();
            
            // نطق الجواب بالعربية
            if (arabicTTS != null) {
                int wordIndex = getWordIndex(currentCorrectWord);
                String arabicTranslation = (wordIndex >= 0 && wordIndex < arabicTranslations.length) 
                    ? arabicTranslations[wordIndex] : "";
                
                String arabicText = "الجواب هو " + currentCorrectWord + " " + arabicTranslation;
                arabicTTS.speakArabic(arabicText);
            }
            
            // أنيميشن للزر الخاطئ
            Button wrongButton = getButtonByText(selectedAnswer);
            if (wrongButton != null) {
                animateWrongAnswer(wrongButton);
            }
        }
        
        // حفظ التقدم
        savePracticeResult(score, isCorrect);
        
        updateScore();
        
        // تأخير بسيط ثم سؤال جديد
        new Handler().postDelayed(() -> generateNewQuestion(), 2500);
    }
    
    private int getWordIndex(String word) {
        for (int i = 0; i < words.length; i++) {
            if (words[i].equals(word)) {
                return i;
            }
        }
        return -1;
    }
    
    private Button getButtonByText(String text) {
        if (btnOption1.getText().toString().equals(text)) return btnOption1;
        if (btnOption2.getText().toString().equals(text)) return btnOption2;
        if (btnOption3.getText().toString().equals(text)) return btnOption3;
        if (btnOption4.getText().toString().equals(text)) return btnOption4;
        return null;
    }
    
    private void animateCorrectAnswer(Button button) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(button, "scaleX", 1f, 1.2f, 1f);
        anim.setDuration(500);
        anim.setInterpolator(new BounceInterpolator());
        anim.start();
        
        ObjectAnimator animY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 1.2f, 1f);
        animY.setDuration(500);
        animY.setInterpolator(new BounceInterpolator());
        animY.start();
    }
    
    private void animateWrongAnswer(Button button) {
        ObjectAnimator anim = ObjectAnimator.ofFloat(button, "translationX", 0f, -20f, 20f, -20f, 20f, 0f);
        anim.setDuration(500);
        anim.start();
    }
    
    private void savePracticeResult(int currentScore, boolean isCorrect) {
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
                currentProgress.updatePracticeResult(currentScore, isCorrect);
                
                // حفظ في قاعدة البيانات
                dal.saveData(currentProgress, "student_progress", null);
            } catch (Exception e) {
                // تجاهل الأخطاء
            }
        }).start();
    }
    
    private void updateScore() {
        tvScore.setText(String.format("النقاط: %d / %d", score, totalQuestions));
        
        // أنيميشن لتحديث النقاط
        ObjectAnimator anim = ObjectAnimator.ofFloat(tvScore, "scaleX", 1f, 1.1f, 1f);
        anim.setDuration(300);
        anim.start();
    }
    
    private boolean contains(String[] array, String value) {
        for (String s : array) {
            if (s != null && s.equals(value)) {
                return true;
            }
        }
        return false;
    }
    
    private void shuffleArray(String[] array) {
        Random random = new Random();
        for (int i = array.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            String temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
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
