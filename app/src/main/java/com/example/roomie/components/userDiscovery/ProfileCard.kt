package com.example.roomie.components.userDiscovery

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.roomie.R
import com.example.roomie.components.GroupProfile
import com.example.roomie.screens.ProfileChip
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily
import com.example.roomie.ui.theme.Spacing
import com.example.roomie.ui.theme.ZainFontFamily
import kotlin.math.roundToInt

@Composable
fun ProfileCard(group: GroupProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        if (group.stats.size == 1) {
            IndividualProfileCard(group)
        } else {
            GroupProfileCard(group)
        }
    }
}

@Composable
fun IndividualProfileCard(group: GroupProfile) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {

            ProfilePictureDisplay(group.members.first().profilePictureUrl, true, size = 100.dp)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = group.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = FontSize.header
                    )
                )
            }
        }

        Spacer(Modifier.height(Spacing.slightlyShort))

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.surface
        )

        Spacer(Modifier.height(Spacing.slightlyShort))

        // Individual Stats
        Column(
            modifier = Modifier.padding(4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraShort)
            ) {
                ProfileSegment(Icons.Default.Cake, "Age: ${group.stats.avgAge?.roundToInt() ?: 0}")
                ProfileSegment(Icons.Default.AttachMoney, "Budget: $${group.stats.avgBudget?.roundToInt() ?: 0}")
            }
            ProfileSegment(Icons.Default.School, "School: ${group.stats.universities.first()}")
            ProfileSegment(Icons.Default.DirectionsCar, "Max Commute Time: ${group.stats.avgCommute?.roundToInt() ?: 0} mins")
        }

        Spacer(Modifier.height(Spacing.slightlyShort))

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.surface
        )

        Spacer(Modifier.height(Spacing.slightlyShort))

        AboutMeSection(group.members.first().bio?: "")
    }
}

@Composable
fun GroupProfileCard(group: GroupProfile) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
        ) {

            ProfilePictureDisplay(group.profilePicture, false, size = 100.dp)

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 30.sp
                    )
                )
            }
        }
    }
}

@Composable
fun ProfilePictureDisplay(
    url: String?,
    isIndividual: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
) {

    val statusIcon: ImageVector = if (isIndividual) {
        Icons.Default.Person
    } else {
        Icons.Default.Groups
    }


    Box(modifier = modifier.size(size)) {
        if (!url.isNullOrEmpty()) {
            AsyncImage(
                model = url,
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(R.drawable.profile),
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        // Bottom left icon overlay
        Icon(
            imageVector = statusIcon,
            contentDescription = "Individual or Group",
            tint = Color.Yellow,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(size / 3)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = CircleShape
                )
                .padding(4.dp)
        )
    }
}

@Composable
fun ProfileSegment(icon: ImageVector, text: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        tonalElevation = 2.dp
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp).padding(end = 6.dp)
            )
            Text(
                text = text,
                color = MaterialTheme.colorScheme.inverseSurface,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun AboutMeSection(
    bio: String,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraShort)
    ) {
        Text(
            text = "About Me:",
            color = MaterialTheme.colorScheme.inverseSurface,
            fontFamily = MontserratFontFamily,
            fontSize = FontSize.subHeader
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scroll)
                .padding(Spacing.extremelyShort)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Text(
                text = bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.inverseSurface,
                fontFamily = ZainFontFamily,
                fontSize = FontSize.body,
                modifier = Modifier.padding(Spacing.extraShort)
            )
        }
    }
}
