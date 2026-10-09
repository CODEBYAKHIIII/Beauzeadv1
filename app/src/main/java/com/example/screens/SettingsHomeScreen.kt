package com.example.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarRate
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesBottomNav
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesNavTab
import com.example.components.showDemoToast
import com.example.data.FirebaseAuthRepository
import com.example.data.authErrorMessage
import com.example.util.WavesAds
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun SettingsHomeScreen(
    onNavigateToPrefixNumbering: () -> Unit,
    onNavigateToCurrency: () -> Unit,
    onNavigateToPaymentTerms: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToLogIn: () -> Unit,
    onNavigateToTab: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val accountEmail = FirebaseAuthRepository.currentUser?.email.orEmpty()

    Scaffold(
        topBar = {
            WavesHeader(title = "Settings")
        },
        bottomBar = {
            WavesBottomNav(
                currentTab = WavesNavTab.SETTINGS,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("settings_home_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // INVOICE DEFAULTS SECTION
            SectionHeader(title = "INVOICE DEFAULTS")
            WavesCard {
                Column {
                    SettingsRowItem(
                        icon = Icons.Filled.Tag,
                        label = "Prefix & Numbering",
                        onClick = onNavigateToPrefixNumbering
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.MonetizationOn,
                        label = "Currency",
                        onClick = onNavigateToCurrency
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.Schedule,
                        label = "Payment Terms",
                        onClick = onNavigateToPaymentTerms
                    )
                }
            }

            // ACCOUNT SECTION
            SectionHeader(title = "ACCOUNT")
            WavesCard {
                Column {
                    SettingsRowItem(
                        icon = Icons.Filled.Email,
                        label = "Email",
                        subtitle = accountEmail,
                        onClick = { showDemoToast(context, accountEmail) }
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.Lock,
                        label = "Change Password",
                        onClick = {
                            if (accountEmail.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        FirebaseAuthRepository.sendPasswordResetEmail(accountEmail)
                                        showDemoToast(context, "If an account exists for this email, reset instructions are on their way.")
                                    } catch (exception: Exception) {
                                        showDemoToast(context, authErrorMessage(exception))
                                    }
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        label = "Log Out",
                        iconTint = DangerRed,
                        labelColor = DangerRed,
                        onClick = onNavigateToLogIn
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.DeleteForever,
                        label = "Delete Account",
                        subtitle = "Permanently delete your account and data",
                        iconTint = DangerRed,
                        labelColor = DangerRed,
                        onClick = onNavigateToDeleteAccount
                    )
                }
            }

            // OTHER SECTION
            SectionHeader(title = "OTHER")
            WavesCard {
                Column {
                    SettingsRowItem(
                        icon = Icons.Filled.Assessment,
                        label = "Reports & Analytics",
                        onClick = onNavigateToReports
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.Star,
                        label = "Remove Ads",
                        subtitle = "Watch a short video - ad-free for 24 hours",
                        onClick = {
                            val activity = context as? android.app.Activity
                            if (activity == null) {
                                showDemoToast(context, "Not available right now.")
                            } else {
                                WavesAds.removeAdsFor24Hours(activity) { _, message ->
                                    showDemoToast(context, message)
                                }
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.Policy,
                        label = "Privacy Policy",
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://waves.metricfluxsolutions.com/privacypolicy")
                                    )
                                )
                            } catch (_: ActivityNotFoundException) {
                                showDemoToast(context, "No browser is available to open the Privacy Policy.")
                            }
                        }
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.StarRate,
                        label = "Rate the App",
                        onClick = { showDemoToast(context, "Thank you for rating 5 stars!") }
                    )
                    HorizontalDivider(color = BorderGray)
                    SettingsRowItem(
                        icon = Icons.Filled.Info,
                        label = "Version",
                        subtitle = "1.2 (Production)",
                        showArrow = false,
                        onClick = {}
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsRowItem(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    iconTint: androidx.compose.ui.graphics.Color = EmeraldInk,
    labelColor: androidx.compose.ui.graphics.Color = TextPrimary,
    showArrow: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = labelColor
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        if (showArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = TextSecondary.copy(alpha = 0.6f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
