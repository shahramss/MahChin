package com.mahchin.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.data.model.TaskStatus

@Composable
fun TaskCard(
    task: TaskItem,
    onDone: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveTomorrow: () -> Unit,
    onMoveCustom: () -> Unit,
    onCancel: () -> Unit,
    onInProgress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (task.status) {
        TaskStatus.NOT_STARTED -> Color(0xFF64748B)
        TaskStatus.IN_PROGRESS -> Color(0xFFF59E0B)
        TaskStatus.DONE -> Color(0xFF16A34A)
        TaskStatus.MOVED_TO_TOMORROW, TaskStatus.MOVED_TO_CUSTOM_DATE -> Color(0xFF2563EB)
        TaskStatus.CANCELED -> Color(0xFFDC2626)
    }
    val priorityColor = when (task.priority) {
        TaskPriority.NORMAL -> Color(0xFF64748B)
        TaskPriority.IMPORTANT -> Color(0xFFEAB308)
        TaskPriority.URGENT -> Color(0xFFDC2626)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                StatusDot(statusColor)
            }
            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(task.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistChip(onClick = {}, label = { Text(task.status.fa) })
                AssistChip(onClick = {}, label = { Text(task.priority.fa) }, leadingIcon = { StatusDot(priorityColor) })
                AssistChip(onClick = {}, label = { Text(task.taskType.fa) })
            }
            if (task.movedFromDate != null || task.movedToDate != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = listOfNotNull(
                        task.movedFromDate?.let { "از: $it" },
                        task.movedToDate?.let { "به: $it" }
                    ).joinToString("  |  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDone, enabled = !task.status.isClosed(), modifier = Modifier.weight(1f)) { Text("انجام شد") }
                OutlinedButton(onClick = onInProgress, enabled = task.status == TaskStatus.NOT_STARTED, modifier = Modifier.weight(1f)) { Text("در حال انجام") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onMoveTomorrow, enabled = !task.status.isClosed(), modifier = Modifier.weight(1f)) { Text("موکول به فردا") }
                OutlinedButton(onClick = onMoveCustom, enabled = !task.status.isClosed(), modifier = Modifier.weight(1f)) { Text("تاریخ دلخواه") }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("ویرایش") }
                OutlinedButton(onClick = onCancel, enabled = !task.status.isClosed(), modifier = Modifier.weight(1f)) { Text("لغو برای امروز") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Spacer(
        modifier = Modifier
            .width(10.dp)
            .height(10.dp)
            .background(color, RoundedCornerShape(50))
    )
}
