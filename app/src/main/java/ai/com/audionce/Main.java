package ai.com.audionce;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.parse.Parse;


public class Main extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AsyncTask<Void,Void,Boolean>(){
            private Context x;

            @Override
            public void onPreExecute(){
                x = getApplicationContext();
            }

            @Override
            public Boolean doInBackground(Void... params){
                Parse.enableLocalDatastore(x);
                Parse.initialize(x, "BZREskeQOkzmd6UyWpTZxHT5GLTalaZBBJgrZweq",
                        "4NLe8vk02kO2kR9EcLuoZL6qxaiogoQ227YJcV9w");
                return true;
            }
        }.execute();
        setContentView(R.layout.activity_main);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    public void onLoginClick(View v){
        startActivity(new Intent(this,LoginActivity.class));
    }

    public void onSignupClick(View v){
        startActivity(new Intent(this,SignupActivity.class));
    }
}
