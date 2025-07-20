package com.example.roomie.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.roomie.screens.SplashScreen
import com.example.roomie.screens.UserTypeSelectionScreen
import com.example.roomie.screens.StudentSignupScreen
import com.example.roomie.screens.LandlordSignupScreen

@Composable
fun RoomieApp() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "splash") {
        composable("splash") { SplashScreen(navController) }
        composable("user_type") { UserTypeSelectionScreen(navController) }
        composable("signup_student") { StudentSignupScreen(navController) }
        composable("signup_landlord") { LandlordSignupScreen(navController) }
    }
}
