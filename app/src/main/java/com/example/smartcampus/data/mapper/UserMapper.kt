package com.example.smartcampus.data.mapper


import com.example.smartcampus.domain.model.User

fun Map<String, Any?>.toUser(): User {
    return User(
        uid = this["uid"] as? String ?: "",
        email = this["email"] as? String ?: "",
        displayName = this["displayName"] as? String ?: "",
        role = this["role"] as? String ?: ""
    )
}

fun User.toMap(): Map<String, Any> {
    return mapOf(
        "uid" to uid,
        "email" to email,
        "displayName" to displayName,
        "role" to role
    )
}