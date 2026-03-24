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
    private String questionId;
    private LinearLayout layoutRecords;
    private TextView tvNoHistory;
    private boolean showingAll = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        questionId = getIntent().getStringExtra("questionId");
        repo = new QuestionRepository(this);
        Question q = repo.getQuestionById(questionId);

        if (q == null) {
            finish();
            return;
        }

        // Question text
        TextView tvQuestion = findViewById(R.id.tvDetailQuestion);
        tvQuestion.setText(q.getDisplayQuestion());

        TextView tvQuestionEn = findViewById(R.id.tvDetailQuestionEn);
        if (q.question_en != null && !q.question_en.isEmpty()) {
            tvQuestionEn.setText(q.question_en);
            tvQuestionEn.setVisibility(View.VISIBLE);
        } else {
            tvQuestionEn.setVisibility(View.GONE);
        }

        // Question image
        ImageView ivDetailImage = findViewById(R.id.ivDetailImage);
        if (q.images != null && !q.images.isEmpty()) {
            String imageName = q.images.get(0);
            try {
                InputStream is = getAssets().open("images/" + imageName);
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                is.close();
                if (bitmap != null) {
                    ivDetailImage.setImageBitmap(bitmap);
                    ivDetailImage.setVisibility(View.VISIBLE);
                }
            } catch (Exception e) {
                ivDetailImage.setVisibility(View.GONE);
            }
        } else {
            ivDetailImage.setVisibility(View.GONE);
        }

        // Options with correct highlighted
        LinearLayout layoutOptions = findViewById(R.id.layoutDetailOptions);
        char[] labels = {'A', 'B', 'C', 'D'};
        List<String> options = q.getDisplayOptions();
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                TextView optView = new TextView(this);
                String label = (i < labels.length) ? labels[i] + ". " : "";
                String text = label + options.get(i);
                if (q.options_en != null && i < q.options_en.size()) {
                    text += "\n" + q.options_en.get(i);
                }
                optView.setText(text);
                optView.setTextSize(18);
                optView.setPadding(24, 20, 24, 20);

                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(10);
                if (i == q.correct_answer) {
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
        TextView tvAttempts = findViewById(R.id.tvDetailAttempts);
        TextView tvWrongs = findViewById(R.id.tvDetailWrongs);
        int attempts = repo.getAttemptCount(questionId);
        int wrongs = repo.getWrongCount(questionId);
        tvAttempts.setText(getString(R.string.practiced_times, attempts));
        tvWrongs.setText(getString(R.string.wrong_times, wrongs));

        // Records
        layoutRecords = findViewById(R.id.layoutRecords);
        tvNoHistory = findViewById(R.id.tvNoHistory);

        MaterialButton btnAll = findViewById(R.id.btnAllRecords);
        MaterialButton btnRecent = findViewById(R.id.btnRecentRecords);

        btnAll.setOnClickListener(v -> {
            showingAll = true;
            loadRecords();
        });

        btnRecent.setOnClickListener(v -> {
            showingAll = false;
            loadRecords();
        });

        loadRecords();
    }

    private void loadRecords() {
        layoutRecords.removeAllViews();

        List<AnswerRecord> records;
        if (showingAll) {
            records = repo.getRecordsForQuestion(questionId);
        } else {
            records = repo.getRecentRecordsForQuestion(questionId);
        }

        if (records.isEmpty()) {
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }

        tvNoHistory.setVisibility(View.GONE);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (AnswerRecord record : records) {
            TextView tv = new TextView(this);
            String time = sdf.format(new Date(record.timestamp));
            String result = record.isCorrect ? "✓ 正确" : "✗ 错误";
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
