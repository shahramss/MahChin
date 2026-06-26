package com.mahchin.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.data.model.Project
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.domain.toEnglishDigits
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.viewmodel.MainViewModel

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        Text("مایندمپ پروژه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "هدف را خرد کن؛ زیرمجموعه‌ها بعداً با تعداد مشخص در روز به تسک تبدیل می‌شوند.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(expanded = projectMenu, onExpandedChange = { projectMenu = !projectMenu }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = selectedProject?.name ?: "پروژه",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("پروژه") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(projectMenu) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = projectMenu, onDismissRequest = { projectMenu = false }) {
                    projects.forEach { p ->
                        DropdownMenuItem(text = { Text(p.name) }, onClick = { vm.selectProject(p.id); projectMenu = false })
                    }
                }
            }
            OutlinedButton(onClick = { addProjectDialog = true }, modifier = Modifier.height(56.dp)) { Text("پروژه +") }
        }

        if (nodes.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("از یک هدف اصلی شروع کن", fontWeight = FontWeight.Bold)
                    Text("مثلاً: سایت X، بعد زیرش طراحی، محتوا، سئو و...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 86.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(flattenNodes(nodes), key = { it.node.id }) { item ->
                MindMapNodeCard(
                    node = item.node,
                    level = item.level,
                    onAddChild = { nodeDialog = NodeDialogState(parentId = item.node.id) },
                    onEdit = { nodeDialog = NodeDialogState(editNode = item.node, parentId = item.node.parentId) },
                    onDelete = { vm.deleteMindMapNode(item.node.id) }
                )
            }
        }
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
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(6.dp))
                Text("تقسیم به تسک")
            }
            Button(
                onClick = { nodeDialog = NodeDialogState(parentId = null) },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Outlined.Add, null, modifier = Modifier.size(19.dp))
                Spacer(Modifier.size(6.dp))
                Text("هدف اصلی")
            }
        }
    }

    if (addProjectDialog) {
        AddProjectDialog(onDismiss = { addProjectDialog = false }, onSave = { vm.addProject(it); addProjectDialog = false })
    }

    nodeDialog?.let { state ->
        NodeEditorDialog(
            state = state,
            onDismiss = { nodeDialog = null },
            onSave = { title, desc ->
                if (state.editNode == null) vm.addMindMapNode(state.parentId, title, desc) else vm.updateMindMapNode(state.editNode.id, title, desc)
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
}

data class NodeDialogState(val editNode: MindMapNode? = null, val parentId: Long? = null)
private data class VisibleNode(val node: MindMapNode, val level: Int)

private fun flattenNodes(nodes: List<MindMapNode>): List<VisibleNode> {
    val children = nodes.groupBy { it.parentId }
    val result = mutableListOf<VisibleNode>()
    fun walk(parentId: Long?, level: Int) {
        children[parentId].orEmpty().forEach { node ->
            result += VisibleNode(node, level)
            walk(node.id, level + 1)
        }
    }
    walk(null, 0)
    return result
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MindMapNodeCard(
    node: MindMapNode,
    level: Int,
    onAddChild: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (level * 18).dp)
            .combinedClickable(onClick = {}, onLongClick = { showActions = true }),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(9.dp).background(if (level == 0) MaterialTheme.colorScheme.primary else Color(0xFF7DD3FC), RoundedCornerShape(99.dp)))
            Column(Modifier.weight(1f)) {
                Text(node.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                if (node.description.isNotBlank()) Text(node.description, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            IconButton(onClick = { showActions = true }, modifier = Modifier.size(34.dp)) {
                Icon(Icons.Outlined.MoreVert, contentDescription = null)
            }
        }
    }

    if (showActions) {
        ModalBottomSheet(onDismissRequest = { showActions = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(node.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("زیرمجموعه‌ها بعداً به تسک‌های پروژه تبدیل می‌شوند.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                MindActionRow("افزودن زیرمجموعه", Icons.Outlined.Add) { showActions = false; onAddChild() }
                MindActionRow("ویرایش", Icons.Outlined.Edit) { showActions = false; onEdit() }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f))
                MindActionRow("حذف", Icons.Outlined.Delete, danger = true) { showActions = false; onDelete() }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun MindActionRow(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean = false, onClick: () -> Unit) {
    val color = if (danger) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
        text = { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("نام پروژه") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = { onSave(title) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun NodeEditorDialog(state: NodeDialogState, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf(state.editNode?.title.orEmpty()) }
    var desc by remember { mutableStateOf(state.editNode?.description.orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (state.editNode == null) "گره جدید" else "ویرایش گره") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("توضیح کوتاه") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = { Button(onClick = { onSave(title, desc) }) { Text("ذخیره") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun DistributeMindMapDialog(initialDate: JalaliDate, onDismiss: () -> Unit, onSave: (JalaliDate, Int) -> Unit) {
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
                Text("زیرمجموعه‌های نهایی به ترتیب، روزی چند تا در تقویم پخش شوند؟")
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
                val y = year.toIntOrNull(); val m = month.toIntOrNull(); val d = day.toIntOrNull(); val p = perDay.toIntOrNull()
                if (y == null || m == null || d == null || p == null) error = "اطلاعات را کامل وارد کن."
                else {
                    try {
                        val safeDay = d.coerceAtMost(JalaliCalendar.monthLength(y, m))
                        onSave(JalaliDate(y, m, safeDay), p)
                    } catch (e: Exception) { error = e.message ?: "تاریخ نامعتبر است." }
                }
            }) { Text("ساخت تسک‌ها") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun SmallNumberField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier) {
    OutlinedTextField(
        value = value.toPersianDigits(),
        onValueChange = { onValueChange(it.toEnglishDigits().filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}
