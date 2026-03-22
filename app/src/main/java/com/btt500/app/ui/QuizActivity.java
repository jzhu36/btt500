package com.btt500.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.btt500.app.data.Question;
import com.btt500.app.data.QuestionRepository;
import com.btt500.app.data.QuizSession;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_QUESTION_COUNT = "question_count";

    private QuestionRepository repo;
    private QuizSession session;
    private List<Question> questions;
    private List<String> sessionResults;
    private int currentIndex = 0;
    private boolean answered = false;

    private TextView tvProgress, tvScore, tvQuestion, tvQuestionEn, tvFeedback, tvCorrectAnswer;
    private LinearLayout layoutOptions;
    private ProgressBar progressBar;
    private MaterialButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        tvProgress = findViewById(R.id.tvProgress);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvQuestionEn = findViewById(R.id.tvQuestionEn);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        layoutOptions = findViewById(R.id.layoutOptions);
        progressBar = findViewById(R.id.progressBar);
        btnNext = findViewById(R.id.btnNext);

        repo = new QuestionRepository(this);

        long sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1);
        int questionCount = getIntent().getIntExtra(EXTRA_QUESTION_COUNT, 50);

        if (sessionId > 0) {
            // Resume existing session
            session = repo.getSessionById(sessionId);
        }

        if (session == null) {
            // Create new session
            session = repo.createSession(questionCount);
        }

        questions = repo.getSessionQuestions(session);
        sessionResults = repo.getSessionResults(session);

        // Find the first unanswered question
        currentIndex = 0;
        for (int i = 0; i < sessionResults.size(); i++) {
            if ("-".equals(sessionResults.get(i))) {
                currentIndex = i;
                break;
            }
        }

        progressBar.setMax(session.totalQuestions);

        btnNext.setOnClickListener(v -> {
            // Find next unanswered question
            int nextIndex = findNextUnanswered(currentIndex + 1);
            if (nextIndex < 0) {
                // All answered, show result
                showResult();
            } else {
                currentIndex = nextIndex;
                showQuestion();
            }
        });

        showQuestion();
    }

    private int findNextUnanswered(int startFrom) {
        // Refresh session results
        sessionResults = repo.getSessionResults(session);
        for (int i = startFrom; i < sessionResults.size(); i++) {
            if ("-".equals(sessionResults.get(i))) {
                return i;
            }
        }
        return -1; // All answered
    }

    private void showQuestion() {
        answered = false;
        Question q = questions.get(currentIndex);

        tvProgress.setText(getString(R.string.question_progress,
                session.answeredCount + 1, session.totalQuestions));
        tvScore.setText(session.correctCount + " ✓");
        progressBar.setProgress(session.answeredCount);

        tvQuestion.setText(q.getDisplayQuestion());

        if (q.question_en != null && !q.question_en.isEmpty()) {
            tvQuestionEn.setText(q.question_en);
            tvQuestionEn.setVisibility(View.VISIBLE);
        } else {
            tvQuestionEn.setVisibility(View.GONE);
        }

        tvFeedback.setVisibility(View.GONE);
        tvCorrectAnswer.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        layoutOptions.removeAllViews();
        List<String> options = q.getDisplayOptions();
        if (options == null) return;

        char[] labels = {'A', 'B', 'C', 'D'};

        for (int i = 0; i < options.size(); i++) {
            final int optionIndex = i;
            TextView optionView = new TextView(this);
            String label = (i < labels.length) ? labels[i] + ". " : "";

            String displayText = label + options.get(i);
            if (q.options_en != null && i < q.options_en.size() && q.options_en.get(i) != null) {
                displayText += "\n" + q.options_en.get(i);
            }
            optionView.setText(displayText);
            optionView.setTextSize(16);
            optionView.setPadding(24, 20, 24, 20);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12);
            bg.setColor(getResources().getColor(R.color.option_bg, null));
            bg.setStroke(2, Color.parseColor("#C5CAE9"));
            optionView.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 12;
            optionView.setLayoutParams(params);

            optionView.setOnClickListener(v -> onOptionSelected(optionIndex));
            layoutOptions.addView(optionView);
        }
    }

    private void onOptionSelected(int selectedIndex) {
        if (answered) return;
        answered = true;

        Question q = questions.get(currentIndex);
        boolean isCorrect = (selectedIndex == q.correct_answer);

        // Record answer in session
        repo.recordSessionAnswer(session, currentIndex, isCorrect);
        // Refresh session from DB
        session = repo.getSessionById(session.id);

        // Update feedback
        tvFeedback.setVisibility(View.VISIBLE);
        if (isCorrect) {
            tvFeedback.setText(R.string.correct);
            tvFeedback.setTextColor(getResources().getColor(R.color.correct_green, null));
        } else {
            tvFeedback.setText(R.string.incorrect);
            tvFeedback.setTextColor(getResources().getColor(R.color.wrong_red, null));
            tvCorrectAnswer.setText(getString(R.string.correct_answer, q.getCorrectOptionText()));
            tvCorrectAnswer.setVisibility(View.VISIBLE);
        }

        // Highlight options
        for (int i = 0; i < layoutOptions.getChildCount(); i++) {
            TextView optView = (TextView) layoutOptions.getChildAt(i);
            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12);

            if (i == q.correct_answer) {
                bg.setColor(Color.parseColor("#C8E6C9"));
                bg.setStroke(2, getResources().getColor(R.color.correct_green, null));
            } else if (i == selectedIndex && !isCorrect) {
                bg.setColor(Color.parseColor("#FFCDD2"));
                bg.setStroke(2, getResources().getColor(R.color.wrong_red, null));
            } else {
                bg.setColor(Color.parseColor("#EEEEEE"));
                bg.setStroke(1, Color.parseColor("#BDBDBD"));
            }
            optView.setBackground(bg);
            optView.setOnClickListener(null);
        }

        // Update score display
        tvScore.setText(session.correctCount + " ✓");
        tvProgress.setText(getString(R.string.question_progress,
                session.answeredCount, session.totalQuestions));
        progressBar.setProgress(session.answeredCount);

        // Show next button
        btnNext.setVisibility(View.VISIBLE);
        if (session.isCompleted) {
            btnNext.setText(R.string.finish);
        } else {
            btnNext.setText(R.string.next_question);
        }
    }

    private void showResult() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("session_id", session.id);
        intent.putExtra("correct", session.correctCount);
        intent.putExtra("total", session.totalQuestions);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (session != null && !session.isCompleted) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("暂停练习")
                    .setMessage("进度已自动保存，下次可以继续。")
                    .setPositiveButton("退出", (d, w) -> finish())
                    .setNegativeButton("继续", null)
                    .show();
        } else {
            finish();
        }
    }
}
