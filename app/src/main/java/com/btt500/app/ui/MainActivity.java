package com.btt500.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.btt500.app.data.Question;
import com.btt500.app.data.QuestionRepository;
import com.btt500.app.data.QuizSession;
import com.google.android.material.button.MaterialButton;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private QuestionRepository repo;
    private LinearLayout layoutSessionHistory;
    private LinearLayout layoutResumeCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new QuestionRepository(this);

        MaterialButton btnStart10 = findViewById(R.id.btnStart10);
        MaterialButton btnStart20 = findViewById(R.id.btnStart20);
        MaterialButton btnStart50 = findViewById(R.id.btnStart50);
        MaterialButton btnHistory = findViewById(R.id.btnHistory);
        MaterialButton btnRecentWrong = findViewById(R.id.btnRecentWrong);
        MaterialButton btnWithNumbers = findViewById(R.id.btnWithNumbers);
        MaterialButton btnUnattempted = findViewById(R.id.btnUnattempted);
        TextView tvTotal = findViewById(R.id.tvTotalQuestions);
        layoutResumeCard = findViewById(R.id.layoutResumeCard);
        layoutSessionHistory = findViewById(R.id.layoutSessionHistory);

        int total = repo.getTotalQuestionCount();
        tvTotal.setText(getString(R.string.total_questions, total));

        btnStart10.setOnClickListener(v -> startNewSession(10));
        btnStart20.setOnClickListener(v -> startNewSession(20));
        btnStart50.setOnClickListener(v -> startNewSession(50));

        btnRecentWrong.setOnClickListener(v -> startFilteredQuiz("recent_wrong"));
        btnWithNumbers.setOnClickListener(v -> startFilteredQuiz("with_numbers"));
        btnUnattempted.setOnClickListener(v -> startFilteredQuiz("unattempted"));

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshResumeCard();
        refreshSessionHistory();
        updateFilterButtonCounts();
    }

    private void startNewSession(int count) {
        Intent intent = new Intent(this, QuizActivity.class);
        intent.putExtra(QuizActivity.EXTRA_QUESTION_COUNT, count);
        startActivity(intent);
    }

    private void startFilteredQuiz(String filterMode) {
        List<Question> questions;
        String label;
        switch (filterMode) {
            case "recent_wrong":
                questions = repo.getRecentlyWrongQuestions();
                label = "最近做错的题";
                break;
            case "with_numbers":
                questions = repo.getQuestionsWithNumbers();
                label = "含数字的题";
                break;
            case "unattempted":
                questions = repo.getUnattemptedQuestions();
                label = "没做过的题";
                break;
            default:
                return;
        }

        if (questions.isEmpty()) {
            Toast.makeText(this, label + "：暂无符合条件的题目", Toast.LENGTH_SHORT).show();
            return;
        }

        QuizSession session = repo.createSessionFromList(questions);
        if (session != null) {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(QuizActivity.EXTRA_SESSION_ID, session.id);
            startActivity(intent);
        }
    }

    private void updateFilterButtonCounts() {
        MaterialButton btnRecentWrong = findViewById(R.id.btnRecentWrong);
        MaterialButton btnWithNumbers = findViewById(R.id.btnWithNumbers);
        MaterialButton btnUnattempted = findViewById(R.id.btnUnattempted);

        int wrongCount = repo.getRecentlyWrongQuestions().size();
        int numberCount = repo.getQuestionsWithNumbers().size();
        int unattemptedCount = repo.getUnattemptedQuestions().size();

        btnRecentWrong.setText("最近做错的题 (" + wrongCount + ")");
        btnWithNumbers.setText("含数字的题 (" + numberCount + ")");
        btnUnattempted.setText("没做过的题 (" + unattemptedCount + ")");
    }

    private void refreshResumeCard() {
        layoutResumeCard.removeAllViews();
        QuizSession incomplete = repo.getIncompleteSession();
        if (incomplete != null) {
            layoutResumeCard.setVisibility(View.VISIBLE);

            // Build resume card
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(24, 20, 24, 20);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12);
            bg.setColor(Color.parseColor("#FFF3E0"));
            bg.setStroke(2, Color.parseColor("#FF9800"));
            card.setBackground(bg);

            TextView tvTitle = new TextView(this);
            tvTitle.setText("有未完成的练习");
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(Color.parseColor("#E65100"));
            card.addView(tvTitle);

            TextView tvInfo = new TextView(this);
            tvInfo.setText(String.format("已完成 %d/%d 题 · 正确 %d 题",
                    incomplete.answeredCount, incomplete.totalQuestions, incomplete.correctCount));
            tvInfo.setTextSize(14);
            tvInfo.setTextColor(Color.parseColor("#BF360C"));
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            infoParams.topMargin = 8;
            tvInfo.setLayoutParams(infoParams);
            card.addView(tvInfo);

            MaterialButton btnResume = new MaterialButton(this);
            btnResume.setText("继续练习");
            btnResume.setTextSize(14);
            LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(44));
            btnParams.topMargin = 12;
            btnResume.setLayoutParams(btnParams);
            btnResume.setOnClickListener(v -> {
                Intent intent = new Intent(this, QuizActivity.class);
                intent.putExtra(QuizActivity.EXTRA_SESSION_ID, incomplete.id);
                startActivity(intent);
            });
            card.addView(btnResume);

            layoutResumeCard.addView(card);
        } else {
            layoutResumeCard.setVisibility(View.GONE);
        }
    }

    private void refreshSessionHistory() {
        layoutSessionHistory.removeAllViews();
        List<QuizSession> sessions = repo.getCompletedSessions();

        if (sessions.isEmpty()) {
            layoutSessionHistory.setVisibility(View.GONE);
            return;
        }

        layoutSessionHistory.setVisibility(View.VISIBLE);

        // Section title
        TextView tvSectionTitle = new TextView(this);
        tvSectionTitle.setText("练习历史");
        tvSectionTitle.setTextSize(18);
        tvSectionTitle.setTypeface(null, Typeface.BOLD);
        tvSectionTitle.setTextColor(getResources().getColor(R.color.dark_text, null));
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.bottomMargin = 12;
        tvSectionTitle.setLayoutParams(titleParams);
        layoutSessionHistory.addView(tvSectionTitle);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());

        for (QuizSession s : sessions) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setPadding(20, 16, 20, 16);
            card.setGravity(Gravity.CENTER_VERTICAL);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setCornerRadius(10);
            cardBg.setColor(getResources().getColor(R.color.light_gray, null));
            cardBg.setStroke(1, Color.parseColor("#E0E0E0"));
            card.setBackground(cardBg);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            cardParams.bottomMargin = 8;
            card.setLayoutParams(cardParams);

            // Left: score circle
            int percentage = s.totalQuestions > 0 ? (s.correctCount * 100 / s.totalQuestions) : 0;
            boolean passed = percentage >= 90;

            TextView tvPercent = new TextView(this);
            tvPercent.setText(percentage + "%");
            tvPercent.setTextSize(20);
            tvPercent.setTypeface(null, Typeface.BOLD);
            tvPercent.setTextColor(passed ? getResources().getColor(R.color.correct_green, null)
                    : getResources().getColor(R.color.wrong_red, null));
            tvPercent.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams pctParams = new LinearLayout.LayoutParams(dpToPx(64), LinearLayout.LayoutParams.WRAP_CONTENT);
            tvPercent.setLayoutParams(pctParams);
            card.addView(tvPercent);

            // Right: details
            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams detParams = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
            detParams.leftMargin = 16;
            details.setLayoutParams(detParams);

            TextView tvTime = new TextView(this);
            tvTime.setText(sdf.format(new Date(s.completedAt)));
            tvTime.setTextSize(13);
            tvTime.setTextColor(getResources().getColor(R.color.medium_gray, null));
            details.addView(tvTime);

            TextView tvStats = new TextView(this);
            int wrongCount = s.totalQuestions - s.correctCount;
            tvStats.setText(String.format("共 %d 题 · 正确 %d · 错误 %d",
                    s.totalQuestions, s.correctCount, wrongCount));
            tvStats.setTextSize(14);
            tvStats.setTextColor(getResources().getColor(R.color.dark_text, null));
            LinearLayout.LayoutParams statsParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            statsParams.topMargin = 4;
            tvStats.setLayoutParams(statsParams);
            details.addView(tvStats);

            card.addView(details);
            layoutSessionHistory.addView(card);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
