package com.example.smartcampus

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
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

    val startDestination = remember {
        if (authRepository.isLoggedIn()) {
            Screen.StudentDashboard.route
        } else {
            Screen.Login.route
        }
    }

    LaunchedEffect(Unit) {
        if (authRepository.isLoggedIn()) {
            val user = authRepository.getCurrentUser()
            user?.let {
                currentUserId = it.uid
                currentUserName = it.displayName
                currentUserRole = it.role
            }
        }
    }

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
        }
    )
}