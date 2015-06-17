package com.newline.sjyn.audionce;

import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

import ai.com.audionce.SoundsPickupService2;

public class Utilities {

    public static final int SOUND_DURATION = 30000;
    public static final int SOUNDS_DISTANCE_AWAY_M = 50;
    public static final double SOUNDS_DISTANCE_AWAY_KM = SOUNDS_DISTANCE_AWAY_M / 1000.0;
    public static final int FLAG_FROM_SERVICE_TO_HUB = 76654;
    public static final int SOUNDS_DISTANCE_APART_METERS = 20;
    public static final double SOUNDS_DISTANCE_APART_KM = SOUNDS_DISTANCE_APART_METERS / 1000.0;
    public static final int NOTIFICATION_ID = 9;
    public static Intent sps;
    private static List<Friend> flist;

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

    @SuppressWarnings("unchecked")
    public static void loadFriends(ParseUser user){
        flist = new ArrayList<>();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("FriendTable")
                .whereEqualTo("user",user);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if( e == null ) {
                    ParseObject po = list.get(0);
                    for (ParseUser pu : (List<ParseUser>) po.get("all_friends")) {
                        try {
                            Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                            f.setType("friends");
                            flist.add(f);
                        } catch (Exception ex){
                            Utilities.makeLogFromThrowable(ex);
                            break;
                        }
                    }
                }
            }
        });
    }

    public static List<Friend> getFriends(){
        return flist;
    }

    public static void addFriend(Friend f){
        flist.add(f);
    }

    public static void removeFriend(Friend f){
        flist.remove(f);
    }

    public static void startSoundPickupService(Context context) {
        context.startService(sps == null ? sps = new Intent(context, SoundsPickupService2.class) : sps);
    }

    public static void stopSoundPickupService(Context context) {
        if(sps != null)
            context.stopService(sps);
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

    public static class InfoLoader{
        private ParseUser parseUser;
        private static InfoLoader instance = new InfoLoader();
        private List<Friend> friends, pending, requested;
        private List<Sound> mySounds, soundsSharedToMe;

        private InfoLoader(){
            friends = pending = requested = new ArrayList<>();
            mySounds = soundsSharedToMe = new ArrayList<>();
            parseUser = ParseUser.getCurrentUser();
        }

        public static InfoLoader getInfoLoaderInstance(){
            return instance;
        }

        @SuppressWarnings({"unchecked"})
        public void loadFriends() throws ParseException {
            ParseObject obj = parseUser.getParseObject("friends");
            ArrayList<ParseObject> fobjs = (ArrayList) obj.get("all_friends");
            for (ParseObject pobj : fobjs) {
                Friend f = Friend.parseFriend((ParseUser) pobj.fetchIfNeeded());
                f.setType("friends");
                if (!friends.contains(f))
                    friends.add(f);
            }
        }

        public List<Friend> getFriendsList() {
            return friends;
        }

        public List<Friend> getPendingList() {
            return pending;
        }

        public List<Friend> getRequestedList() {
            return requested;
        }

        public void loadPendingAndRequested() throws ParseException {
            List<ParseObject> resa = ParseQuery.getQuery("PendngTable")
                    .whereEqualTo("from", parseUser)
                    .find();
            for (ParseObject po : resa) {
                Friend f = Friend.parseFriend(((ParseUser) po.get("to")).fetchIfNeeded());
                f.setType("pending");
                pending.add(f);
            }
            List<ParseObject> resb = ParseQuery.getQuery("PendingTable")
                    .whereEqualTo("to", parseUser)
                    .find();
            for (ParseObject po : resb) {
                Friend f = Friend.parseFriend(((ParseUser) po.get("from")).fetchIfNeeded());
                f.setType("requested");
                requested.add(f);
            }
        }

        public void addPendingFriend(final Friend f) {
            if (!pending.contains(f))
                pending.add(f);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ParseObject newPend = new ParseObject("PendingTable");
                        newPend.put("to", f.getParseUser());
                        newPend.put("from", parseUser);
                        newPend.save();
                    } catch (ParseException pex) {
                        Utilities.makeLogFromThrowable(pex);
                    }
                }
            }).start();
        }

        public boolean appendFriend(final Friend f) {
            boolean cont;
            if (cont = !friends.contains(f))
                friends.add(f);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ParseObject pobj = parseUser.getParseObject("friends").fetchIfNeeded();
                        pobj.add("all_friends", f.getParseUser());
                    } catch (ParseException pex) {
                        Utilities.makeLogFromThrowable(pex);
                    }
                }
            }).start();
            return cont;
        }

        @SuppressWarnings({"unchecked"})
        public void loadSounds() throws ParseException {
            List<ParseObject> mSounds = (List) parseUser.get("sounds");
            for (ParseObject pobj : mSounds) {
                mySounds.add(Sound.parseSound(pobj));
            }
            ParseQuery<ParseObject> que = ParseQuery.getQuery("SharedSounds")
                    .whereEqualTo("user", parseUser);
            List<ParseObject> sToMe = que.find();
            ParseObject sToMeSoundsObj = sToMe.get(0);
            List<ParseObject> sToMeSoundsArray = (List) sToMeSoundsObj.get("sounds");
            for (ParseObject pobj : sToMeSoundsArray) {
                soundsSharedToMe.add(Sound.parseSound(pobj));
            }
        }

        public List<Sound> getMySounds() {
            return mySounds;
        }

        public List<Sound> getSoundsSharedToMe() {
            return soundsSharedToMe;
        }

        public void addSound(Sound s) {
            mySounds.add(s);
        }
    }


}
