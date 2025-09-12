package com.example.roomie.components.listings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

@Composable
fun ListingDetailsContent(
    listing: Listing,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // title
        Text(
            text = listing.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // address
        Text(
            text = listing.address,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )

        // rent
        Text(
            text = "£${listing.rent} per week",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            ListingDetailItem(
                label = "Bedrooms",
                value = listing.bedrooms.toString()
            )
            ListingDetailItem(
                label = "Bathrooms",
                value = listing.bathrooms.toString()
            )
        }
        // available from date
        listing.availableFrom?.let { timestamp ->
            val date = Date(timestamp)
            val dateString = "${date.date}/${(date.month) + 1}/${date.year + 1900}"

            Text(
                text = "Available from: $dateString",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // description
        Text(
            text = "Description:",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = listing.description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )

        // current status (do we need this displayed?)
        Text(
            text = if (listing.isActive) "Active Listing" else "Not Available",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = if (listing.isActive) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
    }
}