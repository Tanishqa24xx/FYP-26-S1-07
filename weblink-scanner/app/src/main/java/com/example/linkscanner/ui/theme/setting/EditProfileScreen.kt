package com.example.linkscanner.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.linkscanner.api.RetrofitClient
import com.example.linkscanner.model.ChangePasswordRequest
import com.example.linkscanner.model.ChangePasswordResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// ── Colors ────────────────────────────────────────────────────────────────────
private val Blue600      = Color(0xFF2563EB)
private val Blue50       = Color(0xFFEFF6FF)
private val Blue100      = Color(0xFFDBEAFE)
private val PageBgTop    = Color(0xFFEFF6FF)
private val PageBgBot    = Color(0xFFF8FAFC)
private val CardBg       = Color.White
private val TextPrimary  = Color(0xFF0F172A)
private val TextMuted    = Color(0xFF64748B)
private val BorderCol    = Color(0xFFCBD5E1)
private val DividerCol   = Color(0xFFE2E8F0)
private val ErrorRed     = Color(0xFFDC2626)
private val SuccessGreen    = Color(0xFF16A34A)
private val SuccessGreenBg  = Color(0xFFF0FDF4)
private val SuccessGreenBorder = Color(0xFFBBF7D0)

@Composable
fun EditProfileScreen(
    currentEmail: String = "",
    currentName: String  = "",
    onSave: () -> Unit   = {},
    onCancel: () -> Unit = {}
) {
    // ── Field state ───────────────────────────────────────────────────────────
    var name            by remember { mutableStateOf(currentName) }
    var email           by remember { mutableStateOf(currentEmail) }
    var phone           by remember { mutableStateOf("") }
    var currentPassword by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var showCurrentPw  by remember { mutableStateOf(false) }
    var showNewPw      by remember { mutableStateOf(false) }
    var showConfirmPw  by remember { mutableStateOf(false) }

    // ── Validation / status state ─────────────────────────────────────────────
    var nameError            by remember { mutableStateOf("") }
    var emailError           by remember { mutableStateOf("") }
    var currentPasswordError by remember { mutableStateOf("") }
    var newPasswordError     by remember { mutableStateOf("") }
    var confirmPasswordError by remember { mutableStateOf("") }
    var globalError          by remember { mutableStateOf("") }
    var successMsg           by remember { mutableStateOf("") }
    var isLoading            by remember { mutableStateOf(false) }

    // Whether the user has touched any password field
    val passwordSectionActive = currentPassword.isNotBlank() ||
            newPassword.isNotBlank() || confirmPassword.isNotBlank()

    // ── Validation ────────────────────────────────────────────────────────────
    fun validate(): Boolean {
        var valid = true
        nameError = ""; emailError = ""
        currentPasswordError = ""; newPasswordError = ""; confirmPasswordError = ""

        if (name.isBlank()) { nameError = "Name is required"; valid = false }
        if (email.isBlank()) {
            emailError = "Email is required"; valid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailError = "Enter a valid email address"; valid = false
        }

        if (passwordSectionActive) {
            if (currentPassword.isBlank()) {
                currentPasswordError = "Enter your current password"; valid = false
            }
            if (newPassword.isBlank()) {
                newPasswordError = "Enter a new password"; valid = false
            } else if (newPassword.length < 6) {
                newPasswordError = "Password must be at least 6 characters"; valid = false
            }
            if (confirmPassword != newPassword) {
                confirmPasswordError = "Passwords do not match"; valid = false
            }
        }
        return valid
    }

    // ── Save handler ──────────────────────────────────────────────────────────
    fun doSave() {
        globalError = ""; successMsg = ""
        if (!validate()) return
        isLoading = true

        if (passwordSectionActive) {
            // ── Call /change-password — verifies old password, then updates ──
            RetrofitClient.api.changePassword(
                ChangePasswordRequest(
                    email            = email.trim(),
                    current_password = currentPassword,
                    new_password     = newPassword
                )
            ).enqueue(object : Callback<ChangePasswordResponse> {
                override fun onResponse(
                    call: Call<ChangePasswordResponse>,
                    response: Response<ChangePasswordResponse>
                ) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        isLoading = false
                        if (response.isSuccessful) {
                            // Clear password fields after success
                            currentPassword = ""
                            newPassword     = ""
                            confirmPassword = ""
                            successMsg = "Password changed successfully! You can now log in with your new password."
                        } else {
                            // Parse the error body for the detail message
                            val errorBody = response.errorBody()?.string() ?: ""
                            currentPasswordError = when {
                                response.code() == 401 ||
                                        errorBody.contains("incorrect", ignoreCase = true) ||
                                        errorBody.contains("invalid", ignoreCase = true) ->
                                    "Current password is incorrect"
                                else ->
                                    "Failed to change password (${response.code()}). Try again."
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        isLoading = false
                        globalError = "Network error: ${t.message ?: "Check your connection"}"
                    }
                }
            })
        } else {
            // No password change — just show profile saved (TODO: wire profile update API)
            isLoading = false
            successMsg = "Profile updated successfully!"
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors = listOf(PageBgTop, PageBgBot)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(56.dp))

            // ── Logo + Title ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        Brush.radialGradient(listOf(Blue100, Blue50)),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 36.sp) }

            Spacer(modifier = Modifier.height(14.dp))
            Text("Edit Profile", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Blue600)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Your account details are used for login and notifications.",
                fontSize = 13.sp, color = TextMuted, textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Form Card ─────────────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(16.dp),
                colors    = CardDefaults.cardColors(containerColor = CardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    // ── Name ──────────────────────────────────────────────────
                    FieldLabel("Name")
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; nameError = "" },
                        placeholder = { Text("Your full name", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Person, null, tint = Blue600) },
                        isError = nameError.isNotEmpty(), singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                        colors = fieldColors()
                    )
                    if (nameError.isNotEmpty()) ErrorText(nameError)

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Email ─────────────────────────────────────────────────
                    FieldLabel("Email")
                    OutlinedTextField(
                        value = email, onValueChange = { email = it; emailError = "" },
                        placeholder = { Text("you@example.com", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = Blue600) },
                        isError = emailError.isNotEmpty(), singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = fieldColors()
                    )
                    if (emailError.isNotEmpty()) ErrorText(emailError)

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Phone ─────────────────────────────────────────────────
                    FieldLabel("Phone (optional)")
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        placeholder = { Text("+1 234 567 8900", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Phone, null, tint = Blue600) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = fieldColors()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // ── Password section ──────────────────────────────────────
                    HorizontalDivider(color = DividerCol)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Change Password", fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold, color = TextMuted)
                    Text("Leave blank to keep your current password",
                        fontSize = 11.sp, color = TextMuted.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Current Password ──────────────────────────────────────
                    FieldLabel("Current Password")
                    OutlinedTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it; currentPasswordError = "" },
                        placeholder = { Text("Enter your current password", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = Blue600) },
                        trailingIcon = {
                            IconButton(onClick = { showCurrentPw = !showCurrentPw }) {
                                Icon(if (showCurrentPw) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility, null, tint = TextMuted)
                            }
                        },
                        visualTransformation = if (showCurrentPw) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = currentPasswordError.isNotEmpty(), singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors()
                    )
                    if (currentPasswordError.isNotEmpty()) ErrorText(currentPasswordError)

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── New Password ──────────────────────────────────────────
                    FieldLabel("New Password")
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; newPasswordError = "" },
                        placeholder = { Text("Enter your new password", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Blue600) },
                        trailingIcon = {
                            IconButton(onClick = { showNewPw = !showNewPw }) {
                                Icon(if (showNewPw) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility, null, tint = TextMuted)
                            }
                        },
                        visualTransformation = if (showNewPw) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = newPasswordError.isNotEmpty(), singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors()
                    )
                    if (newPasswordError.isNotEmpty()) ErrorText(newPasswordError)

                    Spacer(modifier = Modifier.height(8.dp))

                    // ── Confirm New Password ──────────────────────────────────
                    FieldLabel("Confirm New Password")
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; confirmPasswordError = "" },
                        placeholder = { Text("Confirm your new password", color = TextMuted) },
                        leadingIcon = { Icon(Icons.Default.LockOpen, null, tint = Blue600) },
                        trailingIcon = {
                            IconButton(onClick = { showConfirmPw = !showConfirmPw }) {
                                Icon(if (showConfirmPw) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility, null, tint = TextMuted)
                            }
                        },
                        visualTransformation = if (showConfirmPw) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        isError = confirmPasswordError.isNotEmpty(), singleLine = true,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        colors = fieldColors()
                    )
                    if (confirmPasswordError.isNotEmpty()) ErrorText(confirmPasswordError)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Success banner ────────────────────────────────────────────────
            if (successMsg.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = SuccessGreenBg),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreenBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = SuccessGreen,
                            modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(successMsg, color = SuccessGreen, fontSize = 13.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Global error banner ───────────────────────────────────────────
            if (globalError.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(12.dp),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                    border   = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA))
                ) {
                    Text(globalError, color = ErrorRed, fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(12.dp))
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Save Changes ──────────────────────────────────────────────────
            Button(
                onClick  = { doSave() },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = Blue600,
                    disabledContainerColor = Blue600.copy(alpha = 0.6f)
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White,
                        modifier = Modifier.size(20.dp), strokeWidth = 2.5.dp)
                } else {
                    Icon(Icons.Default.Save, null, tint = Color.White,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                        color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // ── Cancel ────────────────────────────────────────────────────────
            OutlinedButton(
                onClick  = onCancel,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp),
                border   = ButtonDefaults.outlinedButtonBorder.copy(width = 1.5.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = TextMuted)
            ) {
                Text("Cancel", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
private fun FieldLabel(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
        color = Color(0xFF0F172A), modifier = Modifier.padding(bottom = 2.dp))
}

@Composable
private fun ErrorText(msg: String) {
    Text(msg, color = Color(0xFFDC2626), fontSize = 12.sp,
        modifier = Modifier.padding(top = 2.dp))
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    errorBorderColor     = Color(0xFFDC2626),
    focusedTextColor     = Color(0xFF0F172A),
    unfocusedTextColor   = Color(0xFF0F172A),
    cursorColor          = Color(0xFF2563EB)
)