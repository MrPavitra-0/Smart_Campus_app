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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import com.example.smartcampus.domain.model.Message
import com.example.smartcampus.domain.model.User
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.ExperimentalFoundationApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    val users by viewModel.users.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showAttachMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingStartTime by remember { mutableStateOf(0L) }
    var replyingTo by remember { mutableStateOf<Message?>(null) }
    var editingMessage by remember { mutableStateOf<Message?>(null) }
    var showForwardDialog by remember { mutableStateOf<Message?>(null) }
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var audioFile by remember { mutableStateOf<File?>(null) }
    var hasAudioPermission by remember { mutableStateOf(false) }

    val checkPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasAudioPermission = granted }

    LaunchedEffect(Unit) {
        val permission = android.content.pm.PackageManager.PERMISSION_GRANTED
        hasAudioPermission = androidx.core.content.ContextCompat
            .checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == permission
    }

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendImage(chatId, currentUserId, currentUserName, it) }
    }

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

    LaunchedEffect(chatId) { viewModel.fetchMessages(chatId) }

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

    // Forward dialog
    showForwardDialog?.let { message ->
        ForwardDialog(
            users = users.filter { it.uid != currentUserId },
            onForward = { targetUser ->
                val targetChatId = listOf(currentUserId, targetUser.uid)
                    .sorted().joinToString("_")
                viewModel.forwardMessage(
                    message, targetChatId, currentUserId, currentUserName
                )
                showForwardDialog = null
            },
            onDismiss = { showForwardDialog = null }
        )
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
                            else MaterialTheme.colorScheme.primary
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
                if (showEmojiPicker) {
                    EmojiPicker(
                        onEmojiSelected = { messageText += it },
                        onDismiss = { showEmojiPicker = false }
                    )
                }
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

                // Reply preview bar
                replyingTo?.let { reply ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(36.dp)
                                    .background(
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(2.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = reply.senderName,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = when (reply.messageType) {
                                        "image" -> "📷 Image"
                                        "audio" -> "🎵 Audio"
                                        "file" -> "📄 ${reply.fileName}"
                                        else -> reply.text
                                    },
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { replyingTo = null }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel reply",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
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

                        OutlinedTextField(
                            value = messageText,
                            onValueChange = { messageText = it },
                            placeholder = {
                                Text(
                                    if (editingMessage != null) "Edit message..."
                                    else "Type a message..."
                                )
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(24.dp)
                        )

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

                        if (messageText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    if (editingMessage != null) {
                                        viewModel.editMessage(
                                            chatId,
                                            editingMessage!!.id,
                                            messageText.trim()
                                        )
                                        editingMessage = null
                                        messageText = ""
                                    } else {
                                        val reply = replyingTo
                                        val message = Message(
                                            chatId = chatId,
                                            senderId = currentUserId,
                                            senderName = currentUserName,
                                            text = messageText.trim(),
                                            messageType = "text",
                                            replyToId = reply?.id ?: "",
                                            replyToText = when (reply?.messageType) {
                                                "image" -> "📷 Image"
                                                "audio" -> "🎵 Audio"
                                                "file" -> "📄 ${reply.fileName}"
                                                else -> reply?.text ?: ""
                                            },
                                            replyToSender = reply?.senderName ?: "",
                                            sentAt = System.currentTimeMillis()
                                        )
                                        viewModel.sendMessage(message)
                                        messageText = ""
                                        replyingTo = null
                                        showEmojiPicker = false
                                    }
                                }
                            ) {
                                Icon(
                                    if (editingMessage != null)
                                        Icons.Default.Check
                                    else
                                        Icons.Default.Send,
                                    contentDescription = "Send",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            if (isRecording) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            try {
                                                mediaRecorder?.apply { stop(); release() }
                                                mediaRecorder = null
                                                audioFile?.delete()
                                                audioFile = null
                                                isRecording = false
                                            } catch (e: Exception) {
                                                mediaRecorder = null
                                                isRecording = false
                                            }
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Cancel",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                    Text(
                                        text = "Recording...",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            )
                                            .clickable {
                                                try {
                                                    val recordingDuration =
                                                        System.currentTimeMillis() - recordingStartTime
                                                    if (recordingDuration < 1000) return@clickable
                                                    mediaRecorder?.apply { stop(); release() }
                                                    mediaRecorder = null
                                                    isRecording = false
                                                    audioFile?.let { file ->
                                                        val uri = androidx.core.content.FileProvider
                                                            .getUriForFile(
                                                                context,
                                                                "${context.packageName}.fileprovider",
                                                                file
                                                            )
                                                        viewModel.sendAudio(
                                                            chatId, currentUserId, currentUserName, uri
                                                        )
                                                    }
                                                } catch (e: Exception) {
                                                    mediaRecorder = null
                                                    isRecording = false
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            contentDescription = "Send audio",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(
                                            MaterialTheme.colorScheme.primary,
                                            CircleShape
                                        )
                                        .clickable {
                                            if (!hasAudioPermission) {
                                                checkPermission.launch(Manifest.permission.RECORD_AUDIO)
                                                return@clickable
                                            }
                                            try {
                                                val file = File(
                                                    context.cacheDir,
                                                    "audio_${System.currentTimeMillis()}.m4a"
                                                )
                                                audioFile = file
                                                val recorder =
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                                                recordingStartTime = System.currentTimeMillis()
                                            } catch (e: Exception) {
                                                mediaRecorder?.release()
                                                mediaRecorder = null
                                                isRecording = false
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = "Tap to record",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
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
                ) { CircularProgressIndicator() }
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
                    .filter { msg ->
                        !msg.deletedForAll && !msg.deletedFor.contains(currentUserId)
                    }
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
                            isCurrentUser = message.senderId == currentUserId,
                            onReply = { replyingTo = message },
                            onEdit = {
                                editingMessage = message
                                messageText = message.text
                            },
                            onDeleteForMe = {
                                viewModel.deleteForMe(chatId, message.id, currentUserId)
                            },
                            onDeleteForEveryone = {
                                viewModel.deleteForEveryone(chatId, message.id)
                            },
                            onForward = { showForwardDialog = message },
                            canEdit = message.senderId == currentUserId &&
                                    System.currentTimeMillis() - message.sentAt < 5 * 60 * 1000 &&
                                    message.messageType == "text"
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: Message,
    isCurrentUser: Boolean,
    onReply: () -> Unit,
    onEdit: () -> Unit,
    onDeleteForMe: () -> Unit,
    onDeleteForEveryone: () -> Unit,
    onForward: () -> Unit,
    canEdit: Boolean
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

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

        Box {
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
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .combinedClickable(
                        onClick = {},
                        onLongClick = { showMenu = true }
                    )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Reply preview
                    if (message.replyToId.isNotEmpty()) {
                        Surface(
                            color = if (isCurrentUser)
                                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f)
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(2.dp)
                                        .height(32.dp)
                                        .background(
                                            if (isCurrentUser)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(1.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = message.replyToSender,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = message.replyToText,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

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
                            var isPlaying by remember { mutableStateOf(false) }
                            var mediaPlayer by remember {
                                mutableStateOf<android.media.MediaPlayer?>(null)
                            }
                            var progress by remember { mutableStateOf(0f) }
                            var duration by remember { mutableStateOf(0) }

                            LaunchedEffect(isPlaying) {
                                while (isPlaying) {
                                    mediaPlayer?.let {
                                        if (it.isPlaying) {
                                            progress =
                                                it.currentPosition.toFloat() / it.duration.toFloat()
                                            duration = it.duration
                                        }
                                    }
                                    kotlinx.coroutines.delay(200)
                                }
                            }

                            DisposableEffect(message.id) {
                                onDispose {
                                    mediaPlayer?.apply {
                                        if (isPlaying) stop()
                                        release()
                                    }
                                    mediaPlayer = null
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .widthIn(min = 180.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        if (isPlaying) {
                                            mediaPlayer?.pause()
                                            isPlaying = false
                                        } else {
                                            if (mediaPlayer == null) {
                                                mediaPlayer =
                                                    android.media.MediaPlayer().apply {
                                                        setDataSource(message.audioUrl)
                                                        prepareAsync()
                                                        setOnPreparedListener {
                                                            start()
                                                            isPlaying = true
                                                        }
                                                        setOnCompletionListener {
                                                            isPlaying = false
                                                            progress = 0f
                                                        }
                                                    }
                                            } else {
                                                mediaPlayer?.start()
                                                isPlaying = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying)
                                            Icons.Default.Pause
                                        else
                                            Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    LinearProgressIndicator(
                                        progress = progress,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        trackColor = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.3f)
                                        else
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (duration > 0) {
                                            val seconds =
                                                (duration * progress / 1000).toInt()
                                            val total = duration / 1000
                                            "${seconds}s / ${total}s"
                                        } else "Audio message",
                                        fontSize = 10.sp,
                                        color = if (isCurrentUser)
                                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
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

                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (message.isEdited) {
                            Text(
                                text = "edited",
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic,
                                color = if (isCurrentUser)
                                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
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
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            // Context menu on long press
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Reply") },
                    leadingIcon = {
                        Icon(Icons.Default.Reply, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onReply()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Forward") },
                    leadingIcon = {
                        Icon(Icons.Default.Forward, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onForward()
                    }
                )
                if (canEdit) {
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
                }
                if (isCurrentUser) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                "Delete for everyone",
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
                            onDeleteForEveryone()
                        }
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete for me") },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null)
                    },
                    onClick = {
                        showMenu = false
                        onDeleteForMe()
                    }
                )
            }
        }
    }
}

@Composable
fun ForwardDialog(
    users: List<User>,
    onForward: (User) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Forward to") },
        text = {
            Column {
                users.forEach { user ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onForward(user) }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = user.displayName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = user.role.replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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