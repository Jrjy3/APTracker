package com.wavehitech.aptracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow


@Dao
interface AccessPointDao {
    @Query("SELECT * FROM access_points WHERE projectId = :projectId")
    fun getAccessPointsForProjectFlow(projectId: String): Flow<List<AccessPointEntity>>

    @Insert
    suspend fun insertAccessPoint(accessPoint: AccessPointEntity)

    @Update
    suspend fun updateAccessPoint(accessPoint: AccessPointEntity)

    @Query("DELETE FROM access_points WHERE id = :id")
    suspend fun deleteAccessPoint(id: String)

    @Query("SELECT * FROM access_points WHERE id = :id LIMIT 1")
    suspend fun getAccessPointById(id: String): AccessPointEntity?

}
