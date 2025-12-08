package com.example.myapplication.ui.idcard.ocr

import android.graphics.Bitmap
import android.util.Log
import com.example.myapplication.data.IdCardInfo
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions

data class OcrResult(
    val extractedInfo: IdCardInfoExtractor.ExtractedInfo,
    val score: Int,
    val fullText: String,
    val pipelineIndex: Int
)

class IdCardOcrProcessor {

    companion object {
        private const val TAG = "IdCardOcr"
        private const val MIN_VALID_SCORE = 30

        private val PIPELINE_NAMES = listOf(
            "원본",
            "기본(CLAHE+샤프닝)",
            "고대비(강한CLAHE+감마)",
            "반사광제거(LAB+배경제거)",
            "적응형이진화"
        )

        private val PIPELINE_PRIORITY = mapOf(
            1 to 1,  // 기본 파이프라인 최우선
            2 to 2,  // 고대비
            0 to 3,  // 원본
            3 to 4,  // 반사광제거
            4 to 5   // 적응형이진화
        )
    }

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        KoreanTextRecognizerOptions.Builder().build()
    )
    private val preprocessor = IdCardImagePreprocessor()
    private val extractor = IdCardInfoExtractor()

    interface Callback {
        fun onSuccess(info: IdCardInfo, fullText: String)
        fun onFailure(message: String)
    }

    fun process(bitmap: Bitmap, callback: Callback) {
        val preprocessedImages = preprocessor.preprocess(bitmap)
        val results = mutableListOf<OcrResult>()
        var processedCount = 0
        val totalCount = preprocessedImages.size

        Log.d(TAG, "========================================")
        Log.d(TAG, "★ OCR 시작: 전처리 이미지 ${totalCount}개 생성")
        Log.d(TAG, "========================================")

        for ((index, img) in preprocessedImages.withIndex()) {
            val inputImage = InputImage.fromBitmap(img, 0)
            val pipelineName = PIPELINE_NAMES.getOrElse(index) { "파이프라인$index" }

            textRecognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    val text = visionText.text
                    val extractedInfo = extractor.extract(text)
                    val score = calculateScore(extractedInfo, text)

                    logPipelineResult(index, pipelineName, text, extractedInfo, score)

                    synchronized(results) {
                        results.add(OcrResult(extractedInfo, score, text, index))
                        processedCount++

                        if (processedCount == totalCount) {
                            selectBestAndCallback(results, preprocessedImages, callback)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "[$index] $pipelineName OCR 실패: ${e.message}")
                    synchronized(results) {
                        processedCount++
                        if (processedCount == totalCount) {
                            selectBestAndCallback(results, preprocessedImages, callback)
                        }
                    }
                }
        }
    }

    private fun logPipelineResult(
        index: Int,
        pipelineName: String,
        fullText: String,
        info: IdCardInfoExtractor.ExtractedInfo,
        score: Int
    ) {
        Log.d(TAG, "----------------------------------------")
        Log.d(TAG, "▶ [$index] $pipelineName | 점수: $score")
        Log.d(TAG, "----------------------------------------")

        Log.d(TAG, "【OCR 원문】")
        fullText.split("\n").forEachIndexed { lineIndex, line ->
            if (line.isNotBlank()) {
                Log.d(TAG, "  $lineIndex: $line")
            }
        }

        Log.d(TAG, "【추출 정보】")
        Log.d(TAG, "  - 이름: ${info.name ?: "(없음)"}")
        Log.d(TAG, "  - 주민번호: ${info.residentNumber ?: "(없음)"}")
        Log.d(TAG, "  - 면허번호: ${info.driverLicenseNumber ?: "(없음)"}")
        Log.d(TAG, "  - 발급일자: ${info.issueDate ?: "(없음)"}")
        Log.d(TAG, "  - 주소: ${info.address ?: "(없음)"}")
        Log.d(TAG, "  - 신분증종류: ${info.idType ?: "(없음)"}")

        Log.d(TAG, "【점수 상세】")
        val breakdown = buildString {
            append("  ")
            info.name?.let {
                if (it.matches(Regex("^[가-힣]{2,4}$"))) append("이름+30 ")
            }
            info.residentNumber?.let {
                if (it.length == 13 && it.matches(Regex("^\\d{6}[1-4]\\d{6}$"))) {
                    append("주민번호+40 ")
                }
            }
            info.driverLicenseNumber?.let {
                if (it.length == 12) append("면허번호+35 ")
            }
            info.issueDate?.let {
                if (it.length == 8) append("발급일(유효)+20 ")
            }
            if (!info.address.isNullOrEmpty()) append("주소+10 ")
            if (fullText.length > 50) append("텍스트량+5 ")
            if (fullText.length > 100) append("텍스트량추가+5 ")
        }
        Log.d(TAG, breakdown)
        Log.d(TAG, "----------------------------------------")
    }

    private fun selectBestAndCallback(
        results: List<OcrResult>,
        images: List<Bitmap>,
        callback: Callback
    ) {
        images.forEach { if (!it.isRecycled) it.recycle() }

        val sortedResults = results.sortedWith(
            compareByDescending<OcrResult> { it.score }
                .thenBy { PIPELINE_PRIORITY[it.pipelineIndex] ?: 99 }
        )

        val best = sortedResults.firstOrNull()

        Log.d(TAG, "========================================")
        Log.d(TAG, "★ OCR 완료: 총 ${results.size}개 파이프라인 처리")
        Log.d(TAG, "========================================")

        sortedResults.forEachIndexed { rank, result ->
            val pipelineName = PIPELINE_NAMES.getOrElse(result.pipelineIndex) { "파이프라인${result.pipelineIndex}" }
            val priority = PIPELINE_PRIORITY[result.pipelineIndex] ?: 99
            val marker = if (rank == 0) "👑" else "  "
            Log.d(TAG, "$marker ${rank + 1}위: [$pipelineName] 점수=${result.score} (우선순위:$priority)")
        }

        val topScore = best?.score ?: 0
        val tiedResults = sortedResults.filter { it.score == topScore }
        if (tiedResults.size > 1) {
            Log.d(TAG, "⚠️ 동점 ${tiedResults.size}개 - 파이프라인 우선순위로 선택")
            tiedResults.forEach {
                val name = PIPELINE_NAMES.getOrElse(it.pipelineIndex) { "?" }
                Log.d(TAG, "   - [$name] 우선순위=${PIPELINE_PRIORITY[it.pipelineIndex]}")
            }
        }
        Log.d(TAG, "========================================")

        if (best == null || best.score < MIN_VALID_SCORE) {
            Log.w(TAG, "❌ 유효한 OCR 결과 없음 (최고점수: ${best?.score ?: 0}, 최소요구: $MIN_VALID_SCORE)")
            callback.onFailure("인식 실패. 다시 시도하세요.")
            return
        }

        val bestPipelineName = PIPELINE_NAMES.getOrElse(best.pipelineIndex) { "파이프라인${best.pipelineIndex}" }
        Log.d(TAG, "✅ 최종 선택: [$bestPipelineName] 점수=${best.score}")
        Log.d(TAG, "   이름=${best.extractedInfo.name}, 주민번호=${best.extractedInfo.residentNumber}")

        val idCardInfo = extractor.applyToIdCardInfo(best.extractedInfo)

        callback.onSuccess(idCardInfo, best.fullText)
    }

    private fun calculateScore(info: IdCardInfoExtractor.ExtractedInfo, text: String): Int {
        var score = 0

        info.name?.let {
            if (it.matches(Regex("^[가-힣]{2,4}$"))) score += 30
        }

        info.residentNumber?.let {
            if (it.length == 13 && it.matches(Regex("^\\d{6}[1-4]\\d{6}$"))) {
                score += 40
            }
        }

        info.driverLicenseNumber?.let {
            if (it.length == 12) score += 35
        }

        info.issueDate?.let {
            if (it.length == 8) score += 20
        }

        if (!info.address.isNullOrEmpty()) score += 10

        if (text.length > 50) score += 5
        if (text.length > 100) score += 5

        return score
    }

    fun close() {
        textRecognizer.close()
    }
}