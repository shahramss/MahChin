package com.mahchin.app.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.domain.toEnglishDigits
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.components.VoiceOutlinedTextField
import com.mahchin.app.ui.viewmodel.MainViewModel
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindMapScreen(vm: MainViewModel) {
    val projects by vm.projects.collectAsState()
    val selectedProjectId by vm.selectedProjectId.collectAsState()
    val nodes by vm.mindMapNodes.collectAsState()
    val selectedProject = projects.firstOrNull { it.id == selectedProjectId } ?: projects.firstOrNull()

    var projectMenu by remember { mutableStateOf(false) }
    var addProjectDialog by remember { mutableStateOf(false) }
    var editProjectDialog by remember { mutableStateOf(false) }
    var deleteProjectDialog by remember { mutableStateOf(false) }
    var projectActions by remember { mutableStateOf(false) }
    var nodeDialog by remember { mutableStateOf<NodeDialogState?>(null) }
    var distributeDialog by remember { mutableStateOf(false) }
    var actionNode by remember { mutableStateOf<MindMapNode?>(null) }
    var centerActions by remember { mutableStateOf(false) }
    var selectedNodeId by remember(selectedProjectId) { mutableStateOf<Long?>(null) }
    val selectedNode = nodes.firstOrNull { it.id == selectedNodeId && it.isActive }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("مایندمپ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "فضای آزاد شبیه XMind؛ بکش، زوم کن، بعد شاخه بساز.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedButton(
                    onClick = { distributeDialog = true },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(44.dp)
                ) { Text("تسک‌سازی") }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ExposedDropdownMenuBox(
                    expanded = projectMenu,
                    onExpandedChange = { projectMenu = !projectMenu },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedProject?.name ?: "پروژه",
                        onValueChange = {},
                        readOnly = true,
                        singleLine = true,
                        label = { Text("پروژه") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectMenu) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = projectMenu,
                        onDismissRequest = { projectMenu = false }
                    ) {
                        projects.forEach { project ->
                            DropdownMenuItem(
                                text = { Text(project.name) },
                                onClick = {
                                    vm.selectProject(project.id)
                                    selectedNodeId = null
                                    projectMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { projectActions = true },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("پروژه", maxLines = 1, softWrap = false) }
            }

            XMindLikeCanvasCard(
                projectTitle = selectedProject?.name ?: "پروژه",
                nodes = nodes,
                selectedNodeId = selectedNodeId,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onNodeTap = { node -> selectedNodeId = node.id },
                onCenterTap = { selectedNodeId = null },
                onNodeLongPress = { actionNode = it },
                onCenterLongPress = { centerActions = true },
                onEmptyTap = { selectedNodeId = null },
                onNodeMove = { node, x, y -> vm.moveMindMapNode(node.id, x, y) }
            )
        }

        MindMapBottomBar(
            selectedNode = selectedNode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            onAddRoot = { nodeDialog = NodeDialogState(parentId = null) },
            onAddChild = { selectedNode?.let { nodeDialog = NodeDialogState(parentId = it.id) } },
            onEditSelected = { selectedNode?.let { nodeDialog = NodeDialogState(editNode = it, parentId = it.parentId) } },
            onDistribute = { distributeDialog = true }
        )
    }

    if (addProjectDialog) {
        ProjectEditorDialog(
            title = "پروژه جدید",
            initialName = "",
            onDismiss = { addProjectDialog = false },
            onSave = {
                vm.addProject(it)
                selectedNodeId = null
                addProjectDialog = false
            }
        )
    }

    if (editProjectDialog && selectedProject != null) {
        ProjectEditorDialog(
            title = "ویرایش پروژه",
            initialName = selectedProject.name,
            onDismiss = { editProjectDialog = false },
            onSave = {
                vm.updateProject(selectedProject.id, it)
                editProjectDialog = false
            }
        )
    }

    if (deleteProjectDialog && selectedProject != null) {
        AlertDialog(
            onDismissRequest = { deleteProjectDialog = false },
            title = { Text("حذف پروژه") },
            text = { Text("پروژه «${selectedProject.name}» حذف شود؟ اگر تنها پروژه باشد حذف نمی‌شود.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteProject(selectedProject.id)
                        selectedNodeId = null
                        deleteProjectDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5C5C))
                ) { Text("حذف") }
            },
            dismissButton = { TextButton(onClick = { deleteProjectDialog = false }) { Text("انصراف") } }
        )
    }

    nodeDialog?.let { state ->
        NodeEditorDialog(
            state = state,
            onDismiss = { nodeDialog = null },
            onSave = { title, desc ->
                if (state.editNode == null) {
                    vm.addMindMapNode(state.parentId, title, desc)
                } else {
                    vm.updateMindMapNode(state.editNode.id, title, desc)
                }
                nodeDialog = null
            }
        )
    }

    if (distributeDialog) {
        DistributeMindMapDialog(
            initialDate = vm.today,
            onDismiss = { distributeDialog = false },
            onSave = { date, perDay ->
                vm.makeTasksFromMindMap(date, perDay)
                distributeDialog = false
            }
        )
    }

    if (projectActions) {
        MindMapActionSheet(
            title = selectedProject?.name ?: "پروژه",
            subtitle = "مدیریت پروژه‌ها؛ اضافه، ویرایش یا حذف پروژه فعلی.",
            onDismiss = { projectActions = false },
            actions = listOf(
                MindAction("افزودن پروژه", Icons.Outlined.Add) {
                    projectActions = false
                    addProjectDialog = true
                },
                MindAction("ویرایش پروژه فعلی", Icons.Outlined.Edit) {
                    projectActions = false
                    editProjectDialog = selectedProject != null
                },
                MindAction("حذف پروژه فعلی", Icons.Outlined.Delete, danger = true) {
                    projectActions = false
                    deleteProjectDialog = selectedProject != null
                }
            )
        )
    }

    if (centerActions) {
        MindMapActionSheet(
            title = selectedProject?.name ?: "پروژه",
            subtitle = "نود مرکزی پروژه است. شاخه اصلی از همین‌جا شروع می‌شود.",
            onDismiss = { centerActions = false },
            actions = listOf(
                MindAction("افزودن شاخه اصلی", Icons.Outlined.Add) {
                    centerActions = false
                    nodeDialog = NodeDialogState(parentId = null)
                }
            )
        )
    }

    actionNode?.let { node ->
        MindMapActionSheet(
            title = node.title,
            subtitle = if (node.description.isBlank()) "گزینه‌های این نود" else node.description,
            onDismiss = { actionNode = null },
            actions = listOf(
                MindAction("افزودن زیرشاخه", Icons.Outlined.Add) {
                    actionNode = null
                    selectedNodeId = node.id
                    nodeDialog = NodeDialogState(parentId = node.id)
                },
                MindAction("افزودن شاخه کنار این", Icons.Outlined.Add) {
                    actionNode = null
                    nodeDialog = NodeDialogState(parentId = node.parentId)
                },
                MindAction("ویرایش", Icons.Outlined.Edit) {
                    actionNode = null
                    nodeDialog = NodeDialogState(editNode = node, parentId = node.parentId)
                },
                MindAction("حذف", Icons.Outlined.Delete, danger = true) {
                    actionNode = null
                    if (selectedNodeId == node.id) selectedNodeId = null
                    vm.deleteMindMapNode(node.id)
                }
            )
        )
    }
}

data class NodeDialogState(val editNode: MindMapNode? = null, val parentId: Long? = null)

private data class GraphNode(
    val node: MindMapNode?,
    val title: String,
    val center: Offset,
    val width: Float,
    val height: Float,
    val color: Color,
    val textColor: Color,
    val level: Int,
    val side: Int,
    val selected: Boolean = false
)

private data class GraphLine(val from: Offset, val to: Offset, val color: Color, val side: Int, val angle: Float = 0f)
private data class MindGraphLayout(val nodes: List<GraphNode>, val lines: List<GraphLine>)

@Composable
private fun XMindLikeCanvasCard(
    projectTitle: String,
    nodes: List<MindMapNode>,
    selectedNodeId: Long?,
    modifier: Modifier,
    onNodeTap: (MindMapNode) -> Unit,
    onCenterTap: () -> Unit,
    onNodeLongPress: (MindMapNode) -> Unit,
    onCenterLongPress: () -> Unit,
    onEmptyTap: () -> Unit,
    onNodeMove: (MindMapNode, Float, Float) -> Unit
) {
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current
    var scale by remember { mutableFloatStateOf(0.48f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var dragPositions by remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }

    Card(
        modifier = modifier.padding(bottom = 82.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, outline.copy(alpha = 0.14f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val origin = Offset(w / 2f, h / 2f) + pan
            val nodesForGraph = nodes.map { node ->
                dragPositions[node.id]?.let { node.copy(x = it.x, y = it.y) } ?: node
            }
            val pixelScale = density.density.coerceAtLeast(1f)
            val graph = remember(projectTitle, nodesForGraph, selectedNodeId, primary, onPrimary, onSurface, pixelScale) {
                buildXMindGraph(projectTitle, nodesForGraph, selectedNodeId, primary, onPrimary, onSurface, pixelScale)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(graph, scale, pan) {
                        var draggingNode: GraphNode? = null
                        detectDragGestures(
                            onDragStart = { start ->
                                val worldStart = screenToWorld(start, origin, scale)
                                draggingNode = graph.nodes.asReversed().firstOrNull { it.node != null && it.hit(worldStart) }
                                draggingNode?.node?.let { onNodeTap(it) }
                            },
                            onDragEnd = {
                                draggingNode?.let { g ->
                                    val node = g.node
                                    if (node != null) {
                                        val finalPos = dragPositions[node.id] ?: g.center
                                        onNodeMove(node, finalPos.x, finalPos.y)
                                    }
                                }
                                draggingNode = null
                            },
                            onDragCancel = { draggingNode = null },
                            onDrag = { change, dragAmount ->
                                val node = draggingNode?.node
                                if (node != null) {
                                    change.consume()
                                    val current = dragPositions[node.id] ?: draggingNode!!.center
                                    val next = current + Offset(dragAmount.x / scale, dragAmount.y / scale)
                                    dragPositions = dragPositions + (node.id to next)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            pan += panChange
                            scale = (scale * zoomChange).coerceIn(0.12f, 2.6f)
                        }
                    }
                    .pointerInput(graph, scale, pan) {
                        detectTapGestures(
                            onTap = { tap ->
                                val worldTap = screenToWorld(tap, origin, scale)
                                val hit = graph.nodes.asReversed().firstOrNull { it.hit(worldTap) }
                                when {
                                    hit?.node != null -> onNodeTap(hit.node)
                                    hit != null -> onCenterTap()
                                    else -> onEmptyTap()
                                }
                            },
                            onLongPress = { tap ->
                                val worldTap = screenToWorld(tap, origin, scale)
                                val hit = graph.nodes.asReversed().firstOrNull { it.hit(worldTap) }
                                when {
                                    hit?.node != null -> onNodeLongPress(hit.node)
                                    hit != null -> onCenterLongPress()
                                    else -> onEmptyTap()
                                }
                            }
                        )
                    }
            ) {
                val gridColor = outline.copy(alpha = 0.08f)
                val grid = 64f * scale
                if (grid >= 24f) {
                    var x = (origin.x % grid) - grid
                    while (x < size.width + grid) {
                        var y = (origin.y % grid) - grid
                        while (y < size.height + grid) {
                            drawCircle(gridColor, radius = 1.4f, center = Offset(x, y))
                            y += grid
                        }
                        x += grid
                    }
                }

                graph.lines.forEach { line ->
                    val start = worldToScreen(line.from, origin, scale)
                    val end = worldToScreen(line.to, origin, scale)
                    val dx = end.x - start.x
                    val dy = end.y - start.y
                    val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                    val radial = Offset(cos(line.angle) * scale, sin(line.angle) * scale)
                    val controlDistance = (distance * 0.42f).coerceIn(70f * scale, 170f * scale)
                    val c1 = start + radial * controlDistance
                    val c2 = end - radial * controlDistance
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                    }
                    drawPath(
                        path = path,
                        color = line.color.copy(alpha = 0.82f),
                        style = Stroke(width = 3.2f * scale.coerceIn(0.35f, 1.1f))
                    )
                }

                graph.nodes.forEach { graphNode ->
                    val center = worldToScreen(graphNode.center, origin, scale)
                    val nodeW = graphNode.width * scale
                    val nodeH = graphNode.height * scale
                    val topLeft = Offset(center.x - nodeW / 2f, center.y - nodeH / 2f)
                    val radius = CornerRadius(16f * scale, 16f * scale)

                    drawRoundRect(
                        color = graphNode.color.copy(alpha = if (graphNode.selected) 0.22f else 0.12f),
                        topLeft = topLeft - Offset(3f * scale, 3f * scale),
                        size = Size(nodeW + 6f * scale, nodeH + 6f * scale),
                        cornerRadius = CornerRadius(18f * scale, 18f * scale),
                        style = Stroke(width = if (graphNode.selected) 2.4f * scale else 1.2f * scale)
                    )
                    val fillColor = when (graphNode.level) {
                        0 -> graphNode.color
                        1 -> graphNode.color
                        2 -> graphNode.color.copy(alpha = 0.94f)
                        else -> graphNode.color.copy(alpha = 0.88f)
                    }
                    drawRoundRect(
                        color = fillColor,
                        topLeft = topLeft,
                        size = Size(nodeW, nodeH),
                        cornerRadius = radius
                    )
                    if (graphNode.selected) {
                        drawRoundRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = topLeft,
                            size = Size(nodeW, nodeH),
                            cornerRadius = radius,
                            style = Stroke(width = 2.2f * scale)
                        )
                    }
                    val nodeTextSize = when (graphNode.level) {
                        0 -> 12.8f
                        1 -> 10.6f
                        2 -> 9.8f
                        else -> 9.2f
                    } * density.density * scale
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.clipRect(
                        topLeft.x + 10f * scale,
                        topLeft.y + 10f * scale,
                        topLeft.x + nodeW - 10f * scale,
                        topLeft.y + nodeH - 10f * scale
                    )
                    drawContext.canvas.nativeCanvas.drawCenteredMultilineText(
                        lines = graphNode.title.wrapNodeTitle(graphNode.level),
                        x = center.x,
                        y = center.y,
                        color = graphNode.textColor.toArgb(),
                        textSize = nodeTextSize,
                        bold = graphNode.level <= 1
                    )
                    drawContext.canvas.nativeCanvas.restore()
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "فضای آزاد مایندمپ",
                    color = onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "لمس نود = انتخاب  •  نگه‌داشتن = منو  •  دو انگشت = زوم",
                    color = onSurfaceVariant.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (selectedNodeId != null) {
                val selected = nodes.firstOrNull { it.id == selectedNodeId }
                if (selected != null) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    ) {
                        Text(
                            "انتخاب: ${selected.title.toPersianDigits()}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { scale = 0.48f; pan = Offset.Zero }) { Text("مرکز") }
                TextButton(onClick = { scale = (scale * 1.15f).coerceAtMost(2.6f) }) { Text("+") }
                TextButton(onClick = { scale = (scale / 1.15f).coerceAtLeast(0.12f) }) { Text("−") }
            }

            if (nodes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("از یک شاخه اصلی شروع کن", fontWeight = FontWeight.Bold)
                    Text(
                        "مثلاً: طراحی، محتوا، سئو. بعد هرکدام را به زیرشاخه تبدیل کن.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MindMapBottomBar(
    selectedNode: MindMapNode?,
    modifier: Modifier,
    onAddRoot: () -> Unit,
    onAddChild: () -> Unit,
    onEditSelected: () -> Unit,
    onDistribute: () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onAddRoot,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                ToolbarButtonText("+", large = true)
            }
            Button(
                onClick = onAddChild,
                enabled = selectedNode != null,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                ToolbarButtonText("++", large = true)
            }
            OutlinedButton(
                onClick = onEditSelected,
                enabled = selectedNode != null,
                modifier = Modifier.weight(0.85f).height(48.dp),
                shape = RoundedCornerShape(18.dp)
            ) { ToolbarButtonText("ویرایش") }
            OutlinedButton(
                onClick = onDistribute,
                modifier = Modifier.weight(0.95f).height(48.dp),
                shape = RoundedCornerShape(18.dp)
            ) { ToolbarButtonText("تسک") }
        }
    }
}

@Composable
private fun ToolbarButtonText(text: String, large: Boolean = false) {
    Text(
        text = text,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        fontSize = if (large) 24.sp else 11.sp,
        fontWeight = FontWeight.Bold
    )
}

private fun buildXMindGraph(
    projectTitle: String,
    allNodes: List<MindMapNode>,
    selectedNodeId: Long?,
    primary: Color,
    onPrimary: Color,
    onSurface: Color,
    pixelScale: Float
): MindGraphLayout {
    val activeNodes = allNodes.filter { it.isActive }
    val children = activeNodes.groupBy { it.parentId }
    val graphNodes = mutableListOf<GraphNode>()
    val lines = mutableListOf<GraphLine>()
    val palette = listOf(
        Color(0xFF00C2FF),
        Color(0xFFFF4D8D),
        Color(0xFF7C3AED),
        Color(0xFFFFB020),
        Color(0xFF22C55E),
        Color(0xFFFF5C35),
        Color(0xFF14B8A6),
        Color(0xFF6366F1),
        Color(0xFFEAB308),
        Color(0xFFEF4444),
        Color(0xFF06B6D4),
        Color(0xFFA3E635)
    )

    graphNodes += GraphNode(
        node = null,
        title = projectTitle.ifBlank { "پروژه" },
        center = Offset.Zero,
        width = nodeWidth(projectTitle, 0, pixelScale),
        height = nodeHeight(projectTitle, 0, pixelScale),
        color = primary,
        textColor = Color.Black,
        level = 0,
        side = 1,
        selected = selectedNodeId == null
    )

    val roots = children[null].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (roots.isEmpty()) return MindGraphLayout(graphNodes, lines)

    val radiusX = 660f * pixelScale
    val radiusY = 500f * pixelScale
    roots.forEachIndexed { index, root ->
        // ساعت‌گرد: از بالای صفحه شروع می‌شود و ریشه‌ها دور پروژه می‌چرخند.
        val angle = ((-90f + (360f / roots.size) * index) * Math.PI / 180.0).toFloat()
        val color = palette[index % palette.size]
        val dir = Offset(cos(angle), sin(angle))
        val autoRootPosition = Offset(dir.x * radiusX, dir.y * radiusY)
        val rootPosition = if (root.x != null && root.y != null) Offset(root.x, root.y) else autoRootPosition
        val actualDir = rootPosition.normalizedOr(dir)
        val actualAngle = atan2(actualDir.y, actualDir.x)
        val width = nodeWidth(root.title, 1, pixelScale)
        val height = nodeHeight(root.title, 1, pixelScale)
        graphNodes += GraphNode(
            node = root,
            title = root.title,
            center = rootPosition,
            width = width,
            height = height,
            color = color,
            textColor = Color.Black,
            level = 1,
            side = if (actualDir.x >= 0f) 1 else -1,
            selected = selectedNodeId == root.id
        )
        lines += GraphLine(
            from = Offset.Zero,
            to = rootPosition - actualDir * (width / 2f),
            color = color,
            side = if (actualDir.x >= 0f) 1 else -1,
            angle = actualAngle
        )
        layoutClockwiseChildren(
            parent = root,
            parentPosition = rootPosition,
            parentWidth = width,
            angle = actualAngle,
            level = 2,
            children = children,
            branchColor = color,
            selectedNodeId = selectedNodeId,
            graphNodes = graphNodes,
            lines = lines,
            onSurface = onSurface,
            pixelScale = pixelScale
        )
    }
    val resolvedNodes = resolveGraphOverlaps(graphNodes, pixelScale)
    return MindGraphLayout(resolvedNodes, rebuildGraphLines(resolvedNodes, primary))
}

private fun layoutClockwiseChildren(
    parent: MindMapNode,
    parentPosition: Offset,
    parentWidth: Float,
    angle: Float,
    level: Int,
    children: Map<Long?, List<MindMapNode>>,
    branchColor: Color,
    selectedNodeId: Long?,
    graphNodes: MutableList<GraphNode>,
    lines: MutableList<GraphLine>,
    onSurface: Color,
    pixelScale: Float
) {
    val directChildren = children[parent.id].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (directChildren.isEmpty()) return

    val radial = Offset(cos(angle), sin(angle))
    val perpendicular = Offset(-sin(angle), cos(angle))
    val leafSpacing = when (level) {
        2 -> 240f * pixelScale
        3 -> 215f * pixelScale
        else -> 190f * pixelScale
    }
    val totalLeaves = directChildren.sumOf { subtreeLeafCount(it, children).coerceAtLeast(1) }
    var cursor = -(totalLeaves * leafSpacing) / 2f

    directChildren.forEach { child ->
        val leaves = subtreeLeafCount(child, children).coerceAtLeast(1)
        val blockHeight = leaves * leafSpacing
        val tangentOffset = cursor + blockHeight / 2f
        val width = nodeWidth(child.title, level, pixelScale)
        val height = nodeHeight(child.title, level, pixelScale)
        val distance = when (level) {
            2 -> 470f * pixelScale
            3 -> 405f * pixelScale
            else -> 360f * pixelScale
        }
        val autoPos = parentPosition + radial * distance + perpendicular * tangentOffset
        val pos = if (child.x != null && child.y != null) Offset(child.x, child.y) else autoPos
        val color = branchColor.copy(alpha = when (level) { 2 -> 0.92f; 3 -> 0.80f; else -> 0.72f })
        val textColor = Color.Black
        graphNodes += GraphNode(
            node = child,
            title = child.title,
            center = pos,
            width = width,
            height = height,
            color = color,
            textColor = textColor,
            level = level,
            side = if (radial.x >= 0f) 1 else -1,
            selected = selectedNodeId == child.id
        )
        val lineDir = (pos - parentPosition).normalizedOr(radial)
        val lineAngle = atan2(lineDir.y, lineDir.x)
        lines += GraphLine(
            from = parentPosition + lineDir * (parentWidth / 2f),
            to = pos - lineDir * (width / 2f),
            color = branchColor,
            side = if (lineDir.x >= 0f) 1 else -1,
            angle = lineAngle
        )
        layoutClockwiseChildren(
            parent = child,
            parentPosition = pos,
            parentWidth = width,
            angle = angle,
            level = level + 1,
            children = children,
            branchColor = branchColor,
            selectedNodeId = selectedNodeId,
            graphNodes = graphNodes,
            lines = lines,
            onSurface = onSurface,
            pixelScale = pixelScale
        )
        cursor += blockHeight
    }
}

private fun subtreeLeafCount(node: MindMapNode, children: Map<Long?, List<MindMapNode>>): Int {
    val list = children[node.id].orEmpty()
    return if (list.isEmpty()) 1 else list.sumOf { subtreeLeafCount(it, children) }
}


private fun resolveGraphOverlaps(nodes: List<GraphNode>, pixelScale: Float): List<GraphNode> {
    if (nodes.size <= 2) return nodes
    val fixed = nodes.toMutableList()
    val centerIndex = fixed.indexOfFirst { it.node == null }
    val minGap = 72f * pixelScale
    repeat(10) {
        var changed = false
        for (i in fixed.indices) {
            for (j in i + 1 until fixed.size) {
                if (i == centerIndex || j == centerIndex) continue
                val a = fixed[i]
                val b = fixed[j]
                val dx = b.center.x - a.center.x
                val dy = b.center.y - a.center.y
                val overlapX = (a.width + b.width) / 2f + minGap - kotlin.math.abs(dx)
                val overlapY = (a.height + b.height) / 2f + minGap - kotlin.math.abs(dy)
                if (overlapX > 0f && overlapY > 0f) {
                    val moveX = if (dx >= 0f) overlapX / 2f else -overlapX / 2f
                    val moveY = if (dy >= 0f) overlapY / 2f else -overlapY / 2f
                    fixed[i] = a.copy(center = Offset(a.center.x - moveX, a.center.y - moveY))
                    fixed[j] = b.copy(center = Offset(b.center.x + moveX, b.center.y + moveY))
                    changed = true
                }
            }
        }
        if (!changed) return fixed
    }
    return fixed
}

private fun rebuildGraphLines(nodes: List<GraphNode>, primary: Color): List<GraphLine> {
    val nodeById = nodes.mapNotNull { graphNode -> graphNode.node?.id?.let { it to graphNode } }.toMap()
    val center = nodes.firstOrNull { it.node == null }
    return nodes.mapNotNull { child ->
        val node = child.node ?: return@mapNotNull null
        val parent = node.parentId?.let { nodeById[it] } ?: center ?: return@mapNotNull null
        val from = parent.center
        val to = child.center
        val direction = (to - from).normalizedOr(Offset(1f, 0f))
        val start = from + direction * (parent.width / 2f)
        val end = to - direction * (child.width / 2f)
        val color = if (parent.node == null) child.color else parent.color
        GraphLine(
            from = start,
            to = end,
            color = color,
            side = if (direction.x >= 0f) 1 else -1,
            angle = atan2(direction.y, direction.x)
        )
    }
}

private fun nodeWidth(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level)
    val longest = lines.maxOfOrNull { it.length } ?: 8
    val charWidth = when (level) {
        0 -> 9.9f
        1 -> 8.7f
        2 -> 8.0f
        else -> 7.5f
    }
    val horizontalPadding = when (level) {
        0 -> 34f
        1 -> 28f
        2 -> 26f
        else -> 24f
    }
    val minWidth = when (level) {
        0 -> 112f
        1 -> 98f
        2 -> 90f
        else -> 84f
    }
    val maxWidth = when (level) {
        0 -> 190f
        1 -> 162f
        2 -> 148f
        else -> 138f
    }
    return ((longest * charWidth + horizontalPadding).coerceIn(minWidth, maxWidth)) * pixelScale
}

private fun nodeHeight(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level).size.coerceAtLeast(1)
    val lineHeight = when (level) {
        0 -> 16.5f
        1 -> 14.8f
        2 -> 13.8f
        else -> 13.0f
    }
    val verticalPadding = when (level) {
        0 -> 38f
        1 -> 34f
        2 -> 32f
        else -> 30f
    }
    return ((verticalPadding + lines * lineHeight).coerceAtLeast(
        when (level) {
            0 -> 58f
            1 -> 52f
            2 -> 48f
            else -> 44f
        }
    )) * pixelScale
}

private fun GraphNode.hit(point: Offset): Boolean {
    return point.x >= center.x - width / 2f - 12f &&
        point.x <= center.x + width / 2f + 12f &&
        point.y >= center.y - height / 2f - 12f &&
        point.y <= center.y + height / 2f + 12f
}

private fun worldToScreen(world: Offset, origin: Offset, scale: Float): Offset {
    return Offset(origin.x + world.x * scale, origin.y + world.y * scale)
}

private fun screenToWorld(screen: Offset, origin: Offset, scale: Float): Offset {
    return Offset((screen.x - origin.x) / scale, (screen.y - origin.y) / scale)
}

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)
private operator fun Offset.minus(other: Offset): Offset = Offset(x - other.x, y - other.y)
private operator fun Offset.times(value: Float): Offset = Offset(x * value, y * value)

private fun Offset.normalizedOr(fallback: Offset): Offset {
    val length = sqrt(x * x + y * y)
    return if (length < 0.001f) fallback else Offset(x / length, y / length)
}

private fun String.wrapNodeTitle(level: Int): List<String> {
    val maxChars = when (level) {
        0 -> 13
        1 -> 11
        2 -> 10
        else -> 10
    }
    val words = trim().toPersianDigits().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("بدون عنوان")
    val lines = mutableListOf<String>()
    var current = ""
    fun flush() {
        if (current.isNotBlank()) {
            lines += current
            current = ""
        }
    }
    words.forEach { word ->
        if (word.length > maxChars) {
            flush()
            word.chunked(maxChars).forEach { lines += it }
        } else if (current.isBlank()) {
            current = word
        } else if ((current.length + 1 + word.length) <= maxChars) {
            current += " $word"
        } else {
            flush()
            current = word
        }
    }
    flush()
    return lines
}

private fun android.graphics.Canvas.drawCenteredMultilineText(
    lines: List<String>,
    x: Float,
    y: Float,
    color: Int,
    textSize: Float,
    bold: Boolean
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        textAlign = Paint.Align.CENTER
        isFakeBoldText = bold
    }
    val lineHeight = (paint.descent() - paint.ascent()) * 1.08f
    val totalHeight = lineHeight * lines.size
    var baseline = y - totalHeight / 2f - paint.ascent()
    lines.forEach { line ->
        drawText(line, x, baseline, paint)
        baseline += lineHeight
    }
}

private data class MindAction(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val danger: Boolean = false,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MindMapActionSheet(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit,
    actions: List<MindAction>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title.toPersianDigits(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle.toPersianDigits(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            actions.forEachIndexed { index, action ->
                MindActionRow(action.title, action.icon, action.danger, action.onClick)
                if (index == actions.lastIndex - 1 && actions.last().danger) {
                    HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                }
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MindActionRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val color = if (danger) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
        Text(title, color = color, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun ProjectEditorDialog(
    title: String,
    initialName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var projectName by remember(initialName) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            VoiceOutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = "نام پروژه",
                singleLine = true,
                prompt = "نام پروژه را بگو",
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onSave(projectName) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun NodeEditorDialog(
    state: NodeDialogState,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(state.editNode?.title.orEmpty()) }
    var desc by remember { mutableStateOf(state.editNode?.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.editNode == null) if (state.parentId == null) "شاخه اصلی جدید" else "زیرشاخه جدید" else "ویرایش نود") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                VoiceOutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "عنوان",
                    singleLine = true,
                    prompt = "عنوان نود را بگو",
                    modifier = Modifier.fillMaxWidth()
                )
                VoiceOutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = "توضیح کوتاه",
                    singleLine = false,
                    minLines = 2,
                    prompt = "توضیح نود را بگو",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { onSave(title, desc) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun DistributeMindMapDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onSave: (JalaliDate, Int) -> Unit
) {
    var year by remember { mutableStateOf(initialDate.year.toString()) }
    var month by remember { mutableStateOf(initialDate.month.toString()) }
    var day by remember { mutableStateOf(initialDate.day.toString()) }
    var perDay by remember { mutableStateOf("5") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تقسیم مایندمپ به تسک") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("زیرشاخه‌های نهایی زیر پروژه و مسیر شاخه‌ها جمع می‌شوند. می‌توانی همه را در یک روز بسازی یا بین روزها تقسیم کنی.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallNumberField("سال", year, { year = it }, Modifier.weight(1f))
                    SmallNumberField("ماه", month, { month = it }, Modifier.weight(1f))
                    SmallNumberField("روز", day, { day = it }, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallNumberField("تعداد تسک در روز", perDay, { perDay = it }, Modifier.weight(1f))
                    OutlinedButton(
                        onClick = { perDay = "9999" },
                        modifier = Modifier.height(56.dp)
                    ) { Text("همه در یک روز") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val y = year.toIntOrNull()
                val m = month.toIntOrNull()
                val d = day.toIntOrNull()
                val p = perDay.toIntOrNull()
                if (y == null || m == null || d == null || p == null) {
                    error = "اطلاعات را کامل وارد کن."
                } else {
                    try {
                        val safeDay = d.coerceAtMost(JalaliCalendar.monthLength(y, m))
                        onSave(JalaliDate(y, m, safeDay), p.coerceAtLeast(1))
                    } catch (e: Exception) {
                        error = e.message ?: "تاریخ نامعتبر است."
                    }
                }
            }) { Text("ساخت تسک‌ها") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun SmallNumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier
) {
    OutlinedTextField(
        value = value.toPersianDigits(),
        onValueChange = { onValueChange(it.toEnglishDigits().filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
