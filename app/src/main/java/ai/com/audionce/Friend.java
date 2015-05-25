package ai.com.audionce;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.support.v7.app.AppCompatActivity;

import com.parse.ParseFile;
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

    public Bitmap getScaledBitmap(int w, int h){
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = Utilities.calculateInSampleSize(opts,w,h);
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return Bitmap.createBitmap(img,0,0,w,h);
    }

    public static Friend parseFriend(final ParseUser user){
        final String un = user.getUsername();
        //TODO -- Fix the bitmap loading :(
        try {
//            byte[] data = ((ParseFile) user.get("profile_picture")).getData();
//            BitmapFactory.Options opts = new BitmapFactory.Options();
//            opts.inSampleSize = Utilities.calculateInSampleSize(opts,75,75);
//            Bitmap map = BitmapFactory.decodeByteArray(data,0,data.length,opts);
            return new Friend(un,null);
        } catch (Exception e){
            return null;
        }
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
