package com.example.smartcampus.domain.usecase

import com.example.smartcampus.domain.repository.AssignmentRepository
import javax.inject.Inject

class GetAssignmentsUseCase @Inject constructor(
    private val assignmentRepository: AssignmentRepository
) {
    operator fun invoke() = assignmentRepository.getAssignments()
}