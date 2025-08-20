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
import androidx.compose.runtime.getValue
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
import com.example.roomie.components.AttachedFile
import com.example.roomie.components.ChatManager
import com.example.roomie.components.Message
import com.example.roomie.components.MessageItem
import com.example.roomie.components.getMimeType
import com.example.roomie.components.AttachmentPreviewSection

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleChatScreen(
    chatManager: ChatManager,
    chatName: String, // passed to save recomputing
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val messagesState = remember { mutableStateListOf<Message>() }

    var attachedFiles by remember { mutableStateOf<List<AttachedFile>>(emptyList()) }

    var inputText by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    val userID: String? = remember { Firebase.auth.currentUser?.uid }
    val userNameCache = remember { mutableStateMapOf<String, String>() }

    var isUploading by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    // Listen for messages
    DisposableEffect(chatManager) {
        val registration = chatManager.listenMessages { messages ->
            messagesState.clear()
            messagesState.addAll(messages)
        }
        onDispose {
            registration.remove()
        }
    }

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
                    } catch (e: Exception) {
                        // Ignore and return null
                    }
                }

                size
            } catch (e: Exception) {
                null
            }
        }
    }

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

            AttachedFile(uri, fileName, type, fileSize)
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
                    // needs to be updated with correct route
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

            if (attachedFiles.isNotEmpty() && !isUploading) {
                AttachmentPreviewSection(
                    attachedFiles = attachedFiles,
                    onRemoveFile = { uriToRemove ->
                        attachedFiles = attachedFiles.filter { it.uri != uriToRemove }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // progress bar for media upload
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

            // Input + send button + image picker
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {

                // Image picker (launches system picker)
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
                                    // Send each attached file
                                    attachedFiles.forEachIndexed { index, file ->
                                        try {
                                            chatManager.sendMessage(
                                                context = context,
                                                senderId = userID!!,
                                                mediaUri = file.uri,
                                                type = file.type,
                                                onProgress = { progress ->
                                                    // Update overall progress (weighted average)
                                                    val weightedProgress = (index + progress) / attachedFiles.size
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            uploadProgress = weightedProgress.coerceIn(0f, 1f)
                                                        }
                                                    }
                                                }
                                            )
                                        } catch (e: Exception) {
                                            Log.e("SingleChatScreen", "Failed to upload ${file.name}: ${e.message}")
                                            uploadSuccess = false
                                            // Continue with other files instead of stopping
                                        }
                                    }

                                    // Send text message if there's any text input
                                    if (inputText.isNotBlank()) {
                                        try {
                                            chatManager.sendMessage(context, userID!!, inputText)
                                        } catch (e: Exception) {
                                            Log.e("SingleChatScreen", "Failed to send text: ${e.message}")
                                            uploadSuccess = false
                                        }
                                    }

                                } catch (e: Exception) {
                                    Log.e("SingleChatScreen", "Upload failed: ${e.message}")
                                    uploadSuccess = false
                                } finally {
                                    // Only clear if all uploads were successful
                                    if (uploadSuccess) {
                                        attachedFiles = emptyList()
                                    }
                                    isUploading = false
                                    uploadProgress = 0f
                                }

                            } else if (inputText.isNotBlank()) {
                                // === SEND TEXT ONLY ===
                                try {
                                    chatManager.sendMessage(context, userID!!, inputText)
                                    inputText = ""
                                } catch (e: Exception) {
                                    Log.e("SingleChatScreen", "Failed to send text: ${e.message}")
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
