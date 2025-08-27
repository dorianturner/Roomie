package com.example.roomie.components

import com.google.firebase.Firebase
import com.google.firebase.functions.functions

object FunctionsProvider {
    val instance = Firebase.functions.apply {
        useEmulator("10.0.2.2", 5001)
    }
}