package com.example.smartcampus.presentation.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacultyDashboardScreen(
    userName: String,
    onNavigateToNotices: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToChat: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Smart Campus") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Welcome, $userName 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Faculty Dashboard",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item { Spacer(modifier = Modifier.height(8.dp)) }

            item {
                DashboardCard(
                    title = "Notice Board",
                    description = "Post and manage announcements",
                    icon = Icons.Default.Notifications,
                    onClick = onNavigateToNotices
                )
            }

            item {
                DashboardCard(
                    title = "Assignments",
                    description = "Create assignments and view submissions",
                    icon = Icons.Default.Assignment,
                    onClick = onNavigateToAssignments
                )
            }

            item {
                DashboardCard(
                    title = "Chat",
                    description = "Message your students directly",
                    icon = Icons.Default.Chat,
                    onClick = onNavigateToChat
                )
            }
        }
    }
}