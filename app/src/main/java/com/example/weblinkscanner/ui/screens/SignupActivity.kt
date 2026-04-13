package com.example.weblinkscanner.ui.screens

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.api.NewRetrofitClient
import com.example.weblinkscanner.data.models.SignupRequest
import com.example.weblinkscanner.data.models.SignupResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit = {},
    onNavigateToLogin: () -> Unit = {}
) {
    // --- State ---
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }

    var fullNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var signUpError by remember { mutableStateOf("") }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // --- Colors ---
    val blueAccent  = Color(0xFF2563EB)
    val lightBg     = Color(0xFFF2F4F8)
    val cardBg      = Color.White
    val textPrimary = Color(0xFF1A1A2E)
    val textHint    = Color(0xFF9E9E9E)
    val errorColor  = Color(0xFFE53935)

    // --- Validation ---
    fun validate(): Boolean {
        var valid = true

        fullNameError = when {
            fullName.isBlank() -> { valid = false; "Full name is required" }
            fullName.trim().length < 2 -> { valid = false; "Name must be at least 2 characters" }
            else -> ""
        }

        emailError = when {
            email.isBlank() -> { valid = false; "Email is required" }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                valid = false; "Enter a valid email address"
            }
            else -> ""
        }

        passwordError = when {
            password.isBlank() -> { valid = false; "Password is required" }
            password.length < 6 -> { valid = false; "Password must be at least 6 characters" }
            else -> ""
        }

        confirmPasswordError = when {
            confirmPassword.isBlank() -> { valid = false; "Please confirm your password" }
            confirmPassword != password -> { valid = false; "Passwords do not match" }
            else -> ""
        }

        return valid
    }

    // --- Sign Up Handler ---
    fun doSignUp() {
        Log.d("SIGNUP", "doSignUp() called")

        signUpError = ""
        fullNameError = ""
        emailError = ""
        passwordError = ""
        confirmPasswordError = ""

        if (!validate()) {
            Log.d("SIGNUP", "Validation failed")
            return
        }

        Log.d("SIGNUP", "Validation passed - calling API")
        isLoading = true

        val request = SignupRequest(
            name     = fullName.trim(),
            email    = email.trim(),
            password = password
        )

        Log.d("SIGNUP", "Request: $request")

        NewRetrofitClient.api.signup(request).enqueue(object : Callback<SignupResponse> {
            override fun onResponse(
                call: Call<SignupResponse>,
                response: Response<SignupResponse>
            ) {
                Log.d("SIGNUP", "Response code: ${response.code()}")
                Log.d("SIGNUP", "Response body: ${response.body()}")
                Log.d("SIGNUP", "Error body: ${response.errorBody()?.string()}")

                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    if (response.isSuccessful) {
                        Log.d("SIGNUP", "Sign up successful!")
                        showSuccessDialog = true  // ← show dialog first
                    } else {
                        signUpError = "Sign up failed (${response.code()}). Please try again."
                    }
                }
            }

            override fun onFailure(call: Call<SignupResponse>, t: Throwable) {
                Log.d("SIGNUP", "Network error: ${t.message}")
                Log.d("SIGNUP", "Cause: ${t.cause}")

                Handler(Looper.getMainLooper()).post {
                    isLoading = false
                    signUpError = "Network error: ${t.message ?: "Please check your connection"}"
                }
            }
        })
    }

    // --- Success Dialog ---
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { },
            icon = {
                Text(text = "✅", fontSize = 36.sp)
            },
            title = {
                Text(
                    text = "Account Created!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Your account has been created successfully. You can now log in.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = Color(0xFF555555),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        onSignUpSuccess()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Go to Login", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- UI ---
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = lightBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Header
            Text(
                text = "🔗 Weblink Scanner",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = blueAccent
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Create your account", fontSize = 14.sp, color = textHint)
            Spacer(modifier = Modifier.height(24.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // --- Full Name ---
                    Text("Full Name", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it; fullNameError = "" },
                        placeholder = { Text("John Doe", color = textHint) },
                        isError = fullNameError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = fieldColors(blueAccent, errorColor, textPrimary)
                    )
                    if (fullNameError.isNotEmpty()) {
                        Text(fullNameError, color = errorColor, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Email ---
                    Text("Email", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it; emailError = "" },
                        placeholder = { Text("you@example.com", color = textHint) },
                        isError = emailError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = fieldColors(blueAccent, errorColor, textPrimary)
                    )
                    if (emailError.isNotEmpty()) {
                        Text(emailError, color = errorColor, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Password ---
                    Text("Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it; passwordError = "" },
                        placeholder = { Text("Min. 6 characters", color = textHint) },
                        isError = passwordError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = textHint
                                )
                            }
                        },
                        colors = fieldColors(blueAccent, errorColor, textPrimary)
                    )
                    if (passwordError.isNotEmpty()) {
                        Text(passwordError, color = errorColor, fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // --- Confirm Password ---
                    Text("Confirm Password", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textPrimary)
                    Spacer(modifier = Modifier.height(2.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; confirmPasswordError = "" },
                        placeholder = { Text("Re-enter your password", color = textHint) },
                        isError = confirmPasswordError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPassword = !showConfirmPassword }) {
                                Icon(
                                    imageVector = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (showConfirmPassword) "Hide password" else "Show password",
                                    tint = textHint
                                )
                            }
                        },
                        colors = fieldColors(blueAccent, errorColor, textPrimary)
                    )
                    if (confirmPasswordError.isNotEmpty()) {
                        Text(confirmPasswordError, color = errorColor, fontSize = 12.sp)
                    }

                    // --- Global API Error ---
                    if (signUpError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = errorColor.copy(alpha = 0.1f))
                        ) {
                            Text(
                                text = signUpError,
                                color = errorColor,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Sign Up Button ---
                    Button(
                        onClick = { doSignUp() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = blueAccent,
                            disabledContainerColor = blueAccent.copy(alpha = 0.5f)
                        )
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.5.dp
                            )
                        } else {
                            Text(
                                "Sign Up",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            TextButton(onClick = onNavigateToLogin) {
                Text(
                    "Already have an account? Log In",
                    color = blueAccent,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// --- Helper ---
@Composable
private fun fieldColors(accent: Color, error: Color, text: Color) =
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = accent,
        unfocusedBorderColor = Color(0xFFE0E0E0),
        errorBorderColor     = error,
        focusedTextColor     = text,
        unfocusedTextColor   = text,
        cursorColor          = accent
    )