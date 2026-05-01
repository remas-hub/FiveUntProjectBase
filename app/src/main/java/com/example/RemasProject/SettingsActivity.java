/**
 * إعدادات التعلّم — مدّات، عدد الاستماعات قبل الاختبار، الحروف المفعّلة، وخصائص الاختبار؛ للمعلّم/المشرف أو اللي عنده صلاحية.
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
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ScrollView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.RemasProject.Hellper.LearningSettingsPrefs;
import com.example.RemasProject.Hellper.UiInsetsHelper;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {

    private static final int LETTER_GRID_COLUMNS = 6;
    /** نفس أخضر الهيدر (#4CAF50) */
    private static final int HEADER_GREEN = 0xFF4CAF50;

    private TextView tvToolbarTitle;
    private ImageButton btnBack;

    private Spinner spinnerSpotlightSeconds;
    private Spinner spinnerSpeakIntervalSeconds;
    private Spinner spinnerListensForQuiz;
    private Spinner spinnerQuizMinLetters;
    private GridLayout gridLettersEnable;
    private Button btnSaveLearningSettings;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        UiInsetsHelper.enableEdgeToEdge(this);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        WindowInsetsControllerCompat c = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        c.setAppearanceLightStatusBars(false);

        View toolbarRoot = findViewById(R.id.toolbar);
        UiInsetsHelper.applyStatusBarPadding(toolbarRoot);
        ScrollView scrollSettings = findViewById(R.id.scrollSettings);
        UiInsetsHelper.applyNavigationBarPadding(scrollSettings);

        prefs = getSharedPreferences(LearningSettingsPrefs.PREFS_NAME, MODE_PRIVATE);
        // حتى لو فتح أحد الرابط يدوياً: نفس شرط الشريط السفلي (معلّم أو بريد مشرف)
        if (!LearningSettingsPrefs.canAccessLearningSettings(prefs)) {
            Toast.makeText(this, "غير مصرّح لك بفتح هذه الصفحة", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnBack = findViewById(R.id.btnBack);
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle);

        spinnerSpotlightSeconds = findViewById(R.id.spinnerSpotlightSeconds);
        spinnerSpeakIntervalSeconds = findViewById(R.id.spinnerSpeakIntervalSeconds);
        spinnerListensForQuiz = findViewById(R.id.spinnerListensForQuiz);
        spinnerQuizMinLetters = findViewById(R.id.spinnerQuizMinLetters);
        gridLettersEnable = findViewById(R.id.gridLettersEnable);
        btnSaveLearningSettings = findViewById(R.id.btnSaveLearningSettings);

        tvToolbarTitle.setText("الإعدادات");
        btnBack.setOnClickListener(v -> onBackPressed());

        setupSpinners();
        populateLetterToggles();
        applySaveButtonGreenCircleStyle();
        btnSaveLearningSettings.setOnClickListener(v -> saveLearningSettings());
    }

    /** زر حفظ دائري أخضر كزر بدء الاختبار والهيدر. */
    private void applySaveButtonGreenCircleStyle() {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(HEADER_GREEN);
        ViewCompat.setBackground(btnSaveLearningSettings, circle);
        btnSaveLearningSettings.setBackgroundTintList(null);
        btnSaveLearningSettings.setTextColor(0xFFFFFFFF);
        btnSaveLearningSettings.setAllCaps(false);
        btnSaveLearningSettings.setStateListAnimator(null);
        btnSaveLearningSettings.setGravity(Gravity.CENTER);
        btnSaveLearningSettings.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10,
                getResources().getDisplayMetrics());
        btnSaveLearningSettings.setPadding(pad, pad, pad, pad);
    }

    private void setupSpinners() {
        ArrayAdapter<String> adSpot = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"10", "15", "20"});
        spinnerSpotlightSeconds.setAdapter(adSpot);

        ArrayAdapter<String> adInterval = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3", "4", "5"});
        spinnerSpeakIntervalSeconds.setAdapter(adInterval);

        ArrayAdapter<String> adListens = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3", "5", "7"});
        spinnerListensForQuiz.setAdapter(adListens);

        ArrayAdapter<String> adQuizMin = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"3", "4", "5"});
        spinnerQuizMinLetters.setAdapter(adQuizMin);

        setSpinnerSelection(spinnerSpotlightSeconds, LearningSettingsPrefs.getSpotlightSeconds(prefs),
                new int[]{10, 15, 20});
        setSpinnerSelection(spinnerSpeakIntervalSeconds,
                LearningSettingsPrefs.getSpeakIntervalSeconds(prefs), new int[]{3, 4, 5});
        setSpinnerSelection(spinnerListensForQuiz,
                LearningSettingsPrefs.getListensForQuizReady(prefs), new int[]{3, 5, 7});
        setSpinnerSelection(spinnerQuizMinLetters,
                LearningSettingsPrefs.getQuizMinLettersToStart(prefs), new int[]{3, 4, 5});
    }

    private static void setSpinnerSelection(Spinner spinner, int value, int[] allowed) {
        for (int i = 0; i < allowed.length; i++) {
            if (allowed[i] == value) {
                spinner.setSelection(i);
                return;
            }
        }
        spinner.setSelection(0);
    }

    private void populateLetterToggles() {
        gridLettersEnable.removeAllViews();
        Set<String> enabled = new HashSet<>(LearningSettingsPrefs.getEnabledLettersOrdered(prefs));
        float density = getResources().getDisplayMetrics().density;
        int cell = Math.round(46 * density);
        int margin = Math.round(5 * density);

        for (int i = 0; i < LearningSettingsPrefs.ALPHABET.length; i++) {
            String letter = LearningSettingsPrefs.ALPHABET[i];
            ToggleButton tb = new ToggleButton(this);
            tb.setTextOff(letter);
            tb.setTextOn(letter);
            tb.setText(letter);
            tb.setChecked(enabled.contains(letter));
            tb.setTextSize(16);
            tb.setMinWidth(cell);
            tb.setMinHeight(cell);
            tb.setPadding(0, 0, 0, 0);
            tb.setGravity(Gravity.CENTER);
            tb.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            applyToggleStyle(tb, tb.isChecked());

            tb.setTag(letter);
            tb.setOnCheckedChangeListener((buttonView, isChecked) ->
                    applyToggleStyle((ToggleButton) buttonView, isChecked));

            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = cell;
            lp.height = cell;
            lp.setMargins(margin, margin, margin, margin);
            lp.columnSpec = GridLayout.spec(i % LETTER_GRID_COLUMNS);
            lp.rowSpec = GridLayout.spec(i / LETTER_GRID_COLUMNS);
            tb.setLayoutParams(lp);
            gridLettersEnable.addView(tb);
        }
    }

    private void applyToggleStyle(ToggleButton tb, boolean on) {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        if (on) {
            bg.setColor(0xFFC8E6C9);
            tb.setTextColor(0xFF1B5E20);
        } else {
            bg.setColor(0xFFE0E0E0);
            tb.setTextColor(0xFF616161);
        }
        tb.setBackground(bg);
    }

    private void saveLearningSettings() {
        Set<String> selected = new HashSet<>();
        for (int i = 0; i < gridLettersEnable.getChildCount(); i++) {
            View ch = gridLettersEnable.getChildAt(i);
            if (ch instanceof ToggleButton && ((ToggleButton) ch).isChecked()) {
                Object tag = ((ToggleButton) ch).getTag();
                if (tag instanceof String) {
                    selected.add(((String) tag).trim().toUpperCase(Locale.US));
                }
            }
        }
        if (selected.size() < LearningSettingsPrefs.MIN_ENABLED_LETTERS) {
            Toast.makeText(this,
                    "اختر على الأقل " + LearningSettingsPrefs.MIN_ENABLED_LETTERS + " حروفاً",
                    Toast.LENGTH_LONG).show();
            return;
        }

        int spotlight = Integer.parseInt((String) spinnerSpotlightSeconds.getSelectedItem());
        int interval = Integer.parseInt((String) spinnerSpeakIntervalSeconds.getSelectedItem());
        int listens = Integer.parseInt((String) spinnerListensForQuiz.getSelectedItem());
        int quizMin = Integer.parseInt((String) spinnerQuizMinLetters.getSelectedItem());

        // كل القيم أعداد صحيحة أو CSV للحروف؛ LettersActivity تسمع التغيير عبر OnSharedPreferenceChangeListener
        SharedPreferences.Editor ed = prefs.edit();
        ed.putInt(LearningSettingsPrefs.KEY_SPOTLIGHT_SECONDS, spotlight);
        ed.putInt(LearningSettingsPrefs.KEY_SPEAK_INTERVAL_SECONDS, interval);
        ed.putInt(LearningSettingsPrefs.KEY_LISTENS_FOR_QUIZ, listens);
        ed.putInt(LearningSettingsPrefs.KEY_QUIZ_MIN_LETTERS, quizMin);
        ed.putString(LearningSettingsPrefs.KEY_ENABLED_LETTERS,
                LearningSettingsPrefs.encodeEnabledLetters(selected));

        if (ed.commit()) {
            Toast.makeText(this, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "تعذّر الحفظ", Toast.LENGTH_SHORT).show();
        }
    }
}
