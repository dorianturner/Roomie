package com.example.roomie.components

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import kotlinx.coroutines.tasks.await

fun computeStats(values: List<Int>): Quadruple<Double, Int, Int, Double> {
    if (values.isEmpty()) return Quadruple(0.0, 0, 0, 0.0)

    val avg = values.average()
    val min = values.minOrNull()!!
    val max = values.maxOrNull()!!
    val variance = values.map { (it - avg) * (it - avg) }.average()
    val stdDev = kotlin.math.sqrt(variance)

    return Quadruple(avg, min, max, stdDev)
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

fun generateGroupStats(members: List<StudentProfile>): GroupStats {
    val ages = members.mapNotNull { it.studentAge }
    val budgets = members.mapNotNull { it.studentMaxBudget }
    val commutes = members.mapNotNull { it.studentMaxCommute }
    val bedtimes = members.mapNotNull { it.studentBedtime }
    val alcohols = members.mapNotNull { it.studentAlcohol }

    val (avgAge, minAge, maxAge, ageStdDev) = computeStats(ages)
    val (avgBudget, minBudget, maxBudget, budgetStdDev) = computeStats(budgets)
    val (avgCommute, minCommute, maxCommute, commuteStdDev) = computeStats(commutes)
    val (avgBedtime, minBedtime, maxBedtime, bedtimeStdDev) = computeStats(bedtimes)
    val (avgAlcohol, minAlcohol, maxAlcohol, alcoholStdDev) = computeStats(alcohols)

    val groupMins = members.mapNotNull { it.groupMin }
    val groupMaxs = members.mapNotNull { it.groupMax }

    val smokingStatuses = members.map { it.studentSmokingStatus }.toSet()
    val commonSmokingStatus = if (smokingStatuses.size == 1) smokingStatuses.first() else null

    val pets = members.map { it.studentPet }.toSet()
    val commonPets = if (pets.size == 1) pets.first() else null

    fun topKeywords(lists: List<List<String>>, topN: Int = 3): List<String> {
        return lists.flatten()
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }

    fun getWords(text: String): List<String> {
        return text.split(Regex("\\s+"))  // split on one or more whitespace
            .filter { it.isNotBlank() }            // remove empty entries
    }

    val topPassions = topKeywords(members.map { getWords(it.passionate ?: "") })
    val topPetPeeves = topKeywords(members.map { getWords(it.studentPetPeeve ?: "") })

    val universities = members.mapNotNull { it.studentUniversity }.filter { it.isNotBlank() }.toSet().toList()

    val profilePictureRatio = members.count { it.profilePictureUrl != null }.toDouble() / members.size

    return GroupStats(
        size = members.size,

        avgAge = avgAge, minAge = minAge, maxAge = maxAge, ageStdDev = ageStdDev,
        avgBudget = avgBudget, minBudget = minBudget, maxBudget = maxBudget, budgetStdDev = budgetStdDev,
        avgCommute = avgCommute, minCommute = minCommute, maxCommute = maxCommute, commuteStdDev = commuteStdDev,
        avgBedtime = avgBedtime, minBedtime = minBedtime, maxBedtime = maxBedtime, bedtimeStdDev = bedtimeStdDev,
        avgAlcohol = avgAlcohol, minAlcohol = minAlcohol, maxAlcohol = maxAlcohol, alcoholStdDev = alcoholStdDev,
        groupMin = groupMins.minOrNull() ?: 0,
        groupMax = groupMaxs.maxOrNull() ?: 0,
        commonSmokingStatus = commonSmokingStatus,
        commonPets = commonPets,
        topPassions = topPassions,
        topPetPeeves = topPetPeeves,
        universities = universities,
        profilePictureRatio = profilePictureRatio
    )
}

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
        val (survivorId, killedId, survivorName) = db.runTransaction { transaction ->
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

            val membersCountA = groupASnap.getLong("membersCount")?.toInt() ?: 0
            val membersCountB = groupBSnap.getLong("membersCount")?.toInt() ?: 0
            val totalMembersCount = membersCountA + membersCountB

            // choose group with fewer members as killed ref - so less groupId updates
            val survivorRef = if (membersCountA > membersCountB) groupARef else groupBRef
            val killedRef = if (survivorRef == groupARef) groupBRef else groupARef

            transaction.update(survivorRef, mapOf(
                "membersCount" to totalMembersCount,
                "mergingWith" to null,
            ))

            transaction.delete(killedRef)

            Triple(
                survivorRef.id,
                killedRef.id,
                if (survivorRef == groupARef) groupASnap.getString("name") else groupBSnap.getString("name")
            )
        }.await()

        val usersA = db.collection("users")
            .whereEqualTo("groupId", groupAID)
            .get().await()

        val usersB = db.collection("users")
            .whereEqualTo("groupId", groupBID)
            .get().await()

        val mergedUsers = (usersA.documents + usersB.documents).mapNotNull { doc ->
            MatchingService.docToStudentProfileSafe(doc)
        }

        val groupStats = generateGroupStats(mergedUsers)

        val batch = db.batch()
        for (doc in if (killedId == groupAID) usersA.documents else usersB.documents) { // killed group’s users
            batch.update(doc.reference, "groupId", survivorId)
        }
        batch.commit().await()

        db.collection("groups").document(survivorId).update(
            mapOf(
                "stats" to groupStats,
                "name" to survivorName
            )
        ).await()

        val group = GroupProfile(
            id = survivorId,
            name = survivorName ?: "",
            members = mergedUsers,
            stats = groupStats,
            // TODO
            profilePicture = "",
            bio = ""
        )

        FunctionsProvider.instance
            .getHttpsCallable("upsertGroupProfileWithLock")
            .call(group.toMap())

        FunctionsProvider.instance
            .getHttpsCallable("deleteGroupFromBlob")
            .call(killedId)

        true
    } catch (e: Exception) {
        Log.e("Merging Groups", "Error finalising merge", e)
        false
    }
}

suspend fun cancelMerge(groupAID: String, groupBID: String): Boolean {
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

            transaction.update(groupARef, "mergingWith", null)
            transaction.update(groupBRef, "mergingWith", null)

            true
        }.await()
    } catch (e: Exception) {
        Log.e("MergeGroups", "Error cancelling merge", e)
        false
    }
}

fun cancelMergeTransaction(groupAID: String, groupBID: String, transaction: Transaction) {
    val db = FirebaseFirestore.getInstance()

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

    transaction.update(groupARef, "mergingWith", null)
    transaction.update(groupBRef, "mergingWith", null)
}