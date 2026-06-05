package com.example.smartcampus.presentation.notice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcampus.domain.model.Notice
import com.example.smartcampus.domain.usecase.GetNoticesUseCase
import com.example.smartcampus.domain.usecase.PostNoticeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.smartcampus.domain.repository.NoticeRepository

sealed class NoticeUiState {
    object Loading : NoticeUiState()
    data class Success(val notices: List<Notice>) : NoticeUiState()
    data class Error(val message: String) : NoticeUiState()
}

@HiltViewModel
class NoticeViewModel @Inject constructor(
    private val getNoticesUseCase: GetNoticesUseCase,
    private val postNoticeUseCase: PostNoticeUseCase,
    private val noticeRepository: NoticeRepository
) : ViewModel() {

    private val _noticeState = MutableStateFlow<NoticeUiState>(NoticeUiState.Loading)
    val noticeState: StateFlow<NoticeUiState> = _noticeState

    private val _postState = MutableStateFlow<String?>(null)
    val postState: StateFlow<String?> = _postState

    init {
        fetchNotices()
    }

    private fun fetchNotices() {
        viewModelScope.launch {
            getNoticesUseCase()
                .catch { e ->
                    _noticeState.value = NoticeUiState.Error(
                        e.message ?: "Failed to load notices"
                    )
                }
                .collect { notices ->
                    _noticeState.value = NoticeUiState.Success(notices)
                }
        }
    }

    fun postNotice(title: String, body: String, authorId: String, authorName: String) {
        viewModelScope.launch {
            val notice = Notice(
                title = title,
                body = body,
                authorId = authorId,
                authorName = authorName,
                postedAt = System.currentTimeMillis()
            )
            postNoticeUseCase(notice)
                .onSuccess {
                    _postState.value = "Notice posted successfully"
                }
                .onFailure { error ->
                    _postState.value = error.message ?: "Failed to post notice"
                }
        }
    }

    fun deleteNotice(noticeId: String) {
        viewModelScope.launch {
            noticeRepository.deleteNotice(noticeId)
                .onSuccess { _postState.value = "Notice deleted" }
                .onFailure { _postState.value = "Failed to delete notice" }
        }
    }

    fun editNotice(noticeId: String, title: String, body: String) {
        viewModelScope.launch {
            noticeRepository.editNotice(noticeId, title, body)
                .onSuccess { _postState.value = "Notice updated" }
                .onFailure { _postState.value = "Failed to update notice" }
        }
    }

    fun resetPostState() {
        _postState.value = null
    }
}