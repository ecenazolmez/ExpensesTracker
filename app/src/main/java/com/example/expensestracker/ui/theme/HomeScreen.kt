// HomeScreen.kt
package com.example.expensestracker.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet
import com.example.expensestracker.model.monthName
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun SheetList(
    sheets: List<ExpenseSheet>,
    onSheetClick: (ExpenseSheet) -> Unit,
    onAddNewSheet: () -> Unit,
    onOpenGraph: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = onAddNewSheet) { Text("+") } }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Expense Sheets", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onOpenGraph) { Text("Graph") }
            }

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

@Composable
fun SheetDetails(
    sheet: ExpenseSheet,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val expenses = remember(sheet.id) { mutableStateListOf<Expense>() }
    var showAddExpense by remember { mutableStateOf(false) }

    val total = expenses.sumOf { it.amount }
    sheet.totalExpense = total
    val remaining = sheet.income - total

    Scaffold(
        floatingActionButton = { FloatingActionButton(onClick = { showAddExpense = true }) { Text("+") } }
    ) { pad ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(pad)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text(
                    "Expenses • ${monthName(sheet.month)} ${sheet.year}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(0.dp))
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
                    else "Deficit: €%.2f".format(abs(remaining)),
                    fontWeight = FontWeight.SemiBold
                )
            }

            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(expenses, key = { it.id }) { e ->
                    ElevatedCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
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

@Composable
fun GraphScreen(sheets: List<ExpenseSheet>, onBack: () -> Unit) {
    val recent = sheets.sortedWith(compareBy({ it.year }, { it.month })).takeLast(4)
    val labels = recent.map { "${monthName(it.month).take(3)} ${it.year % 100}" }
    val incomes = recent.map { it.income }
    val expenses = recent.map { it.totalExpense }

    Scaffold { pad ->
        Column(
            modifier = Modifier
                .padding(pad)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) { Text("Back") }
                Text("Income/Expenses", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(0.dp))
            }

            Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                Canvas(Modifier.fillMaxSize().padding(16.dp)) {
                    val x0 = 40f
                    val bottom = size.height - 32f
                    val right = size.width - 8f
                    val top = 8f

                    drawLine(Color.Gray, Offset(x0, bottom), Offset(right, bottom), strokeWidth = 2f)
                    drawLine(Color.Gray, Offset(x0, bottom), Offset(x0, top), strokeWidth = 2f)

                    val all = (incomes + expenses)
                    val maxY = (all.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
                    val niceMax = (ceil(maxY / 100.0) * 100.0).toFloat().coerceAtLeast(100f)

                    val n = labels.size.coerceAtLeast(1)
                    val dx = if (n == 1) 0f else (right - x0) / (n - 1)
                    val xs = (0 until n).map { i -> x0 + i * dx }
                    fun mapY(v: Double): Float = bottom - ((v.toFloat() / niceMax) * (bottom - top))

                    val incomePath = Path()
                    val expensePath = Path()
                    labels.forEachIndexed { i, _ ->
                        val x = xs[i]
                        val yi = mapY(incomes.getOrElse(i) { 0.0 })
                        val ye = mapY(expenses.getOrElse(i) { 0.0 })
                        if (i == 0) {
                            incomePath.moveTo(x, yi)
                            expensePath.moveTo(x, ye)
                        } else {
                            incomePath.lineTo(x, yi)
                            expensePath.lineTo(x, ye)
                        }
                    }

                    drawPath(incomePath, color = Color(0xFF1E88E5), style = Stroke(width = 4f, cap = StrokeCap.Round))
                    drawPath(expensePath, color = Color(0xFFE53935), style = Stroke(width = 4f, cap = StrokeCap.Round))
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                labels.forEach { Text(it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.height(14.dp).fillMaxWidth(0f)) { drawRect(Color(0xFF1E88E5)) }
                    Spacer(Modifier.height(0.dp)); Text("Income")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.height(14.dp).fillMaxWidth(0f)) { drawRect(Color(0xFFE53935)) }
                    Spacer(Modifier.height(0.dp)); Text("Expenses")
                }
            }
        }
    }
}
