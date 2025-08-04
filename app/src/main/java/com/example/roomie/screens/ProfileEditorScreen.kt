package com.example.roomie.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.roomie.components.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorScreen(
    onProfileSaved: () -> Unit
) {
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { Firebase.auth }
    val db: FirebaseFirestore = remember { Firebase.firestore }
    val scrollState = rememberScrollState()

    // Common fields
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLandlord by remember { mutableStateOf(false) }

    // Landlord field
    val companyField = remember { mutableStateOf(ProfileTextField("Your Company Name", "")) }

    // Student fields
    val ageField = remember { mutableStateOf(ProfileTextField("Your Age", "", KeyboardType.Number)) }
    val universityField = remember { mutableStateOf(ProfileTextField("Your University", "")) }
    val preferencesField = remember { mutableStateOf(ProfileTextField("Basic Preferences", "")) }
    val groupSizeMinField = remember { mutableStateOf(ProfileTextField("Min Group Size", "", KeyboardType.Number)) }
    val groupSizeMaxField = remember { mutableStateOf(ProfileTextField("Max Group Size", "", KeyboardType.Number)) }
    val maxCommuteField = remember { mutableStateOf(ProfileTextField("Max Commute (mins)", "", KeyboardType.Number)) }
    val maxBudgetField = remember { mutableStateOf(ProfileTextField("Max Budget (£ / week)", "", KeyboardType.Number)) }

    LaunchedEffect(auth.currentUser?.uid) {
        auth.currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        name = doc.getString("name").orEmpty()
                        bio = doc.getString("bio").orEmpty()
                        phoneNumber = doc.getString("phoneNumber").orEmpty()
                        isLandlord = doc.getString("profileType") == "landlord"

                        if (isLandlord) {
                            companyField.value = companyField.value.copy(value = doc.getString("landlordCompany").orEmpty())
                        } else {
                            ageField.value = ageField.value.copy(value = doc.getLong("studentAge")?.toString().orEmpty())
                            universityField.value = universityField.value.copy(value = doc.getString("studentUniversity").orEmpty())
                            preferencesField.value = preferencesField.value.copy(value = doc.getString("studentBasicPreferences").orEmpty())
                            val group = doc.get("studentDesiredGroupSize") as? List<*>
                            groupSizeMinField.value = groupSizeMinField.value.copy(value = (group?.getOrNull(0) as? Long)?.toString().orEmpty())
                            groupSizeMaxField.value = groupSizeMaxField.value.copy(value = (group?.getOrNull(1) as? Long)?.toString().orEmpty())
                            maxCommuteField.value = maxCommuteField.value.copy(value = doc.getLong("studentMaxCommute")?.toString().orEmpty())
                            maxBudgetField.value = maxBudgetField.value.copy(value = doc.getLong("studentMaxBudget")?.toString().orEmpty())
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Failed to load profile: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Profile type toggle
        Text("I am a:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = !isLandlord,
                onClick = { isLandlord = false },
                shape = SegmentedButtonDefaults.baseShape
            ) { Text("Student") }

            SegmentedButton(
                selected = isLandlord,
                onClick = { isLandlord = true },
                shape = SegmentedButtonDefaults.baseShape
            ) { Text("Landlord") }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Common fields
        ProfileTextFieldView(
            field = ProfileTextField("Your Name", name),
            onValueChange = { name = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProfileTextFieldView(
            field = ProfileTextField("Bio (Tell us about yourself!)", bio),
            onValueChange = { bio = it }
        )
        Spacer(modifier = Modifier.height(16.dp))
        ProfileTextFieldView(
            field = ProfileTextField("Phone Number (Optional)", phoneNumber, KeyboardType.Phone),
            onValueChange = { phoneNumber = it }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Conditional profile sections
        if (isLandlord) {
            LandlordProfileSection(
                companyField = companyField.value,
                onCompanyChange = { companyField.value = companyField.value.copy(value = it) }
            )
        } else {
            StudentProfileSection(
                ageField = ageField,
                universityField = universityField,
                preferencesField = preferencesField,
                groupSizeMinField = groupSizeMinField,
                groupSizeMaxField = groupSizeMaxField,
                maxCommuteField = maxCommuteField,
                maxBudgetField = maxBudgetField,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val data = mutableMapOf<String, Any>(
                        "name" to name,
                        "bio" to bio,
                        "phoneNumber" to phoneNumber,
                        "profileType" to if (isLandlord) "landlord" else "student",
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    var isMinProfileSet = true

                    if (isLandlord) {
                        val company = companyField.value.value
                        data["landlordCompany"] = company
                        if (name.isBlank() || company.isBlank()) isMinProfileSet = false
                    } else {
                        val age = ageField.value.value.toIntOrNull()
                        val gMin = groupSizeMinField.value.value.toIntOrNull()
                        val gMax = groupSizeMaxField.value.value.toIntOrNull()
                        val commute = maxCommuteField.value.value.toIntOrNull()
                        val budget = maxBudgetField.value.value.toIntOrNull()

                        data["studentAge"] = age ?: 0
                        data["studentUniversity"] = universityField.value.value
                        data["studentBasicPreferences"] = preferencesField.value.value
                        data["studentDesiredGroupSize"] = listOf(gMin, gMax)
                        data["studentMaxCommute"] = commute ?: 0
                        data["studentMaxBudget"] = budget ?: 0

                        if (name.isBlank() || age == null ||
                            universityField.value.value.isBlank() ||
                            preferencesField.value.value.isBlank() ||
                            gMin == null || gMax == null ||
                            commute == null || budget == null
                        ) isMinProfileSet = false
                    }

                    data["minimumRequiredProfileSet"] = isMinProfileSet

                    db.collection("users").document(currentUser.uid)
                        .set(data)
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

        Spacer(modifier = Modifier.height(16.dp))
    }
}
