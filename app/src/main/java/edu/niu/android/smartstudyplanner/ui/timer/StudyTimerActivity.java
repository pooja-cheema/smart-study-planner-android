/**************************************************************
 * Purpose: Full screen Pomodoro timer. Runs the standard     *
 * work/short break/long break cycle (long break every fourth *
 * pomodoro), counts only the work time toward the saved      *
 * session, and shows a mini player at the bottom that mirrors*
 * MusicService so the user can pause music without leaving.  *
 * On exit, prompts the user to save accumulated study time.  *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.timer;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.List;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.databinding.ActivityStudyTimerBinding;
import edu.niu.android.smartstudyplanner.notifications.NotificationHelper;
import edu.niu.android.smartstudyplanner.ui.music.MusicService;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.StudySessionViewModel;

public class StudyTimerActivity extends AppCompatActivity
        implements MusicService.MusicStateListener {

    private ActivityStudyTimerBinding binding;
    private CourseViewModel courseViewModel;
    private StudySessionViewModel sessionViewModel;

    // CountDownTimer driving the visible countdown.
    private CountDownTimer countDownTimer;

    // Whether the timer is currently ticking.
    private boolean isTimerRunning    = false;

    // True when running a break interval; false during work intervals.
    private boolean isBreakMode       = false;

    // Milliseconds left in the current interval.
    private long remainingMs;

    // Lengths of the work and short break intervals in millis.
    private long workDurationMs;
    private long breakDurationMs;

    // Cumulative seconds of work time accumulated in this session.
    // Break time does NOT count, so this is what gets saved as study time.
    private long totalStudiedSeconds  = 0;

    // How many work intervals have completed in this session.
    private int  pomodoroCount        = 0;

    // Wall clock time the session started, captured on first start press.
    private long sessionStartTime     = 0;

    // Course this session is tagged with, or null for "General Study".
    private Long selectedCourseId     = null;

    // User adjustable durations driving the intervals.
    private int workMinutes       = 25;
    private int shortBreakMinutes = 5;
    private int longBreakMinutes  = 15;

    // Course list backing the course spinner; index 0 is "General Study"
    // which keeps selectedCourseId null when chosen.
    private List<Course> courses     = new ArrayList<>();
    private List<String> courseNames = new ArrayList<>();

    // Music service binding state, used to drive the bottom mini player.
    private MusicService musicService;
    private boolean musicBound = false;
    private String currentTrackName = "";

    /**
     * Service connection that grabs the MusicService instance once bound,
     * registers this activity as the playback state listener, and seeds
     * the mini player with whatever is already playing.
     */
    private final ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setStateListener(StudyTimerActivity.this);
            musicBound = true;

            // Seed the mini player so it reflects current playback as soon as we attach.
            updateMiniPlayer(musicService.isPlaying(),
                    musicService.getCurrentTrackIndex(), currentTrackName);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityStudyTimerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar styling: dark blue background with a white title and back arrow.
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Study Timer");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(0xFF0A1245));
        }
        binding.toolbar.setTitleTextColor(0xFFFFFFFF);

        // Tinting the back arrow has to happen after the toolbar lays out
        // so getNavigationIcon() actually returns it.
        binding.toolbar.post(() -> {
            if (binding.toolbar.getNavigationIcon() != null) {
                binding.toolbar.getNavigationIcon().setTint(0xFFFFFFFF);
            }
        });

        courseViewModel  = new ViewModelProvider(this).get(CourseViewModel.class);
        sessionViewModel = new ViewModelProvider(this).get(StudySessionViewModel.class);

        setupCourseSpinner();
        setupDurationControls();
        initTimer();
        setupButtons();

        // Mini player play/pause button drives the bound service.
        binding.miniPlayerToggle.setOnClickListener(v -> {
            if (musicBound && musicService != null) {
                musicService.togglePlayPause();
            }
        });

        // Bind to the music service so we can show and control playback
        // from the bottom mini player without leaving this screen.
        Intent serviceIntent = new Intent(this, MusicService.class);
        bindService(serviceIntent, musicConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Called by MusicService when its playback state changes. Caches the
     * track name and pushes the update to the mini player on the UI thread.
     */
    @Override
    public void onStateChanged(boolean playing, int trackIndex, String trackName) {
        if (trackName != null && !trackName.isEmpty()) {
            currentTrackName = trackName;
        }
        runOnUiThread(() -> updateMiniPlayer(playing, trackIndex, trackName));
    }

    /**
     * Refreshes the mini player. Hides it when no track is loaded; otherwise
     * shows the track name and a play or pause glyph.
     */
    private void updateMiniPlayer(boolean playing, int trackIndex, String trackName) {
        if (trackIndex == -1) {
            binding.miniPlayer.setVisibility(View.GONE);
            return;
        }
        binding.miniPlayer.setVisibility(View.VISIBLE);
        if (trackName != null && !trackName.isEmpty()) {
            binding.miniPlayerTrack.setText(trackName);
        }
        binding.miniPlayerToggle.setText(playing ? "⏸" : "▶");
    }

    /**
     * Builds the course spinner. Index 0 is "General Study" (no course),
     * so selecting it leaves selectedCourseId as null.
     */
    private void setupCourseSpinner() {
        courseViewModel.getAllCourses().observe(this, courseList -> {
            courses.clear();
            courseNames.clear();

            // First entry is the "no course" option.
            courseNames.add("General Study");
            courses.add(null);

            for (Course c : courseList) {
                courses.add(c);
                courseNames.add(c.getName());
            }
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, courseNames);
            adapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCourse.setAdapter(adapter);
        });

        binding.spinnerCourse.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(android.widget.AdapterView<?> parent,
                                               View view, int pos, long id) {
                        // Map the spinner position back to a course (or null).
                        Course c = courses.get(pos);
                        selectedCourseId = (c != null) ? c.getId() : null;
                    }
                    @Override
                    public void onNothingSelected(android.widget.AdapterView<?> parent) {}
                });
    }

    /**
     * Wires up the four plus/minus buttons that adjust work and break
     * lengths. Work is constrained to 5 to 60 minutes in 5 minute steps;
     * break is 1 to 30 minutes in 1 minute steps.
     */
    private void setupDurationControls() {
        binding.textWorkMinutes.setText(String.valueOf(workMinutes));
        binding.textBreakMinutes.setText(String.valueOf(shortBreakMinutes));

        binding.btnWorkMinus.setOnClickListener(v -> {
            if (workMinutes > 5) {
                workMinutes -= 5;
                binding.textWorkMinutes.setText(String.valueOf(workMinutes));
                // If the user changes work time while the timer is idle and
                // not in break mode, refresh the timer to reflect it.
                if (!isTimerRunning && !isBreakMode) initTimer();
            }
        });
        binding.btnWorkPlus.setOnClickListener(v -> {
            if (workMinutes < 60) {
                workMinutes += 5;
                binding.textWorkMinutes.setText(String.valueOf(workMinutes));
                if (!isTimerRunning && !isBreakMode) initTimer();
            }
        });
        binding.btnBreakMinus.setOnClickListener(v -> {
            if (shortBreakMinutes > 1) {
                shortBreakMinutes--;
                binding.textBreakMinutes.setText(String.valueOf(shortBreakMinutes));
            }
        });
        binding.btnBreakPlus.setOnClickListener(v -> {
            if (shortBreakMinutes < 30) {
                shortBreakMinutes++;
                binding.textBreakMinutes.setText(String.valueOf(shortBreakMinutes));
            }
        });
    }

    /**
     * Resets the working duration constants from the user's chosen minutes
     * and shows the right starting time and label for the current mode.
     */
    private void initTimer() {
        workDurationMs  = workMinutes * 60 * 1000L;
        breakDurationMs = shortBreakMinutes * 60 * 1000L;
        remainingMs     = isBreakMode ? breakDurationMs : workDurationMs;
        updateTimerDisplay(remainingMs);
        updateModeLabel();
    }

    /**
     * Wires up the four primary buttons: Start/Pause, Reset, Skip Break,
     * and Save Session. Reset asks for confirmation if a session is in
     * flight; Save bails out with a toast if no work time was recorded.
     */
    private void setupButtons() {
        binding.btnStartPause.setOnClickListener(v -> {
            if (isTimerRunning) pauseTimer(); else startTimer();
        });

        binding.btnReset.setOnClickListener(v -> {
            // Only confirm when there's something to lose.
            if (isTimerRunning || totalStudiedSeconds > 0) {
                new AlertDialog.Builder(this)
                        .setTitle("Reset Timer")
                        .setMessage("Are you sure? Your current session progress will be lost.")
                        .setPositiveButton("Reset", (d, w) -> resetTimer())
                        .setNegativeButton("Cancel", null)
                        .show();
            } else {
                resetTimer();
            }
        });

        binding.btnSkipBreak.setOnClickListener(v -> {
            if (isBreakMode) endBreak();
        });

        binding.btnSaveSession.setOnClickListener(v -> {
            if (totalStudiedSeconds > 0) {
                saveSession();
            } else {
                Toast.makeText(this,
                        "No study time recorded yet. Start the timer first.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Starts (or resumes) the countdown. Each tick updates the display and
     * the progress bar. During work intervals the tick also increments
     * totalStudiedSeconds, which is what eventually gets saved.
     */
    private void startTimer() {
        // Capture the session start time on first start so the saved
        // session has a wall-clock start, not just a duration.
        if (sessionStartTime == 0) sessionStartTime = System.currentTimeMillis();

        countDownTimer = new CountDownTimer(remainingMs, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                remainingMs = millisUntilFinished;
                updateTimerDisplay(remainingMs);
                updateProgressBar(millisUntilFinished);

                // Only count work seconds toward the saved total. Breaks
                // get displayed but never recorded as study time.
                if (!isBreakMode) {
                    totalStudiedSeconds++;
                    updateTotalStudiedDisplay();
                }
            }
            @Override
            public void onFinish() {
                isTimerRunning = false;
                remainingMs    = 0;
                updateProgressBar(0);
                if (!isBreakMode) onWorkSessionComplete();
                else onBreakComplete();
            }
        }.start();

        isTimerRunning = true;
        binding.btnStartPause.setText("Pause");

        // Lock the duration controls so the user can't tweak intervals mid run.
        setControlsEnabled(false);
    }

    /**
     * Pauses the running countdown without losing the elapsed time.
     */
    private void pauseTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        binding.btnStartPause.setText("Resume");
        setControlsEnabled(true);
    }

    /**
     * Cancels the countdown and zeroes every counter so the next start
     * begins a brand new session.
     */
    private void resetTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning      = false;
        isBreakMode         = false;
        pomodoroCount       = 0;
        totalStudiedSeconds = 0;
        sessionStartTime    = 0;
        initTimer();
        binding.btnStartPause.setText("Start");
        binding.textPomodoroCount.setText("0");
        binding.textTotalStudied.setText("00:00");
        binding.progressTimer.setProgress(0);
        binding.btnSkipBreak.setVisibility(View.GONE);
        setControlsEnabled(true);
    }

    /**
     * Called when a work interval ends. Bumps the pomodoro count, posts a
     * notification, switches to break mode, and immediately starts the
     * break (long break every fourth pomodoro).
     */
    private void onWorkSessionComplete() {
        pomodoroCount++;
        binding.textPomodoroCount.setText(String.valueOf(pomodoroCount));
        NotificationHelper.showPomodoroCompleteNotification(this, pomodoroCount);
        Toast.makeText(this,
                "🎉 Pomodoro #" + pomodoroCount + " complete! Take a break.",
                Toast.LENGTH_LONG).show();

        // Every fourth pomodoro earns a long break.
        isBreakMode = true;
        remainingMs = (pomodoroCount % 4 == 0)
                ? longBreakMinutes * 60 * 1000L : breakDurationMs;
        updateModeLabel();
        binding.btnSkipBreak.setVisibility(View.VISIBLE);
        startTimer();
    }

    /**
     * Called when a break interval ends naturally; notifies the user and
     * routes through endBreak so we hop back to a fresh work interval.
     */
    private void onBreakComplete() {
        NotificationHelper.showBreakCompleteNotification(this);
        Toast.makeText(this, "Break over! Ready for the next Pomodoro?",
                Toast.LENGTH_SHORT).show();
        endBreak();
    }

    /**
     * Exits break mode and primes the timer for the next work interval.
     * Used both when a break ends naturally and when the user skips it.
     */
    private void endBreak() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = false;
        isBreakMode    = false;
        remainingMs    = workDurationMs;
        updateTimerDisplay(remainingMs);
        updateModeLabel();
        binding.btnSkipBreak.setVisibility(View.GONE);
        binding.btnStartPause.setText("Start");
        binding.progressTimer.setProgress(0);
        setControlsEnabled(true);
    }

    /**
     * Updates the big "MM:SS" timer label.
     */
    private void updateTimerDisplay(long ms) {
        long secs = ms / 1000;
        binding.textTimer.setText(DateUtils.formatTimerDisplay(secs));
    }

    /**
     * Updates the linear progress bar using the appropriate total length
     * based on whether we're in work or break mode.
     */
    private void updateProgressBar(long remainingMs) {
        long totalMs  = isBreakMode ? breakDurationMs : workDurationMs;
        int  progress = (int)(((totalMs - remainingMs) * 100) / totalMs);
        binding.progressTimer.setProgress(progress);
    }

    /**
     * Updates the mode label and the timer card's background color so
     * focus and break modes are visually distinct.
     */
    private void updateModeLabel() {
        if (isBreakMode) {
            // Long break every fourth pomodoro gets its own label.
            boolean isLongBreak = (pomodoroCount % 4 == 0);
            binding.textMode.setText(isLongBreak ? "Long Break ☕" : "Short Break 🧘");
            binding.cardTimer.setCardBackgroundColor(
                    getColor(edu.niu.android.smartstudyplanner.R.color.break_color));
        } else {
            binding.textMode.setText("Focus Time 🎯");
            binding.cardTimer.setCardBackgroundColor(
                    getColor(edu.niu.android.smartstudyplanner.R.color.work_color));
        }
    }

    /**
     * Updates the running "total studied" label to match the accumulated
     * work seconds.
     */
    private void updateTotalStudiedDisplay() {
        binding.textTotalStudied.setText(
                DateUtils.formatTimerDisplay(totalStudiedSeconds));
    }

    /**
     * Locks or unlocks the duration controls and the course spinner so
     * the user can only change them while the timer is idle.
     */
    private void setControlsEnabled(boolean enabled) {
        binding.btnWorkMinus.setEnabled(enabled);
        binding.btnWorkPlus.setEnabled(enabled);
        binding.btnBreakMinus.setEnabled(enabled);
        binding.btnBreakPlus.setEnabled(enabled);
        binding.spinnerCourse.setEnabled(enabled);
    }

    /**
     * Builds a StudySession out of the current counters and persists it
     * via the ViewModel. On success, shows a confirmation toast and
     * finishes the activity.
     */
    private void saveSession() {
        long endTime = System.currentTimeMillis();
        StudySession session = new StudySession(
                selectedCourseId, "", sessionStartTime, endTime,
                totalStudiedSeconds, StudySession.SESSION_TYPE_POMODORO,
                pomodoroCount, true);
        sessionViewModel.saveSession(session, id -> runOnUiThread(() -> {
            Toast.makeText(this,
                    "Session saved! "
                            + DateUtils.formatDuration(totalStudiedSeconds)
                            + " recorded.",
                    Toast.LENGTH_LONG).show();
            finish();
        }));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Cancel the countdown so it can't fire after the activity dies.
        if (countDownTimer != null) countDownTimer.cancel();
        // Release the music service binding.
        if (musicBound) {
            unbindService(musicConnection);
            musicBound = false;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        // If there's unsaved progress, give the user a chance to save it
        // instead of losing the session by accident.
        if (isTimerRunning || totalStudiedSeconds > 0) {
            new AlertDialog.Builder(this)
                    .setTitle("Exit Timer?")
                    .setMessage("You have "
                            + DateUtils.formatDuration(totalStudiedSeconds)
                            + " of study time recorded.\nDo you want to save before leaving?")
                    .setPositiveButton("Save & Exit", (d, w) -> {
                        if (totalStudiedSeconds > 0) saveSession(); else finish();
                    })
                    .setNegativeButton("Exit Without Saving", (d, w) -> finish())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}