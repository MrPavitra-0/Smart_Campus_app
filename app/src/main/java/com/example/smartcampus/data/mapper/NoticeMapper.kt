package com.example.smartcampus.data.mapper

import com.example.smartcampus.domain.model.Notice

fun Map<String, Any?>.toNotice(id: String): Notice {
    return Notice(
        id = id,
        title = this["title"] as? String ?: "",
        body = this["body"] as? String ?: "",
        authorId = this["authorId"] as? String ?: "",
        authorName = this["authorName"] as? String ?: "",
        postedAt = this["postedAt"] as? Long ?: 0L
    )
}

fun Notice.toMap(): Map<String, Any> {
    return mapOf(
        "title" to title,
        "body" to body,
        "authorId" to authorId,
        "authorName" to authorName,
        "postedAt" to postedAt
    )
}