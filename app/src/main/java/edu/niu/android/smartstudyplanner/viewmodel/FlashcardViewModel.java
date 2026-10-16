/**************************************************************
 * Purpose: ViewModel for the Flashcards screen. Wraps the    *
 * FlashcardDao directly (it doesn't go through StudyRepository*
 * because flashcards are scoped to a single screen) and runs *
 * every write on a single thread executor so the UI thread   *
 * never blocks. Mastery counts that the UI needs but can't   *
 * observe as LiveData are returned through a Callback.       *
 **************************************************************/

package edu.niu.android.smartstudyplanner.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.niu.android.smartstudyplanner.data.database.StudyPlannerDatabase;
import edu.niu.android.smartstudyplanner.data.dao.FlashcardDao;
import edu.niu.android.smartstudyplanner.data.entities.Flashcard;

public class FlashcardViewModel extends AndroidViewModel {

    private final FlashcardDao flashcardDao;

    // Single thread executor so writes run in order and never block the UI thread.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public FlashcardViewModel(@NonNull Application application) {
        super(application);
        flashcardDao = StudyPlannerDatabase
                .getInstance(application)
                .flashcardDao();
    }

    /**
     * Returns the cards for a course as observable LiveData. The UI
     * subscribes once and Room keeps it in sync automatically.
     */
    public LiveData<List<Flashcard>> getFlashcardsByCourse(long courseId) {
        return flashcardDao.getFlashcardsByCourse(courseId);
    }

    /**
     * Inserts a new flashcard on the background thread.
     */
    public void insert(Flashcard flashcard) {
        executor.execute(() -> flashcardDao.insert(flashcard));
    }

    /**
     * Updates a flashcard on the background thread.
     */
    public void update(Flashcard flashcard) {
        executor.execute(() -> flashcardDao.update(flashcard));
    }

    /**
     * Deletes a flashcard on the background thread.
     */
    public void delete(Flashcard flashcard) {
        executor.execute(() -> flashcardDao.delete(flashcard));
    }

    /**
     * Marks the card as mastered and increments its review count.
     */
    public void markMastered(long id) {
        executor.execute(() -> flashcardDao.markMastered(id));
    }

    /**
     * Marks the card as not mastered and increments its review count.
     */
    public void markNotMastered(long id) {
        executor.execute(() -> flashcardDao.markNotMastered(id));
    }

    /**
     * Clears mastery on every card in the course so the deck can be studied fresh.
     */
    public void resetDeck(long courseId) {
        executor.execute(() -> flashcardDao.resetDeck(courseId));
    }

    /**
     * Loads how many cards in a course are mastered and returns it via callback.
     */
    public void getMasteredCount(long courseId, Callback<Integer> callback) {
        executor.execute(() -> callback.onResult(flashcardDao.getMasteredCount(courseId)));
    }

    /**
     * Loads the total card count for a course and returns it via callback.
     */
    public void getTotalCount(long courseId, Callback<Integer> callback) {
        executor.execute(() -> callback.onResult(flashcardDao.getTotalCount(courseId)));
    }

    /**
     * Generic single value callback used for the count helpers above.
     * The implementation is responsible for hopping back to the UI thread
     * before touching any views.
     */
    public interface Callback<T> {
        void onResult(T result);
    }
}