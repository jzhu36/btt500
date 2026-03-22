package com.btt500.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface QuizSessionDao {

    @Insert
    long insert(QuizSession session);

    @Update
    void update(QuizSession session);

    @Query("SELECT * FROM quiz_sessions WHERE id = :id")
    QuizSession getById(long id);

    /** Get the most recent incomplete session, if any */
    @Query("SELECT * FROM quiz_sessions WHERE isCompleted = 0 ORDER BY createdAt DESC LIMIT 1")
    QuizSession getIncompleteSession();

    /** Get all completed sessions, newest first */
    @Query("SELECT * FROM quiz_sessions WHERE isCompleted = 1 ORDER BY completedAt DESC")
    List<QuizSession> getCompletedSessions();

    /** Get all sessions, newest first */
    @Query("SELECT * FROM quiz_sessions ORDER BY createdAt DESC")
    List<QuizSession> getAllSessions();
}
