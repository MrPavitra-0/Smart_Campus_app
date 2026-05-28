package com.example.smartcampus.domain.usecase


import com.example.smartcampus.domain.repository.NoticeRepository
import javax.inject.Inject

class GetNoticesUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository
) {
    operator fun invoke() = noticeRepository.getNotices()
}