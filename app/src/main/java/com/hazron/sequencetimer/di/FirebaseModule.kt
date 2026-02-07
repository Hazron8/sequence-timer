package com.hazron.sequencetimer.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(firebaseAvailability: FirebaseAvailability): FirebaseAuth? {
        return if (firebaseAvailability.isAvailable) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(firebaseAvailability: FirebaseAvailability): FirebaseFirestore? {
        return if (firebaseAvailability.isAvailable) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
}
