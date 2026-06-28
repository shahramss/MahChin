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
                        "سبک XMind؛ نود مرکزی واضح، شاخه‌های نرم و فاصله‌گذاری هوشمند.",
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
    var scale by remember { mutableFloatStateOf(0.34f) }
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
                            scale = (scale * zoomChange).coerceIn(0.07f, 2.8f)
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
                    // گره‌ها شبیه XMind: کپسولی/کارت باریک، با متن کاملاً داخل کادر.
                    val radius = CornerRadius(9f * scale, 9f * scale)

                    // subtle card shadow, then selected outline, then pastel node fill
                    drawRoundRect(
                        color = Color.Black.copy(alpha = if (graphNode.level == 0) 0.16f else 0.10f),
                        topLeft = topLeft + Offset(0f, 5f * scale),
                        size = Size(nodeW, nodeH),
                        cornerRadius = radius
                    )
                    drawRoundRect(
                        color = graphNode.color.copy(alpha = if (graphNode.selected) 0.42f else 0.20f),
                        topLeft = topLeft - Offset(3f * scale, 3f * scale),
                        size = Size(nodeW + 6f * scale, nodeH + 6f * scale),
                        cornerRadius = CornerRadius(13f * scale, 13f * scale),
                        style = Stroke(width = if (graphNode.selected) 2.6f * scale else 1.0f * scale)
                    )
                    val fillColor = when (graphNode.level) {
                        0 -> graphNode.color
                        1 -> graphNode.color
                        2 -> graphNode.color.copy(alpha = 0.96f)
                        else -> graphNode.color.copy(alpha = 0.90f)
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
                        // نود مرکزی همان پروژه است؛ باید تقریباً دو برابر نودهای معمولی دیده شود.
                        0 -> 22.0f
                        1 -> 10.8f
                        2 -> 9.5f
                        else -> 8.8f
                    } * density.density * scale
                    drawContext.canvas.nativeCanvas.save()
                    drawContext.canvas.nativeCanvas.clipRect(
                        topLeft.x + if (graphNode.level == 0) 18f * scale else 10f * scale,
                        topLeft.y + if (graphNode.level == 0) 16f * scale else 10f * scale,
                        topLeft.x + nodeW - if (graphNode.level == 0) 18f * scale else 10f * scale,
                        topLeft.y + nodeH - if (graphNode.level == 0) 16f * scale else 10f * scale
                    )
                    drawContext.canvas.nativeCanvas.drawXMindRtlMultilineText(
                        lines = graphNode.title.wrapNodeTitle(graphNode.level),
                        x = if (graphNode.level == 0) center.x else topLeft.x + nodeW - 14f * scale,
                        centerY = center.y,
                        color = graphNode.textColor.toArgb(),
                        textSize = nodeTextSize,
                        bold = graphNode.level <= 1,
                        align = if (graphNode.level == 0) Paint.Align.CENTER else Paint.Align.RIGHT
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
                TextButton(onClick = { scale = 0.34f; pan = Offset.Zero }) { Text("مرکز") }
                TextButton(onClick = { scale = (scale * 1.15f).coerceAtMost(2.8f) }) { Text("+") }
                TextButton(onClick = { scale = (scale / 1.15f).coerceAtLeast(0.07f) }) { Text("−") }
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
    // Pastel XMind-like colors: readable, calm, and different per branch.
    val palette = listOf(
        Color(0xFFFFA6C9),
        Color(0xFF9FE7F5),
        Color(0xFFFFD084),
        Color(0xFFC4B5FD),
        Color(0xFFA7F3D0),
        Color(0xFFFFB4A2),
        Color(0xFF99F6E4),
        Color(0xFFBFDBFE),
        Color(0xFFFDE68A),
        Color(0xFFFECACA),
        Color(0xFFBAE6FD),
        Color(0xFFD9F99D)
    )

    val centerWidth = nodeWidth(projectTitle, 0, pixelScale)
    val centerHeight = nodeHeight(projectTitle, 0, pixelScale)
    graphNodes += GraphNode(
        node = null,
        title = projectTitle.ifBlank { "پروژه" },
        center = Offset.Zero,
        width = centerWidth,
        height = centerHeight,
        color = primary,
        textColor = Color.Black,
        level = 0,
        side = 1,
        selected = selectedNodeId == null
    )

    val roots = children[null].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (roots.isEmpty()) return MindGraphLayout(graphNodes, lines)

    // شبیه XMind: شاخه‌ها دو طرف مرکز پخش می‌شوند و هر شاخه بر اساس تعداد زیرشاخه‌ها فضای عمودی خودش را می‌گیرد.
    val rightRoots = roots.filterIndexed { index, _ -> index % 2 == 0 }
    val leftRoots = roots.filterIndexed { index, _ -> index % 2 != 0 }

    layoutXMindRootSide(
        roots = rightRoots,
        side = 1,
        sideColorOffset = 0,
        children = children,
        selectedNodeId = selectedNodeId,
        palette = palette,
        graphNodes = graphNodes,
        lines = lines,
        centerWidth = centerWidth,
        centerHeight = centerHeight,
        pixelScale = pixelScale
    )
    layoutXMindRootSide(
        roots = leftRoots,
        side = -1,
        sideColorOffset = 1,
        children = children,
        selectedNodeId = selectedNodeId,
        palette = palette,
        graphNodes = graphNodes,
        lines = lines,
        centerWidth = centerWidth,
        centerHeight = centerHeight,
        pixelScale = pixelScale
    )

    return MindGraphLayout(graphNodes, lines)
}

private fun layoutXMindRootSide(
    roots: List<MindMapNode>,
    side: Int,
    sideColorOffset: Int,
    children: Map<Long?, List<MindMapNode>>,
    selectedNodeId: Long?,
    palette: List<Color>,
    graphNodes: MutableList<GraphNode>,
    lines: MutableList<GraphLine>,
    centerWidth: Float,
    centerHeight: Float,
    pixelScale: Float
) {
    if (roots.isEmpty()) return
    val rootDistance = (centerWidth / 2f) + 520f * pixelScale
    val rootGap = 190f * pixelScale
    val totalHeight = roots.sumOf { branchBlockHeight(it, 1, children, pixelScale).toDouble() }.toFloat() +
        (roots.size - 1).coerceAtLeast(0) * rootGap
    var cursor = -totalHeight / 2f

    roots.forEachIndexed { index, root ->
        val blockHeight = branchBlockHeight(root, 1, children, pixelScale)
        val color = palette[(index * 2 + sideColorOffset) % palette.size]
        val width = nodeWidth(root.title, 1, pixelScale)
        val height = nodeHeight(root.title, 1, pixelScale)
        val autoPosition = Offset(side * rootDistance, cursor + blockHeight / 2f)
        val position = if (root.x != null && root.y != null) Offset(root.x, root.y) else autoPosition

        graphNodes += GraphNode(
            node = root,
            title = root.title,
            center = position,
            width = width,
            height = height,
            color = color,
            textColor = Color.Black,
            level = 1,
            side = side,
            selected = selectedNodeId == root.id
        )
        addGraphLine(lines, Offset.Zero, centerWidth, centerHeight, position, width, height, color)

        layoutXMindChildren(
            parent = root,
            parentPosition = position,
            parentWidth = width,
            parentHeight = height,
            side = side,
            level = 2,
            children = children,
            branchColor = color,
            selectedNodeId = selectedNodeId,
            graphNodes = graphNodes,
            lines = lines,
            pixelScale = pixelScale
        )
        cursor += blockHeight + rootGap
    }
}

private fun layoutXMindChildren(
    parent: MindMapNode,
    parentPosition: Offset,
    parentWidth: Float,
    parentHeight: Float,
    side: Int,
    level: Int,
    children: Map<Long?, List<MindMapNode>>,
    branchColor: Color,
    selectedNodeId: Long?,
    graphNodes: MutableList<GraphNode>,
    lines: MutableList<GraphLine>,
    pixelScale: Float
) {
    val directChildren = children[parent.id].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (directChildren.isEmpty()) return

    val verticalGap = branchVerticalGap(level, pixelScale)
    val totalHeight = directChildren.sumOf { branchBlockHeight(it, level, children, pixelScale).toDouble() }.toFloat() +
        (directChildren.size - 1).coerceAtLeast(0) * verticalGap
    var cursor = parentPosition.y - totalHeight / 2f
    val childX = parentPosition.x + side * branchHorizontalDistance(level, pixelScale)

    directChildren.forEach { child ->
        val blockHeight = branchBlockHeight(child, level, children, pixelScale)
        val width = nodeWidth(child.title, level, pixelScale)
        val height = nodeHeight(child.title, level, pixelScale)
        val autoPosition = Offset(childX, cursor + blockHeight / 2f)
        val position = if (child.x != null && child.y != null) Offset(child.x, child.y) else autoPosition
        val color = branchColor.copy(alpha = when (level) { 2 -> 0.95f; 3 -> 0.86f; else -> 0.78f })

        graphNodes += GraphNode(
            node = child,
            title = child.title,
            center = position,
            width = width,
            height = height,
            color = color,
            textColor = Color.Black,
            level = level,
            side = side,
            selected = selectedNodeId == child.id
        )
        addGraphLine(lines, parentPosition, parentWidth, parentHeight, position, width, height, branchColor)

        layoutXMindChildren(
            parent = child,
            parentPosition = position,
            parentWidth = width,
            parentHeight = height,
            side = side,
            level = level + 1,
            children = children,
            branchColor = branchColor,
            selectedNodeId = selectedNodeId,
            graphNodes = graphNodes,
            lines = lines,
            pixelScale = pixelScale
        )
        cursor += blockHeight + verticalGap
    }
}

private fun branchBlockHeight(
    node: MindMapNode,
    level: Int,
    children: Map<Long?, List<MindMapNode>>,
    pixelScale: Float
): Float {
    val nodeOwnHeight = nodeHeight(node.title, level, pixelScale)
    val directChildren = children[node.id].orEmpty().filter { it.isActive }
    if (directChildren.isEmpty()) return nodeOwnHeight
    val gap = branchVerticalGap(level + 1, pixelScale)
    val childrenHeight = directChildren.sumOf { branchBlockHeight(it, level + 1, children, pixelScale).toDouble() }.toFloat() +
        (directChildren.size - 1).coerceAtLeast(0) * gap
    return max(nodeOwnHeight, childrenHeight)
}

private fun branchVerticalGap(level: Int, pixelScale: Float): Float {
    return when (level) {
        1 -> 150f
        2 -> 118f
        3 -> 96f
        else -> 82f
    } * pixelScale
}

private fun branchHorizontalDistance(level: Int, pixelScale: Float): Float {
    return when (level) {
        2 -> 520f
        3 -> 430f
        4 -> 370f
        else -> 330f
    } * pixelScale
}

private fun addGraphLine(
    lines: MutableList<GraphLine>,
    fromCenter: Offset,
    fromWidth: Float,
    fromHeight: Float,
    toCenter: Offset,
    toWidth: Float,
    toHeight: Float,
    color: Color
) {
    val direction = (toCenter - fromCenter).normalizedOr(Offset(1f, 0f))
    val start = edgePoint(fromCenter, fromWidth, fromHeight, toCenter)
    val end = edgePoint(toCenter, toWidth, toHeight, fromCenter)
    lines += GraphLine(
        from = start,
        to = end,
        color = color,
        side = if (direction.x >= 0f) 1 else -1,
        angle = atan2(direction.y, direction.x)
    )
}

private fun edgePoint(center: Offset, width: Float, height: Float, toward: Offset): Offset {
    val direction = (toward - center).normalizedOr(Offset(1f, 0f))
    val tx = if (abs(direction.x) < 0.001f) Float.MAX_VALUE else (width / 2f) / abs(direction.x)
    val ty = if (abs(direction.y) < 0.001f) Float.MAX_VALUE else (height / 2f) / abs(direction.y)
    val t = min(tx, ty)
    return center + direction * t
}

private fun subtreeLeafCount(node: MindMapNode, children: Map<Long?, List<MindMapNode>>): Int {
    val list = children[node.id].orEmpty()
    return if (list.isEmpty()) 1 else list.sumOf { subtreeLeafCount(it, children) }
}

private fun nodeWidth(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level)
    val longest = lines.maxOfOrNull { it.length } ?: 8
    // XMind-style: width follows the longest readable line; padding is balanced and not too wide.
    val charWidth = when (level) {
        0 -> 15.8f
        1 -> 8.6f
        2 -> 7.7f
        else -> 7.1f
    }
    val horizontalPadding = when (level) {
        0 -> 120f
        1 -> 36f
        2 -> 30f
        else -> 26f
    }
    val minWidth = when (level) {
        0 -> 300f
        1 -> 120f
        2 -> 104f
        else -> 92f
    }
    val maxWidth = when (level) {
        0 -> 860f
        1 -> 430f
        2 -> 370f
        else -> 330f
    }
    return ((longest * charWidth + horizontalPadding).coerceIn(minWidth, maxWidth)) * pixelScale
}

private fun nodeHeight(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level).size.coerceAtLeast(1)
    val lineHeight = when (level) {
        0 -> 31.0f
        1 -> 16.2f
        2 -> 14.4f
        else -> 13.2f
    }
    // نود مرکزی بزرگ‌ترین بخش نقشه است؛ نودهای دیگر کارت‌های XMind-like هستند.
    val verticalPadding = when (level) {
        0 -> 76f
        1 -> 30f
        2 -> 25f
        else -> 22f
    }
    return ((verticalPadding + lines * lineHeight).coerceAtLeast(
        when (level) {
            0 -> 132f
            1 -> 52f
            2 -> 44f
            else -> 38f
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
    // XMind-style wrapping: keep a readable sentence in each line and wrap around 6-8 words,
    // not two words per line. Persian/RTL text stays complete and inside the node.
    val maxWordsPerLine = when (level) {
        0 -> 7
        1 -> 6
        2 -> 6
        else -> 6
    }
    val maxCharsPerLine = when (level) {
        0 -> 64
        1 -> 52
        2 -> 48
        else -> 44
    }
    val words = trim().toPersianDigits().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("بدون عنوان")

    val lines = mutableListOf<String>()
    var currentWords = mutableListOf<String>()

    fun currentText(): String = currentWords.joinToString(" ")
    fun flush() {
        if (currentWords.isNotEmpty()) {
            lines += currentText()
            currentWords = mutableListOf()
        }
    }

    words.forEach { rawWord ->
        val parts = if (rawWord.length > maxCharsPerLine) rawWord.chunked(maxCharsPerLine) else listOf(rawWord)
        parts.forEach { word ->
            if (currentWords.isEmpty()) {
                currentWords += word
            } else {
                val candidate = currentText() + " " + word
                if (currentWords.size < maxWordsPerLine && candidate.length <= maxCharsPerLine) {
                    currentWords += word
                } else {
                    flush()
                    currentWords += word
                }
            }
        }
    }
    flush()
    return lines
}

private fun android.graphics.Canvas.drawXMindRtlMultilineText(
    lines: List<String>,
    x: Float,
    centerY: Float,
    color: Int,
    textSize: Float,
    bold: Boolean,
    align: Paint.Align
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        this.textSize = textSize
        textAlign = align
        isFakeBoldText = bold
    }
    val lineHeight = (paint.descent() - paint.ascent()) * 1.18f
    val totalHeight = lineHeight * lines.size
    var baseline = centerY - totalHeight / 2f - paint.ascent()
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
