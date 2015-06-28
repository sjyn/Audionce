package ai.com.audionce;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.newline.sjyn.audionce.PrioritizedQueue;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

//import com.newline.sjyn.audionce.ActivityTracker;

//**********EXPERIMENTAL*********//
@Deprecated
public class SoundsPickupService2 extends Service implements AudioManager.OnAudioFocusChangeListener{
    private SoundPool pool;
    private PrioritizedQueue<Sound> pQueue;
    private ArrayList<Integer> soundIDs, playIDs;
    private LocationManager locationManager;
    private AudioManager audioManager;
    //    private ActivityTracker tracker;
    private boolean playing;

    public SoundsPickupService2() {
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onCreate(){
        super.onCreate();
        Log.e("AUD","Service2 Starting");
        playing = false;
        pQueue = new PrioritizedQueue<>();
        soundIDs = playIDs = new ArrayList<>();
        pool = new SoundPool(25,AudioManager.STREAM_MUSIC,0);
//        tracker = ActivityTracker.getActivityTracker();
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,15000,
                Utilities.SOUNDS_DISTANCE_AWAY_M,new LocationManagerListener());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
    }

    @Override
    public void onAudioFocusChange(int status) {
        switch (status) {
            case AudioManager.AUDIOFOCUS_GAIN:
                try {
                    resumeAllSounds();
                } catch (Exception ex){
                    playAllSounds();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                destroyAndReconfigureSoundPool();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                pauseAllSounds();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                pauseAllSounds();
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void configureSounds(){
        int i = 26;
        while(!pQueue.isEmpty()){
            Sound s = pQueue.reverseDequeueByPriority();
            soundIDs.add(pool.load(s.getUrl(),i--));
        }
    }

    private void pauseAllSounds(){
        pool.autoPause();
        playing = false;
    }

    private void playAllSounds(){
        Log.e("AUD","Service2 playing");
        float vol = 1.0f;
        int priority = 26;
        for(Integer i : soundIDs){
            if(vol != 0) {
                playIDs.add(pool.play(i, vol, vol, priority--, 0, 1.0f));
                vol -= 0.1;
            }
        }
        playing = true;
//        tracker.postSnackBarWithText("Playing Audio\nTap To Pause", new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if(playing)
//                    pauseAllSounds();
//                else
//                    resumeAllSounds();
//            }
//        });
    }

    @SuppressWarnings("deprecation")
    private void destroyAndReconfigureSoundPool(){
        playing = false;
        pool.autoPause();
        pool.release();
//        pool = null;
        pool = new SoundPool(25,AudioManager.STREAM_MUSIC,0);
        configureSounds();
    }

    private void resumeAllSounds(){
        pool.autoResume();
        playing = true;
    }

    private class LocationManagerListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location){
            ParseGeoPoint pgp = new ParseGeoPoint(location.getLatitude(),location.getLongitude());
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Sounds")
                    .whereWithinKilometers("location",pgp,Utilities.SOUNDS_DISTANCE_AWAY_KM);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if(e == null && !list.isEmpty()){
                        for(ParseObject po : list){
                            Sound s = Sound.parseSound(po);
                            pQueue.enqueue(s);
                        }
                        destroyAndReconfigureSoundPool();
                    } else if (e != null){
                        Utilities.makeLogFromThrowable(e);
                    }
                }
            });
        }

        @Override
        public void onProviderDisabled(String provider){
        }

        @Override
        public void onProviderEnabled(String provider){
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
