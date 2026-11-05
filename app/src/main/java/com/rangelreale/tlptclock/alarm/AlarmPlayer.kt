package com.rangelreale.tlptclock.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import com.rangelreale.tlptclock.settings.AlarmSound
import com.rangelreale.tlptclock.settings.AppSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Plays the level-end alarm as the settings describe.
 *
 * Sound uses the alarm stream, so muting media or the ringer does not silence it. The alarm never
 * lasts longer than [MAX_PLAY_MS], because the system stops a broadcast receiver that runs too long.
 */
object AlarmPlayer {
    const val MAX_PLAY_MS = 8_000L
    private const val TAG = "AlarmPlayer"
    private const val BEEP_MS = 400
    private const val BEEP_GAP_MS = 600L

    private val alarmAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    suspend fun play(context: Context, settings: AppSettings) = coroutineScope {
        if (settings.vibrate) launch { vibrate(context) }
        withTimeoutOrNull(MAX_PLAY_MS) {
            when (settings.sound) {
                AlarmSound.BEEPS -> beeps(settings.beepCount)
                AlarmSound.DEVICE_SOUND -> deviceSound(context, settings.soundUri)
                AlarmSound.NONE -> Unit
            }
        }
    }

    private suspend fun beeps(count: Int) {
        val tones = try {
            ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME)
        } catch (e: RuntimeException) {
            // The system can refuse a tone generator. The on-screen message still shows.
            Log.w(TAG, "No tone generator available", e)
            return
        }
        try {
            repeat(count) {
                tones.startTone(ToneGenerator.TONE_CDMA_HIGH_L, BEEP_MS)
                delay(BEEP_GAP_MS)
            }
        } finally {
            tones.release()
        }
    }

    /** Plays the sound once. Without a picked sound it uses the device's default alarm sound. */
    private suspend fun deviceSound(context: Context, picked: Uri?) {
        val uri = picked ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: return
        val player = MediaPlayer()
        try {
            val done = CompletableDeferred<Unit>()
            player.setAudioAttributes(alarmAttributes)
            player.setDataSource(context, uri)
            player.setOnCompletionListener { done.complete(Unit) }
            player.setOnErrorListener { _, _, _ -> done.complete(Unit); true }
            player.prepare()
            player.start()
            done.await()
        } catch (e: Exception) {
            // The sound may have been deleted. Fall back to beeps so the level end is not missed.
            Log.w(TAG, "Cannot play $uri", e)
            beeps(3)
        } finally {
            player.release()
        }
    }

    private suspend fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        val pattern = longArrayOf(0, 500, 300, 500, 300, 500)
        @Suppress("DEPRECATION") // The replacement needs API 33.
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1), alarmAttributes)
        delay(pattern.sum())
    }
}
