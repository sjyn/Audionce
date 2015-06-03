//TODO -- DELETE THIS ACTIVITY
//package ai.com.audionce;
//
//import android.app.ListActivity;
//import android.app.ListFragment;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.support.v7.app.ActionBarActivity;
//import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.Menu;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.ImageView;
//import android.widget.SearchView;
//import android.widget.TextView;
//
//import com.parse.FindCallback;
//import com.parse.Parse;
//import com.parse.ParseException;
//import com.parse.ParseObject;
//import com.parse.ParseQuery;
//import com.parse.ParseUser;
//
//import org.json.JSONArray;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//
//public class ViewFriendsActivity extends AppCompatActivity {
//
//    @Override
//    public void onCreate(Bundle b){
//        super.onCreate(b);
//
//    }
//
//    public static class FriendsListFragment extends ListFragment {
//        ParseUser currUser;
//        private FriendsListAdapter<ParseObject> fAdapter;
//        private SearchView sv;
//
//        @Override
//        protected void onCreate(Bundle savedInstanceState) {
//            super.onCreate(savedInstanceState);
////        setContentView(R.layout.activity_view_friends);
//            currUser = ParseUser.getCurrentUser();
//            HashMap pFriends = (HashMap) currUser.get("friends");
//            Log.i("AUD", pFriends == null ? "null" : pFriends.toString());
//            fAdapter = new FriendsListAdapter<ParseObject>(this, R.layout.friend_list_item, new ArrayList<ParseObject>());
//        }
//
//        @Override
//        public boolean onCreateOptionsMenu(Menu menu) {
//            getMenuInflater().inflate(R.menu.menu_view_friends, menu);
//            sv = (SearchView) menu.findItem(R.id.search);
//            return true;
//        }
//
//        @Override
//        public boolean onOptionsItemSelected(MenuItem item) {
//            switch (item.getItemId()) {
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
//        }
//
//        private void runQuery(String forUserName) {
//            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("ParseUser");
//            query.whereContains("username", forUserName);
//            query.findInBackground(new FindCallback<ParseObject>() {
//                @Override
//                public void done(List<ParseObject> list, ParseException e) {
//                    if (e == null) {
//                        fAdapter.clear();
//                        fAdapter.addAll(list);
//                    }
//                }
//            });
//        }
//
//        public static class FriendsListAdapter<T> extends ArrayAdapter<T> {
//            private Map friendInfo;
//
//            private static class Holder {
//                ImageView iv;
//                TextView name;
//            }
//
//            public FriendsListAdapter(Context c, int id, List<T> data) {
//                super(c, id, data);
//            }
//
//            @Override
//            public View getView(int pos, View cv, ViewGroup parent) {
//                if (cv == null) {
//                    LayoutInflater inflater = (LayoutInflater) super.getContext().
//                            getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//                    cv = inflater.inflate(R.layout.friend_list_item, parent, false);
//                    Holder holder = new Holder();
//                    holder.iv = (ImageView) cv.findViewById(R.id.friend_picture);
//                    holder.name = (TextView) cv.findViewById(R.id.friend_name);
//                    cv.setTag(holder);
//                }
//                final Holder h = (Holder) cv.getTag();
////            h.name.setText();
//                return cv;
//            }
//        }
//
//        private static class Friend {
//            public String name;
//            public Bitmap picture;
//            private BitmapFactory.Options opts;
//
//            public Friend(ParseObject po) {
//
//            }
//        }
//    }
//}
