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
import androidx.compose.ui.text.style.TextOverflow
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
    val childrenByParent = remember(mindMapNodes) { mindMapNodes.filter { it.isActive }.groupBy { it.parentId } }
    val expandableState = remember { mutableStateMapOf<String, Boolean>() }

    val mindTasks = tasks.filter { it.sourceMindMapNodeId != null && nodeMap.containsKey(it.sourceMindMapNodeId) }
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
        BranchHeaderCard(
            title = projectName,
            subtitle = "${projectTasks.size.toPersianDigits()} کار از مایندمپ",
            tasks = projectTasks,
            expanded = expanded,
            level = 0,
            onToggleExpand = { expandableState[key] = !expanded },
            onToggleDone = { targetStatus -> onSetGroupStatus(projectTasks, targetStatus) }
        )
        if (expanded) {
            val relevantIds = collectRelevantNodeIds(projectTasks, nodeMap)
            val roots = relevantIds.mapNotNull { nodeMap[it] }
                .filter { node -> node.parentId == null || node.parentId !in relevantIds }
                .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
            roots.forEach { root ->
                MindMapNodeTaskBranch(
                    node = root,
                    level = 1,
                    relevantIds = relevantIds,
                    taskByNode = projectTasks.groupBy { it.sourceMindMapNodeId },
                    childrenByParent = childrenByParent,
                    expandedState = expandableState,
                    onSetGroupStatus = onSetGroupStatus,
                    onDone = onDone,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onMoveTomorrow = onMoveTomorrow,
                    onMoveCustom = onMoveCustom,
                    onCancel = onCancel,
                    onInProgress = onInProgress,
                    onReset = onReset,
                    onSetAlarm = onSetAlarm
                )
            }
        }
    }
}
    }

@Composable
private fun MindMapNodeTaskBranch(
    node: MindMapNode,
    level: Int,
    relevantIds: Set<Long>,
    taskByNode: Map<Long?, List<TaskItem>>,
    childrenByParent: Map<Long?, List<MindMapNode>>,
    expandedState: MutableMap<String, Boolean>,
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
    val directTasks = taskByNode[node.id].orEmpty()
    val visibleChildren = childrenByParent[node.id].orEmpty()
        .filter { it.id in relevantIds }
        .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    val descendantTasks = collectDescendantTasks(node.id, taskByNode, childrenByParent)
        .filter { task -> task.sourceMindMapNodeId?.let { it in relevantIds } == true }
        .distinctBy { it.origin.name + "_" + it.id }
    val key = "node_${node.id}"
    val expanded = expandedState[key] ?: true

    BranchHeaderCard(
        title = node.title,
        subtitle = if (visibleChildren.isEmpty()) {
            "${descendantTasks.size.toPersianDigits()} تسک"
        } else {
            "${visibleChildren.size.toPersianDigits()} زیرشاخه، ${descendantTasks.size.toPersianDigits()} تسک"
        },
        tasks = descendantTasks,
        expanded = expanded,
        level = level,
        onToggleExpand = { expandedState[key] = !expanded },
        onToggleDone = { targetStatus -> onSetGroupStatus(descendantTasks, targetStatus) }
    )

    if (expanded) {
        directTasks.forEach { task ->
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
                modifier = Modifier.padding(start = (level * 10).dp)
            )
        }
        visibleChildren.forEach { child ->
            MindMapNodeTaskBranch(
                node = child,
                level = level + 1,
                relevantIds = relevantIds,
                taskByNode = taskByNode,
                childrenByParent = childrenByParent,
                expandedState = expandedState,
                onSetGroupStatus = onSetGroupStatus,
                onDone = onDone,
                onEdit = onEdit,
                onDelete = onDelete,
                onMoveTomorrow = onMoveTomorrow,
                onMoveCustom = onMoveCustom,
                onCancel = onCancel,
                onInProgress = onInProgress,
                onReset = onReset,
                onSetAlarm = onSetAlarm
            )
        }
    }
}

@Composable
private fun BranchHeaderCard(
    title: String,
    subtitle: String,
    tasks: List<TaskItem>,
    expanded: Boolean,
    level: Int,
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
    val muted = tasks.isNotEmpty() && tasks.all { it.status.isClosed() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 10).dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (level == 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f) else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = if (level == 0) 0.30f else 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
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
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = subtitle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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

private fun collectRelevantNodeIds(tasks: List<TaskItem>, nodeMap: Map<Long, MindMapNode>): Set<Long> {
    val result = linkedSetOf<Long>()
    tasks.mapNotNull { it.sourceMindMapNodeId }.forEach { id ->
        var current = nodeMap[id]
        var guard = 0
        while (current != null && guard < 50) {
            result += current.id
            current = current.parentId?.let { nodeMap[it] }
            guard++
        }
    }
    return result
}

private fun collectDescendantTasks(
    nodeId: Long,
    taskByNode: Map<Long?, List<TaskItem>>,
    childrenByParent: Map<Long?, List<MindMapNode>>
): List<TaskItem> {
    val result = mutableListOf<TaskItem>()
    result += taskByNode[nodeId].orEmpty()
    childrenByParent[nodeId].orEmpty().forEach { child ->
        result += collectDescendantTasks(child.id, taskByNode, childrenByParent)
    }
    return result
}
