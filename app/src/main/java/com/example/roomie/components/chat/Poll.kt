package com.example.roomie.components.chat

data class Poll(
    val question: String = "",
    val votes: Map<String, String> = emptyMap(), // userId -> "yes" | "no" | "undecided"
    val closed: Boolean = false,
    val resolution: String? = null
)
