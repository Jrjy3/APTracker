package com.wavehitech.aptracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow


@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects")
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>>

    @Insert
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String): Int
}
