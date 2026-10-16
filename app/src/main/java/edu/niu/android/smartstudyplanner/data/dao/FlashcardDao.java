package edu.niu.android.smartstudyplanner.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import edu.niu.android.smartstudyplanner.data.entities.Flashcard;

import java.util.List;

@Dao
public interface FlashcardDao {

    /**
     * Inserts a new flashcard and returns its auto-generated row id.
     */
    @Insert
    long insert(Flashcard flashcard);

    /**
     * Updates an existing flashcard (matched by primary key).
     */
    @Update
    void update(Flashcard flashcard);

    /**
     * Deletes a flashcard (matched by primary key).
     */
    @Delete
    void delete(Flashcard flashcard);

    /**
     * Returns all flashcards for a course in creation order. UI can observe this directly.
     */
    @Query("SELECT * FROM flashcards WHERE courseId = :courseId ORDER BY createdAt ASC")
    LiveData<List<Flashcard>> getFlashcardsByCourse(long courseId);

    /**
     * Synchronous version that returns a course's deck in random order, for study mode.
     */
    @Query("SELECT * FROM flashcards WHERE courseId = :courseId ORDER BY RANDOM()")
    List<Flashcard> getFlashcardsByCourseSync(long courseId);

    /**
     * Returns only the cards that haven't been mastered yet, in random order.
     */
    @Query("SELECT * FROM flashcards WHERE courseId = :courseId AND mastered = 0 ORDER BY RANDOM()")
    List<Flashcard> getUnmasteredCards(long courseId);

    /**
     * Returns how many cards exist for the given course.
     */
    @Query("SELECT COUNT(*) FROM flashcards WHERE courseId = :courseId")
    int getTotalCount(long courseId);

    /**
     * Returns how many cards in the course are marked mastered.
     */
    @Query("SELECT COUNT(*) FROM flashcards WHERE courseId = :courseId AND mastered = 1")
    int getMasteredCount(long courseId);

    /**
     * Marks a card as mastered and bumps its review count by one.
     */
    @Query("UPDATE flashcards SET mastered = 1, reviewCount = reviewCount + 1 WHERE id = :id")
    void markMastered(long id);

    /**
     * Marks a card as not mastered and bumps its review count by one.
     */
    @Query("UPDATE flashcards SET mastered = 0, reviewCount = reviewCount + 1 WHERE id = :id")
    void markNotMastered(long id);

    /**
     * Clears mastery on every card in a course so the deck can be studied fresh.
     */
    @Query("UPDATE flashcards SET mastered = 0 WHERE courseId = :courseId")
    void resetDeck(long courseId);
}