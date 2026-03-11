package com.example.smkitdemoapp.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.AttributeSet
import android.view.View
import com.sency.smkit.model.SMKitJoint

/**
 * Overlay that draws the detected skeleton (joints and limbs) on top of the camera preview.
 * Expects [poseData] in video resolution; uses [videoWidth] and [videoHeight] to scale to view size.
 * Pose is drawn using raw coordinates; X is mirrored when [isImageFlipped] (front camera) to match preview.
 */
class SkeletonOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var poseData: Map<SMKitJoint, PointF>? = null
        set(value) {
            field = value
            invalidate()
        }

    var videoWidth: Int = 1
    var videoHeight: Int = 1

    /** True when using front camera; skeleton X is mirrored to match preview. */
    var isImageFlipped: Boolean = false
        set(value) {
            if (field != value) {
                field = value
                invalidate()
            }
        }

    private val jointOuterPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val jointInnerPaint = Paint().apply {
        color = Color.parseColor("#4DD0E1") // teal accent
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val limbPaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF") // semi‑transparent white
        style = Paint.Style.STROKE
        strokeWidth = 6f
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    /** Limb connections for 28-joint skeleton (same order works for extended; missing joints skipped). */
    private val limbs = listOf(
        SMKitJoint.Head to SMKitJoint.Neck,
        SMKitJoint.Neck to SMKitJoint.UpperSpine,
        SMKitJoint.UpperSpine to SMKitJoint.MiddleSpine1,
        SMKitJoint.MiddleSpine1 to SMKitJoint.Hip,
        SMKitJoint.Neck to SMKitJoint.LShoulder,
        SMKitJoint.LShoulder to SMKitJoint.LElbow,
        SMKitJoint.LElbow to SMKitJoint.LWrist,
        SMKitJoint.Neck to SMKitJoint.RShoulder,
        SMKitJoint.RShoulder to SMKitJoint.RElbow,
        SMKitJoint.RElbow to SMKitJoint.RWrist,
        SMKitJoint.Hip to SMKitJoint.LHip,
        SMKitJoint.LHip to SMKitJoint.LKnee,
        SMKitJoint.LKnee to SMKitJoint.LAnkle,
        SMKitJoint.LAnkle to SMKitJoint.LHeel,
        SMKitJoint.LAnkle to SMKitJoint.LBigToe,
        SMKitJoint.Hip to SMKitJoint.RHip,
        SMKitJoint.RHip to SMKitJoint.RKnee,
        SMKitJoint.RKnee to SMKitJoint.RAnkle,
        SMKitJoint.RAnkle to SMKitJoint.RHeel,
        SMKitJoint.RAnkle to SMKitJoint.RBigToe
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pose = poseData ?: return
        if (pose.isEmpty() || width == 0 || height == 0 || videoWidth <= 0 || videoHeight <= 0) return

        val scaleX = width.toFloat() / videoWidth
        val scaleY = height.toFloat() / videoHeight
        val scale = scaleX.coerceAtMost(scaleY)
        val offsetX = (width - videoWidth * scale) / 2f
        val offsetY = (height - videoHeight * scale) / 2f

        fun toView(p: PointF): PointF {
            if (p.x < 0 || p.y < 0) return PointF(-1f, -1f)
            // No flips in image space; use raw coordinates.
            var vx = p.x * scale + offsetX
            val vy = p.y * scale + offsetY
            // Mirror X only for front camera to match preview.
            if (isImageFlipped) vx = width - vx
            return PointF(vx, vy)
        }

        fun valid(p: PointF) = p.x >= 0 && p.y >= 0

        for ((a, b) in limbs) {
            val pa = pose[a]?.let(::toView) ?: continue
            val pb = pose[b]?.let(::toView) ?: continue
            if (valid(pa) && valid(pb)) {
                canvas.drawLine(pa.x, pa.y, pb.x, pb.y, limbPaint)
            }
        }

        val outerRadius = 10f
        val innerRadius = 6f
        for ((_, p) in pose) {
            val q = toView(p)
            if (valid(q)) {
                canvas.drawCircle(q.x, q.y, outerRadius, jointOuterPaint)
                canvas.drawCircle(q.x, q.y, innerRadius, jointInnerPaint)
            }
        }
    }
}
