package edu.niu.android.smartstudyplanner.notifications;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import edu.niu.android.smartstudyplanner.data.database.StudyPlannerDatabase;
import edu.niu.android.smartstudyplanner.data.entities.Task;

import java.util.List;

public class NotificationReceiver extends BroadcastReceiver {

    /**
     * Routes the two broadcast types this receiver listens for: BOOT_COMPLETED
     * (after device reboot) and the task deadline alarms scheduled by
     * NotificationHelper.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // Boot just finished, rebuild every pending alarm.
        // Run on a background thread since we hit the database.
        if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            new Thread(() -> rescheduleAllNotifications(context)).start();
            return;
        }

        // Otherwise this is a task deadline alarm firing.
        int notifType = intent.getIntExtra(
                NotificationHelper.EXTRA_NOTIFICATION_TYPE,
                NotificationHelper.NOTIF_TYPE_TASK_DUE
        );

        if (notifType == NotificationHelper.NOTIF_TYPE_TASK_DUE) {
            long taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1);
            String taskTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE);

            // Guard against malformed intents.
            if (taskId != -1 && taskTitle != null) {
                NotificationHelper.showTaskDueNotification(context, taskId, taskTitle);
            }
        }
    }

    /**
     * Pulls every incomplete task that still needs a notification scheduled
     * and rebuilds its alarm 24 hours before the due date. Tasks whose
     * trigger time has already passed are skipped. Runs on a background
     * thread because it reads from Room directly.
     */
    private void rescheduleAllNotifications(Context context) {
        StudyPlannerDatabase db = StudyPlannerDatabase.getInstance(context);

        long now = System.currentTimeMillis();
        List<Task> tasks = db.taskDao().getTasksNeedingNotification(now);

        for (Task task : tasks) {
            // Reminder fires 24 hours before the due date.
            long triggerAt = task.getDueDate() - (24 * 60 * 60 * 1000L);

            if (triggerAt > now) {
                NotificationHelper.scheduleTaskNotification(context, task, triggerAt);
            }
        }
    }
}