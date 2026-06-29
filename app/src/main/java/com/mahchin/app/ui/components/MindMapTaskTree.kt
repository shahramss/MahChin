package com.mahchin.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onSetAlarm: (TaskItem) -> Unit,
    onBatchStatus: (List<TaskItem>, TaskStatus) -> Unit = onSetGroupStatus,
    onBatchDelete: (List<TaskItem>) -> Unit = { it.forEach(onDelete) },
    onBatchMoveTomorrow: (List<TaskItem>) -> Unit = { it.forEach(onMoveTomorrow) },
    onBatchMoveCustom: (List<TaskItem>) -> Unit = { it.firstOrNull()?.let(onMoveCustom) },
    onBatchAlarm: (List<TaskItem>) -> Unit = { it.firstOrNull()?.let(onSetAlarm) }
) {
    val activeNodes = remember(mindMapNodes) { mindMapNodes.filter { it.isActive } }
    val nodeMap = remember(activeNodes) { activeNodes.associateBy { it.id } }
    val children = remember(activeNodes) { activeNodes.groupBy { it.parentId } }
    val expandableState = remember { mutableStateMapOf<String, Boolean>() }
    var selectedKeys by remember { mutableStateOf<Set<String>>(emptySet()) }

    fun key(task: TaskItem): String = task.origin.name + "_" + task.id
    val selectedTasks = tasks.filter { key(it) in selectedKeys }

    fun setSelected(list: List<TaskItem>) {
        val keys = list.map { key(it) }.toSet()
        selectedKeys = if (keys.all { it in selectedKeys }) selectedKeys - keys else selectedKeys + keys
    }

    val mindTasks = tasks
        .filter { it.sourceMindMapNodeId != null && nodeMap.containsKey(it.sourceMindMapNodeId) }
        .sortedWith(compareBy({ it.projectName ?: "" }, { it.mindMapPath ?: "" }, { it.createdAt }))
    val normalTasks = tasks.filterNot { it.sourceMindMapNodeId != null && nodeMap.containsKey(it.sourceMindMapNodeId) }
    val taskByNode = mindTasks.mapNotNull { t -> t.sourceMindMapNodeId?.let { it to t } }.toMap()

    fun subtreeTasks(node: MindMapNode): List<TaskItem> {
        val result = mutableListOf<TaskItem>()
        taskByNode[node.id]?.let { result += it }
        children[node.id].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt })).forEach { child ->
            result += subtreeTasks(child)
        }
        return result
    }

    @Composable
    fun RenderNode(node: MindMapNode, level: Int) {
        val groupTasks = subtreeTasks(node)
        if (groupTasks.isEmpty()) return
        val childNodes = children[node.id].orEmpty()
            .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
            .filter { subtreeTasks(it).isNotEmpty() }
        val ownTask = taskByNode[node.id]
        val hasChildren = childNodes.isNotEmpty()
        val indent = ((level - 1).coerceAtLeast(0) * 12).dp

        if (hasChildren) {
            val k = "node_${node.id}"
            val expanded = expandableState[k] ?: true
            ParentTaskAccordionCard(
                title = ownTask?.title ?: node.title,
                subtitle = "${groupTasks.size.toPersianDigits()} تسک زیرمجموعه",
                tasks = groupTasks,
                expanded = expanded,
                level = level,
                selected = groupTasks.any { key(it) in selectedKeys },
                modifier = Modifier.padding(start = indent),
                onToggleExpand = { expandableState[k] = !expanded },
                onToggleDone = { targetStatus -> onSetGroupStatus(groupTasks, targetStatus) },
                onLongSelect = { setSelected(groupTasks) }
            )
            if (expanded) {
                childNodes.forEach { child -> RenderNode(child, level + 1) }
            }
        } else if (ownTask != null) {
            TaskCard(
                task = ownTask,
                selected = key(ownTask) in selectedKeys,
                onLongSelect = { setSelected(listOf(ownTask)) },
                onDone = { onDone(ownTask) },
                onEdit = { onEdit(ownTask) },
                onDelete = { onDelete(ownTask) },
                onMoveTomorrow = { onMoveTomorrow(ownTask) },
                onMoveCustom = { onMoveCustom(ownTask) },
                onCancel = { onCancel(ownTask) },
                onInProgress = { onInProgress(ownTask) },
                onReset = { onReset(ownTask) },
                onSetAlarm = { onSetAlarm(ownTask) },
                modifier = Modifier.padding(start = indent + 8.dp)
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (selectedTasks.isNotEmpty()) {
            BatchActionBar(
                count = selectedTasks.size,
                onDone = { onBatchStatus(selectedTasks, TaskStatus.DONE); selectedKeys = emptySet() },
                onInProgress = { onBatchStatus(selectedTasks, TaskStatus.IN_PROGRESS); selectedKeys = emptySet() },
                onCancel = { onBatchStatus(selectedTasks, TaskStatus.CANCELED); selectedKeys = emptySet() },
                onReset = { onBatchStatus(selectedTasks, TaskStatus.NOT_STARTED); selectedKeys = emptySet() },
                onTomorrow = { onBatchMoveTomorrow(selectedTasks); selectedKeys = emptySet() },
                onCustom = { onBatchMoveCustom(selectedTasks); selectedKeys = emptySet() },
                onAlarm = { onBatchAlarm(selectedTasks); selectedKeys = emptySet() },
                onDelete = { onBatchDelete(selectedTasks); selectedKeys = emptySet() },
                onClear = { selectedKeys = emptySet() }
            )
        }

        normalTasks.forEach { task ->
            TaskCard(
                task = task,
                selected = key(task) in selectedKeys,
                onLongSelect = { setSelected(listOf(task)) },
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
            val projectKey = "project_$projectId"
            val expanded = expandableState[projectKey] ?: true
            ProjectAccordionCard(
                title = projectName,
                subtitle = "${projectTasks.size.toPersianDigits()} تسک از مایندمپ",
                tasks = projectTasks,
                expanded = expanded,
                selected = projectTasks.any { key(it) in selectedKeys },
                onToggleExpand = { expandableState[projectKey] = !expanded },
                onToggleDone = { targetStatus -> onSetGroupStatus(projectTasks, targetStatus) },
                onLongSelect = { setSelected(projectTasks) }
            )
            if (expanded) {
                val rootNodes = children[null].orEmpty()
                    .filter { it.projectId == projectId && subtreeTasks(it).isNotEmpty() }
                    .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
                rootNodes.forEach { root -> RenderNode(root, 1) }
            }
        }
    }
}

@Composable
private fun BatchActionBar(
    count: Int,
    onDone: () -> Unit,
    onInProgress: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onTomorrow: () -> Unit,
    onCustom: () -> Unit,
    onAlarm: () -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.32f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${count.toPersianDigits()} تسک انتخاب شده", fontWeight = FontWeight.Bold)
                Text("پاک انتخاب", modifier = Modifier.clickable(onClick = onClear), color = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = onDone, modifier = Modifier.weight(1f)) { Text("انجام شد") }
                OutlinedButton(onClick = onInProgress, modifier = Modifier.weight(1f)) { Text("در حال انجام") }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("برگشت") }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(onClick = onTomorrow, modifier = Modifier.weight(1f)) { Text("فردا") }
                OutlinedButton(onClick = onCustom, modifier = Modifier.weight(1f)) { Text("تاریخ") }
                OutlinedButton(onClick = onAlarm, modifier = Modifier.weight(1f)) { Text("آلارم") }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C))
                ) { Text("حذف") }
            }
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text("لغو برای امروز") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ParentTaskAccordionCard(
    title: String,
    subtitle: String,
    tasks: List<TaskItem>,
    expanded: Boolean,
    level: Int,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onToggleExpand: () -> Unit,
    onToggleDone: (TaskStatus) -> Unit,
    onLongSelect: () -> Unit
) {
    val allDone = tasks.isNotEmpty() && tasks.all { it.status == TaskStatus.DONE }
    val anyDone = tasks.any { it.status == TaskStatus.DONE }
    val check = when {
        allDone -> "✓"
        anyDone -> "◐"
        else -> "○"
    }
    val target = if (allDone) TaskStatus.NOT_STARTED else TaskStatus.DONE
    val baseColor = when (level) {
        1 -> MaterialTheme.colorScheme.primaryContainer
        2 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleExpand, onLongClick = onLongSelect),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else baseColor.copy(alpha = 0.72f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.78f else 0.28f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title.toPersianDigits(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (expanded) "⌄" else "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectAccordionCard(
    title: String,
    subtitle: String,
    tasks: List<TaskItem>,
    expanded: Boolean,
    selected: Boolean,
    onToggleExpand: () -> Unit,
    onToggleDone: (TaskStatus) -> Unit,
    onLongSelect: () -> Unit
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
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleExpand, onLongClick = onLongSelect),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.20f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 0.78f else 0.32f)),
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = title.toPersianDigits(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (allDone) TextDecoration.LineThrough else TextDecoration.None,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(text = subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                text = if (expanded) "⌄" else "›",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
    Spacer(Modifier.height(2.dp))
}
