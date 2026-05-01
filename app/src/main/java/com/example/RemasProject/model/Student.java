/**
 * موديل الطالب اللي بنخزّنه بـ Appwrite — اسم، إيميل، صورة، وعلامة معلّم لو بدكم صلاحيات إعدادات.
 */
package com.example.RemasProject.model;

import java.util.Date;

public class Student {
    private String id;
    private String name;
    private String email;
    private String password; // سيتم تشفيره
    /** فارغ حتى يُرفع صورة؛ يجب إرسال القيمة لـ Appwrite عندما يكون الحقل مطلوباً في المخطط. */
    private String profileImageUrl = "";
    private Date createdAt;
    private Date lastLoginAt;
    private boolean isActive;
    /** إن كان true يُسمح للطالب برؤية إعدادات التعلّم في الشريط السفلي (إلى جانب بريد المشرف). */
    private boolean isTeacher;
    
    public Student() {}
    
    public Student(String name, String email, String password) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.profileImageUrl = "";
        this.createdAt = new Date();
        this.lastLoginAt = new Date();
        this.isActive = true;
        this.isTeacher = false;
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public boolean isTeacher() { return isTeacher; }
    public void setIsTeacher(boolean teacher) { this.isTeacher = teacher; }
}

