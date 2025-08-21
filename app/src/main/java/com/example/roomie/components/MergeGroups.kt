package com.example.roomie.components

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

suspend fun mergeGroups(groupAID: String, groupBID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        db.runTransaction { transaction ->
            val groupARef = db.collection("groups").document(groupAID)
            val groupBRef = db.collection("groups").document(groupBID)

            val groupASnap = transaction.get(groupARef)
            val groupBSnap = transaction.get(groupBRef)

            if (!groupASnap.exists() || !groupBSnap.exists()) {
                throw Exception("One or both groups do not exist")
            }

            if (groupASnap.getString("mergingWith") != null ||
                groupBSnap.getString("mergingWith") != null) {
                throw Exception("One or both groups are already being merged")
            }

            transaction.update(groupARef, "mergingWith", groupBID)
            transaction.update(groupBRef, "mergingWith", groupAID)

            true
        }.await()
    } catch (e: Exception) {
        false
    }
}

suspend fun finaliseMergeGroups(groupAID: String, groupBID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        db.runTransaction { transaction ->
            val groupARef = db.collection("groups").document(groupAID)
            val groupBRef = db.collection("groups").document(groupBID)

            val groupASnap = transaction.get(groupARef)
            val groupBSnap = transaction.get(groupBRef)

            if (!groupASnap.exists() || !groupBSnap.exists()) {
                throw Exception("One or both groups do not exist")
            }

            val mergingA = groupASnap.getString("mergingWith")
            val mergingB = groupBSnap.getString("mergingWith")

            if (mergingA != groupBID || mergingB != groupAID) {
                throw Exception("Groups are not mutually merging")
            }

            val membersCountA = groupASnap.getLong("membersCount") ?: 0
            val membersCountB = groupBSnap.getLong("membersCount") ?: 0
            val totalMembersCount = membersCountA + membersCountB

            val statsA = groupASnap.get("stats") as? Map<String, Number> ?: emptyMap()
            val statsB = groupBSnap.get("stats") as? Map<String, Number> ?: emptyMap()

            val mergedStats = mutableMapOf<String, Number>()
            (statsA.keys + statsB.keys).forEach { key ->
                val aVal = (statsA[key]?.toDouble() ?: 0.0)
                val bVal = (statsB[key]?.toDouble() ?: 0.0)
                mergedStats[key] = aVal + bVal
            }

            // choose group with fewer members as killed ref - so less groupId updates
            val survivorRef = if (membersCountA > membersCountB) groupARef else groupBRef
            val killedRef = if (survivorRef == groupARef) groupBRef else groupARef

            transaction.update(survivorRef, mapOf(
                "membersCount" to totalMembersCount,
                "stats" to mergedStats,
                "mergingWith" to null,
            ))

            transaction.delete(killedRef)

            survivorRef.id to killedRef.id
        }.await().also { (survivorId, killedId) ->
            val killedUsers = db.collection("users")
                .whereEqualTo("groupId", killedId)
                .get()
                .await()

            val batch = db.batch()
            for (doc in killedUsers.documents) {
                batch.update(doc.reference, "groupId", survivorId)
            }
            batch.commit().await()
        }

        true
    } catch (e: Exception) {
        false
    }
}