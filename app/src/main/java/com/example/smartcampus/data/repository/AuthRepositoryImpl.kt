package com.example.smartcampus.data.repository

import com.example.smartcampus.data.mapper.toMap
import com.example.smartcampus.data.mapper.toUser
import com.example.smartcampus.domain.model.User
import com.example.smartcampus.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun login(email: String, password: String): Result<User> {
        return runCatching {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            val doc = firestore.collection("users").document(uid).get().await()
            doc.data?.toUser() ?: throw Exception("User data not found")
        }
    }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        role: String
    ): Result<User> {
        return runCatching {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Registration failed")
            val user = User(
                uid = uid,
                email = email,
                displayName = displayName,
                role = role
            )
            firestore.collection("users").document(uid).set(user.toMap()).await()
            user
        }
    }

    override suspend fun logout() {
        auth.signOut()
    }

    override suspend fun getCurrentUser(): User? {
        val uid = auth.currentUser?.uid ?: return null
        return runCatching {
            val doc = firestore.collection("users").document(uid).get().await()
            doc.data?.toUser()
        }.getOrNull()
    }

    override fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}