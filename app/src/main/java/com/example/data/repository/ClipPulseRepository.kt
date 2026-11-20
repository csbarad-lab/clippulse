package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AspectRatio
import com.example.data.model.CaptionSegment
import com.example.data.model.CaptionStyle
import com.example.data.model.HookLabDraft
import com.example.data.model.StreamProject
import com.example.data.model.TargetPlatform
import com.example.data.model.ViralClip
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class ClipPulseRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Sign in with Google to access your ClipPulse studio.")
    }

    fun observeProjects(userId: String): Flow<List<StreamProject>> {
        val path = "users/$userId/projects"
        return db.collection("users")
            .document(userId)
            .collection("projects")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    StreamProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    fun observeClipsForProject(userId: String, projectId: String): Flow<List<ViralClip>> {
        val path = "users/$userId/projects/$projectId/clips"
        return db.collection("users")
            .document(userId)
            .collection("projects")
            .document(projectId)
            .collection("clips")
            .orderBy("viralScore", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    ViralClip::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun getProjectOnce(userId: String, projectId: String): Result<StreamProject?> {
        val path = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            val snap = db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .get().await()
            Result.success(
                snap.toObject(
                    StreamProject::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    fun observeClip(userId: String, projectId: String, clipId: String): Flow<ViralClip?> {
        val path = "users/$userId/projects/$projectId/clips/$clipId"
        return db.collection("users")
            .document(userId)
            .collection("projects")
            .document(projectId)
            .collection("clips")
            .document(clipId)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) null
                else snapshot.toObject(
                    ViralClip::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.GET, path)
                }
                throw error
            }
    }

    suspend fun createProjectWithClips(
        userId: String,
        project: StreamProject,
        clips: List<ViralClip>
    ): Result<String> {
        var docPath = "users/$userId/projects"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            val projectRef = db.collection("users").document(uid).collection("projects").document()
            val projectId = projectRef.id
            docPath = projectRef.path

            val preparedProject = project.copy(clipCount = clips.size.toLong())
            projectRef.set(preparedProject.toCreateMap(uid)).await()

            for (clip in clips.take(20)) {
                val clipRef = projectRef.collection("clips").document()
                docPath = clipRef.path
                clipRef.set(clip.toCreateMap(uid, projectId)).await()
            }

            Result.success(projectId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateClip(
        userId: String,
        projectId: String,
        clipId: String,
        hookTitle: String,
        startTimeSec: Long,
        endTimeSec: Long,
        viralCaption: String,
        hashtags: List<String>,
        thumbnailText: String,
        pinnedComment: String,
        captionStyle: CaptionStyle,
        aspectRatio: AspectRatio,
        captionSegments: List<CaptionSegment>,
        favorite: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            require(projectId.isNotBlank() && clipId.isNotBlank()) { "Invalid clip reference." }
            val safeStart = startTimeSec.coerceIn(0L, 86399L)
            val safeEnd = endTimeSec.coerceIn(safeStart + 1L, 86400L)

            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("clips").document(clipId)
                .update(
                    mapOf(
                        "hookTitle" to hookTitle.ifBlank { "Viral Clip" }.take(160),
                        "startTimeSec" to safeStart,
                        "endTimeSec" to safeEnd,
                        "viralCaption" to viralCaption.ifBlank { hookTitle }.take(2200),
                        "hashtags" to hashtags.take(20).map { it.take(50) },
                        "thumbnailText" to thumbnailText.take(80),
                        "pinnedComment" to pinnedComment.take(400),
                        "captionStyle" to captionStyle.name,
                        "aspectRatio" to aspectRatio.name,
                        "captionSegments" to captionSegments.take(30).map { it.toFirestoreMap() },
                        "favorite" to favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun toggleFavoriteClip(
        userId: String,
        projectId: String,
        clipId: String,
        favorite: Boolean
    ): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("clips").document(clipId)
                .update(
                    mapOf(
                        "favorite" to favorite,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteClip(userId: String, projectId: String, clipId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId/clips/$clipId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            db.collection("users").document(uid)
                .collection("projects").document(projectId)
                .collection("clips").document(clipId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteProject(userId: String, projectId: String): Result<Unit> {
        val docPath = "users/$userId/projects/$projectId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            val projectRef = db.collection("users").document(uid).collection("projects").document(projectId)
            val clipsSnap = projectRef.collection("clips").get().await()
            for (doc in clipsSnap.documents) {
                doc.reference.delete().await()
            }
            projectRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeHookDrafts(userId: String): Flow<List<HookLabDraft>> {
        val path = "users/$userId/hookDrafts"
        return db.collection("users")
            .document(userId)
            .collection("hookDrafts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    HookLabDraft::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                if (auth.currentUser?.uid == userId) {
                    handleFirestoreError(firestoreError ?: error, OperationType.LIST, path)
                }
                throw error
            }
    }

    suspend fun saveHookDraft(
        userId: String,
        topic: String,
        tone: String,
        generatedHooks: String
    ): Result<String> {
        var docPath = "users/$userId/hookDrafts"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            val draftRef = db.collection("users").document(uid).collection("hookDrafts").document()
            docPath = draftRef.path
            val draft = HookLabDraft(
                topic = topic,
                tone = tone,
                generatedHooks = generatedHooks
            )
            draftRef.set(draft.toCreateMap(uid)).await()
            Result.success(draftRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteHookDraft(userId: String, draftId: String): Result<Unit> {
        val docPath = "users/$userId/hookDrafts/$draftId"
        return try {
            val uid = requireUserId()
            check(uid == userId) { "Authenticated user mismatch." }
            db.collection("users").document(uid)
                .collection("hookDrafts").document(draftId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
