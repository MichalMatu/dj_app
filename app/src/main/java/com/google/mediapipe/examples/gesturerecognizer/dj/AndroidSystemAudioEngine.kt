package com.google.mediapipe.examples.gesturerecognizer.dj

import android.content.Context
import android.media.AudioManager

class AndroidSystemAudioEngine(
    context: Context,
    private val adjustSystemVolume: Boolean = false,
    private val delegate: DjAudioEngine = PreviewDjAudioEngine(),
) : DjAudioEngine {
    private val appContext = context.applicationContext
    private val audioManager: AudioManager by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    override fun apply(
        commandEvent: DjCommandEvent,
        currentState: DjEngineState,
    ): DjEngineResult {
        val result = delegate.apply(commandEvent, currentState)
        if (!adjustSystemVolume) return result

        return when (commandEvent.command) {
            DjCommand.VolumeUpDeckA -> result.withSystemVolume(
                direction = AudioManager.ADJUST_RAISE,
            )

            DjCommand.VolumeDownDeckA -> result.withSystemVolume(
                direction = AudioManager.ADJUST_LOWER,
            )

            else -> result
        }
    }

    private fun DjEngineResult.withSystemVolume(direction: Int): DjEngineResult {
        val detail = try {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI,
            )
            "${action.detail} | Phone media ${mediaVolumePercent()}%"
        } catch (error: RuntimeException) {
            "${action.detail} | Phone media unavailable"
        }

        return copy(
            action = action.copy(detail = detail)
        )
    }

    private fun mediaVolumePercent(): Int {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (maxVolume == 0) return 0

        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return (currentVolume * 100f / maxVolume).toInt()
    }
}
