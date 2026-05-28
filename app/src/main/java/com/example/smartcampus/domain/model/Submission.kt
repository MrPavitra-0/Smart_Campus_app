package com.example.smartcampus.domain.model

data class Submission(
    val id: String = "",
    val assignmentId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val fileUrl: String = "",
    val fileName: String = "",
    val submittedAt: Long = 0L
)