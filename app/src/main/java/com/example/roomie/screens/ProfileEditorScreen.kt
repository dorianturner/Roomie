package com.example.roomie.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import com.example.roomie.components.*
import com.example.roomie.ui.theme.Spacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.deletePhoto
import androidx.compose.runtime.rememberCoroutineScope
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch
import kotlin.collections.set

fun validateFields(fields: List<MutableState<ProfileTextField>>): Boolean {
    var isValid = true
    fields.forEach {
        if (!it.value.validate()) {
            isValid = false
        }
    }
    return isValid
}

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
    var isPartOfGroup by remember { mutableStateOf(false) }

    val smokingStatusState = remember { mutableStateOf("Neither") }
    val bedtimeState = remember { mutableIntStateOf(1) }
    val alcoholState = remember { mutableIntStateOf(1) }
    val petState = remember {mutableStateOf("No")}
    val musicField = remember { mutableStateOf(ProfileTextField("The type of music I like the most is...", "", required = false)) }
    val petPeeveField = remember { mutableStateOf(ProfileTextField("My biggest pet peeve is...", "", required = false)) }
    val addictedField = remember { mutableStateOf(ProfileTextField("My ideal night is...", "", required = false)) }
    val idealField = remember { mutableStateOf(ProfileTextField("I am completely addicted to", "", required = false)) }
    val passionateField = remember { mutableStateOf(ProfileTextField("I am passionate about", "", required = false),) }
    var groupId by remember { mutableStateOf<String?>(null) }

    val allFields = remember {
        mutableListOf<MutableState<ProfileTextField>>()
    }

    LaunchedEffect(auth.currentUser?.uid) {
        allFields.clear()

        // add mandatory fields
        allFields.add(nameField)
        allFields.add(bioField)
        allFields.add(phoneNumberField)

        if (isLandlord) {
            allFields.add(companyField)
        } else {
            allFields.addAll(
                listOf(
                    ageField,
                    universityField,
                    preferencesField,
                    groupSizeMinField,
                    groupSizeMaxField,
                    maxCommuteField,
                    maxBudgetField,
                )
            )
        }

        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        nameField.value.value = doc.getString("name").orEmpty()
                        bioField.value.value = doc.getString("bio").orEmpty()
                        phoneNumberField.value.value = doc.getString("phoneNumber").orEmpty()
                        isLandlord = doc.getString("profileType") == "landlord"
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
                            addictedField.value.value = doc.getString("studentAddicted").orEmpty()
                            idealField.value.value = doc.getString("studentIdeal").orEmpty()
                            passionateField.value.value = doc.getString("studentPassionate").orEmpty()
                            groupId = doc.getString("groupId")
                            Log.d("ProfileEditorScreen", "Loaded profile for user $uid, groupId = $groupId")
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
//        Spacer(modifier = Modifier.height(Spacing.medium))
//
//        // Profile type toggle
//        Text("I am a:", style = MaterialTheme.typography.titleMedium)
//        Spacer(modifier = Modifier.height(Spacing.extraShort))
//        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
//            SegmentedButton(
//                selected = !isLandlord,
//                onClick = { isLandlord = false },
//                shape = SegmentedButtonDefaults.baseShape
//            ) { Text("Student") }
//
//            SegmentedButton(
//                selected = isLandlord,
//                onClick = { isLandlord = true },
//                shape = SegmentedButtonDefaults.baseShape
//            ) { Text("Landlord") }
//        }

        Spacer(modifier = Modifier.height(Spacing.medium))

        // Common fields
        ProfilePictureEditor(
            currentUrl = profilePictureUrl,
            onPictureUpdated = { newUrl ->
                profilePictureUrl = newUrl
                // save this URL in Firestore when saving profile
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

        // Conditional profile sections
        if (isLandlord) {
            LandlordProfileSection(
                companyField = companyField.value,
                onCompanyChange = {
                    companyField.value.value = it
                }
            )
        } else {
            // Photo editor section
            ProfilePhotosEdit(
                modifier = Modifier.fillMaxWidth(),
                onPhotosChanged = { updated ->
                    photos = updated
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
                addicted = addictedField,
                ideal = idealField,
                passionate = passionateField
            )
        }

        Spacer(modifier = Modifier.height(Spacing.long))

        // save profile button
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

                    var isMinProfileSet = true

                    val batch = db.batch()

                    if (isLandlord) {
                        val company = companyField.value.value
                        val name = nameField.value.value
                        data["landlordCompany"] = company
                        if (name.isBlank() || company.isBlank()) isMinProfileSet = false

                        // delete all student photos in the cloud.
                        scope.launch {
                            deleteAllStudentPhotos(currentUser.uid, photos)
                            photos = emptyList()
                        }
                    } else {
                        val age = ageField.value.value.toIntOrNull()
                        val gMin = groupSizeMinField.value.value.toIntOrNull()
                        val gMax = groupSizeMaxField.value.value.toIntOrNull()
                        val commute = maxCommuteField.value.value.toIntOrNull()
                        val budget = maxBudgetField.value.value.toIntOrNull()
                        val name = nameField.value.value

                        data["studentAge"] = age ?: 0
                        data["studentUniversity"] = universityField.value.value
                        data["studentDesiredGroupSize"] = listOf(gMin, gMax)
                        data["studentMaxCommute"] = commute ?: 0
                        data["studentMaxBudget"] = budget ?: 0

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
                        data["studentAddicted"] = addictedField.value.value
                        data["studentIdeal"] = idealField.value.value
                        data["studentPassionate"] = passionateField.value.value

                        val currentStudent = StudentProfile(
                            id = currentUser.uid,
                            name = data["name"] as String,
                            studentAge = data["studentAge"] as Int?,
                            photos = listOf(),
                            profilePictureUrl = data["profilePictureUrl"] as String?,
                            studentPet = data["studentPet"] as String?,
                            studentBedtime = data["studentBedtime"] as Int?,
                            studentAlcohol = data["studentAlcohol"] as Int?,
                            studentSmokingStatus = data["studentSmokingStatus"] as String?,
                            groupMin = (data["studentDesiredGroupSize"] as List<Int>?)?.getOrNull(0),
                            groupMax = (data["studentDesiredGroupSize"] as List<Int>?)?.getOrNull(1),
                            studentMaxCommute = data["studentMaxCommute"] as Int?,
                            studentMaxBudget = data["studentMaxBudget"] as Int?,
                            studentUniversity = data["studentUniversity"] as String?,
                            bio = data["bio"] as String?,
                            studentAddicted = data["studentAddicted"] as String?,
                            studentPetPeeve = data["studentPetPeeve"] as String?,
                            passionate = data["studentPassionate"] as String?,
                            studentIdeal = data["studentIdeal"] as String?,
                            studentMusic = data["studentMusic"] as String?,
                            phoneNumber = data["phoneNumber"] as String?,
                        )

                        if (name.isBlank() || age == null ||
                            universityField.value.value.isBlank() ||
                            gMin == null || gMax == null ||
                            commute == null || budget == null
                        ) isMinProfileSet = false

                        if (groupId == null) {
                            // Case 1: user not in a group → create one
                            Log.d("ProfileEditorScreen", "Creating group for user ${currentUser.uid}")
                            data["groupId"] = currentUser.uid
                            val members: List<StudentProfile> = listOf(currentStudent)
                            val stats = generateGroupStats(members)

                            val groupProfile = GroupProfile(
                                id = currentUser.uid,
                                name = data["name"] as String,
                                members = members,
                                stats = stats,
                                // TODO
                                profilePicture = "",
                                bio = ""
                            )

                            FunctionsProvider.instance
                                .getHttpsCallable("upsertGroupProfile")
                                .call(groupProfile.toMap())
                                .addOnSuccessListener { result ->
                                    Log.d("SaveProfile","Group upsert success: ${result.data}")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("SaveProfile","Group upsert failure", e)
                                }

                            batch.set(db.collection("groups").document(currentUser.uid), groupProfile.toMap())
                        } else {
                            // Case 2: user already has a groupId → check stats.size
                            val groupRef = db.collection("groups").document(groupId!!)

                            groupRef.get().addOnSuccessListener { groupDoc ->
                                val stats = groupDoc.get("stats") as? Map<*, *>
                                val size = (stats?.get("size") as? Long) ?: 0L

                                if (size <= 10) {
                                    Log.d("ProfileEditorScreen", "Group has size <= 1, updating stats.")
                                    // Upsert stats because the group only has them
                                    val members: List<StudentProfile> = listOf(currentStudent)
                                    val stats = generateGroupStats(members)

                                    val groupProfile = GroupProfile(
                                        id = groupId!!,
                                        name = data["name"] as String,
                                        members = members,
                                        stats = stats,
                                        // TODO
                                        profilePicture = "",
                                        bio = ""
                                    )


                                    FunctionsProvider.instance
                                        .getHttpsCallable("upsertGroupProfile")
                                        .call(groupProfile.toMap())
                                        .addOnSuccessListener { result ->
                                            Log.d("SaveProfile","Group upsert success: ${result.data} with $groupProfile")
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("SaveProfile","Group upsert failure", e)
                                        }

                                    groupRef.set(groupProfile.toMap())
                                } else {
                                    Log.d("ProfileEditorScreen", "Group has size > 1, not updating stats.")
                                }
                            }
                        }

                    }

                    data["minimumRequiredProfileSet"] = isMinProfileSet

                    batch.set(
                        db.collection("users").document(currentUser.uid),
                        data,
                        SetOptions.merge()
                    )

                    batch.commit()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            onProfileSaved()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Error saving profile: ${it.message}", Toast.LENGTH_LONG).show()
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

suspend fun deleteAllStudentPhotos(uid: String, photos: List<PhotoItem>) {
    photos.forEach { photo ->
        deletePhoto(uid, photo.path)
    }
}