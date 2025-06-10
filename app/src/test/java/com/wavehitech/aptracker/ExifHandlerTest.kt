package com.wavehitech.aptracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
class ExifHandlerTest {

    @Mock
    private lateinit var mockSourceFile: File

    @Mock
    private lateinit var mockDestFile: File

    @Mock
    private lateinit var mockExifInterface: ExifInterface

    @Mock
    private lateinit var mockBitmap: Bitmap

    @Mock
    private lateinit var mockFileOutputStream: FileOutputStream

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `preserveExifData copies important EXIF attributes`() {
        val sourcePath = "/source/image.jpg"
        val destPath = "/dest/image.jpg"

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            val sourceExif = mock(ExifInterface::class.java)
            val destExif = mock(ExifInterface::class.java)

            mockedExif.`when`<ExifInterface> { ExifInterface(sourcePath) }.thenReturn(sourceExif)
            mockedExif.`when`<ExifInterface> { ExifInterface(destPath) }.thenReturn(destExif)

            // Mock some EXIF attributes
            `when`(sourceExif.getAttribute(ExifInterface.TAG_DATETIME)).thenReturn("2024:01:15 10:30:00")
            `when`(sourceExif.getAttribute(ExifInterface.TAG_MAKE)).thenReturn("Canon")
            `when`(sourceExif.getAttribute(ExifInterface.TAG_MODEL)).thenReturn("EOS R5")
            `when`(sourceExif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)).thenReturn("40.7128")

            ExifHandler.preserveExifData(sourcePath, destPath)

            verify(destExif).setAttribute(ExifInterface.TAG_DATETIME, "2024:01:15 10:30:00")
            verify(destExif).setAttribute(ExifInterface.TAG_MAKE, "Canon")
            verify(destExif).setAttribute(ExifInterface.TAG_MODEL, "EOS R5")
            verify(destExif).setAttribute(ExifInterface.TAG_GPS_LATITUDE, "40.7128")
            verify(destExif).setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            verify(destExif).saveAttributes()
        }
    }

    @Test
    fun `preserveExifData handles null attributes gracefully`() {
        val sourcePath = "/source/image.jpg"
        val destPath = "/dest/image.jpg"

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            val sourceExif = mock(ExifInterface::class.java)
            val destExif = mock(ExifInterface::class.java)

            mockedExif.`when`<ExifInterface> { ExifInterface(sourcePath) }.thenReturn(sourceExif)
            mockedExif.`when`<ExifInterface> { ExifInterface(destPath) }.thenReturn(destExif)

            // Return null for all attributes
            `when`(sourceExif.getAttribute(any())).thenReturn(null)

            ExifHandler.preserveExifData(sourcePath, destPath)

            // Verify only orientation is set (to NORMAL)
            verify(destExif).setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            verify(destExif).saveAttributes()
            // Verify null attributes are not set
            verify(destExif, never()).setAttribute(eq(ExifInterface.TAG_DATETIME), any())
        }
    }

    @Test
    fun `preserveExifData handles exceptions gracefully`() {
        val sourcePath = "/source/image.jpg"
        val destPath = "/dest/image.jpg"

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            mockedExif.`when`<ExifInterface> { ExifInterface(sourcePath) }
                .thenThrow(RuntimeException("File not found"))

            // Should not throw exception
            assertDoesNotThrow {
                ExifHandler.preserveExifData(sourcePath, destPath)
            }
        }
    }

    @Test
    fun `processImageWithExif returns true for successful processing`() {
        `when`(mockSourceFile.absolutePath).thenReturn("/source/image.jpg")
        `when`(mockDestFile.absolutePath).thenReturn("/dest/image.jpg")
        `when`(mockSourceFile.name).thenReturn("image.jpg")

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
                mockStatic(BitmapUtils::class.java).use { mockedBitmapUtils ->
                    mockStatic(FileOutputStream::class.java).use { mockedFileOutputStream ->
                        val sourceExif = mock(ExifInterface::class.java)
                        val destExif = mock(ExifInterface::class.java)

                        mockedExif.`when`<ExifInterface> { ExifInterface("/source/image.jpg") }.thenReturn(sourceExif)
                        mockedExif.`when`<ExifInterface> { ExifInterface("/dest/image.jpg") }.thenReturn(destExif)

                        `when`(sourceExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                            .thenReturn(ExifInterface.ORIENTATION_NORMAL)

                        mockedBitmapFactory.`when`<Bitmap> { BitmapFactory.decodeFile("/source/image.jpg") }
                            .thenReturn(mockBitmap)

                        `when`(mockBitmap.width).thenReturn(1920)
                        `when`(mockBitmap.height).thenReturn(1080)

                        mockedBitmapUtils.`when`<Boolean> { BitmapUtils.isLandscape(mockBitmap) }.thenReturn(true)
                        mockedBitmapUtils.`when`<Pair<Int, Int>> {
                            BitmapUtils.calculateResizedDimensions(mockBitmap, 150, 3.44f, 2.09f)
                        }.thenReturn(Pair(516, 290))

                        val resizedBitmap = mock(Bitmap::class.java)
                        mockStatic(Bitmap::class.java).use { mockedBitmap ->
                            mockedBitmap.`when`<Bitmap> { Bitmap.createScaledBitmap(mockBitmap, 516, 290, true) }
                                .thenReturn(resizedBitmap)

                            mockedFileOutputStream.`when`<FileOutputStream> { FileOutputStream(mockDestFile) }
                                .thenReturn(mockFileOutputStream)

                            `when`(resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, mockFileOutputStream))
                                .thenReturn(true)

                            val result = ExifHandler.processImageWithExif(mockSourceFile, mockDestFile, 150)

                            assertTrue(result)
                            verify(resizedBitmap).recycle()
                            verify(mockBitmap).recycle()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `processImageWithExif returns false when bitmap loading fails`() {
        `when`(mockSourceFile.absolutePath).thenReturn("/source/image.jpg")

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
                val sourceExif = mock(ExifInterface::class.java)
                mockedExif.`when`<ExifInterface> { ExifInterface("/source/image.jpg") }.thenReturn(sourceExif)

                `when`(sourceExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                    .thenReturn(ExifInterface.ORIENTATION_NORMAL)

                // Return null bitmap to simulate loading failure
                mockedBitmapFactory.`when`<Bitmap> { BitmapFactory.decodeFile("/source/image.jpg") }
                    .thenReturn(null)

                val result = ExifHandler.processImageWithExif(mockSourceFile, mockDestFile, 150)

                assertFalse(result)
            }
        }
    }

    @Test
    fun `copyWithOrientationCorrection handles normal orientation by copying file`() {
        `when`(mockSourceFile.absolutePath).thenReturn("/source/image.jpg")
        `when`(mockSourceFile.copyTo(mockDestFile, true)).thenReturn(mockDestFile)

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            val sourceExif = mock(ExifInterface::class.java)
            mockedExif.`when`<ExifInterface> { ExifInterface("/source/image.jpg") }.thenReturn(sourceExif)

            `when`(sourceExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                .thenReturn(ExifInterface.ORIENTATION_NORMAL)

            val result = ExifHandler.copyWithOrientationCorrection(mockSourceFile, mockDestFile)

            assertTrue(result)
            verify(mockSourceFile).copyTo(mockDestFile, true)
        }
    }

    @Test
    fun `copyWithOrientationCorrection processes rotated images`() {
        `when`(mockSourceFile.absolutePath).thenReturn("/source/image.jpg")
        `when`(mockDestFile.absolutePath).thenReturn("/dest/image.jpg")

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            mockStatic(BitmapFactory::class.java).use { mockedBitmapFactory ->
                mockStatic(FileOutputStream::class.java).use { mockedFileOutputStream ->
                    val sourceExif = mock(ExifInterface::class.java)
                    val destExif = mock(ExifInterface::class.java)

                    mockedExif.`when`<ExifInterface> { ExifInterface("/source/image.jpg") }.thenReturn(sourceExif)
                    mockedExif.`when`<ExifInterface> { ExifInterface("/dest/image.jpg") }.thenReturn(destExif)

                    `when`(sourceExif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL))
                        .thenReturn(ExifInterface.ORIENTATION_ROTATE_90)

                    mockedBitmapFactory.`when`<Bitmap> { BitmapFactory.decodeFile("/source/image.jpg") }
                        .thenReturn(mockBitmap)

                    `when`(mockBitmap.width).thenReturn(1920)
                    `when`(mockBitmap.height).thenReturn(1080)

                    val rotatedBitmap = mock(Bitmap::class.java)
                    mockStatic(Bitmap::class.java).use { mockedBitmap ->
                        val matrix = mock(Matrix::class.java)
                        mockStatic(Matrix::class.java).use { mockedMatrix ->
                            mockedMatrix.`when`<Matrix> { Matrix() }.thenReturn(matrix)

                            mockedBitmap.`when`<Bitmap> {
                                Bitmap.createBitmap(mockBitmap, 0, 0, 1920, 1080, matrix, true)
                            }.thenReturn(rotatedBitmap)

                            mockedFileOutputStream.`when`<FileOutputStream> { FileOutputStream(mockDestFile) }
                                .thenReturn(mockFileOutputStream)

                            `when`(rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, mockFileOutputStream))
                                .thenReturn(true)

                            val result = ExifHandler.copyWithOrientationCorrection(mockSourceFile, mockDestFile)

                            assertTrue(result)
                            verify(matrix).postRotate(90f)
                            verify(mockBitmap).recycle()
                            verify(rotatedBitmap).recycle()
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `copyWithOrientationCorrection returns false on exception`() {
        `when`(mockSourceFile.absolutePath).thenReturn("/source/image.jpg")

        mockStatic(ExifInterface::class.java).use { mockedExif ->
            mockedExif.`when`<ExifInterface> { ExifInterface("/source/image.jpg") }
                .thenThrow(RuntimeException("File error"))

            val result = ExifHandler.copyWithOrientationCorrection(mockSourceFile, mockDestFile)

            assertFalse(result)
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