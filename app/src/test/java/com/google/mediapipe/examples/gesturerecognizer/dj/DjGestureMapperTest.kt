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

        val first = engine.update(frame("Open_Palm", 0L, centerX = 0.10f, centerY = 0.20f))
        val second = engine.update(frame("Open_Palm", 120L, centerX = 0.26f, centerY = 0.20f))

        assertEquals(1, first.stableFrames)
        assertEquals(2, second.stableFrames)
        assertEquals(120L, second.holdDurationMs)
        assertEquals(HorizontalZone.Left, second.horizontalZone)
        assertEquals(VerticalZone.Top, second.verticalZone)
        assertEquals(MovementDirection.Right, second.movementDirection)
        assertTrue(second.hasMovedDuringHold)
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
            mapper.nextCommand(frame("Open_Palm", 50L, centerX = 0.50f))
        )
    }

    @Test
    fun openPalmMovingRightDoesNotFirePlayPauseBeforeCrossfaderRight() {
        val mapper = DjGestureMapper()

        assertNull(mapper.map(frame("Open_Palm", 0L, centerX = 0.20f)).commandEvent)
        assertNull(mapper.map(frame("Open_Palm", 100L, centerX = 0.50f)).commandEvent)
        assertNull(mapper.map(frame("Open_Palm", 200L, centerX = 0.80f)).commandEvent)

        val event = mapper.map(frame("Open_Palm", 300L, centerX = 0.90f)).commandEvent

        assertEquals(DjCommand.CrossfaderRight, event?.command)
        assertEquals("open-palm-crossfader-right", event?.bindingId)
    }

    @Test
    fun victoryMovingUpDoesNotFireCrossfaderCenterBeforeFxMixUp() {
        val mapper = DjGestureMapper()

        assertNull(mapper.map(frame("Victory", 0L, centerY = 0.80f)).commandEvent)
        assertNull(mapper.map(frame("Victory", 100L, centerY = 0.40f)).commandEvent)

        val event = mapper.map(frame("Victory", 200L, centerY = 0.10f)).commandEvent

        assertEquals(DjCommand.FxMixUpDeckA, event?.command)
        assertEquals("victory-fx-mix-up", event?.bindingId)
    }

    @Test
    fun victoryMovingDownDoesNotFireCrossfaderCenterBeforeFxMixDown() {
        val mapper = DjGestureMapper()

        assertNull(mapper.map(frame("Victory", 0L, centerY = 0.20f)).commandEvent)
        assertNull(mapper.map(frame("Victory", 100L, centerY = 0.60f)).commandEvent)

        val event = mapper.map(frame("Victory", 200L, centerY = 0.90f)).commandEvent

        assertEquals(DjCommand.FxMixDownDeckA, event?.command)
        assertEquals("victory-fx-mix-down", event?.bindingId)
    }

    @Test
    fun iLoveYouBottomHoldDoesNotToggleFxBeforeReset() {
        val mapper = DjGestureMapper()

        assertNull(mapper.map(frame("ILoveYou", 0L, centerY = 0.85f)).commandEvent)
        assertNull(mapper.map(frame("ILoveYou", 100L, centerY = 0.85f)).commandEvent)
        assertNull(mapper.map(frame("ILoveYou", 200L, centerY = 0.85f)).commandEvent)
        assertNull(mapper.map(frame("ILoveYou", 650L, centerY = 0.85f)).commandEvent)

        val event = mapper.map(frame("ILoveYou", 900L, centerY = 0.85f)).commandEvent

        assertEquals(DjCommand.ResetDeckA, event?.command)
        assertEquals("i-love-you-reset", event?.bindingId)
    }

    @Test
    fun iLoveYouTopOrMiddleShortHoldTogglesFx() {
        val topMapper = DjGestureMapper()

        assertNull(topMapper.map(frame("ILoveYou", 0L, centerY = 0.20f)).commandEvent)
        assertNull(topMapper.map(frame("ILoveYou", 100L, centerY = 0.20f)).commandEvent)
        assertEquals(
            DjCommand.ToggleFxDeckA,
            topMapper.map(frame("ILoveYou", 200L, centerY = 0.20f)).commandEvent?.command
        )

        val middleMapper = DjGestureMapper()

        assertNull(middleMapper.map(frame("ILoveYou", 0L, centerY = 0.50f)).commandEvent)
        assertNull(middleMapper.map(frame("ILoveYou", 100L, centerY = 0.50f)).commandEvent)
        assertEquals(
            DjCommand.ToggleFxDeckA,
            middleMapper.map(frame("ILoveYou", 200L, centerY = 0.50f)).commandEvent?.command
        )
    }

    @Test
    fun maxHoldMsBlocksBindingAfterLimit() {
        fun mapper() = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-short-hold",
                    gestureName = "ILoveYou",
                    command = DjCommand.ToggleFxDeckA,
                    triggerMode = DjTriggerMode.OncePerHold,
                    minHoldMs = 50L,
                    maxHoldMs = 100L,
                )
            ),
            stableFramesRequired = 2,
        )

        val withinLimit = mapper()
        assertNull(withinLimit.nextCommand(frame("ILoveYou", 0L)))
        assertEquals(DjCommand.ToggleFxDeckA, withinLimit.nextCommand(frame("ILoveYou", 100L)))

        val overLimit = mapper()
        assertNull(overLimit.nextCommand(frame("ILoveYou", 0L)))
        assertNull(overLimit.nextCommand(frame("ILoveYou", 101L)))
    }

    @Test
    fun requireNoMovementDuringHoldBlocksBindingAfterMovement() {
        fun mapper() = DjGestureMapper(
            bindings = listOf(
                DjGestureBinding(
                    id = "test-no-movement",
                    gestureName = "Open_Palm",
                    command = DjCommand.PlayPauseDeckA,
                    triggerMode = DjTriggerMode.OncePerHold,
                    requireNoMovementDuringHold = true,
                )
            ),
            stableFramesRequired = 2,
        )

        val movedHold = mapper()
        assertNull(movedHold.nextCommand(frame("Open_Palm", 0L, centerX = 0.20f)))
        assertNull(movedHold.nextCommand(frame("Open_Palm", 50L, centerX = 0.50f)))

        val stillHold = mapper()
        assertNull(stillHold.nextCommand(frame("Open_Palm", 0L, centerX = 0.20f)))
        assertEquals(
            DjCommand.PlayPauseDeckA,
            stillHold.nextCommand(frame("Open_Palm", 50L, centerX = 0.20f))
        )
    }

    @Test
    fun smoothingKeepsSmallJitterStill() {
        val engine = GestureInteractionEngine()
        val frames = listOf(
            frame("Open_Palm", 0L, centerX = 0.50f),
            frame("Open_Palm", 33L, centerX = 0.52f),
            frame("Open_Palm", 66L, centerX = 0.48f),
            frame("Open_Palm", 99L, centerX = 0.51f),
            frame("Open_Palm", 132L, centerX = 0.49f),
        )

        frames.forEach { frame ->
            val interaction = engine.update(frame)
            assertEquals(MovementDirection.Still, interaction.movementDirection)
            assertFalse(interaction.hasMovedDuringHold)
        }
    }

    @Test
    fun missingLandmarksDoNotCreateCatchUpMovement() {
        val engine = GestureInteractionEngine()

        engine.update(frame("Open_Palm", 0L, centerX = 0.20f, centerY = 0.50f))

        val missingLandmarks = engine.update(
            frame("Open_Palm", 33L, centerX = null, centerY = null)
        )
        val landmarksReturned = engine.update(
            frame("Open_Palm", 66L, centerX = 0.90f, centerY = 0.50f)
        )

        assertEquals(MovementDirection.Still, missingLandmarks.movementDirection)
        assertEquals(MovementDirection.Still, landmarksReturned.movementDirection)
        assertFalse(landmarksReturned.hasMovedDuringHold)
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

    @Test
    fun resetDeckARestoresDefaultState() {
        val engine = PreviewDjAudioEngine()
        val changedState = DjEngineState(
            deckA = DjDeckState(
                isPlaying = true,
                cueEnabled = true,
                volume = 20,
                filter = 64,
                fxEnabled = true,
                fxMix = 80,
            ),
            crossfader = 15,
        )

        val result = engine.apply(
            commandEvent = commandEvent(DjCommand.ResetDeckA),
            currentState = changedState,
        )

        assertEquals(DjEngineState(), result.state)
        assertEquals(75, result.state.deckA.volume)
        assertEquals(0, result.state.deckA.filter)
        assertFalse(result.state.deckA.fxEnabled)
        assertEquals(0, result.state.deckA.fxMix)
        assertEquals(50, result.state.crossfader)
    }

    private fun commandEvent(command: DjCommand): DjCommandEvent = DjCommandEvent(
        command = command,
        bindingId = "test-${command.name}",
        interaction = GestureInteraction(
            frame = frame("Test", 0L),
            stableFrames = 1,
            holdDurationMs = 0L,
            deltaX = 0f,
            deltaY = 0f,
            horizontalZone = HorizontalZone.Center,
            verticalZone = VerticalZone.Middle,
            movementDirection = MovementDirection.Still,
            hasMovedDuringHold = false,
        ),
    )

    private fun frame(
        name: String,
        timestampMs: Long,
        score: Float = 0.90f,
        centerX: Float? = 0.5f,
        centerY: Float? = 0.5f,
    ): GestureFrame = GestureFrame(
        name = name,
        score = score,
        timestampMs = timestampMs,
        handCount = if (name == GestureFrame.NONE || centerX == null || centerY == null) 0 else 1,
        centerX = centerX,
        centerY = centerY,
    )
}
