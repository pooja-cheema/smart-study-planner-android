package edu.niu.android.smartstudyplanner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import edu.niu.android.smartstudyplanner.data.entities.StudySession;

import java.util.List;

@Dao
public interface StudySessionDao {

    /**
     * Inserts a new study session and returns its auto-generated row id.
     */
    @Insert
    long insert(StudySession session);

    /**
     * Updates an existing session (matched by primary key).
     */
    @Update
    void update(StudySession session);

    /**
     * Deletes a session (matched by primary key).
     */
    @Delete
    void delete(StudySession session);

    /**
     * Returns all sessions newest-first. UI can observe this directly.
     */
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    LiveData<List<StudySession>> getAllSessions();

    /**
     * Returns all sessions for a single course, newest-first.
     */
    @Query("SELECT * FROM study_sessions WHERE courseId = :courseId ORDER BY startTime DESC")
    LiveData<List<StudySession>> getSessionsByCourse(long courseId);

    /**
     * Returns sessions whose start time falls within the given range, oldest-first.
     */
    @Query("SELECT * FROM study_sessions WHERE startTime >= :start AND startTime <= :end ORDER BY startTime ASC")
    LiveData<List<StudySession>> getSessionsInRange(long start, long end);

    /**
     * Synchronous version of getSessionsInRange. Call from a background thread only.
     */
    @Query("SELECT * FROM study_sessions WHERE startTime >= :start AND startTime <= :end ORDER BY startTime ASC")
    List<StudySession> getSessionsInRangeSync(long start, long end);

    /**
     * Returns the total seconds studied since the given timestamp, or 0 if none.
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE startTime >= :since")
    long getTotalStudySecondsSince(long since);

    /**
     * Returns the total seconds studied for one course since the given timestamp.
     */
    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM study_sessions WHERE courseId = :courseId AND startTime >= :since")
    long getTotalStudySecondsForCourseSince(long courseId, long since);

    /**
     * Returns all sessions since the given timestamp as a plain list, oldest-first.
     */
    @Query("SELECT * FROM study_sessions WHERE startTime >= :since ORDER BY startTime ASC")
    List<StudySession> getSessionsSince(long since);

    /**
     * Returns the 10 most recent sessions. Used for the Timer tab's recent list.
     */
    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC LIMIT 10")
    LiveData<List<StudySession>> getRecentSessions();

    /**
     * Returns how many sessions have been recorded since the given timestamp.
     */
    @Query("SELECT COUNT(*) FROM study_sessions WHERE startTime >= :since")
    int getSessionCountSince(long since);
}