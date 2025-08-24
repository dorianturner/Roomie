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

@Composable
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavController) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    // load photos once (re-runs when uid changes)
    LaunchedEffect(uid) {
        if (uid == null) {
            photos = emptyList()
            isLoading = false
        } else {
            isLoading = true
            photos = try {
                fetchUserPhotos(uid)
            } catch (e: Exception) {
                emptyList()
            }
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

        // Avatar: show first uploaded photo if available, otherwise fallback drawable
        Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                if (photos.isNotEmpty()) {
                    AsyncImage(
                        model = photos[0].url,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.profile),
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(200.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // Gallery (pass the photo URLs)
        ProfilePhotoGallery(
            photos = photos.map { it.url },
            modifier = Modifier.fillMaxWidth()
        )

        // Third row: Name and age (placeholder — fetch & show real data if you want)
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraShort, Alignment.CenterHorizontally)
        ) {
            Text(
                text = "Name",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 25.sp
            )
            Text(
                text = "19",
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 25.sp
            )
        }
        // Fourth row: City and country (keep as you wish)
    }
}
