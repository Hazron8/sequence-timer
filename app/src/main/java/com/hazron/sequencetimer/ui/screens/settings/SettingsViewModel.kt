package com.hazron.sequencetimer.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hazron.sequencetimer.R
import com.hazron.sequencetimer.data.remote.AuthRepository
import com.hazron.sequencetimer.data.remote.AuthState
import com.hazron.sequencetimer.data.remote.SyncRepository
import com.hazron.sequencetimer.data.remote.SyncStatus
import com.hazron.sequencetimer.data.remote.UserInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isFirebaseAvailable: Boolean = false,
    val authState: AuthState = AuthState.Loading,
    val syncStatus: SyncStatus = SyncStatus.Idle,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        authRepository.authState,
        _syncStatus,
        _lastSyncTime,
        _errorMessage
    ) { authState, syncStatus, lastSyncTime, errorMessage ->
        SettingsUiState(
            isFirebaseAvailable = authRepository.isFirebaseAvailable,
            authState = if (authRepository.isFirebaseAvailable) authState else AuthState.SignedOut,
            syncStatus = syncStatus,
            lastSyncTime = lastSyncTime,
            errorMessage = errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState(isFirebaseAvailable = authRepository.isFirebaseAvailable)
    )

    fun signIn() {
        viewModelScope.launch {
            _errorMessage.value = null
            val webClientId = context.getString(R.string.default_web_client_id)
            val result = authRepository.signInWithGoogle(webClientId)
            result.fold(
                onSuccess = {
                    // Auto-sync after sign in
                    performSync()
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Sign in failed"
                }
            )
        }
    }

    fun signOut() {
        authRepository.signOut()
        _lastSyncTime.value = null
    }

    fun performSync() {
        if (_syncStatus.value == SyncStatus.Syncing) return
        if (!authRepository.isSignedIn) {
            _errorMessage.value = "Please sign in to sync"
            return
        }

        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            _errorMessage.value = null

            val result = syncRepository.fullSync()
            result.fold(
                onSuccess = {
                    _syncStatus.value = SyncStatus.Success
                    _lastSyncTime.value = System.currentTimeMillis()
                },
                onFailure = { error ->
                    _syncStatus.value = SyncStatus.Error(error.message ?: "Sync failed")
                    _errorMessage.value = error.message ?: "Sync failed"
                }
            )
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
