package com.hazron.sequencetimer.ui.screens.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.domain.RunningTimerState
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Timer
import com.hazron.sequencetimer.ui.theme.TimerGreen
import com.hazron.sequencetimer.ui.theme.TimerOrange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onTimerClick: (Long) -> Unit,
    onAddTimer: () -> Unit,
    onEditTimer: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sequence Timer") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTimer) {
                Icon(Icons.Default.Add, contentDescription = "Add Timer")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Category tabs
            if (uiState.categories.isNotEmpty()) {
                CategoryTabs(
                    categories = uiState.categories,
                    selectedCategoryId = uiState.selectedCategoryId,
                    onCategorySelected = { viewModel.selectCategory(it) }
                )
            }

            // Timer list
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.timers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (uiState.selectedCategoryId != null) {
                                "No timers in this category"
                            } else {
                                "No timers yet"
                            },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Tap + to create your first timer",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.timers, key = { it.id }) { timer ->
                        TimerCard(
                            timer = timer,
                            runningState = uiState.runningStates[timer.id],
                            category = uiState.categories.find { it.id == timer.categoryId },
                            showCategory = uiState.selectedCategoryId == null,
                            onClick = { onTimerClick(timer.id) },
                            onEdit = { onEditTimer(timer.id) },
                            onDelete = { viewModel.deleteTimer(timer) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // "All" tab
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = { onCategorySelected(null) },
            label = { Text("All") },
            leadingIcon = if (selectedCategoryId == null) {
                {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )

        // Category tabs
        categories.forEach { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) },
                leadingIcon = if (selectedCategoryId == category.id) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else {
                    {
                        Icon(
                            imageVector = getCategoryIcon(category.icon),
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun getCategoryIcon(iconName: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when (iconName) {
        "timer" -> Icons.Default.Timer
        "self_improvement" -> Icons.Default.SelfImprovement
        "fitness_center" -> Icons.Default.FitnessCenter
        "restaurant" -> Icons.Default.Restaurant
        "work" -> Icons.Default.Work
        else -> Icons.Default.Category
    }
}

@Composable
private fun TimerCard(
    timer: Timer,
    runningState: RunningTimerState?,
    category: Category?,
    showCategory: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isRunning = runningState?.isRunning == true && runningState.isPaused == false
    val isPaused = runningState?.isPaused == true
    val displayTime = runningState?.remainingSeconds ?: timer.durationSeconds

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isRunning) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Running indicator
            if (isRunning || isPaused) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Running" else "Paused",
                    tint = if (isRunning) TimerGreen else TimerOrange,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatDuration(displayTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isRunning) {
                            TimerGreen
                        } else if (isPaused) {
                            TimerOrange
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (isRunning) {
                        Text(
                            text = "Running",
                            style = MaterialTheme.typography.labelSmall,
                            color = TimerGreen
                        )
                    } else if (isPaused) {
                        Text(
                            text = "Paused",
                            style = MaterialTheme.typography.labelSmall,
                            color = TimerOrange
                        )
                    }

                    // Show category badge when viewing "All"
                    if (showCategory && category != null) {
                        SuggestionChip(
                            onClick = { },
                            label = {
                                Text(
                                    text = category.name,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            modifier = Modifier.height(24.dp)
                        )
                    }

                    Icon(
                        imageVector = when (timer.notificationType) {
                            NotificationType.SILENT -> Icons.Default.NotificationsOff
                            NotificationType.SOUND -> Icons.Default.Notifications
                            NotificationType.ALARM -> Icons.Default.NotificationsActive
                        },
                        contentDescription = timer.notificationType.name,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Timer") },
            text = { Text("Are you sure you want to delete \"${timer.label}\"?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun formatDuration(seconds: Long): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, secs)
        else -> String.format("%d:%02d", minutes, secs)
    }
}
