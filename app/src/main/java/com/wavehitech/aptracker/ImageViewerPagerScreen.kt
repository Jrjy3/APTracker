package com.wavehitech.aptracker

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.Image
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import android.content.Context
import kotlinx.coroutines.withContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import coil.compose.SubcomposeAsyncImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.foundation.gestures.detectTransformGestures
import kotlin.math.atan2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageViewerPagerScreen(
    projectId: String,
    apId: String,
    imageId: String,
    navController: NavHostController
) {
    val context = LocalContext.current
    val viewModel: ProjectViewModel = viewModel(factory = ProjectViewModelFactory(context))
    val coroutineScope = rememberCoroutineScope()

    Log.d("ImageViewerPagerScreen", "Composing with projectId: $projectId, apId: $apId, imageId: $imageId")

    // State for current image
    val currentImageEntity = remember { mutableStateOf<ImageEntity?>(null) }
    var isExplicitlyViewingOriginal by remember { mutableStateOf(false) }

    // Get all images for this access point - use the same query as the AP detail screen
    val images by viewModel.getDisplayImagesForAccessPointFlow(apId)
        .collectAsState(initial = emptyList())

    // Keep track of current image index within the list
    var currentImageIndex by remember { mutableStateOf(0) }

    // Ensure images are loaded first
    var isInitialLoadComplete by remember { mutableStateOf(false) }
    var initialLoadAttempts by remember { mutableStateOf(0) }

    // State for options menu
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Check if current image is a circled version
    val isCircledVersion = currentImageEntity.value?.isCircled == true

    // State for when an image is being zoomed - to disable pager swiping
    var isAnyImageZoomed by remember { mutableStateOf(false) }

    var lastDragDirection by remember { mutableStateOf(Offset.Zero) }

    // Create the pager state after we have images
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { images.size }
    )

    // User scroll settings - only allow swiping when no image is zoomed
    // This is crucial for single-finger swiping to work correctly
    val pagerUserScrollEnabled = !isAnyImageZoomed

    // Initial load to find the current image and its index
    LaunchedEffect(imageId, images) {
        Log.d("ImageViewerPagerScreen", "LaunchedEffect triggered - imageId: $imageId, images size: ${images.size}")

        // Wait for images to be loaded
        if (images.isEmpty()) {
            initialLoadAttempts++
            Log.d("ImageViewerPagerScreen", "No images loaded yet, waiting... (attempt: $initialLoadAttempts)")

            // Force refresh images for this access point
            viewModel.refreshImagesForAccessPoint(apId)

            // If we've tried several times without success, show an error or handle appropriately
            if (initialLoadAttempts > 5) {
                Log.e("ImageViewerPagerScreen", "Failed to load images after multiple attempts")
            }

            return@LaunchedEffect
        }

        try {
            // Reset attempts counter once we have images
            initialLoadAttempts = 0

            // Get the full image entity
            val image = viewModel.getImageById(imageId)
            Log.d("ImageViewerPagerScreen", "Retrieved image entity: ${image?.id}, isCircled: ${image?.isCircled}")

            // IMPORTANT: Don't automatically redirect to circled version
            // Just display the image that was requested
            currentImageEntity.value = image

            // Find the index of the image in our list
            val index = images.indexOfFirst { it.id == imageId }
            Log.d("ImageViewerPagerScreen", "Found image at index: $index")

            if (index >= 0) {
                currentImageIndex = index
                // Scroll to the page
                pagerState.scrollToPage(index)
                isInitialLoadComplete = true
                Log.d("ImageViewerPagerScreen", "Successfully set initial page to $index")

                // Log details about this image
                Log.d("ImageViewerPagerScreen", "Image details - filename: ${image?.filename}, circled: ${image?.isCircled}")
            } else {
                // If we couldn't find the image, log a warning
                Log.w("ImageViewerPagerScreen", "Image not found in list, index: $index")

                // Log all images in the list for debugging
                images.forEachIndexed { idx, img ->
                    Log.d("ImageViewerPagerScreen", "Available image[$idx]: id=${img.id}, filename=${img.filename}")
                }

                // If we have at least one image, show the first one
                if (images.isNotEmpty()) {
                    currentImageEntity.value = images[0]
                    currentImageIndex = 0
                    isInitialLoadComplete = true
                    Log.d("ImageViewerPagerScreen", "Defaulting to first image: ${images[0].id}")
                }
            }
        } catch (e: Exception) {
            Log.e("ImageViewerPagerScreen", "Error loading image data: ${e.message}", e)
            e.printStackTrace()
        }
    }

    // Update current image when page changes
    LaunchedEffect(pagerState.currentPage, images) {
        if (images.isEmpty() || !isInitialLoadComplete) {
            isAnyImageZoomed = false
            Log.d("ImageViewerPagerScreen", "Skipping page change effect - initialLoad: $isInitialLoadComplete, images: ${images.size}")
            return@LaunchedEffect
        }

        if (pagerState.currentPage < images.size) {
            val newImage = images[pagerState.currentPage]
            Log.d("ImageViewerPagerScreen", "Page changed to ${pagerState.currentPage}, image ID: ${newImage.id}")
            currentImageEntity.value = newImage
            currentImageIndex = pagerState.currentPage
        }
    }

    // Function to view original image
    fun viewOriginalImage() {
        if (isCircledVersion && currentImageEntity.value?.originalImageId != null) {
            val originalId = currentImageEntity.value?.originalImageId
            if (originalId != null) {
                Log.d("ImageViewerPagerScreen", "Navigating to view original image: $originalId")
                isExplicitlyViewingOriginal = true
                navController.navigate("originalImageViewer/$projectId/$apId/$originalId")
            } else {
                Toast.makeText(context, "Error: Original image not found", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No original image available", Toast.LENGTH_SHORT).show()
        }
    }

    // State for revert confirmation dialog
    var showRevertConfirmation by remember { mutableStateOf(false) }
    var isReverting by remember { mutableStateOf(false) }

    // Function to revert to original
    fun revertToOriginal() {
        if (isCircledVersion && currentImageEntity.value?.originalImageId != null) {
            showRevertConfirmation = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Extract AP name from the entity
                    val apName = currentImageEntity.value?.let {
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

                    // Show current position
                    if (images.isNotEmpty()) {
                        Text("$apName (${currentImageIndex + 1}/${images.size})",
                            style = MaterialTheme.typography.titleLarge)
                    } else {
                        Text("$apName", style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        Log.d("ImageViewerPagerScreen", "Back button pressed")
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Show circle button only for non-circled images
                    if (currentImageEntity.value != null && !isCircledVersion) {
                        IconButton(onClick = {
                            // Get the current page index
                            val currentPage = pagerState.currentPage
                            if (currentPage < images.size) {
                                Log.d("ImageViewerPagerScreen", "Navigating to single image view for adding circle")
                                // Navigate to single image view to add circle
                                navController.navigate("singleImageView/$projectId/$apId/${images[currentPage].id}")
                            }
                        }) {
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
        content = { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (images.isEmpty()) {
                    // Show loading or empty state
                    Log.d("ImageViewerPagerScreen", "Showing loading indicator - no images available")
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading images...", style = MaterialTheme.typography.bodyMedium)

                        // Add a retry button if we've tried multiple times
                        if (initialLoadAttempts > 3) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                coroutineScope.launch {
                                    Log.d("ImageViewerPagerScreen", "Manual refresh requested")
                                    // Force refresh the images
                                    viewModel.refreshImagesForAccessPoint(apId)
                                    // Reset attempt counter
                                    initialLoadAttempts = 0
                                }
                            }) {
                                Text("Retry")
                            }
                        }
                    }
                } else {
                    // Use HorizontalPager to allow swiping between images
                    Log.d("ImageViewerPagerScreen", "Rendering HorizontalPager with ${images.size} images")
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        key = { page -> images.getOrNull(page)?.id ?: page.toString() },
                        userScrollEnabled = !isAnyImageZoomed
                    ) { page ->
                        // Make sure we don't go out of bounds
                        if (page < images.size) {
                            val image = images[page]
                            Log.d("ImageViewerPagerScreen", "Rendering page $page with image: ${image.id}")

                            // Image display component with fixed implementation
                            SimpleImageViewer(
                                imageEntity = image,
                                context = context,
                                onZoomChanged = { isZoomed ->
                                    // Update the pager's swipe enablement based on zoom state
                                    Log.d("ImageViewerPagerScreen", "Zoom state changed to: $isZoomed")
                                    isAnyImageZoomed = isZoomed
                                }
                            )
                        } else {
                            Log.e("ImageViewerPagerScreen", "Page index out of bounds: $page >= ${images.size}")
                            // Show an error instead of crashing
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Image not available",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    )

    // Revert confirmation dialog
    if (showRevertConfirmation) {
        AlertDialog(
            onDismissRequest = { showRevertConfirmation = false },
            title = { Text("Confirm Revert") },
            text = { Text("Are you sure you want to delete the circled version of the image?") },
            confirmButton = {
                TextButton(onClick = {
                    isReverting = true  // Set loading state
                    val currentId = currentImageEntity.value?.id
                    val originalId = currentImageEntity.value?.originalImageId

                    if (currentId != null && originalId != null) {
                        Log.d("ImageViewerPagerScreen", "Reverting to original - deleting circled version: $currentId")
                        CoroutineScope(Dispatchers.Main).launch {
                            // Delete the circled image
                            viewModel.deleteImage(currentId)

                            // Refresh images and navigate to the original
                            delay(300) // wait for database update
                            viewModel.refreshImagesForAccessPoint(apId)

                            // Navigate to the original image
                            Log.d("ImageViewerPagerScreen", "Navigating to original after revert: $originalId")
                            navController.navigate("imageViewer/$projectId/$apId/$originalId") {
                                popUpTo("imageViewer/$projectId/$apId/$currentId") { inclusive = true }
                            }
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
}

/**
 * Simple image viewer component that handles zoom and pan correctly
 */
@Composable
fun SimpleImageViewer(
    imageEntity: ImageEntity,
    context: Context,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    Log.d("SimpleImageViewer", "Composing for image: ${imageEntity.id}, filename: ${imageEntity.filename}")

    // Helper class to track image dimensions and position
    data class ImageFrame(
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )

    // Container and image size states
    val containerSize = remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    // Loading state
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Image transform state - direct manipulation for gestures
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Animation targets for double-tap zoom
    var targetScale by remember { mutableStateOf(1f) }
    var targetOffsetX by remember { mutableStateOf(0f) }
    var targetOffsetY by remember { mutableStateOf(0f) }
    var isAnimating by remember { mutableStateOf(false) }

    // Animated values for smooth double-tap zoom
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        finishedListener = {
            scale = it
            isAnimating = false
        },
        label = "scaleAnimation"
    )

    val animatedOffsetX by animateFloatAsState(
        targetValue = targetOffsetX,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        finishedListener = {
            offsetX = it
        },
        label = "offsetXAnimation"
    )

    val animatedOffsetY by animateFloatAsState(
        targetValue = targetOffsetY,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        finishedListener = {
            offsetY = it
        },
        label = "offsetYAnimation"
    )

    // Use animated values when animating, direct values when gesturing
    val displayScale = if (isAnimating) animatedScale else scale
    val displayOffsetX = if (isAnimating) animatedOffsetX else offsetX
    val displayOffsetY = if (isAnimating) animatedOffsetY else offsetY

    // Track if we're currently in a manual gesture
    var isInGesture by remember { mutableStateOf(false) }

    // Bitmap state for direct loading
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Get file reference and create URI
    val file = File(context.filesDir, imageEntity.filename)
    val imageUri = remember(imageEntity.id) {
        try {
            if (file.exists() && file.length() > 0) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SimpleImageViewer", "Error creating URI: ${e.message}", e)
            null
        }
    }

    // Function to calculate the visible image frame within container
    fun calculateVisibleImageFrame(): ImageFrame {
        val containerWidth = containerSize.value.width
        val containerHeight = containerSize.value.height

        if (imageSize.width <= 0 || imageSize.height <= 0) {
            return ImageFrame(0f, 0f, containerWidth, containerHeight)
        }

        val imageAspect = imageSize.width / imageSize.height
        val containerAspect = containerWidth / containerHeight

        val visibleWidth: Float
        val visibleHeight: Float
        val frameX: Float
        val frameY: Float

        if (imageAspect > containerAspect) {
            // Image is wider than container (letterboxing on top and bottom)
            visibleWidth = containerWidth
            visibleHeight = containerWidth / imageAspect
            frameX = 0f
            frameY = (containerHeight - visibleHeight) / 2f
        } else {
            // Image is taller than container (letterboxing on sides)
            visibleHeight = containerHeight
            visibleWidth = containerHeight * imageAspect
            frameX = (containerWidth - visibleWidth) / 2f
            frameY = 0f
        }

        return ImageFrame(frameX, frameY, visibleWidth, visibleHeight)
    }

    // Function to calculate max allowed offsets for panning
    fun calculateMaxOffsets(currentScale: Float): Pair<Float, Float> {
        val frame = calculateVisibleImageFrame()

        // Calculate the overflow based on the frame dimensions
        val scaledFrameWidth = frame.width * currentScale
        val scaledFrameHeight = frame.height * currentScale
        val containerWidth = containerSize.value.width
        val containerHeight = containerSize.value.height

        val horizontalOverflow = (scaledFrameWidth - containerWidth) / 2f
        val verticalOverflow = (scaledFrameHeight - containerHeight) / 2f

        return Pair(
            horizontalOverflow.coerceAtLeast(0f),
            verticalOverflow.coerceAtLeast(0f)
        )
    }

    // Load bitmap directly
    LaunchedEffect(imageUri) {
        if (imageUri != null) {
            try {
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    if (inputStream != null) {
                        // Decode bitmap from stream
                        val loadedBitmap = BitmapFactory.decodeStream(inputStream)
                        inputStream.close()

                        if (loadedBitmap != null) {
                            withContext(Dispatchers.Main) {
                                bitmap = loadedBitmap
                                imageSize = Size(loadedBitmap.width.toFloat(), loadedBitmap.height.toFloat())
                                isLoading = false
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                loadError = true
                                errorMessage = "Failed to decode image"
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadError = true
                            errorMessage = "Could not open image file"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("SimpleImageViewer", "Error loading bitmap: ${e.message}", e)
                isLoading = false
                loadError = true
                errorMessage = "Error: ${e.message}"
            }
        } else {
            isLoading = false
            loadError = true
            errorMessage = "Image file not found or invalid"
        }
    }

    // When scale changes, notify the parent about zoom state changes
    // FIX 3: Only report zoomed when actually zoomed AND not in a gesture
    LaunchedEffect(scale, isInGesture) {
        val isZoomed = scale > 1.01f && !isInGesture
        onZoomChanged(isZoomed)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
            },
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else if (loadError || bitmap == null) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Error loading image",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            val imageBitmap = bitmap?.asImageBitmap()
            if (imageBitmap != null) {
                // Create transformable state for pinch/pan gestures
                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                    // Only apply transforms when not animating
                    if (!isAnimating) {
                        // Update scale with constraints
                        scale = (scale * zoomChange).coerceIn(1f, 3f)

                        // FIX 2: Apply full pan sensitivity without scaling
                        if (scale > 1.01f) {
                            // Calculate max offsets to constrain
                            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(scale)

                            // Apply pan at full sensitivity (removed the scaling factor)
                            offsetX = (offsetX + panChange.x).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + panChange.y).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            // Reset offsets when scale is 1
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }

                // Parent Box to properly handle events
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { tapPosition ->
                                    // FIX 1: Add smooth zoom animation back
                                    if (scale > 1.01f) {
                                        // Already zoomed, so zoom out to normal with animation
                                        isAnimating = true
                                        targetScale = 1f
                                        targetOffsetX = 0f
                                        targetOffsetY = 0f
                                    } else {
                                        // Zoom in to the tap position with animation
                                        val newScale = 2.5f

                                        // Get image frame considering the container aspect ratio
                                        val frame = calculateVisibleImageFrame()

                                        // Calculate the tap position relative to the image frame
                                        val imageRelativeX = (tapPosition.x - frame.x) / frame.width
                                        val imageRelativeY = (tapPosition.y - frame.y) / frame.height

                                        // Calculate container center
                                        val containerCenterX = containerSize.value.width / 2
                                        val containerCenterY = containerSize.value.height / 2

                                        // Calculate the focus point in the frame
                                        val focusPointInFrameX = frame.x + frame.width * imageRelativeX
                                        val focusPointInFrameY = frame.y + frame.height * imageRelativeY

                                        // Calculate vector from container center to focus point
                                        val vectorX = focusPointInFrameX - containerCenterX
                                        val vectorY = focusPointInFrameY - containerCenterY

                                        // Calculate required offset to keep tap point centered
                                        val newOffsetX = vectorX * (1 - 1/newScale)
                                        val newOffsetY = vectorY * (1 - 1/newScale)

                                        // Limit offset to keep image visible
                                        val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(newScale)

                                        // Start animation
                                        isAnimating = true
                                        targetScale = newScale
                                        targetOffsetX = newOffsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                        targetOffsetY = newOffsetY.coerceIn(-maxOffsetY, maxOffsetY)

                                        Log.d("SimpleImageViewer", "Animating zoom to: scale=$newScale, " +
                                                "offset=(${targetOffsetX},${targetOffsetY}), " +
                                                "tap=($imageRelativeX,$imageRelativeY), " +
                                                "max=($maxOffsetX,$maxOffsetY)")
                                    }
                                }
                            )
                        }
                        // Add gesture tracking to detect when gestures start/end
                        .pointerInput(Unit) {
                            forEachGesture {
                                awaitPointerEventScope {
                                    // Wait for first pointer down
                                    awaitFirstDown(requireUnconsumed = false)

                                    // Track gesture state for pager interaction
                                    var gestureStarted = false

                                    do {
                                        val event = awaitPointerEvent()

                                        // If we have multiple pointers or significant movement, this is a gesture
                                        if (event.changes.size > 1 ||
                                            event.changes.any { it.positionChanged() &&
                                                    (it.position - it.previousPosition).getDistance() > viewConfiguration.touchSlop }) {
                                            if (!gestureStarted) {
                                                gestureStarted = true
                                                isInGesture = true
                                            }
                                        }

                                    } while (event.changes.any { it.pressed })

                                    // Gesture ended
                                    if (gestureStarted) {
                                        isInGesture = false
                                    }
                                }
                            }
                        }
                ) {
                    // Image with zoom/pan functionality
                    androidx.compose.foundation.Image(
                        bitmap = imageBitmap,
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = displayScale,
                                scaleY = displayScale,
                                translationX = displayOffsetX,
                                translationY = displayOffsetY
                            )
                            // Use the built-in transformable modifier
                            .transformable(state = transformableState),
                        contentScale = ContentScale.Fit
                    )

                    // Reset zoom button
                    AnimatedVisibility(
                        visible = scale > 1.01f || targetScale > 1.01f,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(36.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            FloatingActionButton(
                                onClick = {
                                    // Reset zoom and position with animation
                                    isAnimating = true
                                    targetScale = 1f
                                    targetOffsetX = 0f
                                    targetOffsetY = 0f
                                },
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
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
    }
}

// Helper function to calculate angle between two points
private fun angle(point1: Offset, point2: Offset): Float {
    val deltaX = point2.x - point1.x
    val deltaY = point2.y - point1.y
    return atan2(deltaY, deltaX)
}

// Helper class to track image dimensions and position
data class ImageFrame(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float
)

/**
 * Custom implementation of transform gesture detection with improved behavior
 * for pinch-to-zoom and pan gestures
 */
@Composable
fun ImageViewerPage(
    imageEntity: ImageEntity,
    viewModel: ProjectViewModel,
    projectId: String,
    apId: String,
    onZoomChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Log component entry
    Log.d("ImageViewerPage", "Composing for image: ${imageEntity.id}, filename: ${imageEntity.filename}")

    // Container and image size states
    val containerSize = remember { mutableStateOf(Size.Zero) }
    var imageSize by remember { mutableStateOf(Size.Zero) }

    // Simple loading and error states
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Image transform state
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    // Target scale for animating zoom
    var targetScale by remember { mutableStateOf(1f) }
    var animatingZoom by remember { mutableStateOf(false) }

    // Animated scale value for smoother zoom
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = 300,
            easing = FastOutSlowInEasing
        ),
        // When animation finishes, update the actual scale
        finishedListener = {
            scale = it
            animatingZoom = false
        },
        label = "scaleAnimation"
    )

    // The actual scale to use - direct value for panning, animated for programmatic zoom
    val displayScale = if (animatingZoom) animatedScale else scale

    // Zoomed state
    var isZoomed by remember { mutableStateOf(false) }

    // When the scale changes, update zoom state and notify parent
    LaunchedEffect(scale) {
        val newZoomed = scale > 1.01f
        if (isZoomed != newZoomed) {
            isZoomed = newZoomed
            onZoomChanged(newZoomed)
            Log.d("ImageViewerPage", "Zoom state changed to: $newZoomed")
        }
    }

    // Get file and URI
    val file = File(context.filesDir, imageEntity.filename)
    val imageUri = remember(imageEntity.id) {
        try {
            if (file.exists() && file.length() > 0) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("ImageViewerPage", "Error creating URI: ${e.message}", e)
            null
        }
    }

    // Bitmap state for direct loading
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Function to calculate max allowed offsets for panning
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

    // Function to load the bitmap directly
    LaunchedEffect(imageUri) {
        Log.d("ImageViewerPage", "LaunchedEffect for loading image from URI: $imageUri")
        if (imageUri != null) {
            try {
                // Load bitmap in background
                withContext(Dispatchers.IO) {
                    try {
                        // Open input stream
                        val inputStream = context.contentResolver.openInputStream(imageUri)
                        if (inputStream != null) {
                            // Decode bitmap from input stream
                            val loadedBitmap = BitmapFactory.decodeStream(inputStream)
                            inputStream.close()

                            if (loadedBitmap != null) {
                                Log.d("ImageViewerPage", "Bitmap loaded successfully: ${loadedBitmap.width}x${loadedBitmap.height}")
                                // Update state on main thread
                                withContext(Dispatchers.Main) {
                                    bitmap = loadedBitmap
                                    imageSize = Size(loadedBitmap.width.toFloat(), loadedBitmap.height.toFloat())
                                    isLoading = false
                                }
                            } else {
                                Log.e("ImageViewerPage", "Bitmap decoding failed")
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    loadError = true
                                    errorMessage = "Failed to decode image"
                                }
                            }
                        } else {
                            Log.e("ImageViewerPage", "Could not open input stream")
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                loadError = true
                                errorMessage = "Could not open image file"
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ImageViewerPage", "Error loading bitmap: ${e.message}", e)
                        withContext(Dispatchers.Main) {
                            isLoading = false
                            loadError = true
                            errorMessage = "Error: ${e.message}"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ImageViewerPage", "Error in bitmap loading: ${e.message}", e)
                isLoading = false
                loadError = true
                errorMessage = "Error: ${e.message}"
            }
        } else {
            Log.e("ImageViewerPage", "Image URI is null")
            isLoading = false
            loadError = true
            errorMessage = "Image file not found or invalid"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerSize.value = Size(size.width.toFloat(), size.height.toFloat())
                Log.d("ImageViewerPage", "Container size changed: ${containerSize.value}")
            },
        contentAlignment = Alignment.Center
    ) {
        // Show loading indicator
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Log.d("ImageViewerPage", "Showing loading indicator")
        }
        // Show error message
        else if (loadError || bitmap == null) {
            Log.d("ImageViewerPage", "Showing error state: $errorMessage")
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Error loading image",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error
                )
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // Retry button
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        isLoading = true
                        loadError = false
                        errorMessage = ""
                        bitmap = null
                    }
                ) {
                    Text("Retry")
                }
            }
        }
        // Show image
        else {
            Log.d("ImageViewerPage", "Displaying bitmap")
            // Get the bitmap from state and display it
            val imageBitmap = bitmap?.asImageBitmap()

            if (imageBitmap != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Transformable image with double-tap zoom
                    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                        // Direct scale updates without animation for pinch gestures
                        val prevScale = scale
                        scale = (scale * zoomChange).coerceIn(1f, 3f)

                        // Apply pan changes - scaled properly for the current zoom level
                        if (scale > 1.01f) {
                            // Get max allowed offsets
                            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(scale)

                            // Apply offset scaled by the current scale
                            // This makes panning feel consistent at different zoom levels
                            val scaledOffsetX = offsetChange.x * prevScale
                            val scaledOffsetY = offsetChange.y * prevScale

                            // Update offsets with proper constraints
                            offsetX = (offsetX + scaledOffsetX).coerceIn(-maxOffsetX, maxOffsetX)
                            offsetY = (offsetY + scaledOffsetY).coerceIn(-maxOffsetY, maxOffsetY)
                        } else {
                            // Reset offsets when at normal zoom
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }

                    androidx.compose.foundation.Image(
                        bitmap = imageBitmap,
                        contentDescription = "Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = displayScale,
                                scaleY = displayScale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .transformable(state = transformableState)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = { tapOffset ->
                                        if (scale > 1.01f) {
                                            // Set animation flag
                                            animatingZoom = true

                                            // Reset zoom to 1x
                                            targetScale = 1f
                                            offsetX = 0f
                                            offsetY = 0f
                                        } else {
                                            // Set animation flag
                                            animatingZoom = true

                                            // Zoom in to 2.5x
                                            targetScale = 2.5f
                                            scale = 2.5f // Set immediately for offset calculations

                                            // Calculate the relative position of the tap within the container
                                            val containerSize = containerSize.value
                                            val containerCenterX = containerSize.width / 2f
                                            val containerCenterY = containerSize.height / 2f

                                            // Calculate how far the tap is from center
                                            val distanceX = tapOffset.x - containerCenterX
                                            val distanceY = tapOffset.y - containerCenterY

                                            // Calculate how much to offset to center the tap point
                                            // The factor (1 - 1/scale) adjusts for how the content shifts during zoom
                                            offsetX = -distanceX * (1 - 1/scale)
                                            offsetY = -distanceY * (1 - 1/scale)

                                            // Apply constraints to keep image in bounds
                                            val (maxOffsetX, maxOffsetY) = calculateMaxOffsets(scale)
                                            offsetX = offsetX.coerceIn(-maxOffsetX, maxOffsetX)
                                            offsetY = offsetY.coerceIn(-maxOffsetY, maxOffsetY)
                                        }
                                    }
                                )
                            },
                        contentScale = ContentScale.Fit
                    )

                    // Reset zoom button
                    AnimatedVisibility(
                        visible = scale > 1.01f,
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
                                    // Set animation flag
                                    animatingZoom = true

                                    // Reset zoom with animation
                                    targetScale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
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
    }
}

/**
 * Simplified transform gesture detector that works with the current Compose version
 */
suspend fun PointerInputScope.detectTransformGesturesBasic(
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            // Wait for at least one pointer to press down
            val down = awaitFirstDown(requireUnconsumed = false)
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop

            do {
                val event = awaitPointerEvent()

                // If we have two pointers, calculate zoom and pan
                if (event.changes.size == 2) {
                    // Get the two pointers
                    val pointer1 = event.changes[0]
                    val pointer2 = event.changes[1]

                    // Calculate change in distance between pointers for zoom
                    val previousDistance = (pointer1.previousPosition - pointer2.previousPosition).getDistance()
                    val currentDistance = (pointer1.position - pointer2.position).getDistance()

                    // Guard against division by zero
                    if (previousDistance > 0) {
                        zoom = currentDistance / previousDistance
                    }

                    // Calculate change in center position for pan
                    val previousCenter = (pointer1.previousPosition + pointer2.previousPosition) / 2f
                    val currentCenter = (pointer1.position + pointer2.position) / 2f
                    pan = currentCenter - previousCenter

                    // If this is a zoom interaction (past touch slop), tell the pager to ignore this gesture
                    if (!pastTouchSlop) {
                        // Calculate if we passed the touch slop
                        val panDelta = pan.getDistance()
                        val zoomDelta = abs(1 - zoom) * previousDistance

                        // If past slop for either gesture, mark as consumed
                        if (panDelta > touchSlop || zoomDelta > touchSlop) {
                            pastTouchSlop = true
                        }
                    }

                    // Check if we're zoomed and have moved past touch slop
                    if (pastTouchSlop) {
                        // Run the callback with the gesture data
                        onGesture(currentCenter, pan, zoom, 0f)
                    }
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleImageViewScreen(
    projectId: String,
    apId: String,
    imageId: String,
    navController: NavHostController
) {
    // This is your original ImageViewerScreen but renamed to SingleImageViewScreen
    // to be used for adding circles to images
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
        Log.d("SingleImageViewScreen", "Loading image data for imageId: $imageId, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")

        withContext(Dispatchers.IO) {
            try {
                // Get current image
                val image = viewModel.getImageById(imageId)
                Log.d("SingleImageViewScreen", "Current image: ${image?.filename}, isCircled: ${image?.isCircled}, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")

                withContext(Dispatchers.Main) {
                    imageEntity.value = image
                    Log.d("SingleImageViewScreen", "Updated imageEntity: ${imageEntity.value?.filename}, isCircled: ${imageEntity.value?.isCircled}")
                    // Automatically show circle UI when entering this screen
                    showCircle = true
                }

                // Get original if this is a circled version
                if (image?.isCircled == true && image.originalImageId != null) {
                    Log.d("SingleImageViewScreen", "Getting original image with ID: ${image.originalImageId}")
                    val original = viewModel.getImageById(image.originalImageId)
                    Log.d("SingleImageViewScreen", "Original image: ${original?.filename}")

                    withContext(Dispatchers.Main) {
                        originalImageEntity.value = original
                        Log.d("SingleImageViewScreen", "Updated originalImageEntity: ${originalImageEntity.value?.filename}, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")
                    }
                } else {
                    // Reset originalImageEntity if the current image is not a circled version
                    withContext(Dispatchers.Main) {
                        originalImageEntity.value = null
                        Log.d("SingleImageViewScreen", "Reset originalImageEntity to null, isExplicitlyViewingOriginal: $isExplicitlyViewingOriginal")
                    }
                }

                // Update loading state when complete
                withContext(Dispatchers.Main) {
                    isLoading = false
                }
            } catch (e: Exception) {
                Log.e("SingleImageViewScreen", "Error loading image data: ${e.message}", e)
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
                Log.e("SingleImageViewScreen", "File doesn't exist: ${it.absolutePath}")
                null
            }
        }
    } catch (e: Exception) {
        Log.e("SingleImageViewScreen", "Error getting URI: ${e.message}", e)
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

                    // Navigate back to the pager view with the new circled version
                    navController.navigate("imageViewer/$projectId/$apId/${circledImageEntity.id}") {
                        popUpTo("singleImageView/$projectId/$apId/$imageId") { inclusive = true }
                    }
                }
            } catch (e: Exception) {
                Log.e("SingleImageViewScreen", "Error saving image with circle: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving image: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
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
                    Text("Add Circle to $apName", style = MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save button
                    IconButton(onClick = { saveImageWithCircle() }) {
                        Icon(Icons.Default.Check, contentDescription = "Save with Circle")
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
                            Log.e("SingleImageViewScreen", "Error getting bitmap: ${e.message}", e)
                        }
                    }
                )

                // Circle overlay for editing
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
        }
    }
}

/**
 * Custom implementation of transform gesture detection with improved behavior
 * for pinch-to-zoom and pan gestures
 */
suspend fun PointerInputScope.detectTransformGestures(
    panZoomLock: Boolean = false,
    onGestureStart: () -> Unit = {},
    onGestureEnd: () -> Unit = {},
    onGesture: (centroid: Offset, pan: Offset, zoom: Float, rotation: Float) -> Unit
) {
    forEachGesture {
        awaitPointerEventScope {
            var rotation = 0f
            var zoom = 1f
            var pan = Offset.Zero
            var pastTouchSlop = false
            val touchSlop = viewConfiguration.touchSlop
            var lockedToPanZoom = false

            // For two finger gestures
            var firstFingerPosition = Offset.Zero
            var secondFingerPosition = Offset.Zero

            awaitFirstDown(requireUnconsumed = false)
            onGestureStart()

            do {
                val event = awaitPointerEvent()

                // Check for position changes that are already consumed
                val positionChangesConsumed = event.changes.any {
                    it.positionChanged() && it.position.x != it.previousPosition.x
                }

                if (!positionChangesConsumed) {
                    val pointerSize = event.changes.size

                    // Handle two finger zoom/pan
                    if (pointerSize == 2) {
                        val firstPointer = event.changes[0]
                        val secondPointer = event.changes[1]
                        val firstPointerPosition = firstPointer.position
                        val secondPointerPosition = secondPointer.position

                        if (firstFingerPosition == Offset.Zero && secondFingerPosition == Offset.Zero) {
                            // First detection of two fingers - store initial positions
                            firstFingerPosition = firstPointerPosition
                            secondFingerPosition = secondPointerPosition
                        } else {
                            // Calculate new positions and changes
                            val oldCentroid = (firstFingerPosition + secondFingerPosition) / 2f
                            val newCentroid = (firstPointerPosition + secondPointerPosition) / 2f

                            // Pan gesture
                            pan = newCentroid - oldCentroid

                            // Zoom gesture
                            val oldDistance = (firstFingerPosition - secondFingerPosition).getDistance()
                            val newDistance = (firstPointerPosition - secondPointerPosition).getDistance()
                            zoom = if (oldDistance > 0) newDistance / oldDistance else 1f

                            // Update for next iteration
                            firstFingerPosition = firstPointerPosition
                            secondFingerPosition = secondPointerPosition

                            // Detect if we passed the touch slop threshold
                            if (!pastTouchSlop) {
                                val centroidChange = (newCentroid - oldCentroid).getDistance()
                                val zoomChange = abs(zoom - 1f) * oldDistance

                                if (centroidChange > touchSlop || zoomChange > touchSlop) {
                                    pastTouchSlop = true
                                }
                            }

                            // If we passed the touch slop, consume the position changes
                            if (pastTouchSlop) {
                                // Indicate that we've consumed these changes for the pager to know
                                firstPointer.previousPosition
                                secondPointer.previousPosition

                                // Call gesture callback with the calculated values
                                onGesture(newCentroid, pan, zoom, rotation)
                            }
                        }
                    } else {
                        // If we go back to one finger, reset the two-finger tracking
                        firstFingerPosition = Offset.Zero
                        secondFingerPosition = Offset.Zero
                    }
                }
            } while (!positionChangesConsumed && event.changes.any { it.pressed })

            onGestureEnd()
        }
    }
}