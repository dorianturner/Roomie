package com.example.roomie.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import com.example.roomie.components.chat.ABOVE_NANOSECOND_DIGITS
import com.example.roomie.components.chat.AttachedFile
import com.example.roomie.components.chat.AttachmentPreviewSection
import com.example.roomie.components.chat.ChatManager
import com.example.roomie.components.chat.ChatType
import com.example.roomie.components.chat.Conversation
import com.example.roomie.components.chat.Message
import com.example.roomie.components.chat.MessageItem
import com.example.roomie.components.chat.Poll
import com.example.roomie.components.chat.PollManager
import com.example.roomie.components.chat.PollSection
import com.example.roomie.components.chat.PollViewModel
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A composable screen that displays an individual chat session.
 *
 * This screen handles the presentation of messages, text input, attachment handling (images/videos),
 * and poll functionality within a specific chat. It uses a [ChatManager] to interact with the
 * backend for sending and receiving messages and managing conversation state.
 *
 * Features include:
 * - Displaying a list of messages in reverse chronological order.
 * - A text input field for composing messages.
 * - An attachment button that launches a media picker for selecting images and videos.
 * - Previewing and removing selected attachments before sending.
 * - Displaying an upload progress indicator when attachments are being sent.
 * - Handling active polls: displaying the poll, allowing the current user to vote.
 * - For `ChatType.MY_GROUP` chats without an active poll, it shows a button to propose finalising the group (which creates a poll).
 * - For `ChatType.MERGE` chats, it automatically attempts to create a "Merge groups?" poll if one isn't active.
 * - Listening for real-time updates to messages and conversation details (like active polls and chat type).
 * - Navigating back if the conversation is deleted (e.g., due to a group merge).
 *
 * @param chatManager An instance of [ChatManager] specific to the current chat, used for all chat operations.
 * @param chatName The display name of the chat, passed to avoid recomputing it.
 * @param onBack A lambda function to be invoked when the user navigates back from this screen.
 * @param modifier Optional [Modifier] for customizing the layout and appearance of the screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChatScreen(
    chatManager: ChatManager,
    chatName: String, // passed to save recomputing
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pollManager = PollManager(chatManager)
    val pollViewModel: PollViewModel = remember { PollViewModel(pollManager) }

    val messagesState = remember { mutableStateListOf<Message>() }

    var attachedFiles by remember { mutableStateOf<List<AttachedFile>>(emptyList()) }

    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val userID: String? = remember { Firebase.auth.currentUser?.uid }
    val userNameCache = remember { mutableStateMapOf<String, String>() }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableFloatStateOf(0f) }

    var activePoll by remember { mutableStateOf<Poll?>(null) }
    var showConfirmDialog by remember { mutableStateOf(false) }

    var conversationState by remember { mutableStateOf<Conversation?>(null) }
    var mergePollStarted by remember { mutableStateOf(false) } // prevents duplicate auto-creates

    // used by chatManager to delete conversation and invoke onBack
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(chatManager) {
        chatManager.conversationDeleted
            .flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.STARTED)
            .collect {
                onBack() // trigger navigation
            }
    }


    // Listen for messages and conversation updates
    DisposableEffect(chatManager) {
        val msgRegistration = chatManager.listenMessages { messages ->
            messagesState.clear()
            messagesState.addAll(messages)
        }

        val convoRegistration = chatManager.listenConversation { convo ->
            conversationState = convo.copy()
            activePoll = convo.activePoll?.copy()
            if (convo.activePoll != null) mergePollStarted = true
        }

        onDispose {
            msgRegistration.remove()
            convoRegistration.remove()
        }
    }

    // Auto-start merge poll if applicable
    LaunchedEffect(conversationState?.chatType) {
        val chatType = conversationState?.chatType
        if (chatType == ChatType.MERGE && !mergePollStarted) {
            try {
                val created = pollManager.createPoll("Merge groups?", "merge")
                mergePollStarted = mergePollStarted || created
            } catch (e: Exception) {
                Log.e("SingleChatScreen", "Failed to auto-start merge poll.", e)
            }
        }
    }

    /**
     * Retrieves the display name of a file from its content URI.
     *
     * It first attempts to query the [ContentResolver] for the [OpenableColumns.DISPLAY_NAME].
     * If that fails or returns "Unknown file", it tries to extract the last path segment from the URI.
     * This function is executed on an IO dispatcher.
     *
     * @param context The application context.
     * @param uri The content URI of the file.
     * @return The file name as a [String], or "Unknown file" if it cannot be determined.
     */
    suspend fun getFileName(context: Context, uri: Uri): String {
        return withContext(Dispatchers.IO) {
            var fileName = "Unknown file"

            // Try to get filename from content resolver first
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }

            // If that fails, try to extract from Uri path
            if (fileName == "Unknown file") {
                uri.path?.let { path ->
                    val segments = path.split("/")
                    if (segments.isNotEmpty()) {
                        fileName = segments.last()
                    }
                }
            }

            fileName
        }
    }

    /**
     * Retrieves the size of a file from its content URI.
     *
     * It first attempts to query the [ContentResolver] for the [OpenableColumns.SIZE].
     * If that fails or returns a null/zero size, it falls back to using a [ParcelFileDescriptor]
     * to get the `statSize`. This function is executed on an IO dispatcher.
     *
     * @param context The application context.
     * @param uri The content URI of the file.
     * @return The file size in bytes as a [Long]?, or `null` if the size cannot be determined or an error occurs.
     */
    suspend fun getFileSize(context: Context, uri: Uri): Long? {
        return withContext(Dispatchers.IO) {
            try {
                // Method 1: Using content resolver
                var size: Long? = null
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst() && sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex)
                    }
                }

                // Method 2: Using ParcelFileDescriptor (fallback)
                if (size == null || size == 0L) {
                    try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            size = pfd.statSize
                        }
                    } catch (_: Exception) {
                        // Ignore and return null
                    }
                }

                size
            } catch (_: Exception) {
                null
            }
        }
    }

    /**
     * Gathers information about a file selected for attachment.
     *
     * This function retrieves the file's MIME type, name (using [getFileName]), and size (using [getFileSize]).
     * It then determines a simplified `type` string (e.g., "image", "video", "pdf", "file") based on the MIME type.
     * A unique ID for the attachment is generated using the URI string length and current nanoseconds to
     * help differentiate if the same file is attached multiple times.
     * This function is executed on an IO dispatcher.
     *
     * @param context The application context.
     * @param uri The content URI of the file.
     * @return An [AttachedFile] object containing the URI, unique ID, name, type string, and size.
     */
    suspend fun getFileInfo(context: Context, uri: Uri): AttachedFile {
        return withContext(Dispatchers.IO) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val fileName = getFileName(context, uri)
            val fileSize = getFileSize(context, uri)

            // probably can extrapolate with other mimetype cases below
            val type = when {
                mimeType.startsWith("image") -> "image"
                mimeType.startsWith("video") -> "video"
                mimeType.startsWith("audio") -> "audio"
                mimeType == "application/pdf" -> "pdf"
                else -> "file"
            }

            // used because if the same image is uploaded twice, both are removed when the uri is filtered
            val uid: Long = (uri.toString().length * ABOVE_NANOSECOND_DIGITS) + Timestamp.now().nanoseconds

            AttachedFile(uri, uid, fileName, type, fileSize)
        }
    }

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { selectedUri ->
            coroutineScope.launch {
                val fileInfo = getFileInfo(context, selectedUri)
                attachedFiles = attachedFiles + fileInfo
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(
                    text = chatName,
                    modifier = Modifier.padding(4.dp)
                ) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                reverseLayout = true,
                contentPadding = PaddingValues(8.dp)
            ) {
                items(messagesState.reversed()) { msg ->
                    MessageItem(
                        msg,
                        userNameCache
                    )
                }
            }

            // Poll UI
            if (activePoll != null) {
                PollSection(
                    poll = activePoll!!,
                    onVote = { choice ->
                        pollViewModel.castVote(userId = userID, choice = choice, context = context)
                    },
                    userId = userID!!
                )
            } else {
                // No active poll: show "Propose to Finalise Group" button for MY_GROUP chats
                if (conversationState?.chatType == ChatType.MY_GROUP) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Propose to Finalise Group")
                    }

                    if (showConfirmDialog) {
                        AlertDialog(
                            onDismissRequest = { showConfirmDialog = false },
                            title = { Text("Confirm") },
                            text = { Text("Are you sure you want to propose finalising this group? This will create a poll.") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            pollManager.createPoll("Finalise Group?", "finalise")
                                            showConfirmDialog = false
                                        }
                                    }
                                ) {
                                    Text("Yes")
                                }
                            },
                            dismissButton = {
                                Button(
                                    onClick = { showConfirmDialog = false }
                                ) {
                                    Text("Cancel")
                                }
                            }
                        )
                    }
                }
                // If chatType == MERGE and activePoll == null, auto-create should attempt to run (handled in LaunchedEffect)
            }

            // Attachment Preview Section
            if (attachedFiles.isNotEmpty() && !isUploading) {
                AttachmentPreviewSection(
                    attachedFiles = attachedFiles,
                    onRemoveFile = { uidToRemove ->
                        attachedFiles = attachedFiles.filter { it.uid != uidToRemove }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Upload Progress Bar
            if (isUploading) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { uploadProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Uploading... ${(uploadProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Input Row: Attachment Picker, Text Field, Send Button
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                }) {
                    Icon(Icons.Filled.Attachment, contentDescription = "Pick Image")
                }

                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message") }
                )

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            if (attachedFiles.isNotEmpty()) {
                                // === UPLOAD ALL ATTACHED FILES ===
                                isUploading = true
                                uploadProgress = 0f
                                var uploadSuccess = true

                                try {
                                    attachedFiles.forEachIndexed { index, file ->
                                        try {
                                            chatManager.sendMessage(
                                                senderId = userID!!,
                                                mediaUri = file.uri,
                                                type = file.type,
                                                onProgress = { progress ->
                                                    val weightedProgress = (index + progress) / attachedFiles.size
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            uploadProgress = weightedProgress.coerceIn(0f, 1f)
                                                        }
                                                    }
                                                }
                                            )
                                        } catch (e: Exception) {
                                            Log.e("SingleChatScreen", "Failed to upload ${file.name}.", e)
                                            uploadSuccess = false
                                        }
                                    }

                                    if (inputText.isNotBlank()) {
                                        try {
                                            chatManager.sendMessage(userID!!, inputText)
                                        } catch (e: Exception) {
                                            Log.e("SingleChatScreen", "Failed to send text after attachments.", e)
                                            uploadSuccess = false
                                        }
                                    }

                                } catch (e: Exception) { // General catch for upload process
                                    Log.e("SingleChatScreen", "Upload process failed.", e)
                                    uploadSuccess = false
                                } finally {
                                    if (uploadSuccess) {
                                        inputText = "" // Clear text only if all uploads (and text) were successful
                                        attachedFiles = emptyList()
                                    }
                                    isUploading = false
                                    uploadProgress = 0f
                                }

                            } else if (inputText.isNotBlank()) {
                                // === SEND TEXT ONLY ===
                                try {
                                    chatManager.sendMessage(userID!!, inputText)
                                    inputText = ""
                                } catch (e: Exception) {
                                    Log.e("SingleChatScreen", "Failed to send text message.", e)
                                    // Don't clear input text on error so user can retry
                                }
                            }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}
