package com.example.myapplication.ui.idcard

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.data.IdCardInfo
import com.example.myapplication.view.OverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class IdCardRecognitionActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var overlayView: OverlayView
    private lateinit var statusText: TextView
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var isProcessing = false
    private var lastAnalysisTime = 0L
    private var recognitionCompleted = false
    private val textRecognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
    companion object {
        private const val TAG = "IdCardRecognition"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val ANALYSIS_INTERVAL_MS = 500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_id_card_recognition)

        previewView = findViewById(R.id.previewView)
        overlayView = findViewById(R.id.overlayView)
        statusText = findViewById(R.id.statusText)

        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, IdCardAnalyzer())
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )
            } catch (e: Exception) {
                Log.e(TAG, "카메라 바인딩 실패", e)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private inner class IdCardAnalyzer : ImageAnalysis.Analyzer {

        override fun analyze(imageProxy: ImageProxy) {
            val currentTime = System.currentTimeMillis()

            if (recognitionCompleted) {
                imageProxy.close()
                return
            }

            if (isProcessing || (currentTime - lastAnalysisTime) < ANALYSIS_INTERVAL_MS) {
                imageProxy.close()
                return
            }

            isProcessing = true
            lastAnalysisTime = currentTime

            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )

                performOCR(image, imageProxy)
            } else {
                isProcessing = false
                imageProxy.close()
            }
        }
    }

    private fun performOCR(image: InputImage, imageProxy: ImageProxy) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text

                if (extractedText.isNotEmpty() && !recognitionCompleted) {
                    Log.d(TAG, "인식된 텍스트: $extractedText")

                    val idCardInfo = extractIdCardInfo(extractedText)

                    if (idCardInfo.isValid()) {
                        recognitionCompleted = true

                        runOnUiThread {
                            statusText.text = "✓ 신분증 인식 완료! 이동 중..."
                            overlayView.setDetectionStatus(true)
                        }

                        capturePhotoAndNavigate(idCardInfo)
                    }
                }

                isProcessing = false
                imageProxy.close()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR 실패", e)
                isProcessing = false
                imageProxy.close()
            }
    }

    private fun extractIdCardInfo(text: String): IdCardInfo {
        val info = IdCardInfo()

        // 이름
        val namePattern = Regex("[가-힣]{2,4}")
        val nameMatch = namePattern.find(text)
        info.name = nameMatch?.value ?: ""

        // 주민번호
        val residentPattern = Regex("\\d{6}-?[1-4]\\d{6}")
        val residentMatch = residentPattern.find(text)
        info.residentNumber = residentMatch?.value ?: ""

        // 발급일
        val datePattern = Regex("\\d{4}[.\\-/]\\d{2}[.\\-/]\\d{2}")
        val dateMatch = datePattern.find(text)
        info.issueDate = dateMatch?.value ?: ""

        // 주소
        val addressPattern = Regex("([가-힣]+시|[가-힣]+도)\\s*[가-힣]+구?\\s*[가-힣]+동?")
        val addressMatch = addressPattern.find(text)
        info.address = addressMatch?.value ?: ""

        return info
    }

    private fun capturePhotoAndNavigate(idCardInfo: IdCardInfo) {
        val imageCapture = imageCapture

        if (imageCapture != null) {
            val photoFile = createImageFile()
            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(this),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        idCardInfo.imagePath = photoFile.absolutePath
                        navigateToResult(idCardInfo)
                    }

                    override fun onError(exc: ImageCaptureException) {
                        Log.e(TAG, "사진 저장 실패", exc)
                        navigateToResult(idCardInfo)
                    }
                }
            )
        } else {
            navigateToResult(idCardInfo)
        }
    }

    private fun navigateToResult(idCardInfo: IdCardInfo) {
        val intent = Intent(this, IdCardResultActivity::class.java).apply {
            putExtra("ID_CARD_INFO", idCardInfo)
        }
        startActivity(intent)
        finish()
    }

    private fun createImageFile(): File {
        return File(getExternalFilesDir(null), "id_card_${System.currentTimeMillis()}.jpg")
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        textRecognizer.close()
    }
}