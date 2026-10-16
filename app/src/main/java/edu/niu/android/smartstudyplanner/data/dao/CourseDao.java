package edu.niu.android.smartstudyplanner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import edu.niu.android.smartstudyplanner.data.entities.Course;

import java.util.List;

@Dao
public interface CourseDao {

    /**
     * Inserts a new course and returns the auto-generated row id.
     */
    @Insert
    long insert(Course course);

    /**
     * Updates an existing course (matched by primary key)
     */
    @Update
    void update(Course course);

    /**
     * Deletes a course (matched by primary key).
     */
    @Delete
    void delete(Course course);

    /**
     * Returns all courses sorted by name. UI can observe this directly.
     */
    @Query("SELECT * FROM courses ORDER BY name ASC")
    LiveData<List<Course>> getAllCourses();

    /**
     * Same as getAllCourses() but synchronous. Call from a background thread only.
     */
    @Query("SELECT * FROM courses ORDER BY name ASC")
    List<Course> getAllCoursesSync();

    /**
     * Returns a single course by id as observable LiveData.
     */
    @Query("SELECT * FROM courses WHERE id = :id")
    LiveData<Course> getCourseById(long id);

    /**
     * Synchronous version of getCourseById. Call from a background thread only.
     */
    @Query("SELECT * FROM courses WHERE id = :id")
    Course getCourseByIdSync(long id);

    /**
     * Returns how many courses are currently stored.
     */
    @Query("SELECT COUNT(*) FROM courses")
    int getCourseCount();
}