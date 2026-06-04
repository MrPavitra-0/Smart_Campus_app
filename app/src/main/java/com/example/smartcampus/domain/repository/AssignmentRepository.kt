package com.example.smartcampus.domain.repository

import android.net.Uri
import com.example.smartcampus.domain.model.Assignment
import com.example.smartcampus.domain.model.Submission
import kotlinx.coroutines.flow.Flow


interface AssignmentRepository {
    fun getAssignments(): Flow<List<Assignment>>
    suspend fun postAssignment(assignment: Assignment, fileUri: Uri? = null): Result<Unit>
    suspend fun submitAssignment(submission: Submission, fileUri: Uri): Result<Unit>

    suspend fun deleteAssignment(assignmentId: String): Result<Unit>
    suspend fun editAssignment(
        assignmentId: String,
        title: String,
        description: String,
        fileUri: Uri? = null
    ): Result<Unit>
    fun getSubmissions(assignmentId: String): Flow<List<Submission>>
}