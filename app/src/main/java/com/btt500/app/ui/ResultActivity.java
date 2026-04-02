package com.btt500.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.R;
import com.btt500.app.data.LanguageManager;
import com.google.android.material.button.MaterialButton;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        LanguageManager langMgr = LanguageManager.getInstance(this);
        boolean zh = langMgr.isChinese();

        int correct = getIntent().getIntExtra("correct", 0);
        int total = getIntent().getIntExtra("total", 50);

        // Title
        TextView tvTitle = findViewById(R.id.tvResultTitle);
        tvTitle.setText(zh ? "练习完成" : "Practice Complete");

        TextView tvScore = findViewById(R.id.tvResultScore);
        TextView tvDetail = findViewById(R.id.tvResultDetail);
        TextView tvPassFail = findViewById(R.id.tvPassFail);
        MaterialButton btnRetry = findViewById(R.id.btnRetry);
        MaterialButton btnHome = findViewById(R.id.btnBackHome);

        int percentage = total > 0 ? (correct * 100 / total) : 0;
        tvScore.setText(correct + "/" + total);

        tvDetail.setText((zh ? "正确率: " : "Accuracy: ") + percentage + "%");

        boolean passed = percentage >= 90;
        if (passed) {
            tvScore.setTextColor(getResources().getColor(R.color.correct_green, null));
            tvPassFail.setText(zh ? "通过!" : "Passed!");
            tvPassFail.setTextColor(getResources().getColor(R.color.correct_green, null));
        } else {
            tvScore.setTextColor(getResources().getColor(R.color.wrong_red, null));
            tvPassFail.setText(zh ? "未通过，继续加油!" : "Not passed, keep trying!");
            tvPassFail.setTextColor(getResources().getColor(R.color.wrong_red, null));
        }

        btnRetry.setText(zh ? "再来一次" : "Try Again");
        btnHome.setText(zh ? "返回首页" : "Back to Home");

        btnRetry.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });

        btnHome.setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
