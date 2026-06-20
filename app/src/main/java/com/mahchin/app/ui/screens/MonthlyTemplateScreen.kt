package com.mahchin.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.components.TaskEditorDialog
import com.mahchin.app.ui.viewmodel.MainViewModel

@Composable
fun MonthlyTemplateScreen(vm: MainViewModel) {
    val templates by vm.templates.collectAsState()
    var addDialog by remember { mutableStateOf(false) }
    var editTemplate by remember { mutableStateOf<MonthlyTemplateTask?>(null) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("قالب ماهانه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("برنامه‌ای که اینجا می‌چینی، هر ماه تکرار می‌شود.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        Button(onClick = { addDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("افزودن تسک ثابت ماهانه") }
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (templates.isEmpty()) item { Text("هنوز قالبی تعریف نشده. مثلاً روز ۱۰ هر ماه: بررسی فروش.") }
            items(templates, key = { it.id }) { item ->
                TemplateCard(
                    template = item,
                    onEdit = { editTemplate = item },
                    onDelete = { vm.deleteTemplate(item.id) }
                )
            }
        }
    }

    if (addDialog) {
        TaskEditorDialog(
            titleText = "تسک ثابت ماهانه",
            dayOfMonth = 1,
            onDismiss = { addDialog = false },
            onSave = { title, desc, day, priority ->
                vm.addTemplateTask(title, desc, day ?: 1, priority)
                addDialog = false
            }
        )
    }

    editTemplate?.let { t ->
        TaskEditorDialog(
            titleText = "ویرایش همه ماه‌ها",
            initialTitle = t.title,
            initialDescription = t.description,
            initialPriority = t.priority,
            dayOfMonth = t.dayOfMonth,
            onDismiss = { editTemplate = null },
            onSave = { title, desc, day, priority ->
                vm.updateTemplate(t.id, title, desc, day ?: t.dayOfMonth, priority)
                editTemplate = null
            }
        )
    }
}

@Composable
private fun TemplateCard(template: MonthlyTemplateTask, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(template.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (template.description.isNotBlank()) Text(template.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AssistChip(onClick = {}, label = { Text("روز ${template.dayOfMonth.toPersianDigits()}") })
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(template.priority.fa) })
                AssistChip(onClick = {}, label = { Text("هر ماه تکرار می‌شود") })
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onEdit, modifier = Modifier.weight(1f)) { Text("ویرایش همه ماه‌ها") }
                OutlinedButton(onClick = onDelete, modifier = Modifier.weight(1f)) { Text("حذف همه ماه‌ها") }
            }
        }
    }
}
