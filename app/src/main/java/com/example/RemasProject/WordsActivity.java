package com.example.RemasProject;

import android.animation.ObjectAnimator;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
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

public class WordsActivity extends AppCompatActivity {
    
    private ArabicTextToSpeech arabicTTS;
    private LinearLayout wordsContainer;
    private TextView tvCurrentWord;
    private TextView tvCurrentEmoji;
    private TextView tvToolbarTitle;
    private ImageButton btnBack;
    private Button btnNext, btnPrevious;
    
    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;
    private StudentProgress currentProgress;
    
    // كلمات بسيطة مع أمثلة وترجمة عربية
    private Word[] words = {
        new Word("Apple", "🍎", "A for Apple", "تفاحة"),
        new Word("Ball", "⚽", "B for Ball", "كرة"),
        new Word("Cat", "🐱", "C for Cat", "قطة"),
        new Word("Dog", "🐶", "D for Dog", "كلب"),
        new Word("Elephant", "🐘", "E for Elephant", "فيل"),
        new Word("Fish", "🐟", "F for Fish", "سمكة"),
        new Word("Giraffe", "🦒", "G for Giraffe", "زرافة"),
        new Word("House", "🏠", "H for House", "منزل"),
        new Word("Ice", "🧊", "I for Ice", "ثلج"),
        new Word("Juice", "🧃", "J for Juice", "عصير"),
        new Word("Key", "🗝️", "K for Key", "مفتاح"),
        new Word("Lion", "🦁", "L for Lion", "أسد"),
        new Word("Moon", "🌙", "M for Moon", "قمر"),
        new Word("Nose", "👃", "N for Nose", "أنف"),
        new Word("Orange", "🍊", "O for Orange", "برتقال"),
        new Word("Pencil", "✏️", "P for Pencil", "قلم"),
        new Word("Queen", "👑", "Q for Queen", "ملكة"),
        new Word("Rabbit", "🐰", "R for Rabbit", "أرنب"),
        new Word("Sun", "☀️", "S for Sun", "شمس"),
        new Word("Tree", "🌳", "T for Tree", "شجرة"),
        new Word("Umbrella", "☂️", "U for Umbrella", "مظلة"),
        new Word("Violin", "🎻", "V for Violin", "كمان"),
        new Word("Water", "💧", "W for Water", "ماء"),
        new Word("X-ray", "🩻", "X for X-ray", "أشعة"),
        new Word("Yacht", "⛵", "Y for Yacht", "يخت"),
        new Word("Zebra", "🦓", "Z for Zebra", "حمار وحشي")
    };
    
    private int currentWordIndex = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_words);
        
        initializeToolbar();
        setupSystemBars();
        
        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        dal = new DALAppWriteConnection(this);
        
        tvCurrentWord = findViewById(R.id.tvCurrentWord);
        tvCurrentEmoji = findViewById(R.id.tvCurrentEmoji);
        btnNext = findViewById(R.id.btnNext);
        btnPrevious = findViewById(R.id.btnPrevious);
        wordsContainer = findViewById(R.id.wordsContainer);
        
        // تهيئة TextToSpeech
        arabicTTS = new ArabicTextToSpeech(this, status -> {
            // جاهز للاستخدام
        });
        
        // تحميل التقدم
        loadProgress();
        
        // عرض الكلمة الأولى
        displayCurrentWord();
        
        // أنيميشن للعناصر
        animateViews();
        
        // زر التالي
        btnNext.setOnClickListener(v -> {
            animateButtonClick(btnNext, () -> {
                if (currentWordIndex < words.length - 1) {
                    currentWordIndex++;
                    displayCurrentWord();
                }
            });
        });
        
        // زر السابق
        btnPrevious.setOnClickListener(v -> {
            animateButtonClick(btnPrevious, () -> {
                if (currentWordIndex > 0) {
                    currentWordIndex--;
                    displayCurrentWord();
                }
            });
        });
        
        // عند النقر على الكلمة
        tvCurrentWord.setOnClickListener(v -> speakCurrentWord());
        tvCurrentEmoji.setOnClickListener(v -> speakCurrentWord());
    }
    
    private void initializeToolbar() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        
        tvToolbarTitle.setText("تعلم الكلمات");
        
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
        // أنيميشن للكلمة
        tvCurrentWord.setAlpha(0f);
        tvCurrentWord.setTranslationY(50f);
        tvCurrentWord.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        // أنيميشن للإيموجي
        new Handler().postDelayed(() -> {
            tvCurrentEmoji.setAlpha(0f);
            tvCurrentEmoji.setScaleX(0f);
            tvCurrentEmoji.setScaleY(0f);
            tvCurrentEmoji.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }, 200);
    }
    
    private void displayCurrentWord() {
        Word currentWord = words[currentWordIndex];
        tvCurrentWord.setText(currentWord.getWord());
        if (tvCurrentEmoji != null) {
            tvCurrentEmoji.setText(currentWord.getEmoji());
        }
        
        // تحديث حالة الأزرار
        btnPrevious.setEnabled(currentWordIndex > 0);
        btnNext.setEnabled(currentWordIndex < words.length - 1);
        
        // أنيميشن للكلمة الجديدة
        animateWordChange();
        
        // نطق الكلمة تلقائياً
        speakCurrentWord();
        
        // حفظ التقدم
        markWordLearned(currentWord.getWord());
    }
    
    private void animateWordChange() {
        tvCurrentWord.setAlpha(0f);
        tvCurrentWord.setTranslationX(100f);
        tvCurrentWord.animate()
            .alpha(1f)
            .translationX(0f)
            .setDuration(400)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        tvCurrentEmoji.setScaleX(0.5f);
        tvCurrentEmoji.setScaleY(0.5f);
        tvCurrentEmoji.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(400)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
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
                    .withEndAction(onComplete)
                    .start();
            })
            .start();
    }
    
    private void speakCurrentWord() {
        if (arabicTTS != null && currentWordIndex < words.length) {
            Word word = words[currentWordIndex];
            // نطق بالإنجليزية
            arabicTTS.speakEnglish(word.getPhrase());
            
            // نطق المساعدة بالعربية
            String arabicText = "الكلمة " + word.getWord() + " تعني " + word.getArabicTranslation();
            new Handler().postDelayed(() -> {
                arabicTTS.speakArabic(arabicText);
            }, 1000);
        }
    }
    
    private void markWordLearned(String word) {
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
                currentProgress.markWordLearned(word);
                
                // حفظ في قاعدة البيانات
                dal.saveData(currentProgress, "student_progress", null);
            } catch (Exception e) {
                // تجاهل الأخطاء
            }
        }).start();
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
    
    // كلاس Word
    private static class Word {
        private String word;
        private String emoji;
        private String phrase;
        private String arabicTranslation;
        
        public Word(String word, String emoji, String phrase, String arabicTranslation) {
            this.word = word;
            this.emoji = emoji;
            this.phrase = phrase;
            this.arabicTranslation = arabicTranslation;
        }
        
        public String getWord() { return word; }
        public String getEmoji() { return emoji; }
        public String getPhrase() { return phrase; }
        public String getArabicTranslation() { return arabicTranslation; }
    }
}
