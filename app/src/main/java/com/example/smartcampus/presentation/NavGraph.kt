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
    onLogout: () -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
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
                }
            )
        }

        composable(Screen.Register.route) {
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
                }
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