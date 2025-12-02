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
import android.widget.TextView
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
import com.google.mlkit.vision.text.Text
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
    private var isProcessing = false
    private var recognitionCompleted = false
    private var validFrameCount = 0
    private var vibrator: Vibrator? = null
    private var currentHapticLevel = 0
    private var frameCount = 0
    private var lastLogTime = 0L
    private var lastDetectedRect: Rect? = null
    private var lastValidText: String = ""
    private var lastValidBitmap: Bitmap? = null
    private var analysisWidth = 0
    private var analysisHeight = 0
    private var analysisRotation = 0
    private var lastAnnouncedMessage: String = ""
    private var lastAnnounceTime: Long = 0L
    private val ANNOUNCE_INTERVAL = 2000L
    companion object {
        private const val TAG = "IdCardRecog"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val REQUIRED_VALID_FRAMES = 5
        private const val GUIDE_MATCH_THRESHOLD = 0.90f
        private const val STABILITY_THRESHOLD = 0.08f
        private const val CARD_ASPECT_RATIO = 1.585f
        private var smoothedCardRect: Rect? = null
        private val SMOOTHING_FACTOR = 0.3f
        private const val ID_TYPE_UNKNOWN = 0
        private const val ID_TYPE_RESIDENT = 1
        private const val ID_TYPE_DRIVER = 2
        private var idTypeHistory = mutableListOf<Int>()
        private var confirmedIdType: Int = ID_TYPE_UNKNOWN
        private val ID_TYPE_CONFIRM_THRESHOLD = 5
        private const val RESIDENT_TEXT_WIDTH_RATIO = 0.45f
        private const val RESIDENT_TEXT_HEIGHT_RATIO = 0.55f
        private const val RESIDENT_PHOTO_WIDTH_RATIO = 0.30f
        private const val DRIVER_TEXT_WIDTH_RATIO = 0.55f
        private const val DRIVER_TEXT_HEIGHT_RATIO = 0.65f
        private const val DRIVER_PHOTO_WIDTH_RATIO = 0.25f
        private val RESIDENT_KEYWORDS = listOf(
            "주민등록증", "RESIDENT", "REGISTRATION", "주민번호"
        )
        private val DRIVER_KEYWORDS = listOf(
            "운전면허증", "운전면허", "DRIVER", "LICENSE", "면허번호",
            "적성검사", "갱신기간", "조건", "면허"
        )
        private val EXCLUDE_NAME_WORDS = listOf("주민등록증", "면허",
            "주민등록", "주민번호", "등록증", "운전면허", "면허증", "자동차",
            "대한민국", "경찰청장", "도지사", "시장", "군수", "구청장",
            "발급일", "생년월일", "주소지", "적성검사", "갱신기간",
            "면허번호", "조건", "종류", "보통", "원동기", "대형",
            "성명", "이름", "주소", "발행", "유효기간", "경찰청"
        )
        private val DRIVER_LICENSE_NUMBER_PATTERN = Regex("\\d{2}[- ]?\\d{2}[- ]?\\d{6}[- ]?\\d{2}")
        private val RESIDENT_NUMBER_PATTERN = Regex("\\d{6}[- ]?[1-4]\\d{6}")
        private val NAME_PATTERN = Regex("[가-힣]{2,4}")
        private val ID_CARD_KEYWORDS = listOf("주민등록증", "운전면허증", "RESIDENT", "DRIVER", "LICENSE")
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

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetResolution(android.util.Size(1280, 720))
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

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        analysisWidth = imageProxy.width
        analysisHeight = imageProxy.height
        analysisRotation = imageProxy.imageInfo.rotationDegrees

        val inputImage = InputImage.fromMediaImage(mediaImage, analysisRotation)

        val currentTime = System.currentTimeMillis()
        val shouldLog = currentTime - lastLogTime > 1000
        if (shouldLog) {
            Log.d(TAG, "===== Frame #$frameCount =====")
            lastLogTime = currentTime
        }

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                handleTextResult(visionText, imageProxy, shouldLog)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "텍스트 인식 실패", e)
                imageProxy.close()
            }
    }

    private fun handleTextResult(
        visionText: Text,
        imageProxy: ImageProxy,
        shouldLog: Boolean
    ) {
        val fullText = visionText.text
        val textBlocks = visionText.textBlocks

        if (textBlocks.isEmpty()) {
            handleInvalidFrame("텍스트가 보이지 않습니다")
            imageProxy.close()
            return
        }

        val detectedType = detectIdCardType(fullText)
        updateIdTypeHistory(detectedType)
        val idType = getConfirmedOrBestType()
        val idCardScore = calculateIdCardScore(fullText)
        val isIdCard = idCardScore >= 2

        if (shouldLog) {
            val typeName = when (idType) {
                ID_TYPE_RESIDENT -> "주민등록증"
                ID_TYPE_DRIVER -> "운전면허증"
                else -> "미확인"
            }
            val status = if (confirmedIdType != ID_TYPE_UNKNOWN) "확정" else "감지중(${idTypeHistory.size})"
            Log.d(TAG, "텍스트: ${fullText.take(50)}...")
            Log.d(TAG, "신분증 유형: $typeName, 점수: $idCardScore")
        }

        if (!isIdCard) {
            handleInvalidFrame("신분증을 비춰주세요")
            imageProxy.close()
            return
        }

        val (rotatedWidth, rotatedHeight) = when (analysisRotation) {
            90, 270 -> analysisHeight to analysisWidth
            else -> analysisWidth to analysisHeight
        }

        val guideRect = calculateGuideRect(rotatedWidth, rotatedHeight)
        val textBounds = calculateTextBounds(textBlocks)
        val cardRect = when (idType) {
            ID_TYPE_RESIDENT -> expandForResidentCard(textBounds, rotatedWidth, rotatedHeight)
            ID_TYPE_DRIVER -> expandForDriverLicense(textBounds, rotatedWidth, rotatedHeight)
            else -> expandToCardRatio(textBounds, guideRect, rotatedWidth, rotatedHeight)
        }

        val smoothedRect = smoothRect(cardRect)
        val matchResult = calculateGuideMatch(smoothedRect, guideRect)
        val hapticScore = calculateHapticScore(smoothedRect, guideRect)

        val isInsideGuide = matchResult.insideRatio >= GUIDE_MATCH_THRESHOLD
        val isStable = isStablePosition(cardRect)
        val isValidFrame = isIdCard && isInsideGuide && matchResult.fillRatio >= 0.9f && isStable

        triggerHapticFeedback(isIdCard, hapticScore)

        val fillPercent = (matchResult.fillRatio * 100).toInt()
        val idTypeName = when (idType) {
            ID_TYPE_RESIDENT -> "주민등록증"
            ID_TYPE_DRIVER -> "운전면허증"
            else -> "신분증"
        }

        val statusMessage = when {
            isValidFrame -> "ⓘ 신분증이 중심에 들어왔습니다. "  + "\n" +
                    "촬영 중이니 움직이지 마십시오."
            else -> analyzePosition(smoothedRect, guideRect)
            }
        val guideColor = if (isIdCard) {
            android.graphics.Color.parseColor("#FFE621")
        } else {
            android.graphics.Color.WHITE
        }

        val debugIdType = when (idType) {
            ID_TYPE_RESIDENT -> "주민"
            ID_TYPE_DRIVER -> "면허"
            else -> "?"
        }
        val confirmStatus = if (confirmedIdType != ID_TYPE_UNKNOWN) "✓" else "${idTypeHistory.size}/$ID_TYPE_CONFIRM_THRESHOLD"

        runOnUiThread {
            val shouldAnnounce = statusMessage != overlayView.checkMessage()
            overlayView.setStatusMessage(statusMessage)
            if (shouldAnnounce && (isValidFrame || statusMessage.contains("이동하세요"))) {
                announceForAccessibility(statusMessage)
            }
            overlayView.setGuideColor(guideColor)
            overlayView.setDebugText("[$debugIdType$confirmStatus] 채움:${fillPercent}% 연속:$validFrameCount")
            overlayView.setDetectedRect(smoothedRect, rotatedWidth, rotatedHeight, 0)
        }

        if (isValidFrame) {
            validFrameCount++
            lastValidText = fullText
            lastDetectedRect = cardRect

            if (validFrameCount == REQUIRED_VALID_FRAMES - 1) {
                lastValidBitmap?.recycle()
                lastValidBitmap = cropGuideArea(imageProxy)
            }

            if (validFrameCount >= REQUIRED_VALID_FRAMES) {
                isProcessing = true

                if (lastValidBitmap == null) {
                    lastValidBitmap = cropGuideArea(imageProxy)
                }

                Log.d(TAG, "★★★ 인식 완료! ★★★")
                processAndSaveIdCard(lastValidText, lastValidBitmap)
            }
        } else {
            if (validFrameCount > 0) {
                validFrameCount = maxOf(0, validFrameCount - 1)
            }
            lastDetectedRect = cardRect
        }

        imageProxy.close()
    }

    private fun announceForAccessibility(message: String, force: Boolean = false) {
        if (message.isEmpty()) return

        val currentTime = System.currentTimeMillis()

        // 같은 메시지이고 간격이 짧으면 스킵 (force가 아닌 경우)
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

    // 상태 텍스트 업데이트 + 접근성 알림
    private fun updateStatusText(message: String, announceImportant: Boolean = false) {
        runOnUiThread {
            overlayView.setStatusMessage(message)
            if (announceImportant) {
                announceForAccessibility(message)
            }
        }
    }

    private fun updateIdTypeHistory(detectedType: Int) {
        if (confirmedIdType != ID_TYPE_UNKNOWN) return
        if (detectedType == ID_TYPE_UNKNOWN) return

        idTypeHistory.add(detectedType)

        while (idTypeHistory.size > ID_TYPE_CONFIRM_THRESHOLD * 2) {
            idTypeHistory.removeAt(0)
        }

        if (idTypeHistory.size >= ID_TYPE_CONFIRM_THRESHOLD) {
            val recent = idTypeHistory.takeLast(ID_TYPE_CONFIRM_THRESHOLD)
            if (recent.all { it == detectedType }) {
                confirmedIdType = detectedType
                Log.d(TAG, "★★★ 신분증 종류 확정: ${if (detectedType == ID_TYPE_RESIDENT) "주민등록증" else "운전면허증"} ★★★")
            }
        }
    }

    private fun getConfirmedOrBestType(): Int {
        if (confirmedIdType != ID_TYPE_UNKNOWN) {
            return confirmedIdType
        }

        if (idTypeHistory.isEmpty()) {
            return ID_TYPE_UNKNOWN
        }

        val residentCount = idTypeHistory.count { it == ID_TYPE_RESIDENT }
        val driverCount = idTypeHistory.count { it == ID_TYPE_DRIVER }

        return when {
            residentCount > driverCount -> ID_TYPE_RESIDENT
            driverCount > residentCount -> ID_TYPE_DRIVER
            else -> idTypeHistory.last()
        }
    }

    private fun calculateHapticScore(card: Rect, guide: Rect): Float {
        if (card.isEmpty) return 0f

        val cardArea = card.width().toLong() * card.height()
        val guideArea = guide.width().toLong() * guide.height()

        if (guideArea == 0L) return 0f

        val sizeRatio = cardArea.toFloat() / guideArea

        val intersection = Rect()
        if (!intersection.setIntersect(card, guide)) {
            return 0f
        }

        val intersectionArea = intersection.width().toLong() * intersection.height()
        val fillRatio = intersectionArea.toFloat() / guideArea

        return if (sizeRatio > 1.1f) {
            fillRatio / sizeRatio
        } else {
            fillRatio
        }
    }

    private fun analyzePosition(cardRect: Rect, guideRect: Rect): String {
        if (cardRect.isEmpty) return "신분증을 비춰주세요"

        val cardCenterX = cardRect.centerX()
        val cardCenterY = cardRect.centerY()
        val guideCenterX = guideRect.centerX()
        val guideCenterY = guideRect.centerY()

        val guideWidth = guideRect.width()
        val guideHeight = guideRect.height()
        val cardWidth = cardRect.width()
        val cardHeight = cardRect.height()

        val offsetX = (cardCenterX - guideCenterX).toFloat() / guideWidth
        val offsetY = (cardCenterY - guideCenterY).toFloat() / guideHeight

        val sizeRatio = (cardWidth.toFloat() * cardHeight) / (guideWidth.toFloat() * guideHeight)

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

    /*private fun showToastIfNeeded(message: String) {
        val currentTime = System.currentTimeMillis()

        if (message != lastToastMessage || currentTime - lastToastTime > TOAST_INTERVAL) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            lastToastMessage = message
            lastToastTime = currentTime
        }
    }*/

    private fun smoothRect(newRect: Rect): Rect {
        val prevRect = smoothedCardRect

        if (prevRect == null || prevRect.isEmpty) {
            smoothedCardRect = Rect(newRect)
            return newRect
        }

        val centerDiff = kotlin.math.abs(newRect.centerX() - prevRect.centerX()) +
                kotlin.math.abs(newRect.centerY() - prevRect.centerY())
        val avgSize = (prevRect.width() + prevRect.height()) / 2f

        if (avgSize > 0 && centerDiff / avgSize > 0.3f) {
            smoothedCardRect = Rect(newRect)
            return newRect
        }

        val smoothedLeft = lerp(prevRect.left, newRect.left, SMOOTHING_FACTOR)
        val smoothedTop = lerp(prevRect.top, newRect.top, SMOOTHING_FACTOR)
        val smoothedRight = lerp(prevRect.right, newRect.right, SMOOTHING_FACTOR)
        val smoothedBottom = lerp(prevRect.bottom, newRect.bottom, SMOOTHING_FACTOR)

        val result = Rect(smoothedLeft, smoothedTop, smoothedRight, smoothedBottom)
        smoothedCardRect = result
        return result
    }

    private fun lerp(start: Int, end: Int, factor: Float): Int {
        return (start + (end - start) * factor).toInt()
    }

    private fun detectIdCardType(text: String): Int {
        val upperText = text.uppercase()

        val driverScore = DRIVER_KEYWORDS.count { keyword ->
            text.contains(keyword, ignoreCase = true) || upperText.contains(keyword.uppercase())
        }

        val residentScore = RESIDENT_KEYWORDS.count { keyword ->
            text.contains(keyword, ignoreCase = true) || upperText.contains(keyword.uppercase())
        }

        val hasDriverLicenseNumber = DRIVER_LICENSE_NUMBER_PATTERN.containsMatchIn(text)

        return when {
            hasDriverLicenseNumber -> ID_TYPE_DRIVER
            driverScore >= 2 -> ID_TYPE_DRIVER
            residentScore >= 1 -> ID_TYPE_RESIDENT
            driverScore >= 1 -> ID_TYPE_DRIVER
            else -> ID_TYPE_UNKNOWN
        }
    }

    private fun expandForResidentCard(
        textBounds: Rect,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        if (textBounds.isEmpty) return Rect()

        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()

        val estimatedCardWidth = textWidth / RESIDENT_TEXT_WIDTH_RATIO
        val estimatedCardHeightFromText = textHeight / RESIDENT_TEXT_HEIGHT_RATIO
        val estimatedCardHeightFromRatio = estimatedCardWidth / CARD_ASPECT_RATIO

        val finalHeight = max(estimatedCardHeightFromText, estimatedCardHeightFromRatio)
        val finalWidth = finalHeight * CARD_ASPECT_RATIO

        val cardRight = textBounds.right + (finalWidth * RESIDENT_PHOTO_WIDTH_RATIO)
        val cardCenterY = textBounds.centerY().toFloat()

        val left = (cardRight - finalWidth).toInt()
        val top = (cardCenterY - finalHeight / 2).toInt()
        val right = cardRight.toInt()
        val bottom = (cardCenterY + finalHeight / 2).toInt()

        return Rect(
            max(0, left),
            max(0, top),
            min(imageWidth, right),
            min(imageHeight, bottom)
        )
    }

    private fun expandForDriverLicense(
        textBounds: Rect,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        if (textBounds.isEmpty) return Rect()

        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()

        val estimatedCardWidth = textWidth / DRIVER_TEXT_WIDTH_RATIO
        val estimatedCardHeightFromText = textHeight / DRIVER_TEXT_HEIGHT_RATIO
        val estimatedCardHeightFromRatio = estimatedCardWidth / CARD_ASPECT_RATIO

        val finalHeight = max(estimatedCardHeightFromText, estimatedCardHeightFromRatio)
        val finalWidth = finalHeight * CARD_ASPECT_RATIO

        val cardLeft = textBounds.left - (finalWidth * DRIVER_PHOTO_WIDTH_RATIO)
        val cardCenterY = textBounds.centerY().toFloat()

        val left = cardLeft.toInt()
        val top = (cardCenterY - finalHeight / 2).toInt()
        val right = (cardLeft + finalWidth).toInt()
        val bottom = (cardCenterY + finalHeight / 2).toInt()

        return Rect(
            max(0, left),
            max(0, top),
            min(imageWidth, right),
            min(imageHeight, bottom)
        )
    }

    private fun calculateTextBounds(textBlocks: List<Text.TextBlock>): Rect {
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE

        for (block in textBlocks) {
            block.boundingBox?.let { box ->
                minX = min(minX, box.left)
                minY = min(minY, box.top)
                maxX = max(maxX, box.right)
                maxY = max(maxY, box.bottom)
            }
        }

        return Rect(minX, minY, maxX, maxY)
    }

    private fun expandToCardRatio(
        textBounds: Rect,
        guideRect: Rect,
        imageWidth: Int,
        imageHeight: Int
    ): Rect {
        if (textBounds.isEmpty) return guideRect

        val textWidth = textBounds.width().toFloat()
        val textHeight = textBounds.height().toFloat()
        val textCenterX = textBounds.centerX().toFloat()
        val textCenterY = textBounds.centerY().toFloat()

        val estimatedCardWidth = textWidth / 0.55f
        val estimatedCardHeightFromText = textHeight / 0.75f
        val estimatedCardHeightFromRatio = estimatedCardWidth / CARD_ASPECT_RATIO
        val finalHeight = max(estimatedCardHeightFromText, estimatedCardHeightFromRatio)
        val finalWidth = finalHeight * CARD_ASPECT_RATIO
        val horizontalOffset = finalWidth * 0.1f
        val verticalOffset = finalHeight * 0.05f

        val centerX = textCenterX - horizontalOffset
        val centerY = textCenterY - verticalOffset

        val left = (centerX - finalWidth / 2).toInt()
        val top = (centerY - finalHeight / 2).toInt()
        val right = (centerX + finalWidth / 2).toInt()
        val bottom = (centerY + finalHeight / 2).toInt()

        return Rect(
            max(0, left),
            max(0, top),
            min(imageWidth, right),
            min(imageHeight, bottom)
        )
    }

    private fun calculateGuideRect(imageWidth: Int, imageHeight: Int): Rect {
        val guideWidth = (imageWidth * 0.85f).toInt()
        val guideHeight = (guideWidth / CARD_ASPECT_RATIO).toInt()
        val left = (imageWidth - guideWidth) / 2
        val top = (imageHeight - guideHeight) / 2
        return Rect(left, top, left + guideWidth, top + guideHeight)
    }

    data class GuideMatchResult(val insideRatio: Float, val fillRatio: Float)

    private fun calculateGuideMatch(card: Rect, guide: Rect): GuideMatchResult {
        if (card.isEmpty) return GuideMatchResult(0f, 0f)

        val intersection = Rect()
        if (!intersection.setIntersect(card, guide)) {
            return GuideMatchResult(0f, 0f)
        }

        val intersectionArea = intersection.width().toLong() * intersection.height()
        val cardArea = card.width().toLong() * card.height()
        val guideArea = guide.width().toLong() * guide.height()

        return GuideMatchResult(
            if (cardArea > 0) intersectionArea.toFloat() / cardArea else 0f,
            if (guideArea > 0) intersectionArea.toFloat() / guideArea else 0f
        )
    }

    private fun isStablePosition(currentRect: Rect): Boolean {
        val lastRect = lastDetectedRect ?: return true
        if (lastRect.isEmpty || currentRect.isEmpty) return true

        val avgSize = (lastRect.width() + lastRect.height()) / 2f
        if (avgSize == 0f) return true

        val centerXDiff = kotlin.math.abs(currentRect.centerX() - lastRect.centerX()).toFloat()
        val centerYDiff = kotlin.math.abs(currentRect.centerY() - lastRect.centerY()).toFloat()

        return (centerXDiff + centerYDiff) / avgSize < STABILITY_THRESHOLD
    }

    private fun calculateIdCardScore(text: String): Int {
        var score = 0
        if (RESIDENT_NUMBER_PATTERN.containsMatchIn(text)) score += 2
        for (keyword in ID_CARD_KEYWORDS) {
            if (text.contains(keyword, ignoreCase = true)) { score += 2; break }
        }
        if (NAME_PATTERN.containsMatchIn(text)) score += 1
        if (Regex("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}").containsMatchIn(text)) score += 1
        return score
    }

    private fun handleInvalidFrame(message: String) {
        if (validFrameCount > 0) validFrameCount--

        smoothedCardRect = null
        stopHapticFeedback()

        idTypeHistory.clear()
        if (validFrameCount == 0) {
            confirmedIdType = ID_TYPE_UNKNOWN
        }

        runOnUiThread {
            overlayView.setStatusMessage(message)
            overlayView.clearDetection()
            overlayView.setGuideColor(android.graphics.Color.WHITE)
        }
    }

    private fun triggerHapticFeedback(
        isIdCardDetected: Boolean,
        fillRatio: Float
    ) {
        if (!isIdCardDetected) {
            if (currentHapticLevel != 0) {
                currentHapticLevel = 0
                vibrator?.cancel()
            }
            return
        }

        val newHapticLevel = when {
            fillRatio >= 0.95f -> 5
            fillRatio >= 0.90f -> 4
            fillRatio >= 0.80f -> 3
            fillRatio >= 0.70f -> 2
            else -> 1
        }

        if (newHapticLevel != currentHapticLevel) {
            currentHapticLevel = newHapticLevel
            vibrator?.cancel()
            startVibrationPattern(newHapticLevel)
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

    private fun stopHapticFeedback() {
        currentHapticLevel = 0
        vibrator?.cancel()
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun cropGuideArea(imageProxy: ImageProxy): Bitmap? {
        return try {
            val fullBitmap = imageProxyToBitmap(imageProxy) ?: return null

            val viewGuideRect = overlayView.getGuideRect()

            val viewWidth = overlayView.width.toFloat()
            val viewHeight = overlayView.height.toFloat()
            val bitmapWidth = fullBitmap.width.toFloat()
            val bitmapHeight = fullBitmap.height.toFloat()

            if (viewWidth <= 0 || viewHeight <= 0) {
                return fullBitmap
            }

            val viewAspect = viewWidth / viewHeight
            val bitmapAspect = bitmapWidth / bitmapHeight

            val scale: Float
            val offsetX: Float
            val offsetY: Float

            if (bitmapAspect > viewAspect) {
                scale = bitmapHeight / viewHeight
                offsetX = (bitmapWidth - viewWidth * scale) / 2f
                offsetY = 0f
            } else {
                scale = bitmapWidth / viewWidth
                offsetX = 0f
                offsetY = (bitmapHeight - viewHeight * scale) / 2f
            }

            val CROP_PADDING_RATIO = 0.15f

            val paddingX = viewGuideRect.width() * CROP_PADDING_RATIO
            val paddingY = viewGuideRect.height() * CROP_PADDING_RATIO

            val imageLeft = ((viewGuideRect.left - paddingX) * scale + offsetX).toInt()
            val imageTop = ((viewGuideRect.top - paddingY) * scale + offsetY).toInt()
            val imageRight = ((viewGuideRect.right + paddingX) * scale + offsetX).toInt()
            val imageBottom = ((viewGuideRect.bottom + paddingY) * scale + offsetY).toInt()

            val cropLeft = maxOf(0, imageLeft)
            val cropTop = maxOf(0, imageTop)
            val cropRight = minOf(fullBitmap.width, imageRight)
            val cropBottom = minOf(fullBitmap.height, imageBottom)

            val cropWidth = cropRight - cropLeft
            val cropHeight = cropBottom - cropTop

            if (cropWidth <= 0 || cropHeight <= 0) {
                return fullBitmap
            }

            val croppedBitmap = Bitmap.createBitmap(
                fullBitmap, cropLeft, cropTop, cropWidth, cropHeight
            )

            if (croppedBitmap != fullBitmap) {
                fullBitmap.recycle()
            }

            croppedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "크롭 실패: ${e.message}", e)
            null
        }
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

    private fun processAndSaveIdCard(fullText: String, bitmap: Bitmap?) {
        lifecycleScope.launch {
            updateStatusText("저장 중...", announceImportant = true)

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
                updateStatusText("정보 추출 실패. 다시 시도하세요.", announceImportant = true)
                resetProcessing()
            }
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

            Log.d(TAG, "이미지 저장: ${file.absolutePath}, 크기: ${bitmap.width}x${bitmap.height}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "저장 실패: ${e.message}")
            null
        }
    }

    private fun extractIdCardInfo(text: String): IdCardInfo {
        return IdCardInfo().apply {
            driverLicenseNumber = DRIVER_LICENSE_NUMBER_PATTERN.find(text)?.value
                ?.replace(Regex("[^0-9]"), "") ?: ""
            residentNumber = RESIDENT_NUMBER_PATTERN.find(text)?.value
                ?.replace(Regex("[^0-9]"), "") ?: ""
            issueDate = Regex("\\d{4}[./-]\\d{1,2}[./-]\\d{1,2}").find(text)?.value
                ?.replace(Regex("[^0-9]"), "") ?: ""
            name = extractName(text)
            address = Regex("([가-힣]+(?:시|도))[\\s]*[가-힣]+").find(text)?.value ?: ""
            idType = when (confirmedIdType) {
                ID_TYPE_RESIDENT -> "resident"
                ID_TYPE_DRIVER -> "driver"
                else -> "unknown"
            }
        }
    }

    private fun extractName(text: String): String {
        val matches = NAME_PATTERN.findAll(text)

        for (match in matches) {
            val candidate = match.value

            val isExcluded = EXCLUDE_NAME_WORDS.any { excluded ->
                excluded.contains(candidate) || candidate.contains(excluded)
            }

            if (!isExcluded) {
                if (isLikelyName(candidate)) {
                    return candidate
                }
            }
        }

        for (match in NAME_PATTERN.findAll(text)) {
            val candidate = match.value
            val isExcluded = EXCLUDE_NAME_WORDS.any { excluded ->
                excluded.contains(candidate) || candidate.contains(excluded)
            }
            if (!isExcluded) {
                return candidate
            }
        }

        return ""
    }

    private fun isLikelyName(text: String): Boolean {
        if (text.length !in 2..4) return false

        val commonSurnames = listOf(
            "김", "이", "박", "최", "정", "강", "조", "윤", "장", "임",
            "한", "오", "서", "신", "권", "황", "안", "송", "류", "유",
            "홍", "전", "고", "문", "양", "손", "배", "백", "허", "남"
        )

        val firstChar = text.first().toString()
        return commonSurnames.contains(firstChar)
    }

    private fun resetProcessing() {
        isProcessing = false
        validFrameCount = 0
        lastDetectedRect = null
        smoothedCardRect = null
        lastValidText = ""
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
        lastValidBitmap?.recycle()
    }
}