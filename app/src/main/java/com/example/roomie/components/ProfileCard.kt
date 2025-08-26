package com.example.roomie.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.screens.ProfilePhotoGallery
import com.example.roomie.screens.ProfilePictureDisplay

@Composable
fun ProfileCard(
    photos: List<String>,
    name: String?,
    age: Int?,
    profilePictureUrl: String?,
    bio: String?,
    addicted: String?,
    petPeeve: String?,
    passionate: String?,
    idealNight: String?,
    alwaysClean: String?,
    listening: String?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Profile Picture + Name + Age header ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ProfilePictureDisplay(url = profilePictureUrl, size = 80.dp)

                Column {
                    Text(
                        text = name ?: "Unknown",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    age?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // --- Photo gallery ---
            if (photos.isNotEmpty()) {
                ProfilePhotoGallery(
                    photos = photos,
                    modifier = Modifier.fillMaxWidth(),
                    pageHeight = 250.dp
                )
            }

            // --- Bio ---
            bio?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // --- Lifestyle Q&A ---
            LifestyleSection("I am completely addicted to", addicted)
            LifestyleSection("My biggest pet peeve is", petPeeve)
            LifestyleSection("I am passionate about", passionate)
            LifestyleSection("My ideal night looks like", idealNight)
            LifestyleSection("The one thing I always clean first is", alwaysClean)
            LifestyleSection("You'll always catch me listening to", listening)
        }
    }
}


@Composable
private fun LifestyleSection(label: String, text: String?) {
    if (!text.isNullOrBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    }
}
