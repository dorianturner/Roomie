package com.example.roomie.components.listings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.roomie.components.PhotoItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
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

    val isTempListing = listingId.startsWith("temp-")

    // Track uploading state per image
    var uploading by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            coroutineScope.launch {
                uploading = true
                try {
                    val (url, path) = uploadListingImage(it, listingId)
                    val newPhoto = PhotoItem(url, path)
                    photos = photos + newPhoto
                    onPhotosChanged(photos)

                    if (!isTempListing) {
                        FirebaseFirestore.getInstance()
                            .collection("listings")
                            .document(listingId)
                            .update("photos", photos.map { p -> mapOf("url" to p.url, "path" to p.path) })
                            .await()
                    }
                } catch (_: Exception) {
                    Toast.makeText(context, "Failed to upload image", Toast.LENGTH_SHORT).show()
                }
                uploading = false
            }
        }
    }

    LaunchedEffect(listingId) {
        if (!isTempListing) {
            photos = fetchListingPhotos(listingId)
            onPhotosChanged(photos)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Images:",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )

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
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { /* handled in Gallery */ }
                    )
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val success = if (isTempListing) true else deleteListingPhoto(listingId, photo.path)
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
                        if (uploading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Add, contentDescription = "Add photo")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ListingPhotoGallery(
    photos: List<PhotoItem>,
    modifier: Modifier = Modifier,
    pageHeight: Dp = 250.dp
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedPhoto by remember { mutableStateOf<String?>(null) }

    if (photos.isEmpty()) {
        Box(
            modifier = modifier
                .height(pageHeight)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("No photos yet")
        }
        return
    }

    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    Column(modifier = modifier) {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(pageHeight)
        ) {
            itemsIndexed(photos) { _, photo ->
                AsyncImage(
                    model = photo.url,
                    contentDescription = "Listing photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedPhoto = photo.url
                            showDialog = true
                        }
                )
            }
        }

        val currentPage by remember {
            derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, photos.lastIndex) }
        }

        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        ) {
            repeat(photos.size) { i ->
                val selected = i == currentPage
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (selected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                )
            }
        }
    }

    if (showDialog && selectedPhoto != null) {
        Dialog(onDismissRequest = { showDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { showDialog = false } // tap outside
                    },
                contentAlignment = Alignment.Center
            ) {
                var rawScale by remember { mutableFloatStateOf(1f) }
                var rawOffsetX by remember { mutableFloatStateOf(0f) }
                var rawOffsetY by remember { mutableFloatStateOf(0f) }

                val animatedScale by animateFloatAsState(targetValue = rawScale)
                val animatedOffsetX by animateFloatAsState(targetValue = rawOffsetX)
                val animatedOffsetY by animateFloatAsState(targetValue = rawOffsetY)

                val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
                    rawScale = (rawScale * zoomChange).coerceIn(1f, 5f)
                    rawOffsetX += panChange.x
                    rawOffsetY += panChange.y
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .fillMaxHeight(0.95f)
                        .transformable(state = transformableState)
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    // If no pointers are down, snap back
                                    if (event.changes.all { !it.pressed }) {
                                        rawScale = 1f
                                        rawOffsetX = 0f
                                        rawOffsetY = 0f
                                    }
                                }
                            }
                        }
                ) {
                    AsyncImage(
                        model = selectedPhoto,
                        contentDescription = "Full screen listing photo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = animatedScale,
                                scaleY = animatedScale,
                                translationX = animatedOffsetX,
                                translationY = animatedOffsetY
                            )
                    )
                }
            }
        }
    }
}
