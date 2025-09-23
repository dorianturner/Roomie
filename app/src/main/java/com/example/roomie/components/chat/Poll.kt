package com.example.roomie.components.chat

/**
 * Represents a poll in a chat.
 *
 * @property question The question being asked in the poll.
 * @property votes A map where the key is the user ID and the value is their vote ("yes", "no", or "undecided").
 * @property closed Indicates whether the poll is closed for voting.
 * @property resolution The resolution of the poll, if it has been closed and resolved.
 * @property type The type of poll, e.g., "general".
 */
data class Poll(
    val question: String = "",
    val votes: Map<String, String> = emptyMap(), // userId -> "yes" | "no" | "undecided"
    val closed: Boolean = false,
    val resolution: String? = null,
    val type: String = "general"
)
