package com.wavehitech.aptracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "access_points")
data class AccessPointEntity(
    @PrimaryKey val id: String,
    val projectId: String,  // Foreign key to the project
    val name: String
    // No more pictures list - images are now in their own table
)
