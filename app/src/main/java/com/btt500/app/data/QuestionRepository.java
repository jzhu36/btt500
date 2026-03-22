package com.btt500.app.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages question loading from assets and smart question selection.
 */
public class QuestionRepository {

    private static List<Question> allQuestions;
    private static Map<String, Question> questionMap;
    private final AnswerRecordDao dao;

    public QuestionRepository(Context context) {
        this.dao = AppDatabase.getInstance(context).answerRecordDao();
        if (allQuestions == null) {
            loadQuestions(context);
        }
    }

    private void loadQuestions(Context context) {
        try {
            InputStream is = context.getAssets().open("btt_questions.json");
            InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Type listType = new TypeToken<List<Question>>() {}.getType();
            allQuestions = new Gson().fromJson(reader, listType);
            reader.close();

            questionMap = new HashMap<>();
            for (Question q : allQuestions) {
                questionMap.put(q.id, q);
            }
        } catch (Exception e) {
            allQuestions = new ArrayList<>();
            questionMap = new HashMap<>();
        }
    }

    public int getTotalQuestionCount() {
        return allQuestions.size();
    }

    public Question getQuestionById(String id) {
        return questionMap.get(id);
    }

    public List<Question> getAllQuestions() {
        return allQuestions;
    }

    /**
     * Select questions with priority:
     * 1. Questions answered incorrectly before (highest priority)
     * 2. Questions never attempted
     * 3. Questions with fewest attempts
     */
    public List<Question> selectQuestions(int count) {
        // Get attempt and wrong counts from DB
        List<QuestionAttemptCount> attemptCounts = dao.getAttemptCounts();
        List<QuestionAttemptCount> wrongCounts = dao.getWrongCounts();

        Map<String, Integer> attemptMap = new HashMap<>();
        for (QuestionAttemptCount ac : attemptCounts) {
            attemptMap.put(ac.questionId, ac.cnt);
        }

        Map<String, Integer> wrongMap = new HashMap<>();
        for (QuestionAttemptCount wc : wrongCounts) {
            wrongMap.put(wc.questionId, wc.cnt);
        }

        // Categorize questions
        List<Question> wrongQuestions = new ArrayList<>();
        List<Question> neverAttempted = new ArrayList<>();
        List<Question> attempted = new ArrayList<>();

        for (Question q : allQuestions) {
            int wrongs = wrongMap.getOrDefault(q.id, 0);
            int attempts = attemptMap.getOrDefault(q.id, 0);

            if (wrongs > 0) {
                wrongQuestions.add(q);
            } else if (attempts == 0) {
                neverAttempted.add(q);
            } else {
                attempted.add(q);
            }
        }

        // Shuffle each category
        Collections.shuffle(wrongQuestions);
        Collections.shuffle(neverAttempted);

        // Sort attempted by fewest attempts
        attempted.sort((a, b) -> {
            int attA = attemptMap.getOrDefault(a.id, 0);
            int attB = attemptMap.getOrDefault(b.id, 0);
            return Integer.compare(attA, attB);
        });

        // Build final list: wrong first, then never attempted, then least attempted
        List<Question> result = new ArrayList<>();
        result.addAll(wrongQuestions);
        result.addAll(neverAttempted);
        result.addAll(attempted);

        // Take only 'count' questions and shuffle them for the quiz
        if (result.size() > count) {
            result = new ArrayList<>(result.subList(0, count));
        }
        Collections.shuffle(result);
        return result;
    }

    public void recordAnswer(String questionId, boolean isCorrect) {
        AnswerRecord record = new AnswerRecord(questionId, isCorrect, System.currentTimeMillis());
        dao.insert(record);
    }

    public int getAttemptCount(String questionId) {
        return dao.getAttemptCount(questionId);
    }

    public int getWrongCount(String questionId) {
        return dao.getWrongCount(questionId);
    }

    public List<AnswerRecord> getRecordsForQuestion(String questionId) {
        return dao.getRecordsForQuestion(questionId);
    }

    public List<AnswerRecord> getRecentRecordsForQuestion(String questionId) {
        return dao.getRecentRecordsForQuestion(questionId);
    }

    /**
     * Get all questions with their stats for the history view.
     */
    public List<QuestionStat> getAllQuestionStats() {
        List<QuestionAttemptCount> attemptCounts = dao.getAttemptCounts();
        List<QuestionAttemptCount> wrongCounts = dao.getWrongCounts();

        Map<String, Integer> attemptMap = new HashMap<>();
        for (QuestionAttemptCount ac : attemptCounts) {
            attemptMap.put(ac.questionId, ac.cnt);
        }

        Map<String, Integer> wrongMap = new HashMap<>();
        for (QuestionAttemptCount wc : wrongCounts) {
            wrongMap.put(wc.questionId, wc.cnt);
        }

        List<QuestionStat> stats = new ArrayList<>();
        for (Question q : allQuestions) {
            QuestionStat stat = new QuestionStat();
            stat.question = q;
            stat.attemptCount = attemptMap.getOrDefault(q.id, 0);
            stat.wrongCount = wrongMap.getOrDefault(q.id, 0);
            stats.add(stat);
        }
        return stats;
    }

    public static class QuestionStat {
        public Question question;
        public int attemptCount;
        public int wrongCount;
    }
}
