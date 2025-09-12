package com.example.roomie.components.chat

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

const val ABOVE_NANOSECOND_DIGITS = 10000000000

typealias Uid = Long

data class Conversation(
    val id: String = "",
    @get:PropertyName("isGroup")
    @set:PropertyName("isGroup")
    var isGroup: Boolean = false,
    val participants: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val lastMessage: String? = null,
    val lastMessageAt: Timestamp? = null,
    val activePoll: Poll? = null
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
    val uid: Long,
    val name: String,
    val type: String, // "image", "video", "pdf", etc.
    val size: Long? = null
)