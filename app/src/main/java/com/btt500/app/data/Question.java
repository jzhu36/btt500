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

    /**
     * Get the question text in the specified language.
     * Falls back to the other language if the preferred one is null/empty.
     */
    public String getQuestionText(String lang) {
        if (LanguageManager.LANG_EN.equals(lang)) {
            return (question_en != null && !question_en.isEmpty()) ? question_en : question_zh;
        }
        return (question_zh != null && !question_zh.isEmpty()) ? question_zh : question_en;
    }

    /**
     * Get options in the specified language.
     * Falls back to the other language if the preferred one is null/empty.
     */
    public List<String> getOptions(String lang) {
        if (LanguageManager.LANG_EN.equals(lang)) {
            return (options_en != null && !options_en.isEmpty()) ? options_en : options_zh;
        }
        return (options_zh != null && !options_zh.isEmpty()) ? options_zh : options_en;
    }

    /**
     * Get the correct option text in the specified language.
     */
    public String getCorrectOptionText(String lang) {
        List<String> opts = getOptions(lang);
        if (opts != null && correct_answer >= 0 && correct_answer < opts.size()) {
            char label = (char) ('A' + correct_answer);
            return label + ". " + opts.get(correct_answer);
        }
        return "";
    }

    // Legacy methods for backward compatibility (default to Chinese)
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
