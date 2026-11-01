package com.rangelreale.tlptclock.clock

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import kotlinx.coroutines.delay

/** Plays three beeps on the alarm stream, so that muting media or the ringer does not silence it. */
object LevelEndAlarm {
    suspend fun play() {
        val tones = try {
            ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        } catch (e: RuntimeException) {
            // The system can refuse a tone generator. The on-screen message still shows.
            Log.w("LevelEndAlarm", "No tone generator available", e)
            return
        }
        try {
            repeat(3) {
                tones.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 400)
                delay(600)
            }
        } finally {
            tones.release()
        }
    }
}
