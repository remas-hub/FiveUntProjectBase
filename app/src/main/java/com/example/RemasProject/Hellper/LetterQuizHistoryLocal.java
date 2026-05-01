/**
 * كاش محلي لتاريخ اختبارات الحروف — إذا السحابة تعطل أو الحقل ما انحفظ، منضل نشوف الجلسات القديمة من الجهاز.
 */
package com.example.RemasProject.Hellper;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.RemasProject.model.LetterQuizSession;
import com.example.RemasProject.model.StudentProgress;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public final class LetterQuizHistoryLocal {

    private static final String PREFS = "EnglishLearning";
    private static final String KEY_PREFIX = "letterQuizHistory_v1_";
    private static final int MAX_SESSIONS = 40;
    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<ArrayList<LetterQuizSession>>(){}.getType();

    private LetterQuizHistoryLocal() {
    }

    private static String key(String studentId) {
        return KEY_PREFIX + studentId;
    }

    public static ArrayList<LetterQuizSession> readAll(Context context, String studentId) {
        if (studentId == null || studentId.isEmpty()) {
            return new ArrayList<>();
        }
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = p.getString(key(studentId), null);
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<LetterQuizSession> list = GSON.fromJson(json, LIST_TYPE);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private static void writeAll(Context context, String studentId, ArrayList<LetterQuizSession> list) {
        if (studentId == null || studentId.isEmpty()) {
            return;
        }
        while (list.size() > MAX_SESSIONS) {
            list.remove(list.size() - 1);
        }
        SharedPreferences p = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putString(key(studentId), GSON.toJson(list)).apply();
    }

    /** يُستدعى بعد كل اختبار حتى يبقى السجل ظاهراً حين يفشل رفع Appwrite. */
    public static void append(Context context, String studentId, LetterQuizSession session) {
        if (session == null) {
            return;
        }
        ArrayList<LetterQuizSession> list = readAll(context, studentId);
        list.add(0, new LetterQuizSession(session));
        writeAll(context, studentId, list);
    }

    /**
     * دمج السحابة (إن وُجدت) مع المحلي بدون تكرار ({@code completedAtEpochMs}).
     */
    public static ArrayList<LetterQuizSession> mergedWithRemote(
            Context context,
            String studentId,
            StudentProgress remote) {
        // المفتاح الزمني completedAtEpochMs يمنع تكرار نفس الجلسة إذا كانت محفوظة محلياً وعلى السيرفر
        Set<Long> seen = new HashSet<>();
        ArrayList<LetterQuizSession> merged = new ArrayList<>();
        if (remote != null) {
            for (LetterQuizSession s : remote.getLetterQuizHistory()) {
                if (s != null && seen.add(s.getCompletedAtEpochMs())) {
                    merged.add(new LetterQuizSession(s));
                }
            }
        }
        for (LetterQuizSession s : readAll(context, studentId)) {
            if (s != null && seen.add(s.getCompletedAtEpochMs())) {
                merged.add(new LetterQuizSession(s));
            }
        }
        merged.sort((a, b) -> Long.compare(b.getCompletedAtEpochMs(), a.getCompletedAtEpochMs()));
        // حد أقصى حتى ما يتضخم الـ prefs والـ JSON على السيرفر
        while (merged.size() > MAX_SESSIONS) {
            merged.remove(merged.size() - 1);
        }
        return merged;
    }

    /**
     * يطبّق على التقدّم أي حرف كان «نجاح كامل» في أي جلسة محفوظة (محلياً أو ضمن {@code letterQuizHistory}
     * في المستند) حتى تظهر الشبكة بالبرتقالي حتى لو فشل حفظ الخرائط على السحابة.
     *
     * @return {@code true} إذا تغيّر {@code progress}
     */
    public static boolean applyPassedFullFromHistoryToProgress(
            Context context,
            String studentId,
            StudentProgress progress) {
        if (progress == null || studentId == null || studentId.isEmpty()) {
            return false;
        }
        ArrayList<LetterQuizSession> sessions = mergedWithRemote(context, studentId, progress);
        boolean changed = false;
        for (LetterQuizSession s : sessions) {
            if (s == null || s.getOutcomes() == null) {
                continue;
            }
            for (LetterQuizSession.LetterOutcome o : s.getOutcomes()) {
                if (o == null) {
                    continue;
                }
                boolean fullPass = o.isPassedFull() || o.getCorrectCount() >= 2;
                if (!fullPass) {
                    continue;
                }
                String letter = StudentProgress.normalizeLatinLetterKey(o.getLetter());
                if (letter.isEmpty()) {
                    continue;
                }
                if (!Boolean.TRUE.equals(progress.getLettersQuizPassed().get(letter))) {
                    progress.getLettersQuizPassed().put(letter, true);
                    changed = true;
                }
                if (!Boolean.TRUE.equals(progress.getLettersLearned().get(letter))) {
                    progress.markLetterLearned(letter);
                    changed = true;
                }
            }
        }
        return changed;
    }
}
