package edu.niu.android.smartstudyplanner.ui.planner;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.databinding.FragmentPlannerBinding;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.StudySessionViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class PlannerFragment extends Fragment {

    private FragmentPlannerBinding binding;
    private TaskViewModel taskViewModel;
    private StudySessionViewModel sessionViewModel;
    private CourseViewModel courseViewModel;

    // Course id to name lookup, used to render the course label on each task card.
    private Map<Long, String> courseNameMap = new HashMap<>();

    // One Handler/Runnable pair per visible exam countdown card so each
    // card ticks independently and can be cancelled by id.
    private final Map<Long, Handler>  examHandlers  = new HashMap<>();
    private final Map<Long, Runnable> examRunnables = new HashMap<>();

    // Drives the typewriter animation on the motivational quote.
    private Handler typewriterHandler;
    private Runnable typewriterRunnable;

    // Window for showing exam countdown cards: exams within 48 hours.
    private static final long FORTY_EIGHT_HOURS = 48L * 60 * 60 * 1000;

    // Cap on how many "due today" task rows we render inline before
    // sending the user to the full Due Today screen via "See all".
    private static final int  MAX_TODAY_TASKS   = 2;

    // Pool of motivational quotes; one is picked at random per launch.
    private static final String[] QUOTES = {
            "\"The secret of getting ahead is getting started.\" - Mark Twain",
            "\"It always seems impossible until it's done.\" - Nelson Mandela",
            "\"Don't watch the clock; do what it does. Keep going.\" - Sam Levenson",
            "\"Success is the sum of small efforts repeated.\" - R. Collier",
            "\"You don't have to be great to start, but you have to start to be great.\"",
            "\"Believe you can and you're halfway there.\" - Theodore Roosevelt",
            "\"Study hard, for the well is deep.\" - Richard Baxter",
            "\"Education is the passport to the future.\" - Malcolm X"
    };

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentPlannerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Scope to the activity so the same ViewModel instances are shared
        // across tab switches and the data observers don't re-fire on every
        // return to the Planner.
        taskViewModel    = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        sessionViewModel = new ViewModelProvider(requireActivity()).get(StudySessionViewModel.class);
        courseViewModel  = new ViewModelProvider(requireActivity()).get(CourseViewModel.class);

        setupGreeting();
        setupTypewriterQuote();
        setupToolCards();
        setupSeeAll();
        observeCourses();
        observeStudySessions();
    }

    /**
     * Sets the greeting based on the time of day and shows today's date.
     */
    private void setupGreeting() {
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12)      greeting = "Good Morning! ☀️";
        else if (hour < 17) greeting = "Good Afternoon! 🌤";
        else                greeting = "Good Evening! 🌙";
        binding.textGreeting.setText(greeting);
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault());
        binding.textDate.setText(sdf.format(new Date()));
    }

    /**
     * Picks a random quote and reveals it one character at a time, with
     * a 35ms delay per character and a 600ms initial pause.
     */
    private void setupTypewriterQuote() {
        int idx = (int)(Math.random() * QUOTES.length);
        String quote = QUOTES[idx];
        typewriterHandler = new Handler(Looper.getMainLooper());
        final int[] i = {0};
        typewriterRunnable = new Runnable() {
            @Override public void run() {
                if (binding == null) return;
                if (i[0] <= quote.length()) {
                    binding.textQuote.setText(quote.substring(0, i[0]));
                    i[0]++;
                    typewriterHandler.postDelayed(this, 35);
                }
            }
        };
        typewriterHandler.postDelayed(typewriterRunnable, 600);
    }

    /**
     * Wires up the four tool tiles to their navigation actions.
     */
    private void setupToolCards() {
        binding.cardFlashcards.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_planner_to_flashcards));
        binding.cardGpa.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_planner_to_gpa));
        binding.cardMusic.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_planner_to_music));
        binding.cardTips.setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_planner_to_tips));
    }

    /**
     * "See all" on the Due Today section opens DueTodayActivity.
     */
    private void setupSeeAll() {
        binding.textSeeAllToday.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), DueTodayActivity.class)));
    }

    /**
     * Builds the course id to name lookup, then chains into the tasks
     * observer so course labels resolve correctly on first render.
     */
    private void observeCourses() {
        courseViewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            courseNameMap.clear();
            for (Course c : courses) {
                courseNameMap.put(c.getId(), c.getName());
            }
            observeTasks();
        });
    }

    /**
     * Subscribes to all tasks. On every emit, splits them into the buckets
     * the dashboard needs (today's pending, exams within 48 hours, today's
     * completed for the progress ring) and updates the relevant widgets:
     * progress ring, progress bar, motivational message, exam countdown
     * cards, and the truncated task list.
     */
    private void observeTasks() {
        long[] todayRange = DateUtils.getTodayRange();

        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), allTasks -> {
            if (allTasks == null) return;

            long now        = System.currentTimeMillis();
            long examWindow = now + FORTY_EIGHT_HOURS;

            List<Task> todayTasks     = new ArrayList<>();
            List<Task> examCountdowns = new ArrayList<>();
            List<Task> doneTasks      = new ArrayList<>();

            for (Task t : allTasks) {
                if (t.isCompleted()) { doneTasks.add(t); continue; }
                long due = t.getDueDate();
                if (due > 0) {
                    if (due >= todayRange[0] && due <= todayRange[1])
                        todayTasks.add(t);
                    if (t.getType() == Task.TYPE_EXAM && due > now && due <= examWindow)
                        examCountdowns.add(t);
                }
            }

            examCountdowns.sort((a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));
            todayTasks.sort((a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));

            // Count completed-today separately so the progress ring shows
            // "done out of total" instead of just pending.
            int doneToday  = 0;
            int totalToday = todayTasks.size();
            for (Task t : doneTasks) {
                long due = t.getDueDate();
                if (due > 0 && due >= todayRange[0] && due <= todayRange[1]) {
                    doneToday++; totalToday++;
                }
            }

            binding.progressRing.setProgress(doneToday, Math.max(totalToday, 1));
            binding.textTasksDue.setText(String.valueOf(todayTasks.size()));
            binding.textProgressSub.setText(todayTasks.size() + " tasks remaining today");
            binding.badgeTodayCount.setText(todayTasks.size() + " tasks");

            int percent = totalToday > 0 ? (doneToday * 100 / totalToday) : 0;
            binding.progressBarToday.setProgress(percent);

            // Motivational message tier picked from today's completion percent.
            String msg;
            if (totalToday == 0)        msg = "No tasks today! 🎉";
            else if (percent == 100)    msg = "All done! Amazing! 🎉";
            else if (percent >= 75)     msg = "Almost there! Keep going! 💪";
            else if (percent >= 50)     msg = "Halfway there! 🚀";
            else if (percent >= 25)     msg = "Good start! Keep it up! ⚡";
            else                        msg = "Let's get started! 💪";
            binding.textMotivation.setText(msg);

            renderExamCountdowns(examCountdowns);
            renderTodayTasks(todayTasks);
        });
    }

    /**
     * Cancels any old exam countdown tickers, then builds a card per
     * upcoming exam. Each card has its own Handler/Runnable that ticks
     * once per second to refresh the HRS/MIN/SEC display.
     */
    private void renderExamCountdowns(List<Task> exams) {
        // Cancel previous tickers before rebuilding so we don't leak Runnables.
        for (Map.Entry<Long, Handler> e : examHandlers.entrySet()) {
            Runnable r = examRunnables.get(e.getKey());
            if (r != null) e.getValue().removeCallbacks(r);
        }
        examHandlers.clear();
        examRunnables.clear();
        binding.containerExamCountdowns.removeAllViews();

        if (exams.isEmpty()) {
            binding.sectionExamCountdown.setVisibility(View.GONE);
            return;
        }
        binding.sectionExamCountdown.setVisibility(View.VISIBLE);
        float dp = getResources().getDisplayMetrics().density;

        for (Task exam : exams) {
            LinearLayout card = new LinearLayout(requireContext());
            card.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cardLp.setMargins(0, 0, 0, (int)(12*dp));
            card.setLayoutParams(cardLp);
            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(0xFF0D1757);
            cardBg.setCornerRadius(20*dp);
            card.setBackground(cardBg);
            card.setPadding((int)(18*dp),(int)(18*dp),(int)(18*dp),(int)(18*dp));

            long diff    = exam.getDueDate() - System.currentTimeMillis();
            long hrsLeft = diff / 3600000L;

            TextView header = new TextView(requireContext());
            header.setText("EXAM IN " + hrsLeft + " HRS");
            header.setTextSize(15f);
            header.setTypeface(null, android.graphics.Typeface.BOLD);
            header.setTextColor(0x88FFFFFF);
            LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            hLp.setMargins(0,0,0,(int)(6*dp));
            header.setLayoutParams(hLp);

            TextView taskName = new TextView(requireContext());
            taskName.setText(exam.getTitle());
            taskName.setTextSize(22f);
            taskName.setTypeface(null, android.graphics.Typeface.BOLD);
            taskName.setTextColor(0xFFFFFFFF);
            LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            nLp.setMargins(0,0,0,(int)(3*dp));
            taskName.setLayoutParams(nLp);

            String courseName = getCourseCode(exam.getCourseId());
            TextView courseView = new TextView(requireContext());
            courseView.setText(courseName);
            courseView.setTextSize(18f);
            courseView.setTypeface(null, android.graphics.Typeface.BOLD);
            courseView.setTextColor(0xFF90CAF9);
            LinearLayout.LayoutParams cLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            cLp.setMargins(0,0,0,(int)(3*dp));
            courseView.setLayoutParams(cLp);

            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            TextView dueTime = new TextView(requireContext());
            dueTime.setText("Due " + sdf.format(new Date(exam.getDueDate())));
            dueTime.setTextSize(17f);
            dueTime.setTypeface(null, android.graphics.Typeface.BOLD);
            dueTime.setTextColor(0x88FFFFFF);
            LinearLayout.LayoutParams dLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            dLp.setMargins(0,0,0,(int)(14*dp));
            dueTime.setLayoutParams(dLp);

            // HRS:MIN:SEC blocks. The TextView[1] arrays are out-params so
            // makeTimerBlock can hand back the TextView for the ticker
            // Runnable to update each second.
            LinearLayout timerRow = new LinearLayout(requireContext());
            timerRow.setOrientation(LinearLayout.HORIZONTAL);
            timerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView[] hoursOut = new TextView[1];
            TextView[] minsOut  = new TextView[1];
            TextView[] secsOut  = new TextView[1];

            timerRow.addView(makeTimerBlock(dp, "HRS", hoursOut));
            timerRow.addView(makeSep(dp));
            timerRow.addView(makeTimerBlock(dp, "MIN", minsOut));
            timerRow.addView(makeSep(dp));
            timerRow.addView(makeTimerBlock(dp, "SEC", secsOut));

            card.addView(header);
            card.addView(taskName);
            card.addView(courseView);
            card.addView(dueTime);
            card.addView(timerRow);
            binding.containerExamCountdowns.addView(card);

            // Per second ticker that refreshes this card's HRS/MIN/SEC.
            long targetMs = exam.getDueDate();
            Handler h     = new Handler(Looper.getMainLooper());
            Runnable r    = new Runnable() {
                @Override public void run() {
                    if (binding == null) return;
                    long remaining = targetMs - System.currentTimeMillis();
                    if (remaining <= 0) {
                        hoursOut[0].setText("00");
                        minsOut[0].setText("00");
                        secsOut[0].setText("00");
                        return;
                    }
                    long hrs  = remaining / 3600000L;
                    long mins = (remaining % 3600000L) / 60000L;
                    long secs = (remaining % 60000L) / 1000L;
                    hoursOut[0].setText(String.format(Locale.US, "%02d", hrs));
                    minsOut[0].setText(String.format(Locale.US, "%02d", mins));
                    secsOut[0].setText(String.format(Locale.US, "%02d", secs));
                    header.setText("EXAM IN " + hrs + " HRS");
                    h.postDelayed(this, 1000);
                }
            };
            examHandlers.put(exam.getId(), h);
            examRunnables.put(exam.getId(), r);
            h.post(r);
        }
    }

    /**
     * Builds one HRS/MIN/SEC block: a rounded card with a yellow number
     * on top and a small label below. The number TextView is returned
     * via the numOut out-param so the ticker can keep a reference to it.
     */
    private LinearLayout makeTimerBlock(float dp, String label, TextView[] numOut) {
        LinearLayout block = new LinearLayout(requireContext());
        block.setOrientation(LinearLayout.VERTICAL);
        block.setGravity(android.view.Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF131E5A);
        bg.setCornerRadius(12*dp);
        block.setBackground(bg);
        block.setPadding((int)(12*dp),(int)(8*dp),(int)(12*dp),(int)(8*dp));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(6*dp));
        block.setLayoutParams(lp);

        TextView num = new TextView(requireContext());
        num.setText("00");
        num.setTextSize(24f);
        num.setTypeface(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD);
        num.setTextColor(0xFFFFD600);

        TextView lbl = new TextView(requireContext());
        lbl.setText(label);
        lbl.setTextSize(13f);
        lbl.setTypeface(null, android.graphics.Typeface.BOLD);
        lbl.setTextColor(0x88FFFFFF);

        block.addView(num);
        block.addView(lbl);
        numOut[0] = num;
        return block;
    }

    /**
     * Builds the colon separator placed between HRS, MIN, and SEC blocks.
     */
    private TextView makeSep(float dp) {
        TextView sep = new TextView(requireContext());
        sep.setText(":");
        sep.setTextSize(22f);
        sep.setTypeface(null, android.graphics.Typeface.BOLD);
        sep.setTextColor(0xFFFFD600);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(6*dp));
        sep.setLayoutParams(lp);
        return sep;
    }

    /**
     * Renders the inline "due today" task rows. Caps at MAX_TODAY_TASKS;
     * the rest are reachable through "See all". Shows the empty card if
     * nothing is due today.
     */
    private void renderTodayTasks(List<Task> tasks) {
        binding.containerTodayTasks.removeAllViews();
        if (tasks.isEmpty()) {
            binding.cardTodayEmpty.setVisibility(View.VISIBLE);
            return;
        }
        binding.cardTodayEmpty.setVisibility(View.GONE);
        int count = Math.min(tasks.size(), MAX_TODAY_TASKS);
        for (int i = 0; i < count; i++) {
            binding.containerTodayTasks.addView(buildTaskRow(tasks.get(i)));
        }
    }

    /**
     * Builds a single inline task row: top urgency strip whose color
     * reflects priority and overdue status, then title, course, due
     * time, and the type and priority badges.
     */
    private View buildTaskRow(Task task) {
        float dp = getResources().getDisplayMetrics().density;

        CardView card = new CardView(requireContext());
        CardView.LayoutParams cardLp = new CardView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0,0,0,(int)(12*dp));
        card.setLayoutParams(cardLp);
        card.setRadius(18*dp);
        card.setCardElevation(4*dp);
        card.setCardBackgroundColor(0xFFFFFFFF);

        LinearLayout inner = new LinearLayout(requireContext());
        inner.setOrientation(LinearLayout.VERTICAL);

        View strip = new View(requireContext());
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

        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        int p = (int)(14*dp);
        body.setPadding(p,p,p,p);

        TextView name = new TextView(requireContext());
        name.setText(task.getTitle());
        name.setTextSize(20f);
        name.setTextColor(0xFF0A1245);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        nameLp.setMargins(0,0,0,(int)(4*dp));
        name.setLayoutParams(nameLp);

        TextView courseView = new TextView(requireContext());
        courseView.setText(getCourseCode(task.getCourseId()));
        courseView.setTextSize(18f);
        courseView.setTextColor(stripColor);
        courseView.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams courseLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        courseLp.setMargins(0,0,0,(int)(4*dp));
        courseView.setLayoutParams(courseLp);

        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        TextView sub = new TextView(requireContext());
        sub.setText(task.getPriorityLabel() + " · Due "
                + sdf.format(new Date(task.getDueDate())));
        sub.setTextSize(17f);
        sub.setTextColor(0xFF666688);
        sub.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        subLp.setMargins(0,0,0,(int)(10*dp));
        sub.setLayoutParams(subLp);

        LinearLayout badgeRow = new LinearLayout(requireContext());
        badgeRow.setOrientation(LinearLayout.HORIZONTAL);

        badgeRow.addView(makeBadge(task.getTypeLabel(), task.getType(), true, dp));
        badgeRow.addView(makeBadge(task.getPriorityLabel(), task.getPriority(), false, dp));

        body.addView(name);
        body.addView(courseView);
        body.addView(sub);
        body.addView(badgeRow);

        inner.addView(strip);
        inner.addView(body);
        card.addView(inner);

        return card;
    }

    /**
     * Builds a rounded pill badge. The isType flag picks between the
     * type palette (Exam/Quiz/Project/Assignment/Other) and the priority
     * palette (High/Medium/Low).
     */
    private TextView makeBadge(String text, int value, boolean isType, float dp) {
        TextView badge = new TextView(requireContext());
        badge.setText(text);
        badge.setTextSize(16f);
        badge.setTypeface(null, android.graphics.Typeface.BOLD);
        int bh = (int)(14*dp), bv = (int)(6*dp);
        badge.setPadding(bh, bv, bh, bv);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(20*dp);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMarginEnd((int)(8*dp));
        badge.setLayoutParams(lp);

        if (isType) {
            switch (value) {
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
        } else {
            switch (value) {
                case Task.PRIORITY_HIGH:
                    bg.setColor(0xFFFFF1F2); badge.setTextColor(0xFF9F1239); break;
                case Task.PRIORITY_LOW:
                    bg.setColor(0xFFDCFCE7); badge.setTextColor(0xFF065F46); break;
                default:
                    bg.setColor(0xFFFFF3E0); badge.setTextColor(0xFFE65100); break;
            }
        }
        badge.setBackground(bg);
        return badge;
    }

    /**
     * Returns the course code portion of the stored "CODE - Name" string,
     * or empty string if the course can't be resolved.
     */
    private String getCourseCode(long courseId) {
        if (courseNameMap.containsKey(courseId)) {
            String full = courseNameMap.get(courseId);
            if (full != null && full.contains(" - "))
                return full.substring(0, full.indexOf(" - "));
            return full != null ? full : "";
        }
        return "";
    }

    /**
     * Subscribes to the today and this-week study minute totals and
     * updates the two summary chips. Formats as "Nh", "N.Nh", or "Nm"
     * depending on the size of the value.
     */
    private void observeStudySessions() {
        long[] todayRange = DateUtils.getTodayRange();
        long[] weekRange  = DateUtils.getWeekRange();

        sessionViewModel.getTotalMinutesInRange(todayRange[0], todayRange[1])
                .observe(getViewLifecycleOwner(), mins -> {
                    if (binding == null) return;
                    if (mins == null) mins = 0L;
                    float hours = mins / 60f;
                    binding.textTodayHours.setText(
                            hours == 0 ? "0h" : hours < 1 ? mins + "m" :
                                    String.format(Locale.US, "%.1fh", hours));
                });

        sessionViewModel.getTotalMinutesInRange(weekRange[0], weekRange[1])
                .observe(getViewLifecycleOwner(), mins -> {
                    if (binding == null) return;
                    if (mins == null) mins = 0L;
                    float hours = mins / 60f;
                    binding.textWeekHours.setText(
                            hours == 0 ? "0h" : hours < 1 ? mins + "m" :
                                    String.format(Locale.US, "%.1fh", hours));
                });
    }

    /**
     * Cancels every running ticker (exam countdowns, typewriter quote)
     * and clears the binding. Required because each Runnable holds a
     * reference to fragment views, so leaving them scheduled would leak.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        for (Map.Entry<Long, Handler> e : examHandlers.entrySet()) {
            Runnable r = examRunnables.get(e.getKey());
            if (r != null) e.getValue().removeCallbacks(r);
        }
        examHandlers.clear();
        examRunnables.clear();
        if (typewriterHandler != null && typewriterRunnable != null)
            typewriterHandler.removeCallbacks(typewriterRunnable);
        binding = null;
    }
}