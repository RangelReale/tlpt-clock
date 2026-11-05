package com.rangelreale.tlptclock.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.rangelreale.tlptclock.settings.AlarmSound
import com.rangelreale.tlptclock.settings.AppSettings
import kotlinx.coroutines.delay

/**
 * Plays the level-end alarm as the settings describe: [AppSettings.ringCount] rings, each a beep or
 * up to [DEVICE_RING_MS] of a device sound, with a vibration if it is on.
 *
 * Sound uses the alarm stream, so muting media or the ringer does not silence it. Cancelling the
 * calling coroutine stops the alarm at once.
 */
object AlarmPlayer {
    private const val TAG = "AlarmPlayer"
    private const val BEEP_MS = 400
    private const val BEEP_RING_MS = 1_000L
    const val DEVICE_RING_MS = 3_000L
    private const val DEVICE_GAP_MS = 700L
    private const val SILENT_RING_MS = 1_000L
    private const val VIBRATE_MS = 500L

    private val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    suspend fun play(context: Context, settings: AppSettings) {
        val vibrator = if (settings.vibrate) context.getSystemService(Vibrator::class.java) else null
        val ring: () -> Unit = { vibrator?.let(::vibrateOnce) }
        try {
            when (settings.sound) {
                AlarmSound.BEEPS -> beeps(settings.ringCount, ring)
                AlarmSound.DEVICE_SOUND -> deviceSound(context, settings, ring)
                AlarmSound.NONE -> if (vibrator != null) {
                    repeat(settings.ringCount) {
                        ring()
                        delay(SILENT_RING_MS)
                    }
                }
            }
        } finally {
            vibrator?.cancel()
        }
    }

    private suspend fun beeps(count: Int, ring: () -> Unit) {
        val tones = try {
            ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        } catch (e: RuntimeException) {
            // The system can refuse a tone generator. The on-screen message still shows.
            Log.w(TAG, "No tone generator available", e)
            return
        }
        try {
            repeat(count) {
                ring()
                tones.startTone(ToneGenerator.TONE_CDMA_HIGH_L, BEEP_MS)
                delay(BEEP_RING_MS)
            }
        } finally {
            tones.release()
        }
    }

    /** Without a picked sound this uses the device's default alarm sound. */
    private suspend fun deviceSound(context: Context, settings: AppSettings, ring: () -> Unit) {
        val uri = settings.soundUri ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val player = MediaPlayer()
        try {
            try {
                player.setAudioAttributes(alarmAttributes)
                player.setDataSource(context, uri!!)
                player.prepare()
            } catch (e: Exception) {
                // The sound may have been deleted. Beep instead so the level end is not missed.
                Log.w(TAG, "Cannot play $uri", e)
                beeps(settings.ringCount, ring)
                return
            }
            val ringMs = player.duration.toLong().takeIf { it > 0 }?.coerceAtMost(DEVICE_RING_MS)
                ?: DEVICE_RING_MS
            repeat(settings.ringCount) { index ->
                ring()
                player.seekTo(0)
                player.start()
                delay(ringMs)
                player.pause()
                if (index < settings.ringCount - 1) delay(DEVICE_GAP_MS)
            }
        } finally {
            player.release()
        }
    }

    private fun vibrateOnce(vibrator: Vibrator) {
        if (!vibrator.hasVibrator()) return
        @Suppress("DEPRECATION") // The replacement needs API 33.
        vibrator.vibrate(VibrationEffect.createOneShot(VIBRATE_MS, 255), alarmAttributes)
    }
}
