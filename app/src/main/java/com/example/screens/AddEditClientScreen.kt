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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
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
import com.example.data.Client
import com.example.data.FirestoreDataRepository
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.InputBorderGray
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun AddEditClientScreen(
    clientId: String?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val isEditMode = clientId != null && clientId != "new"
    var existingClient by remember { mutableStateOf<Client?>(null) }

    var isSaving by remember { mutableStateOf(false) }
    var isLoadingExisting by remember { mutableStateOf(isEditMode) }
    var showSuccess by remember { mutableStateOf(false) }
    var hasSubmitted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var addressLine1 by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var country by remember { mutableStateOf(existingClient?.country ?: "India") }
    var countryDropdownOpen by remember { mutableStateOf(false) }
    val countries = listOf("India", "USA", "UK", "UAE", "Australia", "Canada", "Singapore")

    var taxNumber by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(clientId) {
        if (isEditMode) {
            try {
                existingClient = FirestoreDataRepository.getClient(clientId!!)
                    ?: error("Client not found.")
                existingClient?.let { client ->
                    name = client.name
                    email = client.email.orEmpty()
                    phone = client.phone
                    addressLine1 = client.address
                    city = client.city
                    state = client.state
                    postalCode = client.postalCode
                    country = client.country
                    taxNumber = client.taxNumber
                    notes = client.notes
                }
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to load client."
            } finally {
                isLoadingExisting = false
            }
        }
    }

    fun saveClient() {
        hasSubmitted = true
        if (name.isBlank()) return
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveClient(
                    Client(
                        id = if (isEditMode) clientId!! else "",
                        name = name.trim(),
                        email = email.trim().ifBlank { null },
                        phone = phone.trim(),
                        address = addressLine1.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        postalCode = postalCode.trim(),
                        country = country,
                        taxNumber = taxNumber.trim(),
                        notes = notes.trim(),
                        invoiceCount = existingClient?.invoiceCount ?: 0,
                        totalBilled = existingClient?.totalBilled ?: 0.0,
                        totalPaid = existingClient?.totalPaid ?: 0.0,
                        totalDue = existingClient?.totalDue ?: 0.0,
                        isArchived = existingClient?.isArchived ?: false
                    )
                )
                showSuccess = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save client."
            } finally {
                isSaving = false
            }
        }
    }

    if (isLoadingExisting || isSaving) {
        StateScreen(
            type = StateType.LOADING,
            message = if (isLoadingExisting) "Loading client details..." else "Saving client details..."
        )
        return
    }

    if (errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Client Error",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    if (showSuccess) {
        StateScreen(
            type = StateType.SUCCESS,
            title = "Client Saved!",
            message = "Client details have been recorded successfully.",
            primaryButtonText = "DONE",
            onPrimaryClick = onNavigateBack
        )
        return
    }

    val screenTitle = if (isEditMode) "Edit Client" else "Add Client"

    Scaffold(
        topBar = {
            WavesHeader(
                title = screenTitle,
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveClient,
                        modifier = Modifier.testTag("save_client_header_button")
                    ) {
                        Text("Save", color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("add_edit_client_screen")
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

            // Section BASIC
            SectionHeader(title = "BASIC INFORMATION")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    val nameError = if (hasSubmitted && name.isBlank()) "Client name is required" else null
                    WavesTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = "Client Name *",
                        placeholder = "e.g. Rahul Sharma",
                        leadingIcon = Icons.Filled.Person,
                        errorMessage = nameError
                    )
                    WavesTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email Address",
                        placeholder = "rahul@email.com",
                        leadingIcon = Icons.Filled.Email,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    WavesTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        placeholder = "+91 98765 43210",
                        leadingIcon = Icons.Filled.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                }
            }

            // Section ADDRESS
            SectionHeader(title = "BILLING ADDRESS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = addressLine1,
                        onValueChange = { addressLine1 = it },
                        label = "Address Line 1",
                        placeholder = "123 Commercial St"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WavesTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = "City",
                            placeholder = "Mumbai",
                            modifier = Modifier.weight(1f)
                        )
                        WavesTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = "State",
                            placeholder = "Maharashtra",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        WavesTextField(
                            value = postalCode,
                            onValueChange = { postalCode = it },
                            label = "Postal Code",
                            placeholder = "400001",
                            modifier = Modifier.weight(1f)
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            Column {
                                Text(
                                    text = "Country",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                                OutlinedTextField(
                                    value = country,
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .clickable { countryDropdownOpen = true },
                                    shape = RoundedCornerShape(12.dp),
                                    trailingIcon = {
                                        IconButton(onClick = { countryDropdownOpen = true }) {
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
                                    expanded = countryDropdownOpen,
                                    onDismissRequest = { countryDropdownOpen = false }
                                ) {
                                    countries.forEach { item ->
                                        DropdownMenuItem(
                                            text = { Text(item) },
                                            onClick = {
                                                country = item
                                                countryDropdownOpen = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section TAX
            SectionHeader(title = "TAX DETAILS")
            WavesCard {
                WavesTextField(
                    value = taxNumber,
                    onValueChange = { taxNumber = it },
                    label = "Tax / GSTIN Number",
                    placeholder = "27AAAAA0000A1Z5"
                )
            }

            // Section NOTES
            SectionHeader(title = "NOTES")
            WavesCard {
                WavesTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Client Notes",
                    placeholder = "Special billing instructions, payment terms, or client preferences",
                    singleLine = false,
                    maxLines = 4
                )
            }

            // Emerald "SAVE CLIENT" button
            WavesPrimaryButton(
                text = "SAVE CLIENT",
                onClick = ::saveClient
            )

            // Red outlined "DELETE CLIENT" button (edit mode only)
            if (isEditMode) {
                WavesSecondaryButton(
                    text = "DELETE CLIENT",
                    icon = Icons.Filled.Delete,
                    isDestructive = true,
                    onClick = {
                        coroutineScope.launch {
                            isSaving = true
                            try {
                                FirestoreDataRepository.deleteClient(clientId!!)
                                onNavigateBack()
                            } catch (exception: Exception) {
                                errorMessage = exception.localizedMessage ?: "Unable to delete client."
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
