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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.Invoice
import com.example.data.InvoiceStatus
import com.example.data.Product
import com.example.data.ReportDateUtils
import com.example.data.SampleData
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesSecondaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.data.InvoiceItem
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.BorderGray
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun CreateInvoiceScreen(
    onNavigateBack: () -> Unit,
    onNavigateToPreview: (String) -> Unit,
    onNavigateToAddClient: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var invoiceNumber by remember { mutableStateOf("") }
    var issueDate by remember { mutableStateOf(ReportDateUtils.displayDate(ReportDateUtils.currentDate())) }
    var dueDate by remember { mutableStateOf(ReportDateUtils.displayDate(ReportDateUtils.dateAfterDays(ReportDateUtils.currentDate(), 15))) }

    var isSaving by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val clientState by remember { FirestoreDataRepository.observeClients() }
        .collectAsState(initial = FirestoreState.Loading)
    val productState by remember { FirestoreDataRepository.observeProducts() }
        .collectAsState(initial = FirestoreState.Loading)
    val businessState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val clients = (clientState as? FirestoreState.Data)?.value.orEmpty()
    val products = (productState as? FirestoreState.Data)?.value.orEmpty().filterNot { it.isArchived }
    val business = (businessState as? FirestoreState.Data)?.value

    LaunchedEffect(Unit) {
        try {
            invoiceNumber = FirestoreDataRepository.getNextInvoiceNumber()
        } catch (exception: Exception) {
            errorMessage = exception.localizedMessage ?: "Unable to prepare invoice number."
        }
    }

    if (isSaving) {
        StateScreen(
            type = StateType.LOADING,
            message = "Generating and saving invoice..."
        )
        return
    }

    if (showSuccess) {
        StateScreen(
            type = StateType.SUCCESS,
            title = "Invoice Created!",
            message = "Invoice $invoiceNumber was generated successfully.",
            primaryButtonText = "VIEW PREVIEW",
            onPrimaryClick = {
                showSuccess = false
                onNavigateToPreview(invoiceNumber)
            }
        )
        return
    }

    var selectedClient by remember { mutableStateOf<com.example.data.Client?>(null) }
    var clientDropdownOpen by remember { mutableStateOf(false) }

    val items = remember { mutableStateListOf<InvoiceItem>() }

    var discountStr by remember { mutableStateOf("0") }
    var notes by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf("") }
    var productPickerOpen by remember { mutableStateOf(false) }
    var itemEditorOpen by remember { mutableStateOf(false) }
    var editingItemIndex by remember { mutableStateOf<Int?>(null) }
    var itemName by remember { mutableStateOf("") }
    var itemQuantity by remember { mutableStateOf("1") }
    var itemUnitPrice by remember { mutableStateOf("") }
    var itemTaxRate by remember { mutableStateOf(business?.defaultTaxRate?.toString() ?: "18") }

    LaunchedEffect(business) {
        if (business != null) {
            if (notes.isBlank()) notes = business.defaultNotes
            if (terms.isBlank()) terms = business.paymentTerms
        }
    }

    val subtotal = items.sumOf { it.subtotal }
    val discount = discountStr.toDoubleOrNull() ?: 0.0
    val taxAmount = items.sumOf { it.taxAmount }
    val grandTotal = (subtotal - discount).coerceAtLeast(0.0) + taxAmount

    fun openItemEditor(index: Int? = null) {
        editingItemIndex = index
        val item = index?.let(items::getOrNull)
        itemName = item?.name.orEmpty()
        itemQuantity = item?.quantity?.toString() ?: "1"
        itemUnitPrice = item?.unitPrice?.takeIf { it > 0.0 }?.toString().orEmpty()
        itemTaxRate = item?.taxRate?.toString() ?: business?.defaultTaxRate?.toString() ?: "18"
        itemEditorOpen = true
    }

    fun saveItemEditor() {
        val quantity = itemQuantity.toIntOrNull()
        val unitPrice = itemUnitPrice.toDoubleOrNull()
        val taxRate = itemTaxRate.toDoubleOrNull()
        if (itemName.isBlank() || quantity == null || quantity < 1 || unitPrice == null || unitPrice < 0.0 || taxRate == null || taxRate < 0.0) {
            showDemoToast(context, "Enter a name, valid quantity, price, and tax rate.")
            return
        }
        val item = InvoiceItem(
            id = editingItemIndex?.let(items::getOrNull)?.id ?: "item_${System.currentTimeMillis()}",
            name = itemName.trim(),
            quantity = quantity,
            unitPrice = unitPrice,
            taxRate = taxRate
        )
        val index = editingItemIndex
        if (index == null) items.add(item) else items[index] = item
        itemEditorOpen = false
    }

    val isLoadingData = clientState is FirestoreState.Loading ||
        productState is FirestoreState.Loading || businessState is FirestoreState.Loading
    val dataError = (clientState as? FirestoreState.Failure)?.message
        ?: (productState as? FirestoreState.Failure)?.message
        ?: (businessState as? FirestoreState.Failure)?.message

    if (isLoadingData || isSaving) {
        StateScreen(
            type = StateType.LOADING,
            message = if (isLoadingData) "Loading invoice data..." else "Saving invoice..."
        )
        return
    }
    if (dataError != null || errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Invoice Error",
            message = errorMessage.ifBlank { dataError.orEmpty() },
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    fun saveInvoice() {
        val client = selectedClient
        if (client == null) {
            errorMessage = "Select a client before saving the invoice."
            return
        }
        if (items.isEmpty()) {
            errorMessage = "Add at least one product or service."
            return
        }
        val issueDateValue = ReportDateUtils.parse(issueDate)
        val dueDateValue = ReportDateUtils.parse(dueDate)
        if (issueDateValue == null || dueDateValue == null) {
            errorMessage = "Enter dates as day month year, for example 04 Oct 2026."
            return
        }
        if (dueDateValue.before(issueDateValue)) {
            errorMessage = "Due date cannot be before the issue date."
            return
        }
        if (discount < 0.0 || discount > subtotal) {
            errorMessage = "Discount cannot exceed the subtotal."
            return
        }

        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                invoiceNumber = FirestoreDataRepository.createInvoice(
                    Invoice(
                        id = invoiceNumber,
                        clientId = client.id,
                        clientName = client.name,
                        clientEmail = client.email,
                        issueDate = ReportDateUtils.displayDate(issueDateValue),
                        dueDate = ReportDateUtils.displayDate(dueDateValue),
                        items = items.toList(),
                        discount = discount,
                        status = InvoiceStatus.PENDING,
                        paidAmount = 0.0,
                        notes = notes,
                        terms = terms
                    )
                )
                showSuccess = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save invoice."
            } finally {
                isSaving = false
            }
        }
    }

    // Compute average tax rate for display (since different items may differ)
    val averageTaxRate = if (items.isEmpty()) 0.0
        else items.map { it.taxRate }.average()

    Scaffold(
        topBar = {
            WavesHeader(
                title = "New Invoice",
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveInvoice,
                        modifier = Modifier.testTag("invoice_save_header_button")
                    ) {
                        Text(
                            "Save",
                            color = OnPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("create_invoice_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // ===== CLIENT DETAILS =====
            SectionHeader(title = "CLIENT DETAILS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedClient != null) "Bill To" else "Select Client *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Text(
                            text = "+ Add New Client",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldInk,
                            modifier = Modifier.clickable { onNavigateToAddClient() }
                        )
                    }

                    Box {
                        OutlinedTextField(
                            value = selectedClient?.let { "${it.name} (${it.city})" }
                                ?: "Choose Client",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { clientDropdownOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = EmeraldInk
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { clientDropdownOpen = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = InputBorderGray
                            )
                        )

                        DropdownMenu(
                            expanded = clientDropdownOpen,
                            onDismissRequest = { clientDropdownOpen = false }
                        ) {
                            clients.forEach { client ->
                                DropdownMenuItem(
                                    text = { Text("${client.name} — ${client.city}") },
                                    onClick = {
                                        selectedClient = client
                                        clientDropdownOpen = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ===== INVOICE DETAILS =====
            SectionHeader(title = "INVOICE DETAILS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = invoiceNumber,
                        onValueChange = { invoiceNumber = it },
                        label = "Invoice Number"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WavesTextField(
                            value = issueDate,
                            onValueChange = { issueDate = it },
                            label = "Issue Date",
                            modifier = Modifier.weight(1f)
                        )
                        WavesTextField(
                            value = dueDate,
                            onValueChange = { dueDate = it },
                            label = "Due Date",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ===== ITEMS & SERVICES =====
            SectionHeader(title = "ITEMS & SERVICES")
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.forEachIndexed { index, item ->
                    WavesCard {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                Row {
                                    IconButton(
                                        onClick = { openItemEditor(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "Edit item",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (items.size > 1) {
                                                items.removeAt(index)
                                            } else {
                                                showDemoToast(
                                                    context,
                                                    "Invoice must have at least one item"
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Delete item",
                                            tint = DangerRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${item.quantity.toInt()} × ${
                                        SampleData.formatCurrency(item.unitPrice)
                                    } (Tax: ${item.taxRate.toInt()}%)",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                                Text(
                                    text = SampleData.formatCurrency(item.total),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        WavesSecondaryButton(
                            text = "+ From Products",
                            onClick = {
                                if (products.isEmpty()) {
                                    showDemoToast(context, "Add a product or service first.")
                                } else {
                                    productPickerOpen = true
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = productPickerOpen,
                            onDismissRequest = { productPickerOpen = false }
                        ) {
                            products.forEach { product ->
                                DropdownMenuItem(
                                    text = { Text(product.name) },
                                    onClick = {
                                        items.add(
                                            InvoiceItem(
                                                id = "item_${System.currentTimeMillis()}",
                                                name = product.name,
                                                quantity = product.defaultQuantity,
                                                unitPrice = product.unitPrice,
                                                taxRate = product.taxRate
                                            )
                                        )
                                        productPickerOpen = false
                                    }
                                )
                            }
                        }
                    }

                    WavesSecondaryButton(
                        text = "+ Custom Item",
                        onClick = { openItemEditor() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // ===== SUMMARY =====
            SectionHeader(title = "SUMMARY")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", fontSize = 14.sp, color = TextSecondary)
                        Text(
                            SampleData.formatCurrency(subtotal),
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Discount (₹)", fontSize = 14.sp, color = TextSecondary)
                        Box(modifier = Modifier.width(100.dp)) {
                            OutlinedTextField(
                                value = discountStr,
                                onValueChange = { discountStr = it },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = SurfaceColor,
                                    unfocusedContainerColor = SurfaceColor,
                                    focusedBorderColor = AccentCyan,
                                    unfocusedBorderColor = InputBorderGray
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Tax (avg ${averageTaxRate.toInt()}%)",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                        Text(
                            SampleData.formatCurrency(taxAmount),
                            fontSize = 14.sp,
                            color = TextPrimary
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
                            "Grand Total",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            SampleData.formatCurrency(grandTotal),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldInk
                        )
                    }
                }
            }

            // ===== NOTES & TERMS =====
            SectionHeader(title = "NOTES & TERMS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = "Notes for Client",
                        singleLine = false,
                        maxLines = 3
                    )
                    WavesTextField(
                        value = terms,
                        onValueChange = { terms = it },
                        label = "Payment Terms",
                        singleLine = false,
                        maxLines = 3
                    )
                }
            }

            // ===== PREVIEW & SAVE =====
            WavesPrimaryButton(
                text = "PREVIEW & SAVE",
                onClick = ::saveInvoice
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (itemEditorOpen) {
        AlertDialog(
            onDismissRequest = { itemEditorOpen = false },
            title = { Text(if (editingItemIndex == null) "Custom item" else "Edit invoice item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    WavesTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = "Item name"
                    )
                    WavesTextField(
                        value = itemQuantity,
                        onValueChange = { itemQuantity = it },
                        label = "Quantity",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    WavesTextField(
                        value = itemUnitPrice,
                        onValueChange = { itemUnitPrice = it },
                        label = "Unit price",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    WavesTextField(
                        value = itemTaxRate,
                        onValueChange = { itemTaxRate = it },
                        label = "Tax rate %",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = ::saveItemEditor) {
                    Text(if (editingItemIndex == null) "Add" else "Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { itemEditorOpen = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
