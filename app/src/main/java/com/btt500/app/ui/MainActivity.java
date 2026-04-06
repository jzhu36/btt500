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

public class MainActivity extends AppCompatActivity implements LanguageManager.OnLanguageChangedListener {

    private QuestionRepository repo;
    private LanguageManager langMgr;
    private LinearLayout layoutSessionHistory;
    private LinearLayout layoutResumeCard;
    private CheckBox cbLastOneWrong, cbLastTwoWrong, cbWithNumbers, cbUnattempted, cbSigns, cbLines;
    private TextView tvFilteredCount, tvSubtitle, tvTotalQuestions, tvFilterTitle, tvStartTitle;
    private TextView tvSessionHistoryTitle, tvLangToggle;
    private MaterialButton btnStart10, btnStart20, btnStart50, btnStartAll, btnHistory;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repo = new QuestionRepository(this);
        langMgr = LanguageManager.getInstance(this);

        // Bind views
        tvSubtitle = findViewById(R.id.tvSubtitle);
        tvTotalQuestions = findViewById(R.id.tvTotalQuestions);
        tvFilterTitle = findViewById(R.id.tvFilterTitle);
        tvStartTitle = findViewById(R.id.tvStartTitle);
        tvFilteredCount = findViewById(R.id.tvFilteredCount);
        tvSessionHistoryTitle = findViewById(R.id.tvSessionHistoryTitle);
        tvLangToggle = findViewById(R.id.tvLangToggle);
        layoutResumeCard = findViewById(R.id.layoutResumeCard);
        layoutSessionHistory = findViewById(R.id.layoutSessionHistory);
        cbLastOneWrong = findViewById(R.id.cbLastOneWrong);
        cbLastTwoWrong = findViewById(R.id.cbLastTwoWrong);
        cbWithNumbers = findViewById(R.id.cbWithNumbers);
        cbUnattempted = findViewById(R.id.cbUnattempted);
        cbSigns = findViewById(R.id.cbSigns);
        cbLines = findViewById(R.id.cbLines);
        btnStart10 = findViewById(R.id.btnStart10);
        btnStart20 = findViewById(R.id.btnStart20);
        btnStart50 = findViewById(R.id.btnStart50);
        btnStartAll = findViewById(R.id.btnStartAll);
        btnHistory = findViewById(R.id.btnHistory);

        // Language toggle: tap to switch
        tvLangToggle.setOnClickListener(v -> {
            if (langMgr.isChinese()) {
                langMgr.setLanguage(LanguageManager.LANG_EN);
            } else {
                langMgr.setLanguage(LanguageManager.LANG_ZH);
            }
            // onLanguageChanged callback will refresh UI
        });

        // Checkbox change listeners
        CompoundButton.OnCheckedChangeListener filterListener = (buttonView, isChecked) -> {
            updateFilteredCount();
            updateStartAllButton();
        };
        cbLastOneWrong.setOnCheckedChangeListener(filterListener);
        cbLastTwoWrong.setOnCheckedChangeListener(filterListener);
        cbWithNumbers.setOnCheckedChangeListener(filterListener);
        cbUnattempted.setOnCheckedChangeListener(filterListener);
        cbSigns.setOnCheckedChangeListener(filterListener);
        cbLines.setOnCheckedChangeListener(filterListener);

        btnStart10.setOnClickListener(v -> startFromFilter(10));
        btnStart20.setOnClickListener(v -> startFromFilter(20));
        btnStart50.setOnClickListener(v -> startFromFilter(50));
        btnStartAll.setOnClickListener(v -> startFromFilter(-1));

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
        });

        // Register language change listener
        langMgr.addListener(this);

        // Initial UI update
        refreshAllUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshAllUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        langMgr.removeListener(this);
    }

    @Override
    public void onLanguageChanged(String newLanguage) {
        // Immediately refresh all UI when language changes
        refreshAllUI();
    }

    /**
     * Refresh all UI elements based on current language setting.
     */
    private void refreshAllUI() {
        boolean zh = langMgr.isChinese();
        int total = repo.getTotalQuestionCount();

        // Language toggle text: show the OTHER language as the switch target
        if (zh) {
            tvLangToggle.setText("EN");
        } else {
            tvLangToggle.setText("\u4E2D");
        }

        // Subtitle
        tvSubtitle.setText(zh ? "新加坡基本交通理论考试" : "Singapore Basic Theory Test");

        // Total questions
        tvTotalQuestions.setText(zh ? "题库共 " + total + " 题" : total + " questions");

        // Filter title
        tvFilterTitle.setText(zh ? "题目筛选" : "Question Filters");

        // Filter labels with counts
        updateFilterLabels();

        // Filtered count
        updateFilteredCount();

        // Start title
        tvStartTitle.setText(zh ? "选择题数开始练习" : "Select question count");

        // Start buttons
        btnStart10.setText(zh ? "10 题" : "10 Q");
        btnStart20.setText(zh ? "20 题" : "20 Q");
        btnStart50.setText(zh ? "50 题" : "50 Q");
        updateStartAllButton();

        // Browse button
        btnHistory.setText(zh ? "浏览题库" : "Browse Questions");

        // Resume card and session history
        refreshResumeCard();
        refreshSessionHistory();
    }

    private void updateFilterLabels() {
        boolean zh = langMgr.isChinese();
        int lastOneWrongCount = repo.getLastOneWrongQuestions().size();
        int lastTwoWrongCount = repo.getLastTwoWrongQuestions().size();
        int withNumbersCount = repo.getQuestionsWithNumbers().size();
        int unattemptedCount = repo.getUnattemptedQuestions().size();
        int signsCount = repo.getQuestionsAboutSigns().size();
        int linesCount = repo.getQuestionsAboutLines().size();

        if (zh) {
            cbLastOneWrong.setText("上次答错 (" + lastOneWrongCount + ")");
            cbLastTwoWrong.setText("近两次有错 (" + lastTwoWrongCount + ")");
            cbWithNumbers.setText("含数字的题 (" + withNumbersCount + ")");
            cbUnattempted.setText("没做过的题 (" + unattemptedCount + ")");
            cbSigns.setText("标志与图标 (" + signsCount + ")");
            cbLines.setText("路面标线 (" + linesCount + ")");
        } else {
            cbLastOneWrong.setText("Last attempt wrong (" + lastOneWrongCount + ")");
            cbLastTwoWrong.setText("Wrong in last 2 attempts (" + lastTwoWrongCount + ")");
            cbWithNumbers.setText("With numbers (" + withNumbersCount + ")");
            cbUnattempted.setText("Not attempted (" + unattemptedCount + ")");
            cbSigns.setText("Signs & icons (" + signsCount + ")");
            cbLines.setText("Road lines (" + linesCount + ")");
        }
    }

    private void updateFilteredCount() {
        boolean zh = langMgr.isChinese();
        List<Question> pool = getFilteredPool();
        boolean anyFilter = cbLastOneWrong.isChecked() || cbLastTwoWrong.isChecked()
                || cbWithNumbers.isChecked() || cbUnattempted.isChecked()
                || cbSigns.isChecked() || cbLines.isChecked();
        if (zh) {
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

    private void updateStartAllButton() {
        boolean zh = langMgr.isChinese();
        List<Question> pool = getFilteredPool();
        int count = pool.size();
        if (zh) {
            btnStartAll.setText("做全部 (" + count + " 题)");
        } else {
            btnStartAll.setText("Do All (" + count + " Q)");
        }
    }

    private List<Question> getFilteredPool() {
        return repo.getFilteredPool(
                cbLastOneWrong.isChecked(),
                cbLastTwoWrong.isChecked(),
                cbWithNumbers.isChecked(),
                cbUnattempted.isChecked(),
                cbSigns.isChecked(),
                cbLines.isChecked()
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
        boolean zh = langMgr.isChinese();
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
            tvTitle.setText(zh ? "有未完成的练习" : "Incomplete Session");
            tvTitle.setTextSize(16);
            tvTitle.setTypeface(null, Typeface.BOLD);
            tvTitle.setTextColor(Color.parseColor("#E65100"));
            card.addView(tvTitle);

            TextView tvInfo = new TextView(this);
            if (zh) {
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
            btnResume.setText(zh ? "继续练习" : "Resume");
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
        boolean zh = langMgr.isChinese();
        layoutSessionHistory.removeAllViews();
        List<QuizSession> sessions = repo.getCompletedSessions();

        if (sessions.isEmpty()) {
            layoutSessionHistory.setVisibility(View.GONE);
            tvSessionHistoryTitle.setVisibility(View.GONE);
            return;
        }

        layoutSessionHistory.setVisibility(View.VISIBLE);
        tvSessionHistoryTitle.setVisibility(View.VISIBLE);
        tvSessionHistoryTitle.setText(zh ? "练习历史" : "Practice History");

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
            if (zh) {
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
