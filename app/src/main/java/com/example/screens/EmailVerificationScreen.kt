package com.example.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.data.FirebaseAuthRepository
import com.example.data.authErrorMessage
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EmailVerificationScreen(
    email: String,
    initialEmailSent: Boolean,
    onNavigateBack: () -> Unit,
    onVerificationSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(
            if (initialEmailSent) "A verification link was sent to $email."
            else "We couldn't confirm that a verification email was sent. Resend it below."
        )
    }
    var secondsUntilResend by remember { mutableIntStateOf(if (initialEmailSent) 60 else 0) }

    LaunchedEffect(secondsUntilResend) {
        if (secondsUntilResend > 0) {
            delay(1000)
            secondsUntilResend--
        }
    }

    LaunchedEffect(Unit) {
        try {
            if (FirebaseAuthRepository.isCurrentUserEmailVerified()) {
                onVerificationSuccess()
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            message = authErrorMessage(exception)
        }
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Verify your email",
                onBackClick = onNavigateBack
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Check your inbox",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(28.dp))
            WavesPrimaryButton(
                text = if (isLoading) "CHECKING..." else "I'VE VERIFIED MY EMAIL",
                onClick = {
                    if (!isLoading) {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                if (FirebaseAuthRepository.isCurrentUserEmailVerified()) {
                                    onVerificationSuccess()
                                } else {
                                    message = "Your email isn't verified yet. Open the link in your inbox, then check again."
                                }
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                message = authErrorMessage(exception)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    if (!isLoading && secondsUntilResend == 0) {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                FirebaseAuthRepository.sendVerificationEmail()
                                message = "A new verification link was sent to $email."
                                secondsUntilResend = 60
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                message = authErrorMessage(exception)
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && secondsUntilResend == 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = EmeraldInk,
                        strokeWidth = 2.dp,
                        modifier = Modifier.height(18.dp)
                    )
                } else {
                    Text(
                        text = if (secondsUntilResend > 0) "RESEND IN ${secondsUntilResend}s" else "RESEND EMAIL"
                    )
                }
            }
        }
    }
}
