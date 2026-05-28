package com.example.smartcampus.data.repository

import android.net.Uri
import com.example.smartcampus.domain.model.Assignment
import com.example.smartcampus.domain.model.Submission
import com.example.smartcampus.domain.repository.AssignmentRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AssignmentRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
) : AssignmentRepository {

    override fun getAssignments(): Flow<List<Assignment>> = callbackFlow {
        val listener = firestore.collection("assignments")
            .orderBy("postedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val assignments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Assignment::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(assignments)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun postAssignment(assignment: Assignment): Result<Unit> {
        return runCatching {
            firestore.collection("assignments").add(assignment).await()
            Unit
        }
    }

    override suspend fun submitAssignment(
        submission: Submission,
        fileUri: Uri
    ): Result<Unit> {
        return runCatching {
            val storageRef = storage.reference
                .child("submissions/${submission.studentId}/${submission.assignmentId}/${System.currentTimeMillis()}")
            storageRef.putFile(fileUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            val finalSubmission = submission.copy(fileUrl = downloadUrl)
            firestore.collection("submissions").add(finalSubmission).await()
            Unit
        }
    }

    override fun getSubmissions(assignmentId: String): Flow<List<Submission>> = callbackFlow {
        val listener = firestore.collection("submissions")
            .whereEqualTo("assignmentId", assignmentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val submissions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Submission::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(submissions)
            }
        awaitClose { listener.remove() }
    }
}