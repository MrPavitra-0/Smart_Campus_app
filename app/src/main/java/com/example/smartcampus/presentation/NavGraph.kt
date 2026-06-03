package com.example.smartcampus.presentation

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartcampus.presentation.assignment.AssignmentScreen
import com.example.smartcampus.presentation.auth.LoginScreen
import com.example.smartcampus.presentation.auth.RegisterScreen
import com.example.smartcampus.presentation.chat.ChatScreen
import com.example.smartcampus.presentation.chat.UserListScreen
import com.example.smartcampus.presentation.dashboard.FacultyDashboardScreen
import com.example.smartcampus.presentation.dashboard.StudentDashboardScreen
import com.example.smartcampus.presentation.notice.NoticeScreen
import com.example.smartcampus.domain.model.User
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartcampus.presentation.auth.AuthUiState
import com.example.smartcampus.presentation.auth.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object StudentDashboard : Screen("student_dashboard")
    object FacultyDashboard : Screen("faculty_dashboard")
    object Notices : Screen("notices")
    object Assignments : Screen("assignments")
    object UserList : Screen("user_list")
    object Chat : Screen("chat/{chatId}/{otherUserName}") {
        fun createRoute(chatId: String, otherUserName: String) =
            "chat/$chatId/$otherUserName"
    }
}

@Composable
fun SmartCampusNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    currentUserId: String,
    currentUserName: String,
    currentUserRole: String,
    onLogout: () -> Unit,
    onLoginSuccess: (com.example.smartcampus.domain.model.User) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthUiState.Success) {
                    val user = (authState as AuthUiState.Success).user
                    onLoginSuccess(user)
                    if (user.role == "faculty") {
                        navController.navigate(Screen.FacultyDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }

            LoginScreen(
                onLoginSuccess = { role ->
                    if (role == "faculty") {
                        navController.navigate(Screen.FacultyDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                viewModel = authViewModel
            )
        }


        composable(Screen.Register.route) {
            val authViewModel: AuthViewModel = hiltViewModel()
            val authState by authViewModel.authState.collectAsState()

            LaunchedEffect(authState) {
                if (authState is AuthUiState.Success) {
                    val user = (authState as AuthUiState.Success).user
                    onLoginSuccess(user)
                    if (user.role == "faculty") {
                        navController.navigate(Screen.FacultyDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }

            RegisterScreen(
                onRegisterSuccess = { role ->
                    if (role == "faculty") {
                        navController.navigate(Screen.FacultyDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Screen.StudentDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                viewModel = authViewModel
            )
        }

        composable(Screen.StudentDashboard.route) {
            StudentDashboardScreen(
                userName = currentUserName,
                onNavigateToNotices = {
                    navController.navigate(Screen.Notices.route)
                },
                onNavigateToAssignments = {
                    navController.navigate(Screen.Assignments.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.UserList.route)
                },
                onLogout = onLogout
            )
        }

        composable(Screen.FacultyDashboard.route) {
            FacultyDashboardScreen(
                userName = currentUserName,
                onNavigateToNotices = {
                    navController.navigate(Screen.Notices.route)
                },
                onNavigateToAssignments = {
                    navController.navigate(Screen.Assignments.route)
                },
                onNavigateToChat = {
                    navController.navigate(Screen.UserList.route)
                },
                onLogout = onLogout
            )
        }

        composable(Screen.Notices.route) {
            NoticeScreen(
                userRole = currentUserRole,
                userId = currentUserId,
                userName = currentUserName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Assignments.route) {
            AssignmentScreen(
                userRole = currentUserRole,
                userId = currentUserId,
                userName = currentUserName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.UserList.route) {
            UserListScreen(
                currentUserId = currentUserId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToChat = { chatId, otherUserName ->
                    navController.navigate(
                        Screen.Chat.createRoute(chatId, otherUserName)
                    )
                }
            )
        }

        composable(Screen.Chat.route) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""
            ChatScreen(
                chatId = chatId,
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                otherUserName = otherUserName,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}