package com.example.smartcampus.presentation.assignment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcampus.domain.model.Assignment
import com.example.smartcampus.domain.usecase.GetAssignmentsUseCase
import com.example.smartcampus.domain.repository.AssignmentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

sealed class AssignmentUiState {
    object Loading : AssignmentUiState()
    data class Success(val assignments: List<Assignment>) : AssignmentUiState()
    data class Error(val message: String) : AssignmentUiState()
}

@HiltViewModel
class AssignmentViewModel @Inject constructor(
    private val getAssignmentsUseCase: GetAssignmentsUseCase,
    private val assignmentRepository: AssignmentRepository
) : ViewModel() {

    private val _assignmentState = MutableStateFlow<AssignmentUiState>(AssignmentUiState.Loading)
    val assignmentState: StateFlow<AssignmentUiState> = _assignmentState

    private val _postState = MutableStateFlow<String?>(null)
    val postState: StateFlow<String?> = _postState

    init {
        fetchAssignments()
    }

    private fun fetchAssignments() {
        viewModelScope.launch {
            getAssignmentsUseCase()
                .catch { e ->
                    _assignmentState.value = AssignmentUiState.Error(
                        e.message ?: "Failed to load assignments"
                    )
                }
                .collect { assignments ->
                    _assignmentState.value = AssignmentUiState.Success(assignments)
                }
        }
    }

    fun postAssignment(
        title: String,
        description: String,
        facultyId: String,
        facultyName: String,
        fileUri: Uri? = null
    ) {
        viewModelScope.launch {
            _postState.value = if (fileUri != null) "Uploading file..." else "Posting..."
            val assignment = Assignment(
                title = title,
                description = description,
                facultyId = facultyId,
                facultyName = facultyName,
                postedAt = System.currentTimeMillis()
            )
            assignmentRepository.postAssignment(assignment, fileUri)
                .onSuccess {
                    _postState.value = "Assignment posted successfully!"
                }
                .onFailure { error ->
                    _postState.value = "Failed: ${error.message}"
                }
        }
    }


    fun deleteAssignment(assignmentId: String) {
        viewModelScope.launch {
            assignmentRepository.deleteAssignment(assignmentId)
                .onSuccess {
                    _postState.value = "Assignment deleted"
                }
                .onFailure {
                    _postState.value = "Failed to delete"
                }
        }
    }

    fun editAssignment(
        assignmentId: String,
        title: String,
        description: String,
        fileUri: Uri? = null
    ) {
        viewModelScope.launch {
            assignmentRepository.editAssignment(
                assignmentId = assignmentId,
                title = title,
                description = description,
                fileUri = fileUri
            )
                .onSuccess {
                    _postState.value = "Assignment updated"
                }
                .onFailure {
                    _postState.value = "Failed to update"
                }
        }
    }

    fun resetPostState() {
        _postState.value = null
    }
}