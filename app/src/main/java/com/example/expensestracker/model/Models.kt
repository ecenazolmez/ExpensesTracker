
package com.example.expensestracker.model

data class ExpenseSheet(
    val id: Long,
    val month: Int,
    val year: Int,
    var income: Double = 0.0,
    var totalExpense: Double = 0.0
)

data class Expense(
    val id: Long,
    val sheetId: Long,
    val title: String,
    val amount: Double
)

fun monthName(m: Int): String {
    val names = listOf(
        "", "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )
    return names[m.coerceIn(0, 12)]
}
