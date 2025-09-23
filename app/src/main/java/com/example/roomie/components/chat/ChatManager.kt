package com.example.roomie.components.chat

import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Manages chat conversations, including creating, deleting, and sending messages.
 *
 * @property conversationID Optional ID of an existing conversation to manage. If null, a new conversation can be created.
 */
class ChatManager(
    conversationID: String? = null
) {
    /**
     * The ID of the current conversation. Null if no conversation is active or created.
     */
    var conversationId: String? = conversationID
        private set
    /**
     * Firestore database instance.
     */
    val db = FirebaseFirestore.getInstance()
    /**
     * Reference to the current conversation document in Firestore.
     */
    val convoRef = FirebaseFirestore.getInstance()
        .collection("conversations")
        .let { col ->
            if (conversationId.isNullOrEmpty()) col.document() else col.document(conversationId!!)
        }

    private val userNameMap: MutableMap<String, String> = mutableMapOf()

    private val _conversationDeleted = MutableSharedFlow<Unit>()
    /**
     * A flow that emits when the current conversation is deleted.
     */
    val conversationDeleted = _conversationDeleted.asSharedFlow()

    /**
     * Deletes the current conversation from Firestore.
     * Emits a signal to [conversationDeleted] flow upon successful deletion.
     * Does nothing if [conversationId] is null.
     */
    suspend fun deleteConversation() {
        if (conversationId == null) return
        convoRef.delete().await()
        _conversationDeleted.emit(Unit) // signal upwards
    }

    /**
     * Creates a new conversation with the given participants.
     * Sets the [conversationId] for the new conversation.
     *
     * @param participants A list of user IDs to be included in the conversation.
     * @param isGroup Whether the conversation is a group chat. Defaults to false.
     * @throws IllegalStateException if a conversation already exists (i.e., [conversationId] is not null).
     */
    suspend fun createConversation(
        participants: List<String>,
        isGroup: Boolean = false,
    ) {
        check(conversationId == null) { "Conversation already exists" }

        val convoRef = db.collection("conversations").document()
        conversationId = convoRef.id

        val conversation = Conversation(
            id = conversationId!!,
            participants = participants,
            isGroup = isGroup,
            createdAt = Timestamp.now(),
        )

        val batch = db.batch()
        batch.set(convoRef, conversation)

        batch.commit().await()
    }

    /**
     * Listens for real-time updates to messages in the current conversation.
     *
     * @param onMessagesUpdated Callback function invoked when the message list is updated.
     * @return A [ListenerRegistration] that can be used to remove the listener.
     * @throws IllegalStateException if [conversationId] is null.
     */
    fun listenMessages(onMessagesUpdated: (List<Message>) -> Unit): ListenerRegistration {
        val convoId = checkNotNull(conversationId)

        return db.collection("conversations")
            .document(convoId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.d("ChatManager", "Conversation deleted, stopping listener")
                    } else {
                        Log.e("ChatManager", "Listen messages failed: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                onMessagesUpdated(messages)
            }
    }

    /**
     * Upload media to Firebase private bucket and return the download URL.
     *
     * @param mediaUri The URI of the media to upload.
     * @param messageId The ID of the message this media belongs to, used for storage path.
     * @param onProgress Callback to report upload progress (0.0 to 1.0).
     * @return The download URL of the uploaded media.
     * @throws Exception if the upload fails.
     */
    private suspend fun uploadMediaToFirebase(
        mediaUri: Uri,
        messageId: String,
        onProgress: (progress: Float) -> Unit
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("chat-media/$messageId")


                val uploadTask = storageRef.putFile(mediaUri)

                // coverts the task to a suspend function to track the progress
                uploadTask.awaitWithProgress(onProgress)

                val downloadUrl = storageRef.downloadUrl.await().toString()

                Log.d("ChatManager", "Uploaded media to: $downloadUrl")
                downloadUrl

            } catch (e: Exception) {
                Log.e("ChatManager", "Upload failed.", e)
                throw e
            }
        }
    }

    /**
     * Extension function for [UploadTask] to await its completion with progress reporting.
     *
     * @param onProgress Callback to report upload progress (0.0 to 1.0).
     * @return The [UploadTask.TaskSnapshot] upon successful completion.
     * @throws Exception if the upload fails or is canceled.
     */
    private suspend fun UploadTask.awaitWithProgress(
        onProgress: (progress: Float) -> Unit
    ): UploadTask.TaskSnapshot = suspendCancellableCoroutine { continuation ->
        addOnProgressListener { snapshot ->
            val progress = snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount
            onProgress(progress)
        }

        addOnCompleteListener { task ->
            if (task.isSuccessful) {
                continuation.resume(task.result)
            } else {
                continuation.resumeWithException(task.exception ?: Exception("Unknown error"))
            }
        }

        addOnCanceledListener {
            continuation.cancel()
        }

        // Handle coroutine cancellation
        continuation.invokeOnCancellation {
            try {
                cancel()
            } catch (_: Exception) {
                // Ignore cancellation errors
            }
        }
    }

    /**
     * Sends a message to the current conversation.
     * If the message is of a multimedia type, it uploads the media to Firebase Storage.
     *
     * @param senderId The ID of the user sending the message.
     * @param text The text content of the message. Null for media-only messages.
     * @param type The type of the message (e.g., "text", "image", "video"). Defaults to "text".
     * @param mediaUri The URI of the media to send. Null for text-only messages.
     * @param onProgress Optional callback to report media upload progress (0.0 to 1.0).
     */
    suspend fun sendMessage(
        senderId: String,
        text: String? = null,
        type: String = "text",
        mediaUri: Uri? = null,
        onProgress: ((Float) -> Unit)? = null, // optional progress callback
    ) {
        val messageRef = convoRef
            .collection("messages")
            .document()

        // handle multimedia

        var mediaUrl: String? = null

        if (type != "text" && mediaUri != null) {
            mediaUrl = uploadMediaToFirebase(
                mediaUri,
                messageRef.id,
                onProgress = { progress ->
                    // forward progress to caller
                    onProgress?.invoke(progress)
                }
            )
            Log.d("ChatManager", "Uploaded media to firebase, link: $mediaUrl")
        }

        val now = Timestamp.now()

        val message = Message(
            id = messageRef.id,
            senderId = senderId,
            text = text,
            type = type,
            mediaUrl = mediaUrl,
            timestamp = now
        )

        db.runBatch { batch ->
            batch.set(messageRef, message)

            batch.update(convoRef, mapOf(
                "lastMessage" to when {
                    type == "text" && !text.isNullOrBlank() -> text
                    type != "text" && mediaUrl != null -> "[${type.replaceFirstChar {it.uppercase()}}]"
                    else -> ""
                },
                "lastMessageAt" to now
            ))
        }.await()
    }

    /**
     * Adds new participants to the current conversation.
     *
     * @param newParticipants A list of user IDs to add to the conversation.
     * @throws IllegalStateException if [conversationId] is null or the conversation is not found.
     */
    suspend fun addParticipants(newParticipants: List<String>) {
        check(conversationId != null) { "Conversation does not exist" }

        val snapshot = convoRef.get().await()
        val conversation =
            snapshot.toObject(Conversation::class.java)

        check(conversation != null) { "Conversation not found" }

        val updatedParticipants = (conversation.participants + newParticipants).distinct()

        val batch = db.batch()
        batch.update(convoRef, "participants", updatedParticipants)

        batch.commit().await()

        newParticipants.forEach { uid ->
            if (uid !in userNameMap) {
                try {
                    val userSnap = db.collection("users").document(uid).get().await()
                    val name = userSnap.getString("name")
                    if (!name.isNullOrEmpty()) {
                        userNameMap[uid] = name
                    }
                } catch (_: Exception) {
                    // Ignore
                }
            }
        }
    }

    /**
     * Retrieves the title for the current conversation.
     * For 1-on-1 chats, it's the name of the other participant.
     * For group chats, it's a comma-separated list of other participant names.
     *
     * @param currentUserId The ID of the current user, to exclude their name from the title.
     * @return The conversation title. Defaults to "Chat" if participants cannot be determined.
     */
    suspend fun getConversationTitle(currentUserId: String): String {
        val snapshot = convoRef.get().await()
        val participants = snapshot.get("participants") as? List<*> ?: return "Chat" // Handle missing participants

        val otherParticipantIds = participants.filterIsInstance<String>().filter { it != currentUserId }

        if (otherParticipantIds.isEmpty()) return "Chat" // No valid participants

        val db = FirebaseFirestore.getInstance()
        val names = otherParticipantIds.mapNotNull { uid ->
            try {
                val userSnap = db.collection("users").document(uid).get().await()
                userSnap.getString("name")
            } catch (_: Exception) {
                null
            }
        }

        return if (names.size == 1) {
            names.first() // Return first name only if 1-2-1 chat
        } else {
            names.joinToString(", ")
        }
    }

    /**
     * Listens for real-time updates to the current conversation document.
     *
     * @param onConversationUpdated Callback function invoked when the conversation data is updated.
     * @return A [ListenerRegistration] that can be used to remove the listener.
     * @throws IllegalStateException if [conversationId] is null.
     */
    fun listenConversation(
        onConversationUpdated: (Conversation) -> Unit
    ): ListenerRegistration {
        check(conversationId != null) { "Conversation does not exist" }

        return convoRef
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.d("ChatManager", "Conversation deleted, stopping listener")
                    } else {
                        Log.e("ChatManager", "Listen conversation failed: ${error.message}")
                    }
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val conversation = snapshot.toObject(Conversation::class.java)
                    conversation?.let { onConversationUpdated(it) }
                }
            }
    }
}
