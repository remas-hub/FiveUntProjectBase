package com.example.RemasProject.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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
    
    // لعبة الممارسة
    private int practiceScore; // أعلى درجة
    private int practiceGamesPlayed; // عدد الألعاب
    private int practiceCorrectAnswers; // الإجابات الصحيحة
    private int practiceTotalAnswers; // إجمالي الإجابات
    
    // إحصائيات عامة
    private Date lastActivityDate;
    private int totalLearningTime; // بالدقائق
    private Date createdAt;
    private Date updatedAt;
    
    public StudentProgress() {
        this.lettersLearned = new HashMap<>();
        this.wordsProgress = new HashMap<>();
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
    
    public Map<String, Boolean> getLettersLearned() { return lettersLearned; }
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
    
    public int getPracticeScore() { return practiceScore; }
    public void setPracticeScore(int practiceScore) { this.practiceScore = practiceScore; }
    
    public int getPracticeGamesPlayed() { return practiceGamesPlayed; }
    public void setPracticeGamesPlayed(int practiceGamesPlayed) { 
        this.practiceGamesPlayed = practiceGamesPlayed; 
    }
    
    public int getPracticeCorrectAnswers() { return practiceCorrectAnswers; }
    public void setPracticeCorrectAnswers(int practiceCorrectAnswers) { 
        this.practiceCorrectAnswers = practiceCorrectAnswers; 
    }
    
    public int getPracticeTotalAnswers() { return practiceTotalAnswers; }
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
        lettersLearned.put(letter, true);
        updateLettersCount();
        updatedAt = new Date();
        lastActivityDate = new Date();
    }
    
    public void markWordLearned(String word) {
        int count = wordsProgress.getOrDefault(word, 0);
        wordsProgress.put(word, count + 1);
        updateWordsCount();
        updatedAt = new Date();
        lastActivityDate = new Date();
    }
    
    public void updatePracticeResult(int score, boolean isCorrect) {
        practiceGamesPlayed++;
        practiceTotalAnswers++;
        if (isCorrect) {
            practiceCorrectAnswers++;
        }
        if (score > practiceScore) {
            practiceScore = score;
        }
        updatedAt = new Date();
        lastActivityDate = new Date();
    }
    
    public void addLearningTime(int minutes) {
        totalLearningTime += minutes;
        updatedAt = new Date();
    }
    
    private void updateLettersCount() {
        lettersCount = (int) lettersLearned.values().stream().filter(b -> b).count();
    }
    
    private void updateWordsCount() {
        wordsCount = (int) wordsProgress.values().stream().filter(count -> count > 0).count();
    }
    
    public double getProgressPercentage() {
        // الحروف (26) + الكلمات (26) + الممارسة (100 نقطة)
        int totalPossible = 26 + 26 + 100;
        int currentProgress = lettersCount + wordsCount + Math.min(practiceScore, 100);
        return (currentProgress * 100.0) / totalPossible;
    }
}

