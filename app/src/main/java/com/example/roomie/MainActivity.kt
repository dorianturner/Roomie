package com.example.roomie

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.roomie.navigation.RoomieApp
import com.example.roomie.ui.theme.RoomieTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        setContent {
            RoomieTheme {
                RoomieApp()
            }
        }
    }
}
