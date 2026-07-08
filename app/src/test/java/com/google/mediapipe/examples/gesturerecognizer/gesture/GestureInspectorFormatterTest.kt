package com.google.mediapipe.examples.gesturerecognizer.gesture

import com.google.mediapipe.examples.gesturerecognizer.control.GestureAction
import com.google.mediapipe.examples.gesturerecognizer.control.GestureActionEvent
import org.junit.Assert.assertTrue
import org.junit.Test

class GestureInspectorFormatterTest {
    @Test
    fun formatsNoHandState() {
        val display = GestureInspectorFormatter.format(
            snapshot = GestureInspectorSnapshot(
                activePresetName = "Inspector Demo",
                frameSet = GestureFrameSet.empty(),
                interactions = emptyList(),
                actionEvents = emptyList(),
                lastAction = null,
            ),
            inferenceTimeMs = 12L,
        )

        assertTrue(display.summary.contains("Status: no hand"))
        assertTrue(display.handDetails.contains("No hands detected"))
        assertTrue(display.matchedAction.contains("Matched action: None"))
    }

    @Test
    fun formatsTopCandidatesAndMatchedAction() {
        val interaction = interaction()
        val event = GestureActionEvent(
            action = GestureAction(
                id = "action.open_palm_still",
                label = "Open palm still",
            ),
            bindingId = "open-palm-still",
            interaction = interaction,
        )
        val display = GestureInspectorFormatter.format(
            snapshot = GestureInspectorSnapshot(
                activePresetName = "Inspector Demo",
                frameSet = GestureFrameSet(
                    timestampMs = 100L,
                    hands = listOf(interaction.frame),
                ),
                interactions = listOf(interaction),
                actionEvents = listOf(event),
                lastAction = event,
            ),
            inferenceTimeMs = 9L,
        )

        assertTrue(display.summary.contains("Hands: 1"))
        assertTrue(display.matchedAction.contains("Open palm still"))
        assertTrue(display.handDetails.contains("Top [1. Open Palm 90%"))
        assertTrue(display.handDetails.contains("Binding open-palm-still"))
    }

    private fun interaction(): GestureInteraction = GestureInteraction(
        frame = HandGestureFrame(
            handIndex = 0,
            handedness = "Right",
            handednessScore = 0.94f,
            candidates = listOf(
                GestureCandidate("Open_Palm", 0.90f, 1),
                GestureCandidate("Victory", 0.07f, 2),
                GestureCandidate("Closed_Fist", 0.03f, 3),
            ),
            centerX = 0.50f,
            centerY = 0.50f,
            landmarkCount = 21,
        ),
        timestampMs = 100L,
        stableFrames = 3,
        holdDurationMs = 200L,
        rawCenterX = 0.50f,
        rawCenterY = 0.50f,
        smoothedCenterX = 0.50f,
        smoothedCenterY = 0.50f,
        deltaX = 0f,
        deltaY = 0f,
        horizontalZone = HorizontalZone.Center,
        verticalZone = VerticalZone.Middle,
        movementDirection = MovementDirection.Still,
        hasMovedDuringHold = false,
        lostLandmarkFrames = 0,
        isTrackingReliable = true,
    )
}
