package com.example.smartcampus.domain.model

data class Notice(
    val id: String = "",
    val title: String = "",
    val body: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val postedAt: Long = 0L
)