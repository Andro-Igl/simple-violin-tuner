package com.example.simpleviolintunerad_free.audio

/**
 * Configuration for the Violin Tuner.
 *
 * This class contains all configurable parameters for the tuner.
 * Values are defined as companion object constants so they can
 * be easily adjusted without changing code in multiple places.
 *
 * CUSTOMIZABLE:
 * - String frequencies can be changed for alternative tunings
 * - Tolerance for "in tune" display
 * - Audio parameters like sample rate
 */
object ViolinTunerConfig {

    // ===== VIOLIN STRING FREQUENCIES (Standard tuning) =====
    // Frequencies can be adjusted for alternative tunings

    /** G string (lowest string) - Default: 196.00 Hz */
    const val FREQUENCY_G3: Double = 196.00

    /** D string - Default: 293.66 Hz */
    const val FREQUENCY_D4: Double = 293.66

    /** A string - Default: 440.00 Hz (concert pitch) */
    const val FREQUENCY_A4: Double = 440.00

    /** E string (highest string) - Default: 659.26 Hz */
    const val FREQUENCY_E5: Double = 659.26

    // ===== TUNING TOLERANCE =====

    /**
     * Tolerance in cents for the "in tune" display.
     * One semitone = 100 cents
     *
     * CUSTOMIZABLE: Increase for more tolerance, decrease for more precise tuning
     */
    const val CENTS_TOLERANCE_PERFECT: Double = 5.0    // Perfectly tuned (green)
    const val CENTS_TOLERANCE_GOOD: Double = 15.0      // Well tuned (yellow-green)
    const val CENTS_TOLERANCE_ACCEPTABLE: Double = 25.0 // Acceptable (yellow)

    // ===== AUDIO CONFIGURATION =====

    /**
     * Sample rate for audio recording in Hz.
     * 44100 Hz is a standard and works on most devices.
     *
     * NOTE: Changes may affect frequency detection.
     */
    const val SAMPLE_RATE: Int = 44100

    /**
     * Buffer size for audio recording.
     * Larger buffer = better frequency resolution, but more latency
     *
     * Must be a power of 2 for FFT: 2048, 4096, 8192, etc.
     */
    const val BUFFER_SIZE: Int = 8192

    /**
     * Minimum amplitude for pitch detection.
     * Prevents detection on too quiet signals or noise.
     *
     * CUSTOMIZABLE: Increase if too much noise is detected
     */
    const val MIN_AMPLITUDE_THRESHOLD: Double = 0.01

    // ===== FREQUENCY RANGE =====

    /**
     * Minimum detectable frequency in Hz.
     * Slightly below G string for tuning headroom.
     */
    const val MIN_FREQUENCY: Double = 150.0

    /**
     * Maximum detectable frequency in Hz.
     * Slightly above E string for tuning headroom.
     */
    const val MAX_FREQUENCY: Double = 750.0

    // ===== UI CONFIGURATION =====

    /**
     * Update interval for UI in milliseconds.
     * Lower = faster updates, but more CPU load
     */
    const val UI_UPDATE_INTERVAL_MS: Long = 50

    /**
     * Number of recent measurements for smoothing.
     * Higher = more stable display, but slower response
     */
    const val SMOOTHING_SAMPLES: Int = 5
}

/**
 * Data class for a violin string.
 *
 * @param name Display name of the string (e.g. "G", "D", "A", "E")
 * @param frequency Target frequency in Hz
 * @param displayName Full name for display (optional)
 */
data class ViolinString(
    val name: String,
    val frequency: Double,
    val displayName: String = name
)

/**
 * Predefined list of all violin strings.
 *
 * CUSTOMIZABLE: For other instruments or tunings,
 * strings can be added or changed here.
 */
val VIOLIN_STRINGS = listOf(
    ViolinString("G", ViolinTunerConfig.FREQUENCY_G3, "G3 (196 Hz)"),
    ViolinString("D", ViolinTunerConfig.FREQUENCY_D4, "D4 (294 Hz)"),
    ViolinString("A", ViolinTunerConfig.FREQUENCY_A4, "A4 (440 Hz)"),
    ViolinString("E", ViolinTunerConfig.FREQUENCY_E5, "E5 (659 Hz)")
)
