package com.example.smartcampus.domain.repository

import com.example.smartcampus.domain.model.Notice
import kotlinx.coroutines.flow.Flow

interface NoticeRepository {
    fun getNotices(): Flow<List<Notice>>
    suspend fun postNotice(notice: Notice): Result<Unit>
    suspend fun deleteNotice(noticeId: String): Result<Unit>
}