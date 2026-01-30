package com.example.simpleviolintunerad_free.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.simpleviolintunerad_free.audio.ViolinTunerConfig

/**
 * Data class for custom frequency settings.
 *
 * @param gFrequency Frequency for G string in Hz
 * @param dFrequency Frequency for D string in Hz
 * @param aFrequency Frequency for A string (concert pitch) in Hz
 * @param eFrequency Frequency for E string in Hz
 */
data class FrequencySettings(
    val gFrequency: Double = ViolinTunerConfig.FREQUENCY_G3,
    val dFrequency: Double = ViolinTunerConfig.FREQUENCY_D4,
    val aFrequency: Double = ViolinTunerConfig.FREQUENCY_A4,
    val eFrequency: Double = ViolinTunerConfig.FREQUENCY_E5
) {
    companion object {
        /** Default settings (A = 440 Hz) */
        val DEFAULT = FrequencySettings()
    }
}

/**
 * Dialog for frequency settings.
 *
 * Allows the user to set individual frequencies for each string.
 */
@Composable
fun FrequencySettingsDialog(
    currentSettings: FrequencySettings,
    onSettingsChanged: (FrequencySettings) -> Unit,
    onDismiss: () -> Unit
) {
    // Local states for input fields
    var gFreqText by remember { mutableStateOf(formatFrequency(currentSettings.gFrequency)) }
    var dFreqText by remember { mutableStateOf(formatFrequency(currentSettings.dFrequency)) }
    var aFreqText by remember { mutableStateOf(formatFrequency(currentSettings.aFrequency)) }
    var eFreqText by remember { mutableStateOf(formatFrequency(currentSettings.eFrequency)) }

    // Error states
    var gError by remember { mutableStateOf(false) }
    var dError by remember { mutableStateOf(false) }
    var aError by remember { mutableStateOf(false) }
    var eError by remember { mutableStateOf(false) }

    // Check if A string is exactly 440 Hz
    val aFreqValue = parseFrequency(aFreqText)
    val isStandardPitch = aFreqValue == 440.0

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Title
                Text(
                    text = "Frequency Settings",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Adjust the target frequencies for each string",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // G string
                FrequencyInputField(
                    label = "G String",
                    value = gFreqText,
                    onValueChange = {
                        gFreqText = it
                        gError = parseFrequency(it) == null
                    },
                    isError = gError,
                    defaultValue = "196.00"
                )

                // D string
                FrequencyInputField(
                    label = "D String",
                    value = dFreqText,
                    onValueChange = {
                        dFreqText = it
                        dError = parseFrequency(it) == null
                    },
                    isError = dError,
                    defaultValue = "293.66"
                )

                // A string - shows "(Concert Pitch)" only when exactly 440 Hz
                FrequencyInputField(
                    label = if (isStandardPitch) "A String (Concert Pitch)" else "A String",
                    value = aFreqText,
                    onValueChange = {
                        aFreqText = it
                        aError = parseFrequency(it) == null
                    },
                    isError = aError,
                    defaultValue = "440.00"
                )

                // E string
                FrequencyInputField(
                    label = "E String",
                    value = eFreqText,
                    onValueChange = {
                        eFreqText = it
                        eError = parseFrequency(it) == null
                    },
                    isError = eError,
                    defaultValue = "659.26"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Default button
                OutlinedButton(
                    onClick = {
                        val default = FrequencySettings.DEFAULT
                        gFreqText = formatFrequency(default.gFrequency)
                        dFreqText = formatFrequency(default.dFrequency)
                        aFreqText = formatFrequency(default.aFrequency)
                        eFreqText = formatFrequency(default.eFrequency)
                        gError = false
                        dError = false
                        aError = false
                        eError = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Reset to Default")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val g = parseFrequency(gFreqText)
                            val d = parseFrequency(dFreqText)
                            val a = parseFrequency(aFreqText)
                            val e = parseFrequency(eFreqText)

                            if (g != null && d != null && a != null && e != null) {
                                onSettingsChanged(FrequencySettings(g, d, a, e))
                                onDismiss()
                            }
                        },
                        enabled = !gError && !dError && !aError && !eError
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * Input field for a frequency.
 */
@Composable
private fun FrequencyInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    defaultValue: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        suffix = { Text("Hz") },
        isError = isError,
        supportingText = if (isError) {
            { Text("Invalid value (e.g. $defaultValue)") }
        } else null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}

/**
 * Formats a frequency for display.
 */
private fun formatFrequency(frequency: Double): String {
    return String.format(java.util.Locale.US, "%.2f", frequency)
}

/**
 * Parses a frequency from a string.
 * Supports comma and dot as decimal separators.
 *
 * @return The frequency or null for invalid value
 */
private fun parseFrequency(text: String): Double? {
    val normalized = text.replace(',', '.')
    val value = normalized.toDoubleOrNull()

    // Frequency must be positive and in a sensible range
    return if (value != null && value > 50.0 && value < 2000.0) {
        value
    } else {
        null
    }
}
