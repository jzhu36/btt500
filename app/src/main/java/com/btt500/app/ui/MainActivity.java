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
import com.btt500.app.data.LanguageManager;
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
    private LanguageManager langMgr;
    private LinearLayout layoutSessionHistory;
    private LinearLayout layoutResumeCard;
    private CheckBox cbRecentWrong, cbWithNumbers, cbUnattempted;
    private TextView tvFilteredCount;
    private MaterialButton btnLanguage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new QuestionRepository(this);
        langMgr = LanguageManager.getInstance(this);

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
        btnLanguage = findViewById(R.id.btnLanguage);

        int total = repo.getTotalQuestionCount();
        tvTotal.setText(getString(R.string.total_questions, total));

        // Language toggle
        updateLanguageButton();
        btnLanguage.setOnClickListener(v -> {
            langMgr.toggleLanguage();
            updateLanguageButton();
        });

        // Checkbox change listeners
        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> updateFilteredCount();
        cbRecentWrong.setOnCheckedChangeListener(filterListener);
        cbWithNumbers.setOnCheckedChangeListener(filterListener);
        cbUnattempted.setOnCheckedChangeListener(filterListener);

        btnStart10.setOnClickListener(v -> startFromFilter(10));
        btnStart20.setOnClickListener(v -> startFromFilter(20));
        btnStart50.setOnClickListener(v -> startFromFilter(50));
        btnStartAll.setOnClickListener(v -> startFromFilter(-1));

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLanguageButton();
        updateFilterLabels();
        updateFilteredCount();
        refreshResumeCard();
        refreshSessionHistory();
    }

    private void updateLanguageButton() {
        if (langMgr.isChinese()) {
            btnLanguage.setText("当前：中文 | 切换为 English");
        } else {
            btnLanguage.setText("Current: English | Switch to 中文");
        }
    }

    private void updateFilterLabels() {
        int recentWrongCount = repo.getRecentlyWrongQuestions().size();
        int withNumbersCount = repo.getQuestionsWithNumbers().size();
        int unattemptedCount = repo.getUnattemptedQuestions().size();

        if (langMgr.isChinese()) {
            cbRecentWrong.setText("最近做错的题 (" + recentWrongCount + ")");
            cbWithNumbers.setText("含数字的题 (" + withNumbersCount + ")");
            cbUnattempted.setText("没做过的题 (" + unattemptedCount + ")");
        } else {
            cbRecentWrong.setText("Recently Wrong (" + recentWrongCount + ")");
            cbWithNumbers.setText("With Numbers (" + withNumbersCount + ")");
            cbUnattempted.setText("Not Attempted (" + unattemptedCount + ")");
        }
    }

    private void updateFilteredCount() {
        List<Question> pool = getFilteredPool();
        boolean anyFilter = cbRecentWrong.isChecked() || cbWithNumbers.isChecked() || cbUnattempted.isChecked();
        if (langMgr.isChinese()) {
            if (anyFilter) {
                tvFilteredCount.setText("筛选后题池：" + pool.size() + " 题");
            } else {
                tvFilteredCount.setText("当前题池：全部 " + repo.getTotalQuestionCount() + " 题");
            }
        } else {
            if (anyFilter) {
                tvFilteredCount.setText("Filtered pool: " + pool.size() + " questions");
            } else {
                tvFilteredCount.setText("Current pool: all " + repo.getTotalQuestionCount() + " questions");
            }
        }
    }

    private List<Question> getFilteredPool() {
        return repo.getFilteredPool(
                cbRecentWrong.isChecked(),
                cbWithNumbers.isChecked(),
                cbUnattempted.isChecked()
        );
    }

    private void startFromFilter(int count) {
        List<Question> pool = getFilteredPool();

        if (pool.isEmpty()) {
            String msg = langMgr.isChinese() ? "筛选后没有符合条件的题目" : "No questions match the selected filters";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            return;
        }

        List<Question> selected;

        if (count == -1) {
            selected = pool;
            java.util.Collections.shuffle(selected);
        } else if (count >= pool.size()) {
            selected = pool;
            java.util.Collections.shuffle(selected);
        } else {
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
            tvTitle.setText(langMgr.isChinese() ? "有未完成的练习" : "Incomplete Session");
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(Color.parseColor("#E65100"));
            card.addView(tvTitle);

            TextView tvInfo = new TextView(this);
            if (langMgr.isChinese()) {
                tvInfo.setText(String.format("已完成 %d/%d 题 · 正确 %d 题",
                        incomplete.answeredCount, incomplete.totalQuestions, incomplete.correctCount));
            } else {
                tvInfo.setText(String.format("Completed %d/%d · Correct %d",
                        incomplete.answeredCount, incomplete.totalQuestions, incomplete.correctCount));
            }
            tvInfo.setTextSize(14);
            tvInfo.setTextColor(Color.parseColor("#BF360C"));
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            infoParams.topMargin = 8;
            tvInfo.setLayoutParams(infoParams);
            card.addView(tvInfo);

            MaterialButton btnResume = new MaterialButton(this);
            btnResume.setText(langMgr.isChinese() ? "继续练习" : "Resume");
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
        tvSectionTitle.setText(langMgr.isChinese() ? "练习历史" : "Practice History");
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
            if (langMgr.isChinese()) {
                tvStats.setText(String.format("共 %d 题 · 正确 %d · 错误 %d",
                        s.totalQuestions, s.correctCount, wrongCount));
            } else {
                tvStats.setText(String.format("Total %d · Correct %d · Wrong %d",
                        s.totalQuestions, s.correctCount, wrongCount));
            }
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
