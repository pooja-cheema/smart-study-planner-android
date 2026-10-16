/**************************************************************
 * Purpose: ViewModel for study session data. Used by         *
 * TimerFragment, StudyTimerActivity, StatsFragment, and      *
 * PlannerFragment. Exposes session lists as LiveData,        *
 * provides callback based queries for totals and ranges, and *
 * uses a MediatorLiveData to derive the "minutes studied in  *
 * a range" stream the Planner card observes.                 *
 **************************************************************/

package edu.niu.android.smartstudyplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.data.repository.StudyRepository;

import java.util.List;

public class StudySessionViewModel extends AndroidViewModel {

    private final StudyRepository repository;

    // 10 most recent sessions, shown on the Timer tab.
    private final LiveData<List<StudySession>> recentSessions;

    public StudySessionViewModel(@NonNull Application application) {
        super(application);
        repository = new StudyRepository(application);
        recentSessions = repository.getRecentSessions();
    }

    /**
     * Returns the 10 most recent sessions as observable LiveData.
     */
    public LiveData<List<StudySession>> getRecentSessions() {
        return recentSessions;
    }

    /**
     * Returns sessions tagged with a specific course as observable LiveData.
     */
    public LiveData<List<StudySession>> getSessionsByCourse(long courseId) {
        return repository.getSessionsByCourse(courseId);
    }

    /**
     * Returns sessions whose start time falls within the given range,
     * observable.
     */
    public LiveData<List<StudySession>> getSessionsInRange(long start, long end) {
        return repository.getSessionsInRange(start, end);
    }

    /**
     * Returns total minutes studied in the given range as observable
     * LiveData. Composed from getSessionsInRange via MediatorLiveData,
     * which sums each session's stored seconds and divides by 60. Used
     * by the Planner card to show today's total live.
     */
    public LiveData<Long> getTotalMinutesInRange(long start, long end) {
        // MediatorLiveData turns one LiveData into another by observing
        // the source and re-emitting a derived value.
        MediatorLiveData<Long> result = new MediatorLiveData<>();

        result.addSource(repository.getSessionsInRange(start, end), sessions -> {
            if (sessions == null || sessions.isEmpty()) {
                result.setValue(0L);
                return;
            }

            // Add up every session's durationSeconds, then convert to minutes.
            long totalSeconds = 0;
            for (StudySession s : sessions) {
                totalSeconds += s.getDurationSeconds();
            }

            result.setValue(totalSeconds / 60);
        });

        return result;
    }

    /**
     * Saves a completed session through the repository. The callback
     * delivers the new row id once the insert finishes.
     */
    public void saveSession(StudySession session, StudyRepository.Callback<Long> callback) {
        repository.insertSession(session, callback);
    }

    /**
     * Updates an existing session, e.g. when the user adds notes after saving.
     */
    public void updateSession(StudySession session) {
        repository.updateSession(session);
    }

    /**
     * Loads the total seconds studied since a given timestamp.
     * Powers the summary cards on Stats and Planner.
     */
    public void getTotalStudyHours(long since, StudyRepository.Callback<Long> callback) {
        repository.getTotalStudySecondsSince(since, callback);
    }

    /**
     * Loads all sessions since a timestamp as a plain list. Used to build
     * the daily bar chart on Stats.
     */
    public void getSessionsSince(long since,
                                 StudyRepository.Callback<List<StudySession>> callback) {
        repository.getSessionsSinceAsync(since, callback);
    }

    /**
     * Loads sessions within a date range as a plain list, e.g. to count
     * how many sessions happened this week.
     */
    public void getSessionsInRangeAsync(long start, long end,
                                        StudyRepository.Callback<List<StudySession>> callback) {
        repository.getSessionsInRangeAsync(start, end, callback);
    }

    /**
     * Loads total seconds studied for one course since a timestamp.
     * Powers the per course pie chart slices on Stats.
     */
    public void getCourseStudySeconds(long courseId, long since,
                                      StudyRepository.Callback<Long> callback) {
        repository.getTotalSecondsForCourse(courseId, since, callback);
    }
}