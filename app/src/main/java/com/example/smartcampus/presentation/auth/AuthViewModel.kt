package com.example.smartcampus.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smartcampus.domain.model.User
import com.example.smartcampus.domain.usecase.LoginUseCase
import com.example.smartcampus.domain.usecase.RegisterUseCase
import com.example.smartcampus.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: User) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val registerUseCase: RegisterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState

    fun isLoggedIn() = authRepository.isLoggedIn()

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            loginUseCase(email, password)
                .onSuccess { user ->
                    _authState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthUiState.Error(
                        error.message ?: "Login failed"
                    )
                }
        }
    }

    fun register(
        email: String,
        password: String,
        displayName: String,
        role: String
    ) {
        viewModelScope.launch {
            _authState.value = AuthUiState.Loading
            registerUseCase(email, password, displayName, role)
                .onSuccess { user ->
                    _authState.value = AuthUiState.Success(user)
                }
                .onFailure { error ->
                    _authState.value = AuthUiState.Error(
                        error.message ?: "Registration failed"
                    )
                }
        }
    }

    fun resetState() {
        _authState.value = AuthUiState.Idle
    }
}