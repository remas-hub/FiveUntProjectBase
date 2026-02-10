package com.example.RemasProject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class EnglishLearningActivity extends AppCompatActivity {

    private TextView tvStudentName, tvProgress, tvWelcome;
    private Button btnLetters, btnWords, btnPractice;
    private LinearLayout btnHome, btnProgress, btnSettings, btnLogout;
    private TextToSpeech tts;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_english_learning);

        // تهيئة العناصر
        tvStudentName = findViewById(R.id.tvStudentName);
        tvProgress = findViewById(R.id.tvProgress);
        tvWelcome = findViewById(R.id.tvWelcome);

        btnLetters = findViewById(R.id.btnLetters);
        btnWords = findViewById(R.id.btnWords);
        btnPractice = findViewById(R.id.btnPractice);

        btnHome = findViewById(R.id.btnHome);
        btnProgress = findViewById(R.id.btnProgress);
        btnSettings = findViewById(R.id.btnSettings);
        btnLogout = findViewById(R.id.btnLogout);

        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);

        loadStudentInfo();
        setupButtons();
        initializeTTS();
    }

    private void loadStudentInfo() {
        String studentName = prefs.getString("studentName", "طالب");
        int progress = prefs.getInt("progress", 0); // نسبة التقدم مخزنة مسبقاً
        tvStudentName.setText(studentName);
        tvProgress.setText("التقدم: " + progress + "%");
        tvWelcome.setText("مرحباً، " + studentName + "!");
    }

    private void setupButtons() {
        // الأزرار الرئيسية
        btnLetters.setOnClickListener(v -> {
            Intent intent = new Intent(EnglishLearningActivity.this, LettersActivity.class);
            startActivity(intent);
        });

        btnWords.setOnClickListener(v -> {
            Intent intent = new Intent(EnglishLearningActivity.this, WordsActivity.class);
            startActivity(intent);
        });

        btnPractice.setOnClickListener(v -> {
            Intent intent = new Intent(EnglishLearningActivity.this, PracticeActivity.class);
            startActivity(intent);
        });

        // شريط السفلي
        btnHome.setOnClickListener(v -> {
            Toast.makeText(this, "أنت في الصفحة الرئيسية", Toast.LENGTH_SHORT).show();
        });

        btnProgress.setOnClickListener(v -> {
            Intent intent = new Intent(EnglishLearningActivity.this, ProgressActivity.class);
            startActivity(intent);
        });

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(EnglishLearningActivity.this, SettingsActivity.class);
            startActivity(intent);
        });

        btnLogout.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Intent intent = new Intent(EnglishLearningActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void initializeTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("ar", "SA"));
                tts.speak("مرحباً " + tvStudentName.getText(), TextToSpeech.QUEUE_FLUSH, null, null);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}