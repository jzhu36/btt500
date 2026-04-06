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
import com.btt500.app.data.LanguageManager;
import com.btt500.app.data.QuestionRepository;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class HistoryActivity extends AppCompatActivity implements LanguageManager.OnLanguageChangedListener {

    private QuestionRepository repo;
    private LanguageManager langMgr;
    private QuestionStatAdapter adapter;
    private List<QuestionRepository.QuestionStat> allStats;
    private List<QuestionRepository.QuestionStat> filteredStats;
    private TextView tvQuestionCount;
    private TextView tvHistoryTitle;
    private TextView tvHistoryLangToggle;

    // Sort state
    private enum SortField { DEFAULT, PRACTICED, WRONG, TOPIC }
    private SortField currentSort = SortField.DEFAULT;
    private boolean ascending = true;

    // Filter state
    private String filterTopic = null;
    private boolean filterRecentWrong = false;

    // Filter chip tags (language-independent)
    private static final String TAG_ALL = "__all__";
    private static final String TAG_RECENT_WRONG = "__recent_wrong__";

    // Sort buttons
    private MaterialButton btnSortDefault, btnSortPracticed, btnSortWrong, btnSortTopic;

    // Filter chips container
    private LinearLayout layoutFilterChips;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        repo = new QuestionRepository(this);
        langMgr = LanguageManager.getInstance(this);
        tvQuestionCount = findViewById(R.id.tvQuestionCount);
        tvHistoryTitle = findViewById(R.id.tvHistoryTitle);
        tvHistoryLangToggle = findViewById(R.id.tvHistoryLangToggle);

        // Language toggle button
        if (tvHistoryLangToggle != null) {
            updateLangToggleText();
            tvHistoryLangToggle.setOnClickListener(v -> {
                langMgr.toggleLanguage();
                // onLanguageChanged callback will refresh UI
            });
        }

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

        langMgr.addListener(this);

        loadData();
        refreshAllUI();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
        refreshAllUI();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        langMgr.removeListener(this);
    }

    @Override
    public void onLanguageChanged(String newLanguage) {
        refreshAllUI();
    }

    private void refreshAllUI() {
        boolean zh = langMgr.isChinese();
        tvHistoryTitle.setText(zh ? "题库浏览" : "Question Bank");
        updateLangToggleText();
        buildFilterChips();
        updateSortButtonLabels();
        applyFilterAndSort();
    }

    private void updateLangToggleText() {
        if (tvHistoryLangToggle != null && langMgr != null) {
            tvHistoryLangToggle.setText(langMgr.isChinese() ? "EN" : "中");
        }
    }

    private void loadData() {
        allStats = repo.getAllQuestionStats();
    }

    private void buildFilterChips() {
        layoutFilterChips.removeAllViews();
        boolean zh = langMgr.isChinese();

        // "All" chip
        String allLabel = zh ? "全部" : "All";
        addFilterChip(allLabel, TAG_ALL, () -> {
            filterTopic = null;
            filterRecentWrong = false;
            refreshChipStyles();
            applyFilterAndSort();
        });

        // "Last attempt wrong" chip
        String recentWrongLabel = zh ? "上次答错" : "Last Attempt Wrong";
        addFilterChip(recentWrongLabel, TAG_RECENT_WRONG, () -> {
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
            addFilterChip(topic, topic, () -> {
                if (topic.equals(filterTopic)) {
                    filterTopic = null;
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

    private void addFilterChip(String text, String tag, Runnable onClick) {
        TextView chip = new TextView(this);
        chip.setText(text);
        chip.setTextSize(13);
        chip.setPadding(dpToPx(12), dpToPx(6), dpToPx(12), dpToPx(6));
        chip.setTag(tag);

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
            if (TAG_ALL.equals(tag) && filterTopic == null && !filterRecentWrong) {
                isSelected = true;
            } else if (TAG_RECENT_WRONG.equals(tag) && filterRecentWrong) {
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
            ascending = (field == SortField.DEFAULT || field == SortField.PRACTICED || field == SortField.TOPIC);
        }
        updateSortButtonLabels();
        applyFilterAndSort();
    }

    private void updateSortButtonLabels() {
        String arrow = ascending ? " ↑" : " ↓";
        boolean zh = langMgr.isChinese();

        String defaultLabel = zh ? "默认" : "Default";
        String practicedLabel = zh ? "练习次数" : "Practiced";
        String wrongLabel = zh ? "错误次数" : "Wrong";
        String topicLabel = zh ? "题目种类" : "Topic";

        btnSortDefault.setText(currentSort == SortField.DEFAULT ? defaultLabel + arrow : defaultLabel);
        btnSortPracticed.setText(currentSort == SortField.PRACTICED ? practicedLabel + arrow : practicedLabel);
        btnSortWrong.setText(currentSort == SortField.WRONG ? wrongLabel + arrow : wrongLabel);
        btnSortTopic.setText(currentSort == SortField.TOPIC ? topicLabel + arrow : topicLabel);
    }

    private void applyFilterAndSort() {
        // Step 1: Filter
        filteredStats = new ArrayList<>();

        Set<String> recentlyWrongIds = null;
        if (filterRecentWrong) {
            // Use last-one-wrong for the history page filter
            recentlyWrongIds = repo.getLastOneWrongQuestionIds();
        }

        for (QuestionRepository.QuestionStat stat : allStats) {
            if (filterTopic != null) {
                if (stat.question.topic == null || !stat.question.topic.equals(filterTopic)) {
                    continue;
                }
            }
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

        tvQuestionCount.setText(filteredStats.size() + "/" + allStats.size());
        adapter.updateData(filteredStats);
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
