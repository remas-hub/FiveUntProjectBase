package com.example.RemasProject;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.util.HashMap;
import java.util.Locale;

public class ArabicTextToSpeech {
    
    private TextToSpeech ttsEnglish;
    private TextToSpeech ttsArabic;
    private boolean isEnglishReady = false;
    private boolean isArabicReady = false;
    
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
        if (isEnglishReady && ttsEnglish != null) {
            ttsEnglish.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
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

