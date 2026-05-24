package com.sebo.timelog.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.sebo.timelog.data.local.entities.Project
import com.sebo.timelog.data.local.entities.ProjectStatus
import com.sebo.timelog.data.local.relations.ProjectWithWorkLogs
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY updatedAt DESC")
    fun getProjectsByStatus(status: ProjectStatus = ProjectStatus.ACTIVE): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE createdBy = :userId ORDER BY updatedAt DESC")
    fun getProjectsByCreator(userId: String): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdOnce(id: Long): Project?

    @Query("SELECT * FROM projects WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getProjectByCloudIdOnce(cloudId: String): Project?

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectWithWorkLogs(id: Long): Flow<ProjectWithWorkLogs?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: Project): Long

    @Update
    suspend fun update(project: Project)

    @Delete
    suspend fun delete(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM projects")
    fun getProjectCount(): Flow<Int>
}

