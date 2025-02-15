package com.wavehitech.aptracker

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class ProjectViewModel(
    private val projectRepository: ProjectRepository
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

    // Get access points for a specific project
    fun getAccessPointsForProjectFlow(projectId: String): Flow<List<AccessPointEntity>> {
        return projectRepository.getAccessPointsForProjectFlow(projectId)
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
}

// ViewModel factory to pass context and create ProjectRepository
class ProjectViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            val database = DatabaseProvider.getDatabase(context)
            val projectDao = database.projectDao()
            val accessPointDao = database.accessPointDao()
            val repository = ProjectRepository(projectDao, accessPointDao, context)
            return ProjectViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
