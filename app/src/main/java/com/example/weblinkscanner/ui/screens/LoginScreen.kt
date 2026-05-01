package com.example.weblinkscanner.ui.screens

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.weblinkscanner.data.api.NewRetrofitClient
import com.example.weblinkscanner.data.models.ForgotPasswordRequest
import com.example.weblinkscanner.data.models.ForgotPasswordResponse
import com.example.weblinkscanner.data.models.LoginRequest
import com.example.weblinkscanner.data.models.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import androidx.compose.ui.platform.LocalContext
import com.example.weblinkscanner.utils.TokenManager

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LoginScreen(
                onLoginSuccess = { _, _, _, _, _, _ -> },
                onNavigateToSignUp = {}
            )
        }
    }
}

@Composable
fun LoginScreen(
    // name, email and plan are passed back so MenuScreen can display them
    onLoginSuccess: (name: String, email: String, plan: String, userId: String, token: String, role: String) -> Unit,
    onNavigateToSignUp: () -> Unit = {}
) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf("") }
    var isLoading    by remember { mutableStateOf(false) }
    var rememberMe   by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var showForgotDialog  by remember { mutableStateOf(false) }
    var forgotEmail       by remember { mutableStateOf("") }
    var forgotEmailError  by remember { mutableStateOf("") }
    var forgotIsLoading   by remember { mutableStateOf(false) }
    var showForgotSuccess by remember { mutableStateOf(false) }

    val blue        = Color(0xFF2563EB)
    val bgTop       = Color(0xFFEFF6FF)
    val bgBottom    = Color(0xFFF8FAFC)
    val textPrimary = Color(0xFF0F172A)
    val textMuted   = Color(0xFF64748B)
    val errorRed    = Color(0xFFDC2626)

    // --- Forgot Password Dialog ---
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!forgotIsLoading) { showForgotDialog = false; forgotEmail = ""; forgotEmailError = "" }
            },
            icon = { Text("🔑", fontSize = 32.sp) },
            title = {
                Text("Reset Password", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column {
                    Text("Enter your email and we'll send you a link to reset your password.",
                        fontSize = 14.sp, color = Color(0xFF64748B),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = forgotEmail,
                        onValueChange = { forgotEmail = it; forgotEmailError = "" },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = blue) },
                        isError = forgotEmailError.isNotEmpty(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = blue, unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedLabelColor = blue, focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary, errorBorderColor = errorRed, cursorColor = blue
                        )
                    )
                    if (forgotEmailError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(forgotEmailError, color = errorRed, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (forgotEmail.isBlank()) { forgotEmailError = "Please enter your email"; return@Button }
                        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(forgotEmail).matches()) {
                            forgotEmailError = "Enter a valid email address"; return@Button
                        }
                        forgotIsLoading = true
                        NewRetrofitClient.api.forgotPassword(ForgotPasswordRequest(forgotEmail.trim()))
                            .enqueue(object : Callback<ForgotPasswordResponse> {
                                override fun onResponse(call: Call<ForgotPasswordResponse>, response: Response<ForgotPasswordResponse>) {
                                    Handler(Looper.getMainLooper()).post {
                                        forgotIsLoading = false; showForgotDialog = false
                                        forgotEmail = ""; showForgotSuccess = true
                                    }
                                }
                                override fun onFailure(call: Call<ForgotPasswordResponse>, t: Throwable) {
                                    Handler(Looper.getMainLooper()).post {
                                        forgotIsLoading = false
                                        forgotEmailError = "Network error. Please try again."
                                    }
                                }
                            })
                    },
                    enabled = !forgotIsLoading,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue,
                        disabledContainerColor = blue.copy(alpha = 0.6f))
                ) {
                    if (forgotIsLoading) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Text("Send Reset Link", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false; forgotEmail = ""; forgotEmailError = "" },
                    enabled = !forgotIsLoading) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // --- Forgot Password Success Dialog ---
    if (showForgotSuccess) {
        AlertDialog(
            onDismissRequest = { showForgotSuccess = false },
            icon = { Text("📧", fontSize = 32.sp) },
            title = {
                Text("Check Your Email", fontWeight = FontWeight.Bold, fontSize = 20.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Text("A password reset link has been sent to your email.",
                    fontSize = 14.sp, color = Color(0xFF64748B),
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = { showForgotSuccess = false },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue)) {
                    Text("Got it", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(bgTop, bgBottom)))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(72.dp).background(
                        Brush.radialGradient(listOf(Color(0xFFDBEAFE), Color(0xFFEFF6FF))),
                        shape = RoundedCornerShape(20.dp)
                    ), contentAlignment = Alignment.Center
                ) { Text(text = "🔗", fontSize = 36.sp) }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Weblink Scanner", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = blue)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Scan links. Stay safe.", fontSize = 15.sp, color = textMuted)
                Spacer(modifier = Modifier.height(40.dp))

                OutlinedTextField(
                    value = email, onValueChange = { email = it; errorMsg = "" },
                    label = { Text("Email") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = blue) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = blue, unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = blue, focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary, cursorColor = blue
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password, onValueChange = { password = it; errorMsg = "" },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, null, tint = blue) },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = textMuted)
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = blue, unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedLabelColor = blue, focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary, cursorColor = blue
                    )
                )

                Row(modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberMe, onCheckedChange = { rememberMe = it },
                            colors = CheckboxDefaults.colors(checkedColor = blue,
                                uncheckedColor = Color(0xFFCBD5E1)))
                        Text("Remember me", fontSize = 13.sp, color = textMuted)
                    }
                    TextButton(onClick = { forgotEmail = email; showForgotDialog = true }) {
                        Text("Forgot Password?", color = blue, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(errorMsg, color = errorRed, fontSize = 13.sp,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMsg = "Please enter your email and password"; return@Button
                        }
                        isLoading = true; errorMsg = ""
                        NewRetrofitClient.api.login(LoginRequest(email.trim(), password))
                            .enqueue(object : Callback<LoginResponse> {
                                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                    Handler(Looper.getMainLooper()).post {
                                        isLoading = false
                                        if (response.isSuccessful) {
                                            val body  = response.body()
                                            val token = body?.access_token
                                            if (rememberMe && token != null) {
                                                TokenManager.saveSession(
                                                    context = context,
                                                    token   = token,
                                                    name    = body?.name    ?: "",
                                                    email   = email.trim(),
                                                    plan    = body?.plan    ?: "FREE",
                                                    userId  = body?.user_id ?: "00000000-0000-0000-0000-000000000000",
                                                    role    = body?.role    ?: "user"
                                                )
                                            }
                                            // Pass name, email, plan back to AppNavigation
                                            onLoginSuccess(
                                                body?.name        ?: "",
                                                email.trim(),
                                                body?.plan        ?: "FREE",
                                                body?.user_id     ?: "00000000-0000-0000-0000-000000000000",
                                                body?.access_token ?: "",
                                                body?.role        ?: "user"
                                            )
                                        } else {
                                            errorMsg = "Invalid email or password"
                                        }
                                    }
                                }
                                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                    Handler(Looper.getMainLooper()).post {
                                        isLoading = false
                                        errorMsg = t.message ?: "Network error"
                                        Log.d("LOGIN", "Error: ${t.message}")
                                    }
                                }
                            })
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = blue,
                        disabledContainerColor = blue.copy(alpha = 0.6f))
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                    else Text("Log In", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 32.dp)) {
                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 16.dp))
                Text("Don't have an account?", fontSize = 14.sp, color = textMuted)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = blue)
                ) {
                    Text("Create an Account", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = blue)
                }
            }
        }
    }
}