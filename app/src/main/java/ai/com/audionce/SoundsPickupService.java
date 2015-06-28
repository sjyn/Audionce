package ai.com.audionce;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.newline.sjyn.audionce.PrioritizedQueue;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.io.IOException;
import java.util.List;


//TODO -- Play multiple sounds at once using SoundPool
//TODO -- shave

public class SoundsPickupService extends Service implements AudioManager.OnAudioFocusChangeListener {
    private LocationManager manager;
    private PrioritizedQueue<Sound> playQueue;
    private Sound playingSound;
    private MediaPlayer tPlayer;
    private AudioManager audioManager;

    public SoundsPickupService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e("AUD", "Service stopping");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e("AUD", "Service starting");
        playQueue = new PrioritizedQueue<>();
        playingSound = null;
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 30, new SoundLocationListener());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int res = audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (res != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.e("AUD", "Could not get audio focus");
        }
    }

    @Override
    public void onAudioFocusChange(int status) {
        switch (status) {
            case AudioManager.AUDIOFOCUS_GAIN:
                if (tPlayer == null) {
                    try {
                        playMedia();
                    } catch (Exception ex) {
                        Utilities.makeLogFromThrowable(ex);
                    }
                } else {
                    try {
                        tPlayer.start();
                    } catch (Exception ex) {
                        Utilities.makeLogFromThrowable(ex);
                        tPlayer = null;
                    }
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                if (tPlayer != null) {
                    if (tPlayer.isPlaying())
                        tPlayer.stop();
                    tPlayer.release();
                    tPlayer = null;
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                if (tPlayer != null) {
                    if (tPlayer.isPlaying())
                        tPlayer.pause();
                }
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                if (tPlayer != null) {
                    try {
                        if (tPlayer.isPlaying())
                            tPlayer.pause();
                    } catch (Exception ignored) {
                        Utilities.makeLogFromThrowable(ignored);
                        tPlayer = null;
                    }
                }
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("unused")
    private void playMedia() throws IOException {
        Log.e("AUD", "playMedia() called");
        Log.e("AUD", "sound null? " + (playingSound == null));
        Log.e("AUD", "playQueue empty? " + playQueue.isEmpty());
        if(!playQueue.isEmpty() && playingSound == null){
            Log.e("AUD", "preparing to play media");
            tPlayer = new MediaPlayer();
            tPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            playingSound = playQueue.dequeueByPriority();
            makeNotification(playingSound.getTitle());
            Log.e("AUD", playingSound.getUrl());
            tPlayer.setDataSource(getApplicationContext(), Uri.parse(playingSound.getUrl()));
            tPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    try {
                        mp.release();
                        mp = null;
                        playingSound = null;
                        playMedia();
                    } catch (Exception ex) {
                        Log.e("AUD", Log.getStackTraceString(ex));
                    }
                }
            });
            tPlayer.prepare();
            tPlayer.start();
        }
    }

    private class SoundLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(final Location l){
            Log.e("AUD", "Location changed from service");
            ParseGeoPoint mpgp = new ParseGeoPoint(l.getLatitude(),l.getLongitude());
            ParseQuery<ParseObject> soundsNearMe = ParseQuery.getQuery("Sounds");
            soundsNearMe.whereWithinKilometers("location",mpgp,Utilities.SOUNDS_DISTANCE_AWAY_KM);
            soundsNearMe.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if(e == null && !list.isEmpty()){
                        playQueue.clear();
                        Log.e("AUD", "did find sounds? " + list.size());
                        for(ParseObject po : list){
                            try {
                                Sound s = Sound.parseSound(po);
                                LatLng sLatLng = s.getLatLng();
                                Location sLoc = new Location("");
                                sLoc.setLongitude(sLatLng.longitude);
                                sLoc.setLatitude(sLatLng.latitude);
                                s.setPriority(sLoc.distanceTo(l));
                                playQueue.enqueue(s);
//                                Log.e("AUD", "playQueue still empty? " + playQueue.isEmpty());
                            } catch (Exception ex){
                                Utilities.makeLogFromThrowable(ex);
                            }
                        }
                    }
                    if (e == null) {
                        try{
                            playMedia();
                        } catch (Exception ex){
                            Utilities.makeLogFromThrowable(ex);
                        }
                    }
                }
            });
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras){

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private void makeNotification(String title) {
        Intent toHub = new Intent(this, HubActivity.class);
        toHub.putExtra("username",
                PreferenceManager.getDefaultSharedPreferences(this).getString("saved_username", ""));
        toHub.putExtra("password",
                PreferenceManager.getDefaultSharedPreferences(this).getString("saved_password", ""));
        toHub.addFlags(Utilities.FLAG_FROM_SERVICE_TO_HUB);
        PendingIntent pi = PendingIntent.getActivity(getApplicationContext(),
                0, toHub, PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.launcher)
                        .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_notif_icon))
                        .setContentTitle("Audionce")
                        .setContentText("Playing " + title)
                        .setContentIntent(pi);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        v.vibrate(300);
        manager.notify(Utilities.NOTIFICATION_ID, builder.build());
    }
}
