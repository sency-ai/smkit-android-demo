package com.example.smkitdemoapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Overlay that darkens the camera preview and shows a clear guide rectangle (bounding box)
 * for body calibration. Border is white when body is not in frame, green when in frame.
 * Expects [guideRect] in video coordinates; uses [videoWidth] and [videoHeight] to scale to view.
 */
class BodyCalibrationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var guideRect: RectF? = null
        set(value) {
            field = value
            invalidate()
        }

    var videoWidth: Int = 1
    var videoHeight: Int = 1

    var inPosition: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val borderPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rect = guideRect ?: return
        if (width == 0 || height == 0 || videoWidth <= 0 || videoHeight <= 0) return

        val scaleX = width.toFloat() / videoWidth
        val scaleY = height.toFloat() / videoHeight
        val scale = scaleX.coerceAtMost(scaleY)
        val offsetX = (width - videoWidth * scale) / 2f
        val offsetY = (height - videoHeight * scale) / 2f

        val viewRect = RectF(
            rect.left * scale + offsetX,
            rect.top * scale + offsetY,
            rect.right * scale + offsetX,
            rect.bottom * scale + offsetY
        )

        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRoundRect(viewRect, 12f, 12f, Path.Direction.CCW)
        }
        canvas.drawPath(path, dimPaint)

        borderPaint.color = if (inPosition) Color.GREEN else Color.WHITE
        canvas.drawRoundRect(viewRect, 12f, 12f, borderPaint)
    }
}
