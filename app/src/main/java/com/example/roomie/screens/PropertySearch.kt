package com.example.roomie.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.roomie.components.chat.ChatItem
import com.example.roomie.components.chat.ChatManager
import com.example.roomie.components.chat.Conversation
import com.example.roomie.components.listings.ListingItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PropertySearchScreen(modifier: Modifier = Modifier) {

    val listings = remember { List(5) { 1 } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Listings") },
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(color = MaterialTheme.colorScheme.background),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(listings) { listing ->
                ListingItem(
                    address = "123 Justrene Street",
                    price = 200,
                    bedrooms = 4,
                    bathrooms = 4,
                    onClick = { }
                )
            }
        }
    }
    }
