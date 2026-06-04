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
            return try {
                val doc = firestore.collection("users").document(uid).get().await()
                val user = doc.data?.toUser()
                if (user != null) {
                    Result.success(user)
                } else {
                    // User doc doesn't exist, create basic one
                    val basicUser = User(
                        uid = uid,
                        email = email,
                        displayName = email.substringBefore("@"),
                        role = "student"
                    )
                    firestore.collection("users").document(uid).set(basicUser.toMap()).await()
                    Result.success(basicUser)
                }
            } catch (e: Exception) {
                // If Firestore fails, return basic user from Auth
                val basicUser = User(
                    uid = uid,
                    email = email,
                    displayName = email.substringBefore("@"),
                    role = "student"
                )
                Result.success(basicUser)
            }
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