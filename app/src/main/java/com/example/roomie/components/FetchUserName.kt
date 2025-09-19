package com.example.roomie.components

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Fetches a user's name from Firestore given their UID.
 *
 * @param uid The unique identifier of the user.
 * @return The user's name if found, or "Unknown" if the name is not set or an error occurs.
 */
suspend fun fetchUserNameFromFirestore(uid: String): String {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
        snapshot.getString("name") ?: "Unknown"
    } catch (_: Exception) {
        "Unknown"
    }
}
