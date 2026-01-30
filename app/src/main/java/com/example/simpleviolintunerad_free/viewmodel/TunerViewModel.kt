package com.example.simpleviolintunerad_free.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.simpleviolintunerad_free.audio.AudioRecorder
import com.example.simpleviolintunerad_free.audio.PitchDetector
import com.example.simpleviolintunerad_free.audio.ViolinString
import com.example.simpleviolintunerad_free.audio.ViolinTunerConfig
import com.example.simpleviolintunerad_free.data.SettingsRepository
import com.example.simpleviolintunerad_free.ui.components.FrequencySettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * ViewModel for the Violin Tuner.
 *
 * Manages tuner state and coordinates audio analysis.
 * Uses StateFlow for reactive UI updates.
 *
 * ARCHITECTURE:
 * - Separation of UI and logic
 * - Lifecycle-aware (stops automatically on onCleared)
 * - Configurable smoothing algorithm
 * - Custom frequency settings (persistently saved)
 */
class TunerViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder(application)
    private val pitchDetector = PitchDetector()
    private val settingsRepository = SettingsRepository(application)

    // Internal states
    private val _tunerState = MutableStateFlow(TunerState())
    private val _isListening = MutableStateFlow(false)
    private val _hasPermission = MutableStateFlow(audioRecorder.hasPermission())
    private val _selectedString = MutableStateFlow<ViolinString?>(null)
    private val _frequencySettings = MutableStateFlow(settingsRepository.loadFrequencySettings())

    // Public states
    val tunerState: StateFlow<TunerState> = _tunerState.asStateFlow()
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()
    val hasPermission: StateFlow<Boolean> = _hasPermission.asStateFlow()
    val selectedString: StateFlow<ViolinString?> = _selectedString.asStateFlow()
    val frequencySettings: StateFlow<FrequencySettings> = _frequencySettings.asStateFlow()

    /** List of all available strings - dynamically generated from settings */
    val availableStrings: List<ViolinString>
        get() = getStringsFromSettings(_frequencySettings.value)

    // For smoothing measurements
    private val recentMeasurements = mutableListOf<Double>()

    private var listeningJob: Job? = null

    /**
     * Starts listening/pitch detection.
     */
    fun startListening() {
        if (listeningJob?.isActive == true) return

        listeningJob = viewModelScope.launch {
            _isListening.value = true

            try {
                audioRecorder.audioFlow().collect { samples ->
                    val result = pitchDetector.detectPitch(samples)
                    processResult(result)

                    // Short pause for UI updates
                    delay(ViolinTunerConfig.UI_UPDATE_INTERVAL_MS)
                }
            } catch (e: CancellationException) {
                // Normal stop - no error message needed
                _isListening.value = false
            } catch (e: SecurityException) {
                _hasPermission.value = false
                _isListening.value = false
            } catch (e: Exception) {
                _tunerState.value = _tunerState.value.copy(
                    error = "Audio recording error: ${e.message}"
                )
                _isListening.value = false
            }
        }
    }

    /**
     * Stops listening.
     */
    fun stopListening() {
        listeningJob?.cancel()
        listeningJob = null
        _isListening.value = false
        recentMeasurements.clear()
        // Reset state and clear error
        _tunerState.value = TunerState()
    }

    /**
     * Sets the selected string (for manual mode).
     * If null, the closest string is automatically detected.
     */
    fun selectString(string: ViolinString?) {
        _selectedString.value = string
    }

    /**
     * Updates the permission status.
     */
    fun updatePermissionStatus() {
        _hasPermission.value = audioRecorder.hasPermission()
    }

    /**
     * Updates frequency settings and saves them persistently.
     */
    fun updateFrequencySettings(settings: FrequencySettings) {
        _frequencySettings.value = settings
        // Save persistently
        settingsRepository.saveFrequencySettings(settings)
        // Reset selected string as frequencies have changed
        _selectedString.value = null
    }

    /**
     * Creates the string list from current frequency settings.
     */
    private fun getStringsFromSettings(settings: FrequencySettings): List<ViolinString> {
        return listOf(
            ViolinString("G", settings.gFrequency, "G (${formatHz(settings.gFrequency)})"),
            ViolinString("D", settings.dFrequency, "D (${formatHz(settings.dFrequency)})"),
            ViolinString("A", settings.aFrequency, "A (${formatHz(settings.aFrequency)})"),
            ViolinString("E", settings.eFrequency, "E (${formatHz(settings.eFrequency)})")
        )
    }

    /**
     * Formats a frequency for display.
     */
    private fun formatHz(freq: Double): String {
        return if (freq == freq.toLong().toDouble()) {
            "${freq.toLong()} Hz"
        } else {
            String.format(java.util.Locale.US, "%.1f Hz", freq)
        }
    }

    /**
     * Processes a pitch detection result.
     */
    private fun processResult(result: PitchDetector.PitchResult) {
        if (!result.isValid) {
            // No valid signal - show "waiting" status
            _tunerState.value = TunerState(
                isActive = false,
                note = "-",
                frequency = 0.0,
                cents = 0.0,
                tuningStatus = TuningStatus.NO_SIGNAL
            )
            recentMeasurements.clear()
            return
        }

        // Apply smoothing
        val smoothedFrequency = smoothFrequency(result.frequency)

        // Determine target string (from current settings)
        val currentStrings = getStringsFromSettings(_frequencySettings.value)
        val targetString = _selectedString.value
            ?: findClosestStringFromList(smoothedFrequency, currentStrings)

        if (targetString == null) {
            _tunerState.value = TunerState(
                isActive = true,
                frequency = smoothedFrequency,
                tuningStatus = TuningStatus.NO_SIGNAL
            )
            return
        }

        // Calculate deviation
        val cents = PitchDetector.calculateCents(smoothedFrequency, targetString.frequency)
        val status = determineTuningStatus(cents)

        _tunerState.value = TunerState(
            isActive = true,
            note = targetString.name,
            frequency = smoothedFrequency,
            targetFrequency = targetString.frequency,
            cents = cents,
            tuningStatus = status,
            amplitude = result.amplitude,
            error = null
        )
    }

    /**
     * Finds the closest string from a list.
     */
    private fun findClosestStringFromList(frequency: Double, strings: List<ViolinString>): ViolinString? {
        if (frequency <= 0) return null
        return strings.minByOrNull { string ->
            abs(PitchDetector.calculateCents(frequency, string.frequency))
        }
    }

    /**
     * Smooths frequency measurements for more stable display.
     */
    private fun smoothFrequency(frequency: Double): Double {
        recentMeasurements.add(frequency)

        // Keep only the last N measurements
        while (recentMeasurements.size > ViolinTunerConfig.SMOOTHING_SAMPLES) {
            recentMeasurements.removeAt(0)
        }

        // Median filtering for outlier resistance
        return if (recentMeasurements.size >= 3) {
            recentMeasurements.sorted()[recentMeasurements.size / 2]
        } else {
            recentMeasurements.average()
        }
    }

    /**
     * Determines tuning status based on cents deviation.
     */
    private fun determineTuningStatus(cents: Double): TuningStatus {
        val absCents = abs(cents)

        return when {
            absCents <= ViolinTunerConfig.CENTS_TOLERANCE_PERFECT -> TuningStatus.IN_TUNE
            absCents <= ViolinTunerConfig.CENTS_TOLERANCE_GOOD -> {
                if (cents > 0) TuningStatus.SLIGHTLY_SHARP else TuningStatus.SLIGHTLY_FLAT
            }
            absCents <= ViolinTunerConfig.CENTS_TOLERANCE_ACCEPTABLE -> {
                if (cents > 0) TuningStatus.SHARP else TuningStatus.FLAT
            }
            else -> {
                if (cents > 0) TuningStatus.VERY_SHARP else TuningStatus.VERY_FLAT
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}

/**
 * Tuner state.
 *
 * @param isActive Whether a signal is detected
 * @param note Name of detected/selected note
 * @param frequency Measured frequency in Hz
 * @param targetFrequency Target frequency in Hz
 * @param cents Deviation in cents
 * @param tuningStatus Tuning status
 * @param amplitude Signal amplitude (0.0 - 1.0)
 * @param error Error message if present
 */
data class TunerState(
    val isActive: Boolean = false,
    val note: String = "-",
    val frequency: Double = 0.0,
    val targetFrequency: Double = 0.0,
    val cents: Double = 0.0,
    val tuningStatus: TuningStatus = TuningStatus.NO_SIGNAL,
    val amplitude: Double = 0.0,
    val error: String? = null
)

/**
 * Tuning status.
 *
 * CUSTOMIZABLE: Colors and thresholds can be adjusted in the UI.
 */
enum class TuningStatus {
    /** No signal detected */
    NO_SIGNAL,

    /** Perfectly tuned */
    IN_TUNE,

    /** Slightly sharp */
    SLIGHTLY_SHARP,

    /** Slightly flat */
    SLIGHTLY_FLAT,

    /** Sharp */
    SHARP,

    /** Flat */
    FLAT,

    /** Very sharp */
    VERY_SHARP,

    /** Very flat */
    VERY_FLAT
}
