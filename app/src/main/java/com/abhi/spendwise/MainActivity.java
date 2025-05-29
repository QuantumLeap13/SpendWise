package com.abhi.spendwise;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements ExpenseAdapter.OnExpenseLongClickListener {

    private static final int STORAGE_PERMISSION_CODE = 100;

    private ExpenseDatabase expenseDatabase;
    private RecyclerView expenseRecyclerView;
    private ExpenseAdapter expenseAdapter;
    private List<Expense> expenseList = new ArrayList<>();
    private FloatingActionButton addExpenseFab;
    private PieChart pieChart;

    private Button selectDateRangeButton;
    private Button resetFilterButton;
    private Button downloadPdfButton;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        expenseRecyclerView = findViewById(R.id.expenseRecyclerView);
        addExpenseFab = findViewById(R.id.addExpenseFab);
        pieChart = findViewById(R.id.pieChart);
        selectDateRangeButton = findViewById(R.id.selectDateRangeButton);
        resetFilterButton = findViewById(R.id.resetFilterButton);
        downloadPdfButton = findViewById(R.id.downloadPdfButton);  // Initialize Download PDF button

        // Setup RecyclerView with this activity as the long-click listener
        expenseRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        expenseAdapter = new ExpenseAdapter(expenseList, this);
        expenseRecyclerView.setAdapter(expenseAdapter);

        // Setup PieChart
        pieChart.getDescription().setEnabled(false);
        pieChart.setUsePercentValues(true);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(40f);
        pieChart.setTransparentCircleRadius(45f);

        // Initialize database
        expenseDatabase = ExpenseDatabase.getDatabase(this);

        // Load and display data
        loadExpensesFromDb();

        // Add expense FAB click
        addExpenseFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
            startActivity(intent);
        });

        // Select Date Range button click
        selectDateRangeButton.setOnClickListener(v -> showStartDatePicker());

        // Reset Filter button click
        resetFilterButton.setOnClickListener(v -> {
            loadExpensesFromDb();
            Toast.makeText(MainActivity.this, "Filters reset. Showing all expenses.", Toast.LENGTH_SHORT).show();
        });

        // Download PDF button click
        downloadPdfButton.setOnClickListener(v -> checkPermissionAndGeneratePDF());
    }

    private void loadExpensesFromDb() {
        new AsyncTask<Void, Void, List<Expense>>() {
            @Override
            protected List<Expense> doInBackground(Void... voids) {
                return expenseDatabase.expenseDao().getAllExpenses();
            }

            @Override
            protected void onPostExecute(List<Expense> expenses) {
                expenseList.clear();
                expenseList.addAll(expenses);
                expenseAdapter.notifyDataSetChanged();
                updatePieChart(expenses);
            }
        }.execute();
    }

    private void updatePieChart(List<Expense> expenses) {
        Map<String, Double> categoryTotals = new HashMap<>();
        for (Expense expense : expenses) {
            double current = categoryTotals.getOrDefault(expense.getCategory(), 0.0);
            categoryTotals.put(expense.getCategory(), current + expense.getAmount());
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : categoryTotals.entrySet()) {
            entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expenses by Category");
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setValueTextSize(14f);

        PieData data = new PieData(dataSet);
        pieChart.setData(data);
        pieChart.invalidate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpensesFromDb(); // Refresh when returning from AddExpenseActivity
    }

    private void showStartDatePicker() {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog startDatePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String startDateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    try {
                        Date startDate = dateFormat.parse(startDateStr);
                        showEndDatePicker(startDate);
                    } catch (ParseException e) {
                        Toast.makeText(MainActivity.this, "Invalid start date format", Toast.LENGTH_SHORT).show();
                    }
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        startDatePicker.setTitle("Select Start Date");
        startDatePicker.show();
    }

    private void showEndDatePicker(Date startDate) {
        final Calendar calendar = Calendar.getInstance();

        DatePickerDialog endDatePicker = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String endDateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    try {
                        Date endDate = dateFormat.parse(endDateStr);

                        if (endDate.before(startDate)) {
                            Toast.makeText(MainActivity.this, "End date cannot be before start date", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        filterExpensesByDateRange(startDate, endDate);

                    } catch (ParseException e) {
                        Toast.makeText(MainActivity.this, "Invalid end date format", Toast.LENGTH_SHORT).show();
                    }
                },
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));

        endDatePicker.setTitle("Select End Date");
        endDatePicker.show();
    }

    private void filterExpensesByDateRange(Date startDate, Date endDate) {
        new AsyncTask<Void, Void, List<Expense>>() {
            @Override
            protected List<Expense> doInBackground(Void... voids) {
                List<Expense> allExpenses = expenseDatabase.expenseDao().getAllExpenses();
                List<Expense> filtered = new ArrayList<>();

                for (Expense exp : allExpenses) {
                    try {
                        Date expenseDate = dateFormat.parse(exp.getDate());
                        if (!expenseDate.before(startDate) && !expenseDate.after(endDate)) {
                            filtered.add(exp);
                        }
                    } catch (ParseException e) {
                        // Skip invalid date
                    }
                }
                return filtered;
            }

            @Override
            protected void onPostExecute(List<Expense> expenses) {
                expenseList.clear();
                expenseList.addAll(expenses);
                expenseAdapter.notifyDataSetChanged();
                updatePieChart(expenses);
                Toast.makeText(MainActivity.this, "Showing expenses from "
                        + dateFormat.format(startDate) + " to " + dateFormat.format(endDate), Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }

    // Implementation of the long click interface method
    @Override
    public void onExpenseLongClick(final Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Expense")
                .setMessage("Are you sure you want to delete this expense?")
                .setPositiveButton("Yes", (dialog, which) -> deleteExpense(expense))
                .setNegativeButton("No", null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                expenseDatabase.expenseDao().deleteExpense(expense);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                expenseList.remove(expense);
                expenseAdapter.notifyDataSetChanged();
                updatePieChart(expenseList);
                Toast.makeText(MainActivity.this, "Expense deleted", Toast.LENGTH_SHORT).show();
            }
        }.execute();
    }


    // ------------------------- PDF Generation and Permission ------------------------

    private void checkPermissionAndGeneratePDF() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // No WRITE_EXTERNAL_STORAGE permission needed on Android 10+
            generatePDF();
        } else {
            // For Android 9 and below, request permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePDF();
            } else {
                Toast.makeText(this, "Storage permission denied. Cannot generate PDF.", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void generatePDF() {
        if (expenseList.isEmpty()) {
            Toast.makeText(this, "No expenses to export.", Toast.LENGTH_SHORT).show();
            return;
        }

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();
        Paint titlePaint = new Paint();

        // Page info and dimensions
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 size approx 595x842 pts

        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        titlePaint.setTextSize(24);
        titlePaint.setFakeBoldText(true);
        canvas.drawText("SpendWise Expense Report", 150, 50, titlePaint);

        paint.setTextSize(14);

        int yPosition = 100;

        // Headers
        canvas.drawText("Date", 30, yPosition, paint);
        canvas.drawText("Category", 200, yPosition, paint);
        // canvas.drawText("Description", 280, yPosition, paint);
        canvas.drawText("Title", 300, yPosition, paint);
        canvas.drawText("Amount", 500, yPosition, paint);

        yPosition += 25;

        for (Expense expense : expenseList) {
            if (yPosition > 800) {
                // Finish page and start new if content overflows page height
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                yPosition = 50;
            }

            canvas.drawText(expense.getDate(), 30, yPosition, paint);
            canvas.drawText(expense.getCategory(), 200, yPosition, paint);
            canvas.drawText(expense.getTitle(), 300, yPosition, paint);
            canvas.drawText(String.format("%.2f", expense.getAmount()), 500, yPosition, paint);

            yPosition += 20;
        }

        pdfDocument.finishPage(page);

        // Save the PDF to external storage Downloads/SpendWise/
        String directoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/SpendWise";
        File directory = new File(directoryPath);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        String fileName = "SpendWise_Report_" + System.currentTimeMillis() + ".pdf";
        File file = new File(directory, fileName);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved to Downloads/SpendWise folder.", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Error saving PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } finally {
            pdfDocument.close();
        }

    }
}
