package com.example.smartcampus.data.repository

import com.example.smartcampus.data.mapper.toMap
import com.example.smartcampus.data.mapper.toMessage
import com.example.smartcampus.data.mapper.toUser
import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import com.example.smartcampus.domain.repository.ChatRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : ChatRepository {

    override fun getMessages(chatId: String): Flow<List<Message>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toMessage(doc.id)
                } ?: emptyList()
                trySend(messages)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun sendMessage(message: Message): Result<Unit> {
        return runCatching {
            firestore.collection("chats")
                .document(message.chatId)
                .collection("messages")
                .add(message.toMap())
                .await()
            Unit
        }
    }

    override fun getUsers(): Flow<List<User>> = callbackFlow {
        val listener = firestore.collection("users")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val users = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toUser()
                } ?: emptyList()
                trySend(users)
            }
        awaitClose { listener.remove() }
    }
}