package com.btt500.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.google.android.material.button.MaterialButton;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        int correct = getIntent().getIntExtra("correct", 0);
        int total = getIntent().getIntExtra("total", 50);

        TextView tvScore = findViewById(R.id.tvResultScore);
        TextView tvDetail = findViewById(R.id.tvResultDetail);
        TextView tvPassFail = findViewById(R.id.tvPassFail);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);
        MaterialButton btnHome = findViewById(R.id.btnBackHome);

        int percentage = total > 0 ? (correct * 100 / total) : 0;
        tvScore.setText(correct + "/" + total);

        tvDetail.setText("正确率: " + percentage + "%");

        // BTT pass mark is 90% (45/50)
        boolean passed = percentage >= 90;
        if (passed) {
            tvScore.setTextColor(getResources().getColor(R.color.correct_green, null));
            tvPassFail.setText("通过! 🎉");
            tvPassFail.setTextColor(getResources().getColor(R.color.correct_green, null));
        } else {
            tvScore.setTextColor(getResources().getColor(R.color.wrong_red, null));
            tvPassFail.setText("未通过，继续加油!");
            tvPassFail.setTextColor(getResources().getColor(R.color.wrong_red, null));
        }

        btnRetry.setOnClickListener(v -> {
            startActivity(new Intent(this, QuizActivity.class));
            finish();
        });

        btnHome.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
