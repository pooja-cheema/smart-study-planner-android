/**************************************************************
 * Purpose: ViewModel for the Courses screens. Sits between   *
 * CoursesFragment / AddEditCourseActivity and the Repository,*
 * exposing a reactive course list and forwarding insert,     *
 * update, and delete calls. Extends AndroidViewModel so it   *
 * survives configuration changes like screen rotation.       *
 **************************************************************/

package edu.niu.android.smartstudyplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.repository.StudyRepository;

import java.util.List;

public class CourseViewModel extends AndroidViewModel {

    private final StudyRepository repository;

    // Reactive course list. The UI observes this once and Room keeps
    // it in sync automatically as rows are added, edited, or deleted.
    private final LiveData<List<Course>> allCourses;

    public CourseViewModel(@NonNull Application application) {
        super(application);
        repository = new StudyRepository(application);
        allCourses = repository.getAllCourses();
    }

    /**
     * Returns the observable list of all courses for the UI to subscribe to.
     */
    public LiveData<List<Course>> getAllCourses() {
        return allCourses;
    }

    /**
     * Returns a single course by id as observable LiveData. Used by the
     * edit screen to load the existing values into the form.
     */
    public LiveData<Course> getCourseById(long id) {
        return repository.getCourseById(id);
    }

    /**
     * Inserts a new course. The repository handles threading.
     */
    public void insert(Course course) {
        repository.insertCourse(course);
    }

    /**
     * Updates an existing course.
     */
    public void update(Course course) {
        repository.updateCourse(course);
    }

    /**
     * Deletes a course. The CASCADE foreign key in the schema also wipes
     * the course's tasks and flashcards.
     */
    public void delete(Course course) {
        repository.deleteCourse(course);
    }
}