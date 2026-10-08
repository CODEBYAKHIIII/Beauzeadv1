package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.EmailNotVerifiedException
import com.example.data.FirebaseAuthRepository
import com.example.data.authErrorMessage
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.util.WavesValidation
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun LogInScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onNavigateToDashboard: () -> Unit,
    onNavigateToVerifyEmail: (String) -> Unit,
    onNavigateToForgotPassword: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showErrorState by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var unverifiedEmail by remember { mutableStateOf<String?>(null) }

    val emailError = if (hasSubmitted) WavesValidation.email(email) else null
    val passwordError = if (hasSubmitted && password.isBlank()) "Password is required" else null

    if (isLoading) {
        StateScreen(
            type = StateType.LOADING,
            message = "Logging in to your account..."
        )
        return
    }

    if (showErrorState) {
        StateScreen(
            type = StateType.ERROR,
            title = "Login Failed",
            message = errorMessage,
            primaryButtonText = if (unverifiedEmail == null) "TRY AGAIN" else "VERIFY EMAIL",
            onPrimaryClick = {
                val emailToVerify = unverifiedEmail
                showErrorState = false
                unverifiedEmail = null
                if (emailToVerify != null) onNavigateToVerifyEmail(emailToVerify)
            },
            onSecondaryClick = { showErrorState = false }
        )
        return
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Welcome Back",
                onBackClick = onNavigateBack
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("login_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Welcome Back",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Sign in to manage your invoices and business",
                fontSize = 14.sp,
                color = TextSecondary
            )

            WavesTextField(
                value = email,
                onValueChange = { email = it },
                label = "Email",
                placeholder = "name@company.com",
                leadingIcon = Icons.Filled.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                errorMessage = emailError
            )

            WavesTextField(
                value = password,
                onValueChange = { password = it },
                label = "Password",
                placeholder = "Enter your password",
                leadingIcon = Icons.Filled.Lock,
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                errorMessage = passwordError,
                trailingIcon = {
                    IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Icon(
                            imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = "Toggle password visibility",
                            tint = TextSecondary
                        )
                    }
                }
            )

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldInk,
                    modifier = Modifier.clickable { onNavigateToForgotPassword() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            WavesPrimaryButton(
                text = "LOG IN",
                onClick = {
                    hasSubmitted = true
                    if (WavesValidation.email(email) == null && password.isNotBlank()) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                FirebaseAuthRepository.signIn(email, password)
                                showDemoToast(context, "Welcome back!")
                                onNavigateToDashboard()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = authErrorMessage(exception)
                                unverifiedEmail = (exception as? EmailNotVerifiedException)?.email
                                showErrorState = true
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Sign Up",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldInk,
                    modifier = Modifier.clickable { onNavigateToSignUp() }
                )
            }
        }
    }
}
