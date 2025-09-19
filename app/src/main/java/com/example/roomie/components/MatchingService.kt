package com.example.roomie.components

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object MatchingService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun docToStudentProfileSafe(doc: DocumentSnapshot): StudentProfile? {
        val d = doc.data ?: return null

        fun anyToString(any: Any?) = (any as? String) ?: ""
        fun anyToInt(any: Any?, default: Int = 0) = when (any) {
            is Number -> any.toInt()
            is String -> any.toIntOrNull() ?: default
            else -> default
        }
        fun anyToStringList(any: Any?): List<String> = when (any) {
            is List<*> -> any.filterIsInstance<String>()
            is String -> any.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            else -> emptyList()
        }
        fun anyToIntList(any: Any?): List<Int> = when (any) {
            is List<*> -> any.mapNotNull { (it as? Number)?.toInt() }
            is String -> any.split(",").mapNotNull { it.trim().toIntOrNull() }
            else -> listOf(0, 0)
        }
        fun anyToMapStringLong(any: Any?): Map<String, Long> = when (any) {
            is Map<*, *> -> any.mapNotNull { (k, v) ->
                (k as? String)?.let { key ->
                    val value = when (v) {
                        is Number -> v.toLong()
                        is String -> v.toLongOrNull()
                        else -> null
                    }
                    value?.let { key to it }
                }
            }.toMap()
            else -> emptyMap()
        }

        return StudentProfile(
            id = doc.id,
            name = anyToString(d["name"]),
            photos = anyToStringList(d["photos"]),
            studentAge = anyToInt(d["studentAge"]),
            profilePictureUrl = anyToString(d["profilePictureUrl"]),
            studentPet = anyToString(d["studentPet"]),
            studentBedtime = anyToInt(d["studentBedtime"]),
            studentAlcohol = anyToInt(d["studentAlcohol"]),
            studentSmokingStatus = anyToString(d["studentSmokingStatus"]),
            groupMin = anyToIntList(d["studentDesiredGroupSize"]).getOrNull(0),
            groupMax = anyToIntList(d["studentDesiredGroupSize"]).getOrNull(1),
            studentMaxCommute = anyToInt(d["studentMaxCommute"]),
            studentMaxBudget = anyToInt(d["studentMaxBudget"]),
            studentUniversity = anyToString(d["studentUniversity"]),
            bio = anyToString(d["bio"]),
            studentAddicted = anyToString(d["studentAddicted"]),
            studentPetPeeve = anyToString(d["studentPetPeeve"]),
            passionate = anyToString(d["passionate"]),
            studentIdeal = anyToString(d["studentIdeal"]),
            studentMusic = anyToString(d["studentMusic"]),
            phoneNumber = anyToString(d["phoneNumber"]),
            seenUsersTimestamps = anyToMapStringLong(d["seenUsersTimestamps"])
        )
    }

    // Convert Firestore doc -> GroupProfile
    suspend fun docToGroupProfileSafe(doc: DocumentSnapshot): GroupProfile? {
        if (!doc.exists()) return null

        val id = doc.id

        // Load members
        val membersSnap = db.collection("users")
            .whereEqualTo("groupId", id)
            .get()
            .await()
        val members = membersSnap.documents.mapNotNull { docToStudentProfileSafe(it) }

        return GroupProfile(
            id = id,
            name = doc.getString("name") ?: "",
            members = members,
            stats = generateGroupStats(members),
            profilePicture = doc.getString("profilePictureUrl") ?: "",
            bio = ""
        )
    }

    data class MatchResult(
        val id: String = "",
        val score: Double = 0.0
    )
    // Public API
    suspend fun findMatchesForCurrentUser(
        weights: PreferenceWeights
    ): List<GroupProfile> {
        val curUser = auth.currentUser?.uid?.let { db.collection("users").document(it).get().await() } ?: return emptyList()
        val currentUser = docToStudentProfileSafe(curUser) ?: return emptyList()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            user.getIdToken(false).addOnSuccessListener { result ->
                Log.d("FunctionsDebug", "User UID: ${user.uid}")
                Log.d("FunctionsDebug", "Token: ${result.token?.take(40)}...")
            }
        } else {
            Log.e("FunctionsDebug", "No user logged in!")
        }

        val rawResults = FunctionsProvider.instance
            .getHttpsCallable("getGroupMatches")
            .call(
                mapOf(
                    "groupId" to curUser.getString("groupId"),
                    "lastSeenTimestamps" to currentUser.seenUsersTimestamps,
                    "n" to 10,
                    "weights" to weights.toMap()
                )
            )
            .await()
            .data as? List<Map<String, Any>> ?: emptyList()

        if (rawResults.isEmpty()) return emptyList()

        val matches = rawResults.map { map ->
            val id = map["id"] as String
            val rawScore = (map["score"] as Number).toDouble()
            val adjustedScore = if (currentUser.seenUsersTimestamps.containsKey(id)) {
                val now = System.currentTimeMillis()
                val lastSeen = currentUser.seenUsersTimestamps[id] ?: 0L
                val daysSinceSeen = (now - lastSeen) / (1000 * 60 * 60 * 24)
                val decayDays = 7.0
                val factor = (0.7 + (daysSinceSeen / decayDays) * (1.0 - 0.7)).coerceIn(0.7, 1.0)
                rawScore * factor
            } else rawScore
            MatchResult(id = id, score = adjustedScore)
        }

        for (match in matches) {
            Log.d("MatchingService", "Match: ${match.id} with score ${match.score}")
        }

        if (matches.isEmpty()) return emptyList()

        val ids = matches.map { it.id }
        val snapshot = db.collection("groups")
            .whereIn(FieldPath.documentId(), ids)
            .get()
            .await()

        val profilesById = snapshot.documents.associateBy({ it.id }, { docToGroupProfileSafe(it) })
        return matches.mapNotNull { profilesById[it.id] }
    }
}

