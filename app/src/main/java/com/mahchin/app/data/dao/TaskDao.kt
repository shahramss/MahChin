package com.mahchin.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.mahchin.app.data.model.DailyTaskInstance
import com.mahchin.app.data.model.MindMapNode
import com.mahchin.app.data.model.MonthlyTemplateTask
import com.mahchin.app.data.model.OneTimeTask
import com.mahchin.app.data.model.Project
import com.mahchin.app.data.model.UserSettings
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("SELECT * FROM projects WHERE isActive = 1 ORDER BY createdAt ASC")
    fun observeProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isActive = 1 ORDER BY createdAt ASC")
    suspend fun getProjects(): List<Project>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProject(id: Long): Project?

    @Query("SELECT COUNT(*) FROM projects WHERE isActive = 1")
    suspend fun activeProjectCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMindMapNode(node: MindMapNode): Long

    @Update
    suspend fun updateMindMapNode(node: MindMapNode)

    @Query("SELECT * FROM mind_map_nodes WHERE isActive = 1 AND projectId = :projectId ORDER BY parentId ASC, orderIndex ASC, createdAt ASC")
    fun observeMindMapNodes(projectId: Long): Flow<List<MindMapNode>>

    @Query("SELECT * FROM mind_map_nodes WHERE isActive = 1 AND projectId = :projectId ORDER BY parentId ASC, orderIndex ASC, createdAt ASC")
    suspend fun getMindMapNodes(projectId: Long): List<MindMapNode>

    @Query("SELECT * FROM mind_map_nodes WHERE isActive = 1 ORDER BY projectId ASC, parentId ASC, orderIndex ASC, createdAt ASC")
    fun observeAllMindMapNodes(): Flow<List<MindMapNode>>

    @Query("SELECT * FROM mind_map_nodes WHERE isActive = 1 ORDER BY projectId ASC, parentId ASC, orderIndex ASC, createdAt ASC")
    suspend fun getAllMindMapNodes(): List<MindMapNode>

    @Query("SELECT * FROM mind_map_nodes WHERE id = :id LIMIT 1")
    suspend fun getMindMapNode(id: Long): MindMapNode?

    @Query("UPDATE mind_map_nodes SET x = :x, y = :y, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateMindMapNodePosition(id: Long, x: Float, y: Float, updatedAt: Long = System.currentTimeMillis())

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(task: MonthlyTemplateTask): Long

    @Update
    suspend fun updateTemplate(task: MonthlyTemplateTask)

    @Delete
    suspend fun deleteTemplate(task: MonthlyTemplateTask)

    @Query("SELECT * FROM monthly_template_tasks WHERE isActive = 1 ORDER BY dayOfMonth ASC, priority DESC, createdAt ASC")
    fun observeTemplates(): Flow<List<MonthlyTemplateTask>>

    @Query("SELECT * FROM monthly_template_tasks WHERE isActive = 1 ORDER BY dayOfMonth ASC, priority DESC, createdAt ASC")
    suspend fun getActiveTemplates(): List<MonthlyTemplateTask>

    @Query("SELECT * FROM monthly_template_tasks WHERE id = :id LIMIT 1")
    suspend fun getTemplate(id: Long): MonthlyTemplateTask?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDailyInstance(task: DailyTaskInstance): Long

    @Update
    suspend fun updateDailyInstance(task: DailyTaskInstance)

    @Delete
    suspend fun deleteDailyInstance(task: DailyTaskInstance)

    @Query("SELECT * FROM daily_task_instances WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY priority DESC, createdAt ASC")
    fun observeDailyInstances(year: Int, month: Int, day: Int): Flow<List<DailyTaskInstance>>

    @Query("SELECT * FROM daily_task_instances WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY priority DESC, createdAt ASC")
    suspend fun getDailyInstances(year: Int, month: Int, day: Int): List<DailyTaskInstance>

    @Query("SELECT * FROM daily_task_instances WHERE id = :id LIMIT 1")
    suspend fun getDailyInstance(id: Long): DailyTaskInstance?

    @Query("SELECT * FROM daily_task_instances WHERE sourceTemplateId = :sourceTemplateId AND jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day LIMIT 1")
    suspend fun getDailyInstanceBySource(sourceTemplateId: Long, year: Int, month: Int, day: Int): DailyTaskInstance?

    @Query("SELECT * FROM daily_task_instances WHERE alarmAtMillis IS NOT NULL AND alarmAtMillis >= :now")
    suspend fun getFutureDailyTasksWithAlarms(now: Long): List<DailyTaskInstance>

    @Query("SELECT * FROM daily_task_instances WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay ASC")
    fun observeDailyInstancesForMonth(year: Int, month: Int): Flow<List<DailyTaskInstance>>

    @Query("SELECT * FROM daily_task_instances WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay ASC")
    suspend fun getDailyInstancesForMonth(year: Int, month: Int): List<DailyTaskInstance>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOneTimeTask(task: OneTimeTask): Long

    @Update
    suspend fun updateOneTimeTask(task: OneTimeTask)

    @Delete
    suspend fun deleteOneTimeTask(task: OneTimeTask)

    @Query("SELECT * FROM one_time_tasks WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY priority DESC, createdAt ASC")
    fun observeOneTimeTasks(year: Int, month: Int, day: Int): Flow<List<OneTimeTask>>

    @Query("SELECT * FROM one_time_tasks WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day ORDER BY priority DESC, createdAt ASC")
    suspend fun getOneTimeTasks(year: Int, month: Int, day: Int): List<OneTimeTask>

    @Query("SELECT * FROM one_time_tasks WHERE id = :id LIMIT 1")
    suspend fun getOneTimeTask(id: Long): OneTimeTask?

    @Query("SELECT * FROM one_time_tasks WHERE alarmAtMillis IS NOT NULL AND alarmAtMillis >= :now")
    suspend fun getFutureOneTimeTasksWithAlarms(now: Long): List<OneTimeTask>

    @Query("SELECT * FROM one_time_tasks WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay ASC")
    fun observeOneTimeTasksForMonth(year: Int, month: Int): Flow<List<OneTimeTask>>

    @Query("SELECT * FROM one_time_tasks WHERE jalaliYear = :year AND jalaliMonth = :month ORDER BY jalaliDay ASC")
    suspend fun getOneTimeTasksForMonth(year: Int, month: Int): List<OneTimeTask>


    @Query("DELETE FROM daily_task_instances WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day")
    suspend fun deleteDailyInstancesForDate(year: Int, month: Int, day: Int)

    @Query("DELETE FROM one_time_tasks WHERE jalaliYear = :year AND jalaliMonth = :month AND jalaliDay = :day")
    suspend fun deleteOneTimeTasksForDate(year: Int, month: Int, day: Int)

    @Query("DELETE FROM daily_task_instances")
    suspend fun deleteAllDailyInstances()

    @Query("DELETE FROM one_time_tasks")
    suspend fun deleteAllOneTimeTasks()

    @Query("UPDATE monthly_template_tasks SET isActive = 0, updatedAt = :updatedAt WHERE isActive = 1")
    suspend fun deactivateAllTemplates(updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettings)
}
