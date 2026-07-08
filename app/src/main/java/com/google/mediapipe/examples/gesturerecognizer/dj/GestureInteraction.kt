package com.google.mediapipe.examples.gesturerecognizer.dj

import kotlin.math.abs

enum class HorizontalZone {
    Left,
    Center,
    Right,
}

enum class VerticalZone {
    Top,
    Middle,
    Bottom,
}

enum class MovementDirection {
    Still,
    Left,
    Right,
    Up,
    Down,
    Any,
}

data class GestureInteraction(
    val frame: GestureFrame,
    val stableFrames: Int,
    val holdDurationMs: Long,
    val deltaX: Float,
    val deltaY: Float,
    val horizontalZone: HorizontalZone?,
    val verticalZone: VerticalZone?,
    val movementDirection: MovementDirection,
    val hasMovedDuringHold: Boolean,
) {
    val isStable: Boolean
        get() = stableFrames > 0
}

class GestureInteractionEngine(
    private val minTrackedScore: Float = 0.60f,
    private val movementDeadZone: Float = 0.035f,
    private val smoothingAlpha: Float = 0.25f,
) {
    private var activeGestureName: String? = null
    private var stableFrames = 0
    private var holdStartedAtMs = 0L
    private var smoothedCenterX: Float? = null
    private var smoothedCenterY: Float? = null
    private var hasMovedDuringHold = false

    fun update(frame: GestureFrame): GestureInteraction {
        val trackedFrame =
            if (frame.name == GestureFrame.NONE || frame.score < minTrackedScore) {
                frame.copy(name = GestureFrame.NONE, score = 0f)
            } else {
                frame
            }

        if (trackedFrame.name == GestureFrame.NONE) {
            reset()
            return trackedFrame.toInteraction(
                stableFrames = 0,
                holdDurationMs = 0L,
                deltaX = 0f,
                deltaY = 0f,
                movementDirection = MovementDirection.Still,
                hasMovedDuringHold = false,
            )
        }

        val isSameGesture = activeGestureName == trackedFrame.name
        if (!isSameGesture) {
            activeGestureName = trackedFrame.name
            stableFrames = 1
            holdStartedAtMs = trackedFrame.timestampMs
            smoothedCenterX = trackedFrame.centerX
            smoothedCenterY = trackedFrame.centerY
            hasMovedDuringHold = false

            return trackedFrame.toInteraction(
                stableFrames = stableFrames,
                holdDurationMs = 0L,
                deltaX = 0f,
                deltaY = 0f,
                movementDirection = MovementDirection.Still,
                hasMovedDuringHold = hasMovedDuringHold,
            )
        }

        stableFrames += 1

        val (nextSmoothedCenterX, deltaX) = smoothedDelta(
            current = trackedFrame.centerX,
            previous = smoothedCenterX,
        )
        val (nextSmoothedCenterY, deltaY) = smoothedDelta(
            current = trackedFrame.centerY,
            previous = smoothedCenterY,
        )
        smoothedCenterX = nextSmoothedCenterX
        smoothedCenterY = nextSmoothedCenterY

        val movementDirection = movementDirection(deltaX, deltaY)
        if (movementDirection != MovementDirection.Still) {
            hasMovedDuringHold = true
        }

        return trackedFrame.toInteraction(
            stableFrames = stableFrames,
            holdDurationMs = trackedFrame.timestampMs - holdStartedAtMs,
            deltaX = deltaX,
            deltaY = deltaY,
            movementDirection = movementDirection,
            hasMovedDuringHold = hasMovedDuringHold,
        )
    }

    fun reset() {
        activeGestureName = null
        stableFrames = 0
        holdStartedAtMs = 0L
        smoothedCenterX = null
        smoothedCenterY = null
        hasMovedDuringHold = false
    }

    private fun smoothedDelta(current: Float?, previous: Float?): Pair<Float?, Float> {
        if (current == null) return null to 0f
        if (previous == null) return current to 0f

        val smoothed = previous + (current - previous) * smoothingAlpha
        return smoothed to (smoothed - previous)
    }

    private fun movementDirection(deltaX: Float, deltaY: Float): MovementDirection {
        val absX = abs(deltaX)
        val absY = abs(deltaY)
        if (absX < movementDeadZone && absY < movementDeadZone) {
            return MovementDirection.Still
        }

        return if (absX >= absY) {
            if (deltaX > 0f) MovementDirection.Right else MovementDirection.Left
        } else {
            if (deltaY > 0f) MovementDirection.Down else MovementDirection.Up
        }
    }

    private fun GestureFrame.toInteraction(
        stableFrames: Int,
        holdDurationMs: Long,
        deltaX: Float,
        deltaY: Float,
        movementDirection: MovementDirection,
        hasMovedDuringHold: Boolean,
    ): GestureInteraction = GestureInteraction(
        frame = this,
        stableFrames = stableFrames,
        holdDurationMs = holdDurationMs,
        deltaX = deltaX,
        deltaY = deltaY,
        horizontalZone = centerX?.toHorizontalZone(),
        verticalZone = centerY?.toVerticalZone(),
        movementDirection = movementDirection,
        hasMovedDuringHold = hasMovedDuringHold,
    )

    private fun Float.toHorizontalZone(): HorizontalZone = when {
        this < 0.33f -> HorizontalZone.Left
        this < 0.66f -> HorizontalZone.Center
        else -> HorizontalZone.Right
    }

    private fun Float.toVerticalZone(): VerticalZone = when {
        this < 0.33f -> VerticalZone.Top
        this < 0.66f -> VerticalZone.Middle
        else -> VerticalZone.Bottom
    }
}
