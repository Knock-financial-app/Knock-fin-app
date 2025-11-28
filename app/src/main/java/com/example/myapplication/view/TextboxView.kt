package com.example.myapplication.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.text.TextPainter.paint

class TextboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

//    private val paint = Paint().apply {
//        color = Color.WHITE       // 배경 색
//        style = Paint.Style.FILL
//        isAntiAlias = true
//    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#FFE621")    // 테두리 색
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 40f
        isAntiAlias = true
    }

    // 텍스트 저장용 프로퍼티
    private var _text: String = ""

    // getter / setter
    fun getText(): String = _text

    fun setText(value: String) {
        _text = value
        invalidate() // 값 바뀌면 다시 그리기
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 배경 사각형
//        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        // 테두리
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), borderPaint)

        // 텍스트 중앙 정렬
        val x = 20f
        val y = height / 2f - (textPaint.descent() + textPaint.ascent()) / 2
        canvas.drawText(_text, x, y, textPaint)
    }
}
