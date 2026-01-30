package com.example.simpleviolintunerad_free.audio

import kotlin.math.abs
import kotlin.math.log2
import kotlin.math.roundToInt

/**
 * Pitch Detector for frequency detection.
 *
 * This class analyzes audio samples and detects the dominant frequency
 * in the signal. It uses FFT for frequency analysis and provides various
 * methods for pitch calculation.
 *
 * ARCHITECTURE:
 * - Uses FFT from FFT.kt for spectral analysis
 * - Configuration via ViolinTunerConfig
 * - Independent of Android APIs (can also be used for testing)
 */
class PitchDetector(
    private val sampleRate: Int = ViolinTunerConfig.SAMPLE_RATE
) {
    private val fft = FFT()

    /**
     * Result of pitch detection.
     *
     * @param frequency The detected frequency in Hz (0.0 if none detected)
     * @param amplitude The signal amplitude (0.0 - 1.0)
     * @param confidence Confidence value of detection (0.0 - 1.0)
     */
    data class PitchResult(
        val frequency: Double,
        val amplitude: Double,
        val confidence: Double
    ) {
        companion object {
            /** Empty result when no frequency was detected */
            val EMPTY = PitchResult(0.0, 0.0, 0.0)
        }

        /** Checks if a valid frequency was detected */
        val isValid: Boolean
            get() = frequency > 0 && confidence > 0.3
    }

    /**
     * Analyzes audio samples and detects the pitch.
     *
     * @param samples Array of audio samples (normalized to -1.0 to 1.0)
     * @return PitchResult with the detected frequency
     */
    fun detectPitch(samples: DoubleArray): PitchResult {
        if (samples.isEmpty()) return PitchResult.EMPTY

        // Calculate amplitude (RMS)
        val amplitude = calculateRMS(samples)

        // If signal is too quiet, no detection
        if (amplitude < ViolinTunerConfig.MIN_AMPLITUDE_THRESHOLD) {
            return PitchResult.EMPTY
        }

        // Apply Hanning window for better frequency resolution
        val windowedSamples = applyHanningWindow(samples)

        // Prepare FFT
        val n = samples.size
        val real = windowedSamples.copyOf()
        val imag = DoubleArray(n)

        // Perform FFT
        fft.fft(real, imag)

        // Calculate magnitude spectrum
        val magnitudes = fft.magnitude(real, imag)

        // Find dominant frequency
        val result = findDominantFrequency(magnitudes, n)

        return PitchResult(
            frequency = result.first,
            amplitude = amplitude,
            confidence = result.second
        )
    }

    /**
     * Finds the dominant frequency in the spectrum.
     *
     * @param magnitudes Magnitude spectrum from FFT
     * @param n Number of samples
     * @return Pair of (frequency, confidence)
     */
    private fun findDominantFrequency(magnitudes: DoubleArray, n: Int): Pair<Double, Double> {
        val frequencyResolution = sampleRate.toDouble() / n

        // Only search the relevant frequency range
        val minBin = (ViolinTunerConfig.MIN_FREQUENCY / frequencyResolution).roundToInt()
        val maxBin = (ViolinTunerConfig.MAX_FREQUENCY / frequencyResolution).roundToInt()
            .coerceAtMost(n / 2)

        if (minBin >= maxBin) return Pair(0.0, 0.0)

        // Find peak
        var maxMagnitude = 0.0
        var peakBin = minBin

        for (i in minBin until maxBin) {
            if (magnitudes[i] > maxMagnitude) {
                maxMagnitude = magnitudes[i]
                peakBin = i
            }
        }

        if (maxMagnitude == 0.0) return Pair(0.0, 0.0)

        // Quadratic interpolation for more accurate frequency
        val frequency = interpolatePeak(magnitudes, peakBin, frequencyResolution)

        // Confidence based on peak prominence
        val averageMagnitude = magnitudes.slice(minBin until maxBin).average()
        val confidence = if (averageMagnitude > 0) {
            (maxMagnitude / averageMagnitude / 10.0).coerceIn(0.0, 1.0)
        } else {
            0.0
        }

        return Pair(frequency, confidence)
    }

    /**
     * Quadratic interpolation for more accurate peak detection.
     *
     * Uses the three bins around the peak for sub-bin accuracy.
     */
    private fun interpolatePeak(magnitudes: DoubleArray, peakBin: Int, resolution: Double): Double {
        if (peakBin <= 0 || peakBin >= magnitudes.size - 1) {
            return peakBin * resolution
        }

        val alpha = magnitudes[peakBin - 1]
        val beta = magnitudes[peakBin]
        val gamma = magnitudes[peakBin + 1]

        val denominator = alpha - 2 * beta + gamma
        if (abs(denominator) < 1e-10) {
            return peakBin * resolution
        }

        val p = 0.5 * (alpha - gamma) / denominator
        return (peakBin + p) * resolution
    }

    /**
     * Calculates the RMS (Root Mean Square) value of the signal.
     * This is a measure of volume/amplitude.
     */
    private fun calculateRMS(samples: DoubleArray): Double {
        if (samples.isEmpty()) return 0.0

        var sumSquares = 0.0
        for (sample in samples) {
            sumSquares += sample * sample
        }
        return kotlin.math.sqrt(sumSquares / samples.size)
    }

    companion object {
        /**
         * Calculates the deviation in cents between two frequencies.
         *
         * Cents are a logarithmic unit for musical intervals.
         * 100 cents = 1 semitone
         * 1200 cents = 1 octave
         *
         * @param measured The measured frequency
         * @param target The target frequency
         * @return Deviation in cents (positive = too high, negative = too low)
         */
        fun calculateCents(measured: Double, target: Double): Double {
            if (target <= 0 || measured <= 0) return 0.0
            return 1200.0 * log2(measured / target)
        }

        /**
         * Finds the closest violin string to the measured frequency.
         *
         * @param frequency The measured frequency
         * @return The closest ViolinString or null
         */
        fun findClosestString(frequency: Double): ViolinString? {
            if (frequency <= 0) return null

            return VIOLIN_STRINGS.minByOrNull { string ->
                abs(calculateCents(frequency, string.frequency))
            }
        }
    }
}
