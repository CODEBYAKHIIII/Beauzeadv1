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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StatusChip
import com.example.components.WavesCard
import com.example.components.WavesChip
import com.example.components.WavesFAB
import com.example.components.WavesHeader
import com.example.components.showDemoToast
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.DocumentExports
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ClientDetailScreen(
    clientId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEditClient: (String) -> Unit,
    onNavigateToCreateInvoice: () -> Unit,
    onNavigateToInvoiceDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clientsState by remember { FirestoreDataRepository.observeClients() }
        .collectAsState(initial = FirestoreState.Loading)
    val invoicesState by remember { FirestoreDataRepository.observeInvoices() }
        .collectAsState(initial = FirestoreState.Loading)
    val clientData = (clientsState as? FirestoreState.Data)?.value
    val client = clientData?.find { it.id == clientId }
    val clientInvoices = (invoicesState as? FirestoreState.Data)?.value.orEmpty()
        .filter { it.clientId == clientId }

    if (clientsState is FirestoreState.Loading || invoicesState is FirestoreState.Loading) {
        StateScreen(type = StateType.LOADING, message = "Loading client...")
        return
    }
    val loadError = (clientsState as? FirestoreState.Failure)?.message
        ?: (invoicesState as? FirestoreState.Failure)?.message
    if (loadError != null) {
        StateScreen(type = StateType.ERROR, title = "Client Error", message = loadError)
        return
    }
    if (client == null) {
        StateScreen(
            type = StateType.ERROR,
            title = "Client Not Found",
            message = "This client may have been deleted.",
            onPrimaryClick = onNavigateBack
        )
        return
    }

    var selectedTab by remember { mutableStateOf("Invoices") }
    var menuExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            WavesHeader(
                title = client.name,
                onBackClick = onNavigateBack,
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("client_detail_menu_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MoreVert,
                                contentDescription = "Menu",
                                tint = OnPrimary
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Edit Client") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToEditClient(client.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share Statement") },
                                onClick = {
                                    menuExpanded = false
                                    coroutineScope.launch {
                                        try {
                                            val pdf = withContext(Dispatchers.IO) {
                                                DocumentExports.createClientStatementPdf(
                                                    context,
                                                    client,
                                                    clientInvoices
                                                )
                                            }
                                            DocumentExports.share(
                                                context,
                                                pdf,
                                                "application/pdf",
                                                "Share ${client.name}'s statement"
                                            )
                                        } catch (exception: CancellationException) {
                                            throw exception
                                        } catch (exception: Exception) {
                                            showDemoToast(
                                                context,
                                                exception.localizedMessage ?: "Unable to share client statement."
                                            )
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (client.isArchived) "Restore Client" else "Archive Client") },
                                onClick = {
                                    menuExpanded = false
                                    coroutineScope.launch {
                                        try {
                                            FirestoreDataRepository.archiveClient(
                                                client.id,
                                                archived = !client.isArchived
                                            )
                                            onNavigateBack()
                                        } catch (exception: Exception) {
                                            showDemoToast(context, exception.localizedMessage ?: "Unable to archive client.")
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            WavesFAB(
                onClick = onNavigateToCreateInvoice,
                contentDescription = "New Invoice"
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("client_detail_screen")
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

            // ===== PROFILE CARD =====
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(EmeraldInk.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = EmeraldInk,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = client.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Email,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = client.email ?: "No email provided",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Phone,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = client.phone ?: "No phone",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = listOfNotNull(
                                    client.city.takeIf { it.isNotBlank() },
                                    client.country.takeIf { it.isNotBlank() }
                                ).joinToString(", ").ifBlank { "No address" },
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ===== STATS ROW =====
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Billed", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(client.totalBilled),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(BackgroundColor)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Paid", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(client.totalPaid),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }

                    Box(
                        modifier = Modifier
                            .height(32.dp)
                            .width(1.dp)
                            .background(BackgroundColor)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "Due", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatCurrency(client.totalDue),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (client.totalDue > 0) DangerRed else SuccessGreen
                        )
                    }
                }
            }

            // ===== TABS =====
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Invoices", "Payments", "Notes").forEach { tab ->
                    Box(modifier = Modifier.weight(1f)) {
                        WavesChip(
                            text = tab,
                            isSelected = selectedTab == tab,
                            onClick = { selectedTab = tab }
                        )
                    }
                }
            }

            // ===== TAB CONTENT =====
            when (selectedTab) {
                "Invoices" -> {
                    if (clientInvoices.isEmpty()) {
                        EmptyTabMessage(
                            title = "No invoices yet",
                            message = "Tap + to create the first invoice for ${client.name}."
                        )
                    } else {
                        clientInvoices.forEach { invoice ->
                            WavesCard(
                                onClick = { onNavigateToInvoiceDetail(invoice.id) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = invoice.id,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "Due ${invoice.dueDate}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = formatCurrency(invoice.grandTotal),
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                    }
                                    StatusChip(status = invoice.status)
                                }
                            }
                        }
                    }
                }

                "Payments" -> {
                    val allPayments = clientInvoices.flatMap { it.payments }
                    if (allPayments.isEmpty()) {
                        EmptyTabMessage(
                            title = "No payments yet",
                            message = "Payments recorded against invoices will appear here."
                        )
                    } else {
                        allPayments.forEach { payment ->
                            WavesCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = payment.method,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = "${payment.date} · Ref: ${payment.reference.ifBlank { "—" }}",
                                            fontSize = 12.sp,
                                            color = TextSecondary
                                        )
                                        if (payment.notes.isNotBlank()) {
                                            Text(
                                                text = payment.notes,
                                                fontSize = 12.sp,
                                                color = TextSecondary
                                            )
                                        }
                                    }
                                    Text(
                                        text = "+${formatCurrency(payment.amount)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SuccessGreen
                                    )
                                }
                            }
                        }
                    }
                }

                "Notes" -> {
                    WavesCard {
                        Column {
                            Text(
                                text = "Client Notes",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = client.notes?.takeIf { it.isNotBlank() }
                                    ?: "No notes added for this client.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tax ID: ${
                                    client.taxNumber?.takeIf { it.isNotBlank() }
                                        ?: "Not provided"
                                }",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(88.dp))
        }
    }
}

private fun formatCurrency(amount: Double) = "₹%,.0f".format(amount)

@Composable
private fun EmptyTabMessage(title: String, message: String) {
    WavesCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = TextSecondary,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}