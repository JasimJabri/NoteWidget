package com.note.widgets

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class SatValView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var hue = 0f
        set(value) { field = value; invalidate() }
    var sat = 1f
        private set
    var value = 1f
        private set
    var onColorChanged: (() -> Unit)? = null

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        // White → Hue gradient (horizontal)
        val hueColor = Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
        val hGrad = LinearGradient(0f, 0f, w, 0f, Color.WHITE, hueColor, Shader.TileMode.CLAMP)
        // Transparent → Black gradient (vertical)
        val vGrad = LinearGradient(0f, 0f, 0f, h, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP)

        paint.shader = hGrad
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = vGrad
        canvas.drawRect(0f, 0f, w, h, paint)
        paint.shader = null

        // Draw indicator circle
        val cx = sat * w
        val cy = (1f - value) * h
        circlePaint.color = Color.WHITE
        circlePaint.strokeWidth = 6f
        canvas.drawCircle(cx, cy, 24f, circlePaint)
        circlePaint.color = Color.BLACK
        circlePaint.strokeWidth = 2f
        canvas.drawCircle(cx, cy, 24f, circlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                sat = (event.x / width).coerceIn(0f, 1f)
                value = 1f - (event.y / height).coerceIn(0f, 1f)
                onColorChanged?.invoke()
                invalidate()
                parent.requestDisallowInterceptTouchEvent(true)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    fun setColor(s: Float, v: Float) {
        sat = s
        value = v
        invalidate()
    }
}
