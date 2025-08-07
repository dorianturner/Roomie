package com.example.roomie.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@Composable
fun MessageItem(
    message: Message,
    userNameCache: MutableMap<String, String>
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val isCurrentUser = message.senderId == currentUserId

    val bubbleColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant

    var senderName by remember(message.senderId) {
        mutableStateOf(userNameCache[message.senderId] ?: "Unknown")
    }

    LaunchedEffect(message.senderId) {
        if (!userNameCache.containsKey(message.senderId)) {
            Log.d("MessageItem", "Fetching name for ${message.senderId}")
            val name = fetchUserNameFromFirestore(message.senderId)
            userNameCache[message.senderId] = name
            senderName = name
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .background(bubbleColor, RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            if (!isCurrentUser) {
                Text(
                    text = senderName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }

            when (message.type) {
                "text" -> {
                    Text(
                        text = message.text ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 10,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                "image" -> {
                    message.mediaUrl?.let { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = "Image message",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            placeholder = painterResource(com.example.roomie.R.drawable.placeholder), // optional
                            error = painterResource(com.example.roomie.R.drawable.image_error) // optional
                        )
                    } ?: Text("Image unavailable", color = MaterialTheme.colorScheme.error)
                }

                "video" -> {
                    message.mediaUrl?.let { url ->
                        AndroidView(
                            factory = {
                                val player = ExoPlayer.Builder(context).build().apply {
                                    setMediaItem(MediaItem.fromUri(url))
                                    prepare()
                                    playWhenReady = false
                                }
                                PlayerView(context).apply {
                                    this.player = player
                                    useController = true
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                        )
                    } ?: Text("Loading video...")
                }

                "audio" -> {
                    Text(
                        text = "🔊 Audio message",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    // Future: Replace with audio playback
                }

                "file", "pdf" -> {
                    Text(
                        text = "📄 File: ${message.mediaUrl?.take(40)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                    // Future: Add file open/download logic
                }

                else -> {
                    Text(
                        text = "[Unsupported type: ${message.type}]",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Text(
                text = message.timestamp?.let { formatTimestamp(it) } ?: "",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}
