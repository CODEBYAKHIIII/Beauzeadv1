package com.example.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.authErrorMessage
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderGray
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    onNavigateToBusinessProfile: () -> Unit,
    onNavigateToTaxSettings: () -> Unit,
    onNavigateToBankSettings: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToDeleteAccount: () -> Unit,
    onNavigateToLogIn: () -> Unit,
    onNavigateToTab: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val account = FirebaseAuthRepository.currentUser
    val accountName = account?.displayName.orEmpty().ifBlank { account?.email.orEmpty() }
    val accountEmail = account?.email.orEmpty()
    val businessState by FirestoreDataRepository.observeBusinessProfile()
        .collectAsState(initial = FirestoreState.Loading)

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Profile",
                actions = {
                    IconButton(
                        onClick = onNavigateToBusinessProfile,
                        modifier = Modifier.testTag("profile_edit_business")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit business profile",
                            tint = OnPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            WavesBottomNav(
                currentTab = WavesNavTab.PROFILE,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("profile_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            ProfileSectionTitle("YOUR ACCOUNT")
            WavesCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(EmeraldInk.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = accountName.firstOrNull()?.uppercase() ?: "?",
                            color = EmeraldInk,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            text = accountName.ifBlank { "Account" },
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = accountEmail,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        if (account?.isEmailVerified == true) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SuccessGreen.copy(alpha = 0.10f))
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Verified",
                                    color = SuccessGreen,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            ProfileSectionTitle("BUSINESS")
            WavesCard {
                Column {
                    when (val state = businessState) {
                        FirestoreState.Loading -> {
                            ProfileMenuRow(
                                icon = Icons.Filled.Business,
                                title = "Business profile",
                                subtitle = "Loading business details",
                                onClick = onNavigateToBusinessProfile
                            )
                        }
                        is FirestoreState.Failure -> {
                            ProfileMenuRow(
                                icon = Icons.Filled.Business,
                                title = "Business profile",
                                subtitle = state.message,
                                onClick = onNavigateToBusinessProfile
                            )
                        }
                        is FirestoreState.Data -> {
                            val business = state.value
                            ProfileMenuRow(
                                icon = Icons.Filled.Business,
                                title = business.name.ifBlank { "Business profile" },
                                subtitle = if (business.name.isBlank()) {
                                    "Add your business details"
                                } else {
                                    business.tagline.takeIf(String::isNotBlank)
                                        ?: "Edit business details"
                                },
                                onClick = onNavigateToBusinessProfile
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 4.dp),
                                color = CardBorder
                            )
                            ProfileMenuRow(
                                icon = Icons.Filled.Receipt,
                                title = "Business contact & address",
                                subtitle = listOf(
                                    business.email,
                                    business.city,
                                    business.country
                                ).filter(String::isNotBlank).joinToString(" · ")
                                    .ifBlank { "Add contact and address details" },
                                onClick = onNavigateToBusinessProfile,
                                compact = true
                            )
                        }
                    }
                }
            }

            ProfileSectionTitle("PREFERENCES")
            WavesCard {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Filled.Receipt,
                        title = "Tax settings",
                        onClick = onNavigateToTaxSettings
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.Filled.AccountBalance,
                        title = "Bank account",
                        onClick = onNavigateToBankSettings
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.Filled.Inventory2,
                        title = "Products & services",
                        onClick = onNavigateToProducts
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.Filled.Assessment,
                        title = "Reports & analytics",
                        onClick = onNavigateToReports
                    )
                }
            }

            ProfileSectionTitle("ACCOUNT")
            WavesCard {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Filled.Lock,
                        title = "Change password",
                        onClick = {
                            if (accountEmail.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        FirebaseAuthRepository.sendPasswordResetEmail(accountEmail)
                                        showDemoToast(
                                            context,
                                            "If an account exists for this email, reset instructions are on their way."
                                        )
                                    } catch (exception: CancellationException) {
                                        throw exception
                                    } catch (exception: Exception) {
                                        showDemoToast(context, authErrorMessage(exception))
                                    }
                                }
                            }
                        }
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.Filled.DeleteForever,
                        title = "Delete account",
                        iconTint = DangerRed,
                        titleColor = DangerRed,
                        onClick = onNavigateToDeleteAccount
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.AutoMirrored.Filled.ExitToApp,
                        title = "Log out",
                        iconTint = DangerRed,
                        titleColor = DangerRed,
                        onClick = onNavigateToLogIn
                    )
                }
            }

            WavesCard {
                Column {
                    ProfileMenuRow(
                        icon = Icons.Filled.Policy,
                        title = "Terms & Conditions",
                        onClick = {
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
                        }
                    )
                    ProfileDivider()
                    ProfileMenuRow(
                        icon = Icons.Filled.Policy,
                        title = "Privacy policy",
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
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 4.dp),
        color = TextPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp
    )
}

@Composable
private fun ProfileDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 4.dp),
        color = BorderGray.copy(alpha = 0.45f)
    )
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = EmeraldInk,
    titleColor: Color = TextPrimary,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = if (compact) 10.dp else 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(22.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = TextSecondary.copy(alpha = 0.55f)
        )
    }
}
