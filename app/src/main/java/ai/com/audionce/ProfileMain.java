package ai.com.audionce;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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

import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
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
import java.util.ArrayList;
import java.util.List;


public class ProfileMain extends AppCompatActivity {
    private ParseUser currentUser;
    private ListView sounds;
    private ImageView profilePic;
    private TextView username,friends,addSound, noSounds;
    private final int CAMERA_CODE = 1650;
    private final int GALLERY_CODE = 1660;
    private final int CROP_CODE = 1770;
    private final String SAVE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES) + "/picture.png";
    private BitmapFactory.Options opts;
    private static Dialog chooser,editor;
    private Adapters.ProfileSoundsAdapter adapter;
    private File f;


    @Override
    @SuppressWarnings({"ignored", "unchecked"})
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_main);
        f = new File(SAVE_PATH);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
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
                (opts, profilePic.getMaxWidth(), profilePic.getMaxHeight());
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if(currentUser == null){
            Log.e("PROFILE","User null");
        }
        username.setText(currentUser.getUsername());
        final ParseFile picParse = (ParseFile) currentUser.get("profile_picture");
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
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("AUD", "Pressed profile pic");
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
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("AUD", "profile picture pressed");
                createAndDisplayChooser();
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
                                startActivityForResult(new Intent(getApplicationContext(),
                                        CameraActivity.class), CAMERA_CODE);
                                break;
                            case 1:
                                if (Build.VERSION.SDK_INT == 19) {
                                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                                    intent.setType("image/*");
                                    startActivityForResult(intent, GALLERY_CODE);
                                } else {
                                    Intent intent = new Intent();
                                    intent.setType("image/*");
                                    intent.setAction(Intent.ACTION_GET_CONTENT);
//                                intent.putExtra(MediaStore.EXTRA_OUTPUT,Uri.fromFile(f));
                                    startActivityForResult(Intent.createChooser(intent,
                                            "Select Picture"), GALLERY_CODE);
                                }
                                break;
                        }
                    }
                });
        chooser = builder.create();
        chooser.show();
    }

//    @Override
//    public void onPause(){
//        super.onPause();
//        if(chooser != null) {
//            if(chooser.isShowing())
//                chooser.dismiss();
//            chooser = null;
//        }
//        if(editor != null){
//            if(editor.isShowing())
//                editor.dismiss();
//            editor = null;
//        }
//    }

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
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("AUD","In act res");
        Log.i("AUD","Request Code: " + requestCode);
        profilePic.setEnabled(false);
        switch (requestCode){
            case CAMERA_CODE:
                Log.i("AUD", "Camera code");
                if (resultCode == RESULT_OK) {
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(Uri.fromFile(f), "image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("aspectX", 1);
                    intent.putExtra("aspectY", 1);
                    intent.putExtra("outputX", 130);
                    intent.putExtra("outputY", 130);
                    intent.putExtra("return-data", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(SAVE_PATH)));
                    startActivityForResult(intent, CROP_CODE);
                }
                break;
            case GALLERY_CODE:
                if (resultCode == RESULT_OK) {
                    Uri yuri;
                    if (Build.VERSION.SDK_INT == 19) {
                        yuri = data.getData();
//
                    } else {
                        yuri = data.getData();
//                        f = new File(yuri.getPath());
                    }
                    Log.e("AUD", "URI is null? " + (yuri == null ? "yes" : yuri.getPath()));
                    Intent intent = new Intent("com.android.camera.action.CROP");
//                    File file = new File(data.getDataString());
//                    File file = new File(getRealPathFromUri(data.getData()));
                    intent.setDataAndType(yuri, "image/*");
                    intent.putExtra("crop", "true");
                    intent.putExtra("aspectX", 1);
                    intent.putExtra("aspectY", 1);
                    intent.putExtra("outputX", 130);
                    intent.putExtra("outputY", 130);
                    intent.putExtra("return-data", true);
                    intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(f));
                    startActivityForResult(intent, CROP_CODE);
                }
                break;
            case CROP_CODE:
                if (resultCode == RESULT_OK) {
                    final Bitmap bmp = BitmapFactory.decodeFile(SAVE_PATH);
                    profilePic.setImageBitmap(bmp);
                    new AsyncTask<Void, Void, Boolean>() {
                        private ParseUser pu = currentUser;
                        private Bitmap bmpcpy = bmp;

                        public Boolean doInBackground(Void... v) {
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            bmpcpy.compress(Bitmap.CompressFormat.PNG, 100, baos);
                            ParseFile pf = new ParseFile("file.png", baos.toByteArray());
                            try {
                                pf.save();
                                pu.put("profile_picture", pf);
                                pu.save();
                            } catch (Exception ex) {
                                Utilities.makeLogFromThrowable(ex);
                                return false;
                            }
                            return true;
                        }

                        public void onPostExecute(Boolean res) {
                            if (res)
                                Log.e("AUD", "File succsessfully saved");
                        }
                    }.execute();
                }
                break;
            default:
                break;
        }
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e("AUD", "profile picture pressed");
                createAndDisplayChooser();
            }
        });
    }

//    private String getPath(Uri yuri) {
//        String[] projection = {MediaStore.Images.Media.DATA};
//        Cursor cursor = managedQuery(yuri, projection, null, null, null);
//        if (cursor != null) {
//            int column_index = cursor
//                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//            cursor.moveToFirst();
//            return cursor.getString(column_index);
//        }
//        return yuri.getPath();
//    }

//    private static int getCameraOrientationChange(Context x, Uri imageUri, String imagePath){
//        int rotate = 0;
//        try {
//            x.getContentResolver().notifyChange(imageUri, null);
//            File imageFile = new File(imagePath);
//            ExifInterface exif = new ExifInterface(
//                    imageFile.getAbsolutePath());
//            int orientation = exif.getAttributeInt(
//                    ExifInterface.TAG_ORIENTATION,
//                    ExifInterface.ORIENTATION_NORMAL);
//            switch (orientation) {
//                case ExifInterface.ORIENTATION_ROTATE_270:
//                    rotate = 270;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_180:
//                    rotate = 180;
//                    break;
//                case ExifInterface.ORIENTATION_ROTATE_90:
//                    rotate = 90;
//                    break;
//            }
//            Log.v("AUD", "Exif orientation: " + orientation);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return rotate;
//    }

//    private String getRealPathFromUri(Uri yuri){
//        String result = "";
//        Cursor cur = getContentResolver().query(yuri,null,null,null,null);
//        if(cur == null)
//            result = yuri.getPath();
//        else {
//            cur.moveToFirst();
//            int idx = cur.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
//            result = cur.getString(idx);
//            cur.close();
//        }
//        return result;
//    }
}
