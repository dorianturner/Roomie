package com.example.roomie.components

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Handles the logic for finding and retrieving matches for users and groups.
 * It interacts with Firebase Firestore and Firebase Functions to get matching data.
 */
object MatchingService {
    /** Firebase Authentication instance. */
    private val auth = FirebaseAuth.getInstance()
    /** Lazy-initialized Firebase Firestore instance. */
    private val db by lazy {
        FirebaseFirestore.getInstance()
    }

    /**
     * Safely converts a Firestore [DocumentSnapshot] to a [StudentProfile] object.
     * This function handles potential null data and performs type conversions for each field.
     *
     * @param doc The Firestore [DocumentSnapshot] to convert.
     * @return A [StudentProfile] object if the conversion is successful, or `null` otherwise.
     */
    fun docToStudentProfileSafe(doc: DocumentSnapshot): StudentProfile? {
        val d = doc.data ?: return null

        // Helper functions for safe type conversion from Firestore data.
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
            else -> listOf(0, 0) // Default for studentDesiredGroupSize if not present or malformed
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

    /**
     * Safely converts a Firestore [DocumentSnapshot] representing a group into a [GroupProfile] object.
     * This includes fetching and converting all member profiles associated with the group.
     *
     * @param doc The Firestore [DocumentSnapshot] of the group to convert.
     * @return A [GroupProfile] object if the document exists and conversion is successful, or `null` otherwise.
     */
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

    /**
     * Represents a raw match result from the matching algorithm.
     *
     * @property id The ID of the matched group or user.
     * @property score The matching score.
     */
    data class MatchResult(
        val id: String = "",
        val score: Double = 0.0
    )

    /**
     * Finds potential group matches for the currently authenticated user.
     * It fetches the user's profile, calls a Firebase Cloud Function ("getGroupMatches")
     * with the user's data and preference weights, and then retrieves the full profiles
     * of the matched groups.
     *
     * @param weights The [PreferenceWeights] to be used by the matching algorithm.
     * @return A list of [GroupProfile] objects representing the matches, or an empty list if
     *         no user is authenticated, the current user's profile cannot be fetched,
     *         or no matches are found.
     */
    suspend fun findMatchesForCurrentUser(
        weights: PreferenceWeights
    ): List<GroupProfile> {
        val curUserDoc =
            auth.currentUser
                ?.uid
                ?.let {
                    db.collection("users")
                        .document(it)
                        .get()
                        .await()
                }
                ?: return emptyList()
        val currentUserProfile = docToStudentProfileSafe(curUserDoc) ?: return emptyList()

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
                    "groupId" to curUserDoc.getString("groupId"),
                    "lastSeenTimestamps" to currentUserProfile.seenUsersTimestamps,
                    "n" to 10, // Number of matches to retrieve
                    "weights" to weights.toMap()
                )
            )
            .await()
            .data as? List<*>

        val results = rawResults
            ?.mapNotNull { raw ->
                (raw as? Map<*, *>)?.mapNotNull { (k, v) ->
                    if (k is String) k to v else null
                }?.toMap()
            } ?: emptyList()

        if (results.isEmpty()) return emptyList()

        val matches = results.map { map ->
            val id = map["id"] as String
            val rawScore = (map["score"] as Number).toDouble()
            MatchResult(id = id, score = rawScore)
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

        val profilesById =
            snapshot
                .documents
                .associateBy(
                    { it.id },
                    { docToGroupProfileSafe(it) }
                )
        return matches.mapNotNull { profilesById[it.id] }
    }
}
