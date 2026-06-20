package com.mahchin.app.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.components.JalaliDateDialog
import com.mahchin.app.ui.components.TaskCard
import com.mahchin.app.ui.components.TaskEditorDialog
import com.mahchin.app.ui.viewmodel.MainViewModel

@Composable
fun CalendarScreen(vm: MainViewModel) {
    val month by vm.calendarMonth.collectAsState()
    val counts by vm.monthCounts.collectAsState()
    val selectedDate by vm.selectedDate.collectAsState()
    val selectedTasks by vm.selectedDateTasks.collectAsState()

    var addDialog by remember { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var moveTask by remember { mutableStateOf<TaskItem?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = vm::nextMonth, enabled = !(month.year == 1500 && month.month == 12)) { Text("بعدی") }
            Text("${JalaliCalendar.monthName(month.month)} ${month.year.toPersianDigits()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = vm::previousMonth, enabled = !(month.year == 1405 && month.month == 1)) { Text("قبلی") }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("شنبه", "یک", "دو", "سه", "چهار", "پنج", "جمعه").forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        Spacer(Modifier.height(4.dp))
        MonthGrid(month.year, month.month, selectedDate, counts, onSelect = vm::selectDate)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("برنامه ${selectedDate.display}", fontWeight = FontWeight.Bold)
                AssistChip(onClick = {}, label = { Text("${selectedTasks.size.toPersianDigits()} تسک") })
            }
            Button(onClick = { addDialog = true }) { Text("تسک اختصاصی") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (selectedTasks.isEmpty()) item { Text("برای این تاریخ کاری وجود ندارد.") }
            items(selectedTasks, key = { it.origin.name + it.id }) { task ->
                TaskCard(
                    task = task,
                    onDone = { vm.complete(task) },
                    onEdit = { editTask = task },
                    onDelete = { vm.deleteTask(task) },
                    onMoveTomorrow = { vm.moveToTomorrow(task) },
                    onMoveCustom = { moveTask = task },
                    onCancel = { vm.cancelToday(task) },
                    onInProgress = { vm.inProgress(task) }
                )
            }
        }
    }

    if (addDialog) {
        TaskEditorDialog(
            titleText = "تسک اختصاصی برای ${selectedDate.display}",
            onDismiss = { addDialog = false },
            onSave = { title, desc, _, priority ->
                vm.addOneTimeTask(selectedDate, title, desc, priority)
                addDialog = false
            }
        )
    }

    editTask?.let { task ->
        TaskEditorDialog(
            titleText = "ویرایش فقط همین تاریخ",
            initialTitle = task.title,
            initialDescription = task.description,
            initialPriority = task.priority,
            onDismiss = { editTask = null },
            onSave = { title, desc, _, priority ->
                vm.editOnlyThisDate(task, title, desc, priority)
                editTask = null
            }
        )
    }

    moveTask?.let { task ->
        JalaliDateDialog(
            initialDate = selectedDate.plusDays(1),
            onDismiss = { moveTask = null },
            onSave = { date -> vm.moveToCustomDate(task, date); moveTask = null }
        )
    }
}

@Composable
private fun MonthGrid(
    year: Int,
    month: Int,
    selectedDate: JalaliDate,
    counts: Map<Int, Int>,
    onSelect: (JalaliDate) -> Unit
) {
    val offset = JalaliCalendar.firstDayOffsetSaturdayBased(year, month)
    val len = JalaliCalendar.monthLength(year, month)
    val cells = List(offset) { 0 } + (1..len).toList()
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.fillMaxWidth().height(285.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(cells) { day ->
            if (day == 0) {
                Box(Modifier.aspectRatio(1f))
            } else {
                val isSelected = selectedDate.year == year && selectedDate.month == month && selectedDate.day == day
                val count = counts[day] ?: 0
                Card(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clickable { onSelect(JalaliDate(year, month, day)) },
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(day.toPersianDigits(), fontWeight = FontWeight.Bold)
                            if (count > 0) Text("${count.toPersianDigits()} کار", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
