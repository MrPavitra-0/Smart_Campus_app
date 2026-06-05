package com.example.smartcampus.data.repository

import com.example.smartcampus.data.mapper.toMap
import com.example.smartcampus.data.mapper.toNotice
import com.example.smartcampus.domain.model.Notice
import com.example.smartcampus.domain.repository.NoticeRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NoticeRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : NoticeRepository {

    override fun getNotices(): Flow<List<Notice>> = callbackFlow {
        val listener = firestore.collection("notices")
            .orderBy("postedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val notices = snapshot?.documents?.mapNotNull { doc ->
                    doc.data?.toNotice(doc.id)
                } ?: emptyList()
                trySend(notices)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun editNotice(
        noticeId: String,
        title: String,
        body: String
    ): Result<Unit> {
        return runCatching {
            firestore.collection("notices")
                .document(noticeId)
                .update(
                    mapOf(
                        "title" to title,
                        "body" to body
                    )
                )
                .await()
            Unit
        }
    }

    override suspend fun postNotice(notice: Notice): Result<Unit> {
        return runCatching {
            firestore.collection("notices").add(notice.toMap()).await()
            Unit
        }
    }

    override suspend fun deleteNotice(noticeId: String): Result<Unit> {
        return runCatching {
            firestore.collection("notices").document(noticeId).delete().await()
            Unit
        }
    }
}