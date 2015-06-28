package ai.com.audionce;

import android.content.Intent;
import android.content.SharedPreferences;
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
import com.parse.LogInCallback;
import com.parse.ParseException;
import com.parse.ParseUser;


public class LoginActivity extends AppCompatActivity {
    private EditText pw,un;
    private Button li;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        un = (EditText)findViewById(R.id.login_username);
        pw = (EditText)findViewById(R.id.login_password);
        li = (Button)findViewById(R.id.button);
    }

    private void loginUser(final String un, final String pw) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        ParseUser.logInInBackground(un, pw, new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (e == null) {
                    Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_SHORT).show();
                    SharedPreferences.Editor editor = sp.edit();
                    editor.putString("saved_username", un);
                    editor.putString("saved_password", pw);
                    editor.apply();
                    Intent in = new Intent(getApplicationContext(), HubActivity.class);
                    in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(in);
                } else {
                    Utilities.makeToast(getApplicationContext(), "Failed to Login.");
                    li.setEnabled(true);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    public void hasNoAccountPress(View v){
        startActivity(new Intent(this,SignupActivity.class));
    }

    public void login(View v){
        if (Utilities.doesHaveNetworkConnection(this)) {
            li.setEnabled(false);
            loginUser(un.getText().toString(), pw.getText().toString());
        } else {
            Utilities.makeToast(this, "No Network Connection.");
        }
    }
}
