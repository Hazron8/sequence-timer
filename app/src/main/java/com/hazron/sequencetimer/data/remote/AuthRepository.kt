package com.hazron.sequencetimer.data.remote

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.hazron.sequencetimer.di.FirebaseAvailability
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AuthState {
    object Loading : AuthState()
    object SignedOut : AuthState()
    data class SignedIn(val user: UserInfo) : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?
)

@Singleton
class AuthRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth?,
    private val firebaseAvailability: FirebaseAvailability
) {
    private val credentialManager: CredentialManager? = if (firebaseAvailability.isAvailable) {
        CredentialManager.create(context)
    } else {
        null
    }

    val isFirebaseAvailable: Boolean
        get() = firebaseAvailability.isAvailable && firebaseAuth != null

    val authState: Flow<AuthState> = if (firebaseAuth != null) {
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    trySend(AuthState.SignedIn(user.toUserInfo()))
                } else {
                    trySend(AuthState.SignedOut)
                }
            }
            firebaseAuth.addAuthStateListener(listener)
            awaitClose { firebaseAuth.removeAuthStateListener(listener) }
        }
    } else {
        flowOf(AuthState.SignedOut)
    }

    val currentUser: FirebaseUser?
        get() = firebaseAuth?.currentUser

    val isSignedIn: Boolean
        get() = firebaseAuth?.currentUser != null

    suspend fun signInWithGoogle(webClientId: String): Result<UserInfo> {
        if (firebaseAuth == null || credentialManager == null) {
            return Result.failure(Exception("Firebase is not available"))
        }

        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context, request)
            handleSignInResult(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleSignInResult(result: GetCredentialResponse): Result<UserInfo> {
        if (firebaseAuth == null) {
            return Result.failure(Exception("Firebase is not available"))
        }

        return when (val credential = result.credential) {
            is CustomCredential -> {
                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
                    val authResult = firebaseAuth.signInWithCredential(firebaseCredential).await()
                    val user = authResult.user
                        ?: return Result.failure(Exception("Sign in failed"))

                    Result.success(user.toUserInfo())
                } else {
                    Result.failure(Exception("Unexpected credential type"))
                }
            }
            else -> Result.failure(Exception("Unexpected credential type"))
        }
    }

    fun signOut() {
        firebaseAuth?.signOut()
    }

    private fun FirebaseUser.toUserInfo() = UserInfo(
        uid = uid,
        email = email,
        displayName = displayName,
        photoUrl = photoUrl?.toString()
    )
}
