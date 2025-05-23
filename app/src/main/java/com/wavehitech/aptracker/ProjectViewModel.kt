package com.wavehitech.aptracker

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ProjectViewModel(
    private val projectRepository: ProjectRepository,
    private val context: Context // Need the context for file operations
) : ViewModel() {

    // Observable list for projects
    val projects = mutableStateListOf<ProjectEntity>()

    // Add a refresh trigger to force UI updates
    private val _refreshTrigger = MutableStateFlow(0L)
    val refreshTrigger: StateFlow<Long> = _refreshTrigger.asStateFlow()

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
            try {
                Log.d("ProjectViewModel", "Starting deletion of AP $accessPointId")

                // Get the access point before deletion so we know which project to refresh
                val ap = projectRepository.getAccessPointById(accessPointId)
                val projectId = ap?.projectId

                // Delete the access point through the repository
                projectRepository.deleteAccessPoint(accessPointId)

                // Add a delay to ensure database operations complete
                delay(500)

                // Refresh the project's access points if we found a valid project ID
                if (projectId != null) {
                    Log.d("ProjectViewModel", "Refreshing access points for project $projectId after deletion")
                    refreshAccessPointsForProject(projectId)
                }

                Log.d("ProjectViewModel", "AP $accessPointId deletion completed")
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Error deleting access point: ${e.message}", e)
            }
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

    /**
     * Gets a specific AccessPointEntity by ID with proper error handling
     *
     * @param apId The ID of the access point to retrieve
     * @return The AccessPointEntity or null if not found
     */
    suspend fun getAccessPointById(apId: String): AccessPointEntity? {
        return projectRepository.getAccessPointById(apId)
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

    /**
     * Gets images for an access point with smart filtering:
     * - If an image has a circled version, only the circled version is returned
     * - If an image only has a non-circled version, that version is returned
     * This ensures each image appears only once in the display grid.
     *
     * @param accessPointId ID of the access point to get images for
     * @return Flow of filtered images for display
     */
    fun getDisplayImagesForAccessPointFlow(accessPointId: String): Flow<List<ImageEntity>> {
        return projectRepository.getImagesForAccessPointFlow(accessPointId)
            .map { allImages ->
                // Create a smart filtered list for display
                val displayImages = mutableListOf<ImageEntity>()

                // First, separate images by type
                val originalImages = allImages.filter { !it.isCircled }
                val circledImages = allImages.filter { it.isCircled }

                // Create a map of original IDs to circled versions
                val originalToCircledMap = mutableMapOf<String, ImageEntity>()
                circledImages.forEach { circledImage ->
                    circledImage.originalImageId?.let { originalId ->
                        originalToCircledMap[originalId] = circledImage
                    }
                }

                // Process each original image
                originalImages.forEach { originalImage ->
                    // If this original has a circled version, add the circled version
                    // If not, add the original image
                    val circledVersion = originalToCircledMap[originalImage.id]
                    if (circledVersion != null) {
                        displayImages.add(circledVersion)
                    } else {
                        displayImages.add(originalImage)
                    }
                }

                // Sort final list by orderIndex
                displayImages.sortedBy { it.orderIndex }
            }
    }

    /**
     * Force refresh access points for a project
     * This method ensures the latest data is retrieved from the database
     *
     * @param projectId The ID of the project whose access points to refresh
     */
    fun refreshAccessPointsForProject(projectId: String) {
        viewModelScope.launch {
            try {
                // Simply triggering a re-fetch through the repository
                val refreshedAPs = projectRepository.getAccessPointsForProject(projectId)

                // Log the refresh for debugging
                Log.d("ProjectViewModel", "Refreshed ${refreshedAPs.count()} access points for project $projectId")
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Error refreshing access points: ${e.message}", e)
            }
        }
    }

    /**
     * Refreshes images for the specified access point
     * Detects and adds any image files that might not be in the database yet
     * This version adds protection against duplicate images
     */
    fun refreshImagesForAccessPoint(accessPointId: String) {
        viewModelScope.launch {
            try {
                // Get the access point
                val ap = projectRepository.getAccessPointById(accessPointId) ?: return@launch

                // Get all image files for this access point from the file system
                val projectId = ap.projectId
                val apName = ap.name
                val filePattern = "${projectId}_${apName}-\\d+.*\\.jpg"
                val regex = filePattern.toRegex(RegexOption.IGNORE_CASE)

                // List all files in the app's files directory
                val filesDir = context.filesDir  // Use the context from constructor
                val allFiles = filesDir.listFiles() ?: return@launch

                // Filter for files matching this access point's pattern
                val apImageFiles = allFiles.filter { file ->
                    regex.matches(file.name)
                }

                // Get all existing images from the database
                val existingImages = projectRepository.getImagesForAccessPointFlow(accessPointId).first()
                val existingFilenames = existingImages.map { it.filename }.toSet()

                // Find files that aren't registered in the database
                val unregisteredFiles = apImageFiles.filter { file ->
                    !existingFilenames.contains(file.name)
                }

                // Additional check: verify there are no in-progress camera captures
                // If we find files that are very new (less than 2 seconds old), ignore them
                // as they're likely being processed by the camera callback
                val currentTime = System.currentTimeMillis()
                val safeUnregisteredFiles = unregisteredFiles.filter { file ->
                    // If file is older than 2 seconds, it's safe to recover
                    // Otherwise, it's likely an in-progress camera capture
                    currentTime - file.lastModified() > 2000
                }

                // Log how many unregistered images we found
                if (safeUnregisteredFiles.isNotEmpty()) {
                    Log.d("ProjectViewModel", "Found ${safeUnregisteredFiles.size} unregistered images for AP ${ap.name}")

                    // Add each unregistered file to the database
                    val nextOrderIndex = existingImages.size
                    safeUnregisteredFiles.forEachIndexed { index, file ->
                        // Create a new image entity
                        val newImage = ImageEntity(
                            accessPointId = accessPointId,
                            filename = file.name,
                            isCircled = file.name.contains("_circle_"),  // Assume files with _circle_ are circled versions
                            orderIndex = nextOrderIndex + index
                        )

                        // Add to database - use insertImage instead of addImage
                        projectRepository.insertImage(newImage)

                        Log.d("ProjectViewModel", "Added recovered image to database: ${file.name}")
                    }
                }

                // Log completion
                val totalImages = projectRepository.getImagesForAccessPointFlow(accessPointId).first().size
                Log.d("ProjectViewModel", "Refreshed images for AP $accessPointId, found $totalImages images")

            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Error refreshing images: ${e.message}", e)
            }
        }
    }

    /**
     * Check for image files that match the AP naming pattern but aren't in the database
     * This helps recover from scenarios where a file was saved but the DB record wasn't created
     *
     * @param accessPointId The ID of the access point to check
     */
    private suspend fun checkForUncataloguedImages(accessPointId: String) {
        try {
            // Get AP details
            val ap = getAccessPointById(accessPointId) ?: return

            // Get list of all images in the database for this AP
            val dbImages = projectRepository.getImagesForAccessPointFlow(accessPointId).first()
            val dbFilenames = dbImages.map { it.filename }.toSet()

            // Get all files in the app's files directory
            val allFiles = context.filesDir.listFiles()?.filter {
                it.isFile && it.name.endsWith(".jpg", ignoreCase = true)
            } ?: return

            // Find files that match this AP but aren't in the database
            val apPattern = ".*_${ap.name}-\\d+.*\\.jpg"
            val apRegex = apPattern.toRegex(RegexOption.IGNORE_CASE)

            val unregisteredFiles = allFiles.filter {
                apRegex.matches(it.name) && !dbFilenames.contains(it.name)
            }

            if (unregisteredFiles.isNotEmpty()) {
                Log.d("ProjectViewModel", "Found ${unregisteredFiles.size} unregistered images for AP ${ap.name}")

                // Add these files to the database
                unregisteredFiles.forEach { file ->
                    val newImage = ImageEntity(
                        accessPointId = accessPointId,
                        filename = file.name,
                        isCircled = false,
                        orderIndex = dbImages.size + unregisteredFiles.indexOf(file)
                    )

                    projectRepository.insertImage(newImage)
                    Log.d("ProjectViewModel", "Added recovered image to database: ${newImage.filename}")
                }
            }
        } catch (e: Exception) {
            Log.e("ProjectViewModel", "Error checking for uncatalogued images: ${e.message}", e)
        }
    }

    fun addImage(image: ImageEntity) {
        viewModelScope.launch {
            Log.d("ProjectViewModel", "Adding new image: ${image.filename} for AP ${image.accessPointId}")
            projectRepository.insertImage(image)

            // Add a small delay to ensure database operation completes
            delay(200)

            // Trigger a refresh after adding a new image
            refreshImagesForAccessPoint(image.accessPointId)
        }
    }

    fun updateImage(image: ImageEntity) {
        viewModelScope.launch {
            projectRepository.updateImage(image)
            // Trigger a refresh after updating an image
            refreshImagesForAccessPoint(image.accessPointId)
        }
    }

    fun deleteImage(id: String) {
        viewModelScope.launch {
            val image = projectRepository.getImageById(id)
            image?.let {
                val accessPointId = it.accessPointId

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

                // Trigger a refresh after deleting an image
                refreshImagesForAccessPoint(accessPointId)
            }
        }
    }

    suspend fun getImageById(id: String): ImageEntity? {
        return projectRepository.getImageById(id)
    }

    suspend fun getCircledVersionOfImage(originalId: String): ImageEntity? {
        return projectRepository.getCircledVersionOfImage(originalId)
    }

    /**
     * Reorders all images for an access point to maintain sequential numbering
     * This is called after deletion to ensure images are numbered 1, 2, 3, etc.
     *
     * @param accessPointId ID of the access point whose images need reordering
     */
    suspend fun reorderImagesAfterDeletion(accessPointId: String) {
        withContext(Dispatchers.IO) {
            try {
                // Get all images for this AP
                val allImages = getImagesForAccessPointFlow(accessPointId).first()

                // Group by whether they're circled or original
                val originalImages = allImages.filter { !it.isCircled }
                val circledImages = allImages.filter { it.isCircled }

                // Map to associate originals with their circled versions
                val originalToCircledMap = mutableMapOf<String, ImageEntity>()
                circledImages.forEach { circledImage ->
                    circledImage.originalImageId?.let { originalId ->
                        originalToCircledMap[originalId] = circledImage
                    }
                }

                // Update original images first
                val updatedOriginals = originalImages.sortedBy { it.orderIndex }
                    .mapIndexed { index, image ->
                        // Create a copy of the image with the new orderIndex
                        image.copy(orderIndex = index)
                    }

                // Update circled images to match their original's orderIndex
                val updatedCircled = circledImages.map { circledImage ->
                    circledImage.originalImageId?.let { originalId ->
                        val originalImage = updatedOriginals.find { it.id == originalId }
                        // If we found the original, update the circled image's orderIndex to match
                        if (originalImage != null) {
                            circledImage.copy(orderIndex = originalImage.orderIndex)
                        } else {
                            circledImage
                        }
                    } ?: circledImage
                }

                // Now update all images in the database - use the repository directly
                // since we're already in a coroutine context
                updatedOriginals.forEach { image ->
                    projectRepository.updateImage(image)
                }

                updatedCircled.forEach { image ->
                    projectRepository.updateImage(image)
                }

                // Log the reordering
                Log.d("ProjectViewModel", "Reordered ${updatedOriginals.size} original images and ${updatedCircled.size} circled images for AP: $accessPointId")

                // We'll refresh after all updates are complete
                refreshImagesForAccessPoint(accessPointId)
            } catch (e: Exception) {
                Log.e("ProjectViewModel", "Error reordering images: ${e.message}", e)
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