package com.mahchin.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
        TaskStatus.NOT_STARTED -> MaterialTheme.colorScheme.onSurfaceVariant
        TaskStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
        TaskStatus.DONE -> Color(0xFF20E0D2)
        TaskStatus.MOVED_TO_TOMORROW, TaskStatus.MOVED_TO_CUSTOM_DATE -> MaterialTheme.colorScheme.secondary
        TaskStatus.CANCELED -> Color(0xFFFF6B6B)
    }
    val priorityText = when (task.priority) {
        TaskPriority.NORMAL -> "عادی"
        TaskPriority.IMPORTANT -> "مهم"
        TaskPriority.URGENT -> "فوری"
    }
    val closed = task.status.isClosed()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    StatusDot(statusColor)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(
                            task.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                AssistChip(onClick = {}, label = { Text(priorityText) })
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = {}, label = { Text(task.status.fa) })
                AssistChip(onClick = {}, label = { Text(task.taskType.fa) })
            }

            if (task.movedFromDate != null || task.movedToDate != null) {
                Text(
                    text = listOfNotNull(
                        task.movedFromDate?.let { "از: $it" },
                        task.movedToDate?.let { "به: $it" }
                    ).joinToString("  |  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onDone,
                    enabled = !closed,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("انجام شد")
                }
                OutlinedButton(onClick = onMoveTomorrow, enabled = !closed, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("فردا")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onInProgress, enabled = task.status == TaskStatus.NOT_STARTED, modifier = Modifier.weight(1f)) { Text("در حال انجام") }
                TextButton(onClick = onMoveCustom, enabled = !closed, modifier = Modifier.weight(1f)) { Text("تاریخ دلخواه") }
                TextButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("ویرایش")
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = onCancel, enabled = !closed, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("لغو امروز")
                }
                TextButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف") }
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(12.dp)
            .background(color, CircleShape)
    )
}
