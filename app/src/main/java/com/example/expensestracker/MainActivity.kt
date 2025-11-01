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
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet
import com.example.expensestracker.ui.theme.AddSheetDialog
import com.example.expensestracker.ui.theme.GraphScreen
import com.example.expensestracker.ui.theme.SheetDetails
import com.example.expensestracker.ui.theme.SheetList

enum class Screen { Sheets, Details, Graph }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { ExpenseApp() } }
    }
}

@Composable
fun ExpenseApp() {
    val sheets = remember { mutableStateListOf<ExpenseSheet>() }
    val expensesMap = remember { mutableStateMapOf<Long, SnapshotStateList<Expense>>() }

    fun expensesOf(id: Long): SnapshotStateList<Expense> =
        expensesMap.getOrPut(id) { mutableStateListOf() }

    fun totalOf(id: Long): Double = expensesOf(id).sumOf { it.amount }

    var screen by remember { mutableStateOf(Screen.Sheets) }
    var selected by remember { mutableStateOf<ExpenseSheet?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    when (screen) {
        Screen.Sheets -> SheetList(
            sheets = sheets,
            onSheetClick = { s -> selected = s; screen = Screen.Details },
            onAddNewSheet = { showAddSheet = true },
            onOpenGraph = { screen = Screen.Graph }
        )
        Screen.Details -> {
            val s = selected ?: return
            SheetDetails(
                sheet = s,
                expenses = expensesOf(s.id),
                onBack = { screen = Screen.Sheets }
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
                val nextId = (sheets.maxOfOrNull { it.id } ?: 0L) + 1
                sheets += ExpenseSheet(id = nextId, month = month, year = year)
                showAddSheet = false
            }
        )
    }
}
