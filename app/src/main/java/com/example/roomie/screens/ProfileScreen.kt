package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.roomie.ui.theme.Spacing
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.ProfileCard
import com.example.roomie.components.fetchUserPhotos
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(uid) {
        if (uid == null) {
            photos = emptyList()
            profileData = null
            isLoading = false
        } else {
            isLoading = true
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()

            profileData = userDoc?.data

            photos = if (userDoc?.getString("profileType") != "landlord") {
                try { fetchUserPhotos(uid) } catch (_: Exception) { emptyList() }
            } else emptyList()

            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = Spacing.short, vertical = Spacing.extraShort),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.short)
        ) {
            // Header row with title + edit button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Profile",
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Left,
                    fontSize = 40.sp,
                )

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = { navController.navigate("profile_editor") },
                    modifier = Modifier.padding(Spacing.extraShort)
                ) {
                    Text("Edit")
                }
            }

            // Profile card itself
            profileData?.let { data ->
                ProfileCard(
                    photos = photos.map { it.url },
                    name = data["name"] as? String,
                    profilePictureUrl = data["profilePictureUrl"] as? String,

                    // icon-row fields
                    age = (data["studentAge"] as? Long)?.toInt(),
                    pets = data["studentPet"] as? String,
                    bedtime = data["studentBedtime"] as? String,
                    smokingStatus = data["studentSmokingStatus"] as? String,

                    // group size stored as list [min, max]
                    groupMin = ((data["studentDesiredGroupSize"] as? List<*>)?.getOrNull(0) as? Long)?.toInt(),
                    groupMax = ((data["studentDesiredGroupSize"] as? List<*>)?.getOrNull(1) as? Long)?.toInt(),

                    maxCommute = (data["studentMaxCommute"] as? Long)?.toInt(),
                    maxBudget = (data["studentMaxBudget"] as? Long)?.toInt(),
                    university = data["studentUniversity"] as? String,

                    // rest
                    bio = data["bio"] as? String,
                    addicted = data["studentAddicted"] as? String,
                    petPeeve = data["studentPetPeeve"] as? String,
                    passionate = data["studentPassionate"] as? String,
                    idealNight = data["studentIdeal"] as? String,
                    alwaysClean = data["studentPet"] as? String,
                    listening = data["studentMusic"] as? String
                )
            } ?: run {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Profile not found", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
