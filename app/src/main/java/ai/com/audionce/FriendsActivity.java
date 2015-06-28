package ai.com.audionce;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.ListFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.github.rahatarmanahmed.cpv.CircularProgressView;
import com.newline.sjyn.audionce.Adapters;
import com.newline.sjyn.audionce.Friend;
import com.newline.sjyn.audionce.Utilities;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("ALL")
public class FriendsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_friends);
        ActionBar ab = getSupportActionBar();
        ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        ab.setStackedBackgroundDrawable(
                new ColorDrawable(getResources().getColor(R.color.ab_pink)));
        Tab tab = ab.newTab()
                .setText("Friends")
                .setTabListener(new TabListener<>(this, "Friends", FriendFragment.class));
        ab.addTab(tab);
        tab = ab.newTab()
                .setText("Pending")
                .setTabListener(new TabListener<>(this, "Pending", PendingFragment.class));
        ab.addTab(tab);
        tab = ab.newTab()
                .setText("Search")
                .setTabListener(new TabListener<>(this, "Search", SearchFragment.class));
        ab.addTab(tab);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_friends, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, Settings.class));
                break;
            case R.id.log_out:
                ParseUser.logOut();
                Intent in = new Intent(this, LoginActivity.class);
                Utilities.stopSoundPickupService(this);
                in.putExtra("should_auto_login_from_intent", "no");
                startActivity(in);
                break;
            case R.id.goto_map:
                startActivity(new Intent(this, HubActivity.class));
                break;
            case R.id.new_sound_from_hub:
                startActivity(new Intent(this, NewSoundActivity.class));
                break;
            case R.id.edit_profile:
                startActivity(new Intent(this, ProfileMain.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment fragment;
        private final Activity activity;
        private final String tag;
        private final Class<T> clazz;

        public TabListener(Activity act, String tag, Class<T> clz) {
            activity = act;
            this.tag = tag;
            clazz = clz;
        }

        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ft) {
            if (fragment == null) {
                fragment = Fragment.instantiate(activity, clazz.getName());
                ft.add(android.R.id.content, fragment, tag);
            } else {
                ft.attach(fragment);
            }
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ft) {
            if (fragment != null) {
                ft.detach(fragment);
            }
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {

        }
    }

    public static class FriendFragment extends ListFragment {
        private Adapters.FriendAdapter adapter;
        private final String noFriendsText =
                "You haven't added any friends yet.\nYou can add friends by selecting the " +
                        "search option above.\n\nBy adding friends, you can drop private sounds " +
                        "that only select friends can see.\n\nAs soon as someone accepts your friend" +
                        " request, you will see them here.";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle instance) {
            if (adapter != null)
                adapter.clear();
            populateFriends(inflater);
            return super.onCreateView(inflater, container, instance);
        }

        @Override
        public void onStart() {
            super.onStart();
            setEmptyText(noFriendsText);
            configureLongClick();
        }

        private void configureLongClick() {
            getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    final Friend f = (Friend) getListAdapter().getItem(position);
                    createRemoveFriendDialog(f);
                    return false;
                }
            });
        }

        private void createRemoveFriendDialog(final Friend friend) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                    .setTitle("Unfriend " + friend.getUsername() + "?")
                    .setMessage("Are you sure you want to unfriend this person? You will have" +
                            " to re-add them if you want to be friends with this person again.")
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("Unfriend", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Utilities.removeFriend(friend);
                            new AsyncTask<Void, Void, Boolean>() {
                                private ParseUser tUser = ParseUser.getCurrentUser();
                                private Friend fri = friend;

                                @Override
                                public Boolean doInBackground(Void... v) {
                                    try {
                                        ParseUser friendUser = fri.getParseUser().fetchIfNeeded();
                                        removeMyFriend(friendUser);
                                        removeTheirFriend(friendUser);
                                        tUser.save();
                                        friendUser.save();
                                    } catch (Exception ex) {
                                        return false;
                                    }
                                    return true;
                                }

                                @Override
                                public void onPostExecute(Boolean res) {
                                    if (res) {
                                        adapter.remove(friend);
                                        adapter.notifyDataSetChanged();
                                    }
                                }

                                @SuppressWarnings({"unchecked"})
                                private void removeMyFriend(ParseUser removeMe) throws Exception {
                                    ParseObject ft = ((ParseObject) tUser.get("friends")).fetchIfNeeded();
                                    List<ParseUser> tFriends = (List<ParseUser>) ft.get("all_friends");
                                    tFriends.remove(removeMe);
                                    ft.save();
                                }

                                @SuppressWarnings({"unchecked"})
                                private void removeTheirFriend(ParseUser removeFrom) throws Exception {
                                    ParseObject ft = ((ParseObject) removeFrom.get("friends")).fetchIfNeeded();
                                    List<ParseUser> tFriends = (List<ParseUser>) ft.get("all_friends");
                                    tFriends.remove(tUser);
                                    ft.save();
                                }
                            }.execute();
                        }
                    });
            builder.create().show();
        }

        private void populateFriends(final LayoutInflater inflater) {
            new AsyncTask<Void, Void, Boolean>() {
                private ArrayList<Friend> cpy;

                @Override
                public Boolean doInBackground(Void... v) {
                    cpy = new ArrayList<>();
                    try {
                        ParseObject po = ParseUser.getCurrentUser().
                                getParseObject("friends").fetchIfNeeded();
                        List<ParseUser> list = po.getList("all_friends");
                        for (ParseUser pu : list) {
                            Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                            f.setType("friends");
                            cpy.add(f);
                        }
                    } catch (Exception ex) {
                        return false;
                    }
                    return true;
                }

                @Override
                public void onPostExecute(Boolean res) {
                    super.onPostExecute(res);
                    Utilities.setFriendsList(cpy);
                    adapter = new Adapters.FriendAdapter
                            (inflater.getContext(), Utilities.getFriends());
                    setListAdapter(adapter);
                }
            }.execute();
        }
    }

    public static class PendingFragment extends ListFragment {
        private Adapters.FriendAdapter adapter;
        private final String noPendingText = "You have no pending requests.\n\n" +
                "If a request is \"pending,\" you are waiting on that person to accept your " +
                "request. You can rescind the request by holding down on the person.\n\nIf a request " +
                "is \"requested,\" you can accept them by clicking on the request.";

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle instance) {
            populatePendingFriends(inflater);
            return super.onCreateView(inflater, container, instance);
        }

        @Override
        public void onStart() {
            super.onStart();
            configureLongClick();
            setEmptyText(noPendingText);
        }

        private void configureLongClick() {
            getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                    final Friend f = adapter.getItem(position);
                    if ("pending".equals(f.getType())) {
                        new AsyncTask<Void, Void, Boolean>() {
                            private ParseUser user = ParseUser.getCurrentUser();
                            private Friend fri = f;

                            @Override
                            public Boolean doInBackground(Void... v) {
                                try {
                                    ParseQuery.getQuery("PendingTable")
                                            .whereEqualTo("to", fri.getParseUser().fetchIfNeeded())
                                            .whereEqualTo("from", user)
                                            .find()
                                            .get(0)
                                            .fetchIfNeeded()
                                            .delete();
                                } catch (Exception ex) {
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onPostExecute(Boolean res) {
                                if (res) {
                                    adapter.remove(f);
                                    adapter.notifyDataSetChanged();
                                    Utilities.makeToast(getActivity(), "Request rescinded.");
                                }
                            }
                        }.execute();
                    }
                    return false;
                }
            });
            getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Friend fri = adapter.getItem(position);
                    if ("requested".equals(fri.getType())) {
                        new AsyncTask<Void, Void, Boolean>() {
                            private ParseUser user = ParseUser.getCurrentUser();
                            private Friend f = fri;

                            @Override
                            public Boolean doInBackground(Void... v) {
                                try {
                                    setYourFriends(f.getParseUser().fetchIfNeeded());
                                    setTheirFriends(f.getParseUser().fetchIfNeeded());
                                    cleanUpPending();
                                } catch (Exception ex) {
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onPostExecute(Boolean res) {
                                if (res) {
                                    Utilities.addFriend(fri);
                                    adapter.remove(fri);
                                    adapter.notifyDataSetChanged();
                                    Utilities.makeToast(getActivity(), "Friend added!");
                                }
                            }

                            private void setYourFriends(ParseUser addMe) throws Exception {
                                ParseObject obj = user.getParseObject("friends");
                                obj.add("all_friends", addMe);
                                obj.save();
                                user.save();
                                Friend f = Friend.parseFriend(addMe);
                                assert f != null;
                                f.setType("friends");
                                Utilities.addFriend(f);
                            }

                            private void setTheirFriends(ParseUser changeMe) throws Exception {
                                ParseObject pobj = changeMe.getParseObject("friends");
                                pobj.add("all_friends", user);
                                pobj.save();
                                changeMe.save();
                            }

                            private void cleanUpPending() throws Exception {
                                ParseQuery<ParseObject> ptq = ParseQuery.getQuery("PendingTable");
                                ptq.whereEqualTo("from", f.getParseUser().fetchIfNeeded())
                                        .whereEqualTo("to", user);
                                List<ParseObject> finds = ptq.find();
                                finds.get(0).delete();
                            }
                        }.execute();
                    }
                }
            });
        }

        private void populatePendingFriends(final LayoutInflater inflater) {
            new AsyncTask<Void, Void, Boolean>() {
                private ParseUser tUser = ParseUser.getCurrentUser();
                private List<Friend> pend, reqs;

                @Override
                public Boolean doInBackground(Void... v) {
                    pend = reqs = new ArrayList<>();
                    ParseQuery<ParseObject> q1 = ParseQuery.getQuery("PendingTable");
                    q1.whereEqualTo("from", tUser);
                    ParseQuery<ParseObject> q2 = ParseQuery.getQuery("PendingTable");
                    q2.whereEqualTo("to", tUser);
                    try {
                        List<ParseObject> q1Res = q1.find();
                        for (ParseObject po : q1Res) {
                            ParseUser pu = po.getParseUser("to");
                            Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                            assert f != null;
                            f.setType("pending");
                            pend.add(f);
                        }
                        List<ParseObject> q2Res = q2.find();
                        for (ParseObject po : q2Res) {
                            ParseUser pu = po.getParseUser("from");
                            Friend f = Friend.parseFriend(pu.fetchIfNeeded());
                            assert f != null;
                            f.setType("requested");
                            reqs.add(f);
                        }
                    } catch (Exception ex) {
                        return false;
                    }
                    return true;
                }

                @Override
                public void onPostExecute(Boolean res) {
                    if (res) {
                        Collections.sort(pend);
                        Collections.sort(reqs);
                        List<Friend> all = new ArrayList<>();
                        all.addAll(pend);
                        all.addAll(reqs);
                        Set<Friend> set = new HashSet<>(all);
                        all.clear();
                        all.addAll(set);
                        adapter = new Adapters.FriendAdapter(inflater.getContext(), all);
                        setListAdapter(adapter);
                    }
                }
            }.execute();
        }
    }

    public static class SearchFragment extends Fragment {
        private Adapters.FriendSearchAdapter adapter;
        private ImageButton ib;
        private ListView lv;
        private EditText et;
        private CircularProgressView cpv;

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle instance) {
            return inflater.inflate(R.layout.search_fragment_layout, container, false);
        }

        @Override
        public void onStart() {
            super.onStart();
            ib = (ImageButton) getView().findViewById(R.id.imageButton3);
            lv = (ListView) getView().findViewById(R.id.search_list);
            et = (EditText) getView().findViewById(R.id.search_edit_text);
            cpv = (CircularProgressView) getView().findViewById(R.id.progress_view);
            cpv.setVisibility(View.GONE);
            ib.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String q = et.getText().toString();
                    if (!"".equals(q))
                        runQueryAndPopulateList(q);
                }
            });
        }

        private void runQueryAndPopulateList(final String query) {
            new AsyncTask<Void, Void, Boolean>() {
                private String qCpy = query;
                private ParseUser puC = ParseUser.getCurrentUser();
                private List<Friend> foundFriends;

                @Override
                public void onPreExecute() {
                    try {
                        InputMethodManager imm = (InputMethodManager) getActivity().
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(getActivity().getCurrentFocus().
                                getWindowToken(), 0);
                    } catch (NullPointerException ignored) {
                    }
                    cpv.setVisibility(View.VISIBLE);
                    cpv.startAnimation();
                    ib.setEnabled(false);
                    if (adapter != null) {
                        adapter.clear();
                        adapter.notifyDataSetChanged();
                    }
                    foundFriends = new ArrayList<>();
                }

                @Override
                public Boolean doInBackground(Void... v) {
                    ParseQuery<ParseUser> userResult = ParseQuery.getQuery(ParseUser.class);
                    userResult.whereContains("username", qCpy)
                            .whereContains("username", Character.toLowerCase(qCpy.charAt(0)) + qCpy.substring(1))
                            .whereContains("username", Character.toUpperCase(qCpy.charAt(0)) + qCpy.substring(1))
                            .whereNotEqualTo("username", puC.getUsername());
                    try {
                        List<ParseUser> results = userResult.find();
                        for (ParseUser pu : results) {
                            ParseUser puc = pu.fetchIfNeeded();
                            ParseQuery qu = ParseQuery.getQuery("PendingTable")
                                    .whereEqualTo("from", puC)
                                    .whereEqualTo("to", puc);
                            if (qu.find().isEmpty()) {
                                Friend f = Friend.parseFriend(puc);
                                assert f != null;
                                f.setType("add");
                                foundFriends.add(f);
                            }
                        }
                        foundFriends.removeAll(Utilities.getFriends());
                    } catch (Exception ex) {
                        return false;
                    }
                    return true;
                }

                @Override
                public void onPostExecute(Boolean res) {
                    if (res) {
                        adapter = new Adapters.FriendSearchAdapter(getActivity(), foundFriends);
                        configureAdapter();
                        lv.setAdapter(adapter);
                    }
                    cpv.clearAnimation();
                    cpv.setVisibility(View.GONE);
                    ib.setEnabled(true);
                }
            }.execute();
        }

        private void configureAdapter() {
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final Friend f = adapter.getItem(position);
                    new AsyncTask<Void, Void, Boolean>() {
                        private Friend fri = f;
                        private ParseUser pu = ParseUser.getCurrentUser();

                        @Override
                        public Boolean doInBackground(Void... v) {
                            try {
                                ParseObject newPend = new ParseObject("PendingTable");
                                newPend.put("to", fri.getParseUser().fetchIfNeeded());
                                newPend.put("from", pu);
                                newPend.save();
                            } catch (Exception ex) {
                                return false;
                            }
                            return true;
                        }

                        @Override
                        public void onPostExecute(Boolean res) {
                            if (res) {
                                adapter.remove(f);
                                adapter.notifyDataSetChanged();
                                Utilities.makeToast(getActivity(), "Friend requested.");
                            } else {
                                Utilities.makeToast(getActivity(), "Error requesting friend.");
                            }
                        }
                    }.execute();
                }
            });
        }
    }
}
