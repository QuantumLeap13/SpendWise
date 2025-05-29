package com.abhi.spendwise;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private EditText titleEditText, amountEditText, categoryEditText, dateEditText;
    private Button saveButton;
    private ExpenseDatabase expenseDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        titleEditText = findViewById(R.id.titleEditText);
        amountEditText = findViewById(R.id.amountEditText);
        categoryEditText = findViewById(R.id.categoryEditText);
        saveButton = findViewById(R.id.saveButton);

        expenseDatabase = ExpenseDatabase.getDatabase(this);

        saveButton.setOnClickListener(v -> saveExpense());
    }

    private void saveExpense() {
        String title = titleEditText.getText().toString().trim();
        String amountStr = amountEditText.getText().toString().trim();
        String category = categoryEditText.getText().toString().trim();

        if (TextUtils.isEmpty(title) || TextUtils.isEmpty(amountStr) || TextUtils.isEmpty(category)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get current date and time as formatted string
        String currentDateTime = new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()
        ).format(new java.util.Date());

        Expense expense = new Expense(title, amount, category, currentDateTime);

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                expenseDatabase.expenseDao().insertExpense(expense);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                Toast.makeText(AddExpenseActivity.this, "Expense saved", Toast.LENGTH_SHORT).show();
                finish();
            }
        }.execute();
    }


}
