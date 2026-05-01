/**
 * آداپتر لقائمة تاريخ الاختبارات — كل سطر جلسة قديمة، تقدر تفتحه وتشوف مين نجح ومين لا بالحروف.
 */
package com.example.RemasProject;

import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.RemasProject.model.LetterQuizSession;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LetterQuizHistoryAdapter extends RecyclerView.Adapter<LetterQuizHistoryAdapter.SessionVH> {

    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("yyyy-MM-dd  •  HH:mm", Locale.getDefault());
    private final List<LetterQuizSession> sessions = new ArrayList<>();
    private final SparseBooleanArray expanded = new SparseBooleanArray();

    public void setSessions(List<LetterQuizSession> list) {
        // كل تحديث للقائمة يصفّر حالة الطي حتى ما يختلط موضع «مفتوح» مع صف جديد
        sessions.clear();
        expanded.clear();
        if (list != null) {
            sessions.addAll(list);
        }
        notifyDataSetChanged();
    }

    private void toggle(int adapterPosition) {
        boolean now = !expanded.get(adapterPosition, false);
        expanded.put(adapterPosition, now);
        notifyItemChanged(adapterPosition);
    }

    @NonNull
    @Override
    public SessionVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_letter_quiz_history, parent, false);
        return new SessionVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionVH holder, int position) {
        LetterQuizSession s = sessions.get(position);
        holder.tvBadge.setText(String.valueOf(position + 1));
        holder.tvDate.setText(dateFormat.format(new Date(s.getCompletedAtEpochMs())));

        boolean isOpen = expanded.get(position, false);
        holder.tvExpandHint.setText(isOpen ? "▲" : "▼");

        holder.containerOutcomes.removeAllViews();
        if (isOpen) {
            holder.containerOutcomes.setVisibility(View.VISIBLE);
            bindOutcomes(holder.containerOutcomes, s);
        } else {
            holder.containerOutcomes.setVisibility(View.GONE);
        }

        holder.rowSessionHeader.setOnClickListener(v -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) {
                toggle(pos);
            }
        });
    }

    private void bindOutcomes(LinearLayout container, LetterQuizSession s) {
        LayoutInflater inf = LayoutInflater.from(container.getContext());
        // قاعدة العرض: مرتان صح = نجاح كامل؛ مرة = تشجيع؛ صفر = يحتاج استماع إضافي
        for (LetterQuizSession.LetterOutcome o : s.getOutcomes()) {
            if (o == null) {
                continue;
            }
            View row = inf.inflate(R.layout.item_quiz_letter_outcome, container, false);
            TextView emoji = row.findViewById(R.id.tvOutcomeEmoji);
            TextView letter = row.findViewById(R.id.tvOutcomeLetter);
            TextView label = row.findViewById(R.id.tvOutcomeLabel);

            String L = o.getLetter() != null ? o.getLetter() : "?";
            letter.setText(L);

            boolean full = o.isPassedFull() || o.getCorrectCount() >= 2;
            int c = o.getCorrectCount();
            row.setBackgroundResource(R.drawable.bg_outcome_row);
            if (full) {
                emoji.setText("⭐");
                label.setText("نجاح كامل — أحسنت!\nصحيح مرتين (2/2)");
            } else if (c == 1) {
                emoji.setText("👍");
                label.setText("إجابة صحيحة واحدة\nجرّب مرة أخرى لاحقاً (1/2)");
            } else {
                emoji.setText("🔄");
                label.setText("عد للاستماع للحرف\nيُصفّر العداد (0/2)");
            }
            container.addView(row);
        }
    }

    @Override
    public int getItemCount() {
        return sessions.size();
    }

    static final class SessionVH extends RecyclerView.ViewHolder {
        final LinearLayout rowSessionHeader;
        final TextView tvBadge;
        final TextView tvDate;
        final TextView tvExpandHint;
        final LinearLayout containerOutcomes;

        SessionVH(@NonNull View itemView) {
            super(itemView);
            rowSessionHeader = itemView.findViewById(R.id.rowSessionHeader);
            tvBadge = itemView.findViewById(R.id.tvSessionBadge);
            tvDate = itemView.findViewById(R.id.tvSessionDate);
            tvExpandHint = itemView.findViewById(R.id.tvExpandHint);
            containerOutcomes = itemView.findViewById(R.id.containerOutcomes);
        }
    }
}
