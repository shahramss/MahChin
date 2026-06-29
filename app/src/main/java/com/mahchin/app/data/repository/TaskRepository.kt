package com.mahchin.app.data.repository

import com.mahchin.app.data.dao.TaskDao
import com.mahchin.app.data.model.DailyTaskInstance
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.OneTimeTask
import com.mahchin.app.data.model.Project
import com.mahchin.app.data.model.TaskItem
import com.mahchin.app.data.model.TaskOrigin
import com.mahchin.app.data.model.TaskPriority
import com.mahchin.app.data.model.TaskStatus
import com.mahchin.app.data.model.UserSettings
import com.mahchin.app.domain.JalaliCalendar
import com.mahchin.app.domain.JalaliDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class TaskRepository(private val dao: TaskDao) {

    val settingsFlow: Flow<UserSettings> = dao.observeSettings().map { it ?: UserSettings() }
    val templatesFlow: Flow<List<MonthlyTemplateTask>> = dao.observeTemplates()
    val projectsFlow: Flow<List<Project>> = dao.observeProjects()
    val allMindMapNodesFlow: Flow<List<MindMapNode>> = dao.observeAllMindMapNodes()

    suspend fun getSettingsOrDefault(): UserSettings = withContext(Dispatchers.IO) {
        dao.getSettings() ?: UserSettings().also { dao.upsertSettings(it) }
    }

    suspend fun ensureDefaultSettings() = withContext(Dispatchers.IO) {
        if (dao.getSettings() == null) dao.upsertSettings(UserSettings())
        ensureDefaultProject()
    }

    suspend fun ensureDefaultProject(): Long = withContext(Dispatchers.IO) {
        val existing = dao.getProjects().firstOrNull()
        existing?.id ?: dao.insertProject(Project(name = "عمومی"))
    }

    suspend fun updateSettings(settings: UserSettings) = withContext(Dispatchers.IO) {
        dao.upsertSettings(settings.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun addProject(name: String): Long = withContext(Dispatchers.IO) {
        val clean = name.trim().ifBlank { "پروژه جدید" }
        dao.insertProject(Project(name = clean))
    }

    suspend fun updateProject(id: Long, name: String) = withContext(Dispatchers.IO) {
        val clean = name.trim().ifBlank { "پروژه" }
        dao.getProject(id)?.let { project ->
            dao.updateProject(project.copy(name = clean, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteProject(id: Long): Long? = withContext(Dispatchers.IO) {
        val active = dao.getProjects()
        if (active.size <= 1) return@withContext active.firstOrNull()?.id
        val project = dao.getProject(id) ?: return@withContext active.firstOrNull()?.id
        dao.updateProject(project.copy(isActive = false, updatedAt = System.currentTimeMillis()))
        dao.getProjects().firstOrNull()?.id ?: ensureDefaultProject()
    }

    fun observeMindMapNodes(projectId: Long): Flow<List<MindMapNode>> = dao.observeMindMapNodes(projectId)

    suspend fun addMindMapNode(projectId: Long, parentId: Long?, title: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        dao.insertMindMapNode(
            MindMapNode(
                projectId = projectId,
                parentId = parentId,
                title = title.trim().ifBlank { "گره جدید" },
                description = description.trim()
            )
        )
    }

    suspend fun updateMindMapNode(id: Long, title: String, description: String = "") = withContext(Dispatchers.IO) {
        dao.getMindMapNode(id)?.let {
            dao.updateMindMapNode(it.copy(title = title.trim(), description = description.trim(), updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun updateMindMapNodePosition(id: Long, x: Float, y: Float) = withContext(Dispatchers.IO) {
        dao.updateMindMapNodePosition(id, x, y, System.currentTimeMillis())
    }

    suspend fun deleteMindMapNode(id: Long) = withContext(Dispatchers.IO) {
        val node = dao.getMindMapNode(id) ?: return@withContext
        val allProjectNodes = dao.getMindMapNodes(node.projectId)
        val idsToDelete = collectDescendantIds(id, allProjectNodes).plus(id).toSet()
        val now = System.currentTimeMillis()
        allProjectNodes.filter { it.id in idsToDelete }.forEach {
            dao.updateMindMapNode(it.copy(isActive = false, updatedAt = now))
        }
    }

    suspend fun addTemplateTask(
        title: String,
        description: String,
        dayOfMonth: Int,
        priority: TaskPriority,
        projectId: Long? = null,
        sourceMindMapNodeId: Long? = null,
        alarmHour: Int? = null,
        alarmMinute: Int? = null
    ) = withContext(Dispatchers.IO) {
        dao.insertTemplate(
            MonthlyTemplateTask(
                projectId = projectId,
                sourceMindMapNodeId = sourceMindMapNodeId,
                title = title.trim(),
                description = description.trim(),
                dayOfMonth = dayOfMonth.coerceIn(1, 31),
                priority = priority,
                alarmHour = alarmHour,
                alarmMinute = alarmMinute
            )
        )
    }

    suspend fun updateTemplateTask(
        id: Long,
        title: String,
        description: String,
        dayOfMonth: Int,
        priority: TaskPriority,
        projectId: Long? = null
    ) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let {
            dao.updateTemplate(
                it.copy(
                    projectId = projectId ?: it.projectId,
                    title = title.trim(),
                    description = description.trim(),
                    dayOfMonth = dayOfMonth.coerceIn(1, 31),
                    priority = priority,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun setTemplateAlarm(id: Long, hour: Int?, minute: Int?) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let {
            dao.updateTemplate(it.copy(alarmHour = hour, alarmMinute = minute, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deleteTemplateTask(id: Long) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let { dao.updateTemplate(it.copy(isActive = false, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun setTemplateStatus(id: Long, status: TaskStatus) = withContext(Dispatchers.IO) {
        dao.getTemplate(id)?.let { dao.updateTemplate(it.copy(status = status, updatedAt = System.currentTimeMillis())) }
    }

    suspend fun addOneTimeTask(
        date: JalaliDate,
        title: String,
        description: String,
        priority: TaskPriority,
        projectId: Long? = null,
        sourceMindMapNodeId: Long? = null,
        alarmAtMillis: Long? = null
    ) = withContext(Dispatchers.IO) {
        dao.insertOneTimeTask(
            OneTimeTask(
                projectId = projectId,
                sourceMindMapNodeId = sourceMindMapNodeId,
                title = title.trim(),
                description = description.trim(),
                dayOfMonth = date.day,
                jalaliYear = date.year,
                jalaliMonth = date.month,
                jalaliDay = date.day,
                priority = priority,
                alarmAtMillis = alarmAtMillis
            )
        )
    }

    suspend fun ensureTasksForDate(date: JalaliDate) = withContext(Dispatchers.IO) {
        val templates = dao.getActiveTemplates()
        val monthLength = JalaliCalendar.monthLength(date.year, date.month)
        templates
            .filter { JalaliCalendar.normalizeDayForMonth(it.dayOfMonth, date.year, date.month) == date.day }
            .forEach { template ->
                val alarmMillis = if (template.alarmHour != null && template.alarmMinute != null) {
                    toEpochMillis(date, template.alarmHour, template.alarmMinute)
                } else null
                val existing = dao.getDailyInstanceBySource(template.id, date.year, date.month, date.day)
                if (existing == null) {
                    dao.insertDailyInstance(
                        DailyTaskInstance(
                            sourceTemplateId = template.id,
                            projectId = template.projectId,
                            sourceMindMapNodeId = template.sourceMindMapNodeId,
                            title = template.title,
                            description = template.description,
                            dayOfMonth = template.dayOfMonth.coerceAtMost(monthLength),
                            jalaliYear = date.year,
                            jalaliMonth = date.month,
                            jalaliDay = date.day,
                            priority = template.priority,
                            alarmAtMillis = alarmMillis
                        )
                    )
                } else if (existing.alarmAtMillis != alarmMillis || existing.projectId != template.projectId) {
                    dao.updateDailyInstance(existing.copy(projectId = template.projectId, alarmAtMillis = alarmMillis, updatedAt = System.currentTimeMillis()))
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
            dao.observeOneTimeTasks(date.year, date.month, date.day),
            dao.observeProjects(),
            dao.observeAllMindMapNodes()
        ) { daily, oneTime, projects, nodes ->
            val projectMap = projects.associateBy { it.id }
            val nodeMap = nodes.associateBy { it.id }
            (daily.map { it.toItem(projectMap, nodeMap) } + oneTime.map { it.toItem(projectMap, nodeMap) })
                .sortedWith(compareByDescending<TaskItem> { it.priority.weight }.thenBy { it.createdAt })
        }
    }

    suspend fun getTasksForDate(date: JalaliDate): List<TaskItem> = withContext(Dispatchers.IO) {
        ensureTasksForDate(date)
        val projects = dao.getProjects().associateBy { it.id }
        val nodes = dao.getAllMindMapNodes().associateBy { it.id }
        (dao.getDailyInstances(date.year, date.month, date.day).map { it.toItem(projects, nodes) } +
            dao.getOneTimeTasks(date.year, date.month, date.day).map { it.toItem(projects, nodes) })
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

    suspend fun setTaskAlarm(item: TaskItem, alarmAtMillis: Long?) = withContext(Dispatchers.IO) {
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let { dao.updateDailyInstance(it.copy(alarmAtMillis = alarmAtMillis, updatedAt = System.currentTimeMillis())) }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let { dao.updateOneTimeTask(it.copy(alarmAtMillis = alarmAtMillis, updatedAt = System.currentTimeMillis())) }
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

    suspend fun editTaskOnlyThisDate(
        item: TaskItem,
        title: String,
        description: String,
        priority: TaskPriority,
        projectId: Long? = null
    ) = withContext(Dispatchers.IO) {
        when (item.origin) {
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let {
                dao.updateDailyInstance(it.copy(title = title.trim(), description = description.trim(), priority = priority, projectId = projectId ?: it.projectId, updatedAt = System.currentTimeMillis()))
            }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let {
                dao.updateOneTimeTask(it.copy(title = title.trim(), description = description.trim(), priority = priority, projectId = projectId ?: it.projectId, updatedAt = System.currentTimeMillis()))
            }
            TaskOrigin.TEMPLATE -> Unit
        }
    }

    suspend fun moveTaskToDate(item: TaskItem, targetDate: JalaliDate, movedStatus: TaskStatus = TaskStatus.MOVED_TO_CUSTOM_DATE) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val fromDate = item.dateKey
        val targetKey = targetDate.key
        val movedCopy = OneTimeTask(
            projectId = item.projectId,
            sourceMindMapNodeId = item.sourceMindMapNodeId,
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
            TaskOrigin.DAILY_INSTANCE -> dao.getDailyInstance(item.id)?.let { dao.updateDailyInstance(it.copy(status = movedStatus, movedToDate = targetKey, updatedAt = now)) }
            TaskOrigin.ONE_TIME -> dao.getOneTimeTask(item.id)?.let { dao.updateOneTimeTask(it.copy(status = movedStatus, movedToDate = targetKey, updatedAt = now)) }
            TaskOrigin.TEMPLATE -> Unit
        }
    }

    suspend fun moveTaskToTomorrow(item: TaskItem) {
        val date = JalaliDate(item.jalaliYear, item.jalaliMonth, item.jalaliDay).plusDays(1)
        moveTaskToDate(item, date, TaskStatus.MOVED_TO_TOMORROW)
    }

    suspend fun moveRemainingToTomorrow(date: JalaliDate) = withContext(Dispatchers.IO) {
        getTasksForDate(date).filter { !it.status.isClosed() }.forEach { moveTaskToTomorrow(it) }
    }

    suspend fun createTasksFromMindMap(projectId: Long, startDate: JalaliDate, tasksPerDay: Int): Int = withContext(Dispatchers.IO) {
        val nodes = dao.getMindMapNodes(projectId)
        if (nodes.isEmpty()) return@withContext 0
        val nodeMap = nodes.associateBy { it.id }
        val children = nodes.groupBy { it.parentId }
        val taskNodes = orderedAllMindMapNodes(children[null].orEmpty(), children).ifEmpty { nodes }
        val perDay = if (tasksPerDay >= 9999) taskNodes.size.coerceAtLeast(1) else tasksPerDay.coerceAtLeast(1)
        taskNodes.forEachIndexed { index, node ->
            val date = startDate.plusDays((index / perDay).toLong())
            val path = buildMindMapPath(node.id, nodeMap)
            dao.insertOneTimeTask(
                OneTimeTask(
                    projectId = projectId,
                    sourceMindMapNodeId = node.id,
                    title = node.title,
                    description = "مسیر: $path",
                    dayOfMonth = date.day,
                    jalaliYear = date.year,
                    jalaliMonth = date.month,
                    jalaliDay = date.day,
                    priority = TaskPriority.NORMAL
                )
            )
        }
        taskNodes.size
    }

    private fun orderedAllMindMapNodes(roots: List<MindMapNode>, children: Map<Long?, List<MindMapNode>>): List<MindMapNode> {
        val result = mutableListOf<MindMapNode>()
        fun visit(node: MindMapNode) {
            result += node
            children[node.id].orEmpty()
                .sortedWith(compareBy({ it.orderIndex }, { it.createdAt }))
                .forEach { visit(it) }
        }
        roots.sortedWith(compareBy({ it.orderIndex }, { it.createdAt })).forEach { visit(it) }
        return result
    }


    suspend fun clearTasksForDate(date: JalaliDate) = withContext(Dispatchers.IO) {
        dao.deleteDailyInstancesForDate(date.year, date.month, date.day)
        dao.deleteOneTimeTasksForDate(date.year, date.month, date.day)
    }

    suspend fun clearNonTemplateTasksForDate(date: JalaliDate) = withContext(Dispatchers.IO) {
        dao.deleteNonTemplateDailyInstancesForDate(date.year, date.month, date.day)
        dao.deleteOneTimeTasksForDate(date.year, date.month, date.day)
    }

    suspend fun clearAllTasks() = withContext(Dispatchers.IO) {
        dao.deleteAllDailyInstances()
        dao.deleteAllOneTimeTasks()
    }

    suspend fun clearAllTemplates() = withContext(Dispatchers.IO) {
        dao.deactivateAllTemplates(System.currentTimeMillis())
    }

    suspend fun getFutureAlarms(): List<TaskItem> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val projects = dao.getProjects().associateBy { it.id }
        val nodes = dao.getAllMindMapNodes().associateBy { it.id }
        (dao.getFutureDailyTasksWithAlarms(now).map { it.toItem(projects, nodes) } +
            dao.getFutureOneTimeTasksWithAlarms(now).map { it.toItem(projects, nodes) })
            .filter { it.alarmAtMillis != null && !it.status.isClosed() }
    }

    fun toEpochMillis(date: JalaliDate, hour: Int, minute: Int): Long {
        return date.toGregorian()
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    private fun DailyTaskInstance.toItem(projects: Map<Long, Project>, nodes: Map<Long, MindMapNode>): TaskItem = TaskItem(
        id = id,
        origin = TaskOrigin.DAILY_INSTANCE,
        sourceTemplateId = sourceTemplateId,
        projectId = projectId,
        projectName = projectId?.let { projects[it]?.name },
        sourceMindMapNodeId = sourceMindMapNodeId,
        mindMapPath = sourceMindMapNodeId?.let { buildMindMapPath(it, nodes) },
        title = title,
        description = description,
        dayOfMonth = dayOfMonth,
        jalaliYear = jalaliYear,
        jalaliMonth = jalaliMonth,
        jalaliDay = jalaliDay,
        taskType = taskType,
        status = status,
        priority = priority,
        alarmAtMillis = alarmAtMillis,
        createdAt = createdAt,
        updatedAt = updatedAt,
        movedFromDate = movedFromDate,
        movedToDate = movedToDate
    )

    private fun OneTimeTask.toItem(projects: Map<Long, Project>, nodes: Map<Long, MindMapNode>): TaskItem = TaskItem(
        id = id,
        origin = TaskOrigin.ONE_TIME,
        projectId = projectId,
        projectName = projectId?.let { projects[it]?.name },
        sourceMindMapNodeId = sourceMindMapNodeId,
        mindMapPath = sourceMindMapNodeId?.let { buildMindMapPath(it, nodes) },
        title = title,
        description = description,
        dayOfMonth = dayOfMonth,
        jalaliYear = jalaliYear,
        jalaliMonth = jalaliMonth,
        jalaliDay = jalaliDay,
        taskType = taskType,
        status = status,
        priority = priority,
        alarmAtMillis = alarmAtMillis,
        createdAt = createdAt,
        updatedAt = updatedAt,
        movedFromDate = movedFromDate,
        movedToDate = movedToDate
    )

    private fun collectDescendantIds(parentId: Long, nodes: List<MindMapNode>): List<Long> {
        val children = nodes.filter { it.parentId == parentId && it.isActive }
        return children.flatMap { child -> listOf(child.id) + collectDescendantIds(child.id, nodes) }
    }

    private fun buildMindMapPath(id: Long, nodes: Map<Long, MindMapNode>): String {
        val parts = mutableListOf<String>()
        var current = nodes[id]
        var guard = 0
        while (current != null && guard < 20) {
            parts.add(current.title)
            current = current.parentId?.let { nodes[it] }
            guard++
        }
        return parts.asReversed().joinToString(" › ")
    }

}

data class MonthlyReport(
    val done: Int,
    val moved: Int,
    val canceled: Int,
    val total: Int,
    val completionPercent: Int
)
