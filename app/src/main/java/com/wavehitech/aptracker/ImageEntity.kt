package com.wavehitech.aptracker

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "images")
data class ImageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val accessPointId: String,  // Foreign key to the access point
    val filename: String,       // The actual filename stored in internal storage
    val isCircled: Boolean,     // Indicates if this image has circles
    val originalImageId: String? = null, // Reference to original image if this is a circled version
    val orderIndex: Int         // For maintaining display order
)