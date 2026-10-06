package com.example.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.BannerAdPlaceholder
import com.example.components.StatusChip
import com.example.components.WavesBottomNav
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesNavTab
import com.example.components.WavesPrimaryButton
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.showDemoToast
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.BusinessProfile
import com.example.data.InvoiceDisplayFormat
import com.example.data.Invoice
import com.example.data.InvoiceStatus
import com.example.data.ReportDateUtils
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToInvoiceList: () -> Unit,
    onNavigateToInvoiceDetail: (String) -> Unit,
    onNavigateToBusinessProfile: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToProducts: () -> Unit,
    onNavigateToTab: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val invoiceState by remember { FirestoreDataRepository.observeInvoices() }
        .collectAsState(initial = FirestoreState.Loading)
    val businessState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    if (invoiceState is FirestoreState.Loading || businessState is FirestoreState.Loading) {
        StateScreen(type = StateType.LOADING, message = "Loading dashboard...")
        return
    }
    if (invoiceState is FirestoreState.Failure || businessState is FirestoreState.Failure) {
        val error = (invoiceState as? FirestoreState.Failure)?.message
            ?: (businessState as? FirestoreState.Failure)?.message
            ?: "Unable to load dashboard data."
        StateScreen(
            type = StateType.ERROR,
            title = "Dashboard Error",
            message = error
        )
        return
    }
    val invoices = ((invoiceState as? FirestoreState.Data<*>)?.value as? List<Invoice>).orEmpty()
    val business = (businessState as? FirestoreState.Data<*>)?.value as? BusinessProfile
        ?: run {
            StateScreen(
                type = StateType.ERROR,
                title = "Dashboard Error",
                message = "Business settings are unavailable."
            )
            return
        }
    val greeting by produceState(initialValue = InvoiceDisplayFormat.greeting()) {
        while (true) {
            value = InvoiceDisplayFormat.greeting()
            delay(60_000)
        }
    }
    val userName = com.example.data.FirebaseAuthRepository.currentUser?.displayName
        ?.takeIf(String::isNotBlank)
        ?: com.example.data.FirebaseAuthRepository.currentUser?.email?.substringBefore("@")
        ?: "there"
    val currentMonthKey = ReportDateUtils.monthKey(ReportDateUtils.currentDate())
    val revenueThisMonth = invoices.sumOf { invoice ->
        invoice.payments.filter { ReportDateUtils.monthKey(it.date) == currentMonthKey }
            .sumOf { it.amount }
    }
    val pendingCount = invoices.count {
        it.status == InvoiceStatus.PENDING || it.status == InvoiceStatus.HALF_PAID ||
            it.status == InvoiceStatus.OVERDUE
    }
    val recentInvoices = invoices.sortedByDescending { it.id }.take(3)

    Scaffold(
        topBar = {
            WavesHeader(
                title = "WAVES",
                actions = {
                    IconButton(
                        onClick = { showDemoToast(context, "No unread notifications") },
                        modifier = Modifier.testTag("dashboard_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = OnPrimary
                        )
                    }
                    IconButton(
                        onClick = { onNavigateToTab(WavesNavTab.SETTINGS) },
                        modifier = Modifier.testTag("dashboard_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = "Settings",
                            tint = OnPrimary
                        )
                    }
                }
            )
        },
        bottomBar = {
            WavesBottomNav(
                currentTab = WavesNavTab.HOME,
                onTabSelected = onNavigateToTab
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("dashboard_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$greeting, $userName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Big emerald button: "+ NEW INVOICE"
            WavesPrimaryButton(
                text = "+ NEW INVOICE",
                icon = Icons.Filled.Add,
                onClick = onNavigateToCreateInvoice
            )

            // Two stat cards side-by-side: invoice count and collected revenue this month.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WavesCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToInvoiceList
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Total Invoices",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Filled.ReceiptLong,
                                contentDescription = null,
                                tint = EmeraldInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "${invoices.size} Invoices",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$pendingCount pending",
                            fontSize = 11.sp,
                            color = AccentCyan
                        )
                    }
                }

                WavesCard(
                    modifier = Modifier.weight(1f),
                    onClick = onNavigateToReports
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Revenue",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Icon(
                                imageVector = Icons.Filled.Assessment,
                                contentDescription = null,
                                tint = AccentCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = InvoiceDisplayFormat.formatCurrency(revenueThisMonth, business.country),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "This Month",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Quick Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionChip(
                    label = "Business",
                    icon = Icons.Filled.Business,
                    onClick = onNavigateToBusinessProfile,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Products",
                    icon = Icons.Filled.Inventory2,
                    onClick = onNavigateToProducts,
                    modifier = Modifier.weight(1f)
                )
                QuickActionChip(
                    label = "Reports",
                    icon = Icons.Filled.Assessment,
                    onClick = onNavigateToReports,
                    modifier = Modifier.weight(1f)
                )
            }

            // Section "Recent Invoices" with "See All" link
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Invoices",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "See All",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldInk,
                    modifier = Modifier
                        .clickable { onNavigateToInvoiceList() }
                        .padding(4.dp)
                )
            }

            if (recentInvoices.isEmpty()) {
                Text(
                    text = "No invoices yet. Create an invoice to see it here.",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
            recentInvoices.forEach { invoice ->
                WavesCard(
                    onClick = { onNavigateToInvoiceDetail(invoice.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = invoice.id,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "· ${invoice.clientName}",
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = InvoiceDisplayFormat.formatCurrency(invoice.grandTotal, business.country),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        StatusChip(status = invoice.status)
                    }
                }
            }

            // Banner ad placeholder at bottom
            BannerAdPlaceholder()

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun QuickActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = androidx.compose.ui.graphics.Color(0xFFE6F4EA)
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = EmeraldInk,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldInk
            )
        }
    }
}
