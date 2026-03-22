package com.btt500.app.ui;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.btt500.app.R;
import com.btt500.app.data.QuestionRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity {

    private QuestionRepository repo;
    private QuestionStatAdapter adapter;
    private List<QuestionRepository.QuestionStat> allStats;
    private List<QuestionRepository.QuestionStat> filteredStats;
    private TextView tvQuestionCount;

    // Sort state
    private enum SortField { DEFAULT, PRACTICED, WRONG, TOPIC }
    private SortField currentSort = SortField.DEFAULT;
    private boolean ascending = true;

    // Filter state
    private String filterTopic = null; // null = all
    private boolean filterRecentWrong = false;

    // Sort buttons
    private MaterialButton btnSortDefault, btnSortPracticed, btnSortWrong, btnSortTopic;

    // Filter chips container
    private LinearLayout layoutFilterChips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        repo = new QuestionRepository(this);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);

        RecyclerView recycler = findViewById(R.id.recyclerHistory);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new QuestionStatAdapter(this, new ArrayList<>());
        recycler.setAdapter(adapter);

        btnSortDefault = findViewById(R.id.btnSortDefault);
        btnSortPracticed = findViewById(R.id.btnSortLeastPracticed);
        btnSortWrong = findViewById(R.id.btnSortMostWrong);
        btnSortTopic = findViewById(R.id.btnSortTopic);
        layoutFilterChips = findViewById(R.id.layoutFilterChips);

        btnSortDefault.setOnClickListener(v -> toggleSort(SortField.DEFAULT));
        btnSortPracticed.setOnClickListener(v -> toggleSort(SortField.PRACTICED));
        btnSortWrong.setOnClickListener(v -> toggleSort(SortField.WRONG));
        btnSortTopic.setOnClickListener(v -> toggleSort(SortField.TOPIC));

        loadData();
        buildFilterChips();
        applyFilterAndSort();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        applyFilterAndSort();
    }

    private void loadData() {
        allStats = repo.getAllQuestionStats();
    }

    private void buildFilterChips() {
        layoutFilterChips.removeAllViews();

        // "All" chip
        addFilterChip("全部", () -> {
            filterTopic = null;
            filterRecentWrong = false;
            refreshChipStyles();
            applyFilterAndSort();
        });

        // "Recently wrong" chip
        addFilterChip("最近做错", () -> {
            filterRecentWrong = !filterRecentWrong;
            if (filterRecentWrong) {
                filterTopic = null;
            }
            refreshChipStyles();
            applyFilterAndSort();
        });

        // Topic chips
        List<String> topics = repo.getAllTopics();
        for (String topic : topics) {
            addFilterChip(topic, () -> {
                if (topic.equals(filterTopic)) {
                    filterTopic = null; // deselect
                } else {
                    filterTopic = topic;
                    filterRecentWrong = false;
                }
                refreshChipStyles();
                applyFilterAndSort();
            });
        }

        refreshChipStyles();
    }

    private void addFilterChip(String text, Runnable onClick) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        chip.setTag(text);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMarginEnd(dpToPx(6));
        chip.setLayoutParams(params);

        chip.setOnClickListener(v -> onClick.run());
        layoutFilterChips.addView(chip);
    }

    private void refreshChipStyles() {
        for (int i = 0; i < layoutFilterChips.getChildCount(); i++) {
            TextView chip = (TextView) layoutFilterChips.getChildAt(i);
            String tag = (String) chip.getTag();

            boolean isSelected = false;
            if ("全部".equals(tag) && filterTopic == null && !filterRecentWrong) {
                isSelected = true;
            } else if ("最近做错".equals(tag) && filterRecentWrong) {
                isSelected = true;
            } else if (tag != null && tag.equals(filterTopic)) {
                isSelected = true;
            }

            GradientDrawable bg = new GradientDrawable();
            bg.setCornerRadius(dpToPx(16));
            if (isSelected) {
                bg.setColor(getResources().getColor(R.color.primary, null));
                chip.setTextColor(Color.WHITE);
                chip.setTypeface(null, Typeface.BOLD);
            } else {
                bg.setColor(Color.parseColor("#E8EAF6"));
                chip.setTextColor(getResources().getColor(R.color.dark_text, null));
                chip.setTypeface(null, Typeface.NORMAL);
            }
            chip.setBackground(bg);
        }
    }

    private void toggleSort(SortField field) {
        if (currentSort == field) {
            ascending = !ascending;
        } else {
            currentSort = field;
            // Default direction based on field
            ascending = (field == SortField.DEFAULT || field == SortField.PRACTICED || field == SortField.TOPIC);
        }
        updateSortButtonLabels();
        applyFilterAndSort();
    }

    private void updateSortButtonLabels() {
        String arrow = ascending ? " ↑" : " ↓";
        btnSortDefault.setText(currentSort == SortField.DEFAULT ? "默认" + arrow : "默认");
        btnSortPracticed.setText(currentSort == SortField.PRACTICED ? "练习次数" + arrow : "练习次数");
        btnSortWrong.setText(currentSort == SortField.WRONG ? "错误次数" + arrow : "错误次数");
        btnSortTopic.setText(currentSort == SortField.TOPIC ? "题目种类" + arrow : "题目种类");
    }

    private void applyFilterAndSort() {
        // Step 1: Filter
        filteredStats = new ArrayList<>();

        Set<String> recentlyWrongIds = null;
        if (filterRecentWrong) {
            recentlyWrongIds = repo.getRecentlyWrongQuestionIds();
        }

        for (QuestionRepository.QuestionStat stat : allStats) {
            // Apply topic filter
            if (filterTopic != null) {
                if (stat.question.topic == null || !stat.question.topic.equals(filterTopic)) {
                    continue;
                }
            }
            // Apply recently wrong filter
            if (filterRecentWrong && recentlyWrongIds != null) {
                if (!recentlyWrongIds.contains(stat.question.id)) {
                    continue;
                }
            }
            filteredStats.add(stat);
        }

        // Step 2: Sort
        switch (currentSort) {
            case PRACTICED:
                Collections.sort(filteredStats, (a, b) -> {
                    int cmp = Integer.compare(a.attemptCount, b.attemptCount);
                    return ascending ? cmp : -cmp;
                });
                break;
            case WRONG:
                Collections.sort(filteredStats, (a, b) -> {
                    int cmp = Integer.compare(a.wrongCount, b.wrongCount);
                    return ascending ? cmp : -cmp;
                });
                break;
            case TOPIC:
                Collections.sort(filteredStats, (a, b) -> {
                    String topicA = a.question.topic != null ? a.question.topic : "";
                    String topicB = b.question.topic != null ? b.question.topic : "";
                    int cmp = topicA.compareTo(topicB);
                    return ascending ? cmp : -cmp;
                });
                break;
            case DEFAULT:
            default:
                if (!ascending) {
                    Collections.reverse(filteredStats);
                }
                break;
        }

        // Update count display
        tvQuestionCount.setText(filteredStats.size() + "/" + allStats.size());

        adapter.updateData(filteredStats);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
