package com.example.myapplication.ui.idcard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

class IdCardDetector(private val context: Context) {

    companion object {
        private const val TAG = "IdCardDetector"
        private const val MODEL_FILE = "best_float32.tflite"
        private const val INPUT_SIZE = 640
        private const val CONFIDENCE_THRESHOLD = 0.25f
        private const val IOU_THRESHOLD = 0.5f
    }

    private var interpreter: Interpreter? = null
    private var outputShape: IntArray? = null

    init {
        loadModel()
    }

    private fun loadModel() {
        try {
            val model = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }
            interpreter = Interpreter(model, options)
            outputShape = interpreter?.getOutputTensor(0)?.shape()
        } catch (e: Exception) {
            Log.e(TAG, "모델 로드 실패: ${e.message}", e)
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val assetFileDescriptor = context.assets.openFd(MODEL_FILE)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    data class LetterboxInfo(
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val originalWidth: Int,
        val originalHeight: Int
    )

    private fun letterboxResize(bitmap: Bitmap): Pair<Bitmap, LetterboxInfo> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height

        val scale = min(
            INPUT_SIZE.toFloat() / originalWidth,
            INPUT_SIZE.toFloat() / originalHeight
        )

        val newWidth = (originalWidth * scale).toInt()
        val newHeight = (originalHeight * scale).toInt()

        val padX = (INPUT_SIZE - newWidth) / 2f
        val padY = (INPUT_SIZE - newHeight) / 2f

        val resized = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        val letterboxed = Bitmap.createBitmap(INPUT_SIZE, INPUT_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(Color.rgb(114, 114, 114))
        canvas.drawBitmap(resized, padX, padY, null)

        resized.recycle()

        val info = LetterboxInfo(
            scale = scale,
            padX = padX,
            padY = padY,
            originalWidth = originalWidth,
            originalHeight = originalHeight
        )

        Log.d(TAG, "📐 Letterbox: ${originalWidth}x${originalHeight} → scale=${"%.3f".format(scale)}, pad=(${padX.toInt()}, ${padY.toInt()})")

        return Pair(letterboxed, info)
    }

    private fun letterboxToOriginal(box: RectF, info: LetterboxInfo): RectF {
        val x1 = (box.left - info.padX) / info.scale
        val y1 = (box.top - info.padY) / info.scale
        val x2 = (box.right - info.padX) / info.scale
        val y2 = (box.bottom - info.padY) / info.scale

        return RectF(
            x1.coerceIn(0f, info.originalWidth.toFloat()),
            y1.coerceIn(0f, info.originalHeight.toFloat()),
            x2.coerceIn(0f, info.originalWidth.toFloat()),
            y2.coerceIn(0f, info.originalHeight.toFloat())
        )
    }

    fun detect(bitmap: Bitmap): DetectionResult? {
        val interpreter = this.interpreter ?: return null
        val shape = outputShape ?: return null

        val (letterboxed, letterboxInfo) = letterboxResize(bitmap)

        val inputBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        inputBuffer.order(ByteOrder.nativeOrder())
        inputBuffer.rewind()

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        letterboxed.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        for (pixel in pixels) {
            inputBuffer.putFloat(((pixel shr 16) and 0xFF) / 255.0f)
            inputBuffer.putFloat(((pixel shr 8) and 0xFF) / 255.0f)
            inputBuffer.putFloat((pixel and 0xFF) / 255.0f)
        }

        letterboxed.recycle()

        val outputBuffer = ByteBuffer.allocateDirect(shape[0] * shape[1] * shape[2] * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        inputBuffer.rewind()
        outputBuffer.rewind()
        interpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        val output = FloatArray(shape[0] * shape[1] * shape[2])
        outputBuffer.asFloatBuffer().get(output)

        return postProcess(output, shape, letterboxInfo)
    }

    private fun postProcess(output: FloatArray, shape: IntArray, letterboxInfo: LetterboxInfo): DetectionResult? {
        val results = mutableListOf<DetectionResult>()

        val isTransposed = shape[1] > shape[2]
        val numDetections = if (isTransposed) shape[1] else shape[2]
        val numValues = if (isTransposed) shape[2] else shape[1]

        var maxConf = 0f
        var maxIdx = -1

        for (i in 0 until numDetections) {
            val xc: Float
            val yc: Float
            val w: Float
            val h: Float
            val confidence: Float

            if (isTransposed) {
                val baseIdx = i * numValues
                xc = output[baseIdx + 0]
                yc = output[baseIdx + 1]
                w = output[baseIdx + 2]
                h = output[baseIdx + 3]
                confidence = output[baseIdx + 4]
            } else {
                xc = output[0 * numDetections + i]
                yc = output[1 * numDetections + i]
                w = output[2 * numDetections + i]
                h = output[3 * numDetections + i]
                confidence = output[4 * numDetections + i]
            }

            if (confidence > maxConf) {
                maxConf = confidence
                maxIdx = i
            }

            if (confidence >= CONFIDENCE_THRESHOLD) {
                val left640 = (xc - w / 2) * INPUT_SIZE
                val top640 = (yc - h / 2) * INPUT_SIZE
                val right640 = (xc + w / 2) * INPUT_SIZE
                val bottom640 = (yc + h / 2) * INPUT_SIZE

                val box640 = RectF(left640, top640, right640, bottom640)

                val boxOriginal = letterboxToOriginal(box640, letterboxInfo)

                val boxWidth = boxOriginal.width()
                val boxHeight = boxOriginal.height()

                if (boxWidth > 50 && boxHeight > 30) {
                    results.add(DetectionResult(
                        boundingBox = boxOriginal,
                        confidence = confidence
                    ))
                }
            }
        }

        val nmsResults = applyNMS(results)
        val best = nmsResults.maxByOrNull { it.confidence }

        return best
    }

    private fun applyNMS(detections: List<DetectionResult>): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            selected.add(best)
            sorted.removeAll { calculateIoU(best.boundingBox, it.boundingBox) >= IOU_THRESHOLD }
        }

        return selected
    }

    private fun calculateIoU(box1: RectF, box2: RectF): Float {
        val intersectLeft = max(box1.left, box2.left)
        val intersectTop = max(box1.top, box2.top)
        val intersectRight = min(box1.right, box2.right)
        val intersectBottom = min(box1.bottom, box2.bottom)

        if (intersectLeft >= intersectRight || intersectTop >= intersectBottom) return 0f

        val intersectionArea = (intersectRight - intersectLeft) * (intersectBottom - intersectTop)
        val box1Area = box1.width() * box1.height()
        val box2Area = box2.width() * box2.height()

        return intersectionArea / (box1Area + box2Area - intersectionArea)
    }

    fun isLoaded(): Boolean = interpreter != null

    fun close() {
        interpreter?.close()
        interpreter = null
    }

    data class DetectionResult(
        val boundingBox: RectF,
        val confidence: Float
    )
}