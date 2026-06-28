package com.mahchin.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskStatus
import com.mahchin.app.domain.toPersianDigits

@Composable
fun MindMapAwareTaskList(
    tasks: List<TaskItem>,
    mindMapNodes: List<MindMapNode>,
    onSetGroupStatus: (List<TaskItem>, TaskStatus) -> Unit,
    onDone: (TaskItem) -> Unit,
    onEdit: (TaskItem) -> Unit,
    onDelete: (TaskItem) -> Unit,
    onMoveTomorrow: (TaskItem) -> Unit,
    onMoveCustom: (TaskItem) -> Unit,
    onCancel: (TaskItem) -> Unit,
    onInProgress: (TaskItem) -> Unit,
    onReset: (TaskItem) -> Unit,
    onSetAlarm: (TaskItem) -> Unit
) {
    val nodeMap = remember(mindMapNodes) { mindMapNodes.filter { it.isActive }.associateBy { it.id } }
    val expandableState = remember { mutableStateMapOf<String, Boolean>() }

    val mindTasks = tasks
        .filter { it.sourceMindMapNodeId != null && nodeMap.containsKey(it.sourceMindMapNodeId) }
        .sortedWith(compareBy({ it.projectName ?: "" }, { it.mindMapPath ?: "" }, { it.createdAt }))
    val normalTasks = tasks.filterNot { it.sourceMindMapNodeId != null && nodeMap.containsKey(it.sourceMindMapNodeId) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        normalTasks.forEach { task ->
            TaskCard(
                task = task,
                onDone = { onDone(task) },
                onEdit = { onEdit(task) },
                onDelete = { onDelete(task) },
                onMoveTomorrow = { onMoveTomorrow(task) },
                onMoveCustom = { onMoveCustom(task) },
                onCancel = { onCancel(task) },
                onInProgress = { onInProgress(task) },
                onReset = { onReset(task) },
                onSetAlarm = { onSetAlarm(task) }
            )
        }

        mindTasks.groupBy { it.projectId ?: -1L }.forEach { (projectId, projectTasks) ->
            val projectName = projectTasks.firstOrNull()?.projectName ?: "بدون پروژه"
            val key = "project_$projectId"
            val expanded = expandableState[key] ?: true
            ProjectAccordionCard(
                title = projectName,
                subtitle = "${projectTasks.size.toPersianDigits()} تسک از مایندمپ",
                tasks = projectTasks,
                expanded = expanded,
                onToggleExpand = { expandableState[key] = !expanded },
                onToggleDone = { targetStatus -> onSetGroupStatus(projectTasks, targetStatus) }
            )
            if (expanded) {
                projectTasks.forEach { task ->
                    TaskCard(
                        task = task,
                        onDone = { onDone(task) },
                        onEdit = { onEdit(task) },
                        onDelete = { onDelete(task) },
                        onMoveTomorrow = { onMoveTomorrow(task) },
                        onMoveCustom = { onMoveCustom(task) },
                        onCancel = { onCancel(task) },
                        onInProgress = { onInProgress(task) },
                        onReset = { onReset(task) },
                        onSetAlarm = { onSetAlarm(task) },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProjectAccordionCard(
    title: String,
    subtitle: String,
    tasks: List<TaskItem>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    onToggleDone: (TaskStatus) -> Unit
) {
    val allDone = tasks.isNotEmpty() && tasks.all { it.status == TaskStatus.DONE }
    val anyDone = tasks.any { it.status == TaskStatus.DONE }
    val check = when {
        allDone -> "✓"
        anyDone -> "◐"
        else -> "○"
    }
    val target = if (allDone) TaskStatus.NOT_STARTED else TaskStatus.DONE

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.32f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = check,
                modifier = Modifier
                    .clickable(enabled = tasks.isNotEmpty()) { onToggleDone(target) }
                    .padding(horizontal = 4.dp),
                color = if (allDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onToggleExpand),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title.toPersianDigits(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = subtitle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Text(
                text = if (expanded) "⌄" else "›",
                modifier = Modifier.clickable(onClick = onToggleExpand),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}
