package com.wavehitech.aptracker

import android.graphics.Bitmap
import androidx.media3.common.util.Log

/**
 * Utility functions for bitmap processing during export
 */
object BitmapUtils {
    /**
     * Determines if a bitmap is in landscape orientation
     */
    fun isLandscape(bitmap: Bitmap): Boolean {
        return bitmap.width > bitmap.height
    }

    /**
     * Calculates the dimensions for a resized image based on target physical dimensions and PPI
     *
     * @param bitmap The source bitmap
     * @param ppi Pixels per inch to use for the calculation
     * @param targetWidthInches Target width in inches for landscape images
     * @param targetHeightInches Target height in inches for portrait images
     * @return Pair of (width, height) in pixels
     */
    fun calculateResizedDimensions(
        bitmap: Bitmap,
        ppi: Int,
        targetWidthInches: Float = 3.44f,
        targetHeightInches: Float = 2.09f
    ): Pair<Int, Int> {
        val isLandscape = isLandscape(bitmap)

        return if (isLandscape) {
            // For landscape images, set width to target width * PPI
            val targetWidth = (targetWidthInches * ppi).toInt()
            // Calculate height to maintain aspect ratio
            val targetHeight = (bitmap.height * targetWidth.toFloat() / bitmap.width).toInt()
            Pair(targetWidth, targetHeight)
        } else {
            // For portrait images, set height to target height * PPI
            val targetHeight = (targetHeightInches * ppi).toInt()
            // Calculate width to maintain aspect ratio
            val targetWidth = (bitmap.width * targetHeight.toFloat() / bitmap.height).toInt()
            Pair(targetWidth, targetHeight)
        }
    }

    /**
     * Creates a resized bitmap based on target physical dimensions and PPI
     *
     * @param sourceBitmap The source bitmap
     * @param ppi Pixels per inch to use for the calculation
     * @param targetWidthInches Target width in inches for landscape images
     * @param targetHeightInches Target height in inches for portrait images
     * @return Resized bitmap
     */
    fun createResizedBitmap(
        sourceBitmap: Bitmap,
        ppi: Int,
        targetWidthInches: Float = 3.44f,
        targetHeightInches: Float = 2.09f
    ): Bitmap {
        val (width, height) = calculateResizedDimensions(
            sourceBitmap,
            ppi,
            targetWidthInches,
            targetHeightInches
        )

        return Bitmap.createScaledBitmap(sourceBitmap, width, height, true)
    }

    /**
     * Logs information about bitmap resizing operation
     */
    fun logResizeOperation(
        filename: String,
        originalWidth: Int,
        originalHeight: Int,
        newWidth: Int,
        newHeight: Int,
        ppi: Int,
        isLandscape: Boolean
    ) {
        Log.d("BitmapResize", """
            Resizing image: $filename
            Orientation: ${if (isLandscape) "Landscape" else "Portrait"}
            Original dimensions: $originalWidth x $originalHeight px
            Resized dimensions: $newWidth x $newHeight px
            PPI: $ppi
            Physical size: ${newWidth / ppi.toFloat()} x ${newHeight / ppi.toFloat()} inches
        """.trimIndent())
    }
}