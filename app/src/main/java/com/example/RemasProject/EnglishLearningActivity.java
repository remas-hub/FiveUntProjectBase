/**
 * الشاشة الرئيسية بعد الدخول — إحصائيات سريعة، صورة الطالب، وزر كبير يودّيك لتعلّم الحروف؛ فيها كمان الشريط السفلي والإعدادات حسب الصلاحيات.
 */
package com.example.RemasProject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.app.AppCompatActivity;

import com.example.RemasProject.Hellper.AppwriteImageLoader;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.Hellper.LearningBottomNavHelper;
import com.example.RemasProject.Hellper.LearningSettingsPrefs;
import com.example.RemasProject.Hellper.LetterQuizHistoryLocal;
import com.example.RemasProject.Hellper.UiInsetsHelper;
import com.example.RemasProject.model.LetterQuizSession;
import com.example.RemasProject.model.StudentProgress;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

public class EnglishLearningActivity extends AppCompatActivity {

    private TextView tvStudentName;
    private TextView tvWelcome;
    private TextView tvHomeSubtitle;
    private TextView tvStatLettersRequired;
    private TextView tvStatQuizSessions;
    private TextView tvStatLettersPassed;
    private TextView tvStatLettersRemaining;
    private ImageView ivProfile;
    private AppCompatButton btnLetters;
    private TextToSpeech tts;
    private SharedPreferences prefs;

    private final AtomicInteger statsLoadGeneration = new AtomicInteger(0);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_english_learning);

        UiInsetsHelper.enableEdgeToEdge(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);
        LinearLayout topBar = findViewById(R.id.topBar);
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        UiInsetsHelper.applyStatusBarPadding(topBar);
        UiInsetsHelper.applyNavigationBarPadding(bottomBar);

        tvStudentName = findViewById(R.id.tvStudentName);
        ivProfile = findViewById(R.id.ivProfile);
        tvWelcome = findViewById(R.id.tvWelcome);
        tvHomeSubtitle = findViewById(R.id.tvHomeSubtitle);
        tvStatLettersRequired = findViewById(R.id.tvStatLettersRequired);
        tvStatQuizSessions = findViewById(R.id.tvStatQuizSessions);
        tvStatLettersPassed = findViewById(R.id.tvStatLettersPassed);
        tvStatLettersRemaining = findViewById(R.id.tvStatLettersRemaining);
        btnLetters = findViewById(R.id.btnLetters);

        prefs = getSharedPreferences(LearningSettingsPrefs.PREFS_NAME, MODE_PRIVATE);

        // الاسم والترحيب من الـ prefs؛ الإحصائيات بتيجي لاحقاً من السحابة في خيط منفصل
        loadStudentInfo();
        loadProfileImageInTopBar();
        setupProfileEntryClick();
        setupLettersButton();
        initializeTTS();

        LearningBottomNavHelper.bindFromMain(this);
        refreshLetterDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStudentInfo();
        loadProfileImageInTopBar();
        refreshLetterDashboardStats();
    }

    private void setupProfileEntryClick() {
        View.OnClickListener openEdit = v ->
                startActivity(new Intent(this, EditStudentProfileActivity.class));
        tvStudentName.setOnClickListener(openEdit);
        ivProfile.setOnClickListener(openEdit);
    }

    private void loadProfileImageInTopBar() {
        if (ivProfile == null) {
            return;
        }
        String url = prefs.getString("studentProfileImage", null);
        AppwriteImageLoader.loadCircleProfile(this, ivProfile, url,
                android.R.drawable.ic_menu_myplaces);
    }

    private void loadStudentInfo() {
        String studentName = prefs.getString("studentName", "طالب");
        tvStudentName.setText(studentName);
        tvWelcome.setText("مرحباً، " + studentName + "!");
        tvHomeSubtitle.setText("تابع خطتك بالحروف بخطوة هادئة وواضحة — بدون تعقيد.");
    }

    /**
     * يحدّث بطاقة الإحصائيات من الإعدادات الحالية وتقدّم الطالب (محليًا + السحابة عند الإمكان).
     */
    private void refreshLetterDashboardStats() {
        // كل ما تنادي الشاشة التحديث بنرفع رقم الجيل؛ أي رد قديم من الشبكة بنتجاهله لو الجيل تغيّر (منع خلط نتائج)
        final int gen = statsLoadGeneration.incrementAndGet();
        List<String> enabledList = LearningSettingsPrefs.getEnabledLettersOrdered(prefs);
        final int required = enabledList.size();

        runOnUiThread(() -> {
            if (gen != statsLoadGeneration.get()) {
                return;
            }
            tvStatLettersRequired.setText(String.valueOf(required));
            tvStatQuizSessions.setText("…");
            tvStatLettersPassed.setText("…");
            tvStatLettersRemaining.setText("…");
        });

        String studentId = prefs.getString("studentId", null);
        if (studentId == null || studentId.trim().isEmpty()) {
            runOnUiThread(() -> {
                if (gen != statsLoadGeneration.get()) {
                    return;
                }
                tvStatQuizSessions.setText("0");
                tvStatLettersPassed.setText("0");
                tvStatLettersRemaining.setText(String.valueOf(required));
            });
            return;
        }

        new Thread(() -> {
            int quizSessions = 0;
            int passedLetters = 0;
            try {
                DALAppWriteConnection dal = new DALAppWriteConnection(EnglishLearningActivity.this);
                DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progResult =
                        dal.getStudentProgressForStudent(studentId);
                StudentProgress progress = null;
                if (progResult.success && progResult.data != null && !progResult.data.isEmpty()) {
                    ArrayList<StudentProgress> plist = progResult.data;
                    // قد يكون في أكثر من مستند تقدم لنفس الطالب (نسخ قديمة)؛ الأحدث أساس والباقي يدمج فيه
                    progress = DALAppWriteConnection.pickNewestStudentProgress(plist);
                    if (progress != null) {
                        for (StudentProgress p : plist) {
                            if (p != progress) {
                                progress.mergeLetterStateFrom(p);
                            }
                        }
                        progress.normalizeLetterMapKeys();
                    }
                }
                if (progress == null) {
                    String name = prefs.getString("studentName", "طالب");
                    progress = new StudentProgress(studentId, name);
                }
                // من السجل المحلي + السحابة: نحدّث خرائط «نجح الاختبار» حتى تنعكس على العدّ حتى لو تعذّر حفظها بالكامل على السيرفر
                LetterQuizHistoryLocal.applyPassedFullFromHistoryToProgress(
                        getApplicationContext(), studentId, progress);
                ArrayList<LetterQuizSession> merged =
                        LetterQuizHistoryLocal.mergedWithRemote(getApplicationContext(), studentId, progress);
                quizSessions = merged.size();
                for (String letter : enabledList) {
                    if (Boolean.TRUE.equals(progress.getLettersQuizPassed().get(letter))) {
                        passedLetters++;
                    }
                }
            } catch (Exception e) {
                StudentProgress stub = new StudentProgress(studentId, prefs.getString("studentName", "طالب"));
                LetterQuizHistoryLocal.applyPassedFullFromHistoryToProgress(
                        getApplicationContext(), studentId, stub);
                ArrayList<LetterQuizSession> merged =
                        LetterQuizHistoryLocal.mergedWithRemote(getApplicationContext(), studentId, stub);
                quizSessions = merged.size();
                for (String letter : enabledList) {
                    if (Boolean.TRUE.equals(stub.getLettersQuizPassed().get(letter))) {
                        passedLetters++;
                    }
                }
            }
            int remaining = Math.max(0, required - passedLetters);
            final int fQuiz = quizSessions;
            final int fPassed = passedLetters;
            final int fRem = remaining;
            runOnUiThread(() -> {
                if (gen != statsLoadGeneration.get()) {
                    return;
                }
                tvStatQuizSessions.setText(String.valueOf(fQuiz));
                tvStatLettersPassed.setText(String.valueOf(fPassed));
                tvStatLettersRemaining.setText(String.valueOf(fRem));
            });
        }).start();
    }

    private void setupLettersButton() {
        btnLetters.setOnClickListener(v ->
                startActivity(new Intent(EnglishLearningActivity.this, LettersActivity.class)));
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
