package com.example.smartcampus.domain.repository

import android.net.Uri
import com.example.smartcampus.domain.model.Assignment
import com.example.smartcampus.domain.model.Submission
import kotlinx.coroutines.flow.Flow

interface AssignmentRepository {
    fun getAssignments(): Flow<List<Assignment>>
    suspend fun postAssignment(assignment: Assignment): Result<Unit>
    suspend fun submitAssignment(submission: Submission, fileUri: Uri): Result<Unit>
    fun getSubmissions(assignmentId: String): Flow<List<Submission>>
}