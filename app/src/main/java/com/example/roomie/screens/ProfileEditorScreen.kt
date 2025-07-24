package com.example.roomie.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class) // For SegmentedButton and SingleChoiceSegmentedButtonRow
@Composable
fun ProfileEditorScreen(
    onProfileSaved: () -> Unit
) {
    // --- General Profile Fields ---
    var name by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }

    // --- Profile Type Selection ---
    var isLandlord by remember { mutableStateOf(false) } // true: Landlord, false: Student

    // --- Landlord Specific Fields ---
    // Store as String for TextField, convert to Int on save
    var landlordCompany by remember { mutableStateOf("") }

    // --- Student Specific Fields ---
    // Store as String for TextField, convert to Int on save
    var studentAgeInput by remember { mutableStateOf("") }
    var studentUniversity by remember { mutableStateOf("") }
    var studentBasicPreferences by remember { mutableStateOf("") }
    // Desired Group Size Range (min and max as Strings for TextFields)
    var studentDesiredGroupSizeMinInput by remember { mutableStateOf("") }
    var studentDesiredGroupSizeMaxInput by remember { mutableStateOf("") }
    // Store as String for TextField, convert to Int on save
    var studentMaxCommuteInput by remember { mutableStateOf("") }
    var studentMaxBudgetInput by remember { mutableStateOf("") }

    // --- Firebase and Context ---
    val context = LocalContext.current
    val auth: FirebaseAuth = remember { Firebase.auth }
    val db: FirebaseFirestore = remember { Firebase.firestore }

    // --- Scroll State for the Column ---
    val scrollState = rememberScrollState()

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
                            landlordCompany = doc.getString("landlordCompany").orEmpty()
                        } else {
                            studentAgeInput = doc.getLong("studentAge")?.toString().orEmpty()
                            studentUniversity = doc.getString("studentUniversity").orEmpty()
                            studentBasicPreferences = doc.getString("studentBasicPreferences").orEmpty()
                            val group = doc.get("studentDesiredGroupSize") as? List<*>
                            studentDesiredGroupSizeMinInput = (group?.getOrNull(0) as? Long)?.toString().orEmpty()
                            studentDesiredGroupSizeMaxInput = (group?.getOrNull(1) as? Long)?.toString().orEmpty()
                            studentMaxCommuteInput = doc.getLong("studentMaxCommute")?.toString().orEmpty()
                            studentMaxBudgetInput = doc.getLong("studentMaxBudget")?.toString().orEmpty()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to load profile: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState), // Apply vertical scrolling here
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text("Complete Your Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        // Profile Type Selection
        Text("I am a:", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            SegmentedButton(
                selected = !isLandlord,
                onClick = { isLandlord = false },
                shape = SegmentedButtonDefaults.baseShape
            ) {
                Text("Student")
            }

            SegmentedButton(
                selected = isLandlord,
                onClick = { isLandlord = true },
                shape = SegmentedButtonDefaults.baseShape
            ) {
                Text("Landlord")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))

        // --- Common Fields ---
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio (Tell us about yourself!)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text("Phone Number (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // --- Conditional Profile Fields ---
        if (isLandlord) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Landlord Profile", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = landlordCompany,
                    onValueChange = { landlordCompany = it },
                    label = { Text("Your Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else { // Student profile
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Student Profile", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = studentAgeInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            studentAgeInput = newValue
                        }
                    },
                    label = { Text("Your Age") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = studentUniversity,
                    onValueChange = { studentUniversity = it },
                    label = { Text("Your University") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = studentBasicPreferences,
                    onValueChange = { studentBasicPreferences = it },
                    label = { Text("Basic Preferences (e.g., quiet, social, clean)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                Spacer(modifier = Modifier.height(16.dp))
                // Desired Group Size (Range)
                Text("Desired Group Size (Min-Max):", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = studentDesiredGroupSizeMinInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                studentDesiredGroupSizeMinInput = newValue
                            }
                        },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    Text("-")
                    TextField(
                        value = studentDesiredGroupSizeMaxInput,
                        onValueChange = { newValue ->
                            if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                                studentDesiredGroupSizeMaxInput = newValue
                            }
                        },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = studentMaxCommuteInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            studentMaxCommuteInput = newValue
                        }
                    },
                    label = { Text("Max Commute to University (mins)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = studentMaxBudgetInput,
                    onValueChange = { newValue ->
                        if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                            studentMaxBudgetInput = newValue
                        }
                    },
                    label = { Text("Max Budget for Rent (£ / week)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // --- Save Button ---
        Button(
            onClick = {
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    // Create a mutable map for user profile data
                    val userProfileData = mutableMapOf<String, Any>(
                        "name" to name,
                        "bio" to bio,
                        "phoneNumber" to phoneNumber,
                        "profileType" to if (isLandlord) "landlord" else "student",
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    var isMinProfileSet = true // Flag for minimum required profile

                    if (isLandlord) {
                        userProfileData["landlordCompany"] = landlordCompany

                        // Check landlord minimum fields
                        if (name.isBlank() || landlordCompany.isBlank()) {
                            isMinProfileSet = false
                        }
                    } else { // Student profile
                        val age = studentAgeInput.toIntOrNull()
                        val desiredGroupSizeMin = studentDesiredGroupSizeMinInput.toIntOrNull()
                        val desiredGroupSizeMax = studentDesiredGroupSizeMaxInput.toIntOrNull()
                        val maxCommute = studentMaxCommuteInput.toIntOrNull()
                        val maxBudget = studentMaxBudgetInput.toIntOrNull()

                        userProfileData["studentUniversity"] = studentUniversity
                        userProfileData["studentBasicPreferences"] = studentBasicPreferences
                        userProfileData["studentAge"] = age ?: 0 // Store Int
                        // Store desiredGroupSize as List<Int?> (null if input is empty/invalid)
                        userProfileData["studentDesiredGroupSize"] = listOf(desiredGroupSizeMin, desiredGroupSizeMax)
                        userProfileData["studentMaxCommute"] = maxCommute ?: 0 // Store Int
                        userProfileData["studentMaxBudget"] = maxBudget ?: 0 // Store Int

                        // Check student minimum fields
                        if (name.isBlank() || age == null ||
                            studentUniversity.isBlank() || studentBasicPreferences.isBlank() ||
                            desiredGroupSizeMin == null || desiredGroupSizeMax == null ||
                            maxCommute == null || maxBudget == null
                        ) {
                            isMinProfileSet = false
                        }
                    }

                    userProfileData["minimumRequiredProfileSet"] = isMinProfileSet

                    // Save the user profile data to Firestore
                    db.collection("users").document(currentUser.uid)
                        .set(userProfileData) // Use .set() to create or overwrite the document
                        .addOnSuccessListener {
                            Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                            onProfileSaved() // Trigger navigation back
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                            println("Error saving profile: $e") // Log for debugging
                        }
                } else {
                    Toast.makeText(context, "No user logged in. Please log in first.", Toast.LENGTH_LONG).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Profile")
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}
