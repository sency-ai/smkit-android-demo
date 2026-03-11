package com.example.smkitdemoapp.skeleton

import android.graphics.PointF
import com.sency.smkit.model.SMKitJoint

/**
 * Skeleton model type. The SDK selects the model automatically based on device capability.
 * - [Standard28]: 28 joints, used on newer devices.
 * - [Extended]: 33 joints (different set and order), used on older devices.
 */
enum class SkeletonType {
    Standard28,
    Extended
}

/** Joints that appear only in the extended skeleton. */
private val extendedOnlyJoints = setOf(
    SMKitJoint.LEyeInner,
    SMKitJoint.LEyeOuter,
    SMKitJoint.REyeInner,
    SMKitJoint.REyeOuter,
    SMKitJoint.LMouth,
    SMKitJoint.RMouth,
    SMKitJoint.LPinky,
    SMKitJoint.RPinky,
    SMKitJoint.LIndex,
    SMKitJoint.RIndex,
    SMKitJoint.LThumb,
    SMKitJoint.RThumb
)

/**
 * Detects which skeleton type is in use from the current pose data.
 * Use this in [handlePositionData] and branch with a when/switch on the result.
 */
fun detectSkeletonType(poseData: Map<SMKitJoint, PointF>?): SkeletonType {
    if (poseData.isNullOrEmpty()) return SkeletonType.Standard28
    return if (poseData.keys.any { it in extendedOnlyJoints }) {
        SkeletonType.Extended
    } else {
        SkeletonType.Standard28
    }
}
