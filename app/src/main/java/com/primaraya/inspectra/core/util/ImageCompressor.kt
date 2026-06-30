package com.primaraya.inspectra.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.min

object ImageCompressor {

    private const val MAX_WIDTH = 1024f
    private const val MAX_HEIGHT = 1024f
    private const val COMPRESS_QUALITY = 80

    suspend fun compressImage(context: Context, uri: Uri): File? = withContext(Dispatchers.IO) {
        try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (originalBitmap == null) return@withContext null

            // Read EXIF to handle rotation
            var rotation = 0
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                rotation = when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }

            // Calculate scaling
            val width = originalBitmap.width
            val height = originalBitmap.height
            val scale = min(MAX_WIDTH / width, MAX_HEIGHT / height)

            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            if (scale < 1) {
                matrix.postScale(scale, scale)
            }

            val scaledBitmap = Bitmap.createBitmap(
                originalBitmap, 0, 0, width, height, matrix, true
            )

            val file = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, outputStream)
            outputStream.flush()
            outputStream.close()

            scaledBitmap.recycle()
            if (scaledBitmap != originalBitmap) {
                originalBitmap.recycle()
            }

            return@withContext file
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
