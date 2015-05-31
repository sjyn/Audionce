package ai.com.audionce;

import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.support.v7.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ViewFriendsActivityThree extends AppCompatActivity {
    private ListView fList;
    private List<Friend> friends;
    private Adapters.FriendAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_friends_activity_three);
        populateFriends();
    }

    @Override
    public void onPause(){
        super.onPause();

    }

    @SuppressWarnings("unchecked")
    private void populateFriends(){
        final ParseUser curr = ParseUser.getCurrentUser();
        fList = (ListView)findViewById(R.id.friends_list);
        fList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                displayRemoveFriendDialog(friends.get(position));
                return true;
            }
        });
        friends = new ArrayList<>();
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
                    friends.addAll(fCpy);
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
                break;
            case R.id.log_out:
                ParseUser.logOut();
                startActivity(new Intent(this, LoginActivity.class));
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
