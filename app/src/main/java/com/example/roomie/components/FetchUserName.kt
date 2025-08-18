package com.example.roomie.components

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun fetchUserNameFromFirestore(uid: String): String {
    return try {
        val snapshot = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .get()
            .await()
        snapshot.getString("name") ?: "Unknown"
    } catch (e: Exception) {
        "Unknown"
    }
}