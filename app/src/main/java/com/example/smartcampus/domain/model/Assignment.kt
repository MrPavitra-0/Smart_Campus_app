package com.example.smartcampus.domain.model

data class Assignment(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val facultyId: String = "",
    val facultyName: String = "",
    val fileUrl: String = "",
    val dueAt: Long = 0L,
    val postedAt: Long = 0L
)