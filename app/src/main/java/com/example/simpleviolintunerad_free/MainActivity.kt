package com.example.simpleviolintunerad_free

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.simpleviolintunerad_free.ui.screens.TunerScreen
import com.example.simpleviolintunerad_free.ui.theme.SimpleViolinTunerAdFreeTheme

/**
 * Main Activity of the Violin Tuner App.
 *
 * This Activity is the entry point of the app and hosts
 * the Compose UI. All logic is delegated to ViewModel and
 * UI components for better testability.
 *
 * ARCHITECTURE:
 * - UI: Jetpack Compose (Material3)
 * - State Management: ViewModel with StateFlow
 * - Audio: Custom implementation without external dependencies
 *
 * PERMISSIONS:
 * - android.permission.RECORD_AUDIO (requested at runtime)
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleViolinTunerAdFreeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TunerScreen()
                }
            }
        }
    }
}

/**
 * Preview for the Tuner Screen.
 */
@Preview(showBackground = true)
@Composable
fun TunerPreview() {
    SimpleViolinTunerAdFreeTheme {
        TunerScreen()
    }
}