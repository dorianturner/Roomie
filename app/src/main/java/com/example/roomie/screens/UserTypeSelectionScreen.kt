package com.example.roomie.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun UserTypeSelectionScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = { navController.navigate("signup_student") }) {
            Text("I'm a Student")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { navController.navigate("signup_landlord") }) {
            Text("I'm a Landlord")
        }
    }
}
