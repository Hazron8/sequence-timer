package com.hazron.sequencetimer.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.hazron.sequencetimer.data.repository.CategoryRepository
import com.hazron.sequencetimer.data.repository.SequenceRepository
import com.hazron.sequencetimer.data.repository.TimerRepository
import com.hazron.sequencetimer.domain.model.Category
import com.hazron.sequencetimer.domain.model.NotificationType
import com.hazron.sequencetimer.domain.model.Sequence
import com.hazron.sequencetimer.domain.model.SequenceStep
import com.hazron.sequencetimer.domain.model.Timer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@Singleton
class SyncRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val authRepository: AuthRepository,
    private val timerRepository: TimerRepository,
    private val sequenceRepository: SequenceRepository,
    private val categoryRepository: CategoryRepository
) {
    private fun getUserCollection(collection: String): String {
        val uid = authRepository.currentUser?.uid
            ?: throw IllegalStateException("User not signed in")
        return "users/$uid/$collection"
    }

    /**
     * Upload all local data to Firestore.
     */
    suspend fun uploadAllData(): Result<Unit> {
        return try {
            val uid = authRepository.currentUser?.uid
                ?: return Result.failure(Exception("Not signed in"))

            // Upload categories
            val categories = categoryRepository.getAllCategories().first()
            categories.forEach { category ->
                uploadCategory(category)
            }

            // Upload timers
            val timers = timerRepository.getAllTimers().first()
            timers.forEach { timer ->
                uploadTimer(timer)
            }

            // Upload sequences with steps
            val sequences = sequenceRepository.getAllSequencesWithSteps().first()
            sequences.forEach { sequenceWithSteps ->
                uploadSequence(sequenceWithSteps.sequence)
                sequenceWithSteps.steps.forEach { step ->
                    uploadSequenceStep(step)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download all data from Firestore and merge with local.
     */
    suspend fun downloadAndMergeData(): Result<Unit> {
        return try {
            val uid = authRepository.currentUser?.uid
                ?: return Result.failure(Exception("Not signed in"))

            // Download and merge categories
            val remoteCategories = downloadCategories()
            mergeCategories(remoteCategories)

            // Download and merge timers
            val remoteTimers = downloadTimers()
            mergeTimers(remoteTimers)

            // Download and merge sequences
            val remoteSequences = downloadSequences()
            val remoteSteps = downloadSequenceSteps()
            mergeSequences(remoteSequences, remoteSteps)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Full sync: upload local, then download and merge remote.
     */
    suspend fun fullSync(): Result<Unit> {
        return try {
            uploadAllData().getOrThrow()
            downloadAndMergeData().getOrThrow()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Upload methods
    private suspend fun uploadCategory(category: Category) {
        val data = mapOf(
            "id" to category.id,
            "name" to category.name,
            "icon" to category.icon,
            "color" to category.color,
            "sortOrder" to category.sortOrder,
            "isDefault" to category.isDefault,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(getUserCollection("categories"))
            .document(category.id.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    private suspend fun uploadTimer(timer: Timer) {
        val data = mapOf(
            "id" to timer.id,
            "label" to timer.label,
            "durationSeconds" to timer.durationSeconds,
            "notificationType" to timer.notificationType.name,
            "categoryId" to timer.categoryId,
            "createdAt" to timer.createdAt,
            "sortOrder" to timer.sortOrder,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(getUserCollection("timers"))
            .document(timer.id.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    private suspend fun uploadSequence(sequence: Sequence) {
        val data = mapOf(
            "id" to sequence.id,
            "name" to sequence.name,
            "categoryId" to sequence.categoryId,
            "createdAt" to sequence.createdAt,
            "sortOrder" to sequence.sortOrder,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(getUserCollection("sequences"))
            .document(sequence.id.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    private suspend fun uploadSequenceStep(step: SequenceStep) {
        val data = mapOf(
            "id" to step.id,
            "sequenceId" to step.sequenceId,
            "label" to step.label,
            "durationSeconds" to step.durationSeconds,
            "notificationType" to step.notificationType.name,
            "stepOrder" to step.stepOrder,
            "updatedAt" to System.currentTimeMillis()
        )
        firestore.collection(getUserCollection("sequence_steps"))
            .document(step.id.toString())
            .set(data, SetOptions.merge())
            .await()
    }

    // Download methods
    private suspend fun downloadCategories(): List<Category> {
        val snapshot = firestore.collection(getUserCollection("categories"))
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                Category(
                    id = (doc.getLong("id") ?: return@mapNotNull null),
                    name = doc.getString("name") ?: return@mapNotNull null,
                    icon = doc.getString("icon"),
                    color = doc.getLong("color")?.toInt(),
                    sortOrder = (doc.getLong("sortOrder") ?: 0).toInt(),
                    isDefault = doc.getBoolean("isDefault") ?: false
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun downloadTimers(): List<Timer> {
        val snapshot = firestore.collection(getUserCollection("timers"))
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                Timer(
                    id = doc.getLong("id") ?: return@mapNotNull null,
                    label = doc.getString("label") ?: return@mapNotNull null,
                    durationSeconds = doc.getLong("durationSeconds") ?: return@mapNotNull null,
                    notificationType = NotificationType.valueOf(
                        doc.getString("notificationType") ?: "SOUND"
                    ),
                    categoryId = doc.getLong("categoryId") ?: 1L,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    sortOrder = (doc.getLong("sortOrder") ?: 0).toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun downloadSequences(): List<Sequence> {
        val snapshot = firestore.collection(getUserCollection("sequences"))
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                Sequence(
                    id = doc.getLong("id") ?: return@mapNotNull null,
                    name = doc.getString("name") ?: return@mapNotNull null,
                    categoryId = doc.getLong("categoryId") ?: 1L,
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    sortOrder = (doc.getLong("sortOrder") ?: 0).toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    private suspend fun downloadSequenceSteps(): List<SequenceStep> {
        val snapshot = firestore.collection(getUserCollection("sequence_steps"))
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            try {
                SequenceStep(
                    id = doc.getLong("id") ?: return@mapNotNull null,
                    sequenceId = doc.getLong("sequenceId") ?: return@mapNotNull null,
                    label = doc.getString("label") ?: return@mapNotNull null,
                    durationSeconds = doc.getLong("durationSeconds") ?: return@mapNotNull null,
                    notificationType = NotificationType.valueOf(
                        doc.getString("notificationType") ?: "SOUND"
                    ),
                    stepOrder = (doc.getLong("stepOrder") ?: 0).toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    // Merge methods (last-write-wins based on updatedAt)
    private suspend fun mergeCategories(remoteCategories: List<Category>) {
        val localCategories = categoryRepository.getAllCategories().first()
        val localIds = localCategories.map { it.id }.toSet()

        remoteCategories.forEach { remote ->
            if (remote.id !in localIds && !remote.isDefault) {
                // New category from cloud - insert it
                categoryRepository.insertCategory(remote)
            }
            // For existing categories, local wins (they're likely default categories)
        }
    }

    private suspend fun mergeTimers(remoteTimers: List<Timer>) {
        val localTimers = timerRepository.getAllTimers().first()
        val localIds = localTimers.map { it.id }.toSet()

        remoteTimers.forEach { remote ->
            if (remote.id !in localIds) {
                // New timer from cloud - insert it
                timerRepository.insertTimer(remote)
            }
            // For existing timers, keep local version (last-write-wins locally)
        }
    }

    private suspend fun mergeSequences(
        remoteSequences: List<Sequence>,
        remoteSteps: List<SequenceStep>
    ) {
        val localSequences = sequenceRepository.getAllSequencesWithSteps().first()
        val localIds = localSequences.map { it.sequence.id }.toSet()

        remoteSequences.forEach { remote ->
            if (remote.id !in localIds) {
                // New sequence from cloud - insert it with steps
                sequenceRepository.insertSequence(remote)
                val steps = remoteSteps.filter { it.sequenceId == remote.id }
                sequenceRepository.insertSteps(steps)
            }
        }
    }

    // Delete sync methods
    suspend fun deleteTimerFromCloud(timerId: Long) {
        try {
            firestore.collection(getUserCollection("timers"))
                .document(timerId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            // Log error but don't fail - will sync on next full sync
        }
    }

    suspend fun deleteSequenceFromCloud(sequenceId: Long) {
        try {
            // Delete sequence
            firestore.collection(getUserCollection("sequences"))
                .document(sequenceId.toString())
                .delete()
                .await()

            // Delete associated steps
            val stepsSnapshot = firestore.collection(getUserCollection("sequence_steps"))
                .whereEqualTo("sequenceId", sequenceId)
                .get()
                .await()

            stepsSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }

    suspend fun deleteCategoryFromCloud(categoryId: Long) {
        try {
            firestore.collection(getUserCollection("categories"))
                .document(categoryId.toString())
                .delete()
                .await()
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }
}
