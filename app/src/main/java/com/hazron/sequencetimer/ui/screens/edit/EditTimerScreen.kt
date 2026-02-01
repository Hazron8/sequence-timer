package com.hazron.sequencetimer.ui.screens.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.domain.model.NotificationType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTimerScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: EditTimerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNewTimer) "New Timer" else "Edit Timer")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveTimer(onSaved) },
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Label
                OutlinedTextField(
                    value = uiState.label,
                    onValueChange = viewModel::updateLabel,
                    label = { Text("Timer Label") },
                    placeholder = { Text("e.g., Pasta") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Duration
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DurationField(
                        value = uiState.hours,
                        onValueChange = viewModel::updateHours,
                        label = "Hours",
                        modifier = Modifier.weight(1f)
                    )
                    DurationField(
                        value = uiState.minutes,
                        onValueChange = viewModel::updateMinutes,
                        label = "Minutes",
                        modifier = Modifier.weight(1f)
                    )
                    DurationField(
                        value = uiState.seconds,
                        onValueChange = viewModel::updateSeconds,
                        label = "Seconds",
                        modifier = Modifier.weight(1f)
                    )
                }

                // Notification Type
                Text(
                    text = "When Timer Ends",
                    style = MaterialTheme.typography.titleMedium
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationTypeOption.entries.forEach { option ->
                        NotificationTypeCard(
                            option = option,
                            isSelected = uiState.notificationType == option.type,
                            onClick = { viewModel.updateNotificationType(option.type) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DurationField(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = if (value == 0) "" else value.toString(),
        onValueChange = { text ->
            val newValue = text.filter { it.isDigit() }.take(2).toIntOrNull() ?: 0
            onValueChange(newValue)
        },
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

private enum class NotificationTypeOption(
    val type: NotificationType,
    val title: String,
    val description: String
) {
    SILENT(
        NotificationType.SILENT,
        "Silent",
        "Notification only, no sound"
    ),
    SOUND(
        NotificationType.SOUND,
        "Sound",
        "Notification with a beep"
    ),
    ALARM(
        NotificationType.ALARM,
        "Alarm",
        "Full-screen alert that must be dismissed"
    )
}

@Composable
private fun NotificationTypeCard(
    option: NotificationTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder()
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = option.title,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
