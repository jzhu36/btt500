package com.btt500.app.data;

import java.util.List;

public class Question {
    public String id;
    public String question_zh;
    public String question_en;
    public List<String> options_zh;
    public List<String> options_en;
    public int correct_answer;
    public String topic;
    public List<String> images;

    public String getDisplayQuestion() {
        return question_zh;
    }

    public List<String> getDisplayOptions() {
        return options_zh;
    }

    public String getCorrectOptionText() {
        if (options_zh != null && correct_answer >= 0 && correct_answer < options_zh.size()) {
            char label = (char) ('A' + correct_answer);
            return label + ". " + options_zh.get(correct_answer);
        }
        return "";
    }
}
