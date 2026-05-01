/**
 * الشريط السفلي اللي بربط الرئيسية، الإعدادات، الدخول، واختبار الحروف — ألوان وأيقونات وتوجيه بسيط.
 */
package com.example.RemasProject.Hellper;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.RemasProject.LetterTestActivity;
import com.example.RemasProject.LoginActivity;
import com.example.RemasProject.R;
import com.example.RemasProject.SettingsActivity;

public final class LearningBottomNavHelper {

    private static final int GREEN = 0xFF4CAF50;
    private static final int GRAY = 0xFF757575;
    private static final int RED = 0xFFF44336;

    private LearningBottomNavHelper() {
    }

    public static void highlight(AppCompatActivity activity, int selectedNavId) {
        // الصفحة النشطة بلون أخضر، الباقي رمادي؛ الخروج دائماً أحمر تمييزاً أنه إجراء مختلف
        setNavRow(activity, R.id.btnHome, selectedNavId == R.id.btnHome);
        setNavRow(activity, R.id.btnTests, selectedNavId == R.id.btnTests);
        View settingsRow = activity.findViewById(R.id.btnSettings);
        if (settingsRow != null && settingsRow.getVisibility() == View.VISIBLE) {
            setNavRow(activity, R.id.btnSettings, selectedNavId == R.id.btnSettings);
        }

        LinearLayout logout = activity.findViewById(R.id.btnLogout);
        if (logout != null && logout.getChildCount() >= 2) {
            ((ImageView) logout.getChildAt(0)).setColorFilter(RED, PorterDuff.Mode.SRC_IN);
            ((TextView) logout.getChildAt(1)).setTextColor(RED);
        }
    }

    private static void setNavRow(Activity activity, int rowId, boolean selected) {
        LinearLayout row = activity.findViewById(rowId);
        if (row == null || row.getChildCount() < 2) {
            return;
        }
        int c = selected ? GREEN : GRAY;
        ((ImageView) row.getChildAt(0)).setColorFilter(c, PorterDuff.Mode.SRC_IN);
        ((TextView) row.getChildAt(1)).setTextColor(c);
    }

    /** إظهار أو إخفاء زر الإعدادات حسب صلاحية المعلّم / بريد المشرف. */
    public static void applySettingsNavVisibility(AppCompatActivity activity) {
        SharedPreferences prefs = activity.getSharedPreferences(
                LearningSettingsPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        View settings = activity.findViewById(R.id.btnSettings);
        if (settings == null) {
            return;
        }
        boolean show = LearningSettingsPrefs.canAccessLearningSettings(prefs);
        settings.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    /** من الشاشة الرئيسية — لا تُنهِ النشاط عند فتح الاختبار. */
    public static void bindFromMain(AppCompatActivity activity) {
        applySettingsNavVisibility(activity);
        highlight(activity, R.id.btnHome);

        // الرئيسية هون ما بنفتح Activity ثانية عشان ما نكدّس المكدس — بس تنبيه بسيط
        activity.findViewById(R.id.btnHome).setOnClickListener(v ->
                Toast.makeText(activity, "أنت في الصفحة الرئيسية", Toast.LENGTH_SHORT).show());

        activity.findViewById(R.id.btnTests).setOnClickListener(v ->
                activity.startActivity(new Intent(activity, LetterTestActivity.class)));

        SharedPreferences prefs = activity.getSharedPreferences(
                LearningSettingsPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        View btnSettings = activity.findViewById(R.id.btnSettings);
        if (btnSettings != null && LearningSettingsPrefs.canAccessLearningSettings(prefs)) {
            btnSettings.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, SettingsActivity.class)));
        } else if (btnSettings != null) {
            btnSettings.setOnClickListener(null);
            btnSettings.setClickable(false);
        }

        bindLogout(activity);
    }

    /** من صفحة اختبار الحروف — الرئيسية تعيدك بالـ finish فقط. */
    public static void bindFromLetterTest(AppCompatActivity activity) {
        applySettingsNavVisibility(activity);
        highlight(activity, R.id.btnTests);

        activity.findViewById(R.id.btnHome).setOnClickListener(v -> activity.finish());

        activity.findViewById(R.id.btnTests).setOnClickListener(v -> { /* الصفحة الحالية */ });

        SharedPreferences prefs = activity.getSharedPreferences(
                LearningSettingsPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE);
        View btnSettings = activity.findViewById(R.id.btnSettings);
        if (btnSettings != null && LearningSettingsPrefs.canAccessLearningSettings(prefs)) {
            btnSettings.setOnClickListener(v ->
                    activity.startActivity(new Intent(activity, SettingsActivity.class)));
        } else if (btnSettings != null) {
            btnSettings.setOnClickListener(null);
            btnSettings.setClickable(false);
        }

        bindLogout(activity);
    }

    private static void bindLogout(AppCompatActivity activity) {
        activity.findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // مسح الجلسة المحلية بالكامل ثم صفحة دخول نظيفة بدون رجوع للخلف
            SharedPreferences prefs = activity.getSharedPreferences(LearningSettingsPrefs.PREFS_NAME, android.content.Context.MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(activity, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(intent);
            activity.finish();
        });
    }
}
