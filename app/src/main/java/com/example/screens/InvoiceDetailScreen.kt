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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StatusChip
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesSecondaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.data.InvoiceDisplayFormat
import com.example.data.InvoiceStatus
import com.example.data.BusinessProfile
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.DocumentExports
import com.example.data.PaymentRecord
import com.example.data.ReportDateUtils
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.WavesValidation
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val invoiceState by remember(invoiceId) { FirestoreDataRepository.observeInvoice(invoiceId) }
        .collectAsState(initial = FirestoreState.Loading)
    var showPaymentSheet by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }
    var showWriteOffConfirmation by remember { mutableStateOf(false) }
    var showCancelConfirmation by remember { mutableStateOf(false) }
    var isExportingPdf by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val invoice = when (val state = invoiceState) {
        FirestoreState.Loading -> {
            com.example.components.StateScreen(type = com.example.components.StateType.LOADING, message = "Loading invoice...")
            return
        }
        is FirestoreState.Failure -> {
            com.example.components.StateScreen(type = com.example.components.StateType.ERROR, title = "Invoice Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value ?: run {
            com.example.components.StateScreen(type = com.example.components.StateType.ERROR, title = "Invoice Not Found", message = "This invoice may have been deleted.")
            return
        }
    }

    val balanceDue = invoice.balanceDue

    // Currency display follows the invoice's own billing currency (chosen on the
    // creation page); invoices created before that feature follow the business
    // country, matching the PDF exports.
    val businessState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val businessCountry = ((businessState as? FirestoreState.Data<*>)?.value as? BusinessProfile)
        ?.country.orEmpty().ifBlank { "India" }
    val invoiceCurrencyCountry = invoice.currencyCountry.ifBlank { businessCountry }
    fun fmt(amount: Double): String = InvoiceDisplayFormat.formatCurrency(amount, invoiceCurrencyCountry)

    fun updateInvoiceStatus(status: InvoiceStatus) {
        coroutineScope.launch {
            try {
                FirestoreDataRepository.updateInvoiceStatus(invoice.id, status)
            } catch (exception: Exception) {
                showDemoToast(context, exception.localizedMessage ?: "Unable to update invoice.")
            }
        }
    }

    fun shareInvoicePdf() {
        coroutineScope.launch {
            try {
                val pdf = withContext(Dispatchers.IO) {
                    val business = FirestoreDataRepository.getBusinessProfile()
                    val client = FirestoreDataRepository.getClient(invoice.clientId)
                    DocumentExports.createInvoicePdf(context, invoice, business, client)
                }
                DocumentExports.share(context, pdf, "application/pdf", "Share invoice ${invoice.id}")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showDemoToast(context, exception.localizedMessage ?: "Unable to share invoice PDF.")
            }
        }
    }

    fun downloadInvoicePdf() {
        if (isExportingPdf) return
        coroutineScope.launch {
            isExportingPdf = true
            var pdf: File? = null
            try {
                pdf = withContext(Dispatchers.IO) {
                    val business = FirestoreDataRepository.getBusinessProfile()
                    val client = FirestoreDataRepository.getClient(invoice.clientId)
                    DocumentExports.createInvoicePdf(context, invoice, business, client)
                }
                val location = withContext(Dispatchers.IO) {
                    DocumentExports.saveToDownloads(
                        context,
                        pdf,
                        "invoice-${invoice.id}.pdf",
                        "application/pdf"
                    )
                }
                // saveToDownloads moves the temporary file into Downloads; nothing left to clean.
                pdf = null
                showDemoToast(context, "Invoice PDF saved to $location", android.widget.Toast.LENGTH_LONG)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showDemoToast(context, exception.localizedMessage ?: "Unable to download invoice PDF.")
            } finally {
                pdf?.let { leftover -> runCatching { leftover.delete() } }
                isExportingPdf = false
            }
        }
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = invoice.id,
                onBackClick = onNavigateBack,
                actions = {
                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("invoice_detail_menu_button")
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
                                text = { Text("Preview PDF") },
                                onClick = {
                                    menuExpanded = false
                                    onNavigateToPreview(invoice.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Delete Invoice") },
                                onClick = {
                                    menuExpanded = false
                                    showDeleteConfirmation = true
                                }
                            )
                        }
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("invoice_detail_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // ===== STATUS CARD =====
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = fmt(invoice.grandTotal),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        if (balanceDue > 0) {
                            Text(
                                text = "${fmt(balanceDue)} balance due",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DangerRed
                            )
                        } else {
                            Text(
                                text = "Fully paid on time",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SuccessGreen
                            )
                        }
                    }

                    StatusChip(status = invoice.status)
                }
            }

            // ===== CLIENT =====
            SectionHeader(title = "CLIENT")
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldInk.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = EmeraldInk,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = invoice.clientName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = invoice.clientEmail ?: "Client Email: N/A",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // ===== DATES =====
            SectionHeader(title = "DATES")
            WavesCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "Issue Date", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = invoice.issueDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "Due Date", fontSize = 12.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = invoice.dueDate,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }

            // ===== ITEMS =====
            SectionHeader(title = "ITEMS (${invoice.items.size})")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    invoice.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    "${item.quantity.toInt()} × ${
                                        fmt(item.unitPrice)
                                    }",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                fmt(item.total),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }

            // ===== PAYMENT SUMMARY =====
            SectionHeader(title = "PAYMENT SUMMARY")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 13.sp, color = TextSecondary)
                        Text(
                            fmt(invoice.subtotal),
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tax (${invoice.items.firstOrNull()?.taxRate?.toInt() ?: 18}%)",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Text(
                            fmt(invoice.taxAmount),
                            fontSize = 13.sp,
                            color = TextPrimary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Paid so far", fontSize = 13.sp, color = SuccessGreen)
                        Text(
                            "-${fmt(invoice.paidAmount)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SuccessGreen
                        )
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        color = BorderGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Balance Due",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            fmt(balanceDue),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (balanceDue > 0) DangerRed else SuccessGreen
                        )
                    }
                }
            }

            // ===== ACTIONS =====
            SectionHeader(title = "ACTIONS")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Payment actions only make sense while there is an outstanding balance.
                if (balanceDue > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButtonItem(
                            text = "Record Payment",
                            icon = Icons.Filled.Payment,
                            isPrimary = true,
                            onClick = { showPaymentSheet = true },
                            modifier = Modifier.weight(1f)
                        )
                        ActionButtonItem(
                            text = "Mark Paid",
                            icon = Icons.Filled.CheckCircle,
                            isPrimary = false,
                            onClick = { showMarkPaidDialog = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ActionButtonItem(
                        text = if (isExportingPdf) "Downloading..." else "Download PDF",
                        icon = Icons.Filled.Download,
                        isPrimary = false,
                        onClick = ::downloadInvoicePdf,
                        modifier = Modifier.weight(1f)
                    )
                    ActionButtonItem(
                        text = "Share",
                        icon = Icons.Filled.Share,
                        isPrimary = false,
                        onClick = ::shareInvoicePdf,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Write-off and cancellation are final states; hide them once applied.
                if (invoice.status != InvoiceStatus.CANCELLED && invoice.status != InvoiceStatus.WRITTEN_OFF) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ActionButtonItem(
                            text = "Write Off",
                            icon = Icons.Filled.Cancel,
                            isPrimary = false,
                            isDestructive = true,
                            onClick = { showWriteOffConfirmation = true },
                            modifier = Modifier.weight(1f)
                        )
                        ActionButtonItem(
                            text = "Cancel Invoice",
                            icon = Icons.Filled.Cancel,
                            isPrimary = false,
                            isDestructive = true,
                            onClick = { showCancelConfirmation = true },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                ActionButtonItem(
                    text = "Delete",
                    icon = Icons.Filled.Delete,
                    isPrimary = false,
                    isDestructive = true,
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete this invoice?") },
                text = {
                    Text(
                        "Invoice ${invoice.id} and its payment history will be permanently removed. " +
                            "This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            coroutineScope.launch {
                                try {
                                    FirestoreDataRepository.deleteInvoice(invoice.id)
                                    onNavigateBack()
                                } catch (exception: Exception) {
                                    showDemoToast(context, exception.localizedMessage ?: "Unable to delete invoice.")
                                }
                            }
                        }
                    ) {
                        Text("Delete", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showPaymentSheet) {
            RecordPaymentBottomSheet(
                balanceDue = balanceDue,
                sheetState = sheetState,
                onDismissRequest = { showPaymentSheet = false },
                onPaymentSaved = { amount, method, reference ->
                    FirestoreDataRepository.recordPayment(
                        invoice.id,
                        PaymentRecord(
                            id = "payment_${System.currentTimeMillis()}",
                            amount = amount,
                            date = ReportDateUtils.displayDate(ReportDateUtils.currentDate()),
                            method = method,
                            reference = reference,
                            notes = ""
                        )
                    )
                }
            )
        }

        if (showMarkPaidDialog) {
            MarkPaidDialog(
                balanceDue = balanceDue,
                formatAmount = { fmt(it) },
                onDismissRequest = { showMarkPaidDialog = false },
                onPaymentConfirmed = { amount, paymentDate ->
                    FirestoreDataRepository.recordPayment(
                        invoice.id,
                        PaymentRecord(
                            id = "payment_${UUID.randomUUID()}",
                            amount = amount,
                            date = paymentDate,
                            method = "Manual",
                            reference = "Marked as paid in app",
                            notes = ""
                        )
                    )
                }
            )
        }

        if (showWriteOffConfirmation) {
            AlertDialog(
                onDismissRequest = { showWriteOffConfirmation = false },
                title = { Text("Write off this invoice?") },
                text = {
                    Text(
                        "Invoice ${invoice.id} will be marked as written off" +
                            (if (balanceDue > 0) " and its outstanding balance of ${fmt(balanceDue)} will be cleared" else "") +
                            ". This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showWriteOffConfirmation = false
                            updateInvoiceStatus(InvoiceStatus.WRITTEN_OFF)
                        }
                    ) {
                        Text("Write Off", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showWriteOffConfirmation = false }) {
                        Text("Keep Invoice")
                    }
                }
            )
        }

        if (showCancelConfirmation) {
            AlertDialog(
                onDismissRequest = { showCancelConfirmation = false },
                title = { Text("Cancel this invoice?") },
                text = {
                    Text(
                        "Invoice ${invoice.id} will be cancelled and any outstanding balance " +
                            "will no longer be collectible. This cannot be undone."
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showCancelConfirmation = false
                            updateInvoiceStatus(InvoiceStatus.CANCELLED)
                        }
                    ) {
                        Text("Cancel Invoice", color = DangerRed, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelConfirmation = false }) {
                        Text("Keep Invoice")
                    }
                }
            )
        }
    }
}

@Composable
private fun ActionButtonItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    isDestructive: Boolean = false
) {
    if (isPrimary) {
        WavesPrimaryButton(
            text = text,
            icon = icon,
            onClick = onClick,
            modifier = modifier.height(48.dp)
        )
    } else {
        WavesSecondaryButton(
            text = text,
            icon = icon,
            isDestructive = isDestructive,
            onClick = onClick,
            modifier = modifier.height(48.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecordPaymentBottomSheet(
    balanceDue: Double,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    onPaymentSaved: suspend (amount: Double, method: String, reference: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var amountStr by remember {
        mutableStateOf(if (balanceDue > 0) balanceDue.toString() else "")
    }
    var selectedMethod by remember { mutableStateOf("Bank") }
    var reference by remember { mutableStateOf("") }
    var isSavingPayment by remember { mutableStateOf(false) }
    var paymentError by remember { mutableStateOf("") }
    val methods = listOf("Cash", "Bank", "UPI", "Card", "Other")
    var hasAttemptedSave by remember { mutableStateOf(false) }

    // Inline validation errors (validate-on-save, surfaced via WavesTextField errorMessage)
    val parsedAmountValue = amountStr.trim().replace(",", "").toDoubleOrNull()
    val amountError = if (hasAttemptedSave) {
        WavesValidation.firstError(
            WavesValidation.amount(amountStr, "Amount"),
            if (parsedAmountValue != null && parsedAmountValue > balanceDue) {
                "Amount exceeds the outstanding balance"
            } else null
        )
    } else null
    val referenceError = if (hasAttemptedSave && reference.trim().length > 200) {
        "Reference is too long (max 200 characters)"
    } else null

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Record Payment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            WavesTextField(
                value = amountStr,
                onValueChange = { amountStr = it; paymentError = "" },
                label = "Amount",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                errorMessage = amountError
            )

            Column {
                Text(
                    "Method",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    methods.forEach { method ->
                        val selected = selectedMethod == method
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (selected) EmeraldInk else SurfaceColor)
                                .clickable { selectedMethod = method },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                method,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) OnPrimary else TextSecondary
                            )
                        }
                    }
                }
            }

            WavesTextField(
                value = reference,
                onValueChange = { reference = it },
                label = "Reference / Txn ID",
                errorMessage = referenceError
            )
            if (paymentError.isNotBlank()) {
                Text(paymentError, color = DangerRed, fontSize = 13.sp)
            }

            WavesPrimaryButton(
                text = if (isSavingPayment) "SAVING..." else "SAVE PAYMENT",
                onClick = {
                    hasAttemptedSave = true
                    val amount = amountStr.trim().replace(",", "").toDoubleOrNull()
                    when {
                        // Inline field errors are shown on the inputs; block the save.
                        amountError != null || referenceError != null -> Unit
                        amount == null || amount <= 0.0 ->
                            paymentError = "Enter a payment amount greater than zero."
                        !isSavingPayment -> coroutineScope.launch {
                            isSavingPayment = true
                            paymentError = ""
                            try {
                                onPaymentSaved(amount, selectedMethod, reference)
                                showDemoToast(context, "Payment saved")
                                onDismissRequest()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                paymentError = exception.localizedMessage ?: "Unable to save payment."
                            } finally {
                                isSavingPayment = false
                            }
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * "Mark Paid" confirmation dialog. Asks for the payment date and whether the
 * invoice is settled in full or with a custom amount, then records the payment
 * through the same transactional path as the payment sheet so the payment
 * history and the invoice status stay consistent.
 */
@Composable
private fun MarkPaidDialog(
    balanceDue: Double,
    formatAmount: (Double) -> String,
    onDismissRequest: () -> Unit,
    onPaymentConfirmed: suspend (amount: Double, paymentDate: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val modes = listOf("Paid in Full", "Custom Paid")
    var paymentMode by remember { mutableStateOf(modes.first()) }
    var amountText by remember { mutableStateOf("") }
    var dateText by remember {
        mutableStateOf(ReportDateUtils.displayDate(ReportDateUtils.currentDate()))
    }
    var isSaving by remember { mutableStateOf(false) }
    var hasAttemptedSave by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf("") }

    val isCustomMode = paymentMode == "Custom Paid"
    val parsedAmount = amountText.trim().replace(",", "").toDoubleOrNull()

    val amountError = if (hasAttemptedSave && isCustomMode) {
        WavesValidation.firstError(
            WavesValidation.amount(amountText, "Amount"),
            if (parsedAmount != null && parsedAmount > balanceDue) {
                "Amount exceeds the outstanding balance"
            } else null
        )
    } else null
    val dateError = if (hasAttemptedSave) {
        WavesValidation.dateText(dateText, "Date of payment")
    } else null

    // Live "balance left" preview for the custom amount mode.
    val balanceLeft = when {
        !isCustomMode -> 0.0
        parsedAmount == null -> balanceDue
        else -> (balanceDue - parsedAmount).coerceAtLeast(0.0)
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismissRequest() },
        title = { Text("Mark as Paid") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Outstanding balance: ${formatAmount(balanceDue)}",
                    fontSize = 13.sp,
                    color = TextSecondary
                )

                Column {
                    Text(
                        "Payment amount",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        modes.forEach { mode ->
                            val selected = paymentMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (selected) EmeraldInk else SurfaceColor)
                                    .clickable { paymentMode = mode },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    mode,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selected) OnPrimary else TextSecondary
                                )
                            }
                        }
                    }
                }

                if (isCustomMode) {
                    WavesTextField(
                        value = amountText,
                        onValueChange = { amountText = it; saveError = "" },
                        label = "Amount received",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        errorMessage = amountError
                    )
                    Text(
                        text = "Balance left: ${formatAmount(balanceLeft)}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (balanceLeft > 0.0) DangerRed else SuccessGreen
                    )
                } else {
                    Text(
                        "The full outstanding balance of ${formatAmount(balanceDue)} will be recorded as paid.",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }

                WavesTextField(
                    value = dateText,
                    onValueChange = { dateText = it; saveError = "" },
                    label = "Date of payment",
                    placeholder = "dd MMM yyyy",
                    errorMessage = dateError
                )

                if (saveError.isNotBlank()) {
                    Text(saveError, color = DangerRed, fontSize = 13.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving,
                onClick = {
                    hasAttemptedSave = true
                    val paymentDate = ReportDateUtils.parse(dateText)?.let(ReportDateUtils::displayDate)
                    val amount = if (isCustomMode) parsedAmount else balanceDue
                    when {
                        dateError != null || amountError != null -> Unit
                        paymentDate == null -> saveError = "Enter a valid date (dd MMM yyyy)"
                        amount == null || amount <= 0.0 ->
                            saveError = "Enter a payment amount greater than zero."
                        amount > balanceDue -> saveError = "Amount exceeds the outstanding balance"
                        !isSaving -> coroutineScope.launch {
                            isSaving = true
                            saveError = ""
                            try {
                                onPaymentConfirmed(amount, paymentDate)
                                showDemoToast(
                                    context,
                                    if (amount >= balanceDue) {
                                        "Invoice marked as paid"
                                    } else {
                                        "Payment recorded. Balance left: " +
                                            formatAmount((balanceDue - amount).coerceAtLeast(0.0))
                                    },
                                    android.widget.Toast.LENGTH_LONG
                                )
                                onDismissRequest()
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                saveError = exception.localizedMessage ?: "Unable to record the payment."
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                }
            ) {
                Text(
                    if (isSaving) "SAVING..." else "Confirm",
                    color = EmeraldInk,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(enabled = !isSaving, onClick = onDismissRequest) {
                Text("Cancel")
            }
        }
    )
}
