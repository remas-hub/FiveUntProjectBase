/**
 * كل الإعدادات اللي بتخص الحروف والاختبار — من الشاشطون للاستماع لحد الحروف المفعّلة؛ قراءة وكتابة على SharedPreferences بأسماء ثابتة.
 */
package com.example.RemasProject.Hellper;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class LearningSettingsPrefs {

    public static final String PREFS_NAME = "EnglishLearning";

    public static final String KEY_SPOTLIGHT_SECONDS = "setting_spotlight_seconds";
    public static final String KEY_SPEAK_INTERVAL_SECONDS = "setting_speak_interval_seconds";
    public static final String KEY_LISTENS_FOR_QUIZ = "setting_listens_for_quiz";
    public static final String KEY_QUIZ_MIN_LETTERS = "setting_quiz_min_letters";
    /** حروف مفعّلة للوحة التعلّم والاختبار، مفصولة بفواصل، مثل {@code A,B,C,D} */
    public static final String KEY_ENABLED_LETTERS = "setting_enabled_letters_csv";

    /**
     * يُحدَّد عند تسجيل الدخول: معلّم في مستند الطالب أو بريد المشرف {@link #ADMIN_TEACHER_EMAIL}.
     */
    public static final String KEY_IS_TEACHER = "isTeacher";

    /** بريد يُعامل كمعلّم دائماً (إظهار الإعدادات). */
    public static final String ADMIN_TEACHER_EMAIL = "admin@admin.com";

    public static final int MIN_ENABLED_LETTERS = 4;

    public static final String[] ALPHABET = {
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
            "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
            "U", "V", "W", "X", "Y", "Z"
    };

    private LearningSettingsPrefs() {}

    public static boolean isAdminTeacherEmail(String email) {
        if (email == null) {
            return false;
        }
        return ADMIN_TEACHER_EMAIL.equalsIgnoreCase(email.trim());
    }

    /**
     * صلاحية فتح إعدادات التعلّم من الشريط السفلي أو نشاط الإعدادات.
     */
    public static boolean canAccessLearningSettings(SharedPreferences p) {
        if (p == null) {
            return false;
        }
        if (p.getBoolean(KEY_IS_TEACHER, false)) {
            return true;
        }
        return isAdminTeacherEmail(p.getString("studentEmail", ""));
    }

    /** ما يُخزَّن في التفضيلات بعد تسجيل الدخول من مستند الطالب + البريد. */
    public static boolean effectiveIsTeacher(boolean isTeacherFromDocument, String studentEmail) {
        return isTeacherFromDocument || isAdminTeacherEmail(studentEmail);
    }

    /** مفاتيح تؤثر على واجهة لوحة الحروف أو الاختبار — لإعادة الرسم عند التغيير. */
    public static boolean affectsLettersPanelOrQuiz(String key) {
        if (key == null) {
            return false;
        }
        return KEY_SPOTLIGHT_SECONDS.equals(key)
                || KEY_SPEAK_INTERVAL_SECONDS.equals(key)
                || KEY_LISTENS_FOR_QUIZ.equals(key)
                || KEY_QUIZ_MIN_LETTERS.equals(key)
                || KEY_ENABLED_LETTERS.equals(key);
    }

    public static int getSpotlightSeconds(SharedPreferences p) {
        int v = p.getInt(KEY_SPOTLIGHT_SECONDS, 10);
        if (v == 10 || v == 15 || v == 20) {
            return v;
        }
        return 10;
    }

    /** الفاصل بالثواني بين كل نطق للحرف أثناء ظهور النافذة. */
    public static int getSpeakIntervalSeconds(SharedPreferences p) {
        int v = p.getInt(KEY_SPEAK_INTERVAL_SECONDS, 5);
        if (v == 3 || v == 4 || v == 5) {
            return v;
        }
        return 5;
    }

    /** عدد الاستماعات ليُعتبر الحرف جاهزاً للاختبار (ولون «جاهز» في لوحة التعلّم). */
    public static int getListensForQuizReady(SharedPreferences p) {
        int v = p.getInt(KEY_LISTENS_FOR_QUIZ, 5);
        if (v == 3 || v == 5 || v == 7) {
            return v;
        }
        return 5;
    }

    /** أقل عدد حروف جاهزة لتفعيل زر بدء الاختبار. */
    public static int getQuizMinLettersToStart(SharedPreferences p) {
        int v = p.getInt(KEY_QUIZ_MIN_LETTERS, 4);
        if (v == 3 || v == 4 || v == 5) {
            return v;
        }
        return 4;
    }

    /**
     * الحروف المعروضة في لوحة التعلّم والمشاركة في منطق «جاهز للاختبار».
     * إذا لم يُحفظ شيء أو كانت القيمة غير صالحة (&lt; ٤ حروف)، يُعاد الأبجد كاملاً.
     */
    public static List<String> getEnabledLettersOrdered(SharedPreferences p) {
        Set<String> parsed = parseEnabledLettersCsv(p.getString(KEY_ENABLED_LETTERS, null));
        if (parsed.size() < MIN_ENABLED_LETTERS) {
            // إعداد غير مكتمل أو أقل من الحد الأدنى — نرجع الأبجد كاملاً حتى ما نكسر الشاشة
            return new ArrayList<>(Arrays.asList(ALPHABET));
        }
        List<String> out = new ArrayList<>();
        // الترتيب ثابت حسب A..Z وليس حسب ترتيب الكتابة في الـ CSV
        for (String L : ALPHABET) {
            if (parsed.contains(L)) {
                out.add(L);
            }
        }
        return out;
    }

    public static Set<String> getEnabledLettersSet(SharedPreferences p) {
        return new HashSet<>(getEnabledLettersOrdered(p));
    }

    static Set<String> parseEnabledLettersCsv(String csv) {
        Set<String> set = new HashSet<>();
        if (csv == null || csv.trim().isEmpty()) {
            return set;
        }
        for (String part : csv.split(",")) {
            String t = part.trim().toUpperCase(Locale.US);
            if (t.length() == 1 && t.charAt(0) >= 'A' && t.charAt(0) <= 'Z') {
                set.add(t);
            }
        }
        return set;
    }

    public static String encodeEnabledLetters(Set<String> letters) {
        List<String> sorted = new ArrayList<>(letters);
        Collections.sort(sorted);
        return TextUtils.join(",", sorted);
    }
}
