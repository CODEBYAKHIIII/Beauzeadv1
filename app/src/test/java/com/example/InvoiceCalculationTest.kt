package com.example

import com.example.data.Invoice
import com.example.data.InvoiceItem
import com.example.data.InvoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class InvoiceCalculationTest {
    @Test
    fun subtotal_isSumOfItemSubtotals() {
        val invoice = invoice(
            items = listOf(
                InvoiceItem("one", "First", 1, 5000.0, 0.0),
                InvoiceItem("two", "Second", 1, 2000.0, 0.0),
                InvoiceItem("three", "Third", 1, 1500.0, 0.0)
            )
        )

        assertEquals(8500.0, invoice.subtotal, 0.001)
    }

    @Test
    fun balanceDue_isGrandTotalMinusPaid() {
        val invoice = invoice(
            items = listOf(InvoiceItem("one", "Item", 1, 8260.0, 0.0)),
            paidAmount = 4130.0,
            status = InvoiceStatus.HALF_PAID
        )

        assertEquals(4130.0, invoice.balanceDue, 0.001)
    }

    @Test
    fun balanceDue_isZeroForCancelledInvoices() {
        val invoice = invoice(
            items = listOf(InvoiceItem("one", "Item", 1, 8260.0, 0.0)),
            paidAmount = 0.0,
            status = InvoiceStatus.CANCELLED
        )

        assertEquals(0.0, invoice.balanceDue, 0.001)
    }

    private fun invoice(
        items: List<InvoiceItem>,
        paidAmount: Double = 0.0,
        status: InvoiceStatus = InvoiceStatus.PENDING
    ) = Invoice(
        id = "INV-004",
        clientId = "client-1",
        clientName = "Test Client",
        clientEmail = null,
        issueDate = "05 Oct 2026",
        dueDate = "20 Oct 2026",
        items = items,
        status = status,
        paidAmount = paidAmount
    )
}