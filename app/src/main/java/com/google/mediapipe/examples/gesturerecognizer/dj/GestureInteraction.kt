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
) {
    val isStable: Boolean
        get() = stableFrames > 0
}

class GestureInteractionEngine(
    private val minTrackedScore: Float = 0.60f,
    private val movementDeadZone: Float = 0.035f,
) {
    private var activeGestureName: String? = null
    private var stableFrames = 0
    private var holdStartedAtMs = 0L
    private var previousCenterX: Float? = null
    private var previousCenterY: Float? = null

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
            )
        }

        val isSameGesture = activeGestureName == trackedFrame.name
        if (!isSameGesture) {
            activeGestureName = trackedFrame.name
            stableFrames = 1
            holdStartedAtMs = trackedFrame.timestampMs
            previousCenterX = trackedFrame.centerX
            previousCenterY = trackedFrame.centerY

            return trackedFrame.toInteraction(
                stableFrames = stableFrames,
                holdDurationMs = 0L,
                deltaX = 0f,
                deltaY = 0f,
                movementDirection = MovementDirection.Still,
            )
        }

        stableFrames += 1

        val deltaX = delta(trackedFrame.centerX, previousCenterX)
        val deltaY = delta(trackedFrame.centerY, previousCenterY)
        previousCenterX = trackedFrame.centerX
        previousCenterY = trackedFrame.centerY

        return trackedFrame.toInteraction(
            stableFrames = stableFrames,
            holdDurationMs = trackedFrame.timestampMs - holdStartedAtMs,
            deltaX = deltaX,
            deltaY = deltaY,
            movementDirection = movementDirection(deltaX, deltaY),
        )
    }

    fun reset() {
        activeGestureName = null
        stableFrames = 0
        holdStartedAtMs = 0L
        previousCenterX = null
        previousCenterY = null
    }

    private fun delta(current: Float?, previous: Float?): Float {
        return if (current == null || previous == null) 0f else current - previous
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
    ): GestureInteraction = GestureInteraction(
        frame = this,
        stableFrames = stableFrames,
        holdDurationMs = holdDurationMs,
        deltaX = deltaX,
        deltaY = deltaY,
        horizontalZone = centerX?.toHorizontalZone(),
        verticalZone = centerY?.toVerticalZone(),
        movementDirection = movementDirection,
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
