package com.example.smartcampus.data.repository

import android.net.Uri
import com.example.smartcampus.data.mapper.toMap
import com.example.smartcampus.data.mapper.toMessage
import com.example.smartcampus.data.mapper.toUser
import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import com.example.smartcampus.domain.repository.ChatRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage
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

    override suspend fun sendImageMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        imageUri: Uri
    ): Result<Unit> {
        return runCatching {
            val storageRef = storage.reference
                .child("chats/$chatId/images/${System.currentTimeMillis()}")
            storageRef.putFile(imageUri).await()
            val imageUrl = storageRef.downloadUrl.await().toString()
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                imageUrl = imageUrl,
                messageType = "image",
                sentAt = System.currentTimeMillis()
            )
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message.toMap())
                .await()
            Unit
        }
    }

    override suspend fun sendFileMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        fileUri: Uri,
        fileName: String
    ): Result<Unit> {
        return runCatching {
            val storageRef = storage.reference
                .child("chats/$chatId/files/${System.currentTimeMillis()}_$fileName")
            storageRef.putFile(fileUri).await()
            val fileUrl = storageRef.downloadUrl.await().toString()
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                fileUrl = fileUrl,
                fileName = fileName,
                messageType = "file",
                sentAt = System.currentTimeMillis()
            )
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message.toMap())
                .await()
            Unit
        }
    }

    override suspend fun sendAudioMessage(
        chatId: String,
        senderId: String,
        senderName: String,
        audioUri: Uri
    ): Result<Unit> {
        return runCatching {
            val storageRef = storage.reference
                .child("chats/$chatId/audio/${System.currentTimeMillis()}.m4a")
            storageRef.putFile(audioUri).await()
            val audioUrl = storageRef.downloadUrl.await().toString()
            val message = Message(
                chatId = chatId,
                senderId = senderId,
                senderName = senderName,
                audioUrl = audioUrl,
                messageType = "audio",
                sentAt = System.currentTimeMillis()
            )
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .add(message.toMap())
                .await()
            Unit
        }
    }

    override suspend fun deleteMessageForMe(
        chatId: String,
        messageId: String,
        userId: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update("deletedFor", FieldValue.arrayUnion(userId))
                .await()
            Unit
        }
    }

    override suspend fun deleteMessageForEveryone(
        chatId: String,
        messageId: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "deletedForAll" to true,
                        "text" to "This message was deleted",
                        "imageUrl" to "",
                        "fileUrl" to "",
                        "audioUrl" to "",
                        "messageType" to "text"
                    )
                )
                .await()
            Unit
        }
    }

    override suspend fun editMessage(
        chatId: String,
        messageId: String,
        newText: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("chats")
                .document(chatId)
                .collection("messages")
                .document(messageId)
                .update(
                    mapOf(
                        "text" to newText,
                        "isEdited" to true,
                        "editedAt" to System.currentTimeMillis()
                    )
                )
                .await()
            Unit
        }
    }

    override suspend fun forwardMessage(
        message: Message,
        targetChatId: String,
        senderId: String,
        senderName: String
    ): Result<Unit> {
        return runCatching {
            val forwardedMessage = message.copy(
                id = "",
                chatId = targetChatId,
                senderId = senderId,
                senderName = senderName,
                sentAt = System.currentTimeMillis(),
                replyToId = "",
                replyToText = "",
                replyToSender = "",
                isEdited = false,
                deletedForAll = false,
                deletedFor = emptyList()
            )
            firestore.collection("chats")
                .document(targetChatId)
                .collection("messages")
                .add(forwardedMessage.toMap())
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