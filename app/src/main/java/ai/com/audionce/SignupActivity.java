package ai.com.audionce;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
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
import com.parse.ParseQuery;
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
        return super.onOptionsItemSelected(item);
    }

    public void onAlreadyHaveAccountPress(View v){
        startActivity(new Intent(this,LoginActivity.class));
    }

    private void saveUsernamePassword(String un, String pw){
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("should_auto_login", true);
        editor.putString("saved_username", un);
        editor.putString("saved_password",pw);
        editor.apply();
    }

    public void createNewAccount(View v){
        su.setEnabled(false);
        final String pwa = pw1.getText().toString().trim();
        final String pwb = pw2.getText().toString().trim();
        final String username = un.getText().toString().trim();
        if(!pwa.equals(pwb)){
            makeToast("Passwords do not match.");
        } else {
            new AsyncTask<Void,Void,Utilities.SignupState>(){
                private String cUsername = username;
                private String cPassword = pwa;

                @Override
                public Utilities.SignupState doInBackground(Void... v){
                    try{
                        ParseQuery<ParseObject> doesUsernameExist = ParseQuery.getQuery("User")
                                .whereEqualTo("username",cUsername);
                        if(!doesUsernameExist.find().isEmpty())
                            return Utilities.SignupState.USERNAME_ALREADY_EXISTS;
                        final ParseUser user = new ParseUser();
                        user.setUsername(cUsername);
                        user.setPassword(cPassword);
                        user.put("sounds", new ArrayList<ParseObject>());
                        Bitmap picture = BitmapFactory.decodeResource(getResources(),R.drawable.def_profile);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        picture.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        final ParseFile file = new ParseFile("file.png",baos.toByteArray());
                        file.save();
                        user.put("profile_picture", file);
                        user.signUp();
                        ParseObject ft = new ParseObject("FriendTable");
                        ft.put("user",user);
                        ft.put("all_friends", new ArrayList<ParseUser>());
                        ft.save();
                        user.put("friends", ft);
                        user.save();
                    } catch (Exception ex){
                        Utilities.makeLogFromThrowable(ex);
                        return Utilities.SignupState.ERROR_THROWN;
                    }
                    return Utilities.SignupState.ALL_OKAY;
                }

                @Override
                public void onPostExecute(Utilities.SignupState state){
                    switch(state){
                        case USERNAME_ALREADY_EXISTS:
                            makeToast("Username \"" + username +"\" is already taken.");
                            break;
                        case ALL_OKAY:
                            saveUsernamePassword(username,pwa);
                            Intent in = new Intent(getApplicationContext(), HubActivity.class);
                            in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            getApplicationContext().startActivity(in);
                            makeToast("User signed up!");
                            break;
                    }
                }
            }.execute();
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    private void makeToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();
    }
}
