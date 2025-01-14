package com.tdtu.mywallet;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ExpensesDBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Expenses.db";
    private static final int DATABASE_VERSION = 1;

    public ExpensesDBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE expenses " +
                "(_id INTEGER PRIMARY KEY, " +
                "amount TEXT, " +
                "kind TEXT, " +
                "description TEXT, " +
                "date TEXT, " +
                "time TEXT);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Handle Database Upgrades
        // Should go into play on major database changes.
    }

    //---------------------------------------------
    //Handling of getting expenses, grouped by kind
    //---------------------------------------------

    public List<Expense> getExpensesGroupedByKind() {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        String[] projection = {"kind", "SUM(amount) as total_amount"};
        String groupBy = "kind";
        Cursor cursor = db.query("expenses",
                projection,
                null,
                null,
                groupBy,
                null,
                null);

        // Create an Expense object with the kind and total amount for every match
        while (cursor.moveToNext()) {
            String kind = cursor.getString(
                    Math.max(0,cursor.getColumnIndex("kind")));

            double totalAmount = cursor.getDouble(
                    Math.max(0,cursor.getColumnIndex("total_amount")));

            Expense expense = new Expense(kind,
                    String.valueOf(totalAmount),
                    "",
                    "",
                    "");

            expenses.add(expense);
        }

        cursor.close();
        db.close();

        return expenses;
    }

    // --------------------------------------------------------------
    // Handling of current date for filtering
    //
    // HACK Make this more elegant, since this is an eyesore already.
    // PLDT 20:04 12-10-2023
    // --------------------------------------------------------------

    public String getTodayFormattedDate() {
        // Get the current date
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd",
                Locale.getDefault());

        // Format the date as "YYYY-MM-DD"
        return dateFormat.format(calendar.getTime());
    }

    //-------------------------
    //Handling of getting today's expenses
    //-------------------------

    public List<ExpenseDaily> getExpensesMadeToday() {
        List<ExpenseDaily> todayExpenses = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // ---------------------------------------------------------
        // Get today's date in the desired format, which is ISO 8601
        //
        // https://vi.wikipedia.org/wiki/ISO_8601
        // https://en.wikipedia.org/wiki/ISO_8601
        // ---------------------------------------------------------
        String todayDate = getTodayFormattedDate();

        String selection = "date = ?";
        String[] selectionArgs = { todayDate };

        Cursor cursor = db.query("expenses",
                null,
                selection,
                selectionArgs,
                null,
                null,
                null);

        while (cursor.moveToNext()) {
            String amount = cursor.getString(
                    Math.max(0,cursor.getColumnIndex("amount")));

            String kind = cursor.getString(
                    Math.max(0,cursor.getColumnIndex("kind")));

            String description = cursor.getString(
                    Math.max(0,cursor.getColumnIndex("description")));

            ExpenseDaily expenseDaily = new ExpenseDaily(amount, kind, description);
            todayExpenses.add(expenseDaily);
        }

        cursor.close();
        db.close();
        return todayExpenses;
    }

    //-------------------------------------------
    //Handling of getting sum of today's expenses
    //-------------------------------------------

    public double getTotalExpensesForToday() {
        double todayExpenses = 0.0;

        //Obtain database
        SQLiteDatabase db = this.getReadableDatabase();

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(new Date());

        //Create database query
        String query = "SELECT SUM(amount) FROM expenses WHERE date = ?";
        String[] selectionArgs = {currentDate};

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor.moveToFirst()) {
            // Assuming the total amount is in the first column
            todayExpenses = cursor.getDouble(0);
        }

        cursor.close();
        db.close();

        return todayExpenses;
    }

    //---------------------------------------------------
    //Handling of getting sum of current month's expenses
    //---------------------------------------------------

    public double getTotalExpensesForMonth() {
        double totalExpenses = 0.0;

        SQLiteDatabase db = this.getReadableDatabase();

        // Calculate the first day of the current month
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String currentDate = dateFormat.format(calendar.getTime());
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        String firstDayOfMonth = dateFormat.format(calendar.getTime());

        String query = "SELECT SUM(amount) FROM expenses WHERE date >= ? AND date <= ?";
        String[] selectionArgs = {firstDayOfMonth, currentDate};

        Cursor cursor = db.rawQuery(query, selectionArgs);

        if (cursor.moveToFirst()) {
            totalExpenses = cursor.getDouble(0);
        }

        cursor.close();
        db.close();

        return totalExpenses;
    }
}
