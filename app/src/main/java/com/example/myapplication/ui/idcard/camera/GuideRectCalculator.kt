package com.example.myapplication.ui.idcard.camera

import android.graphics.RectF
import com.example.myapplication.view.OverlayView
import kotlin.math.abs

class GuideRectCalculator(
    private val cardAspectRatio: Float = 1.585f,
    private val guideMatchThreshold: Float = 0.85f,
    private val stabilityThreshold: Float = 0.08f,
    private val smoothingFactor: Float = 0.3f
) {
    private var smoothedCardRect: RectF? = null
    private var lastDetectedRect: RectF? = null

    data class MatchResult(
        val insideRatio: Float,
        val fillRatio: Float
    )

    data class FrameAnalysis(
        val isValidFrame: Boolean,
        val hapticScore: Float,
        val statusMessage: String,
        val smoothedRect: RectF
    )

    fun analyzeFrame(
        cardRect: RectF,
        bitmapWidth: Int,
        bitmapHeight: Int,
        overlayView: OverlayView
    ): FrameAnalysis {
        val guideRect = getGuideRectInBitmapCoords(bitmapWidth, bitmapHeight, overlayView)
        val smoothedRect = smoothRect(cardRect)

        val hapticScore = calculateHapticScore(smoothedRect, guideRect)
        val matchResult = calculateGuideMatch(smoothedRect, guideRect)

        val isInsideGuide = matchResult.insideRatio >= guideMatchThreshold
        val isStable = isStablePosition(cardRect)
        val isValidFrame = isInsideGuide && matchResult.fillRatio >= 0.80f && isStable

        val statusMessage = when {
            isValidFrame -> "ⓘ 신분증이 중심에 들어왔습니다.\n촬영 중이니 움직이지 마십시오."
            else -> analyzePosition(smoothedRect, guideRect)
        }

        lastDetectedRect = RectF(cardRect)

        return FrameAnalysis(isValidFrame, hapticScore, statusMessage, smoothedRect)
    }

    fun calculateHapticScore(card: RectF, guide: RectF): Float {
        if (card.isEmpty) return 0f

        val intersection = RectF()
        if (!intersection.setIntersect(card, guide)) return 0f

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

        val offsetX = abs(card.centerX() - guide.centerX()) / guide.width()
        val offsetY = abs(card.centerY() - guide.centerY()) / guide.height()
        val centerScore = 1f - (offsetX + offsetY).coerceAtMost(1f)

        return (insideRatio * 0.3f + fillRatio * 0.3f + centerScore * 0.2f + sizeScore * 0.2f)
            .coerceIn(0f, 1f)
    }

    fun calculateGuideMatch(card: RectF, guide: RectF): MatchResult {
        if (card.isEmpty) return MatchResult(0f, 0f)

        val intersection = RectF()
        if (!intersection.setIntersect(card, guide)) return MatchResult(0f, 0f)

        val intersectionArea = intersection.width() * intersection.height()
        val cardArea = card.width() * card.height()
        val guideArea = guide.width() * guide.height()

        return MatchResult(
            if (cardArea > 0) intersectionArea / cardArea else 0f,
            if (guideArea > 0) intersectionArea / guideArea else 0f
        )
    }

    fun getGuideRectInBitmapCoords(
        bitmapWidth: Int,
        bitmapHeight: Int,
        overlayView: OverlayView
    ): RectF {
        val viewGuide = overlayView.getGuideRect()
        val viewWidth = overlayView.width.toFloat()
        val viewHeight = overlayView.height.toFloat()

        if (viewWidth <= 0 || viewHeight <= 0) {
            return calculateDefaultGuideRect(bitmapWidth, bitmapHeight)
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

        return RectF(
            ((viewGuide.left + offsetX) / scale).coerceIn(0f, bitmapWidth.toFloat()),
            ((viewGuide.top + offsetY) / scale).coerceIn(0f, bitmapHeight.toFloat()),
            ((viewGuide.right + offsetX) / scale).coerceIn(0f, bitmapWidth.toFloat()),
            ((viewGuide.bottom + offsetY) / scale).coerceIn(0f, bitmapHeight.toFloat())
        )
    }

    private fun calculateDefaultGuideRect(imageWidth: Int, imageHeight: Int): RectF {
        val guideWidth = imageWidth * 0.85f
        val guideHeight = guideWidth / cardAspectRatio
        val left = (imageWidth - guideWidth) / 2
        val top = (imageHeight - guideHeight) / 2
        return RectF(left, top, left + guideWidth, top + guideHeight)
    }

    fun smoothRect(newRect: RectF): RectF {
        val prevRect = smoothedCardRect

        if (prevRect == null || prevRect.isEmpty) {
            smoothedCardRect = RectF(newRect)
            return newRect
        }

        val centerDiff = abs(newRect.centerX() - prevRect.centerX()) +
                abs(newRect.centerY() - prevRect.centerY())
        val avgSize = (prevRect.width() + prevRect.height()) / 2f

        if (avgSize > 0 && centerDiff / avgSize > 0.3f) {
            smoothedCardRect = RectF(newRect)
            return newRect
        }

        val result = RectF(
            lerp(prevRect.left, newRect.left, smoothingFactor),
            lerp(prevRect.top, newRect.top, smoothingFactor),
            lerp(prevRect.right, newRect.right, smoothingFactor),
            lerp(prevRect.bottom, newRect.bottom, smoothingFactor)
        )
        smoothedCardRect = result
        return result
    }

    private fun isStablePosition(currentRect: RectF): Boolean {
        val lastRect = lastDetectedRect ?: return true
        if (lastRect.isEmpty || currentRect.isEmpty) return true

        val avgSize = (lastRect.width() + lastRect.height()) / 2f
        if (avgSize == 0f) return true

        val centerXDiff = abs(currentRect.centerX() - lastRect.centerX())
        val centerYDiff = abs(currentRect.centerY() - lastRect.centerY())

        return (centerXDiff + centerYDiff) / avgSize < stabilityThreshold
    }

    private fun analyzePosition(cardRect: RectF, guideRect: RectF): String {
        if (cardRect.isEmpty) return "신분증을 비춰주세요"

        val offsetX = (cardRect.centerX() - guideRect.centerX()) / guideRect.width()
        val offsetY = (cardRect.centerY() - guideRect.centerY()) / guideRect.height()
        val sizeRatio = (cardRect.width() * cardRect.height()) /
                (guideRect.width() * guideRect.height())

        val messages = mutableListOf<String>()
        val threshold = 0.10f

        if (offsetX < -threshold) messages.add("왼쪽으로")
        else if (offsetX > threshold) messages.add("오른쪽으로")

        if (offsetY < -threshold) messages.add("위로")
        else if (offsetY > threshold) messages.add("아래로")

        if (sizeRatio < 0.7f) messages.add("더 가까이")
        else if (sizeRatio > 1.2f) messages.add("더 멀리")

        return if (messages.isNotEmpty()) {
            "휴대폰을 ${messages.joinToString(", ")} 이동하세요"
        } else "잘하고 있어요!"
    }

    private fun lerp(start: Float, end: Float, factor: Float) = start + (end - start) * factor

    fun reset() {
        smoothedCardRect = null
        lastDetectedRect = null
    }
}