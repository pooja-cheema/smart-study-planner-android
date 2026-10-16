/**************************************************************
 * Purpose: Home screen widget. Shows the current count of    *
 * pending tasks and today's total study time. Refreshes      *
 * every 30 minutes per the metadata in res/xml, and tapping  *
 * anywhere on the widget opens the app.                      *
 **************************************************************/

package edu.niu.android.smartstudyplanner;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import edu.niu.android.smartstudyplanner.data.database.StudyPlannerDatabase;
import edu.niu.android.smartstudyplanner.ui.MainActivity;
import edu.niu.android.smartstudyplanner.utils.DateUtils;

public class StudyPlannerWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
                         int[] appWidgetIds) {
        // Refresh each instance the user has placed on their home screen.
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId);
        }
    }

    /**
     * Refreshes a single widget instance. Loads the pending task count
     * and today's study time on a background thread and pushes the
     * resulting text into the RemoteViews when ready.
     */
    static void updateWidget(Context context, AppWidgetManager appWidgetManager,
                             int appWidgetId) {
        // Tap anywhere on the widget opens MainActivity.
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews views = new RemoteViews(
                context.getPackageName(), R.layout.widget_study_planner);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // Database access happens on a worker thread so we don't block the
        // system's widget update path.
        new Thread(() -> {
            try {
                StudyPlannerDatabase db = StudyPlannerDatabase.getInstance(context);

                // Pending task count for the top label.
                int pendingTasks = db.taskDao().getPendingTaskCount();

                // Today's total study time, formatted as Xh Ym for the bottom label.
                long todayStart = DateUtils.getTodayRange()[0];
                long studySeconds = db.studySessionDao()
                        .getTotalStudySecondsSince(todayStart);
                String studyTime = DateUtils.formatDuration(studySeconds);

                views.setTextViewText(R.id.widget_pending_tasks,
                        "📋 Pending: " + pendingTasks + " task"
                                + (pendingTasks == 1 ? "" : "s"));
                views.setTextViewText(R.id.widget_study_time,
                        "⏱ Today: " + studyTime);

                // Push the populated RemoteViews to the system once we have real values.
                appWidgetManager.updateAppWidget(appWidgetId, views);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        // Initial push so the widget paints right away with placeholder text
        // while the background thread fetches the real numbers.
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}