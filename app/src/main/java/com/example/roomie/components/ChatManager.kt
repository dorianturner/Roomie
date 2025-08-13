package com.example.roomie.components

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.firebase.storage.FirebaseStorage

fun getMimeType(context: Context, uri: Uri): String? {
    return context.contentResolver.getType(uri)
}

class ChatManager(
    conversationID: String? = null
) {
    var conversationId: String? = conversationID
        private set
    private val db = FirebaseFirestore.getInstance()
    private val convoRef = FirebaseFirestore.getInstance()
        .collection("conversations")
        .let { col ->
            if (conversationId.isNullOrEmpty()) col.document() else col.document(conversationId!!)
        }

    private val userNameMap: MutableMap<String, String> = mutableMapOf()

    // creates new conversationID if one wasn't passed in

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

        // optional last read behaviour- omitted for now

        /*
        participants.forEach { userId ->
            val userConvoRef = db.collection("users")
                .document(userId)
                .collection("conversations")
                .document(conversationId!!)
            batch.set(userConvoRef, mapOf("conversationId" to conversationId, "lastReadAt" to Timestamp.now()))
        }
        */

        batch.commit().await()
    }

    fun listenMessages(onMessagesUpdated: (List<Message>) -> Unit): ListenerRegistration {
        val convoId = checkNotNull(conversationId)

        return db.collection("conversations")
            .document(convoId)
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Handle error
                    return@addSnapshotListener
                }
                val messages = snapshot?.toObjects(Message::class.java) ?: emptyList()
                onMessagesUpdated(messages)
            }
    }

    /**
     * Upload media to Supabase private bucket and return the relative path.
     */
    private suspend fun uploadMediaToFirebase(
        mediaUri: Uri,
        messageId: String
    ): String {
        return withContext(Dispatchers.IO) {
            try {
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("chat-media/$messageId")

                storageRef.putFile(mediaUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()
                Log.d("ChatManager", "Uploaded media to: $downloadUrl")
                downloadUrl
            } catch (e: Exception) {
                Log.e("ChatManager", "Upload failed: ${e.message}")
                throw e
            }
        }
    }

    suspend fun sendMessage(
        context: Context,
        senderId: String,
        text: String? = null,
        type: String = "text",
        mediaUri: Uri? = null,
    ) {
        check(conversationId != null) { "Conversation does not exist" }

        val conversationRef = db.collection("conversations")
            .document(conversationId!!)
        val messageRef = conversationRef
            .collection("messages")
            .document()

        // handle multimedia

        var mediaUrl: String? = null

        if (type != "text" && mediaUri != null) {
            mediaUrl = uploadMediaToFirebase(mediaUri, messageRef.id)
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

            batch.update(conversationRef, mapOf(
                "lastMessage" to when {
                    type == "text" && !text.isNullOrBlank() -> text
                    type != "text" && mediaUrl != null -> "[${type.replaceFirstChar {it.uppercase()}}]"
                    else -> ""
                },
                "lastMessageAt" to now
            ))
        }.await()
    }

    suspend fun addParticipants(newParticipants: List<String>) {
        check(conversationId != null) { "Conversation does not exist" }

        val snapshot = convoRef.get().await()
        val conversation = snapshot.toObject(Conversation::class.java) ?: throw IllegalStateException("Conversation not found")

        val updatedParticipants = (conversation.participants + newParticipants).distinct()

        val batch = db.batch()
        batch.update(convoRef, "participants", updatedParticipants)

        // optional last read behaviour- omitted for now

        /*
        newParticipants.forEach { userId ->
            val userConvoRef = db.collection("users")
                .document(userId)
                .collection("conversations")
                .document(conversationId!!)
            batch.set(userConvoRef, mapOf("conversationId" to conversationId!!, "lastReadAt" to Timestamp.now()))
        }
        */

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
            } catch (e: Exception) {
                null
            }
        }

        return if (names.size == 1) {
            names.first() // Return first name only if 1-2-1 chat
        } else {
            "Group: ${names.joinToString(", ")}"
        }
    }

}
