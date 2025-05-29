package com.abhi.spendwise;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ExpenseDao {

    // Insert a new expense
    @Insert
    void insertExpense(Expense expense);

    // Fetch all expenses sorted by date descending (latest first)
    @Query("SELECT * FROM expenses ORDER BY date DESC")
    List<Expense> getAllExpenses();

    // Fetch expenses between dates, sorted by date descending
    @Query("SELECT * FROM expenses WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    List<Expense> getExpensesBetweenDates(String startDate, String endDate);

    // Delete an expense
    @Delete
    void deleteExpense(Expense expense);

    // Optional: update existing expense (useful for editing)
    @Update
    void updateExpense(Expense expense);
}
