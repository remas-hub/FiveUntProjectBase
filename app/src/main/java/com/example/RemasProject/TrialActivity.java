package com.example.RemasProject;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class TrialActivity extends AppCompatActivity {
    
    private TextToSpeech textToSpeech;
    private TextView tvTrialTitle, tvTrialDescription, tvLetter, tvWord;
    private Button btnTryLetter, btnTryWord, btnContinue;
    private LinearLayout layoutTrialContent;
    
    private String[] trialLetters = {"A", "B", "C"};
    private String[] trialWords = {"Apple", "Ball", "Cat"};
    private String[] trialEmojis = {"🍎", "⚽", "🐱"};
    private int currentTrialIndex = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trial);
        
        initializeViews();
        setupTrialContent();
        animateViews();
    }
    
    private void initializeViews() {
        tvTrialTitle = findViewById(R.id.tvTrialTitle);
        tvTrialDescription = findViewById(R.id.tvTrialDescription);
        tvLetter = findViewById(R.id.tvLetter);
        tvWord = findViewById(R.id.tvWord);
        btnTryLetter = findViewById(R.id.btnTryLetter);
        btnTryWord = findViewById(R.id.btnTryWord);
        btnContinue = findViewById(R.id.btnContinue);
        layoutTrialContent = findViewById(R.id.layoutTrialContent);
        
        // تهيئة TextToSpeech
        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = textToSpeech.setLanguage(Locale.US);
                if (result == TextToSpeech.LANG_MISSING_DATA || 
                    result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // تجاهل الخطأ
                }
            }
        });
        
        // زر تجربة الحرف
        btnTryLetter.setOnClickListener(v -> {
            animateButtonClick(btnTryLetter, () -> {
                speakLetter(trialLetters[currentTrialIndex]);
                animateLetter();
            });
        });
        
        // زر تجربة الكلمة
        btnTryWord.setOnClickListener(v -> {
            animateButtonClick(btnTryWord, () -> {
                speakWord(trialWords[currentTrialIndex]);
                animateWord();
            });
        });
        
        // زر المتابعة
        btnContinue.setOnClickListener(v -> {
            animateButtonClick(btnContinue, () -> {
                navigateToLogin();
            });
        });
    }
    
    private void setupTrialContent() {
        // عرض الحرف والكلمة الأولى
        updateTrialContent();
    }
    
    private void updateTrialContent() {
        tvLetter.setText(trialLetters[currentTrialIndex]);
        tvWord.setText(trialWords[currentTrialIndex] + " " + trialEmojis[currentTrialIndex]);
    }
    
    private void animateViews() {
        // أنيميشن العنوان
        tvTrialTitle.setAlpha(0f);
        tvTrialTitle.setTranslationY(-50f);
        tvTrialTitle.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();
        
        // أنيميشن الوصف
        new Handler().postDelayed(() -> {
            tvTrialDescription.setAlpha(0f);
            tvTrialDescription.setTranslationY(30f);
            tvTrialDescription.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }, 200);
        
        // أنيميشن المحتوى
        new Handler().postDelayed(() -> {
            layoutTrialContent.setAlpha(0f);
            layoutTrialContent.setTranslationY(50f);
            layoutTrialContent.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(600)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .start();
        }, 400);
        
        // أنيميشن الأزرار
        animateButtonIn(btnTryLetter, 600);
        animateButtonIn(btnTryWord, 700);
        animateButtonIn(btnContinue, 800);
    }
    
    private void animateButtonIn(Button button, long delay) {
        new Handler().postDelayed(() -> {
            button.setAlpha(0f);
            button.setTranslationY(30f);
            button.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new BounceInterpolator())
                .start();
        }, delay);
    }
    
    private void animateButtonClick(Button button, Runnable onComplete) {
        button.animate()
            .scaleX(0.95f)
            .scaleY(0.95f)
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
    
    private void animateLetter() {
        tvLetter.setScaleX(0.8f);
        tvLetter.setScaleY(0.8f);
        tvLetter.animate()
            .scaleX(1.2f)
            .scaleY(1.2f)
            .setDuration(200)
            .withEndAction(() -> {
                tvLetter.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new BounceInterpolator())
                    .start();
            })
            .start();
    }
    
    private void animateWord() {
        tvWord.setScaleX(0.8f);
        tvWord.setScaleY(0.8f);
        tvWord.animate()
            .scaleX(1.1f)
            .scaleY(1.1f)
            .setDuration(200)
            .withEndAction(() -> {
                tvWord.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .setInterpolator(new BounceInterpolator())
                    .start();
            })
            .start();
    }
    
    private void speakLetter(String letter) {
        if (textToSpeech != null) {
            textToSpeech.speak(letter, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    private void speakWord(String word) {
        if (textToSpeech != null) {
            textToSpeech.speak(word, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }
    
    private void navigateToLogin() {
        // حفظ أن المستخدم شاهد صفحة التجربة
        SharedPreferences prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("hasSeenTrial", true);
        editor.apply();
        
        Intent intent = new Intent(TrialActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}

