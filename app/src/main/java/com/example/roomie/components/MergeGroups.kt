package com.example.roomie.components

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Computes basic statistics (average, minimum, maximum, and standard deviation) for a list of integers.
 *
 * @param values A list of [Int] for which to compute statistics.
 * @return A [Quadruple] containing the average (Double), minimum (Int), maximum (Int), and standard deviation (Double).
 *         Returns (0.0, 0, 0, 0.0) if the input list is empty.
 */
fun computeStats(values: List<Int>): Quadruple<Double, Int, Int, Double> {
    if (values.isEmpty()) return Quadruple(0.0, 0, 0, 0.0)

    val avg = values.average()
    val min = values.minOrNull()!!
    val max = values.maxOrNull()!!
    val variance = values.map { (it - avg) * (it - avg) }.average()
    val stdDev = kotlin.math.sqrt(variance)

    return Quadruple(avg, min, max, stdDev)
}

/**
 * A generic data class to hold four values of potentially different types.
 *
 * @param A The type of the first value.
 * @param B The type of the second value.
 * @param C The type of the third value.
 * @param D The type of the fourth value.
 * @property first The first value.
 * @property second The second value.
 * @property third The third value.
 * @property fourth The fourth value.
 */
data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

/**
 * Generates aggregated statistics for a group based on its members' profiles.
 *
 * @param members A list of [StudentProfile] objects representing the members of the group.
 * @return A [GroupStats] object containing the calculated statistics.
 */
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

    /**
     * Extracts the top N most frequent keywords from a list of string lists.
     *
     * @param lists A list of lists of strings.
     * @param topN The number of top keywords to return. Defaults to 3.
     * @return A list of the top N keywords.
     */
    fun topKeywords(lists: List<List<String>>, topN: Int = 3): List<String> {
        return lists.flatten()
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(topN)
            .map { it.key }
    }

    /**
     * Splits a text into a list of words, filtering out blank entries.
     *
     * @param text The input string.
     * @return A list of words.
     */
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
        profilePictureRatio = profilePictureRatio,

        status = 0
    )
}

/**
 * Initiates the merging process for two groups.
 * This involves a Firestore transaction to set `mergingWith` flags on both group documents
 * and update their `stats.status` to 1 (merging). It then updates the corresponding
 * group profiles in a binary blob storage (via Firebase Functions) to reflect this merging status.
 *
 * @param groupAID The ID of the first group.
 * @param groupBID The ID of the second group.
 * @return `true` if the merge initiation was successful (both Firestore and blob updates), `false` otherwise.
 */
suspend fun mergeGroups(groupAID: String, groupBID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        // Step 1: set Firestore flags
        db.runTransaction { transaction ->
            val groupARef = db.collection("groups").document(groupAID)
            val groupBRef = db.collection("groups").document(groupBID)

            val groupASnap = transaction.get(groupARef)
            val groupBSnap = transaction.get(groupBRef)

            check(
                groupASnap.exists() && groupBSnap.exists()
            ) { "One or both groups do not exist" }

            check(
                groupASnap.getString("mergingWith") == null &&
                        groupBSnap.getString("mergingWith") == null
            ) { "One or both groups are already being merged" }

            transaction.update(groupARef, "mergingWith", groupBID)
            transaction.update(groupBRef, "mergingWith", groupAID)
            transaction.update(groupARef, "stats.status", 1)
            transaction.update(groupBRef, "stats.status", 1)

            groupASnap to groupBSnap
        }.await().let { (groupASnap, groupBSnap) ->
            // Step 2: set binary blob flags
            try {
                val groupA = MatchingService.docToGroupProfileSafe(groupASnap)
                val groupB = MatchingService.docToGroupProfileSafe(groupBSnap)

                val groupANew = groupA!!.copy(stats = groupA.stats.copy(status = 1))
                val groupBNew = groupB!!.copy(stats = groupB.stats.copy(status = 1))
                Log.d("MergeGroups", "Setting 'do not show' flag in blob")
                Log.d("MergeGroups", "Group A: $groupANew")
                Log.d("MergeGroups", "Group B: $groupBNew")

                val resA = FunctionsProvider.instance
                    .getHttpsCallable("upsertGroupProfile")
                    .call(groupANew.toMap())
                    .await()

                Log.d("SaveProfile", "Group upsert success: ${resA.data} with ${groupANew.id}")

                val resB = FunctionsProvider.instance
                    .getHttpsCallable("upsertGroupProfile")
                    .call(groupBNew.toMap())
                    .await()

                Log.d("SaveProfile", "Group upsert success: ${resB.data} with ${groupBNew.id}")

                Log.d("MergeGroups", "Successfully set 'do not show' flag in blob")
                true
            } catch (e: Exception) {
                Log.e("MergeGroups", "Failed to set 'do not show' flag in blob", e)
                false
            }
        }
    } catch (e: Exception) {
        Log.e("MergeGroups", "Error merging", e)

        false
    }
}

/**
 * Finalises the merge of two groups that have already been marked for merging.
 * This function performs the following steps in a Firestore transaction:
 * 1. Identifies a "survivor" group (preferentially the one with more members) and a "killed" group.
 * 2. Updates the survivor group's member count and clears its `mergingWith` flag.
 * 3. Deletes the killed group's document.
 * After the transaction, it:
 * 4. Fetches all users from both original groups.
 * 5. Updates the `groupId` of users from the killed group to point to the survivor group.
 * 6. Regenerates [GroupStats] for the merged group.
 * 7. Updates the survivor group's document with the new stats and name.
 * 8. Updates the survivor group's profile and deletes the killed group's profile from blob storage
 *    (via Firebase Functions `upsertGroupProfileWithLock` and `deleteGroupFromBlob`).
 *
 * @param groupAID The ID of the first group involved in the merge.
 * @param groupBID The ID of the second group involved in the merge.
 * @return `true` if the finalisation process is successful, `false` otherwise.
 */
suspend fun finaliseMergeGroups(groupAID: String, groupBID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        val (survivorId, killedId, survivorName) = db.runTransaction { transaction ->
            val groupARef = db.collection("groups").document(groupAID)
            val groupBRef = db.collection("groups").document(groupBID)

            val groupASnap = transaction.get(groupARef)
            val groupBSnap = transaction.get(groupBRef)

            if (!groupASnap.exists() || !groupBSnap.exists()) {
                throw IllegalAccessException("One or both groups do not exist")
            }

            val mergingA = groupASnap.getString("mergingWith")
            val mergingB = groupBSnap.getString("mergingWith")

            if (mergingA != groupBID || mergingB != groupAID) {
                throw IllegalStateException("Groups are not mutually merging")
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

/**
 * Cancels an ongoing merge operation between two groups.
 * This function performs a Firestore transaction to clear the `mergingWith` flags and reset
 * `stats.status` to 0 (normal) for both groups. It then updates the corresponding
 * group profiles in blob storage (via Firebase Functions) to reflect this cancellation.
 *
 * @param groupAID The ID of the first group.
 * @param groupBID The ID of the second group.
 * @return `true` if the cancellation was successful (both Firestore and blob updates), `false` otherwise.
 */
suspend fun cancelMerge(groupAID: String, groupBID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        db.runTransaction { transaction ->
            val groupARef = db.collection("groups").document(groupAID)
            val groupBRef = db.collection("groups").document(groupBID)

            val groupASnap = transaction.get(groupARef)
            val groupBSnap = transaction.get(groupBRef)

            check(
                groupASnap.exists() && groupBSnap.exists()
            ) { "One or both groups do not exist" }

            val mergingA = groupASnap.getString("mergingWith")
            val mergingB = groupBSnap.getString("mergingWith")

            check(
                mergingA == groupBID && mergingB == groupAID
            ) { "Groups are not mutually merging" }

            transaction.update(groupARef, "mergingWith", null)
            transaction.update(groupBRef, "mergingWith", null)
            transaction.update(groupARef, "stats.status", 0)
            transaction.update(groupBRef, "stats.status", 0)

            groupASnap to groupBSnap
        }.await().let { (aSnap, bSnap) ->
            try {
                val groupA = MatchingService.docToGroupProfileSafe(aSnap)
                val groupB = MatchingService.docToGroupProfileSafe(bSnap)

                val groupANew = groupA!!.copy(stats = groupA.stats.copy(status = 0))
                val groupBNew = groupB!!.copy(stats = groupB.stats.copy(status = 0))
                Log.d("MergeGroups", "Unsetting 'do not show' flag in blob")
                Log.d("MergeGroups", "Group A: $groupANew")
                Log.d("MergeGroups", "Group B: $groupBNew")

                val resA = FunctionsProvider.instance
                    .getHttpsCallable("upsertGroupProfile")
                    .call(groupANew.toMap())
                    .await()

                Log.d("SaveProfile", "Group upsert success: ${resA.data} with ${groupANew.id}")

                val resB = FunctionsProvider.instance
                    .getHttpsCallable("upsertGroupProfile")
                    .call(groupBNew.toMap())
                    .await()

                Log.d("SaveProfile", "Group upsert success: ${resB.data} with ${groupBNew.id}")

                Log.d("MergeGroups", "Successfully unset 'do not show' flag in blob")
                true
            } catch (e: Exception) {
                Log.e("MergeGroups", "Failed to unset 'do not show' flag in blob", e)
                false
            }
        }
    } catch (e: Exception) {
        Log.e("MergeGroups", "Error cancelling merge", e)
        false
    }
}

/**
 * Finalises a group, setting its status to 2.
 * This typically means the group is locked and no longer actively participating in matching.
 * It updates the group's status in both Firestore and blob storage (via Firebase Functions).
 *
 * @param groupID The ID of the group to finalise.
 * @return `true` if the finalisation was successful, `false` otherwise.
 */
suspend fun finaliseGroup(groupID: String): Boolean {
    val db = FirebaseFirestore.getInstance()

    return try {
        val groupRef = db.collection("groups").document(groupID)
        val snap = groupRef.get().await()
        val group = MatchingService.docToGroupProfileSafe(snap)

        FunctionsProvider.instance
            .getHttpsCallable("upsertGroupProfileWithLock")
            .call(group!!.copy(stats = group.stats.copy(status = 2)).toMap())

        groupRef.update("stats.status", 2).await()

        true
    } catch (e: Exception) {
        Log.e("FinaliseGroups", "Failed to set 'do not show' flag in blob", e)

        false
    }
}
