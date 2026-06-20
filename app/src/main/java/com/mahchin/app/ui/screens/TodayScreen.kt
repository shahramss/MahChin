package com.mahchin.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.components.JalaliDateDialog
import com.mahchin.app.ui.components.TaskCard
import com.mahchin.app.ui.components.TaskEditorDialog
import com.mahchin.app.ui.viewmodel.MainViewModel

@Composable
fun TodayScreen(vm: MainViewModel) {
    val tasks by vm.todayTasks.collectAsState()
    val settings by vm.settings.collectAsState()
    val today = vm.today
    val done = tasks.count { it.status.isClosed() }
    val remaining = tasks.size - done
    val progress = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size.toFloat()

    var addDialog by remember { mutableStateOf(false) }
    var editTask by remember { mutableStateOf<TaskItem?>(null) }
    var moveTask by remember { mutableStateOf<TaskItem?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 98.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "امروز",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        "${JalaliCalendar.weekdayName(today)}، ${today.display}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${done.toPersianDigits()} انجام‌شده",
                                modifier = Modifier.weight(1f),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "${remaining.toPersianDigits()} باقی‌مانده",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        Text(
                            "تیک را دوباره بزنی، برداشته می‌شود. گزینه‌های بیشتر با نگه‌داشتن روی تسک.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "یادآوری ${settings.reminderIntensity.fa} از ${settings.startHour.toPersianDigits()} تا ${settings.endHour.toPersianDigits()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f)
                        )
                    }
                }
            }

            if (remaining > 0) {
                item {
                    OutlinedButton(
                        onClick = vm::moveAllRemainingTodayToTomorrow,
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) { Text("انتقال همه کارهای باز به فردا") }
                }
            }

            if (tasks.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("امروز خلوت است", fontWeight = FontWeight.Bold)
                            Text("یک تسک اضافه کن یا برنامه ثابت را از قالب ماهانه بچین.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            FilledTonalButton(onClick = { addDialog = true }) { Text("افزودن اولین تسک") }
                        }
                    }
                }
            } else {
                items(tasks, key = { it.origin.name + it.id }) { task ->
                    TaskCard(
                        task = task,
                        onDone = { vm.toggleDone(task) },
                        onEdit = { editTask = task },
                        onDelete = { vm.deleteTask(task) },
                        onMoveTomorrow = { vm.moveToTomorrow(task) },
                        onMoveCustom = { moveTask = task },
                        onCancel = { vm.cancelToday(task) },
                        onInProgress = { vm.inProgress(task) },
                        onReset = { vm.resetStatus(task) }
                    )
                }
            }
        }

        Button(
            onClick = { addDialog = true },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 14.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(horizontal = 18.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(21.dp))
            Spacer(Modifier.size(8.dp))
            Text("افزودن تسک امروز", fontWeight = FontWeight.Bold)
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
