package com.example.roomie.components.listings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.roomie.components.*
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

// contains a composable for editing the profile photos and one for viewing the profile photos.

@Composable
fun ListingPhotosEdit(
    listingId: String,
    modifier: Modifier = Modifier,
    maxPhotos: Int = 10,
    onPhotosChanged: (List<PhotoItem>) -> Unit = {}
) {
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                try {
                    val (url, path) = uploadListingImage(it, listingId)
                    val newPhoto = PhotoItem(url, path)
                    photos = photos + newPhoto
                    onPhotosChanged(photos)

                    FirebaseFirestore.getInstance()
                        .collection("listings")
                        .document(listingId)
                        .update("photos", photos.map { p -> mapOf("url" to p.url, "path" to p.path) })
                        .await()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // load photos initially
    LaunchedEffect(listingId) {
        photos = fetchListingPhotos(listingId)
        onPhotosChanged(photos)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                Box(modifier = Modifier.size(120.dp)) {
                    AsyncImage(
                        model = photo.url,
                        contentDescription = "Listing photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = deleteListingPhoto(listingId, photo.path)
                                if (success) {
                                    photos = photos.filterNot { it.path == photo.path }
                                    onPhotosChanged(photos)
                                } else {
                                    Toast.makeText(context, "Failed to delete photo", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete photo",
                            tint = Color.White,
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        )
                    }
                }
            }

            if (photos.size < maxPhotos) {
                item {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add photo")
                    }
                }
            }
        }
    }
}

@Composable
fun ListingPhotoGallery(
    photos: List<PhotoItem>,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<String?>(null) }

    if (photos.isNotEmpty()) {
        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(photos) { photo ->
                AsyncImage(
                    model = photo.url,
                    contentDescription = "Listing photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedPhoto = photo.url
                            showDialog = true
                        }
                )
            }
        }
    }

    if (showDialog && selectedPhoto != null) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { showDialog = false },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = selectedPhoto,
                    contentDescription = "Full screen listing photo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
    }
}