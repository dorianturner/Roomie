package com.example.roomie.components

data class GroupProfile(
    val id: String,
    val name: String,
    val members: List<StudentProfile>,
    val stats: GroupStats,
    val profilePicture: String,
    val bio: String,
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "members" to members.map { it.toMap() },
            "stats" to stats.toMap()
        )
    }
}

data class GroupStats(
    val size: Int,

    // --- Age ---
    val avgAge: Double?,        // mean
    val minAge: Int?,           // youngest
    val maxAge: Int?,           // oldest
    val ageStdDev: Double?,     // tight vs diverse group

    // --- Budget ---
    val avgBudget: Double?,
    val minBudget: Int?,
    val maxBudget: Int?,
    val budgetStdDev: Double?,

    // --- Commute ---
    val avgCommute: Double?,
    val minCommute: Int?,
    val maxCommute: Int?,
    val commuteStdDev: Double?,

    // --- Group size preferences ---
    val groupMin: Int?,
    val groupMax: Int?,

    // --- Bedtime ---
    val avgBedtime: Double?,
    val minBedtime: Int?,
    val maxBedtime: Int?,
    val bedtimeStdDev: Double?,

    // --- Alcohol ---
    val avgAlcohol: Double?,
    val minAlcohol: Int?,
    val maxAlcohol: Int?,
    val alcoholStdDev: Double?,

    // --- Lifestyle (mode / most common choice) ---
    val commonSmokingStatus: String?,  // "non-smoker", "smoker"
    val commonPets: String?,           // "cat person", "dog person", "no pets"

    // --- Personality/soft factors ---
    val topPassions: List<String>,     // top N shared keywords
    val topPetPeeves: List<String>,

    // --- Meta ---
    val universities: List<String>,     // mix of schools
    val profilePictureRatio: Double,    // % with profile pictures
    val status: Int
    // 0 - normal
    // 1 - merging
    // 2 - finalised
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "size" to size,

            "avgAge" to avgAge,
            "minAge" to minAge,
            "maxAge" to maxAge,
            "ageStdDev" to ageStdDev,

            "avgBudget" to avgBudget,
            "minBudget" to minBudget,
            "maxBudget" to maxBudget,
            "budgetStdDev" to budgetStdDev,

            "avgCommute" to avgCommute,
            "minCommute" to minCommute,
            "maxCommute" to maxCommute,
            "commuteStdDev" to commuteStdDev,

            "groupMin" to groupMin,
            "groupMax" to groupMax,

            "avgBedtime" to avgBedtime,
            "minBedtime" to minBedtime,
            "maxBedtime" to maxBedtime,
            "bedtimeStdDev" to bedtimeStdDev,

            "avgAlcohol" to avgAlcohol,
            "minAlcohol" to minAlcohol,
            "maxAlcohol" to maxAlcohol,
            "alcoholStdDev" to alcoholStdDev,

            "commonSmokingStatus" to commonSmokingStatus,
            "commonPets" to commonPets,

            "topPassions" to topPassions,
            "topPetPeeves" to topPetPeeves,

            "universities" to universities,
            "profilePictureRatio" to profilePictureRatio,

            "status" to status,
        )
    }
}
