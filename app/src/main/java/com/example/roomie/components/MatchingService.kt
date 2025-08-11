package com.example.roomie.components

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
        val currentUserId = auth.currentUser?.uid ?: return emptyList()
        val currentUser = getUserProfile(currentUserId) ?: return emptyList()

        return when (currentUser.profileType) {
            "student" -> findStudentMatches(currentUser)
            "landlord" -> emptyList() // We don't care about matching landlords
            else -> emptyList()
        }
    }

    private suspend fun getUserProfile(uid: String): StudentProfile? {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(StudentProfile::class.java)?.copy(id = snapshot.id)
    }

    // --- Step 2: Query Firestore for broad matches ---
    private suspend fun queryBroadMatches(current: StudentProfile): List<StudentProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("profileType", "student")
            .whereEqualTo("studentUniversity", current.studentUniversity)
            .whereLessThanOrEqualTo("studentMaxCommute", current.studentMaxCommute)
            .whereLessThanOrEqualTo("studentMaxBudget", current.studentMaxBudget)
            .get().await()

        return snapshot.documents.mapNotNull {
            it.toObject(StudentProfile::class.java)?.copy(id = it.id)
        }
    }

    // --- Step 3: Refine matches in-memory ---
    private fun refineMatches(current: StudentProfile, candidates: List<StudentProfile>): List<StudentProfile> {
        return candidates.filter { other ->
            other.id != current.id &&
                    isSubsetRange(
                        current.studentDesiredGroupSize[0],
                        current.studentDesiredGroupSize[1],
                        other.studentDesiredGroupSize[0],
                        other.studentDesiredGroupSize[1]
                    )
        }
    }

    private fun isSubsetRange(minA: Int, maxA: Int, minB: Int, maxB: Int): Boolean {
        return minA >= minB && maxA <= maxB
    }

    // --- Step 4: Orchestrator for student matches ---
    private suspend fun findStudentMatches(current: StudentProfile): List<StudentProfile> {
        val broadMatches = queryBroadMatches(current)
        return refineMatches(current, broadMatches)
    }
}

