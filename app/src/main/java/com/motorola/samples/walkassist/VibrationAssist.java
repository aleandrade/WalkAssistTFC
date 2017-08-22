package com.motorola.samples.walkassist;

import android.content.Context;
import android.os.Vibrator;

/**
 * Created by andrade on 8/21/17.
 */

public final class VibrationAssist {
    private static int mPreviousValue = 0;

    public static void setPreviousValue (int previousValue){
        mPreviousValue = previousValue;
    }

    public static void vibrateProximity(int proximity, Context context){
        proximity *= 10;
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if(mPreviousValue < 2500) {
            if (Math.abs(proximity - mPreviousValue) > 100) {
                if (proximity < 4000) {
                    vibrator.vibrate(new long[]{0, 150, (long) proximity*7/10, 150}, 2);

                } else {
                    vibrator.cancel();
                }
            }
        } else {
            if (Math.abs(proximity - mPreviousValue) > 500) {
                if (proximity < 4000) {
                    vibrator.vibrate(new long[]{0, 150, (long) proximity*7/10, 150}, 2);

                } else {
                    vibrator.cancel();
                }
            }
            mPreviousValue = proximity;
        }
        mPreviousValue = proximity;
    }

    public static void cancelVibration(Context context){
        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.cancel();
        mPreviousValue = 0;
    }

}
