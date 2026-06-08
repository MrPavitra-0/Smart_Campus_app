package com.example.smartcampus.presentation.assignment

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.smartcampus.domain.model.Assignment
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssignmentScreen(
    userRole: String,
    userId: String,
    userName: String,
    onNavigateBack: () -> Unit,
    viewModel: AssignmentViewModel = hiltViewModel()
) {
    val assignmentState by viewModel.assignmentState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showDetailDialog by remember { mutableStateOf<Assignment?>(null) }
    var showEditDialog by remember { mutableStateOf<Assignment?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val postState by viewModel.postState.collectAsState()
    val context = LocalContext.current


    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // Take persistent permission
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(it, takeFlags)
            selectedFileUri = it
            selectedFileName = context.contentResolver
                .query(it, null, null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "Selected file"
        }
    }

    LaunchedEffect(postState) {
        postState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetPostState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assignments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (userRole == "faculty") {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Assignment")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        when (assignmentState) {
            is AssignmentUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AssignmentUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (assignmentState as AssignmentUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is AssignmentUiState.Success -> {
                val assignments = (assignmentState as AssignmentUiState.Success).assignments
                if (assignments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No assignments yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(assignments) { assignment ->
                            AssignmentCard(
                                assignment = assignment,
                                userRole = userRole,
                                onCardClick = { showDetailDialog = assignment },
                                onEdit = { showEditDialog = assignment },
                                onDelete = { viewModel.deleteAssignment(assignment.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Create Assignment Dialog
    if (showCreateDialog) {
        AssignmentDialog(
            title = title,
            description = description,
            selectedFileName = selectedFileName,
            onTitleChange = { title = it },
            onDescriptionChange = { description = it },
            onAttachFile = { filePickerLauncher.launch(arrayOf("*/*")) },
            onConfirm = {
                if (title.isNotBlank() && description.isNotBlank()) {
                    viewModel.postAssignment(
                        title = title,
                        description = description,
                        facultyId = userId,
                        facultyName = userName,
                        fileUri = selectedFileUri
                    )
                    title = ""
                    description = ""
                    selectedFileUri = null
                    selectedFileName = ""
                    showCreateDialog = false
                }
            },
            onDismiss = {
                showCreateDialog = false
                title = ""
                description = ""
                selectedFileUri = null
                selectedFileName = ""
            },
            dialogTitle = "Create Assignment"
        )
    }

    // Edit Assignment Dialog
    showEditDialog?.let { assignment ->
        var editTitle by remember { mutableStateOf(assignment.title) }
        var editDescription by remember { mutableStateOf(assignment.description) }
        var editFileUri by remember { mutableStateOf<Uri?>(null) }
        var editFileName by remember { mutableStateOf("") }

        val editFileLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                editFileUri = it
                editFileName = context.contentResolver
                    .query(it, null, null, null, null)
                    ?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(
                            android.provider.OpenableColumns.DISPLAY_NAME
                        )
                        cursor.moveToFirst()
                        cursor.getString(nameIndex)
                    } ?: "Selected file"
            }
        }

        AssignmentDialog(
            title = editTitle,
            description = editDescription,
            selectedFileName = editFileName,
            onTitleChange = { editTitle = it },
            onDescriptionChange = { editDescription = it },
            onAttachFile = { editFileLauncher.launch("*/*") },
            onConfirm = {
                if (editTitle.isNotBlank() && editDescription.isNotBlank()) {
                    viewModel.editAssignment(
                        assignmentId = assignment.id,
                        title = editTitle,
                        description = editDescription,
                        fileUri = editFileUri
                    )
                    showEditDialog = null
                }
            },
            onDismiss = { showEditDialog = null },
            dialogTitle = "Edit Assignment"
        )
    }

    // Detail/View Dialog
    showDetailDialog?.let { assignment ->
        AlertDialog(
            onDismissRequest = { showDetailDialog = null },
            title = {
                Text(
                    text = assignment.title,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = assignment.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider()
                    Text(
                        text = "Posted by: ${assignment.facultyName}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Date: ${
                            SimpleDateFormat(
                                "dd MMM yyyy",
                                Locale.getDefault()
                            ).format(Date(assignment.postedAt))
                        }",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (assignment.fileUrl.isNotEmpty()) {
                        Divider()
                        Button(
                            onClick = {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(assignment.fileUrl)
                                )
                                context.startActivity(intent)
                                showDetailDialog = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Download / View File")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDetailDialog = null }) {
                    Text("Close")
                }
            }
        )
    }
}

// Reusable dialog for create/edit
@Composable
fun AssignmentDialog(
    title: String,
    description: String,
    selectedFileName: String,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onAttachFile: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dialogTitle: String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(dialogTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedButton(
                    onClick = onAttachFile,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedFileName.isEmpty())
                            "Attach File (PDF/Image)"
                        else selectedFileName,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (selectedFileName.isNotEmpty()) {
                    Text(
                        text = "✓ $selectedFileName",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Confirm") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AssignmentCard(
    assignment: Assignment,
    userRole: String,
    onCardClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Assignment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = assignment.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                if (userRole == "faculty") {
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Options"
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit") },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                },
                                onClick = {
                                    showMenu = false
                                    onEdit()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        "Delete",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = assignment.description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "By ${assignment.facultyName}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.getDefault()
                        ).format(Date(assignment.postedAt)),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (assignment.fileUrl.isNotEmpty()) {
                    AssistChip(
                        onClick = { onCardClick() },
                        label = { Text("Has file", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Assignment") },
            text = { Text("Are you sure you want to delete this assignment?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}