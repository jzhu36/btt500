package com.btt500.app.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AnswerRecordDao {

    @Insert
    void insert(AnswerRecord record);

    @Query("SELECT * FROM answer_records WHERE questionId = :questionId ORDER BY timestamp DESC")
    List<AnswerRecord> getRecordsForQuestion(String questionId);

    @Query("SELECT * FROM answer_records WHERE questionId = :questionId ORDER BY timestamp DESC LIMIT 10")
    List<AnswerRecord> getRecentRecordsForQuestion(String questionId);

    @Query("SELECT COUNT(*) FROM answer_records WHERE questionId = :questionId")
    int getAttemptCount(String questionId);

    @Query("SELECT COUNT(*) FROM answer_records WHERE questionId = :questionId AND isCorrect = 0")
    int getWrongCount(String questionId);

    @Query("SELECT DISTINCT questionId FROM answer_records")
    List<String> getAllAttemptedQuestionIds();

    @Query("SELECT questionId, COUNT(*) as cnt FROM answer_records GROUP BY questionId")
    List<QuestionAttemptCount> getAttemptCounts();

    @Query("SELECT questionId, COUNT(*) as cnt FROM answer_records WHERE isCorrect = 0 GROUP BY questionId")
    List<QuestionAttemptCount> getWrongCounts();

    @Query("SELECT * FROM answer_records ORDER BY timestamp DESC")
    List<AnswerRecord> getAllRecords();
}
