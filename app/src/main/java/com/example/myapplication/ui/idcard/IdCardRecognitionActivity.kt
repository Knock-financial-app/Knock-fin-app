package com.example.myapplication.ui.idcard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import android.graphics.YuvImage
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.main.MainActivity
import com.example.myapplication.view.OverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class IdCardRecognitionActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var textRecognizer: TextRecognizer
    private lateinit var cancelBtn: ImageView
    private var idCardDetector: IdCardDetector? = null
    private var isProcessing = false
    private var recognitionCompleted = false
    private var validFrameCount = 0
    private var vibrator: Vibrator? = null
    private var currentHapticLevel = 0
    private var frameCount = 0
    private var lastLogTime = 0L
    private var lastDetectedRect: RectF? = null
    private var lastValidBitmap: Bitmap? = null
    private var analysisRotation = 0
    private var lastAnnouncedMessage: String = ""
    private var lastAnnounceTime: Long = 0L
    private val ANNOUNCE_INTERVAL = 2000L
    private var smoothedCardRect: RectF? = null

    companion object {
        private const val TAG = "IdCardRecog"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val REQUIRED_VALID_FRAMES = 3
        private const val GUIDE_MATCH_THRESHOLD = 0.85f
        private const val STABILITY_THRESHOLD = 0.08f
        private const val CARD_ASPECT_RATIO = 1.585f
        private const val SMOOTHING_FACTOR = 0.3f
        private const val ID_TYPE_UNKNOWN = 0
        private const val ID_TYPE_RESIDENT = 1
        private const val ID_TYPE_DRIVER = 2
        private var confirmedIdType: Int = ID_TYPE_UNKNOWN
        private var idTypeHistory = mutableListOf<Int>()
        private const val ID_TYPE_CONFIRM_THRESHOLD = 3
        private val DRIVER_LICENSE_NUMBER_PATTERN = Regex("(\\d[\\s]*){2}[\\s-]*(\\d[\\s]*){2}[\\s-]*(\\d[\\s]*){6}[\\s-]*(\\d[\\s]*){2}")
        private val RESIDENT_NUMBER_PATTERN = Regex("(\\d[\\s]*){6}[\\s-]*[1-4](\\d[\\s]*){6}")
        private val NAME_PATTERN = Regex("[가-힣]{2,4}")

        private val RESIDENT_KEYWORDS = listOf("주민등록증", "RESIDENT", "REGISTRATION", "주민번호")
        private val DRIVER_KEYWORDS = listOf("운전면허증", "운전면허", "DRIVER", "LICENSE", "면허번호")
        private val EXCLUDE_NAME_WORDS = listOf(
            "주민등록증", "주민", "면허", "주민등록", "주민번호", "등록증",
            "운전면허", "면허증", "자동차", "대한민국", "경찰청장", "도지사",
            "시장", "군수", "구청장", "발급일", "생년월일", "주소지", "적성검사",
            "갱신기간", "면허번호", "조건", "종류", "보통", "원동기", "대형",
            "성명", "이름", "주소", "발행", "유효기간", "경찰청"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_recognition)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        cancelBtn = findViewById(R.id.cancel_button)

        cancelBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
        initVibrator()
        initDetector()

        textRecognizer = TextRecognition.getClient(
            KoreanTextRecognizerOptions.Builder().build()
        )

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initVibrator() {
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (e: Exception) { null }
    }

    private fun initDetector() {
        try {
            idCardDetector = IdCardDetector(this)
        } catch (e: Exception) {
            Toast.makeText(this, "모델 로드 실패", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startCamera() {
        Toast.makeText(this, "촬영을 시작합니다.", Toast.LENGTH_SHORT).show()
        resetProcessing()

        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processFrame(imageProxy)
                    }
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "카메라 실패", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processFrame(imageProxy: ImageProxy) {
        frameCount++

        if (recognitionCompleted || isProcessing) {
            imageProxy.close()
            return
        }

        val detector = idCardDetector
        if (detector == null || !detector.isLoaded()) {
            imageProxy.close()
            return
        }

        analysisRotation = imageProxy.imageInfo.rotationDegrees

        val currentTime = System.currentTimeMillis()
        val shouldLog = currentTime - lastLogTime > 1000
        if (shouldLog) {
            lastLogTime = currentTime
        }

        val bitmap = imageProxyToBitmap(imageProxy)
        if (bitmap == null) {
            imageProxy.close()
            return
        }

        val detection = detector.detect(bitmap)

        if (detection == null) {
            handleInvalidFrame("신분증을 비춰주세요")
            bitmap.recycle()
            imageProxy.close()
            return
        }

        val guideRect = getGuideRectInBitmapCoords(bitmap.width, bitmap.height)
        val smoothedRect = smoothRect(detection.boundingBox)

        val hapticResult = calculateHapticScore(smoothedRect, guideRect)
        val matchResult = calculateGuideMatch(smoothedRect, guideRect)

        val isInsideGuide = matchResult.insideRatio >= GUIDE_MATCH_THRESHOLD
        val isStable = isStablePosition(detection.boundingBox)
        val isValidFrame = isInsideGuide && matchResult.fillRatio >= 0.80f && isStable

        triggerHapticFeedback(true, hapticResult)

        val fillPercent = (matchResult.fillRatio * 100).toInt()
        val confPercent = (detection.confidence * 100).toInt()

        val statusMessage = when {
            isValidFrame -> "ⓘ 신분증이 중심에 들어왔습니다.\n촬영 중이니 움직이지 마십시오."
            else -> analyzePosition(smoothedRect, guideRect)
        }

        val guideColor = android.graphics.Color.parseColor("#FFE621")

        runOnUiThread {
            val shouldAnnounce = statusMessage != overlayView.checkMessage()
            overlayView.setStatusMessage(statusMessage)
            if (shouldAnnounce && (isValidFrame || statusMessage.contains("이동하세요"))) {
                announceForAccessibility(statusMessage)
            }
            overlayView.setGuideColor(guideColor)
            overlayView.setDebugText("[YOLO] 신뢰도:${confPercent}% 채움:${fillPercent}% 햅틱:${(hapticResult * 100).toInt()}%")

            val intRect = Rect(
                smoothedRect.left.toInt(),
                smoothedRect.top.toInt(),
                smoothedRect.right.toInt(),
                smoothedRect.bottom.toInt()
            )
            overlayView.setDetectedRect(intRect, bitmap.width, bitmap.height, 0)
        }

        if (isValidFrame) {
            validFrameCount++
            lastDetectedRect = RectF(detection.boundingBox)

            if (validFrameCount == REQUIRED_VALID_FRAMES - 1) {
                lastValidBitmap?.recycle()
                lastValidBitmap = cropDetectedArea(bitmap, detection.boundingBox)
            }

            if (validFrameCount >= REQUIRED_VALID_FRAMES) {
                isProcessing = true

                if (lastValidBitmap == null) {
                    lastValidBitmap = cropDetectedArea(bitmap, detection.boundingBox)
                }

                Log.d(TAG, "★★★ 신분증 탐지 완료! OCR 시작 ★★★")
                performOcrAndSave(lastValidBitmap)
            }
        } else {
            if (validFrameCount > 0) {
                validFrameCount = maxOf(0, validFrameCount - 1)
            }
            lastDetectedRect = RectF(detection.boundingBox)
        }

        bitmap.recycle()
        imageProxy.close()
    }

    private fun calculateHapticScore(card: RectF, guide: RectF): Float {
        if (card.isEmpty) return 0f

        val intersection = RectF()
        if (!intersection.setIntersect(card, guide)) {
            return 0f
        }

        val intersectionArea = intersection.width() * intersection.height()
        val cardArea = card.width() * card.height()
        val guideArea = guide.width() * guide.height()

        if (guideArea == 0f || cardArea == 0f) return 0f

        val insideRatio = intersectionArea / cardArea
        val fillRatio = intersectionArea / guideArea
        val sizeRatio = cardArea / guideArea
        val sizeScore = when {
            sizeRatio < 0.3f -> 0.1f
            sizeRatio < 0.5f -> 0.3f + (sizeRatio - 0.3f) * 1.5f
            sizeRatio < 0.7f -> 0.6f + (sizeRatio - 0.5f) * 2f
            sizeRatio <= 1.1f -> 1.0f
            sizeRatio <= 1.3f -> 1.0f - (sizeRatio - 1.1f) * 2f
            sizeRatio <= 1.5f -> 0.6f - (sizeRatio - 1.3f) * 1.5f
            else -> 0.2f
        }

        val offsetX = kotlin.math.abs(card.centerX() - guide.centerX()) / guide.width()
        val offsetY = kotlin.math.abs(card.centerY() - guide.centerY()) / guide.height()
        val centerScore = (1f - (offsetX + offsetY).coerceAtMost(1f))

        val score = (insideRatio * 0.3f + fillRatio * 0.3f + centerScore * 0.2f + sizeScore * 0.2f)

        return score.coerceIn(0f, 1f)
    }

    private fun triggerHapticFeedback(isIdCardDetected: Boolean, hapticScore: Float) {
        if (!isIdCardDetected) {
            if (currentHapticLevel != 0) {
                currentHapticLevel = 0
                vibrator?.cancel()
            }
            return
        }

        val newHapticLevel = when {
            hapticScore >= 0.90f -> 5
            hapticScore >= 0.75f -> 4
            hapticScore >= 0.60f -> 3
            hapticScore >= 0.40f -> 2
            hapticScore >= 0.20f -> 1
            else -> 0
        }

        if (newHapticLevel != currentHapticLevel) {
            currentHapticLevel = newHapticLevel
            vibrator?.cancel()

            if (newHapticLevel > 0) {
                startVibrationPattern(newHapticLevel)
            }
        }
    }

    private fun startVibrationPattern(level: Int) {
        vibrator?.let { vib ->
            if (!vib.hasVibrator()) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                when (level) {
                    5 -> {
                        vib.vibrate(VibrationEffect.createOneShot(10000L, 200))
                    }
                    4 -> {
                        val pattern = longArrayOf(0, 50, 60, 50, 60, 50, 60)
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    }
                    3 -> {
                        val pattern = longArrayOf(0, 50, 120, 50, 120, 50, 120)
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    }
                    2 -> {
                        val pattern = longArrayOf(0, 50, 250, 50, 250, 50, 250)
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    }
                    1 -> {
                        val pattern = longArrayOf(0, 50, 500, 50, 500, 50, 500)
                        vib.vibrate(VibrationEffect.createWaveform(pattern, 0))
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val interval = when (level) {
                    5 -> longArrayOf(0, 10000)
                    4 -> longArrayOf(0, 50, 60)
                    3 -> longArrayOf(0, 50, 120)
                    2 -> longArrayOf(0, 50, 250)
                    else -> longArrayOf(0, 50, 500)
                }
                vib.vibrate(interval, if (level == 5) -1 else 0)
            }
        }
    }

    private fun cropDetectedArea(bitmap: Bitmap, boundingBox: RectF): Bitmap? {
        return try {
            val padding = 20

            val left = max(0, (boundingBox.left - padding).toInt())
            val top = max(0, (boundingBox.top - padding).toInt())
            val right = min(bitmap.width, (boundingBox.right + padding).toInt())
            val bottom = min(bitmap.height, (boundingBox.bottom + padding).toInt())

            val width = right - left
            val height = bottom - top

            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, left, top, width, height)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "크롭 실패: ${e.message}", e)
            null
        }
    }

    private fun preprocessForOcr(bitmap: Bitmap): Bitmap {
        val gray = toGrayscale(bitmap)
        val contrast = adjustContrast(gray, 1.5f)
        val bright = adjustBrightness(contrast, 10f)
        val sharp = sharpen(bright)

        gray.recycle()
        contrast.recycle()
        bright.recycle()

        return sharp
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix().apply {
            setSaturation(0f)
        }

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint()

        val translate = (-.5f * contrast + .5f) * 255f
        val colorMatrix = ColorMatrix(floatArrayOf(
            contrast, 0f, 0f, 0f, translate,
            0f, contrast, 0f, 0f, translate,
            0f, 0f, contrast, 0f, translate,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint()

        val colorMatrix = ColorMatrix(floatArrayOf(
            1f, 0f, 0f, 0f, brightness,
            0f, 1f, 0f, 0f, brightness,
            0f, 0f, 1f, 0f, brightness,
            0f, 0f, 0f, 1f, 0f
        ))

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }

    private fun sharpen(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val pixels = IntArray(width * height)
        val resultPixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val kernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                var r = 0f; var g = 0f; var b = 0f

                for (ky in -1..1) {
                    for (kx in -1..1) {
                        val pixel = pixels[(y + ky) * width + (x + kx)]
                        val k = kernel[(ky + 1) * 3 + (kx + 1)]

                        r += ((pixel shr 16) and 0xFF) * k
                        g += ((pixel shr 8) and 0xFF) * k
                        b += (pixel and 0xFF) * k
                    }
                }

                resultPixels[y * width + x] = (0xFF shl 24) or
                        (r.toInt().coerceIn(0, 255) shl 16) or
                        (g.toInt().coerceIn(0, 255) shl 8) or
                        b.toInt().coerceIn(0, 255)
            }
        }

        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }

    private fun performOcrAndSave(croppedBitmap: Bitmap?) {
        if (croppedBitmap == null) {
            runOnUiThread {
                Toast.makeText(this, "이미지 크롭 실패", Toast.LENGTH_SHORT).show()
            }
            resetProcessing()
            return
        }

        val processedBitmap = preprocessForOcr(croppedBitmap)
        //TODO inputImage -> croppedBitmap으로 바꾸기
        val inputImage = InputImage.fromBitmap(processedBitmap, 0)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val fullText = visionText.text
                Log.d(TAG, "OCR 결과:\n$fullText")

                lifecycleScope.launch {
                    processAndSaveIdCard(fullText, processedBitmap)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR 실패: ${e.message}", e)
                runOnUiThread {
                    Toast.makeText(this, "텍스트 인식 실패", Toast.LENGTH_SHORT).show()
                }
                resetProcessing()
            }
    }

    private fun announceForAccessibility(message: String, force: Boolean = false) {
        if (message.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        if (!force && message == lastAnnouncedMessage &&
            currentTime - lastAnnounceTime < ANNOUNCE_INTERVAL) {
            return
        }

        val accessibilityManager = getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

        if (accessibilityManager?.isEnabled == true) {
            val event = AccessibilityEvent.obtain().apply {
                eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                className = javaClass.name
                packageName = packageName
                text.add(message)
            }
            accessibilityManager.sendAccessibilityEvent(event)

            lastAnnouncedMessage = message
            lastAnnounceTime = currentTime
        }
    }

    private fun smoothRect(newRect: RectF): RectF {
        val prevRect = smoothedCardRect

        if (prevRect == null || prevRect.isEmpty) {
            smoothedCardRect = RectF(newRect)
            return newRect
        }

        val centerDiff = kotlin.math.abs(newRect.centerX() - prevRect.centerX()) +
                kotlin.math.abs(newRect.centerY() - prevRect.centerY())
        val avgSize = (prevRect.width() + prevRect.height()) / 2f

        if (avgSize > 0 && centerDiff / avgSize > 0.3f) {
            smoothedCardRect = RectF(newRect)
            return newRect
        }

        val result = RectF(
            lerp(prevRect.left, newRect.left, SMOOTHING_FACTOR),
            lerp(prevRect.top, newRect.top, SMOOTHING_FACTOR),
            lerp(prevRect.right, newRect.right, SMOOTHING_FACTOR),
            lerp(prevRect.bottom, newRect.bottom, SMOOTHING_FACTOR)
        )
        smoothedCardRect = result
        return result
    }

    private fun lerp(start: Float, end: Float, factor: Float): Float {
        return start + (end - start) * factor
    }

    private fun calculateGuideRect(imageWidth: Int, imageHeight: Int): RectF {
        val guideWidth = imageWidth * 0.85f
        val guideHeight = guideWidth / CARD_ASPECT_RATIO
        val left = (imageWidth - guideWidth) / 2
        val top = (imageHeight - guideHeight) / 2
        return RectF(left, top, left + guideWidth, top + guideHeight)
    }

    private fun getGuideRectInBitmapCoords(bitmapWidth: Int, bitmapHeight: Int): RectF {
        val viewGuide = overlayView.getGuideRect()
        val viewWidth = overlayView.width.toFloat()
        val viewHeight = overlayView.height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) {
            return calculateGuideRect(bitmapWidth, bitmapHeight)
        }

        val viewAspect = viewWidth / viewHeight
        val bitmapAspect = bitmapWidth.toFloat() / bitmapHeight

        val scale: Float
        val offsetX: Float
        val offsetY: Float

        if (bitmapAspect > viewAspect) {
            scale = viewHeight / bitmapHeight
            offsetX = (bitmapWidth * scale - viewWidth) / 2f
            offsetY = 0f
        } else {
            scale = viewWidth / bitmapWidth
            offsetX = 0f
            offsetY = (bitmapHeight * scale - viewHeight) / 2f
        }

        val left = (viewGuide.left + offsetX) / scale
        val top = (viewGuide.top + offsetY) / scale
        val right = (viewGuide.right + offsetX) / scale
        val bottom = (viewGuide.bottom + offsetY) / scale

        return RectF(
            left.coerceIn(0f, bitmapWidth.toFloat()),
            top.coerceIn(0f, bitmapHeight.toFloat()),
            right.coerceIn(0f, bitmapWidth.toFloat()),
            bottom.coerceIn(0f, bitmapHeight.toFloat())
        )
    }

    data class GuideMatchResult(val insideRatio: Float, val fillRatio: Float)

    private fun calculateGuideMatch(card: RectF, guide: RectF): GuideMatchResult {
        if (card.isEmpty) return GuideMatchResult(0f, 0f)

        val intersection = RectF()
        if (!intersection.setIntersect(card, guide)) {
            return GuideMatchResult(0f, 0f)
        }

        val intersectionArea = intersection.width() * intersection.height()
        val cardArea = card.width() * card.height()
        val guideArea = guide.width() * guide.height()

        return GuideMatchResult(
            if (cardArea > 0) intersectionArea / cardArea else 0f,
            if (guideArea > 0) intersectionArea / guideArea else 0f
        )
    }

    private fun isStablePosition(currentRect: RectF): Boolean {
        val lastRect = lastDetectedRect ?: return true
        if (lastRect.isEmpty || currentRect.isEmpty) return true

        val avgSize = (lastRect.width() + lastRect.height()) / 2f
        if (avgSize == 0f) return true

        val centerXDiff = kotlin.math.abs(currentRect.centerX() - lastRect.centerX())
        val centerYDiff = kotlin.math.abs(currentRect.centerY() - lastRect.centerY())

        return (centerXDiff + centerYDiff) / avgSize < STABILITY_THRESHOLD
    }

    private fun analyzePosition(cardRect: RectF, guideRect: RectF): String {
        if (cardRect.isEmpty) return "신분증을 비춰주세요"

        val offsetX = (cardRect.centerX() - guideRect.centerX()) / guideRect.width()
        val offsetY = (cardRect.centerY() - guideRect.centerY()) / guideRect.height()
        val sizeRatio = (cardRect.width() * cardRect.height()) / (guideRect.width() * guideRect.height())

        val messages = mutableListOf<String>()
        val OFFSET_THRESHOLD = 0.10f

        when {
            offsetX < -OFFSET_THRESHOLD -> messages.add("왼쪽으로")
            offsetX > OFFSET_THRESHOLD -> messages.add("오른쪽으로")
        }

        when {
            offsetY < -OFFSET_THRESHOLD -> messages.add("위로")
            offsetY > OFFSET_THRESHOLD -> messages.add("아래로")
        }

        when {
            sizeRatio < 0.7f -> messages.add("더 가까이")
            sizeRatio > 1.2f -> messages.add("더 멀리")
        }

        return if (messages.isNotEmpty()) {
            "휴대폰을 " + messages.joinToString(", ") + " 이동하세요"
        } else {
            "잘하고 있어요!"
        }
    }

    private fun handleInvalidFrame(message: String) {
        if (validFrameCount > 0) validFrameCount--

        smoothedCardRect = null
        stopHapticFeedback()

        runOnUiThread {
            overlayView.setStatusMessage(message)
            overlayView.clearDetection()
            overlayView.setGuideColor(android.graphics.Color.WHITE)
        }
    }

    private fun stopHapticFeedback() {
        currentHapticLevel = 0
        vibrator?.cancel()
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
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

            if (bitmap != null && analysisRotation != 0) {
                val matrix = Matrix().apply { postRotate(analysisRotation.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) bitmap.recycle()
                bitmap = rotated
            }

            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "비트맵 변환 실패: ${e.message}", e)
            null
        }
    }

    private suspend fun processAndSaveIdCard(fullText: String, bitmap: Bitmap?) {
        runOnUiThread {
            overlayView.setStatusMessage("저장 중...")
        }

        val imagePath = withContext(Dispatchers.IO) { saveImage(bitmap) }
        val idCardInfo = extractIdCardInfo(fullText)

        if (imagePath != null) {
            idCardInfo.imagePath = imagePath
        }

        Log.d(TAG, "저장 완료: $imagePath")
        Log.d(TAG, "추출 정보: $idCardInfo")

        if (idCardInfo.isValid()) {
            recognitionCompleted = true
            navigateToResult(idCardInfo)
        } else {
            runOnUiThread {
                Toast.makeText(this@IdCardRecognitionActivity, "정보 추출 실패. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
            resetProcessing()
        }
    }

    private fun saveImage(bitmap: Bitmap?): String? {
        if (bitmap == null) return null

        return try {
            val dir = File(filesDir, "id_cards").apply { if (!exists()) mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val file = File(dir, "id_card_$timestamp.jpg")

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos)
            }

            Log.d(TAG, "이미지 저장: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "저장 실패: ${e.message}")
            null
        }
    }

    private fun extractIdCardInfo(text: String): IdCardInfo {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }

        val idType = detectIdCardType(text)

        val residentMatch = RESIDENT_NUMBER_PATTERN.find(text)
        val residentNum = residentMatch?.value?.replace(Regex("[^0-9]"), "") ?: ""

        val driverLicenseNum = extractDriverLicenseNumber(text)

        return IdCardInfo.current.apply {
            this.driverLicenseNumber = driverLicenseNum
            this.residentNumber = residentNum
            this.issueDate = extractIssueDate(text)
            this.address = Regex("([가-힣]+(?:시|도))[\\s]*[가-힣]+").find(text)?.value ?: ""
            this.idType = when (idType) {
                ID_TYPE_RESIDENT -> "resident"
                ID_TYPE_DRIVER -> "driver"
                else -> "resident"
            }
            this.name = extractName(lines, text)
        }
    }

    private fun detectIdCardType(text: String): Int {
        val driverScore = DRIVER_KEYWORDS.count { text.contains(it, ignoreCase = true) }
        val residentScore = RESIDENT_KEYWORDS.count { text.contains(it, ignoreCase = true) }
        val hasDriverLicenseNumber = DRIVER_LICENSE_NUMBER_PATTERN.containsMatchIn(text)

        return when {
            hasDriverLicenseNumber -> ID_TYPE_DRIVER
            driverScore >= 2 -> ID_TYPE_DRIVER
            residentScore >= 1 -> ID_TYPE_RESIDENT
            driverScore >= 1 -> ID_TYPE_DRIVER
            else -> ID_TYPE_UNKNOWN
        }
    }

    private fun extractDriverLicenseNumber(text: String): String {
        val match = DRIVER_LICENSE_NUMBER_PATTERN.find(text)
        return match?.value?.replace(Regex("[^0-9]"), "")?.takeIf { it.length == 12 } ?: ""
    }

    private fun extractName(lines: List<String>, fullText: String): String {
        for (match in NAME_PATTERN.findAll(fullText)) {
            val candidate = match.value
            if (isValidName(candidate)) {
                return candidate
            }
        }
        return ""
    }

    private fun isValidName(text: String): Boolean {
        if (text.length !in 2..4) return false

        val isExcluded = EXCLUDE_NAME_WORDS.any { it.contains(text) || text.contains(it) }
        if (isExcluded) return false

        val commonSurnames = listOf(
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "유",
            "홍", "전", "고", "문", "양", "손", "배", "백", "허", "남"
        )
        return commonSurnames.contains(text.first().toString())
    }

    private fun extractIssueDate(text: String): String {
        val datePattern = Regex("(19|20)\\d{2}[.,:;\\-/년\\s]*\\d{1,2}[.,:;\\-/월\\s]*\\d{1,2}")
        val match = datePattern.findAll(text).lastOrNull() ?: return ""

        val numbers = Regex("\\d+").findAll(match.value).map { it.value }.toList()
        return if (numbers.size >= 3) {
            "${numbers[0]}${numbers[1].padStart(2, '0')}${numbers[2].padStart(2, '0')}"
        } else ""
    }

    private fun resetProcessing() {
        isProcessing = false
        validFrameCount = 0
        lastDetectedRect = null
        smoothedCardRect = null
        lastValidBitmap?.recycle()
        lastValidBitmap = null
        idTypeHistory.clear()
        confirmedIdType = ID_TYPE_UNKNOWN
    }

    private fun navigateToResult(idCardInfo: IdCardInfo) {
        startActivity(Intent(this, IdCardResultActivity::class.java).putExtra("ID_CARD_INFO", idCardInfo))
        finish()
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) startCamera()
        else { Toast.makeText(this, "카메라 권한 필요", Toast.LENGTH_SHORT).show(); finish() }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopHapticFeedback()
        cameraExecutor.shutdown()
        textRecognizer.close()
        idCardDetector?.close()
        lastValidBitmap?.recycle()
    }
}