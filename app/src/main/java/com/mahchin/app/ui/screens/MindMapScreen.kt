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
                        "طراحی تازه شبیه XMind؛ نودهای خوانا، فونت بزرگ، چیدمان دوطرفه و بدون تداخل.",
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
    val fontSize: Float,
    val horizontalPadding: Float,
    val verticalPadding: Float,
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
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val density = LocalDensity.current

    // بازنویسی کامل: زوم اولیه خوانا، بوم آزاد و چیدمان خودکار شبیه XMind.
    var scale by remember { mutableFloatStateOf(0.54f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var manualPositions by remember { mutableStateOf<Map<Long, Offset>>(emptyMap()) }

    Card(
        modifier = modifier.padding(bottom = 82.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071321)),
        border = BorderStroke(1.dp, Color(0xFF223244).copy(alpha = 0.72f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val origin = Offset(w / 2f, h / 2f) + pan
            val pixelScale = density.density.coerceAtLeast(1f)
            val graph = remember(projectTitle, nodes, selectedNodeId, manualPositions, primary, onPrimary, onSurface, pixelScale) {
                buildXMindGraph(
                    projectTitle = projectTitle,
                    allNodes = nodes,
                    selectedNodeId = selectedNodeId,
                    primary = primary,
                    onPrimary = onPrimary,
                    onSurface = onSurface,
                    pixelScale = pixelScale,
                    manualPositions = manualPositions
                )
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
                                        val finalPos = manualPositions[node.id] ?: g.center
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
                                    val current = manualPositions[node.id] ?: draggingNode!!.center
                                    val next = current + Offset(dragAmount.x / scale, dragAmount.y / scale)
                                    manualPositions = manualPositions + (node.id to next)
                                }
                            }
                        )
                    }
                    .pointerInput(Unit) {
                        detectTransformGestures { _, panChange, zoomChange, _ ->
                            pan += panChange
                            scale = (scale * zoomChange).coerceIn(0.08f, 3.6f)
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
                drawRect(Color(0xFF071321))
                drawCircle(
                    color = Color(0xFF13243A).copy(alpha = 0.42f),
                    radius = size.maxDimension * 0.68f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )

                graph.lines.forEach { line ->
                    val start = worldToScreen(line.from, origin, scale)
                    val end = worldToScreen(line.to, origin, scale)
                    val side = if (end.x >= start.x) 1f else -1f
                    val dx = abs(end.x - start.x)
                    val control = dx.coerceIn(90f * scale, 260f * scale)
                    val c1 = Offset(start.x + side * control, start.y)
                    val c2 = Offset(end.x - side * control, end.y)
                    val path = Path().apply {
                        moveTo(start.x, start.y)
                        cubicTo(c1.x, c1.y, c2.x, c2.y, end.x, end.y)
                    }
                    drawPath(
                        path = path,
                        color = line.color.copy(alpha = 0.90f),
                        style = Stroke(width = (if (line.side == 0) 4.2f else 3.4f) * scale.coerceIn(0.55f, 1.15f))
                    )
                }

                graph.nodes.forEach { graphNode ->
                    val center = worldToScreen(graphNode.center, origin, scale)
                    val nodeW = graphNode.width * scale
                    val nodeH = graphNode.height * scale
                    val topLeft = Offset(center.x - nodeW / 2f, center.y - nodeH / 2f)
                    val radiusValue = when (graphNode.level) {
                        0 -> 18f
                        1 -> 14f
                        else -> 12f
                    } * scale
                    val radius = CornerRadius(radiusValue, radiusValue)

                    drawRoundRect(
                        color = Color.Black.copy(alpha = if (graphNode.level == 0) 0.24f else 0.13f),
                        topLeft = topLeft + Offset(0f, 7f * scale),
                        size = Size(nodeW, nodeH),
                        cornerRadius = radius
                    )

                    if (graphNode.selected) {
                        drawRoundRect(
                            color = Color(0xFF32F1E5).copy(alpha = 0.90f),
                            topLeft = topLeft - Offset(5f * scale, 5f * scale),
                            size = Size(nodeW + 10f * scale, nodeH + 10f * scale),
                            cornerRadius = CornerRadius(radiusValue + 5f * scale, radiusValue + 5f * scale),
                            style = Stroke(width = 3.3f * scale)
                        )
                    }

                    val fillColor = when (graphNode.level) {
                        0 -> Color.White
                        1 -> graphNode.color.copy(alpha = 0.99f)
                        2 -> graphNode.color.copy(alpha = 0.94f)
                        else -> graphNode.color.copy(alpha = 0.88f)
                    }
                    drawRoundRect(
                        color = fillColor,
                        topLeft = topLeft,
                        size = Size(nodeW, nodeH),
                        cornerRadius = radius
                    )

                    val textSize = graphNode.fontSize * pixelScale * scale
                    val horizontalPadding = graphNode.horizontalPadding * pixelScale * scale
                    val verticalPadding = graphNode.verticalPadding * pixelScale * scale
                    // متن دیگر کلیپ نمی‌شود؛ خود نود بر اساس تعداد خطوط متن اندازه می‌گیرد.
                    // فاصله داخلی نودها تغییر نکرده است.
                    drawContext.canvas.nativeCanvas.drawXMindRtlMultilineText(
                        lines = graphNode.title.wrapNodeTitle(graphNode.level),
                        x = if (graphNode.level == 0) center.x else topLeft.x + nodeW - horizontalPadding,
                        top = topLeft.y + verticalPadding,
                        bottom = topLeft.y + nodeH - verticalPadding,
                        color = graphNode.textColor.toArgb(),
                        textSize = textSize,
                        bold = graphNode.level <= 1,
                        align = if (graphNode.level == 0) Paint.Align.CENTER else Paint.Align.RIGHT
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(7.dp)
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
                // برای جلوگیری از قاطی شدن متن انتخاب با عنوان بالای مایندمپ،
                // انتخاب نود فقط با کادر دور خود نود نمایش داده می‌شود.
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = { scale = 0.54f; pan = Offset.Zero; manualPositions = emptyMap() }) { Text("مرکز") }
                TextButton(onClick = { scale = (scale * 1.15f).coerceAtMost(3.6f) }) { Text("+") }
                TextButton(onClick = { scale = (scale / 1.15f).coerceAtLeast(0.08f) }) { Text("−") }
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
    pixelScale: Float,
    manualPositions: Map<Long, Offset>
): MindGraphLayout {
    val activeNodes = allNodes.filter { it.isActive }
    val children = activeNodes.groupBy { it.parentId }
    val graphNodes = mutableListOf<GraphNode>()

    val palette = listOf(
        Color(0xFFFFA59F), Color(0xFFFFC28A), Color(0xFFAEEAC7), Color(0xFF7EE6DD),
        Color(0xFFA7D6FF), Color(0xFFC9B3FF), Color(0xFFFF9BC1), Color(0xFFFFD36E),
        Color(0xFFB5EAD7), Color(0xFFD9B8FF), Color(0xFFA8D8EA), Color(0xFFFFB4A2)
    )

    val centerStyle = nodeStyle(0)
    val centerWidth = nodeWidth(projectTitle, 0, pixelScale)
    val centerHeight = nodeHeight(projectTitle, 0, pixelScale)
    graphNodes += GraphNode(
        node = null,
        title = projectTitle.ifBlank { "پروژه" },
        center = Offset.Zero,
        width = centerWidth,
        height = centerHeight,
        color = Color.White,
        textColor = Color.Black,
        level = 0,
        side = 0,
        fontSize = centerStyle.fontSize,
        horizontalPadding = centerStyle.horizontalPadding,
        verticalPadding = centerStyle.verticalPadding,
        selected = selectedNodeId == null
    )

    val roots = children[null].orEmpty().sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (roots.isEmpty()) return MindGraphLayout(graphNodes, emptyList())

    val rightRoots = mutableListOf<MindMapNode>()
    val leftRoots = mutableListOf<MindMapNode>()
    var rightWeight = 0f
    var leftWeight = 0f
    roots.forEachIndexed { index, root ->
        val weight = branchBlockHeight(root, 1, children, pixelScale)
        if (index == 0 || rightWeight <= leftWeight) {
            rightRoots += root
            rightWeight += weight
        } else {
            leftRoots += root
            leftWeight += weight
        }
    }

    layoutBalancedRootSide(rightRoots, 1, 0, children, selectedNodeId, palette, graphNodes, centerWidth, pixelScale, manualPositions)
    layoutBalancedRootSide(leftRoots, -1, 1, children, selectedNodeId, palette, graphNodes, centerWidth, pixelScale, manualPositions)

    val byId = graphNodes.mapNotNull { g -> g.node?.id?.let { it to g } }.toMap()
    val lines = mutableListOf<GraphLine>()
    activeNodes.forEach { node ->
        val target = byId[node.id] ?: return@forEach
        val parentGraph = node.parentId?.let { byId[it] } ?: graphNodes.first()
        addGraphLine(
            lines = lines,
            fromCenter = parentGraph.center,
            fromWidth = parentGraph.width,
            fromHeight = parentGraph.height,
            toCenter = target.center,
            toWidth = target.width,
            toHeight = target.height,
            color = rootFamilyColor(node, activeNodes, children, palette),
            lineSide = target.side
        )
    }
    return MindGraphLayout(graphNodes, lines)
}

private data class NodeStyle(val fontSize: Float, val horizontalPadding: Float, val verticalPadding: Float)

private fun nodeStyle(level: Int): NodeStyle = when (level) {
    0 -> NodeStyle(fontSize = 42f, horizontalPadding = 28f, verticalPadding = 20f)
    1 -> NodeStyle(fontSize = 32f, horizontalPadding = 24f, verticalPadding = 16f)
    2 -> NodeStyle(fontSize = 27f, horizontalPadding = 20f, verticalPadding = 13f)
    else -> NodeStyle(fontSize = 23f, horizontalPadding = 18f, verticalPadding = 11f)
}

private fun layoutBalancedRootSide(
    roots: List<MindMapNode>,
    side: Int,
    colorOffset: Int,
    children: Map<Long?, List<MindMapNode>>,
    selectedNodeId: Long?,
    palette: List<Color>,
    graphNodes: MutableList<GraphNode>,
    centerWidth: Float,
    pixelScale: Float,
    manualPositions: Map<Long, Offset>
) {
    if (roots.isEmpty()) return
    val centerGap = 210f * pixelScale
    val totalHeight = roots.sumOf { branchBlockHeight(it, 1, children, pixelScale).toDouble() }.toFloat() +
        (roots.size - 1).coerceAtLeast(0) * rootVerticalGap(pixelScale)
    var cursor = -totalHeight / 2f

    roots.forEachIndexed { index, root ->
        val blockHeight = branchBlockHeight(root, 1, children, pixelScale)
        val rootWidth = nodeWidth(root.title, 1, pixelScale)
        val rootHeight = nodeHeight(root.title, 1, pixelScale)
        val x = side * (centerWidth / 2f + centerGap + rootWidth / 2f)
        val autoPosition = Offset(x, cursor + blockHeight / 2f)
        val position = manualPositions[root.id] ?: autoPosition
        val color = palette[(index * 2 + colorOffset) % palette.size]
        val style = nodeStyle(1)
        graphNodes += GraphNode(root, root.title, position, rootWidth, rootHeight, color, Color.Black, 1, side, style.fontSize, style.horizontalPadding, style.verticalPadding, selectedNodeId == root.id)
        layoutChildrenStrictTree(root, position, rootWidth, side, 2, children, color, selectedNodeId, graphNodes, pixelScale, manualPositions)
        cursor += blockHeight + rootVerticalGap(pixelScale)
    }
}

private fun layoutChildrenStrictTree(
    parent: MindMapNode,
    parentPosition: Offset,
    parentWidth: Float,
    side: Int,
    level: Int,
    children: Map<Long?, List<MindMapNode>>,
    branchColor: Color,
    selectedNodeId: Long?,
    graphNodes: MutableList<GraphNode>,
    pixelScale: Float,
    manualPositions: Map<Long, Offset>
) {
    val directChildren = children[parent.id].orEmpty().filter { it.isActive }.sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    if (directChildren.isEmpty()) return
    val totalHeight = directChildren.sumOf { branchBlockHeight(it, level, children, pixelScale).toDouble() }.toFloat() +
        (directChildren.size - 1).coerceAtLeast(0) * childVerticalGap(level, pixelScale)
    var cursor = parentPosition.y - totalHeight / 2f

    directChildren.forEach { child ->
        val blockHeight = branchBlockHeight(child, level, children, pixelScale)
        val width = nodeWidth(child.title, level, pixelScale)
        val height = nodeHeight(child.title, level, pixelScale)
        val x = parentPosition.x + side * (parentWidth / 2f + horizontalGap(level, pixelScale) + width / 2f)
        val autoPosition = Offset(x, cursor + blockHeight / 2f)
        val position = manualPositions[child.id] ?: autoPosition
        val style = nodeStyle(level)
        graphNodes += GraphNode(
            child,
            child.title,
            position,
            width,
            height,
            branchColor.copy(alpha = when (level) { 2 -> 0.93f; 3 -> 0.84f; else -> 0.74f }),
            Color.Black,
            level,
            side,
            style.fontSize,
            style.horizontalPadding,
            style.verticalPadding,
            selectedNodeId == child.id
        )
        layoutChildrenStrictTree(child, position, width, side, level + 1, children, branchColor, selectedNodeId, graphNodes, pixelScale, manualPositions)
        cursor += blockHeight + childVerticalGap(level, pixelScale)
    }
}

private fun branchBlockHeight(node: MindMapNode, level: Int, children: Map<Long?, List<MindMapNode>>, pixelScale: Float): Float {
    val own = nodeHeight(node.title, level, pixelScale)
    val directChildren = children[node.id].orEmpty().filter { it.isActive }
    if (directChildren.isEmpty()) return own
    val kids = directChildren.sumOf { branchBlockHeight(it, level + 1, children, pixelScale).toDouble() }.toFloat() +
        (directChildren.size - 1).coerceAtLeast(0) * childVerticalGap(level + 1, pixelScale)
    return max(own, kids)
}

private fun rootVerticalGap(pixelScale: Float): Float = 74f * pixelScale
private fun childVerticalGap(level: Int, pixelScale: Float): Float = (when (level) { 2 -> 34f; 3 -> 26f; else -> 22f }) * pixelScale
private fun horizontalGap(level: Int, pixelScale: Float): Float = (when (level) { 2 -> 150f; 3 -> 120f; else -> 96f }) * pixelScale

private fun addGraphLine(lines: MutableList<GraphLine>, fromCenter: Offset, fromWidth: Float, fromHeight: Float, toCenter: Offset, toWidth: Float, toHeight: Float, color: Color, lineSide: Int) {
    val start = edgePoint(fromCenter, fromWidth, fromHeight, toCenter)
    val end = edgePoint(toCenter, toWidth, toHeight, fromCenter)
    lines += GraphLine(from = start, to = end, color = color, side = lineSide, angle = 0f)
}

private fun edgePoint(center: Offset, width: Float, height: Float, toward: Offset): Offset {
    val direction = (toward - center).normalizedOr(Offset(1f, 0f))
    val tx = if (abs(direction.x) < 0.001f) Float.MAX_VALUE else (width / 2f) / abs(direction.x)
    val ty = if (abs(direction.y) < 0.001f) Float.MAX_VALUE else (height / 2f) / abs(direction.y)
    return center + direction * min(tx, ty)
}

private fun rootFamilyColor(node: MindMapNode, activeNodes: List<MindMapNode>, children: Map<Long?, List<MindMapNode>>, palette: List<Color>): Color {
    val byId = activeNodes.associateBy { it.id }
    var cursor = node
    while (cursor.parentId != null) cursor = byId[cursor.parentId] ?: break
    val roots = children[null].orEmpty().filter { it.isActive }.sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
    val index = roots.indexOfFirst { it.id == cursor.id }.coerceAtLeast(0)
    return palette[index % palette.size]
}

private fun nodeWidth(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level)
    val longest = lines.maxOfOrNull { it.length } ?: 6
    val style = nodeStyle(level)
    val textWidth = longest * style.fontSize * 0.58f
    val minWidth = when (level) { 0 -> 230f; 1 -> 168f; 2 -> 142f; else -> 122f }
    val maxWidth = when (level) { 0 -> 560f; 1 -> 430f; 2 -> 340f; else -> 290f }
    return (textWidth + style.horizontalPadding * 2f).coerceIn(minWidth, maxWidth) * pixelScale
}

private fun nodeHeight(title: String, level: Int, pixelScale: Float): Float {
    val lines = title.wrapNodeTitle(level).size.coerceAtLeast(1)
    val style = nodeStyle(level)
    val minHeight = when (level) { 0 -> 112f; 1 -> 82f; 2 -> 66f; else -> 56f }
    return (lines * style.fontSize * 1.62f + style.verticalPadding * 2.4f).coerceAtLeast(minHeight) * pixelScale
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
    val maxWordsPerLine = when (level) { 0 -> 7; 1 -> 7; 2 -> 6; else -> 6 }
    val maxCharsPerLine = when (level) { 0 -> 44; 1 -> 38; 2 -> 32; else -> 28 }
    val words = trim().toPersianDigits().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return listOf("بدون عنوان")
    val lines = mutableListOf<String>()
    var current = mutableListOf<String>()
    fun currentText(): String = current.joinToString(" ")
    fun flush() { if (current.isNotEmpty()) { lines += currentText(); current = mutableListOf() } }
    words.forEach { raw ->
        val pieces = if (raw.length > maxCharsPerLine) raw.chunked(maxCharsPerLine) else listOf(raw)
        pieces.forEach { word ->
            if (current.isEmpty()) current += word else {
                val candidate = currentText() + " " + word
                if (current.size < maxWordsPerLine && candidate.length <= maxCharsPerLine) current += word else { flush(); current += word }
            }
        }
    }
    flush()
    return lines
}

private fun android.graphics.Canvas.drawXMindRtlMultilineText(
    lines: List<String>,
    x: Float,
    top: Float,
    bottom: Float,
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
    val fm = paint.fontMetrics
    // از top/bottom واقعی فونت استفاده می‌کنیم تا بالای حروف فارسی در خط اول بریده نشود.
    val glyphHeight = fm.bottom - fm.top
    val lineHeight = glyphHeight * 1.08f
    val contentHeight = glyphHeight + (lines.size - 1).coerceAtLeast(0) * lineHeight
    val available = (bottom - top).coerceAtLeast(contentHeight)
    var baseline = top + (available - contentHeight).coerceAtLeast(0f) / 2f - fm.top
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
