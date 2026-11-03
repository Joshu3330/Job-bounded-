package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.JobApplication
import com.example.data.model.UserProfile
import com.example.util.OperationType
import com.example.util.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in before accessing Firestore.")
    }

    // --- Profile Operations ---

    fun observeProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId/profile/main"
        val docRef = db.collection("users").document(userId).collection("profile").document("main")
        emitAll(
            docRef.snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java)
                    } else {
                        null
                    }
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.GET, path)
                    throw e
                }
        )
    }

    suspend fun saveProfile(profile: UserProfile): Result<Unit> {
        val uid = requireUserId()
        val path = "users/$uid/profile/main"
        val docRef = db.collection("users").document(uid).collection("profile").document("main")
        return try {
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "fullName" to profile.fullName.trim(),
                "email" to profile.email.trim(),
                "phone" to profile.phone.trim(),
                "headline" to profile.headline.trim(),
                "summary" to profile.summary.trim(),
                "skills" to profile.skills.trim(),
                "workExperience" to profile.workExperience.trim(),
                "education" to profile.education.trim(),
                "careerGoals" to profile.careerGoals.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (profile.createdAt == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            } else {
                payload["createdAt"] = profile.createdAt
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- Job Application Operations ---

    fun observeApplications(userId: String): Flow<List<JobApplication>> = flow {
        val path = "users/$userId/job_applications"
        val collectionRef = db.collection("users").document(userId).collection("job_applications")
        emitAll(
            collectionRef.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(JobApplication::class.java)
                }
                .catch { e ->
                    if (e is Exception) handleFirestoreError(e, OperationType.LIST, path)
                    throw e
                }
        )
    }

    suspend fun saveApplication(application: JobApplication): Result<String> {
        val uid = requireUserId()
        val collectionRef = db.collection("users").document(uid).collection("job_applications")
        val docRef = if (application.id.isNotBlank()) {
            collectionRef.document(application.id)
        } else {
            collectionRef.document()
        }
        return try {
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "company" to application.company.trim(),
                "jobTitle" to application.jobTitle.trim(),
                "jobDescription" to application.jobDescription.trim(),
                "applicationDate" to application.applicationDate.trim(),
                "status" to application.status,
                "notes" to application.notes.trim(),
                "deadline" to application.deadline.trim(),
                "salary" to application.salary.trim(),
                "location" to application.location.trim(),
                "tailoredResume" to application.tailoredResume.trim(),
                "tailoredCoverLetter" to application.tailoredCoverLetter.trim(),
                "matchScore" to application.matchScore,
                "matchAnalysis" to application.matchAnalysis.trim(),
                "desiredTone" to application.desiredTone,
                "emphasizedSkills" to application.emphasizedSkills.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (application.createdAt == null) {
                payload["createdAt"] = FieldValue.serverTimestamp()
            } else {
                payload["createdAt"] = application.createdAt
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, collectionRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateApplication(application: JobApplication): Result<Unit> {
        val uid = requireUserId()
        require(application.id.isNotBlank()) { "Application ID must not be blank for update" }
        val docRef = db.collection("users").document(uid).collection("job_applications").document(application.id)
        return try {
            val payload = hashMapOf<String, Any?>(
                "userId" to uid,
                "company" to application.company.trim(),
                "jobTitle" to application.jobTitle.trim(),
                "jobDescription" to application.jobDescription.trim(),
                "applicationDate" to application.applicationDate.trim(),
                "status" to application.status,
                "notes" to application.notes.trim(),
                "deadline" to application.deadline.trim(),
                "salary" to application.salary.trim(),
                "location" to application.location.trim(),
                "tailoredResume" to application.tailoredResume.trim(),
                "tailoredCoverLetter" to application.tailoredCoverLetter.trim(),
                "matchScore" to application.matchScore,
                "matchAnalysis" to application.matchAnalysis.trim(),
                "desiredTone" to application.desiredTone,
                "emphasizedSkills" to application.emphasizedSkills.trim(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (application.createdAt != null) {
                payload["createdAt"] = application.createdAt
            }
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteApplication(userId: String, appId: String): Result<Unit> {
        val docRef = db.collection("users").document(userId).collection("job_applications").document(appId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }
}
