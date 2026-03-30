package com.btt500.app.data;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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
    private final QuizSessionDao sessionDao;

    public QuestionRepository(Context context) {
        AppDatabase db = AppDatabase.getInstance(context);
        this.dao = db.answerRecordDao();
        this.sessionDao = db.quizSessionDao();
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
     * Get all unique topics from the question bank.
     */
    public List<String> getAllTopics() {
        Set<String> topicSet = new HashSet<>();
        for (Question q : allQuestions) {
            if (q.topic != null && !q.topic.isEmpty()) {
                topicSet.add(q.topic);
            }
        }
        List<String> topics = new ArrayList<>(topicSet);
        Collections.sort(topics);
        return topics;
    }

    /**
     * Select questions using weighted random sampling from the full pool.
     * Questions whose most recent OR second most recent answer was WRONG get 5x weight.
     * Questions never attempted get 5x weight.
     * All other questions (last two answers both correct) get 1x weight.
     */
    public List<Question> selectQuestions(int count) {
        return selectQuestionsFromPool(allQuestions, count);
    }

    /**
     * Select questions using weighted random sampling from a given pool.
     * Questions whose most recent OR second most recent answer was WRONG get 5x weight.
     * Questions never attempted get 5x weight.
     * All other questions (last two answers both correct) get 1x weight.
     */
    public List<Question> selectQuestionsFromPool(List<Question> pool, int count) {
        if (pool == null || pool.isEmpty()) return new ArrayList<>();

        Set<String> lastWrongIds = getRecentlyWrongQuestionIds();
        Set<String> attemptedIds = new HashSet<>(dao.getAllAttemptedQuestionIds());

        List<Question> poolCopy = new ArrayList<>(pool);
        double[] weights = new double[poolCopy.size()];
        double totalWeight = 0;
        for (int i = 0; i < poolCopy.size(); i++) {
            String qid = poolCopy.get(i).id;
            if (lastWrongIds.contains(qid) || !attemptedIds.contains(qid)) {
                weights[i] = 5.0;
            } else {
                weights[i] = 1.0;
            }
            totalWeight += weights[i];
        }

        List<Question> result = new ArrayList<>();
        java.util.Random random = new java.util.Random();
        int remaining = Math.min(count, poolCopy.size());

        for (int picked = 0; picked < remaining; picked++) {
            double r = random.nextDouble() * totalWeight;
            double cumulative = 0;
            int selectedIdx = poolCopy.size() - 1;
            for (int i = 0; i < poolCopy.size(); i++) {
                if (weights[i] <= 0) continue;
                cumulative += weights[i];
                if (cumulative >= r) {
                    selectedIdx = i;
                    break;
                }
            }
            result.add(poolCopy.get(selectedIdx));
            totalWeight -= weights[selectedIdx];
            weights[selectedIdx] = 0;
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

    // ==================== Session Methods ====================

    /**
     * Create a new quiz session with the given question count from the full pool.
     */
    public QuizSession createSession(int questionCount) {
        List<Question> selected = selectQuestions(questionCount);
        return createSessionFromList(selected);
    }

    /**
     * Get the most recent incomplete session, if any.
     */
    public QuizSession getIncompleteSession() {
        return sessionDao.getIncompleteSession();
    }

    /**
     * Get a session by ID.
     */
    public QuizSession getSessionById(long id) {
        return sessionDao.getById(id);
    }

    /**
     * Update a session (e.g., after answering a question).
     */
    public void updateSession(QuizSession session) {
        sessionDao.update(session);
    }

    /**
     * Get all completed sessions.
     */
    public List<QuizSession> getCompletedSessions() {
        return sessionDao.getCompletedSessions();
    }

    /**
     * Get all sessions.
     */
    public List<QuizSession> getAllSessions() {
        return sessionDao.getAllSessions();
    }

    /**
     * Parse question IDs from a session's CSV.
     */
    public List<Question> getSessionQuestions(QuizSession session) {
        List<Question> result = new ArrayList<>();
        if (session.questionIdsCsv == null || session.questionIdsCsv.isEmpty()) {
            return result;
        }
        String[] ids = session.questionIdsCsv.split(",");
        for (String id : ids) {
            Question q = getQuestionById(id.trim());
            if (q != null) {
                result.add(q);
            }
        }
        return result;
    }

    /**
     * Parse results from a session's CSV.
     * Returns: "1" for correct, "0" for wrong, "-" for unanswered
     */
    public List<String> getSessionResults(QuizSession session) {
        if (session.resultsCsv == null || session.resultsCsv.isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(session.resultsCsv.split(",")));
    }

    /**
     * Record an answer within a session.
     */
    public void recordSessionAnswer(QuizSession session, int questionIndex, boolean isCorrect) {
        List<String> results = getSessionResults(session);
        if (questionIndex < results.size()) {
            results.set(questionIndex, isCorrect ? "1" : "0");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(results.get(i));
        }
        session.resultsCsv = sb.toString();

        session.answeredCount++;
        if (isCorrect) {
            session.correctCount++;
        }

        if (session.answeredCount >= session.totalQuestions) {
            session.isCompleted = true;
            session.completedAt = System.currentTimeMillis();
        }

        sessionDao.update(session);

        // Also record in the global answer_records table
        String[] ids = session.questionIdsCsv.split(",");
        if (questionIndex < ids.length) {
            recordAnswer(ids[questionIndex].trim(), isCorrect);
        }
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

        List<AnswerRecord> allRecords = dao.getAllRecords();
        Map<String, List<AnswerRecord>> recordsByQuestion = new HashMap<>();
        for (AnswerRecord r : allRecords) {
            recordsByQuestion.computeIfAbsent(r.questionId, k -> new ArrayList<>()).add(r);
        }

        List<QuestionStat> stats = new ArrayList<>();
        for (Question q : allQuestions) {
            QuestionStat stat = new QuestionStat();
            stat.question = q;
            stat.attemptCount = attemptMap.getOrDefault(q.id, 0);
            stat.wrongCount = wrongMap.getOrDefault(q.id, 0);
            stat.records = recordsByQuestion.getOrDefault(q.id, new ArrayList<>());
            stats.add(stat);
        }
        return stats;
    }

    /**
     * Get question IDs that should receive 5x weight.
     * A question qualifies if its most recent answer OR second most recent answer was wrong.
     */
    public Set<String> getRecentlyWrongQuestionIds() {
        List<AnswerRecord> allRecords = dao.getAllRecords();
        Map<String, List<Boolean>> recentResults = new HashMap<>();
        for (AnswerRecord r : allRecords) {
            List<Boolean> results = recentResults.computeIfAbsent(r.questionId, k -> new ArrayList<>());
            if (results.size() < 2) {
                results.add(r.isCorrect);
            }
        }
        Set<String> result = new HashSet<>();
        for (Map.Entry<String, List<Boolean>> entry : recentResults.entrySet()) {
            List<Boolean> results = entry.getValue();
            boolean lastWrong = !results.get(0);
            boolean secondLastWrong = results.size() > 1 && !results.get(1);
            if (lastWrong || secondLastWrong) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    // ==================== Filtered Selection Methods ====================

    /**
     * Get questions whose most recent OR second most recent answer was wrong.
     * Returns shuffled list.
     */
    public List<Question> getRecentlyWrongQuestions() {
        Set<String> wrongIds = getRecentlyWrongQuestionIds();
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (wrongIds.contains(q.id)) {
                result.add(q);
            }
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * Get questions that contain numbers (digits) in the question text or any option.
     * Checks both Chinese and English text.
     * Returns shuffled list.
     */
    public List<Question> getQuestionsWithNumbers() {
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (containsDigit(q.question_zh) || containsDigit(q.question_en)) {
                result.add(q);
                continue;
            }
            boolean found = false;
            if (q.options_zh != null) {
                for (String opt : q.options_zh) {
                    if (containsDigit(opt)) { found = true; break; }
                }
            }
            if (!found && q.options_en != null) {
                for (String opt : q.options_en) {
                    if (containsDigit(opt)) { found = true; break; }
                }
            }
            if (found) result.add(q);
        }
        Collections.shuffle(result);
        return result;
    }

    private boolean containsDigit(String s) {
        if (s == null) return false;
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    /**
     * Get questions that have never been attempted.
     * Returns shuffled list.
     */
    public List<Question> getUnattemptedQuestions() {
        Set<String> attemptedIds = new HashSet<>(dao.getAllAttemptedQuestionIds());
        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (!attemptedIds.contains(q.id)) {
                result.add(q);
            }
        }
        Collections.shuffle(result);
        return result;
    }

    /**
     * Get the filtered question pool based on active filters.
     * Filters are combined with UNION (OR) logic: a question is included if it matches ANY active filter.
     * If no filters are active, returns all questions.
     */
    public List<Question> getFilteredPool(boolean filterRecentWrong, boolean filterWithNumbers, boolean filterUnattempted) {
        if (!filterRecentWrong && !filterWithNumbers && !filterUnattempted) {
            return new ArrayList<>(allQuestions);
        }

        Set<String> recentWrongIds = filterRecentWrong ? getRecentlyWrongQuestionIds() : new HashSet<>();
        Set<String> attemptedIds = (filterUnattempted) ? new HashSet<>(dao.getAllAttemptedQuestionIds()) : new HashSet<>();

        // Pre-compute number question IDs
        Set<String> numberQuestionIds = new HashSet<>();
        if (filterWithNumbers) {
            for (Question q : allQuestions) {
                if (isQuestionWithNumbers(q)) {
                    numberQuestionIds.add(q.id);
                }
            }
        }

        // Union: include question if it matches any active filter
        Set<String> includedIds = new HashSet<>();
        for (Question q : allQuestions) {
            if (filterRecentWrong && recentWrongIds.contains(q.id)) {
                includedIds.add(q.id);
            }
            if (filterWithNumbers && numberQuestionIds.contains(q.id)) {
                includedIds.add(q.id);
            }
            if (filterUnattempted && !attemptedIds.contains(q.id)) {
                includedIds.add(q.id);
            }
        }

        List<Question> result = new ArrayList<>();
        for (Question q : allQuestions) {
            if (includedIds.contains(q.id)) {
                result.add(q);
            }
        }
        return result;
    }

    private boolean isQuestionWithNumbers(Question q) {
        if (containsDigit(q.question_zh) || containsDigit(q.question_en)) return true;
        if (q.options_zh != null) {
            for (String opt : q.options_zh) {
                if (containsDigit(opt)) return true;
            }
        }
        if (q.options_en != null) {
            for (String opt : q.options_en) {
                if (containsDigit(opt)) return true;
            }
        }
        return false;
    }

    /**
     * Create a session from a pre-selected list of questions.
     * Used for filtered/custom quiz modes.
     */
    public QuizSession createSessionFromList(List<Question> selected) {
        if (selected == null || selected.isEmpty()) return null;

        QuizSession session = new QuizSession();
        session.totalQuestions = selected.size();
        session.answeredCount = 0;
        session.correctCount = 0;
        session.isCompleted = false;
        session.createdAt = System.currentTimeMillis();
        session.completedAt = 0;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(selected.get(i).id);
        }
        session.questionIdsCsv = sb.toString();

        StringBuilder resSb = new StringBuilder();
        for (int i = 0; i < selected.size(); i++) {
            if (i > 0) resSb.append(",");
            resSb.append("-");
        }
        session.resultsCsv = resSb.toString();

        long id = sessionDao.insert(session);
        session.id = id;
        return session;
    }

    public static class QuestionStat {
        public Question question;
        public int attemptCount;
        public int wrongCount;
        public List<AnswerRecord> records; // ordered by timestamp DESC
    }
}
