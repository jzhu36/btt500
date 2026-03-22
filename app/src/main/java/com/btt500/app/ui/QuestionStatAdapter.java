package com.btt500.app.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btt500.app.R;
import com.btt500.app.data.AnswerRecord;
import com.btt500.app.data.QuestionRepository;

import java.util.Collections;
import java.util.List;

public class QuestionStatAdapter extends RecyclerView.Adapter<QuestionStatAdapter.ViewHolder> {

    private List<QuestionRepository.QuestionStat> stats;
    private final Context context;

    public QuestionStatAdapter(Context context, List<QuestionRepository.QuestionStat> stats) {
        this.context = context;
        this.stats = stats;
    }

    public void updateData(List<QuestionRepository.QuestionStat> newStats) {
        this.stats = newStats;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_question_stat, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QuestionRepository.QuestionStat stat = stats.get(position);

        holder.tvQuestion.setText((position + 1) + ". " + stat.question.getDisplayQuestion());

        // Topic tag
        if (stat.question.topic != null && !stat.question.topic.isEmpty()) {
            holder.tvTopic.setText(stat.question.topic);
            holder.tvTopic.setVisibility(View.VISIBLE);
        } else {
            holder.tvTopic.setVisibility(View.GONE);
        }

        // History icons: build a row of ✓ and ✗
        holder.layoutHistoryIcons.removeAllViews();
        if (stat.records != null && !stat.records.isEmpty()) {
            // Records are ordered DESC (newest first), we want to display oldest first (left to right)
            List<AnswerRecord> orderedRecords = new java.util.ArrayList<>(stat.records);
            Collections.reverse(orderedRecords);

            for (AnswerRecord record : orderedRecords) {
                TextView icon = new TextView(context);
                if (record.isCorrect) {
                    icon.setText("✓");
                    icon.setTextColor(context.getResources().getColor(R.color.correct_green, null));
                } else {
                    icon.setText("✗");
                    icon.setTextColor(context.getResources().getColor(R.color.wrong_red, null));
                }
                icon.setTextSize(14);
                icon.setTypeface(null, Typeface.BOLD);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMarginEnd(4);
                icon.setLayoutParams(params);
                holder.layoutHistoryIcons.addView(icon);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, QuestionDetailActivity.class);
            intent.putExtra("questionId", stat.question.id);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvTopic;
        LinearLayout layoutHistoryIcons;

        ViewHolder(View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvItemQuestion);
            tvTopic = itemView.findViewById(R.id.tvItemTopic);
            layoutHistoryIcons = itemView.findViewById(R.id.layoutHistoryIcons);
        }
    }
}
