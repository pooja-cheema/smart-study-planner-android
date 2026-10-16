package edu.niu.android.smartstudyplanner.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import edu.niu.android.smartstudyplanner.data.dao.CourseDao;
import edu.niu.android.smartstudyplanner.data.dao.StudySessionDao;
import edu.niu.android.smartstudyplanner.data.dao.TaskDao;
import edu.niu.android.smartstudyplanner.data.database.StudyPlannerDatabase;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.data.entities.Task;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StudyRepository {

    private final CourseDao courseDao;
    private final TaskDao taskDao;
    private final StudySessionDao sessionDao;

    // Background thread pool used for all writes and synchronous reads.
    private final ExecutorService executor;

    /**
     * Builds the repository by grabbing the three DAOs from the database
     * singleton and starting a small thread pool for background work.
     */
    public StudyRepository(Application application) {
        StudyPlannerDatabase db = StudyPlannerDatabase.getInstance(application);
        courseDao  = db.courseDao();
        taskDao    = db.taskDao();
        sessionDao = db.studySessionDao();
        executor   = Executors.newFixedThreadPool(4);
    }

    // Courses

    /**
     * Returns all courses as observable LiveData.
     */
    public LiveData<List<Course>> getAllCourses() {
        return courseDao.getAllCourses();
    }

    /**
     * Returns a single course by id as observable LiveData.
     */
    public LiveData<Course> getCourseById(long id) {
        return courseDao.getCourseById(id);
    }

    /**
     * Inserts a course on the background thread.
     */
    public void insertCourse(Course course) {
        executor.execute(() -> courseDao.insert(course));
    }

    /**
     * Updates a course on the background thread.
     */
    public void updateCourse(Course course) {
        executor.execute(() -> courseDao.update(course));
    }

    /**
     * Deletes a course on the background thread. Tasks and flashcards
     * for the course are removed automatically via foreign key cascade.
     */
    public void deleteCourse(Course course) {
        executor.execute(() -> courseDao.delete(course));
    }

    /**
     * Loads all courses on the background thread and returns them via callback.
     */
    public void getCoursesAsync(Callback<List<Course>> callback) {
        executor.execute(() -> callback.onResult(courseDao.getAllCoursesSync()));
    }

    // Tasks

    /**
     * Returns every task as observable LiveData.
     */
    public LiveData<List<Task>> getAllTasks() {
        return taskDao.getAllTasks();
    }

    /**
     * Returns incomplete tasks as observable LiveData. Powers the Pending tab.
     */
    public LiveData<List<Task>> getPendingTasks() {
        return taskDao.getPendingTasks();
    }

    /**
     * Returns completed tasks as observable LiveData. Powers the Completed tab.
     */
    public LiveData<List<Task>> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    /**
     * Returns tasks for one course as observable LiveData.
     */
    public LiveData<List<Task>> getTasksByCourse(long courseId) {
        return taskDao.getTasksByCourse(courseId);
    }

    /**
     * Returns incomplete tasks due within the given today range.
     */
    public LiveData<List<Task>> getTasksDueToday(long start, long end) {
        return taskDao.getTasksDueToday(start, end);
    }

    /**
     * Returns incomplete tasks due within the given week range.
     */
    public LiveData<List<Task>> getTasksDueThisWeek(long start, long end) {
        return taskDao.getTasksDueThisWeek(start, end);
    }

    /**
     * Returns incomplete tasks whose due date has already passed.
     */
    public LiveData<List<Task>> getOverdueTasks(long now) {
        return taskDao.getOverdueTasks(now);
    }

    /**
     * Inserts a task on the background thread. Caller does not get the new id back.
     */
    public void insertTask(Task task) {
        executor.execute(() -> taskDao.insert(task));
    }

    /**
     * Inserts a task on the background thread and returns the generated id
     * via callback. Used by AddEditTaskActivity when scheduling a notification,
     * since the alarm needs the row id.
     */
    public void insertTaskAndGetId(Task task, Callback<Long> callback) {
        executor.execute(() -> {
            long id = taskDao.insert(task);
            if (callback != null) callback.onResult(id);
        });
    }

    /**
     * Updates a task on the background thread.
     */
    public void updateTask(Task task) {
        executor.execute(() -> taskDao.update(task));
    }

    /**
     * Deletes a task on the background thread.
     */
    public void deleteTask(Task task) {
        executor.execute(() -> taskDao.delete(task));
    }

    /**
     * Marks a task complete and stores when it was completed.
     */
    public void markTaskCompleted(long taskId, long completedAt) {
        executor.execute(() -> taskDao.markTaskCompleted(taskId, completedAt));
    }

    /**
     * Reverts a task back to incomplete and clears its completion time.
     */
    public void markTaskIncomplete(long taskId) {
        executor.execute(() -> taskDao.markTaskIncomplete(taskId));
    }

    /**
     * Loads one task by id on the background thread and returns it via callback.
     */
    public void getTaskByIdAsync(long id, Callback<Task> callback) {
        executor.execute(() -> callback.onResult(taskDao.getTaskByIdSync(id)));
    }

    // Study sessions

    /**
     * Returns every session as observable LiveData.
     */
    public LiveData<List<StudySession>> getAllSessions() {
        return sessionDao.getAllSessions();
    }

    /**
     * Returns the 10 most recent sessions as observable LiveData.
     * Used by the Timer tab's recent list.
     */
    public LiveData<List<StudySession>> getRecentSessions() {
        return sessionDao.getRecentSessions();
    }

    /**
     * Returns sessions tagged with the given course as observable LiveData.
     */
    public LiveData<List<StudySession>> getSessionsByCourse(long courseId) {
        return sessionDao.getSessionsByCourse(courseId);
    }

    /**
     * Returns sessions whose start time falls in the given range, observable.
     */
    public LiveData<List<StudySession>> getSessionsInRange(long start, long end) {
        return sessionDao.getSessionsInRange(start, end);
    }

    /**
     * Inserts a session on the background thread and returns the generated id.
     */
    public void insertSession(StudySession session, Callback<Long> callback) {
        executor.execute(() -> {
            long id = sessionDao.insert(session);
            if (callback != null) callback.onResult(id);
        });
    }

    /**
     * Updates an existing session, e.g. when the user adds notes after saving.
     */
    public void updateSession(StudySession session) {
        executor.execute(() -> sessionDao.update(session));
    }

    /**
     * Loads the total seconds studied since a timestamp on the background thread.
     * Used by the Stats and Planner summary cards.
     */
    public void getTotalStudySecondsSince(long since, Callback<Long> callback) {
        executor.execute(() ->
                callback.onResult(sessionDao.getTotalStudySecondsSince(since)));
    }

    /**
     * Loads all sessions since a timestamp as a plain list. Used to build
     * the daily bar chart on the Stats screen.
     */
    public void getSessionsSinceAsync(long since, Callback<List<StudySession>> callback) {
        executor.execute(() ->
                callback.onResult(sessionDao.getSessionsSince(since)));
    }

    /**
     * Loads sessions in a range as a plain list, e.g. to count this week's sessions.
     */
    public void getSessionsInRangeAsync(long start, long end,
                                        Callback<List<StudySession>> callback) {
        executor.execute(() ->
                callback.onResult(sessionDao.getSessionsInRangeSync(start, end)));
    }

    /**
     * Loads the total seconds studied for one course since a timestamp.
     * Used to build the per course pie chart slices on the Stats screen.
     */
    public void getTotalSecondsForCourse(long courseId, long since,
                                         Callback<Long> callback) {
        executor.execute(() -> callback.onResult(
                sessionDao.getTotalStudySecondsForCourseSince(courseId, since)));
    }

    /**
     * Generic callback used by repository methods that load data on the
     * background thread. Implementations are responsible for posting back
     * to the main thread before touching the UI.
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}