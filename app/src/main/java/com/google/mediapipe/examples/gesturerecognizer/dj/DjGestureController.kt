package com.google.mediapipe.examples.gesturerecognizer.dj

data class DjControllerSnapshot(
    val interaction: GestureInteraction,
    val engineState: DjEngineState,
    val lastAction: DjActionEvent?,
) {
    val gesture: GestureFrame
        get() = interaction.frame

    val deckA: DjDeckState
        get() = engineState.deckA

    val crossfader: Int
        get() = engineState.crossfader
}

class DjGestureController(
    private val mapper: DjGestureMapper = DjGestureMapper(),
    private val audioEngine: DjAudioEngine = PreviewDjAudioEngine(),
) {
    private var engineState = DjEngineState()
    private var lastAction: DjActionEvent? = null

    fun handle(frame: GestureFrame): DjControllerSnapshot {
        val mapperResult = mapper.map(frame)
        mapperResult.commandEvent?.let { commandEvent ->
            val engineResult = audioEngine.apply(commandEvent, engineState)
            engineState = engineResult.state
            lastAction = engineResult.action
        }

        return DjControllerSnapshot(
            interaction = mapperResult.interaction,
            engineState = engineState,
            lastAction = lastAction,
        )
    }

    fun reset() {
        mapper.reset()
        engineState = DjEngineState()
        lastAction = null
    }
}
