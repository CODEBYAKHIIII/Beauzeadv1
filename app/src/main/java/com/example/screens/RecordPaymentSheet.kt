package com.example.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.WavesChip
import com.example.components.WavesPrimaryButton
import com.example.components.WavesTextField
import com.example.components.showDemoToast
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.SurfaceColor
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentBottomSheet(
    balanceDue: Double,
    onDismissRequest: () -> Unit,
    onPaymentSaved: (Double, String) -> Unit,
    sheetState: SheetState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var amount by remember { mutableStateOf(balanceDue.toString()) }
    var selectedMethod by remember { mutableStateOf("UPI") }
    val paymentMethods = listOf("Cash", "Bank", "UPI", "Card", "Other")

    var referenceNumber by remember { mutableStateOf("UPI98234710") }
    var paymentDate by remember { mutableStateOf("04 Oct 2026") }
    var paymentNotes by remember { mutableStateOf("Received via Google Pay") }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = SurfaceColor,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        modifier = modifier.testTag("record_payment_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Record Payment",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Amount field (pre-filled with balance due)
            WavesTextField(
                value = amount,
                onValueChange = { amount = it },
                label = "Payment Amount (₹)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )

            // Method chips: [Cash] [Bank] [UPI] [Card] [Other]
            Column {
                Text(
                    text = "Payment Method",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    paymentMethods.forEach { method ->
                        WavesChip(
                            text = method,
                            isSelected = selectedMethod == method,
                            onClick = { selectedMethod = method }
                        )
                    }
                }
            }

            // Reference field
            WavesTextField(
                value = referenceNumber,
                onValueChange = { referenceNumber = it },
                label = "Reference / Transaction ID",
                placeholder = "e.g. UTR / Check # / Ref"
            )

            // Date field
            WavesTextField(
                value = paymentDate,
                onValueChange = { paymentDate = it },
                label = "Payment Date"
            )

            // Notes field
            WavesTextField(
                value = paymentNotes,
                onValueChange = { paymentNotes = it },
                label = "Payment Notes",
                placeholder = "Any additional details",
                singleLine = false,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Emerald "SAVE PAYMENT" button
            WavesPrimaryButton(
                text = "SAVE PAYMENT",
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: balanceDue
                    showDemoToast(context, "Payment of ₹$parsedAmount recorded successfully!")
                    onPaymentSaved(parsedAmount, selectedMethod)
                    onDismissRequest()
                }
            )

            // Text "Cancel"
            Text(
                text = "Cancel",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDismissRequest() }
                    .padding(8.dp)
            )
        }
    }
}
