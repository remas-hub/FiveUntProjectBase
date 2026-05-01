/**
 * Glide لوحده ما يزبط مع روابط تنزيل Appwrite لأنه بدون مفاتيح؛ هون بنحمّل الصورة بالـ API key عبر الطبقة ونعرضها بالـ ImageView.
 */
package com.example.RemasProject.Hellper;

import android.app.Activity;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

public final class AppwriteImageLoader {

    private AppwriteImageLoader() {}

    public static boolean looksLikeAppwriteStorageDownloadUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        boolean hostOk = url.contains("appwrite.io");
        boolean pathOk = url.contains("/storage/buckets/") && url.contains("/files/")
                && url.contains("/download");
        return hostOk && pathOk;
    }

    /**
     * يعرض الصورة دائرية؛ روابط Appwrite تُنزَّل عبر DAL مع المصادقة.
     */
    public static void loadCircleProfile(Activity activity, ImageView imageView, String url,
                                        int placeholderDrawableRes) {
        if (activity == null || imageView == null) {
            return;
        }
        if (url == null || url.isEmpty()) {
            imageView.setImageResource(placeholderDrawableRes);
            return;
        }
        if (!looksLikeAppwriteStorageDownloadUrl(url)) {
            // رابط عادي (مثلاً CDN) — Glide يكفي
            Glide.with(activity).load(url).circleCrop().into(imageView);
            return;
        }

        // رابط Appwrite /download: لازم بايتات مع رؤوس المشروع والمفتاح، بعدها نمرّرها لـ Glide كـ bytes
        new Thread(() -> {
            DALAppWriteConnection dal = new DALAppWriteConnection(activity.getApplicationContext());
            DALAppWriteConnection.OperationResult<byte[]> r = dal.downloadStorageAuthenticated(url);
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (r.success && r.data != null && r.data.length > 0) {
                    Glide.with(activity).load(r.data).circleCrop().into(imageView);
                } else {
                    imageView.setImageResource(placeholderDrawableRes);
                }
            });
        }).start();
    }
}
