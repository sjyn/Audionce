package com.newline.sjyn.audionce;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseUser;

public class Friend implements Comparable<Friend> {
    private String username;
    private Bitmap img;
    private ParseUser user;
    private String type;


    public Friend(String un, Bitmap img){
        username = un;
        this.img = img;
    }

    private Friend(String un, Bitmap img, ParseUser usr){
        username = un;
        this.img = img;
        user = usr;
    }

    public void setType(String s){
        type = s;
    }

    public String getType(){
        return type;
    }

    public void setParseUser(ParseUser user){
        this.user = user;
    }

    public ParseUser getParseUser(){
        return user;
    }

    public String getUsername(){
        return username;
    }

    public void setImage(Bitmap bitmap){
        img = bitmap;
    }

    public Bitmap getImage(){
        return img;
    }

    public Bitmap getScaledBitmap(int w, int h){
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Utilities.calculateInSampleSize(opts, w, h);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return Bitmap.createBitmap(img,0,0,w,h);
    }

    public static Friend parseFriend(final ParseUser user){
        final String un = user.getUsername();
        Bitmap toImage = null;
        try {
            byte[] array = user.getParseFile("profile_picture").getData();
            toImage = BitmapFactory.decodeByteArray(array, 0, array.length);
        } catch (Exception ex){
            Utilities.makeLogFromThrowable(ex);
            return null;
        }
        return new Friend(un,toImage,user);
//        user.getParseFile("profile_picture").getDataInBackground(new GetDataCallback() {
//            @Override
//            public void done(byte[] bytes, ParseException e) {
//                if(e == null){
//                    toImage = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
//                }
//            }
//        });
    }

    @Override
    public int compareTo(Friend f){
        return this.username.compareTo(f.username);
    }

    @Override
    public boolean equals(Object o){
        return (o instanceof Friend) && ((Friend)o).username.equals(this.username);
    }
}
