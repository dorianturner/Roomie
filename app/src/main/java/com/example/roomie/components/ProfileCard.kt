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
import androidx.compose.ui.text.style.TextOverflow
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
//                    age?.let {
//                        Text(
//                            text = it.toString(),
//                            style = MaterialTheme.typography.bodyLarge.copy(
//                                color = MaterialTheme.colorScheme.onSurfaceVariant
//                            )
//                        )
//                    }
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Row 1: birthday, pets, bedtime, smoking
                    val row1 = listOfNotNull(
                        age?.let { R.drawable.ic_birthday to it.toString() },
                        pets?.takeIf { it.isNotBlank() }?.let { R.drawable.ic_pets to it },
                        bedtime?.takeIf { it.isNotBlank() }?.let { R.drawable.ic_bedtime to it },
                        smokingStatus?.takeIf { it.isNotBlank() }?.let {
                            R.drawable.ic_smoking to if (it == "Neither") "No" else it
                        }
                    )

                    if (row1.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row1.forEach { (drawable, label) ->
                                // each item is a compact unit that will wrap as a whole
                                IconWithLabelUnit(drawable, label)
                            }
                        }
                    }

                    // Row 2: group size, commute, budget
                    val gText = when {
                        groupMin != null && groupMax != null -> "$groupMin - $groupMax"
                        groupMin != null -> "Min $groupMin"
                        groupMax != null -> "Max $groupMax"
                        else -> null
                    }

                    val row2 = listOfNotNull(
                        gText?.let { R.drawable.ic_group_size to it },
                        maxCommute?.let { R.drawable.ic_commute to "$it min" },
                        maxBudget?.let { R.drawable.ic_budget to "${it}/wk" } // preserved format
                    )

                    if (row2.isNotEmpty()) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row2.forEach { (drawable, label) ->
                                IconWithLabelUnit(drawable, label)
                            }
                        }
                    }

                    // Row 3: university (single)
                    university?.takeIf { it.isNotBlank() }?.let { uni ->
                        // single item; keep it left-aligned but allow it to use whole width
                        Row(modifier = Modifier.fillMaxWidth()) {
                            IconWithLabelUnit(R.drawable.ic_university, uni, takeFullWidth = true)
                        }
                    }
                }
            } // end icon box

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
private fun IconWithLabelUnit(drawableId: Int, label: String, takeFullWidth: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = if (takeFullWidth) Modifier.fillMaxWidth() else Modifier.wrapContentWidth()
    ) {
        Icon(
            painter = painterResource(id = drawableId),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
