package com.mahchin.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.components.JalaliDateDialog
import com.mahchin.app.ui.components.TaskCard
import com.mahchin.app.ui.components.TaskEditorDialog
import com.mahchin.app.ui.viewmodel.MainViewModel

@Composable
fun TodayScreen(vm: MainViewModel) {
    val tasks by vm.todayTasks.collectAsState()
    val today = vm.today
    val done = tasks.count { it.status.isClosed() }
    val remaining = tasks.size - done
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size.toFloat()

    var addDialog by remember { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var moveTask by remember { mutableStateOf<TaskItem?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("ماه‌چین", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("${JalaliCalendar.weekdayName(today)}، ${today.display}", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("پیشرفت امروز", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                Text("${done.toPersianDigits()} انجام/بسته شده، ${remaining.toPersianDigits()} باقی‌مانده از ${tasks.size.toPersianDigits()} تسک")
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { addDialog = true }, modifier = Modifier.weight(1f)) { Text("افزودن تسک امروز") }
            OutlinedButton(onClick = vm::moveAllRemainingTodayToTomorrow, modifier = Modifier.weight(1f), enabled = remaining > 0) { Text("انتقال باقی‌مانده‌ها") }
        }
        Spacer(Modifier.height(12.dp))
        if (tasks.isEmpty()) {
            Text("برای امروز کاری ثبت نشده. از قالب ماهانه یا دکمه افزودن تسک امروز استفاده کن.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(tasks, key = { it.origin.name + it.id }) { task ->
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
    }

    if (addDialog) {
        TaskEditorDialog(
            titleText = "تسک اختصاصی امروز",
            onDismiss = { addDialog = false },
            onSave = { title, desc, _, priority ->
                vm.addTodayTask(title, desc, priority)
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
            initialDate = today.plusDays(1),
            onDismiss = { moveTask = null },
            onSave = { date ->
                vm.moveToCustomDate(task, date)
                moveTask = null
            }
        )
    }
}
