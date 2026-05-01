/**
 * صف بقاعدة البيانات يحكي كم مرة الطالب سمع هالحرف — منحدّث العداد كل ما يضغط استماع.
 */
package com.example.RemasProject.model;

import java.util.Date;

public class LetterListenRecord {

    public static final String COLLECTION_NAME = "letter_listens";

    private String id;
    /** معرف الطالب (نفس القيمة المحفوظة كـ studentId في الجلسة ومستند الطالب). */
    private String studentRef;
    /** الحرف اللاتيني الواحد، مثل "A". */
    private String letter;
    private int listenCount;
    private Date updatedAt;

    public LetterListenRecord() {
        this.updatedAt = new Date();
    }

    public LetterListenRecord(String studentRef, String letter, int listenCount) {
        this.studentRef = studentRef;
        this.letter = letter;
        this.listenCount = listenCount;
        this.updatedAt = new Date();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStudentRef() {
        return studentRef;
    }

    public void setStudentRef(String studentRef) {
        this.studentRef = studentRef;
    }

    public String getLetter() {
        return letter;
    }

    public void setLetter(String letter) {
        this.letter = letter;
    }

    public int getListenCount() {
        return listenCount;
    }

    public void setListenCount(int listenCount) {
        this.listenCount = listenCount;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void incrementListen() {
        // يُستدعى قبل PATCH؛ الطابع الزمني يساعد على تتبع آخر نشاط استماع
        this.listenCount++;
        this.updatedAt = new Date();
    }
}