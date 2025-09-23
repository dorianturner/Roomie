package com.example.roomie.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import com.example.roomie.ui.theme.Spacing
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * This Composable represents the initial splash/authentication screen where users can log in or create an account.
 *
 * @param onLoginSuccess Callback to be invoked upon successful login, typically navigates to the main content.
 * @param onCreateAccountSuccess Callback to be invoked upon successful account creation, typically navigates to profile creation.
 */

@Composable
fun SplashScreen(
    onLoginSuccess: () -> Unit,
    onCreateAccountSuccess: () -> Unit,
) {
    val auth: FirebaseAuth = Firebase.auth
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showLoginFields by remember { mutableStateOf(false) }
    var showCreateAccountFields by remember { mutableStateOf(false) }
    var showVerificationScreen by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var lastCharTime by remember { mutableLongStateOf(0L) }
    val coroutineScope = rememberCoroutineScope()

    val visualTransformation = remember(password, passwordVisible) {
        if (passwordVisible) VisualTransformation.None
        else LastCharVisibleTransformation(lastCharTime)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
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
            Spacer(modifier = Modifier.height(Spacing.short))
            Button(onClick = { showCreateAccountFields = true }) {
                Text("Create Account")
            }
        } else {
            // Input fields for email and password
            TextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.long)
            )
            Spacer(modifier = Modifier.height(Spacing.extraShort))
            TextField(
                value = password,
                onValueChange = { new ->
                    password = new
                    lastCharTime = System.currentTimeMillis()

                    // launch coroutine to hide last char after 1 second
                    coroutineScope.launch {
                        delay(1000)
                        lastCharTime = 0L
                    }
                },
                label = { Text("Password") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.long),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = visualTransformation,
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                    }
                }
            )
            Spacer(modifier = Modifier.height(Spacing.short))

            if (showLoginFields) {
                Button(onClick = {
                    performLogin(auth, context, email, password) { success ->
                        if (!success) return@performLogin

                        // only proceed to login if the account is fully set up
                        // otherwise jump to account setup

                        val user = auth.currentUser
                        if (user == null) return@performLogin // should never trigger
                        Firebase.firestore.collection("users").document(user.uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.getBoolean("minimumRequiredProfileSet") == true) {
                                    onLoginSuccess()
                                } else {
                                    onCreateAccountSuccess()
                                }
                            }
                            .addOnFailureListener {
                                onCreateAccountSuccess() // fallback
                            }
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
            Spacer(modifier = Modifier.height(Spacing.extraShort))
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

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Please verify your email.\n A verification link was sent to your inbox.",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.short))

        Button(onClick = {
            auth.currentUser?.reload()?.addOnCompleteListener { task ->
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

        Spacer(modifier = Modifier.height(Spacing.extraShort))

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

class LastCharVisibleTransformation(
    private val lastCharTime: Long
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (text.isEmpty()) return TransformedText(AnnotatedString(""), OffsetMapping.Identity)

        val currentTime = System.currentTimeMillis()
        val showLast = lastCharTime != 0L && currentTime - lastCharTime < 1000

        val masked = buildString {
            repeat(text.length - if (showLast) 1 else 0) { append('\u2022') } // bullet char
            if (showLast) append(text.last())
        }

        return TransformedText(AnnotatedString(masked), OffsetMapping.Identity)
    }
}
