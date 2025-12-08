package com.example.myapplication.ui.idcard.util

import android.graphics.*
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object ImageUtils {

    @ExperimentalGetImage
    fun imageProxyToBitmap(imageProxy: ImageProxy, rotation: Int): Bitmap? {
        return try {
            val image = imageProxy.image ?: return null
            val width = image.width
            val height = image.height

            val yPlane = image.planes[0]
            val uPlane = image.planes[1]
            val vPlane = image.planes[2]

            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer

            val yRowStride = yPlane.rowStride
            val uvRowStride = uPlane.rowStride
            val uvPixelStride = uPlane.pixelStride

            val nv21 = ByteArray(width * height * 3 / 2)
            var pos = 0

            for (row in 0 until height) {
                yBuffer.position(row * yRowStride)
                yBuffer.get(nv21, pos, width)
                pos += width
            }

            val uvHeight = height / 2
            for (row in 0 until uvHeight) {
                for (col in 0 until width / 2) {
                    val vIndex = row * uvRowStride + col * uvPixelStride
                    val uIndex = row * uvRowStride + col * uvPixelStride

                    vBuffer.position(vIndex)
                    nv21[pos++] = vBuffer.get()

                    uBuffer.position(uIndex)
                    nv21[pos++] = uBuffer.get()
                }
            }

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, width, height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, width, height), 100, out)
            val bytes = out.toByteArray()
            out.close()

            var bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

            if (bitmap != null && rotation != 0) {
                val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                bitmap = rotated
            }

            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun cropArea(bitmap: Bitmap, boundingBox: RectF, padding: Int = 20): Bitmap? {
        return try {
            val left = max(0, (boundingBox.left - padding).toInt())
            val top = max(0, (boundingBox.top - padding).toInt())
            val right = min(bitmap.width, (boundingBox.right + padding).toInt())
            val bottom = min(bitmap.height, (boundingBox.bottom + padding).toInt())

            val width = right - left
            val height = bottom - top

            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, left, top, width, height)
            } else null
        } catch (e: Exception) {
            null
        }
    }

    fun saveToFile(bitmap: Bitmap?, directory: File): String? {
        if (bitmap == null) return null

        return try {
            if (!directory.exists()) directory.mkdirs()

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(directory, "id_card_$timestamp.jpg")

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }
}