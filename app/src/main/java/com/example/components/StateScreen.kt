package com.example.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class StateType {
    LOADING,
    SUCCESS,
    ERROR
}

@Composable
fun StateScreen(
    type: StateType,
    title: String = when (type) {
        StateType.LOADING -> ""
        StateType.SUCCESS -> "Success!"
        StateType.ERROR -> "Something went wrong"
    },
    message: String = when (type) {
        StateType.LOADING -> "Please wait..."
        StateType.SUCCESS -> "Action completed successfully."
        StateType.ERROR -> "An unexpected issue occurred. Please try again."
    },
    primaryButtonText: String = when (type) {
        StateType.SUCCESS -> "CONTINUE"
        StateType.ERROR -> "TRY AGAIN"
        StateType.LOADING -> ""
    },
    onPrimaryClick: () -> Unit = {},
    onSecondaryClick: (() -> Unit)? = null,
    secondaryButtonText: String = "CANCEL",
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize()
            .testTag("state_screen_${type.name.lowercase()}"),
        color = BackgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (type) {
                StateType.LOADING -> {
                    // --- A) LOADING STATE ---
                    // CircularProgressIndicator (color #064E3B, size 48dp)
                    // Text below: "Please wait..." (14sp, #6B7280)
                    CircularProgressIndicator(
                        color = EmeraldInk,
                        strokeWidth = 3.5.dp,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("state_loading_spinner")
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (title.isNotBlank()) title else message,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                }

                StateType.SUCCESS -> {
                    // --- B) SUCCESS STATE ---
                    // Circle icon with white checkmark, background #16A34A, 72dp
                    // Title: "Success!" (20sp Bold #0A2540)
                    // Message below (14sp #6B7280)
                    // Emerald "CONTINUE" button (56dp, full width)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF16A34A))
                            .testTag("state_success_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Success",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onPrimaryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("state_success_continue_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldInk,
                            contentColor = OnPrimary
                        )
                    ) {
                        Text(
                            text = primaryButtonText,
                            style = ButtonTextStyle,
                            color = OnPrimary
                        )
                    }
                }

                StateType.ERROR -> {
                    // --- C) ERROR STATE ---
                    // Circle icon with white X, background #DC2626, 72dp
                    // Title: "Something went wrong" (20sp Bold #0A2540)
                    // Message below (14sp #6B7280)
                    // Emerald "TRY AGAIN" button (56dp, full width)
                    // Outlined "CANCEL" button below
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDC2626))
                            .testTag("state_error_icon"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Error",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = message,
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = onPrimaryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("state_error_retry_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldInk,
                            contentColor = OnPrimary
                        )
                    ) {
                        Text(
                            text = primaryButtonText,
                            style = ButtonTextStyle,
                            color = OnPrimary
                        )
                    }

                    if (onSecondaryClick != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onSecondaryClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .testTag("state_error_cancel_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF9CA3AF)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = SurfaceColor,
                                contentColor = TextPrimary
                            )
                        ) {
                            Text(
                                text = secondaryButtonText,
                                style = ButtonTextStyle,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
