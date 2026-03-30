package com.btt500.app.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
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
    private CheckBox cbRecentWrong, cbWithNumbers, cbUnattempted;
    private TextView tvFilteredCount;

    // Cached counts for filter labels
    private int recentWrongCount = 0;
    private int withNumbersCount = 0;
    private int unattemptedCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new QuestionRepository(this);

        MaterialButton btnStart10 = findViewById(R.id.btnStart10);
        MaterialButton btnStart20 = findViewById(R.id.btnStart20);
        MaterialButton btnStart50 = findViewById(R.id.btnStart50);
        MaterialButton btnStartAll = findViewById(R.id.btnStartAll);
        MaterialButton btnHistory = findViewById(R.id.btnHistory);
        TextView tvTotal = findViewById(R.id.tvTotalQuestions);
        layoutResumeCard = findViewById(R.id.layoutResumeCard);
        layoutSessionHistory = findViewById(R.id.layoutSessionHistory);
        cbRecentWrong = findViewById(R.id.cbRecentWrong);
        cbWithNumbers = findViewById(R.id.cbWithNumbers);
        cbUnattempted = findViewById(R.id.cbUnattempted);
        tvFilteredCount = findViewById(R.id.tvFilteredCount);

        int total = repo.getTotalQuestionCount();
        tvTotal.setText(getString(R.string.total_questions, total));

        // Checkbox change listeners to update filtered count
        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> updateFilteredCount();
        cbRecentWrong.setOnCheckedChangeListener(filterListener);
        cbWithNumbers.setOnCheckedChangeListener(filterListener);
        cbUnattempted.setOnCheckedChangeListener(filterListener);

        btnStart10.setOnClickListener(v -> startFromFilter(10));
        btnStart20.setOnClickListener(v -> startFromFilter(20));
        btnStart50.setOnClickListener(v -> startFromFilter(50));
        btnStartAll.setOnClickListener(v -> startFromFilter(-1)); // -1 means all

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateFilterLabels();
        updateFilteredCount();
        refreshResumeCard();
        refreshSessionHistory();
    }

    /**
     * Update checkbox labels with counts.
     */
    private void updateFilterLabels() {
        recentWrongCount = repo.getRecentlyWrongQuestions().size();
        withNumbersCount = repo.getQuestionsWithNumbers().size();
        unattemptedCount = repo.getUnattemptedQuestions().size();

        cbRecentWrong.setText("最近做错的题 (" + recentWrongCount + ")");
        cbWithNumbers.setText("含数字的题 (" + withNumbersCount + ")");
        cbUnattempted.setText("没做过的题 (" + unattemptedCount + ")");
    }

    /**
     * Update the filtered pool count display.
     */
    private void updateFilteredCount() {
        List<Question> pool = getFilteredPool();
        boolean anyFilter = cbRecentWrong.isChecked() || cbWithNumbers.isChecked() || cbUnattempted.isChecked();
        if (anyFilter) {
            tvFilteredCount.setText("筛选后题池：" + pool.size() + " 题");
        } else {
            tvFilteredCount.setText("当前题池：全部 " + repo.getTotalQuestionCount() + " 题");
        }
    }

    private List<Question> getFilteredPool() {
        return repo.getFilteredPool(
                cbRecentWrong.isChecked(),
                cbWithNumbers.isChecked(),
                cbUnattempted.isChecked()
        );
    }

    /**
     * Start a quiz from the filtered pool.
     * @param count number of questions to pick, or -1 for all in the pool
     */
    private void startFromFilter(int count) {
        List<Question> pool = getFilteredPool();

        if (pool.isEmpty()) {
            Toast.makeText(this, "筛选后没有符合条件的题目", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Question> selected;
        boolean anyFilter = cbRecentWrong.isChecked() || cbWithNumbers.isChecked() || cbUnattempted.isChecked();

        if (count == -1) {
            // "全部做完" — use all questions in pool, shuffled
            selected = pool;
            java.util.Collections.shuffle(selected);
        } else if (count >= pool.size()) {
            // Requested count >= pool size, use all
            selected = pool;
            java.util.Collections.shuffle(selected);
        } else {
            // Use weighted random sampling from the filtered pool
            selected = repo.selectQuestionsFromPool(pool, count);
        }

        QuizSession session = repo.createSessionFromList(selected);
        if (session != null) {
            Intent intent = new Intent(this, QuizActivity.class);
            intent.putExtra(QuizActivity.EXTRA_SESSION_ID, session.id);
            startActivity(intent);
        }
    }

    private void refreshResumeCard() {
        layoutResumeCard.removeAllViews();
        QuizSession incomplete = repo.getIncompleteSession();
        if (incomplete != null) {
            layoutResumeCard.setVisibility(View.VISIBLE);

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
