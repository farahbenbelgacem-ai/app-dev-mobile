package com.example.myapplication.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Expense;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    public interface OnExpenseDeleteListener {
        void onDeleteExpense(Expense expense);
    }

    private List<Expense> expenses = new ArrayList<>();
    private final OnExpenseDeleteListener deleteListener;

    public ExpenseAdapter(OnExpenseDeleteListener deleteListener) {
        this.deleteListener = deleteListener;
    }

    public void setExpenses(List<Expense> expenses) {
        this.expenses = expenses;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense, deleteListener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvIcon, tvCategory, tvNote, tvDate, tvAmount;
        private final ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvIcon = itemView.findViewById(R.id.tv_expense_icon);
            tvCategory = itemView.findViewById(R.id.tv_expense_category);
            tvNote = itemView.findViewById(R.id.tv_expense_note);
            tvDate = itemView.findViewById(R.id.tv_expense_date);
            tvAmount = itemView.findViewById(R.id.tv_expense_amount);
            btnDelete = itemView.findViewById(R.id.btn_delete_expense);
        }

        public void bind(Expense expense, OnExpenseDeleteListener listener) {
            tvCategory.setText(expense.getCategory());
            tvNote.setText(expense.getNote() != null && !expense.getNote().isEmpty() ? expense.getNote() : "Aucune note");
            tvDate.setText(expense.getDate());
            tvAmount.setText(String.format(Locale.FRANCE, "-%.2f €", expense.getAmount()));

            // Extract Emoji icon from category string if available
            String icon = "💰";
            if (expense.getCategory() != null) {
                if (expense.getCategory().contains("🍔")) icon = "🍔";
                else if (expense.getCategory().contains("🚌")) icon = "🚌";
                else if (expense.getCategory().contains("🍻")) icon = "🍻";
                else if (expense.getCategory().contains("📚")) icon = "📚";
                else if (expense.getCategory().contains("📱")) icon = "📱";
                else if (expense.getCategory().contains("🛒")) icon = "🛒";
            }
            tvIcon.setText(icon);

            btnDelete.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteExpense(expense);
                }
            });
        }
    }
}
