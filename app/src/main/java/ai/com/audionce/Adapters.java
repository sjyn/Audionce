package ai.com.audionce;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.List;

public final class Adapters {

    public static class FriendAdapter extends ArrayAdapter<Friend> {
        private List<Friend> list;

        private static class Holder{
            TextView name;
            TextView status;
            ImageView image;
        }

        public FriendAdapter(Context c, List<Friend> list){
            super(c,R.layout.friend_list_item,list);
            this.list = list;
        }

        @Override
        public View getView(int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.friend_list_item,parent,false);
                Holder h = new Holder();
                h.name = (TextView)cv.findViewById(R.id.friend_name);
                h.image = (ImageView)cv.findViewById(R.id.friend_picture);
                h.status = (TextView)cv.findViewById(R.id.remove_friend);
                cv.setTag(h);
            }
            Holder holder = (Holder)cv.getTag();
            Friend f = list.get(pos);
            final int w = holder.image.getWidth();
            final int h = holder.image.getHeight();
            holder.status.setText("(" + f.getType() + ")");
            holder.name.setText(f.getUsername());
//            holder.image.setImageBitmap(f.getScaledBitmap(w,h));
            holder.image.setImageDrawable(getContext().getResources().getDrawable(R.drawable.def_profile));
            return cv;
        }
    }

    public static class FriendSearchAdapter extends ArrayAdapter<Friend> {
        private List<Friend> searches;

        private static class Holder{
            TextView tv;
            ImageView iv;
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
                h.iv = (ImageView)cv.findViewById(R.id.friend_search_picture);
                cv.setTag(h);
            }
            Holder holder = (Holder)cv.getTag();
            Friend f = searches.get(pos);
            holder.tv.setText(f.getUsername());
//            holder.iv.setImageBitmap(f.getScaledBitmap(holder.iv.getWidth(),holder.iv.getHeight()));
            holder.iv.setImageDrawable(getContext().getResources().getDrawable(R.drawable.def_profile));
            return cv;
        }
    }

    public static class ProfileSoundsAdapter extends ArrayAdapter<Sound> {
        private List<Sound> sounds;
        private boolean isSoundPlaying;
        private MediaPlayer player;

        public ProfileSoundsAdapter(Context c, List<Sound> snds){
            super(c,R.layout.sounds_list_item);
            this.sounds = snds;
            isSoundPlaying = false;
            player = new MediaPlayer();
        }

        private static class Holder{
            TextView tv;
            ImageButton button;
        }

        @Override
        public View getView(final int pos, View cv, ViewGroup parent){
            if(cv == null){
                LayoutInflater inflater = LayoutInflater.from(super.getContext());
                cv = inflater.inflate(R.layout.sounds_list_item,parent,false);
                Holder h = new Holder();
                h.tv = (TextView)cv.findViewById(R.id.sound_title);
                h.button = (ImageButton)cv.findViewById(R.id.play_sound_button);
                cv.setTag(h);
            }
            Holder holder = (Holder)cv.getTag();
            holder.tv.setText(sounds.get(pos).getTitle());
            holder.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
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
                            player.setDataSource(sounds.get(pos).getUrl());
                            player.prepareAsync();
                        } catch (IOException ioe) {
                            Log.e("AUD",Log.getStackTraceString(ioe));
                        }
                    }
                }
            });
            return cv;
        }
    }
}
