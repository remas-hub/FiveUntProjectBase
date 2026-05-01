/**
 * يظبط الحواف مع النوتش وشريط الحالة — عشان المحتوى ما يطلع تحت الساعة أو زرّات النظام.
 */
package com.example.RemasProject.Hellper;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

public final class UiInsetsHelper {

    private UiInsetsHelper() {
    }

    public static void enableEdgeToEdge(AppCompatActivity activity) {
        // السماح للمحتوى يمتد خلف شريط الحالة والتنقل؛ الحشوة تصير مسؤوليتنا يدوياً على العناصر المهمة
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);
    }

    /**
     * يضيف حشوة الجذر مع الحفاظ على الحشوة الموجودة في XML (مناسب لمعظم الشاشات).
     */
    public static void applySafePadding(View target) {
        final int pl = target.getPaddingLeft();
        final int pt = target.getPaddingTop();
        final int pr = target.getPaddingRight();
        final int pb = target.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(target, (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(pl + insets.left, pt + insets.top, pr + insets.right, pb + insets.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(target);
    }

    /**
     * هيدر ملون: حشوة علوية = شريط الحالة + النوتش (بدون تكرار على الجذر).
     */
    public static void applyStatusBarPadding(View header) {
        final int baseTop = header.getPaddingTop();
        final int pl = header.getPaddingLeft();
        final int pr = header.getPaddingRight();
        final int pb = header.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, windowInsets) -> {
            Insets topInsets = windowInsets.getInsets(
                    WindowInsetsCompat.Type.statusBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(pl, baseTop + topInsets.top, pr, pb);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /**
     * شريط سفلي: حشوة سفلية = شريط التنقل / الإيماءات.
     */
    public static void applyNavigationBarPadding(View bar) {
        final int pl = bar.getPaddingLeft();
        final int pt = bar.getPaddingTop();
        final int pr = bar.getPaddingRight();
        final int baseBottom = bar.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(bar, (v, windowInsets) -> {
            int bottom = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
            // لو الكيبورد طالع نأخذ الأكبر حتى الشريط السفلي ما يختفي تحت لوحة المفاتيح
            int ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int extraBottom = Math.max(bottom, ime);
            v.setPadding(pl, pt, pr, baseBottom + extraBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(bar);
    }
}
