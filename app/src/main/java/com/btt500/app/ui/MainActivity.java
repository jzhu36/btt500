package com.btt500.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.btt500.app.BTTApplication;
import com.btt500.app.R;
import com.btt500.app.data.QuestionRepository;
import com.google.android.material.button.MaterialButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MaterialButton btnStart = findViewById(R.id.btnStartQuiz);
        MaterialButton btnHistory = findViewById(R.id.btnHistory);
        TextView tvTotal = findViewById(R.id.tvTotalQuestions);

        QuestionRepository repo = new QuestionRepository(this);
        int total = repo.getTotalQuestionCount();
        tvTotal.setText(getString(R.string.total_questions, total));

        btnStart.setOnClickListener(v -> {
            Intent intent = new Intent(this, QuizActivity.class);
            startActivity(intent);
        });

        btnHistory.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistoryActivity.class);
            startActivity(intent);
        });
    }
}
