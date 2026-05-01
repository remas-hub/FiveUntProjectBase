/**
 * جلسة اختبار وحدة — وقت الإنهاء وقائمة بنتيجة كل حرف؛ بنخزنها ضمن تقدم الطالب كـ JSON.
 */
package com.example.RemasProject.model;

import java.util.ArrayList;
import java.util.List;

public class LetterQuizSession {

    private long completedAtEpochMs;
    private List<LetterOutcome> outcomes;

    public LetterQuizSession() {
        this.outcomes = new ArrayList<>();
    }

    public LetterQuizSession(long completedAtEpochMs, List<LetterOutcome> outcomes) {
        this.completedAtEpochMs = completedAtEpochMs;
        this.outcomes = outcomes != null ? outcomes : new ArrayList<>();
    }

    /** نسخة مستقلّة للدمج من مستند آخر. */
    public LetterQuizSession(LetterQuizSession src) {
        if (src == null) {
            this.completedAtEpochMs = 0;
            this.outcomes = new ArrayList<>();
            return;
        }
        this.completedAtEpochMs = src.completedAtEpochMs;
        this.outcomes = new ArrayList<>();
        if (src.outcomes != null) {
            for (LetterOutcome o : src.outcomes) {
                if (o != null) {
                    this.outcomes.add(new LetterOutcome(o));
                }
            }
        }
    }

    public long getCompletedAtEpochMs() {
        return completedAtEpochMs;
    }

    public void setCompletedAtEpochMs(long completedAtEpochMs) {
        this.completedAtEpochMs = completedAtEpochMs;
    }

    public List<LetterOutcome> getOutcomes() {
        if (outcomes == null) {
            outcomes = new ArrayList<>();
        }
        return outcomes;
    }

    public void setOutcomes(List<LetterOutcome> outcomes) {
        this.outcomes = outcomes != null ? outcomes : new ArrayList<>();
    }

    public static class LetterOutcome {
        private String letter;
        /** عدد الإجابات الصحيحة من أصل سؤالين لكل حرف. */
        private int correctCount;
        private boolean passedFull;

        public LetterOutcome() {
        }

        public LetterOutcome(String letter, int correctCount, boolean passedFull) {
            this.letter = letter;
            this.correctCount = correctCount;
            this.passedFull = passedFull;
        }

        public LetterOutcome(LetterOutcome o) {
            if (o == null) {
                return;
            }
            this.letter = o.letter;
            this.correctCount = o.correctCount;
            this.passedFull = o.passedFull;
        }

        public String getLetter() {
            return letter;
        }

        public void setLetter(String letter) {
            this.letter = letter;
        }

        public int getCorrectCount() {
            return correctCount;
        }

        public void setCorrectCount(int correctCount) {
            this.correctCount = correctCount;
        }

        public boolean isPassedFull() {
            return passedFull;
        }

        public void setPassedFull(boolean passedFull) {
            this.passedFull = passedFull;
        }
    }
}
