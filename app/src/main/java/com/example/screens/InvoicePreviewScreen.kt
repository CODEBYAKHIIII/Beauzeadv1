package com.example.screens

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.components.WavesSecondaryButton
import com.example.components.showDemoToast
import com.example.data.BusinessProfile
import com.example.data.Client
import com.example.data.DocumentExports
import com.example.data.FirestoreDataRepository
import com.example.data.FirestoreState
import com.example.data.Invoice
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.OnPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun InvoicePreviewScreen(
    invoiceId: String,
    onNavigateBack: () -> Unit,
    onNavigateToInvoiceDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isRendering by remember { mutableStateOf(true) }
    var renderError by remember { mutableStateOf<String?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(0) }
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val invoiceState by remember(invoiceId) { FirestoreDataRepository.observeInvoice(invoiceId) }
        .collectAsState(initial = FirestoreState.Loading)
    val businessState by remember { FirestoreDataRepository.observeBusinessProfile() }
        .collectAsState(initial = FirestoreState.Loading)
    val clientsState by remember { FirestoreDataRepository.observeClients() }
        .collectAsState(initial = FirestoreState.Loading)
    val invoice = (invoiceState as? FirestoreState.Data<*>)?.value as? Invoice
    val business = (businessState as? FirestoreState.Data<*>)?.value as? BusinessProfile
    val clients = (clientsState as? FirestoreState.Data<*>)?.value as? List<Client>
    val client = clients?.find { it.id == invoice?.clientId }

    if (invoiceState is FirestoreState.Loading || businessState is FirestoreState.Loading || clientsState is FirestoreState.Loading) {
        StateScreen(type = StateType.LOADING, message = "Loading invoice preview...")
        return
    }
    val loadError = (invoiceState as? FirestoreState.Failure)?.message
        ?: (businessState as? FirestoreState.Failure)?.message
        ?: (clientsState as? FirestoreState.Failure)?.message
    if (loadError != null || invoice == null || business == null) {
        StateScreen(
            type = StateType.ERROR,
            title = "Preview Unavailable",
            message = loadError ?: "This invoice could not be found.",
            onPrimaryClick = onNavigateBack
        )
        return
    }

    LaunchedEffect(invoice, business, client) {
        isRendering = true
        renderError = null
        pageBitmap = null
        pdfFile?.delete()
        pdfFile = null
        try {
            val generated = withContext(Dispatchers.IO) {
                val file = DocumentExports.createInvoicePdf(context, invoice, business, client)
                try {
                    val count = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                        PdfRenderer(descriptor).use { it.pageCount }
                    }
                    file to count
                } catch (exception: Exception) {
                    file.delete()
                    throw exception
                }
            }
            pdfFile = generated.first
            pageCount = generated.second
            currentPage = 0
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            renderError = exception.localizedMessage ?: "Unable to render the invoice PDF."
        } finally {
            isRendering = false
        }
    }

    LaunchedEffect(pdfFile, currentPage) {
        val file = pdfFile ?: return@LaunchedEffect
        try {
            val bitmap = withContext(Dispatchers.IO) {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                    PdfRenderer(descriptor).use { renderer ->
                        renderer.openPage(currentPage).use { page ->
                            val width = 900
                            val height = width * page.height / page.width
                            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
                                page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            }
                        }
                    }
                }
            }
            pageBitmap?.recycle()
            pageBitmap = bitmap
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            renderError = exception.localizedMessage ?: "Unable to render this invoice page."
        }
    }

    DisposableEffect(pdfFile) {
        onDispose {
            pageBitmap?.recycle()
            pdfFile?.delete()
        }
    }

    fun exportInvoice(share: Boolean) {
        if (isExporting) return
        coroutineScope.launch {
            isExporting = true
            try {
                val pdf = withContext(Dispatchers.IO) {
                    DocumentExports.createInvoicePdf(context, invoice, business, client)
                }
                if (share) {
                    DocumentExports.share(context, pdf, "application/pdf", "Share invoice ${invoice.id}")
                } else {
                    val location = withContext(Dispatchers.IO) {
                        DocumentExports.saveToDownloads(
                            context,
                            pdf,
                            "invoice-${invoice.id}.pdf",
                            "application/pdf"
                        )
                    }
                    showDemoToast(context, "Invoice PDF saved to $location")
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                showDemoToast(context, exception.localizedMessage ?: "Unable to export invoice PDF.")
            } finally {
                isExporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Invoice preview",
                onBackClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = { exportInvoice(share = true) },
                        modifier = Modifier.testTag("preview_share_header_button")
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share invoice", tint = OnPrimary)
                    }
                }
            )
        },
        containerColor = BackgroundColor,
        modifier = modifier.testTag("invoice_preview_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isRendering -> CircularProgressIndicator(color = EmeraldInk)
                renderError != null -> Text(
                    text = renderError.orEmpty(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
                pageBitmap == null -> CircularProgressIndicator(color = EmeraldInk)
                pageBitmap != null -> {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "Invoice PDF page ${currentPage + 1}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(595f / 842f)
                            .testTag("invoice_pdf_page_preview")
                    )
                    if (pageCount > 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { currentPage = (currentPage - 1).coerceAtLeast(0) },
                                enabled = currentPage > 0
                            ) {
                                Icon(Icons.Filled.ArrowBack, contentDescription = "Previous page")
                            }
                            Text(
                                "Page ${currentPage + 1} of $pageCount",
                                color = TextSecondary,
                                fontSize = 13.sp
                            )
                            IconButton(
                                onClick = { currentPage = (currentPage + 1).coerceAtMost(pageCount - 1) },
                                enabled = currentPage < pageCount - 1
                            ) {
                                Icon(Icons.Filled.ArrowForward, contentDescription = "Next page")
                            }
                        }
                    }
                }
            }

            WavesPrimaryButton(
                text = if (isExporting) "PREPARING PDF..." else "DOWNLOAD PDF",
                icon = Icons.Filled.Download,
                enabled = !isExporting,
                onClick = { exportInvoice(share = false) }
            )
            WavesSecondaryButton(
                text = "SHARE PDF",
                icon = Icons.Filled.Share,
                onClick = { exportInvoice(share = true) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
