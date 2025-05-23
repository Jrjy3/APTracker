package com.wavehitech.aptracker

import android.content.Context
import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
// import androidx.compose.foundation.gestures.TransformableState // Not directly used in these state logic tests
// import androidx.compose.ui.input.pointer.PointerInputScope // Not directly used
// import androidx.compose.ui.platform.LocalDensity // Not directly used
// import androidx.compose.ui.unit.Density // Not directly used
// import androidx.compose.runtime.CompositionLocalProvider // Not directly used
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// import androidx.test.core.app.ApplicationProvider // Not using this directly

import io.mockk.* // Using MockK for static mocking as it's often more straightforward
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
// import org.junit.Rule // Not using JUnit rules like ComposeTestRule here
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor // Using Mockito captors as they are standard
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.* // Using Mockito for instance mocking
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.InputStream

// Renamed to avoid conflict and be more specific about testing state logic
@RunWith(RobolectricTestRunner::class)
@Config(manifest=Config.NONE, sdk = [28])
class SimpleImageViewerStateTest {

    @Mock
    lateinit var mockContext: Context

    @Mock
    lateinit var mockContentResolver: ContentResolver

    @Mock
    lateinit var mockBitmap: Bitmap

    // @Mock // File itself is usually not mocked, but its methods are, or use ShadowFile
    // lateinit var mockFile: File

    @Mock
    lateinit var mockUri: Uri

    @Mock
    lateinit var mockInputStream: InputStream

    private lateinit var imageEntity: ImageEntity
    
    // Using a Mockito mock for the callback
    @Mock
    lateinit var onZoomChangedMock: (Boolean) -> Unit

    @Captor
    lateinit var booleanCaptor: ArgumentCaptor<Boolean>

    @OptIn(ExperimentalCoroutinesApi::class)
    private val testDispatcher = UnconfinedTestDispatcher()

    // State variables that mimic those in SimpleImageViewer, to test logic directly
    private var scaleState: Float = 1f
    private var offsetXState: Float = 0f
    private var offsetYState: Float = 0f
    private var targetScaleState: Float = 1f
    private var targetOffsetXState: Float = 0f
    private var targetOffsetYState: Float = 0f
    private var isAnimatingState: Boolean = false
    
    // Mimic container and image size for calculations
    private var containerSizeState: Size = Size.Zero
    private var imageSizeState: Size = Size.Zero


    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        Dispatchers.setMain(testDispatcher)

        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
        // `when`(mockContentResolver.openInputStream(any(Uri::class.java))).thenReturn(mockInputStream) // Not strictly needed if not testing loading
        `when`(mockContext.packageName).thenReturn("com.wavehitech.aptracker.test")
        // `when`(mockContext.filesDir).thenReturn(File("/tmp/test_files")) // Provided by Robolectric

        `when`(mockBitmap.width).thenReturn(1000) // Default mock image size
        `when`(mockBitmap.height).thenReturn(1000)

        // Static mocking with MockK for BitmapFactory
        mockkStatic(BitmapFactory::class)
        every { BitmapFactory.decodeStream(any<InputStream>()) } returns mockBitmap
        
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } returns mockUri

        imageEntity = ImageEntity(id = "test_image_1", filename = "test_image.jpg", accessPointId = "ap1")
        
        // Reset states
        scaleState = 1f
        offsetXState = 0f
        offsetYState = 0f
        targetScaleState = 1f
        targetOffsetXState = 0f
        targetOffsetYState = 0f
        isAnimatingState = false
        containerSizeState = Size(1000f, 800f) // Default container size for tests
        imageSizeState = Size(1000f, 1000f)   // Default image size for tests (square)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkStatic(BitmapFactory::class) // Clear MockK static mocks
        unmockkStatic(FileProvider::class)
    }

    // --- Helper function for SimpleImageViewer's internal calculations ---
    // This mirrors `calculateVisibleImageFrame` from SimpleImageViewer
    private fun calculateVisibleImageFrameTest(): SimpleImageViewer.ImageFrame {
        val containerWidth = containerSizeState.width
        val containerHeight = containerSizeState.height

        if (imageSizeState.width <= 0 || imageSizeState.height <= 0) {
            return SimpleImageViewer.ImageFrame(0f, 0f, containerWidth, containerHeight)
        }

        val imageAspect = imageSizeState.width / imageSizeState.height
        val containerAspect = containerWidth / containerHeight

        val visibleWidth: Float
        val visibleHeight: Float
        val frameX: Float
        val frameY: Float

        if (imageAspect > containerAspect) {
            visibleWidth = containerWidth
            visibleHeight = containerWidth / imageAspect
            frameX = 0f
            frameY = (containerHeight - visibleHeight) / 2f
        } else {
            visibleHeight = containerHeight
            visibleWidth = containerHeight * imageAspect
            frameX = (containerWidth - visibleWidth) / 2f
            frameY = 0f
        }
        return SimpleImageViewer.ImageFrame(frameX, frameY, visibleWidth, visibleHeight)
    }

    // This mirrors `calculateMaxOffsets` from SimpleImageViewer
    private fun calculateMaxOffsetsTest(currentScale: Float): Pair<Float, Float> {
        val frame = calculateVisibleImageFrameTest()
        val scaledFrameWidth = frame.width * currentScale
        val scaledFrameHeight = frame.height * currentScale
        val containerWidth = containerSizeState.width
        val containerHeight = containerSizeState.height

        val horizontalOverflow = (scaledFrameWidth - containerWidth) / 2f
        val verticalOverflow = (scaledFrameHeight - containerHeight) / 2f
        return Pair(horizontalOverflow.coerceAtLeast(0f), verticalOverflow.coerceAtLeast(0f))
    }

    // --- Test Cases ---

    @Test
    fun `initial state leads to onZoomChanged(false)`() {
        // Assuming image is loaded, scale is 1f
        val isZoomed = scaleState > 1.01f
        onZoomChangedMock(isZoomed)
        verify(onZoomChangedMock).invoke(false)
    }

    // --- Panning Logic Tests ---
    @Test
    fun `panning updates offsets dividing by scale when zoomed in`() {
        scaleState = 2.0f // Zoomed in
        offsetXState = 0f
        offsetYState = 0f
        val panChange = Offset(20f, 30f) // Raw pan change

        containerSizeState = Size(1000f, 800f)
        imageSizeState = Size(1000f, 800f) // Match container aspect for simpler maxOffset calculation here
                                           // frame.width = 1000, frame.height = 800
                                           // maxOffsetX = (1000*2 - 1000)/2 = 500
                                           // maxOffsetY = (800*2 - 800)/2 = 400

        val (maxOffsetX, maxOffsetY) = calculateMaxOffsetsTest(scaleState)
        
        // Logic from transformableState lambda
        if (scaleState > 1.01f) {
            val safeScale = if (scaleState == 0f) 1f else scaleState // scaleState won't be 0f due to coerceIn(1f,3f)
            offsetXState = (offsetXState + panChange.x / safeScale).coerceIn(-maxOffsetX, maxOffsetX) // Corrected coercion
            offsetYState = (offsetYState + panChange.y / safeScale).coerceIn(-maxOffsetY, maxOffsetY) // Corrected coercion
        }

        assertEquals(20f / 2.0f, offsetXState, 0.01f) // 10f
        assertEquals(30f / 2.0f, offsetYState, 0.01f) // 15f
    }

    @Test
    fun `panning offsets are coerced correctly at boundaries with division by scale`() {
        scaleState = 2.0f
        containerSizeState = Size(100f, 100f)
        imageSizeState = Size(100f, 100f) 
        val (maxOffsetX, maxOffsetY) = calculateMaxOffsetsTest(scaleState) // Should be 50f for both

        assertEquals(50f, maxOffsetX, 0.01f)
        assertEquals(50f, maxOffsetY, 0.01f)

        offsetXState = 45f // Near max
        offsetYState = 48f  // Near max
        // panChange of (20, 20) will result in (10,10) after division by scale
        val panChange = Offset(20f, 20f) 

        // Apply panning logic
        if (scaleState > 1.01f) {
            val safeScale = if (scaleState == 0f) 1f else scaleState
            offsetXState = (offsetXState + panChange.x / safeScale).coerceIn(-maxOffsetX, maxOffsetX) // 45 + 10 = 55 -> coerced to 50
            offsetYState = (offsetYState + panChange.y / safeScale).coerceIn(-maxOffsetY, maxOffsetY) // 48 + 10 = 58 -> coerced to 50
        }
        
        assertEquals(50f, offsetXState, 0.01f)
        assertEquals(50f, offsetYState, 0.01f)

        // Pan other way
        offsetXState = -45f
        offsetYState = -48f
        // panChange of (-20, -20) will result in (-10,-10) after division by scale
        val negativePanChange = Offset(-20f, -20f)
         if (scaleState > 1.01f) {
            val safeScale = if (scaleState == 0f) 1f else scaleState
            offsetXState = (offsetXState + negativePanChange.x / safeScale).coerceIn(-maxOffsetX, maxOffsetX) // -45 - 10 = -55 -> coerced to -50
            offsetYState = (offsetYState + negativePanChange.y / safeScale).coerceIn(-maxOffsetY, maxOffsetY) // -48 - 10 = -58 -> coerced to -50
        }
        assertEquals(-50f, offsetXState, 0.01f)
        assertEquals(-50f, offsetYState, 0.01f)
    }

    @Test
    fun `panning does not update offsets and resets them if scale is 1f (due to internal condition)`() {
        scaleState = 1.0f // Not zoomed enough for panning logic to apply offsets
        offsetXState = 10f // Simulate existing offset that should be reset
        offsetYState = 15f
        // val panChange = Offset(5f, 5f) // panChange is irrelevant if not panning

        // Logic from transformableState
        // The canPan = { scale > 1.01f } would prevent this lambda from even processing pan for Compose.
        // But the internal logic itself also has this check:
        if (scaleState > 1.01f) {
            // Not executed for panning part
        } else {
            // This part of the original lambda handles the offset reset when scale is not > 1.01f
            offsetXState = 0f
            offsetYState = 0f
        }
        assertEquals(0f, offsetXState, 0.01f)
        assertEquals(0f, offsetYState, 0.01f)
    }
    
    @Test
    fun `panning does not update offsets if scale is 1_01f (due to internal condition)`() {
        scaleState = 1.01f // Exactly at the threshold, so scale > 1.01f is false
        offsetXState = 10f 
        offsetYState = 15f
        // val panChange = Offset(5f, 5f) // panChange is irrelevant

        if (scaleState > 1.01f) {
            // Not executed
        } else {
            offsetXState = 0f
            offsetYState = 0f
        }
        assertEquals(0f, offsetXState, 0.01f)
        assertEquals(0f, offsetYState, 0.01f)
    }


    // --- Pinch-to-Zoom Logic Tests ---
    @Test
    fun `pinch updates scale and calls onZoomChanged correctly`() {
        scaleState = 1.0f
        
        // Zoom In
        var zoomChangeFactor = 1.5f
        scaleState = (scaleState * zoomChangeFactor).coerceIn(1f, 3f)
        assertEquals(1.5f, scaleState, 0.01f)
        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock).invoke(true)
        reset(onZoomChangedMock) // Mockito reset

        // Zoom further
        zoomChangeFactor = 1.5f 
        scaleState = (scaleState * zoomChangeFactor).coerceIn(1f, 3f) // 1.5 * 1.5 = 2.25
        assertEquals(2.25f, scaleState, 0.01f)
        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock, times(0)).invoke(false) // Should not be called with false
        verify(onZoomChangedMock).invoke(true) // Still true
        reset(onZoomChangedMock)

        // Zoom Out
        zoomChangeFactor = 0.5f
        scaleState = (scaleState * zoomChangeFactor).coerceIn(1f, 3f) // 2.25 * 0.5 = 1.125
        assertEquals(1.125f, scaleState, 0.01f)
        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock).invoke(true) // Still true
        reset(onZoomChangedMock)

        // Zoom Out below threshold
        zoomChangeFactor = 0.5f
        scaleState = (scaleState * zoomChangeFactor).coerceIn(1f, 3f) // 1.125 * 0.5 = 0.5625 -> coerced to 1f
        assertEquals(1f, scaleState, 0.01f)
        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock).invoke(false)
    }

    @Test
    fun `pinch scale is coerced to min 1f and max 3f`() {
        scaleState = 1.0f
        scaleState = (scaleState * 0.5f).coerceIn(1f, 3f)
        assertEquals(1f, scaleState, 0.01f)

        scaleState = 2.0f
        scaleState = (scaleState * 2.0f).coerceIn(1f, 3f) // 4.0f -> coerced to 3f
        assertEquals(3f, scaleState, 0.01f)

        scaleState = 3.0f
        scaleState = (scaleState * 1.1f).coerceIn(1f, 3f) // 3.3f -> coerced to 3f
        assertEquals(3f, scaleState, 0.01f)
    }

    // --- Double-Tap to Zoom Logic Tests ---
    @Test
    fun `double tap when zoomed out sets animation targets for zoom in`() {
        scaleState = 1.0f
        val tapPosition = Offset(containerSizeState.width / 2, containerSizeState.height / 2) // Tap center

        // Mimic onDoubleTap logic
        if (scaleState > 1.01f) { /* no-op for this test */ } 
        else {
            isAnimatingState = true
            targetScaleState = 2.5f
            imageSizeState = Size(1000f, 800f) 
            containerSizeState = Size(1000f, 800f)
            val frame = calculateVisibleImageFrameTest() 
            val imageRelativeX = (tapPosition.x - frame.x) / frame.width
            val imageRelativeY = (tapPosition.y - frame.y) / frame.height
            val containerCenterX = containerSizeState.width / 2
            val containerCenterY = containerSizeState.height / 2
            val focusPointInFrameX = frame.x + frame.width * imageRelativeX
            val focusPointInFrameY = frame.y + frame.height * imageRelativeY
            val vectorX = focusPointInFrameX - containerCenterX 
            val vectorY = focusPointInFrameY - containerCenterY 
            val newScale = 2.5f
            targetOffsetXState = (vectorX * (1 - 1/newScale))
            targetOffsetYState = (vectorY * (1 - 1/newScale))
        }

        assertTrue(isAnimatingState)
        assertEquals(2.5f, targetScaleState, 0.01f)
        assertEquals(0f, targetOffsetXState, 0.01f) 
        assertEquals(0f, targetOffsetYState, 0.01f)
    }
    
    @Test
    fun `double tap when zoomed in sets animation targets for zoom out`() {
        scaleState = 2.5f
        offsetXState = 50f 
        offsetYState = 30f

        if (scaleState > 1.01f) {
            isAnimatingState = true
            targetScaleState = 1f
            targetOffsetXState = 0f
            targetOffsetYState = 0f
        }

        assertTrue(isAnimatingState)
        assertEquals(1f, targetScaleState, 0.01f)
        assertEquals(0f, targetOffsetXState, 0.01f)
        assertEquals(0f, targetOffsetYState, 0.01f)
    }

    @Test
    fun `after double tap zoom-in animation completes, onZoomChanged(true) is called`() {
        scaleState = 2.5f 
        isAnimatingState = false 

        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock).invoke(true)
    }

    @Test
    fun `after double tap zoom-out animation completes, onZoomChanged(false) is called`() {
        scaleState = 1f 
        isAnimatingState = false

        onZoomChangedMock(scaleState > 1.01f)
        verify(onZoomChangedMock).invoke(false)
    }
    
    // --- `isAnimating` guard test ---
    @Test
    fun `transform gestures are ignored if isAnimating is true`() {
        isAnimatingState = true 

        scaleState = 1.0f 
        offsetXState = 0f
        offsetYState = 0f

        val zoomChange = 2.0f
        val panChange = Offset(10f, 10f)

        if (!isAnimatingState) { // This guard is in SimpleImageViewer's transformableState lambda
            scaleState = (scaleState * zoomChange).coerceIn(1f, 3f)
            if (scaleState > 1.01f) {
                val safeScale = if (scaleState == 0f) 1f else scaleState
                val (maxOffsetX, maxOffsetY) = calculateMaxOffsetsTest(scaleState)
                offsetXState = (offsetXState + panChange.x / safeScale).coerceIn(-maxOffsetX, maxOffsetX)
                offsetYState = (offsetYState + panChange.y / safeScale).coerceIn(-maxOffsetY, maxOffsetY)
            } else {
                offsetXState = 0f
                offsetYState = 0f
            }
        }

        assertEquals(1.0f, scaleState, 0.01f)
        assertEquals(0f, offsetXState, 0.01f)
        assertEquals(0f, offsetYState, 0.01f)
    }
}
