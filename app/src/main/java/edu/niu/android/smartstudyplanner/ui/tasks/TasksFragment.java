/**************************************************************
 * Purpose: Tasks tab. Hosts four sub tabs (Pending,          *
 * Completed, All, Overdue), each backed by a different       *
 * LiveData query. Provides per tab sort and filter sheets    *
 * (by type and by course), a shared confirmation dialog for  *
 * complete/incomplete/delete actions, and a floating action  *
 * button that opens AddEditTaskActivity in add mode. The     *
 * sort and filter selections are remembered per tab.         *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.tasks;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.niu.android.smartstudyplanner.R;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.databinding.FragmentTasksBinding;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class TasksFragment extends Fragment {

    private FragmentTasksBinding binding;
    private TaskViewModel taskViewModel;
    private CourseViewModel courseViewModel;
    private TaskAdapter adapter;

    // Tab constants. Match the indices in tabSortBy/tabFilterType/tabFilterCourse below.
    private static final int TAB_PENDING   = 0;
    private static final int TAB_COMPLETED = 1;
    private static final int TAB_ALL       = 2;
    private static final int TAB_OVERDUE   = 3;

    // Currently active tab.
    private int currentTab = TAB_PENDING;

    // Per tab sort/filter state. Each tab remembers its own selections so
    // moving between tabs doesn't reset what the user picked.
    private final String[] tabSortBy       = { "none", "none", "none", "none" };
    private final int[]    tabFilterType   = { -1, -1, -1, -1 };
    private final long[]   tabFilterCourse = { -1L, -1L, -1L, -1L };

    // Latest unfiltered list emitted by the active tab's LiveData query.
    private List<Task>        currentFullList = new ArrayList<>();

    // All courses, used to build the course filter sheet rows.
    private List<Course>      allCourses      = new ArrayList<>();

    // Course id to name lookup, handed to the adapter so rows can show course labels.
    private Map<Long, String> courseNameMap   = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentTasksBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        taskViewModel   = new ViewModelProvider(this).get(TaskViewModel.class);
        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        setupHeaderDate();
        setupAdapter();
        setupRecyclerView();
        setupFabGradient();
        setupTabClicks();
        setupSortButton();
        setupFilterButton();
        observeCourses();

        // Land on Pending by default.
        switchTab(TAB_PENDING);
    }

    // Header date

    /**
     * Shows today's date (e.g. "Friday, May 1") at the top of the screen.
     */
    private void setupHeaderDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.getDefault());
        binding.textHeaderDate.setText(sdf.format(new Date()));
    }

    // Courses

    /**
     * Subscribes to the course list. Updates the local cache and the
     * adapter's id-to-name map so task rows can resolve their course
     * label as soon as the data arrives.
     */
    private void observeCourses() {
        courseViewModel.getAllCourses().observe(getViewLifecycleOwner(), courses -> {
            allCourses = courses != null ? courses : new ArrayList<>();
            courseNameMap.clear();
            for (Course c : allCourses) {
                courseNameMap.put(c.getId(), c.getName());
            }
            adapter.setCourseNameMap(courseNameMap);
        });
    }

    // Custom dialog

    /**
     * Shows the shared confirmation dialog used by complete, incomplete,
     * and delete actions. Caller supplies the icon, title, message, the
     * confirm button label and color, and the action to run on confirm.
     * Cancel forces a rebind so any optimistic UI change rolls back.
     */
    private void showTaskDialog(String icon, String title, String message,
                                String confirmText, int confirmColor,
                                Runnable onConfirm) {
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

        ((TextView) dialogView.findViewById(R.id.dialog_icon)).setText(icon);
        ((TextView) dialogView.findViewById(R.id.dialog_title)).setText(title);
        ((TextView) dialogView.findViewById(R.id.dialog_message)).setText(message);

        TextView confirmBtn = dialogView.findViewById(R.id.dialog_btn_confirm);
        confirmBtn.setText(confirmText);

        // Confirm button takes the caller's color and uses white text.
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable confirmBg = new GradientDrawable();
        confirmBg.setColor(confirmColor);
        confirmBg.setCornerRadius(14 * dp);
        confirmBtn.setBackground(confirmBg);
        confirmBtn.setTextColor(0xFFFFFFFF);

        confirmBtn.setOnClickListener(v -> {
            onConfirm.run();
            dialog.dismiss();
        });

        // Cancel rebinds the list so any in-flight visual change reverts.
        dialogView.findViewById(R.id.dialog_btn_cancel)
                .setOnClickListener(v -> {
                    adapter.notifyDataSetChanged();
                    dialog.dismiss();
                });

        dialog.show();

        // Constrain dialog to 88% of screen width so it doesn't span edge to edge.
        if (dialog.getWindow() != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.88f);
            dialog.getWindow().setLayout(
                    width,
                    android.view.WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }

    // Adapter

    /**
     * Builds the TaskAdapter and wires up its three callbacks. Tapping
     * the circle toggles complete/incomplete with confirmation. Edit
     * launches AddEditTaskActivity in edit mode. Delete confirms first,
     * then removes the task.
     */
    private void setupAdapter() {
        adapter = new TaskAdapter(task -> {
            String courseName = getShortCourseName(task.getCourseId());
            if (task.isCompleted()) {
                showTaskDialog("↩", "Mark as Incomplete?",
                        "Move \"" + task.getTitle() + "\"\nof " + courseName + " back to pending?",
                        "YES", 0xFF0369A1,
                        () -> taskViewModel.markIncomplete(task.getId()));
            } else {
                showTaskDialog("✓", "Mark as Complete?",
                        "Mark \"" + task.getTitle() + "\"\nof " + courseName + " as done?",
                        "YES", 0xFF065F46,
                        () -> taskViewModel.markCompleted(task.getId()));
            }
        });

        adapter.setOnEditClickListener(task -> {
            Intent intent = new Intent(requireContext(), AddEditTaskActivity.class);
            intent.putExtra(AddEditTaskActivity.EXTRA_TASK_ID, task.getId());
            startActivity(intent);
        });

        adapter.setOnDeleteClickListener(task -> {
            String courseName = getShortCourseName(task.getCourseId());
            showTaskDialog("🗑", "Delete Task?",
                    "Delete \"" + task.getTitle() + "\"\nof " + courseName
                            + "?\n\nThis cannot be undone.",
                    "DELETE", 0xFFC62828,
                    () -> taskViewModel.delete(task));
        });
    }

    /**
     * Returns the course code portion of the stored "CODE - Name" string,
     * or "this course" if the id can't be resolved. Used in confirmation
     * dialog messages.
     */
    private String getShortCourseName(long courseId) {
        String name = courseNameMap.get(courseId);
        if (name == null) return "this course";
        if (name.contains(" - ")) name = name.substring(0, name.indexOf(" - "));
        return name;
    }

    /**
     * Plain vertical RecyclerView setup; row inflation is handled by the
     * adapter.
     */
    private void setupRecyclerView() {
        binding.recyclerTasks.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerTasks.setAdapter(adapter);
    }

    // FAB

    /**
     * Styles the floating action button with a black to ocean blue
     * gradient (matches the Courses FAB) and wires it up to launch
     * AddEditTaskActivity in add mode.
     */
    private void setupFabGradient() {
        float dp = getResources().getDisplayMetrics().density;
        GradientDrawable g = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{ 0xFF000000, 0xFF0369A1 });
        g.setCornerRadius(32f * dp);
        binding.fabAddTask.setBackground(g);
        binding.fabAddTask.setTextColor(0xFFFFFFFF);
        binding.fabAddTask.setElevation(16f);
        binding.fabAddTask.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AddEditTaskActivity.class)));
    }

    // Tab clicks

    /**
     * Wires up the four tab buttons. Each one calls switchTab so it can
     * load the right LiveData query and re-style the selection.
     */
    private void setupTabClicks() {
        binding.tabPending.setOnClickListener(v   -> switchTab(TAB_PENDING));
        binding.tabCompleted.setOnClickListener(v -> switchTab(TAB_COMPLETED));
        binding.tabAll.setOnClickListener(v       -> switchTab(TAB_ALL));
        binding.tabOverdue.setOnClickListener(v   -> switchTab(TAB_OVERDUE));
    }

    // Sort sheet

    /**
     * Wires the sort button to open the sort bottom sheet.
     */
    private void setupSortButton() {
        binding.btnSort.setOnClickListener(v -> showSortSheet());
    }

    /**
     * Builds and shows the sort bottom sheet for the currently active
     * tab. The selected option is stored in tabSortBy[currentTab] and
     * applied immediately.
     */
    private void showSortSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        float dp = getResources().getDisplayMetrics().density;

        // Use a ScrollView in case there's not enough vertical room.
        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.setBackgroundColor(0xFFFFFFFF);

        android.widget.LinearLayout root = buildSheetRoot(dp);
        root.addView(buildSheetHeader("↕  Sort By", currentTab, dp));
        root.addView(buildResetButton("Reset Sort", dp, () -> {
            tabSortBy[currentTab] = "none";
            applyFilterSort();
            sheet.dismiss();
        }));

        // Sort option labels, internal keys, and icons. Indices are paired.
        String[] sortLabels = {
                "Default order",
                "Due Date - Earliest first",
                "Due Date - Latest first",
                "Priority - High to Low",
                "Priority - Low to High",
        };
        String[] sortKeys  = {
                "none", "due_date_asc", "due_date_desc", "priority_desc", "priority_asc"
        };
        String[] sortIcons = { "○", "↑", "↓", "▲", "▽" };

        android.widget.LinearLayout card = makeCard(dp);
        for (int i = 0; i < sortLabels.length; i++) {
            final String key = sortKeys[i];
            boolean selected = tabSortBy[currentTab].equals(key);
            card.addView(makeBeautifulOptionRow(
                    sortIcons[i], sortLabels[i], selected,
                    0xFF0A1245, i < sortLabels.length - 1, dp,
                    () -> { tabSortBy[currentTab] = key; applyFilterSort(); sheet.dismiss(); }));
        }
        root.addView(card);
        sv.addView(root);
        sheet.setContentView(sv);
        sheet.show();
    }

    // Filter sheet

    /**
     * Wires the filter button to open the filter bottom sheet.
     */
    private void setupFilterButton() {
        binding.btnFilter.setOnClickListener(v -> showFilterSheet());
    }

    /**
     * Builds and shows the filter bottom sheet with two sections: filter
     * by type, and filter by course. Selections are stored per tab in
     * tabFilterType and tabFilterCourse.
     */
    private void showFilterSheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        float dp = getResources().getDisplayMetrics().density;

        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        sv.setBackgroundColor(0xFFFFFFFF);

        android.widget.LinearLayout root = buildSheetRoot(dp);
        root.addView(buildSheetHeader("⊞  Filter", currentTab, dp));
        root.addView(buildResetButton("Reset All Filters", dp, () -> {
            tabFilterType[currentTab]   = -1;
            tabFilterCourse[currentTab] = -1L;
            applyFilterSort();
            sheet.dismiss();
        }));

        // Filter by type section.
        root.addView(makeBeautifulSectionHeader("Filter by Type", 0xFF4A148C, dp));

        String[] typeLabels = { "All Types", "Assignment", "Exam", "Quiz", "Project", "Other" };
        String[] typeIcons  = { "✦", "📝", "📋", "❓", "🗂", "•" };
        int[]    typeValues = { -1, Task.TYPE_ASSIGNMENT, Task.TYPE_EXAM,
                Task.TYPE_QUIZ, Task.TYPE_PROJECT, Task.TYPE_OTHER };

        android.widget.LinearLayout typeCard = makeCard(dp);
        for (int i = 0; i < typeLabels.length; i++) {
            final int val = typeValues[i];
            boolean selected = tabFilterType[currentTab] == val;
            typeCard.addView(makeBeautifulOptionRow(
                    typeIcons[i], typeLabels[i], selected,
                    0xFF4A148C, i < typeLabels.length - 1, dp,
                    () -> { tabFilterType[currentTab] = val; applyFilterSort(); sheet.dismiss(); }));
        }
        root.addView(typeCard);

        // Filter by course section.
        root.addView(makeBeautifulSectionHeader("Filter by Course", 0xFF065F46, dp));

        android.widget.LinearLayout courseCard = makeCard(dp);

        // "All Courses" row first; selected when no course filter is set.
        courseCard.addView(makeBeautifulOptionRow(
                "✦", "All Courses", tabFilterCourse[currentTab] == -1L,
                0xFF065F46, !allCourses.isEmpty(), dp,
                () -> { tabFilterCourse[currentTab] = -1L; applyFilterSort(); sheet.dismiss(); }));

        // One row per course, showing the code portion of the stored name.
        for (int i = 0; i < allCourses.size(); i++) {
            Course course = allCourses.get(i);
            String displayName = course.getName();
            if (displayName != null && displayName.contains(" - "))
                displayName = displayName.substring(0, displayName.indexOf(" - "));
            final long   cid = course.getId();
            final String lbl = displayName;
            boolean selected  = tabFilterCourse[currentTab] == cid;
            courseCard.addView(makeBeautifulOptionRow(
                    "•", lbl, selected,
                    0xFF065F46, i < allCourses.size() - 1, dp,
                    () -> { tabFilterCourse[currentTab] = cid; applyFilterSort(); sheet.dismiss(); }));
        }
        root.addView(courseCard);

        sv.addView(root);
        sheet.setContentView(sv);
        sheet.show();
    }

    // Sheet helpers

    /**
     * Builds the white padded vertical container the sheet contents sit in.
     */
    private android.widget.LinearLayout buildSheetRoot(float dp) {
        android.widget.LinearLayout root = new android.widget.LinearLayout(requireContext());
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFFFFFFFF);
        root.setPadding((int)(24*dp), (int)(28*dp), (int)(24*dp), (int)(60*dp));
        return root;
    }

    /**
     * Builds the sheet header: the title on the left and a colored pill
     * on the right that shows which tab the sheet applies to.
     */
    private View buildSheetHeader(String title, int tab, float dp) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int)(20*dp));
        row.setLayoutParams(lp);

        // Title takes the remaining horizontal space.
        TextView titleTv = new TextView(requireContext());
        titleTv.setText(title);
        titleTv.setTextSize(26f);
        titleTv.setTextColor(0xFF0A1245);
        titleTv.setTypeface(null, android.graphics.Typeface.BOLD);
        titleTv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(titleTv);

        // Tab pill: a colored chip showing the active tab name.
        String[] tabNames  = { "Pending", "Comp", "All", "Overdue" };
        int[]    tabColors = { 0xFF0369A1, 0xFF065F46, 0xFF37474F, 0xFFB71C1C };
        TextView pill = new TextView(requireContext());
        pill.setText(tabNames[tab]);
        pill.setTextSize(14f);
        pill.setTextColor(0xFFFFFFFF);
        pill.setTypeface(null, android.graphics.Typeface.BOLD);
        pill.setPadding((int)(14*dp), (int)(6*dp), (int)(14*dp), (int)(6*dp));
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(20*dp);
        pillBg.setColor(tabColors[tab]);
        pill.setBackground(pillBg);
        row.addView(pill);
        return row;
    }

    /**
     * Builds the red-tinted "reset" button used at the top of each sheet.
     */
    private View buildResetButton(String label, float dp, Runnable onClick) {
        TextView btn = new TextView(requireContext());
        btn.setText("↺  " + label);
        btn.setTextSize(16f);
        btn.setTextColor(0xFFC62828);
        btn.setTypeface(null, android.graphics.Typeface.BOLD);
        btn.setGravity(android.view.Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(12*dp);
        bg.setColor(0xFFFFEBEE);
        bg.setStroke((int)(1.5f*dp), 0xFFEF9A9A);
        btn.setBackground(bg);
        btn.setPadding((int)(16*dp), (int)(14*dp), (int)(16*dp), (int)(14*dp));
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int)(20*dp));
        btn.setLayoutParams(lp);
        btn.setOnClickListener(v -> onClick.run());
        return btn;
    }

    /**
     * Builds a section header used in the filter sheet: a vertical color
     * bar on the left followed by a bold colored label.
     */
    private View makeBeautifulSectionHeader(String text, int color, float dp) {
        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, (int)(20*dp), 0, (int)(10*dp));
        row.setLayoutParams(lp);

        // Colored vertical bar to the left of the section text.
        View bar = new View(requireContext());
        bar.setBackgroundColor(color);
        android.widget.LinearLayout.LayoutParams barLp =
                new android.widget.LinearLayout.LayoutParams((int)(5*dp), (int)(26*dp));
        barLp.setMarginEnd((int)(10*dp));
        bar.setLayoutParams(barLp);
        row.addView(bar);

        TextView tv = new TextView(requireContext());
        tv.setText(text);
        tv.setTextSize(18f);
        tv.setTextColor(color);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        row.addView(tv);
        return row;
    }

    /**
     * Builds a rounded card container used to group related option rows
     * inside a sheet.
     */
    private android.widget.LinearLayout makeCard(float dp) {
        android.widget.LinearLayout card = new android.widget.LinearLayout(requireContext());
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(18*dp);
        bg.setColor(0xFFF8F9FF);
        bg.setStroke((int)(1.5f*dp), 0xFFDDE2FF);
        card.setBackground(bg);
        android.widget.LinearLayout.LayoutParams lp =
                new android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, (int)(8*dp));
        card.setLayoutParams(lp);
        return card;
    }

    /**
     * Builds one row in a sort or filter card: a radio dot on the left,
     * an icon, then the label, with an optional thin divider underneath.
     * Selected rows get a subtle background tint and adopt the accent
     * color for the dot, icon, and text.
     */
    private View makeBeautifulOptionRow(String icon, String label,
                                        boolean selected, int accentColor,
                                        boolean showDivider, float dp, Runnable onClick) {

        // Wrapper holds the row plus an optional divider underneath.
        android.widget.LinearLayout wrapper = new android.widget.LinearLayout(requireContext());
        wrapper.setOrientation(android.widget.LinearLayout.VERTICAL);

        android.widget.LinearLayout row = new android.widget.LinearLayout(requireContext());
        row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);
        row.setPadding((int)(18*dp), (int)(18*dp), (int)(18*dp), (int)(18*dp));

        // Subtle accent-tinted background for the selected row only.
        if (selected) {
            GradientDrawable selBg = new GradientDrawable();
            selBg.setColor((accentColor & 0x00FFFFFF) | 0x18000000);
            row.setBackground(selBg);
        }

        // Radio dot: filled when selected, outlined when not.
        View dot = new View(requireContext());
        GradientDrawable dotBg = new GradientDrawable();
        dotBg.setShape(GradientDrawable.OVAL);
        dotBg.setColor(selected ? accentColor : 0x00000000);
        dotBg.setStroke((int)(2*dp), selected ? accentColor : 0xFFCCCCCC);
        dot.setBackground(dotBg);
        android.widget.LinearLayout.LayoutParams dotLp =
                new android.widget.LinearLayout.LayoutParams((int)(18*dp), (int)(18*dp));
        dotLp.setMarginEnd((int)(12*dp));
        dot.setLayoutParams(dotLp);
        row.addView(dot);

        // Icon glyph (an emoji or symbol) before the label.
        TextView iconTv = new TextView(requireContext());
        iconTv.setText(icon);
        iconTv.setTextSize(18f);
        iconTv.setTextColor(selected ? accentColor : 0xFF888888);
        android.widget.LinearLayout.LayoutParams iconLp =
                new android.widget.LinearLayout.LayoutParams(
                        (int)(30*dp), android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        iconTv.setLayoutParams(iconLp);
        row.addView(iconTv);

        // Label takes the remaining horizontal space.
        TextView labelTv = new TextView(requireContext());
        labelTv.setText(label);
        labelTv.setTextSize(18f);
        labelTv.setTextColor(selected ? accentColor : 0xFF2C2C2C);
        labelTv.setTypeface(null, android.graphics.Typeface.BOLD);
        labelTv.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
                0, android.widget.LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(labelTv);

        row.setOnClickListener(v -> onClick.run());
        wrapper.addView(row);

        // Thin inset divider so options visually separate inside one card.
        if (showDivider) {
            View divider = new View(requireContext());
            divider.setBackgroundColor(0xFFE8EAFF);
            android.widget.LinearLayout.LayoutParams divLp =
                    new android.widget.LinearLayout.LayoutParams(
                            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, (int)(1*dp));
            divLp.setMarginStart((int)(18*dp));
            divLp.setMarginEnd((int)(18*dp));
            divider.setLayoutParams(divLp);
            wrapper.addView(divider);
        }
        return wrapper;
    }

    // Apply filter and sort

    /**
     * Filters and sorts the latest tab list according to the per tab
     * selections, hands the result to the adapter, and updates the
     * empty state. Scrolls back to the top so the user sees the new
     * first row instead of being stuck mid list.
     */
    private void applyFilterSort() {
        String sort    = tabSortBy[currentTab];
        int    fType   = tabFilterType[currentTab];
        long   fCourse = tabFilterCourse[currentTab];

        // Apply both filters in one pass.
        List<Task> filtered = new ArrayList<>();
        for (Task t : currentFullList) {
            if (fType   != -1  && t.getType()     != fType)   continue;
            if (fCourse != -1L && t.getCourseId() != fCourse) continue;
            filtered.add(t);
        }

        // Sort the survivors. Priority sorts also push completed tasks
        // to the bottom so the active work stays on top.
        switch (sort) {
            case "due_date_asc":
                Collections.sort(filtered, (a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));
                break;
            case "due_date_desc":
                Collections.sort(filtered, (a, b) -> Long.compare(b.getDueDate(), a.getDueDate()));
                break;
            case "priority_desc":
                Collections.sort(filtered, (a, b) -> {
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    return Integer.compare(b.getPriority(), a.getPriority());
                });
                break;
            case "priority_asc":
                Collections.sort(filtered, (a, b) -> {
                    if (a.isCompleted() != b.isCompleted()) return a.isCompleted() ? 1 : -1;
                    return Integer.compare(a.getPriority(), b.getPriority());
                });
                break;
        }

        adapter.submitList(new ArrayList<>(filtered));
        binding.recyclerTasks.post(() -> binding.recyclerTasks.scrollToPosition(0));
        showEmpty(filtered.isEmpty());
    }

    // Tab switching

    /**
     * Switches the active tab. Updates the adapter's tab tint, restyles
     * the tab buttons, and subscribes to the right LiveData query for
     * the selected tab.
     */
    private void switchTab(int tab) {
        currentTab = tab;
        adapter.setCurrentTab(tab);
        highlightTab(tab);

        // Each tab uses its own query so the database does the heavy lifting.
        switch (tab) {
            case TAB_PENDING:
                taskViewModel.getPendingTasks().observe(getViewLifecycleOwner(), tasks -> {
                    currentFullList = tasks != null ? tasks : new ArrayList<>();
                    applyFilterSort();
                });
                break;
            case TAB_COMPLETED:
                taskViewModel.getCompletedTasks().observe(getViewLifecycleOwner(), tasks -> {
                    currentFullList = tasks != null ? tasks : new ArrayList<>();
                    applyFilterSort();
                });
                break;
            case TAB_ALL:
                taskViewModel.getAllTasks().observe(getViewLifecycleOwner(), tasks -> {
                    currentFullList = tasks != null ? tasks : new ArrayList<>();
                    applyFilterSort();
                });
                break;
            case TAB_OVERDUE:
                taskViewModel.getOverdueTasks().observe(getViewLifecycleOwner(), tasks -> {
                    currentFullList = tasks != null ? tasks : new ArrayList<>();
                    applyFilterSort();
                });
                break;
        }
    }

    // Tab highlight

    /**
     * Restyles the four tab buttons so the active one is bigger, fully
     * opaque, and uses its selected background. Also updates the empty
     * state text and subtitle to match the selected tab.
     */
    private void highlightTab(int selectedTab) {
        // Per tab styling: { unselected bg, selected bg, unselected text, selected text }
        int[][] configs = {
                { R.drawable.tab_unselected_bg_blue,  R.drawable.tab_selected_bg_blue,  0xCCFFFFFF, 0xFF0369A1 },
                { R.drawable.tab_unselected_bg_green, R.drawable.tab_selected_bg_green, 0xCCFFFFFF, 0xFF065F46 },
                { R.drawable.tab_unselected_bg_gray,  R.drawable.tab_selected_bg_gray,  0xCCFFFFFF, 0xFF37474F },
                { R.drawable.tab_unselected_bg_red,   R.drawable.tab_selected_bg_red,   0xCCFFFFFF, 0xFFB71C1C },
        };

        TextView[] tabs = {
                binding.tabPending, binding.tabCompleted,
                binding.tabAll, binding.tabOverdue
        };

        // Empty state messages, one per tab, shown when the filtered list is empty.
        String[] emptyMessages = {
                "No pending tasks!", "No completed tasks!",
                "No tasks yet!", "No overdue tasks!"
        };
        String[] emptySubs = {
                "You're all caught up 🎉", "Complete a task to see it here",
                "Tap + to add your first task", "Great job staying on track!"
        };

        float dp = getResources().getDisplayMetrics().density;

        for (int i = 0; i < tabs.length; i++) {
            if (i == selectedTab) {
                // Selected: bigger text, full opacity, more padding.
                tabs[i].setBackgroundResource(configs[i][1]);
                tabs[i].setTextColor(configs[i][3]);
                tabs[i].setTextSize(18f);
                tabs[i].setAlpha(1f);
                tabs[i].setPaddingRelative(
                        (int)(26*dp), (int)(14*dp), (int)(26*dp), (int)(14*dp));
            } else {
                // Unselected: smaller, dimmer, less padding.
                tabs[i].setBackgroundResource(configs[i][0]);
                tabs[i].setTextColor(configs[i][2]);
                tabs[i].setTextSize(15f);
                tabs[i].setAlpha(0.8f);
                tabs[i].setPaddingRelative(
                        (int)(18*dp), (int)(10*dp), (int)(18*dp), (int)(10*dp));
            }
        }

        binding.textEmpty.setText(emptyMessages[selectedTab]);
        binding.textEmptySub.setText(emptySubs[selectedTab]);
    }

    /**
     * Toggles between the empty state block and the RecyclerView.
     */
    private void showEmpty(boolean empty) {
        binding.layoutEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.recyclerTasks.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}