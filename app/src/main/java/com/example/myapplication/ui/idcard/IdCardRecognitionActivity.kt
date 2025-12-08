package com.example.myapplication.ui.idcard

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.ui.idcard.camera.GuideRectCalculator
import com.example.myapplication.ui.idcard.detector.IdCardDetector
import com.example.myapplication.ui.idcard.ocr.IdCardOcrProcessor
import com.example.myapplication.ui.idcard.util.HapticFeedbackManager
import com.example.myapplication.ui.idcard.util.ImageUtils
import com.example.myapplication.ui.main.MainActivity
import com.example.myapplication.view.OverlayView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdCardRecognitionActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IdCardRecog"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val REQUIRED_VALID_FRAMES = 3
    }

    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var cancelBtn: ImageView
    private lateinit var cameraExecutor: ExecutorService
    private var idCardDetector: IdCardDetector? = null
    private val ocrProcessor = IdCardOcrProcessor()
    private val guideCalculator = GuideRectCalculator()
    private lateinit var hapticManager: HapticFeedbackManager
    private var isProcessing = false
    private var recognitionCompleted = false
    private var validFrameCount = 0
    private var isFinishing = false
    private var analysisRotation = 0
    private var lastValidBitmap: Bitmap? = null
    private var lastAnnouncedMessage = ""
    private var lastAnnounceTime = 0L
    private val announceInterval = 2000L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_recognition)
        enableEdgeToEdge()

        initViews()
        initComponents()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun initViews() {
        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        cancelBtn = findViewById(R.id.cancel_button)
        cancelBtn.setOnClickListener { showCancelConfirmDialog() }
    }

    private fun initComponents() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        hapticManager = HapticFeedbackManager(this)

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
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetAspectRatio(AspectRatio.RATIO_DEFAULT)
                .build()
                .also { it.setAnalyzer(cameraExecutor) { processFrame(it) } }

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
        if (isFinishing || recognitionCompleted || isProcessing) {
            imageProxy.close()
            return
        }

        val detector = idCardDetector
        if (detector == null || !detector.isLoaded()) {
            imageProxy.close()
            return
        }

        analysisRotation = imageProxy.imageInfo.rotationDegrees
        val bitmap = ImageUtils.imageProxyToBitmap(imageProxy, analysisRotation)

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

        val analysis = guideCalculator.analyzeFrame(
            detection.boundingBox, bitmap.width, bitmap.height, overlayView
        )

        hapticManager.update(true, analysis.hapticScore)

        updateUI(analysis, detection.confidence, bitmap.width, bitmap.height)

        if (analysis.isValidFrame) {
            handleValidFrame(bitmap, detection.boundingBox)
        } else {
            if (validFrameCount > 0) validFrameCount--
        }

        bitmap.recycle()
        imageProxy.close()
    }

    private fun handleValidFrame(bitmap: Bitmap, boundingBox: RectF) {
        validFrameCount++

        if (validFrameCount == REQUIRED_VALID_FRAMES - 1) {
            lastValidBitmap?.recycle()
            lastValidBitmap = ImageUtils.cropArea(bitmap, boundingBox)
        }

        if (validFrameCount >= REQUIRED_VALID_FRAMES) {
            isProcessing = true

            if (lastValidBitmap == null) {
                lastValidBitmap = ImageUtils.cropArea(bitmap, boundingBox)
            }

            Log.d(TAG, "★★★ 신분증 탐지 완료! OCR 시작 ★★★")
            performOcr(lastValidBitmap)
        }
    }

    private fun performOcr(bitmap: Bitmap?) {
        if (bitmap == null) {
            runOnUiThread { Toast.makeText(this, "이미지 크롭 실패", Toast.LENGTH_SHORT).show() }
            resetProcessing()
            return
        }

        ocrProcessor.process(bitmap, object : IdCardOcrProcessor.Callback {
            override fun onSuccess(info: IdCardInfo, fullText: String) {
                lifecycleScope.launch {
                    saveAndNavigate(info, fullText, bitmap)
                }
            }

            override fun onFailure(message: String) {
                runOnUiThread { Toast.makeText(this@IdCardRecognitionActivity, message, Toast.LENGTH_SHORT).show() }
                resetProcessing()
            }
        })
    }

    private suspend fun saveAndNavigate(info: IdCardInfo, fullText: String, bitmap: Bitmap?) {
        runOnUiThread { overlayView.setStatusMessage("저장 중...") }

        val imagePath = withContext(Dispatchers.IO) {
            ImageUtils.saveToFile(bitmap, File(filesDir, "id_cards"))
        }

        if (imagePath != null) {
            info.imagePath = imagePath
        }
        Log.d(TAG, "저장 완료: $imagePath")
        Log.d(TAG, "추출 정보: $info")

        if (info.isValid()) {
            recognitionCompleted = true
            startActivity(Intent(this, IdCardResultActivity::class.java)
                .putExtra("ID_CARD_INFO", info))
            finish()
        } else {
            runOnUiThread {
                Toast.makeText(this, "정보 추출 실패. 다시 시도하세요.", Toast.LENGTH_SHORT).show()
            }
            resetProcessing()
        }
    }

    private fun updateUI(analysis: GuideRectCalculator.FrameAnalysis,
                         confidence: Float,
                         bitmapWidth: Int,
                         bitmapHeight: Int) {
        runOnUiThread {
            val shouldAnnounce = analysis.statusMessage != overlayView.checkMessage()
            overlayView.setStatusMessage(analysis.statusMessage)

            if (shouldAnnounce && (analysis.isValidFrame || analysis.statusMessage.contains("이동하세요"))) {
                announceForAccessibility(analysis.statusMessage)
            }

            overlayView.setGuideColor(Color.parseColor("#FFE621"))
            overlayView.setDebugText("[YOLO] 신뢰도:${(confidence * 100).toInt()}% 햅틱:${(analysis.hapticScore * 100).toInt()}%")

            val intRect = Rect(
                analysis.smoothedRect.left.toInt(),
                analysis.smoothedRect.top.toInt(),
                analysis.smoothedRect.right.toInt(),
                analysis.smoothedRect.bottom.toInt()
            )
            overlayView.setDetectedRect(intRect, bitmapWidth, bitmapHeight, 0)
        }
    }

    private fun handleInvalidFrame(message: String) {
        if (validFrameCount > 0) validFrameCount--
        guideCalculator.reset()
        hapticManager.stop()

        runOnUiThread {
            overlayView.setStatusMessage(message)
            overlayView.clearDetection()
            overlayView.setGuideColor(Color.WHITE)
        }
    }

    private fun resetProcessing() {
        isProcessing = false
        validFrameCount = 0
        guideCalculator.reset()
        lastValidBitmap?.recycle()
        lastValidBitmap = null
    }

    private fun showCancelConfirmDialog() {
        isFinishing = true
        hapticManager.stop()

        val dialogView = layoutInflater.inflate(R.layout.dialog_cancel_confirm, null)
        val dialog = android.app.Dialog(this).apply {
            requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
            setContentView(dialogView)
            window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
            window?.setLayout(
                (resources.displayMetrics.widthPixels * 0.85).toInt(),
                android.view.WindowManager.LayoutParams.WRAP_CONTENT
            )
            setCancelable(true)
            setOnCancelListener { isFinishing = false }
        }

        dialogView.findViewById<android.widget.Button>(R.id.btnCancel).setOnClickListener {
            isFinishing = false
            dialog.dismiss()
        }
        dialogView.findViewById<android.widget.Button>(R.id.btnConfirm).setOnClickListener {
            dialog.dismiss()
            safeFinish()
        }
        dialog.show()
    }

    private fun safeFinish() {
        isFinishing = true
        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll()
        } catch (e: Exception) {
            Log.e(TAG, "카메라 정리 실패: ${e.message}")
        }

        previewView.postDelayed({
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
            finish()
        }, 100)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isFinishing) showCancelConfirmDialog()
    }

    private fun announceForAccessibility(message: String) {
        if (message.isEmpty()) return
        val currentTime = System.currentTimeMillis()

        if (message == lastAnnouncedMessage && currentTime - lastAnnounceTime < announceInterval) return

        (getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)?.let { am ->
            if (am.isEnabled) {
                val event = AccessibilityEvent.obtain().apply {
                    eventType = AccessibilityEvent.TYPE_ANNOUNCEMENT
                    className = javaClass.name
                    packageName = packageName
                    text.add(message)
                }
                am.sendAccessibilityEvent(event)
                lastAnnouncedMessage = message
                lastAnnounceTime = currentTime
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && allPermissionsGranted()) {
            startCamera()
        } else {
            Toast.makeText(this, "카메라 권한 필요", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isFinishing = true
        hapticManager.stop()
        cameraExecutor.shutdown()
        ocrProcessor.close()
        idCardDetector?.close()
        lastValidBitmap?.recycle()
    }
}