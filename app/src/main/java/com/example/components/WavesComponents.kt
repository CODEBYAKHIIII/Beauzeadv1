package com.example.components

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.InvoiceStatus
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BannerBg
import com.example.ui.theme.BannerBorder
import com.example.ui.theme.BorderGray
import com.example.ui.theme.ButtonTextStyle
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.HeaderTitleStyle
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.NeutralGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber

fun showDemoToast(context: Context, message: String = "Demo only") {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}

@Composable
fun WavesHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("waves_header"),
        color = EmeraldInk
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("header_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = OnPrimary
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }

            Text(
                text = title,
                style = HeaderTitleStyle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            )

            if (actions != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            } else {
                Spacer(modifier = Modifier.width(16.dp))
            }
        }
    }
}

enum class WavesNavTab(val route: String, val label: String, val icon: ImageVector) {
    HOME("dashboard", "Home", Icons.Filled.Home),
    INVOICES("invoices", "Invoices", Icons.Filled.Description),
    CLIENTS("clients", "Clients", Icons.Filled.People),
    PROFILE("profile", "Profile", Icons.Filled.Person);

    companion object {
        val SETTINGS get() = PROFILE
    }
}

@Composable
fun WavesBottomNav(
    currentTab: WavesNavTab,
    onTabSelected: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .testTag("waves_bottom_nav"),
        color = EmeraldInk
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WavesNavTab.entries.forEach { tab ->
                val isSelected = tab == currentTab
                val alpha = if (isSelected) 1f else 0.6f

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTabSelected(tab) }
                        .padding(vertical = 6.dp)
                        .testTag("nav_tab_${tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                        tint = OnPrimary.copy(alpha = alpha),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.label,
                        color = OnPrimary.copy(alpha = alpha),
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun WavesCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp), spotColor = Color(0x1A000000))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            content()
        }
    }
}

@Composable
fun WavesPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("primary_button_${text.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = EmeraldInk,
            contentColor = OnPrimary,
            disabledContainerColor = EmeraldInk.copy(alpha = 0.5f),
            disabledContentColor = OnPrimary.copy(alpha = 0.7f)
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = OnPrimary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ButtonTextStyle,
            color = OnPrimary
        )
    }
}

@Composable
fun WavesSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    isDestructive: Boolean = false
) {
    val borderColor = if (isDestructive) DangerRed else EmeraldInk
    val textColor = if (isDestructive) DangerRed else EmeraldInk

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("secondary_button_${text.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = SurfaceColor,
            contentColor = textColor
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = ButtonTextStyle,
            color = textColor
        )
    }
}

@Composable
fun WavesTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    errorMessage: String? = null,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val isError = !errorMessage.isNullOrEmpty()
    val isFilled = value.isNotEmpty()

    val borderColor = when {
        isError -> Color(0xFFDC2626)
        isFocused -> Color(0xFF00D4B8)
        isFilled -> Color(0xFF064E3B)
        else -> Color(0xFF9CA3AF)
    }

    val borderWidth = when {
        isError || isFocused -> 2.dp
        else -> 1.5.dp
    }

    val leadingIconTint = when {
        isError -> Color(0xFFDC2626)
        isFocused -> Color(0xFF00D4B8)
        else -> Color(0xFF000000)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) Color(0xFF000000) else Color(0xFF9CA3AF),
            modifier = Modifier.padding(bottom = 6.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            placeholder = placeholder?.let {
                {
                    Text(
                        text = it,
                        color = Color(0xFF4B5563),
                        fontSize = 14.sp
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (singleLine) Modifier.height(56.dp) else Modifier)
                .onFocusChanged { isFocused = it.isFocused }
                .border(borderWidth, borderColor, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .testTag("input_${label.lowercase().replace(" ", "_")}"),
            shape = RoundedCornerShape(12.dp),
            leadingIcon = leadingIcon?.let {
                {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = leadingIconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            trailingIcon = if (isError) {
                {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                trailingIcon
            },
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            maxLines = maxLines,
            isError = isError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                errorContainerColor = Color.White,
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = Color.Transparent,
                focusedTextColor = Color(0xFF000000),
                unfocusedTextColor = Color(0xFF000000),
                errorTextColor = Color(0xFF000000),
                focusedPlaceholderColor = Color(0xFF4B5563),
                unfocusedPlaceholderColor = Color(0xFF4B5563),
                errorPlaceholderColor = Color(0xFF4B5563),
                cursorColor = Color(0xFF00D4B8)
            )
        )

        if (isError) {
            Text(
                text = errorMessage ?: "",
                color = Color(0xFFDC2626),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 4.dp, start = 4.dp)
            )
        }
    }
}

@Composable
fun WavesChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(32.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag("chip_${text.lowercase()}"),
        shape = CircleShape,
        color = if (isSelected) EmeraldInk else SurfaceColor,
        border = BorderStroke(1.dp, if (isSelected) EmeraldInk else BorderGray)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) OnPrimary else TextSecondary
            )
        }
    }
}

@Composable
fun StatusChip(status: InvoiceStatus) {
    val (bgColor, textColor) = when (status) {
        InvoiceStatus.PAID -> Color(0xFFDCFCE7) to SuccessGreen
        InvoiceStatus.HALF_PAID -> Color(0xFFFEF3C7) to WarningAmber
        InvoiceStatus.PENDING -> Color(0xFFF1F5F9) to NeutralGray
        InvoiceStatus.OVERDUE -> Color(0xFFFEE2E2) to DangerRed
        InvoiceStatus.CANCELLED -> Color(0xFFE2E8F0) to NeutralGray
        InvoiceStatus.WRITTEN_OFF -> Color(0xFFE2E8F0) to NeutralGray
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = status.label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun WavesFAB(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Add"
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = EmeraldInk,
        contentColor = OnPrimary,
        shape = CircleShape,
        modifier = modifier.testTag("waves_fab")
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = contentDescription,
            tint = OnPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun BannerAdPlaceholder(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable { showDemoToast(context, "Sponsored Ad Demo") }
            .testTag("banner_ad_placeholder"),
        shape = RoundedCornerShape(8.dp),
        color = BannerBg,
        border = BorderStroke(1.dp, BannerBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(AccentCyan)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("Ad", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Grow your business with Waves Pro", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Upgrade for automatic payment reminders", fontSize = 10.sp, color = TextSecondary)
                }
            }
            Text("Learn More →", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmeraldInk)
        }
    }
}

@Composable
fun EmptyStateView(
    icon: ImageVector,
    title: String,
    message: String,
    buttonText: String? = null,
    onButtonClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp)
            .testTag("empty_state_view"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF9CA3AF),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        if (buttonText != null && onButtonClick != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onButtonClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldInk,
                    contentColor = OnPrimary
                )
            ) {
                Text(
                    text = buttonText,
                    style = ButtonTextStyle,
                    color = OnPrimary
                )
            }
        }
    }
}
