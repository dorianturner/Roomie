package com.example.roomie.components

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.google.firebase.storage.storage

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

    suspend fun sendMessage(
        senderId: String,
        text: String? = null,
        type: String = "text",
        mediaUri: Uri? = null,
    ) {
        check(conversationId != null) { "Conversation does not exist" }

        val messageRef = db.collection("conversations")
            .document(conversationId!!)
            .collection("messages")
            .document()

        // handle multimedia

        var mediaUrl: String? = null

        if (type != "text" && mediaUri != null) {
            val storageRef = Firebase.storage
                .reference
                .child("chat_media/$conversationId/${messageRef.id}")

            val uploadTask = storageRef.putFile(mediaUri).await()
            mediaUrl = storageRef.downloadUrl.await().toString()
        } else {
            assert(type == "text" && mediaUri == null)
            // why would you have one and not the other??
        }

        val message = Message(
            id = messageRef.id,
            senderId = senderId,
            text = text,
            type = type,
            mediaUrl = mediaUrl,
            timestamp = Timestamp.now()
        )

        messageRef.set(message).await()
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
