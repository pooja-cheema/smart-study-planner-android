/**************************************************************
 * Purpose: Timer tab summary screen. Shows the current mode  *
 * (Focus, Short Break, Long Break) with a static minute      *
 * preview, four pomodoro dots, today's and this week's study *
 * totals, and a list of recent sessions. The actual timer    *
 * runs inside StudyTimerActivity, which is launched by the   *
 * "Start Study Session" button.                              *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.timer;

import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.databinding.FragmentTimerBinding;
import edu.niu.android.smartstudyplanner.ui.stats.SessionAdapter;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.StudySessionViewModel;

public class TimerFragment extends Fragment {

    private FragmentTimerBinding binding;
    private StudySessionViewModel viewModel;
    private CourseViewModel courseViewModel;
    private SessionAdapter adapter;

    // Currently selected mode pill (0 = Focus, 1 = Short Break, 2 = Long Break).
    private int currentMode = 0;

    // Default minutes shown for each mode preview.
    private static final int[]    MODE_MINUTES = {25, 5, 15};

    // Label shown under the timer display for each mode.
    private static final String[] MODE_LABELS  = {"FOCUS TIME", "SHORT BREAK", "LONG BREAK"};

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTimerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel       = new ViewModelProvider(this).get(StudySessionViewModel.class);
        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        setupModePills();
        setupDots();
        setupStartButton();
        setupRecentSessions();
        observeCourses();
        loadStats();
    }

    // Mode pills

    /**
     * Wires up the three mode pills. Defaults to Focus on first display.
     */
    private void setupModePills() {
        binding.pillPomodoro.setOnClickListener(v -> switchMode(0));
        binding.pillShortBreak.setOnClickListener(v -> switchMode(1));
        binding.pillLongBreak.setOnClickListener(v -> switchMode(2));
        switchMode(0);
    }

    /**
     * Updates the visible mode preview: timer label, minute readout, and
     * the active/inactive styling on the three pills.
     */
    private void switchMode(int mode) {
        currentMode = mode;

        // Update the static preview shown above the start button.
        int mins = MODE_MINUTES[mode];
        binding.textTimerDisplay.setText(String.format("%02d:00", mins));
        binding.textTimerLabel.setText(MODE_LABELS[mode]);

        // Pill backgrounds: only the chosen mode uses the active drawable.
        binding.pillPomodoro.setBackgroundResource(
                mode == 0 ? R.drawable.timer_pill_active : R.drawable.timer_pill_inactive);
        binding.pillShortBreak.setBackgroundResource(
                mode == 1 ? R.drawable.timer_pill_active : R.drawable.timer_pill_inactive);
        binding.pillLongBreak.setBackgroundResource(
                mode == 2 ? R.drawable.timer_pill_active : R.drawable.timer_pill_inactive);

        // Inactive pill text is dimmed so the active one stands out.
        binding.pillPomodoro.setTextColor(mode == 0   ? 0xFFFFFFFF : 0x88FFFFFF);
        binding.pillShortBreak.setTextColor(mode == 1 ? 0xFFFFFFFF : 0x88FFFFFF);
        binding.pillLongBreak.setTextColor(mode == 2  ? 0xFFFFFFFF : 0x88FFFFFF);
    }

    // Pomodoro dots

    /**
     * Paints the four pomodoro indicator dots. The first two are filled
     * blue as a static preview; the right pair are faded white.
     */
    private void setupDots() {
        View[] dots = {binding.dot1, binding.dot2, binding.dot3, binding.dot4};
        float dp = getResources().getDisplayMetrics().density;

        // Each dot is just a circular GradientDrawable on a plain View.
        for (int i = 0; i < dots.length; i++) {
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);
            dotBg.setColor(i < 2 ? 0xFF0369A1 : 0x33FFFFFF);
            dots[i].setBackground(dotBg);
        }
    }

    // Start button

    /**
     * Launches the full screen Pomodoro timer activity.
     */
    private void setupStartButton() {
        binding.btnStartStudySession.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), StudyTimerActivity.class)));
    }

    // Courses

    /**
     * Subscribes to the courses LiveData and pushes the id-to-name map
     * into the SessionAdapter so each session row can show its course
     * label. No UI of its own.
     */
    private void observeCourses() {
        courseViewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            Map<Long, String> map = new HashMap<>();
            for (Course c : courses) {
                map.put(c.getId(), c.getName());
            }
            adapter.setCourseNameMap(map);
        });
    }

    // Recent sessions

    /**
     * Builds the recent sessions list and subscribes to its data. The
     * RecyclerView's nested scrolling is disabled so it scrolls together
     * with the surrounding ScrollView instead of fighting it.
     */
    private void setupRecentSessions() {
        adapter = new SessionAdapter();
        binding.recyclerRecentSessions.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerRecentSessions.setAdapter(adapter);
        binding.recyclerRecentSessions.setNestedScrollingEnabled(false);

        viewModel.getRecentSessions().observe(getViewLifecycleOwner(), sessions -> {
            adapter.submitList(sessions);
            // Toggle the empty-state line above the list.
            binding.textNoSessions.setVisibility(
                    sessions.isEmpty() ? View.VISIBLE : View.GONE);
            updatePomodoroCount(sessions);
        });
    }

    /**
     * Sums every session's pomodoroCount and shows the total in the
     * pomodoros chip. Free study sessions add zero, so they're harmless.
     */
    private void updatePomodoroCount(List<StudySession> sessions) {
        int total = 0;
        for (StudySession s : sessions) {
            total += s.getPomodoroCount();
        }
        binding.textPomodoroCount.setText(String.valueOf(total));
    }

    // Stats

    /**
     * Loads today's and this week's total study time and pushes the
     * formatted strings into the two summary chips. Each callback hops
     * to the UI thread before touching views.
     */
    private void loadStats() {
        long[] week  = DateUtils.getWeekRange();
        long[] today = DateUtils.getTodayRange();

        viewModel.getTotalStudyHours(week[0], seconds -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        binding.textWeekStudyTime.setText(
                                DateUtils.formatDuration(seconds)));
            }
        });

        viewModel.getTotalStudyHours(today[0], seconds -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        binding.textTodayStudyTime.setText(
                                DateUtils.formatDuration(seconds)));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}