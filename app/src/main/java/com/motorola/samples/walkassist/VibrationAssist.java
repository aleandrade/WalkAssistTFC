package com.motorola.samples.walkassist;

import android.content.Context;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;

import java.util.Locale;

/**
 * Created by andrade on 8/21/17.
 */

public final class VibrationAssist {
    private static double relativePreviousValue = 0;
    private static double lPreviousValue = 0;
    private static double rPreviousValue = 0;


    public static void setPreviousValue (int previousValue){
        relativePreviousValue = previousValue;
    }

    public static void vibrateProximity(int proximityL, int proximityM, int proximityR, TextToSpeech tts, final Context context){
        double value = (double) proximityM / 4095;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if(proximityM > 700){
          if(Math.abs(value - relativePreviousValue) > 0.05 || relativePreviousValue < 0.1) {
              if(value < 0.55) {
                  double result = (double) (1 - value) * 1000;
                  vibrator.vibrate(new long[]{0, 150, (long) result, 150}, 2);

              } else {
                  double result = (double) (1.01 - value) * 400;
                  vibrator.vibrate(new long[]{0, 150, (long) result, 150}, 2);
              }
          }
        } else {
            vibrator.cancel();
        }

        if(proximityR>2000 && proximityL>2000 && Math.abs(rPreviousValue - proximityR) > 400) {
            if(!tts.isSpeaking()){
                tts.speak("Dois lados obstáculo", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } else if(proximityR>2000 && Math.abs(rPreviousValue - proximityR) > 400){
            if(!tts.isSpeaking()){
                tts.speak("Direita obstáculo", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        } else if(proximityL>2000 && Math.abs(lPreviousValue - proximityL) > 400) {
            if(!tts.isSpeaking()) {
                tts.speak("Esquerda obstáculo", TextToSpeech.QUEUE_FLUSH, null, null);
            }
        }

        relativePreviousValue = value;
        lPreviousValue = proximityL;
        rPreviousValue = proximityR;
    }

    public static void cancelVibration(Context context){
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();
        relativePreviousValue = 0;
    }

}
