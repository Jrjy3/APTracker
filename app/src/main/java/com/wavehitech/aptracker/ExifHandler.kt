package com.wavehitech.aptracker

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.util.Log
import android.graphics.Matrix
import java.io.File
import java.io.FileOutputStream

/**
 * Functions for handling EXIF data during image export
 */
object ExifHandler {

    /**
     * Gets the rotation degrees based on EXIF orientation
     *
     * @param orientation EXIF orientation tag value
     * @return Rotation in degrees (0, 90, 180, or 270)
     */
    private fun getRotationFromOrientation(orientation: Int): Float {
        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f // Default to no rotation
        }
    }

    /**
     * Loads a bitmap from file path with correct orientation based on EXIF data
     *
     * @param filePath Path to the image file
     * @return Correctly rotated bitmap or null if file can't be read
     */
    private fun loadRotatedBitmap(filePath: String): Bitmap? {
        return try {
            // Read EXIF orientation
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            // Load the bitmap
            val bitmap = BitmapFactory.decodeFile(filePath)

            // Return rotated bitmap if needed
            if (bitmap != null && orientation != ExifInterface.ORIENTATION_NORMAL) {
                val rotation = getRotationFromOrientation(orientation)
                if (rotation != 0f) {
                    // Create rotation matrix
                    val matrix = Matrix()
                    matrix.postRotate(rotation)

                    // Create rotated bitmap
                    val rotatedBitmap = Bitmap.createBitmap(
                        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                    )

                    // Recycle original to save memory
                    bitmap.recycle()

                    return rotatedBitmap
                }
            }

            bitmap // Return original if no rotation needed
        } catch (e: Exception) {
            Log.e("ExifHandler", "Error loading rotated bitmap: ${e.message}", e)
            null
        }
    }

    /**
     * Preserves relevant EXIF data when saving a modified bitmap
     *
     * @param sourceFilePath Original image file path
     * @param destinationFilePath Destination file path
     */
    fun preserveExifData(sourceFilePath: String, destinationFilePath: String) {
        try {
            val sourceExif = ExifInterface(sourceFilePath)
            val destExif = ExifInterface(destinationFilePath)

            // Copy important EXIF attributes
            val attributes = arrayOf(
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP
            )

            for (attr in attributes) {
                val value = sourceExif.getAttribute(attr)
                if (value != null) {
                    destExif.setAttribute(attr, value)
                }
            }

            // Set orientation to normal since we've already rotated the bitmap
            destExif.setAttribute(ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL.toString())

            // Save the updated EXIF data
            destExif.saveAttributes()

        } catch (e: Exception) {
            Log.e("ExifHandler", "Error preserving EXIF data: ${e.message}", e)
        }
    }

    /**
     * Process a bitmap (resize and rotate) with EXIF awareness
     *
     * @param sourceFile Source image file
     * @param destFile Destination file for processed image
     * @param ppi PPI to use for resizing
     * @param targetWidthInches Target width in inches for landscape
     * @param targetHeightInches Target height in inches for portrait
     * @return true if successful, false otherwise
     */
    fun processImageWithExif(
        sourceFile: File,
        destFile: File,
        ppi: Int,
        targetWidthInches: Float = 3.44f,
        targetHeightInches: Float = 2.09f,
        quality: Int = 90
    ): Boolean {
        try {
            // Load bitmap with correct rotation
            val bitmap = loadRotatedBitmap(sourceFile.absolutePath) ?: return false

            // Get dimensions for resizing
            val isLandscape = BitmapUtils.isLandscape(bitmap)
            val (newWidth, newHeight) = BitmapUtils.calculateResizedDimensions(
                bitmap, ppi, targetWidthInches, targetHeightInches
            )

            // Log the processing details
            Log.d("ExifHandler", """
                Processing image: ${sourceFile.name}
                Original dimensions: ${bitmap.width} x ${bitmap.height} px
                Orientation: ${if (isLandscape) "Landscape" else "Portrait"}
                Target dimensions: $newWidth x $newHeight px
                PPI: $ppi
                Physical size: ${newWidth / ppi.toFloat()} x ${newHeight / ppi.toFloat()} inches
            """.trimIndent())

            // Create resized bitmap
            val resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

            // Save the resized image
            FileOutputStream(destFile).use { out ->
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }

            // Copy EXIF data (except orientation since we've handled rotation)
            preserveExifData(sourceFile.absolutePath, destFile.absolutePath)

            // Clean up
            resizedBitmap.recycle()
            bitmap.recycle()

            return true

        } catch (e: Exception) {
            Log.e("ExifHandler", "Error processing image: ${e.message}", e)
            return false
        }
    }

    /**
     * Copies an image file with just orientation correction (no resizing)
     *
     * @param sourceFile Source image file
     * @param destFile Destination file
     * @return true if successful, false otherwise
     */
    fun copyWithOrientationCorrection(sourceFile: File, destFile: File): Boolean {
        try {
            // Check if orientation correction is needed
            val exif = ExifInterface(sourceFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            if (orientation == ExifInterface.ORIENTATION_NORMAL) {
                // No rotation needed, just copy the file
                sourceFile.copyTo(destFile, overwrite = true)
                return true
            }

            // Load and rotate the bitmap
            val bitmap = loadRotatedBitmap(sourceFile.absolutePath) ?: return false

            // Save the rotated image
            FileOutputStream(destFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }

            // Preserve EXIF data but set orientation to normal
            preserveExifData(sourceFile.absolutePath, destFile.absolutePath)

            // Clean up
            bitmap.recycle()

            return true

        } catch (e: Exception) {
            Log.e("ExifHandler", "Error copying with orientation correction: ${e.message}", e)
            return false
        }
    }
}