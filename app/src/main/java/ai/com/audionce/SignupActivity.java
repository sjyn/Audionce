package ai.com.audionce;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.newline.sjyn.audionce.Utilities;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

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
        if (Utilities.doesHaveNetworkConnection(this)) {
            su.setEnabled(false);
            final String pwa = pw1.getText().toString().trim();
            final String pwb = pw2.getText().toString().trim();
            final String username = un.getText().toString().trim();
            if (pwa.length() < 6) {
                Utilities.makeToast(this, "Password must be 6 or more characters");
                su.setEnabled(true);
            } else if (!pwa.equals(pwb)) {
                makeToast("Passwords do not match.");
                su.setEnabled(true);
            } else if (username.length() < 6) {
                Utilities.makeToast(this, "Username must be 6 or more characters");
                su.setEnabled(true);
            } else {
                new AsyncTask<Void, String, Utilities.SignupState>() {
                    private String cUsername = username;
                    private String cPassword = pwa;
                    private ProgressDialog pd;

                    @Override
                    public void onPreExecute(){
                        pd = ProgressDialog.show(SignupActivity.this,"Creating User...","");
                        pd.show();
                    }

                    @Override
                    public Utilities.SignupState doInBackground(Void... v) {
                        try {
                            ParseQuery<ParseObject> doesUsernameExist = ParseQuery.getQuery("User")
                                    .whereEqualTo("username", cUsername);
                            if (!doesUsernameExist.find().isEmpty())
                                return Utilities.SignupState.USERNAME_ALREADY_EXISTS;
                            final ParseUser user = new ParseUser();
                            publishProgress("Setting Username and Password...");
                            user.setUsername(cUsername);
                            user.setPassword(cPassword);
                            publishProgress("Creating Your List of Sounds...");
                            user.put("sounds", new ArrayList<ParseObject>());
                            publishProgress("Creating Your Default Profile Picture...");
                            Bitmap picture = BitmapFactory.decodeResource(getResources(), R.drawable.def_profile);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            picture.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            final ParseFile file = new ParseFile("file", baos.toByteArray());
                            file.save();
                            user.put("profile_picture", file);
                            user.signUp();
                            publishProgress("Finishing Up...");
                            ParseObject sharedSounds = new ParseObject("SharedSounds");
                            sharedSounds.put("sounds",new ArrayList<ParseObject>());
                            sharedSounds.put("user",user);
                            sharedSounds.save();
                            user.put("shared_sounds",sharedSounds);
                            ParseObject ft = new ParseObject("FriendTable");
                            ft.put("user", user);
                            ft.put("all_friends", new ArrayList<ParseUser>());
                            ft.save();
                            user.put("friends", ft);
                            user.save();
                        } catch (Exception ex) {
                            Utilities.makeLogFromThrowable(ex);
                            return Utilities.SignupState.ERROR_THROWN;
                        }
                        return Utilities.SignupState.ALL_OKAY;
                    }

                    @Override
                    public void onProgressUpdate(String... s){
                        pd.setMessage(s[0]);
                    }

                    @Override
                    public void onPostExecute(Utilities.SignupState state) {
                        pd.dismiss();
                        switch (state) {
                            case USERNAME_ALREADY_EXISTS:
                                makeToast("Username \"" + username + "\" is already taken.");
                                break;
                            case ALL_OKAY:
                                saveUsernamePassword(username, pwa);
                                Intent in = new Intent(getApplicationContext(), HubActivity.class);
                                in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                getApplicationContext().startActivity(in);
                                makeToast("User signed up!");
                                break;
                        }
                    }
                }.execute();
            }
        } else {
            Utilities.makeToast(this, "No Network Connection.");
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
