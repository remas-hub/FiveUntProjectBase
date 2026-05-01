/**
 * يجهّزلك صوتين — إنجليزي وعربي — عشان تسمع الحروف والكلمات؛ يتعامل مع التهيئة والأخطاء على الخيط الرئيسي بشكل مرتب.
 */
package com.example.RemasProject;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class ArabicTextToSpeech {
    
    private TextToSpeech ttsEnglish;
    private TextToSpeech ttsArabic;
    private boolean isEnglishReady = false;
    private boolean isArabicReady = false;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    
    public ArabicTextToSpeech(Context context, TextToSpeech.OnInitListener listener) {
        // تهيئة TextToSpeech للإنجليزية
        ttsEnglish = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = ttsEnglish.setLanguage(Locale.US);
                isEnglishReady = (result != TextToSpeech.LANG_MISSING_DATA && 
                                 result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (listener != null) listener.onInit(status);
            }
        });
        
        // تهيئة TextToSpeech للعربية
        ttsArabic = new TextToSpeech(context, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale arabicLocale = new Locale("ar", "SA");
                int result = ttsArabic.setLanguage(arabicLocale);
                isArabicReady = (result != TextToSpeech.LANG_MISSING_DATA && 
                                result != TextToSpeech.LANG_NOT_SUPPORTED);
                if (!isArabicReady) {
                    // محاولة مع locale عام
                    result = ttsArabic.setLanguage(new Locale("ar"));
                    isArabicReady = (result != TextToSpeech.LANG_MISSING_DATA && 
                                    result != TextToSpeech.LANG_NOT_SUPPORTED);
                }
            }
        });
    }
    
    public void speakEnglish(String text) {
        speakEnglish(text, null);
    }

    /**
     * نطق بالإنجليزية؛ يُستدعى {@code onUtteranceDone} على الخيط الرئيسي بعد انتهاء النطق (أو عند الخطأ).
     */
    public void speakEnglish(String text, Runnable onUtteranceDone) {
        if (ttsEnglish == null) {
            if (onUtteranceDone != null) {
                mainHandler.post(onUtteranceDone);
            }
            return;
        }
        if (!isEnglishReady) {
            Log.d("TTS", "الإنجليزية غير جاهزة بعد");
            if (onUtteranceDone != null) {
                mainHandler.post(onUtteranceDone);
            }
            return;
        }
        final Runnable finish = () -> {
            if (onUtteranceDone != null) {
                onUtteranceDone.run();
            }
        };
        String utteranceId = "en_" + System.nanoTime();
        // UtteranceProgressListener يخبرنا متى انتهى النطق؛ نرجع للـ main thread عشان الكود اللي بعد الصوت يشتغل بأمان
        ttsEnglish.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            private void done() {
                mainHandler.post(finish);
                ttsEnglish.setOnUtteranceProgressListener(null);
            }

            @Override
            public void onStart(String utteranceId) {
            }

            @Override
            public void onDone(String utteranceId) {
                done();
            }

            @Override
            public void onError(String utteranceId) {
                done();
            }

            @Override
            public void onError(String utteranceId, int errorCode) {
                done();
            }
        });
        Bundle params = new Bundle();
        int code = ttsEnglish.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        if (code != TextToSpeech.SUCCESS) {
            ttsEnglish.setOnUtteranceProgressListener(null);
            mainHandler.post(finish);
        }
    }
    
    public void speakArabic(String text) {
        if (isArabicReady && ttsArabic != null) {
            ttsArabic.speak(text, TextToSpeech.QUEUE_ADD, null, null);
        } else {
            // Fallback: استخدام الإنجليزية إذا لم تكن العربية متوفرة
            Log.d("TTS", "العربية غير متوفرة، استخدام الإنجليزية");
        }
    }
    
    public void speakBoth(String englishText, String arabicText) {
        speakEnglish(englishText);
        // تأخير بسيط قبل نطق العربية
        new android.os.Handler().postDelayed(() -> {
            speakArabic(arabicText);
        }, 500);
    }
    
    public void shutdown() {
        if (ttsEnglish != null) {
            ttsEnglish.stop();
            ttsEnglish.shutdown();
        }
        if (ttsArabic != null) {
            ttsArabic.stop();
            ttsArabic.shutdown();
        }
    }
    
    public void stop() {
        if (ttsEnglish != null) {
            ttsEnglish.stop();
        }
        if (ttsArabic != null) {
            ttsArabic.stop();
        }
    }
}

