/**
 * لوحة الحروف — تسمع الحرف، السبوتلايت، الألوان حسب الإعدادات والتقدم؛ تحفظ الاستماع والتقدم على السحابة مع fallback محلي إذا لزم.
 */
package com.example.RemasProject;

import static android.view.View.TEXT_ALIGNMENT_CENTER;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.Hellper.LearningSettingsPrefs;
import com.example.RemasProject.Hellper.LetterQuizHistoryLocal;
import com.example.RemasProject.Hellper.UiInsetsHelper;
import com.example.RemasProject.model.LetterListenRecord;
import com.example.RemasProject.model.StudentProgress;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class LettersActivity extends AppCompatActivity {

    private static final String LOG_TAG = "LettersActivity";
    /** خلفية افتراضية لكل الحروف (رمادي فاتح) */
    private static final int LETTER_BG_DEFAULT = 0xFFE0E0E0;
    /** بعد 5 استماعات أو أكثر (وجاهز للاختبار): خلفية خضراء */
    private static final int LETTER_BG_AFTER_FIVE = 0xFF81C784;
    /** نجاح الاختبار مرتين لنفس الحرف */
    private static final int LETTER_BG_QUIZ_PASSED = 0xFFFF9800;

    private ArabicTextToSpeech arabicTTS;
    private GridLayout gridLayout;
    private TextView tvToolbarTitle;
    private ImageButton btnBack;
    private FrameLayout letterSpotlightOverlay;
    private TextView tvLetterSpotlight;
    private TextView tvSpotlightCountdown;
    private final Handler spotlightHandler = new Handler(Looper.getMainLooper());
    private boolean letterSpotlightShowing;
    private String currentSpotlightLetter = "";
    private int spotlightSecondsLeft = 10;
    /** يُحدَّث من الإعدادات عند بناء الشبكة */
    private String[] letters = LearningSettingsPrefs.ALPHABET.clone();
    private final List<Runnable> spotlightSpeakRunnables = new ArrayList<>();
    
    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;
    private StudentProgress currentProgress;
    /** عدد مرات الاستماع لكل حرف من مجموعة letter_listens */
    private final Map<String, Integer> listenCountsByLetter = new HashMap<>();

    /** يتجاهل نتائج تحميل قديمة إن استُؤنف النشاط مرات متتابعة. */
    private final AtomicInteger progressLoadGeneration = new AtomicInteger(0);

    private final SharedPreferences.OnSharedPreferenceChangeListener learningSettingsListener =
            (sharedPreferences, key) -> {
                // تغيير إعداد مثل مدة السبوتلايت أو الحروف المفعّلة يستدعي إعادة رسم الشبكة فوراً
                if (!LearningSettingsPrefs.affectsLettersPanelOrQuiz(key)) {
                    return;
                }
                runOnUiThread(() -> rebuildLetterGridKeepingListenState());
            };

    private final Runnable spotlightDismissRunnable = this::dismissLetterSpotlight;

    private final Runnable spotlightCountdownTickRunnable = new Runnable() {
        @Override
        public void run() {
            // عدّاد تنازلي للعرض فقط؛ ما يوقف السبوتلايت — الإغلاق بوقت ثابت من postDelayed
            if (!letterSpotlightShowing) {
                return;
            }
            spotlightSecondsLeft--;
            if (tvSpotlightCountdown != null) {
                tvSpotlightCountdown.setText(String.valueOf(spotlightSecondsLeft));
            }
            if (spotlightSecondsLeft > 1) {
                spotlightHandler.postDelayed(this, 1000L);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letters);

        UiInsetsHelper.enableEdgeToEdge(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat wic = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        wic.setAppearanceLightStatusBars(false);

        View toolbarRoot = findViewById(R.id.toolbar);
        UiInsetsHelper.applyStatusBarPadding(toolbarRoot);
        ScrollView scrollLetters = findViewById(R.id.scrollLetters);
        UiInsetsHelper.applyNavigationBarPadding(scrollLetters);

        initializeToolbar();
        
        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        dal = new DALAppWriteConnection(this);
        
        gridLayout = findViewById(R.id.gridLetters);
        letterSpotlightOverlay = findViewById(R.id.letterSpotlightOverlay);
        tvLetterSpotlight = findViewById(R.id.tvLetterSpotlight);
        tvSpotlightCountdown = findViewById(R.id.tvSpotlightCountdown);

        // تهيئة TextToSpeech
        arabicTTS = new ArabicTextToSpeech(this, status -> {
            // جاهز للاستخدام
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        prefs.registerOnSharedPreferenceChangeListener(learningSettingsListener);
    }

    @Override
    protected void onStop() {
        prefs.unregisterOnSharedPreferenceChangeListener(learningSettingsListener);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuildLetterGridKeepingListenState();
        loadProgressAndListenCountsThenBuildGrid();
    }

    /**
     * إعادة بناء الشبكة من الإعدادات الحالية مع الإبقاء على أعداد الاستماع المحمّلة في الذاكرة.
     * يُستدعى عند العودة من الإعدادات أو عند تغيّر التفضيلات حتى لا تنتظر جلب الشبكة من السحابة.
     */
    private void rebuildLetterGridKeepingListenState() {
        if (letterSpotlightShowing || gridLayout == null) {
            return;
        }
        gridLayout.removeAllViews();
        createLetterButtons();
    }

    /**
     * يحمّل تقدم الطالب وأعداد الاستماع من Appwrite ثم يبني الشبكة على الخيط الرئيسي.
     */
    private void loadProgressAndListenCountsThenBuildGrid() {
        final int generation = progressLoadGeneration.incrementAndGet();
        new Thread(() -> {
            Map<String, Integer> fetchedListens = new HashMap<>();
            try {
                if (studentId != null) {
                    // 1) تقدّم الطالب من student_progress  2) دمج نسخ متعددة إن وُجدت  3) إثراء من سجل الاختبارات المحلي
                    // 4) أعداد الاستماع من letter_listens  5) إن تغيّر شيء مهم نعمل PATCH للسحابة
                    DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progResult =
                            dal.getStudentProgressForStudent(studentId);
                    boolean mergedProgress = false;
                    boolean normalizedMaps = false;
                    if (progResult.success && progResult.data != null && !progResult.data.isEmpty()) {
                        ArrayList<StudentProgress> plist = progResult.data;
                        currentProgress = DALAppWriteConnection.pickNewestStudentProgress(plist);
                        if (currentProgress != null) {
                            for (StudentProgress p : plist) {
                                if (p != currentProgress) {
                                    mergedProgress |= currentProgress.mergeLetterStateFrom(p);
                                }
                            }
                            normalizedMaps = currentProgress.normalizeLetterMapKeys();
                        }
                    } else if (progResult.success) {
                        currentProgress = null;
                    }

                    if (currentProgress == null && studentId != null) {
                        String studentName = prefs.getString("studentName", "طالب");
                        currentProgress = new StudentProgress(studentId, studentName);
                    }

                    boolean fromQuizHistory = false;
                    if (currentProgress != null && studentId != null) {
                        fromQuizHistory = LetterQuizHistoryLocal.applyPassedFullFromHistoryToProgress(
                                getApplicationContext(), studentId, currentProgress);
                    }

                    DALAppWriteConnection.OperationResult<ArrayList<LetterListenRecord>> listenResult =
                            dal.getLetterListenRecordsForStudent(studentId);
                    if (listenResult.success && listenResult.data != null) {
                        for (LetterListenRecord r : listenResult.data) {
                            if (r.getLetter() != null && !r.getLetter().isEmpty()) {
                                String lk = StudentProgress.normalizeLatinLetterKey(r.getLetter());
                                fetchedListens.merge(lk, r.getListenCount(), Math::max);
                            }
                        }
                    }

                    boolean syncedQuizToLearned = currentProgress != null
                            && currentProgress.syncLettersLearnedFromQuizPassed();
                    if (currentProgress != null
                            && (mergedProgress || normalizedMaps || syncedQuizToLearned || fromQuizHistory)) {
                        String pid = currentProgress.getId();
                        if (pid != null && !pid.isEmpty()) {
                            dal.updateData(currentProgress, "student_progress", pid, null);
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "loadProgressAndListenCounts", e);
            }
            final Map<String, Integer> applyListens = fetchedListens;
            runOnUiThread(() -> {
                if (generation != progressLoadGeneration.get()) {
                    return;
                }
                listenCountsByLetter.clear();
                listenCountsByLetter.putAll(applyListens);
                rebuildLetterGridKeepingListenState();
            });
        }).start();
    }
    
    private void initializeToolbar() {
        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        
        tvToolbarTitle.setText("تعلم الحروف");
        
        btnBack.setOnClickListener(v -> onBackPressed());
    }
    
    private CharSequence formatLetterButtonLabel(String letter) {
        int n = listenCountsByLetter.getOrDefault(letter, 0);
        String line1 = letter;
        String full = line1 + "\n(" + n + ")";
        SpannableString ss = new SpannableString(full);
        int startCount = line1.length() + 1;
        if (startCount < full.length()) {
            ss.setSpan(new RelativeSizeSpan(0.52f), startCount, full.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ss;
    }

    /**
     * برتقالي فقط إذا تحقّق «مكتمل» مع عدد الاستماعات الحالي من الإعدادات؛ وإلا أخضر عند بلوغ الحد دون ذلك؛ وإلا رمادي.
     * بهذا إن رُفع حد الاستماع لا يبقى البرتقالي من اختبار قديم حتى يكتمل العدد الجديد، وإن خُفّ الحد يظهر البرتقالي فور استيفاء الحد الجديد.
     */
    private void applyLetterButtonAppearance(Button btn, String letter) {
        btn.setText(formatLetterButtonLabel(letter));
        int n = listenCountsByLetter.getOrDefault(letter, 0);
        int listensNeed = LearningSettingsPrefs.getListensForQuizReady(prefs);
        boolean quizOrLearned = currentProgress != null
                && (Boolean.TRUE.equals(currentProgress.getLettersQuizPassed().get(letter))
                || Boolean.TRUE.equals(currentProgress.getLettersLearned().get(letter)));
        boolean letterDoneOrange = quizOrLearned && n >= listensNeed;
        int fill = LETTER_BG_DEFAULT;
        if (letterDoneOrange) {
            fill = LETTER_BG_QUIZ_PASSED;
        } else if (n >= listensNeed) {
            fill = LETTER_BG_AFTER_FIVE;
        }
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(fill);
        btn.setBackground(circle);
        btn.setTextColor(0xFF000000);
    }

    @Override
    public void onBackPressed() {
        if (letterSpotlightShowing) {
            return;
        }
        super.onBackPressed();
    }

    /** نافذة معتمة مع عداد تنازلي؛ النطق كل X ثوانٍ حتى انتهاء مدة الظهور؛ لا تفاعل مع بقية الشاشة. */
    private void showLetterSpotlight(String letter, Button btn) {
        if (letterSpotlightShowing || letterSpotlightOverlay == null || tvLetterSpotlight == null) {
            return;
        }
        letterSpotlightShowing = true;
        currentSpotlightLetter = letter;
        clearSpotlightSpeakRunnables();
        spotlightHandler.removeCallbacks(spotlightDismissRunnable);
        spotlightHandler.removeCallbacks(spotlightCountdownTickRunnable);

        int durSec = LearningSettingsPrefs.getSpotlightSeconds(prefs);
        int intervalSec = LearningSettingsPrefs.getSpeakIntervalSeconds(prefs);

        tvLetterSpotlight.setText(letter);
        spotlightSecondsLeft = durSec;
        if (tvSpotlightCountdown != null) {
            tvSpotlightCountdown.setText(String.valueOf(spotlightSecondsLeft));
        }
        letterSpotlightOverlay.setVisibility(View.VISIBLE);
        spotlightHandler.postDelayed(spotlightCountdownTickRunnable, 1000L);

        // نُسجّل الاستماع مع أول ظهور للسبوتلايت (لا ننتظر انتهاء العداد أو إغلاق النافذة)
        persistLetterListenAndProgress(letter, btn);

        long totalMs = durSec * 1000L;
        spotlightHandler.postDelayed(spotlightDismissRunnable, totalMs);

        if (arabicTTS != null) {
            arabicTTS.stop();
        }
        for (int sec = 0; sec < durSec; sec += intervalSec) {
            final int delayMs = sec * 1000;
            Runnable r = () -> {
                if (!letterSpotlightShowing || arabicTTS == null) {
                    return;
                }
                arabicTTS.speakEnglish(currentSpotlightLetter);
            };
            spotlightSpeakRunnables.add(r);
            spotlightHandler.postDelayed(r, delayMs);
        }
    }

    private void clearSpotlightSpeakRunnables() {
        for (Runnable r : spotlightSpeakRunnables) {
            spotlightHandler.removeCallbacks(r);
        }
        spotlightSpeakRunnables.clear();
    }

    private void dismissLetterSpotlight() {
        letterSpotlightShowing = false;
        clearSpotlightSpeakRunnables();
        spotlightHandler.removeCallbacks(spotlightDismissRunnable);
        spotlightHandler.removeCallbacks(spotlightCountdownTickRunnable);
        if (letterSpotlightOverlay != null) {
            letterSpotlightOverlay.setVisibility(View.GONE);
        }
        if (arabicTTS != null) {
            arabicTTS.stop();
        }
    }
    
    private void createLetterButtons() {
        gridLayout.post(this::createLetterButtonsAfterMeasure);
    }

    /**
     * يحسب الأعمدة وحجم الدائرة من عرض وارتفاع المنطقة المعروضة (مع تمرير عمودي عند الحاجة).
     */
    private void createLetterButtonsAfterMeasure() {
        gridLayout.removeAllViews();

        List<String> enabled = LearningSettingsPrefs.getEnabledLettersOrdered(prefs);
        letters = enabled.toArray(new String[0]);

        final int letterCount = letters.length;
        if (letterCount == 0) {
            return;
        }
        final float density = getResources().getDisplayMetrics().density;
        final int gap = Math.round(8 * density);
        final int minCell = Math.round(36 * density);

        int availW = gridLayout.getWidth();
        ScrollView scrollView = null;
        if (gridLayout.getParent() instanceof ScrollView) {
            scrollView = (ScrollView) gridLayout.getParent();
        }

        int availH = 0;
        if (scrollView != null) {
            availH = scrollView.getHeight() - scrollView.getPaddingTop() - scrollView.getPaddingBottom();
        }

        if (availW <= 0) {
            int hPad = scrollView != null
                    ? scrollView.getPaddingLeft() + scrollView.getPaddingRight()
                    : Math.round(32 * density);
            availW = Math.max(minCell, getResources().getDisplayMetrics().widthPixels - hPad);
        }

        if (availH <= minCell) {
            android.graphics.Rect rect = new android.graphics.Rect();
            getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
            int roughToolbar = Math.round(56 * density);
            availH = Math.max(minCell * 4, rect.height() - roughToolbar - Math.round(32 * density));
        }

        int bestCols = 4;
        int bestCell = 0;
        // نجرّب عدد أعمدة من 3 لـ 8 ونختار اللي يعطي أكبر «خلية» مربعة قدر الإمكان ضمن المساحة المتاحة
        for (int cols = 3; cols <= 8; cols++) {
            int rows = (int) Math.ceil(letterCount / (double) cols);
            int cw = (availW - cols * gap) / cols;
            int ch = (availH - rows * gap) / rows;
            int cell = Math.min(cw, ch);
            if (cell > bestCell) {
                bestCell = cell;
                bestCols = cols;
            }
        }

        int cols = bestCols;
        int rows = (int) Math.ceil(letterCount / (double) cols);
        int cellW = (availW - cols * gap) / cols;
        int cellH = (availH - rows * gap) / rows;
        int buttonSize = Math.max(1, Math.min(cellW, cellH));

        gridLayout.setColumnCount(cols);
        gridLayout.setRowCount(rows);

        float textPx = Math.max(12f * density, Math.min(26f * density, buttonSize * 0.26f));

        for (int i = 0; i < letterCount; i++) {
            String letter = letters[i];
            Button btn = new Button(this);
            btn.setTextSize(TypedValue.COMPLEX_UNIT_PX, textPx);
            btn.setAllCaps(false);
            btn.setTextAlignment(TEXT_ALIGNMENT_CENTER);
            btn.setTypeface(null, android.graphics.Typeface.BOLD);
            int inset = Math.max(2, Math.round(buttonSize * 0.08f));
            btn.setPadding(inset, inset, inset, inset);

            int row = i / cols;
            int col = i % cols;
            GridLayout.LayoutParams params = new GridLayout.LayoutParams(
                    GridLayout.spec(row),
                    GridLayout.spec(col));
            params.width = buttonSize;
            params.height = buttonSize;
            params.setMargins(gap / 2, gap / 2, gap / 2, gap / 2);
            btn.setLayoutParams(params);

            applyLetterButtonAppearance(btn, letter);

            final String letterToSpeak = letter;
            btn.setOnClickListener(v -> {
                if (letterSpotlightShowing) {
                    return;
                }
                animateButtonClick(btn, () -> showLetterSpotlight(letterToSpeak, btn));
            });

            gridLayout.addView(btn);
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
    
    /**
     * حفظ «متعلّم» في {@code student_progress}، وعدّ مرات الاستماع في مجموعة Appwrite {@link LetterListenRecord#COLLECTION_NAME}.
     */
    private void persistLetterListenAndProgress(String letter, Button btn) {
        if (studentId == null) {
            return;
        }

        new Thread(() -> {
            try {
                Log.d(LOG_TAG, "persistLetter: letter=" + letter + " studentId=" + studentId);

                // أول ضغطة بعد فتح الشاشة قد لا يكون currentProgress محمّل؛ نعيد جلب خفيف قبل الحفظ
                if (currentProgress == null) {
                    DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progResult =
                            dal.getStudentProgressForStudent(studentId);
                    Log.d(LOG_TAG, "get student_progress success=" + progResult.success
                            + " msg=" + progResult.message
                            + " count=" + (progResult.data != null ? progResult.data.size() : -1));

                    if (progResult.success && progResult.data != null && !progResult.data.isEmpty()) {
                        currentProgress = DALAppWriteConnection.pickNewestStudentProgress(progResult.data);
                    }

                    if (currentProgress == null) {
                        String studentName = prefs.getString("studentName", "طالب");
                        currentProgress = new StudentProgress(studentId, studentName);
                    }
                }

                // نحدّث خريطة «تعلّم الحرف» في نفس المستند؛ saveData قد ينشئ أو يحدّث حسب وجود المستند
                currentProgress.markLetterLearned(letter);
                DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progSave =
                        dal.saveData(currentProgress, "student_progress", null);
                Log.d(LOG_TAG, "save student_progress success=" + progSave.success + " msg=" + progSave.message);

                LetterListenRecord existing = null;
                DALAppWriteConnection.OperationResult<ArrayList<LetterListenRecord>> listenResult =
                        dal.getLetterListenRecordsForStudent(studentId);
                Log.d(LOG_TAG, "get " + LetterListenRecord.COLLECTION_NAME
                        + " success=" + listenResult.success
                        + " msg=" + listenResult.message
                        + " count=" + (listenResult.data != null ? listenResult.data.size() : -1));

                if (listenResult.success && listenResult.data != null) {
                    for (LetterListenRecord r : listenResult.data) {
                        if (letter.equals(r.getLetter())) {
                            existing = r;
                            break;
                        }
                    }
                }

                int listenTotal = 1;
                boolean listenSavedOk;

                // مستند استماع واحد لكل (طالب + حرف): إن وُجد نرفع العداد بـ PATCH، وإلا POST جديد
                if (existing != null && existing.getId() != null && !existing.getId().isEmpty()) {
                    existing.incrementListen();
                    listenTotal = existing.getListenCount();
                    DALAppWriteConnection.OperationResult<LetterListenRecord> patchResult =
                            dal.updateData(existing, LetterListenRecord.COLLECTION_NAME, existing.getId(), null);
                    listenSavedOk = patchResult.success;
                    Log.d(LOG_TAG, "PATCH letter_listen success=" + patchResult.success
                            + " msg=" + patchResult.message);
                } else {
                    LetterListenRecord created = new LetterListenRecord(studentId, letter, 1);
                    DALAppWriteConnection.OperationResult<ArrayList<LetterListenRecord>> saveListen =
                            dal.saveData(created, LetterListenRecord.COLLECTION_NAME, null);
                    listenSavedOk = saveListen.success;
                    Log.d(LOG_TAG, "POST letter_listen success=" + saveListen.success
                            + " msg=" + saveListen.message);
                    if (saveListen.success && saveListen.data != null && !saveListen.data.isEmpty()) {
                        listenTotal = saveListen.data.get(0).getListenCount();
                    }
                }

                final int finalListenCount = listenTotal;
                runOnUiThread(() -> {
                    if (listenSavedOk) {
                        listenCountsByLetter.put(letter, finalListenCount);
                    }
                    if (btn != null && (progSave.success || listenSavedOk)) {
                        applyLetterButtonAppearance(btn, letter);
                    }
                });
            } catch (Exception e) {
                Log.e(LOG_TAG, "persistLetter خطأ", e);
            }
        }).start();
    }
    
    @Override
    protected void onDestroy() {
        clearSpotlightSpeakRunnables();
        spotlightHandler.removeCallbacks(spotlightDismissRunnable);
        spotlightHandler.removeCallbacks(spotlightCountdownTickRunnable);
        letterSpotlightShowing = false;
        if (arabicTTS != null) {
            arabicTTS.shutdown();
        }
        super.onDestroy();
    }
}
