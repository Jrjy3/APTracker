package com.wavehitech.aptracker

import android.graphics.Bitmap
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BitmapUtilsTest {

    @Test
    fun `isLandscape returns true for landscape bitmap`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1920)
        `when`(mockBitmap.height).thenReturn(1080)

        val result = BitmapUtils.isLandscape(mockBitmap)

        assertTrue(result)
    }

    @Test
    fun `isLandscape returns false for portrait bitmap`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1080)
        `when`(mockBitmap.height).thenReturn(1920)

        val result = BitmapUtils.isLandscape(mockBitmap)

        assertFalse(result)
    }

    @Test
    fun `isLandscape returns false for square bitmap`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1080)
        `when`(mockBitmap.height).thenReturn(1080)

        val result = BitmapUtils.isLandscape(mockBitmap)

        assertFalse(result)
    }

    @Test
    fun `calculateResizedDimensions for landscape image uses width target`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(3840)
        `when`(mockBitmap.height).thenReturn(2160)

        val ppi = 300
        val targetWidthInches = 4.0f
        val targetHeightInches = 3.0f

        val (width, height) = BitmapUtils.calculateResizedDimensions(
            mockBitmap, ppi, targetWidthInches, targetHeightInches
        )

        val expectedWidth = (targetWidthInches * ppi).toInt() // 1200
        val expectedHeight = (2160 * expectedWidth.toFloat() / 3840).toInt() // 675

        assertEquals(expectedWidth, width)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun `calculateResizedDimensions for portrait image uses height target`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(2160)
        `when`(mockBitmap.height).thenReturn(3840)

        val ppi = 300
        val targetWidthInches = 4.0f
        val targetHeightInches = 6.0f

        val (width, height) = BitmapUtils.calculateResizedDimensions(
            mockBitmap, ppi, targetWidthInches, targetHeightInches
        )

        val expectedHeight = (targetHeightInches * ppi).toInt() // 1800
        val expectedWidth = (2160 * expectedHeight.toFloat() / 3840).toInt() // 1012

        assertEquals(expectedWidth, width)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun `calculateResizedDimensions with default values`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1920)
        `when`(mockBitmap.height).thenReturn(1080)

        val ppi = 200

        val (width, height) = BitmapUtils.calculateResizedDimensions(mockBitmap, ppi)

        val expectedWidth = (3.44f * ppi).toInt() // 688
        val expectedHeight = (1080 * expectedWidth.toFloat() / 1920).toInt() // 387

        assertEquals(expectedWidth, width)
        assertEquals(expectedHeight, height)
    }

    @Test
    fun `calculateResizedDimensions maintains aspect ratio for landscape`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(1600)
        `when`(mockBitmap.height).thenReturn(900)

        val ppi = 150
        val targetWidthInches = 5.0f

        val (width, height) = BitmapUtils.calculateResizedDimensions(
            mockBitmap, ppi, targetWidthInches, 3.0f
        )

        val originalAspectRatio = 1600.0f / 900.0f
        val newAspectRatio = width.toFloat() / height.toFloat()

        assertEquals(originalAspectRatio, newAspectRatio, 0.01f)
    }

    @Test
    fun `calculateResizedDimensions maintains aspect ratio for portrait`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(900)
        `when`(mockBitmap.height).thenReturn(1600)

        val ppi = 150
        val targetHeightInches = 8.0f

        val (width, height) = BitmapUtils.calculateResizedDimensions(
            mockBitmap, ppi, 5.0f, targetHeightInches
        )

        val originalAspectRatio = 900.0f / 1600.0f
        val newAspectRatio = width.toFloat() / height.toFloat()

        assertEquals(originalAspectRatio, newAspectRatio, 0.01f)
    }

    @Test
    fun `createResizedBitmap creates bitmap with correct dimensions`() {
        val mockBitmap = mock(Bitmap::class.java)
        `when`(mockBitmap.width).thenReturn(2000)
        `when`(mockBitmap.height).thenReturn(1500)

        val ppi = 100
        val targetWidthInches = 4.0f
        val targetHeightInches = 3.0f

        mockStatic(Bitmap::class.java).use { mockedBitmap ->
            val expectedWidth = (targetWidthInches * ppi).toInt()
            val expectedHeight = (1500 * expectedWidth.toFloat() / 2000).toInt()
            
            val resultBitmap = mock(Bitmap::class.java)
            mockedBitmap.`when`<Bitmap> { 
                Bitmap.createScaledBitmap(mockBitmap, expectedWidth, expectedHeight, true) 
            }.thenReturn(resultBitmap)

            val result = BitmapUtils.createResizedBitmap(mockBitmap, ppi, targetWidthInches, targetHeightInches)

            assertEquals(resultBitmap, result)
            mockedBitmap.verify { 
                Bitmap.createScaledBitmap(mockBitmap, expectedWidth, expectedHeight, true) 
            }
        }
    }

    @Test
    fun `logResizeOperation does not throw exception`() {
        // This test ensures the logging function executes without errors
        assertDoesNotThrow {
            BitmapUtils.logResizeOperation(
                filename = "test.jpg",
                originalWidth = 1920,
                originalHeight = 1080,
                newWidth = 960,
                newHeight = 540,
                ppi = 150,
                isLandscape = true
            )
        }
    }

    private fun assertDoesNotThrow(block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}