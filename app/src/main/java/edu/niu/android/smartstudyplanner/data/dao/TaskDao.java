package edu.niu.android.smartstudyplanner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import edu.niu.android.smartstudyplanner.data.entities.Task;

import java.util.List;

@Dao
public interface TaskDao {

    /**
     * Inserts a new task and returns its auto-generated row id.
     */
    @Insert
    long insert(Task task);

    /**
     * Updates an existing task (matched by primary key).
     */
    @Update
    void update(Task task);

    /**
     * Deletes a task (matched by primary key).
     */
    @Delete
    void delete(Task task);

    /**
     * Returns every task sorted by due date. UI can observe this directly.
     */
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC")
    LiveData<List<Task>> getAllTasks();

    /**
     * Returns incomplete tasks sorted by due date. Powers the Pending tab.
     */
    @Query("SELECT * FROM tasks WHERE completed = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getPendingTasks();

    /**
     * Returns completed tasks sorted by completion time. Powers the Completed tab.
     */
    @Query("SELECT * FROM tasks WHERE completed = 1 ORDER BY completedAt DESC")
    LiveData<List<Task>> getCompletedTasks();

    /**
     * Returns all tasks for one course, sorted by due date.
     */
    @Query("SELECT * FROM tasks WHERE courseId = :courseId ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByCourse(long courseId);

    /**
     * Returns one task by id. Synchronous - call from a background thread only.
     */
    @Query("SELECT * FROM tasks WHERE id = :id")
    Task getTaskByIdSync(long id);

    /**
     * Returns incomplete tasks whose due date falls within today's range.
     */
    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueDate >= :startOfDay AND dueDate <= :endOfDay ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksDueToday(long startOfDay, long endOfDay);

    /**
     * Returns incomplete tasks whose due date falls within the current week.
     */
    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueDate >= :startOfWeek AND dueDate <= :endOfWeek ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksDueThisWeek(long startOfWeek, long endOfWeek);

    /**
     * Returns incomplete tasks whose due date has already passed.
     */
    @Query("SELECT * FROM tasks WHERE completed = 0 AND dueDate < :now ORDER BY dueDate ASC")
    LiveData<List<Task>> getOverdueTasks(long now);

    /**
     * Returns future incomplete tasks that have a reminder turned on.
     * Used by NotificationReceiver after device reboot to rebuild alarms.
     */
    @Query("SELECT * FROM tasks WHERE completed = 0 AND notificationSet = 1 AND dueDate > :now")
    List<Task> getTasksNeedingNotification(long now);

    /**
     * Marks the given task complete and stores when it was completed.
     */
    @Query("UPDATE tasks SET completed = 1, completedAt = :completedAt WHERE id = :taskId")
    void markTaskCompleted(long taskId, long completedAt);

    /**
     * Reverts a task back to incomplete and clears its completion time.
     */
    @Query("UPDATE tasks SET completed = 0, completedAt = 0 WHERE id = :taskId")
    void markTaskIncomplete(long taskId);

    /**
     * Returns how many tasks have been completed since the given timestamp.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 1 AND completedAt >= :since")
    int getCompletedCountSince(long since);

    /**
     * Returns how many tasks are still pending.
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE completed = 0")
    int getPendingTaskCount();
}