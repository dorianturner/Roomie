package com.example.roomie.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.roomie.ui.theme.Spacing
import com.example.roomie.R
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.fetchUserPhotos
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var profilePictureUrl by remember { mutableStateOf<String?>(null) }
    var isLandlord by remember { mutableStateOf(false) }
    var name by remember {mutableStateOf<String?>(null)}
    var age by remember { mutableStateOf<Int?>(null) }

    // load photos once (re-runs when uid changes)
    LaunchedEffect(uid) {
        if (uid == null) {
            photos = emptyList()
            profilePictureUrl = null
            isLandlord = false
            isLoading = false
        } else {
            isLoading = true
            val userDoc = FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .await()
            isLandlord = userDoc?.getString("profileType") == "landlord"
            name = userDoc?.getString("name")
            if (!isLandlord) age = userDoc?.getLong("studentAge")?.toInt()

            // load profile picture separately
            profilePictureUrl = userDoc?.getString("profilePictureUrl")

            // load photos only if student
            photos = if (!isLandlord) {
                try { fetchUserPhotos(uid) } catch (e: Exception) { emptyList() }
            } else emptyList()

            isLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = Spacing.short, vertical = Spacing.extraShort),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.short)
    ) {
        // First row: Profile title text and edit button
        Row(
            modifier = Modifier
                .fillMaxWidth(),
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
                modifier = Modifier
                    .padding(Spacing.extraShort)
            ) {
                Text("Edit")
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Spacing.short),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.short)
        ) {
            // Smaller profile pic, left aligned
            ProfilePictureDisplay(url = profilePictureUrl, size = 80.dp)

            // Name + age to the right
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = name ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onBackground
                )
                if (age != null){
                    Text(
                        text = age.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 18.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }

        // Gallery (pass the photo URLs)
        if (!isLandlord){
            ProfilePhotoGallery(
                photos = photos.map { it.url },
                modifier = Modifier.fillMaxWidth()
            )
        }


        // Third row: Name and age (placeholder — fetch & show real data if you want)

        // Fourth row: City and country (keep as you wish)
    }
}
