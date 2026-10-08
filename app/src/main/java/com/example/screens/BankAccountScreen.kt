package com.example.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.input.KeyboardType
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.WavesValidation

/**
 * The field accepts either an Indian IFSC or an international SWIFT/BIC,
 * so it is valid when either central validator accepts the value.
 */
private fun validateIfscOrSwift(value: String): String? = when {
    value.isBlank() -> "IFSC / SWIFT / IBAN is required"
    WavesValidation.ifsc(value) == null || WavesValidation.swiftBic(value) == null -> null
    else -> "Enter a valid IFSC (e.g. HDFC0000123) or SWIFT/BIC (8 or 11 characters)"
}

@Composable
fun BankAccountScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val profileState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val initial = when (val state = profileState) {
        FirestoreState.Loading -> {
            StateScreen(type = StateType.LOADING, message = "Loading bank details...")
            return
        }
        is FirestoreState.Failure -> {
            StateScreen(type = StateType.ERROR, title = "Bank Details Error", message = state.message)
            return
        }
        is FirestoreState.Data -> state.value
    }

    var isSaving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var bankName by remember { mutableStateOf(initial.bankName) }
    var accountHolder by remember { mutableStateOf(initial.accountHolder) }
    var accountNumber by remember { mutableStateOf(initial.accountNumber) }
    var ifscCode by remember { mutableStateOf(initial.ifscCode) }
    var branch by remember { mutableStateOf(initial.branch) }
    var upiId by remember { mutableStateOf(initial.upiId) }
    var hasAttemptedSave by remember { mutableStateOf(false) }

    val bankNameError =
        if (hasAttemptedSave) WavesValidation.required(bankName, "Bank name") else null
    val accountHolderError =
        if (hasAttemptedSave) WavesValidation.name(accountHolder, "Account holder name") else null
    val accountNumberError =
        if (hasAttemptedSave) WavesValidation.bankAccountNumber(accountNumber) else null
    val ifscError = if (hasAttemptedSave) validateIfscOrSwift(ifscCode) else null
    val upiIdError =
        if (hasAttemptedSave && upiId.isNotBlank()) WavesValidation.upiId(upiId) else null

    fun saveBankDetails() {
        hasAttemptedSave = true
        if (listOfNotNull(
                WavesValidation.required(bankName, "Bank name"),
                WavesValidation.name(accountHolder, "Account holder name"),
                WavesValidation.bankAccountNumber(accountNumber),
                validateIfscOrSwift(ifscCode),
                if (upiId.isNotBlank()) WavesValidation.upiId(upiId) else null
            ).isNotEmpty()
        ) {
            return
        }
        coroutineScope.launch {
            isSaving = true
            errorMessage = ""
            try {
                FirestoreDataRepository.saveBusinessProfile(
                    initial.copy(
                        bankName = bankName,
                        accountHolder = accountHolder,
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        branch = branch,
                        upiId = upiId
                    )
                )
                showDemoToast(context, "Bank details saved successfully!")
                onNavigateBack()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                errorMessage = exception.localizedMessage ?: "Unable to save bank details."
            } finally {
                isSaving = false
            }
        }
    }

    if (isSaving) {
        StateScreen(type = StateType.LOADING, message = "Saving bank details...")
        return
    }
    if (errorMessage.isNotBlank()) {
        StateScreen(
            type = StateType.ERROR,
            title = "Bank Details Error",
            message = errorMessage,
            onPrimaryClick = { errorMessage = "" },
            onSecondaryClick = { errorMessage = "" }
        )
        return
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Bank Account",
                onBackClick = onNavigateBack,
                actions = {
                    TextButton(
                        onClick = ::saveBankDetails,
                        modifier = Modifier.testTag("bank_settings_save_button")
                    ) {
                        Text("Save", color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("bank_account_screen")
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

            // Info note: "These details appear on every invoice PDF."
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
                        text = "These details appear on every invoice PDF to make it easy for your clients to pay directly via Bank Transfer or UPI.",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            SectionHeader(title = "BANK ACCOUNT DETAILS")
            WavesCard {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WavesTextField(
                        value = bankName,
                        onValueChange = { bankName = it },
                        label = "Bank Name",
                        placeholder = "e.g. HDFC Bank",
                        leadingIcon = Icons.Filled.AccountBalance,
                        errorMessage = bankNameError
                    )

                    WavesTextField(
                        value = accountHolder,
                        onValueChange = { accountHolder = it },
                        label = "Account Holder Name",
                        placeholder = "e.g. Waves Studio Pvt Ltd",
                        errorMessage = accountHolderError
                    )

                    WavesTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it },
                        label = "Account Number",
                        placeholder = "e.g. 50200012345678",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        errorMessage = accountNumberError
                    )

                    WavesTextField(
                        value = ifscCode,
                        onValueChange = { ifscCode = it },
                        label = "IFSC / SWIFT / IBAN",
                        placeholder = "e.g. HDFC0001234",
                        errorMessage = ifscError
                    )

                    WavesTextField(
                        value = branch,
                        onValueChange = { branch = it },
                        label = "Branch Name",
                        placeholder = "e.g. Bandra West, Mumbai"
                    )

                    WavesTextField(
                        value = upiId,
                        onValueChange = { upiId = it },
                        label = "UPI ID / VPA",
                        placeholder = "e.g. company@okhdfcbank",
                        errorMessage = upiIdError
                    )
                }
            }

            WavesPrimaryButton(
                text = "SAVE BANK DETAILS",
                onClick = ::saveBankDetails
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
