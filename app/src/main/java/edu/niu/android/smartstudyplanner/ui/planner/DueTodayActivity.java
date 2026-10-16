package edu.niu.android.smartstudyplanner.ui.planner;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.lifecycle.ViewModelProvider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.databinding.ActivityDueTodayBinding;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class DueTodayActivity extends AppCompatActivity {

    private ActivityDueTodayBinding binding;
    private TaskViewModel taskViewModel;
    private CourseViewModel courseViewModel;

    // Lookup of course id to name, used to render the course label on each card.
    private Map<Long, String> courseNameMap = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityDueTodayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }

        // Tinting the back arrow has to happen after the toolbar lays out
        // so getNavigationIcon() actually returns it.
        binding.toolbar.post(() -> {
            if (binding.toolbar.getNavigationIcon() != null) {
                binding.toolbar.getNavigationIcon().setTint(0xFFFFFFFF);
            }
        });

        // We don't navigate from here. Setting the holder and finishing
        // lets MainActivity.onResume switch tabs in one step, which avoids
        // a flash of the Planner tab between this screen and Tasks.
        binding.btnManageTasks.setOnClickListener(v -> {
            NavigationRequest.navigateTo = R.id.navigation_tasks;
            finish();
        });

        taskViewModel   = new ViewModelProvider(this).get(TaskViewModel.class);
        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        // Build the course id to name lookup before listing tasks so the
        // course label resolves correctly on the very first render.
        courseViewModel.getAllCourses().observe(this, courses -> {
            courseNameMap.clear();
            for (Course c : courses) courseNameMap.put(c.getId(), c.getName());
            loadTodayTasks();
        });
    }

    /**
     * Subscribes to all tasks, filters down to the ones that are still
     * pending and due within today's range, sorts them by due time, and
     * rebuilds the list of cards.
     */
    private void loadTodayTasks() {
        long[] todayRange = DateUtils.getTodayRange();
        taskViewModel.getAllTasks().observe(this, allTasks -> {
            if (allTasks == null) return;
            List<Task> todayTasks = new ArrayList<>();
            for (Task t : allTasks) {
                if (!t.isCompleted()) {
                    long due = t.getDueDate();
                    if (due >= todayRange[0] && due <= todayRange[1])
                        todayTasks.add(t);
                }
            }
            todayTasks.sort((a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));
            binding.textSubtitle.setText(todayTasks.size() + " task"
                    + (todayTasks.size() == 1 ? "" : "s") + " due today");
            binding.containerDueToday.removeAllViews();
            for (Task task : todayTasks) {
                binding.containerDueToday.addView(buildTaskCard(task));
            }
        });
    }

    /**
     * Builds a single task card in code: a colored urgency strip on top,
     * then title, course name, due time, and the type and priority
     * badges. The strip color reflects priority and "overdue" status.
     */
    private View buildTaskCard(Task task) {
        float dp = getResources().getDisplayMetrics().density;

        CardView card = new CardView(this);
        CardView.LayoutParams cardLp = new CardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, (int)(12*dp));
        card.setLayoutParams(cardLp);
        card.setRadius(18*dp);
        card.setCardElevation(4*dp);
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);

        // Top strip color, picked from the priority and overdue state.
        View strip = new View(this);
        strip.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, (int)(10*dp)));
        boolean isOverdue = task.getDueDate() < System.currentTimeMillis();
        int stripColor;
        if (isOverdue || task.getPriority() == Task.PRIORITY_HIGH)
            stripColor = 0xFF9F1239;
        else if (task.getPriority() == Task.PRIORITY_LOW)
            stripColor = 0xFF065F46;
        else
            stripColor = 0xFF0369A1;
        strip.setBackgroundColor(stripColor);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(14*dp);
        body.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText(task.getTitle());
        title.setTextSize(20f);
        title.setTextColor(0xFF0A1245);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        titleLp.setMargins(0, 0, 0, (int)(4*dp));
        title.setLayoutParams(titleLp);

        // Show only the course code part of the stored "CODE - Name" string.
        String courseName = "";
        if (courseNameMap.containsKey(task.getCourseId())) {
            String full = courseNameMap.get(task.getCourseId());
            if (full != null && full.contains(" - "))
                courseName = full.substring(0, full.indexOf(" - "));
            else courseName = full != null ? full : "";
        }
        TextView courseView = new TextView(this);
        courseView.setText(courseName);
        courseView.setTextSize(18f);
        courseView.setTextColor(stripColor);
        courseView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams courseLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        courseLp.setMargins(0, 0, 0, (int)(4*dp));
        courseView.setLayoutParams(courseLp);

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        TextView sub = new TextView(this);
        sub.setText(task.getPriorityLabel() + " · Due "
                + sdf.format(new Date(task.getDueDate())));
        sub.setTextSize(17f);
        sub.setTextColor(0xFF666688);
        sub.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0, 0, 0, (int)(10*dp));
        sub.setLayoutParams(subLp);

        LinearLayout badgeRow = new LinearLayout(this);
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);
        badgeRow.addView(makeTypeBadge(task.getTypeLabel(), task.getType(), dp));
        badgeRow.addView(makePrioBadge(task.getPriorityLabel(), task.getPriority(), dp));

        body.addView(title);
        body.addView(courseView);
        body.addView(sub);
        body.addView(badgeRow);
        inner.addView(strip);
        inner.addView(body);
        card.addView(inner);
        return card;
    }

    /**
     * Builds the rounded "type" pill (Exam, Quiz, Project, Assignment,
     * Other) with its own background and text color.
     */
    private TextView makeTypeBadge(String text, int type, float dp) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(16f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        int bh = (int)(14*dp), bv = (int)(6*dp);
        badge.setPadding(bh, bv, bh, bv);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(20*dp);
        switch (type) {
            case Task.TYPE_EXAM:
                bg.setColor(0xFFFFF1F2); badge.setTextColor(0xFF9F1239); break;
            case Task.TYPE_QUIZ:
                bg.setColor(0xFFE8EAF6); badge.setTextColor(0xFF283593); break;
            case Task.TYPE_PROJECT:
                bg.setColor(0xFFE8F5E9); badge.setTextColor(0xFF1B5E20); break;
            case Task.TYPE_ASSIGNMENT:
                bg.setColor(0xFFFFF3E0); badge.setTextColor(0xFFE65100); break;
            default:
                bg.setColor(0xFFEEEEEE); badge.setTextColor(0xFF555555); break;
        }
        badge.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(8*dp));
        badge.setLayoutParams(lp);
        return badge;
    }

    /**
     * Builds the rounded priority pill (High, Medium, Low) with its own
     * background and text color.
     */
    private TextView makePrioBadge(String text, int priority, float dp) {
        TextView badge = new TextView(this);
        badge.setText(text);
        badge.setTextSize(16f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        int bh = (int)(14*dp), bv = (int)(6*dp);
        badge.setPadding(bh, bv, bh, bv);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(20*dp);
        switch (priority) {
            case Task.PRIORITY_HIGH:
                bg.setColor(0xFFFFF1F2); badge.setTextColor(0xFF9F1239); break;
            case Task.PRIORITY_LOW:
                bg.setColor(0xFFDCFCE7); badge.setTextColor(0xFF065F46); break;
            default:
                bg.setColor(0xFFFFF3E0); badge.setTextColor(0xFFE65100); break;
        }
        badge.setBackground(bg);
        return badge;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}