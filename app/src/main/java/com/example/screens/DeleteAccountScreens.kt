package com.example.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.data.AccountDeletionRepository
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.WavesValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DeleteAccountReasonScreen(
    onNavigateBack: () -> Unit,
    onOtpSent: (String) -> Unit
) {
    var reason by rememberSaveable { mutableStateOf("") }
    var isSending by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = { WavesHeader(title = "Delete Account", onBackClick = onNavigateBack) },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "We’re sorry to see you go",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Tell us why you’re deleting your account. Your account and associated Waves data will be permanently deleted after email verification.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            WavesCard {
                Column(modifier = Modifier.fillMaxWidth()) {
                    WavesTextField(
                        value = reason,
                        onValueChange = {
                            if (it.length <= MAX_REASON_LENGTH) reason = it
                        },
                        label = "Reason (optional)",
                        placeholder = "Share your feedback",
                        singleLine = false,
                        maxLines = 6,
                        errorMessage = errorMessage
                    )
                    Text(
                        text = "${reason.length}/$MAX_REASON_LENGTH",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
            Text(
                text = "This action cannot be undone. A six-digit verification code will be sent to ${com.example.data.FirebaseAuthRepository.currentUser?.email.orEmpty()}.",
                fontSize = 13.sp,
                color = DangerRed
            )
            Spacer(modifier = Modifier.height(4.dp))
            WavesPrimaryButton(
                text = if (isSending) "Sending code..." else "Continue",
                enabled = !isSending,
                onClick = {
                    coroutineScope.launch {
                        isSending = true
                        errorMessage = null
                        try {
                            AccountDeletionRepository.sendVerificationCode(reason.trim())
                            onOtpSent(reason)
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            errorMessage = exception.localizedMessage
                                ?: "We couldn’t send a verification code. Please try again."
                        } finally {
                            isSending = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun DeleteAccountOtpScreen(
    reason: String,
    onNavigateBack: () -> Unit,
    onAccountDeleted: () -> Unit
) {
    var otp by rememberSaveable { mutableStateOf("") }
    var resendSeconds by rememberSaveable { mutableIntStateOf(60) }
    var isResending by rememberSaveable { mutableStateOf(false) }
    var isDeleting by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val email = com.example.data.FirebaseAuthRepository.currentUser?.email.orEmpty()

    LaunchedEffect(resendSeconds) {
        if (resendSeconds > 0) {
            delay(1_000)
            resendSeconds--
        }
    }

    Scaffold(
        topBar = { WavesHeader(title = "Verify Deletion", onBackClick = onNavigateBack) },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Verify it’s you",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Enter the six-digit code sent to $email.",
                fontSize = 14.sp,
                color = TextSecondary
            )
            WavesCard {
                WavesTextField(
                    value = otp,
                    onValueChange = { value ->
                        if (value.length <= OTP_LENGTH && value.all { it in '0'..'9' }) {
                            otp = value
                            errorMessage = null
                        }
                    },
                    label = "6-digit verification code",
                    placeholder = "000000",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    errorMessage = errorMessage
                )
            }
            androidx.compose.material3.TextButton(
                onClick = {
                    coroutineScope.launch {
                        isResending = true
                        errorMessage = null
                        try {
                            AccountDeletionRepository.sendVerificationCode(reason)
                            resendSeconds = 60
                        } catch (exception: CancellationException) {
                            throw exception
                        } catch (exception: Exception) {
                            errorMessage = exception.localizedMessage
                                ?: "We couldn’t resend the code. Please try again."
                        } finally {
                            isResending = false
                        }
                    }
                },
                enabled = resendSeconds == 0 && !isResending && !isDeleting
            ) {
                Text(
                    text = when {
                        isResending -> "Sending..."
                        resendSeconds > 0 -> "Resend code in ${resendSeconds}s"
                        else -> "Resend code"
                    },
                    color = EmeraldInk
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = {
                    val otpValidationError = WavesValidation.otp(otp, OTP_LENGTH)
                    if (otpValidationError == null) {
                        coroutineScope.launch {
                            isDeleting = true
                            errorMessage = null
                            try {
                                AccountDeletionRepository.deleteAccount(otp)
                                onAccountDeleted()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = exception.localizedMessage
                                    ?: "We couldn’t delete your account. Please try again."
                            } finally {
                                isDeleting = false
                            }
                        }
                    } else {
                        errorMessage = otpValidationError
                    }
                },
                enabled = !isDeleting && !isResending,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White,
                    disabledContainerColor = DangerRed.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = if (isDeleting) "Deleting account..." else "Delete my account",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private const val MAX_REASON_LENGTH = 350
private const val OTP_LENGTH = 6
