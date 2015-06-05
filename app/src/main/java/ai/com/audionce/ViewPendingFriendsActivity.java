package ai.com.audionce;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Friend;
import com.newline.sjyn.audionce.Utilities;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class ViewPendingFriendsActivity extends AppCompatActivity {
    private ListView populateMe;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private Adapters.FriendAdapter adapter;
    private Adapters.FriendSearchAdapter sAdapter;
    private List<Friend> adaList, searchedFriends;
    private SearchView sv;
    private MenuItem search;
    private TextView noResultsFound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pending_friends);
        populateMe = (ListView)findViewById(R.id.pending_friends_list_view);
        adaList = searchedFriends = new ArrayList<>();
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        noResultsFound = (TextView)findViewById(R.id.no_results_found);
        noResultsFound.setVisibility(View.GONE);
        Intent didSearch = getIntent();
        if(didSearch == null || !Intent.ACTION_SEARCH.equals(didSearch.getAction())) {
            adapter = new Adapters.FriendAdapter(this,adaList);
            repopulatePending();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_view_pending_friends, menu);
        SearchManager manager = (SearchManager)getSystemService(Context.SEARCH_SERVICE);
        search = menu.findItem(R.id.action_search);
        MenuItemCompat.setOnActionExpandListener(search, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                if(populateMe.getAdapter() == sAdapter){
                    Log.e("AUD", "Changing adapter to adapter");
                    populateMe.setAdapter(adapter);
                    repopulatePending();
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

            }
        });
        sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                Log.e("AUD", "Query submitted");
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

    private void repopulatePending(){
        Log.e("AUD", "Repopulating pending");
        adaList.clear();
//        adapter.notifyDataSetChanged();
        new AsyncTask<Void,Void,Boolean>(){
            private ParseUser tUser = currUser;
            private List<Friend> pend, reqs;

            @Override
            public Boolean doInBackground(Void... v){
                pend = reqs = new ArrayList<>();
                ParseQuery<ParseObject> q1 = ParseQuery.getQuery("PendingTable");
                q1.whereEqualTo("from", tUser);
                ParseQuery<ParseObject> q2 = ParseQuery.getQuery("PendingTable");
                q2.whereEqualTo("to",tUser);
                try{
                    List<ParseObject> q1Res = q1.find();
                    for(ParseObject po : q1Res){
                        ParseUser pu = po.getParseUser("to");
                        Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                        f.setType("pending");
                        pend.add(f);
                    }
                    List<ParseObject> q2Res = q2.find();
                    for(ParseObject po : q2Res){
                        ParseUser pu = po.getParseUser("from");
                        Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                        f.setType("requested");
                        reqs.add(f);
                    }
                } catch (Exception ex){
                    Log.e("AUD",Log.getStackTraceString(ex));
                    return false;
                }
                return true;
            }

            @Override
            public void onPostExecute(Boolean res){
                if(res){
                    Log.e("AUD", "repopulation okay");
                    Collections.sort(pend);
                    Collections.sort(reqs);
                    Set<Friend> fset = new LinkedHashSet<Friend>();
                    adaList.addAll(reqs);
                    adaList.addAll(pend);
                    fset.addAll(adaList);
                    adaList.clear();
                    adaList.addAll(fset);
                    Log.e("AUD", "adaList size: " + adaList.size());
                    adapter.notifyDataSetChanged();
                    populateMe.setAdapter(adapter);
                }
            }
        }.execute();
        setNonSearchAdapterListViewSetting();
    }

    private void runQuery(String q){
        searchedFriends.clear();
        ParseQuery<ParseUser> userResult = ParseQuery.getQuery(ParseUser.class);
        userResult.whereContains("username",q)
                  .whereContains("username",Character.toLowerCase(q.charAt(0)) + q.substring(1))
                  .whereContains("username",Character.toUpperCase(q.charAt(0)) + q.substring(1))
                  .whereNotEqualTo("username", currUser.getUsername());
        userResult.findInBackground(new FindCallback<ParseUser>() {
            @Override
            public void done(List<ParseUser> list, ParseException e) {
                if (e == null && !list.isEmpty()) {
                    noResultsFound.setVisibility(View.GONE);
                    for (ParseUser pu : list) {
                        Friend f = Friend.parseFriend(pu);
                        f.setType("add");
                        searchedFriends.add(f);
                    }
                    sAdapter = new Adapters.FriendSearchAdapter(getApplicationContext(), searchedFriends);
                    populateMe.setAdapter(sAdapter);
                    setSearchAdapterListViewSettings();
                } else {
                    if(e != null){
                        Utilities.makeLogFromThrowable(e);
                    }
                    noResultsFound.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void setNonSearchAdapterListViewSetting(){
        populateMe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Friend f = adaList.get(position);
                switch (f.getType()) {
                    case "pending":
                        break;
                    case "requested":
                        new AsyncTask<Void, Void, Boolean>() {
                            private ParseUser tUser = currUser;

                            @Override
                            public Boolean doInBackground(Void... v) {
                                try {
                                    setYourFriends(f.getParseUser().fetchIfNeeded());
                                    setTheirFriends(f.getParseUser().fetchIfNeeded());
                                    cleanUpPending();
                                } catch (Exception ex) {
                                    Log.e("AUD", Log.getStackTraceString(ex));
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onPostExecute(Boolean res) {
                                if (res) {
                                    makeToast("Friend Added!");
                                    adaList.remove(f);
                                    adapter.notifyDataSetChanged();
                                }
                            }

                            private void setYourFriends(ParseUser addMe) throws Exception {
                                ParseObject obj = tUser.getParseObject("friends");
                                obj.add("all_friends", addMe);
                                obj.save();
                                tUser.save();
                            }

                            private void setTheirFriends(ParseUser changeMe) throws Exception {
                                ParseObject pobj = changeMe.getParseObject("friends");
                                pobj.add("all_friends", tUser);
                                pobj.save();
                                changeMe.save();
                            }

                            private void cleanUpPending() throws Exception {
                                ParseQuery<ParseObject> ptq = ParseQuery.getQuery("PendingTable");
                                ptq.whereEqualTo("from", f.getParseUser().fetchIfNeeded())
                                        .whereEqualTo("to", tUser);
                                List<ParseObject> finds = ptq.find();
                                finds.get(0).delete();
                            }
                        }.execute();
                        break;
                }
            }
        });
    }

    private void setSearchAdapterListViewSettings(){
        populateMe.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                new AsyncTask<Void, Void, Boolean>() {
                    private ParseUser tUser;
                    private ParseUser reqUser;

                    @Override
                    public void onPreExecute() {
                        tUser = currUser;
                        reqUser = searchedFriends.get(position).getParseUser();
                    }

                    @Override
                    public Boolean doInBackground(Void... params) {
                        ParseObject mFriendTable = tUser.getParseObject("friends");
                        List<ParseUser> mFriend = (List<ParseUser>)mFriendTable.get("all_friends");
                        if(!mFriend.contains(reqUser)) {
                            ParseObject pt = new ParseObject("PendingTable");
                            pt.put("from", tUser);
                            pt.put("to", reqUser);
                            ParseQuery pq = ParseQuery.getQuery("PendingTable");
                            pq.whereEqualTo("from", currUser)
                                    .whereEqualTo("to", reqUser);
                            try {
                                if (pq.find().isEmpty()) {
                                    pt.save();
                                } else {
                                    return false;
                                }
                            } catch (Exception ex) {
                                Log.e("AUD", Log.getStackTraceString(ex));
                                return false;
                            }
                            return true;
                        }
                        return false;
                    }

                    @Override
                    public void onPostExecute(Boolean res) {
                        if (res) {
                            makeToast("Friend Requested!");
                        } else {
                            makeToast("You are already friends with this person.");
                        }
                    }
                }.execute();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
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
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }


}
