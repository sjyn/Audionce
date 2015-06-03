package ai.com.audionce;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Utilities {

    public static final int SOUND_DURATION = 30000;
    public static final int SOUNDS_DISTANCE_AWAY_M = 50;
    public static final double SOUNDS_DISTANCE_AWAY_KM = SOUNDS_DISTANCE_AWAY_M / 1000.0;
    public static final String LOGIN_ALWAYS_ENABLED_PATH =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) +
                    "/audionce_prefs_do_not_delete.txt";
    public static final int FLAG_FROM_SERVICE_TO_HUB = 76654;
    public static final int NOTIFICATION_ID = 9;

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

    public static boolean doesHaveNetworkConnection(Context c) {
        boolean wifi = false;
        boolean mobile = false;
        ConnectivityManager manager = (ConnectivityManager) c.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo[] info = manager.getAllNetworkInfo();
        for (NetworkInfo ni : info) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                wifi = ni.isConnected();
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                mobile = ni.isConnected();
        }
        return wifi || mobile;
    }

    public static void makeToast(Context c, String s) {
        Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
    }

    public enum SignupState {
        USERNAME_ALREADY_EXISTS,
        ALL_OKAY,
        ERROR_THROWN
    }

}
