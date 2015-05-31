package ai.com.audionce;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.SearchView;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class ViewPendingFriendsActivity extends AppCompatActivity {
    private ListView populateMe;
    private final ParseUser currUser = ParseUser.getCurrentUser();
    private Adapters.FriendAdapter adapter;
    private Adapters.FriendSearchAdapter sAdapter;
    private List<Friend> adaList, searchedFriends;
    private SearchView sv;
    private MenuItem search;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_pending_friends);
        populateMe = (ListView)findViewById(R.id.pending_friends_list_view);
        adaList = searchedFriends = new ArrayList<>();
        Intent didSearch = getIntent();
        if(didSearch == null || !Intent.ACTION_SEARCH.equals(didSearch.getAction())) {
            ParseQuery<ParseObject> pending = ParseQuery.getQuery("PendingTable")
                    .whereEqualTo("to", currUser)
                    .whereEqualTo("from", currUser);
            pending.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> list, ParseException e) {
                    if (e == null && !list.isEmpty()) {
                        for (ParseObject po : list) {
                            Friend f = Friend.parseFriend((ParseUser) po);
                            f.setType("pending");
                            adaList.add(f);
                        }
                        Collections.sort(adaList);
                        adapter = new Adapters.FriendAdapter(getApplicationContext(), adaList);
                        populateMe.setAdapter(adapter);
                    }
                }
            });
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
                    populateMe.setAdapter(adapter);
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
                    for (ParseUser pu : list) {
                        Friend f = Friend.parseFriend(pu);
                        f.setParseUser(pu);
                        f.setType("add");
                        searchedFriends.add(f);
                    }
                    sAdapter = new Adapters.FriendSearchAdapter(getApplicationContext(), searchedFriends);
                    populateMe.setAdapter(sAdapter);
                    setSearchAdapterListViewSettings();
                } else {
                    //TODO -- Set a textview to say no results found
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
                        ParseObject pt = new ParseObject("PendingTable");
                        pt.put("from", tUser);
                        pt.put("to", reqUser);
                        ParseQuery pq = ParseQuery.getQuery("PendingTable");
                        pq.whereEqualTo("from",currUser)
                                .whereEqualTo("to",reqUser);
                        try {
                            if(pq.find().isEmpty())
                                pt.save();
                            else
                                return false;
                        } catch (Exception ex) {
                            Log.e("AUD", Log.getStackTraceString(ex));
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void onPostExecute(Boolean res) {
                        if (res) {
                            makeToast("Friend Requested!");
                        } else {
                            makeToast("Friend Request Failed");
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
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void makeToast(String s){
        Toast.makeText(this,s,Toast.LENGTH_SHORT).show();
    }
}
