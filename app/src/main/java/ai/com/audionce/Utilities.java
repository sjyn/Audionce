package ai.com.audionce;

import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

public class Utilities {

    public static final int SOUND_DURATION = 30000;

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

}
