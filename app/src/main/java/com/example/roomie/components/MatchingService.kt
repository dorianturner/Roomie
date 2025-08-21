package com.example.roomie.components

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object MatchingService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun getCurrentUserGroup(): GroupProfile? {
        val uid = auth.currentUser?.uid ?: return null
        val userSnap = db.collection("users").document(uid).get().await()
        val groupId = userSnap.getString("groupId") ?: return null
        return getGroupById(groupId)
    }

    suspend fun getGroupById(groupId: String): GroupProfile? {
        val usersSnap = db.collection("users")
            .whereEqualTo("groupId", groupId)
            .get()
            .await()

        val members = usersSnap.documents.mapNotNull { docToStudentProfileSafe(it) }
        if (members.isEmpty()) return null

        val stats = computeStats(members)
        return GroupProfile(
            id = groupId,
            name = if (members.size == 1) members[0].name else "Group: ${members.joinToString { it.name }}",
            members = members,
            stats = stats
        )
    }

    private fun computeStats(members: List<StudentProfile>): GroupStats {
        val size = members.size
        val avgBudget = members.map { it.studentMaxBudget }.average().toInt()
        val avgCommute = members.map { it.studentMaxCommute }.average().toInt()
        val avgAge = members.map {
            // You may need to add `studentAge` to StudentProfile if not already there
            it.studentDesiredGroupSize.firstOrNull() ?: 0
        }.average().toInt()

        return GroupStats(size, avgBudget, avgCommute, avgAge)
    }

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
            name = anyToString(d["name"]),
            bio = anyToString(d["bio"]),
            profileType = anyToString(d["profileType"]),
            studentUniversity = anyToString(d["studentUniversity"]),
            studentBasicPreferences = anyToStringList(d["studentBasicPreferences"]),
            studentDesiredGroupSize = anyToIntList(d["studentDesiredGroupSize"]),
            studentMaxCommute = anyToInt(d["studentMaxCommute"]),
            studentMaxBudget = anyToInt(d["studentMaxBudget"]),
            studentAge = anyToInt(d["studentAge"]),
            seenGroupsTimestamps = anyToMapStringLong(d["seenGroupsTimestamps"])
        )
    }

    // Public API
    suspend fun findMatchesForCurrentUser(
        weights: PreferenceWeights
    ): List<GroupProfile> {
        // fetch current user's group
        val currentGroup = getCurrentUserGroup() ?: return emptyList()
        Log.d("MatchingService", "Current group: ${currentGroup.name}, UID: ${currentGroup.id}, Members: ${currentGroup.members.joinToString { it.name }}")

        val broadMatches = queryBroadGroupMatches(currentGroup, weights)
        Log.d("MatchingService", "Broad matches: ${broadMatches.joinToString { it.name }}")
        return refineGroupMatches(currentGroup, broadMatches, weights)
    }

    // Broad Firestore filters for groups
    private suspend fun queryBroadGroupMatches(
        current: GroupProfile,
        weights: PreferenceWeights
    ): List<GroupProfile> {
        // TODO: Add hard filtering based on the other group having at least space for my group
        var query = db.collection("groups").whereGreaterThanOrEqualTo("membersCount", 1) // placeholder

        if (weights.commute == 5) {
            query = query.whereLessThanOrEqualTo("stats.sumCommute", current.stats.avgCommute)
        }

        if (weights.budget == 5) {
            query = query.whereLessThanOrEqualTo("stats.sumBudgets", current.stats.avgBudget)
        }

        val snapshot = query.get().await()
        Log.d("MatchingService", "Broad group matches: ${snapshot.documents.size}")
        return snapshot.documents.mapNotNull { docToGroupProfileSafe(it) }
    }

    // Refine & rank groups in memory
    private fun refineGroupMatches(
        current: GroupProfile,
        candidates: List<GroupProfile>,
        weights: PreferenceWeights,
    ): List<GroupProfile> {
        val candidatesNotSelf = candidates.filter { it.id != current.id }

        val ranked = candidatesNotSelf
            .map { group ->
                val score = computeGroupRelevancyScore(current, group, weights)
                group to score
            }
            .sortedByDescending { it.second }

        ranked.forEach {
            Log.d("MatchingService", "Final score for group ${it.first.name}: ${it.second}")
        }

        return ranked.map { it.first }
    }

    // Group-level scoring
    private fun computeGroupRelevancyScore(
        current: GroupProfile,
        other: GroupProfile,
        weights: PreferenceWeights
    ): Double {
        var score = 0.0
        var totalWeight = 0.0

        val decay_duration_ms = 5 * 24 * 60 * 60 * 1000L // 5 days
        val now = System.currentTimeMillis()

        // Commute similarity
        val commuteDiff = (other.stats.avgCommute - current.stats.avgCommute).coerceAtLeast(0)
        val commuteScore = if (commuteDiff <= 0) 1.0 else 1.0 / (1 + (commuteDiff.toDouble() / 2))
        score += commuteScore * weights.commute
        totalWeight += weights.commute

        // Budget similarity
        val budgetDiff = (other.stats.avgBudget - current.stats.avgBudget).coerceAtLeast(0)
        val budgetScore = if (budgetDiff == 0) 1.0 else 1.0 / (1 + (budgetDiff.toDouble() / 10))
        score += budgetScore * weights.budget
        totalWeight += weights.budget

        val baseScore = if (totalWeight > 0) score / totalWeight else 0.0 // normalised into [0,1]

        val uid = auth.currentUser!!.uid
        val lastSeen = current.members.first { it.id == uid }.seenGroupsTimestamps[other.id]
        val penalty = if (lastSeen != null) {
            val elapsed = now - lastSeen
            val decayFactor = (1.0 - (elapsed.toDouble() / decay_duration_ms)).coerceIn(0.0, 1.0)
            val p = weights.lastSeen * decayFactor
            Log.d( "MatchingService", "Penalty for ${other.members.joinToString { it.name }}: elapsed=${elapsed / 1000}s, decayFactor=$decayFactor, penalty=$p" )
            p
        } else { 0.0 }

        return baseScore - penalty
    }

    // Convert Firestore doc -> GroupProfile
    private suspend fun docToGroupProfileSafe(doc: DocumentSnapshot): GroupProfile? {
        if (!doc.exists()) return null

        val id = doc.id

        // Load members
        val membersSnap = db.collection("users")
            .whereEqualTo("groupId", id)
            .get()
            .await()
        val members = membersSnap.documents.mapNotNull { docToStudentProfileSafe(it) }

        val stats =
            GroupStats(
                size = members.size,
                avgBudget = members.map { it.studentMaxBudget }.average().toInt(),
                avgCommute = members.map { it.studentMaxCommute }.average().toInt(),
                avgAge = members.map { it.studentAge }.average().toInt()
            )

        return GroupProfile(
            id = id,
            name = if (members.size == 1) members[0].name else "Group: ${members.joinToString { it.name }}",
            members = members,
            stats = stats
        )
    }
}

