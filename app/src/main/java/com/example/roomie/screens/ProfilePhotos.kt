package com.example.roomie.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.addPhotoToUser
import com.example.roomie.components.deletePhoto
import com.example.roomie.components.fetchUserPhotos
import com.example.roomie.components.uploadProfileImage
import com.example.roomie.ui.theme.FontSize
import com.example.roomie.ui.theme.MontserratFontFamily
import com.example.roomie.ui.theme.ZainFontFamily
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.launch

// contains a composable for editing the profile photos and one for viewing the profile photos.

@Composable
fun ProfilePhotosEdit(
    modifier: Modifier = Modifier,
    onPhotosChanged: (List<PhotoItem>) -> Unit = {},
    maxPhotos: Int = 5
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val scope = rememberCoroutineScope()

    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var showConfirmDeleteForPath by remember { mutableStateOf<String?>(null) }

    // initial load
    LaunchedEffect(uid) {
        isLoading = true
        photos = try { fetchUserPhotos(uid) } catch (_: Exception) { emptyList() }
        isLoading = false
        onPhotosChanged(photos)
    }

    // Image picker (single image)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        if (photos.size >= maxPhotos) return@rememberLauncherForActivityResult
        scope.launch {
            isLoading = true
            try {
                val (url, path) = uploadProfileImage(uri, uid)

                // Ensure user doc exists
                val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                userRef.get().addOnSuccessListener { doc ->
                    if (!doc.exists()) {
                        userRef.set(mapOf("photos" to emptyList<Map<String, String>>()), SetOptions.merge())
                    }
                }

                // Add photo to Firestore array
                val added = addPhotoToUser(uid, url, path)

                if (added) {
                    photos = fetchUserPhotos(uid)
                    onPhotosChanged(photos)
                }
            } finally {
                isLoading = false
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Photos",
            fontFamily = MontserratFontFamily,
            fontSize = FontSize.subHeader,
            color = MaterialTheme.colorScheme.inverseSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Show existing photos
            photos.forEach { p ->
                Box(modifier = Modifier.size(100.dp)) {
                    AsyncImage(
                        model = p.url,
                        contentDescription = "profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { showConfirmDeleteForPath = p.path },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Delete photo")
                    }
                }
            }

            // Add button (only if less than max)
            if (photos.size < maxPhotos) {
                Card(
                    modifier = Modifier
                        .size(100.dp)
                        .clickable { picker.launch("image/*") },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Add, contentDescription = "Add photo")
                    }
                }
            }
        }
    }

    // Delete confirmation
    if (showConfirmDeleteForPath != null) {
        AlertDialog(
            onDismissRequest = { showConfirmDeleteForPath = null },
            confirmButton = {
                TextButton(onClick = {
                    val pathToDelete = showConfirmDeleteForPath!!
                    showConfirmDeleteForPath = null
                    scope.launch {
                        isLoading = true
                        try {
                            val ok = deletePhoto(uid, pathToDelete)
                            if (ok) photos = fetchUserPhotos(uid)
                            onPhotosChanged(photos)
                        } finally { isLoading = false }
                    }
                }) {
                    Text(
                        "Delete",
                        fontFamily = ZainFontFamily,
                        color = MaterialTheme.colorScheme.inverseSurface,
                        fontSize = FontSize.body
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDeleteForPath = null }) {
                        Text(
                            "Cancel",
                            fontFamily = ZainFontFamily,
                            color = MaterialTheme.colorScheme.inverseSurface,
                            fontSize = FontSize.body
                        )
                }
            },
            text = {
                Text(
                    text = "Delete this photo?",
                    style = TextStyle(
                        fontFamily = MontserratFontFamily,
                        fontSize = FontSize.body,
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.surfaceBright,
            textContentColor = MaterialTheme.colorScheme.surfaceBright,
            shape = RoundedCornerShape(40.dp)
        )
    }
}

@Composable
fun ProfilePhotoGallery(
    photos: List<String>,
    modifier: Modifier = Modifier,
    pageHeight: Dp = 250.dp
) {
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
            itemsIndexed(photos) { _, url ->
                AsyncImage(
                    model = url,
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillParentMaxWidth()
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
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
}
