package com.example.roomie.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.roomie.screens.ProfilePhotoGallery
import com.example.roomie.screens.ProfilePictureDisplay
import com.example.roomie.R

@Composable
fun ProfileCard(
    photos: List<String>,
    name: String?,
    age: Int?,
    profilePictureUrl: String?,
    // icon-row fields
    birthday: Int?,                  // age
    pets: String?,                   // "Yes"/"No" or text
    bedtime: String?,
    smokingStatus: String?,          // "Smoke"/"Vape"/"Neither"
    groupMin: Int?,
    groupMax: Int?,
    maxCommute: Int?,
    maxBudget: Int?,
    university: String?,
    // rest of the content
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
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header: PFP + name + age
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
                    birthday?.let {
                        Text(
                            text = it.toString(),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Gallery
            if (photos.isNotEmpty()) {
                ProfilePhotoGallery(
                    photos = photos,
                    modifier = Modifier.fillMaxWidth(),
                    pageHeight = 250.dp
                )
            }

            // ---------- Icon rows (under the gallery) ----------
            // row 1: birthday, pets, bedtime, smoking
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                birthday?.let { IconWithLabel(R.drawable.ic_birthday, it.toString()) }
                pets?.takeIf { it.isNotBlank() }?.let { IconWithLabel(R.drawable.ic_pets, it) }
                bedtime?.takeIf { it.isNotBlank() }?.let { IconWithLabel(R.drawable.ic_bedtime, it) }
                smokingStatus?.takeIf { it.isNotBlank() }?.let { IconWithLabel(R.drawable.ic_smoking, if (it == "Neither") "No" else it) }
            }

            // row 2: group size (min-max), max commute, max budget
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (groupMin != null || groupMax != null) {
                    val gText = when {
                        groupMin != null && groupMax != null -> "$groupMin - $groupMax"
                        groupMin != null -> "Min $groupMin"
                        groupMax != null -> "Max $groupMax"
                        else -> null
                    }
                    gText?.let { IconWithLabel(R.drawable.ic_group_size, it) }
                }

                maxCommute?.let { IconWithLabel(R.drawable.ic_commute, "$it min") }
                maxBudget?.let { IconWithLabel(R.drawable.ic_budget, "${it}/wk") }
            }

            // row 3: university (single)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                university?.takeIf { it.isNotBlank() }?.let {
                    IconWithLabel(R.drawable.ic_university, it)
                }
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

            // --- Lifestyle Q&A (kept below) ---
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
private fun IconWithLabel(drawableId: Int, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(end = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = drawableId),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurface
            )
        )
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
