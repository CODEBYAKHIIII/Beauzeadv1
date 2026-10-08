package com.example.data

import android.content.ContentValues
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.NumberFormat
import java.util.Locale
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object DocumentExports {
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val INVOICE_ITEMS_PER_PAGE = 10

    fun createInvoicePdf(
        context: Context,
        invoice: Invoice,
        business: BusinessProfile,
        client: Client?
    ): File {
        // Best-effort logo: a failed logo download must never break the whole invoice export.
        val logo = business.logoUrl
            .takeIf(String::isNotBlank)
            ?.let { url -> runCatching { downloadLogo(url) }.getOrNull() }
        val currency = currencyFormatter(business.country)
        val pages = invoice.items.chunked(INVOICE_ITEMS_PER_PAGE).ifEmpty { listOf(emptyList()) }
        val document = PdfDocument()
        try {
            pages.forEachIndexed { pageIndex, pageItems ->
                val page = document.startPage(
                    PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageIndex + 1).create()
                )
                drawInvoicePage(
                    canvas = page.canvas,
                    invoice = invoice,
                    business = business,
                    client = client,
                    items = pageItems,
                    firstItemNumber = pageIndex * INVOICE_ITEMS_PER_PAGE + 1,
                    currency = currency,
                    logo = logo,
                    isFinalPage = pageIndex == pages.lastIndex,
                    pageNumber = pageIndex + 1,
                    totalPages = pages.size
                )
                document.finishPage(page)
            }
            val file = File.createTempFile("waves-invoice-", ".pdf", context.cacheDir)
            try {
                FileOutputStream(file).use(document::writeTo)
            } catch (exception: Exception) {
                file.delete()
                throw exception
            }
            return file
        } finally {
            document.close()
            logo?.recycle()
        }
    }

    fun createReportPdf(context: Context, month: String, invoices: List<Invoice>): File {
        val monthInvoices = invoices.filter { ReportDateUtils.monthKey(it.issueDate) == month }
        val activeInvoices = monthInvoices.filterNot {
            it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF
        }
        val collected = invoices.sumOf { invoice ->
            invoice.payments.filter { ReportDateUtils.monthKey(it.date) == month }.sumOf { it.amount }
        }
        return createPdf(
            context,
            "Invoice report $month",
            buildList {
                add("Invoice report - ${ReportDateUtils.displayMonth(month) ?: month}")
                add("Invoiced: ${money(activeInvoices.sumOf { it.grandTotal })}")
                add("Collected this month: ${money(collected)}")
                add("Outstanding: ${money(activeInvoices.sumOf { it.balanceDue })}")
                add("Invoice count: ${activeInvoices.size}")
                add("")
                add("Invoice | Client | Issue date | Due date | Status | Total | Paid | Balance")
                activeInvoices.forEach { invoice ->
                    add("${invoice.id} | ${invoice.clientName} | ${invoice.issueDate} | ${invoice.dueDate} | ${invoice.status.label} | ${money(invoice.grandTotal)} | ${money(invoice.paidAmount)} | ${money(invoice.balanceDue)}")
                }
            }
        )
    }

    fun createReportCsv(context: Context, month: String, invoices: List<Invoice>): File {
        val monthInvoices = invoices.filter { ReportDateUtils.monthKey(it.issueDate) == month }
        val activeInvoices = monthInvoices.filterNot {
            it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF
        }
        val file = File.createTempFile("waves-report-", ".csv", context.cacheDir)
        file.writeText(
            buildString {
                appendLine(listOf("Invoice", "Client", "Issue date", "Due date", "Status", "Subtotal", "Discount", "Tax", "Total", "Paid", "Balance due").joinToString(",") { csv(it) })
                activeInvoices.forEach { invoice ->
                    appendLine(
                        listOf(
                            invoice.id,
                            invoice.clientName,
                            invoice.issueDate,
                            invoice.dueDate,
                            invoice.status.label,
                            money(invoice.subtotal),
                            money(invoice.discount),
                            money(invoice.taxAmount),
                            money(invoice.grandTotal),
                            money(invoice.paidAmount),
                            money(invoice.balanceDue)
                        ).joinToString(",") { csv(it) }
                    )
                }
                appendLine()
                appendLine("${csv("Report month")},${csv(ReportDateUtils.displayMonth(month) ?: month)}")
            },
            Charsets.UTF_8
        )
        return file
    }

    fun createClientStatementPdf(context: Context, client: Client, invoices: List<Invoice>): File =
        createPdf(
            context,
            "Statement ${client.name}",
            buildList {
                add("Account statement")
                add(client.name)
                addAll(listOf(client.email.orEmpty(), client.phone, client.address, "${client.city}, ${client.country}")
                    .filter(String::isNotBlank))
                add("")
                add("Invoice | Issue date | Due date | Status | Total | Paid | Balance")
                invoices.forEach { invoice ->
                    add("${invoice.id} | ${invoice.issueDate} | ${invoice.dueDate} | ${invoice.status.label} | ${money(invoice.grandTotal)} | ${money(invoice.paidAmount)} | ${money(invoice.balanceDue)}")
                }
                add("")
                add("Total billed: ${money(invoices.filterNot { it.status == InvoiceStatus.CANCELLED || it.status == InvoiceStatus.WRITTEN_OFF }.sumOf { it.grandTotal })}")
                add("Total paid: ${money(invoices.sumOf { it.paidAmount })}")
                add("Balance due: ${money(invoices.sumOf { it.balanceDue })}")
            }
        )

    fun saveToDownloads(context: Context, file: File, displayName: String, mimeType: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create a Downloads file.")
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: error("Unable to open the Downloads file.")
                output.use { file.inputStream().use { input -> input.copyTo(output) } }
                context.contentResolver.update(
                    uri,
                    ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                    null,
                    null
                )
                return "Downloads/$displayName"
            } catch (exception: Exception) {
                context.contentResolver.delete(uri, null, null)
                throw exception
            }
        }

        val directory = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: error("Downloads storage is unavailable.")
        if (!directory.exists() && !directory.mkdirs()) error("Unable to create the Downloads folder.")
        file.copyTo(File(directory, displayName), overwrite = true)
        return directory.resolve(displayName).absolutePath
    }

    fun share(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun copyInvoiceNumber(context: Context, invoiceId: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Invoice number", invoiceId))
    }

    private fun drawInvoicePage(
        canvas: Canvas,
        invoice: Invoice,
        business: BusinessProfile,
        client: Client?,
        items: List<InvoiceItem>,
        firstItemNumber: Int,
        currency: NumberFormat,
        logo: Bitmap?,
        isFinalPage: Boolean,
        pageNumber: Int = 1,
        totalPages: Int = 1
    ) {
        val ink = Color.rgb(28, 35, 32)
        val green = Color.rgb(24, 105, 72)
        val muted = Color.rgb(92, 103, 97)
        val rule = Color.rgb(218, 226, 220)
        val pale = Color.rgb(244, 248, 245)

        // Shared layout grid. Every right-aligned value in the document shares RIGHT_EDGE,
        // so the invoice number, dates, table totals and summary totals form one clean line.
        val leftEdge = 36f
        val contentEdge = 559f
        val rightEdge = 551f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ink
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val bold = Paint(paint).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val small = Paint(paint).apply { textSize = 8f }
        val label = Paint(bold).apply {
            color = green
            textSize = 8f
        }
        val totalPaint = Paint(bold).apply {
            color = Color.BLACK
            textSize = 13f
        }

        fun text(value: String, x: Float, y: Float, using: Paint = paint) {
            canvas.drawText(value, x, y, using)
        }
        fun rightText(value: String, right: Float, y: Float, using: Paint = paint) {
            using.textAlign = Paint.Align.RIGHT
            canvas.drawText(value, right, y, using)
            using.textAlign = Paint.Align.LEFT
        }
        // Vertically centre a text row: baseline sits 0.35 x textSize below the row centre.
        fun centeredBaseline(rowCenter: Float, using: Paint): Float = rowCenter + using.textSize * 0.35f
        fun fit(value: String, maxWidth: Float, using: Paint): String {
            if (using.measureText(value) <= maxWidth) return value
            val ellipsis = "..."
            var end = value.length
            while (end > 0 && using.measureText(value.substring(0, end) + ellipsis) > maxWidth) end--
            return value.substring(0, end) + ellipsis
        }
        fun rule(y: Float, from: Float = leftEdge, to: Float = contentEdge) {
            canvas.drawLine(from, y, to, y, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = rule
                strokeWidth = 0.8f
            })
        }

        canvas.drawColor(Color.WHITE)

        // ---------- Header: logo + business block ----------
        if (logo != null) {
            canvas.drawBitmap(logo, null, Rect(36, 30, 92, 86), null)
        } else {
            canvas.drawRoundRect(
                RectF(36f, 30f, 92f, 86f),
                5f,
                5f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pale }
            )
        }

        val businessX = 104f
        text(fit(business.name, 245f, bold), businessX, 44f, Paint(bold).apply {
            color = ink
            textSize = 14f
        })
        if (business.tagline.isNotBlank()) text(fit(business.tagline, 245f, small), businessX, 58f, small)
        val businessAddress = listOf(
            business.addressLine1,
            business.addressLine2,
            listOf(business.city, business.state, business.postalCode, business.country)
                .filter(String::isNotBlank).joinToString(", ")
        ).filter(String::isNotBlank)
        businessAddress.take(3).forEachIndexed { index, line ->
            text(fit(line, 245f, small), businessX, 70f + index * 11f, small)
        }

        // ---------- Header: invoice meta; labels share a column, values share the right edge ----------
        val titlePaint = Paint(bold).apply {
            color = green
            textSize = 21f
            textAlign = Paint.Align.RIGHT
        }
        text("INVOICE", rightEdge, 40f, titlePaint)
        rightText(fit(invoice.id, 175f, bold), rightEdge, 60f, bold)
        val dateLabelX = 436f
        text("Issue date", dateLabelX, 77f, small)
        rightText(formatInvoiceDate(invoice.issueDate), rightEdge, 77f, small)
        text("Due date", dateLabelX, 90f, small)
        rightText(formatInvoiceDate(invoice.dueDate), rightEdge, 90f, small)
        rule(105f)

        // ---------- Bill To / Business tax block ----------
        canvas.drawRoundRect(
            RectF(36f, 116f, 559f, 199f),
            6f,
            6f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pale }
        )
        text("BILL TO", 48f, 132f, label)
        text(fit(client?.name ?: invoice.clientName, 280f, bold), 48f, 149f, bold)
        val clientAddress = listOf(
            client?.address.orEmpty(),
            listOfNotNull(client?.city, client?.state, client?.postalCode, client?.country)
                .filter(String::isNotBlank).joinToString(", ")
        ).filter(String::isNotBlank)
        clientAddress.take(2).forEachIndexed { index, line ->
            text(fit(line, 280f, small), 48f, 163f + index * 11f, small)
        }
        val clientContact = listOfNotNull(
            client?.email?.takeIf(String::isNotBlank),
            client?.phone?.takeIf(String::isNotBlank),
            client?.taxNumber?.takeIf(String::isNotBlank)?.let { "Tax: $it" }
        ).joinToString("  |  ")
        if (clientContact.isNotBlank()) text(fit(clientContact, 280f, small), 48f, 190f, small)
        if (business.taxNumber.isNotBlank()) {
            text("BUSINESS TAX NUMBER", 350f, 132f, label)
            text(fit("${business.taxLabel}: ${business.taxNumber}", 195f, small), 350f, 149f, small)
        }

        // ---------- Items table ----------
        text("INVOICE ITEMS", 36f, 222f, Paint(bold).apply { textSize = 11f })
        val tableTop = 238f
        val tableHeight = 22f
        canvas.drawRoundRect(
            RectF(36f, tableTop, 559f, tableTop + tableHeight),
            3f,
            3f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ink }
        )
        val whiteBold = Paint(bold).apply { color = Color.WHITE; textSize = 7.5f }
        val headerBaseline = centeredBaseline(tableTop + tableHeight / 2f, whiteBold)
        text("SL NO", 45f, headerBaseline, whiteBold)
        text("ITEM", 80f, headerBaseline, whiteBold)
        rightText("QTY", 342f, headerBaseline, whiteBold)
        rightText("UNIT PRICE", 414f, headerBaseline, whiteBold)
        rightText("TAX", 481f, headerBaseline, whiteBold)
        rightText("TOTAL", rightEdge, headerBaseline, whiteBold)

        val rowPaint = Paint(paint).apply { textSize = 8.5f }
        val rowBold = Paint(bold).apply { textSize = 8.5f }
        val rowHeight = 26f
        var rowY = tableTop + tableHeight
        items.forEachIndexed { index, item ->
            val rowBottom = rowY + rowHeight
            if (index % 2 == 1) {
                canvas.drawRect(36f, rowY, 559f, rowBottom, Paint().apply { color = pale })
            }
            val rowBaseline = centeredBaseline(rowY + rowHeight / 2f, rowPaint)
            text((firstItemNumber + index).toString(), 45f, rowBaseline, rowPaint)
            text(fit(item.name, 220f, rowPaint), 80f, rowBaseline, rowPaint)
            rightText(item.quantity.toString(), 342f, rowBaseline, rowPaint)
            rightText(currency.format(item.unitPrice), 414f, rowBaseline, rowPaint)
            rightText(currency.format(item.taxAmount), 481f, rowBaseline, rowPaint)
            rightText(currency.format(item.total), rightEdge, rowBaseline, rowBold)
            canvas.drawLine(36f, rowBottom, 559f, rowBottom, Paint().apply {
                color = rule
                strokeWidth = 0.5f
            })
            rowY = rowBottom
        }

        // ---------- Summary + terms + account details (final page only) ----------
        if (isFinalPage) {
            val summaryTop = maxOf(352f, rowY + 16f)
            val summaryLabelX = 340f

            // Left column: terms, top-aligned with the Subtotal row.
            text("TERMS", 36f, summaryTop + 12f, label)
            val terms = invoice.terms.ifBlank { "Nill" }
            text(fit(terms, 280f, small), 36f, summaryTop + 27f, small)

            // Right column: totals; every value sits on the shared right edge.
            text("Subtotal", summaryLabelX, summaryTop + 12f, small)
            rightText(if (invoice.subtotal == 0.0) "Nill" else currency.format(invoice.subtotal), rightEdge, summaryTop + 12f, small)
            text("Tax", summaryLabelX, summaryTop + 28f, small)
            rightText(if (invoice.taxAmount == 0.0) "Nill" else currency.format(invoice.taxAmount), rightEdge, summaryTop + 28f, small)
            text("Discount", summaryLabelX, summaryTop + 44f, small)
            rightText(if (invoice.discount == 0.0) "Nill" else currency.format(invoice.discount), rightEdge, summaryTop + 44f, small)
            rule(summaryTop + 52f, from = summaryLabelX, to = rightEdge)
            text("TOTAL", summaryLabelX, summaryTop + 70f, totalPaint)
            rightText(currency.format(invoice.grandTotal), rightEdge, summaryTop + 70f, totalPaint)
            val duePaint = Paint(bold).apply {
                color = Color.BLACK
                textSize = 9f
            }
            text("Due date", summaryLabelX, summaryTop + 92f, duePaint)
            rightText(formatInvoiceDate(invoice.dueDate), rightEdge, summaryTop + 92f, duePaint)

            // Account details: strict two-column grid with identical per-row baselines.
            rule(summaryTop + 108f)
            text("ACCOUNT DETAILS FOR PAYMENT", 36f, summaryTop + 122f, label)
            val accountLines = listOf(
                business.bankName.takeIf(String::isNotBlank),
                business.accountHolder.takeIf(String::isNotBlank)?.let { "Account holder: $it" },
                business.accountNumber.takeIf(String::isNotBlank)?.let { "Account no: $it" },
                business.ifscCode.takeIf(String::isNotBlank)?.let { "IFSC: $it" },
                business.branch.takeIf(String::isNotBlank)?.let { "Branch: $it" },
                business.upiId.takeIf(String::isNotBlank)?.let { "UPI: $it" }
            ).filterNotNull()
            if (accountLines.isEmpty()) {
                text("Nill", 36f, summaryTop + 138f, small)
            } else {
                val split = (accountLines.size + 1) / 2
                accountLines.forEachIndexed { index, value ->
                    val column = if (index < split) 0 else 1
                    val row = if (index < split) index else index - split
                    text(fit(value, 245f, small), 36f + column * 265f, summaryTop + 138f + row * 12f, small)
                }
            }
        }
        drawInvoiceFooter(canvas, business, ink, muted, rule, green, pageNumber, totalPages)
    }

    private fun drawInvoiceFooter(
        canvas: Canvas,
        business: BusinessProfile,
        ink: Int,
        muted: Int,
        rule: Int,
        green: Int,
        pageNumber: Int = 1,
        totalPages: Int = 1
    ) {
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = rule
            strokeWidth = 0.8f
        }
        canvas.drawLine(36f, 789f, 559f, 789f, dividerPaint)
        val messagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = green
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Thank you for your business!", 297.5f, 805f, messagePaint)

        // Three fixed footer slots so contact blocks never drift.
        val slots = listOf(36f, 210f, 384f)
        val contacts = listOf(
            Pair("web", business.website.removePrefix("https://").removePrefix("http://").takeIf(String::isNotBlank)),
            Pair("phone", business.phone.takeIf(String::isNotBlank)),
            Pair("email", business.email.takeIf(String::isNotBlank))
        ).filter { it.second != null }
        val contactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = muted
            textSize = 7f
        }
        contacts.forEachIndexed { index, (kind, value) ->
            val x = slots.getOrElse(index) { 36f + index * 174f }
            drawContactIcon(canvas, kind, x, 820f, green)
            val availableWidth = 158f
            val formatted = value.orEmpty()
            var end = formatted.length
            while (end > 0 && contactPaint.measureText(formatted.substring(0, end)) > availableWidth) end--
            canvas.drawText(formatted.substring(0, end), x + 10f, 822f, contactPaint)
        }

        if (totalPages > 1) {
            val pagePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted
                textSize = 7f
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("Page $pageNumber of $totalPages", 559f, 834f, pagePaint)
        }
    }

    private fun drawContactIcon(canvas: Canvas, kind: String, x: Float, y: Float, color: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        when (kind) {
            "web" -> {
                canvas.drawCircle(x, y - 2f, 4f, paint)
                canvas.drawOval(RectF(x - 2f, y - 6f, x + 2f, y + 2f), paint)
                canvas.drawLine(x - 3.5f, y - 2f, x + 3.5f, y - 2f, paint)
            }
            "phone" -> {
                val path = Path().apply {
                    moveTo(x - 3f, y - 6f)
                    cubicTo(x - 5f, y - 3f, x + 1f, y + 4f, x + 4f, y + 1f)
                    moveTo(x - 3f, y - 6f)
                    lineTo(x - 1f, y - 4f)
                    lineTo(x - 2f, y - 2f)
                    moveTo(x + 2f, y)
                    lineTo(x + 4f, y + 1f)
                }
                canvas.drawPath(path, paint)
            }
            else -> {
                canvas.drawRoundRect(RectF(x - 5f, y - 6f, x + 5f, y + 2f), 1f, 1f, paint)
                canvas.drawLine(x - 4f, y - 5f, x, y - 2f, paint)
                canvas.drawLine(x, y - 2f, x + 4f, y - 5f, paint)
            }
        }
    }

    private fun currencyFormatter(country: String): NumberFormat {
        val code = when (country.trim().lowercase(Locale.ROOT)) {
            "uk", "united kingdom" -> "GB"
            "usa", "united states", "united states of america" -> "US"
            "uae", "united arab emirates" -> "AE"
            else -> Locale.getISOCountries().firstOrNull { code ->
                Locale.Builder().setRegion(code).build().getDisplayCountry(Locale.ENGLISH)
                    .equals(country.trim(), ignoreCase = true)
            } ?: error("Select a supported country before creating an invoice.")
        }
        val locale = Locale.Builder().setLanguage("en").setRegion(code).build()
        return NumberFormat.getCurrencyInstance(locale).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    private fun formatInvoiceDate(value: String): String {
        val formats = listOf(
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH),
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)
        )
        val date = formats.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(value.trim(), formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        } ?: return value
        return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT))
    }

    private fun createPdf(
        context: Context,
        title: String,
        lines: List<String>,
        logoUrl: String? = null
    ): File {
        val logoBitmap = logoUrl
            ?.takeIf(String::isNotBlank)
            ?.let(::downloadLogo)
        val document = PdfDocument()
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val titlePaint = Paint(paint).apply {
            textSize = 17f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        var canvas: android.graphics.Canvas? = null
        var y = 0f
        fun newPage() {
            page?.let(document::finishPage)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page!!.canvas
            logoBitmap?.takeIf { pageNumber == 1 }?.let {
                canvas!!.drawBitmap(it, null, Rect(42, 30, 94, 82), null)
            }
            canvas!!.drawText(title, if (logoBitmap != null && pageNumber == 1) 110f else 42f, 48f, titlePaint)
            y = if (logoBitmap != null && pageNumber == 1) 98f else 78f
        }
        try {
            newPage()
            lines.forEach { line ->
                if (y > 795f) newPage()
                val printable = line
                val maxWidth = 510f
                var remaining = printable
                while (remaining.isNotEmpty()) {
                    val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
                    canvas!!.drawText(remaining.substring(0, count), 42f, y, paint)
                    remaining = remaining.substring(count)
                    y += 16f
                    if (y > 795f && remaining.isNotEmpty()) newPage()
                }
                y += 4f
            }
            page?.let(document::finishPage)

            val file = File.createTempFile("waves-export-", ".pdf", context.cacheDir)
            try {
                FileOutputStream(file).use(document::writeTo)
            } catch (exception: Exception) {
                file.delete()
                throw exception
            }
            return file
        } finally {
            document.close()
            logoBitmap?.recycle()
        }
    }

    private fun downloadLogo(url: String): Bitmap {
        val connection = URL(url).openConnection().apply {
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        val bitmap = connection.getInputStream().use { stream ->
            BitmapFactory.decodeStream(stream) ?: error("The business logo could not be decoded.")
        }
        if (bitmap.width != LOGO_OUTPUT_DIMENSION || bitmap.height != LOGO_OUTPUT_DIMENSION) {
            bitmap.recycle()
            error("Re-crop the business logo to 500x500 px before exporting this invoice.")
        }
        return bitmap
    }

    private const val LOGO_OUTPUT_DIMENSION = 500

    private fun csv(value: String): String {
        val safeValue = if (value.firstOrNull()?.let { it in setOf('=', '+', '-', '@', '\t', '\r') } == true) {
            "'$value"
        } else {
            value
        }
        return "\"${safeValue.replace("\"", "\"\"")}\""
    }

    private fun money(value: Double): String = String.format(Locale.US, "%.2f", value)
}
