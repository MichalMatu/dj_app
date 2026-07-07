package com.google.mediapipe.examples.gesturerecognizer.dj

import kotlin.math.max
import kotlin.math.min

data class DjDeckState(
    val isPlaying: Boolean = false,
    val cueEnabled: Boolean = false,
    val volume: Int = 75,
    val filter: Int = 0,
    val fxEnabled: Boolean = false,
    val fxMix: Int = 0,
)

data class DjEngineState(
    val deckA: DjDeckState = DjDeckState(),
    val crossfader: Int = 50,
)

data class DjActionEvent(
    val command: DjCommand,
    val label: String,
    val detail: String,
    val timestampMs: Long,
    val bindingId: String,
)

data class DjEngineResult(
    val state: DjEngineState,
    val action: DjActionEvent,
)

interface DjAudioEngine {
    fun apply(
        commandEvent: DjCommandEvent,
        currentState: DjEngineState,
    ): DjEngineResult
}

class PreviewDjAudioEngine : DjAudioEngine {
    override fun apply(
        commandEvent: DjCommandEvent,
        currentState: DjEngineState,
    ): DjEngineResult {
        val interaction = commandEvent.interaction
        val timestampMs = interaction.frame.timestampMs
        val state = currentState

        return when (commandEvent.command) {
            DjCommand.PlayPauseDeckA -> {
                val deck = state.deckA.copy(isPlaying = !state.deckA.isPlaying)
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Play/Pause",
                    detail = if (deck.isPlaying) "Deck A playing" else "Deck A paused",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.CueDeckA -> {
                val deck = state.deckA.copy(cueEnabled = !state.deckA.cueEnabled)
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Cue",
                    detail = if (deck.cueEnabled) "Cue enabled" else "Cue disabled",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.VolumeUpDeckA -> {
                val deck = state.deckA.copy(volume = min(100, state.deckA.volume + 5))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Volume +",
                    detail = "Volume ${deck.volume}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.VolumeDownDeckA -> {
                val deck = state.deckA.copy(volume = max(0, state.deckA.volume - 5))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Volume -",
                    detail = "Volume ${deck.volume}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.FilterUpDeckA -> {
                val deck = state.deckA.copy(filter = min(100, state.deckA.filter + 8))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Filter +",
                    detail = "Filter ${deck.filter}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.FilterDownDeckA -> {
                val deck = state.deckA.copy(filter = max(-100, state.deckA.filter - 8))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A Filter -",
                    detail = "Filter ${deck.filter}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.CrossfaderLeft -> {
                val crossfader = max(0, state.crossfader - movementStep(interaction.deltaX))
                state.copy(crossfader = crossfader).result(
                    commandEvent = commandEvent,
                    label = "Crossfader Left",
                    detail = "Crossfader $crossfader%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.CrossfaderRight -> {
                val crossfader = min(100, state.crossfader + movementStep(interaction.deltaX))
                state.copy(crossfader = crossfader).result(
                    commandEvent = commandEvent,
                    label = "Crossfader Right",
                    detail = "Crossfader $crossfader%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.CrossfaderCenter -> {
                state.copy(crossfader = 50).result(
                    commandEvent = commandEvent,
                    label = "Crossfader Center",
                    detail = "Crossfader 50%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.ToggleFxDeckA -> {
                val deck = state.deckA.copy(fxEnabled = !state.deckA.fxEnabled)
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A FX",
                    detail = if (deck.fxEnabled) "FX enabled" else "FX disabled",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.FxMixUpDeckA -> {
                val deck = state.deckA.copy(fxMix = min(100, state.deckA.fxMix + 8))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A FX Mix +",
                    detail = "FX mix ${deck.fxMix}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.FxMixDownDeckA -> {
                val deck = state.deckA.copy(fxMix = max(0, state.deckA.fxMix - 8))
                state.withDeck(deck).result(
                    commandEvent = commandEvent,
                    label = "Deck A FX Mix -",
                    detail = "FX mix ${deck.fxMix}%",
                    timestampMs = timestampMs,
                )
            }

            DjCommand.ResetDeckA -> {
                DjEngineState().result(
                    commandEvent = commandEvent,
                    label = "Deck A Reset",
                    detail = "Controls reset",
                    timestampMs = timestampMs,
                )
            }
        }
    }

    private fun DjEngineState.withDeck(deck: DjDeckState): DjEngineState = copy(deckA = deck)

    private fun DjEngineState.result(
        commandEvent: DjCommandEvent,
        label: String,
        detail: String,
        timestampMs: Long,
    ): DjEngineResult = DjEngineResult(
        state = this,
        action = DjActionEvent(
            command = commandEvent.command,
            label = label,
            detail = detail,
            timestampMs = timestampMs,
            bindingId = commandEvent.bindingId,
        ),
    )

    private fun movementStep(delta: Float): Int {
        return max(3, min(16, (kotlin.math.abs(delta) * 220f).toInt()))
    }
}
