/**************************************************************
 * Purpose: Form screen for adding a new task or editing an   *
 * existing one. Edit mode is triggered by passing the task   *
 * id through the EXTRA_TASK_ID intent extra; otherwise add   *
 * mode. Lets the user pick type, priority, parent course,    *
 * and due date/time, and toggle a deadline reminder that     *
 * fires 24 hours before the task is due.                     *
 **************************************************************/

package edu.niu.android.smartstudyplanner.ui.tasks;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.databinding.ActivityAddEditTaskBinding;
import edu.niu.android.smartstudyplanner.notifications.NotificationHelper;
import edu.niu.android.smartstudyplanner.utils.DateUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;
import edu.niu.android.smartstudyplanner.viewmodel.TaskViewModel;

public class AddEditTaskActivity extends AppCompatActivity {

    // Intent extra key. Pass the task id to edit, omit to add.
    public static final String EXTRA_TASK_ID = "task_id";

    private ActivityAddEditTaskBinding binding;
    private TaskViewModel taskViewModel;
    private CourseViewModel courseViewModel;

    // Loaded task in edit mode, null in add mode.
    private Task existingTask;

    // Task id passed in via the intent, or 1 in add mode.
    private long taskId = -1;

    // Holds the chosen due date and time across the two pickers.
    private Calendar dueDateCalendar = Calendar.getInstance();

    // Course list backing the spinner; index 0 is a "select a course" placeholder.
    private List<Course> courses     = new ArrayList<>();
    private List<String> courseNames = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditTaskBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        taskViewModel   = new ViewModelProvider(this).get(TaskViewModel.class);
        courseViewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        // Decide which mode we're in based on whether an id was passed.
        taskId = getIntent().getLongExtra(EXTRA_TASK_ID, -1);
        boolean isEditMode = taskId != -1;

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Task" : "Add Task");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setTitleTextColor(0xFFFFFFFF);

        // Tinting the back arrow has to happen after the toolbar lays out
        // so getNavigationIcon() actually returns it.
        binding.toolbar.post(() -> {
            if (binding.toolbar.getNavigationIcon() != null) {
                binding.toolbar.getNavigationIcon().setTint(0xFFFFFFFF);
            }
        });

        setupSpinners();
        setupDateTimePicker();

        // In edit mode, load the existing task and prefill the form.
        if (isEditMode) {
            taskViewModel.getTaskById(taskId, task -> {
                if (task != null) {
                    existingTask = task;
                    runOnUiThread(() -> populateFields(task));
                }
            });
        }

        binding.btnSave.setOnClickListener(v -> saveTask());
    }

    /**
     * Builds the three spinners on the form. Type and priority are simple
     * static lists; the course spinner is populated reactively from the
     * database and re-selects the existing task's course in edit mode.
     */
    private void setupSpinners() {
        // Type spinner: matches the Task.TYPE_* constants by index order.
        String[] types = {"Assignment", "Exam", "Quiz", "Project", "Other"};
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, types);
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerType.setAdapter(typeAdapter);

        // Priority spinner. Default to Medium (index 1) for new tasks.
        String[] priorities = {"Low", "Medium", "High"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, priorities);
        priorityAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerPriority.setAdapter(priorityAdapter);
        binding.spinnerPriority.setSelection(1);

        // Course spinner is reactive: when courses change in the DB, the
        // dropdown rebuilds. Index 0 is a placeholder so the user is forced
        // to make an active choice instead of accidentally accepting the
        // first course in the list.
        courseViewModel.getAllCourses().observe(this, courseList -> {
            courses.clear();
            courseNames.clear();

            courseNames.add("-- Select a Course --");
            courses.add(null);

            for (Course c : courseList) {
                courses.add(c);
                courseNames.add(c.getName());
            }

            ArrayAdapter<String> courseAdapter = new ArrayAdapter<>(
                    this, android.R.layout.simple_spinner_item, courseNames);
            courseAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerCourse.setAdapter(courseAdapter);

            // In edit mode, jump the spinner to the task's existing course.
            if (existingTask != null) {
                for (int i = 0; i < courses.size(); i++) {
                    Course c = courses.get(i);
                    if (c != null && c.getId() == existingTask.getCourseId()) {
                        binding.spinnerCourse.setSelection(i);
                        break;
                    }
                }
            }
        });
    }

    /**
     * Wires up the date/time picker button. Tapping it shows a date picker
     * first, then chains into a time picker once a date is chosen, so the
     * user always provides both in one flow.
     */
    private void setupDateTimePicker() {
        updateDueDateButton();

        binding.btnPickDate.setOnClickListener(v -> {
            // First dialog: pick the date.
            new DatePickerDialog(this, (datePicker, year, month, day) -> {
                dueDateCalendar.set(Calendar.YEAR, year);
                dueDateCalendar.set(Calendar.MONTH, month);
                dueDateCalendar.set(Calendar.DAY_OF_MONTH, day);

                // Second dialog: pick the time on the date that was just chosen.
                new TimePickerDialog(this, (timePicker, hour, minute) -> {
                    dueDateCalendar.set(Calendar.HOUR_OF_DAY, hour);
                    dueDateCalendar.set(Calendar.MINUTE, minute);
                    dueDateCalendar.set(Calendar.SECOND, 0);
                    updateDueDateButton();

                }, dueDateCalendar.get(Calendar.HOUR_OF_DAY),
                        dueDateCalendar.get(Calendar.MINUTE),
                        false
                ).show();

            }, dueDateCalendar.get(Calendar.YEAR),
                    dueDateCalendar.get(Calendar.MONTH),
                    dueDateCalendar.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    /**
     * Refreshes the date/time button label to match the current calendar
     * value. Called on first display and after each time the user picks
     * a new date.
     */
    private void updateDueDateButton() {
        binding.btnPickDate.setText(
                DateUtils.formatDateTime(dueDateCalendar.getTimeInMillis()));
    }

    /**
     * Fills the form fields from an existing task. Course selection is
     * handled separately in setupSpinners once the courses LiveData
     * resolves.
     */
    private void populateFields(Task task) {
        binding.editTaskTitle.setText(task.getTitle());
        binding.editTaskDescription.setText(task.getDescription());
        binding.spinnerType.setSelection(task.getType());
        binding.spinnerPriority.setSelection(task.getPriority());
        dueDateCalendar.setTimeInMillis(task.getDueDate());
        updateDueDateButton();
        binding.switchNotification.setChecked(task.isNotificationSet());
    }

    /**
     * Validates the form, saves the task, and (re)schedules its deadline
     * notification. In add mode we have to wait for the row id before
     * scheduling because the alarm uses the id as its request code.
     */
    private void saveTask() {
        String title = binding.editTaskTitle.getText().toString().trim();

        if (title.isEmpty()) {
            binding.editTaskTitle.setError("Title is required");
            return;
        }

        String  description = binding.editTaskDescription.getText().toString().trim();
        int     type        = binding.spinnerType.getSelectedItemPosition();
        int     priority    = binding.spinnerPriority.getSelectedItemPosition();
        long    dueDate     = dueDateCalendar.getTimeInMillis();
        boolean notify      = binding.switchNotification.isChecked();

        // Course is required; index 0 is the placeholder "select a course".
        int    coursePos      = binding.spinnerCourse.getSelectedItemPosition();
        Course selectedCourse = courses.get(coursePos);
        long   courseId;

        if (selectedCourse != null) {
            courseId = selectedCourse.getId();
        } else {
            Toast.makeText(this, "Please select a course", Toast.LENGTH_SHORT).show();
            return;
        }

        if (existingTask != null) {
            // Edit mode: mutate the loaded task in place and persist it.
            existingTask.setTitle(title);
            existingTask.setDescription(description);
            existingTask.setType(type);
            existingTask.setPriority(priority);
            existingTask.setDueDate(dueDate);
            existingTask.setCourseId(courseId);
            existingTask.setNotificationSet(notify);
            taskViewModel.update(existingTask);

            // Reschedule or cancel the deadline reminder based on the toggle.
            // Reminder fires 24 hours before due; if that's already past we
            // skip it and let the user know.
            if (notify) {
                long triggerAt = dueDate - (24 * 60 * 60 * 1000L);
                if (triggerAt > System.currentTimeMillis()) {
                    NotificationHelper.scheduleTaskNotification(
                            this, existingTask, triggerAt);
                } else {
                    Toast.makeText(this,
                            "Due date is less than 24h away - notification skipped",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                NotificationHelper.cancelTaskNotification(this, existingTask.getId());
            }

            Toast.makeText(this, "Task updated", Toast.LENGTH_SHORT).show();

        } else {
            // Add mode: build a new Task and insert it.
            Task newTask = new Task(
                    courseId, title, description,
                    type, priority, dueDate,
                    System.currentTimeMillis()
            );
            newTask.setNotificationSet(notify);

            if (notify) {
                long triggerAt = dueDate - (24 * 60 * 60 * 1000L);
                if (triggerAt > System.currentTimeMillis()) {
                    // Insert via the callback variant so we get the new row
                    // id back; the alarm uses the id as its request code so
                    // multiple task alarms don't overwrite each other.
                    taskViewModel.insertAndGetId(newTask, id -> {
                        newTask.setId(id);
                        NotificationHelper.scheduleTaskNotification(
                                AddEditTaskActivity.this, newTask, triggerAt);
                    });
                } else {
                    taskViewModel.insert(newTask);
                    Toast.makeText(this,
                            "Due date is less than 24h away - notification skipped",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                taskViewModel.insert(newTask);
            }

            Toast.makeText(this, "Task added", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}