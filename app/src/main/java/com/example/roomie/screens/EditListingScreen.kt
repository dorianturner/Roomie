package com.example.roomie.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.roomie.components.PhotoItem
import com.example.roomie.components.listings.ListingData
import com.example.roomie.components.listings.ListingPhotosEdit
import com.example.roomie.components.listings.saveListing
import com.example.roomie.ui.theme.Spacing
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    modifier: Modifier = Modifier,
    listingId: String? = null,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser

    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("") }
    var bedroomsText by remember { mutableStateOf("") }
    var bathroomsText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<PhotoItem>>(emptyList()) }
    var availableFrom by remember { mutableStateOf<Long?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    val openDialog = remember { mutableStateOf(false) }

    val isNew = listingId == null

    // --- Load listing if editing ---
    LaunchedEffect(listingId) {
        if (!isNew) {
            try {
                val snapshot = db.collection("listings").document(listingId).get().await()
                val data = snapshot.toObject(ListingData::class.java)
                if (data != null) {
                    title = data.title
                    address = data.address
                    rentText = data.rent?.toString() ?: ""
                    bedroomsText = data.bedrooms?.toString() ?: ""
                    bathroomsText = data.bathrooms?.toString() ?: ""
                    description = data.description ?: ""
                    photos = data.photos
                    availableFrom = data.availableFromEpoch
                }
            } catch (e: Exception) {
                message = "Failed to load listing, ${e.message}"
            }
        }
    }

    val dateFormatter = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = availableFrom)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "Add Listing" else "Edit Listing") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        content = { innerPadding ->
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(innerPadding)
                    .padding(horizontal = Spacing.short, vertical = Spacing.short),
                verticalArrangement = Arrangement.spacedBy(Spacing.short)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rentText,
                    onValueChange = { rentText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Rent (weekly)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = bedroomsText,
                        onValueChange = { bedroomsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Bedrooms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bathroomsText,
                        onValueChange = { bathroomsText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Bathrooms") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(onClick = { openDialog.value = true }) {
                    Text(
                        text = availableFrom?.let { millis ->
                            "Available from: ${dateFormatter.format(java.util.Date(millis))}"
                        } ?: "Select available from date"
                    )
                }

                if (openDialog.value) {
                    DatePickerDialog(
                        onDismissRequest = { openDialog.value = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    openDialog.value = false
                                    availableFrom = dateState.selectedDateMillis
                                }
                            ) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { openDialog.value = false }) { Text("Cancel") }
                        }
                    ) {
                        DatePicker(state = dateState)
                    }
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

                ListingPhotosEdit(
                    listingId = listingId ?: "temp-${UUID.randomUUID()}",
                    onPhotosChanged = { updated -> photos = updated }
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val rent = rentText.toIntOrNull()
                        val bedrooms = bedroomsText.toIntOrNull()
                        val bathrooms = bathroomsText.toIntOrNull()

                        val listing = ListingData(
                            title = title.trim(),
                            description = description.trim().ifEmpty { null },
                            address = address.trim(),
                            rent = rent,
                            bedrooms = bedrooms,
                            bathrooms = bathrooms,
                            photos = photos,
                            availableFromEpoch = availableFrom
                        )

                        scope.launch {
                            isSaving = true
                            message = null
                            val ok = try {
                                if (isNew) {
                                    saveListing(listing)
                                } else {
                                    val ownerName = currentUser?.let {
                                        db.collection("users").document(it.uid).get().await().getString("name")
                                    }
                                    db.collection("listings").document(listingId).set(
                                        listing.toMap(currentUser?.uid ?: "", ownerName)
                                    ).await()
                                    true
                                }
                            } catch (_: Exception) {
                                false
                            }
                            isSaving = false
                            if (ok) {
                                navController.popBackStack()
                            } else {
                                message = "Failed to save listing. Check fields / network."
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Saving...")
                    } else {
                        Text(if (isNew) "Publish Listing" else "Save Changes")
                    }
                }


                // Delete listing button
                var showDeleteDialog by remember { mutableStateOf(false) }

                if (!isNew) {
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete Listing", color = MaterialTheme.colorScheme.onError)
                    }
                }

                // Confirmation Dialog
                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text("Confirm Deletion") },
                        text = { Text("Are you sure you want to delete this listing? This action cannot be undone.") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showDeleteDialog = false
                                    scope.launch {
                                        try {
                                            db.collection("listings").document(listingId!!).delete().await()
                                            kotlinx.coroutines.delay(500)
                                            navController.navigate("profile") {
                                                popUpTo("profile") { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            message = "Failed to delete listing, ${e.message}"
                                        }
                                    }
                                }
                            ) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text("Cancel")
                            }
                        }
                    )
                }

                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}
