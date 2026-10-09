package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed interface FirestoreState<out T> {
    data object Loading : FirestoreState<Nothing>
    data class Data<T>(val value: T) : FirestoreState<T>
    data class Failure(val message: String) : FirestoreState<Nothing>
}

class DuplicateInvoiceNumberException : Exception("That invoice number is already in use.")
class PaymentExceedsBalanceException : Exception("Payment amount exceeds the remaining balance.")

object FirestoreDataRepository {
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private fun userDocument() = firestore.collection("users").document(requireVerifiedUid())

    private fun requireVerifiedUid(): String {
        val user = FirebaseAuth.getInstance().currentUser
            ?: throw IllegalStateException("Sign in to access your business data.")
        if (!user.isEmailVerified) {
            throw IllegalStateException("Verify your email before accessing business data.")
        }
        return user.uid
    }

    private fun collection(name: String): CollectionReference = userDocument().collection(name)
    private fun businessDocument() = userDocument().collection("settings").document("business")

    private fun <T> CollectionReference.observeDocuments(
        decode: (DocumentSnapshot) -> T
    ): Flow<FirestoreState<List<T>>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(FirestoreState.Failure(error.localizedMessage ?: "Unable to load data."))
            } else {
                trySend(FirestoreState.Data(snapshot?.documents.orEmpty().map(decode)))
            }
        }
        awaitClose { listener.remove() }
    }

    private fun <T> com.google.firebase.firestore.DocumentReference.observeDocument(
        decode: (DocumentSnapshot?) -> T
    ): Flow<FirestoreState<T>> = callbackFlow {
        val listener = addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(FirestoreState.Failure(error.localizedMessage ?: "Unable to load data."))
            } else {
                trySend(FirestoreState.Data(decode(snapshot)))
            }
        }
        awaitClose { listener.remove() }
    }

    fun observeClients(): Flow<FirestoreState<List<Client>>> = combine(
        collection("clients").observeDocuments { it.toClient() },
        observeInvoices()
    ) { clientsState, invoicesState ->
        when {
            clientsState is FirestoreState.Failure -> clientsState
            invoicesState is FirestoreState.Failure -> invoicesState
            clientsState is FirestoreState.Data && invoicesState is FirestoreState.Data -> {
                val invoicesByClient = invoicesState.value.groupBy { it.clientId }
                FirestoreState.Data(clientsState.value.map { client ->
                    val invoices = invoicesByClient[client.id].orEmpty()
                    client.copy(
                        invoiceCount = invoices.size,
                        totalBilled = invoices.filterNot { it.status == InvoiceStatus.CANCELLED }
                            .sumOf { it.grandTotal },
                        totalPaid = invoices.sumOf { it.paidAmount },
                        totalDue = invoices.filterNot { it.status == InvoiceStatus.CANCELLED }
                            .sumOf { it.balanceDue }
                    )
                })
            }
            else -> FirestoreState.Loading
        }
    }

    fun observeClient(clientId: String): Flow<FirestoreState<Client?>> =
        collection("clients").document(clientId).observeDocument { it?.takeIf(DocumentSnapshot::exists)?.toClient() }

    fun observeProducts(): Flow<FirestoreState<List<Product>>> =
        collection("products").observeDocuments { it.toProduct() }

    fun observeInvoices(): Flow<FirestoreState<List<Invoice>>> =
        collection("invoices").observeDocuments { it.toInvoice() }

    fun observeInvoice(invoiceId: String): Flow<FirestoreState<Invoice?>> =
        collection("invoices").document(invoiceId).observeDocument { it?.takeIf(DocumentSnapshot::exists)?.toInvoice() }

    fun observeBusinessProfile(): Flow<FirestoreState<BusinessProfile>> =
        businessDocument().observeDocument { it?.takeIf(DocumentSnapshot::exists)?.toBusinessProfile() ?: emptyBusinessProfile() }

    suspend fun getClient(clientId: String): Client? =
        collection("clients").document(clientId).get().await().takeIf { it.exists() }?.toClient()

    suspend fun getProduct(productId: String): Product? =
        collection("products").document(productId).get().await().takeIf { it.exists() }?.toProduct()

    suspend fun getBusinessProfile(): BusinessProfile =
        businessDocument().get().await().takeIf { it.exists() }?.toBusinessProfile() ?: emptyBusinessProfile()

    suspend fun saveClient(client: Client): String {
        val documentId = client.id.ifBlank { UUID.randomUUID().toString() }
        collection("clients").document(documentId).set(client.toFirestoreMap() + updatedAt())
            .await()
        return documentId
    }

    suspend fun deleteClient(clientId: String) {
        collection("clients").document(clientId).delete().await()
    }

    suspend fun archiveClient(clientId: String, archived: Boolean = true) {
        collection("clients").document(clientId).update("isArchived", archived, "updatedAt", FieldValue.serverTimestamp())
            .await()
    }

    suspend fun saveProduct(product: Product): String {
        val documentId = product.id.ifBlank { UUID.randomUUID().toString() }
        collection("products").document(documentId).set(product.toFirestoreMap() + updatedAt())
            .await()
        return documentId
    }

    suspend fun archiveProduct(productId: String, archived: Boolean = true) {
        collection("products").document(productId).update("isArchived", archived, "updatedAt", FieldValue.serverTimestamp())
            .await()
    }

    suspend fun saveBusinessProfile(profile: BusinessProfile) {
        businessDocument().set(profile.toFirestoreMap() + updatedAt(), SetOptions.merge()).await()
    }

    suspend fun uploadBusinessLogo(uri: Uri): String {
        val uid = requireVerifiedUid()
        val upload = FirebaseStorage.getInstance().reference
            .child("users/$uid/business/logo")
            .putFile(uri)
            .await()
        val downloadUrl = upload.storage.downloadUrl.await().toString()
        businessDocument().set(mapOf("logoUrl" to downloadUrl) + updatedAt(), SetOptions.merge()).await()
        return downloadUrl
    }

    suspend fun getNextInvoiceNumber(): String {
        val profile = businessDocument().get().await()
        val defaults = emptyBusinessProfile()
        val prefix = profile.getString("prefix") ?: defaults.prefix
        val nextNumber = profile.getString("nextNumber") ?: defaults.nextNumber
        return prefix + nextNumber
    }

    suspend fun createInvoice(invoice: Invoice): String {
        require(invoice.clientId.isNotBlank()) { "Select a client before saving the invoice." }
        require(invoice.items.isNotEmpty()) { "Add at least one item before saving the invoice." }
        require(invoice.items.all {
            it.name.isNotBlank() && it.quantity > 0 && it.unitPrice >= 0.0 && it.taxRate >= 0.0
        }) { "Invoice items must have a name, positive quantity, and non-negative price and tax." }
        require(invoice.discount >= 0.0 && invoice.discount <= invoice.subtotal) {
            "Discount must be between zero and the invoice subtotal."
        }

        val invoices = collection("invoices")
        val clientReference = collection("clients").document(invoice.clientId)
        val profileReference = businessDocument()
        return firestore.runTransaction { transaction ->
            val clientSnapshot = transaction.get(clientReference)
            if (!clientSnapshot.exists()) throw IllegalStateException("The selected client no longer exists.")
            if (clientSnapshot.getBoolean("isArchived") == true) {
                throw IllegalStateException("Restore the selected client before creating an invoice.")
            }
            val profile = transaction.get(profileReference)
            val defaults = emptyBusinessProfile()
            val prefix = profile.getString("prefix") ?: defaults.prefix
            val nextNumber = profile.getString("nextNumber") ?: defaults.nextNumber
            val suggestedId = prefix + nextNumber
            val invoiceId = invoice.id.trim().ifBlank { suggestedId }
            val invoiceReference = invoices.document(invoiceId)
            if (transaction.get(invoiceReference).exists()) {
                throw DuplicateInvoiceNumberException()
            }

            val savedInvoice = invoice.copy(
                id = invoiceId,
                clientName = clientSnapshot.getString("name") ?: invoice.clientName,
                clientEmail = clientSnapshot.getString("email") ?: invoice.clientEmail,
                status = InvoiceStatus.PENDING,
                paidAmount = 0.0,
                payments = emptyList()
            )
            transaction.set(invoiceReference, savedInvoice.toFirestoreMap() + createdAndUpdatedAt())

            if (invoiceId == suggestedId) {
                transaction.set(
                    profileReference,
                    mapOf("prefix" to prefix, "nextNumber" to incrementNumber(nextNumber)) + updatedAt(),
                    SetOptions.merge()
                )
            }
            invoiceId
        }.await()
    }

    suspend fun updateInvoiceStatus(invoiceId: String, status: InvoiceStatus) {
        val reference = collection("invoices").document(invoiceId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            if (!snapshot.exists()) throw IllegalStateException("Invoice no longer exists.")
            val invoice = snapshot.toInvoice()
            val paidAmount = when (status) {
                InvoiceStatus.PAID -> invoice.grandTotal
                InvoiceStatus.HALF_PAID -> maxOf(invoice.paidAmount, invoice.grandTotal / 2.0)
                else -> invoice.paidAmount
            }
            val resultingStatus = when {
                status == InvoiceStatus.PAID -> InvoiceStatus.PAID
                status == InvoiceStatus.HALF_PAID && paidAmount >= invoice.grandTotal -> InvoiceStatus.PAID
                status == InvoiceStatus.HALF_PAID -> InvoiceStatus.HALF_PAID
                else -> status
            }
            val additionalPayment = (paidAmount - invoice.paidAmount).coerceAtLeast(0.0)
            val payments = if (additionalPayment > 0.0) {
                invoice.payments + PaymentRecord(
                    id = "payment_${UUID.randomUUID()}",
                    amount = additionalPayment,
                    date = ReportDateUtils.displayDate(ReportDateUtils.currentDate()),
                    method = "Manual",
                    reference = "Status updated in app",
                    notes = ""
                )
            } else {
                invoice.payments
            }
            transaction.update(
                reference,
                mapOf(
                    "status" to resultingStatus.name,
                    "paidAmount" to paidAmount,
                    "payments" to payments.map { it.toFirestoreMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun recordPayment(invoiceId: String, payment: PaymentRecord) {
        require(payment.amount > 0.0) { "Enter a payment amount greater than zero." }
        val reference = collection("invoices").document(invoiceId)
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(reference)
            if (!snapshot.exists()) throw IllegalStateException("Invoice no longer exists.")
            val invoice = snapshot.toInvoice()
            if (payment.amount > invoice.balanceDue) throw PaymentExceedsBalanceException()
            val newPaidAmount = invoice.paidAmount + payment.amount
            val newStatus = when {
                newPaidAmount >= invoice.grandTotal -> InvoiceStatus.PAID
                newPaidAmount > 0.0 -> InvoiceStatus.HALF_PAID
                else -> InvoiceStatus.PENDING
            }
            val payments = invoice.payments + payment
            transaction.update(
                reference,
                mapOf(
                    "paidAmount" to newPaidAmount,
                    "status" to newStatus.name,
                    "payments" to payments.map { it.toFirestoreMap() },
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun duplicateInvoice(invoiceId: String): String {
        val profileReference = businessDocument()
        val invoiceReference = collection("invoices").document(invoiceId)
        val invoices = collection("invoices")
        return firestore.runTransaction { transaction ->
            val originalSnapshot = transaction.get(invoiceReference)
            if (!originalSnapshot.exists()) throw IllegalStateException("Invoice no longer exists.")
            val profile = transaction.get(profileReference)
            val defaults = emptyBusinessProfile()
            val prefix = profile.getString("prefix") ?: defaults.prefix
            val nextNumber = profile.getString("nextNumber") ?: defaults.nextNumber
            val newId = prefix + nextNumber
            val newReference = invoices.document(newId)
            if (transaction.get(newReference).exists()) throw DuplicateInvoiceNumberException()

            val duplicate = originalSnapshot.toInvoice().copy(
                id = newId,
                status = InvoiceStatus.PENDING,
                paidAmount = 0.0,
                payments = emptyList()
            )
            transaction.set(newReference, duplicate.toFirestoreMap() + createdAndUpdatedAt())
            transaction.set(
                profileReference,
                mapOf("prefix" to prefix, "nextNumber" to incrementNumber(nextNumber)) + updatedAt(),
                SetOptions.merge()
            )
            newId
        }.await()
    }

    suspend fun deleteInvoice(invoiceId: String) {
        collection("invoices").document(invoiceId).delete().await()
    }

    private fun updatedAt() = mapOf("updatedAt" to FieldValue.serverTimestamp())
    private fun createdAndUpdatedAt() = mapOf(
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun incrementNumber(value: String): String =
        ((value.toIntOrNull() ?: 0) + 1).toString().padStart(maxOf(value.length, 3), '0')

    private fun emptyBusinessProfile() = BusinessProfile().copy(
        name = "",
        tagline = "",
        email = "",
        phone = "",
        website = "",
        addressLine1 = "",
        addressLine2 = "",
        city = "",
        state = "",
        postalCode = "",
        country = "India",
        taxLabel = "GSTIN",
        taxNumber = "",
        defaultTaxRate = 18.0,
        pricesIncludeTax = false,
        bankName = "",
        accountHolder = "",
        accountNumber = "",
        ifscCode = "",
        branch = "",
        upiId = "",
        prefix = "INV-",
        nextNumber = "001",
        currency = "INR (₹)",
        paymentTerms = "Net 15 Days",
        defaultNotes = ""
    )

    private fun DocumentSnapshot.valueString(field: String, fallback: String = "") = getString(field) ?: fallback
    private fun DocumentSnapshot.valueDouble(field: String, fallback: Double = 0.0): Double =
        getDouble(field) ?: getLong(field)?.toDouble() ?: fallback
    private fun DocumentSnapshot.valueInt(field: String, fallback: Int = 0): Int =
        getLong(field)?.toInt() ?: getDouble(field)?.toInt() ?: fallback
    private fun DocumentSnapshot.valueBoolean(field: String, fallback: Boolean = false) = getBoolean(field) ?: fallback

    private fun DocumentSnapshot.toClient() = Client(
        id = id,
        name = valueString("name"),
        email = getString("email")?.takeIf(String::isNotBlank),
        phone = valueString("phone"),
        address = valueString("address"),
        city = valueString("city"),
        state = valueString("state"),
        postalCode = valueString("postalCode"),
        country = valueString("country", "India"),
        taxNumber = valueString("taxNumber"),
        notes = valueString("notes"),
        invoiceCount = valueInt("invoiceCount"),
        totalBilled = valueDouble("totalBilled"),
        totalPaid = valueDouble("totalPaid"),
        totalDue = valueDouble("totalDue"),
        isArchived = valueBoolean("isArchived")
    )

    private fun Client.toFirestoreMap() = mapOf(
        "name" to name.trim(),
        "email" to email?.trim()?.takeIf(String::isNotBlank),
        "phone" to phone.trim(),
        "address" to address.trim(),
        "city" to city.trim(),
        "state" to state.trim(),
        "postalCode" to postalCode.trim(),
        "country" to country,
        "taxNumber" to taxNumber.trim(),
        "notes" to notes.trim(),
        "isArchived" to isArchived
    )

    private fun DocumentSnapshot.toProduct() = Product(
        id = id,
        name = valueString("name"),
        description = valueString("description"),
        sku = valueString("sku"),
        unitPrice = valueDouble("unitPrice"),
        unit = valueString("unit", "pcs"),
        defaultQuantity = valueInt("defaultQuantity", 1),
        taxRate = valueDouble("taxRate", 18.0),
        isArchived = valueBoolean("isArchived")
    )

    private fun Product.toFirestoreMap() = mapOf(
        "name" to name.trim(),
        "description" to description.trim(),
        "sku" to sku.trim(),
        "unitPrice" to unitPrice,
        "unit" to unit,
        "defaultQuantity" to defaultQuantity,
        "taxRate" to taxRate,
        "isArchived" to isArchived
    )

    private fun DocumentSnapshot.toBusinessProfile() = BusinessProfile(
        name = valueString("name"),
        tagline = valueString("tagline"),
        email = valueString("email"),
        phone = valueString("phone"),
        website = valueString("website"),
        logoUrl = valueString("logoUrl"),
        addressLine1 = valueString("addressLine1"),
        addressLine2 = valueString("addressLine2"),
        city = valueString("city"),
        state = valueString("state"),
        postalCode = valueString("postalCode"),
        country = valueString("country", "India"),
        taxLabel = valueString("taxLabel", "GSTIN"),
        taxNumber = valueString("taxNumber"),
        defaultTaxRate = valueDouble("defaultTaxRate", 18.0),
        pricesIncludeTax = valueBoolean("pricesIncludeTax"),
        bankName = valueString("bankName"),
        accountHolder = valueString("accountHolder"),
        accountNumber = valueString("accountNumber"),
        ifscCode = valueString("ifscCode"),
        branch = valueString("branch"),
        upiId = valueString("upiId"),
        prefix = valueString("prefix", "INV-"),
        nextNumber = valueString("nextNumber", "001"),
        currency = valueString("currency", "INR (₹)"),
        paymentTerms = valueString("paymentTerms", "Net 15 Days"),
        defaultNotes = valueString("defaultNotes")
    )

    private fun BusinessProfile.toFirestoreMap() = mapOf(
        "name" to name.trim(),
        "tagline" to tagline.trim(),
        "email" to email.trim(),
        "phone" to phone.trim(),
        "website" to website.trim(),
        "logoUrl" to logoUrl,
        "addressLine1" to addressLine1.trim(),
        "addressLine2" to addressLine2.trim(),
        "city" to city.trim(),
        "state" to state.trim(),
        "postalCode" to postalCode.trim(),
        "country" to country,
        "taxLabel" to taxLabel.trim(),
        "taxNumber" to taxNumber.trim(),
        "defaultTaxRate" to defaultTaxRate,
        "pricesIncludeTax" to pricesIncludeTax,
        "bankName" to bankName.trim(),
        "accountHolder" to accountHolder.trim(),
        "accountNumber" to accountNumber.trim(),
        "ifscCode" to ifscCode.trim(),
        "branch" to branch.trim(),
        "upiId" to upiId.trim(),
        "prefix" to prefix.trim(),
        "nextNumber" to nextNumber.trim(),
        "currency" to currency,
        "paymentTerms" to paymentTerms,
        "defaultNotes" to defaultNotes.trim()
    )

    private fun DocumentSnapshot.toInvoice(): Invoice {
        val itemMaps = get("items") as? List<*>
        val paymentMaps = get("payments") as? List<*>
        val invoice = Invoice(
            id = valueString("id", id),
            clientId = valueString("clientId"),
            clientName = valueString("clientName"),
            clientEmail = getString("clientEmail")?.takeIf(String::isNotBlank),
            issueDate = valueString("issueDate"),
            dueDate = valueString("dueDate"),
            items = itemMaps.orEmpty().mapNotNull { (it as? Map<*, *>)?.toInvoiceItem() },
            discount = valueDouble("discount"),
            status = runCatching { InvoiceStatus.valueOf(valueString("status", InvoiceStatus.PENDING.name)) }
                .getOrDefault(InvoiceStatus.PENDING),
            paidAmount = valueDouble("paidAmount"),
            notes = valueString("notes"),
            terms = valueString("terms"),
            currencyCountry = valueString("currencyCountry"),
            payments = paymentMaps.orEmpty().mapNotNull { (it as? Map<*, *>)?.toPaymentRecord() }
        )
        return if (
            invoice.balanceDue > 0.0 && ReportDateUtils.isBeforeToday(invoice.dueDate) &&
            (invoice.status == InvoiceStatus.PENDING || invoice.status == InvoiceStatus.HALF_PAID)
        ) {
            invoice.copy(status = InvoiceStatus.OVERDUE)
        } else {
            invoice
        }
    }

    private fun Invoice.toFirestoreMap() = mapOf(
        "id" to id,
        "clientId" to clientId,
        "clientName" to clientName,
        "clientEmail" to clientEmail,
        "issueDate" to issueDate,
        "dueDate" to dueDate,
        "items" to items.map { it.toFirestoreMap() },
        "discount" to discount,
        "status" to status.name,
        "paidAmount" to paidAmount,
        "notes" to notes,
        "terms" to terms,
        "currencyCountry" to currencyCountry,
        "payments" to payments.map { it.toFirestoreMap() }
    )

    private fun Map<*, *>.string(field: String, fallback: String = "") = this[field] as? String ?: fallback
    private fun Map<*, *>.number(field: String, fallback: Double = 0.0) = (this[field] as? Number)?.toDouble() ?: fallback

    private fun Map<*, *>.toInvoiceItem() = InvoiceItem(
        id = string("id"),
        name = string("name"),
        quantity = number("quantity").toInt(),
        unitPrice = number("unitPrice"),
        taxRate = number("taxRate", 18.0)
    )

    private fun InvoiceItem.toFirestoreMap() = mapOf(
        "id" to id,
        "name" to name,
        "quantity" to quantity,
        "unitPrice" to unitPrice,
        "taxRate" to taxRate
    )

    private fun Map<*, *>.toPaymentRecord() = PaymentRecord(
        id = string("id"),
        amount = number("amount"),
        date = string("date"),
        method = string("method"),
        reference = string("reference"),
        notes = string("notes")
    )

    private fun PaymentRecord.toFirestoreMap() = mapOf(
        "id" to id,
        "amount" to amount,
        "date" to date,
        "method" to method,
        "reference" to reference,
        "notes" to notes
    )
}
