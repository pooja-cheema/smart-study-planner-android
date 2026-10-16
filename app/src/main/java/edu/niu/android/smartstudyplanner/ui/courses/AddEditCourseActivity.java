package edu.niu.android.smartstudyplanner.ui.courses;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.databinding.ActivityAddEditCourseBinding;
import edu.niu.android.smartstudyplanner.utils.ColorUtils;
import edu.niu.android.smartstudyplanner.viewmodel.CourseViewModel;

public class AddEditCourseActivity extends AppCompatActivity {

    // Intent extra key. Pass the course id to edit, omit to add.
    public static final String EXTRA_COURSE_ID = "course_id";

    private ActivityAddEditCourseBinding binding;
    private CourseViewModel viewModel;

    // Loaded course in edit mode, null in add mode.
    private Course existingCourse;

    // Currently selected swatch hex string.
    private String selectedColor;

    // Course id passed in via the intent, or -1 in add mode.
    private long courseId = -1;

    // The 24 swatch views in display order.
    private android.view.View[] colorSwatches;

    // The palette the swatches draw from.
    private final String[] colors = ColorUtils.COURSE_COLORS;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAddEditCourseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(CourseViewModel.class);

        courseId = getIntent().getLongExtra(EXTRA_COURSE_ID, -1);
        boolean isEditMode = courseId != -1;

        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(isEditMode ? "Edit Course" : "Add Course");
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

        selectedColor = colors[0];
        setupColorPicker();

        // In edit mode, load the existing course and fill the form once.
        // The null check on existingCourse stops repeat populates if the
        // LiveData re-emits while the user is editing.
        if (isEditMode) {
            viewModel.getCourseById(courseId).observe(this, course -> {
                if (course != null && existingCourse == null) {
                    existingCourse = course;
                    populateFields(course);
                }
            });
        }

        binding.btnSave.setOnClickListener(v -> saveCourse());
    }

    /**
     * Wires up all 24 swatches with their palette color and a click handler,
     * then selects the first swatch as the default.
     */
    private void setupColorPicker() {
        colorSwatches = new android.view.View[]{
                binding.color0,  binding.color1,  binding.color2,  binding.color3,
                binding.color4,  binding.color5,  binding.color6,  binding.color7,
                binding.color8,  binding.color9,  binding.color10, binding.color11,
                binding.color12, binding.color13, binding.color14, binding.color15,
                binding.color16, binding.color17, binding.color18, binding.color19,
                binding.color20, binding.color21, binding.color22, binding.color23
        };

        for (int i = 0; i < colorSwatches.length && i < colors.length; i++) {
            final int index = i;
            colorSwatches[i].setBackgroundColor(ColorUtils.parseColor(colors[i]));
            colorSwatches[i].setOnClickListener(v -> selectColor(index));
        }

        selectColor(0);
    }

    /**
     * Marks one swatch as picked: it scales up and goes full opacity while
     * the rest dim to half. Stores the chosen hex string in selectedColor.
     */
    private void selectColor(int index) {
        selectedColor = colors[index];
        for (int i = 0; i < colorSwatches.length; i++) {
            if (i == index) {
                colorSwatches[i].setScaleX(1.3f);
                colorSwatches[i].setScaleY(1.3f);
                colorSwatches[i].setAlpha(1f);
            } else {
                colorSwatches[i].setScaleX(1f);
                colorSwatches[i].setScaleY(1f);
                colorSwatches[i].setAlpha(0.5f);
            }
        }
    }

    /**
     * Fills the form fields from an existing course. The stored name is
     * "CODE - Name" so it gets split back into the two inputs the user sees.
     * Also reselects the matching swatch.
     */
    private void populateFields(Course course) {
        String fullName = course.getName();
        if (fullName != null && fullName.contains(" - ")) {
            binding.editCourseCode.setText(
                    fullName.substring(0, fullName.indexOf(" - ")));
            binding.editCourseName.setText(
                    fullName.substring(fullName.indexOf(" - ") + 3));
        } else {
            binding.editCourseName.setText(fullName);
        }

        binding.editInstructor.setText(course.getInstructor());
        binding.editSchedule.setText(course.getSchedule());
        binding.editRoom.setText(course.getRoom());
        binding.editSemester.setText(course.getSemester());

        for (int i = 0; i < colors.length; i++) {
            if (colors[i].equals(course.getColor())) {
                selectColor(i);
                break;
            }
        }
    }

    /**
     * Validates the required fields, then either updates the existing course
     * or inserts a new one through the ViewModel. Shows a toast and finishes
     * the activity on success.
     */
    private void saveCourse() {
        String code = binding.editCourseCode.getText().toString().trim().toUpperCase();
        String name = binding.editCourseName.getText().toString().trim();

        if (code.isEmpty()) {
            binding.editCourseCode.setError("Course code is required");
            return;
        }
        if (name.isEmpty()) {
            binding.editCourseName.setError("Course name is required");
            return;
        }

        // Combine code + name back into the single stored field.
        String fullName   = code + " - " + name;
        String instructor = binding.editInstructor.getText().toString().trim();
        String schedule   = binding.editSchedule.getText().toString().trim();
        String room       = binding.editRoom.getText().toString().trim();
        String semester   = binding.editSemester.getText().toString().trim();

        if (existingCourse != null) {
            existingCourse.setName(fullName);
            existingCourse.setInstructor(instructor);
            existingCourse.setColor(selectedColor);
            existingCourse.setSchedule(schedule);
            existingCourse.setRoom(room);
            existingCourse.setSemester(semester);
            viewModel.update(existingCourse);
            Toast.makeText(this, "Course updated", Toast.LENGTH_SHORT).show();
        } else {
            Course course = new Course(
                    fullName, instructor, selectedColor,
                    schedule, room, semester,
                    System.currentTimeMillis()
            );
            viewModel.insert(course);
            Toast.makeText(this, "Course added", Toast.LENGTH_SHORT).show();
        }

        finish();
    }

    /**
     * Closes the screen when the toolbar back arrow is tapped.
     */
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}