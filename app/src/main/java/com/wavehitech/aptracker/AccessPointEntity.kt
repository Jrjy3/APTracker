package com.wavehitech.aptracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey val id: String,
    val projectId: String,  // Foreign key to the project
    val name: String,
    val pictures: List<String> // This will store the picture paths
)
