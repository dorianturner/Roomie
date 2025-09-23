package com.example.roomie.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.roomie.components.FunctionsProvider
import com.example.roomie.components.GroupProfile
import com.example.roomie.components.LandlordProfileSection
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.ProfileTextField
import com.example.roomie.components.ProfileTextFieldView
import com.example.roomie.components.StudentProfile
import com.example.roomie.components.StudentProfileSection
import com.example.roomie.components.deletePhoto
import com.example.roomie.components.generateGroupStats
import com.example.roomie.ui.theme.Spacing
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.launch

/**
 * Validates a list of [ProfileTextField]s by calling the `validate()` method on each.
 *
 * This function iterates through a provided list of [MutableState] objects, each holding
 * a [ProfileTextField]. It calls the `validate()` method of each `ProfileTextField` instance.
 * If any field's validation fails, this function returns `false`.
 *
 * @param fields A list of [MutableState]<[ProfileTextField]> representing the input fields to be validated.
 * @return `true` if all fields in the list are valid according to their respective `validate()` methods,
 *         `false` if at least one field is invalid.
 */
fun validateFields(fields: List<MutableState<ProfileTextField>>): Boolean {
    var isValid = true
    fields.forEach {
        if (!it.value.validate()) {
            isValid = false
        }
    }
    return isValid
}

/**
 * A composable screen that allows users to create or edit their profile information.
 *
 * The screen manages various profile fields, including common details (name, bio, phone number, profile picture),
 * landlord-specific fields (company name), and student-specific fields (age, university, group preferences,
 * lifestyle choices, and additional photos).
 *
 * Profile data is loaded from Firebase Firestore when the screen initializes or when the authenticated user's
 * ID changes. The `allFields` list, used for validation by [validateFields], includes common fields and
 * fields specific to the current profile type (landlord or student). Note that `preferencesField` for students
 * and the optional lifestyle text fields (music, pet peeve, etc.) are not automatically added to `allFields`
 * and thus are not validated by the generic `validateFields` function in the current setup; their validation
 * would need to be handled separately or they should be added to `allFields` if required.
 *
 * Upon saving, the screen validates the required fields. If valid, it constructs a data map and performs
 * a batch write to Firestore.
 * - For **landlord profiles**, it saves landlord-specific data. If the user was previously a student and
 *   switches to landlord, their student-specific photos are deleted.
 * - For **student profiles**, it saves student-specific data, including lifestyle preferences (smoking, bedtime,
 *   alcohol, which are explicitly validated). It also handles group association:
 *     - If the student has no `groupId`, a new group is created in Firestore. The student's UID is used
 *       as the new group's ID, and the student's name is used as the group's name. The student becomes
 *       the sole member. This new group's profile is also sent to the `upsertGroupProfile` Firebase Cloud Function.
 *     - If the student has an existing `groupId`, the group document is fetched from Firestore. If the
 *       group's `stats.size` is small (e.g., <= 10 based on current logic, though an earlier log message
 *       in the code had a condition of `<=1`), its statistics are regenerated based *solely* on the
 *       current student's updated profile. The updated group profile (name, members list containing only
 *       the current student, and new stats) is then saved back to Firestore and also sent to the
 *       `upsertGroupProfile` Firebase Cloud Function. If the group is larger, no update to the group's
 *       details is performed from this screen. (Note: TODOs for group profile picture and bio are present).
 *
 * After a successful save operation, the [onProfileSaved] callback is invoked.
 *
 * @param modifier Optional [Modifier] for customizing the layout and appearance of the screen.
 * @param onProfileSaved A callback function that is invoked when the profile has been successfully saved.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    modifier: Modifier = Modifier,
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { Firebase.auth }
    val db: FirebaseFirestore = remember { Firebase.firestore }
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    // photos
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }

    // Common fields
    val nameField = remember { mutableStateOf(ProfileTextField("Your Name", "")) }
    val bioField = remember { mutableStateOf(ProfileTextField("About you", "", required = false)) }
    val phoneNumberField = remember { mutableStateOf(ProfileTextField("Phone Number", "", KeyboardType.Phone, false)) }
    var isLandlord by remember { mutableStateOf(false) }

    // Landlord field
    val companyField = remember { mutableStateOf(ProfileTextField("Your Company Name", "")) }

    // Student fields
    val ageField = remember { mutableStateOf(ProfileTextField("Your Age", "", KeyboardType.Number)) }
    val universityField = remember { mutableStateOf(ProfileTextField("Your University", "")) }
    val preferencesField = remember { mutableStateOf(ProfileTextField("Basic Preferences", "", required = false)) }
    val groupSizeMinField = remember { mutableStateOf(ProfileTextField("Min", "", KeyboardType.Number)) }
    val groupSizeMaxField = remember { mutableStateOf(ProfileTextField("Max", "", KeyboardType.Number)) }
    val maxCommuteField = remember { mutableStateOf(ProfileTextField("Max Commute (mins)", "", KeyboardType.Number)) }
    val maxBudgetField = remember { mutableStateOf(ProfileTextField("Max Budget (£ / week)", "", KeyboardType.Number)) }

    val smokingStatusState = remember { mutableStateOf("Neither") }
    val bedtimeState = remember { mutableIntStateOf(1) }
    val alcoholState = remember { mutableIntStateOf(1) }
    val petState = remember {mutableStateOf("No")}
    val musicField = remember { mutableStateOf(ProfileTextField(
        "The type of music I like the most is...",
        "",
        required = false))
    }
    val petPeeveField = remember { mutableStateOf(ProfileTextField("My biggest pet peeve is...", "", required = false)) }
    val addictedField = remember { mutableStateOf(ProfileTextField("My ideal night is...", "", required = false)) }
    val idealField = remember { mutableStateOf(ProfileTextField("I am completely addicted to", "", required = false)) }
    val passionateField = remember { mutableStateOf(ProfileTextField("I am passionate about", "", required = false)) }
    var groupId by remember { mutableStateOf<String?>(null) }

    val allFields = remember {
        mutableListOf<MutableState<ProfileTextField>>()
    }

    LaunchedEffect(auth.currentUser?.uid) { // Re-runs when user ID changes
        allFields.clear()

        // add mandatory fields
        allFields.add(nameField)
        allFields.add(bioField) // Though not required, it's in allFields for consistency if its 'required' flag changes
        allFields.add(phoneNumberField) // Same as bioField

        // Conditionally add fields to `allFields` based on the profile type
        // This list is used by the `validateFields` function.
        if (isLandlord) {
            allFields.add(companyField)
        } else {
            allFields.addAll(
                listOf(
                    ageField,
                    universityField,
                    preferencesField, // Obsolete
                    groupSizeMinField,
                    groupSizeMaxField,
                    maxCommuteField,
                    maxBudgetField,
                )
            )
            // Note: Lifestyle string-based ProfileTextFields (musicField, petPeeveField, etc.)
            // are not added to `allFields` and thus not validated by `validateFields`.
        }

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        nameField.value.value = doc.getString("name").orEmpty()
                        bioField.value.value = doc.getString("bio").orEmpty()
                        phoneNumberField.value.value = doc.getString("phoneNumber").orEmpty()
                        // Determine profile type from loaded data
                        val loadedProfileType = doc.getString("profileType")
                        isLandlord = loadedProfileType == "landlord" // Update isLandlord state
                        profilePictureUrl = doc.getString("profilePictureUrl")

                        if (isLandlord) {
                            companyField.value.value = doc.getString("landlordCompany").orEmpty()
                        } else {
                            ageField.value.value = doc.getLong("studentAge")?.toString().orEmpty()
                            universityField.value.value = doc.getString("studentUniversity").orEmpty()
                            preferencesField.value.value = doc.getString("studentBasicPreferences").orEmpty()
                            val group = doc.get("studentDesiredGroupSize") as? List<*>
                            groupSizeMinField.value.value = (group?.getOrNull(0) as? Long)?.toString().orEmpty()
                            groupSizeMaxField.value.value = (group?.getOrNull(1) as? Long)?.toString().orEmpty()
                            maxCommuteField.value.value = doc.getLong("studentMaxCommute")?.toString().orEmpty()
                            maxBudgetField.value.value = doc.getLong("studentMaxBudget")?.toString().orEmpty()
                            // load lifestyle fields
                            smokingStatusState.value = doc.getString("studentSmokingStatus") ?: "Neither"
                            val bedtimeAny = doc.get("studentBedtime")
                            bedtimeState.intValue = when (bedtimeAny) {
                                is Number -> bedtimeAny.toInt()
                                is String -> bedtimeAny.toIntOrNull() ?: 1
                                else -> 1
                            }
                            alcoholState.intValue = (doc.getLong("studentAlcohol")?.toInt() ?: 1)
                            petState.value = doc.getString("studentPet") ?: "No"
                            musicField.value.value = doc.getString("studentMusic").orEmpty()
                            petPeeveField.value.value = doc.getString("studentPetPeeve").orEmpty()
                            addictedField.value.value = doc.getString("studentAddicted").orEmpty() // Label: "My ideal night is..."
                            idealField.value.value = doc.getString("studentIdeal").orEmpty()       // Label: "I am completely addicted to"
                            passionateField.value.value = doc.getString("studentPassionate").orEmpty()
                            groupId = doc.getString("groupId")
                            Log.d("ProfileEditorScreen", "Loaded profile for user $uid, groupId = $groupId, isLandlord = $isLandlord")
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(vertical = Spacing.short),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(Spacing.short))

        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Common fields: Profile Picture Editor
        ProfilePictureEditor(
            currentUrl = profilePictureUrl,
            onPictureUpdated = { newUrl ->
                profilePictureUrl = newUrl
                // URL is saved to Firestore when the main "Save Profile" button is clicked
            }
        )

        Spacer(modifier = Modifier.height(Spacing.short))

        ProfileTextFieldView(
            field = nameField.value,
            onValueChange = {
                nameField.value.value = it
            }
        )

        Spacer(modifier = Modifier.height(Spacing.short))

        ProfileTextFieldView(
            field = bioField.value,
            onValueChange = {
                bioField.value.value = it
            }
        )

        Spacer(modifier = Modifier.height(Spacing.short))

        ProfileTextFieldView(
            field = phoneNumberField.value,
            onValueChange = {
                phoneNumberField.value.value = it
            }
        )

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Conditional profile sections based on isLandlord state
        if (isLandlord) {
            LandlordProfileSection(
                companyField = companyField.value,
                onCompanyChange = {
                    companyField.value.value = it
                }
            )
        } else {
            // Student-specific sections: Photo editor and detailed student profile fields
            ProfilePhotosEdit(
                modifier = Modifier.fillMaxWidth(),
                onPhotosChanged = { updated ->
                    photos = updated // 'photos' state is updated, used when saving if user becomes landlord
                }
            )

            Spacer(modifier = Modifier.height(Spacing.short))

            StudentProfileSection(
                ageField = ageField,
                universityField = universityField,
                groupSizeMinField = groupSizeMinField,
                groupSizeMaxField = groupSizeMaxField,
                maxCommuteField = maxCommuteField,
                maxBudgetField = maxBudgetField,
                smokingStatus = smokingStatusState,
                bedtime = bedtimeState,
                alcoholLevel = alcoholState,
                pet = petState,
                musicField = musicField,
                petPeeve = petPeeveField,
                addicted = addictedField, // "My ideal night is..."
                ideal = idealField,       // "I am completely addicted to"
                passionate = passionateField
            )
        }

        Spacer(modifier = Modifier.height(Spacing.long))

        // Save profile button
        Button(
            onClick = {
                val isValid = validateFields(allFields)

                if (!isValid) {
                    Toast.makeText(context, "Please fill all required fields.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val data = mutableMapOf<String, Any>(
                        "name" to nameField.value.value,
                        "bio" to bioField.value.value,
                        "phoneNumber" to phoneNumberField.value.value,
                        "profileType" to if (isLandlord) "landlord" else "student",
                        "lastUpdated" to System.currentTimeMillis(),
                    )

                    data["profilePictureUrl"] = profilePictureUrl ?: ""

                    var isMinProfileSet = true // Flag to check if minimum required fields for a profile type are set

                    val batch = db.batch()

                    if (isLandlord) {
                        val company = companyField.value.value
                        val name = nameField.value.value
                        data["landlordCompany"] = company
                        if (name.isBlank() || company.isBlank()) isMinProfileSet = false

                        // If user becomes a landlord, delete their existing student photos from storage
                        scope.launch {
                            deleteAllStudentPhotos(currentUser.uid, photos)
                            photos = emptyList() // Clear local photo state
                        }
                    } else { // Student profile
                        val age = ageField.value.value.toIntOrNull()
                        val gMin = groupSizeMinField.value.value.toIntOrNull()
                        val gMax = groupSizeMaxField.value.value.toIntOrNull()
                        val commute = maxCommuteField.value.value.toIntOrNull()
                        val budget = maxBudgetField.value.value.toIntOrNull()
                        val name = nameField.value.value

                        data["studentAge"] = age ?: 0
                        data["studentUniversity"] = universityField.value.value
                        data["studentDesiredGroupSize"] = listOf(gMin, gMax)
                        data["studentMaxCommute"] = commute ?: 0 // Default to 0 if not specified
                        data["studentMaxBudget"] = budget ?: 0 // Default to 0 if not specified

                        // Validate core lifestyle fields before saving
                        if (smokingStatusState.value.isBlank() || bedtimeState.intValue !in 1..5 || alcoholState.intValue !in 1..5) {
                            Toast.makeText(context, "Please fill smoking, bedtime and drinking preferences.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        data["studentSmokingStatus"] = smokingStatusState.value
                        data["studentBedtime"] = bedtimeState.intValue
                        data["studentAlcohol"] = alcoholState.intValue
                        data["studentPet"] = petState.value
                        data["studentMusic"] = musicField.value.value
                        data["studentPetPeeve"] = petPeeveField.value.value
                        data["studentAddicted"] = addictedField.value.value // "My ideal night is..."
                        data["studentIdeal"] = idealField.value.value       // "I am completely addicted to"
                        data["studentPassionate"] = passionateField.value.value

                        val groupSizes = data["studentDesiredGroupSize"] as? List<*>

                        // Create a StudentProfile object from the current data for group operations
                        val currentStudent = StudentProfile(
                            id = currentUser.uid,
                            name = data["name"] as String,
                            studentAge = data["studentAge"] as Int?,
                            photos = listOf(), // Photos for group stats would come from a different source if needed
                            profilePictureUrl = data["profilePictureUrl"] as String?,
                            studentPet = data["studentPet"] as String?,
                            studentBedtime = data["studentBedtime"] as Int?,
                            studentAlcohol = data["studentAlcohol"] as Int?,
                            studentSmokingStatus = data["studentSmokingStatus"] as String?,
                            groupMin = groupSizes?.getOrNull(0) as? Int,
                            groupMax = groupSizes?.getOrNull(1) as? Int,
                            studentMaxCommute = data["studentMaxCommute"] as Int?,
                            studentMaxBudget = data["studentMaxBudget"] as Int?,
                            studentUniversity = data["studentUniversity"] as String?,
                            bio = data["bio"] as String?,
                            studentAddicted = data["studentAddicted"] as String?, // "My ideal night is..."
                            studentPetPeeve = data["studentPetPeeve"] as String?, // "My biggest pet peeve is..."
                            passionate = data["studentPassionate"] as String?,    // "I am passionate about"
                            studentIdeal = data["studentIdeal"] as String?,       // "I am completely addicted to"
                            studentMusic = data["studentMusic"] as String?,       // "The type of music I like the most is..."
                            phoneNumber = data["phoneNumber"] as String?,
                        )

                        // Check minimum fields for student profile type
                        if (name.isBlank() || age == null ||
                            universityField.value.value.isBlank() ||
                            gMin == null || gMax == null ||
                            commute == null || budget == null
                        ) isMinProfileSet = false

                        if (groupId == null) {
                            // Case 1: Student is not in a group -> create a new group for them
                            Log.d("ProfileEditorScreen", "Creating group for user ${currentUser.uid}")
                            data["groupId"] = currentUser.uid // Assign user's UID as their new group ID
                            val members: List<StudentProfile> = listOf(currentStudent)
                            val stats = generateGroupStats(members) // Generate stats for the new group

                            val groupProfile = GroupProfile(
                                id = currentUser.uid, // Group ID is user's UID
                                name = data["name"] as String, // Group name is user's name
                                members = members,
                                stats = stats,
                                profilePicture = "",
                                bio = ""
                            )

                            // Upsert group profile to blob storage / search index via Cloud Function
                            FunctionsProvider.instance
                                .getHttpsCallable("upsertGroupProfile")
                                .call(groupProfile.toMap())
                                .addOnSuccessListener { result ->
                                    Log.d("SaveProfile","Group upsert success: ${result.data}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("SaveProfile","Group upsert failure", e)
                                }
                            // Add group creation to Firestore batch
                            batch.set(db.collection("groups").document(currentUser.uid), groupProfile.toMap())
                        } else {
                            // Case 2: Student already has a groupId -> check group size and update if small
                            val groupRef = db.collection("groups").document(groupId!!)

                            groupRef.get().addOnSuccessListener { groupDoc ->
                                if (groupDoc.exists()) {
                                    val groupData = groupDoc.data
                                    val statsMap = groupData?.get("stats") as? Map<*, *>
                                    val statusAny = statsMap?.get("status")
                                    val status: Int = when (statusAny) { // Handle potential number types for status
                                        is Long -> statusAny.toInt()
                                        is Double -> statusAny.toInt()
                                        is Int -> statusAny
                                        else -> 0 // Default status if not found or wrong type
                                    }
                                    val size = (statsMap?.get("size") as? Number)?.toLong() ?: 0L // Handle potential number types for size

                                    if (size <= 1) { // Condition to update group if only one person
                                        Log.d("ProfileEditorScreen", "Group $groupId has size $size, updating stats based on current user.")
                                        // Update group: members list contains only the current student, stats are regenerated
                                        val members: List<StudentProfile> = listOf(currentStudent)
                                        val newStats = generateGroupStats(members).copy(status = status) // Preserve existing status

                                        val updatedGroupProfile = GroupProfile(
                                            id = groupId!!,
                                            name = groupData?.get("name") as String,
                                            members = members,             // Members list is just the current student
                                            stats = newStats,
                                            profilePicture = groupData.get("profilePicture") as? String ?: "", // Preserve existing
                                            bio = groupData.get("bio") as? String ?: ""                         // Preserve existing
                                        )

                                        // Upsert updated group profile to blob storage / search index
                                        FunctionsProvider.instance
                                            .getHttpsCallable("upsertGroupProfile")
                                            .call(updatedGroupProfile.toMap())
                                            .addOnSuccessListener { result ->
                                                Log.d("SaveProfile","Updated group upsert success: ${result.data} for group $groupId")
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("SaveProfile","Updated group upsert failure for $groupId", e)
                                            }
                                        // Update group in Firestore (added to batch later, this is direct for now in example)
                                        // Ideally, this should also be part of the batch if possible, or handled carefully.
                                        // For simplicity here, it's a direct set, but a batch update would be safer.
                                        // batch.set(groupRef, updatedGroupProfile.toMap()) // This would be the batched version
                                        groupRef.set(updatedGroupProfile.toMap()) // Direct set as per original logic structure
                                             .addOnFailureListener { e -> Log.e("ProfileEditorScreen", "Failed to update group $groupId in Firestore directly.", e)}

                                    } else {
                                        Log.d("ProfileEditorScreen", "Group $groupId has size $size (>10), not updating stats from profile edit.")
                                    }
                                } else {
                                    Log.w("ProfileEditorScreen", "Group $groupId not found, cannot update stats.")
                                }
                            }.addOnFailureListener { e ->
                                Log.e("ProfileEditorScreen", "Failed to get group $groupId for stats update.", e)
                            }
                        }
                    }

                    data["minimumRequiredProfileSet"] = isMinProfileSet

                    // Update the user's document in Firestore with all collected data
                    batch.set(
                        db.collection("users").document(currentUser.uid),
                        data,
                        SetOptions.merge() // Merge to avoid overwriting fields not handled here
                    )

                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            onProfileSaved() // Callback to navigate or refresh
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "No user logged in.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }

        Spacer(modifier = Modifier.height(Spacing.short))
    }
}

/**
 * Deletes all specified student photos from Firebase Storage.
 *
 * This function iterates through a list of [PhotoItem] objects and calls [deletePhoto]
 * for each photo, using the provided user ID (`uid`) and the photo's path.
 *
 * @param uid The unique identifier of the user whose photos are being deleted.
 * @param photos A list of [PhotoItem] objects representing the photos to be deleted.
 *               Each [PhotoItem] should contain the `path` to the photo in Firebase Storage.
 */
suspend fun deleteAllStudentPhotos(uid: String, photos: List<PhotoItem>) {
    photos.forEach { photo ->
        // Assuming deletePhoto is a suspend function that handles deletion from Firebase Storage
        deletePhoto(uid, photo.path) 
    }
}
