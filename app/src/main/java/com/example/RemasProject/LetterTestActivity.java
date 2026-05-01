/**
 * اختبار الحروف — تختار مستوى الصعوبة، تختبر الحروف، تشوف السجل؛ كلها مربوطة بالإعدادات وبيانات التقدم من Appwrite.
 */
package com.example.RemasProject;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.RemasProject.Hellper.DALAppWriteConnection;
import com.example.RemasProject.Hellper.LearningBottomNavHelper;
import com.example.RemasProject.Hellper.LearningSettingsPrefs;
import com.example.RemasProject.Hellper.LetterQuizHistoryLocal;
import com.example.RemasProject.Hellper.UiInsetsHelper;
import com.example.RemasProject.model.LetterListenRecord;
import com.example.RemasProject.model.LetterQuizSession;
import com.example.RemasProject.model.StudentProgress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * اختبار الحروف: حروف بـ ٥ استماعات أو أكثر، سؤالان لكل حرف، حفظ النتيجة في التقدم وعداد الاستماع.
 */
public class LetterTestActivity extends AppCompatActivity {

    /** أزرار الاختبار — أزرق مادي */
    private static final int QUIZ_BUTTON_BLUE = 0xFF2196F3;
    /** زر بدء الاختبار — أخضر عند الجاهزية */
    private static final int START_QUIZ_GREEN = 0xFF4CAF50;
    /** زر بدء الاختبار — رمادي عند عدم الجاهزية (تحميل / لا يكفي حروف / غير مسجّل) */
    private static final int START_QUIZ_DISABLED_BG = 0xFFBDBDBD;
    private static final int START_QUIZ_DISABLED_TEXT = 0xFF616161;

    private static final int MIN_PICK = 4;
    private static final int MAX_PICK = 6;

    /** يُقرأ من الإعدادات في كل {@link #loadEligibilityData()} */
    private int listensRequired = 5;
    private int minLettersForTest = 4;
    private List<String> enabledLettersForQuiz = new ArrayList<>();

    private static final String[] LETTERS = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

    private SharedPreferences prefs;
    private DALAppWriteConnection dal;
    private String studentId;

    private LinearLayout panelSetup;
    private LinearLayout panelQuiz;
    private LinearLayout panelDone;
    /** صف اختيار العدد ٤–٦ عند وجود أكثر من ٦ حروف جاهزة */
    private LinearLayout rowPickCount;
    private TextView tvHistoryTitle;
    private TextView tvHistoryEmpty;
    private RecyclerView rvQuizHistory;
    private LetterQuizHistoryAdapter quizHistoryAdapter;
    private TextView tvSetupInfo;
    private Spinner spinnerLetterCount;
    private Button btnStartQuiz;

    private ArabicTextToSpeech quizTts;

    private TextView tvQuizProgress;
    private TextView tvQuizLetter;
    private TextView tvQuizPrompt;
    private Button btnAnswer0;
    private Button btnAnswer1;
    private Button btnAnswer2;
    private Button btnAnswer3;

    private TextView tvDoneSummary;
    private Button btnDoneClose;

    private final List<String> readyLettersAll = new ArrayList<>();
    private final Map<String, Integer> listenCounts = new HashMap<>();
    private StudentProgress cachedProgress;

    private List<String> selectedLetters = new ArrayList<>();
    private List<Question> questions = new ArrayList<>();
    private int questionIndex;
    private final Map<String, Integer> correctPerLetter = new HashMap<>();

    private static final class Question {
        final String letter;

        Question(String letter) {
            this.letter = letter;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_letter_test);

        UiInsetsHelper.enableEdgeToEdge(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);

        View toolbarRoot = findViewById(R.id.toolbar);
        UiInsetsHelper.applyStatusBarPadding(toolbarRoot);
        View bottomBar = findViewById(R.id.bottomBar);
        UiInsetsHelper.applyNavigationBarPadding(bottomBar);

        ImageButton btnBack = findViewById(R.id.btnBack);
        TextView tvToolbarTitle = findViewById(R.id.tvToolbarTitle);
        tvToolbarTitle.setText("اختبار الحروف");
        btnBack.setOnClickListener(v -> onBackPressed());

        prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        studentId = prefs.getString("studentId", null);
        // الشاشة ثلاث لوحات: إعداد/اختبار/ملخص؛ على Resume نحدّث من السيرفر من يستحق الاختبار
        dal = new DALAppWriteConnection(this);

        quizTts = new ArabicTextToSpeech(this, status -> { });

        panelSetup = findViewById(R.id.panelSetup);
        panelQuiz = findViewById(R.id.panelQuiz);
        panelDone = findViewById(R.id.panelDone);
        rowPickCount = findViewById(R.id.rowPickCount);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvHistoryEmpty = findViewById(R.id.tvHistoryEmpty);
        rvQuizHistory = findViewById(R.id.rvQuizHistory);
        rvQuizHistory.setLayoutManager(new LinearLayoutManager(this));
        quizHistoryAdapter = new LetterQuizHistoryAdapter();
        rvQuizHistory.setAdapter(quizHistoryAdapter);
        tvSetupInfo = findViewById(R.id.tvSetupInfo);
        spinnerLetterCount = findViewById(R.id.spinnerLetterCount);
        btnStartQuiz = findViewById(R.id.btnStartQuiz);

        tvQuizProgress = findViewById(R.id.tvQuizProgress);
        tvQuizLetter = findViewById(R.id.tvQuizLetter);
        tvQuizPrompt = findViewById(R.id.tvQuizPrompt);
        btnAnswer0 = findViewById(R.id.btnAnswer0);
        btnAnswer1 = findViewById(R.id.btnAnswer1);
        btnAnswer2 = findViewById(R.id.btnAnswer2);
        btnAnswer3 = findViewById(R.id.btnAnswer3);

        tvDoneSummary = findViewById(R.id.tvDoneSummary);
        btnDoneClose = findViewById(R.id.btnDoneClose);

        ArrayAdapter<String> spinAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"4", "5", "6"});
        spinnerLetterCount.setAdapter(spinAdapter);

        btnStartQuiz.setOnClickListener(v -> startQuizFlow());
        btnDoneClose.setOnClickListener(v -> finish());

        LearningBottomNavHelper.bindFromLetterTest(this);

        tvQuizLetter.setOnClickListener(v -> {
            if (questionIndex >= 0 && questionIndex < questions.size()) {
                playQuestionAudio(questions.get(questionIndex));
            }
        });

        // كل زر إجابة يخزّن فهرسه في Tag لأن النص يتبدل بعد كل سؤال (الحروف عشوائية)
        View.OnClickListener answerListener = v -> {
            int tag = (Integer) v.getTag();
            onAnswerChosen(tag);
        };
        btnAnswer0.setOnClickListener(answerListener);
        btnAnswer1.setOnClickListener(answerListener);
        btnAnswer2.setOnClickListener(answerListener);
        btnAnswer3.setOnClickListener(answerListener);

        applyStartQuizCircleStyle(false);
        applyQuizBlueCircularStyles();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadEligibilityData();
    }

    /** دائرة خضراء عند جاهزية الاختبار، ورمادية عند التحميل أو عدم استيفاء الشروط. */
    private void applyStartQuizCircleStyle(boolean quizReady) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(quizReady ? START_QUIZ_GREEN : START_QUIZ_DISABLED_BG);
        ViewCompat.setBackground(btnStartQuiz, circle);
        btnStartQuiz.setBackgroundTintList(null);
        btnStartQuiz.setTextColor(quizReady ? 0xFFFFFFFF : START_QUIZ_DISABLED_TEXT);
        btnStartQuiz.setAllCaps(false);
        btnStartQuiz.setStateListAnimator(null);
        btnStartQuiz.setGravity(Gravity.CENTER);
        btnStartQuiz.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getResources().getDisplayMetrics());
        btnStartQuiz.setPadding(pad, pad, pad, pad);
    }

    /** أزرار الإجابة دائرية زرقاء؛ محاذاة الحرف في المنتصف (LTR)؛ إلغاء تلوين Material3. */
    private void applyQuizBlueCircularStyles() {
        Button[] answers = {btnAnswer0, btnAnswer1, btnAnswer2, btnAnswer3};
        for (Button btn : answers) {
            GradientDrawable circle = new GradientDrawable();
            circle.setShape(GradientDrawable.OVAL);
            circle.setColor(QUIZ_BUTTON_BLUE);
            ViewCompat.setBackground(btn, circle);
            btn.setBackgroundTintList(null);
            btn.setTextColor(0xFFFFFFFF);
            btn.setAllCaps(false);
            btn.setStateListAnimator(null);
            btn.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            btn.setTextDirection(View.TEXT_DIRECTION_LTR);
            btn.setGravity(Gravity.CENTER);
            btn.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4,
                    getResources().getDisplayMetrics());
            btn.setPadding(pad, pad, pad, pad);
        }
    }

    @Override
    protected void onDestroy() {
        if (quizTts != null) {
            quizTts.shutdown();
            quizTts = null;
        }
        super.onDestroy();
    }

    private void refreshQuizSettingsFromPrefs() {
        listensRequired = LearningSettingsPrefs.getListensForQuizReady(prefs);
        minLettersForTest = LearningSettingsPrefs.getQuizMinLettersToStart(prefs);
        enabledLettersForQuiz = LearningSettingsPrefs.getEnabledLettersOrdered(prefs);
    }

    private void loadEligibilityData() {
        refreshQuizSettingsFromPrefs();

        tvSetupInfo.setText("جاري التحميل…");
        btnStartQuiz.setEnabled(false);
        applyStartQuizCircleStyle(false);

        if (studentId == null) {
            tvSetupInfo.setText("سجّل الدخول أولاً لتتمكن من الاختبار.");
            return;
        }

        final int listensReqSnapshot = listensRequired;
        final List<String> lettersPoolSnapshot = new ArrayList<>(enabledLettersForQuiz);

        new Thread(() -> {
            listenCounts.clear();
            readyLettersAll.clear();
            cachedProgress = null;

            try {
                DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progResult =
                        dal.getStudentProgressForStudent(studentId);
                if (progResult.success && progResult.data != null && !progResult.data.isEmpty()) {
                    ArrayList<StudentProgress> plist = progResult.data;
                    cachedProgress = DALAppWriteConnection.pickNewestStudentProgress(plist);
                    if (cachedProgress != null) {
                        for (StudentProgress p : plist) {
                            if (p != cachedProgress) {
                                cachedProgress.mergeLetterStateFrom(p);
                            }
                        }
                        cachedProgress.normalizeLetterMapKeys();
                    }
                }

                DALAppWriteConnection.OperationResult<ArrayList<LetterListenRecord>> listenResult =
                        dal.getLetterListenRecordsForStudent(studentId);
                if (listenResult.success && listenResult.data != null) {
                    for (LetterListenRecord r : listenResult.data) {
                        if (r.getLetter() != null && !r.getLetter().isEmpty()) {
                            String lk = StudentProgress.normalizeLatinLetterKey(r.getLetter());
                            listenCounts.merge(lk, r.getListenCount(), Math::max);
                        }
                    }
                }

                // حرف «غير جاهز للاختبار» إن سبق نجاحه بالاختبار أو وُسِم متعلّماً — حتى لا يُعاد اختباره من هنا
                Set<String> passed = new HashSet<>();
                if (cachedProgress != null) {
                    Map<String, Boolean> qp = cachedProgress.getLettersQuizPassed();
                    for (Map.Entry<String, Boolean> e : qp.entrySet()) {
                        if (Boolean.TRUE.equals(e.getValue())) {
                            passed.add(StudentProgress.normalizeLatinLetterKey(e.getKey()));
                        }
                    }
                    for (Map.Entry<String, Boolean> e : cachedProgress.getLettersLearned().entrySet()) {
                        if (Boolean.TRUE.equals(e.getValue())) {
                            passed.add(StudentProgress.normalizeLatinLetterKey(e.getKey()));
                        }
                    }
                }

                // جاهز = ضمن الحروف المفعّلة في الإعدادات + عدد استماعات كافٍ + لم يُنجز اختباره بعد
                for (String letter : lettersPoolSnapshot) {
                    int n = listenCounts.getOrDefault(letter, 0);
                    if (n >= listensReqSnapshot && !passed.contains(letter)) {
                        readyLettersAll.add(letter);
                    }
                }
                Collections.sort(readyLettersAll);
            } catch (Exception ignored) {
            }

            runOnUiThread(this::bindSetupPanel);
        }).start();
    }

    private void populateQuizHistoryPanel() {
        if (rvQuizHistory == null || tvHistoryTitle == null) {
            return;
        }
        tvHistoryTitle.setVisibility(View.VISIBLE);

        ArrayList<LetterQuizSession> sessions =
                LetterQuizHistoryLocal.mergedWithRemote(this, studentId, cachedProgress);
        if (sessions.isEmpty()) {
            if (tvHistoryEmpty != null) {
                tvHistoryEmpty.setVisibility(View.VISIBLE);
            }
            rvQuizHistory.setVisibility(View.GONE);
            quizHistoryAdapter.setSessions(null);
            return;
        }
        if (tvHistoryEmpty != null) {
            tvHistoryEmpty.setVisibility(View.GONE);
        }
        rvQuizHistory.setVisibility(View.VISIBLE);
        quizHistoryAdapter.setSessions(sessions);
    }

    private void bindSetupPanel() {
        if (studentId == null) {
            return;
        }

        populateQuizHistoryPanel();

        if (readyLettersAll.size() < minLettersForTest) {
            tvSetupInfo.setText(String.format(Locale.getDefault(),
                    "تحتاج على الأقل %d حروف استمعت لكل منها %d مرات أو أكثر، ولم يُنجَز اختبارها بعد.\n\n"
                            + "حروفك الجاهزة الآن: %d",
                    minLettersForTest, listensRequired, readyLettersAll.size()));
            btnStartQuiz.setEnabled(false);
            applyStartQuizCircleStyle(false);
            rowPickCount.setVisibility(View.GONE);
            return;
        }

        btnStartQuiz.setEnabled(true);
        applyStartQuizCircleStyle(true);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.getDefault(),
                "لديك %d حرفاً جاهزاً للاختبار (استماع ≥ %d، ولم يُجتَز الاختبار بعد).\n",
                readyLettersAll.size(), listensRequired));
        sb.append("اختبار سماعي: اضغط السماعة للاستماع ثم اختر الحرف الإنجليزي — مرتان لكل حرف.\n");

        if (readyLettersAll.size() > MAX_PICK) {
            sb.append(String.format(Locale.getDefault(),
                    "اختر عدد الحروف لهذا الاختبار بين %d و %d (سيتم اختيار الحروف عشوائياً).",
                    MIN_PICK, MAX_PICK));
            tvSetupInfo.setText(sb.toString());
            rowPickCount.setVisibility(View.VISIBLE);
        } else {
            sb.append("سيُستخدم كل الحروف الجاهزة في هذا الاختبار.");
            tvSetupInfo.setText(sb.toString());
            rowPickCount.setVisibility(View.GONE);
        }
    }

    private void startQuizFlow() {
        if (studentId == null || readyLettersAll.size() < minLettersForTest) {
            return;
        }

        selectedLetters = new ArrayList<>();
        if (readyLettersAll.size() <= MAX_PICK) {
            selectedLetters.addAll(readyLettersAll);
        } else {
            // أكثر من 6 جاهزين: المستخدم يختار 4–6 من السبينر ثم عيّنة عشوائية
            int k = MIN_PICK + spinnerLetterCount.getSelectedItemPosition();
            k = Math.max(MIN_PICK, Math.min(MAX_PICK, k));
            List<String> copy = new ArrayList<>(readyLettersAll);
            Collections.shuffle(copy);
            selectedLetters.addAll(copy.subList(0, k));
            Collections.sort(selectedLetters);
        }

        questions.clear();
        // سؤالان لكل حرف مختار؛ ترتيب الأسئلة عشوائي لكن نحسب الصحيح لكل حرف في correctPerLetter
        for (String letter : selectedLetters) {
            questions.add(new Question(letter));
            questions.add(new Question(letter));
        }
        Collections.shuffle(questions);

        correctPerLetter.clear();
        for (String letter : selectedLetters) {
            correctPerLetter.put(letter, 0);
        }

        questionIndex = 0;
        panelSetup.setVisibility(View.GONE);
        panelDone.setVisibility(View.GONE);
        panelQuiz.setVisibility(View.VISIBLE);

        showQuestion(0);
    }

    /** نطق اسم الحرف بالإنجليزية؛ تُعطَّل الإجابات حتى ينتهي النطق. */
    private void playQuestionAudio(Question q) {
        if (quizTts == null || q == null) {
            return;
        }
        quizTts.stop();
        setQuizAnswerButtonsEnabled(false);
        quizTts.speakEnglish(q.letter, () -> setQuizAnswerButtonsEnabled(true));
    }

    private void setQuizAnswerButtonsEnabled(boolean enabled) {
        Button[] btns = {btnAnswer0, btnAnswer1, btnAnswer2, btnAnswer3};
        for (Button b : btns) {
            if (b != null) {
                b.setEnabled(enabled);
                b.setAlpha(enabled ? 1f : 0.35f);
            }
        }
        if (tvQuizPrompt != null) {
            if (enabled) {
                tvQuizPrompt.setText("اختر الحرف الصحيح 👇");
            } else {
                tvQuizPrompt.setText("اضغط السماعة 🔊 أولاً!\nاسمع الحرف ثم اختر الإجابة");
            }
        }
    }

    private void showQuestion(int index) {
        if (index < 0 || index >= questions.size()) {
            return;
        }

        Question q = questions.get(index);
        tvQuizProgress.setText(String.format(Locale.getDefault(),
                "السؤال %d من %d", index + 1, questions.size()));
        tvQuizLetter.setText("🔊");

        List<String> options = buildFourEnglishOptions(q.letter);
        Button[] btns = {btnAnswer0, btnAnswer1, btnAnswer2, btnAnswer3};
        for (int i = 0; i < 4; i++) {
            btns[i].setText(options.get(i));
            btns[i].setTag(i);
        }
        setQuizAnswerButtonsEnabled(false);
    }

    private List<String> buildFourEnglishOptions(String correctLetter) {
        // إجابة صحيحة واحدة + ثلاث مقاطع عشوائية من باقي الأبجد، ثم خلط المراكز الأربعة
        List<String> wrong = new ArrayList<>();
        for (String L : LETTERS) {
            if (!L.equals(correctLetter)) {
                wrong.add(L);
            }
        }
        Collections.shuffle(wrong);

        List<String> opts = new ArrayList<>();
        opts.add(correctLetter);
        for (int i = 0; i < wrong.size() && opts.size() < 4; i++) {
            opts.add(wrong.get(i));
        }
        while (opts.size() < 4) {
            opts.add("?");
        }
        Collections.shuffle(opts);
        return opts;
    }

    private void onAnswerChosen(int optionIndex) {
        if (questionIndex >= questions.size()) {
            return;
        }
        if (btnAnswer0 != null && !btnAnswer0.isEnabled()) {
            return;
        }

        Question q = questions.get(questionIndex);
        Button[] btns = {btnAnswer0, btnAnswer1, btnAnswer2, btnAnswer3};
        String chosen = btns[optionIndex].getText().toString();

        if (Objects.equals(chosen, q.letter)) {
            correctPerLetter.put(q.letter, correctPerLetter.getOrDefault(q.letter, 0) + 1);
        }

        // الانتقال للسؤال التالي؛ عند الانتهاء نجمع النتائج ونرفعها للسحابة ونسجّل محلياً
        if (quizTts != null) {
            quizTts.stop();
        }

        questionIndex++;
        if (questionIndex >= questions.size()) {
            persistResultsThenShowDone();
        } else {
            showQuestion(questionIndex);
        }
    }

    private void persistResultsThenShowDone() {
        panelQuiz.setVisibility(View.GONE);
        tvDoneSummary.setText("جاري حفظ النتائج…");
        panelDone.setVisibility(View.VISIBLE);

        new Thread(() -> {
            StringBuilder summary = new StringBuilder();

            try {
                // تحديث التقدّم + إلحاق جلسة جديدة؛ الفشل صفر استماع للحروف التي لم تحقق صفر إجابات صحيحة في المرتين
                StudentProgress progress = null;
                DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> progResult =
                        dal.getStudentProgressForStudent(studentId);
                if (progResult.success && progResult.data != null && !progResult.data.isEmpty()) {
                    ArrayList<StudentProgress> plist = progResult.data;
                    progress = DALAppWriteConnection.pickNewestStudentProgress(plist);
                    for (StudentProgress p : plist) {
                        if (p != progress) {
                            progress.mergeLetterStateFrom(p);
                        }
                    }
                    progress.normalizeLetterMapKeys();
                    progress.syncLettersLearnedFromQuizPassed();
                }
                if (progress == null) {
                    String name = prefs.getString("studentName", "طالب");
                    progress = new StudentProgress(studentId, name);
                }

                List<String> lettersToResetListen = new ArrayList<>();

                ArrayList<LetterQuizSession.LetterOutcome> sessionOutcomes = new ArrayList<>();

                for (String letter : selectedLetters) {
                    int ok = correctPerLetter.getOrDefault(letter, 0);
                    String ln = StudentProgress.normalizeLatinLetterKey(letter);
                    if (ok >= 2) {
                        progress.getLettersQuizPassed().put(ln, true);
                        progress.markLetterLearned(ln);
                        summary.append(String.format(Locale.getDefault(),
                                "• %s: نجاح كامل — سيظهر بالبرتقالي.\n", letter));
                    } else if (ok == 1) {
                        summary.append(String.format(Locale.getDefault(),
                                "• %s: إجابة صحيحة واحدة — يبقى للاختبارات القادمة.\n", letter));
                    } else {
                        lettersToResetListen.add(ln);
                        summary.append(String.format(Locale.getDefault(),
                                "• %s: لم يُجب صحيحاً في المرتين — يُصفَّر عداد الاستماع.\n", letter));
                    }
                    LetterQuizSession.LetterOutcome lo = new LetterQuizSession.LetterOutcome();
                    lo.setLetter(ln);
                    lo.setCorrectCount(ok);
                    lo.setPassedFull(ok >= 2);
                    sessionOutcomes.add(lo);
                }

                LetterQuizSession quizSession = new LetterQuizSession();
                quizSession.setCompletedAtEpochMs(System.currentTimeMillis());
                quizSession.setOutcomes(sessionOutcomes);
                progress.addLetterQuizSession(quizSession);
                LetterQuizHistoryLocal.append(getApplicationContext(), studentId, quizSession);

                progress.setUpdatedAt(new java.util.Date());
                boolean progOk;
                if (progress.getId() != null && !progress.getId().isEmpty()) {
                    DALAppWriteConnection.OperationResult<StudentProgress> ur =
                            dal.updateData(progress, "student_progress", progress.getId(), null);
                    progOk = ur.success;
                } else {
                    DALAppWriteConnection.OperationResult<ArrayList<StudentProgress>> sr =
                            dal.saveData(progress, "student_progress", null);
                    progOk = sr.success;
                }

                if (!progOk) {
                    summary.append("\nتنبيه: تعذّر حفظ حالة الاختبار في التقدم.\n");
                }

                DALAppWriteConnection.OperationResult<ArrayList<LetterListenRecord>> listenResult =
                        dal.getLetterListenRecordsForStudent(studentId);

                if (listenResult.success && listenResult.data != null) {
                    for (String letter : lettersToResetListen) {
                        String ln = StudentProgress.normalizeLatinLetterKey(letter);
                        for (LetterListenRecord r : listenResult.data) {
                            if (ln.equals(StudentProgress.normalizeLatinLetterKey(r.getLetter()))
                                    && r.getId() != null && !r.getId().isEmpty()) {
                                r.setListenCount(0);
                                dal.updateData(r, LetterListenRecord.COLLECTION_NAME, r.getId(), null);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                summary.append("\nخطأ أثناء الحفظ: ").append(e.getMessage());
            }

            String finalText = summary.length() > 0 ? summary.toString() : "تم.";
            runOnUiThread(() -> tvDoneSummary.setText(finalText));
        }).start();
    }
}
