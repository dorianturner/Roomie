package com.example.roomie.components.listings

import android.net.Uri
import com.example.roomie.components.PhotoItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

suspend fun uploadListingImage(uri: Uri, listingId: String): Pair<String, String> {
    val path = "listings/$listingId/photos/${UUID.randomUUID()}.jpg"
    val ref = FirebaseStorage.getInstance().reference.child(path)
    ref.putFile(uri).await()
    val url = ref.downloadUrl.await().toString()
    return url to path
}

suspend fun fetchListingPhotos(listingId: String): List<PhotoItem> {
    val doc =
        FirebaseFirestore.getInstance()
            .collection("listings")
            .document(listingId)
            .get()
            .await()
    val raw = (doc.get("photos") as? List<*>)  // cast to List<*> first
        ?.mapNotNull { item ->
            (item as? Map<*, *>)?.mapNotNull {
                val key = it.key as? String
                val value = it.value as? String
                if (key != null && value != null) key to value else null
            }?.toMap()
        } ?: emptyList()
    return raw.mapNotNull { m ->
        val url = m["url"]; val path = m["path"]
        if (url != null && path != null) PhotoItem(url, path) else null
    }
}

suspend fun deleteListingPhoto(listingId: String, path: String): Boolean {
    val storageRef = FirebaseStorage.getInstance().reference.child(path)
    val listingRef =
        FirebaseFirestore
            .getInstance()
            .collection("listings")
            .document(listingId)

    return try {
        storageRef.delete().await()
        FirebaseFirestore.getInstance().runTransaction { tx ->
            val snap = tx.get(listingRef)
            val current = (snap.get("photos") as? List<*>)
                ?.mapNotNull { item ->
                    (item as? Map<*, *>)?.mapNotNull {
                        val key = it.key as? String
                        val value = it.value as? String
                        if (key != null && value != null) key to value else null
                    }?.toMap()
                }?.toMutableList() ?: mutableListOf()
            val newList = current.filterNot { it["path"] == path }
            tx.update(listingRef, "photos", newList)
        }.await()
        true
    } catch (_: Exception) {
        false
    }
}
