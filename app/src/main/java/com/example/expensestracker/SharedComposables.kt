// SharedComposables.kt
package com.example.expensestracker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensestracker.model.Expense
import com.example.expensestracker.model.ExpenseSheet
import com.example.expensestracker.model.monthName
import kotlin.math.abs
import kotlin.math.ceil
import java.util.Calendar

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
    val cal = remember { Calendar.getInstance() }
    var month by remember { mutableStateOf((cal.get(Calendar.MONTH) + 1).toString()) } // 1..12
    var year by remember { mutableStateOf(cal.get(Calendar.YEAR).toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Create New Sheet") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = month, onValueChange = { month = it }, label = { Text("Month (1–12)") }, singleLine = true)
                OutlinedTextField(value = year,  onValueChange = { year  = it }, label = { Text("Year") }, singleLine = true)
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
    expenses: SnapshotStateList<Expense>,
    onBack: () -> Unit,
    onUpdateIncome: (Double) -> Unit,
    onAddExpense: (String, Double) -> Unit,
    onEditExpense: (Long, String, Double) -> Unit,
    onDeleteExpense: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddExpense by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<Expense?>(null) }

    val total = expenses.sumOf { it.amount }
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
                Spacer(Modifier.width(48.dp))
            }

            var incomeText by remember(sheet.id) { mutableStateOf(if (sheet.income == 0.0) "" else sheet.income.toString()) }
            OutlinedTextField(
                value = incomeText,
                onValueChange = {
                    incomeText = it
                    val v = it.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onUpdateIncome(v)
                },
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
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(e.title)
                                Text("€%.2f".format(e.amount), fontWeight = FontWeight.SemiBold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { editTarget = e }) { Text("Edit") }
                                TextButton(onClick = { onDeleteExpense(e.id) }) { Text("Delete") }
                            }
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
                onAddExpense(title, amount)
                showAddExpense = false
            }
        )
    }
    if (editTarget != null) {
        EditExpenseDialog(
            initial = editTarget!!,
            onCancel = { editTarget = null },
            onConfirm = { id, title, amount ->
                onEditExpense(id, title, amount)
                editTarget = null
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
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (€)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.replace(',', '.').toDoubleOrNull()
                if (title.isBlank()) { error = "Title required"; return@TextButton }
                if (a == null || a <= 0) { error = "Enter amount > 0"; return@TextButton }
                onConfirm(title.trim(), a)
            }) { Text("Add") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}

@Composable
fun EditExpenseDialog(
    initial: Expense,
    onCancel: () -> Unit,
    onConfirm: (id: Long, title: String, amount: Double) -> Unit
) {
    var title by remember { mutableStateOf(initial.title) }
    var amount by remember { mutableStateOf(initial.amount.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Edit Expense") },
        text = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount (€)") }, singleLine = true)
                if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val a = amount.replace(',', '.').toDoubleOrNull()
                if (title.isBlank()) { error = "Title required"; return@TextButton }
                if (a == null || a <= 0) { error = "Enter amount > 0"; return@TextButton }
                onConfirm(initial.id, title.trim(), a)
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onCancel) { Text("Cancel") } }
    )
}

@Composable
fun GraphScreen(
    sheets: List<ExpenseSheet>,
    totalOf: (Long) -> Double,
    onBack: () -> Unit
) {
    val sorted = remember(sheets) { sheets.sortedWith(compareBy({ it.year }, { it.month })) }
    var start by remember(sorted) { mutableStateOf((sorted.size - 4).coerceAtLeast(0)) }
    var acc by remember { mutableStateOf(0f) }
    val thresholdPx = 40f

    val window = sorted.drop(start).take(4)
    val labels = window.map { "${monthName(it.month).take(3)} ${it.year % 100}" }
    val incomes = window.map { it.income }
    val expenses = window.map { totalOf(it.id) }

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
                Spacer(Modifier.width(48.dp))
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(sorted.size) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                val dx = dragAmount.x
                                acc += dx
                                if (acc <= -thresholdPx) {
                                    if (start + 4 < sorted.size) start += 1
                                    acc = 0f
                                } else if (acc >= thresholdPx) {
                                    if (start > 0) start -= 1
                                    acc = 0f
                                }
                                change.consume()
                            }
                        )
                    }
            ) {
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

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                labels.forEach { Text(it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(14.dp)) { drawRect(Color(0xFF1E88E5)) }
                    Spacer(Modifier.width(6.dp)); Text("Income")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Canvas(Modifier.size(14.dp)) { drawRect(Color(0xFFE53935)) }
                    Spacer(Modifier.width(6.dp)); Text("Expenses")
                }
            }
        }
    }
}
