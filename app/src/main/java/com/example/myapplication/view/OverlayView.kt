package com.example.myapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var isDetected = false

    private val guidePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 8f
        pathEffect = DashPathEffect(floatArrayOf(20f, 10f), 0f)
    }

    private val detectedPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    fun setDetectionStatus(detected: Boolean) {
        isDetected = detected
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val cardWidth = width * 0.85f
        val cardHeight = cardWidth / 1.586f

        val left = (width - cardWidth) / 2
        val top = (height - cardHeight) / 2
        val right = left + cardWidth
        val bottom = top + cardHeight

        val cardRect = RectF(left, top, right, bottom)

        val path = Path().apply {
            addRect(0f, 0f, width, height, Path.Direction.CW)
            addRoundRect(cardRect, 20f, 20f, Path.Direction.CCW)
        }
        canvas.drawPath(path, overlayPaint)

        val paint = if (isDetected) detectedPaint else guidePaint
        canvas.drawRoundRect(cardRect, 20f, 20f, paint)

        val cornerLength = 50f
        drawCorners(canvas, cardRect, cornerLength, paint)
    }

    private fun drawCorners(canvas: Canvas, rect: RectF, length: Float, paint: Paint) {
        val cornerPaint = Paint(paint).apply {
            strokeWidth = 12f
            strokeCap = Paint.Cap.ROUND
        }

        canvas.drawLine(rect.left, rect.top, rect.left + length, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + length, cornerPaint)

        canvas.drawLine(rect.right, rect.top, rect.right - length, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + length, cornerPaint)

        canvas.drawLine(rect.left, rect.bottom, rect.left + length, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - length, cornerPaint)

        canvas.drawLine(rect.right, rect.bottom, rect.right - length, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - length, cornerPaint)
    }
}