package com.btt500.app.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "answer_records")
public class AnswerRecord {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String questionId;

    public boolean isCorrect;

    public long timestamp;

    public AnswerRecord() {}

    public AnswerRecord(String questionId, boolean isCorrect, long timestamp) {
        this.questionId = questionId;
        this.isCorrect = isCorrect;
        this.timestamp = timestamp;
    }
}
