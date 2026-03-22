package com.btt500.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Represents a quiz session. A session has a fixed number of questions (10, 20, or 50).
 * Users can exit mid-session and resume later.
 */
@Entity(tableName = "quiz_sessions")
public class QuizSession {

    @PrimaryKey(autoGenerate = true)
    public long id;

    /** Total number of questions in this session */
    public int totalQuestions;

    /** Number of questions answered so far */
    public int answeredCount;

    /** Number of correct answers so far */
    public int correctCount;

    /** Comma-separated list of question IDs for this session */
    public String questionIdsCsv;

    /** Comma-separated list of answered results: "1" for correct, "0" for wrong, "" for unanswered */
    public String resultsCsv;

    /** Whether the session is completed */
    public boolean isCompleted;

    /** Timestamp when session was created */
    public long createdAt;

    /** Timestamp when session was completed (0 if not completed) */
    public long completedAt;

    public QuizSession() {}
}
