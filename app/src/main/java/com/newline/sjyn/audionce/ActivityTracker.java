package com.newline.sjyn.audionce;

import android.content.Context;
import android.view.View;

import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;

import ai.com.audionce.R;

public class ActivityTracker {

    public enum ActiveActivity {
        ACTIVITY_HUB,
        ACTIVITY_NEW_SOUND,
        ACTIVITY_PROFILE,
        ACTIVITY_SETTINGS,
        ACTIVITY_FRIENDS,
        ACTIVITY_SEARCH
    }

    private static ActivityTracker tracker = new ActivityTracker();
    private Context curr;
    private ActiveActivity thisActivity;

    private ActivityTracker(){

    }

    public static ActivityTracker getActivityTracker(){
        return tracker;
    }

    public void update(Context context, ActiveActivity activity){
        curr = context;
        thisActivity = activity;
    }

//    public ActiveActivity getCurrentActivity(){
//        return thisActivity;
//    }

    public void postSnackBarWithText(String txt, View.OnClickListener click){
        Snackbar snackbar =  Snackbar.with(curr)
                .color(curr.getResources().getColor(R.color.ab_pink_light))
                .text(txt)
                .swipeToDismiss(true)
                .duration(Snackbar.SnackbarDuration.LENGTH_INDEFINITE);
        snackbar.setOnClickListener(click);
        SnackbarManager.show(snackbar);
    }
}
