package ai.com.audionce;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class ProfileMain extends AppCompatActivity {
    private ParseUser currentUser;
    private ListView sounds;
    private ImageView profilePic;
    private TextView username,friends,addSound, noSounds;
    private final int CAMERA_CODE = 1650;
    private final int GALLERY_CODE = 1660;
    private final String SAVE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES) + "/picture.png";
    private BitmapFactory.Options opts;
    private static Dialog chooser,editor;
    private Adapters.ProfileSoundsAdapter adapter;


    @Override
    @SuppressWarnings({"ignored", "unchecked"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_main);
        File f = new File(SAVE_PATH);
        if(!f.exists()){
            try {
                f.createNewFile();
            } catch (IOException ioe){
                Toast.makeText(this,"Failed to create file",Toast.LENGTH_SHORT).show();
            }
        }
        sounds = (ListView)findViewById(R.id.sounds_list_view);
        profilePic = (ImageView)findViewById(R.id.profile_picture_main);
        username = (TextView)findViewById(R.id.profile_username_main);
        friends = (TextView)findViewById(R.id.friends_text_main);
        addSound = (TextView)findViewById(R.id.profile_add_text_main);
        noSounds = (TextView)findViewById(R.id.no_sounds_text);
        noSounds.setVisibility(View.GONE);
        currentUser = ParseUser.getCurrentUser();
        opts = new BitmapFactory.Options();
        opts.inSampleSize = Utilities.calculateInSampleSize
                (opts,profilePic.getMaxWidth(),profilePic.getMaxHeight());
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if(currentUser == null){
            Log.e("PROFILE","User null");
        }
        username.setText(currentUser.getUsername());
        final ParseFile picParse = (ParseFile)currentUser.get("profile_picture");
        picParse.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] bytes, ParseException e) {
                if (e == null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                    profilePic.setBackground(new BitmapDrawable(getResources(), bmp));
                }
            }
        });
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        friends.setText("Friends (" + sp.getInt("num_friends", 0) + ")");
//        ParseQuery<ParseObject> fQue = ParseQuery.getQuery("FriendTable");
//        fQue.whereEqualTo("user", currentUser);
//        fQue.findInBackground(new FindCallback<ParseObject>() {
//            @Override
//            public void done(List<ParseObject> list, ParseException e) {
//                if (e == null) {
//                    ParseObject lst = list.get(0);
//                    List<ParseUser> fnds = (List<ParseUser>) lst.get("all_friends");
//                    friends.setText("Friends (" + fnds.size() + ")");
//                } else {
//                    Log.e("AUD", Log.getStackTraceString(e));
//                }
//            }
//        });
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAndDisplayChooser();
            }
        });
        friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), ViewFriendsActivityThree.class));
            }
        });
        addSound.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), NewSoundActivity.class));
            }
        });
        username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAndDisplayUsernameEditor();
            }
        });
        loadSounds();
    }

    private void loadSounds(){
        new AsyncTask<Void,Void,Boolean>(){
            private ParseUser tUser;
            private List<Sound> fSounds;

            @Override
            public void onPreExecute(){
                tUser = currentUser;
                fSounds = new ArrayList<>();
            }

            @Override
            @SuppressWarnings("unchecked")
            public Boolean doInBackground(Void... c){
                List<ParseObject> uSounds = (List<ParseObject>)tUser.get("sounds");
                if (uSounds.isEmpty())
                    return false;
                for(ParseObject po : uSounds){
                    try {
                        fSounds.add(Sound.parseSound(po.fetchIfNeeded()));
                    } catch (Exception ex){
                        Log.e("AUD",Log.getStackTraceString(ex));
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean res){
                if(res){
                    adapter = new Adapters.ProfileSoundsAdapter(getApplicationContext(),fSounds);
                    sounds.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    noSounds.setVisibility(View.GONE);
                } else {
                    noSounds.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onResume(){
        super.onResume();
        ParseQuery<ParseObject> fQue = ParseQuery.getQuery("FriendTable");
        fQue.whereEqualTo("user",currentUser);
        fQue.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> list, ParseException e) {
                if (e == null) {
                    ParseObject lst = list.get(0);
                    List<ParseUser> fnds = (List<ParseUser>) lst.get("all_friends");
                    friends.setText("Friends (" + fnds.size() + ")");
                } else {
                    Log.e("AUD", Log.getStackTraceString(e));
                }
            }
        });
        loadSounds();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_profile_main, menu);
        return true;
    }

    private void createAndDisplayUsernameEditor(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View v = inflater.inflate(R.layout.change_name_dialog,null);
        final EditText et = (EditText)v.findViewById(R.id.new_name_field);
        builder.setView(v).
                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).
                setPositiveButton("Change", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if(et == null){
                            makeToast("NULL");
                            return;
                        }
                        final String newName = et.getText().toString();
                        if(!newName.equals("")){
                            currentUser.setUsername(newName);
                            currentUser.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if(e == null){
                                        username.setText(currentUser.getUsername());
                                        PreferenceManager.getDefaultSharedPreferences(getApplicationContext())
                                                .edit().putString("saved_username", newName).apply();
                                        makeToast("Username Updated.");
                                    }
                                }
                            });
                        }
                    }
                });
        editor = builder.create();
        editor.show();
    }

    private void makeToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

    private void createAndDisplayChooser(){
        AlertDialog.Builder builder = new AlertDialog.Builder(ProfileMain.this);
        builder.setTitle("Complete Action Using...").
                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).
                setItems(new String[]{"Camera", "Gallery"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                //TODO -- create new camera activity
//                                Intent in = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//                                in.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(SAVE_PATH)));
//                                startActivityForResult(in, CAMERA_CODE);
                                break;
                            case 1:
                                Intent intent = new Intent();
                                intent.setType("image/*");
                                intent.setAction(Intent.ACTION_GET_CONTENT);
                                startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_CODE);
                                break;
                        }
                    }
                });
        chooser = builder.create();
        chooser.show();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(chooser != null) {
            if(chooser.isShowing())
                chooser.dismiss();
            chooser = null;
        }
        if(editor != null){
            if(editor.isShowing())
                editor.dismiss();
            editor = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Utilities.stopSoundPickupService(this);
                Intent in = new Intent(this,LoginActivity.class);
                in.putExtra("should_auto_login_from_intent", "no");
                startActivity(in);
                break;
            case R.id.goto_map:
                startActivity(new Intent(this,HubActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        Log.i("AUD","In act res");
        Log.i("AUD","Request Code: " + requestCode);
        profilePic.setEnabled(false);
        switch (requestCode){
            case CAMERA_CODE:
                Log.i("AUD","Camera code");
                if(resultCode == RESULT_OK){
                    Log.i("AUD","Camera Okay");
                    new AsyncTask<Boolean,Void,Boolean>(){
                        private Bitmap bmp;
                        private BitmapFactory.Options options;
                        private int sw,sh;
                        private ParseFile newPic;

                        @Override
                        public void onPreExecute(){
                            profilePic.setImageBitmap(null);
                            profilePic.setImageDrawable(null);
                            options = opts;
                            sw = profilePic.getWidth();
                            sh = profilePic.getHeight();
                            Log.i("AUD","Pre exec finished");
                        }

                        @Override
                        public Boolean doInBackground(Boolean... b){
                            Matrix mat = new Matrix();
                            mat.postRotate(270);
                            bmp = BitmapFactory.decodeFile(SAVE_PATH,options);
                            bmp = Bitmap.createBitmap(bmp,0,0,sw,sh,mat,true);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmp.compress(Bitmap.CompressFormat.PNG,100,baos);
                            newPic = new ParseFile("file",baos.toByteArray());
                            try {
                                newPic.save();
                                return true;
                            } catch (ParseException pe){
                                return false;
                            }
                        }

                        @Override
                        public void onPostExecute(Boolean b){
                            Log.i("AUD","post exec started");
                            if(b){
                                profilePic.setImageBitmap(bmp);
                                currentUser.put("profile_picture", newPic);
                                currentUser.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null){
//                                            Toast.makeText(getApplicationContext(),
//                                                    "Saved Picture!", Toast.LENGTH_SHORT).show();
                                            Log.e("AUD","pic saved from camera");
                                        } else {
//                                            Toast.makeText(getApplicationContext(),
//                                                    "Failed to save Picture",Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            } else {
//                                Toast.makeText(getApplicationContext(),
//                                        "Failed to save Picture",Toast.LENGTH_SHORT).show();
                            }
                        }
                    }.execute();
                }
                break;
            case GALLERY_CODE:
                if(resultCode == RESULT_OK) {
                    new AsyncTask<Object,Void,Boolean>(){
                        private Intent data;
                        private Context ths;
                        private BitmapFactory.Options options;
                        private int sw,sh;
                        private Bitmap map;
                        private ParseFile pf;

                        @Override
                        public void onPreExecute(){
                            profilePic.setImageBitmap(null);
                            profilePic.setImageDrawable(null);
                            profilePic.setBackground(null);
                            options = opts;
                            sw = profilePic.getWidth();
                            sh = profilePic.getHeight();
                            Log.i("AUD","Galllery h: " + sh + " w: " + sw);
                        }

                        @Override
                        public Boolean doInBackground(Object... o){
                            try{
                                data = (Intent)o[1];
                                ths = (Context)o[0];
                                Uri selectedImage = data.getData();
                                InputStream is = ths.getContentResolver().openInputStream(selectedImage);
                                map = BitmapFactory.decodeStream(is, null, options);
                                is.close();
                                map = Bitmap.createScaledBitmap(map,sw,sh,false);
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                map.compress(Bitmap.CompressFormat.PNG,100,baos);
                                pf = new ParseFile("file",baos.toByteArray());
                                pf.save();
                                return true;
                            } catch (Exception e){
                                return false;
                            }
                        }

                        @Override
                        public void onPostExecute(Boolean b){
                            if(b){
                                profilePic.setImageBitmap(map);
                                currentUser.put("profile_picture", pf);
                                currentUser.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if(e == null){
                                            Log.i("AUD","Saved pic from gallery");
                                        }
                                        profilePic.setEnabled(true);
                                    }
                                });
                            }
                        }
                    }.execute(this,data);
                }
                break;
            default:
                break;
        }
    }
}
