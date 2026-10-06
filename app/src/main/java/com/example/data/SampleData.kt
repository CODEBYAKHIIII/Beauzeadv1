package com.example.data

data class Client(
    val id: String,
    val name: String,
    val email: String?,
    val phone: String,
    val address: String = "123 Commercial Avenue",
    val city: String,
    val state: String = "Maharashtra",
    val postalCode: String = "400001",
    val country: String = "India",
    val taxNumber: String = "GSTIN27AAAAA1234Z",
    val notes: String = "Preferred communication via email. VIP Client.",
    val invoiceCount: Int,
    val totalBilled: Double,
    val totalPaid: Double,
    val totalDue: Double,
    val isArchived: Boolean = false
)

data class Product(
    val id: String,
    val name: String,
    val description: String = "Standard agency deliverable and service unit",
    val sku: String = "PRD-001",
    val unitPrice: Double,
    val unit: String, // pcs, yr, hr, kg, mo, day
    val defaultQuantity: Int = 1,
    val taxRate: Double = 18.0,
    val isArchived: Boolean = false
)

enum class InvoiceStatus(val label: String) {
    PAID("PAID"),
    HALF_PAID("HALF PAID"),
    PENDING("PENDING"),
    OVERDUE("OVERDUE"),
    CANCELLED("CANCELLED"),
    WRITTEN_OFF("WRITTEN OFF")
}

data class InvoiceItem(
    val id: String,
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val taxRate: Double = 18.0
) {
    val subtotal: Double get() = quantity * unitPrice
    val taxAmount: Double get() = subtotal * (taxRate / 100.0)
    val total: Double get() = subtotal + taxAmount
}

data class PaymentRecord(
    val id: String,
    val amount: Double,
    val date: String,
    val method: String,
    val reference: String,
    val notes: String
)

data class Invoice(
    val id: String, // e.g. INV-001
    val clientId: String,
    val clientName: String,
    val clientEmail: String?,
    val issueDate: String,
    val dueDate: String,
    val items: List<InvoiceItem>,
    val discount: Double = 0.0,
    val status: InvoiceStatus,
    val paidAmount: Double,
    val notes: String = "Payment due within 15 days.",
    val terms: String = "Standard commercial terms apply.",
    val payments: List<PaymentRecord> = emptyList()
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val taxAmount: Double get() = items.sumOf { it.taxAmount }
    val grandTotal: Double get() = (subtotal - discount) + taxAmount
    val balanceDue: Double
        get() = if (status == InvoiceStatus.CANCELLED || status == InvoiceStatus.WRITTEN_OFF) {
            0.0
        } else {
            (grandTotal - paidAmount).coerceAtLeast(0.0)
        }
}

data class BusinessProfile(
    val name: String = "Waves Studio",
    val tagline: String = "Creative & Technology Solutions",
    val email: String = "hello@waves.app",
    val phone: String = "+91 98765 43210",
    val website: String = "https://waves.app",
    val logoUrl: String = "",
    val addressLine1: String = "123 Main St",
    val addressLine2: String = "Suite 400",
    val city: String = "Mumbai",
    val state: String = "Maharashtra",
    val postalCode: String = "400001",
    val country: String = "India",
    val taxLabel: String = "GSTIN",
    val taxNumber: String = "27AAAAA0000A1Z5",
    val defaultTaxRate: Double = 18.0,
    val pricesIncludeTax: Boolean = false,
    val bankName: String = "HDFC Bank",
    val accountHolder: String = "Waves Studio Pvt Ltd",
    val accountNumber: String = "50200012345678",
    val ifscCode: String = "HDFC0001234",
    val branch: String = "Bandra West, Mumbai",
    val upiId: String = "wavesstudio@okhdfcbank",
    val prefix: String = "INV-",
    val nextNumber: String = "005",
    val currency: String = "INR (₹)",
    val paymentTerms: String = "Net 15 Days",
    val defaultNotes: String = "Thank you for your business! Payment is due within 15 days of issue date."
)

object SampleData {
    val defaultBusiness = BusinessProfile()

    val clients = listOf(
        Client(
            id = "c1",
            name = "Rahul Sharma",
            email = "rahul@email.com",
            phone = "+91 98765 43210",
            city = "Mumbai",
            invoiceCount = 3,
            totalBilled = 25760.0,
            totalPaid = 20760.0,
            totalDue = 5000.0
        ),
        Client(
            id = "c2",
            name = "Priya Mehta",
            email = "priya@email.com",
            phone = "+91 91234 56789",
            city = "Delhi",
            invoiceCount = 2,
            totalBilled = 18500.0,
            totalPaid = 18500.0,
            totalDue = 0.0
        ),
        Client(
            id = "c3",
            name = "Amit Verma",
            email = null,
            phone = "+91 90000 11111",
            city = "Pune",
            invoiceCount = 1,
            totalBilled = 3200.0,
            totalPaid = 0.0,
            totalDue = 3200.0
        )
    )

    val products = listOf(
        Product(
            id = "p1",
            name = "Website Design",
            description = "Complete UI/UX design & responsive landing page",
            sku = "DSG-WEB-01",
            unitPrice = 5000.0,
            unit = "pcs",
            taxRate = 18.0
        ),
        Product(
            id = "p2",
            name = "Hosting (annual)",
            description = "Cloud server hosting with SSL & CDN included",
            sku = "HST-ANN-02",
            unitPrice = 2000.0,
            unit = "yr",
            taxRate = 18.0
        ),
        Product(
            id = "p3",
            name = "Consulting",
            description = "Technical consulting and architectural advisory",
            sku = "CNS-HR-03",
            unitPrice = 1500.0,
            unit = "hr",
            taxRate = 18.0
        )
    )

    val invoices = listOf(
        Invoice(
            id = "INV-001",
            clientId = "c1",
            clientName = "Rahul Sharma",
            clientEmail = "rahul@email.com",
            issueDate = "15 Sep 2026",
            dueDate = "30 Sep 2026",
            items = listOf(
                InvoiceItem("i1", "Website Design", 1, 5000.0, 18.0),
                InvoiceItem("i2", "Hosting (annual)", 1, 2000.0, 18.0)
            ),
            discount = 0.0,
            status = InvoiceStatus.PAID,
            paidAmount = 8260.0,
            payments = listOf(
                PaymentRecord("pay1", 8260.0, "20 Sep 2026", "UPI", "UPI98234710", "Full payment received")
            )
        ),
        Invoice(
            id = "INV-002",
            clientId = "c2",
            clientName = "Priya Mehta",
            clientEmail = "priya@email.com",
            issueDate = "22 Sep 2026",
            dueDate = "07 Oct 2026",
            items = listOf(
                InvoiceItem("i3", "Website Design", 2, 5000.0, 18.0),
                InvoiceItem("i4", "Consulting", 1, 593.22, 18.0)
            ),
            discount = 0.0,
            status = InvoiceStatus.HALF_PAID,
            paidAmount = 6250.0,
            payments = listOf(
                PaymentRecord("pay2", 6250.0, "25 Sep 2026", "Bank Transfer", "TXN8829104", "50% Advance received")
            )
        ),
        Invoice(
            id = "INV-003",
            clientId = "c3",
            clientName = "Amit Verma",
            clientEmail = null,
            issueDate = "10 Sep 2026",
            dueDate = "25 Sep 2026",
            items = listOf(
                InvoiceItem("i5", "Consulting", 2, 1355.93, 18.0)
            ),
            discount = 0.0,
            status = InvoiceStatus.OVERDUE,
            paidAmount = 0.0
        ),
        Invoice(
            id = "INV-004",
            clientId = "c1",
            clientName = "Rahul Sharma",
            clientEmail = "rahul@email.com",
            issueDate = "01 Oct 2026",
            dueDate = "16 Oct 2026",
            items = listOf(
                InvoiceItem("i6", "Website Design", 1, 5000.0, 18.0),
                InvoiceItem("i7", "Hosting (annual)", 1, 2000.0, 18.0)
            ),
            discount = 0.0,
            status = InvoiceStatus.HALF_PAID,
            paidAmount = 4130.0,
            payments = listOf(
                PaymentRecord("pay3", 4130.0, "02 Oct 2026", "UPI", "UPI77189201", "First installment")
            )
        )
    )

    fun formatCurrency(amount: Double): String {
        return "₹%,.0f".format(amount)
    }

    fun formatCurrencyPrecise(amount: Double): String {
        return "₹%,.2f".format(amount)
    }
}
