package com.example.roomie.components.listings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ListingItem(
    address: String,
    // displayImage: type?,
    price: Int,
    bedrooms: Int,
    bathrooms: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile picture
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                // image here?
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Main content column (name + lastMessage)
            Column(
                modifier = Modifier.weight(1f) // take all remaining space except date column
            ) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2, // allow wrapping for long names
                    overflow = TextOverflow.Ellipsis
                )
                // may want to add icons here instead of words
                Text(
                    text = "$$price per week | $bedrooms bedrooms | $bathrooms bathrooms",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}