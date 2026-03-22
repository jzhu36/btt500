package com.btt500.app.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {AnswerRecord.class, QuizSession.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract AnswerRecordDao answerRecordDao();
    public abstract QuizSessionDao quizSessionDao();

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `quiz_sessions` ("
                    + "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                    + "`totalQuestions` INTEGER NOT NULL, "
                    + "`answeredCount` INTEGER NOT NULL, "
                    + "`correctCount` INTEGER NOT NULL, "
                    + "`questionIdsCsv` TEXT, "
                    + "`resultsCsv` TEXT, "
                    + "`isCompleted` INTEGER NOT NULL, "
                    + "`createdAt` INTEGER NOT NULL, "
                    + "`completedAt` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "btt500.db"
                    ).addMigrations(MIGRATION_1_2)
                     .allowMainThreadQueries()
                     .build();
                }
            }
        }
        return INSTANCE;
    }
}
