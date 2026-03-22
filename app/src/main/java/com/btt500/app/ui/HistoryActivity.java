package com.btt500.app.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.btt500.app.R;
import com.btt500.app.data.QuestionRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private QuestionRepository repo;
    private QuestionStatAdapter adapter;
    private List<QuestionRepository.QuestionStat> allStats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        repo = new QuestionRepository(this);
        allStats = repo.getAllQuestionStats();

        RecyclerView recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionStatAdapter(this, new ArrayList<>(allStats));
        recycler.setAdapter(adapter);

        MaterialButton btnDefault = findViewById(R.id.btnSortDefault);
        MaterialButton btnLeast = findViewById(R.id.btnSortLeastPracticed);
        MaterialButton btnMostWrong = findViewById(R.id.btnSortMostWrong);

        btnDefault.setOnClickListener(v -> sortDefault());
        btnLeast.setOnClickListener(v -> sortByLeastPracticed());
        btnMostWrong.setOnClickListener(v -> sortByMostWrong());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh stats when returning from detail
        allStats = repo.getAllQuestionStats();
        sortDefault();
    }

    private void sortDefault() {
        List<QuestionRepository.QuestionStat> sorted = new ArrayList<>(allStats);
        adapter.updateData(sorted);
    }

    private void sortByLeastPracticed() {
        List<QuestionRepository.QuestionStat> sorted = new ArrayList<>(allStats);
        Collections.sort(sorted, (a, b) -> Integer.compare(a.attemptCount, b.attemptCount));
        adapter.updateData(sorted);
    }

    private void sortByMostWrong() {
        List<QuestionRepository.QuestionStat> sorted = new ArrayList<>(allStats);
        Collections.sort(sorted, (a, b) -> Integer.compare(b.wrongCount, a.wrongCount));
        adapter.updateData(sorted);
    }
}
