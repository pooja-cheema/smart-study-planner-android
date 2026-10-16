package edu.niu.android.smartstudyplanner.notifications;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.ui.MainActivity;

public class NotificationHelper {

    // Channel IDs. Each channel has its own importance and vibration settings.
    public static final String CHANNEL_TASKS = "channel_tasks";
    public static final String CHANNEL_TIMER = "channel_timer";
    public static final String CHANNEL_STUDY = "channel_study";

    // Keys for the values passed to NotificationReceiver via Intent extras.
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_TASK_TITLE = "task_title";
    public static final String EXTRA_NOTIFICATION_TYPE = "notification_type";

    // Notification type values used in the EXTRA_NOTIFICATION_TYPE extra.
    public static final int NOTIF_TYPE_TASK_DUE = 1;
    public static final int NOTIF_TYPE_STUDY_REMINDER = 2;

    /**
     * Creates the app's three notification channels. Called once at app
     * startup from StudyPlannerApp.onCreate. No op below Android 8 (API 26).
     */
    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    context.getSystemService(NotificationManager.class);

            // Task deadline reminders. HIGH importance so they pop up.
            NotificationChannel taskChannel = new NotificationChannel(
                    CHANNEL_TASKS,
                    "Task Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            taskChannel.setDescription("Reminders for upcoming assignment and exam deadlines");
            taskChannel.enableVibration(true);
            manager.createNotificationChannel(taskChannel);

            // Pomodoro events. LOW importance so they don't make sound.
            NotificationChannel timerChannel = new NotificationChannel(
                    CHANNEL_TIMER,
                    "Study Timer",
                    NotificationManager.IMPORTANCE_LOW
            );
            timerChannel.setDescription("Notifications for Pomodoro timer sessions");
            manager.createNotificationChannel(timerChannel);

            // General study reminders. DEFAULT importance.
            NotificationChannel studyChannel = new NotificationChannel(
                    CHANNEL_STUDY,
                    "Study Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            studyChannel.setDescription("General study session reminders");
            manager.createNotificationChannel(studyChannel);
        }
    }

    /**
     * Schedules an exact alarm for the given task. When the alarm fires
     * NotificationReceiver builds and shows the deadline notification.
     * The task id is used as the PendingIntent request code so each
     * task has its own alarm and they don't overwrite one another.
     */
    public static void scheduleTaskNotification(Context context, Task task, long triggerAt) {
        // Intent that NotificationReceiver will receive when the alarm fires.
        Intent intent = new Intent(context, NotificationReceiver.class);
        intent.putExtra(EXTRA_TASK_ID, task.getId());
        intent.putExtra(EXTRA_TASK_TITLE, task.getTitle());
        intent.putExtra(EXTRA_NOTIFICATION_TYPE, NOTIF_TYPE_TASK_DUE);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) task.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager =
                (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ requires the SCHEDULE_EXACT_ALARM permission,
                // so check before scheduling.
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent);
            }
        }
    }

    /**
     * Cancels a previously scheduled task alarm. Called when a task is
     * deleted or its notification is turned off. FLAG_NO_CREATE means
     * this is a no op if no alarm was ever scheduled for the task.
     */
    public static void cancelTaskNotification(Context context, long taskId) {
        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                (int) taskId,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            AlarmManager alarmManager =
                    (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    /**
     * Builds and shows the "task due soon" notification. Called from
     * NotificationReceiver when a scheduled deadline alarm fires. Tapping
     * the notification opens MainActivity. The task id is used as the
     * notification id so each task has its own slot in the shade.
     */
    public static void showTaskDueNotification(Context context, long taskId, String taskTitle) {
        // Intent that opens the app when the notification is tapped.
        Intent tapIntent = new Intent(context, MainActivity.class);
        tapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent tapPending = PendingIntent.getActivity(
                context, 0, tapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_TASKS)
                        .setSmallIcon(R.drawable.ic_notification)
                        .setContentTitle("Task Due Soon!")
                        .setContentText(taskTitle + " is due soon")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText("Don't forget: \"" + taskTitle
                                        + "\" is coming up. Open the app to review."))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setAutoCancel(true)
                        .setContentIntent(tapPending)
                        .setVibrate(new long[]{0, 300, 200, 300});

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) taskId, builder.build());
        }
    }

    /**
     * Shows the "Pomodoro complete, take a break" notification. Uses
     * 9000 + pomodoroNumber as the id so each pomodoro gets its own
     * notification instead of replacing the previous one.
     */
    public static void showPomodoroCompleteNotification(Context context, int pomodoroNumber) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_TIMER)
                        .setSmallIcon(R.drawable.ic_timer)
                        .setContentTitle("Pomodoro #" + pomodoroNumber + " Complete! 🎉")
                        .setContentText("Great work! Take a short 5-minute break.")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true)
                        .setVibrate(new long[]{0, 500, 200, 500});

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(9000 + pomodoroNumber, builder.build());
        }
    }

    /**
     * Shows the "break is over" notification. Always uses id 9999 so a
     * new break notification replaces the previous one.
     */
    public static void showBreakCompleteNotification(Context context) {
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, CHANNEL_TIMER)
                        .setSmallIcon(R.drawable.ic_timer)
                        .setContentTitle("Break's Over!")
                        .setContentText("Time to get back to studying. You've got this!")
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setAutoCancel(true);

        NotificationManager manager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(9999, builder.build());
        }
    }
}