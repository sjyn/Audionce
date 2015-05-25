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
    private List<Friend> friends, searched;
    private Adapters.FriendAdapter adapter;
    private Adapters.FriendSearchAdapter sAdapter;
    private ParseUser fragUser;
    private SearchView sv;
    private TextView noFriends;
    private MenuItem search;
    private List<Friend> friendsBeingAdded;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_friends_activity_three);
        fragUser = ParseUser.getCurrentUser();
        friendsBeingAdded = new ArrayList<>();
        loadActivity();
    }

    @Override
    public void onPause(){
        super.onPause();
        if(removeFriendDialog != null){
            if(removeFriendDialog.isShowing())
                removeFriendDialog.dismiss();
            removeFriendDialog = null;
        }
    }

    private void loadActivity(){
        fList = (ListView)findViewById(R.id.friends_list);
        Intent didSearch = getIntent();
        noFriends = (TextView)findViewById(R.id.no_friends_view);
        noFriends.setVisibility(View.GONE);
        if(didSearch == null || !Intent.ACTION_SEARCH.equals(didSearch.getAction())) {
            populateFriends();
        } else {
            if(Intent.ACTION_SEARCH.equals(didSearch.getAction())){
                String qu = didSearch.getStringExtra(SearchManager.QUERY);
                runQuery(qu);
            }
        }
        adjustListView();
        adjustFlings();
    }

    private void adjustFlings(){

    }

    @SuppressWarnings("unchecked")
    private void adjustListView(){
        fList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (fList.getAdapter() == sAdapter) {
                    Log.e("AUD", "pressed searched friend");
                    final ParseUser rUser = searched.get(position).getParseUser();
                    ParseQuery<ParseObject> que = ParseQuery.getQuery("PendingTable");
                    que.whereEqualTo("to", rUser);
                    que.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> list, ParseException e) {
                            if (e == null) {
                                if (list.isEmpty()) {
                                    Log.e("AUD", "Saving in pending");
                                    ParseObject request = new ParseObject("PendingTable");
                                    request.put("from", fragUser);
                                    request.put("to", rUser);
                                    request.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                makeToast("Friend Requested");
                                                populateFriends();
                                                MenuItemCompat.collapseActionView(search);
                                            } else {
                                                Log.e("AUD", Log.getStackTraceString(e));
                                            }
                                        }
                                    });
                                }
                            }
                        }
                    });
                } else if (fList.getAdapter() == adapter) {
                    final Friend f = friends.get(position);
                    final ParseUser tapped = f.getParseUser();
                    switch (f.getType()) {
                        case "pending":
                            if(!friendsBeingAdded.contains(f)) {
                                friendsBeingAdded.add(f);
                                new AddFriendTask().execute(tapped, f);
                            }
                            break;
                        case "friends":
                            break;
                        case "requested":
                            break;
                    }
                }
            }
        });
        fList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (fList.getAdapter() == adapter) {
                    Friend f = friends.get(position);
                    ParseUser pu = f.getParseUser();
                    switch (f.getType()) {
                        case "friends":
                            displayRemoveFriendDialog(pu);
                            break;
                        case "pending":
                            new RemovePendingTask().execute(pu,f);
                            break;
                    }
                }
                return true;
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void populateFriends(){
        friends = new ArrayList<>();
        adapter = new Adapters.FriendAdapter(this,friends);
        new PopulateTask().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_friends_activity_three, menu);
        SearchManager manager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        search = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                changeAdapter(0);
                if (friends.isEmpty()) {
                    noFriends.setText("You haven't added anyone yet!\nSearch for friends above.");
                    noFriends.setVisibility(View.VISIBLE);
                } else {
                    noFriends.setVisibility(View.GONE);
                }
                return true;
            }
        });
        sv = (SearchView) search.getActionView();
        sv.setSearchableInfo(manager.getSearchableInfo(getComponentName()));
        sv.setIconifiedByDefault(false);
        sv.setOnSearchClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                noFriends.setVisibility(View.GONE);
            }
        });
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                noFriends.setVisibility(View.GONE);
                runQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void runQuery(String q){
        ParseQuery<ParseUser> userResult = ParseQuery.getQuery(ParseUser.class);
        userResult.whereContains("username",q)
                  .whereContains("username",Character.toLowerCase(q.charAt(0)) + q.substring(1))
                  .whereContains("username",Character.toUpperCase(q.charAt(0)) + q.substring(1))
                  .whereNotEqualTo("username",fragUser.getUsername());
        searched = new ArrayList<>();
        userResult.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                if(e == null){
                    for(ParseUser usr : list){
                        Friend f = Friend.parseFriend(usr);
                        f.setParseUser(usr);
                        if(!friends.contains(f))
                            searched.add(f);
                    }
                    if(searched.size() == 0)
                        Log.e("AUD","size 0");
                    sAdapter = new Adapters.FriendSearchAdapter(getApplicationContext(),searched);
                    changeAdapter(1);
                    if(searched.size() == 0){
                        noFriends.setText("No Results Found");
                        noFriends.setVisibility(View.VISIBLE);
                    }
                }
                else {
                    Log.e("AUD",e.getMessage());
                }
            }
        });
    }

    private void changeAdapter(int code){
        switch (code){
            case 0:
                fList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
                break;
            case 1:
                fList.setAdapter(sAdapter);
                sAdapter.notifyDataSetChanged();
                break;
        }
    }

    private void makeToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }

//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
//                               ASYNCTASKS
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-

    private class RemovePendingTask extends AsyncTask<Object,Void,Boolean>{
        private ParseUser tCpy, rCpy;
        private Friend f;

        @Override
        public void onPreExecute(){
            tCpy = fragUser;
        }

        @Override
        public Boolean doInBackground(Object... o){
            rCpy = (ParseUser)o[0];
            f = (Friend)o[1];
            ParseQuery<ParseObject> qu = ParseQuery.getQuery("PendingTable");
            qu.whereEqualTo("from",rCpy)
                    .whereEqualTo("to",tCpy);
            try{
                List<ParseObject> list = qu.find();
                list.get(0).delete();
            } catch(Exception e){
                Log.e("AUD",Log.getStackTraceString(e));
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean res){
            if(res){
                makeToast("Request Denied.");
                friends.remove(f);
                if(friends.isEmpty()){
                    noFriends.setText("You haven't added anyone yet!\nSearch for friends above.");
                    noFriends.setVisibility(View.VISIBLE);
                } else {
                    noFriends.setVisibility(View.GONE);
                }
                adapter.notifyDataSetChanged();
            }
        }

    }

    private class RemoveFriendTask extends AsyncTask<Object,Void,Boolean>{
        private ParseUser tCpy, rCpy;

        @Override
        public void onPreExecute(){
            tCpy = fragUser;
        }

        @Override
        public Boolean doInBackground(Object... o){
            rCpy = (ParseUser)o[0];
            ParseQuery<ParseObject> ft = ParseQuery.getQuery("FriendTable");
            ft.whereEqualTo("user",tCpy);
            try{
                List<ParseObject> res = ft.find();
                List<ParseUser> resList = (List<ParseUser>)res.get(0).get("all_friends");
                for(ParseUser pu : resList){
                    if(pu.fetch().equals(rCpy)){
                        resList.remove(pu);
                        break;
                    }
                }
                res.get(0).put("all_friends",resList);
                res.get(0).save();
            } catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            ft = ParseQuery.getQuery("FriendTable");
            ft.whereEqualTo("user",rCpy);
            try{
                List<ParseObject> res = ft.find();
                List<ParseUser> resList = (List<ParseUser>)res.get(0).get("all_friends");
                for(ParseUser pu : resList){
                    if(pu.fetch().equals(tCpy)){
                        resList.remove(pu);
                        break;
                    }
                }
                res.get(0).put("all_friends",resList);
                res.get(0).save();
            } catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean res){
            if(res){
                makeToast("Friend Removed");
                populateFriends();
            }
        }
    }


    private class AddFriendTask extends AsyncTask<Object,Void,Boolean>{
        private ParseUser tCpy, cCpy;
        private Friend f;

        @Override
        public void onPreExecute() {
            cCpy = fragUser;
        }

        @Override
        public Boolean doInBackground(Object... o) {
            tCpy = (ParseUser)o[0];
            f = (Friend)o[1];
            ParseQuery<ParseObject> switchToFriends =
                    ParseQuery.getQuery("FriendTable");
            switchToFriends.whereEqualTo("user", cCpy);
            try {
                List<ParseObject> sRes = switchToFriends.find();
                sRes.get(0).add("all_friends", tCpy);
                sRes.get(0).save();
            } catch (Exception pe) {
                return false;
            }
            ParseQuery<ParseObject> editOtherUsersFriends =
                    ParseQuery.getQuery("FriendTable");
            editOtherUsersFriends.whereEqualTo("user", tCpy);
            try {
                List<ParseObject> oRes = editOtherUsersFriends.find();
                oRes.get(0).add("all_friends", cCpy);
                oRes.get(0).save();
            } catch (Exception ex) {
                return false;
            }
            ParseQuery<ParseObject> removePending =
                    ParseQuery.getQuery("PendingTable");
            removePending.whereEqualTo("to", cCpy)
                    .whereEqualTo("from", tCpy);
            try {
                List<ParseObject> rRes = removePending.find();
                rRes.get(0).delete();
            } catch (Exception ex) {
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean res) {
            if (res) {
                f.setType("friends");
                adapter.notifyDataSetChanged();
                makeToast("Friend Added!");
            }
            friendsBeingAdded.remove(f);
        }
    }


    private class PopulateTask extends AsyncTask<Void,Void,Boolean>{
        private List<Friend> tmpReq, tmpPnd, tmpFri;
        private ParseUser tCpy;

        @Override
        public void onPreExecute(){
            tmpReq = new ArrayList<>();
            tmpPnd = new ArrayList<>();
            tmpFri = new ArrayList<>();
            tCpy = fragUser;
        }

        @Override
        public Boolean doInBackground(Void... v){
            ParseQuery<ParseObject> getPending = ParseQuery.getQuery("PendingTable");
            getPending.whereEqualTo("to",tCpy);
            try{
                List<ParseObject> fRes = getPending.find();
                for(ParseObject pobj : fRes){
                    ParseUser from = pobj.getParseUser("from").fetch();
                    Friend f = Friend.parseFriend(from);
                    f.setType("pending");
                    f.setParseUser(from);
                    tmpPnd.add(f);
                }
                Collections.sort(tmpPnd);
            }catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            ParseQuery<ParseObject> getFriends = ParseQuery.getQuery("FriendTable");
            getFriends.whereEqualTo("user",tCpy);
            try{
                List<ParseObject> sRes = getFriends.find();
                List<ParseUser> fnds = (List<ParseUser>)sRes.get(0).get("all_friends");
                for(ParseUser p : fnds){
                    ParseUser fetched = p.fetchIfNeeded();
                    Friend f = Friend.parseFriend(fetched);
                    f.setType("friends");
                    f.setParseUser(fetched);
                    tmpFri.add(f);
                }
                Collections.sort(tmpFri);
            } catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            ParseQuery<ParseObject> getRequested = ParseQuery.getQuery("PendingTable");
            getRequested.whereEqualTo("from",tCpy);
            try{
                List<ParseObject> tRes = getRequested.find();
                for(ParseObject pobj : tRes){
                    ParseUser user = pobj.getParseUser("to").fetchIfNeeded();
                    Friend f = Friend.parseFriend(user);
                    f.setType("requested");
                    f.setParseUser(user);
                    tmpReq.add(f);
                }
                Collections.sort(tmpReq);
            } catch(Exception ex){
                Log.e("AUD",Log.getStackTraceString(ex));
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute(Boolean res){
            if(res){
                friends.addAll(tmpPnd);
                friends.addAll(tmpFri);
                friends.addAll(tmpReq);
                if(friends.isEmpty()){
                    noFriends.setText("You haven't added anyone yet!\nSearch for friends above.");
                    noFriends.setVisibility(View.VISIBLE);
                } else {
                    noFriends.setVisibility(View.GONE);
                }
                fList.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }
        }
    }
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
//                               DIALOGS
//-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
    private static AlertDialog removeFriendDialog;

    private void displayRemoveFriendDialog(final ParseUser pu){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Remove Friend")
                .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new RemoveFriendTask().execute(pu);
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
}
