package com.google.mediapipe.examples.gesturerecognizer.dj

data class DjGestureMapperResult(
    val interaction: GestureInteraction,
    val commandEvent: DjCommandEvent?,
)

class DjGestureMapper(
    private val bindings: List<DjGestureBinding> = DjGestureConfiguration.defaultBindings(),
    private val stableFramesRequired: Int = 3,
    private val interactionEngine: GestureInteractionEngine = GestureInteractionEngine(),
) {
    private val consumedOnceBindings = mutableSetOf<String>()
    private val lastTriggeredAtByBinding = mutableMapOf<String, Long>()
    private var activeGestureName: String? = null

    fun map(frame: GestureFrame): DjGestureMapperResult {
        val interaction = interactionEngine.update(frame)
        if (interaction.frame.name == GestureFrame.NONE) {
            resetHold()
            return DjGestureMapperResult(interaction, null)
        }

        if (activeGestureName != interaction.frame.name) {
            activeGestureName = interaction.frame.name
            consumedOnceBindings.clear()
        }

        val commandEvent = bindings.firstNotNullOfOrNull { binding ->
            if (binding.matches(interaction)) {
                binding.nextEvent(interaction)
            } else {
                null
            }
        }

        return DjGestureMapperResult(interaction, commandEvent)
    }

    fun nextCommand(frame: GestureFrame): DjCommand? = map(frame).commandEvent?.command

    fun reset() {
        interactionEngine.reset()
        resetHold()
        lastTriggeredAtByBinding.clear()
    }

    private fun DjGestureBinding.matches(interaction: GestureInteraction): Boolean {
        if (gestureName != interaction.frame.name) return false
        if (interaction.stableFrames < stableFramesRequired) return false
        if (interaction.frame.score < minScore) return false
        if (interaction.holdDurationMs < minHoldMs) return false
        if (horizontalZones != null && interaction.horizontalZone !in horizontalZones) return false
        if (verticalZones != null && interaction.verticalZone !in verticalZones) return false
        if (movement != null && !movement.matches(interaction.movementDirection)) return false
        return true
    }

    private fun MovementDirection.matches(actual: MovementDirection): Boolean {
        return this == actual || (this == MovementDirection.Any && actual != MovementDirection.Still)
    }

    private fun DjGestureBinding.nextEvent(interaction: GestureInteraction): DjCommandEvent? {
        val lastTriggeredAt = lastTriggeredAtByBinding[id]
        val intervalMs = when (triggerMode) {
            DjTriggerMode.OncePerHold -> cooldownMs
            DjTriggerMode.RepeatWhileHeld -> repeatIntervalMs
            DjTriggerMode.ContinuousWhileHeld -> repeatIntervalMs
        }

        if (lastTriggeredAt != null && interaction.frame.timestampMs - lastTriggeredAt < intervalMs) {
            return null
        }

        if (triggerMode == DjTriggerMode.OncePerHold) {
            if (id in consumedOnceBindings) return null
            consumedOnceBindings.add(id)
        }

        lastTriggeredAtByBinding[id] = interaction.frame.timestampMs
        return DjCommandEvent(
            command = command,
            bindingId = id,
            interaction = interaction,
        )
    }

    private fun resetHold() {
        activeGestureName = null
        consumedOnceBindings.clear()
    }
}
