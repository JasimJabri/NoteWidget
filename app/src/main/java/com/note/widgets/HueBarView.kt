package com.note.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class HueBarView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var hue = 0f
        private set
    var onHueChanged: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.WHITE
        strokeWidth = 5f
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        val colors = IntArray(13) { i -> Color.HSVToColor(floatArrayOf(i * 30f, 1f, 1f)) }
        paint.shader = LinearGradient(0f, 0f, w, 0f, colors, null, Shader.TileMode.CLAMP)
        canvas.drawRoundRect(0f, 0f, w, h, h / 2, h / 2, paint)
        paint.shader = null

        val cx = (hue / 360f) * w
        circlePaint.color = Color.WHITE
        circlePaint.strokeWidth = 5f
        canvas.drawCircle(cx, h / 2, h / 2 - 4f, circlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                hue = ((event.x / width).coerceIn(0f, 1f)) * 360f
                onHueChanged?.invoke()
                invalidate()
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setHue(h: Float) {
        hue = h
        invalidate()
    }
}
