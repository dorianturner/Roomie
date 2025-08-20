package com.example.roomie.components

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

// maybe not needed, but kind of useful (we should really refactor user profiles)
data class StudentProfile(
    val id: String = "",
    val name: String = "",
    val bio: String = "",
    val profileType: String = "student",
    val studentUniversity: String = "",
    val studentBasicPreferences: List<String> = emptyList(),
    val studentDesiredGroupSize: List<Int> = listOf(0, 0),
    val studentMaxCommute: Int = 0,
    val studentMaxBudget: Int = 0,
    val seenUsersTimestamps: Map<String, Long> = emptyMap()
)

object MatchingService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Public API
    suspend fun findMatchesForCurrentUser(
        weights: PreferenceWeights
    ): List<StudentProfile> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val current = getUserProfile(uid) ?: return emptyList()
        return when (current.profileType) {
            "student" -> findStudentMatches(current, weights)
            else -> emptyList()
        }
    }

    // Safe fetch of the current user's profile
    private suspend fun getUserProfile(uid: String): StudentProfile? {
        val snap = db.collection("users").document(uid).get().await()
        return docToStudentProfileSafe(snap)
    }

    // Firestore for broad filters
    // Firestore allows only one range filter per query for some fkn reason
    private suspend fun queryBroadMatches(
        current: StudentProfile,
        weights: PreferenceWeights
    ): List<StudentProfile> {
        // TODO: ADD hard filtering based on the other person having maximum group size >= your current group size + 1

        var query = db.collection("users")
            .whereEqualTo("profileType", "student")

        // If "University" is must-have (weight == 5), enforce in Firestore
        if (weights.university == 5) {
            query = query.whereEqualTo("studentUniversity", current.studentUniversity)
        }

        // If "Commute" is must-have, enforce in Firestore
        if (weights.commute == 5) {
            query = query.whereLessThanOrEqualTo("studentMaxCommute", current.studentMaxCommute)
        }

        // If "Budget" is must-have, enforce in Firestore
        if (weights.budget == 5) {
            query = query.whereLessThanOrEqualTo("studentMaxBudget", current.studentMaxBudget)
        }

        val snapshot = query.get().await()
        return snapshot.documents.mapNotNull { docToStudentProfileSafe(it) }
    }


    // in-memory filters
    private fun refineMatches(
        current: StudentProfile,
        candidates: List<StudentProfile>,
        weights: PreferenceWeights
    ): List<StudentProfile> {
        Log.d("MatchingService", "Refining matches for ${current.name}")
        Log.d("MatchingService", "All current candidates: ${candidates.joinToString(", ") { it.name }}")

        (candidates + current).forEach {
            Log.d(
                "MatchingService",
                "For ${it.name}: minA=${it.studentDesiredGroupSize.getOrNull(0) ?: 0}, " +
                        "maxA=${it.studentDesiredGroupSize.getOrNull(1) ?: 0}, " +
                        "maxBudgetAllowed=${it.studentMaxBudget}"
            )
        }

        val candidatesNotSelf = candidates.filter { candidate -> candidate.id != current.id }

        Log.d("MatchingService", "All seen candidates: ${current.seenUsersTimestamps.keys.joinToString(", ")}")
        Log.d("MatchingService", "All candidates considered: ${candidatesNotSelf.joinToString(", ") { it.name }}")

        val SEEN_PENALTY = 1.0
        val DECAY_DURATION_MS = 5 * 24 * 60 * 60 * 1000L // 5 days
        val now = System.currentTimeMillis()

        val candidatesRanked = candidatesNotSelf
            .map { candidate ->
                val baseScore = computeRelevancyScore(current, candidate, weights)

                val lastSeen = current.seenUsersTimestamps[candidate.id]
                val penalty = if (lastSeen != null) {
                    val elapsed = now - lastSeen
                    val decayFactor = (1.0 - (elapsed.toDouble() / DECAY_DURATION_MS)).coerceIn(0.0, 1.0)
                    val p = SEEN_PENALTY * decayFactor
                    Log.d(
                        "MatchingService",
                        "Penalty for ${candidate.name}: elapsed=${elapsed / 1000}s, decayFactor=$decayFactor, penalty=$p"
                    )
                    p
                } else {
                    0.0
                }

                val adjustedScore = baseScore - penalty
                Log.d("MatchingService", "For ${candidate.name}: base=$baseScore, penalty=$penalty, adjusted=$adjustedScore")

                candidate to adjustedScore
            }
            .sortedByDescending { it.second }

        candidatesRanked.forEach {
            Log.d("MatchingService", "Final score for ${it.first.name}: ${it.second}")
        }

        return candidatesRanked.map { it.first }
    }



    private fun computeRelevancyScore(
        current: StudentProfile,
        other: StudentProfile,
        weights: PreferenceWeights
    ): Double {
        var score = 0.0
        var totalWeight = 0.0

        // University match: exact or not
        val uniScore = if (current.studentUniversity == other.studentUniversity) 1.0 else 0.0
        score += uniScore * weights.university
        totalWeight += weights.university

        // Budget: penalize if over budget, else closer is better
        val budgetDiff = (other.studentMaxBudget - current.studentMaxBudget).coerceAtLeast(0)
        val budgetScore = if (budgetDiff == 0) 1.0 else 1.0 / (1 + (budgetDiff.toDouble() / 10))
        score += budgetScore * weights.budget
        totalWeight += weights.budget

        // Commute: closer is better, but allow outside range with lower score
        val commuteDiff = (other.studentMaxCommute - current.studentMaxCommute).coerceAtLeast(0)
        val commuteScore = if (commuteDiff <= 0) 1.0 else 1.0 / (1 + (commuteDiff.toDouble() / 2))
        score += commuteScore * weights.commute
        totalWeight += weights.commute

        // Group size overlap: Jaccard-like similarity
        val (minA, maxA) = current.studentDesiredGroupSize
        val (minB, maxB) = other.studentDesiredGroupSize
        val overlap = (minA..maxA).intersect(minB..maxB)
        val groupScore = if (overlap.isNotEmpty()) 1.0 else 0.5 // partial credit
        score += groupScore * weights.groupSize
        totalWeight += weights.groupSize

        // Basic preferences (strings): overlap ratio
        // want to have some more robust algo for this
        val commonPrefs = current.studentBasicPreferences.intersect(other.studentBasicPreferences.toSet()).size
        val prefScore = if (current.studentBasicPreferences.isNotEmpty()) {
            commonPrefs.toDouble() / current.studentBasicPreferences.size
        } else 0.0
        score += prefScore * weights.preferences
        totalWeight += weights.preferences

        return if (totalWeight > 0) score / totalWeight else 0.0
    }

    private suspend fun findStudentMatches(current: StudentProfile, weights: PreferenceWeights): List<StudentProfile> {
        val broad = queryBroadMatches(current, weights)
        return refineMatches(current, broad, weights)
    }

    // to avoid toObject() type errors
    private fun docToStudentProfileSafe(doc: DocumentSnapshot): StudentProfile? {
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
            name = anyToString(d["name"]).ifEmpty { "Unknown Name" },
            bio = anyToString(d["bio"]).ifEmpty { "This user is a mystery . . ." },
            profileType = anyToString(d["profileType"]).ifEmpty { "student" },
            studentUniversity = anyToString(d["studentUniversity"]),
            studentBasicPreferences = anyToStringList(d["studentBasicPreferences"]),
            studentDesiredGroupSize = anyToIntList(d["studentDesiredGroupSize"]).let {
                if (it.size >= 2) it else listOf(it.getOrNull(0) ?: 0, it.getOrNull(1) ?: 0)
            },
            studentMaxCommute = anyToInt(d["studentMaxCommute"]),
            studentMaxBudget = anyToInt(d["studentMaxBudget"]),
            seenUsersTimestamps = anyToMapStringLong(d["seenUsersTimestamps"])
        )
    }
}

