package com.wavehitech.aptracker

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE accessPointId = :apId ORDER BY orderIndex ASC")
    fun getImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE accessPointId = :apId AND isCircled = 0 ORDER BY orderIndex ASC")
    fun getOriginalImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>>

    @Query("SELECT * FROM images WHERE accessPointId = :apId AND isCircled = 1 ORDER BY orderIndex ASC")
    fun getCircledImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>>

    @Insert
    suspend fun insertImage(image: ImageEntity)

    @Update
    suspend fun updateImage(image: ImageEntity)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImage(id: String)

    @Query("SELECT * FROM images WHERE id = :id LIMIT 1")
    suspend fun getImageById(id: String): ImageEntity?

    @Query("SELECT * FROM images WHERE filename = :filename LIMIT 1")
    suspend fun getImageByFilename(filename: String): ImageEntity?

    @Query("SELECT * FROM images WHERE originalImageId = :originalId LIMIT 1")
    suspend fun getCircledVersionOfImage(originalId: String): ImageEntity?

    @Query("DELETE FROM images WHERE accessPointId = :apId")
    suspend fun deleteAllImagesForAccessPoint(apId: String)

    // Add this method to update just the order index
    @Query("UPDATE images SET orderIndex = :newIndex WHERE id = :imageId")
    suspend fun updateImageOrder(imageId: String, newIndex: Int)
}