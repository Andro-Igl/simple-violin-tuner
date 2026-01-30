package com.example.simpleviolintunerad_free.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.simpleviolintunerad_free.ui.components.*
import com.example.simpleviolintunerad_free.viewmodel.TunerViewModel

/**
 * Main screen of the Violin Tuner.
 *
 * This screen orchestrates all UI components and
 * connects them with the ViewModel.
 *
 * STRUCTURE:
 * - Permission handling
 * - Tuner display (Gauge, Note, Frequency)
 * - String selection
 * - Start/Stop button
 *
 * CUSTOMIZABLE:
 * - Layout can be easily adjusted
 * - Components can be swapped out
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TunerScreen(
    viewModel: TunerViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    // States from ViewModel
    val tunerState by viewModel.tunerState.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val hasPermission by viewModel.hasPermission.collectAsState()
    val selectedString by viewModel.selectedString.collectAsState()
    val frequencySettings by viewModel.frequencySettings.collectAsState()

    // UI States
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updatePermissionStatus()
        if (isGranted) {
            viewModel.startListening()
        }
    }

    // Update UI when permission changes and auto-start
    LaunchedEffect(Unit) {
        viewModel.updatePermissionStatus()
        // Auto-start if permission is granted
        if (viewModel.hasPermission.value && !viewModel.isListening.value) {
            viewModel.startListening()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Simple Violin Tuner",
                        fontSize = 22.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(64.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Frequency Settings") },
                                onClick = {
                                    showMenu = false
                                    showSettingsDialog = true
                                }
                            )
                        }
                    }
                },
                modifier = Modifier.height(68.dp)
            )
        },
        modifier = modifier
    ) { innerPadding ->

        if (!hasPermission) {
            // Permission Request Screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                PermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                )
            }
        } else {
            // Main Tuner UI
            TunerContent(
                tunerState = tunerState,
                isListening = isListening,
                selectedString = selectedString,
                availableStrings = viewModel.availableStrings,
                onStringSelected = viewModel::selectString,
                onToggleListening = {
                    if (isListening) {
                        viewModel.stopListening()
                    } else {
                        viewModel.startListening()
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }

        // Frequency Settings Dialog
        if (showSettingsDialog) {
            FrequencySettingsDialog(
                currentSettings = frequencySettings,
                onSettingsChanged = { newSettings ->
                    viewModel.updateFrequencySettings(newSettings)
                },
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

/**
 * Main content of the tuner.
 */
@Composable
private fun TunerContent(
    tunerState: com.example.simpleviolintunerad_free.viewmodel.TunerState,
    isListening: Boolean,
    selectedString: com.example.simpleviolintunerad_free.audio.ViolinString?,
    availableStrings: List<com.example.simpleviolintunerad_free.audio.ViolinString>,
    onStringSelected: (com.example.simpleviolintunerad_free.audio.ViolinString?) -> Unit,
    onToggleListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Upper area: Tuner Display - takes available space
        TunerDisplay(
            state = tunerState,
            modifier = Modifier.weight(1f)
        )

        // Error display
        tunerState.error?.let { error ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Spacer for gap between Gauge and buttons
        Spacer(modifier = Modifier.height(24.dp))

        // Bottom area: String selection and button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // String selection
            StringSelector(
                strings = availableStrings,
                selectedString = selectedString,
                onStringSelected = onStringSelected
            )


            // Start/Stop Button
            TunerControlButton(
                isListening = isListening,
                onToggle = onToggleListening
            )
        }
    }
}
