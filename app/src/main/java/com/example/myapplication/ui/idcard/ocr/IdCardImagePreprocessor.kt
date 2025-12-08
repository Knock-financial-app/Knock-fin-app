package com.example.myapplication.ui.idcard.ocr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.util.Log
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo
import kotlin.math.abs

class IdCardImagePreprocessor {

    companion object {
        private const val TAG = "IdCardPreprocessor"
        private var isOpenCvLoaded = false

        init {
            try {
                System.loadLibrary("opencv_java4")
                isOpenCvLoaded = true
                Log.d(TAG, "OpenCV 로드 성공")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "OpenCV 로드 실패: ${e.message}")
                isOpenCvLoaded = false
            }
        }
    }

    /**
     * 여러 전처리 파이프라인을 적용하여 다양한 결과 반환
     * OCR 후 가장 좋은 결과를 선택하도록 함
     */
    fun preprocess(bitmap: Bitmap): List<Bitmap> {
        if (!isOpenCvLoaded) {
            Log.w(TAG, "OpenCV 미로드 - 폴백 전처리 사용")
            return fallbackPreprocess(bitmap)
        }

        val results = mutableListOf<Bitmap>()

        try {
            val src = Mat()
            Utils.bitmapToMat(bitmap, src)

            // 원본도 포함 (전처리 없이도 잘 되는 경우 대비)
            results.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))

            // 1. 기본 파이프라인
            results.add(basicPipeline(src.clone()))

            // 2. 고대비 파이프라인 (어두운 이미지)
            results.add(highContrastPipeline(src.clone()))

            // 3. 반사광 제거 파이프라인
            results.add(antiGlarePipeline(src.clone()))

            // 4. 적응형 이진화 파이프라인
            results.add(adaptiveThresholdPipeline(src.clone()))

            src.release()

        } catch (e: Exception) {
            Log.e(TAG, "OpenCV 전처리 실패: ${e.message}", e)
            return fallbackPreprocess(bitmap)
        }

        return results
    }

    /**
     * 기본 파이프라인: 일반적인 상황에 효과적
     */
    private fun basicPipeline(src: Mat): Bitmap {
        try {
            // 1. 최소 해상도 보장
            val resized = ensureMinResolution(src, 1200)

            // 2. 기울기 보정
            val deskewed = deskew(resized)

            // 3. 그레이스케일
            val gray = Mat()
            Imgproc.cvtColor(deskewed, gray, Imgproc.COLOR_BGR2GRAY)

            // 4. 노이즈 제거
            val denoised = Mat()
            Photo.fastNlMeansDenoising(gray, denoised, 10f, 7, 21)

            // 5. CLAHE (적응형 히스토그램 평활화)
            val clahe = Imgproc.createCLAHE(2.0, Size(8.0, 8.0))
            val enhanced = Mat()
            clahe.apply(denoised, enhanced)

            // 6. 언샤프 마스킹
            val sharpened = unsharpMask(enhanced, 1.0, 1.5)

            val result = matToBitmap(sharpened)

            // 메모리 해제
            if (resized != src) resized.release()
            if (deskewed != resized) deskewed.release()
            gray.release()
            denoised.release()
            enhanced.release()
            sharpened.release()

            return result

        } catch (e: Exception) {
            Log.e(TAG, "basicPipeline 실패: ${e.message}")
            return matToBitmap(src)
        }
    }

    /**
     * 고대비 파이프라인: 어둡거나 흐린 이미지
     */
    private fun highContrastPipeline(src: Mat): Bitmap {
        try {
            val resized = ensureMinResolution(src, 1200)
            val deskewed = deskew(resized)

            val gray = Mat()
            Imgproc.cvtColor(deskewed, gray, Imgproc.COLOR_BGR2GRAY)

            // 강한 CLAHE
            val clahe = Imgproc.createCLAHE(4.0, Size(8.0, 8.0))
            val enhanced = Mat()
            clahe.apply(gray, enhanced)

            // 감마 보정 (밝게)
            val gammaCorrected = gammaCorrection(enhanced, 1.3)

            // 샤프닝
            val sharpened = unsharpMask(gammaCorrected, 1.0, 2.0)

            val result = matToBitmap(sharpened)

            if (resized != src) resized.release()
            if (deskewed != resized) deskewed.release()
            gray.release()
            enhanced.release()
            gammaCorrected.release()
            sharpened.release()

            return result

        } catch (e: Exception) {
            Log.e(TAG, "highContrastPipeline 실패: ${e.message}")
            return matToBitmap(src)
        }
    }

    /**
     * 반사광 제거 파이프라인: 광택 있는 신분증
     */
    private fun antiGlarePipeline(src: Mat): Bitmap {
        try {
            val resized = ensureMinResolution(src, 1200)

            // LAB 색공간 변환
            val lab = Mat()
            Imgproc.cvtColor(resized, lab, Imgproc.COLOR_BGR2Lab)

            val channels = mutableListOf<Mat>()
            Core.split(lab, channels)

            // L 채널에 CLAHE 적용
            val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
            clahe.apply(channels[0], channels[0])

            Core.merge(channels, lab)

            val bgr = Mat()
            Imgproc.cvtColor(lab, bgr, Imgproc.COLOR_Lab2BGR)

            val gray = Mat()
            Imgproc.cvtColor(bgr, gray, Imgproc.COLOR_BGR2GRAY)

            // 배경 제거로 조명 균일화
            val normalized = removeBackground(gray)

            val result = matToBitmap(normalized)

            if (resized != src) resized.release()
            lab.release()
            channels.forEach { it.release() }
            bgr.release()
            gray.release()
            normalized.release()

            return result

        } catch (e: Exception) {
            Log.e(TAG, "antiGlarePipeline 실패: ${e.message}")
            return matToBitmap(src)
        }
    }

    /**
     * 적응형 이진화 파이프라인: 조명이 불균일한 경우
     */
    private fun adaptiveThresholdPipeline(src: Mat): Bitmap {
        try {
            val resized = ensureMinResolution(src, 1200)
            val deskewed = deskew(resized)

            val gray = Mat()
            Imgproc.cvtColor(deskewed, gray, Imgproc.COLOR_BGR2GRAY)

            // 가우시안 블러
            val blurred = Mat()
            Imgproc.GaussianBlur(gray, blurred, Size(3.0, 3.0), 0.0)

            // 적응형 이진화
            val binary = Mat()
            Imgproc.adaptiveThreshold(
                blurred, binary, 255.0,
                Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY,
                21, 10.0
            )

            // 모폴로지 연산으로 노이즈 제거
            val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(2.0, 2.0))
            val cleaned = Mat()
            Imgproc.morphologyEx(binary, cleaned, Imgproc.MORPH_CLOSE, kernel)

            val result = matToBitmap(cleaned)

            if (resized != src) resized.release()
            if (deskewed != resized) deskewed.release()
            gray.release()
            blurred.release()
            binary.release()
            kernel.release()
            cleaned.release()

            return result

        } catch (e: Exception) {
            Log.e(TAG, "adaptiveThresholdPipeline 실패: ${e.message}")
            return matToBitmap(src)
        }
    }

    // ============ 유틸리티 함수들 ============

    private fun ensureMinResolution(src: Mat, minWidth: Int): Mat {
        if (src.cols() >= minWidth) return src

        val scale = minWidth.toDouble() / src.cols()
        val dst = Mat()
        Imgproc.resize(src, dst, Size(), scale, scale, Imgproc.INTER_CUBIC)
        return dst
    }

    private fun deskew(src: Mat): Mat {
        try {
            val gray = Mat()
            if (src.channels() > 1) {
                Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
            } else {
                src.copyTo(gray)
            }

            val edges = Mat()
            Imgproc.Canny(gray, edges, 50.0, 150.0, 3)

            val lines = Mat()
            Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)

            if (lines.rows() == 0) {
                gray.release()
                edges.release()
                lines.release()
                return src
            }

            var angleSum = 0.0
            var count = 0

            for (i in 0 until lines.rows()) {
                val line = lines.get(i, 0)
                val angle = Math.atan2(line[3] - line[1], line[2] - line[0]) * 180 / Math.PI
                if (abs(angle) < 45) {
                    angleSum += angle
                    count++
                }
            }

            gray.release()
            edges.release()
            lines.release()

            if (count == 0 || abs(angleSum / count) < 0.5) return src

            val avgAngle = angleSum / count
            val center = Point(src.cols() / 2.0, src.rows() / 2.0)
            val rotMat = Imgproc.getRotationMatrix2D(center, avgAngle, 1.0)
            val rotated = Mat()
            Imgproc.warpAffine(src, rotated, rotMat, src.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)

            rotMat.release()
            return rotated

        } catch (e: Exception) {
            Log.e(TAG, "deskew 실패: ${e.message}")
            return src
        }
    }

    private fun unsharpMask(src: Mat, sigma: Double, amount: Double): Mat {
        val blurred = Mat()
        Imgproc.GaussianBlur(src, blurred, Size(0.0, 0.0), sigma)

        val sharpened = Mat()
        Core.addWeighted(src, 1.0 + amount, blurred, -amount, 0.0, sharpened)

        blurred.release()
        return sharpened
    }

    private fun gammaCorrection(src: Mat, gamma: Double): Mat {
        val lookUpTable = Mat(1, 256, CvType.CV_8U)
        val data = ByteArray(256)

        for (i in 0..255) {
            data[i] = (Math.pow(i / 255.0, 1.0 / gamma) * 255).toInt().coerceIn(0, 255).toByte()
        }

        lookUpTable.put(0, 0, data)
        val dst = Mat()
        Core.LUT(src, lookUpTable, dst)

        lookUpTable.release()
        return dst
    }

    private fun removeBackground(gray: Mat): Mat {
        val background = Mat()
        Imgproc.GaussianBlur(gray, background, Size(51.0, 51.0), 0.0)

        val normalized = Mat()
        Core.divide(gray, background, normalized, 255.0)

        background.release()
        return normalized
    }

    private fun matToBitmap(mat: Mat): Bitmap {
        val bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888)

        if (mat.channels() == 1) {
            val bgr = Mat()
            Imgproc.cvtColor(mat, bgr, Imgproc.COLOR_GRAY2RGBA)
            Utils.matToBitmap(bgr, bitmap)
            bgr.release()
        } else {
            val rgba = Mat()
            Imgproc.cvtColor(mat, rgba, Imgproc.COLOR_BGR2RGBA)
            Utils.matToBitmap(rgba, bitmap)
            rgba.release()
        }

        return bitmap
    }

    // ============ 폴백 (OpenCV 없이) ============

    private fun fallbackPreprocess(bitmap: Bitmap): List<Bitmap> {
        val results = mutableListOf<Bitmap>()

        // 원본
        results.add(bitmap.copy(Bitmap.Config.ARGB_8888, false))

        // 그레이스케일 + 대비
        results.add(fallbackEnhance(bitmap, contrast = 1.5f, brightness = 10f))

        // 고대비
        results.add(fallbackEnhance(bitmap, contrast = 2.0f, brightness = 20f))

        // 저대비 (너무 밝은 이미지용)
        results.add(fallbackEnhance(bitmap, contrast = 1.2f, brightness = -10f))

        return results
    }

    private fun fallbackEnhance(bitmap: Bitmap, contrast: Float, brightness: Float): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(result)
        val paint = Paint()

        val translate = (-.5f * contrast + .5f) * 255f + brightness
        val colorMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, translate,
                0f, contrast, 0f, 0f, translate,
                0f, 0f, contrast, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            )
        )

        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        return result
    }
}