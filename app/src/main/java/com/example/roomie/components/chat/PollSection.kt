package com.example.roomie.components.chat

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A composable function that displays a poll, allowing users to vote.
 * It shows the poll question and provides voting buttons if the poll is open.
 * If the poll is closed, it displays the poll's resolution.
 *
 * @param poll The [Poll] object containing the details of the poll.
 * @param userId The ID of the current user, used to determine their current vote.
 * @param onVote A lambda function to be invoked when the user casts a vote. It takes the vote option (e.g., "yes", "no", "undecided") as a String.
 */
@Composable
fun PollSection(
    poll: Poll,
    userId: String,
    onVote: (String) -> Unit
) {
    val currentVote = poll.votes[userId] ?: "undecided"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = poll.question,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .align(Alignment.CenterHorizontally),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }


        if (!poll.closed) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                VoteButton(
                    label = "No",
                    isSelected = currentVote == "no",
                    onClick = { onVote("no") },
                    backgroundColor = Color(0xFFE57373), // res
                    icon = { Icon(Icons.Default.Clear, contentDescription = "No", tint = Color.White) },
                    modifier = Modifier.weight(1f)
                )

                VoteButton(
                    label = "Undecided",
                    isSelected = currentVote == "undecided",
                    onClick = { onVote("undecided") },
                    backgroundColor = Color(0xFFB0BEC5), // grey
                    icon = { Icon(Icons.AutoMirrored.Filled.Help, contentDescription = "Undecided", tint = Color.White) },
                    modifier = Modifier.weight(1f)
                )

                VoteButton(
                    label = "Yes",
                    isSelected = currentVote == "yes",
                    onClick = { onVote("yes") },
                    backgroundColor = Color(0xFF81C784), // green
                    icon = { Icon(Icons.Default.Check, contentDescription = "Yes", tint = Color.White) },
                    modifier = Modifier.weight(1f)
                )
            }
        } else {
            Text(
                text = "Resolution: ${poll.resolution}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/**
 * A composable function that represents a button for voting in a poll.
 * It displays an icon and a label, and its appearance changes based on whether it's selected.
 *
 * @param label The text label for the button (e.g., "Yes", "No").
 * @param isSelected A boolean indicating whether this vote option is currently selected by the user.
 * @param onClick A lambda function to be invoked when the button is clicked.
 * @param backgroundColor The background color of the button.
 * @param icon A composable lambda that renders the icon for the button.
 * @param modifier A [Modifier] for this composable. Defaults to [Modifier].
 */
@Composable
fun VoteButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    backgroundColor: Color,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (isSelected) 3.dp else 0.dp
    val borderColor = if (isSelected) Color.Black else Color.DarkGray

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
            .padding(horizontal = 6.dp)
            .border(borderWidth, borderColor, RoundedCornerShape(12.dp)),
        onClick = onClick
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            icon()
            Text(
                text = label,
                color = Color.White,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
