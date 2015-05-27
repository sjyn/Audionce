package ai.com.audionce;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;


public class SignupActivity extends AppCompatActivity {
    private EditText un,pw1,pw2;
    private Button su;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        un = (EditText)findViewById(R.id.username_field);
        pw1 = (EditText)findViewById(R.id.pw1_field);
        pw2 = (EditText)findViewById(R.id.pw2_field);
        su = (Button)findViewById(R.id.new_account);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_signup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onAlreadyHaveAccountPress(View v){
        startActivity(new Intent(this,LoginActivity.class));
    }

    public void createNewAccount(View v){
        su.setEnabled(false);
        final String pwa = pw1.getText().toString();
        final String pwb = pw2.getText().toString();
        final String username = un.getText().toString();
        if(pwa.equals(pwb) && !pwa.equals("") && !pwb.equals("") && !username.equals("")) {
            final ParseUser user = new ParseUser();
            user.setPassword(pwa);
            user.setUsername(username);
            user.put("sounds", new ArrayList<ParseObject>());
            Bitmap picture = BitmapFactory.decodeResource(getResources(),R.drawable.def_profile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.PNG, 100, baos);
            final ParseFile file = new ParseFile("file",baos.toByteArray());
            file.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        user.put("profile_picture", file);
                        //TODO -- FIX THREAD
                        user.signUpInBackground(new SignUpCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    ParseObject ft = new ParseObject("FriendTable");
                                    ParseObject snds = new ParseObject("Sounds");
                                    ft.put("user",user);
                                    ft.put("all_friends", new ArrayList<ParseUser>());
                                    try {
                                        ft.save();
                                    } catch (Exception ex){
                                        Log.e("AUD",Log.getStackTraceString(ex));
                                    }
                                    user.put("friends", ft);
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                Intent in = new Intent(getApplicationContext(), HubActivity.class);
                                                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                                getApplicationContext().startActivity(in);
                                            }
                                            else {
                                                Log.e("AUD",Log.getStackTraceString(e));
                                            }
                                        }
                                    });
                                } else {
                                    showToast(e.getMessage());
                                    Log.e("AUD",Log.getStackTraceString(e));
                                }
                                su.setEnabled(true);
                            }
                        });

                    }
                }
            });
        } else {
            showToast("Passwords do not match");
            su.setEnabled(true);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
}
