/**
 * هاد أول ما يفتح التطبيق — شاشة ترحيبية خفيفة؛ إذا في طالب محفوظ بننتقل للرئيسية، وإلا لشاشة الدخول/التسجيل.
 */
package com.example.RemasProject;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.RemasProject.Hellper.UiInsetsHelper;

public class SplashActivity extends AppCompatActivity {
    
    private ImageView ivLogo;
    private TextView tvTitle, tvSubtitle;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // المحتوى يطلع من تحت شريط الحالة؛ الحشوة على الجذر حتى ما يختفي اللوقو تحت النوتش
        UiInsetsHelper.enableEdgeToEdge(this);
        View content = findViewById(android.R.id.content);
        if (content instanceof ViewGroup && ((ViewGroup) content).getChildCount() > 0) {
            UiInsetsHelper.applySafePadding(((ViewGroup) content).getChildAt(0));
        }
        
        initializeViews();
        startAnimations();
    }
    
    private void initializeViews() {
        ivLogo = findViewById(R.id.ivLogo);
        tvTitle = findViewById(R.id.tvTitle);
        tvSubtitle = findViewById(R.id.tvSubtitle);

        // إخفاء العناصر في البداية
        ivLogo.setAlpha(0f);
        tvTitle.setAlpha(0f);
        tvSubtitle.setAlpha(0f);
    }
    
    private void startAnimations() {
        // تتابع بصري: شعار → عنوان → فرعي، وبعد ~3 ثواني نقرر وجهة التطبيق
        // أنيميشن الشعار
        new Handler().postDelayed(() -> animateLogo(), 300);
        
        // أنيميشن العنوان
        new Handler().postDelayed(() -> animateTitle(), 800);
        
        // أنيميشن العنوان الفرعي
        new Handler().postDelayed(() -> animateSubtitle(), 1200);
        
        // الانتقال للشاشة التالية
        new Handler().postDelayed(() -> navigateToNext(), 3000);
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
        // نفس اسم الـ prefs اللي بيحفظه LoginActivity بعد الدخول الناجح
        SharedPreferences prefs = getSharedPreferences("EnglishLearning", MODE_PRIVATE);
        String studentId = prefs.getString("studentId", null);

        Intent intent;
        if (studentId != null && !studentId.trim().isEmpty()) {
            intent = new Intent(SplashActivity.this, EnglishLearningActivity.class);
        } else {
            intent = new Intent(SplashActivity.this, LoginActivity.class);
        }

        startActivity(intent);
        finish();
    }
}

