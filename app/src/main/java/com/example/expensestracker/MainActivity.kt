// MainActivity.kt
package com.example.expensestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.platform.LocalContext
import com.example.expensestracker.data.db.ExpensesDb
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet

enum class Screen { Sheets, Details, Graph }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ExpenseApp() } }
    }
}

@Composable
fun ExpenseApp() {

    val context = LocalContext.current
    val db = remember { ExpensesDb(context) }


    val sheets = remember { mutableStateListOf<ExpenseSheet>() }
    val expensesMap = remember { mutableStateMapOf<Long, SnapshotStateList<Expense>>() }

    fun expensesOf(id: Long): SnapshotStateList<Expense> =
        expensesMap.getOrPut(id) { mutableStateListOf() }

    fun totalOf(id: Long): Double = expensesOf(id).sumOf { it.amount }

    var screen by remember { mutableStateOf(Screen.Sheets) }
    var selected by remember { mutableStateOf<ExpenseSheet?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        sheets.clear()
        sheets.addAll(db.getSheets())
        // Silme vb. sonrası seçili kırık kalmasın
        selected = selected?.let { sel -> sheets.find { it.id == sel.id } }
    }


    LaunchedEffect(selected?.id) {
        val s = selected ?: return@LaunchedEffect
        expensesOf(s.id).apply {
            clear()
            addAll(db.getExpenses(s.id))
        }
    }


    when (screen) {
        Screen.Sheets -> SheetList(
            sheets = sheets,
            onSheetClick = { s ->
                selected = s
                screen = Screen.Details
            },
            onAddNewSheet = { showAddSheet = true },
            onOpenGraph = { screen = Screen.Graph },
            onEditSheet = { sheetId, newMonth, newYear ->
                db.updateSheetMonthYear(sheetId, newMonth, newYear)
                val idx = sheets.indexOfFirst { it.id == sheetId }
                if (idx >= 0) {
                    val old = sheets[idx]
                    sheets[idx] = old.copy(month = newMonth, year = newYear)
                }
            },
            onDeleteSheet = { sheetId ->
                db.deleteSheet(sheetId)
                sheets.removeAll { it.id == sheetId }
                expensesMap.remove(sheetId)
                if (selected?.id == sheetId) {
                    selected = null
                    screen = Screen.Sheets
                }
            }
        )

        Screen.Details -> {
            val s = selected ?: return
            SheetDetails(
                sheet = s,
                expenses = expensesOf(s.id),
                onBack = { screen = Screen.Sheets },
                onUpdateIncome = { income ->
                    s.income = income
                    db.updateSheetIncome(s.id, income)
                },
                onAddExpense = { title, amount, date ->
                    val newId = db.insertExpense(s.id, title, amount, date)
                    expensesOf(s.id).add(Expense(newId, s.id, title, amount, date))
                },
                onEditExpense = { id, title, amount, date ->
                    db.updateExpense(id, title, amount, date)
                    val list = expensesOf(s.id)
                    val idx = list.indexOfFirst { it.id == id }
                    if (idx >= 0) list[idx] = list[idx].copy(title = title, amount = amount, date = date)
                },
                onDeleteExpense = { id ->
                    db.deleteExpense(id)
                    expensesOf(s.id).removeAll { it.id == id }
                }
            )
        }

        Screen.Graph -> GraphScreen(
            sheets = sheets,
            totalOf = { id -> totalOf(id) },
            onBack = { screen = Screen.Sheets }
        )
    }


    if (showAddSheet) {
        AddSheetDialog(
            onCancel = { showAddSheet = false },
            onConfirm = { month, year ->
                val dbId = db.insertSheet(month, year, 0.0)
                val newSheet = ExpenseSheet(id = dbId, month = month, year = year)
                sheets += newSheet
                showAddSheet = false
            }
        )
    }
}
