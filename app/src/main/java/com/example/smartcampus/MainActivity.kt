package com.example.smartcampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.smartcampus.domain.repository.AuthRepository
import com.example.smartcampus.presentation.SmartCampusNavGraph
import com.example.smartcampus.presentation.Screen
import com.example.smartcampus.ui.theme.SmartCampusTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartCampusTheme {
                SmartCampusApp(authRepository = authRepository)
            }
        }
    }
}

@Composable
fun SmartCampusApp(authRepository: AuthRepository) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    var currentUserId by remember { mutableStateOf("") }
    var currentUserName by remember { mutableStateOf("") }
    var currentUserRole by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var startDestination by remember { mutableStateOf(Screen.Login.route) }

    LaunchedEffect(Unit) {
        if (authRepository.isLoggedIn()) {
            val user = authRepository.getCurrentUser()
            user?.let {
                currentUserId = it.uid
                currentUserName = it.displayName
                currentUserRole = it.role
                startDestination = if (it.role == "faculty") {
                    Screen.FacultyDashboard.route
                } else {
                    Screen.StudentDashboard.route
                }
            }
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        SmartCampusNavGraph(
            navController = navController,
            startDestination = startDestination,
            currentUserId = currentUserId,
            currentUserName = currentUserName,
            currentUserRole = currentUserRole,
            onLogout = {
                coroutineScope.launch {
                    authRepository.logout()
                    currentUserId = ""
                    currentUserName = ""
                    currentUserRole = ""
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            },
            onLoginSuccess = { user ->
                currentUserId = user.uid
                currentUserName = user.displayName
                currentUserRole = user.role
            }
        )
    }
}