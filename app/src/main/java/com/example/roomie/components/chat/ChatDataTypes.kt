package com.example.roomie.components.chat

import android.net.Uri
import com.google.firebase.Timestamp

data class Conversation(
    val id: String = "",
    val isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Timestamp? = null
)

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String? = null,                     // Optional for media-only messages
    val type: String = "text",                    // "text", "image", "video", "file", etc.
    val mediaUrl: String? = null,                 // For multimedia
    val mediaMetadata: Map<String, Any>? = null,  // width/height/duration
    val timestamp: Timestamp? = null
)

data class AttachedFile(
    val uri: Uri,
    val name: String,
    val type: String, // "image", "video", "pdf", etc.
    val size: Long? = null
)