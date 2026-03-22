package com.btt500.app.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.btt500.app.R;
import com.btt500.app.data.QuestionRepository;

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
        holder.tvAttempts.setText(context.getString(R.string.practiced_times, stat.attemptCount));
        holder.tvWrongs.setText(context.getString(R.string.wrong_times, stat.wrongCount));

        if (stat.question.topic != null) {
            holder.tvTopic.setText(stat.question.topic);
        } else {
            holder.tvTopic.setText("");
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
        TextView tvQuestion, tvAttempts, tvWrongs, tvTopic;

        ViewHolder(View itemView) {
            super(itemView);
            tvQuestion = itemView.findViewById(R.id.tvItemQuestion);
            tvAttempts = itemView.findViewById(R.id.tvItemAttempts);
            tvWrongs = itemView.findViewById(R.id.tvItemWrongs);
            tvTopic = itemView.findViewById(R.id.tvItemTopic);
        }
    }
}
