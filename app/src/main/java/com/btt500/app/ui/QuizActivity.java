package com.btt500.app.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.btt500.app.data.LanguageManager;
import com.btt500.app.data.Question;
import com.btt500.app.data.QuestionRepository;
import com.btt500.app.data.QuizSession;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    private static final String TAG = "QuizActivity";

    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_QUESTION_COUNT = "question_count";

    private QuestionRepository repo;
    private LanguageManager langMgr;
    private QuizSession session;
    private List<Question> questions;
    private List<String> sessionResults;
    private int currentIndex = 0;
    private boolean answered = false;

    private TextView tvProgress, tvScore, tvQuestion, tvFeedback, tvCorrectAnswer, tvLangToggle;
    private ImageView ivQuestionImage;
    private LinearLayout layoutOptions;
    private ProgressBar progressBar;
    private MaterialButton btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        try {
            initializeComponents();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize quiz", e);
            Toast.makeText(this,
                    "Failed to load quiz. Please try again.",
                    Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initializeComponents() {
        // Initialize data layer first
        repo = new QuestionRepository(this);
        langMgr = LanguageManager.getInstance(this);

        // Bind views
        tvProgress = findViewById(R.id.tvProgress);
        tvScore = findViewById(R.id.tvScore);
        tvQuestion = findViewById(R.id.tvQuestion);
        tvFeedback = findViewById(R.id.tvFeedback);
        tvCorrectAnswer = findViewById(R.id.tvCorrectAnswer);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        layoutOptions = findViewById(R.id.layoutOptions);
        progressBar = findViewById(R.id.progressBar);
        btnNext = findViewById(R.id.btnNext);

        // Language toggle button
        tvLangToggle = findViewById(R.id.tvLangToggle);
        if (tvLangToggle != null) {
            updateLangToggleText();
            tvLangToggle.setOnClickListener(v -> {
                langMgr.toggleLanguage();
                updateLangToggleText();
                refreshCurrentQuestion();
            });
        }

        // Hide the English subtitle TextView since we now use single language
        TextView tvQuestionEn = findViewById(R.id.tvQuestionEn);
        if (tvQuestionEn != null) {
            tvQuestionEn.setVisibility(View.GONE);
        }

        // Load or create session
        long sessionId = getIntent().getLongExtra(EXTRA_SESSION_ID, -1);
        int questionCount = getIntent().getIntExtra(EXTRA_QUESTION_COUNT, 50);

        if (sessionId > 0) {
            session = repo.getSessionById(sessionId);
        }

        if (session == null) {
            session = repo.createSession(questionCount);
        }

        if (session == null) {
            Log.e(TAG, "Failed to create quiz session");
            Toast.makeText(this,
                    langMgr.isChinese() ? "创建练习失败" : "Failed to create quiz session",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        questions = repo.getSessionQuestions(session);
        sessionResults = repo.getSessionResults(session);

        if (questions == null || questions.isEmpty()) {
            Log.e(TAG, "No questions loaded for session " + session.id);
            Toast.makeText(this,
                    langMgr.isChinese() ? "没有可用的题目" : "No questions available",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Find first unanswered question
        currentIndex = 0;
        for (int i = 0; i < sessionResults.size(); i++) {
            if ("-".equals(sessionResults.get(i))) {
                currentIndex = i;
                break;
            }
        }

        // Ensure currentIndex is valid
        if (currentIndex >= questions.size()) {
            currentIndex = 0;
        }

        progressBar.setMax(session.totalQuestions);

        btnNext.setOnClickListener(v -> {
            int nextIndex = findNextUnanswered(currentIndex + 1);
            if (nextIndex < 0) {
                showResult();
            } else {
                currentIndex = nextIndex;
                showQuestion();
            }
        });

        // Handle back press with modern API
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackNavigation();
            }
        });

        showQuestion();
    }

    private void updateLangToggleText() {
        if (tvLangToggle != null && langMgr != null) {
            tvLangToggle.setText(langMgr.isChinese() ? "EN" : "中");
        }
    }

    private void refreshCurrentQuestion() {
        if (questions == null || questions.isEmpty()) return;
        if (currentIndex < 0 || currentIndex >= questions.size()) return;

        if (answered) {
            showAnsweredQuestion();
        } else {
            showQuestion();
        }
    }

    private void showAnsweredQuestion() {
        if (questions == null || currentIndex >= questions.size()) return;

        Question q = questions.get(currentIndex);
        String lang = langMgr.getLanguage();

        tvProgress.setText(formatProgress(session.answeredCount, session.totalQuestions));
        tvQuestion.setText(q.getQuestionText(lang));

        // Show question image
        loadQuestionImage(q);

        // Rebuild options with answer highlighting
        layoutOptions.removeAllViews();
        List<String> options = q.getOptions(lang);
        if (options == null) return;
        char[] labels = {'A', 'B', 'C', 'D'};

        // Check session results - "1" means correct, "0" means wrong, "-" means unanswered
        String resultStr = (sessionResults != null && currentIndex < sessionResults.size())
                ? sessionResults.get(currentIndex) : "-";
        boolean isCorrect = "1".equals(resultStr);

        for (int i = 0; i < options.size(); i++) {
            TextView optView = new TextView(this);
            String label = (i < labels.length) ? labels[i] + ". " : "";
            optView.setText(label + options.get(i));
            optView.setTextSize(18);
            optView.setPadding(28, 24, 28, 24);

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(12);
            if (i == q.correct_answer) {
                bg.setColor(Color.parseColor("#C8E6C9"));
                bg.setStroke(2, getResources().getColor(R.color.correct_green, null));
            } else {
                bg.setColor(Color.parseColor("#EEEEEE"));
                bg.setStroke(1, Color.parseColor("#BDBDBD"));
            }
            optView.setBackground(bg);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 12;
            optView.setLayoutParams(params);
            layoutOptions.addView(optView);
        }

        // Update feedback text
        tvFeedback.setVisibility(View.VISIBLE);
        if (isCorrect) {
            tvFeedback.setText(langMgr.isChinese() ? "✓ 正确" : "✓ Correct");
            tvFeedback.setTextColor(getResources().getColor(R.color.correct_green, null));
            tvCorrectAnswer.setVisibility(View.GONE);
        } else {
            tvFeedback.setText(langMgr.isChinese() ? "✗ 错误" : "✗ Incorrect");
            tvFeedback.setTextColor(getResources().getColor(R.color.wrong_red, null));
            String correctLabel = langMgr.isChinese() ? "正确答案：" : "Correct answer: ";
            tvCorrectAnswer.setText(correctLabel + q.getCorrectOptionText(lang));
            tvCorrectAnswer.setVisibility(View.VISIBLE);
        }

        // Update next button text
        btnNext.setVisibility(View.VISIBLE);
        if (session.isCompleted) {
            btnNext.setText(langMgr.isChinese() ? "完成" : "Finish");
        } else {
            btnNext.setText(langMgr.isChinese() ? "下一题" : "Next");
        }
    }

    private int findNextUnanswered(int startFrom) {
        sessionResults = repo.getSessionResults(session);
        if (sessionResults == null) return -1;
        for (int i = startFrom; i < sessionResults.size(); i++) {
            if ("-".equals(sessionResults.get(i))) {
                return i;
            }
        }
        return -1;
    }

    private String formatProgress(int current, int total) {
        if (langMgr.isChinese()) {
            return String.format("第 %d / %d 题", current, total);
        } else {
            return String.format("Q %d / %d", current, total);
        }
    }

    private void showQuestion() {
        if (questions == null || questions.isEmpty()) return;
        if (currentIndex < 0 || currentIndex >= questions.size()) return;

        answered = false;
        Question q = questions.get(currentIndex);
        String lang = langMgr.getLanguage();

        tvProgress.setText(formatProgress(session.answeredCount + 1, session.totalQuestions));
        tvScore.setText(session.correctCount + " ✓");
        progressBar.setProgress(session.answeredCount);

        // Display question in selected language only
        tvQuestion.setText(q.getQuestionText(lang));

        // Show question image if available
        loadQuestionImage(q);

        tvFeedback.setVisibility(View.GONE);
        tvCorrectAnswer.setVisibility(View.GONE);
        btnNext.setVisibility(View.GONE);

        layoutOptions.removeAllViews();
        List<String> options = q.getOptions(lang);
        if (options == null) return;

        char[] labels = {'A', 'B', 'C', 'D'};

        for (int i = 0; i < options.size(); i++) {
            final int optionIndex = i;
            TextView optionView = new TextView(this);
            String label = (i < labels.length) ? labels[i] + ". " : "";

            String displayText = label + options.get(i);
            optionView.setText(displayText);
            optionView.setTextSize(18);
            optionView.setPadding(28, 24, 28, 24);

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

    private void loadQuestionImage(Question q) {
        if (q.images != null && !q.images.isEmpty()) {
            String imageName = q.images.get(0);
            try {
                InputStream is = getAssets().open("images/" + imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                if (bitmap != null) {
                    ivQuestionImage.setImageBitmap(bitmap);
                    ivQuestionImage.setVisibility(View.VISIBLE);
                    return;
                }
            } catch (Exception e) {
                Log.w(TAG, "Failed to load image: " + imageName, e);
            }
        }
        ivQuestionImage.setVisibility(View.GONE);
    }

    private void onOptionSelected(int selectedIndex) {
        if (answered) return;
        if (questions == null || currentIndex >= questions.size()) return;

        answered = true;

        Question q = questions.get(currentIndex);
        String lang = langMgr.getLanguage();
        boolean isCorrect = (selectedIndex == q.correct_answer);

        repo.recordSessionAnswer(session, currentIndex, isCorrect);
        session = repo.getSessionById(session.id);
        // Refresh session results after recording answer
        sessionResults = repo.getSessionResults(session);

        tvFeedback.setVisibility(View.VISIBLE);
        if (isCorrect) {
            tvFeedback.setText(langMgr.isChinese() ? "✓ 正确" : "✓ Correct");
            tvFeedback.setTextColor(getResources().getColor(R.color.correct_green, null));
        } else {
            tvFeedback.setText(langMgr.isChinese() ? "✗ 错误" : "✗ Incorrect");
            tvFeedback.setTextColor(getResources().getColor(R.color.wrong_red, null));

            String correctLabel = langMgr.isChinese() ? "正确答案：" : "Correct answer: ";
            tvCorrectAnswer.setText(correctLabel + q.getCorrectOptionText(lang));
            tvCorrectAnswer.setVisibility(View.VISIBLE);
        }

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

        tvScore.setText(session.correctCount + " ✓");
        tvProgress.setText(formatProgress(session.answeredCount, session.totalQuestions));
        progressBar.setProgress(session.answeredCount);

        btnNext.setVisibility(View.VISIBLE);
        if (session.isCompleted) {
            btnNext.setText(langMgr.isChinese() ? "完成" : "Finish");
        } else {
            btnNext.setText(langMgr.isChinese() ? "下一题" : "Next");
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

    private void handleBackNavigation() {
        if (session != null && !session.isCompleted) {
            String title = langMgr.isChinese() ? "暂停练习" : "Pause Practice";
            String msg = langMgr.isChinese() ? "进度已自动保存，下次可以继续。" : "Progress saved. You can resume later.";
            String exit = langMgr.isChinese() ? "退出" : "Exit";
            String cont = langMgr.isChinese() ? "继续" : "Continue";
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(msg)
                    .setPositiveButton(exit, (d, w) -> finish())
                    .setNegativeButton(cont, null)
                    .show();
        } else {
            finish();
        }
    }
}
