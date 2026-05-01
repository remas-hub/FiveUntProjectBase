/**
 * شو الطالب إنجز — حروف متعلمة، كلمات، اختبارات ناجحة، وسجل جلسات الاختبار؛ هاد المستند اللي بنحدّثه من الشاشات.
 */
package com.example.RemasProject.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class StudentProgress {
    private String id;
    private String studentId;
    private String studentName;
    
    // تقدم الحروف
    private Map<String, Boolean> lettersLearned; // الحروف التي تعلمها
    private int lettersCount; // عدد الحروف المتقنة
    
    // تقدم الكلمات
    private Map<String, Integer> wordsProgress; // الكلمات مع عدد المرات التي تعلمها
    private int wordsCount; // عدد الكلمات المتقنة
    
    /** حروف نجح في اختبارها مرتين (يُعرض بلون برتقالي في الشبكة). */
    private Map<String, Boolean> lettersQuizPassed;

    /** سجل جلسات اختبار الحروف (الأحدث أولاً في الواجهة بعد الإضافة). */
    private List<LetterQuizSession> letterQuizHistory;

    private static final int MAX_LETTER_QUIZ_HISTORY_SESSIONS = 40;

    /**
     * حقول لعبة الممارسة المحذوفة من الواجهة؛ ما زالت مطلوبة في مخطط Appwrite؛ تُرسل كأصفار.
     */
    private int practiceScore;
    private int practiceGamesPlayed;
    private int practiceCorrectAnswers;
    private int practiceTotalAnswers;
    
    // إحصائيات عامة
    private Date lastActivityDate;
    private int totalLearningTime; // بالدقائق
    private Date createdAt;
    private Date updatedAt;
    
    public StudentProgress() {
        this.lettersLearned = new HashMap<>();
        this.wordsProgress = new HashMap<>();
        this.lettersQuizPassed = new HashMap<>();
        this.letterQuizHistory = new ArrayList<>();
        this.lettersCount = 0;
        this.wordsCount = 0;
        this.practiceScore = 0;
        this.practiceGamesPlayed = 0;
        this.practiceCorrectAnswers = 0;
        this.practiceTotalAnswers = 0;
        this.totalLearningTime = 0;
        this.createdAt = new Date();
        this.updatedAt = new Date();
        this.lastActivityDate = new Date();
    }
    
    public StudentProgress(String studentId, String studentName) {
        this();
        this.studentId = studentId;
        this.studentName = studentName;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public Map<String, Boolean> getLettersLearned() {
        if (lettersLearned == null) {
            lettersLearned = new HashMap<>();
        }
        return lettersLearned;
    }
    public void setLettersLearned(Map<String, Boolean> lettersLearned) { 
        this.lettersLearned = lettersLearned;
        updateLettersCount();
    }
    
    public int getLettersCount() { return lettersCount; }
    public void setLettersCount(int lettersCount) { this.lettersCount = lettersCount; }
    
    public Map<String, Integer> getWordsProgress() { return wordsProgress; }
    public void setWordsProgress(Map<String, Integer> wordsProgress) { 
        this.wordsProgress = wordsProgress;
        updateWordsCount();
    }
    
    public int getWordsCount() { return wordsCount; }
    public void setWordsCount(int wordsCount) { this.wordsCount = wordsCount; }
    
    public Map<String, Boolean> getLettersQuizPassed() {
        if (lettersQuizPassed == null) {
            lettersQuizPassed = new HashMap<>();
        }
        return lettersQuizPassed;
    }
    
    public void setLettersQuizPassed(Map<String, Boolean> lettersQuizPassed) {
        this.lettersQuizPassed = lettersQuizPassed != null ? lettersQuizPassed : new HashMap<>();
    }

    public List<LetterQuizSession> getLetterQuizHistory() {
        if (letterQuizHistory == null) {
            letterQuizHistory = new ArrayList<>();
        }
        return letterQuizHistory;
    }

    public void setLetterQuizHistory(List<LetterQuizSession> letterQuizHistory) {
        this.letterQuizHistory = letterQuizHistory != null ? letterQuizHistory : new ArrayList<>();
    }

    public void addLetterQuizSession(LetterQuizSession session) {
        if (session == null) {
            return;
        }
        getLetterQuizHistory().add(0, session);
        trimLetterQuizHistory();
    }

    private void trimLetterQuizHistory() {
        List<LetterQuizSession> L = getLetterQuizHistory();
        while (L.size() > MAX_LETTER_QUIZ_HISTORY_SESSIONS) {
            L.remove(L.size() - 1);
        }
    }

    private void sortLetterQuizHistoryDescending() {
        getLetterQuizHistory().sort((a, b) ->
                Long.compare(b.getCompletedAtEpochMs(), a.getCompletedAtEpochMs()));
    }

    /**
     * يدمج سجل الاختبارات من مستند آخر دون تكرار نفس الطابع الزمني.
     *
     * @return {@code true} إذا تغيّر السجل
     */
    public boolean mergeLetterQuizHistoryFrom(StudentProgress other) {
        if (other == null) {
            return false;
        }
        List<LetterQuizSession> theirs = other.getLetterQuizHistory();
        if (theirs.isEmpty()) {
            return false;
        }
        Set<Long> seen = new HashSet<>();
        for (LetterQuizSession s : getLetterQuizHistory()) {
            if (s != null) {
                seen.add(s.getCompletedAtEpochMs());
            }
        }
        boolean added = false;
        for (LetterQuizSession s : theirs) {
            if (s != null && seen.add(s.getCompletedAtEpochMs())) {
                getLetterQuizHistory().add(new LetterQuizSession(s));
                added = true;
            }
        }
        if (!added) {
            return false;
        }
        sortLetterQuizHistoryDescending();
        trimLetterQuizHistory();
        return true;
    }
    
    public int getPracticeScore() {
        return practiceScore;
    }
    
    public void setPracticeScore(int practiceScore) {
        this.practiceScore = practiceScore;
    }
    
    public int getPracticeGamesPlayed() {
        return practiceGamesPlayed;
    }
    
    public void setPracticeGamesPlayed(int practiceGamesPlayed) {
        this.practiceGamesPlayed = practiceGamesPlayed;
    }
    
    public int getPracticeCorrectAnswers() {
        return practiceCorrectAnswers;
    }
    
    public void setPracticeCorrectAnswers(int practiceCorrectAnswers) {
        this.practiceCorrectAnswers = practiceCorrectAnswers;
    }
    
    public int getPracticeTotalAnswers() {
        return practiceTotalAnswers;
    }
    
    public void setPracticeTotalAnswers(int practiceTotalAnswers) {
        this.practiceTotalAnswers = practiceTotalAnswers;
    }
    
    public Date getLastActivityDate() { return lastActivityDate; }
    public void setLastActivityDate(Date lastActivityDate) { this.lastActivityDate = lastActivityDate; }
    
    public int getTotalLearningTime() { return totalLearningTime; }
    public void setTotalLearningTime(int totalLearningTime) { this.totalLearningTime = totalLearningTime; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
    
    // Methods
    public void markLetterLearned(String letter) {
        getLettersLearned().put(normalizeLatinLetterKey(letter), true);
        updateLettersCount();
        updatedAt = new Date();
        lastActivityDate = new Date();
    }

    /** حرف لاتيني واحد بصيغة العرض الموحّدة (A–Z). */
    public static String normalizeLatinLetterKey(String raw) {
        // المفتاح في الخرائط دائماً حرف كبير واحد؛ يمنع تكرار "a" و"A" كمدخلين مختلفين
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.length() != 1) {
            return t;
        }
        char c = t.charAt(0);
        if (c >= 'a' && c <= 'z') {
            return String.valueOf((char) (c - 'a' + 'A'));
        }
        return t;
    }

    private static Map<String, Boolean> normalizeLetterBoolMap(Map<String, Boolean> src) {
        if (src == null || src.isEmpty()) {
            return new HashMap<>();
        }
        Map<String, Boolean> out = new HashMap<>();
        for (Map.Entry<String, Boolean> e : src.entrySet()) {
            if (e.getKey() == null) {
                continue;
            }
            String k = normalizeLatinLetterKey(e.getKey());
            if (k.isEmpty()) {
                continue;
            }
            boolean v = Boolean.TRUE.equals(e.getValue()) || Boolean.TRUE.equals(out.get(k));
            out.put(k, v);
        }
        return out;
    }

    /**
     * يوحّد مفاتيح الحروف (مثل {@code a} → {@code A}) بعد القراءة من السحابة.
     *
     * @return {@code true} إذا تغيّرت الخرائط (يستحق حفظ PATCH)
     */
    public boolean normalizeLetterMapKeys() {
        Map<String, Boolean> newL = normalizeLetterBoolMap(getLettersLearned());
        Map<String, Boolean> newQ = normalizeLetterBoolMap(getLettersQuizPassed());
        boolean changed = !newL.equals(getLettersLearned()) || !newQ.equals(getLettersQuizPassed());
        setLettersLearned(newL);
        setLettersQuizPassed(newQ);
        return changed;
    }

    /**
     * يدمج حروفاً ناجحة/متعلّمة من مستند تقدّم آخر (مثل صف ثانٍ في Appwrite لنفس الطالب).
     *
     * @return {@code true} إذا حدث تعديل
     */
    public boolean mergeLetterStateFrom(StudentProgress other) {
        if (other == null) {
            return false;
        }
        boolean changed = false;
        for (Map.Entry<String, Boolean> e : other.getLettersQuizPassed().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                continue;
            }
            String k = normalizeLatinLetterKey(e.getKey());
            if (k.isEmpty()) {
                continue;
            }
            if (!Boolean.TRUE.equals(getLettersQuizPassed().get(k))) {
                getLettersQuizPassed().put(k, true);
                changed = true;
            }
        }
        for (Map.Entry<String, Boolean> e : other.getLettersLearned().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                continue;
            }
            String k = normalizeLatinLetterKey(e.getKey());
            if (k.isEmpty()) {
                continue;
            }
            if (!Boolean.TRUE.equals(getLettersLearned().get(k))) {
                getLettersLearned().put(k, true);
                changed = true;
            }
        }
        if (mergeLetterQuizHistoryFrom(other)) {
            changed = true;
        }
        if (changed) {
            updateLettersCount();
            updatedAt = new Date();
            lastActivityDate = new Date();
        }
        return changed;
    }

    /**
     * يضمن أن كل حرف في {@code lettersQuizPassed} يُسجَّل أيضاً في {@code lettersLearned}
     * (التقدّم الإجمالي وشاشة «تعلّم الحروف» تعتمد على الأخير).
     *
     * @return {@code true} إذا حدث تعديل ويستحق رفع التحديث إلى السحابة
     */
    public boolean syncLettersLearnedFromQuizPassed() {
        boolean changed = false;
        for (Map.Entry<String, Boolean> e : getLettersQuizPassed().entrySet()) {
            if (!Boolean.TRUE.equals(e.getValue())) {
                continue;
            }
            String letter = normalizeLatinLetterKey(e.getKey());
            if (letter.isEmpty()) {
                continue;
            }
            if (!Boolean.TRUE.equals(getLettersLearned().get(letter))) {
                getLettersLearned().put(letter, true);
                changed = true;
            }
        }
        if (changed) {
            updateLettersCount();
            updatedAt = new Date();
            lastActivityDate = new Date();
        }
        return changed;
    }
    
    public void markWordLearned(String word) {
        int count = wordsProgress.getOrDefault(word, 0);
        wordsProgress.put(word, count + 1);
        updateWordsCount();
        updatedAt = new Date();
        lastActivityDate = new Date();
    }
    
    public void addLearningTime(int minutes) {
        totalLearningTime += minutes;
        updatedAt = new Date();
    }
    
    private void updateLettersCount() {
        lettersCount = (int) getLettersLearned().values().stream().filter(b -> Boolean.TRUE.equals(b)).count();
    }
    
    private void updateWordsCount() {
        wordsCount = (int) wordsProgress.values().stream().filter(count -> count > 0).count();
    }
    
    public double getProgressPercentage() {
        int totalPossible = 26 + 26;
        int currentProgress = lettersCount + wordsCount;
        return totalPossible <= 0 ? 0 : (currentProgress * 100.0) / totalPossible;
    }
}

