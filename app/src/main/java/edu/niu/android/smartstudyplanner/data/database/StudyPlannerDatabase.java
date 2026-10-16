package edu.niu.android.smartstudyplanner.data.database;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteDatabase;

import java.util.concurrent.Executors;

import edu.niu.android.smartstudyplanner.data.dao.CourseDao;
import edu.niu.android.smartstudyplanner.data.dao.FlashcardDao;
import edu.niu.android.smartstudyplanner.data.dao.StudySessionDao;
import edu.niu.android.smartstudyplanner.data.dao.TaskDao;
import edu.niu.android.smartstudyplanner.data.entities.Course;
import edu.niu.android.smartstudyplanner.data.entities.Flashcard;
import edu.niu.android.smartstudyplanner.data.entities.StudySession;
import edu.niu.android.smartstudyplanner.data.entities.Task;

@Database(
        entities = {Course.class, Task.class, StudySession.class, Flashcard.class},
        version = 3,
        exportSchema = false
)
public abstract class StudyPlannerDatabase extends RoomDatabase {

    private static volatile StudyPlannerDatabase INSTANCE;

    public abstract CourseDao courseDao();
    public abstract TaskDao taskDao();
    public abstract StudySessionDao studySessionDao();
    public abstract FlashcardDao flashcardDao();

    /**
     * Returns the singleton database instance, creating it on first call.
     * Uses double checked locking so the build only runs once across threads.
     */
    public static StudyPlannerDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (StudyPlannerDatabase.class) {
                if (INSTANCE == null) {

                    final Context appContext = context.getApplicationContext();

                    // Holds the database reference so the onCreate callback can
                    // pass it to the seeder. The callback runs while build() is
                    // still finishing, so INSTANCE itself isn't safe to read yet.
                    final StudyPlannerDatabase[] dbHolder = new StudyPlannerDatabase[1];

                    dbHolder[0] = Room.databaseBuilder(
                                    appContext,
                                    StudyPlannerDatabase.class,
                                    "study_planner_db")
                            .fallbackToDestructiveMigration()
                            .addCallback(new RoomDatabase.Callback() {
                                @Override
                                public void onCreate(
                                        @NonNull SupportSQLiteDatabase db) {
                                    super.onCreate(db);
                                    Executors.newSingleThreadExecutor().execute(() -> {
                                        try {
                                            // Small delay so build() has finished
                                            // and dbHolder[0] is fully assigned.
                                            Thread.sleep(500);
                                        } catch (InterruptedException e) {
                                            Thread.currentThread().interrupt();
                                        }
                                        DatabaseSeeder.seedIfNeeded(
                                                appContext, dbHolder[0]);
                                    });
                                }
                            })
                            .build();

                    INSTANCE = dbHolder[0];
                }
            }
        }
        return INSTANCE;
    }
}