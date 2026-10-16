package edu.niu.android.smartstudyplanner.ui.countdown;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.databinding.FragmentCountdownBinding;
import edu.niu.android.smartstudyplanner.databinding.ItemCountdownBinding;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class CountdownFragment extends Fragment {

    private FragmentCountdownBinding binding;
    private TaskViewModel taskViewModel;

    // Drives the per second tick that refreshes every visible card.
    private final Handler countdownHandler = new Handler(Looper.getMainLooper());

    // Stored as a field so we can cancel it from onPause and onDestroyView.
    private Runnable countdownRunnable;

    // Snapshot of the current task list, refreshed whenever LiveData emits.
    private List<Task> currentTasks = new ArrayList<>();

    // One binding per visible card, kept in sync with currentTasks so each
    // card can be updated independently without rebuilding the list every tick.
    private final List<ItemCountdownBinding> cardBindings = new ArrayList<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCountdownBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel = new ViewModelProvider(this).get(TaskViewModel.class);

        observeTasks();
        setupCountdownTick();
    }

    /**
     * Subscribes to the task list and rebuilds the countdown cards whenever
     * it changes. Only incomplete tasks with a due date are shown, sorted
     * soonest first. Toggles the empty state when there's nothing to show.
     */
    private void observeTasks() {
        taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
            // Keep only future, incomplete tasks with a real due date.
            List<Task> filtered = new ArrayList<>();
            for (Task t : tasks) {
                if (t.getDueDate() > 0 && !t.isCompleted()) {
                    filtered.add(t);
                }
            }

            filtered.sort((a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));

            currentTasks = filtered;

            buildCountdownCards();

            if (filtered.isEmpty()) {
                binding.textEmptyCountdown.setVisibility(View.VISIBLE);
                binding.layoutCountdownList.setVisibility(View.GONE);
            } else {
                binding.textEmptyCountdown.setVisibility(View.GONE);
                binding.layoutCountdownList.setVisibility(View.VISIBLE);
            }
        });
    }

    /**
     * Inflates one card per task and adds it to the container. Sets the static
     * fields (name, type, urgency color) here; the live numbers are filled in
     * by updateAllCountdowns.
     */
    private void buildCountdownCards() {
        binding.layoutCountdownList.removeAllViews();
        cardBindings.clear();

        for (Task task : currentTasks) {
            ItemCountdownBinding itemBinding = ItemCountdownBinding.inflate(
                    LayoutInflater.from(requireContext()),
                    binding.layoutCountdownList,
                    false
            );

            itemBinding.textTaskName.setText(task.getTitle());
            itemBinding.textTaskType.setText(task.getTypeLabel());

            applyUrgencyColour(itemBinding, task.getDueDate());

            // Slide each card in as it's added.
            itemBinding.getRoot().startAnimation(
                    AnimationUtils.loadAnimation(requireContext(), R.anim.item_fall_down));

            binding.layoutCountdownList.addView(itemBinding.getRoot());
            cardBindings.add(itemBinding);
        }

        // Show real numbers right away instead of waiting for the first tick.
        updateAllCountdowns();
    }

    /**
     * Starts a self scheduling Runnable that refreshes the displays every
     * second. Cancelled in onPause and onDestroyView.
     */
    private void setupCountdownTick() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                updateAllCountdowns();
                countdownHandler.postDelayed(this, 1000);
            }
        };
    }

    /**
     * Refreshes every visible card. Computes the remaining time, splits it
     * into days/hours/minutes/seconds, updates the urgency color (it can
     * change as time passes), and sets the elapsed progress bar.
     */
    private void updateAllCountdowns() {
        if (!isAdded() || binding == null) return;

        long now = System.currentTimeMillis();

        for (int i = 0; i < currentTasks.size() && i < cardBindings.size(); i++) {
            Task task = currentTasks.get(i);
            ItemCountdownBinding card = cardBindings.get(i);

            long dueDate = task.getDueDate();
            long millisRemaining = dueDate - now;

            if (millisRemaining <= 0) {
                // Due date has passed: zero everything and show overdue badge.
                card.textDays.setText("00");
                card.textHours.setText("00");
                card.textMinutes.setText("00");
                card.textSeconds.setText("00");
                card.badgeOverdue.setVisibility(View.VISIBLE);
                card.viewUrgencyStrip.setBackgroundColor(Color.parseColor("#B71C1C"));
                card.progressTimeElapsed.setProgress(100);

            } else {
                card.badgeOverdue.setVisibility(View.GONE);

                long days    = TimeUnit.MILLISECONDS.toDays(millisRemaining);
                long hours   = TimeUnit.MILLISECONDS.toHours(millisRemaining) % 24;
                long minutes = TimeUnit.MILLISECONDS.toMinutes(millisRemaining) % 60;
                long seconds = TimeUnit.MILLISECONDS.toSeconds(millisRemaining) % 60;

                card.textDays.setText(String.format("%02d", days));
                card.textHours.setText(String.format("%02d", hours));
                card.textMinutes.setText(String.format("%02d", minutes));
                card.textSeconds.setText(String.format("%02d", seconds));

                // Re-run urgency in case we just crossed a threshold.
                applyUrgencyColour(card, dueDate);

                // Progress bar shows how far we are between createdAt and dueDate.
                long createdAt = task.getCreatedAt();
                if (createdAt > 0 && dueDate > createdAt) {
                    long total   = dueDate - createdAt;
                    long elapsed = now - createdAt;
                    int progress = (int) Math.min(100,
                            (elapsed * 100L / total));
                    card.progressTimeElapsed.setProgress(progress);

                    // Tint the progress bar to match the urgency level.
                    if (days < 1) {
                        card.progressTimeElapsed.setProgressTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#F44336")));
                    } else if (days < 2) {
                        card.progressTimeElapsed.setProgressTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#FF9800")));
                    } else if (days < 7) {
                        card.progressTimeElapsed.setProgressTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#FFC107")));
                    } else {
                        card.progressTimeElapsed.setProgressTintList(
                                android.content.res.ColorStateList.valueOf(
                                        Color.parseColor("#4CAF50")));
                    }
                }
            }
        }
    }

    /**
     * Sets the left strip color and the urgency text label based on how
     * much time is left. Thresholds: green > 7 days, yellow 2 to 7 days,
     * orange 1 to 2 days, red under 24 hours, dark red overdue.
     */
    private void applyUrgencyColour(ItemCountdownBinding card, long dueDate) {
        long now = System.currentTimeMillis();
        long millisRemaining = dueDate - now;
        long daysRemaining = TimeUnit.MILLISECONDS.toDays(millisRemaining);

        int colour;
        String urgencyLabel;

        if (millisRemaining <= 0) {
            colour = Color.parseColor("#B71C1C");
            urgencyLabel = "Overdue";
        } else if (daysRemaining < 1) {
            colour = Color.parseColor("#B71C1C");
            urgencyLabel = "Critical - due today!";
        } else if (daysRemaining < 2) {
            colour = Color.parseColor("#E64A19");
            urgencyLabel = "Urgent - due tomorrow";
        } else if (daysRemaining < 7) {
            colour = Color.parseColor("#F9A825");
            urgencyLabel = "Coming up - " + daysRemaining + " days left";
        } else {
            colour = Color.parseColor("#388E3C");
            urgencyLabel = daysRemaining + " days remaining";
        }

        card.viewUrgencyStrip.setBackgroundColor(colour);
        card.textUrgencyLabel.setText(urgencyLabel);
        card.textUrgencyLabel.setTextColor(colour);
    }

    /**
     * Formats a due date timestamp as something like "Mon, Apr 28 · 11:59 PM".
     */
    private String formatDueDate(long dueDate) {
        if (dueDate <= 0) return "No due date";
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("EEE, MMM d · hh:mm a",
                        java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(dueDate));
    }

    /**
     * Stops the ticker when the fragment isn't visible so we aren't burning
     * CPU updating views the user can't see.
     */
    @Override
    public void onPause() {
        super.onPause();
        countdownHandler.removeCallbacks(countdownRunnable);
    }

    /**
     * Restarts the ticker when the fragment becomes visible again.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (countdownRunnable != null) {
            countdownHandler.removeCallbacks(countdownRunnable);
            countdownHandler.postDelayed(countdownRunnable, 1000);
        }
    }

    /**
     * Cancels the Handler callback and clears the binding. Removing the
     * callback is required: the Runnable holds a reference to the
     * fragment's views, so leaving it scheduled would leak memory.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        countdownHandler.removeCallbacks(countdownRunnable);
        binding = null;
    }
}