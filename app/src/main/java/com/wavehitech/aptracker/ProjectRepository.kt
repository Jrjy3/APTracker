package com.wavehitech.aptracker

import android.content.Context
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first


class ProjectRepository(
    private val projectDao: ProjectDao,
    private val accessPointDao: AccessPointDao,
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

    // Delete a single access point and its associated pictures
    suspend fun deleteAccessPoint(accessPointId: String) {
        // Retrieve the access point from the database.
        val accessPoint = accessPointDao.getAccessPointById(accessPointId)
        accessPoint?.let { ap ->
            // Delete each associated picture from the app's files directory.
            ap.pictures.forEach { picturePath ->
                val file = File(context.filesDir, picturePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete the access point from the database.
            accessPointDao.deleteAccessPoint(accessPointId)
        }
    }

    // Delete a project and its associated access points
    suspend fun deleteProjectWithAccessPoints(projectId: String) {
        // Get the current list of access points for this project (only once)
        val points = accessPointDao.getAccessPointsForProjectFlow(projectId).first()

        // For each access point, delete its associated pictures from the filesystem
        points.forEach { accessPoint ->
            accessPoint.pictures.forEach { picturePath ->
                val file = File(context.filesDir, picturePath)
                if (file.exists()) {
                    file.delete()
                }
            }
            // Delete the access point from the database
            accessPointDao.deleteAccessPoint(accessPoint.id)
        }

        // Finally, delete the project from the database.
        projectDao.deleteProject(projectId)
    }
}
