/**************************************************************
 * Purpose: Custom Application class. This is the first thing *
 * that runs when the app starts, before any Activity or      *
 * Fragment is created. Used to create the notification       *
 * channels on launch so they are ready before any code tries *
 * to post a notification. Registered in AndroidManifest.xml  *
 * via android:name on the application tag.                   *
 **************************************************************/

package edu.niu.android.smartstudyplanner;

import android.app.Application;

import edu.niu.android.smartstudyplanner.notifications.NotificationHelper;

public class StudyPlannerApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /**
         * Create the notification channels right at startup. Android 8+
         * requires a channel before any notification can be shown, and
         * it's safe to call this every launch since the system ignores
         * create requests for channels that already exist.
         */
        NotificationHelper.createNotificationChannels(this);
    }
}