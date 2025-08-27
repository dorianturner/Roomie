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
import com.example.roomie.components.StudentProfile
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
    var isLandlord by remember {mutableStateOf(false)}
    var profileData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var name by remember {mutableStateOf("")}
    var companyName by remember {mutableStateOf("")}
    var pfpUrl by remember {mutableStateOf("")}

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
            isLandlord = userDoc?.getString("profileType") == "landlord"
            name = userDoc?.getString("name") ?: "Unknown User"
            companyName = userDoc?.getString("landlordCompany") ?: "companyless"
            pfpUrl = userDoc?.getString("profilePictureUrl") ?: ""
            photos = if (!isLandlord) {
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

            if (isLandlord) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = Spacing.short),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.short)
                ) {
                    // Smaller profile pic, left aligned
                    ProfilePictureDisplay(url = pfpUrl, size = 80.dp)

                    // Name + age to the right
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = companyName,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            } else {
                // Profile card itself
                profileData?.let { data ->
                    ProfileCard(
                        StudentProfile(
                            id = uid ?: "",
                            name = name,
                            photos = photos.map { it.url },
                            studentAge = (data["studentAge"] as? Long)?.toInt(),
                            profilePictureUrl = pfpUrl,
                            studentPet = data["studentPet"] as? String,
                            studentBedtime = data["studentBedtime"] as? Int,
                            studentAlcohol = data["studentAlcohol"] as? Int,
                            studentSmokingStatus = data["studentSmokingStatus"] as? String,
                            groupMin = data["groupMin"] as? Int,
                            groupMax = data["groupMax"] as? Int,
                            studentMaxCommute = data["studentMaxCommute"] as? Int,
                            studentMaxBudget = data["studentMaxBudget"] as? Int,
                            studentUniversity = data["studentUniversity"] as? String,
                            bio = data["bio"] as? String,
                            studentAddicted = data["studentAddicted"] as? String,
                            studentPetPeeve = data["studentPetPeeve"] as? String,
                            passionate = data["studentPassionate"] as? String,
                            studentIdeal = data["studentIdeal"] as? String,
                            studentMusic = data["studentMusic"] as? String,
                        )
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
}
