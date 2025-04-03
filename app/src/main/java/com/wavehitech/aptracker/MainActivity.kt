@file:OptIn(ExperimentalMaterialApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.wavehitech.aptracker

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.BackHandler
import androidx.core.content.FileProvider
import android.net.Uri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Embedded
import androidx.room.Relation
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import android.graphics.BitmapFactory
import android.util.Log
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Data Models Section
 */

/**
 * AccessPoint represents a single AP with unique ID, name, and associated pictures
 * @property id Unique identifier for the access point
 * @property name Display name for the access point (e.g., "AP01")
 * @property pictures Observable list of image URIs as strings
 */
data class AccessPoint(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val pictures: SnapshotStateList<String> = mutableStateListOf()
)

/**
 * Data class representing a project with its related access points
 * Used for Room database relationships
 * @property project Project entity from the database
 * @property accessPoints List of access point entities related to this project
 */
data class ProjectWithAccessPoints(
    @Embedded val project: ProjectEntity, // Project details
    @Relation(parentColumn = "id", entityColumn = "projectId")
    val accessPoints: List<AccessPointEntity> // Related access points
)

/**
 * ModernNewProjectScreen: Screen for creating a new project with access points
 * Allows user to enter project name and expected number of access points
 *
 * @param viewModel ProjectViewModel to store the created project
 * @param navController Navigation controller for screen transitions
 */
@Composable
fun ModernNewProjectScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    // State for form inputs
    var projectName by remember { mutableStateOf(TextFieldValue("")) }
    var expectedAPsText by remember { mutableStateOf(TextFieldValue("0")) }

    Scaffold(
        // Top app bar with screen title
        topBar = {
            TopAppBar(
                title = { Text("New Project", style = MaterialTheme.typography.h6) },
                elevation = 4.dp
            )
        },
        content = { paddingValues ->
            // Form layout
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
            ) {
                // Project name input field
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Expected number of access points input field
                OutlinedTextField(
                    value = expectedAPsText,
                    onValueChange = { expectedAPsText = it },
                    label = { Text("Expected Number of APs") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))

                // Create project button
                Button(
                    onClick = {
                        // Parse input and handle potential invalid number
                        val expectedAPs = expectedAPsText.text.toIntOrNull() ?: 0

                        // Create new project entity
                        val newProject = ProjectEntity(
                            id = UUID.randomUUID().toString(),
                            name = projectName.text
                        )

                        // Generate access points with sequential naming (AP01, AP02, etc.)
                        val accessPoints = (1..expectedAPs).map { i ->
                            val apName = "AP" + i.toString().padStart(2, '0')
                            AccessPointEntity(
                                id = UUID.randomUUID().toString(),
                                projectId = newProject.id,
                                name = apName,
                                pictures = emptyList()
                            )
                        }

                        // Save the project and its access points to the database
                        viewModel.addProjectWithAccessPoints(newProject, accessPoints)

                        // Navigate back to the project list
                        navController.popBackStack()
                    },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Create Project")
                }
            }
        }
    )
}

/**
 * MainActivity: The entry point of the AP Tracker application
 * Sets up the Compose UI and navigation controller
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ModernMainNavHost()
        }
    }
}

/**
 * ModernMainNavHost: Manages navigation between different screens in the app
 * Uses Room database via ProjectViewModel for data persistence
 */
@Composable
fun ModernMainNavHost() {
    // Create navigation controller to handle screen transitions
    val navController = rememberNavController()

    // Initialize ViewModel with factory to provide application context
    val viewModel: ProjectViewModel =
        viewModel(factory = ProjectViewModelFactory(LocalContext.current))

    // Setup navigation graph with all possible routes
    NavHost(navController = navController, startDestination = "projectList") {
        // Project list screen - displays all projects
        composable("projectList") {
            ModernProjectListScreen(viewModel = viewModel, navController = navController)
        }

        // New project screen - form to create a new project
        composable("newProject") {
            ModernNewProjectScreen(viewModel = viewModel, navController = navController)
        }

        // Project detail screen - shows access points for a specific project
        composable(
            "projectDetail/{projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.StringType })
        ) { backStackEntry ->
            // Extract project ID from navigation arguments
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable

            // Find the selected project
            val projectEntity = viewModel.projects.find { it.id == projectId }
            projectEntity?.let { project ->
                // Get access points for this project as a Flow and collect as state
                val accessPoints by viewModel.getAccessPointsForProjectFlow(project.id)
                    .collectAsState(initial = emptyList())

                // Display project details screen with project and its access points
                ModernProjectDetailScreen(
                    projectWithAP = ProjectWithAccessPoints(project, accessPoints),
                    navController = navController
                )
            }
        }

        // Access point detail screen - shows details for a specific AP
        composable(
            "apDetail/{projectId}/{apId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("apId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            // Extract project and AP IDs from navigation arguments
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val apId = backStackEntry.arguments?.getString("apId") ?: return@composable

            // Find the selected project
            val projectEntity = viewModel.projects.find { it.id == projectId }
            if (projectEntity != null) {
                // Get access points for this project as a Flow and collect as state
                val accessPoints by viewModel.getAccessPointsForProjectFlow(projectId)
                    .collectAsState(initial = emptyList())

                // Find the selected access point
                val apEntity = accessPoints.find { it.id == apId }
                if (apEntity != null) {
                    // Display access point details screen
                    ModernAPDetailScreen(
                        projectWithAP = ProjectWithAccessPoints(projectEntity, accessPoints),
                        accessPoint = apEntity,
                        navController = navController,
                        viewModel = viewModel
                    )
                } else {
                    // Display error if access point not found
                    Text("Access Point not found", modifier = Modifier.fillMaxSize(), style = MaterialTheme.typography.h6)
                }
            } else {
                // Display error if project not found
                Text("Project not found", modifier = Modifier.fillMaxSize(), style = MaterialTheme.typography.h6)
            }
        }
    }
}

/**
 * ProjectListItem: Individual project row in the project list
 * @param project Project entity to display
 * @param selectionMode Whether multi-select mode is active
 * @param isSelected Whether this item is currently selected
 * @param onClick Callback for when the item is clicked
 * @param onLongClick Callback for when the item is long-pressed
 */
@Composable
fun ProjectListItem(
    project: ProjectEntity,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show checkbox in selection mode
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() }
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        // Project name
        Text(
            text = project.name,
            style = MaterialTheme.typography.subtitle1,
            modifier = Modifier.weight(1f)
        )
    }
}


/**
 * ModernProjectListScreen: Displays a list of projects with selection and deletion functionality
 * @param viewModel ProjectViewModel to access project data
 * @param navController Navigation controller for screen transitions
 */
@Composable
fun ModernProjectListScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    // State for multi-select deletion feature
    var selectionMode by remember { mutableStateOf(false) }
    val selectedProjects = remember { mutableStateListOf<ProjectEntity>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // BackHandler: Override back button behavior when in selection mode
    // When selection mode is active, clear selection instead of navigating back
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedProjects.clear()
    }

    Scaffold(
        // Top app bar configuration
        topBar = {
            TopAppBar(
                title = {
                    // Show selection count or default title based on mode
                    Text(
                        text = if (selectionMode) "${selectedProjects.size} Selected" else "Projects",
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    // Show back button only in selection mode
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedProjects.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    }
                },
                actions = {
                    // Show delete button only in selection mode with items selected
                    if (selectionMode && selectedProjects.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                }
            )
        },
        // Floating action button for adding new projects
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { navController.navigate("newProject") }) {
                    Icon(Icons.Default.Add, contentDescription = "New Project")
                }
            }
        },
        // Main content area
        content = { paddingValues ->
            // Show empty state message when no projects exist
            if (viewModel.projects.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No projects available. Tap + to add.", style = MaterialTheme.typography.body1)
                }
            } else {
                // Display list of projects
                LazyColumn(
                    modifier = Modifier.padding(paddingValues),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(viewModel.projects) { project ->
                        ProjectListItem(
                            project = project,
                            selectionMode = selectionMode,
                            isSelected = selectedProjects.contains(project),
                            onClick = {
                                if (selectionMode) {
                                    // Toggle selection in selection mode
                                    if (selectedProjects.contains(project)) {
                                        selectedProjects.remove(project)
                                        if (selectedProjects.isEmpty()) selectionMode = false
                                    } else {
                                        selectedProjects.add(project)
                                    }
                                } else {
                                    // Navigate to project details in normal mode
                                    navController.navigate("projectDetail/${project.id}")
                                }
                            },
                            onLongClick = {
                                // Enter selection mode on long press
                                if (!selectionMode) selectionMode = true
                                if (!selectedProjects.contains(project)) {
                                    selectedProjects.add(project)
                                }
                            }
                        )
                        Divider()
                    }
                }
            }
        }
    )
}

/**
 * ModernProjectDetailScreen: Displays details for a project and its access points
 * Features include:
 * - Listing access points with multi-select capability
 * - Adding new access points
 * - Exporting project data
 * - Sorting access points by various criteria
 *
 * @param projectWithAP The project entity with related access points
 * @param navController Navigation controller for screen transitions
 */
@Composable
fun ModernProjectDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(context))

    // Collect access points from the database as a Flow
    val accessPoints by viewModel.getAccessPointsForProjectFlow(projectWithAP.project.id)
        .collectAsState(initial = projectWithAP.accessPoints)

    // Verify image files exist when access point list changes
    // This handles cases where files may have been deleted outside the app
    LaunchedEffect(accessPoints) {
        accessPoints.forEach { ap ->
            // Filter out any file names that don't exist in the filesystem
            val validImages = ap.pictures.filter { pictureName ->
                val file = File(context.filesDir, pictureName)
                file.exists()
            }
            // Update the database if any images were found to be missing
            if (validImages.size != ap.pictures.size) {
                viewModel.updateAccessPoint(ap.copy(pictures = validImages))
            }
        }
    }

    // UI state variables
    var sortOption by remember { mutableStateOf("Alphabetical") }
    var showAddAPDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var apSelectionMode by remember { mutableStateOf(false) }
    val selectedAPs = remember { mutableStateListOf<AccessPointEntity>() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // Sort access points based on selected option
    val sortedAPs = when (sortOption) {
        "Alphabetical" -> accessPoints.sortedWith(compareBy(
            // First sort by the letters in the name (e.g., "AP")
            { it.name.takeWhile { char -> !char.isDigit() } },
            // Then sort by the number (e.g., "01" becomes 1)
            { it.name.dropWhile { char -> !char.isDigit() }.toIntOrNull() ?: 0 }
        ))
        "Number of Pictures" -> accessPoints.sortedByDescending { it.pictures.size }
        "APs without Pictures" -> accessPoints.sortedBy { if (it.pictures.isEmpty()) 0 else 1 }
        else -> accessPoints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Show selection count or project name based on mode
                    Text(
                        text = if (apSelectionMode) "${selectedAPs.size} Selected" else projectWithAP.project.name,
                        style = MaterialTheme.typography.h6
                    )
                },
                navigationIcon = {
                    if (apSelectionMode) {
                        // In selection mode, back button clears selection
                        IconButton(onClick = {
                            apSelectionMode = false
                            selectedAPs.clear()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit selection")
                        }
                    } else {
                        // Normal back button behavior
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (apSelectionMode && selectedAPs.isNotEmpty()) {
                        // Show delete button when items are selected
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    } else {
                        // Show export button in normal mode
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(Icons.Default.Archive, contentDescription = "Export Project")
                        }

                        // Sort dropdown menu
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                            }
                            DropdownMenu(expanded = dropdownExpanded, onDismissRequest = { dropdownExpanded = false }) {
                                DropdownMenuItem(onClick = {
                                    sortOption = "Alphabetical"
                                    dropdownExpanded = false
                                }) { Text("Alphabetical") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "Number of Pictures"
                                    dropdownExpanded = false
                                }) { Text("Number of Pictures") }
                                DropdownMenuItem(onClick = {
                                    sortOption = "APs without Pictures"
                                    dropdownExpanded = false
                                }) { Text("APs without Pictures") }
                            }
                        }
                    }
                },
                elevation = 4.dp
            )
        },
        floatingActionButton = {
            // Only show FAB in normal mode
            if (!apSelectionMode) {
                FloatingActionButton(onClick = { showAddAPDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add AP")
                }
            }
        },
        content = { paddingValues ->
            // List of access points
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(sortedAPs) { ap ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .combinedClickable(
                                onClick = {
                                    if (apSelectionMode) {
                                        // Toggle selection in selection mode
                                        if (selectedAPs.contains(ap)) {
                                            selectedAPs.remove(ap)
                                            if (selectedAPs.isEmpty()) apSelectionMode = false
                                        } else {
                                            selectedAPs.add(ap)
                                        }
                                    } else {
                                        // Navigate to AP details in normal mode
                                        navController.navigate("apDetail/${projectWithAP.project.id}/${ap.id}")
                                    }
                                },
                                onLongClick = {
                                    // Enter selection mode on long press
                                    if (!apSelectionMode) { apSelectionMode = true }
                                    if (!selectedAPs.contains(ap)) { selectedAPs.add(ap) }
                                }
                            )
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (apSelectionMode) {
                            // Show checkbox in selection mode
                            Checkbox(
                                checked = selectedAPs.contains(ap),
                                onCheckedChange = {
                                    if (it) selectedAPs.add(ap) else {
                                        selectedAPs.remove(ap)
                                        if (selectedAPs.isEmpty()) apSelectionMode = false
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // AP information
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // AP name and photo count
                            Text(
                                text = "${ap.name} (${ap.pictures.size})",
                                style = MaterialTheme.typography.subtitle1
                            )
                            // Help text
                            Text(
                                text = "Tap to view pictures",
                                style = MaterialTheme.typography.body2,
                                color = Color.Gray
                            )
                        }
                    }
                    Divider()
                }
            }
        }
    )

    // Confirmation dialog for deleting selected APs
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete ${selectedAPs.size} selected AP(s)?") },
            confirmButton = {
                Button(onClick = {
                    // Delete all selected access points
                    selectedAPs.forEach { ap ->
                        viewModel.deleteAccessPoint(ap.id)
                    }
                    // Clean up selection state
                    selectedAPs.clear()
                    apSelectionMode = false
                    showDeleteConfirmDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Display Add AP dialog when requested
    if (showAddAPDialog) {
        AddAPDialog(
            projectId = projectWithAP.project.id,
            viewModel = viewModel,
            onDismiss = { showAddAPDialog = false }
        )
    }

    // Export dialog (with pre-export preparation)
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            // Handler for Save option
            onSave = { progressState, isZipping, onDismissExport ->
                // Prepare images before export: rename files for consistency
                projectWithAP.accessPoints.forEach { ap ->
                    val renamedImages = renameAPImages(context, ap.name, ap.pictures)
                    if (renamedImages != ap.pictures) {
                        viewModel.updateAccessPoint(ap.copy(pictures = renamedImages))
                    }
                }
                // Export files to zip in background thread
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    withContext(Dispatchers.Main) {
                        // UI updates can be added here
                        val savedFile = saveZipToDocuments(context, zipFile)
                        Toast.makeText(
                            context,
                            "Saved to ${savedFile?.absolutePath ?: "Error saving file"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            // Handler for Share option
            onShare = { progressState, isZipping, onDismissExport ->
                // Prepare images before sharing
                projectWithAP.accessPoints.forEach { ap ->
                    val renamedImages = renameAPImages(context, ap.name, ap.pictures)
                    if (renamedImages != ap.pictures) {
                        viewModel.updateAccessPoint(ap.copy(pictures = renamedImages))
                    }
                }
                // Create and share zip in background thread
                CoroutineScope(Dispatchers.IO).launch {
                    val zipFile = exportProjectToZip(context, projectWithAP, progressState, isZipping, onDismissExport)
                    withContext(Dispatchers.Main) {
                        shareZipFile(context, zipFile)
                    }
                }
            },
            // Warning message for APs with insufficient photos
            warningMessage = accessPoints.filter { it.pictures.size < 2 }
                .joinToString { it.name }
                .takeIf { it.isNotEmpty() }
                ?.let { "Warning: The following AP(s) have fewer than 2 pictures: $it" }
        )
    }

    // Handle system back button to clear selection when active
    BackHandler(enabled = apSelectionMode) {
        apSelectionMode = false
        selectedAPs.clear()
    }
}

/**
 * Custom Saver for preserving Uri objects across recompositions
 * This allows us to maintain camera URI references when the device configuration changes
 */
val UriNullableSaver = Saver<Uri?, String>(
    save = { it?.toString() ?: "" },  // Convert URI to string or empty string if null
    restore = { if (it.isEmpty()) null else Uri.parse(it) }  // Parse string back to URI if not empty
)

/**
 * ModernAPDetailScreen: Displays and manages photos for a specific access point
 *
 * This screen provides:
 * - Photo grid view with multi-select capability
 * - Camera integration for capturing new photos
 * - Deletion of selected photos
 * - Auto-renumbering of photos to maintain sequential naming
 *
 * @param projectWithAP The project that contains this access point
 * @param accessPoint The specific access point to display
 * @param navController Navigation controller for screen transitions
 * @param viewModel ViewModel to persist changes to the database
 */
@Composable
fun ModernAPDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    accessPoint: AccessPointEntity,
    navController: NavHostController,
    viewModel: ProjectViewModel
) {
    val context = LocalContext.current

    // State for the list of image file names stored in the database
    // We use a custom stateSaver to preserve this across recompositions
    val fullSizeImagesState = rememberSaveable(stateSaver = listSaver(
        save = { it },
        restore = { it }
    )) { mutableStateOf(accessPoint.pictures) }

    // Holds the filename for a newly captured image
    var currentFileName by remember { mutableStateOf("") }

    // Validate and reorganize image files when first entering the screen
    LaunchedEffect(accessPoint.id) {
        // Remove any non-existent files from the list
        val validImages = fullSizeImagesState.value.filter { pictureName ->
            val file = File(context.filesDir, pictureName)
            file.exists()
        }

        // Rename files to ensure sequential numbering (AP01-1.jpg, AP01-2.jpg, etc.)
        val renamedImages = renameAPImages(context, accessPoint.name, validImages)

        // Update the database if any changes were made
        if (renamedImages != fullSizeImagesState.value) {
            fullSizeImagesState.value = renamedImages
            viewModel.updateAccessPoint(accessPoint.copy(pictures = renamedImages))
        }
    }

    // UI state variables
    var selectionMode by remember { mutableStateOf(false) } // Whether multi-select mode is active
    val selectedPictures = remember { mutableStateListOf<String>() } // Currently selected pictures
    var showDeleteDialog by remember { mutableStateOf(false) } // Whether to show deletion confirmation

    // URI for the camera output file, preserved across configuration changes
    var imageUri: Uri? by rememberSaveable(stateSaver = UriNullableSaver) { mutableStateOf(null) }

    // Override system back button in selection mode to clear selection instead of navigating back
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedPictures.clear()
    }

    // Camera integration using ActivityResult API
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            // If photo capture was successful, add the new file to our list
            fullSizeImagesState.value = fullSizeImagesState.value + currentFileName

            // Persist changes to database
            viewModel.updateAccessPoint(accessPoint.copy(pictures = fullSizeImagesState.value))
        }
    }

    Scaffold(
        // Top app bar with title and actions
        topBar = {
            TopAppBar(
                title = { Text("${accessPoint.name} Pictures", style = MaterialTheme.typography.h6) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show delete button only in selection mode with items selected
                    if (selectionMode && selectedPictures.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                },
                elevation = 4.dp
            )
        },
        // Floating action button for capturing new photos
        floatingActionButton = {
            FloatingActionButton(onClick = {
                try {
                    // Generate a unique sequential filename for the new image
                    currentFileName = "${accessPoint.name}-${fullSizeImagesState.value.size + 1}.jpg"
                    val file = File(context.filesDir, currentFileName)

                    // Ensure parent directories exist
                    file.parentFile?.mkdirs()

                    // Get a content URI for this file using FileProvider
                    val authority = "${context.packageName}.provider"
                    val uri = FileProvider.getUriForFile(
                        context,
                        authority,
                        file
                    )
                    imageUri = uri

                    // Launch camera with this URI as the output destination
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    // Handle potential FileProvider errors
                    Log.e("ModernAPDetailScreen", "Error launching camera: ${e.message}", e)
                    Toast.makeText(context, "Error launching camera: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Picture")
            }
        },
        // Main content area
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (fullSizeImagesState.value.isEmpty()) {
                    // Empty state when no images are available
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No pictures available. Tap + to add.", style = MaterialTheme.typography.body1)
                    }
                } else {
                    // Grid display of images
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(fullSizeImagesState.value) { pictureName ->
                            val file = File(context.filesDir, pictureName)

                            // Safety check: skip missing files
                            if (!file.exists()) {
                                return@items
                            }

                            // Get content URI with error handling
                            val imageUri = try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                            } catch (e: Exception) {
                                Log.e("ModernAPDetailScreen", "Error getting URI for file: $pictureName", e)
                                null
                            }

                            // Skip rendering if we couldn't get a valid URI
                            if (imageUri == null) {
                                return@items
                            }

                            // Image tile with selection capabilities
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)  // Keep square aspect ratio
                                    .combinedClickable(
                                        onClick = {
                                            if (selectionMode) {
                                                // Toggle selection in selection mode
                                                if (selectedPictures.contains(pictureName)) {
                                                    selectedPictures.remove(pictureName)
                                                    if (selectedPictures.isEmpty()) selectionMode = false
                                                } else {
                                                    selectedPictures.add(pictureName)
                                                }
                                            } else {
                                                // Open image viewer in normal mode
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                                        setDataAndType(imageUri, "image/*")
                                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) {
                                                    Log.e("ModernAPDetailScreen", "Error viewing image: ${e.message}", e)
                                                    Toast.makeText(context, "Error viewing image", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            // Enter selection mode on long press
                                            if (!selectionMode) selectionMode = true
                                            if (!selectedPictures.contains(pictureName)) {
                                                selectedPictures.add(pictureName)
                                            }
                                        }
                                    )
                            ) {

                                // Display the image using Coil's AsyncImage with explicit cache handling
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .diskCachePolicy(CachePolicy.DISABLED) // Disable disk caching
                                        .memoryCachePolicy(CachePolicy.DISABLED) // Disable memory caching
                                        .build(),
                                    contentDescription = "Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Overlay for selected items
                                if (selectionMode && selectedPictures.contains(pictureName)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),  // Semi-transparent overlay
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    // Confirmation dialog for deleting photos
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                val count = if (selectionMode) selectedPictures.size else 1
                Text("Are you sure you want to delete $count image(s)?")
            },
            confirmButton = {
                Button(onClick = {
                    if (selectionMode) {
                        // Delete the actual files from storage
                        selectedPictures.forEach { pictureName ->
                            val file = File(context.filesDir, pictureName)
                            if (file.exists()) file.delete()
                        }

                        // Remove references from our state
                        fullSizeImagesState.value = fullSizeImagesState.value - selectedPictures.toSet()

                        // Renumber remaining files to maintain sequential naming
                        val renamedImages = renameAPImages(context, accessPoint.name, fullSizeImagesState.value)
                        fullSizeImagesState.value = renamedImages

                        // Update database with changes
                        viewModel.updateAccessPoint(accessPoint.copy(pictures = renamedImages))

                        // Clear selection state
                        selectedPictures.clear()
                        selectionMode = false
                    }
                    showDeleteDialog = false
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Saves an image to device's scoped storage using MediaStore API
 * This approach doesn't require READ/WRITE_EXTERNAL_STORAGE permissions on Android 10+
 *
 * @param context Application context
 * @param uri Source URI of the image to save
 * @param accessPointName Name of the access point (for consistent naming)
 * @param pictureCount Current picture count (for sequential naming)
 * @return URI of the saved image or null if saving failed
 */
fun saveImageToScopedStorage(context: Context, uri: Uri, accessPointName: String, pictureCount: Int): Uri? {
    // Create a consistently formatted filename with sequential numbering
    val formattedFileName = "${accessPointName}-${pictureCount + 1}.jpg"

    // Prepare ContentValues for MediaStore insertion
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, formattedFileName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        // On Android 10+ (Q), we can specify a relative path
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/APTracker")
        }
    }

    // Insert the item into MediaStore to get a content URI
    val imageUri: Uri? = context.contentResolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    )

    // Process the image if we got a valid URI
    imageUri?.let {
        context.contentResolver.openOutputStream(it)?.use { outputStream ->
            // Read the original EXIF orientation data
            val orientation = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                ExifInterface(inputStream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            // Decode the source image
            val bitmap = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it)
            }

            if (bitmap != null) {
                // Apply any needed rotation based on EXIF data
                val rotatedBitmap = rotateBitmapIfNeeded(bitmap, orientation)
                // Save to the output stream with high quality
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            } else {
                Log.e("saveImageToScopedStorage", "Failed to decode bitmap from input stream.")
            }
        }
    }

    return imageUri
}

/**
 * Rotates a bitmap based on EXIF orientation data
 *
 * @param bitmap The source bitmap to rotate
 * @param orientation EXIF orientation value from the image
 * @return A rotated bitmap if needed, or the original bitmap
 */
fun rotateBitmapIfNeeded(bitmap: Bitmap, orientation: Int): Bitmap {
    // Determine rotation angle based on EXIF orientation tag
    val degrees = when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f  // No rotation needed
    }

    // Only create a new bitmap if rotation is needed
    return if (degrees != 0f) {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    } else {
        bitmap  // Return original if no rotation needed
    }
}

/**
 * Saves a bitmap to a file in the app's internal storage
 *
 * @param context Application context
 * @param bitmap The bitmap to save
 * @param fileName Name for the saved file
 * @return Absolute path to the saved file
 */
fun saveBitmapToFile(context: Context, bitmap: Bitmap, fileName: String): String {
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
    }
    return file.absolutePath
}

/**
 * Creates a ZIP file containing all images from a project
 * Shows progress updates and allows for cancellation
 *
 * @param context Application context
 * @param projectWithAP Project data with access points
 * @param progressState Mutable state for tracking progress percentage
 * @param isZipping Mutable state indicating if zipping is in progress
 * @param onDismiss Callback when operation completes
 * @return The created ZIP file
 */
fun exportProjectToZip(
    context: Context,
    projectWithAP: ProjectWithAccessPoints,
    progressState: MutableState<Int>,
    isZipping: MutableState<Boolean>,
    onDismiss: () -> Unit
): File {
    // Create a zip file in the app's cache directory
    val zipFile = File(context.cacheDir, "${projectWithAP.project.name}.zip")

    // Calculate total files for progress tracking
    val totalFiles = projectWithAP.accessPoints.sumOf { it.pictures.size }
    var processedFiles = 0

    // Create ZIP stream and add files
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        projectWithAP.accessPoints.forEach { ap ->
            ap.pictures.forEachIndexed { index, pictureName ->
                // Get source file from internal storage
                val file = File(context.filesDir, pictureName)
                if (file.exists()) {
                    // Create consistent filename for the ZIP entry
                    val formattedFileName = "${ap.name}-${index + 1}.jpg"
                    val entry = ZipEntry(formattedFileName)

                    // Add file to ZIP
                    zos.putNextEntry(entry)
                    file.inputStream().copyTo(zos)
                    zos.closeEntry()

                    // Update progress
                    processedFiles++
                    progressState.value = ((processedFiles.toFloat() / totalFiles) * 100).toInt()
                }
            }
        }
    }

    // Switch back to Main thread to update UI when done
    CoroutineScope(Dispatchers.Main).launch {
        isZipping.value = false
        onDismiss() // close the export dialog
    }

    return zipFile
}

/**
 * Converts a URI to a File object
 *
 * @param context Application context
 * @param uri URI to convert
 * @return File object or null if conversion failed
 */
fun getFileFromUri(context: Context, uri: Uri): File? {
    val filePathColumn = arrayOf(MediaStore.Images.Media.DATA)
    val cursor = context.contentResolver.query(uri, filePathColumn, null, null, null)

    return cursor?.use {
        if (it.moveToFirst()) {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val filePath = it.getString(columnIndex)
            if (filePath != null) File(filePath) else null
        } else {
            null
        }
    }
}

/**
 * Dialog for exporting project data with progress indicator
 * Provides options to save locally or share with other apps
 *
 * @param onDismiss Callback when dialog is dismissed
 * @param onSave Callback for saving to documents directory
 * @param onShare Callback for sharing via system share sheet
 * @param warningMessage Optional warning message to display
 */
@Composable
fun ExportDialog(
    onDismiss: () -> Unit,
    onSave: (MutableState<Int>, MutableState<Boolean>, () -> Unit) -> Unit,
    onShare: (MutableState<Int>, MutableState<Boolean>, () -> Unit) -> Unit,
    warningMessage: String? = null
) {
    // State for tracking export progress
    val isZipping = remember { mutableStateOf(false) }
    val progress = remember { mutableStateOf(0) }

    // Prevent dismissal during zipping operation
    Dialog(onDismissRequest = { if (!isZipping.value) onDismiss() }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog title changes based on state
                Text(
                    text = if (isZipping.value) "Zipping Files..." else "Export Project",
                    style = MaterialTheme.typography.h6
                )

                // Show progress bar during zipping
                if (isZipping.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress.value / 100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show warning message if provided
                warningMessage?.let {
                    if (!isZipping.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState())) {
                            Text(text = it, color = Color.Red, style = MaterialTheme.typography.body2)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons (only shown when not zipping)
                if (!isZipping.value) {
                    // Share and Cancel buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isZipping.value = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    onShare(progress, isZipping, onDismiss)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Share")
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save to Documents button
                    Button(
                        onClick = {
                            isZipping.value = true
                            CoroutineScope(Dispatchers.IO).launch {
                                onSave(progress, isZipping, onDismiss)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save to Documents")
                    }
                }
            }
        }
    }
}

/**
 * Dialog for adding a new Access Point to a project
 *
 * @param projectId ID of the project to add the AP to
 * @param viewModel ViewModel for database operations
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun AddAPDialog(
    projectId: String,
    viewModel: ProjectViewModel,
    onDismiss: () -> Unit
) {
    // State for the input field
    var apName by remember { mutableStateOf(TextFieldValue("")) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Access Point") },
        text = {
            OutlinedTextField(
                value = apName,
                onValueChange = { apName = it },
                label = { Text("AP Name (e.g., AP05)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = {
                val name = apName.text.trim()
                if (name.isNotEmpty()) {
                    // Create new AccessPoint entity
                    val newAP = AccessPointEntity(
                        id = UUID.randomUUID().toString(),
                        projectId = projectId,
                        name = name,
                        pictures = emptyList()
                    )
                    // Save to database
                    viewModel.addAccessPoint(newAP)
                    onDismiss()
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/**
 * Saves a ZIP file to the device's Documents directory
 *
 * @param context Application context
 * @param zipFile The ZIP file to save
 * @return The saved file or null if saving failed
 */
fun saveZipToDocuments(context: Context, zipFile: File): File? {
    return try {
        // Get system Documents directory
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        if (!documentsDir.exists()) {
            documentsDir.mkdirs()
        }

        // Create destination file
        val destinationFile = File(documentsDir, zipFile.name)

        // Copy ZIP file to destination
        zipFile.copyTo(destinationFile, overwrite = true)
        destinationFile
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * Shares a ZIP file using the system share sheet
 *
 * @param context Application context
 * @param zipFile The ZIP file to share
 */
fun shareZipFile(context: Context, zipFile: File) {
    // Create content URI using FileProvider for secure sharing
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        zipFile
    )

    // Create and launch share intent
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share ZIP file"))
}

/**
 * Renames all image files for the given access point so that numbering starts at 1
 *
 * @param context Application context
 * @param accessPointName Name of the access point (e.g., "AP01")
 * @param images List of file names (not absolute paths) stored in filesDir
 * @return New list of file names after renaming
 */
fun renameAPImages(context: Context, accessPointName: String, images: List<String>): List<String> {
    return images.mapIndexed { index, fileName ->
        // Generate new sequential filename (AP01-1.jpg, AP01-2.jpg, etc.)
        val newIndex = index + 1
        val newFileName = "$accessPointName-$newIndex.jpg"

        // Get file references
        val oldFile = File(context.filesDir, fileName)
        val newFile = File(context.filesDir, newFileName)

        // Rename file if it exists and name needs to change
        if (oldFile.exists() && oldFile.name != newFileName) {
            oldFile.renameTo(newFile)
        }

        // Return just the file name (not full path)
        newFile.name
    }
}