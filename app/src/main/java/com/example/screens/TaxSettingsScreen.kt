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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesCard
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Country → Tax label + tax number format hint.
 * Label: what the tax is called in that country.
 * Format: what the number looks like (used as placeholder/helper).
 */
private data class CountryTax(
    val country: String,
    val label: String,
    val formatHint: String
)

private val countryTaxMap = listOf(
    CountryTax("India", "GSTIN / PAN", "22AAAAA0000A1Z5"),
    CountryTax("UK", "VAT / UTR / NINO", "GB123456789"),
    CountryTax("USA", "EIN / SSN", "12-3456789"),
    CountryTax("UAE", "TRN", "100123456700003"),
    CountryTax("Australia", "ABN / TFN", "12 345 678 901"),
    CountryTax("Canada", "BN / SIN", "123456789RT0001"),
    CountryTax("Germany", "USt-IdNr / Steuernummer", "DE123456789"),
    CountryTax("Singapore", "UEN / GST Reg No", "202012345A"),
    CountryTax("Nigeria", "TIN / VAT Reg No", "12345678-0001"),
    CountryTax("Kenya", "KRA PIN", "P051234567X"),
    CountryTax("South Africa", "VAT Reg No", "4123456789"),
    CountryTax("Brazil", "CNPJ / CPF", "12.345.678/0001-90")
)

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
    var selectedCountry by remember { mutableStateOf(profile.country) }
    var countryDropdownOpen by remember { mutableStateOf(false) }

    val initial = countryTaxMap.find { it.country == selectedCountry } ?: countryTaxMap.first()
    var taxLabel by remember { mutableStateOf(profile.taxLabel.ifBlank { initial.label }) }
    var taxFormatHint by remember { mutableStateOf(initial.formatHint) }
    var taxNumber by remember { mutableStateOf(profile.taxNumber) }

    fun saveTaxSettings() {
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveBusinessProfile(
                    profile.copy(
                        country = selectedCountry,
                        taxLabel = taxLabel,
                        taxNumber = taxNumber
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

    fun updateDefaultsForCountry(country: String) {
        val config = countryTaxMap.find { it.country == country } ?: return
        selectedCountry = country
        taxLabel = config.label
        taxFormatHint = config.formatHint
        taxNumber = ""  // clear so user enters new value
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
                        text = "Country selection auto-fills the tax label and format hint. You can override the label if needed.",
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
                    // Country dropdown
                    Box {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp)
                                    .clickable { countryDropdownOpen = true },
                                shape = RoundedCornerShape(12.dp),
                                trailingIcon = {
                                    IconButton(onClick = { countryDropdownOpen = true }) {
                                        Icon(
                                            Icons.Filled.ArrowDropDown,
                                            contentDescription = null
                                        )
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
                                expanded = countryDropdownOpen,
                                onDismissRequest = { countryDropdownOpen = false }
                            ) {
                                countryTaxMap.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.country) },
                                        onClick = {
                                            updateDefaultsForCountry(item.country)
                                            countryDropdownOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Tax label (auto-filled, editable)
                    WavesTextField(
                        value = taxLabel,
                        onValueChange = { taxLabel = it },
                        label = "Tax Label"
                    )

                    // Tax number with country-specific placeholder
                    WavesTextField(
                        value = taxNumber,
                        onValueChange = { taxNumber = it },
                        label = "Tax Number",
                        placeholder = taxFormatHint
                    )
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
