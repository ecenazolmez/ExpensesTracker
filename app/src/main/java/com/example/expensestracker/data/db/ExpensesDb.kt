// ExpensesDb.kt
package com.example.expensestracker.data.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet

class ExpensesDb(context: Context) :
    SQLiteOpenHelper(context, "expenses.db", null, 2) { // v2: date sütunu

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE sheets (
                id     INTEGER PRIMARY KEY AUTOINCREMENT,
                month  INTEGER NOT NULL,
                year   INTEGER NOT NULL,
                income REAL    NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE expenses (
                id      INTEGER PRIMARY KEY AUTOINCREMENT,
                sheetId INTEGER NOT NULL,
                title   TEXT    NOT NULL,
                amount  REAL    NOT NULL,
                date    TEXT    NOT NULL DEFAULT (date('now')),
                FOREIGN KEY(sheetId) REFERENCES sheets(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_sheetId ON expenses(sheetId)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE expenses ADD COLUMN date TEXT NOT NULL DEFAULT (date('now'))")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_sheetId ON expenses(sheetId)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_date ON expenses(date)")
        }
    }

    // ------ Sheets ------
    fun insertSheet(month: Int, year: Int, income: Double = 0.0): Long {
        val cv = ContentValues().apply {
            put("month", month)
            put("year", year)
            put("income", income)
        }
        return writableDatabase.insert("sheets", null, cv)
    }

    fun getSheets(): List<ExpenseSheet> {
        val out = mutableListOf<ExpenseSheet>()
        readableDatabase.query(
            "sheets",
            arrayOf("id", "month", "year", "income"),
            null, null, null, null,
            "year ASC, month ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out += ExpenseSheet(
                    id = c.getLong(0),
                    month = c.getInt(1),
                    year = c.getInt(2),
                    income = c.getDouble(3)
                )
            }
        }
        return out
    }

    fun updateSheetIncome(id: Long, income: Double) {
        val cv = ContentValues().apply { put("income", income) }
        writableDatabase.update("sheets", cv, "id=?", arrayOf(id.toString()))
    }

    fun updateSheetMonthYear(id: Long, month: Int, year: Int) {
        val cv = ContentValues().apply {
            put("month", month)
            put("year", year)
        }
        writableDatabase.update("sheets", cv, "id=?", arrayOf(id.toString()))
    }

    fun deleteSheet(id: Long) {
        writableDatabase.delete("sheets", "id=?", arrayOf(id.toString()))
    }

    // ------ Expenses ------
    fun insertExpense(sheetId: Long, title: String, amount: Double, date: String = today()): Long {
        val cv = ContentValues().apply {
            put("sheetId", sheetId)
            put("title", title)
            put("amount", amount)
            put("date", date)
        }
        return writableDatabase.insert("expenses", null, cv)
    }

    fun getExpenses(sheetId: Long): List<Expense> {
        val out = mutableListOf<Expense>()
        readableDatabase.query(
            "expenses",
            arrayOf("id", "sheetId", "title", "amount", "date"),
            "sheetId=?",
            arrayOf(sheetId.toString()),
            null, null,
            "date ASC, id ASC"
        ).use { c ->
            while (c.moveToNext()) {
                out += Expense(
                    id = c.getLong(0),
                    sheetId = c.getLong(1),
                    title = c.getString(2),
                    amount = c.getDouble(3),
                    date = c.getString(4)
                )
            }
        }
        return out
    }

    fun updateExpense(id: Long, title: String, amount: Double, date: String) {
        val cv = ContentValues().apply {
            put("title", title)
            put("amount", amount)
            put("date", date)
        }
        writableDatabase.update("expenses", cv, "id=?", arrayOf(id.toString()))
    }

    fun deleteExpense(id: Long) {
        writableDatabase.delete("expenses", "id=?", arrayOf(id.toString()))
    }

    fun sumExpenses(sheetId: Long): Double {
        readableDatabase.rawQuery(
            "SELECT IFNULL(SUM(amount),0) FROM expenses WHERE sheetId=?",
            arrayOf(sheetId.toString())
        ).use { c ->
            return if (c.moveToFirst()) c.getDouble(0) else 0.0
        }
    }

    private fun today(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            .format(java.util.Date())
}
