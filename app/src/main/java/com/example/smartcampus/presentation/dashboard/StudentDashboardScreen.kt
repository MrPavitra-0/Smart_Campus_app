package com.example.smartcampus.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDashboardScreen(
    userName: String,
    onNavigateToNotices: () -> Unit,
    onNavigateToAssignments: () -> Unit,
    onNavigateToChat: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Smart Campus",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                // Welcome header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF1A237E),
                                    Color(0xFF3949AB)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column {
                        Text(
                            text = "Welcome back,",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "$userName 👋",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Student Dashboard",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Quick Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                DashboardCard(
                    title = "Notice Board",
                    description = "View latest announcements",
                    icon = Icons.Default.Notifications,
                    gradient = listOf(
                        Color(0xFF6A1B9A),
                        Color(0xFFAB47BC)
                    ),
                    onClick = onNavigateToNotices
                )
            }

            item {
                DashboardCard(
                    title = "Assignments",
                    description = "View & submit your work",
                    icon = Icons.Default.Assignment,
                    gradient = listOf(
                        Color(0xFF0277BD),
                        Color(0xFF29B6F6)
                    ),
                    onClick = onNavigateToAssignments
                )
            }

            item {
                DashboardCard(
                    title = "Chat",
                    description = "Message faculty directly",
                    icon = Icons.Default.Chat,
                    gradient = listOf(
                        Color(0xFF00695C),
                        Color(0xFF26A69A)
                    ),
                    onClick = onNavigateToChat
                )
            }
        }
    }
}