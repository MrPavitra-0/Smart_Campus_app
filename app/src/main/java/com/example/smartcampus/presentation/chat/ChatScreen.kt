package com.example.smartcampus.presentation.chat

import android.Manifest
import android.content.Intent
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.smartcampus.domain.model.Message
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    currentUserId: String,
    currentUserName: String,
    otherUserName: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val chatState by viewModel.chatState.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Audio recorder
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }

    // Image picker
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendImage(chatId, currentUserId, currentUserName, it)
        }
    }

    // File picker
    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = context.contentResolver
                .query(it, null, null, null, null)
                ?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(
                        android.provider.OpenableColumns.DISPLAY_NAME
                    )
                    cursor.moveToFirst()
                    cursor.getString(nameIndex)
                } ?: "file"
            viewModel.sendFile(chatId, currentUserId, currentUserName, it, fileName)
        }
    }


    LaunchedEffect(chatId) {
        viewModel.fetchMessages(chatId)
    }

    LaunchedEffect(chatState) {
        if (chatState is ChatUiState.Success) {
            val messages = (chatState as ChatUiState.Success).messages
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    LaunchedEffect(uploadState) {
        uploadState?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetUploadState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(otherUserName, fontSize = 16.sp)
                        Text(
                            text = if (isRecording) "🔴 Recording..." else "Online",
                            fontSize = 12.sp,
                            color = if (isRecording)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.primary
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Column {
                // Emoji Picker
                if (showEmojiPicker) {
                    EmojiPicker(
                        onEmojiSelected = { emoji ->
                            messageText += emoji
                        },
                        onDismiss = { showEmojiPicker = false }
                    )
                }

                // Attachment menu
                if (showAttachMenu) {
                    AttachmentMenu(
                        onImageClick = {
                            showAttachMenu = false
                            imageLauncher.launch("image/*")
                        },
                        onVideoClick = {
                            showAttachMenu = false
                            imageLauncher.launch("video/*")
                        },
                        onDocumentClick = {
                            showAttachMenu = false
                            fileLauncher.launch("*/*")
                        },
                        onDismiss = { showAttachMenu = false }
                    )
                }

                // Input bar
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Emoji button
                        IconButton(onClick = {
                            showEmojiPicker = !showEmojiPicker
                            showAttachMenu = false
                        }) {
                            Icon(
                                Icons.Default.EmojiEmotions,
                                contentDescription = "Emoji",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Text field
                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = { Text("Type a message...") },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

                        // Attachment button
                        IconButton(onClick = {
                            showAttachMenu = !showAttachMenu
                            showEmojiPicker = false
                        }) {
                            Icon(
                                Icons.Default.AttachFile,
                                contentDescription = "Attach",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Send or Mic button
                        if (messageText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    viewModel.sendMessage(
                                        chatId = chatId,
                                        senderId = currentUserId,
                                        senderName = currentUserName,
                                        text = messageText.trim()
                                    )
                                    messageText = ""
                                    showEmojiPicker = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            // Mic button - hold to record
                            var hasAudioPermission by remember { mutableStateOf(false) }

                            val checkPermission = rememberLauncherForActivityResult(
                                contract = ActivityResultContracts.RequestPermission()
                            ) { granted ->
                                hasAudioPermission = granted
                            }

                            LaunchedEffect(Unit) {
                                val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
                                hasAudioPermission = androidx.core.content.ContextCompat
                                    .checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == permission
                            }

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        if (isRecording) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary,
                                        CircleShape
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onPress = {
                                                if (!hasAudioPermission) {
                                                    checkPermission.launch(Manifest.permission.RECORD_AUDIO)
                                                    return@detectTapGestures
                                                }
                                                // Start recording
                                                try {
                                                    val file = File(
                                                        context.cacheDir,
                                                        "audio_${System.currentTimeMillis()}.m4a"
                                                    )
                                                    audioFile = file
                                                    val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                                        MediaRecorder(context)
                                                    } else {
                                                        @Suppress("DEPRECATION")
                                                        MediaRecorder()
                                                    }
                                                    recorder.apply {
                                                        setAudioSource(MediaRecorder.AudioSource.MIC)
                                                        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                                                        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                                                        setOutputFile(file.absolutePath)
                                                        prepare()
                                                        start()
                                                    }
                                                    mediaRecorder = recorder
                                                    isRecording = true

                                                    // Wait until finger is lifted
                                                    tryAwaitRelease()

                                                    // Stop recording
                                                    recorder.apply {
                                                        stop()
                                                        release()
                                                    }
                                                    mediaRecorder = null
                                                    isRecording = false

                                                    // Send audio
                                                    val uri = Uri.fromFile(file)
                                                    viewModel.sendAudio(
                                                        chatId,
                                                        currentUserId,
                                                        currentUserName,
                                                        uri
                                                    )
                                                } catch (e: Exception) {
                                                    mediaRecorder?.release()
                                                    mediaRecorder = null
                                                    isRecording = false
                                                }
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = if (isRecording) "Stop" else "Hold to record",
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        when (chatState) {
            is ChatUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is ChatUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (chatState as ChatUiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            is ChatUiState.Success -> {
                val messages = (chatState as ChatUiState.Success).messages
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 8.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(messages) { message ->
                        MessageBubble(
                            message = message,
                            isCurrentUser = message.senderId == currentUserId
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmojiPicker(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val emojis = listOf(
        "😀", "😂", "😍", "🥰", "😎", "🤔", "😢", "😡",
        "👍", "👎", "👏", "🙏", "🤝", "✌️", "🤞", "💪",
        "❤️", "🔥", "⭐", "✅", "❌", "💯", "🎉", "🎊",
        "📚", "📝", "📌", "📎", "🖊️", "📅", "⏰", "🔔",
        "😊", "😇", "🤣", "😅", "😆", "😋", "😜", "🤩",
        "👋", "🤙", "☝️", "🖐️", "🤚", "👌", "🤌", "🫡"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Emojis",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // Emoji grid
            val rows = emojis.chunked(8)
            rows.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { emoji ->
                        TextButton(
                            onClick = { onEmojiSelected(emoji) },
                            contentPadding = PaddingValues(4.dp),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text(text = emoji, fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentMenu(
    onImageClick: () -> Unit,
    onVideoClick: () -> Unit,
    onDocumentClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            AttachmentOption(
                icon = Icons.Default.Image,
                label = "Image",
                color = Color(0xFF4CAF50),
                onClick = onImageClick
            )
            AttachmentOption(
                icon = Icons.Default.VideoFile,
                label = "Video",
                color = Color(0xFF2196F3),
                onClick = onVideoClick
            )
            AttachmentOption(
                icon = Icons.Default.InsertDriveFile,
                label = "Document",
                color = Color(0xFFFF9800),
                onClick = onDocumentClick
            )
        }
    }
}

@Composable
fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(56.dp)
                .background(color.copy(alpha = 0.15f), CircleShape)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCurrentUser) Alignment.End else Alignment.Start
    ) {
        if (!isCurrentUser) {
            Text(
                text = message.senderName,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            )
        }

        Surface(
            color = if (isCurrentUser)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isCurrentUser) 16.dp else 4.dp,
                bottomEnd = if (isCurrentUser) 4.dp else 16.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                when (message.messageType) {
                    "image" -> {
                        Image(
                            painter = rememberAsyncImagePainter(message.imageUrl),
                            contentDescription = "Image",
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                    "file" -> {
                        Column(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable {
                                    if (message.fileUrl.isNotEmpty()) {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse(message.fileUrl)
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.InsertDriveFile,
                                    contentDescription = null,
                                    tint = if (isCurrentUser)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Column {
                                    Text(
                                        text = message.fileName.ifEmpty { "File" },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Tap to open",
                                        fontSize = 11.sp,
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    "audio" -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse(message.audioUrl)
                                    )
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = "Play",
                                    tint = if (isCurrentUser)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Text(
                                text = "Audio message",
                                fontSize = 13.sp,
                                color = if (isCurrentUser)
                                    MaterialTheme.colorScheme.onPrimary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = SimpleDateFormat(
                        "hh:mm a",
                        Locale.getDefault()
                    ).format(Date(message.sentAt)),
                    fontSize = 10.sp,
                    color = if (isCurrentUser)
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}