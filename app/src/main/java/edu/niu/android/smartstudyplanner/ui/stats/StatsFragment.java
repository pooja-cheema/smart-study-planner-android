/**************************************************************
 * Purpose: Stats tab. Aggregates the user's study history    *
 * into summary cards (today, this week, sessions, streak), a *
 * 7 day daily bar chart, a 4 week heatmap of study activity, *
 * a per course time distribution, and a tasks donut chart    *
 * (completed, pending, overdue). Charts are drawn with the   *
 * MPAndroidChart library; the heatmap and course bars are    *
 * built programmatically.                                    *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.stats;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.databinding.FragmentStatsBinding;
import edu.niu.android.smartstudyplanner.utils.ColorUtils;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.StudySessionViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class StatsFragment extends Fragment {

    private FragmentStatsBinding binding;
    private StudySessionViewModel sessionViewModel;
    private CourseViewModel courseViewModel;
    private TaskViewModel taskViewModel;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionViewModel = new ViewModelProvider(this).get(StudySessionViewModel.class);
        courseViewModel  = new ViewModelProvider(this).get(CourseViewModel.class);
        taskViewModel    = new ViewModelProvider(this).get(TaskViewModel.class);

        // Each loader populates one section of the screen independently.
        loadSummaryStats();
        loadDailyBarChart();
        loadHeatmap();
        loadCourseBars();
        loadTaskDonut();
    }

    // Summary stats

    /**
     * Fills the four summary cards at the top of the screen: today's
     * study time, this week's study time, sessions this week, and the
     * current daily streak. Each card pulls its own number through a
     * background callback and posts back to the UI thread.
     */
    private void loadSummaryStats() {
        long[] today = DateUtils.getTodayRange();
        long[] week  = DateUtils.getWeekRange();

        // Today's total, formatted as Xh Ym.
        sessionViewModel.getTotalStudyHours(today[0], seconds -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                    binding.textTodayTotal.setText(DateUtils.formatDuration(seconds)));
        });

        // This week's total, formatted by hand so we can drop the "h"
        // when the value is under an hour.
        sessionViewModel.getTotalStudyHours(week[0], seconds -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                long minutes      = seconds / 60;
                long hours        = minutes / 60;
                long remainingMin = minutes % 60;
                String hoursStr = hours > 0
                        ? hours + "h " + remainingMin + "m"
                        : remainingMin + "m";
                binding.textWeekHours.setText(hoursStr);
            });
        });

        // Number of sessions this week.
        sessionViewModel.getSessionsInRangeAsync(week[0], week[1], sessions -> {
            if (!isAdded()) return;
            requireActivity().runOnUiThread(() ->
                    binding.textWeekSessions.setText(
                            sessions.size() + " session"
                                    + (sessions.size() == 1 ? "" : "s")));
        });

        // Look back 30 days for the streak so a long break can break it.
        sessionViewModel.getSessionsSince(DateUtils.getStartOfDaysAgo(29), sessions -> {
            if (!isAdded()) return;
            int streak = calculateStreak(sessions);
            requireActivity().runOnUiThread(() ->
                    binding.textStreak.setText(streak + " 🔥"));
        });
    }

    /**
     * Counts the streak of consecutive days (working backward from today)
     * on which at least one session was recorded. Sessions are bucketed
     * by their yyyy-MM-dd start date so multiple sessions on the same
     * day count as one studied day.
     */
    private int calculateStreak(List<StudySession> sessions) {
        if (sessions == null || sessions.isEmpty()) return 0;

        // Build the set of yyyy-MM-dd days the user has studied.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Set<String> studiedDays = new HashSet<>();
        for (StudySession s : sessions) {
            studiedDays.add(sdf.format(new Date(s.getStartTime())));
        }

        // Walk backward day by day and stop at the first gap.
        int streak = 0;
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < 30; i++) {
            if (studiedDays.contains(sdf.format(cal.getTime()))) {
                streak++;
                cal.add(Calendar.DAY_OF_YEAR, -1);
            } else {
                break;
            }
        }
        return streak;
    }

    // Daily bar chart

    /**
     * Builds the 7 day study time bar chart. Pulls the last week's
     * sessions, totals their hours per weekday slot, and configures
     * MPAndroidChart with the sized, bold styling used elsewhere on the
     * screen.
     */
    private void loadDailyBarChart() {
        long sevenDaysAgo = DateUtils.getStartOfDaysAgo(6);

        sessionViewModel.getSessionsSince(sevenDaysAgo, sessions -> {
            if (!isAdded()) return;

            // Initialize all seven weekday buckets to 0 so the chart
            // shows empty bars for days with no sessions.
            Map<Integer, Float> dailyHours = new HashMap<>();
            for (int i = 0; i < 7; i++) dailyHours.put(i, 0f);

            // Accumulate hours into whichever weekday each session started on.
            for (StudySession s : sessions) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(s.getStartTime());
                int dayIndex = cal.get(Calendar.DAY_OF_WEEK) - 1;
                float cur = dailyHours.getOrDefault(dayIndex, 0f);
                dailyHours.put(dayIndex, cur + (s.getDurationSeconds() / 3600f));
            }

            // Convert the bucket map into MPAndroidChart's BarEntry list,
            // tracking the max so the y axis can scale to it.
            List<BarEntry> entries = new ArrayList<>();
            float maxHours = 0f;
            for (int i = 0; i < 7; i++) {
                float h = dailyHours.getOrDefault(i, 0f);
                entries.add(new BarEntry(i, h));
                if (h > maxHours) maxHours = h;
            }

            final float finalMax = maxHours;

            requireActivity().runOnUiThread(() -> {
                BarChart chart = binding.barChartDaily;
                String[] labels = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};

                // Bar styling: deep blue, large bold value labels.
                BarDataSet dataSet = new BarDataSet(entries, "Study Hours");
                dataSet.setColor(0xFF0369A1);
                dataSet.setValueTextSize(15f);
                dataSet.setValueTextColor(Color.parseColor("#0A1245"));
                dataSet.setValueTypeface(Typeface.DEFAULT_BOLD);

                BarData data = new BarData(dataSet);
                data.setBarWidth(0.6f);

                // Strip the chart of borders/legend/grid so it reads cleanly.
                chart.setData(data);
                chart.getDescription().setEnabled(false);
                chart.getLegend().setEnabled(false);
                chart.setDrawGridBackground(false);
                chart.setDrawBorders(false);
                chart.setExtraBottomOffset(8f);

                // Y axis: only the left side, minimum height of 4h so the
                // chart never looks empty on very low study days.
                chart.getAxisRight().setEnabled(false);
                chart.getAxisLeft().setAxisMinimum(0f);
                chart.getAxisLeft().setAxisMaximum(Math.max(4f, finalMax * 1.2f));
                chart.getAxisLeft().setGranularity(0.5f);
                chart.getAxisLeft().setTextSize(15f);
                chart.getAxisLeft().setTextColor(Color.parseColor("#0A1245"));
                chart.getAxisLeft().setTypeface(Typeface.DEFAULT_BOLD);

                // X axis: weekday labels along the bottom.
                XAxis xAxis = chart.getXAxis();
                xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
                xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                xAxis.setGranularity(1f);
                xAxis.setDrawGridLines(false);
                xAxis.setTextSize(15f);
                xAxis.setTextColor(Color.parseColor("#0A1245"));
                xAxis.setTypeface(Typeface.DEFAULT_BOLD);

                chart.animateY(1000);
                chart.invalidate();
            });
        });
    }

    // Heatmap

    /**
     * Loads the last 28 days of sessions, totals their hours per day,
     * and triggers the heatmap rebuild on the UI thread.
     */
    private void loadHeatmap() {
        sessionViewModel.getSessionsSince(DateUtils.getStartOfDaysAgo(27), sessions -> {
            if (!isAdded()) return;

            // Bucket each session's hours into its yyyy-MM-dd start day.
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Map<String, Float> dayHours = new HashMap<>();
            for (StudySession s : sessions) {
                String day = sdf.format(new Date(s.getStartTime()));
                float cur  = dayHours.getOrDefault(day, 0f);
                dayHours.put(day, cur + (s.getDurationSeconds() / 3600f));
            }

            // Walk forward 28 days starting 27 days ago so the day list is
            // ordered oldest to newest and includes every slot in the grid.
            List<String> days = new ArrayList<>();
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -27);
            for (int i = 0; i < 28; i++) {
                days.add(sdf.format(cal.getTime()));
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }

            requireActivity().runOnUiThread(() -> buildHeatmap(days, dayHours));
        });
    }

    /**
     * Renders the 4 row by 7 column heatmap. Each cell is colored by how
     * many hours were studied that day, going from very pale blue (none)
     * to deep blue (2+ hours).
     */
    private void buildHeatmap(List<String> days, Map<String, Float> dayHours) {
        LinearLayout container = binding.layoutHeatmap;
        container.removeAllViews();

        float dp    = getResources().getDisplayMetrics().density;
        int dotSize = (int)(30 * dp);
        int gap     = (int)(5 * dp);

        for (int row = 0; row < 4; row++) {
            // Each row is its own horizontal layout so the cells line up.
            LinearLayout rowLayout = new LinearLayout(requireContext());
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, gap);
            rowLayout.setLayoutParams(rowLp);

            for (int col = 0; col < 7; col++) {
                int idx = row * 7 + col;
                if (idx >= days.size()) break;

                // Pick the cell color from the day's hours bucket.
                float hours = dayHours.getOrDefault(days.get(idx), 0f);
                int color;
                if (hours == 0)      color = 0xFFE0F2FE;
                else if (hours < 1f) color = 0xFFBAE6FD;
                else if (hours < 2f) color = 0xFF7DD3FC;
                else                 color = 0xFF0369A1;

                // Rounded square drawn as a tinted GradientDrawable on a View.
                View dot = new View(requireContext());
                android.graphics.drawable.GradientDrawable bg =
                        new android.graphics.drawable.GradientDrawable();
                bg.setColor(color);
                bg.setCornerRadius(6 * dp);
                dot.setBackground(bg);

                LinearLayout.LayoutParams dotLp =
                        new LinearLayout.LayoutParams(dotSize, dotSize);
                dotLp.setMarginEnd(gap);
                dot.setLayoutParams(dotLp);
                rowLayout.addView(dot);
            }

            container.addView(rowLayout);
        }
    }

    // Course bars

    /**
     * Subscribes to the courses list and queries each course's last 30
     * days of study seconds in parallel. Once every callback has come
     * back, the bars are rendered on the UI thread. The pending counter
     * is what tells us "every async result has arrived".
     */
    private void loadCourseBars() {
        long thirtyDaysAgo = DateUtils.getStartOfDaysAgo(29);

        courseViewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            if (courses == null || courses.isEmpty()) {
                binding.textNoCourseData.setVisibility(View.VISIBLE);
                return;
            }

            Map<Course, Float> courseHours = new HashMap<>();
            final int[] pending = {courses.size()};

            // Fan out one query per course.
            for (Course course : courses) {
                sessionViewModel.getCourseStudySeconds(
                        course.getId(), thirtyDaysAgo, seconds -> {
                            // synchronized because callbacks may arrive on
                            // different threads from the executor.
                            synchronized (courseHours) {
                                courseHours.put(course, seconds / 3600f);
                            }
                            pending[0]--;
                            if (pending[0] == 0 && isAdded()) {
                                requireActivity().runOnUiThread(() ->
                                        buildCourseBars(courseHours));
                            }
                        });
            }
        });
    }

    /**
     * Builds the per course study time bars. Sorts highest to lowest so
     * the heaviest course is on top, scales each bar's width relative
     * to the leader, and tints each bar with the course's stored color.
     */
    private void buildCourseBars(Map<Course, Float> courseHours) {
        LinearLayout container = binding.layoutCourseBars;
        container.removeAllViews();
        binding.textNoCourseData.setVisibility(View.GONE);

        float dp = getResources().getDisplayMetrics().density;

        // Top padding so there's air between the heading and the first bar.
        container.setPadding(0, (int)(12 * dp), 0, 0);

        // Sort highest to lowest so the heaviest study load sits on top.
        List<Map.Entry<Course, Float>> sorted = new ArrayList<>(courseHours.entrySet());
        sorted.sort((a, b) -> Float.compare(b.getValue(), a.getValue()));

        // Find the largest value so every bar can be scaled relative to it.
        float maxHours = 0f;
        for (Map.Entry<Course, Float> e : sorted) {
            if (e.getValue() > maxHours) maxHours = e.getValue();
        }
        if (maxHours == 0f) maxHours = 1f;

        for (Map.Entry<Course, Float> entry : sorted) {
            Course course = entry.getKey();
            float  hours  = entry.getValue();
            int    color  = ColorUtils.parseColor(course.getColor());

            // Show only the course code, not the full "CODE - Name".
            String fullName = course.getName();
            String code = fullName.contains(" - ")
                    ? fullName.substring(0, fullName.indexOf(" - "))
                    : fullName;

            // One row: course code, bar, hours number.
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            rowLp.setMargins(0, 0, 0, (int)(14 * dp));
            row.setLayoutParams(rowLp);

            // Course code label on the left.
            TextView nameView = new TextView(requireContext());
            nameView.setText(code);
            nameView.setTextSize(17f);
            nameView.setTypeface(Typeface.DEFAULT_BOLD);
            nameView.setTextColor(hours > 0 ? color : 0xFF999999);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                    (int)(78 * dp), LinearLayout.LayoutParams.WRAP_CONTENT);
            nameView.setLayoutParams(nameLp);
            row.addView(nameView);

            // Background track for the bar (full width, pale blue).
            android.widget.FrameLayout barBg =
                    new android.widget.FrameLayout(requireContext());
            LinearLayout.LayoutParams barBgLp =
                    new LinearLayout.LayoutParams(0, (int)(10 * dp), 1f);
            barBgLp.setMarginEnd((int)(10 * dp));
            barBg.setLayoutParams(barBgLp);
            android.graphics.drawable.GradientDrawable bgShape =
                    new android.graphics.drawable.GradientDrawable();
            bgShape.setColor(0xFFE0F4FF);
            bgShape.setCornerRadius(5 * dp);
            barBg.setBackground(bgShape);

            // Foreground fill bar; width is set in barBg.post() below
            // because we need the parent's measured width first.
            View barFill = new View(requireContext());
            final float pct = hours / maxHours;
            android.widget.FrameLayout.LayoutParams fillLp =
                    new android.widget.FrameLayout.LayoutParams(
                            0, android.widget.FrameLayout.LayoutParams.MATCH_PARENT);
            barFill.setLayoutParams(fillLp);
            android.graphics.drawable.GradientDrawable fillShape =
                    new android.graphics.drawable.GradientDrawable();
            fillShape.setColor(hours > 0 ? color : 0xFFCCCCCC);
            fillShape.setCornerRadius(5 * dp);
            barFill.setBackground(fillShape);
            barBg.addView(barFill);

            // Once the parent FrameLayout has been laid out, set the fill
            // width as a percentage of the available space.
            barBg.post(() -> {
                int totalW = barBg.getWidth();
                android.widget.FrameLayout.LayoutParams lp =
                        (android.widget.FrameLayout.LayoutParams)
                                barFill.getLayoutParams();
                lp.width = (int)(totalW * pct);
                barFill.setLayoutParams(lp);
            });

            row.addView(barBg);

            // Hours number on the right, tinted to match the bar.
            TextView hrsView = new TextView(requireContext());
            hrsView.setText(hours > 0 ? String.format("%.1fh", hours) : "0h");
            hrsView.setTextSize(17f);
            hrsView.setTypeface(Typeface.DEFAULT_BOLD);
            hrsView.setTextColor(hours > 0 ? color : 0xFF999999);
            hrsView.setLayoutParams(new LinearLayout.LayoutParams(
                    (int)(43 * dp), LinearLayout.LayoutParams.WRAP_CONTENT));
            row.addView(hrsView);

            container.addView(row);
        }
    }

    // Task donut

    /**
     * Subscribes to the three task buckets (completed, pending, overdue)
     * and rebuilds the donut whenever any of them changes. Each LiveData
     * is observed in turn so we always have the latest value of all three
     * when calling buildTaskDonut.
     */
    private void loadTaskDonut() {
        taskViewModel.getCompletedTasks().observe(getViewLifecycleOwner(), completed -> {
            int c = completed != null ? completed.size() : 0;
            binding.textTasksCompleted.setText(String.valueOf(c));

            taskViewModel.getPendingTasks().observe(getViewLifecycleOwner(), pending -> {
                int p = pending != null ? pending.size() : 0;
                binding.textTasksPending.setText(String.valueOf(p));

                taskViewModel.getOverdueTasks().observe(getViewLifecycleOwner(), overdue -> {
                    int o = overdue != null ? overdue.size() : 0;
                    binding.textTasksOverdue.setText(String.valueOf(o));
                    buildTaskDonut(c, p, o);
                });
            });
        });
    }

    /**
     * Renders the tasks donut. Shows three slices when data exists; when
     * there are no tasks at all, draws a single neutral ring so the chart
     * still has something to display. The center text is the percent of
     * tasks the user has completed.
     */
    private void buildTaskDonut(int completed, int pending, int overdue) {
        if (!isAdded()) return;

        PieChart chart = binding.pieChartTasks;
        List<PieEntry> entries = new ArrayList<>();
        List<Integer>  colors  = new ArrayList<>();

        // Add a slice per non zero bucket so empty buckets don't produce zero width slivers.
        if (completed > 0) { entries.add(new PieEntry(completed, "")); colors.add(0xFF065F46); }
        if (pending   > 0) { entries.add(new PieEntry(pending,   "")); colors.add(0xFF0369A1); }
        if (overdue   > 0) { entries.add(new PieEntry(overdue,   "")); colors.add(0xFFC62828); }

        // No tasks at all: draw a neutral placeholder ring.
        if (entries.isEmpty()) {
            entries.add(new PieEntry(1, ""));
            colors.add(0xFFE0E4FF);
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setDrawValues(false);
        dataSet.setSliceSpace(3f);

        PieData data = new PieData(dataSet);
        chart.setData(data);
        chart.setHoleRadius(55f);
        chart.setTransparentCircleRadius(58f);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);

        // Center text shows the completion percent.
        int total = completed + pending + overdue;
        int pct   = total > 0 ? (completed * 100 / total) : 0;
        chart.setCenterText(pct + "%");
        chart.setCenterTextSize(18f);
        chart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        chart.setCenterTextColor(0xFF065F46);

        chart.animateY(800);
        chart.invalidate();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}