/**************************************************************
 * Purpose: Date and time helpers used across the app. Holds  *
 * the shared SimpleDateFormat instances, builds today/week   *
 * timestamp ranges for Room queries, formats durations and   *
 * countdown displays, and produces relative due date labels  *
 * like "Due Today" or "Overdue 2d".                          *
 **************************************************************/

package edu.niu.android.smartstudyplanner.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    // Shared format objects, built once and reused so we aren't allocating
    // a new SimpleDateFormat on every format call.

    public static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy", Locale.US);

    public static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("MMM dd, yyyy h:mm a", Locale.US);

    public static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("h:mm a", Locale.US);

    public static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("EEE", Locale.US);

    public static final SimpleDateFormat SHORT_DATE_FORMAT =
            new SimpleDateFormat("MM/dd", Locale.US);

    /**
     * Formats a timestamp as a date like "Jan 15, 2025".
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp as a date and time like "Jan 15, 2025 10:30 AM".
     */
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp as a time like "10:30 AM".
     */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp as a 3 letter weekday like "Mon" or "Tue".
     */
    public static String formatDayOfWeek(long timestamp) {
        return DAY_FORMAT.format(new Date(timestamp));
    }

    /**
     * Formats a timestamp as a short numeric date like "01/15".
     */
    public static String formatShortDate(long timestamp) {
        return SHORT_DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Returns [startOfToday, endOfToday] in millis, covering midnight
     * through 23:59:59 of the current day. Used by today-range queries
     * across the app.
     */
    public static long[] getTodayRange() {
        Calendar cal = Calendar.getInstance();

        // Set to 00:00:00.000 of today.
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        // Bump to 23:59:59 of the same day.
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }

    /**
     * Returns [startOfWeek, endOfWeek] in millis, from Sunday midnight
     * to Saturday 23:59:59 of the current calendar week.
     */
    public static long[] getWeekRange() {
        Calendar cal = Calendar.getInstance();

        // Walk back to Sunday and clamp to midnight.
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        // Jump six days forward to Saturday and clamp to end of day.
        cal.add(Calendar.DAY_OF_WEEK, 6);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        return new long[]{start, end};
    }

    /**
     * Returns the millis timestamp for the start (midnight) of the day
     * that was the given number of days ago. Used to bound "last N days"
     * queries on the Stats screen.
     */
    public static long getStartOfDaysAgo(int days) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -days);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * Returns a human label describing how far away a due date is.
     * Examples: "Due Today", "Due Tomorrow", "In 3 days", "Overdue 2d".
     */
    public static String getRelativeDueDate(long dueDate) {
        long[] today = getTodayRange();

        // Tomorrow's range starts the millisecond after today ends and
        // covers the next 24 hours.
        long[] tomorrow = {today[1] + 1, today[1] + 86_400_000L};

        if (dueDate >= today[0] && dueDate <= today[1]) {
            return "Due Today";
        }
        if (dueDate >= tomorrow[0] && dueDate <= tomorrow[1]) {
            return "Due Tomorrow";
        }
        if (dueDate < today[0]) {
            // Past due: count whole days between then and the start of today.
            long daysPast = (today[0] - dueDate) / 86_400_000L;
            return "Overdue " + daysPast + "d";
        }

        // Future: count whole days between the end of today and the due date.
        long daysUntil = (dueDate - today[1]) / 86_400_000L;
        return "In " + daysUntil + " days";
    }

    /**
     * Formats a duration in seconds as a short readable string like
     * "1h 30m", "45m 12s", or "30s". Used by session lists and stats.
     */
    public static String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;

        if (h > 0) return String.format(Locale.US, "%dh %02dm", h, m);
        if (m > 0) return String.format(Locale.US, "%dm %02ds", m, s);
        return String.format(Locale.US, "%ds", s);
    }

    /**
     * Formats a number of seconds as MM:SS, the format the Pomodoro
     * timer uses for its big countdown display.
     */
    public static String formatTimerDisplay(long seconds) {
        long m = seconds / 60;
        long s = seconds % 60;
        return String.format(Locale.US, "%02d:%02d", m, s);
    }
}