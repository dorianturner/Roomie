package com.example.roomie.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Firebase
import com.google.firebase.auth.auth


/**
 * This Composable represents the initial splash/authentication screen where users can log in or create an account.
 *
 * @param onLoginSuccess Callback to be invoked upon successful login, typically navigates to the main content.
 * @param onCreateAccountSuccess Callback to be invoked upon successful account creation, typically navigates to profile creation.
 */

@Composable
fun SplashScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccountSuccess: () -> Unit
) {
    val auth: FirebaseAuth = Firebase.auth
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showLoginFields by remember { mutableStateOf(false) }
    var showCreateAccountFields by remember { mutableStateOf(false) }
    var showVerificationScreen by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (showVerificationScreen) {
            WaitForEmailVerificationScreen(auth = auth, onVerified = {
                // What to do after verification? E.g. navigate or show login
                onCreateAccountSuccess()
                showVerificationScreen = false
            })
        } else if (!showLoginFields && !showCreateAccountFields) {
            // Initial splash screen buttons
            Button(onClick = { showLoginFields = true }) {
                Text("Login")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { showCreateAccountFields = true }) {
                Text("Create Account")
            }
        } else {
            // Input fields for email and password
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (showLoginFields) {
                Button(onClick = {
                    performLogin(auth, context, email, password) { success ->
                        if (success) onLoginSuccess()
                    }
                }) {
                    Text("Perform Login")
                }
            } else if (showCreateAccountFields) {
                Button(onClick = {
                    performCreateAccount(auth, context, email, password) { success ->
                        if (success) {
                            showCreateAccountFields = false
                            showVerificationScreen = true
                        }
                    }
                }) {
                    Text("Create Account Now")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                // Reset state to go back to initial splash buttons
                showLoginFields = false
                showCreateAccountFields = false
                email = ""
                password = ""
            }) {
                Text("Back")
            }
        }
    }
}

@Composable
fun WaitForEmailVerificationScreen(
    auth: FirebaseAuth,
    onVerified: () -> Unit
) {
    val context = LocalContext.current
    var isChecking by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Please verify your email.\n A verification link was sent to your inbox.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            isChecking = true
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
                isChecking = false
                if (task.isSuccessful && auth.currentUser?.isEmailVerified == true) {
                    Toast.makeText(context, "Email verified!", Toast.LENGTH_SHORT).show()
                    onVerified()
                } else {
                    Toast.makeText(context, "Still not verified. Try again later.", Toast.LENGTH_SHORT).show()
                }
            }
        }) {
            Text("Click here once verified")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            auth.currentUser?.sendEmailVerification()?.addOnCompleteListener { resendTask ->
                if (resendTask.isSuccessful) {
                    Toast.makeText(context, "Verification email resent.", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Failed to resend: ${resendTask.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
        }) {
            Text("Resend Email")
        }
    }
}

// --- Authentication Logic Functions (can be moved to a separate utility file if preferred) ---

fun performLogin(auth: FirebaseAuth, context: Context, email: String, password: String, onComplete: (Boolean) -> Unit) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
        onComplete(false)
        return
    }

    auth.signInWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(context, "Authentication Succeeded.", Toast.LENGTH_SHORT).show()
                onComplete(true)
            } else {
                Toast.makeText(context, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        }
}

fun performCreateAccount(auth: FirebaseAuth, context: Context, email: String, password: String, onComplete: (Boolean) -> Unit) {
    if (email.isBlank() || password.isBlank()) {
        Toast.makeText(context, "Email and password cannot be empty.", Toast.LENGTH_SHORT).show()
        onComplete(false)
        return
    }

    auth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.sendEmailVerification()
                    ?.addOnCompleteListener { verifyTask ->
                        if (verifyTask.isSuccessful) {
                            Toast.makeText(context, "Account created. Verification email sent.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "Account created, but failed to send verification email: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                onComplete(true)
            } else {
                val errorMessage = when (task.exception) {
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException -> "Password is too weak. Please use a stronger password."
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                    is com.google.firebase.auth.FirebaseAuthUserCollisionException -> "An account with this email already exists."
                    else -> "Account creation failed: ${task.exception?.message}"
                }
                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                onComplete(false)
            }
        }
}
