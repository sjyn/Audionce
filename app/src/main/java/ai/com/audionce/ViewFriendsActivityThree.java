package ai.com.audionce;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.ParseObject;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;


public class ViewFriendsActivityThree extends AppCompatActivity {
    private ListView fList;
    private List<Friend> friends;
    private Adapters.FriendAdapter adapter;
    private TextView noFriends;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_friends_activity_three);
        fList = (ListView)findViewById(R.id.friends_list);
        friends = new ArrayList<>();
        noFriends = (TextView)findViewById(R.id.no_friends_view);
        noFriends.setVisibility(View.GONE);
        noFriends.setText("You haven't added any friends yet.\nYou can add" +
            " friends by searching above.");
        fList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                displayRemoveFriendDialog(friends.get(position));
                return true;
            }
        });
//        if(savedInstanceState == null) {
//            Log.e("AUD","populating friends from onCreate");
//            populateFriends();
//        }
    }

    @Override
    public void onPause(){
        super.onPause();
    }

    @Override
    public void onResume(){
        super.onResume();
        Log.e("AUD", "populating friends from onResume");
        populateFriends();
    }

    @SuppressWarnings("unchecked")
    private void populateFriends(){
        Log.e("AUD", "populateFriends called");
        final ParseUser curr = ParseUser.getCurrentUser();
        adapter = new Adapters.FriendAdapter(this,friends);
        fList.setAdapter(adapter);
        new AsyncTask<Void,Void,Boolean>(){
            private ParseUser tu = curr;
            private List<Friend> fCpy = new ArrayList<>();

            @Override
            public Boolean doInBackground(Void... v){
                ParseObject ft = (ParseObject)tu.get("friends");
                List<ParseUser> frnds = (List<ParseUser>)ft.get("all_friends");
                if(frnds.isEmpty())
                    return false;
                for(ParseUser pu : frnds){
                    try {
                        Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                        f.setType("friends");
                        fCpy.add(f);
                    } catch (Exception ex){
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean res){
                if(res){
                    friends.clear();
                    friends.addAll(fCpy);
                    if(friends.isEmpty()){
                        noFriends.setVisibility(View.VISIBLE);
                    } else {
                        noFriends.setVisibility(View.GONE);
                    }
                    Log.e("AUD","Friends list size: " + friends.size());
                    adapter.notifyDataSetChanged();
                }
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_friends_activity_three, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Intent in = new Intent(this,LoginActivity.class);
                Utilities.stopSoundPickupService(this);
                in.putExtra("should_auto_login_from_intent","no");
                startActivity(in);
                break;
            case R.id.action_goto_pending:
                startActivity(new Intent(this,ViewPendingFriendsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
//                               DIALOGS
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    private static AlertDialog removeFriendDialog;

    private void displayRemoveFriendDialog(final Friend f){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Friend")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new RemoveFriendTask().execute(f);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setMessage("Are you sure you want to remove this friend from your list?");
        removeFriendDialog = builder.create();
        removeFriendDialog.show();
    }

    class RemoveFriendTask extends AsyncTask<Object,Void,Boolean> {
        private ParseUser tUser = ParseUser.getCurrentUser();
        private Friend fri;

        @Override
        public Boolean doInBackground(Object... o){
            try {
                fri = (Friend)o[0];
                ParseUser removeMe = fri.getParseUser().fetchIfNeeded();
                removeMyFriend(removeMe);
                removeTheirFriend(removeMe);
                tUser.save();
                removeMe.save();
            } catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean res){
            if(res) {
                makeToast("Friend removed");
                friends.remove(fri);
                if(friends.isEmpty())
                    noFriends.setVisibility(View.VISIBLE);
                adapter.notifyDataSetChanged();
            }
        }

        private void removeMyFriend(ParseUser removeMe) throws Exception {
            ParseObject ft = ((ParseObject)tUser.get("friends")).fetchIfNeeded();
            List<ParseUser> tFriends = (List<ParseUser>)ft.get("all_friends");
            tFriends.remove(removeMe);
            ft.save();
        }

        private void removeTheirFriend(ParseUser removeFrom) throws Exception{
            ParseObject ft = ((ParseObject)removeFrom.get("friends")).fetchIfNeeded();
            List<ParseUser> tFriends = (List<ParseUser>)ft.get("all_friends");
            tFriends.remove(tUser);
            ft.save();
        }
    }
}
