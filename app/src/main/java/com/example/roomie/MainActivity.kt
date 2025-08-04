package com.example.roomie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.example.roomie.navigation.RoomieNavHost
import com.example.roomie.navigation.Routes
import com.example.roomie.ui.theme.RoomieTheme // Your app's theme
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase Auth
        auth = Firebase.auth

        // Determine the initial navigation destination based on authentication status
        val startDestination = if (auth.currentUser != null) {
            // User is already signed in, go directly to the main content
            Routes.MAIN_CONTENT
        } else {
            // No user signed in, go to the splash/authentication screen
            Routes.SPLASH_SCREEN
        }

        setContent {
            RoomieTheme {
                // A surface container using the 'background' color from the theme
                Surface {
                    val navController = rememberNavController()
                    // Set up your navigation host with the determined start destination
                    RoomieNavHost(navController = navController, startDestination = startDestination)
                }
            }
        }
    }

    // Optional: Lifecycle methods to manage authentication state
    // You can add onStart() if you want to re-check user status every time the activity starts,
    // which might be useful if the app was in the background for a long time.
    // For many apps, checking once in onCreate is sufficient for initial routing.

    /*
    public override fun onStart() {
        super.onStart()
        // Here you could re-evaluate auth.currentUser if you need dynamic redirection
        // based on external auth changes (e.g., user signed out from another device).
        // However, for typical app flow, the onCreate check and NavHost handles it well.
    }
    */
}
