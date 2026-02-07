package com.hazron.sequencetimer.di

import android.content.Context
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Checks if Firebase is available (has google-services.json configured).
 * This allows the app to work without Firebase for development/testing.
 */
@Singleton
class FirebaseAvailability @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isAvailable: Boolean by lazy {
        try {
            FirebaseApp.getInstance()
            true
        } catch (e: IllegalStateException) {
            // Firebase not initialized - google-services.json is missing
            false
        }
    }
}
