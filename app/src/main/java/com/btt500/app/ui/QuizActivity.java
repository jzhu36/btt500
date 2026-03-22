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
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final int TOTAL_QUESTIONS = 50;

    private QuestionRepository repo;
    private List<Question> questions;
    private int currentIndex = 0;
    private int correctCount = 0;
    private boolean answered = false;

    private TextView tvProgress, tvScore, tvQuestion, tvQuestionEn, tvFeedback, tvCorrectAnswer;
    private LinearLayout layoutOptions;
    private ProgressBar progressBar;
    private MaterialButton btnNext;

    // Track results for the result screen
    private ArrayList<String> questionIds = new ArrayList<>();
    private ArrayList<Boolean> results = new ArrayList<>();

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
        questions = repo.selectQuestions(TOTAL_QUESTIONS);

        progressBar.setMax(TOTAL_QUESTIONS);

        btnNext.setOnClickListener(v -> {
            currentIndex++;
            if (currentIndex >= questions.size()) {
                showResult();
            } else {
                showQuestion();
            }
        });

        showQuestion();
    }

    private void showQuestion() {
        answered = false;
        Question q = questions.get(currentIndex);

        tvProgress.setText(getString(R.string.question_progress, currentIndex + 1, questions.size()));
        tvScore.setText(correctCount + " ✓");
        progressBar.setProgress(currentIndex);

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

            // Show both Chinese and English options
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

        if (isCorrect) {
            correctCount++;
        }

        // Record answer
        repo.recordAnswer(q.id, isCorrect);
        questionIds.add(q.id);
        results.add(isCorrect);

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

        // Show next button
        btnNext.setVisibility(View.VISIBLE);
        if (currentIndex >= questions.size() - 1) {
            btnNext.setText(R.string.finish);
        } else {
            btnNext.setText(R.string.next_question);
        }

        tvScore.setText(correctCount + " ✓");
    }

    private void showResult() {
        Intent intent = new Intent(this, ResultActivity.class);
        intent.putExtra("correct", correctCount);
        intent.putExtra("total", questions.size());
        intent.putStringArrayListExtra("questionIds", questionIds);
        ArrayList<String> resultStrings = new ArrayList<>();
        for (Boolean b : results) {
            resultStrings.add(b.toString());
        }
        intent.putStringArrayListExtra("results", resultStrings);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Confirm exit
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("退出练习")
                .setMessage("确定要退出吗？当前进度将不会保存。")
                .setPositiveButton("退出", (d, w) -> finish())
                .setNegativeButton("继续", null)
                .show();
    }
}
