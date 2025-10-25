package com.example.expensestracker.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensestracker.model.*

@Composable
fun SheetList(
    sheets: List<ExpenseSheet>,
    onSheetClick: (ExpenseSheet) -> Unit,
    modifier: Modifier = Modifier
) {
    if (sheets.isEmpty()) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No sheets yet. Tap + to add one.")
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(sheets) { s ->
                ElevatedCard(
                    Modifier.fillMaxWidth().clickable { onSheetClick(s) }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${monthName(s.month)} ${s.year}",
                            fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                        Text("Tap to open", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun AddSheetDialog(
    onCancel: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var month by remember { mutableStateOf("") }
    var year  by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Create New Sheet") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(month, { month = it }, label = { Text("Month (1–12)") }, singleLine = true)
                OutlinedTextField(year,  { year  = it }, label = { Text("Year (e.g., 2025)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton({
                val m = month.toIntOrNull()
                val y = year.toIntOrNull()
                if (m == null || m !in 1..12) { error = "Enter month 1..12"; return@TextButton }
                if (y == null || y < 1900)     { error = "Enter valid year"; return@TextButton }
                onConfirm(m, y)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}

@Composable
fun SheetDetails(sheet: ExpenseSheet, modifier: Modifier = Modifier) {
    val expenses = remember(sheet.id) { mutableStateListOf<Expense>() }
    var showAddExpense by remember { mutableStateOf(false) }

    val total = expenses.sumOf { it.amount }
    val surplus = sheet.income - total

    Column(
        modifier = modifier.fillMaxSize().padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Expenses for ${monthName(sheet.month)} ${sheet.year}", fontWeight = FontWeight.SemiBold)

        var incomeText by remember(sheet.id) { mutableStateOf(if (sheet.income == 0.0) "" else sheet.income.toString()) }
        OutlinedTextField(
            value = incomeText,
            onValueChange = { incomeText = it; sheet.income = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("Monthly Income (€)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Expenses: €%.2f".format(total))
            Text(if (surplus >= 0) "Remaining: €%.2f".format(surplus) else "Deficit: €%.2f".format(surplus),
                fontWeight = FontWeight.SemiBold)
        }

        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(expenses) { e ->
                ElevatedCard(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(e.title)
                        Text("€%.2f".format(e.amount), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Button(onClick = { showAddExpense = true }, modifier = Modifier.align(Alignment.End)) {
            Text("Add Expense")
        }
    }

    if (showAddExpense) {
        AddExpenseDialog(
            onCancel = { showAddExpense = false },
            onConfirm = { title, amount ->
                val newId = (expenses.maxOfOrNull { it.id } ?: 0L) + 1
                expenses += Expense(newId, sheet.id, title, amount)
                showAddExpense = false
            }
        )
    }
}

@Composable
fun AddExpenseDialog(onCancel: () -> Unit, onConfirm: (title: String, amount: Double) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error  by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Expense") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(amount, { amount = it }, label = { Text("Amount (€)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton({
                val a = amount.toDoubleOrNull()
                if (title.isBlank()) { error = "Title required"; return@TextButton }
                if (a == null || a < 0) { error = "Enter valid amount"; return@TextButton }
                onConfirm(title.trim(), a)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}


