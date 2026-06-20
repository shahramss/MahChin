package com.mahchin.app.data.repository

import com.mahchin.app.data.dao.TaskDao
import com.mahchin.app.data.model.DailyTaskInstance
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.OneTimeTask
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskOrigin
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.data.model.TaskStatus
import com.mahchin.app.data.model.TaskType
import com.mahchin.app.data.model.UserSettings
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepository(private val dao: TaskDao) {

    val settingsFlow: Flow<UserSettings> = dao.observeSettings().map { it ?: UserSettings() }
    val templatesFlow: Flow<List<MonthlyTemplateTask>> = dao.observeTemplates()

    suspend fun getSettingsOrDefault(): UserSettings = withContext(Dispatchers.IO) {
        dao.getSettings() ?: UserSettings().also { dao.upsertSettings(it) }
    }

    suspend fun ensureDefaultSettings() = withContext(Dispatchers.IO) {
        if (dao.getSettings() == null) dao.upsertSettings(UserSettings())
    }

    suspend fun updateSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        dao.upsertSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addTemplateTask(title: String, description: String, dayOfMonth: Int, priority: TaskPriority) = withContext(Dispatchers.IO) {
        dao.insertTemplate(
            MonthlyTemplateTask(
                title = title.trim(),
                description = description.trim(),
                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                priority = priority
            )
        )
    }

    suspend fun updateTemplateTask(id: Long, title: String, description: String, dayOfMonth: Int, priority: TaskPriority) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let {
            dao.updateTemplate(
                it.copy(
                    title = title.trim(),
                    description = description.trim(),
                    dayOfMonth = dayOfMonth.coerceIn(1, 31),
                    priority = priority,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteTemplateTask(id: Long) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let { dao.updateTemplate(it.copy(isActive = false, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun setTemplateStatus(id: Long, status: TaskStatus) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let {
            dao.updateTemplate(it.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun addOneTimeTask(date: JalaliDate, title: String, description: String, priority: TaskPriority) = withContext(Dispatchers.IO) {
        dao.insertOneTimeTask(
            OneTimeTask(
                title = title.trim(),
                description = description.trim(),
                dayOfMonth = date.day,
                jalaliYear = date.year,
                jalaliMonth = date.month,
                jalaliDay = date.day,
                priority = priority
            )
        )
    }

    suspend fun ensureTasksForDate(date: JalaliDate) = withContext(Dispatchers.IO) {
        val templates = dao.getActiveTemplates()
        val monthLength = JalaliCalendar.monthLength(date.year, date.month)
        templates
            .filter { JalaliCalendar.normalizeDayForMonth(it.dayOfMonth, date.year, date.month) == date.day }
            .forEach { template ->
                if (dao.getDailyInstanceBySource(template.id, date.year, date.month, date.day) == null) {
                    dao.insertDailyInstance(
                        DailyTaskInstance(
                            sourceTemplateId = template.id,
                            title = template.title,
                            description = template.description,
                            dayOfMonth = template.dayOfMonth.coerceAtMost(monthLength),
                            jalaliYear = date.year,
                            jalaliMonth = date.month,
                            jalaliDay = date.day,
                            priority = template.priority
                        )
                    )
                }
            }
    }

    suspend fun ensureTasksForMonth(year: Int, month: Int) = withContext(Dispatchers.IO) {
        val len = JalaliCalendar.monthLength(year, month)
        for (day in 1..len) ensureTasksForDate(JalaliDate(year, month, day))
    }

    fun observeTasksForDate(date: JalaliDate): Flow<List<TaskItem>> {
        return combine(
            dao.observeDailyInstances(date.year, date.month, date.day),
            dao.observeOneTimeTasks(date.year, date.month, date.day)
        ) { daily, oneTime ->
            (daily.map { it.toItem() } + oneTime.map { it.toItem() })
                .sortedWith(compareByDescending<TaskItem> { it.priority.weight }.thenBy { it.createdAt })
        }
    }

    suspend fun getTasksForDate(date: JalaliDate): List<TaskItem> = withContext(Dispatchers.IO) {
        ensureTasksForDate(date)
        (dao.getDailyInstances(date.year, date.month, date.day).map { it.toItem() } +
            dao.getOneTimeTasks(date.year, date.month, date.day).map { it.toItem() })
            .sortedWith(compareByDescending<TaskItem> { it.priority.weight }.thenBy { it.createdAt })
    }

    fun observeMonthCounts(year: Int, month: Int): Flow<Map<Int, Int>> {
        return combine(
            dao.observeTemplates(),
            dao.observeDailyInstancesForMonth(year, month),
            dao.observeOneTimeTasksForMonth(year, month)
        ) { templates, daily, oneTime ->
            val counts = mutableMapOf<Int, Int>()
            templates.filter { it.isActive }.forEach { t ->
                val d = JalaliCalendar.normalizeDayForMonth(t.dayOfMonth, year, month)
                counts[d] = (counts[d] ?: 0) + 1
            }
            oneTime.forEach { counts[it.jalaliDay] = (counts[it.jalaliDay] ?: 0) + 1 }
            daily.filter { it.sourceTemplateId == null }.forEach { counts[it.jalaliDay] = (counts[it.jalaliDay] ?: 0) + 1 }
            counts
        }
    }

    fun observeReport(year: Int, month: Int): Flow<MonthlyReport> {
        return combine(
            dao.observeDailyInstancesForMonth(year, month),
            dao.observeOneTimeTasksForMonth(year, month)
        ) { daily, oneTime ->
            val all = daily.map { it.status } + oneTime.map { it.status }
            val done = all.count { it == TaskStatus.DONE }
            val moved = all.count { it == TaskStatus.MOVED_TO_TOMORROW || it == TaskStatus.MOVED_TO_CUSTOM_DATE }
            val canceled = all.count { it == TaskStatus.CANCELED }
            val total = all.size.coerceAtLeast(1)
            MonthlyReport(done, moved, canceled, total = all.size, completionPercent = (done * 100) / total)
        }
    }

    suspend fun setStatus(item: TaskItem, status: TaskStatus) = withContext(Dispatchers.IO) {
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let { dao.updateDailyInstance(it.copy(status = status, updatedAt = System.currentTimeMillis())) }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let { dao.updateOneTimeTask(it.copy(status = status, updatedAt = System.currentTimeMillis())) }
            TaskOrigin.TEMPLATE -> Unit
        }
    }

    suspend fun deleteTask(item: TaskItem) = withContext(Dispatchers.IO) {
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let { dao.deleteDailyInstance(it) }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let { dao.deleteOneTimeTask(it) }
            TaskOrigin.TEMPLATE -> deleteTemplateTask(item.id)
        }
    }

    suspend fun editTaskOnlyThisDate(item: TaskItem, title: String, description: String, priority: TaskPriority) = withContext(Dispatchers.IO) {
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let {
                dao.updateDailyInstance(it.copy(title = title.trim(), description = description.trim(), priority = priority, updatedAt = System.currentTimeMillis()))
            }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let {
                dao.updateOneTimeTask(it.copy(title = title.trim(), description = description.trim(), priority = priority, updatedAt = System.currentTimeMillis()))
            }
            TaskOrigin.TEMPLATE -> Unit
        }
    }

    suspend fun moveTaskToDate(item: TaskItem, targetDate: JalaliDate, movedStatus: TaskStatus = TaskStatus.MOVED_TO_CUSTOM_DATE) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fromDate = item.dateKey
        val targetKey = targetDate.key
        val movedCopy = OneTimeTask(
            title = item.title,
            description = item.description,
            dayOfMonth = targetDate.day,
            jalaliYear = targetDate.year,
            jalaliMonth = targetDate.month,
            jalaliDay = targetDate.day,
            priority = item.priority,
            createdAt = now,
            updatedAt = now,
            movedFromDate = fromDate
        )
        dao.insertOneTimeTask(movedCopy)
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let {
                dao.updateDailyInstance(it.copy(status = movedStatus, movedToDate = targetKey, updatedAt = now))
            }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let {
                dao.updateOneTimeTask(it.copy(status = movedStatus, movedToDate = targetKey, updatedAt = now))
            }
            TaskOrigin.TEMPLATE -> Unit
        }
    }

    suspend fun moveTaskToTomorrow(item: TaskItem) {
        val today = JalaliDate(item.jalaliYear, item.jalaliMonth, item.jalaliDay)
        moveTaskToDate(item, today.plusDays(1), TaskStatus.MOVED_TO_TOMORROW)
    }

    suspend fun moveRemainingToTomorrow(date: JalaliDate) = withContext(Dispatchers.IO) {
        val items = getTasksForDate(date).filter { it.status == TaskStatus.NOT_STARTED || it.status == TaskStatus.IN_PROGRESS }
        val tomorrow = date.plusDays(1)
        items.forEach { moveTaskToDate(it, tomorrow, TaskStatus.MOVED_TO_TOMORROW) }
    }

    suspend fun remainingCount(date: JalaliDate): Int = withContext(Dispatchers.IO) {
        getTasksForDate(date).count { it.status == TaskStatus.NOT_STARTED || it.status == TaskStatus.IN_PROGRESS }
    }

    private fun DailyTaskInstance.toItem(): TaskItem = TaskItem(
        id = id,
        origin = TaskOrigin.DAILY_INSTANCE,
        sourceTemplateId = sourceTemplateId,
        title = title,
        description = description,
        dayOfMonth = dayOfMonth,
        jalaliYear = jalaliYear,
        jalaliMonth = jalaliMonth,
        jalaliDay = jalaliDay,
        taskType = taskType,
        status = status,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt,
        movedFromDate = movedFromDate,
        movedToDate = movedToDate
    )

    private fun OneTimeTask.toItem(): TaskItem = TaskItem(
        id = id,
        origin = TaskOrigin.ONE_TIME,
        title = title,
        description = description,
        dayOfMonth = dayOfMonth,
        jalaliYear = jalaliYear,
        jalaliMonth = jalaliMonth,
        jalaliDay = jalaliDay,
        taskType = taskType,
        status = status,
        priority = priority,
        createdAt = createdAt,
        updatedAt = updatedAt,
        movedFromDate = movedFromDate,
        movedToDate = movedToDate
    )
}

data class MonthlyReport(
    val done: Int,
    val moved: Int,
    val canceled: Int,
    val total: Int,
    val completionPercent: Int
)
