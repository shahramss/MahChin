package com.mahchin.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.ReminderIntensity
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.data.model.TaskStatus
import com.mahchin.app.data.model.UserSettings
import com.mahchin.app.data.repository.MonthlyReport
import com.mahchin.app.data.repository.TaskRepository
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import com.mahchin.app.notification.NotificationHelper
import com.mahchin.app.notification.ReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val app: Application,
    private val repository: TaskRepository
) : AndroidViewModel(app) {

    val today: JalaliDate = JalaliCalendar.today()

    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<JalaliDate> = _selectedDate

    private val _calendarMonth = MutableStateFlow(JalaliDate(today.year, today.month, 1))
    val calendarMonth: StateFlow<JalaliDate> = _calendarMonth

    private val _settingsMessage = MutableStateFlow<String?>(null)
    val settingsMessage: StateFlow<String?> = _settingsMessage

    val settings: StateFlow<UserSettings> = repository.settingsFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        UserSettings()
    )

    val templates: StateFlow<List<MonthlyTemplateTask>> = repository.templatesFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayTasks: StateFlow<List<TaskItem>> = MutableStateFlow(today).flatMapLatest {
        repository.observeTasksForDate(today)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val selectedDateTasks: StateFlow<List<TaskItem>> = selectedDate.flatMapLatest { date ->
        repository.observeTasksForDate(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthCounts: StateFlow<Map<Int, Int>> = calendarMonth.flatMapLatest { month ->
        repository.observeMonthCounts(month.year, month.month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    @OptIn(ExperimentalCoroutinesApi::class)
    val report: StateFlow<MonthlyReport> = calendarMonth.flatMapLatest { month ->
        repository.observeReport(month.year, month.month)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthlyReport(0, 0, 0, 0, 0))

    init {
        viewModelScope.launch {
            repository.ensureDefaultSettings()
            repository.ensureTasksForDate(today)
            repository.ensureTasksForMonth(today.year, today.month)
            ReminderScheduler.schedulePeriodic(app, repository.getSettingsOrDefault())
        }
    }

    fun selectDate(date: JalaliDate) {
        _selectedDate.value = date
        viewModelScope.launch { repository.ensureTasksForDate(date) }
    }

    fun nextMonth() {
        val next = JalaliCalendar.nextMonth(_calendarMonth.value)
        _calendarMonth.value = next
        viewModelScope.launch { repository.ensureTasksForMonth(next.year, next.month) }
    }

    fun previousMonth() {
        val prev = JalaliCalendar.previousMonth(_calendarMonth.value)
        _calendarMonth.value = prev
        viewModelScope.launch { repository.ensureTasksForMonth(prev.year, prev.month) }
    }

    fun addTodayTask(title: String, description: String, priority: TaskPriority) = addOneTimeTask(today, title, description, priority)

    fun addOneTimeTask(date: JalaliDate, title: String, description: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addOneTimeTask(date, title, description, priority)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun addTemplateTask(title: String, description: String, dayOfMonth: Int, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addTemplateTask(title, description, dayOfMonth, priority)
            repository.ensureTasksForDate(today)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun updateTemplate(id: Long, title: String, description: String, dayOfMonth: Int, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch { repository.updateTemplateTask(id, title, description, dayOfMonth, priority) }
    }

    fun deleteTemplate(id: Long) {
        viewModelScope.launch { repository.deleteTemplateTask(id) }
    }

    fun complete(item: TaskItem) = setStatus(item, TaskStatus.DONE)
    fun toggleDone(item: TaskItem) = setStatus(
        item,
        if (item.status == TaskStatus.DONE) TaskStatus.NOT_STARTED else TaskStatus.DONE
    )
    fun resetStatus(item: TaskItem) = setStatus(item, TaskStatus.NOT_STARTED)
    fun inProgress(item: TaskItem) = setStatus(item, TaskStatus.IN_PROGRESS)
    fun cancelToday(item: TaskItem) = setStatus(item, TaskStatus.CANCELED)

    fun toggleTemplateDone(template: MonthlyTemplateTask) {
        val next = if (template.status == TaskStatus.DONE) TaskStatus.NOT_STARTED else TaskStatus.DONE
        viewModelScope.launch { repository.setTemplateStatus(template.id, next) }
    }

    fun setStatus(item: TaskItem, status: TaskStatus) {
        viewModelScope.launch {
            repository.setStatus(item, status)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun editOnlyThisDate(item: TaskItem, title: String, description: String, priority: TaskPriority) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.editTaskOnlyThisDate(item, title, description, priority)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun deleteTask(item: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(item)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun moveToTomorrow(item: TaskItem) {
        viewModelScope.launch {
            repository.moveTaskToTomorrow(item)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun moveToCustomDate(item: TaskItem, date: JalaliDate) {
        viewModelScope.launch {
            repository.moveTaskToDate(item, date)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun moveAllRemainingTodayToTomorrow() {
        viewModelScope.launch {
            repository.moveRemainingToTomorrow(today)
            ReminderScheduler.scheduleImmediateCheck(app)
        }
    }

    fun saveReminderSettings(startHour: Int, endHour: Int) {
        val start = startHour.coerceIn(0, 23)
        val end = endHour.coerceIn(1, 24).coerceAtLeast((start + 1).coerceAtMost(24))
        updateSettings { it.copy(startHour = start, endHour = end) }
        _settingsMessage.value = "تنظیمات یادآوری ذخیره شد و زمان‌بندی دوباره فعال شد."
    }

    fun sendTestNotification() {
        val ok = NotificationHelper.showTestNotification(app)
        _settingsMessage.value = if (ok) {
            "نوتیفیکیشن آزمایشی ارسال شد."
        } else {
            "مجوز نوتیفیکیشن فعال نیست. از تنظیمات گوشی اجازه Notification را بده."
        }
    }

    fun clearSettingsMessage() {
        _settingsMessage.value = null
    }

    fun updateSettings(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch {
            val newSettings = transform(settings.value)
            repository.updateSettings(newSettings)
            ReminderScheduler.schedulePeriodic(app, newSettings)
        }
    }
}

class MainViewModelFactory(
    private val app: Application,
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(app, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
