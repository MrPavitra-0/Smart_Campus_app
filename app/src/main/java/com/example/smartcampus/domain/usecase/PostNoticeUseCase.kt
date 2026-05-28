package com.example.smartcampus.domain.usecase

import com.example.smartcampus.domain.model.Notice
import com.example.smartcampus.domain.repository.NoticeRepository
import javax.inject.Inject

class PostNoticeUseCase @Inject constructor(
    private val noticeRepository: NoticeRepository
) {
    suspend operator fun invoke(notice: Notice) =
        noticeRepository.postNotice(notice)
}