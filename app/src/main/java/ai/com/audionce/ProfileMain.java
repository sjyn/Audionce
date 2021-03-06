package ai.com.audionce;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Sound;
import com.newline.sjyn.audionce.Utilities;
import com.nononsenseapps.filepicker.FilePickerActivity;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pkmmte.view.CircularImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//TODO -- delete sound from list
public class ProfileMain extends AppCompatActivity {
    private ParseUser currentUser;
    private ListView sounds;
    private CircularImageView profilePic;
    private TextView username;
    private TextView friends;
    private TextView noSounds;
    private final int CAMERA_CODE = 1650;
    private final int GALLERY_CODE = 1660;
    private final int CROP_CODE = 1770;
    private final String SAVE_PATH = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES) + "/picture.png";
    private BitmapFactory.Options opts;
    private Adapters.ProfileSoundsAdapter adapter;
    private File f;


    @Override
    @SuppressWarnings({"unchecked", "ResultOfMethodCallIgnored"})
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
        profilePic = (CircularImageView) findViewById(R.id.profile_picture_main);
        username = (TextView)findViewById(R.id.profile_username_main);
        friends = (TextView)findViewById(R.id.friends_text_main);
        TextView addSound = (TextView) findViewById(R.id.profile_add_text_main);
        noSounds = (TextView)findViewById(R.id.no_sounds_text);
        noSounds.setVisibility(View.GONE);
        currentUser = ParseUser.getCurrentUser();
        opts = new BitmapFactory.Options();
        opts.inSampleSize = Utilities.calculateInSampleSize
                (opts, profilePic.getMaxWidth(), profilePic.getMaxHeight());
        opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
        username.setText(currentUser.getUsername());
        final ParseFile picParse = (ParseFile) currentUser.get("profile_picture");
        picParse.getDataInBackground(new GetDataCallback() {
            @Override
            public void done(byte[] bytes, ParseException e) {
                if (e == null) {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, opts);
                    profilePic.setImageBitmap(bmp);
                }
            }
        });
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        friends.setText("Friends (" + sp.getInt("num_friends", 0) + ")");
        profilePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAndDisplayChooser();
            }
        });
        friends.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(), FriendsActivity.class));
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
                    sounds.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            Sound s = (Sound) sounds.getAdapter().getItem(position);
                            showDeleteSoundDialog(s, adapter);
                            return true;
                        }
                    });
                } else {
                    noSounds.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    private void showDeleteSoundDialog(final Sound s, final Adapters.ProfileSoundsAdapter ada) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Delete This Sound")
                .setMessage("Are you sure you want to delete " + s.getTitle() + "?\n" +
                        "This action cannot be undone!")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new AsyncTask<Void, Void, Boolean>() {
                            @Override
                            public Boolean doInBackground(Void... v) {
                                try {
                                    s.getParseObject().fetchIfNeeded().delete();
                                } catch (Exception ex) {
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onPostExecute(Boolean res) {
                                if (res) {
                                    Utilities.makeToast(getApplicationContext(), "Sound Removed");
                                    ada.remove(s);
                                    ada.notifyDataSetChanged();
                                } else {
                                    Utilities.makeToast(getApplicationContext(), "Error Removing Sound");
                                }
                            }
                        }.execute();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        builder.create().show();
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

    @SuppressLint("InflateParams")
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
                        if (newName.length() < 6) {
                            Utilities.makeToast(getApplicationContext(),
                                    "Username must be 6 or more characters");
                        } else if (!newName.equals("")) {
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
        Dialog editor = builder.create();
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
                                final String galleryPath = Environment.getExternalStorageDirectory()
                                        + "/" + Environment.DIRECTORY_DCIM + "/";
                                Intent i = new Intent(getApplicationContext(),
                                        FilePickerActivity.class);
                                i.putExtra(FilePickerActivity.EXTRA_START_PATH,
                                        galleryPath);
                                startActivityForResult(i, GALLERY_CODE);
                                break;
                        }
                    }
                });
        Dialog chooser = builder.create();
        chooser.show();
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
            case R.id.new_sound_from_hub:
                startActivity(new Intent(this,NewSoundActivity.class));
                break;
            case R.id.goto_friends:
                startActivity(new Intent(this, FriendsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        profilePic.setEnabled(false);
        switch (requestCode){
            case CAMERA_CODE:
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
                    Intent intent = new Intent("com.android.camera.action.CROP");
                    intent.setDataAndType(data.getData(), "image/*");
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
                    profilePic.invalidate();
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
                                return false;
                            }
                            return true;
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
                createAndDisplayChooser();
            }
        });
    }
}
