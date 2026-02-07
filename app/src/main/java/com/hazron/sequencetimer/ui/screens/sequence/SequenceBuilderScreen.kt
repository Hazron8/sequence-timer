package com.hazron.sequencetimer.ui.screens.sequence

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.ui.components.TimeDurationPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequenceBuilderScreen(
    onNavigateBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: SequenceBuilderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (uiState.isNewSequence) "New Sequence" else "Edit Sequence")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.saveSequence(onSaved) },
                        enabled = uiState.isValid && !uiState.isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::addStep) {
                Icon(Icons.Default.Add, contentDescription = "Add Step")
            }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Sequence name
                item {
                    OutlinedTextField(
                        value = uiState.sequenceName,
                        onValueChange = viewModel::updateSequenceName,
                        label = { Text("Sequence Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                // Category selector
                if (uiState.categories.isNotEmpty()) {
                    item {
                        CategoryDropdown(
                            categories = uiState.categories,
                            selectedCategoryId = uiState.categoryId,
                            onCategorySelected = viewModel::updateCategory
                        )
                    }
                }

                // Summary
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Steps",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${uiState.steps.count { it.isValid }}",
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Total Duration",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatDuration(uiState.totalDurationSeconds),
                                    style = MaterialTheme.typography.titleLarge
                                )
                            }
                        }
                    }
                }

                // Section header
                item {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                // Steps
                itemsIndexed(
                    uiState.steps,
                    key = { _, step -> step.tempId }
                ) { index, step ->
                    StepCard(
                        stepNumber = index + 1,
                        step = step,
                        canDelete = uiState.steps.size > 1,
                        canMoveUp = index > 0,
                        canMoveDown = index < uiState.steps.size - 1,
                        onLabelChange = { viewModel.updateStepLabel(index, it) },
                        onDurationChange = { h, m, s -> viewModel.updateStepDuration(index, h, m, s) },
                        onNotificationTypeChange = { viewModel.updateStepNotificationType(index, it) },
                        onDelete = { viewModel.removeStep(index) },
                        onMoveUp = { viewModel.moveStep(index, index - 1) },
                        onMoveDown = { viewModel.moveStep(index, index + 1) }
                    )
                }

                // Bottom spacing for FAB
                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    stepNumber: Int,
    step: EditableStep,
    canDelete: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onLabelChange: (String) -> Unit,
    onDurationChange: (Int, Int, Int) -> Unit,
    onNotificationTypeChange: (NotificationType) -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var showDurationPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header row with step number and reorder controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step $stepNumber",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(
                        onClick = onMoveUp,
                        enabled = canMoveUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        enabled = canMoveDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        enabled = canDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = if (canDelete) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Step label
            OutlinedTextField(
                value = step.label,
                onValueChange = onLabelChange,
                label = { Text("Step Label") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Duration - clickable to open WheelPicker dialog
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDurationPicker = true },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Duration",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(step.durationSeconds),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit duration",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Notification type selector (compact)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NotificationType.entries.forEach { type ->
                    FilterChip(
                        selected = step.notificationType == type,
                        onClick = { onNotificationTypeChange(type) },
                        label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        leadingIcon = if (step.notificationType == type) {
                            {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize)
                                )
                            }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Duration picker dialog
    if (showDurationPicker) {
        DurationPickerDialog(
            hours = step.hours,
            minutes = step.minutes,
            seconds = step.seconds,
            onConfirm = { h, m, s ->
                onDurationChange(h, m, s)
                showDurationPicker = false
            },
            onDismiss = { showDurationPicker = false }
        )
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

@Composable
private fun DurationPickerDialog(
    hours: Int,
    minutes: Int,
    seconds: Int,
    onConfirm: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var tempHours by remember { mutableIntStateOf(hours) }
    var tempMinutes by remember { mutableIntStateOf(minutes) }
    var tempSeconds by remember { mutableIntStateOf(seconds) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Duration") },
        text = {
            TimeDurationPicker(
                hours = tempHours,
                minutes = tempMinutes,
                seconds = tempSeconds,
                onHoursChange = { tempHours = it },
                onMinutesChange = { tempMinutes = it },
                onSecondsChange = { tempSeconds = it }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(tempHours, tempMinutes, tempSeconds) }) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun formatDuration(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}
