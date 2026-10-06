package com.example.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.OnPrimary

private val LandingBackground = Color(0xFFF8FAFC)
private val LandingEmerald = Color(0xFF064E3B)
private val DarkTextColor = Color(0xFF000000)
private val DarkSecondaryText = Color(0xFF000000)
private val AccountPromptColor = Color(0xFF000000)

@Composable
fun LandingScreen(
    onNavigateToSignUp: () -> Unit,
    onNavigateToLogIn: () -> Unit,
    onNavigateToDashboard: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val features = listOf(
        "Create invoices in 30s",
        "Global tax support",
        "Export PDF instantly",
        "Works 100% offline"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(LandingBackground)
            .padding(horizontal = 24.dp)
            .testTag("landing_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Flexible spacer to push the logo, company name, tagline, and features DOWN
        Spacer(modifier = Modifier.weight(1f))

        // Logo Image — 120dp × 120dp
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "WAVES Logo",
            modifier = Modifier
                .size(120.dp)
                .testTag("landing_logo")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Company Name: WAVES — 28sp Bold, Dark Font Color
        Text(
            text = "WAVES",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = DarkTextColor,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Tagline: Free Invoice & Accounting — Dark Font Color (#0A2540)
        Text(
            text = "Free Invoice & Accounting",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DarkTextColor,
            textAlign = TextAlign.Center
        )

        // Sits just above the 4 points
        Spacer(modifier = Modifier.height(28.dp))

        // 4 Points Feature List — centered block, max-width 320dp
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .align(Alignment.CenterHorizontally)
                .testTag("landing_features_block"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = LandingEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = feature,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkTextColor
                    )
                }
            }
        }

        // Flexible spacer between the 4 points and the action buttons
        Spacer(modifier = Modifier.weight(1.2f))

        // GET STARTED Button — full-width, 56dp height, 12dp radius, Emerald #064E3B
        Button(
            onClick = onNavigateToSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("get_started_button"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = LandingEmerald,
                contentColor = OnPrimary
            )
        ) {
            Text(
                text = "GET STARTED",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = OnPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Log In Row — "Already have an account? " (Dark) + "Log In" (Bold, Emerald #064E3B)
        Row(
            modifier = Modifier.testTag("login_link_row"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account? ",
                fontSize = 14.sp,
                color = AccountPromptColor
            )
            Text(
                text = "Log In",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = LandingEmerald,
                modifier = Modifier
                    .clickable { onNavigateToLogIn() }
                    .testTag("landing_login_link")
            )
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}
