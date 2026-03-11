package com.example.smkitdemoapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.core.content.ContextCompat
import com.example.smkitdemoapp.R
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * ROM (range of motion) gauge mirroring the Flutter/iOS demo: semi-circle arc with
 * green target zone and needle; shows current value as 0–100%.
 */
class RomGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** Current ROM value, typically 0f–1f. */
    var value: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    /** Target zone lower bound (0f–1f). */
    var rangeMin: Float = 0f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    /** Target zone upper bound (0f–1f). */
    var rangeMax: Float = 1f
        set(v) {
            field = v.coerceIn(0f, 1f)
            invalidate()
        }

    /** When true, percentage text is green when in range. */
    var isInPosition: Boolean = false
        set(v) {
            if (field != v) {
                field = v
                invalidate()
            }
        }

    private val strokeWidth = 30f
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@RomGaugeView.strokeWidth
        strokeCap = Paint.Cap.ROUND
        color = Color.argb(0x40, 0xFF, 0xFF, 0xFF)
    }
    private val zonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = this@RomGaugeView.strokeWidth
        strokeCap = Paint.Cap.ROUND
    }
    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        color = Color.argb(0xB3, 0xFF, 0xFF, 0xFF)
    }
    private val arcRect = RectF()

    private fun normalize(v: Float) = 0.5f * v.coerceIn(0f, 1f) + 0.5f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = (140 * resources.displayMetrics.density).toInt()
        setMeasuredDimension(size, (100 * resources.displayMetrics.density).toInt())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val centerX = w / 2
        val radius = (min(w, h * 1.4f) / 2) - strokeWidth / 2
        val centerY = radius + strokeWidth / 2

        arcRect.set(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius
        )

        // Background arc: bottom half (180° to 360°)
        canvas.drawArc(arcRect, 180f, 180f, false, backgroundPaint)

        val normMin = normalize(rangeMin)
        val normMax = normalize(rangeMax)
        if (normMax > normMin) {
            zonePaint.shader = LinearGradient(
                0f, 0f, w, 0f,
                ContextCompat.getColor(context, R.color.green),
                ContextCompat.getColor(context, R.color.green).let { c ->
                    Color.argb(0x99, Color.red(c), Color.green(c), Color.blue(c))
                },
                Shader.TileMode.CLAMP
            )
            // Map normalized 0.5..1.0 to arc 180°..360°
            val rangeStartDeg = 360f * normMin
            val rangeSweepDeg = 360f * (normMax - normMin).coerceIn(0f, 1f)
            canvas.drawArc(arcRect, rangeStartDeg, rangeSweepDeg, false, zonePaint)
            zonePaint.shader = null
        }

        // Needle: value 0 -> left (180°), value 1 -> right (0°)
        val needleAngleDeg = 180f - value.coerceIn(0f, 1f) * 180f
        val needleAngleRad = (needleAngleDeg * PI / 180).toFloat()
        val needleLen = radius * 0.85f
        val needleEndX = centerX + needleLen * cos(needleAngleRad)
        val needleEndY = centerY + needleLen * sin(needleAngleRad)
        canvas.drawLine(centerX, centerY, needleEndX, needleEndY, needlePaint)

        // Percentage and "ROM" label below center
        val normalizedValue = normalize(value)
        val isInRange = normalizedValue >= normalize(rangeMin) && normalizedValue <= normalize(rangeMax)
        textPaint.color = if (isInRange && isInPosition) ContextCompat.getColor(context, R.color.green)
        else Color.WHITE
        textPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 18f, resources.displayMetrics)
        labelPaint.textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 10f, resources.displayMetrics)
        val percentText = "${(value.coerceIn(0f, 1f) * 100).toInt()}%"
        val textY = centerY + 24
        canvas.drawText(percentText, centerX, textY, textPaint)
        canvas.drawText("ROM", centerX, textY + 18, labelPaint)
    }
}
