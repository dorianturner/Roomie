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
import com.example.roomie.components.SupabaseClient.supabase
import io.ktor.http.ContentType
import io.github.jan.supabase.storage.storage
import kotlin.time.DurationUnit
import kotlin.time.toDuration

object Constants {
    const val BUCKET_NAME = "chat-media"
}

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
    private suspend fun uploadMediaToSupabase(
        context: Context,
        mediaUri: Uri,
        storagePath: String
    ): String {
        return withContext(Dispatchers.IO) {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(mediaUri)
                ?: throw IllegalArgumentException("Cannot open mediaUri stream")

            val bytes = inputStream.readBytes()
            inputStream.close()

            val mimeType = getMimeType(context, mediaUri)
            val typeAndSubtype: Pair<String, String>? = mimeType?.split("/")?.let{
                    when (it.size) {
                        2 -> it[0] to it[1]
                        1 -> it[0] to "*"
                        else -> null
                    }
                }

            try {
                val response = supabase.storage.from(Constants.BUCKET_NAME).upload(
                    storagePath,
                    bytes) {
                    contentType = typeAndSubtype?.let { (type, subtype) -> ContentType(type, subtype) }
                }
                storagePath
            } catch (e: Exception) {
                Log.e("ChatManager", "Upload error: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Generate a signed URL for a media path, expires in 1 hour.
     */
    suspend fun getSignedMediaUrl(mediaPath: String, expiresInSeconds: Int = 3600): String? {
        try {
            return supabase.storage.from(Constants.BUCKET_NAME)
                .createSignedUrl(
                    mediaPath,
                    expiresInSeconds.toDuration(DurationUnit.SECONDS)
                )
        } catch (e: Exception) {
            Log.e("ChatManager", "Signed URL error: ${e.message}")
            return null
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

        val messageRef = db.collection("conversations")
            .document(conversationId!!)
            .collection("messages")
            .document()

        // handle multimedia

        var mediaUrl: String? = null

        if (type != "text" && mediaUri != null) {
            val path = "$conversationId/${messageRef.id}"
            mediaUrl = uploadMediaToSupabase(context, mediaUri, path)
            Log.d("ChatManager", "Uploaded media to Supabase path: $mediaUrl")
        }

        val message = Message(
            id = messageRef.id,
            senderId = senderId,
            text = text,
            type = type,
            mediaUrl = mediaUrl, // actually mediaPath for Supabase
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
