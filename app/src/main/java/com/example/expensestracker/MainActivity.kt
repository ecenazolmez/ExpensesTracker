package com.example.expensestracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet
import com.example.expensestracker.model.monthName
import com.example.expensestracker.ui.theme.SheetList

enum class Screen { Sheets, Details }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ExpenseApp()
            }
        }
    }
}

@Composable
fun ExpenseApp() {
    val sheets = remember { mutableStateListOf<ExpenseSheet>() }
    var screen by remember { mutableStateOf(Screen.Sheets) }
    var selected by remember { mutableStateOf<ExpenseSheet?>(null) }

    LaunchedEffect(Unit) {
        if (sheets.isEmpty()) {
            sheets += ExpenseSheet(1, 10, 2025)
            sheets += ExpenseSheet(2, 11, 2025)
        }
    }

    when (screen) {
        Screen.Sheets -> SheetList(
            sheets = sheets,
            onSheetClick = { s ->
                selected = s
                screen = Screen.Details
            }
        )

        Screen.Details -> SheetDetails(
            sheet = selected!!,
            onBack = { screen = Screen.Sheets }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SheetDetails(sheet: ExpenseSheet, onBack: () -> Unit) {
    val expenses = remember(sheet.id) { mutableStateListOf<Expense>() }
    var incomeText by remember(sheet.id) { mutableStateOf(if (sheet.income == 0.0) "" else sheet.income.toString()) }
    var newTitle by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }

    val total = expenses.sumOf { it.amount }
    val remaining = sheet.income - total

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expenses • ${monthName(sheet.month)} ${sheet.year}") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = incomeText,
                onValueChange = {
                    incomeText = it
                    sheet.income = it.toDoubleOrNull() ?: 0.0
                },
                label = { Text("Monthly Income (€)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Expenses: €%.2f".format(total))
                Text(
                    if (remaining >= 0) "Remaining: €%.2f".format(remaining)
                    else "Deficit: €%.2f".format(-remaining),
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { e ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(e.title)
                            Text("€%.2f".format(e.amount), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = newAmount,
                    onValueChange = { newAmount = it },
                    label = { Text("Amount (€)") },
                    singleLine = true,
                    modifier = Modifier.width(150.dp)
                )
            }

            Button(
                onClick = {
                    val a = newAmount.toDoubleOrNull() ?: return@Button
                    if (newTitle.isBlank()) return@Button
                    val nextId = (expenses.maxOfOrNull { it.id } ?: 0L) + 1
                    expenses += Expense(nextId, sheet.id, newTitle.trim(), a)
                    newTitle = ""
                    newAmount = ""
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Add Expense")
            }
        }
    }
}
