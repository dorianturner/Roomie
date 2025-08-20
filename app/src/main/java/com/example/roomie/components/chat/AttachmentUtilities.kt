package com.example.roomie.components.chat

import android.net.Uri

data class AttachedFile(
    val uri: Uri,
    val name: String,
    val type: String, // "image", "video", "pdf", etc.
    val size: Long? = null
)