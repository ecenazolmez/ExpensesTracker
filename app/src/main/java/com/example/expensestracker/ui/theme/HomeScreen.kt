package com.example.expensestracker.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet
import com.example.expensestracker.model.monthName

// ---------------- SHEET LIST ----------------
@Composable
fun SheetList(
    sheets: List<ExpenseSheet>,
    onSheetClick: (ExpenseSheet) -> Unit,
    onAddNewSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = onAddNewSheet) { Text("+") }
        }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp)
        ) {
            Text("Expense Sheets", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))

            if (sheets.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No sheets yet. Tap + to add one.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sheets, key = { it.id }) { s ->
                        ElevatedCard(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSheetClick(s) }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text(
                                    "${monthName(s.month)} ${s.year}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text("Tap to open", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------- NEW SHEET DIALOG --------------
@Composable
fun AddSheetDialog(
    onCancel: () -> Unit,
    onConfirm: (month: Int, year: Int) -> Unit
) {
    var month by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Create New Sheet") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (1–12)") }, singleLine = true)
                OutlinedTextField(value = year,  onValueChange = { year  = it }, label = { Text("Year (e.g., 2025)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val m = month.toIntOrNull()
                val y = year.toIntOrNull()
                if (m == null || m !in 1..12) { error = "Enter month 1–12"; return@TextButton }
                if (y == null || y < 1900)    { error = "Enter valid year"; return@TextButton }
                onConfirm(m, y)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}

// -------------- SHEET DETAILS ----------------
@Composable
fun SheetDetails(
    sheet: ExpenseSheet,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenses = remember(sheet.id) { mutableStateListOf<Expense>() }
    var showAddExpense by remember { mutableStateOf(false) }

    val total = expenses.sumOf { it.amount }
    val remaining = sheet.income - total

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddExpense = true }) { Text("+") }
        }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // simple header with back action
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Expenses • ${monthName(sheet.month)} ${sheet.year}",
                    fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(48.dp))
            }

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
                Text(
                    if (remaining >= 0) "Remaining: €%.2f".format(remaining)
                    else "Deficit: €%.2f".format(kotlin.math.abs(remaining)),
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses, key = { it.id }) { e ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(e.title)
                            Text("€%.2f".format(e.amount), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                if (expenses.isEmpty()) item { Text("No expenses yet.") }
            }
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

// -------------- ADD EXPENSE DIALOG --------------
@Composable
fun AddExpenseDialog(
    onCancel: () -> Unit,
    onConfirm: (title: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Expense") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title,   onValueChange = { title = it },  label = { Text("Title") },      singleLine = true)
                OutlinedTextField(value = amount,  onValueChange = { amount = it }, label = { Text("Amount (€)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.toDoubleOrNull()
                if (title.isBlank()) { error = "Title required"; return@TextButton }
                if (a == null || a <= 0) { error = "Enter amount > 0"; return@TextButton }
                onConfirm(title.trim(), a)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}
