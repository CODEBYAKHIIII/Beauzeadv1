package com.example.screens

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesSecondaryButton
import com.example.components.WavesTextField
import com.example.data.FirestoreDataRepository
import com.example.data.Product
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AddEditProductScreen(
    productId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = productId != null && productId != "new"
    var existingProduct by remember { mutableStateOf<Product?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var isLoadingExisting by remember { mutableStateOf(isEditMode) }
    var showSuccess by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }

    var unitPrice by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("pcs") }
    var unitDropdownOpen by remember { mutableStateOf(false) }
    val units = listOf("pcs", "hrs", "kg", "yr", "mo", "day")

    var defaultQuantity by remember { mutableStateOf("1") }
    var taxRate by remember { mutableStateOf("18") }

    LaunchedEffect(productId) {
        if (isEditMode) {
            try {
                existingProduct = FirestoreDataRepository.getProduct(productId!!)
                    ?: error("Product not found.")
                existingProduct?.let { product ->
                    name = product.name
                    description = product.description
                    sku = product.sku
                    unitPrice = product.unitPrice.toString()
                    unit = product.unit
                    defaultQuantity = product.defaultQuantity.toString()
                    taxRate = product.taxRate.toString()
                }
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to load product."
            } finally {
                isLoadingExisting = false
            }
        }
    }

    fun saveProduct() {
        hasSubmitted = true
        val parsedPrice = unitPrice.toDoubleOrNull()
        if (name.isBlank() || parsedPrice == null || parsedPrice < 0.0) return
        val parsedQuantity = defaultQuantity.toIntOrNull()
        val parsedTax = taxRate.toDoubleOrNull()
        if (parsedQuantity == null || parsedQuantity < 1 || parsedTax == null || parsedTax < 0.0) {
            errorMessage = "Enter a valid quantity and tax rate."
            return
        }
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveProduct(
                    Product(
                        id = if (isEditMode) productId!! else "",
                        name = name.trim(),
                        description = description.trim(),
                        sku = sku.trim(),
                        unitPrice = parsedPrice,
                        unit = unit,
                        defaultQuantity = parsedQuantity,
                        taxRate = parsedTax,
                        isArchived = existingProduct?.isArchived ?: false
                    )
                )
                showSuccess = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save product."
            } finally {
                isSaving = false
            }
        }
    }

    if (isLoadingExisting || isSaving) {
        StateScreen(
            type = StateType.LOADING,
            message = if (isLoadingExisting) "Loading product details..." else "Saving product details..."
        )
        return
    }

    if (errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Product Error",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    if (showSuccess) {
        StateScreen(
            type = StateType.SUCCESS,
            title = "Product Saved!",
            message = "Product details have been recorded successfully.",
            primaryButtonText = "DONE",
            onPrimaryClick = onNavigateBack
        )
        return
    }

    val screenTitle = if (isEditMode) "Edit Product" else "Add Product"

    Scaffold(
        topBar = {
            WavesHeader(
                title = screenTitle,
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveProduct,
                        modifier = Modifier.testTag("save_product_header_button")
                    ) {
                        Text("Save", color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("add_edit_product_screen")
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

            // Section BASIC: Name*, Description, SKU
            SectionHeader(title = "BASIC INFORMATION")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val nameError = if (hasSubmitted && name.isBlank()) "Product name is required" else null
                    WavesTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Product / Service Name *",
                        placeholder = "e.g. Website Design",
                        leadingIcon = Icons.Filled.Inventory2,
                        errorMessage = nameError
                    )
                    WavesTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = "Description",
                        placeholder = "Brief explanation of deliverables",
                        singleLine = false,
                        maxLines = 3
                    )
                    WavesTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = "Item Code / SKU",
                        placeholder = "e.g. SRV-001"
                    )
                }
            }

            // Section PRICING: Unit Price*, Unit dropdown, Default Quantity
            SectionHeader(title = "PRICING")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = unitPrice,
                        onValueChange = { unitPrice = it },
                        label = "Unit Price (₹) *",
                        placeholder = "0.00",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = "Unit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = unit,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clickable { unitDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { unitDropdownOpen = true }) {
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
                                    expanded = unitDropdownOpen,
                                    onDismissRequest = { unitDropdownOpen = false }
                                ) {
                                    units.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                unit = item
                                                unitDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        WavesTextField(
                            value = defaultQuantity,
                            onValueChange = { defaultQuantity = it },
                            label = "Default Quantity",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section TAX: Tax Rate % (blank = use default)
            SectionHeader(title = "TAX")
            WavesCard {
                WavesTextField(
                    value = taxRate,
                    onValueChange = { taxRate = it },
                    label = "Tax Rate % (blank = use default)",
                    placeholder = "18",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Emerald "SAVE PRODUCT" button
            WavesPrimaryButton(
                text = "SAVE PRODUCT",
                onClick = ::saveProduct
            )

            if (isEditMode) {
                WavesSecondaryButton(
                    text = if (existingProduct?.isArchived == true) "RESTORE PRODUCT" else "ARCHIVE PRODUCT",
                    icon = Icons.Filled.Archive,
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                FirestoreDataRepository.archiveProduct(
                                    productId!!,
                                    archived = existingProduct?.isArchived != true
                                )
                                onNavigateBack()
                            } catch (exception: Exception) {
                                errorMessage = exception.localizedMessage ?: "Unable to update product status."
                            } finally {
                                isSaving = false
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
