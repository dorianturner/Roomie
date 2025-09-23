package com.example.roomie.components

/**
 * Represents a group's profile.
 *
 * @property id The unique identifier of the group.
 * @property name The name of the group.
 * @property members A list of [StudentProfile] objects representing the members of the group.
 * @property stats A [GroupStats] object containing aggregated statistics about the group.
 * @property profilePicture The URL or identifier for the group's profile picture.
 * @property bio A short biography or description of the group.
 */
data class GroupProfile(
    val id: String,
    val name: String,
    val members: List<StudentProfile>,
    val stats: GroupStats,
    val profilePicture: String,
    val bio: String,
) {
    /**
     * Converts the [GroupProfile] object to a [Map] for database storage or other uses.
     *
     * @return A map representation of the group profile.
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "members" to members.map { it.toMap() },
            "stats" to stats.toMap()
        )
    }
}

/**
 * Represents aggregated statistics for a group.
 *
 * @property size The number of members in the group.
 * @property avgAge The average age of the group members (mean).
 * @property minAge The age of the youngest member in the group.
 * @property maxAge The age of the oldest member in the group.
 * @property ageStdDev The standard deviation of ages, indicating how diverse the group is in terms of age.
 * @property avgBudget The average budget of the group members.
 * @property minBudget The minimum budget among group members.
 * @property maxBudget The maximum budget among group members.
 * @property budgetStdDev The standard deviation of budgets.
 * @property avgCommute The average commute time of the group members.
 * @property minCommute The minimum commute time among group members.
 * @property maxCommute The maximum commute time among group members.
 * @property commuteStdDev The standard deviation of commute times.
 * @property groupMin The minimum preferred group size by members.
 * @property groupMax The maximum preferred group size by members.
 * @property avgBedtime The average bedtime of the group members.
 * @property minBedtime The earliest bedtime among group members.
 * @property maxBedtime The latest bedtime among group members.
 * @property bedtimeStdDev The standard deviation of bedtimes.
 * @property avgAlcohol The average alcohol consumption preference of the group members.
 * @property minAlcohol The minimum alcohol consumption preference among group members.
 * @property maxAlcohol The maximum alcohol consumption preference among group members.
 * @property alcoholStdDev The standard deviation of alcohol consumption preferences.
 * @property commonSmokingStatus The most common smoking status (e.g., "non-smoker", "smoker").
 * @property commonPets The most common pet preference (e.g., "cat person", "dog person", "no pets").
 * @property topPassions A list of the top N shared passions or keywords among group members.
 * @property topPetPeeves A list of the top N shared pet peeves among group members.
 * @property universities A list of universities attended by group members, indicating the mix of schools.
 * @property profilePictureRatio The percentage of group members who have profile pictures.
 * @property status The current status of the group (0 - normal, 1 - merging, 2 - finalised).
 */
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
    /**
     * Converts the [GroupStats] object to a [Map] for database storage or other uses.
     *
     * @return A map representation of the group statistics.
     */
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
