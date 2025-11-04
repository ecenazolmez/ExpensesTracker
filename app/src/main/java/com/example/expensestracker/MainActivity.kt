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
    // --- DB ---
    val context = LocalContext.current
    val db = remember { ExpensesDb(context) }

    // --- UI State ---
    val sheets = remember { mutableStateListOf<ExpenseSheet>() }
    val expensesMap = remember { mutableStateMapOf<Long, SnapshotStateList<Expense>>() }

    fun expensesOf(id: Long): SnapshotStateList<Expense> =
        expensesMap.getOrPut(id) { mutableStateListOf() }

    fun totalOf(id: Long): Double = expensesOf(id).sumOf { it.amount }

    var screen by remember { mutableStateOf(Screen.Sheets) }
    var selected by remember { mutableStateOf<ExpenseSheet?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    // --- Açılışta tüm sheet'leri DB'den yükle ---
    LaunchedEffect(Unit) {
        sheets.clear()
        sheets.addAll(db.getSheets())
    }

    // --- Seçili sheet değişince giderleri DB'den yükle ---
    LaunchedEffect(selected?.id) {
        val s = selected ?: return@LaunchedEffect
        expensesOf(s.id).apply {
            clear()
            addAll(db.getExpenses(s.id))
        }
    }

    // --- Ekranlar ---
    when (screen) {
        Screen.Sheets -> SheetList(
            sheets = sheets,
            onSheetClick = { s ->
                selected = s
                screen = Screen.Details
            },
            onAddNewSheet = { showAddSheet = true },
            onOpenGraph = { screen = Screen.Graph }
        )

        Screen.Details -> {
            val s = selected ?: return
            SheetDetails(
                sheet = s,
                expenses = expensesOf(s.id),
                onBack = { screen = Screen.Sheets },
                // Aşağıdaki 4 callback'i SharedComposables.kt'deki SheetDetails imzanla eşleştir.
                onUpdateIncome = { income ->
                    s.income = income
                    db.updateSheetIncome(s.id, income)
                },
                onAddExpense = { title, amount ->
                    val newId = db.insertExpense(s.id, title, amount)
                    expensesOf(s.id).add(Expense(newId, s.id, title, amount))
                },
                onEditExpense = { id, title, amount ->
                    db.updateExpense(id, title, amount)
                    val list = expensesOf(s.id)
                    val idx = list.indexOfFirst { it.id == id }
                    if (idx >= 0) list[idx] = list[idx].copy(title = title, amount = amount)
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
