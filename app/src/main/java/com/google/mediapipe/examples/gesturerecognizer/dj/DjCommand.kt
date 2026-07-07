package com.google.mediapipe.examples.gesturerecognizer.dj

enum class DjCommand {
    PlayPauseDeckA,
    CueDeckA,
    VolumeUpDeckA,
    VolumeDownDeckA,
    FilterDownDeckA,
    FilterUpDeckA,
    CrossfaderLeft,
    CrossfaderRight,
    CrossfaderCenter,
    ToggleFxDeckA,
    FxMixUpDeckA,
    FxMixDownDeckA,
    ResetDeckA,
}

enum class DjTriggerMode {
    OncePerHold,
    RepeatWhileHeld,
    ContinuousWhileHeld,
}

data class DjGestureBinding(
    val id: String,
    val gestureName: String,
    val command: DjCommand,
    val triggerMode: DjTriggerMode,
    val minScore: Float = 0.60f,
    val minHoldMs: Long = 0L,
    val horizontalZones: Set<HorizontalZone>? = null,
    val verticalZones: Set<VerticalZone>? = null,
    val movement: MovementDirection? = null,
    val repeatIntervalMs: Long = 300L,
    val cooldownMs: Long = 500L,
)

data class DjCommandEvent(
    val command: DjCommand,
    val bindingId: String,
    val interaction: GestureInteraction,
)
