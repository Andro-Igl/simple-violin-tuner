package com.example.simpleviolintunerad_free.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * FFT (Fast Fourier Transform) implementation for pitch detection.
 *
 * This class uses the Cooley-Tukey FFT algorithm, which is efficient
 * and well-understood. The implementation is completely self-contained
 * without external dependencies, ensuring long-term stability.
 *
 * NOTE: Input size must be a power of 2.
 *
 * Algorithm reference:
 * https://en.wikipedia.org/wiki/Cooley%E2%80%93Tukey_FFT_algorithm
 */
class FFT {

    /**
     * Performs an FFT on the given data.
     *
     * @param real Array of real values (modified in-place)
     * @param imag Array of imaginary values (modified in-place)
     *
     * After execution, real and imag contain the FFT coefficients.
     */
    fun fft(real: DoubleArray, imag: DoubleArray) {
        val n = real.size

        // Check that n is a power of 2
        require(n > 0 && (n and (n - 1)) == 0) {
            "Size must be a power of 2, but was $n"
        }
        require(real.size == imag.size) {
            "Real and imaginary arrays must have the same size"
        }

        // Bit-Reversal Permutation
        bitReversalPermutation(real, imag)

        // Cooley-Tukey iterative FFT
        var size = 2
        while (size <= n) {
            val halfSize = size / 2
            val tableStep = n / size

            var i = 0
            while (i < n) {
                var k = 0
                var j = i
                while (j < i + halfSize) {
                    val angle = -2.0 * PI * k * tableStep / n
                    val cosAngle = cos(angle)
                    val sinAngle = sin(angle)

                    val tReal = real[j + halfSize] * cosAngle - imag[j + halfSize] * sinAngle
                    val tImag = real[j + halfSize] * sinAngle + imag[j + halfSize] * cosAngle

                    real[j + halfSize] = real[j] - tReal
                    imag[j + halfSize] = imag[j] - tImag
                    real[j] = real[j] + tReal
                    imag[j] = imag[j] + tImag

                    k++
                    j++
                }
                i += size
            }
            size *= 2
        }
    }

    /**
     * Calculates the magnitude spectrum from FFT results.
     *
     * @param real Array of real FFT coefficients
     * @param imag Array of imaginary FFT coefficients
     * @return Array of magnitude values
     */
    fun magnitude(real: DoubleArray, imag: DoubleArray): DoubleArray {
        return DoubleArray(real.size) { i ->
            sqrt(real[i] * real[i] + imag[i] * imag[i])
        }
    }

    /**
     * Bit-reversal permutation for FFT.
     * Reorganizes data for the butterfly algorithm.
     */
    private fun bitReversalPermutation(real: DoubleArray, imag: DoubleArray) {
        val n = real.size
        var j = 0

        for (i in 0 until n - 1) {
            if (i < j) {
                // Swap real[i] with real[j]
                var temp = real[i]
                real[i] = real[j]
                real[j] = temp

                // Swap imag[i] with imag[j]
                temp = imag[i]
                imag[i] = imag[j]
                imag[j] = temp
            }

            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }
    }
}

/**
 * Applies a Hanning window to the signal.
 *
 * The window reduces spectral leakage and improves
 * frequency resolution.
 *
 * @param signal The signal to be windowed
 * @return The windowed signal
 */
fun applyHanningWindow(signal: DoubleArray): DoubleArray {
    val n = signal.size
    return DoubleArray(n) { i ->
        val window = 0.5 * (1 - cos(2.0 * PI * i / (n - 1)))
        signal[i] * window
    }
}
