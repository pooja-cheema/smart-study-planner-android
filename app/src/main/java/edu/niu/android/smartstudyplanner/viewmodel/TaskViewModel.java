/**************************************************************
 * Purpose: ViewModel for the Tasks screens. Used by          *
 * TasksFragment, AddEditTaskActivity, CountdownFragment, and *
 * the Planner dashboard. Exposes filtered task lists as      *
 * LiveData (all, pending, completed, due today, due this     *
 * week, overdue) and forwards inserts, updates, and complete *
 * toggles to the Repository.                                 *
 **************************************************************/

package edu.niu.android.smartstudyplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import edu.niu.android.smartstudyplanner.data.entities.Task;
import edu.niu.android.smartstudyplanner.data.repository.StudyRepository;
import edu.niu.android.smartstudyplanner.utils.DateUtils;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {

    private final StudyRepository repository;

    // Pre fetched lists exposed to the UI. Room keeps each one in sync
    // automatically as the underlying tasks table changes.
    private final LiveData<List<Task>> allTasks;
    private final LiveData<List<Task>> pendingTasks;
    private final LiveData<List<Task>> completedTasks;

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository     = new StudyRepository(application);
        allTasks       = repository.getAllTasks();
        pendingTasks   = repository.getPendingTasks();
        completedTasks = repository.getCompletedTasks();
    }

    /**
     * Returns every task as observable LiveData.
     */
    public LiveData<List<Task>> getAllTasks()       { return allTasks; }

    /**
     * Returns incomplete tasks as observable LiveData. Powers the Pending tab.
     */
    public LiveData<List<Task>> getPendingTasks()   { return pendingTasks; }

    /**
     * Returns completed tasks as observable LiveData. Powers the Completed tab.
     */
    public LiveData<List<Task>> getCompletedTasks() { return completedTasks; }

    /**
     * Returns all tasks for one course as observable LiveData.
     */
    public LiveData<List<Task>> getTasksByCourse(long courseId) {
        return repository.getTasksByCourse(courseId);
    }

    /**
     * Returns incomplete tasks due within today's range as observable LiveData.
     */
    public LiveData<List<Task>> getTasksDueToday() {
        long[] range = DateUtils.getTodayRange();
        return repository.getTasksDueToday(range[0], range[1]);
    }

    /**
     * Returns incomplete tasks due within the current week as observable LiveData.
     */
    public LiveData<List<Task>> getTasksDueThisWeek() {
        long[] range = DateUtils.getWeekRange();
        return repository.getTasksDueThisWeek(range[0], range[1]);
    }

    /**
     * Returns incomplete tasks whose due date has already passed.
     */
    public LiveData<List<Task>> getOverdueTasks() {
        return repository.getOverdueTasks(System.currentTimeMillis());
    }

    /**
     * Inserts a new task. Caller does not get the new id back.
     */
    public void insert(Task task) {
        repository.insertTask(task);
    }

    /**
     * Inserts a new task and returns the generated id via callback. Used
     * by AddEditTaskActivity when scheduling a notification, since the
     * alarm uses the row id as its request code.
     */
    public void insertAndGetId(Task task, StudyRepository.Callback<Long> callback) {
        repository.insertTaskAndGetId(task, callback);
    }

    /**
     * Updates an existing task.
     */
    public void update(Task task)  { repository.updateTask(task); }

    /**
     * Deletes a task.
     */
    public void delete(Task task)  { repository.deleteTask(task); }

    /**
     * Marks the task complete and stamps the current time as the completion time.
     */
    public void markCompleted(long taskId) {
        repository.markTaskCompleted(taskId, System.currentTimeMillis());
    }

    /**
     * Reverts a task back to incomplete and clears its completion time.
     */
    public void markIncomplete(long taskId) {
        repository.markTaskIncomplete(taskId);
    }

    /**
     * Loads one task by id on a background thread and returns it via callback.
     * Used by the edit screen to populate the form.
     */
    public void getTaskById(long id, StudyRepository.Callback<Task> callback) {
        repository.getTaskByIdAsync(id, callback);
    }
}