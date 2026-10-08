package com.example.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirebaseAuthRepository
import com.example.data.authErrorMessage
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.util.WavesValidation
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderGray
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.DangerRed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun SignUpScreen(
    onNavigateBack: () -> Unit,
    onNavigateToLogIn: () -> Unit,
    onNavigateToVerifyEmail: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var agreeToTerms by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val fullNameError = if (hasSubmitted) WavesValidation.name(fullName, "Full name") else null
    val emailError = if (hasSubmitted) WavesValidation.email(email) else null
    val passwordError = if (hasSubmitted) WavesValidation.password(password) else null

    if (errorMessage.isNotEmpty()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Account Creation Failed",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    if (isLoading) {
        StateScreen(
            type = StateType.LOADING,
            message = "Creating your account..."
        )
        return
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Create Account",
                onBackClick = onNavigateBack
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("signup_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Create Account",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = "Start invoicing in 30 seconds",
                fontSize = 14.sp,
                color = TextSecondary
            )

            WavesTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = "Full Name",
                placeholder = "e.g. John Doe",
                leadingIcon = Icons.Filled.Person,
                errorMessage = fullNameError
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
                placeholder = "Min 6 characters",
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = agreeToTerms,
                    onCheckedChange = { agreeToTerms = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldInk,
                        checkmarkColor = AccentCyan
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "I agree to",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Terms & Conditions",
                    modifier = Modifier.clickable {
                        try {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://waves.metricfluxsolutions.com/termsandconditions")
                                )
                            )
                        } catch (_: ActivityNotFoundException) {
                            showDemoToast(context, "No browser is available to open Terms & Conditions.")
                        }
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldInk,
                    textDecoration = TextDecoration.Underline
                )
            }
            if (hasSubmitted && !agreeToTerms) {
                Text(
                    text = "You must agree to the terms to create an account.",
                    fontSize = 12.sp,
                    color = DangerRed,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            WavesPrimaryButton(
                text = "CREATE ACCOUNT",
                onClick = {
                    hasSubmitted = true
                    if (WavesValidation.name(fullName, "Full name") == null &&
                        WavesValidation.email(email) == null &&
                        WavesValidation.password(password) == null &&
                        agreeToTerms
                    ) {
                        isLoading = true
                        coroutineScope.launch {
                            try {
                                val result = FirebaseAuthRepository.register(
                                    fullName = fullName,
                                    email = email,
                                    password = password,
                                    termsAccepted = agreeToTerms
                                )
                                onNavigateToVerifyEmail(email, result.verificationEmailSent)
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = authErrorMessage(exception)
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
                    text = "Already have an account? ",
                    fontSize = 14.sp,
                    color = TextSecondary
                )
                Text(
                    text = "Log In",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldInk,
                    modifier = Modifier.clickable { onNavigateToLogIn() }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
