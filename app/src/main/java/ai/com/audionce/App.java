package ai.com.audionce;

import android.app.Application;

import com.parse.Parse;
import com.parse.ParseUser;

public class App extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "BZREskeQOkzmd6UyWpTZxHT5GLTalaZBBJgrZweq",
                "4NLe8vk02kO2kR9EcLuoZL6qxaiogoQ227YJcV9w");
        ParseUser.enableRevocableSessionInBackground();
    }
}
