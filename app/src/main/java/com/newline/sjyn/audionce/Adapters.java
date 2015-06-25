package com.newline.sjyn.audionce;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.TextView;

import com.pkmmte.view.CircularImageView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ai.com.audionce.NewSoundActivity;
import ai.com.audionce.R;

public final class Adapters {

    public static class ShareWithFriendsAdapter extends  ArrayAdapter<Friend> {
        private List<Friend> list;
        private Map<Friend,Boolean> isFriendSelected;

        private static class Holder{
            TextView name;
            CheckBox shareWith;
            CircularImageView image;
        }

        public ShareWithFriendsAdapter(Context c, List<Friend> list){
            super(c, R.layout.share_list_item, list);
            this.list = list;
            isFriendSelected = new LinkedHashMap<>();
            for(Friend f : list){
                isFriendSelected.put(f,false);
            }
        }

        @Override
        public View getView(final int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.share_list_item,parent,false);
                Holder h = new Holder();
                h.name = (TextView)cv.findViewById(R.id.name);
                h.shareWith = (CheckBox)cv.findViewById(R.id.check_box);
                h.image = (CircularImageView) cv.findViewById(R.id.picture);
                cv.setTag(h);
            }
            final Holder holder = (Holder)cv.getTag();
            holder.shareWith.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        isFriendSelected.put(list.get(pos),true);
                        NewSoundActivity.notifyTextView(false);
                        Log.e("AUD","Checked state");
                    } else {
                        isFriendSelected.put(list.get(pos), false);
                        if(getSelectedFriends().isEmpty())
                            NewSoundActivity.notifyTextView(true);
                        else
                            NewSoundActivity.notifyTextView(false);
                        Log.e("AUD", "Unchecked state");
                    }
                }
            });
            Friend f = list.get(pos);
            holder.image.setImageBitmap(f.getImage());
            holder.name.setText(f.getUsername());
            if(isFriendChecked(f))
                holder.shareWith.setChecked(true);
            else
                holder.shareWith.setChecked(false);
            return cv;
        }

        public boolean isFriendChecked(Friend f){
            return isFriendSelected.get(f);
        }

        public void checkFriend(Friend f){
            isFriendSelected.put(f,true);
        }

        public void uncheckFriend(Friend f){
            isFriendSelected.put(f,false);
        }

        public List<Friend> getSelectedFriends(){
            List<Friend> ret = new ArrayList<>();
            for(Friend f : isFriendSelected.keySet()){
                if(isFriendSelected.get(f))
                    ret.add(f);
            }
            return ret;
        }
    }

    public static class FriendAdapter extends ArrayAdapter<Friend> {
        private List<Friend> list;

        private static class Holder{
            TextView name;
            TextView status;
            CircularImageView image;
        }

        public FriendAdapter(Context c, List<Friend> list){
            super(c, R.layout.friend_list_item, list);
            this.list = list;
        }

        @Override
        public View getView(int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.friend_list_item,parent,false);
                Holder h = new Holder();
                h.name = (TextView)cv.findViewById(R.id.friend_name);
                h.image = (CircularImageView) cv.findViewById(R.id.friend_picture);
                h.status = (TextView)cv.findViewById(R.id.remove_friend);
                cv.setTag(h);
            }
            final Holder holder = (Holder)cv.getTag();
            Friend f = list.get(pos);
            holder.image.setImageBitmap(f.getImage());
            holder.status.setText("(" + f.getType() + ")");
            holder.name.setText(f.getUsername());
            return cv;
        }
    }

    public static class FriendSearchAdapter extends ArrayAdapter<Friend> {
        private List<Friend> searches;

        private static class Holder{
            TextView tv;
            CircularImageView iv;
        }

        public FriendSearchAdapter(Context c, List<Friend> res){
            super(c,R.layout.friend_search_item,res);
            this.searches = res;
        }

        @Override
        public View getView(int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.friend_search_item,parent,false);
                Holder h = new Holder();
                h.tv = (TextView)cv.findViewById(R.id.friend_search_name);
                h.iv = (CircularImageView) cv.findViewById(R.id.friend_search_picture);
                cv.setTag(h);
            }
            final Holder holder = (Holder)cv.getTag();
            Friend f = searches.get(pos);
            holder.tv.setText(f.getUsername());
            holder.iv.setImageBitmap(f.getImage());
            return cv;
        }
    }

    public static class ProfileSoundsAdapter extends ArrayAdapter<Sound> {
        private List<Sound> sounds;
        private boolean isSoundPlaying;
        private MediaPlayer player;

        public ProfileSoundsAdapter(Context c, List<Sound> snds){
            super(c,R.layout.sounds_list_item,snds);
            this.sounds = snds;
            isSoundPlaying = false;
            player = new MediaPlayer();
        }

        private static class Holder{
            TextView tv;
            ImageButton button;
            TextView pop;
        }

        @Override
        public View getView(int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.sounds_list_item,parent,false);
                Holder h = new Holder();
                h.tv = (TextView)cv.findViewById(R.id.sound_title);
                h.button = (ImageButton)cv.findViewById(R.id.play_sound_button);
                h.pop = (TextView) cv.findViewById(R.id.pub_or_priv);
                cv.setTag(h);
            }
            Log.e("AUD", "Sound getView called");
            Holder holder = (Holder)cv.getTag();
            holder.tv.setText(sounds.get(pos).getTitle());
            holder.pop.setText("");
            final int tPos = pos;
//            holder.button.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    displayDeleteSoundDialog(sounds.get(tPos));
//                    return false;
//                }
//            });
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.e("AUD","sound button pressed");
                    if (!isSoundPlaying) {
                        player.setAudioStreamType(AudioManager.STREAM_MUSIC);
                        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                            @Override
                            public void onPrepared(MediaPlayer mp) {
                                mp.start();
                                isSoundPlaying = true;
                            }
                        });
                        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                            @Override
                            public void onCompletion(MediaPlayer mp) {
                                player.release();
                                player = new MediaPlayer();
                                isSoundPlaying = false;
                            }
                        });
                        try {
                            player.setDataSource(sounds.get(tPos).getUrl());
                            player.prepareAsync();
                        } catch (IOException ioe) {
                            Log.e("AUD",Log.getStackTraceString(ioe));
                        }
                    }
                }
            });
            return cv;
        }

        private void displayDeleteSoundDialog(final Sound sound) {
            AlertDialog.Builder builder = new AlertDialog.Builder(super.getContext())
                    .setMessage("Delete this sound?")
                    .setTitle("Delete " + sound.getTitle())
                    .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new AsyncTask<Void, Void, Boolean>() {
                                @Override
                                public Boolean doInBackground(Void... v) {
                                    try {
                                        sound.getParseObject().fetchIfNeeded().delete();
                                    } catch (Exception ex) {
                                        Utilities.makeLogFromThrowable(ex);
                                        return false;
                                    }
                                    return true;
                                }
                            }.execute();
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.create().show();
        }
    }
}
