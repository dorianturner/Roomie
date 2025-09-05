package com.example.roomie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigation.NavController
import com.example.roomie.components.listings.ListingData
import com.example.roomie.components.listings.saveListing
import com.example.roomie.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditListingScreen(
    modifier: Modifier = Modifier,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var title by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var rentText by remember { mutableStateOf("") }
    var bedroomsText by remember { mutableStateOf("") }
    var bathroomsText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    //var photosText by remember { mutableStateOf("") } // comma-separated URLs for now
    var isSaving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add listing") },
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

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )

//                OutlinedTextField(
//                    value = photosText,
//                    onValueChange = { photosText = it },
//                    label = { Text("Photos (comma-separated URLs) — optional") },
//                    modifier = Modifier.fillMaxWidth()
//                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        val rent = rentText.toIntOrNull()
                        val bedrooms = bedroomsText.toIntOrNull()
                        val bathrooms = bathroomsText.toIntOrNull()
                        //val photos = photosText.split(",").map { it.trim() }.filter { it.isNotBlank() }

                        val listing = ListingData(
                            title = title.trim(),
                            description = description.trim().ifEmpty { null },
                            address = address.trim(),
                            rent = rent,
                            bedrooms = bedrooms,
                            bathrooms = bathrooms,
                            //photos = photos
                        )

                        scope.launch {
                            isSaving = true
                            message = null
                            val ok = try {
                                saveListing(listing)
                            } catch (e: Exception) {
                                false
                            }
                            isSaving = false
                            if (ok) {
                                // navigate back (or to listing details)
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
                        Text("Publish listing")
                    }
                }

                message?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    )
}