package com.wavehitech.aptracker

import android.app.Application
import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

class ProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val context: Context // Need the context for file operations
) : ViewModel() {

    // Observable list for projects
    val projects = mutableStateListOf<ProjectEntity>()

    init {
        // Collecting projects from the database
        viewModelScope.launch {
            projectRepository.getAllProjectsFlow().collect { list ->
                projects.clear()
                projects.addAll(list)
            }
        }
    }

    // Add a new project with access points
    fun addProjectWithAccessPoints(project: ProjectEntity, accessPoints: List<AccessPointEntity>) {
        viewModelScope.launch {
            projectRepository.addProjectWithAccessPoints(project, accessPoints)
        }
    }

    // Update an existing access point
    fun updateAccessPoint(accessPoint: AccessPointEntity) {
        viewModelScope.launch {
            projectRepository.updateAccessPoint(accessPoint)
        }
    }

    // Add a new access point
    fun addAccessPoint(accessPoint: AccessPointEntity) {
        viewModelScope.launch {
            projectRepository.addAccessPoint(accessPoint)
        }
    }

    // Delete a specific access point (and optionally its associated images)
    fun deleteAccessPoint(accessPointId: String) {
        viewModelScope.launch {
            projectRepository.deleteAccessPoint(accessPointId)
        }
    }

    // Delete the project and its associated access points from the repository
    fun deleteProjectWithAccessPoints(projectId: String) {
        viewModelScope.launch {
            projectRepository.deleteProjectWithAccessPoints(projectId)
        }
    }

    // Get a specific access point by ID
    suspend fun getAccessPoint(accessPointId: String): AccessPointEntity? {
        return projectRepository.getAccessPointById(accessPointId)
    }

    // Image-related methods

    // Get access points for a specific project
    fun getAccessPointsForProjectFlow(projectId: String): Flow<List<AccessPointEntity>> {
        return projectRepository.getAccessPointsForProjectFlow(projectId)
    }

    // Get all images for an access point (both original and circled versions)
    fun getImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>> {
        return projectRepository.getImagesForAccessPointFlow(apId)
    }

    // Get display images (preferring circled versions when available)
    fun getDisplayImagesForAccessPointFlow(apId: String): Flow<List<ImageEntity>> {
        return projectRepository.getImagesForAccessPointFlow(apId)
            .map { allImages ->
                // Group images by whether they have circled versions
                val originals = mutableListOf<ImageEntity>()
                val circledMap = mutableMapOf<String, ImageEntity>()

                // Sort into categories
                allImages.forEach { image ->
                    if (image.isCircled) {
                        image.originalImageId?.let { originalId ->
                            circledMap[originalId] = image
                        }
                    } else {
                        originals.add(image)
                    }
                }

                // For each original, use circled version if available
                originals.map { original ->
                    circledMap[original.id] ?: original
                }.sortedBy { it.orderIndex }
            }
    }

    fun addImage(image: ImageEntity) {
        viewModelScope.launch {
            projectRepository.insertImage(image)
        }
    }

    fun updateImage(image: ImageEntity) {
        viewModelScope.launch {
            projectRepository.updateImage(image)
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            val image = projectRepository.getImageById(id)
            image?.let {
                // Delete the file from storage
                val file = File(context.filesDir, it.filename)
                if (file.exists()) file.delete()

                // If this is an original with a circled version, delete the circled version too
                if (!it.isCircled) {
                    val circledVersion = projectRepository.getCircledVersionOfImage(it.id)
                    circledVersion?.let { circled ->
                        // Delete circled file
                        val circledFile = File(context.filesDir, circled.filename)
                        if (circledFile.exists()) circledFile.delete()

                        // Delete from database
                        projectRepository.deleteImage(circled.id)
                    }
                }

                // Delete the image entity
                projectRepository.deleteImage(id)
            }
        }
    }

    suspend fun getImageById(id: String): ImageEntity? {
        return projectRepository.getImageById(id)
    }

    suspend fun getCircledVersionOfImage(originalId: String): ImageEntity? {
        return projectRepository.getCircledVersionOfImage(originalId)
    }

    fun reorderImagesAfterDeletion(apId: String) {
        viewModelScope.launch {
            val images = projectRepository.getImagesForAccessPointFlow(apId).first()
            val sortedImages = images.sortedBy { it.orderIndex }

            // Reassign order indexes sequentially
            sortedImages.forEachIndexed { index, image ->
                if (image.orderIndex != index) {
                    projectRepository.updateImageOrder(image.id, index)
                }
            }
        }
    }
}


// ViewModel factory to pass context and create ProjectRepository
class ProjectViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = DatabaseProvider.getDatabase(context)
            val projectDao = database.projectDao()
            val accessPointDao = database.accessPointDao()
            val imageDao = database.imageDao() // Get the image DAO
            val repository = ProjectRepository(projectDao, accessPointDao, imageDao, context)
            return ProjectViewModel(repository, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
