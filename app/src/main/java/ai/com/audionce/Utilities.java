package ai.com.audionce;

import android.graphics.BitmapFactory;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;

public class Utilities {

    public static final int SOUND_DURATION = 30000;
    public static final int SOUNDS_DISTANCE_AWAY_M = 15;
    public static final double SOUNDS_DISTANCE_AWAY_KM = SOUNDS_DISTANCE_AWAY_M / 1000.0;
    public static final String LOGIN_ALWAYS_ENABLED_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
                    "/audionce_prefs_do_not_delete.txt";

    public static int calculateInSampleSize(BitmapFactory.Options opts, int finW, int finH){
        int inSampleSize = 1;
        if(opts.outHeight > finH || opts.outWidth > finW){
            int halfW = opts.outWidth / 2;
            int halfH = opts.outHeight / 2;
            while((halfH / inSampleSize) > finH && (halfW / inSampleSize) > finW){
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    public static void makeLogFromThrowable(Throwable ex){
        Log.e("AUD",Log.getStackTraceString(ex));
    }

    public enum SignupState {
        USERNAME_ALREADY_EXISTS,
        ALL_OKAY,
        ERROR_THROWN;
    }

}
