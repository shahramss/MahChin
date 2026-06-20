package com.mahchin.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.domain.toEnglishDigits
import com.mahchin.app.domain.toPersianDigits

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorDialog(
    titleText: String,
    initialTitle: String = "",
    initialDescription: String = "",
    initialPriority: TaskPriority = TaskPriority.NORMAL,
    dayOfMonth: Int? = null,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, day: Int?, priority: TaskPriority) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var desc by remember { mutableStateOf(initialDescription) }
    var day by remember { mutableStateOf(dayOfMonth?.toString() ?: "") }
    var priority by remember { mutableStateOf(initialPriority) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titleText) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان تسک") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("توضیحات") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (dayOfMonth != null) {
                    OutlinedTextField(
                        value = day.toPersianDigits(),
                        onValueChange = { day = it.toEnglishDigits().filter { ch -> ch.isDigit() } },
                        label = { Text("روز ماه؛ ۱ تا ۳۱") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = priority.fa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("اولویت") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TaskPriority.entries.forEach { p ->
                            DropdownMenuItem(text = { Text(p.fa) }, onClick = { priority = p; expanded = false })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, desc, day.toIntOrNull(), priority) }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
fun JalaliDateDialog(
    initialDate: JalaliDate,
    onDismiss: () -> Unit,
    onSave: (JalaliDate) -> Unit
) {
    var year by remember { mutableStateOf(initialDate.year.toString()) }
    var month by remember { mutableStateOf(initialDate.month.toString()) }
    var day by remember { mutableStateOf(initialDate.day.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("انتخاب تاریخ شمسی") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("سال‌های مجاز: ۱۴۰۵ تا ۱۵۰۰")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    DateField("سال", year, { year = it }, Modifier.weight(1f))
                    DateField("ماه", month, { month = it }, Modifier.weight(1f))
                    DateField("روز", day, { day = it }, Modifier.weight(1f))
                }
                error?.let { Text(it, color = androidx.compose.material3.MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = {
                val y = year.toIntOrNull()
                val m = month.toIntOrNull()
                val d = day.toIntOrNull()
                if (y == null || m == null || d == null) {
                    error = "تاریخ را کامل وارد کن."
                } else {
                    try {
                        val safeDay = d.coerceAtMost(JalaliCalendar.monthLength(y, m))
                        onSave(JalaliDate(y, m, safeDay))
                    } catch (e: Exception) {
                        error = e.message ?: "تاریخ نامعتبر است."
                    }
                }
            }) { Text("تأیید") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun DateField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value.toPersianDigits(),
        onValueChange = { onValueChange(it.toEnglishDigits().filter { ch -> ch.isDigit() }) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier.padding(top = 4.dp)
    )
}
