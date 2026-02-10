package com.example.RemasProject;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    
    private ImageView ivLogo;
    private TextView tvTitle, tvSubtitle;
    private View viewBackground;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        
        initializeViews();
        startAnimations();
    }
    
    private void initializeViews() {
        ivLogo = findViewById(R.id.ivLogo);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);
        viewBackground = findViewById(R.id.viewBackground);
        
        // إخفاء العناصر في البداية
        ivLogo.setAlpha(0f);
        tvTitle.setAlpha(0f);
        tvSubtitle.setAlpha(0f);
    }
    
    private void startAnimations() {
        // أنيميشن الخلفية
        animateBackground();
        
        // أنيميشن الشعار
        new Handler().postDelayed(() -> animateLogo(), 300);
        
        // أنيميشن العنوان
        new Handler().postDelayed(() -> animateTitle(), 800);
        
        // أنيميشن العنوان الفرعي
        new Handler().postDelayed(() -> animateSubtitle(), 1200);
        
        // الانتقال للشاشة التالية
        new Handler().postDelayed(() -> navigateToNext(), 3000);
    }
    
    private void animateBackground() {
        // تأثير تدرج لوني متحرك
        ValueAnimator colorAnimator = ValueAnimator.ofFloat(0f, 1f);
        colorAnimator.setDuration(2000);
        colorAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        colorAnimator.addUpdateListener(animation -> {
            float progress = animation.getAnimatedFraction();
            // يمكن إضافة تغيير لون الخلفية هنا
        });
        colorAnimator.start();
    }
    
    private void animateLogo() {
        ivLogo.setScaleX(0f);
        ivLogo.setScaleY(0f);
        ivLogo.setRotation(-180f);
        
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 0f, 1.2f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 0f, 1.2f, 1f);
        ObjectAnimator rotation = ObjectAnimator.ofFloat(ivLogo, "rotation", -180f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(ivLogo, "alpha", 0f, 1f);
        
        scaleX.setDuration(800);
        scaleY.setDuration(800);
        rotation.setDuration(800);
        alpha.setDuration(800);
        
        scaleX.setInterpolator(new BounceInterpolator());
        scaleY.setInterpolator(new BounceInterpolator());
        rotation.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        
        scaleX.start();
        scaleY.start();
        rotation.start();
        alpha.start();
        
        // تأثير نبض مستمر
        new Handler().postDelayed(() -> startPulseAnimation(), 1000);
    }
    
    private void startPulseAnimation() {
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(ivLogo, "scaleX", 1f, 1.1f, 1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(ivLogo, "scaleY", 1f, 1.1f, 1f);
        
        pulseX.setDuration(1000);
        pulseY.setDuration(1000);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseX.setInterpolator(new AccelerateDecelerateInterpolator());
        pulseY.setInterpolator(new AccelerateDecelerateInterpolator());
        
        pulseX.start();
        pulseY.start();
    }
    
    private void animateTitle() {
        tvTitle.setTranslationY(50f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(tvTitle, "translationY", 50f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(tvTitle, "alpha", 0f, 1f);
        
        translateY.setDuration(600);
        alpha.setDuration(600);
        translateY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        
        translateY.start();
        alpha.start();
    }
    
    private void animateSubtitle() {
        tvSubtitle.setTranslationY(30f);
        ObjectAnimator translateY = ObjectAnimator.ofFloat(tvSubtitle, "translationY", 30f, 0f);
        ObjectAnimator alpha = ObjectAnimator.ofFloat(tvSubtitle, "alpha", 0f, 1f);
        
        translateY.setDuration(600);
        alpha.setDuration(600);
        translateY.setInterpolator(new DecelerateInterpolator());
        alpha.setInterpolator(new AccelerateDecelerateInterpolator());
        
        translateY.start();
        alpha.start();
    }
    
    private void navigateToNext() {
        // التحقق من تسجيل الدخول
        SharedPreferences prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        String studentId = prefs.getString("studentId", null);
        boolean hasSeenTrial = prefs.getBoolean("hasSeenTrial", false);
        
        Intent intent;
        if (studentId != null) {
            // الانتقال للشاشة الرئيسية
            intent = new Intent(SplashActivity.this, EnglishLearningActivity.class);
        } else if (!hasSeenTrial) {
            // الانتقال لصفحة التجربة أولاً
            intent = new Intent(SplashActivity.this, TrialActivity.class);
        } else {
            // الانتقال لصفحة تسجيل الدخول
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }
        
        startActivity(intent);
        finish();
    }
}

