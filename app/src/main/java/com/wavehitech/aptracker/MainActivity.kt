@file:OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)

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
import androidx.compose.material.icons.filled.ZoomOut
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
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import android.graphics.drawable.BitmapDrawable

// For drawing the circle and handles
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill

// For math operations
import kotlin.math.abs

// For the bottom slider control
import androidx.compose.material.Slider


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
                                name = apName
                                // No pictures parameter needed anymore
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

    // Load image data when screen is shown
    LaunchedEffect(imageId) {
        withContext(Dispatchers.IO) {
            try {
                // Get current image
                val image = viewModel.getImageById(imageId)
                Log.d("ImageViewerScreen", "Current image: ${image?.filename}, isCircled: ${image?.isCircled}")

                withContext(Dispatchers.Main) {
                    imageEntity.value = image
                }

                // Get original if this is a circled version
                if (image?.isCircled == true && image.originalImageId != null) {
                    Log.d("ImageViewerScreen", "Getting original image with ID: ${image.originalImageId}")
                    val original = viewModel.getImageById(image.originalImageId)
                    Log.d("ImageViewerScreen", "Original image: ${original?.filename}")

                    withContext(Dispatchers.Main) {
                        originalImageEntity.value = original
                    }
                }

                // Only redirect to circled version if we're not explicitly viewing the original
                if (imageEntity.value?.isCircled == false && !isExplicitlyViewingOriginal) {
                    val circled = viewModel.getCircledVersionOfImage(imageEntity.value?.id ?: "")
                    Log.d("ImageViewerScreen", "Circled version: ${circled?.filename}")

                    if (circled != null) {
                        withContext(Dispatchers.Main) {
                            // If a circled version exists, navigate to it instead
                            navController.navigate("imageViewer/$projectId/$apId/${circled.id}") {
                                popUpTo("imageViewer/$projectId/$apId/$imageId") { inclusive = true }
                            }
                        }
                    }
                }

                // Reset the flag after loading is complete
                withContext(Dispatchers.Main) {
                    isExplicitlyViewingOriginal = false
                }
            } catch (e: Exception) {
                Log.e("ImageViewerScreen", "Error loading image data: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error loading image: ${e.message}", Toast.LENGTH_SHORT).show()
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

    // Get file reference
    val file = imageEntity.value?.let { File(context.filesDir, it.filename) }

    // Get content URI
    val imageUri = try {
        file?.let {
            FileProvider.getUriForFile(context, "${context.packageName}.provider", it)
        }
    } catch (e: Exception) {
        Log.e("ImageViewerScreen", "Error getting URI: ${e.message}", e)
        null
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
        finishedListener = { isGestureInProgress = false }
    )

    val offsetXAnimated by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
    )

    val offsetYAnimated by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
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

                val scaleX = imageWidth / containerWidth
                val scaleY = imageHeight / containerHeight

                // Scale circle dimensions
                val scaledCircleX = circlePosition.x * scaleX
                val scaledCircleY = circlePosition.y * scaleY
                val scaledCircleRadius = circleRadius * scaleX
                val scaledStrokeWidth = circleThickness * scaleX

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
                // Set the flag that we're explicitly viewing the original
                isExplicitlyViewingOriginal = true

                // Log for debugging
                Log.d("ImageViewerScreen", "Navigating to original image with ID: $originalId")

                // Navigate to original using popUpTo to clear the back stack
                navController.navigate("imageViewer/$projectId/$apId/$originalId") {
                    popUpTo("imageViewer/$projectId/$apId/$imageId") { inclusive = true }
                }
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
                    Text("$apName", style = MaterialTheme.typography.h6)
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
                                DropdownMenuItem(onClick = {
                                    viewOriginalImage()
                                    showOptionsMenu = false
                                }) {
                                    Text("View Original")
                                }

                                DropdownMenuItem(onClick = {
                                    revertToOriginal()
                                    showOptionsMenu = false
                                }) {
                                    Text("Revert to Original")
                                }
                            }
                        }
                    }
                }
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
                        .background(MaterialTheme.colors.surface)
                        .padding(16.dp)
                ) {
                    Text("Circle Thickness: ${circleThickness.toInt()}px")
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
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
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
                        contentDescription = "Full-size image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .then(
                                if (!showCircle) {
                                    Modifier
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
                                        }
                                } else {
                                    Modifier
                                }
                            ),
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

                    // Draw circle overlay when enabled
                    if (showCircle) {
                        // Circle overlay with resize handles
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            // Check if a resize handle was touched
                                            val handleSize = 40f

                                            // East handle (right)
                                            val eastHandle = Offset(circlePosition.x + circleRadius, circlePosition.y)
                                            if ((offset - eastHandle).getDistance() < handleSize) {
                                                selectedHandle = "east"
                                                return@detectDragGestures
                                            }

                                            // West handle (left)
                                            val westHandle = Offset(circlePosition.x - circleRadius, circlePosition.y)
                                            if ((offset - westHandle).getDistance() < handleSize) {
                                                selectedHandle = "west"
                                                return@detectDragGestures
                                            }

                                            // North handle (top)
                                            val northHandle = Offset(circlePosition.x, circlePosition.y - circleRadius)
                                            if ((offset - northHandle).getDistance() < handleSize) {
                                                selectedHandle = "north"
                                                return@detectDragGestures
                                            }

                                            // South handle (bottom)
                                            val southHandle = Offset(circlePosition.x, circlePosition.y + circleRadius)
                                            if ((offset - southHandle).getDistance() < handleSize) {
                                                selectedHandle = "south"
                                                return@detectDragGestures
                                            }

                                            // If near circle edge (but not on handle), move entire circle
                                            val distanceFromCenter = (offset - circlePosition).getDistance()
                                            val isNearEdge = abs(distanceFromCenter - circleRadius) < handleSize

                                            if (distanceFromCenter < circleRadius || isNearEdge) {
                                                selectedHandle = "move"
                                            } else {
                                                selectedHandle = null
                                            }
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()

                                            when (selectedHandle) {
                                                "east" -> {
                                                    // Resize from right
                                                    val newRadius = (change.position.x - circlePosition.x)
                                                    if (newRadius > 30f) {
                                                        circleRadius = newRadius
                                                    }
                                                }
                                                "west" -> {
                                                    // Resize from left
                                                    val newRadius = (circlePosition.x - change.position.x)
                                                    if (newRadius > 30f) {
                                                        circleRadius = newRadius
                                                    }
                                                }
                                                "north" -> {
                                                    // Resize from top
                                                    val newRadius = (circlePosition.y - change.position.y)
                                                    if (newRadius > 30f) {
                                                        circleRadius = newRadius
                                                    }
                                                }
                                                "south" -> {
                                                    // Resize from bottom
                                                    val newRadius = (change.position.y - circlePosition.y)
                                                    if (newRadius > 30f) {
                                                        circleRadius = newRadius
                                                    }
                                                }
                                                "move" -> {
                                                    // Move circle
                                                    circlePosition = Offset(
                                                        (circlePosition.x + dragAmount.x).coerceIn(
                                                            circleRadius,
                                                            containerSize.value.width - circleRadius
                                                        ),
                                                        (circlePosition.y + dragAmount.y).coerceIn(
                                                            circleRadius,
                                                            containerSize.value.height - circleRadius
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            selectedHandle = null
                                        }
                                    )
                                }
                        ) {
                            // Draw circle
                            drawCircle(
                                color = Color.Red,
                                radius = circleRadius,
                                center = circlePosition,
                                style = Stroke(width = circleThickness)
                            )

                            // Draw handles
                            val handleRadius = 15f

                            // East handle (right)
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius,
                                center = Offset(circlePosition.x + circleRadius, circlePosition.y),
                                style = Fill
                            )
                            drawCircle(
                                color = Color.Red,
                                radius = handleRadius,
                                center = Offset(circlePosition.x + circleRadius, circlePosition.y),
                                style = Stroke(width = 2f)
                            )

                            // West handle (left)
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius,
                                center = Offset(circlePosition.x - circleRadius, circlePosition.y),
                                style = Fill
                            )
                            drawCircle(
                                color = Color.Red,
                                radius = handleRadius,
                                center = Offset(circlePosition.x - circleRadius, circlePosition.y),
                                style = Stroke(width = 2f)
                            )

                            // North handle (top)
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius,
                                center = Offset(circlePosition.x, circlePosition.y - circleRadius),
                                style = Fill
                            )
                            drawCircle(
                                color = Color.Red,
                                radius = handleRadius,
                                center = Offset(circlePosition.x, circlePosition.y - circleRadius),
                                style = Stroke(width = 2f)
                            )

                            // South handle (bottom)
                            drawCircle(
                                color = Color.White,
                                radius = handleRadius,
                                center = Offset(circlePosition.x, circlePosition.y + circleRadius),
                                style = Fill
                            )
                            drawCircle(
                                color = Color.Red,
                                radius = handleRadius,
                                center = Offset(circlePosition.x, circlePosition.y + circleRadius),
                                style = Stroke(width = 2f)
                            )
                        }
                    }
                }

                // Reset zoom button (only when zoomed in and circle not visible)
                AnimatedVisibility(
                    visible = scale > 1.01f && !showCircle,
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
                            backgroundColor = MaterialTheme.colors.primary,
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
                                contentDescription = "Reset Zoom",
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                // Error state
                Text("Error loading image", style = MaterialTheme.typography.h6)
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
                Button(onClick = {
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
                Button(onClick = { showRevertConfirmation = false }) {
                    Text("No")
                }
            }
        )
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
                                text = "${ap.name} (${apImageCounts[ap.id] ?: 0})",
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
                // Prepare images before export using the new ImageEntity system
                CoroutineScope(Dispatchers.IO).launch {
                    // For each access point, ensure filenames are properly sequential
                    projectWithAP.accessPoints.forEach { ap ->
                        // Get all images for this AP
                        val images = viewModel.getImagesForAccessPointFlow(ap.id).first()

                        // Sort by order index
                        val sortedImages = images.sortedBy { it.orderIndex }

                        // Update filenames if needed
                        sortedImages.forEachIndexed { index, image ->
                            val expectedFilename = "${projectWithAP.project.id}_${ap.name}-${index + 1}.jpg"

                            if (image.filename != expectedFilename) {
                                // Rename the file
                                val oldFile = File(context.filesDir, image.filename)
                                val newFile = File(context.filesDir, expectedFilename)

                                if (oldFile.exists()) {
                                    oldFile.renameTo(newFile)

                                    // Update database
                                    viewModel.updateImage(image.copy(filename = expectedFilename))
                                }
                            }
                        }
                    }

                    // Now create the zip file
                    val zipFile = createProjectZip(
                        context,
                        projectWithAP,
                        viewModel,
                        progressState,
                        isZipping,
                        onDismissExport
                    )

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
                // Prepare images before export using the new ImageEntity system
                CoroutineScope(Dispatchers.IO).launch {
                    // For each access point, ensure filenames are properly sequential
                    projectWithAP.accessPoints.forEach { ap ->
                        // Get all images for this AP
                        val images = viewModel.getImagesForAccessPointFlow(ap.id).first()

                        // Sort by order index
                        val sortedImages = images.sortedBy { it.orderIndex }

                        // Update filenames if needed
                        sortedImages.forEachIndexed { index, image ->
                            val expectedFilename = "${projectWithAP.project.id}_${ap.name}-${index + 1}.jpg"

                            if (image.filename != expectedFilename) {
                                // Rename the file
                                val oldFile = File(context.filesDir, image.filename)
                                val newFile = File(context.filesDir, expectedFilename)

                                if (oldFile.exists()) {
                                    oldFile.renameTo(newFile)

                                    // Update database
                                    viewModel.updateImage(image.copy(filename = expectedFilename))
                                }
                            }
                        }
                    }

                    // Now create the zip file
                    val zipFile = createProjectZip(
                        context,
                        projectWithAP,
                        viewModel,
                        progressState,
                        isZipping,
                        onDismissExport
                    )

                    withContext(Dispatchers.Main) {
                        shareZipFile(context, zipFile)
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

// New helper function to create the project zip file with the new image system
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

    // Calculate total number of images to track progress
    var totalImages = 0
    var processedImages = 0

    projectWithAP.accessPoints.forEach { ap ->
        val imageCount = viewModel.getImagesForAccessPointFlow(ap.id).first().size
        totalImages += imageCount
    }

    // Create ZIP stream and add files
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        projectWithAP.accessPoints.forEach { ap ->
            // Get all images for this AP
            val images = viewModel.getImagesForAccessPointFlow(ap.id).first()

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

    // Switch back to Main thread to update UI when done
    withContext(Dispatchers.Main) {
        isZipping.value = false
        onDismiss()
    }

    return zipFile
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

    // Get display images (preferring circled versions when available)
    val displayImages by viewModel.getDisplayImagesForAccessPointFlow(accessPoint.id)
        .collectAsState(initial = emptyList())

    // Current photo capture state
    var currentImageName by remember { mutableStateOf("") }
    var imageUri: Uri? by rememberSaveable(stateSaver = UriNullableSaver) { mutableStateOf(null) }

    // Selection mode state
    var selectionMode by remember { mutableStateOf(false) }
    val selectedImages = remember { mutableStateListOf<String>() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Camera integration
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && imageUri != null) {
            // If photo capture was successful, add a new ImageEntity to the database
            CoroutineScope(Dispatchers.IO).launch {
                // Get the next order index
                val nextOrderIndex = displayImages.size

                // Create new image entity
                val newImage = ImageEntity(
                    accessPointId = accessPoint.id,
                    filename = currentImageName,
                    isCircled = false,
                    orderIndex = nextOrderIndex
                )

                // Add to database
                viewModel.addImage(newImage)
            }
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
                title = { Text("${accessPoint.name} Pictures", style = MaterialTheme.typography.h6) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode && selectedImages.isNotEmpty()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    }
                },
                elevation = 4.dp
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                try {
                    // Generate a unique sequential filename for the new image
                    currentImageName = "${projectWithAP.project.id}_${accessPoint.name}-${displayImages.size + 1}.jpg"
                    val file = File(context.filesDir, currentImageName)

                    // Ensure parent directories exist
                    file.parentFile?.mkdirs()

                    // Get content URI for this file
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                    imageUri = uri

                    // Launch camera
                    cameraLauncher.launch(uri)
                } catch (e: Exception) {
                    Log.e("ModernAPDetailScreen", "Error launching camera: ${e.message}", e)
                    Toast.makeText(context, "Error launching camera: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }) {
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
                        Text("No pictures available. Tap + to add.", style = MaterialTheme.typography.body1)
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
                                    .combinedClickable(
                                        onClick = {
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
                                        },
                                        onLongClick = {
                                            // Enter selection mode
                                            if (!selectionMode) selectionMode = true
                                            if (!selectedImages.contains(image.id)) {
                                                selectedImages.add(image.id)
                                            }
                                        }
                                    )
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
                                            .background(Color.Black.copy(alpha = 0.4f)),
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
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(4.dp)
                                                .size(16.dp)
                                                .background(
                                                    color = Color.Red,
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
    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = {
                Text("Are you sure you want to delete ${selectedImages.size} image(s)?")
            },
            confirmButton = {
                Button(onClick = {
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
                    Text("Delete")
                }
            }, // Add this comma here
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

fun getCleanApNameFromFilename(filename: String): String {
    // Find the underscore that separates project ID from AP name
    val underscoreIndex = filename.indexOf('_')
    if (underscoreIndex >= 0) {
        // Extract the part after the underscore up to the dash
        val dashIndex = filename.indexOf('-', underscoreIndex)
        if (dashIndex >= 0) {
            return filename.substring(underscoreIndex + 1, dashIndex)
        }
    }
    return filename // Return the original if parsing fails
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
                        name = name
                        // Removed the pictures parameter as it's no longer needed
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