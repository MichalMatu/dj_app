package com.google.mediapipe.examples.gesturerecognizer.dj

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DjGestureMapperTest {
    @Test
    fun oncePerHoldCommandFiresOnlyOnceUntilGestureChanges() {
        val mapper = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-open-palm",
                    gestureName = "Open_Palm",
                    command = DjCommand.PlayPauseDeckA,
                    triggerMode = DjTriggerMode.OncePerHold,
                )
            ),
            stableFramesRequired = 2,
        )

        assertNull(mapper.nextCommand(frame("Open_Palm", 0L)))
        assertEquals(
            DjCommand.PlayPauseDeckA,
            mapper.nextCommand(frame("Open_Palm", 33L))
        )
        assertNull(mapper.nextCommand(frame("Open_Palm", 66L)))
        assertNull(mapper.nextCommand(frame("Open_Palm", 1200L)))

        assertNull(mapper.nextCommand(frame(GestureFrame.NONE, 1233L, 0f)))
        assertNull(mapper.nextCommand(frame("Open_Palm", 1300L)))
        assertEquals(
            DjCommand.PlayPauseDeckA,
            mapper.nextCommand(frame("Open_Palm", 1333L))
        )
    }

    @Test
    fun repeatCommandUsesRepeatIntervalAfterGestureIsStable() {
        val mapper = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-thumb-up",
                    gestureName = "Thumb_Up",
                    command = DjCommand.VolumeUpDeckA,
                    triggerMode = DjTriggerMode.RepeatWhileHeld,
                    repeatIntervalMs = 300L,
                )
            ),
            stableFramesRequired = 2,
        )

        assertNull(mapper.nextCommand(frame("Thumb_Up", 0L)))
        assertEquals(
            DjCommand.VolumeUpDeckA,
            mapper.nextCommand(frame("Thumb_Up", 50L))
        )
        assertNull(mapper.nextCommand(frame("Thumb_Up", 100L)))
        assertNull(mapper.nextCommand(frame("Thumb_Up", 349L)))
        assertEquals(
            DjCommand.VolumeUpDeckA,
            mapper.nextCommand(frame("Thumb_Up", 350L))
        )
    }

    @Test
    fun lowConfidenceFramesDoNotBuildHoldState() {
        val mapper = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-open-palm",
                    gestureName = "Open_Palm",
                    command = DjCommand.PlayPauseDeckA,
                    triggerMode = DjTriggerMode.OncePerHold,
                )
            ),
            stableFramesRequired = 2,
        )

        assertNull(mapper.nextCommand(frame("Open_Palm", 0L, 0.2f)))
        assertNull(mapper.nextCommand(frame("Open_Palm", 33L, 0.2f)))
        assertNull(mapper.nextCommand(frame("Open_Palm", 66L)))
        assertEquals(
            DjCommand.PlayPauseDeckA,
            mapper.nextCommand(frame("Open_Palm", 99L))
        )
    }

    @Test
    fun interactionEngineTracksHoldZoneAndMovement() {
        val engine = GestureInteractionEngine()

        val first = engine.update(frame("Open_Palm", 0L, centerX = 0.20f, centerY = 0.20f))
        val second = engine.update(frame("Open_Palm", 120L, centerX = 0.30f, centerY = 0.20f))

        assertEquals(1, first.stableFrames)
        assertEquals(2, second.stableFrames)
        assertEquals(120L, second.holdDurationMs)
        assertEquals(HorizontalZone.Left, second.horizontalZone)
        assertEquals(VerticalZone.Top, second.verticalZone)
        assertEquals(MovementDirection.Right, second.movementDirection)
        assertTrue(second.deltaX > 0f)
    }

    @Test
    fun mapperCanUseMovementConditionForCombinationGestures() {
        val mapper = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-crossfader-right",
                    gestureName = "Open_Palm",
                    command = DjCommand.CrossfaderRight,
                    triggerMode = DjTriggerMode.ContinuousWhileHeld,
                    movement = MovementDirection.Right,
                    repeatIntervalMs = 0L,
                )
            ),
            stableFramesRequired = 2,
        )

        assertNull(mapper.nextCommand(frame("Open_Palm", 0L, centerX = 0.30f)))
        assertEquals(
            DjCommand.CrossfaderRight,
            mapper.nextCommand(frame("Open_Palm", 50L, centerX = 0.40f))
        )
    }

    @Test
    fun controllerAppliesDeckStateChangesThroughPreviewEngine() {
        val controller = DjGestureController(
            mapper = DjGestureMapper(
                bindings = listOf(
                    DjGestureBinding(
                        id = "test-open-palm",
                        gestureName = "Open_Palm",
                        command = DjCommand.PlayPauseDeckA,
                        triggerMode = DjTriggerMode.OncePerHold,
                    )
                ),
                stableFramesRequired = 2,
            )
        )

        controller.handle(frame("Open_Palm", 0L))
        val playingSnapshot = controller.handle(frame("Open_Palm", 33L))

        assertTrue(playingSnapshot.deckA.isPlaying)
        assertEquals(DjCommand.PlayPauseDeckA, playingSnapshot.lastAction?.command)

        controller.handle(frame(GestureFrame.NONE, 66L, 0f))
        controller.handle(frame("Open_Palm", 600L))
        val pausedSnapshot = controller.handle(frame("Open_Palm", 633L))

        assertFalse(pausedSnapshot.deckA.isPlaying)
    }

    private fun frame(
        name: String,
        timestampMs: Long,
        score: Float = 0.90f,
        centerX: Float = 0.5f,
        centerY: Float = 0.5f,
    ): GestureFrame = GestureFrame(
        name = name,
        score = score,
        timestampMs = timestampMs,
        handCount = if (name == GestureFrame.NONE) 0 else 1,
        centerX = centerX,
        centerY = centerY,
    )
}
