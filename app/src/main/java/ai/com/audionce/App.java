package ai.com.audionce;

import android.app.Application;

import com.parse.Parse;

/**
 * Created by SJYN on 5/19/15.
 */
public class App extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        Parse.enableLocalDatastore(this);
        Parse.initialize(this, "BZREskeQOkzmd6UyWpTZxHT5GLTalaZBBJgrZweq",
                "4NLe8vk02kO2kR9EcLuoZL6qxaiogoQ227YJcV9w");
    }
}
