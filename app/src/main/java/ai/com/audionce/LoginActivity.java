package ai.com.audionce;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
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

import com.parse.LogInCallback;
import com.parse.Parse;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_login, menu);
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

    @Override
    public void onPause(){
        super.onPause();
        finish();
    }

    public void hasNoAccountPress(View v){
        startActivity(new Intent(this,SignupActivity.class));
    }

    public void login(View v){
        li.setEnabled(false);
        ParseUser.logInInBackground(un.getText().toString(), pw.getText().toString(), new LogInCallback() {
            @Override
            public void done(ParseUser parseUser, ParseException e) {
                if (parseUser != null) {

                    Toast.makeText(getApplicationContext(), "Logged in!", Toast.LENGTH_SHORT).show();
                    Intent in = new Intent(getApplicationContext(), HubActivity.class);
                    in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    getApplicationContext().startActivity(in);
                } else {
                    Log.e("LOGIN ERROR", e.getMessage());
                    li.setEnabled(true);
                }
            }
        });

    }
}
