package com.hazron.sequencetimer.ui.screens.edit

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.ui.components.TimeDurationPicker

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
                // Label with smart placeholder
                OutlinedTextField(
                    value = if (uiState.isLabelFocused) uiState.label else uiState.displayLabel,
                    onValueChange = viewModel::updateLabel,
                    label = { Text("Timer Label") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onFocusChanged { focusState ->
                            viewModel.onLabelFocusChanged(focusState.isFocused)
                        },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = if (uiState.label.isBlank()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                )

                // Category selector
                if (uiState.categories.isNotEmpty()) {
                    CategoryDropdown(
                        categories = uiState.categories,
                        selectedCategoryId = uiState.categoryId,
                        onCategorySelected = viewModel::updateCategory
                    )
                }

                // Duration with scroll wheel picker
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.titleMedium
                )

                TimeDurationPicker(
                    hours = uiState.hours,
                    minutes = uiState.minutes,
                    seconds = uiState.seconds,
                    onHoursChange = viewModel::updateHours,
                    onMinutesChange = viewModel::updateMinutes,
                    onSecondsChange = viewModel::updateSeconds
                )

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryDropdown(
    categories: List<Category>,
    selectedCategoryId: Long,
    onCategorySelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedCategory = categories.find { it.id == selectedCategoryId }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedCategory?.name ?: "Select Category",
            onValueChange = {},
            readOnly = true,
            label = { Text("Category") },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onCategorySelected(category.id)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}
