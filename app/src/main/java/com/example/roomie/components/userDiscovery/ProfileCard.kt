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
import androidx.compose.material.icons.filled.Group
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.roomie.R
import com.example.roomie.components.GroupProfile
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily
import com.example.roomie.ui.theme.Spacing
import com.example.roomie.ui.theme.ZainFontFamily
import kotlin.math.roundToInt

/**
 * A composable function that displays a profile card for a group or individual.
 * The card shows the profile picture, name, relevant stats, and a bio.
 *
 * @param group The [GroupProfile] object containing the data to display.
 */
@Composable
fun ProfileCard(group: GroupProfile) {
    val isIndividual: Boolean = group.stats.size == 1
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(Spacing.short)
                .fillMaxSize(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.extremelyShort),
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                ProfilePictureDisplay(
                    group.members.first().profilePictureUrl,
                    isIndividual,
                    size = 90.dp)

                // Name Column
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = group.name,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
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
            if (isIndividual) {
                IndividualCardContent(group)
            } else {
                GroupCardContent(group)
            }

            Spacer(Modifier.height(Spacing.slightlyShort))

            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surface
            )

            Spacer(Modifier.height(Spacing.slightlyShort))

            AboutSection(group.members.first().bio?: "", isIndividual)
        }
    }
}


/**
 * A composable function that displays the content specific to an individual's profile card.
 * This includes age, budget, school, and max commute time.
 *
 * @param group The [GroupProfile] object containing the individual's data.
 */
@Composable
fun IndividualCardContent(group: GroupProfile) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraShort)
        ) {
            ProfileSegment(
                Icons.Default.Cake,
                "Age",
                "${group.stats.avgAge?.roundToInt() ?: 0}"
            )
            ProfileSegment(
                R.drawable.ic_budget,
                "Budget",
                "£${group.stats.avgBudget?.roundToInt() ?: 0}"
            )
        }
        ProfileSegment(
            Icons.Default.School,
            "School",
            group.stats.universities.first()
        )
        ProfileSegment(
            Icons.Default.DirectionsCar,
            "Max Commute Time",
            "${group.stats.avgCommute?.roundToInt() ?: 0} mins"
        )
    }
}

/**
 * A composable function that displays the content specific to a group's profile card.
 * This includes group size, average age, average budget, and max commute time.
 *
 * @param group The [GroupProfile] object containing the group's data.
 */
@Composable
fun GroupCardContent(group: GroupProfile) {
    Column(
        modifier = Modifier.padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraShort)
        ) {
            ProfileSegment(
                Icons.Default.Groups,
                "Group Size",
                group.stats.size.toString()
            )
            ProfileSegment(
                Icons.Default.Cake,
                "Avg. Age",
                "${group.stats.avgAge?.roundToInt() ?: 0}"
            )
        }
        ProfileSegment(
            R.drawable.ic_budget,
            "Avg. Budget",
            "£${group.stats.avgBudget?.roundToInt() ?: 0}"
        )
        ProfileSegment(
            Icons.Default.DirectionsCar,
            "Max Commute Time",
            "${group.stats.avgCommute?.roundToInt() ?: 0} mins"
        )
    }
}

/**
 * A composable function that displays a profile picture.
 * It handles loading the image from a URL or showing a default image if the URL is null or empty.
 * It also overlays an icon indicating whether the profile is for an individual or a group.
 *
 * @param url The URL of the profile picture. Can be null or empty.
 * @param isIndividual A boolean indicating if the profile is for an individual (true) or a group (false).
 * @param modifier Optional [Modifier] for this composable.
 * @param size The desired size of the profile picture.
 */
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
        Icons.Default.Group
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
            tint = MaterialTheme.colorScheme.surfaceTint,
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

/**
 * A composable function that displays a segment of profile information,
 * typically consisting of an icon, a category label, and a response value.
 *
 * @param icon The [ImageVector] to display for the segment.
 * @param category The category label for the information (e.g., "Age", "Budget").
 * @param response The actual value or response for the category.
 */
@Composable
fun ProfileSegment(icon: ImageVector, category: String, response: String) {
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
                contentDescription = null, // Decorative icon
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(25.dp).padding(end = 6.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.inverseSurface)
                    ) {
                        append("${category}: ")
                    }
                    withStyle(
                        style = SpanStyle(color = MaterialTheme.colorScheme.surfaceTint)
                    ) {
                        append(response)
                    }
                },
                fontFamily = ZainFontFamily,
                fontSize = FontSize.subHeader
            )
        }
    }
}

// overload to handle R.drawable
@Composable
fun ProfileSegment(iconRes: Int, category: String, response: String) {
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
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(25.dp)
                    .padding(end = 6.dp)
            )
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.inverseSurface)) {
                        append("$category: ")
                    }
                    withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.surfaceTint)) {
                        append(response)
                    }
                },
                fontFamily = ZainFontFamily,
                fontSize = FontSize.subHeader
            )
        }
    }
}

/**
 * A composable function that displays the "About Me" or "About Us" section of a profile.
 * It shows a title and the bio text in a scrollable box.
 *
 * @param bio The biography text to display.
 * @param isIndividual A boolean indicating if the profile is for an individual (true) or a group (false).
 * @param modifier Optional [Modifier] for this composable.
 */
@Composable
fun AboutSection(
    bio: String,
    isIndividual: Boolean,
    modifier: Modifier = Modifier
) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(Spacing.extraShort)
    ) {
        Text(
            text = if (isIndividual) "About Me:" else "About Us: ",
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
