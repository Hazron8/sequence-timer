package com.hazron.sequencetimer.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hazron.sequencetimer.data.remote.AuthState
import com.hazron.sequencetimer.data.remote.SyncStatus
import com.hazron.sequencetimer.data.remote.UserInfo
import com.hazron.sequencetimer.ui.theme.TimerGreen
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Account section (only show when Firebase is available)
            if (uiState.isFirebaseAvailable) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                AccountCard(
                    authState = uiState.authState,
                    onSignIn = viewModel::signIn,
                    onSignOut = viewModel::signOut
                )

                // Sync section (only show when signed in)
                if (uiState.authState is AuthState.SignedIn) {
                    Text(
                        text = "Cloud Sync",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SyncCard(
                        syncStatus = uiState.syncStatus,
                        lastSyncTime = uiState.lastSyncTime,
                        onSync = viewModel::performSync
                    )
                }
            }

            // Error snackbar
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = viewModel::clearError) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            // About section
            Text(
                text = "About",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Card {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "BetterTimer",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "Version 1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "A simple timer app with sequences and cloud sync.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(
    authState: AuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    Card {
        when (authState) {
            is AuthState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AuthState.SignedOut -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        text = "Sign in to sync your timers across devices",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = onSignIn,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign in with Google")
                    }
                }
            }

            is AuthState.SignedIn -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar
                    Surface(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = authState.user.displayName?.firstOrNull()?.uppercase() ?: "?",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = authState.user.displayName ?: "User",
                            style = MaterialTheme.typography.titleMedium
                        )
                        authState.user.email?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    TextButton(onClick = onSignOut) {
                        Text("Sign Out")
                    }
                }
            }

            is AuthState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    Text(
                        text = authState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )

                    Button(onClick = onSignIn) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncCard(
    syncStatus: SyncStatus,
    lastSyncTime: Long?,
    onSync: () -> Unit
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Sync Status",
                        style = MaterialTheme.typography.titleSmall
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (syncStatus) {
                            is SyncStatus.Idle -> {
                                Icon(
                                    Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Ready to sync",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            is SyncStatus.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Syncing...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            is SyncStatus.Success -> {
                                Icon(
                                    Icons.Default.CloudDone,
                                    contentDescription = null,
                                    tint = TimerGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Synced",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TimerGreen
                                )
                            }

                            is SyncStatus.Error -> {
                                Icon(
                                    Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Sync failed",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onSync,
                    enabled = syncStatus !is SyncStatus.Syncing
                ) {
                    Icon(
                        Icons.Default.Sync,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sync Now")
                }
            }

            lastSyncTime?.let { time ->
                HorizontalDivider()
                Text(
                    text = "Last synced: ${formatSyncTime(time)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatSyncTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000} minutes ago"
        diff < 86400_000 -> "${diff / 3600_000} hours ago"
        else -> {
            val formatter = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            formatter.format(Date(timestamp))
        }
    }
}
