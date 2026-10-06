package com.example.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesBottomNav
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesNavTab
import com.example.components.WavesSecondaryButton
import com.example.components.showDemoToast
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.InvoiceStatus
import com.example.data.DocumentExports
import com.example.data.ReportDateUtils
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.NeutralGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun ReportsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToTab: (WavesNavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    val invoiceState by remember { FirestoreDataRepository.observeInvoices() }
        .collectAsState(initial = FirestoreState.Loading)
    val allInvoices = when (val state = invoiceState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading reports...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, title = "Report Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }
    val currentDate = remember { ReportDateUtils.currentDate() }
    val months = remember(allInvoices) {
        val earliestInvoiceDate = allInvoices.mapNotNull { ReportDateUtils.parse(it.issueDate) }.minOrNull()
        ReportDateUtils.monthKeys(earliestInvoiceDate ?: currentDate, currentDate)
            .ifEmpty { listOf(ReportDateUtils.monthKey(currentDate)) }
    }
    var reportMonthKey by remember { mutableStateOf(ReportDateUtils.monthKey(currentDate)) }
    val currentMonthIndex = months.indexOf(reportMonthKey).takeIf { it >= 0 } ?: months.lastIndex
    val monthLabel = ReportDateUtils.displayMonth(reportMonthKey) ?: reportMonthKey
    val monthInvoices = allInvoices.filter { ReportDateUtils.monthKey(it.issueDate) == reportMonthKey }
    val activeMonthInvoices = monthInvoices.filterNot {
        it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF
    }
    val invoicedAmount = activeMonthInvoices.sumOf { it.grandTotal }
    val collectedAmount = allInvoices.sumOf { invoice ->
        invoice.payments.filter { ReportDateUtils.monthKey(it.date) == reportMonthKey }
            .sumOf { it.amount }
    }
    val outstandingAmount = activeMonthInvoices.sumOf { it.balanceDue }
    val statusCounts = mapOf(
        "Paid" to monthInvoices.count { it.status == InvoiceStatus.PAID },
        "Half Paid" to monthInvoices.count { it.status == InvoiceStatus.HALF_PAID },
        "Pending" to monthInvoices.count { it.status == InvoiceStatus.PENDING },
        "Overdue" to monthInvoices.count { it.status == InvoiceStatus.OVERDUE },
        "Cancelled" to monthInvoices.count { it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF }
    )
    val topClients = monthInvoices.filterNot {
        it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF
    }
        .groupBy { it.clientId }
        .map { (_, invoices) ->
            Triple(invoices.first().clientName, invoices.size, invoices.sumOf { it.grandTotal })
        }
        .sortedByDescending { it.third }
        .take(5)

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Reports",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier.testTag("reports_settings_button")
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
        modifier = modifier.testTag("reports_screen")
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

            // ===== MONTH SELECTOR =====
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (currentMonthIndex > 0) reportMonthKey = months[currentMonthIndex - 1]
                        },
                        enabled = currentMonthIndex > 0
                    ) {
                        Icon(
                            Icons.Filled.ArrowBackIosNew,
                            contentDescription = "Previous Month",
                            tint = EmeraldInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Text(
                        text = monthLabel,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = {
                            if (currentMonthIndex < months.lastIndex) reportMonthKey = months[currentMonthIndex + 1]
                        },
                        enabled = currentMonthIndex < months.lastIndex
                    ) {
                        Icon(
                            Icons.Filled.ArrowForwardIos,
                            contentDescription = "Next Month",
                            tint = EmeraldInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // ===== 2x2 STAT GRID =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatGridItem(
                    label = "Invoiced",
                    value = formatCurrency(invoicedAmount),
                    valueColor = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = "Collected",
                    value = formatCurrency(collectedAmount),
                    valueColor = SuccessGreen,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatGridItem(
                    label = "Outstanding",
                    value = formatCurrency(outstandingAmount),
                    valueColor = DangerRed,
                    modifier = Modifier.weight(1f)
                )
                StatGridItem(
                    label = "Total Invoices",
                    value = monthInvoices.size.toString(),
                    valueColor = EmeraldInk,
                    modifier = Modifier.weight(1f)
                )
            }

            // ===== STATUS BREAKDOWN =====
            SectionHeader(title = "STATUS BREAKDOWN")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BreakdownBarRow(label = "Paid", count = statusCounts.getValue("Paid"), total = monthInvoices.size, barColor = SuccessGreen)
                    BreakdownBarRow(label = "Half Paid", count = statusCounts.getValue("Half Paid"), total = monthInvoices.size, barColor = WarningAmber)
                    BreakdownBarRow(label = "Pending", count = statusCounts.getValue("Pending"), total = monthInvoices.size, barColor = NeutralGray)
                    BreakdownBarRow(label = "Overdue", count = statusCounts.getValue("Overdue"), total = monthInvoices.size, barColor = DangerRed)
                    BreakdownBarRow(label = "Cancelled", count = statusCounts.getValue("Cancelled"), total = monthInvoices.size, barColor = Color.LightGray)
                }
            }

            // ===== TOP CLIENTS =====
            SectionHeader(title = "TOP CLIENTS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    topClients.forEachIndexed { index, client ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldInk.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldInk
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = client.first,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${client.second} invoices",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(client.third),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // ===== EXPORT BUTTONS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WavesSecondaryButton(
                    text = "Export CSV",
                    icon = Icons.Filled.Download,
                    onClick = {
                        if (!isExporting) coroutineScope.launch {
                            isExporting = true
                            try {
                                val reportFile = withContext(Dispatchers.IO) {
                                    DocumentExports.createReportCsv(context, reportMonthKey, allInvoices)
                                }
                                val location = withContext(Dispatchers.IO) {
                                    DocumentExports.saveToDownloads(
                                        context,
                                        reportFile,
                                        "waves-report-$reportMonthKey.csv",
                                        "text/csv"
                                    )
                                }
                                showDemoToast(context, "CSV report saved to $location")
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                showDemoToast(context, exception.localizedMessage ?: "Unable to export CSV report.")
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                WavesSecondaryButton(
                    text = "Export PDF",
                    icon = Icons.Filled.PictureAsPdf,
                    onClick = {
                        if (!isExporting) coroutineScope.launch {
                            isExporting = true
                            try {
                                val reportFile: File = withContext(Dispatchers.IO) {
                                    DocumentExports.createReportPdf(context, reportMonthKey, allInvoices)
                                }
                                val location = withContext(Dispatchers.IO) {
                                    DocumentExports.saveToDownloads(
                                        context,
                                        reportFile,
                                        "waves-report-$reportMonthKey.pdf",
                                        "application/pdf"
                                    )
                                }
                                showDemoToast(context, "PDF report saved to $location")
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                showDemoToast(context, exception.localizedMessage ?: "Unable to export PDF report.")
                            } finally {
                                isExporting = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun formatCurrency(amount: Double) = "₹%,.0f".format(amount)

@Composable
private fun StatGridItem(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    WavesCard(modifier = modifier) {
        Column {
            Text(text = label, fontSize = 12.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

@Composable
private fun BreakdownBarRow(
    label: String,
    count: Int,
    total: Int,
    barColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$count",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFFF1F5F9))
        ) {
            if (count > 0 && total > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            fraction = (count.toFloat() / total.toFloat())
                                .coerceIn(0.04f, 1f)
                        )
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
        }
    }
}
