package com.wavehitech.aptracker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import androidx.room.Transaction
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val accessPointDao: AccessPointDao,
    private val imageDao: ImageDao,
    private val context: Context
) {

    // Get all projects
    fun getAllProjectsFlow(): Flow<List<ProjectEntity>> {
        return projectDao.getAllProjectsFlow()
    }

    // Add a new project with associated access points
    suspend fun addProjectWithAccessPoints(project: ProjectEntity, accessPoints: List<AccessPointEntity>) {
        projectDao.insertProject(project)
        accessPoints.forEach { accessPoint ->
            accessPointDao.insertAccessPoint(accessPoint)
        }
    }

    // Get access points for a specific project
    fun getAccessPointsForProjectFlow(projectId: String): Flow<List<AccessPointEntity>> {
        return accessPointDao.getAccessPointsForProjectFlow(projectId)
    }

    // Update an access point
    suspend fun updateAccessPoint(accessPoint: AccessPointEntity) {
        accessPointDao.updateAccessPoint(accessPoint)
    }

    // Add an access point
    suspend fun addAccessPoint(accessPoint: AccessPointEntity) {
        accessPointDao.insertAccessPoint(accessPoint)
    }

    /**
     * Get all access points for a project directly (not as Flow)
     * This is useful for immediate data refresh
     *
     * @param projectId The ID of the project
     * @return List of access points for the project
     */
    suspend fun getAccessPointsForProject(projectId: String): List<AccessPointEntity> {
        return try {
            accessPointDao.getAccessPointsForProject(projectId)
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Error getting access points for project $projectId: ${e.message}", e)
            emptyList()
        }
    }

    // Delete a single access point and its associated images
    @Transaction
    suspend fun deleteAccessPoint(accessPointId: String) {
        // Add logging for debugging
        Log.d("ProjectRepository", "Deleting access point $accessPointId")

        // Get all images for this access point
        val images = imageDao.getImagesForAccessPointFlow(accessPointId).first()

        // Log how many images we're going to delete
        Log.d("ProjectRepository", "Found ${images.size} images to delete")

        // Delete each image file from storage (this happens outside the database transaction)
        images.forEach { image ->
            // Delete the image file
            val file = File(context.filesDir, image.filename)
            if (file.exists()) {
                val deleted = file.delete()
                Log.d("ProjectRepository", "Deleted file ${image.filename}: $deleted")
            } else {
                Log.d("ProjectRepository", "File ${image.filename} didn't exist")
            }
        }

        // Add a small delay to ensure file operations complete
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(200)
        }

        // Delete all images for this access point in a single database operation
        try {
            val deletedCount = imageDao.deleteAllImagesForAccessPoint(accessPointId)
            Log.d("ProjectRepository", "Deleted $deletedCount image entities for AP $accessPointId")
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Failed to delete image entities: ${e.message}")
        }

        // Delete the access point from the database
        try {
            accessPointDao.deleteAccessPoint(accessPointId)
            Log.d("ProjectRepository", "Successfully deleted AP $accessPointId")
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Failed to delete AP $accessPointId: ${e.message}")
        }

        // Verify no orphaned images remain
        val remainingImages = imageDao.checkForImagesWithAccessPoint(accessPointId)
        if (remainingImages.isNotEmpty()) {
            Log.w("ProjectRepository", "Found ${remainingImages.size} orphaned images after deletion!")
        }
    }

    // Delete a project and its associated access points
    @Transaction
    suspend fun deleteProjectWithAccessPoints(projectId: String) {
        Log.d("ProjectRepository", "Deleting project $projectId with all access points")

        // Get the current list of access points for this project
        val points = accessPointDao.getAccessPointsForProjectFlow(projectId).first()
        Log.d("ProjectRepository", "Found ${points.size} access points to delete")

        // For each access point, delete its associated image files
        // (We'll let the database CASCADE handling take care of the database entities)
        points.forEach { accessPoint ->
            // Get all images for this access point
            val images = imageDao.getImagesForAccessPointFlow(accessPoint.id).first()
            Log.d("ProjectRepository", "Found ${images.size} images for AP ${accessPoint.id}")

            // Delete each image file (this operates outside the database)
            images.forEach { image ->
                val file = File(context.filesDir, image.filename)
                if (file.exists()) {
                    val deleted = file.delete()
                    Log.d("ProjectRepository", "Deleted file ${image.filename}: $deleted")
                } else {
                    Log.d("ProjectRepository", "File ${image.filename} didn't exist")
                }
            }
        }

        // Add a small delay to ensure file operations complete
        withContext(Dispatchers.IO) {
            kotlinx.coroutines.delay(300)
        }

        // Delete the project - cascading delete will handle access points and images in the database
        projectDao.deleteProject(projectId)
        Log.d("ProjectRepository", "Project $projectId deleted from database")
    }

    /**
     * Gets a single access point by ID
     *
     * @param apId The ID of the access point to retrieve
     * @return The AccessPointEntity or null if not found
     */
    suspend fun getAccessPointById(apId: String): AccessPointEntity? {
        return try {
            accessPointDao.getAccessPointById(apId)
        } catch (e: Exception) {
            Log.e("ProjectRepository", "Error getting access point $apId: ${e.message}", e)
            null
        }
    }

    // Image methods
    fun getImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>> {
        return imageDao.getImagesForAccessPointFlow(apId)
    }

    suspend fun insertImage(image: ImageEntity) {
        imageDao.insertImage(image)
    }

    suspend fun updateImage(image: ImageEntity) {
        imageDao.updateImage(image)
    }

    suspend fun deleteImage(id: String) {
        imageDao.deleteImage(id)
    }

    suspend fun getImageById(id: String): ImageEntity? {
        return imageDao.getImageById(id)
    }

    suspend fun getCircledVersionOfImage(originalId: String): ImageEntity? {
        return imageDao.getCircledVersionOfImage(originalId)
    }

    suspend fun updateImageOrder(imageId: String, newIndex: Int) {
        imageDao.updateImageOrder(imageId, newIndex)
    }
}