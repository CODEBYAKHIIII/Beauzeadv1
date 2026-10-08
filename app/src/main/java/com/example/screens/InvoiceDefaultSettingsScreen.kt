package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.InvoiceDisplayFormat
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import com.example.util.WavesValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

enum class InvoiceDefaultSetting(val title: String) {
    PREFIX_NUMBERING("Prefix & Numbering"),
    CURRENCY("Currency"),
    PAYMENT_TERMS("Payment Terms")
}

private val INVOICE_PREFIX_REGEX = Regex("^[A-Za-z0-9\\-/]{1,16}$")

private fun validateInvoicePrefix(value: String): String? = when {
    value.isBlank() -> "Invoice prefix is required"
    !INVOICE_PREFIX_REGEX.matches(value.trim()) -> "Use letters, numbers, - or / only"
    else -> null
}

private fun validateNextInvoiceNumber(value: String): String? = when {
    value.isBlank() -> "Next invoice number is required"
    value.any { !it.isDigit() } -> "Next invoice number must be numeric"
    else -> null
}

private fun validateDefaultNotes(value: String): String? =
    if (value.length > 1000) "Notes are too long" else null

@Composable
fun InvoiceDefaultSettingsScreen(
    setting: InvoiceDefaultSetting,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val profile = when (val state = profileState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading ${setting.title.lowercase()}...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, title = "Settings Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var prefix by remember(setting, profile) { mutableStateOf(profile.prefix) }
    var nextNumber by remember(setting, profile) { mutableStateOf(profile.nextNumber) }
    var country by remember(setting, profile) { mutableStateOf(profile.country) }
    var paymentTerms by remember(setting, profile) { mutableStateOf(profile.paymentTerms) }
    var defaultNotes by remember(setting, profile) { mutableStateOf(profile.defaultNotes) }
    var countryMenuExpanded by remember { mutableStateOf(false) }
    var hasAttemptedSave by remember(setting) { mutableStateOf(false) }

    val prefixError =
        if (hasAttemptedSave && setting == InvoiceDefaultSetting.PREFIX_NUMBERING) {
            validateInvoicePrefix(prefix)
        } else null
    val nextNumberError =
        if (hasAttemptedSave && setting == InvoiceDefaultSetting.PREFIX_NUMBERING) {
            validateNextInvoiceNumber(nextNumber)
        } else null
    val paymentTermsError =
        if (hasAttemptedSave && setting == InvoiceDefaultSetting.PAYMENT_TERMS) {
            WavesValidation.required(paymentTerms, "Default payment terms")
        } else null
    val defaultNotesError =
        if (hasAttemptedSave && setting == InvoiceDefaultSetting.PAYMENT_TERMS) {
            validateDefaultNotes(defaultNotes)
        } else null

    fun save() {
        hasAttemptedSave = true
        val errors = when (setting) {
            InvoiceDefaultSetting.PREFIX_NUMBERING -> listOfNotNull(
                validateInvoicePrefix(prefix),
                validateNextInvoiceNumber(nextNumber)
            )
            InvoiceDefaultSetting.CURRENCY -> emptyList()
            InvoiceDefaultSetting.PAYMENT_TERMS -> listOfNotNull(
                WavesValidation.required(paymentTerms, "Default payment terms"),
                validateDefaultNotes(defaultNotes)
            )
        }
        if (errors.isNotEmpty()) {
            return
        }
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                val updated = when (setting) {
                    InvoiceDefaultSetting.PREFIX_NUMBERING -> profile.copy(
                        prefix = prefix.trim(),
                        nextNumber = nextNumber.trim()
                    )
                    InvoiceDefaultSetting.CURRENCY -> profile.copy(
                        country = country,
                        currency = InvoiceDisplayFormat.currencyCode(country)
                    )
                    InvoiceDefaultSetting.PAYMENT_TERMS -> profile.copy(
                        paymentTerms = paymentTerms.trim(),
                        defaultNotes = defaultNotes.trim()
                    )
                }
                FirestoreDataRepository.saveBusinessProfile(updated)
                showDemoToast(context, "${setting.title} saved.")
                onNavigateBack()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save ${setting.title.lowercase()}."
            } finally {
                isSaving = false
            }
        }
    }

    if (isSaving) {
        StateScreen(type = StateType.LOADING, message = "Saving ${setting.title.lowercase()}...")
        return
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = setting.title,
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::save,
                        modifier = Modifier.testTag("invoice_defaults_save")
                    ) {
                        Text("Save", color = OnPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("invoice_default_${setting.name.lowercase()}")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            WavesCard {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (setting) {
                        InvoiceDefaultSetting.PREFIX_NUMBERING -> {
                            WavesTextField(
                                value = prefix,
                                onValueChange = { prefix = it },
                                label = "Invoice prefix",
                                errorMessage = prefixError
                            )
                            WavesTextField(
                                value = nextNumber,
                                onValueChange = { value -> nextNumber = value.filter(Char::isDigit) },
                                label = "Next invoice number",
                                errorMessage = nextNumberError
                            )
                            Text(
                                "The next invoice will be numbered ${prefix}${nextNumber.ifBlank { "001" }}.",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                        InvoiceDefaultSetting.CURRENCY -> {
                            Box {
                                Column {
                                    Text(
                                        "Business country",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    OutlinedTextField(
                                        value = country,
                                        onValueChange = {},
                                        readOnly = true,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(56.dp)
                                            .clickable { countryMenuExpanded = true }
                                            .testTag("invoice_currency_country"),
                                        trailingIcon = {
                                            IconButton(onClick = { countryMenuExpanded = true }) {
                                                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Choose country")
                                            }
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = SurfaceColor,
                                            unfocusedContainerColor = SurfaceColor,
                                            focusedBorderColor = AccentCyan,
                                            unfocusedBorderColor = InputBorderGray
                                        )
                                    )
                                }
                                DropdownMenu(
                                    expanded = countryMenuExpanded,
                                    onDismissRequest = { countryMenuExpanded = false }
                                ) {
                                    InvoiceDisplayFormat.supportedCountries.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                country = option
                                                countryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Text(
                                "Invoice currency: ${InvoiceDisplayFormat.currencyName(country)}",
                                color = TextSecondary,
                                fontSize = 14.sp
                            )
                            Text(
                                "Changing the business country also changes the applicable tax jurisdiction.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        InvoiceDefaultSetting.PAYMENT_TERMS -> {
                            WavesTextField(
                                value = paymentTerms,
                                onValueChange = { paymentTerms = it },
                                label = "Default payment terms",
                                errorMessage = paymentTermsError
                            )
                            WavesTextField(
                                value = defaultNotes,
                                onValueChange = { defaultNotes = it },
                                label = "Default invoice notes",
                                singleLine = false,
                                maxLines = 3,
                                errorMessage = defaultNotesError
                            )
                        }
                    }
                }
            }
            if (errorMessage.isNotBlank()) {
                Text(
                    text = errorMessage,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    modifier = Modifier.testTag("invoice_defaults_error")
                )
            }
        }
    }
}
