package com.hazron.sequencetimer.ui.screens.sequence

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.ui.theme.TimerGreen
import com.hazron.sequencetimer.ui.theme.TimerOrange
import com.hazron.sequencetimer.ui.theme.TimerRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SequencePlaybackScreen(
    onNavigateBack: () -> Unit,
    viewModel: SequencePlaybackViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.sequenceWithSteps?.sequence?.name ?: "Sequence",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
        } else if (uiState.sequenceWithSteps == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Sequence not found")
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Step progress indicator
                StepProgressIndicator(
                    currentStep = uiState.currentStepIndex + 1,
                    totalSteps = uiState.totalSteps,
                    overallProgress = uiState.overallProgress,
                    isComplete = uiState.isComplete
                )

                // Current step info
                CurrentStepDisplay(
                    stepLabel = uiState.currentStep?.label ?: "",
                    remainingSeconds = uiState.remainingSeconds,
                    progress = uiState.progress,
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused,
                    isComplete = uiState.isComplete
                )

                // Next step preview
                if (uiState.nextStep != null && !uiState.isComplete) {
                    NextStepPreview(
                        stepLabel = uiState.nextStep!!.label,
                        durationSeconds = uiState.nextStep!!.durationSeconds
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Control buttons
                SequenceControls(
                    isRunning = uiState.isRunning,
                    isPaused = uiState.isPaused,
                    isComplete = uiState.isComplete,
                    canSkipPrevious = uiState.currentStepIndex > 0,
                    canSkipNext = uiState.currentStepIndex < uiState.totalSteps - 1,
                    onPlayPause = {
                        when {
                            uiState.isComplete -> viewModel.resetSequence()
                            !uiState.isRunning && !uiState.isPaused -> viewModel.startSequence()
                            uiState.isPaused -> viewModel.resumeSequence()
                            else -> viewModel.pauseSequence()
                        }
                    },
                    onReset = viewModel::resetSequence,
                    onSkipPrevious = viewModel::skipToPreviousStep,
                    onSkipNext = viewModel::skipToNextStep
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StepProgressIndicator(
    currentStep: Int,
    totalSteps: Int,
    overallProgress: Float,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (isComplete) "Complete!" else "Step $currentStep of $totalSteps",
            style = MaterialTheme.typography.titleMedium,
            color = if (isComplete) TimerGreen else MaterialTheme.colorScheme.onSurface
        )

        LinearProgressIndicator(
            progress = { overallProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = if (isComplete) TimerGreen else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun CurrentStepDisplay(
    stepLabel: String,
    remainingSeconds: Long,
    progress: Float,
    isRunning: Boolean,
    isPaused: Boolean,
    isComplete: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step label
        Text(
            text = stepLabel,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Circular progress with time
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(240.dp)
        ) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                color = when {
                    isComplete -> TimerGreen
                    progress < 0.25f -> TimerOrange
                    else -> TimerGreen
                }
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formatTime(remainingSeconds),
                    style = MaterialTheme.typography.displayLarge,
                    textAlign = TextAlign.Center
                )

                if (isComplete) {
                    Text(
                        text = "All Done!",
                        style = MaterialTheme.typography.titleMedium,
                        color = TimerGreen
                    )
                } else if (isPaused) {
                    Text(
                        text = "Paused",
                        style = MaterialTheme.typography.titleMedium,
                        color = TimerOrange
                    )
                }
            }
        }
    }
}

@Composable
private fun NextStepPreview(
    stepLabel: String,
    durationSeconds: Long
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Up Next",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stepLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = formatTime(durationSeconds),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SequenceControls(
    isRunning: Boolean,
    isPaused: Boolean,
    isComplete: Boolean,
    canSkipPrevious: Boolean,
    canSkipNext: Boolean,
    onPlayPause: () -> Unit,
    onReset: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSkipNext: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Reset button
        FilledTonalIconButton(
            onClick = onReset,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Reset",
                modifier = Modifier.size(28.dp)
            )
        }

        // Skip previous
        FilledTonalIconButton(
            onClick = onSkipPrevious,
            enabled = canSkipPrevious && !isComplete,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.SkipPrevious,
                contentDescription = "Previous Step",
                modifier = Modifier.size(28.dp)
            )
        }

        // Play/Pause button
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(80.dp)
        ) {
            Icon(
                imageVector = when {
                    isComplete -> Icons.Default.Refresh
                    !isRunning || isPaused -> Icons.Default.PlayArrow
                    else -> Icons.Default.Pause
                },
                contentDescription = when {
                    isComplete -> "Restart"
                    !isRunning || isPaused -> "Start"
                    else -> "Pause"
                },
                modifier = Modifier.size(40.dp)
            )
        }

        // Skip next
        FilledTonalIconButton(
            onClick = onSkipNext,
            enabled = canSkipNext && !isComplete,
            modifier = Modifier.size(56.dp)
        ) {
            Icon(
                Icons.Default.SkipNext,
                contentDescription = "Next Step",
                modifier = Modifier.size(28.dp)
            )
        }

        // Spacer to balance layout
        Spacer(modifier = Modifier.size(56.dp))
    }
}

private fun formatTime(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}
