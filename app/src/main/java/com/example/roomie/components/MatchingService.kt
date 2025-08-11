package com.example.roomie.components

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await


// maybe not needed, but kind of useful (we should really refactor user profiles)
data class StudentProfile(
    val id: String = "",
    val profileType: String = "student",
    val studentUniversity: String = "",
    val studentBasicPreferences: List<String> = emptyList(),
    val studentDesiredGroupSize: List<Int> = listOf(0, 0),
    val studentMaxCommute: Int = 0,
    val studentMaxBudget: Int = 0
)

object MatchingService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Public API
    suspend fun findMatchesForCurrentUser(): List<StudentProfile> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val current = getUserProfile(uid) ?: return emptyList()
        return when (current.profileType) {
            "student" -> findStudentMatches(current)
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
    private suspend fun queryBroadMatches(current: StudentProfile): List<StudentProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("profileType", "student")
            .whereEqualTo("studentUniversity", current.studentUniversity)
            .whereLessThanOrEqualTo("studentMaxCommute", current.studentMaxCommute)
            .get()
            .await()

        return snapshot.documents.mapNotNull { docToStudentProfileSafe(it) }
    }

    // in-memory filters
    private fun refineMatches(current: StudentProfile, candidates: List<StudentProfile>): List<StudentProfile> {
        val minA = current.studentDesiredGroupSize.getOrNull(0) ?: 0
        val maxA = current.studentDesiredGroupSize.getOrNull(1) ?: 0
        val maxBudgetAllowed = current.studentMaxBudget

        return candidates.filter { other ->
            if (other.id == current.id) return@filter false
            if (other.studentMaxBudget > maxBudgetAllowed) return@filter false

            val minB = other.studentDesiredGroupSize.getOrNull(0) ?: 0
            val maxB = other.studentDesiredGroupSize.getOrNull(1) ?: Int.MAX_VALUE
            minA >= minB && maxA <= maxB
        }
    }


    private suspend fun findStudentMatches(current: StudentProfile): List<StudentProfile> {
        val broad = queryBroadMatches(current)
        return refineMatches(current, broad)
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

        return StudentProfile(
            id = doc.id,
            profileType = anyToString(d["profileType"]).ifEmpty { "student" },
            studentUniversity = anyToString(d["studentUniversity"]),
            studentBasicPreferences = anyToStringList(d["studentBasicPreferences"]),
            studentDesiredGroupSize = anyToIntList(d["studentDesiredGroupSize"]).let {
                if (it.size >= 2) it else listOf(it.getOrNull(0) ?: 0, it.getOrNull(1) ?: 0)
            },
            studentMaxCommute = anyToInt(d["studentMaxCommute"]),
            studentMaxBudget = anyToInt(d["studentMaxBudget"])
        )
    }
}

