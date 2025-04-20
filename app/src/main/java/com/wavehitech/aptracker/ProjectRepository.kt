package com.wavehitech.aptracker

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

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

    // Delete a single access point and its associated images
    suspend fun deleteAccessPoint(accessPointId: String) {
        // Get all images for this access point
        val images = imageDao.getImagesForAccessPointFlow(accessPointId).first()

        // Delete each image file and database entry
        images.forEach { image ->
            // Delete the image file
            val file = File(context.filesDir, image.filename)
            if (file.exists()) {
                file.delete()
            }
            // Delete the image entity
            imageDao.deleteImage(image.id)
        }

        // Delete the access point from the database
        accessPointDao.deleteAccessPoint(accessPointId)
    }

    // Delete a project and its associated access points
    suspend fun deleteProjectWithAccessPoints(projectId: String) {
        // Get the current list of access points for this project
        val points = accessPointDao.getAccessPointsForProjectFlow(projectId).first()

        // For each access point, delete its associated images and then the access point
        points.forEach { accessPoint ->
            // Get all images for this access point
            val images = imageDao.getImagesForAccessPointFlow(accessPoint.id).first()

            // Delete each image file and database entry
            images.forEach { image ->
                val file = File(context.filesDir, image.filename)
                if (file.exists()) {
                    file.delete()
                }
                imageDao.deleteImage(image.id)
            }

            // Delete the access point
            accessPointDao.deleteAccessPoint(accessPoint.id)
        }

        // Finally, delete the project from the database
        projectDao.deleteProject(projectId)
    }

    // Get a specific access point by ID
    suspend fun getAccessPointById(accessPointId: String): AccessPointEntity? {
        return accessPointDao.getAccessPointById(accessPointId)
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