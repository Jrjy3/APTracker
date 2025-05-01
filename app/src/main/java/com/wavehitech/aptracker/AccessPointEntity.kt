package com.wavehitech.aptracker

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "access_points",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("projectId")] // Add index on foreign key for performance
)
data class AccessPointEntity(
    @PrimaryKey val id: String,
    val projectId: String,  // Foreign key to the project
    val name: String
)