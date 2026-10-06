package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseAuthRepository
import com.example.data.authErrorMessage
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesTextField
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var hasSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var requestSent by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val isEmailValid = email.isNotBlank() && email.contains("@") && email.contains(".")
    val emailError = if (hasSubmitted && !isEmailValid) "Enter a valid email" else null

    if (requestSent) {
        StateScreen(
            type = StateType.SUCCESS,
            title = "Check your email",
            message = "If an account exists for this email, password reset instructions are on their way.",
            primaryButtonText = "BACK TO LOG IN",
            onPrimaryClick = onNavigateToLogIn
        )
        return
    }

    if (errorMessage.isNotEmpty()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Couldn't send reset email",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    if (isLoading) {
        StateScreen(
            type = StateType.LOADING,
            message = "Sending password reset link..."
        )
        return
    }

    Scaffold(
        topBar = {
            // Emerald header: ← Back | no title
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("forgot_password_header"),
                color = EmeraldInk
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("forgot_password_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = OnPrimary
                        )
                    }
                }
            }
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("forgot_password_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Lock/Key icon — Cyan #00D4B8, size 64dp, top spacing 48dp
            Spacer(modifier = Modifier.height(48.dp))
            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "Key Icon",
                tint = AccentCyan,
                modifier = Modifier
                    .size(64.dp)
                    .testTag("forgot_password_icon")
            )

            // 2. Title "Forgot Password?" — 24sp Bold #0A2540, top 24dp
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Forgot Password?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            // 3. Subtitle "Enter your registered email to reset password" — 14sp #6B7280, centered, top 8dp
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Enter your registered email to receive a secure password reset link",
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // 4. Email input field — top 40dp
            Spacer(modifier = Modifier.height(40.dp))
            WavesTextField(
                value = email,
                onValueChange = { email = it },
                label = "Registered Email",
                placeholder = "name@company.com",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                errorMessage = emailError
            )

            // 5. Emerald reset-link button — top 32dp
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    hasSubmitted = true
                    if (isEmailValid) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                FirebaseAuthRepository.sendPasswordResetEmail(email)
                                requestSent = true
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = authErrorMessage(exception)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = email.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("send_otp_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldInk,
                    contentColor = OnPrimary,
                    disabledContainerColor = EmeraldInk.copy(alpha = 0.4f),
                    disabledContentColor = OnPrimary.copy(alpha = 0.6f)
                )
            ) {
                Text(
                    text = "SEND RESET LINK",
                    style = ButtonTextStyle,
                    color = OnPrimary
                )
            }

            // 6. Text link at bottom: "Remember password? Log In" — top 16dp
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.clickable { onNavigateBack() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Remember password? ",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Log In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
