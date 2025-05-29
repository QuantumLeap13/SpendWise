package com.abhi.spendwise;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenseList;
    private OnExpenseLongClickListener longClickListener;

    public interface OnExpenseLongClickListener {
        void onExpenseLongClick(Expense expense);
    }

    public ExpenseAdapter(List<Expense> expenseList, OnExpenseLongClickListener listener) {
        this.expenseList = expenseList;
        this.longClickListener = listener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenseList.get(position);
        holder.titleTextView.setText(expense.getTitle());
        holder.amountTextView.setText("₹" + expense.getAmount());
        holder.categoryTextView.setText(expense.getCategory());
        holder.dateTextView.setText(expense.getDate());

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onExpenseLongClick(expense);
            }
            return true; // consume the event
        });
    }

    @Override
    public int getItemCount() {
        return expenseList.size();
    }

    public static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView titleTextView, amountTextView, categoryTextView, dateTextView;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
        }
    }

    // Helper method to remove an expense from the list and notify adapter
    public void removeExpense(Expense expense) {
        int position = expenseList.indexOf(expense);
        if (position != -1) {
            expenseList.remove(position);
            notifyItemRemoved(position);
        }
    }
}
