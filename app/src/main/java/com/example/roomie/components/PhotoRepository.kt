package com.example.roomie.components

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class PhotoItem(val url: String = "", val path: String = "")

/** Uploads image uri to Storage and returns Pair(downloadUrl, storagePath) */
suspend fun uploadProfileImage(uri: Uri, uid: String): Pair<String, String> =
    suspendCancellableCoroutine { cont ->
        val path = "users/$uid/photos/${UUID.randomUUID()}.jpg"
        val ref = FirebaseStorage.getInstance().reference.child(path)
        val uploadTask = ref.putFile(uri)
        uploadTask
            .addOnSuccessListener {
                ref.downloadUrl
                    .addOnSuccessListener { dl ->
                        cont.resume(Pair(dl.toString(), path))
                    }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }
            .addOnFailureListener { e -> cont.resumeWithException(e) }
    }

/** Adds {url,path} to user's photos list in Firestore */
suspend fun addPhotoToUser(uid: String, url: String, path: String): Boolean =
    suspendCancellableCoroutine { cont ->
        val db = FirebaseFirestore.getInstance()
        val ref = db.collection("users").document(uid)
        // Use transaction to safely append
        db.runTransaction { tx ->
            val snap = tx.get(ref)
            val current = (snap.get("photos") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
            current.add(mapOf("url" to url, "path" to path))
            tx.update(ref, "photos", current)
            null
        }.addOnSuccessListener { cont.resume(true) }
            .addOnFailureListener { cont.resume(false) }
    }

/** Delete storage file at path and remove entry from Firestore */
suspend fun deletePhoto(uid: String, storagePath: String): Boolean =
    suspendCancellableCoroutine { cont ->
        val storageRef = FirebaseStorage.getInstance().reference.child(storagePath)
        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection("users").document(uid)

        // first delete storage
        storageRef.delete()
            .addOnSuccessListener {
                // then remove from Firestore (transactional-ish)
                db.runTransaction { tx ->
                    val snap = tx.get(userRef)
                    val current = (snap.get("photos") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
                    val newList = current.filterNot { it["path"] == storagePath }
                    tx.update(userRef, "photos", newList)
                    null
                }.addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { cont.resume(false) }
            }
            .addOnFailureListener { e ->
                // maybe file missing - still attempt to update Firestore
                db.runTransaction { tx ->
                    val snap = tx.get(userRef)
                    val current = (snap.get("photos") as? List<Map<String, String>>)?.toMutableList() ?: mutableListOf()
                    val newList = current.filterNot { it["path"] == storagePath }
                    tx.update(userRef, "photos", newList)
                    null
                }.addOnSuccessListener { cont.resume(true) }
                    .addOnFailureListener { cont.resumeWithException(e) }
            }
    }

/** Fetch photo list from Firestore */
suspend fun fetchUserPhotos(uid: String): List<PhotoItem> =
    suspendCancellableCoroutine { cont ->
        val db = FirebaseFirestore.getInstance()
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val raw = doc.get("photos") as? List<Map<String, String>> ?: emptyList()
                val list = raw.mapNotNull { m ->
                    val url = m["url"]; val path = m["path"]
                    if (url != null && path != null) PhotoItem(url, path) else null
                }
                cont.resume(list)
            }
            .addOnFailureListener { cont.resumeWithException(it) }
    }
