package com.example.roomie.components.listings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar
import java.util.Date

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
            val date = Date()
            val calendar = Calendar.getInstance()
            calendar.time = date
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            val month = calendar.get(Calendar.MONTH) + 1
            val year = calendar.get(Calendar.YEAR)
            val dateString = "$day/$month/$year"

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
