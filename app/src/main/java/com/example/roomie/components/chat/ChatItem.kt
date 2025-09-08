package com.example.roomie.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.roomie.components.formatTimestamp
import com.google.firebase.Timestamp

@Composable
fun ChatItem(
    name: String,
    lastMessage: String,
    time: Timestamp?,
    onClick: () -> Unit,
    isGroup: Boolean = false,
    groupParticipants: String? = null, // potentially could list participants
    lastMessenger: String? = null, // to show who sent latest message
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = if (isGroup) {
            CardDefaults.cardColors(
                MaterialTheme.colorScheme.outline,
                MaterialTheme.colorScheme.outlineVariant
            )
        } else {
            CardDefaults.cardColors(
                MaterialTheme.colorScheme.secondaryContainer,
                MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }

    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            if (!isGroup) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main content column (name + lastMessage)
            Column(
                modifier = Modifier.weight(1f) // take all remaining space except date column
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, // allow wrapping for long names
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (isGroup) {
                        "${lastMessenger}: $lastMessage"
                    } else {
                        lastMessage
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date on its own column
            Text(
                text = time?.let { formatTimestamp(it) } ?: "",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .wrapContentWidth(Alignment.End)
            )
        }
    }
}
