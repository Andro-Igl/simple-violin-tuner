package com.example.simpleviolintunerad_free.audio

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlin.coroutines.coroutineContext

/**
 * Audio Recorder for microphone input.
 *
 * This class wraps the Android AudioRecord API and provides
 * audio samples as a Flow. It is optimized for continuous
 * real-time recording.
 *
 * USAGE:
 * ```
 * val recorder = AudioRecorder(context)
 * recorder.audioFlow().collect { samples ->
 *     // Process audio samples
 * }
 * ```
 *
 * PERMISSIONS:
 * The app requires android.permission.RECORD_AUDIO
 */
class AudioRecorder(private val context: Context) {

    private val sampleRate = ViolinTunerConfig.SAMPLE_RATE
    private val bufferSize = ViolinTunerConfig.BUFFER_SIZE

    // Minimum buffer for AudioRecord (can be larger than ours)
    private val minBufferSize = AudioRecord.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    )

    // Actual buffer (at least as large as required by the system)
    private val actualBufferSize = maxOf(bufferSize, minBufferSize)

    /**
     * Checks if microphone permission has been granted.
     */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Provides a continuous Flow of audio samples.
     *
     * The Flow emits DoubleArrays with normalized samples (-1.0 to 1.0).
     * Recording continues as long as the Flow is being collected.
     *
     * @return Flow of audio sample arrays
     * @throws SecurityException if microphone permission is not granted
     */
    @Suppress("MissingPermission")
    fun audioFlow(): Flow<DoubleArray> = flow {
        if (!hasPermission()) {
            throw SecurityException("Microphone permission not granted")
        }

        val audioRecord = createAudioRecord()

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            throw IllegalStateException("AudioRecord could not be initialized")
        }

        try {
            audioRecord.startRecording()

            val buffer = ShortArray(bufferSize)

            while (coroutineContext.isActive) {
                val readCount = audioRecord.read(buffer, 0, buffer.size)

                if (readCount > 0) {
                    // Convert Short samples to Double (-1.0 to 1.0)
                    val samples = DoubleArray(readCount) { i ->
                        buffer[i].toDouble() / Short.MAX_VALUE
                    }
                    emit(samples)
                }
            }
        } finally {
            audioRecord.stop()
            audioRecord.release()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Creates an AudioRecord instance.
     *
     * Private function for better testability.
     */
    @Suppress("MissingPermission")
    private fun createAudioRecord(): AudioRecord {
        return AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            actualBufferSize * 2 // Short = 2 Bytes
        )
    }

    companion object {
        /**
         * Checks if the device has a microphone.
         */
        fun hasMicrophone(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_MICROPHONE
            )
        }
    }
}
