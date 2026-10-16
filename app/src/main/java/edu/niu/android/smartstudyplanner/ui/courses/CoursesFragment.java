package edu.niu.android.smartstudyplanner.ui.courses;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.List;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.databinding.FragmentCoursesBinding;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;

public class CoursesFragment extends Fragment {

    private FragmentCoursesBinding binding;
    private CourseViewModel viewModel;
    private CourseAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCoursesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        setupRecyclerView();
        setupObserver();
        setupSearch();
        setupFab();
    }

    /**
     * Builds the RecyclerView with the CourseAdapter. The adapter is
     * given two callbacks: edit opens AddEditCourseActivity with the
     * course id, delete opens the confirmation dialog. Also applies a
     * fall down layout animation so rows stagger in on first display.
     */
    private void setupRecyclerView() {
        adapter = new CourseAdapter(
                course -> {
                    Intent intent = new Intent(requireContext(), AddEditCourseActivity.class);
                    intent.putExtra(AddEditCourseActivity.EXTRA_COURSE_ID, course.getId());
                    startActivity(intent);
                },
                course -> showDeleteDialog(course)
        );

        binding.recyclerCourses.setLayoutManager(
                new LinearLayoutManager(requireContext()));
        binding.recyclerCourses.setAdapter(adapter);

        binding.recyclerCourses.setLayoutAnimation(
                android.view.animation.AnimationUtils.loadLayoutAnimation(
                        requireContext(),
                        R.anim.layout_animation_fall_down));
    }

    /**
     * Subscribes to the course list LiveData. Updates the adapter, the
     * "N courses enrolled" header, and the empty state whenever the
     * database changes.
     */
    private void setupObserver() {
        viewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            adapter.setFullList(courses);

            int count = courses.size();
            binding.textCourseCount.setText(
                    count + " course" + (count == 1 ? "" : "s") + " enrolled");

            updateEmptyState(courses.isEmpty());
        });
    }

    /**
     * Hooks up the search box. Filtering is delegated to the adapter,
     * which keeps a separate copy of the unfiltered list.
     */
    private void setupSearch() {
        binding.editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                List<Course> filtered = adapter.filter(s.toString());
                updateEmptyState(filtered.isEmpty());
            }
        });
    }

    /**
     * Toggles between the empty state message and the list, used both
     * when the database has no courses and when the search produces no
     * matches.
     */
    private void updateEmptyState(boolean empty) {
        binding.textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerCourses.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    /**
     * Shows the "Delete course?" confirmation. Reuses the shared task
     * action dialog layout, restyled with a red confirm button and a
     * warning that linked tasks will also be deleted.
     */
    private void showDeleteDialog(Course course) {
        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        View dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_task_action, null);
        dialog.setContentView(dialogView);

        // Transparent window background so the rounded card is visible.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ((TextView) dialogView.findViewById(R.id.dialog_icon)).setText("🗑");
        ((TextView) dialogView.findViewById(R.id.dialog_title)).setText("Delete Course?");
        ((TextView) dialogView.findViewById(R.id.dialog_message)).setText(
                "Delete \"" + extractCode(course.getName()) + "\"?\n\n"
                        + "All linked tasks will also be deleted.");

        TextView confirmBtn = dialogView.findViewById(R.id.dialog_btn_confirm);
        confirmBtn.setText("DELETE");

        // Red rounded background to signal the destructive action.
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setColor(0xFFC62828);
        confirmBg.setCornerRadius(14 * dp);
        confirmBtn.setBackground(confirmBg);
        confirmBtn.setTextColor(0xFFFFFFFF);

        confirmBtn.setOnClickListener(v -> {
            viewModel.delete(course);
            dialog.dismiss();
        });

        dialogView.findViewById(R.id.dialog_btn_cancel)
                .setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Constrain the dialog to 88% of screen width so it doesn't span edge to edge.
        if (dialog.getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(
                    width,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    /**
     * Returns the course code portion of a stored "CODE - Name" string,
     * used in the delete dialog so the message shows just "CSCI 524"
     * instead of the full course title.
     */
    private String extractCode(String fullName) {
        if (fullName == null) return "";
        if (fullName.contains(" - ")) return fullName.substring(0, fullName.indexOf(" - "));
        return fullName;
    }

    /**
     * Styles the floating action button with a horizontal gradient and
     * gives it a small press animation. Tapping opens
     * AddEditCourseActivity in add mode.
     */
    private void setupFab() {
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ 0xFF000000, 0xFF0369A1 });
        g.setCornerRadius(32f * dp);
        binding.fabAddCourse.setBackground(g);
        binding.fabAddCourse.setTextColor(0xFFFFFFFF);
        binding.fabAddCourse.setElevation(16f);
        binding.fabAddCourse.setOnClickListener(v -> {
            // Quick scale down then back up for a tactile press feel.
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(80)
                    .withEndAction(() -> {
                        v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
                        startActivity(new Intent(requireContext(), AddEditCourseActivity.class));
                    }).start();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}