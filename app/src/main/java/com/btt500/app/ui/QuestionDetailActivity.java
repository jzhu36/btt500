package com.btt500.app.ui;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.btt500.app.data.AnswerRecord;
import com.btt500.app.data.LanguageManager;
import com.btt500.app.data.Question;
import com.btt500.app.data.QuestionRepository;
import com.google.android.material.button.MaterialButton;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QuestionDetailActivity extends AppCompatActivity {

    private QuestionRepository repo;
    private LanguageManager langMgr;
    private String questionId;
    private Question question;
    private LinearLayout layoutRecords, layoutOptions;
    private TextView tvNoHistory, tvTitle, tvQuestion, tvQuestionEn, tvAttempts, tvWrongs, tvLangToggle;
    private ImageView ivDetailImage;
    private MaterialButton btnAll, btnRecent;
    private boolean showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        questionId = getIntent().getStringExtra("questionId");
        repo = new QuestionRepository(this);
        langMgr = LanguageManager.getInstance(this);
        question = repo.getQuestionById(questionId);

        if (question == null) {
            finish();
            return;
        }

        // Bind views
        tvTitle = findViewById(R.id.tvDetailTitle);
        tvQuestion = findViewById(R.id.tvDetailQuestion);
        tvQuestionEn = findViewById(R.id.tvDetailQuestionEn);
        ivDetailImage = findViewById(R.id.ivDetailImage);
        layoutOptions = findViewById(R.id.layoutDetailOptions);
        tvAttempts = findViewById(R.id.tvDetailAttempts);
        tvWrongs = findViewById(R.id.tvDetailWrongs);
        layoutRecords = findViewById(R.id.layoutRecords);
        tvNoHistory = findViewById(R.id.tvNoHistory);
        btnAll = findViewById(R.id.btnAllRecords);
        btnRecent = findViewById(R.id.btnRecentRecords);
        tvLangToggle = findViewById(R.id.tvDetailLangToggle);

        // Language toggle
        updateLangToggleText();
        tvLangToggle.setOnClickListener(v -> {
            langMgr.toggleLanguage();
            updateLangToggleText();
            refreshUI();
        });

        // Hide English subtitle (single language mode)
        if (tvQuestionEn != null) {
            tvQuestionEn.setVisibility(View.GONE);
        }

        // Load image
        loadQuestionImage();

        // Record tabs
        btnAll.setOnClickListener(v -> {
            showingAll = true;
            loadRecords();
        });
        btnRecent.setOnClickListener(v -> {
            showingAll = false;
            loadRecords();
        });

        refreshUI();
    }

    private void updateLangToggleText() {
        tvLangToggle.setText(langMgr.isChinese() ? "EN" : "\u4e2d");
    }

    private void refreshUI() {
        boolean zh = langMgr.isChinese();
        String lang = langMgr.getLanguage();

        // Title
        tvTitle.setText(zh ? "\u9898\u76ee\u8be6\u60c5" : "Question Detail");

        // Question text
        tvQuestion.setText(question.getQuestionText(lang));

        // Options
        layoutOptions.removeAllViews();
        char[] labels = {'A', 'B', 'C', 'D'};
        List<String> options = question.getOptions(lang);
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                TextView optView = new TextView(this);
                String label = (i < labels.length) ? labels[i] + ". " : "";
                optView.setText(label + options.get(i));
                optView.setTextSize(18);
                optView.setPadding(24, 20, 24, 20);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(10);
                if (i == question.correct_answer) {
                    bg.setColor(Color.parseColor("#C8E6C9"));
                    bg.setStroke(2, getResources().getColor(R.color.correct_green, null));
                } else {
                    bg.setColor(Color.parseColor("#F5F5F5"));
                    bg.setStroke(1, Color.parseColor("#E0E0E0"));
                }
                optView.setBackground(bg);

                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.bottomMargin = 8;
                optView.setLayoutParams(params);
                layoutOptions.addView(optView);
            }
        }

        // Stats
        int attempts = repo.getAttemptCount(questionId);
        int wrongs = repo.getWrongCount(questionId);
        if (zh) {
            tvAttempts.setText("\u7ec3\u4e60 " + attempts + " \u6b21");
            tvWrongs.setText("\u9519\u8bef " + wrongs + " \u6b21");
        } else {
            tvAttempts.setText("Practiced: " + attempts);
            tvWrongs.setText("Wrong: " + wrongs);
        }

        // Tab buttons
        btnAll.setText(zh ? "\u5168\u90e8\u8bb0\u5f55" : "All Records");
        btnRecent.setText(zh ? "\u6700\u8fd1\u5341\u6b21" : "Recent 10");

        loadRecords();
    }

    private void loadQuestionImage() {
        if (question.images != null && !question.images.isEmpty()) {
            String imageName = question.images.get(0);
            try {
                InputStream is = getAssets().open("images/" + imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                if (bitmap != null) {
                    ivDetailImage.setImageBitmap(bitmap);
                    ivDetailImage.setVisibility(View.VISIBLE);
                    return;
                }
            } catch (Exception e) {
                // ignore
            }
        }
        ivDetailImage.setVisibility(View.GONE);
    }

    private void loadRecords() {
        layoutRecords.removeAllViews();
        boolean zh = langMgr.isChinese();

        List<AnswerRecord> records;
        if (showingAll) {
            records = repo.getRecordsForQuestion(questionId);
        } else {
            records = repo.getRecentRecordsForQuestion(questionId);
        }

        if (records.isEmpty()) {
            tvNoHistory.setText(zh ? "\u6682\u65e0\u7b54\u9898\u8bb0\u5f55" : "No records yet");
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }

        tvNoHistory.setVisibility(View.GONE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (AnswerRecord record : records) {
            TextView tv = new TextView(this);
            String time = sdf.format(new Date(record.timestamp));
            String result;
            if (zh) {
                result = record.isCorrect ? "\u2713 \u6b63\u786e" : "\u2717 \u9519\u8bef";
            } else {
                result = record.isCorrect ? "\u2713 Correct" : "\u2717 Incorrect";
            }
            tv.setText(time + "  " + result);
            tv.setTextSize(14);
            tv.setPadding(16, 10, 16, 10);

            if (record.isCorrect) {
                tv.setTextColor(getResources().getColor(R.color.correct_green, null));
            } else {
                tv.setTextColor(getResources().getColor(R.color.wrong_red, null));
            }

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.bottomMargin = 4;
            tv.setLayoutParams(params);
            layoutRecords.addView(tv);
        }
    }
}
