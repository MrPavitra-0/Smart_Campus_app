package com.example.smartcampus.domain.usecase


import com.example.smartcampus.domain.repository.AuthRepository
import javax.inject.Inject

class RegisterUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(
        email: String,
        password: String,
        displayName: String,
        role: String
    ) = authRepository.register(email, password, displayName, role)
}