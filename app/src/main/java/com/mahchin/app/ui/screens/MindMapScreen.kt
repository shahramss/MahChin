package com.mahchin.app.ui.screens

import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.domain.toEnglishDigits
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.viewmodel.MainViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    var nodeDialog by remember { mutableStateOf<NodeDialogState?>(null) }
    var distributeDialog by remember { mutableStateOf(false) }
    var actionNode by remember { mutableStateOf<MindMapNode?>(null) }
    var centerActions by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "مایندمپ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "مثل نقشه ذهنی بساز؛ شاخه‌های نهایی بعداً به تسک‌های روزانه تبدیل می‌شوند.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )

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
                                    projectMenu = false
                                }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { addProjectDialog = true },
                    modifier = Modifier.height(56.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("پروژه +") }
            }

            MindMapCanvasCard(
                projectTitle = selectedProject?.name ?: "پروژه",
                nodes = nodes,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onCenterLongPress = { centerActions = true },
                onNodeLongPress = { actionNode = it },
                onEmptyMapClick = { nodeDialog = NodeDialogState(parentId = null) }
            )

            Text(
                "راهنما: روی نود نگه دار تا زیرشاخه، ویرایش یا حذف باز شود. روی فضای خالی یا دکمه پایین برای افزودن شاخه اصلی بزن.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { distributeDialog = true },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Text("تقسیم به تسک")
            }
            Button(
                onClick = { nodeDialog = NodeDialogState(parentId = null) },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("شاخه اصلی")
            }
        }
    }

    if (addProjectDialog) {
        AddProjectDialog(
            onDismiss = { addProjectDialog = false },
            onSave = {
                vm.addProject(it)
                addProjectDialog = false
            }
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

    if (centerActions) {
        MindMapActionSheet(
            title = selectedProject?.name ?: "پروژه",
            subtitle = "این نود مرکزی پروژه است.",
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
                    nodeDialog = NodeDialogState(parentId = node.id)
                },
                MindAction("ویرایش", Icons.Outlined.Edit) {
                    actionNode = null
                    nodeDialog = NodeDialogState(editNode = node, parentId = node.parentId)
                },
                MindAction("حذف", Icons.Outlined.Delete, danger = true) {
                    actionNode = null
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
    val position: Offset,
    val radius: Float,
    val color: Color,
    val textColor: Color,
    val level: Int
)

private data class GraphLine(val from: Offset, val to: Offset, val color: Color)

private data class MindGraphLayout(val nodes: List<GraphNode>, val lines: List<GraphLine>)

@Composable
private fun MindMapCanvasCard(
    projectTitle: String,
    nodes: List<MindMapNode>,
    modifier: Modifier,
    onCenterLongPress: () -> Unit,
    onNodeLongPress: (MindMapNode) -> Unit,
    onEmptyMapClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val outline = MaterialTheme.colorScheme.outline
    val onSurface = MaterialTheme.colorScheme.onSurface
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val density = LocalDensity.current

    Card(
        modifier = modifier.padding(bottom = 70.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = surface),
        border = BorderStroke(1.dp, outline.copy(alpha = 0.18f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = with(density) { maxWidth.toPx() }
            val h = with(density) { maxHeight.toPx() }
            val graph = remember(projectTitle, nodes, w, h, primary, onSurface, onPrimary) {
                buildMindGraph(projectTitle, nodes, w, h, primary, onSurface, onPrimary)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(graph) {
                        detectTapGestures(
                            onTap = { tap ->
                                val hit = graph.nodes.asReversed().firstOrNull { tap.distanceTo(it.position) <= it.radius + 10f }
                                if (hit == null) onEmptyMapClick()
                            },
                            onLongPress = { tap ->
                                val hit = graph.nodes.asReversed().firstOrNull { tap.distanceTo(it.position) <= it.radius + 14f }
                                when {
                                    hit?.node != null -> onNodeLongPress(hit.node)
                                    hit != null -> onCenterLongPress()
                                    else -> onEmptyMapClick()
                                }
                            }
                        )
                    }
            ) {
                graph.lines.forEach { line ->
                    drawLine(
                        color = line.color.copy(alpha = 0.78f),
                        start = line.from,
                        end = line.to,
                        strokeWidth = 4.2f
                    )
                }

                graph.nodes.forEach { graphNode ->
                    drawCircle(
                        color = graphNode.color.copy(alpha = if (graphNode.level == 0) 0.22f else 0.12f),
                        radius = graphNode.radius + 9f,
                        center = graphNode.position,
                        style = Stroke(width = 2.2f)
                    )
                    drawCircle(
                        color = graphNode.color,
                        radius = graphNode.radius,
                        center = graphNode.position
                    )
                    drawContext.canvas.nativeCanvas.drawCenteredText(
                        text = graphNode.title.cleanNodeTitle(graphNode.level),
                        x = graphNode.position.x,
                        y = graphNode.position.y,
                        color = graphNode.textColor.toArgb(),
                        textSize = when (graphNode.level) {
                            0 -> 38f
                            1 -> 30f
                            else -> 24f
                        },
                        bold = graphNode.level <= 1
                    )
                }
            }

            if (nodes.isEmpty()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("هنوز شاخه‌ای نداری", fontWeight = FontWeight.Bold)
                    Text(
                        "روی نود وسط نگه دار یا دکمه «شاخه اصلی» را بزن.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

private fun buildMindGraph(
    projectTitle: String,
    allNodes: List<MindMapNode>,
    width: Float,
    height: Float,
    primary: Color,
    onSurface: Color,
    onPrimary: Color
): MindGraphLayout {
    val safeWidth = width.coerceAtLeast(320f)
    val safeHeight = height.coerceAtLeast(360f)
    val center = Offset(safeWidth / 2f, safeHeight / 2f)
    val children = allNodes.filter { it.isActive }.groupBy { it.parentId }
    val graphNodes = mutableListOf<GraphNode>()
    val lines = mutableListOf<GraphLine>()
    val palette = listOf(
        Color(0xFF00D1FF),
        Color(0xFF22C55E),
        Color(0xFFFFB020),
        Color(0xFFFF3D8A),
        Color(0xFF8B5CF6),
        Color(0xFF14F195),
        Color(0xFFFF5C35),
        Color(0xFF2DD4BF),
        Color(0xFF6366F1),
        Color(0xFFEAB308),
        Color(0xFF06B6D4),
        Color(0xFFEF4444)
    )

    graphNodes += GraphNode(
        node = null,
        title = projectTitle.ifBlank { "پروژه" },
        position = center,
        radius = min(safeWidth, safeHeight) * 0.12f,
        color = primary,
        textColor = onPrimary,
        level = 0
    )

    val rootNodes = children[null].orEmpty()
    val rootCount = max(rootNodes.size, 1)
    val crowded = rootNodes.size >= 7
    val rootDistance = min(safeWidth, safeHeight) * if (crowded) 0.34f else 0.31f
    val rootRadius = if (crowded) 36f else 42f
    rootNodes.forEachIndexed { index, node ->
        val angle = (-PI / 2.0) + (2.0 * PI * index / rootCount)
        val pos = clampOffset(
            center + Offset((cos(angle) * rootDistance).toFloat(), (sin(angle) * rootDistance).toFloat()),
            safeWidth,
            safeHeight,
            46f
        )
        val color = palette[index % palette.size]
        graphNodes += GraphNode(node, node.title, pos, rootRadius, color, Color.White, level = 1)
        lines += GraphLine(center, pos, color)
        addChildGraphNodes(
            parent = node,
            parentPosition = pos,
            parentAngle = angle,
            level = 2,
            children = children,
            width = safeWidth,
            height = safeHeight,
            branchColor = color,
            graphNodes = graphNodes,
            lines = lines,
            textColor = onSurface
        )
    }

    return MindGraphLayout(graphNodes, lines)
}

private fun addChildGraphNodes(
    parent: MindMapNode,
    parentPosition: Offset,
    parentAngle: Double,
    level: Int,
    children: Map<Long?, List<MindMapNode>>,
    width: Float,
    height: Float,
    branchColor: Color,
    graphNodes: MutableList<GraphNode>,
    lines: MutableList<GraphLine>,
    textColor: Color
) {
    val directChildren = children[parent.id].orEmpty()
    if (directChildren.isEmpty() || level > 4) return

    val spread = if (directChildren.size == 1) 0.0 else PI / (1.8 + level)
    val start = parentAngle - spread / 2.0
    val step = if (directChildren.size == 1) 0.0 else spread / (directChildren.size - 1)
    val distance = when (level) {
        2 -> 105f
        3 -> 82f
        else -> 68f
    }
    val radius = when (level) {
        2 -> 31f
        3 -> 25f
        else -> 21f
    }

    directChildren.forEachIndexed { index, child ->
        val angle = start + step * index
        val raw = parentPosition + Offset((cos(angle) * distance).toFloat(), (sin(angle) * distance).toFloat())
        val pos = clampOffset(raw, width, height, radius + 10f)
        val color = when (level) {
            2 -> branchColor.copy(alpha = 0.94f)
            3 -> branchColor.copy(alpha = 0.78f)
            else -> branchColor.copy(alpha = 0.62f)
        }
        val childTextColor = Color.White
        graphNodes += GraphNode(child, child.title, pos, radius, color, childTextColor, level)
        lines += GraphLine(parentPosition, pos, branchColor)
        addChildGraphNodes(child, pos, angle, level + 1, children, width, height, branchColor, graphNodes, lines, textColor)
    }
}

private operator fun Offset.plus(other: Offset): Offset = Offset(x + other.x, y + other.y)

private fun Offset.distanceTo(other: Offset): Float {
    return sqrt((x - other.x).pow(2) + (y - other.y).pow(2))
}

private fun clampOffset(offset: Offset, width: Float, height: Float, margin: Float): Offset {
    return Offset(
        x = offset.x.coerceIn(margin, width - margin),
        y = offset.y.coerceIn(margin, height - margin)
    )
}

private fun String.cleanNodeTitle(level: Int): String {
    val maxChars = when (level) {
        0 -> 18
        1 -> 14
        2 -> 12
        else -> 10
    }
    return trim().toPersianDigits().let { if (it.length > maxChars) it.take(maxChars - 1) + "…" else it }
}

private fun android.graphics.Canvas.drawCenteredText(
    text: String,
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
    val baseline = y - (paint.descent() + paint.ascent()) / 2f
    drawText(text, x, baseline, paint)
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
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
private fun AddProjectDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("پروژه جدید") },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("نام پروژه") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = { Button(onClick = { onSave(title) }) { Text("ذخیره") } },
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
        title = { Text(if (state.editNode == null) "نود جدید" else "ویرایش نود") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیح کوتاه") },
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
                Text("زیرشاخه‌های نهایی به ترتیب، روزی چند تا در تقویم پخش شوند؟")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SmallNumberField("سال", year, { year = it }, Modifier.weight(1f))
                    SmallNumberField("ماه", month, { month = it }, Modifier.weight(1f))
                    SmallNumberField("روز", day, { day = it }, Modifier.weight(1f))
                }
                SmallNumberField("تعداد تسک در روز", perDay, { perDay = it }, Modifier.fillMaxWidth())
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
