package com.example.myapplication.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "OverlayView"
        private const val CARD_ASPECT_RATIO = 1.585f
        private const val GUIDE_WIDTH_RATIO = 0.90f
        private const val CORNER_RADIUS = 16f
    }
    private var detectedCorners: List<PointF>? = null
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var detectedRect: Rect? = null
    private var guideColor: Int = Color.WHITE
    private var debugText: String = ""
    private var showDebug: Boolean = true
    private val guidePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val guideCornerPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val detectionPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val detectionFillPaint = Paint().apply {
        color = Color.argb(40, 0, 255, 0)
        style = Paint.Style.FILL
    }

    private val cornerPointPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 40f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        setShadowLayer(4f, 2f, 2f, Color.BLACK)
    }

    private val debugPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 24f
        isAntiAlias = true
        typeface = Typeface.MONOSPACE
    }

    private val debugBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    fun setDetectedRect(rect: Rect, imgWidth: Int, imgHeight: Int, rotation: Int) {
        this.detectedRect = rect
        this.imageWidth = imgWidth
        this.imageHeight = imgHeight
        this.detectedCorners = null

        invalidate()
    }

    fun setDebugText(text: String) {
        this.debugText = text
        invalidate()
    }

    fun clearDetection() {
        detectedCorners = null
        detectedRect = null
        invalidate()
    }

    fun setGuideColor(color: Int) {
        guideColor = color
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val guideRect = calculateGuideRect()

        drawDarkOverlay(canvas, guideRect)
        drawGuideFrame(canvas, guideRect)

        detectedCorners?.let { corners ->
            drawDetectedPolygon(canvas, corners)
        } ?: detectedRect?.let { rect ->
            drawDetectedRect(canvas, rect)
        }

        drawInstructionText(canvas, guideRect)

        if (showDebug) {
            drawDebugInfo(canvas)
        }
    }

    private fun calculateGuideRect(): RectF {
        val guideWidth = width * GUIDE_WIDTH_RATIO
        val guideHeight = guideWidth / CARD_ASPECT_RATIO
        val left = (width - guideWidth) / 2
        val top = (height - guideHeight) / 2
        return RectF(left, top, left + guideWidth, top + guideHeight)
    }

    private fun drawDarkOverlay(canvas: Canvas, guideRect: RectF) {
        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

        val clearPaint = Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        }
        canvas.drawRoundRect(guideRect, CORNER_RADIUS, CORNER_RADIUS, clearPaint)
        canvas.restoreToCount(saveCount)
    }

    private fun drawGuideFrame(canvas: Canvas, guideRect: RectF) {
        guidePaint.color = guideColor
        guideCornerPaint.color = guideColor

        canvas.drawRoundRect(guideRect, CORNER_RADIUS, CORNER_RADIUS, guidePaint)

        val cornerLength = 40f

        canvas.drawLine(guideRect.left, guideRect.top + CORNER_RADIUS,
            guideRect.left, guideRect.top + cornerLength + CORNER_RADIUS, guideCornerPaint)
        canvas.drawLine(guideRect.left + CORNER_RADIUS, guideRect.top,
            guideRect.left + cornerLength + CORNER_RADIUS, guideRect.top, guideCornerPaint)

        canvas.drawLine(guideRect.right, guideRect.top + CORNER_RADIUS,
            guideRect.right, guideRect.top + cornerLength + CORNER_RADIUS, guideCornerPaint)
        canvas.drawLine(guideRect.right - CORNER_RADIUS, guideRect.top,
            guideRect.right - cornerLength - CORNER_RADIUS, guideRect.top, guideCornerPaint)

        canvas.drawLine(guideRect.left, guideRect.bottom - CORNER_RADIUS,
            guideRect.left, guideRect.bottom - cornerLength - CORNER_RADIUS, guideCornerPaint)
        canvas.drawLine(guideRect.left + CORNER_RADIUS, guideRect.bottom,
            guideRect.left + cornerLength + CORNER_RADIUS, guideRect.bottom, guideCornerPaint)

        canvas.drawLine(guideRect.right, guideRect.bottom - CORNER_RADIUS,
            guideRect.right, guideRect.bottom - cornerLength - CORNER_RADIUS, guideCornerPaint)
        canvas.drawLine(guideRect.right - CORNER_RADIUS, guideRect.bottom,
            guideRect.right - cornerLength - CORNER_RADIUS, guideRect.bottom, guideCornerPaint)
    }

    private fun drawDetectedPolygon(canvas: Canvas, corners: List<PointF>) {
        if (imageWidth <= 0 || imageHeight <= 0) return

        val viewCorners = corners.map { transformPoint(it) }

        val path = Path().apply {
            moveTo(viewCorners[0].x, viewCorners[0].y)
            lineTo(viewCorners[1].x, viewCorners[1].y)
            lineTo(viewCorners[2].x, viewCorners[2].y)
            lineTo(viewCorners[3].x, viewCorners[3].y)
            close()
        }

        canvas.drawPath(path, detectionFillPaint)
        canvas.drawPath(path, detectionPaint)

        for (corner in viewCorners) {
            canvas.drawCircle(corner.x, corner.y, 10f, cornerPointPaint)
        }

        if (showDebug) {
            val numberPaint = Paint().apply {
                color = Color.WHITE
                textSize = 20f
                textAlign = Paint.Align.CENTER
            }
            viewCorners.forEachIndexed { index, corner ->
                canvas.drawText("${index + 1}", corner.x, corner.y - 15f, numberPaint)
            }
        }
    }
    private fun drawDetectedRect(canvas: Canvas, imageRect: Rect) {
        if (imageWidth <= 0 || imageHeight <= 0) return

        val viewRect = transformRect(imageRect)

        canvas.drawRoundRect(viewRect, 8f, 8f, detectionFillPaint)
        canvas.drawRoundRect(viewRect, 8f, 8f, detectionPaint)

        canvas.drawCircle(viewRect.left, viewRect.top, 8f, cornerPointPaint)
        canvas.drawCircle(viewRect.right, viewRect.top, 8f, cornerPointPaint)
        canvas.drawCircle(viewRect.left, viewRect.bottom, 8f, cornerPointPaint)
        canvas.drawCircle(viewRect.right, viewRect.bottom, 8f, cornerPointPaint)
    }

    private fun transformPoint(imagePoint: PointF): PointF {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scale = minOf(scaleX, scaleY)

        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f

        return PointF(
            imagePoint.x * scale + offsetX,
            imagePoint.y * scale + offsetY
        )
    }

    private fun transformRect(imageRect: Rect): RectF {
        val scaleX = width.toFloat() / imageWidth
        val scaleY = height.toFloat() / imageHeight
        val scale = minOf(scaleX, scaleY)

        val offsetX = (width - imageWidth * scale) / 2f
        val offsetY = (height - imageHeight * scale) / 2f

        return RectF(
            imageRect.left * scale + offsetX,
            imageRect.top * scale + offsetY,
            imageRect.right * scale + offsetX,
            imageRect.bottom * scale + offsetY
        )
    }

    private fun drawInstructionText(canvas: Canvas, guideRect: RectF) {
        canvas.drawText("신분증을 가이드에 맞춰주세요", width / 2f, guideRect.top - 30f, textPaint)
    }

    private fun drawDebugInfo(canvas: Canvas) {
        val lines = mutableListOf<String>()
        lines.add("뷰: ${width}x${height}")
        lines.add("이미지: ${imageWidth}x${imageHeight}")

        detectedCorners?.let { corners ->
            lines.add("코너 수: ${corners.size}")
        }

        if (debugText.isNotEmpty()) {
            lines.add(debugText)
        }

        val padding = 8f
        val lineHeight = 28f
        val bgHeight = lines.size * lineHeight + padding * 2
        canvas.drawRect(0f, 0f, 450f, bgHeight, debugBgPaint)

        lines.forEachIndexed { index, line ->
            canvas.drawText(line, padding, padding + (index + 1) * lineHeight - 6f, debugPaint)
        }
    }

    fun getGuideRect(): RectF {
        return calculateGuideRect()
    }
}