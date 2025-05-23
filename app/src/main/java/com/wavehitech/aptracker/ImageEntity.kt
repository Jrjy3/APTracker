package com.wavehitech.aptracker

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = AccessPointEntity::class,
            parentColumns = ["id"],
            childColumns = ["accessPointId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("accessPointId"), // Index for foreign key
        Index("originalImageId") // Index for relations between images
    ]
)
data class ImageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accessPointId: String,  // Foreign key to the access point
    val filename: String,       // The actual filename stored in internal storage
    val isCircled: Boolean,     // Indicates if this image has circles
    val originalImageId: String? = null, // Reference to original image if this is a circled version
    val orderIndex: Int         // For maintaining display order
)