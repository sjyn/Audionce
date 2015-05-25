//package ai.com.audionce;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.v4.app.ListFragment;
//import android.support.v7.app.AppCompatActivity;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.SearchView;
//import com.parse.ParseUser;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//
//
//public class ViewFriendsActivityTwo extends AppCompatActivity {
//    private SearchView sv;
//    private ParseUser user;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_view_friends_activity_two);
//        user = ParseUser.getCurrentUser();
//        ViewFriendsActivityTwoFragment vfat = new ViewFriendsActivityTwoFragment();
//        getSupportFragmentManager().beginTransaction().replace(android.R.id.content,vfat).commit();
//    }
//
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.menu_view_friends, menu);
//        sv = (SearchView)menu.findItem(R.id.search);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//                case R.id.action_settings:
//                    break;
//                case R.id.log_out:
//                    ParseUser.logOut();
//                    startActivity(new Intent(this, LoginActivity.class));
//                    break;
//                case R.id.search:
//                    runQuery(sv.getQuery().toString());
//                    break;
//            }
//            return super.onOptionsItemSelected(item);
//    }
//
//    private void runQuery(String query){
//
//    }
//
//    public static class ViewFriendsActivityTwoFragment extends ListFragment {
//        private List<Friend> friends;
//        private Adapters.FriendAdapter adapter;
//        private ParseUser fragUser;
//
//        public ViewFriendsActivityTwoFragment() {
//
//        }
//
//        @Override
//        public void onCreate(Bundle instance){
//            super.onCreate(instance);
//
//            friends = new ArrayList<>();
//            fragUser = ParseUser.getCurrentUser();
//            HashMap<ParseUser,String> map = (HashMap<ParseUser,String>)fragUser.get("friends");
//            List<ParseUser> keys = new ArrayList<>(map.keySet());
//            List<Friend> frnds, reqs, pnds;
//            frnds = new ArrayList<>();
//            reqs = new ArrayList<>();
//            pnds = new ArrayList<>();
//            for(ParseUser p : keys){
//                String type = map.get(p);
//                if(type.contains("friends") || type.contains("Friends")){
//                    frnds.add(Friend.parseFriend(p));
//                }
//                if(type.contains("pending") || type.contains("Pending")){
//                    pnds.add(Friend.parseFriend(p));
//                }
//                if(type.contains("request") || type.contains("Request")){
//                    reqs.add(Friend.parseFriend(p));
//                }
//            }
//            Collections.sort(frnds);
//            Collections.sort(reqs);
//            Collections.sort(pnds);
//            friends.addAll(frnds);
//            friends.addAll(reqs);
//            friends.addAll(pnds);
//            adapter = new Adapters.FriendAdapter(getActivity(),friends);
//            setListAdapter(adapter);
//            adapter.notifyDataSetChanged();
//        }
//
//        @Override
//        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//            return inflater.inflate(R.layout.fragment_view_friends_activity_two, container, false);
//        }
//    }
//
//
//
//
//}
