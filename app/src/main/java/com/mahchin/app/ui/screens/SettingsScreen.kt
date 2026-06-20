package com.mahchin.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mahchin.app.data.model.ReminderIntensity
import com.mahchin.app.domain.toEnglishDigits
import com.mahchin.app.domain.toPersianDigits
import com.mahchin.app.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel) {
    val settings by vm.settings.collectAsState()
    var expanded by remember { mutableStateOf(false) }
    var startHour by remember(settings.startHour) { mutableStateOf(settings.startHour.toString()) }
    var endHour by remember(settings.endHour) { mutableStateOf(settings.endHour.toString()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("تنظیمات", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("یادآوری", fontWeight = FontWeight.Bold)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(
                        value = settings.reminderIntensity.fa,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("شدت یادآوری") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        ReminderIntensity.entries.forEach { intensity ->
                            DropdownMenuItem(text = { Text(intensity.fa) }, onClick = {
                                vm.updateSettings { it.copy(reminderIntensity = intensity) }
                                expanded = false
                            })
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = startHour.toPersianDigits(),
                        onValueChange = { startHour = it.toEnglishDigits().filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("شروع") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endHour.toPersianDigits(),
                        onValueChange = { endHour = it.toEnglishDigits().filter { ch -> ch.isDigit() }.take(2) },
                        label = { Text("پایان") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Button(onClick = {
                    val s = startHour.toIntOrNull()?.coerceIn(0, 23) ?: 8
                    val e = endHour.toIntOrNull()?.coerceIn(1, 24) ?: 22
                    vm.updateSettings { it.copy(startHour = s, endHour = e.coerceAtLeast(s + 1).coerceAtMost(24)) }
                }, modifier = Modifier.fillMaxWidth()) { Text("ذخیره ساعت یادآوری") }
            }
        }

        SettingSwitch("فعال‌سازی صدا", settings.soundEnabled) { checked -> vm.updateSettings { it.copy(soundEnabled = checked) } }
        SettingSwitch("فعال‌سازی ویبره", settings.vibrationEnabled) { checked -> vm.updateSettings { it.copy(vibrationEnabled = checked) } }
        SettingSwitch("حالت تاریک", settings.darkMode) { checked -> vm.updateSettings { it.copy(darkMode = checked) } }
        SettingSwitch("بکاپ خودکار اندروید", settings.backupEnabled) { checked -> vm.updateSettings { it.copy(backupEnabled = checked) } }

        Text(
            "بکاپ ساده از طریق Auto Backup اندروید برای دیتابیس فعال شده است. برای نسخه بعدی می‌شود خروجی JSON/فایل دستی هم اضافه کرد.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingSwitch(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
