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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.TextButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.CubicBezierEasing
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import kotlinx.coroutines.launch
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
import androidx.compose.foundation.shape.CircleShape
import android.graphics.BitmapFactory
import android.util.Log
import android.content.ContentValues
import android.provider.MediaStore
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.material.icons.filled.MoreVert
import android.graphics.drawable.BitmapDrawable

// For drawing the circle and handles
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType

// For math operations
import kotlin.math.abs
import androidx.core.graphics.scale
import androidx.navigation.NavController


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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNewProjectScreen(
    viewModel: ProjectViewModel,
    navController: NavHostController
) {
    // State for form inputs
    var projectName by remember { mutableStateOf(TextFieldValue("")) }
    var expectedAPsText by remember { mutableStateOf(TextFieldValue("0")) }
    // Create a focus requester for the first field
    val projectNameFocusRequester = remember { FocusRequester() }

    Scaffold(
        // Top app bar with screen title
        topBar = {
            TopAppBar(
                title = { Text("New Project", style = MaterialTheme.typography.titleLarge) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        // Form layout
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Project name input field with auto-focus and capitalization
            OutlinedTextField(
                value = projectName,
                onValueChange = { projectName = it },
                label = { Text("Project Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(projectNameFocusRequester),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words
                ),
                singleLine = true
            )

            // Request focus when composable is first displayed
            LaunchedEffect(Unit) {
                projectNameFocusRequester.requestFocus()
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expected number of access points input field
            OutlinedTextField(
                value = expectedAPsText,
                onValueChange = { expectedAPsText = it },
                label = { Text("Expected Number of APs") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                ),
                singleLine = true
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
                            name = apName
                            // No pictures parameter needed anymore
                        )
                    }

                    // Save the project and its access points to the database
                    viewModel.addProjectWithAccessPoints(newProject, accessPoints)

                    // Navigate back to the project list
                    navController.popBackStack()
                },
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Create Project")
            }
        }
    }
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
    val navController = rememberNavController()
    val viewModel: ProjectViewModel =
        viewModel(factory = ProjectViewModelFactory(LocalContext.current))

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

        composable(
            "imageViewer/{projectId}/{apId}/{imageId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("apId") { type = NavType.StringType },
                navArgument("imageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val apId = backStackEntry.arguments?.getString("apId") ?: return@composable
            val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable

            ImageViewerScreen(
                projectId = projectId,
                apId = apId,
                imageId = imageId,
                navController = navController
            )
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

            // Loading and error states
            var isLoading by remember { mutableStateOf(true) }
            var loadError by remember { mutableStateOf<String?>(null) }
            var retryCount by remember { mutableStateOf(0) }
            val maxRetries = 3

            // State to track if data is fully ready to display
            var dataReady by remember { mutableStateOf(false) }

            // Remember the found entities
            val projectEntity = remember { mutableStateOf<ProjectEntity?>(null) }
            val apEntity = remember { mutableStateOf<AccessPointEntity?>(null) }
            val accessPoints = remember { mutableStateListOf<AccessPointEntity>() }

            // Force refresh counter to trigger reload
            var refreshCounter by remember { mutableStateOf(0) }

            // Find the selected project
            LaunchedEffect(projectId, refreshCounter) {
                projectEntity.value = viewModel.projects.find { it.id == projectId }
            }

            // Use LaunchedEffect to load the AP with proper error handling and initial delay
            // Use LaunchedEffect with the vararg version that accepts multiple keys
            LaunchedEffect(projectId, apId, retryCount, refreshCounter) {
                try {
                    // Reset state at the beginning of each attempt
                    isLoading = true
                    dataReady = false

                    // Check if project exists
                    if (projectEntity.value == null) {
                        loadError = "Project not found"
                        isLoading = false
                        return@LaunchedEffect
                    }

                    // Force refresh the AP to ensure it's in the database
                    viewModel.refreshAccessPointsForProject(projectId)

                    // Add initial delay before first attempt to ensure database is ready
                    val initialDelay = if (retryCount == 0) 500L else 300L * (retryCount + 1)
                    delay(initialDelay)

                    // Query for the specific AP - use a direct suspended call
                    val ap = viewModel.getAccessPointById(apId)
                    Log.d("Navigation", "Attempt ${retryCount+1}: AP query result: $ap")

                    if (ap == null) {
                        // AP not found, try to retry
                        if (retryCount < maxRetries) {
                            // Increase delay for each retry attempt
                            Log.d("Navigation", "AP $apId not found, retry #${retryCount + 1} in ${initialDelay}ms")
                            retryCount++  // This will trigger the LaunchedEffect again
                        } else {
                            // Max retries reached, show error
                            loadError = "Access Point not found after $maxRetries attempts. It may have been deleted."
                            isLoading = false
                        }
                    } else {
                        // AP found, store it and reset error state
                        apEntity.value = ap
                        loadError = null

                        // Get all access points for this project
                        val points = viewModel.getAccessPointsForProjectFlow(projectId).first()
                        accessPoints.clear()
                        accessPoints.addAll(points)

                        // Set loading complete and data ready
                        isLoading = false
                        dataReady = true
                    }
                } catch (e: Exception) {
                    Log.e("Navigation", "Error loading AP detail: ${e.message}", e)
                    loadError = "Error: ${e.message}"
                    isLoading = false
                }
            }

            // Add a button to retry or go back in the loading UI
            if (isLoading || !dataReady) {
                // Show loading indicator while loading or before data is ready
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CircularProgressIndicator()

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (retryCount > 0) "Loading... (Attempt ${retryCount + 1}/$maxRetries)" else "Loading...",
                            style = MaterialTheme.typography.bodyLarge
                        )

                        // Add a retry button if we're still loading
                        if (retryCount > 0) {
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    // Force refresh by incrementing counter
                                    refreshCounter++
                                    // Reset retry count
                                    retryCount = 0
                                }
                            ) {
                                Text("Retry")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            } else if (loadError != null) {
                // Show error with retry button
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = loadError ?: "Unknown error",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                // Retry loading
                                isLoading = true
                                loadError = null
                                retryCount = 0
                                refreshCounter++
                            }
                        ) {
                            Text("Retry")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { navController.popBackStack() }
                        ) {
                            Text("Go Back")
                        }
                    }
                }
            } else if (dataReady && projectEntity.value != null && apEntity.value != null) {
                // Data is ready, show the AP detail screen
                ModernAPDetailScreen(
                    projectWithAP = ProjectWithAccessPoints(projectEntity.value!!, accessPoints.toList()),
                    accessPoint = apEntity.value!!,
                    navController = navController,
                    viewModel = viewModel
                )
            }
        }

        composable(
            "originalImageViewer/{projectId}/{apId}/{imageId}",
            arguments = listOf(
                navArgument("projectId") { type = NavType.StringType },
                navArgument("apId") { type = NavType.StringType },
                navArgument("imageId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getString("projectId") ?: return@composable
            val apId = backStackEntry.arguments?.getString("apId") ?: return@composable
            val imageId = backStackEntry.arguments?.getString("imageId") ?: return@composable

            OriginalImageViewerScreen(
                projectId = projectId,
                apId = apId,
                imageId = imageId,
                navController = navController
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerScreen(
    projectId: String,
    apId: String,
    imageId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(context))

    // States for image data
    val imageEntity = remember { mutableStateOf<ImageEntity?>(null) }
    val originalImageEntity = remember { mutableStateOf<ImageEntity?>(null) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // UI states
    var showCircle by remember { mutableStateOf(false) }
    var circlePosition by remember { mutableStateOf(Offset.Zero) }
    var circleRadius by remember { mutableStateOf(100f) }
    var circleThickness by remember { mutableStateOf(35f) }
    var selectedHandle by remember { mutableStateOf<String?>(null) }

    // Container and image size states
    val containerSize = remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }
    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }

    var isExplicitlyViewingOriginal by remember { mutableStateOf(false) }

    // Add loading state
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }

    // Load image data when screen is shown
    LaunchedEffect(imageId) {
        Log.d("ImageViewerScreen", "Loading image data for imageId: $imageId, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")

        withContext(Dispatchers.IO) {
            try {
                // Get current image
                val image = viewModel.getImageById(imageId)
                Log.d("ImageViewerScreen", "Current image: ${image?.filename}, isCircled: ${image?.isCircled}, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")

                withContext(Dispatchers.Main) {
                    imageEntity.value = image
                    Log.d("ImageViewerScreen", "Updated imageEntity: ${imageEntity.value?.filename}, isCircled: ${imageEntity.value?.isCircled}")
                }

                // Get original if this is a circled version
                if (image?.isCircled == true && image.originalImageId != null) {
                    Log.d("ImageViewerScreen", "Getting original image with ID: ${image.originalImageId}")
                    val original = viewModel.getImageById(image.originalImageId)
                    Log.d("ImageViewerScreen", "Original image: ${original?.filename}")

                    withContext(Dispatchers.Main) {
                        originalImageEntity.value = original
                        Log.d("ImageViewerScreen", "Updated originalImageEntity: ${originalImageEntity.value?.filename}, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")
                    }
                } else {
                    // Reset originalImageEntity if the current image is not a circled version
                    withContext(Dispatchers.Main) {
                        originalImageEntity.value = null
                        Log.d("ImageViewerScreen", "Reset originalImageEntity to null, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")
                    }
                }

                // Only redirect to circled version if we're not explicitly viewing the original
                if (!isExplicitlyViewingOriginal && imageEntity.value?.isCircled == false) {
                    val circled = viewModel.getCircledVersionOfImage(imageEntity.value?.id ?: "")
                    Log.d("ImageViewerScreen", "Circled version: ${circled?.filename}, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")

                    if (circled != null) {
                        withContext(Dispatchers.Main) {
                            Log.d("ImageViewerScreen", "Navigating to circled version: ${circled.id}")
                            navController.navigate("imageViewer/$projectId/$apId/${circled.id}") {
                                popUpTo("imageViewer/$projectId/$apId/$imageId") { inclusive = true }
                            }
                        }
                    }
                }

                // Reset the flag only if the current image is not the original and we're not explicitly viewing the original
                if (!isExplicitlyViewingOriginal && imageEntity.value?.id != originalImageEntity.value?.id) {
                    withContext(Dispatchers.Main) {
                        isExplicitlyViewingOriginal = false
                        Log.d("ImageViewerScreen", "isExplicitlyViewingOriginal reset to false")
                    }
                }

                // Update loading state when complete
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("ImageViewerScreen", "Error loading image data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                    isLoading = false
                    loadError = true
                }
            }
        }
    }

    // Initialize circle position when shown
    LaunchedEffect(showCircle, containerSize.value) {
        if (showCircle && circlePosition == Offset.Zero && containerSize.value != Size.Zero) {
            // Position circle at 25% from top, center horizontally
            circlePosition = Offset(
                containerSize.value.width / 2,
                containerSize.value.height * 0.25f
            )
        }
    }

    // Check if this is a circled version
    val isCircledVersion = imageEntity.value?.isCircled == true

    // Image transform state setup
    var targetScale by remember { mutableStateOf(1f) }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }
    var isGestureInProgress by remember { mutableStateOf(false) }

    // Animation values
    val scaleAnimated by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = { isGestureInProgress = false },
        label = "scaleAnimation"
    )

    val offsetXAnimated by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "offsetXAnimation"
    )

    val offsetYAnimated by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "offsetYAnimation"
    )

    // Rendering values
    val scale = if (isGestureInProgress) targetScale else scaleAnimated
    val offsetX = if (isGestureInProgress) targetOffsetX else offsetXAnimated
    val offsetY = if (isGestureInProgress) targetOffsetY else offsetYAnimated

    // Get file reference and URI
    val file = imageEntity.value?.let { File(context.filesDir, it.filename) }
    val imageUri = try {
        file?.let {
            if (it.exists()) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
            } else {
                Log.e("ImageViewerScreen", "File doesn't exist: ${it.absolutePath}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("ImageViewerScreen", "Error getting URI: ${e.message}", e)
        null
    }

    // Function to calculate max allowed offsets
    fun calculateMaxOffsets(scale: Float): Pair<Float, Float> {
        val scaledImageWidth = imageSize.width * scale
        val scaledImageHeight = imageSize.height * scale
        val containerWidth = containerSize.value.width
        val containerHeight = containerSize.value.height

        val horizontalOverflow = (scaledImageWidth - containerWidth) / 2f
        val verticalOverflow = (scaledImageHeight - containerHeight) / 2f

        return Pair(
            horizontalOverflow.coerceAtLeast(0f),
            verticalOverflow.coerceAtLeast(0f)
        )
    }

    // Transform state for gesture handling
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        isGestureInProgress = true

        // Apply zoom constraints
        targetScale = (targetScale * zoomChange).coerceIn(1f, 3f)

        if (targetScale > 1f) {
            // Calculate max offsets
            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(targetScale)

            // Apply scaled delta
            val scaledDeltaX = offsetChange.x * targetScale
            val scaledDeltaY = offsetChange.y * targetScale

            // Constrain offsets
            targetOffsetX = (targetOffsetX + scaledDeltaX).coerceIn(-maxOffsetX, maxOffsetX)
            targetOffsetY = (targetOffsetY + scaledDeltaY).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            // Reset offset at minimum zoom
            targetOffsetX = 0f
            targetOffsetY = 0f
        }
    }

    // Function to save image with circle
    fun saveImageWithCircle() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get original bitmap
                val originalBitmap = imageBitmap.value ?: return@launch

                // Create mutable copy
                val circledBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

                // Create canvas
                val canvas = android.graphics.Canvas(circledBitmap)

                // Calculate scale factors
                val containerWidth = containerSize.value.width
                val containerHeight = containerSize.value.height
                val imageWidth = originalBitmap.width.toFloat()
                val imageHeight = originalBitmap.height.toFloat()

                // Calculate the actual displayed bounds of the image within the container
                val containerAspectRatio = containerWidth / containerHeight
                val imageAspectRatio = imageWidth / imageHeight

                // Calculate the actual dimensions of the displayed image (accounting for letterboxing)
                val actualDisplayedWidth: Float
                val actualDisplayedHeight: Float
                val offsetX: Float
                val offsetY: Float

                if (imageAspectRatio > containerAspectRatio) {
                    // Image is wider than container (letterboxing on top and bottom)
                    actualDisplayedWidth = containerWidth
                    actualDisplayedHeight = containerWidth / imageAspectRatio
                    offsetX = 0f
                    offsetY = (containerHeight - actualDisplayedHeight) / 2f
                } else {
                    // Image is taller than container (letterboxing on sides)
                    actualDisplayedHeight = containerHeight
                    actualDisplayedWidth = containerHeight * imageAspectRatio
                    offsetX = (containerWidth - actualDisplayedWidth) / 2f
                    offsetY = 0f
                }

                // Adjust the circle position to account for letterboxing
                val adjustedCircleX = (circlePosition.x - offsetX).coerceIn(0f, actualDisplayedWidth)
                val adjustedCircleY = (circlePosition.y - offsetY).coerceIn(0f, actualDisplayedHeight)

                // Scale the adjusted position to the actual image dimensions
                val scaledCircleX = adjustedCircleX * (imageWidth / actualDisplayedWidth)
                val scaledCircleY = adjustedCircleY * (imageHeight / actualDisplayedHeight)

                // Scale the radius and stroke width proportionally
                val scaledCircleRadius = circleRadius * (imageWidth / actualDisplayedWidth)
                val scaledStrokeWidth = circleThickness * (imageWidth / actualDisplayedWidth)

                // Create paint
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.RED
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = scaledStrokeWidth
                    isAntiAlias = true
                }

                // Draw circle
                canvas.drawCircle(scaledCircleX, scaledCircleY, scaledCircleRadius, paint)

                // Create circled filename
                val originalFilename = imageEntity.value?.filename ?: return@launch
                val extension = originalFilename.substringAfterLast('.')
                val baseName = originalFilename.substringBeforeLast('.')
                val circledFilename = "${baseName}_circle_.${extension}"

                // Save new image
                val circledFile = File(context.filesDir, circledFilename)
                FileOutputStream(circledFile).use { out ->
                    circledBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }

                // Create new image entity for circled version
                val currentImage = imageEntity.value ?: return@launch
                val circledImageEntity = ImageEntity(
                    accessPointId = apId,
                    filename = circledFilename,
                    isCircled = true,
                    originalImageId = currentImage.id,
                    orderIndex = currentImage.orderIndex
                )

                // Save to database
                viewModel.addImage(circledImageEntity)

                // Clear image cache
                withContext(Dispatchers.Main) {
                    coil.ImageLoader.Builder(context)
                        .allowHardware(true)
                        .crossfade(true)
                        .build()

                    // Extract AP name for the toast message
                    val apName = currentImage.filename.let {
                        val projectIdAndApName = it.split('_')
                        val apAndNumber = if (projectIdAndApName.size > 1) {
                            val apAndNumberParts = projectIdAndApName[1].split('-')
                            if (apAndNumberParts.size > 1) {
                                "${apAndNumberParts[0]}-${apAndNumberParts[1].substringBefore('.')}"
                            } else {
                                apAndNumberParts[0]
                            }
                        } else {
                            "Image"
                        }
                        apAndNumber
                    }

                    // Show toast instead of dialog
                    Toast.makeText(
                        context,
                        "$apName with circle has been saved",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Hide circle UI
                    showCircle = false

                    // Small delay to ensure database update completes
                    delay(200)

                    // Navigate to the circled version
                    navController.navigate("imageViewer/$projectId/$apId/${circledImageEntity.id}") {
                        popUpTo("imageViewer/$projectId/$apId/$imageId") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageViewerScreen", "Error saving image with circle: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Function to view original image
    fun viewOriginalImage() {
        if (isCircledVersion && originalImageEntity.value != null) {
            // First, verify that the original entity has a valid ID
            val originalId = originalImageEntity.value?.id
            if (originalId != null) {
                // Navigate to the original image viewer
                navController.navigate("originalImageViewer/$projectId/$apId/$originalId")
            } else {
                // Show error toast if original ID is null
                Toast.makeText(context, "Error: Original image not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Show toast if not a circled version or original not found
            Toast.makeText(context, "No original image available", Toast.LENGTH_SHORT).show()
        }
    }

    // Add a state variable for the confirmation dialog
    var showRevertConfirmation by remember { mutableStateOf(false) }
    // Add a loading state for when reverting
    var isReverting by remember { mutableStateOf(false) }

    // Update the revertToOriginal function to show the dialog instead of immediately deleting
    fun revertToOriginal() {
        if (isCircledVersion && originalImageEntity.value != null) {
            showRevertConfirmation = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Extract AP name from the entity
                    val apName = imageEntity.value?.let {
                        val projectIdAndApName = it.filename.split('_')
                        val apAndNumber = if (projectIdAndApName.size > 1) {
                            val apAndNumberParts = projectIdAndApName[1].split('-')
                            if (apAndNumberParts.size > 1) {
                                // Format as "AP01-1" (AP name and image number)
                                "${apAndNumberParts[0]}-${apAndNumberParts[1].substringBefore('.')}"
                            } else {
                                apAndNumberParts[0]
                            }
                        } else {
                            "Image"
                        }
                        apAndNumber
                    } ?: "Image"
                    Text("$apName", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Circle editing actions
                    if (showCircle) {
                        // Save circle button
                        IconButton(onClick = { saveImageWithCircle() }) {
                            Icon(Icons.Default.Check, contentDescription = "Save with Circle")
                        }
                    } else if (!isCircledVersion) {
                        // Add circle button for non-circled images
                        IconButton(onClick = { showCircle = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Circle")
                        }
                    }

                    // Options menu for circled images
                    if (isCircledVersion) {
                        Box {
                            IconButton(onClick = { showOptionsMenu = !showOptionsMenu }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "More Options")
                            }

                            DropdownMenu(
                                expanded = showOptionsMenu,
                                onDismissRequest = { showOptionsMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("View Original") },
                                    onClick = {
                                        isExplicitlyViewingOriginal = true
                                        viewOriginalImage()
                                        showOptionsMenu = false
                                    }
                                )

                                DropdownMenuItem(
                                    text = { Text("Revert to Original") },
                                    onClick = {
                                        revertToOriginal()
                                        showOptionsMenu = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            // Circle thickness slider
            AnimatedVisibility(
                visible = showCircle,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Text(
                        "Circle Thickness: ${circleThickness.toInt()}px",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Slider(
                        value = circleThickness,
                        onValueChange = { circleThickness = it },
                        valueRange = 5f..60f,
                        steps = 11,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .onSizeChanged { size ->
                    containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
                },
            contentAlignment = Alignment.Center
        ) {
            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            // Show error message
            else if (loadError || imageUri == null) {
                Text(
                    "Error loading image",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            // Show image
            else {
                // Image with zoom/pan capabilities
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(imageUri)
                        .crossfade(true)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = "Image",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .transformable(state = state)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapOffset ->
                                    isGestureInProgress = false
                                    if (targetScale > 1f) {
                                        // Reset zoom
                                        targetScale = 1f
                                        targetOffsetX = 0f
                                        targetOffsetY = 0f
                                    } else {
                                        // Zoom in on tap point
                                        targetScale = 2.5f

                                        // Calculate container center
                                        val containerSize = containerSize.value
                                        val containerCenterX = containerSize.width / 2f
                                        val containerCenterY = containerSize.height / 2f

                                        // Calculate distance from center
                                        val distanceX = tapOffset.x - containerCenterX
                                        val distanceY = tapOffset.y - containerCenterY

                                        // Apply scaling factor to offset
                                        targetOffsetX = distanceX * (1 - 1/targetScale) * -1f
                                        targetOffsetY = distanceY * (1 - 1/targetScale) * -1f

                                        // Constrain offsets
                                        val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(targetScale)
                                        targetOffsetX = targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        targetOffsetY = targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                    }
                                }
                            )
                        },
                    contentScale = ContentScale.Fit,
                    onSuccess = { success ->
                        val drawable = success.result.drawable
                        // Store image dimensions
                        imageSize = Size(
                            drawable.intrinsicWidth.toFloat(),
                            drawable.intrinsicHeight.toFloat()
                        )

                        // Store bitmap for later use
                        try {
                            imageBitmap.value = (drawable as? BitmapDrawable)?.bitmap
                        } catch (e: Exception) {
                            Log.e("ImageViewerScreen", "Error getting bitmap: ${e.message}", e)
                        }
                    }
                )

                // Circle overlay for editing
                if (showCircle) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        // Determine if we're dragging the circle or a handle
                                        val distance = (offset - circlePosition).getDistance()
                                        val handleDistance = abs(distance - circleRadius)

                                        if (handleDistance < 40f) {
                                            // Dragging a handle to resize
                                            selectedHandle = "resize"
                                        } else if (distance < circleRadius) {
                                            // Dragging the circle itself
                                            selectedHandle = "move"
                                        } else {
                                            // Not dragging anything
                                            selectedHandle = null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        when (selectedHandle) {
                                            "move" -> {
                                                // Move the entire circle
                                                circlePosition = Offset(
                                                    (circlePosition.x + dragAmount.x).coerceIn(0f, containerSize.value.width),
                                                    (circlePosition.y + dragAmount.y).coerceIn(0f, containerSize.value.height)
                                                )
                                            }
                                            "resize" -> {
                                                // Calculate new radius based on drag
                                                val dragOffset = change.position
                                                val newDistance = (dragOffset - circlePosition).getDistance()
                                                // Update radius with constraints
                                                circleRadius = newDistance.coerceIn(50f, 500f)
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        selectedHandle = null
                                    }
                                )
                            }
                    ) {
                        // Draw the circle
                        drawCircle(
                            color = Color.Red,
                            center = circlePosition,
                            radius = circleRadius,
                            style = Stroke(width = circleThickness)
                        )

                        // Draw resize handle
                        drawCircle(
                            color = Color.Red,
                            center = Offset(
                                circlePosition.x + circleRadius * 0.707f,
                                circlePosition.y + circleRadius * 0.707f
                            ),
                            radius = 12f,
                            style = Fill
                        )
                    }
                }

                // Reset zoom button
                AnimatedVisibility(
                    visible = scale > 1.01f && !showCircle,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(36.dp),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        val shadowOffset = with(LocalDensity.current) { 8.dp.toPx() }

                        FloatingActionButton(
                            onClick = {
                                // Reset zoom with animation
                                isGestureInProgress = false
                                targetScale = 1f

                                // Small delay to allow scale animation to start
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(50)
                                    targetOffsetX = 0f
                                    targetOffsetY = 0f
                                }
                            },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .graphicsLayer(
                                    scaleX = 1f,
                                    scaleY = 1f,
                                    translationY = -shadowOffset
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOut,
                                contentDescription = "Reset Zoom"
                            )
                        }
                    }
                }
            }
        }
    }

    // Revert confirmation dialog
    if (showRevertConfirmation) {
        AlertDialog(
            onDismissRequest = { showRevertConfirmation = false },
            title = { Text("Confirm Revert") },
            text = { Text("Are you sure you want to delete the circled version of the image?") },
            confirmButton = {
                TextButton(onClick = {
                    isReverting = true  // Set loading state
                    CoroutineScope(Dispatchers.Main).launch {
                        // Delete the circled image
                        viewModel.deleteImage(imageId)

                        // Navigate to original
                        navController.navigate("imageViewer/$projectId/$apId/${originalImageEntity.value?.id}") {
                            popUpTo("imageViewer/$projectId/$apId/$imageId") { inclusive = true }
                        }
                    }
                    showRevertConfirmation = false
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevertConfirmation = false }) {
                    Text("No")
                }
            }
        )
    }

    // Add a loading overlay if reverting
    if (isReverting) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = Color.White,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            )
        }
    }

    // Handle back button
    BackHandler {
        if (showCircle) {
            // If circle is visible, hide it first
            showCircle = false
        } else {
            // Otherwise navigate back
            navController.popBackStack()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OriginalImageViewerScreen(
    projectId: String,
    apId: String,
    imageId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(context))

    // States for image data
    val imageEntity = remember { mutableStateOf<ImageEntity?>(null) }
    val imageBitmap = remember { mutableStateOf<Bitmap?>(null) }

    // Add a loading state to track if we're still loading or had an error
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    // Container and image size states
    val containerSize = remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    // Load original image data when screen is shown
    LaunchedEffect(imageId) {
        withContext(Dispatchers.IO) {
            try {
                // Get original image
                val image = viewModel.getImageById(imageId)
                Log.d("OriginalImageViewerScreen", "Original image: ${image?.filename}")

                // Process URI inside the coroutine
                if (image != null) {
                    try {
                        val file = File(context.filesDir, image.filename)
                        if (file.exists()) {
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.provider",
                                file
                            )
                            withContext(Dispatchers.Main) {
                                imageEntity.value = image
                                imageUri = uri
                                isLoading = false
                            }
                        } else {
                            throw Exception("Image file not found")
                        }
                    } catch (e: Exception) {
                        Log.e("OriginalImageViewerScreen", "Error getting URI: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            loadError = true
                            isLoading = false
                        }
                    }
                } else {
                    throw Exception("Image data not found")
                }
            } catch (e: Exception) {
                Log.e("OriginalImageViewerScreen", "Error loading image data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    isLoading = false
                    loadError = true
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Image transform state setup
    var targetScale by remember { mutableStateOf(1f) }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }
    var isGestureInProgress by remember { mutableStateOf(false) }

    // Animation values
    val scaleAnimated by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        finishedListener = { isGestureInProgress = false },
        label = "scaleAnimation"
    )

    val offsetXAnimated by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "offsetXAnimation"
    )

    val offsetYAnimated by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "offsetYAnimation"
    )

    // Rendering values
    val scale = if (isGestureInProgress) targetScale else scaleAnimated
    val offsetX = if (isGestureInProgress) targetOffsetX else offsetXAnimated
    val offsetY = if (isGestureInProgress) targetOffsetY else offsetYAnimated

    // Function to calculate max allowed offsets
    fun calculateMaxOffsets(scale: Float): Pair<Float, Float> {
        val scaledImageWidth = imageSize.width * scale
        val scaledImageHeight = imageSize.height * scale
        val containerWidth = containerSize.value.width
        val containerHeight = containerSize.value.height

        val horizontalOverflow = (scaledImageWidth - containerWidth) / 2f
        val verticalOverflow = (scaledImageHeight - containerHeight) / 2f

        return Pair(
            horizontalOverflow.coerceAtLeast(0f),
            verticalOverflow.coerceAtLeast(0f)
        )
    }

    // Transform state for gesture handling
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        isGestureInProgress = true

        // Apply zoom constraints
        targetScale = (targetScale * zoomChange).coerceIn(1f, 3f)

        if (targetScale > 1f) {
            // Calculate max offsets
            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(targetScale)

            // Apply scaled delta
            val scaledDeltaX = offsetChange.x * targetScale
            val scaledDeltaY = offsetChange.y * targetScale

            // Constrain offsets
            targetOffsetX = (targetOffsetX + scaledDeltaX).coerceIn(-maxOffsetX, maxOffsetX)
            targetOffsetY = (targetOffsetY + scaledDeltaY).coerceIn(-maxOffsetY, maxOffsetY)
        } else {
            // Reset offset at minimum zoom
            targetOffsetX = 0f
            targetOffsetY = 0f
        }
    }

    Scaffold(
        topBar = {
            // Updated to Material 3 TopAppBar
            TopAppBar(
                title = {
                    // Extract AP name from the entity
                    val apName = imageEntity.value?.let {
                        val projectIdAndApName = it.filename.split('_')
                        val apAndNumber = if (projectIdAndApName.size > 1) {
                            val apAndNumberParts = projectIdAndApName[1].split('-')
                            if (apAndNumberParts.size > 1) {
                                // Format as "AP01-1" (AP name and image number)
                                "${apAndNumberParts[0]}-${apAndNumberParts[1].substringBefore('.')}"
                            } else {
                                apAndNumberParts[0]
                            }
                        } else {
                            "Image"
                        }
                        apAndNumber
                    } ?: "Image"
                    Text("$apName (Original)", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    // Show loading indicator while the image is being loaded
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                } else if (loadError || imageUri == null) {
                    // Only show error when we have actually determined there's an error
                    Text(
                        "Error loading original image",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    // Image container
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
                            }
                    ) {
                        // Image with zoom/pan
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUri)
                                .crossfade(true)
                                .memoryCachePolicy(CachePolicy.DISABLED)
                                .diskCachePolicy(CachePolicy.DISABLED)
                                .build(),
                            contentDescription = "Original image",
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offsetX,
                                    translationY = offsetY
                                )
                                .transformable(state = state)
                                .pointerInput(Unit) {
                                    detectTapGestures(
                                        onDoubleTap = { tapOffset ->
                                            isGestureInProgress = false
                                            if (targetScale > 1f) {
                                                // Reset zoom
                                                targetScale = 1f
                                                targetOffsetX = 0f
                                                targetOffsetY = 0f
                                            } else {
                                                // Zoom in on tap point
                                                targetScale = 2.5f

                                                // Calculate container center
                                                val containerSize = containerSize.value
                                                val containerCenterX = containerSize.width / 2f
                                                val containerCenterY = containerSize.height / 2f

                                                // Calculate distance from center
                                                val distanceX = tapOffset.x - containerCenterX
                                                val distanceY = tapOffset.y - containerCenterY

                                                // Apply scaling factor to offset
                                                targetOffsetX = distanceX * (1 - 1/targetScale) * -1f
                                                targetOffsetY = distanceY * (1 - 1/targetScale) * -1f

                                                // Constrain offsets
                                                val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(targetScale)
                                                targetOffsetX = targetOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                                targetOffsetY = targetOffsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                            }
                                        }
                                    )
                                },
                            contentScale = ContentScale.Fit,
                            onSuccess = { success ->
                                val drawable = success.result.drawable
                                // Store image dimensions
                                imageSize = Size(
                                    drawable.intrinsicWidth.toFloat(),
                                    drawable.intrinsicHeight.toFloat()
                                )

                                // Store bitmap for later use
                                try {
                                    imageBitmap.value = (drawable as? BitmapDrawable)?.bitmap
                                } catch (e: Exception) {
                                    Log.e("OriginalImageViewerScreen", "Error getting bitmap: ${e.message}", e)
                                }
                            },
                            onError = {
                                // Handle image loading error
                                loadError = true
                            }
                        )
                    }

                    // Reset zoom button (only when zoomed in)
                    AnimatedVisibility(
                        visible = scale > 1.01f,
                        enter = slideInVertically(
                            initialOffsetY = { it + 40 },
                            animationSpec = tween(
                                durationMillis = 450,
                                easing = CubicBezierEasing(0.0f, 0.75f, 0.1f, 1.1f)
                            )
                        ) + fadeIn(
                            animationSpec = tween(300)
                        ),
                        exit = slideOutVertically(
                            targetOffsetY = { it + 30 },
                            animationSpec = tween(
                                durationMillis = 400,
                                easing = CubicBezierEasing(0.5f, -0.2f, 0.9f, 0.3f)
                            )
                        ) + fadeOut(
                            animationSpec = tween(300)
                        ),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(36.dp),
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            val shadowOffset = with(LocalDensity.current) { 8.dp.toPx() }

                            // Updated to Material 3 FloatingActionButton
                            FloatingActionButton(
                                onClick = {
                                    // Reset zoom with animation
                                    isGestureInProgress = false
                                    targetScale = 1f

                                    // Small delay to allow scale animation to start
                                    CoroutineScope(Dispatchers.Main).launch {
                                        delay(50)
                                        targetOffsetX = 0f
                                        targetOffsetY = 0f
                                    }
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                elevation = FloatingActionButtonDefaults.elevation(
                                    defaultElevation = 6.dp,
                                    pressedElevation = 8.dp
                                ),
                                modifier = Modifier
                                    .graphicsLayer(
                                        scaleX = 1f,
                                        scaleY = 1f,
                                        translationY = -shadowOffset
                                    )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ZoomOut,
                                    contentDescription = "Reset Zoom"
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    // Handle back button
    BackHandler {
        navController.popBackStack()
    }
}

// Helper function to calculate offset for centering zoom on tap point
private fun calculateCenteredTapOffset(tapOffset: Offset, containerSize: Size, scale: Float): Offset {
    // Get the center of the container
    val containerCenter = Offset(containerSize.width / 2f, containerSize.height / 2f)

    // Calculate the distance from tap point to center
    val distanceFromCenter = tapOffset - containerCenter

    // Apply scaling factor to offset to center the zoom at the tap location
    // The multiplier may need adjustment based on testing
    return distanceFromCenter * (1 - 1/scale) * -1f
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
            .clickable { onClick() }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() },
                    onTap = { onClick() }
                )
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Show checkbox in selection mode
        if (selectionMode) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        // Project name
        Text(
            text = project.name,
            style = MaterialTheme.typography.bodyLarge, // Updated from subtitle1
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}


/**
 * ModernProjectListScreen: Displays a list of projects with selection and deletion functionality
 * @param viewModel ProjectViewModel to access project data
 * @param navController Navigation controller for screen transitions
 */
@OptIn(ExperimentalMaterial3Api::class)
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
                        style = MaterialTheme.typography.headlineSmall // Updated from h6
                    )
                },
                navigationIcon = {
                    // Show back button only in selection mode
                    if (selectionMode) {
                        IconButton(onClick = {
                            selectionMode = false
                            selectedProjects.clear()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    // Show delete button only in selection mode with items selected
                    if (selectionMode && selectedProjects.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        // Floating action button for adding new projects
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(
                    onClick = { navController.navigate("newProject") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
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
                    Text(
                        "No projects available. Tap + to add.",
                        style = MaterialTheme.typography.bodyLarge, // Updated from body1
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    )

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("Delete Projects")
            },
            text = {
                Text("Are you sure you want to delete ${selectedProjects.size} project(s)?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Delete selected projects
                        for (project in selectedProjects) {
                            viewModel.deleteProjectWithAccessPoints(project.id)
                        }
                        // Exit selection mode
                        selectionMode = false
                        selectedProjects.clear()
                        showDeleteDialog = false
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
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
@OptIn(ExperimentalMaterial3Api::class)
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

    // Map to store image counts per access point
    val apImageCounts = remember { mutableStateMapOf<String, Int>() }

    // Load image counts for each access point
    LaunchedEffect(accessPoints) {
        accessPoints.forEach { ap ->
            // For each AP, launch a coroutine to get its image count
            CoroutineScope(Dispatchers.IO).launch {
                // This is the correct way to call first() as a suspend function
                val images = viewModel.getDisplayImagesForAccessPointFlow(ap.id).first()
                val imageCount = images.size

                withContext(Dispatchers.Main) {
                    apImageCounts[ap.id] = imageCount
                }
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
        "Number of Pictures" -> accessPoints.sortedByDescending { apImageCounts[it.id] ?: 0 }
        "APs without Pictures" -> accessPoints.sortedBy { if (apImageCounts[it.id] == 0) 0 else 1 }
        else -> accessPoints
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Show selection count or project name based on mode
                    Text(
                        text = if (apSelectionMode) "${selectedAPs.size} Selected" else projectWithAP.project.name,
                        style = MaterialTheme.typography.headlineSmall // Updated from h6
                    )
                },
                navigationIcon = {
                    if (apSelectionMode) {
                        // In selection mode, back button clears selection
                        IconButton(onClick = {
                            apSelectionMode = false
                            selectedAPs.clear()
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Exit selection",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Normal back button behavior
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (apSelectionMode && selectedAPs.isNotEmpty()) {
                        // Show delete button when items are selected
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        // Show export button in normal mode
                        IconButton(onClick = { showExportDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Archive,
                                contentDescription = "Export Project",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Sort dropdown menu
                        var dropdownExpanded by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { dropdownExpanded = true }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            DropdownMenu(
                                expanded = dropdownExpanded,
                                onDismissRequest = { dropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Alphabetical") },
                                    onClick = {
                                        sortOption = "Alphabetical"
                                        dropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Number of Pictures") },
                                    onClick = {
                                        sortOption = "Number of Pictures"
                                        dropdownExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("APs without Pictures") },
                                    onClick = {
                                        sortOption = "APs without Pictures"
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            // Only show FAB in normal mode
            if (!apSelectionMode) {
                FloatingActionButton(
                    onClick = { showAddAPDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
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
                            .clickable {
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
                            }
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onLongPress = {
                                        // Enter selection mode on long press
                                        if (!apSelectionMode) { apSelectionMode = true }
                                        if (!selectedAPs.contains(ap)) { selectedAPs.add(ap) }
                                    },
                                    onTap = {
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
                                    }
                                )
                            }
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
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        } else {
                            Spacer(modifier = Modifier.width(16.dp))
                        }

                        // AP information
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // AP name and photo count
                            Text(
                                text = "${ap.name} (${apImageCounts[ap.id] ?: 0})",
                                style = MaterialTheme.typography.bodyLarge, // Updated from subtitle1
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            // Help text
                            Text(
                                text = "Tap to view pictures",
                                style = MaterialTheme.typography.bodyMedium, // Updated from body2
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Updated from Color.Gray
                            )
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
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
                TextButton(
                    onClick = {
                        // Delete all selected access points
                        selectedAPs.forEach { ap ->
                            viewModel.deleteAccessPoint(ap.id)
                        }
                        // Clean up selection state
                        selectedAPs.clear()
                        apSelectionMode = false
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteConfirmDialog = false }
                ) {
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // Display Add AP dialog when requested
    if (showAddAPDialog) {
        AddAPDialog(
            projectId = projectWithAP.project.id,
            viewModel = viewModel,
            onDismiss = { showAddAPDialog = false },
            // Handle successful AP addition
            onAPAdded = { newAP ->
                // Show a toast message
                Toast.makeText(
                    context,
                    "${newAP.name} added successfully",
                    Toast.LENGTH_SHORT
                ).show()

                // Force refresh the access points list
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Trigger a refresh through the viewModel
                        viewModel.refreshAccessPointsForProject(projectWithAP.project.id)

                        // Wait a moment to ensure the UI updates
                        delay(200)

                        // Add the new AP to the image counts map with 0 images
                        withContext(Dispatchers.Main) {
                            apImageCounts[newAP.id] = 0
                        }
                    } catch (e: Exception) {
                        Log.e("ProjectDetailScreen", "Error refreshing APs after add: ${e.message}", e)
                    }
                }
            }
        )
    }

    // Export dialog (with pre-export preparation)
    if (showExportDialog) {
        ExportDialog(
            onDismiss = { showExportDialog = false },
            // Handler for Save option
            onSave = { progressState, isZipping, onDismissExport ->
                // Use a coroutine for background processing
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Log the export process starting
                        Log.d("ExportProject", "Starting export process for save...")

                        // Create the zip file without modifying the database
                        val zipFile = createProjectZip(
                            context,
                            projectWithAP,
                            viewModel,
                            progressState,
                            isZipping,
                            onDismissExport
                        )

                        withContext(Dispatchers.Main) {
                            // Save to documents directory and provide feedback
                            val savedFile = saveZipToDocuments(context, zipFile)
                            Toast.makeText(
                                context,
                                "Saved to ${savedFile?.absolutePath ?: "Error saving file"}",
                                Toast.LENGTH_LONG
                            ).show()

                            // Clean up the temporary zip file after saving
                            if (zipFile.exists() && savedFile != null && savedFile.exists()) {
                                CoroutineScope(Dispatchers.IO).launch {
                                    zipFile.delete()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle any errors during export
                        Log.e("ExportProject", "Error during save export: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error exporting: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            isZipping.value = false
                            onDismissExport()
                        }
                    }
                }
            },
            // Handler for Share option
            onShare = { progressState, isZipping, onDismissExport ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        // Log the export process starting
                        Log.d("ExportProject", "Starting export process for share...")

                        // Create the zip file without modifying the database
                        val zipFile = createProjectZip(
                            context,
                            projectWithAP,
                            viewModel,
                            progressState,
                            isZipping,
                            onDismissExport
                        )

                        withContext(Dispatchers.Main) {
                            // Share the zip file
                            shareZipFile(context, zipFile)
                        }
                    } catch (e: Exception) {
                        // Handle any errors during export
                        Log.e("ExportProject", "Error during share export: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error exporting: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            isZipping.value = false
                            onDismissExport()
                        }
                    }
                }
            },
            // Warning message for APs with insufficient photos
            warningMessage = accessPoints.filter { apImageCounts[it.id] ?: 0 < 2 }
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
 * Creates a ZIP file containing project images organized into three folders:
 * - Original: Contains all original (non-circled) images
 * - Circled: Contains all circled versions of images
 * - Resized: Contains properly sized images for printing (with PPI adjusted based on total count)
 *
 * The resized folder prioritizes circled versions when available
 *
 * @param context Application context
 * @param projectWithAP Project data with access points
 * @param viewModel ViewModel for database access (read-only)
 * @param progressState Mutable state for tracking progress percentage
 * @param isZipping Mutable state for tracking zipping state
 * @param onDismiss Callback when operation completes
 * @return The created ZIP file
 */
suspend fun createProjectZip(
    context: Context,
    projectWithAP: ProjectWithAccessPoints,
    viewModel: ProjectViewModel,
    progressState: MutableState<Int>,
    isZipping: MutableState<Boolean>,
    onDismiss: () -> Unit
): File {
    // Create a zip file in the app's cache directory
    val zipFile = File(context.cacheDir, "${projectWithAP.project.name}.zip")

    // Create temporary directory for organizing files
    val tempDir = File(context.cacheDir, "temp_export")
    if (tempDir.exists()) {
        tempDir.deleteRecursively()
    }
    tempDir.mkdirs()

    // Create subdirectories for our folder structure
    val originalDir = File(tempDir, "Original")
    val circledDir = File(tempDir, "Circled")
    val resizedDir = File(tempDir, "Resized")

    originalDir.mkdirs()
    circledDir.mkdirs()
    resizedDir.mkdirs()

    // Track progress
    var totalSteps = 0
    var currentStep = 0

    // For each access point, process its images
    projectWithAP.accessPoints.forEach { ap ->
        // Get ALL images for this AP (both original and circled)
        val allImages = viewModel.getImagesForAccessPointFlow(ap.id).first()

        // Group images by whether they're circled or not
        val originalImages = allImages.filter { !it.isCircled }
        val circledImages = allImages.filter { it.isCircled }

        // Create a map to associate originals with their circled versions
        val originalToCircledMap = mutableMapOf<String, ImageEntity>()

        // Build the map: original ID -> circled image
        circledImages.forEach { circledImage ->
            circledImage.originalImageId?.let { originalId ->
                originalToCircledMap[originalId] = circledImage
            }
        }

        // Log summary for this AP
        Log.d("ExportZip", """
            AP: ${ap.name}
            Total images: ${allImages.size}
            Original images: ${originalImages.size}
            Circled images: ${circledImages.size}
        """.trimIndent())

        // Calculate total steps for progress tracking
        // Each original: 1 copy to Original folder + maybe 1 resize if no circled version
        // Each circled: 1 copy to Circled folder + 1 resize to Resized folder
        val origWithNoCircleCount = originalImages.count { image ->
            !originalToCircledMap.containsKey(image.id)
        }

        totalSteps += originalImages.size + // Copy to Original
                circledImages.size + // Copy to Circled
                origWithNoCircleCount + // Resize originals with no circled version
                circledImages.size // Resize all circled versions

        // Process original images (sorted by order index)
        val sortedOriginals = originalImages.sortedBy { it.orderIndex }

        sortedOriginals.forEachIndexed { index, originalImage ->
            val imageNumber = index + 1
            val formattedFileName = "${ap.name}-$imageNumber.jpg"
            val sourceFile = File(context.filesDir, originalImage.filename)

            if (sourceFile.exists()) {
                try {
                    // Step 1: Copy all original images to the "Original" folder with standardized naming
                    val originalOutputFile = File(originalDir, formattedFileName)

                    // Simple file copy with no database changes
                    sourceFile.inputStream().use { input ->
                        originalOutputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Fix orientation if needed, but don't modify original
                    fixImageOrientation(originalOutputFile)

                    // Update progress
                    currentStep++
                    progressState.value = ((currentStep.toFloat() / totalSteps) * 100).toInt()

                    // Step 2: If this original has no circled version, add it to Resized folder
                    if (!originalToCircledMap.containsKey(originalImage.id)) {
                        val resizedOutputFile = File(resizedDir, formattedFileName)

                        // Resize the image without modifying the original
                        resizeImage(
                            sourceFile,
                            resizedOutputFile,
                            calculatePpi(totalSteps),
                            3.44f,
                            2.09f,
                            90
                        )

                        // Update progress
                        currentStep++
                        progressState.value = ((currentStep.toFloat() / totalSteps) * 100).toInt()

                        Log.d("ExportZip", "Added original image to Resized (no circle available): ${formattedFileName}")
                    } else {
                        Log.d("ExportZip", "Skipping original image for Resized (has circle): ${formattedFileName}")
                    }
                } catch (e: Exception) {
                    Log.e("ExportZip", "Error processing original image: ${e.message}", e)
                }
            } else {
                Log.e("ExportZip", "Original image file doesn't exist: ${originalImage.filename}")
            }
        }

        // Process circled images
        circledImages.forEach { circledImage ->
            try {
                // Find the original image to determine position/numbering
                val originalId = circledImage.originalImageId
                val originalImageIdx = sortedOriginals.indexOfFirst { it.id == originalId }

                // Determine image number based on the original's position (1-based)
                val imageNumber = if (originalImageIdx >= 0) {
                    originalImageIdx + 1
                } else {
                    // Fallback if original not found (should not happen)
                    Log.w("ExportZip", "Could not find original image for circled image: ${circledImage.filename}")
                    circledImage.orderIndex + 1
                }

                val formattedFileName = "${ap.name}-$imageNumber.jpg"
                val sourceFile = File(context.filesDir, circledImage.filename)

                if (sourceFile.exists()) {
                    // Step 3: Copy circled image to "Circled" folder
                    val circledOutputFile = File(circledDir, formattedFileName)

                    // Simple file copy with no database changes
                    sourceFile.inputStream().use { input ->
                        circledOutputFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    // Fix orientation if needed, but don't modify original
                    fixImageOrientation(circledOutputFile)

                    // Update progress
                    currentStep++
                    progressState.value = ((currentStep.toFloat() / totalSteps) * 100).toInt()

                    // Step 4: Add circled image to "Resized" folder
                    val resizedOutputFile = File(resizedDir, formattedFileName)

                    // Resize the image without modifying the original
                    resizeImage(
                        sourceFile,
                        resizedOutputFile,
                        calculatePpi(totalSteps),
                        3.44f,
                        2.09f,
                        90
                    )

                    // Update progress
                    currentStep++
                    progressState.value = ((currentStep.toFloat() / totalSteps) * 100).toInt()

                    Log.d("ExportZip", "Added circled image to Resized folder: ${formattedFileName}")
                } else {
                    // This is actually fine - not all images have circled versions
                    // Convert to debug log instead of error
                    Log.d("ExportZip", "No circled version found for image: ${formattedFileName}")
                }
            } catch (e: Exception) {
                Log.e("ExportZip", "Error processing circled image: ${e.message}", e)
            }
        }
    }

    Log.d("ExportZip", "Starting ZIP creation. Files in Original: ${originalDir.listFiles()?.size ?: 0}, Circled: ${circledDir.listFiles()?.size ?: 0}, Resized: ${resizedDir.listFiles()?.size ?: 0}")

    // Create ZIP stream and add all the organized folders
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        // Add folder entries first to ensure proper structure
        val rootFolders = arrayOf("Original/", "Circled/", "Resized/")
        for (folder in rootFolders) {
            val entry = ZipEntry(folder)
            zos.putNextEntry(entry)
            zos.closeEntry()
        }

        // Function to add files from a directory to the ZIP
        fun addFilesToZip(directory: File, pathInZip: String) {
            val files = directory.listFiles()
            if (files == null || files.isEmpty()) {
                Log.w("ExportZip", "No files found in directory: ${directory.absolutePath}")
                return
            }

            Log.d("ExportZip", "Adding ${files.size} files from ${directory.name} to $pathInZip")

            files.forEach { file ->
                if (!file.isDirectory) {
                    try {
                        val entry = ZipEntry("$pathInZip${file.name}")
                        zos.putNextEntry(entry)

                        // Copy file contents to ZIP
                        file.inputStream().use { input ->
                            input.copyTo(zos)
                        }

                        zos.closeEntry()
                    } catch (e: Exception) {
                        Log.e("ExportZip", "Error adding file ${file.name} to ZIP: ${e.message}", e)
                    }
                }
            }
        }

        // Add files from each directory
        addFilesToZip(originalDir, "Original/")
        addFilesToZip(circledDir, "Circled/")
        addFilesToZip(resizedDir, "Resized/")
    }

    // Clean up temp directory
    try {
        tempDir.deleteRecursively()
    } catch (e: Exception) {
        Log.e("ExportZip", "Error cleaning up temp directory: ${e.message}", e)
    }

    // Update UI and dismiss dialog
    withContext(Dispatchers.Main) {
        isZipping.value = false
        onDismiss()
    }

    return zipFile
}

/**
 * Helper function to calculate PPI based on total image count
 */
private fun calculatePpi(totalImageCount: Int): Int {
    return when {
        totalImageCount < 100 -> 150
        totalImageCount < 200 -> 125
        totalImageCount < 250 -> 100
        else -> 75
    }
}

/**
 * Fixes image orientation based on EXIF data without modifying the original file
 * This only affects the provided file, not the source
 */
private fun fixImageOrientation(imageFile: File) {
    try {
        // Read EXIF data
        val exif = ExifInterface(imageFile.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        // If orientation is normal, no correction needed
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return
        }

        // Get rotation angle
        val rotationAngle = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }

        // If no rotation needed, return
        if (rotationAngle == 0) {
            return
        }

        // Load bitmap
        val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)

        // Create rotation matrix
        val matrix = Matrix()
        matrix.postRotate(rotationAngle.toFloat())

        // Create rotated bitmap
        val rotatedBitmap = Bitmap.createBitmap(
            originalBitmap,
            0,
            0,
            originalBitmap.width,
            originalBitmap.height,
            matrix,
            true
        )

        // Recycle original to save memory
        originalBitmap.recycle()

        // Save rotated image back to file
        FileOutputStream(imageFile).use { out ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }

        // Recycle rotated bitmap
        rotatedBitmap.recycle()

        // Update EXIF to normal orientation
        val updatedExif = ExifInterface(imageFile.absolutePath)
        updatedExif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        updatedExif.saveAttributes()
    } catch (e: Exception) {
        Log.e("ExportZip", "Error fixing image rotation: ${e.message}", e)
    }
}

/**
 * Resizes an image to the specified dimensions without modifying the original file
 */
/**
 * Resizes an image while preserving its aspect ratio
 * Creates a new resized copy without modifying the original file
 *
 * @param sourceFile Original image file
 * @param outputFile Destination file for the resized image
 * @param ppi Pixels per inch for the target size calculation
 * @param targetWidthInches Maximum width in inches
 * @param targetHeightInches Maximum height in inches
 * @param quality JPEG quality (0-100)
 */
private fun resizeImage(
    sourceFile: File,
    outputFile: File,
    ppi: Int,
    targetWidthInches: Float,
    targetHeightInches: Float,
    quality: Int
) {
    try {
        // Calculate target dimensions in pixels
        val maxWidthPixels = (targetWidthInches * ppi).toInt()
        val maxHeightPixels = (targetHeightInches * ppi).toInt()

        // Load source bitmap information
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(sourceFile.absolutePath, options)

        // Get source dimensions
        val sourceWidth = options.outWidth
        val sourceHeight = options.outHeight

        // Determine if the image is portrait or landscape
        val isPortrait = sourceHeight > sourceWidth

        // Calculate target dimensions based on orientation while preserving aspect ratio
        val finalWidth: Int
        val finalHeight: Int

        if (isPortrait) {
            // For portrait orientation, height is the constraint
            // Swap target dimensions to match portrait orientation
            val targetWidthPx = maxHeightPixels
            val targetHeightPx = maxWidthPixels

            // Calculate resize ratios
            val ratioHeight = targetHeightPx / sourceHeight.toFloat()
            val ratioWidth = targetWidthPx / sourceWidth.toFloat()

            // Use the smaller ratio to ensure image fits within bounds
            val ratio = ratioHeight.coerceAtMost(ratioWidth)

            // Calculate final dimensions
            finalWidth = (sourceWidth * ratio).toInt()
            finalHeight = (sourceHeight * ratio).toInt()
        } else {
            // For landscape orientation, width is the constraint
            // Calculate resize ratios
            val ratioWidth = maxWidthPixels / sourceWidth.toFloat()
            val ratioHeight = maxHeightPixels / sourceHeight.toFloat()

            // Use the smaller ratio to ensure image fits within bounds
            val ratio = ratioWidth.coerceAtMost(ratioHeight)

            // Calculate final dimensions
            finalWidth = (sourceWidth * ratio).toInt()
            finalHeight = (sourceHeight * ratio).toInt()
        }

        // Calculate sample size for memory-efficient decoding
        options.inSampleSize = calculateInSampleSize(options, finalWidth, finalHeight)
        options.inJustDecodeBounds = false
        options.inScaled = false  // Don't pre-scale the bitmap

        // Load bitmap at reduced sample size
        val sourceBitmap = BitmapFactory.decodeFile(sourceFile.absolutePath, options)

        // Create the final sized bitmap with proper aspect ratio
        // Create the final sized bitmap with proper aspect ratio using KTX extension
        val resizedBitmap = sourceBitmap.scale(
            width = finalWidth,
            height = finalHeight,
            filter = true  // Use bilinear filtering for better quality
        )

        // Save the resized bitmap
        FileOutputStream(outputFile).use { out ->
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }

        // Recycle bitmaps to free memory
        if (resizedBitmap != sourceBitmap) {
            sourceBitmap.recycle()
        }
        resizedBitmap.recycle()

        // Copy EXIF data except orientation (which we've already fixed)
        val sourceExif = ExifInterface(sourceFile.absolutePath)
        val targetExif = ExifInterface(outputFile.absolutePath)

        // Copy relevant EXIF tags
        val tags = arrayOf(
            ExifInterface.TAG_DATETIME,
            ExifInterface.TAG_MAKE,
            ExifInterface.TAG_MODEL,
            ExifInterface.TAG_FLASH,
            ExifInterface.TAG_GPS_LATITUDE,
            ExifInterface.TAG_GPS_LONGITUDE,
            ExifInterface.TAG_GPS_LATITUDE_REF,
            ExifInterface.TAG_GPS_LONGITUDE_REF
        )

        for (tag in tags) {
            val value = sourceExif.getAttribute(tag)
            if (value != null) {
                targetExif.setAttribute(tag, value)
            }
        }

        // Set orientation to normal
        targetExif.setAttribute(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL.toString()
        )
        targetExif.saveAttributes()

        Log.d("ImageResize", "Resized image from ${sourceWidth}x${sourceHeight} to ${finalWidth}x${finalHeight}")

    } catch (e: Exception) {
        Log.e("ExportZip", "Error resizing image: ${e.message}", e)
    }
}

/**
 * Calculate the optimal inSampleSize value to load a smaller bitmap
 */
private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val height = options.outHeight
    val width = options.outWidth
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight = height / 2
        val halfWidth = width / 2

        while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}

// Helper function to get clean AP name from filename
private fun getCleanApNameFromFilename(filename: String): String {
    // Find the underscore that separates project ID from AP name
    val underscoreIndex = filename.indexOf('_')
    if (underscoreIndex >= 0) {
        // Extract the part after the underscore up to the dash
        val dashIndex = filename.indexOf('-', underscoreIndex)
        if (dashIndex >= 0) {
            return filename.substring(underscoreIndex + 1, dashIndex)
        }
    }
    return filename.split('_').getOrNull(1)?.split('-')?.getOrNull(0) ?: "Image"
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernAPDetailScreen(
    projectWithAP: ProjectWithAccessPoints,
    accessPoint: AccessPointEntity,
    navController: NavHostController,
    viewModel: ProjectViewModel
) {
    val context = LocalContext.current

    // Get the lifecycle owner
    val lifecycleOwner = LocalLifecycleOwner.current

    // Get display images (preferring circled versions when available)
    val displayImages by viewModel.getDisplayImagesForAccessPointFlow(accessPoint.id)
        .collectAsState(initial = emptyList())

    // Add these state variables right after the displayImages declaration
    var forceRefreshCounter by remember { mutableStateOf(0) }

    // Add this LaunchedEffect to refresh when the screen is composed or counter changes
    LaunchedEffect(accessPoint.id, forceRefreshCounter) {
        Log.d("APDetailScreen", "LaunchedEffect triggered refresh (counter: $forceRefreshCounter)")
        // Force a manual refresh of images when entering the screen
        viewModel.refreshImagesForAccessPoint(accessPoint.id)

        // Give UI time to update, then refresh again
        delay(500)
        viewModel.refreshImagesForAccessPoint(accessPoint.id)
    }

    // Add this for navigation monitoring
    DisposableEffect(navController) {
        // Create a listener
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route?.contains("apDetail/${projectWithAP.project.id}/${accessPoint.id}") == true) {
                // We're back to this screen, increment counter to trigger refresh
                forceRefreshCounter++
            }
        }

        // Add listener
        navController.addOnDestinationChangedListener(listener)

        // This is provided by DisposableEffect
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Add this for lifecycle monitoring
    DisposableEffect(lifecycleOwner) {
        // Create observer
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Screen resumed, trigger refresh
                Log.d("APDetailScreen", "Screen resumed, refreshing images")
                viewModel.refreshImagesForAccessPoint(accessPoint.id)

                // Small delay then refresh again
                CoroutineScope(Dispatchers.Main).launch {
                    delay(500)
                    viewModel.refreshImagesForAccessPoint(accessPoint.id)
                }
            }
        }

        // Add observer to lifecycle
        lifecycleOwner.lifecycle.addObserver(observer)

        // This is provided by DisposableEffect
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Current photo capture state
    var currentImageName by remember { mutableStateOf("") }
    var imageUri: Uri? by rememberSaveable(stateSaver = UriNullableSaver) { mutableStateOf(null) }

    // Selection mode state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedImages = remember { mutableStateListOf<String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Camera integration
    // Improved camera launcher that's resilient to rotation changes
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            // If photo capture was successful, process the image and add to database
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d("APDetailScreen", "Camera returned success=true, processing captured image")

                    // Get the current filename
                    val capturedImageName = currentImageName

                    // Get the file from URI
                    val file = File(context.filesDir, capturedImageName)

                    if (file.exists()) {
                        // Get file size for debugging
                        val fileSize = file.length()
                        Log.d("APDetailScreen", "Captured image file exists, size: $fileSize bytes")

                        if (fileSize == 0L) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Error: Captured image is empty",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            return@launch
                        }

                        // Add delay to ensure file is fully written
                        delay(500)

                        // Check the dimensions before rotation
                        val beforeOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(file.absolutePath, beforeOptions)
                        Log.d("APDetailScreen", "Before rotation: ${beforeOptions.outWidth}x${beforeOptions.outHeight}")

                        // Fix orientation and get the corrected file
                        val correctedFile = fixImageRotation(file)

                        // Check dimensions after rotation
                        val afterOptions = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(correctedFile.absolutePath, afterOptions)
                        Log.d("APDetailScreen", "After rotation: ${afterOptions.outWidth}x${afterOptions.outHeight}")

                        // IMPORTANT: Check if this image was already added to the database by the refresh mechanism
                        val existingImages = viewModel.getImagesForAccessPointFlow(accessPoint.id).first()
                        val imageAlreadyExists = existingImages.any { it.filename == correctedFile.name }

                        if (!imageAlreadyExists) {
                            // Get the next order index
                            val nextOrderIndex = existingImages.size

                            Log.d("APDetailScreen", "Will add new image with orderIndex: $nextOrderIndex")

                            // Create new image entity
                            val newImage = ImageEntity(
                                accessPointId = accessPoint.id,
                                filename = correctedFile.name,
                                isCircled = false,
                                orderIndex = nextOrderIndex
                            )

                            // Add to database
                            viewModel.addImage(newImage)
                            Log.d("APDetailScreen", "Added new image to database: ${newImage.id}, filename: ${newImage.filename}")
                        } else {
                            Log.d("APDetailScreen", "Image ${correctedFile.name} already exists in database, skipping addition")
                        }

                        // Force a UI refresh
                        delay(300) // Wait for database to settle
                        viewModel.refreshImagesForAccessPoint(accessPoint.id)

                        // Force a forceRefreshCounter increment to ensure UI updates
                        withContext(Dispatchers.Main) {
                            forceRefreshCounter++
                            Toast.makeText(
                                context,
                                "Image saved successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // If file doesn't exist, log error
                        Log.e("APDetailScreen", "Captured image file doesn't exist: $capturedImageName")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error: Captured image not found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    // Log and show any errors
                    Log.e("APDetailScreen", "Error saving captured image: ${e.message}")
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            "Error processing image: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        } else if (!success) {
            // Camera capture was cancelled or failed
            Log.d("APDetailScreen", "Camera capture cancelled or failed, success=$success")
        }
    }

    // Back button handler
    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedImages.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "${accessPoint.name} Pictures",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    if (selectionMode && selectedImages.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Selected",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {  // This needs to be a named parameter
            FloatingActionButton(
                onClick = {
                    try {
                        // Get all images for this AP, including both original and circled versions
                        CoroutineScope(Dispatchers.IO).launch {
                            val allImages = viewModel.getImagesForAccessPointFlow(accessPoint.id).first()

                            // Find the highest image number used across all images (both original and circled)
                            val highestNumber = allImages.mapNotNull { image ->
                                // Extract the number from the filename pattern: PROJECTID_APXX-N_TIMESTAMP.jpg
                                // or PROJECTID_APXX-N.jpg or similar patterns
                                val filenamePattern = ".*${accessPoint.name}-([0-9]+).*\\.jpg".toRegex(RegexOption.IGNORE_CASE)
                                val match = filenamePattern.find(image.filename)
                                match?.groupValues?.get(1)?.toIntOrNull()
                            }.maxOrNull() ?: 0

                            // Use the next number in sequence
                            val nextNumber = highestNumber + 1

                            // Generate a unique sequential filename with timestamp
                            val timestamp = System.currentTimeMillis()
                            val newImageName = "${projectWithAP.project.id}_${accessPoint.name}-$nextNumber" +
                                    "_${timestamp}.jpg"

                            withContext(Dispatchers.Main) {
                                // Store the generated name for use in the camera callback
                                currentImageName = newImageName

                                // Create the file and prepare camera
                                val file = File(context.filesDir, newImageName)

                                // Ensure parent directories exist
                                file.parentFile?.mkdirs()

                                // Clean up any existing file with the same name
                                if (file.exists()) {
                                    file.delete()
                                }

                                // Get content URI for this file
                                val contentUri = try {
                                    FileProvider.getUriForFile(
                                        context,
                                        "${context.packageName}.provider",
                                        file
                                    )
                                } catch (e: IllegalArgumentException) {
                                    Log.e("APDetailScreen", "Error getting URI for file: ${file.absolutePath}", e)
                                    Toast.makeText(
                                        context,
                                        "Error preparing camera: Unable to create file path",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    null
                                }

                                if (contentUri != null) {
                                    // Store the URI for later use by the camera launcher
                                    imageUri = contentUri

                                    // Launch camera with the URI
                                    cameraLauncher.launch(contentUri)

                                    // Log the camera launch
                                    Log.d("APDetailScreen", "Camera launched with URI: $contentUri, filename: $currentImageName")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Log and show any errors
                        Log.e("APDetailScreen", "Error launching camera: ${e.message}", e)
                        Toast.makeText(
                            context,
                            "Error launching camera: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Picture")
            }
        },
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (displayImages.isEmpty()) {
                    // Empty state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No pictures available. Tap + to add.",
                            style = MaterialTheme.typography.bodyLarge, // Updated from body1
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    // Grid of images
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(displayImages) { image ->
                            val file = File(context.filesDir, image.filename)

                            // Skip missing files
                            if (!file.exists()) {
                                return@items
                            }

                            // Get URI with error handling
                            val imageUri = try {
                                FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.provider",
                                    file
                                )
                            } catch (e: Exception) {
                                Log.e("ModernAPDetailScreen", "Error getting URI: ${e.message}", e)
                                null
                            }

                            if (imageUri == null) return@items

                            // Image tile
                            Box(
                                modifier = Modifier
                                    .padding(4.dp)
                                    .aspectRatio(1f)
                                    .clickable {
                                        if (selectionMode) {
                                            // Toggle selection
                                            if (selectedImages.contains(image.id)) {
                                                selectedImages.remove(image.id)
                                                if (selectedImages.isEmpty()) selectionMode = false
                                            } else {
                                                selectedImages.add(image.id)
                                            }
                                        } else {
                                            // Open image viewer
                                            navController.navigate(
                                                "imageViewer/${projectWithAP.project.id}/${accessPoint.id}/${image.id}"
                                            )
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onLongPress = {
                                                // Enter selection mode
                                                if (!selectionMode) selectionMode = true
                                                if (!selectedImages.contains(image.id)) {
                                                    selectedImages.add(image.id)
                                                }
                                            },
                                            onTap = {
                                                if (selectionMode) {
                                                    // Toggle selection
                                                    if (selectedImages.contains(image.id)) {
                                                        selectedImages.remove(image.id)
                                                        if (selectedImages.isEmpty()) selectionMode = false
                                                    } else {
                                                        selectedImages.add(image.id)
                                                    }
                                                } else {
                                                    // Open image viewer
                                                    navController.navigate(
                                                        "imageViewer/${projectWithAP.project.id}/${accessPoint.id}/${image.id}"
                                                    )
                                                }
                                            }
                                        )
                                    }
                            ) {
                                // Display image
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUri)
                                        .diskCachePolicy(CachePolicy.DISABLED)
                                        .memoryCachePolicy(CachePolicy.DISABLED)
                                        .build(),
                                    contentDescription = "Picture",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )

                                // Selection overlay
                                if (selectionMode && selectedImages.contains(image.id)) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)), // Updated from Color.Black
                                        contentAlignment = Alignment.TopEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = MaterialTheme.colorScheme.onPrimary, // Updated from Color.White
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(24.dp)
                                        )
                                    }
                                }

                                // Show circle icon indicator for circled images
                                if (image.isCircled) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Has Circle",
                                            tint = MaterialTheme.colorScheme.onError, // Updated from Color.White
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(16.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.error, // Updated from Color.Red
                                                    shape = CircleShape
                                                )
                                                .padding(2.dp)
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

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to delete ${selectedImages.size} image(s)?")
            },
            confirmButton = {
                TextButton(onClick = { // Updated from Button
                    // Delete selected images
                    CoroutineScope(Dispatchers.IO).launch {
                        selectedImages.forEach { imageId ->
                            // Get the current image
                            val image = viewModel.getImageById(imageId)

                            if (image != null) {
                                // If this is a circled image, delete it and also find and delete its original
                                if (image.isCircled && image.originalImageId != null) {
                                    // Delete the circled image
                                    viewModel.deleteImage(imageId)

                                    // Delete the original image too
                                    viewModel.deleteImage(image.originalImageId)
                                }
                                // If this is an original image, find and delete any circled versions
                                else if (!image.isCircled) {
                                    // Find circled version
                                    val circledImage = viewModel.getCircledVersionOfImage(imageId)

                                    // Delete the original image
                                    viewModel.deleteImage(imageId)

                                    // Delete the circled version if it exists
                                    if (circledImage != null) {
                                        viewModel.deleteImage(circledImage.id)
                                    }
                                }
                            }
                        }

                        // Reorder remaining images
                        viewModel.reorderImagesAfterDeletion(accessPoint.id)

                        // Clear selection
                        withContext(Dispatchers.Main) {
                            selectedImages.clear()
                            selectionMode = false
                            showDeleteDialog = false
                        }
                    }
                }) {
                    Text(
                        "Delete",
                        color = MaterialTheme.colorScheme.error // Added color
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { // Updated from Button
                    Text("Cancel")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Fixes the rotation of an image based on its EXIF orientation data
 *
 * @param imageFile The source image file that may have incorrect orientation
 * @return The corrected image file (may be the same file if no rotation was needed)
 */
private fun fixImageRotation(imageFile: File): File {
    try {
        // First check if the file is valid
        if (!imageFile.exists() || imageFile.length() == 0L) {
            Log.e("ImageFix", "Invalid image file: ${imageFile.absolutePath}, exists=${imageFile.exists()}, size=${imageFile.length()}")
            return imageFile
        }

        // Read EXIF data - wrap in try/catch as some camera apps don't write proper EXIF
        val orientation = try {
            val exif = ExifInterface(imageFile.absolutePath)
            exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
        } catch (e: Exception) {
            Log.e("ImageFix", "Error reading EXIF: ${e.message}")
            ExifInterface.ORIENTATION_UNDEFINED
        }

        Log.d("ImageFix", "Original orientation from EXIF: $orientation")

        // If orientation is undefined, we need to detect it based on image dimensions
        var rotationAngle = 0
        if (orientation == ExifInterface.ORIENTATION_UNDEFINED) {
            // Check image dimensions to guess orientation
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            val width = options.outWidth
            val height = options.outHeight

            Log.d("ImageFix", "Image dimensions: ${width}x${height}")

            // If width > height and significantly so, it's likely landscape
            // We'll rotate based on what we know about typical phone cameras
            if (width > height && width.toFloat() / height.toFloat() > 1.2f) {
                // Most front cameras default to a 90-degree rotation when held normally
                rotationAngle = 90
                Log.d("ImageFix", "Auto-detected landscape orientation, will rotate $rotationAngle degrees")
            }
        } else {
            // Get rotation angle from EXIF
            rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
            Log.d("ImageFix", "Using EXIF rotation angle: $rotationAngle")
        }

        // If no rotation needed, return original
        if (rotationAngle == 0) {
            return imageFile
        }

        // Create a temporary file to avoid issues with simultaneous read/write
        val tempFile = File(imageFile.parentFile, "temp_${imageFile.name}")
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            // Load bitmap with proper options
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
            }
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath, options)

            if (originalBitmap == null) {
                Log.e("ImageFix", "Failed to decode bitmap from file: ${imageFile.absolutePath}")
                return imageFile
            }

            Log.d("ImageFix", "Original bitmap dimensions: ${originalBitmap.width}x${originalBitmap.height}")

            // Create rotation matrix
            val matrix = Matrix()
            matrix.postRotate(rotationAngle.toFloat())

            // Create rotated bitmap
            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap,
                0,
                0,
                originalBitmap.width,
                originalBitmap.height,
                matrix,
                true
            )

            Log.d("ImageFix", "Rotated bitmap dimensions: ${rotatedBitmap.width}x${rotatedBitmap.height}")

            // Save rotated image to temp file
            FileOutputStream(tempFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                out.flush()
            }

            // Recycle bitmaps to prevent memory leaks
            if (rotatedBitmap != originalBitmap) {
                originalBitmap.recycle()
            }
            rotatedBitmap.recycle()

            // Verify temp file was created successfully
            if (tempFile.exists() && tempFile.length() > 0) {
                // Copy the temp file to original and delete temp
                tempFile.copyTo(imageFile, overwrite = true)
                tempFile.delete()

                // Update EXIF to normal orientation
                try {
                    val updatedExif = ExifInterface(imageFile.absolutePath)
                    updatedExif.setAttribute(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL.toString()
                    )
                    updatedExif.saveAttributes()
                } catch (e: Exception) {
                    Log.e("ImageFix", "Error updating EXIF: ${e.message}")
                }

                Log.d("ImageFix", "Image rotation fixed successfully")
            } else {
                Log.e("ImageFix", "Failed to create temp file for rotation")
            }
        } catch (e: Exception) {
            Log.e("ImageFix", "Error during bitmap rotation: ${e.message}")
            e.printStackTrace()

            // Clean up temp file if it exists
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }

        return imageFile
    } catch (e: Exception) {
        Log.e("ImageFix", "Error fixing image rotation: ${e.message}")
        e.printStackTrace()
        return imageFile  // Return original in case of error
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
    viewModel: ProjectViewModel, // Add ViewModel parameter
    progressState: MutableState<Int>,
    isZipping: MutableState<Boolean>,
    onDismiss: () -> Unit
): File {
    // Create a zip file in the app's cache directory
    val zipFile = File(context.cacheDir, "${projectWithAP.project.name}.zip")

    // Calculate total number of images to track progress
    var totalImages = 0
    var processedImages = 0

    // First, we need to count total images across all APs
    runBlocking {
        projectWithAP.accessPoints.forEach { ap ->
            val imageCount = viewModel.getDisplayImagesForAccessPointFlow(ap.id).first().size
            totalImages += imageCount
        }
    }

    // Create ZIP stream and add files
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        runBlocking {
            projectWithAP.accessPoints.forEach { ap ->
                // Get all images for this AP
                val images = viewModel.getDisplayImagesForAccessPointFlow(ap.id).first()

                // Sort by order index
                val sortedImages = images.sortedBy { it.orderIndex }

                // Add each image to the zip
                sortedImages.forEachIndexed { index, image ->
                    // Get source file from internal storage
                    val file = File(context.filesDir, image.filename)
                    if (file.exists()) {
                        // Create clean filename for the ZIP entry (without project ID)
                        val formattedFileName = "${ap.name}-${index + 1}.jpg"
                        val entry = ZipEntry(formattedFileName)

                        // Add file to ZIP
                        zos.putNextEntry(entry)
                        file.inputStream().copyTo(zos)
                        zos.closeEntry()

                        // Update progress
                        processedImages++
                        progressState.value = ((processedImages.toFloat() / totalImages) * 100).toInt()
                    }
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
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog title changes based on state
                Text(
                    text = if (isZipping.value) "Zipping Files..." else "Export Project",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Show progress bar during zipping
                if (isZipping.value) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progress.value / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Show warning message if provided
                warningMessage?.let {
                    if (!isZipping.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(150.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium
                            )
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
                        FilledTonalButton(
                            onClick = {
                                isZipping.value = true
                                CoroutineScope(Dispatchers.IO).launch {
                                    onShare(progress, isZipping, onDismiss)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Share")
                        }

                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Save to Documents button
                    FilledTonalButton(
                        onClick = {
                            isZipping.value = true
                            CoroutineScope(Dispatchers.IO).launch {
                                onSave(progress, isZipping, onDismiss)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
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
 * With improved state management and callback handling
 *
 * @param projectId ID of the project to add the AP to
 * @param viewModel ProjectViewModel for database operations
 * @param onDismiss Callback when dialog is dismissed
 * @param onAPAdded Callback when AP is successfully added
 */
@Composable
fun AddAPDialog(
    projectId: String,
    viewModel: ProjectViewModel,
    onDismiss: () -> Unit,
    onAPAdded: (AccessPointEntity) -> Unit = {}
) {
    // State for the input field
    var apName by remember { mutableStateOf(TextFieldValue("")) }
    var isAdding by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Create a focus requester
    val apNameFocusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Access Point") },
        text = {
            Column {
                OutlinedTextField(
                    value = apName,
                    onValueChange = {
                        apName = it
                        errorMessage = null // Clear error on input change
                    },
                    label = { Text("AP Name (e.g., AP05)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(apNameFocusRequester),
                    isError = errorMessage != null,
                    enabled = !isAdding,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters
                    ),
                    singleLine = true
                )

                // Request focus when dialog appears
                LaunchedEffect(Unit) {
                    apNameFocusRequester.requestFocus()
                }

                // Show error message if any
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // Show loading indicator while adding
                if (isAdding) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val name = apName.text.trim()
                    if (name.isEmpty()) {
                        errorMessage = "AP name cannot be empty"
                        return@Button
                    }

                    // Set adding state to true to show progress
                    isAdding = true

                    // Create new AccessPoint entity with a unique ID
                    val newAPId = UUID.randomUUID().toString()
                    val newAP = AccessPointEntity(
                        id = newAPId,
                        projectId = projectId,
                        name = name
                    )

                    // Use a coroutine to add the AP to avoid UI thread blocking
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            // Add to database
                            viewModel.addAccessPoint(newAP)

                            // Wait a moment to ensure the database operation completes
                            delay(300)

                            // Verify the AP was actually added by querying for it
                            val addedAP = viewModel.getAccessPointById(newAPId)

                            withContext(Dispatchers.Main) {
                                if (addedAP != null) {
                                    // Notify caller of success with the new AP
                                    onAPAdded(addedAP)
                                    onDismiss()
                                } else {
                                    // AP was not found in database after adding
                                    errorMessage = "Failed to save AP to database"
                                    isAdding = false
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                errorMessage = "Error: ${e.message}"
                                isAdding = false
                            }
                        }
                    }
                },
                enabled = !isAdding
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isAdding
            ) {
                Text("Cancel")
            }
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
fun renameAPImages(context: Context, projectId: String, accessPointName: String, images: List<String>): List<String> {
    return images.mapIndexed { index, fileName ->
        // Generate new sequential filename that includes the project ID (proj123_AP01-1.jpg)
        val newIndex = index + 1
        val newFileName = "${projectId}_${accessPointName}-$newIndex.jpg"

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

private fun getOriginalBitmapDimensions(file: File): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.absolutePath, options)
    return Pair(options.outWidth, options.outHeight)
}