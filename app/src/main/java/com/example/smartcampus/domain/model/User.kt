package com.example.smartcampus.domain.model

data class User(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "" // "student" or "faculty"
)