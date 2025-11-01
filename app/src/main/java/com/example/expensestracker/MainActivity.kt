
package com.example.expensestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
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
            SheetDetails(sheet = s, onBack = { screen = Screen.Sheets })
        }
        Screen.Graph -> GraphScreen(sheets = sheets, onBack = { screen = Screen.Sheets })
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
