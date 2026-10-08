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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import com.example.util.TaxIdType
import com.example.util.WavesTaxCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Tax Settings.
 *
 * Country and Tax Type are both strict dropdowns — the tax label is never
 * typed by hand. Each country offers its own tax identification types
 * (e.g. India -> GSTIN / PAN / No Tax, USA -> EIN / SSN / No Tax), and the
 * tax number is validated against the selected type before saving.
 * "No Tax" clears the number so nothing is printed on invoices.
 */
@Composable
fun TaxSettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val profile = when (val state = profileState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading tax settings...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, title = "Tax Settings Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var hasAttemptedSave by remember { mutableStateOf(false) }

    // Restore previously saved country + type; blank when never configured.
    val savedCountryConfig = WavesTaxCatalog.configFor(profile.country)
    var selectedCountry by remember {
        mutableStateOf(savedCountryConfig?.country.orEmpty())
    }
    var selectedTypeId by remember {
        mutableStateOf(
            if (savedCountryConfig != null && profile.taxLabel.isNotBlank()) {
                WavesTaxCatalog.typeIdFromLabel(savedCountryConfig.country, profile.taxLabel)
            } else {
                null
            }
        )
    }
    var taxNumber by remember { mutableStateOf(profile.taxNumber) }

    var countryDropdownOpen by remember { mutableStateOf(false) }
    var typeDropdownOpen by remember { mutableStateOf(false) }

    val availableTypes = if (selectedCountry.isBlank()) {
        emptyList()
    } else {
        WavesTaxCatalog.typesFor(selectedCountry)
    }
    val selectedType = availableTypes.find { it.id == selectedTypeId }

    // ---- validation (on save, shown inline) ----
    val countryError = if (hasAttemptedSave && selectedCountry.isBlank()) {
        "Country is required"
    } else {
        null
    }
    val typeError = if (hasAttemptedSave && selectedType == null) {
        "Tax type is required"
    } else {
        null
    }
    val taxNumberError = if (hasAttemptedSave) {
        selectedType?.validate(taxNumber)
    } else {
        null
    }

    fun saveTaxSettings() {
        hasAttemptedSave = true
        if (selectedCountry.isBlank() || selectedType == null) return
        if (selectedType.validate(taxNumber) != null) return
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveBusinessProfile(
                    profile.copy(
                        country = selectedCountry,
                        taxLabel = selectedType.label,
                        taxNumber = if (selectedType.isNoTax) "" else taxNumber.trim().uppercase()
                    )
                )
                showDemoToast(context, "Tax settings saved successfully!")
                onNavigateBack()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save tax settings."
            } finally {
                isSaving = false
            }
        }
    }

    if (isSaving) {
        StateScreen(type = StateType.LOADING, message = "Saving tax settings...")
        return
    }
    if (errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Tax Settings Error",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    fun onCountrySelected(country: String) {
        selectedCountry = country
        selectedTypeId = null
        taxNumber = ""
        countryDropdownOpen = false
    }

    fun onTaxTypeSelected(type: TaxIdType) {
        if (type.id != selectedTypeId) taxNumber = ""
        selectedTypeId = type.id
        typeDropdownOpen = false
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Tax Settings",
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveTaxSettings,
                        modifier = Modifier.testTag("tax_settings_save_button")
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
        modifier = modifier.testTag("tax_settings_screen")
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

            // ===== INFO CARD =====
            WavesCard {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = EmeraldInk,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Pick your country, then choose the tax type it uses — " +
                            "India offers GSTIN, PAN or No Tax. The number is checked " +
                            "before saving. Choose \"No Tax\" to keep invoices tax-free.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            // ===== TAX CONFIGURATION =====
            SectionHeader(title = "TAX CONFIGURATION")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // ---- Country dropdown ----
                    Column {
                        Text(
                            text = "Country",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = selectedCountry,
                            onValueChange = {},
                            readOnly = true,
                            enabled = true,
                            placeholder = {
                                Text(
                                    "Select country",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            },
                            isError = countryError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable { countryDropdownOpen = true },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(onClick = { countryDropdownOpen = true }) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select country",
                                        tint = if (countryError != null) {
                                            Color(0xFFDC2626)
                                        } else {
                                            Color.Unspecified
                                        }
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = InputBorderGray,
                                errorBorderColor = Color(0xFFDC2626)
                            )
                        )
                        DropdownMenu(
                            expanded = countryDropdownOpen,
                            onDismissRequest = { countryDropdownOpen = false }
                        ) {
                            WavesTaxCatalog.countries.forEach { country ->
                                DropdownMenuItem(
                                    text = { Text(country) },
                                    leadingIcon = if (country == selectedCountry) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = EmeraldInk
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = { onCountrySelected(country) }
                                )
                            }
                        }
                        if (countryError != null) {
                            Text(
                                text = countryError.orEmpty(),
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // ---- Tax type dropdown (country-specific options) ----
                    Column {
                        Text(
                            text = "Tax Type",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = selectedType?.label.orEmpty(),
                            onValueChange = {},
                            readOnly = true,
                            enabled = selectedCountry.isNotBlank(),
                            placeholder = {
                                Text(
                                    if (selectedCountry.isBlank()) {
                                        "Select a country first"
                                    } else {
                                        "Select tax type"
                                    },
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            },
                            isError = typeError != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clickable(enabled = selectedCountry.isNotBlank()) {
                                    typeDropdownOpen = true
                                },
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = {
                                IconButton(
                                    enabled = selectedCountry.isNotBlank(),
                                    onClick = { typeDropdownOpen = true }
                                ) {
                                    Icon(
                                        Icons.Filled.ArrowDropDown,
                                        contentDescription = "Select tax type",
                                        tint = if (typeError != null) {
                                            Color(0xFFDC2626)
                                        } else {
                                            Color.Unspecified
                                        }
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = SurfaceColor,
                                unfocusedContainerColor = SurfaceColor,
                                focusedBorderColor = AccentCyan,
                                unfocusedBorderColor = InputBorderGray,
                                errorBorderColor = Color(0xFFDC2626),
                                disabledContainerColor = SurfaceColor,
                                disabledBorderColor = InputBorderGray
                            )
                        )
                        DropdownMenu(
                            expanded = typeDropdownOpen,
                            onDismissRequest = { typeDropdownOpen = false }
                        ) {
                            availableTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.label) },
                                    leadingIcon = if (type.id == selectedTypeId) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = EmeraldInk
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = { onTaxTypeSelected(type) }
                                )
                            }
                        }
                        if (typeError != null) {
                            Text(
                                text = typeError.orEmpty(),
                                fontSize = 12.sp,
                                color = Color(0xFFDC2626),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    // ---- Tax number (validated per selected type) ----
                    if (selectedType == null || !selectedType.isNoTax) {
                        WavesTextField(
                            value = taxNumber,
                            onValueChange = { taxNumber = it },
                            label = "Tax Number *",
                            placeholder = selectedType?.hint?.ifBlank { null },
                            enabled = selectedType != null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Ascii
                            ),
                            errorMessage = taxNumberError
                        )
                    } else {
                        Text(
                            text = "No tax identification will be printed on your invoices.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }
            }

            // ===== SAVE BUTTON =====
            WavesPrimaryButton(
                text = "SAVE TAX SETTINGS",
                onClick = ::saveTaxSettings
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
