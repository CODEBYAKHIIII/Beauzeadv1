package com.example.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.exifinterface.media.ExifInterface
import com.example.components.WavesHeader
import com.example.components.WavesPrimaryButton
import com.example.ui.theme.BackgroundColor
import com.example.ui.theme.CardBorder
import com.example.ui.theme.DangerRed
import com.example.ui.theme.EmeraldInk
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun LogoCropScreen(
    sourceUri: Uri,
    onNavigateBack: () -> Unit,
    onChooseAnotherImage: () -> Unit,
    onSaveCroppedImage: suspend (Uri) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var imageBitmap by remember(sourceUri) {
        mutableStateOf<ImageBitmap?>(null)
    }
    var sourceBitmap by remember(sourceUri) {
        mutableStateOf<Bitmap?>(null)
    }
    var isLoading by rememberSaveable(sourceUri.toString()) { mutableStateOf(true) }
    var isSaving by rememberSaveable { mutableStateOf(false) }
    var scale by rememberSaveable(sourceUri.toString()) { mutableFloatStateOf(1f) }
    var panX by rememberSaveable(sourceUri.toString()) { mutableFloatStateOf(0f) }
    var panY by rememberSaveable(sourceUri.toString()) { mutableFloatStateOf(0f) }
    var viewport by remember(sourceUri) { mutableStateOf(IntSize.Zero) }
    var errorMessage by rememberSaveable(sourceUri.toString()) { mutableStateOf<String?>(null) }

    LaunchedEffect(sourceUri) {
        isLoading = true
        errorMessage = null
        try {
            val decoded = withContext(Dispatchers.IO) {
                decodeOrientedBitmap(context, sourceUri)
            }
            sourceBitmap = decoded
            imageBitmap = decoded.asImageBitmap()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            errorMessage = exception.localizedMessage ?: "This image could not be opened."
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            WavesHeader(
                title = "Crop business logo",
                onBackClick = onNavigateBack,
                actions = {
                    androidx.compose.material3.TextButton(
                        onClick = onChooseAnotherImage,
                        enabled = !isSaving
                    ) {
                        Text("Change", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WavesPrimaryButton(
                    text = when {
                        isSaving -> "Cropping and uploading..."
                        else -> "CROP & UPLOAD LOGO"
                    },
                    enabled = sourceBitmap != null && !isLoading && !isSaving,
                    icon = Icons.Filled.Crop,
                    onClick = {
                        val bitmap = sourceBitmap ?: return@WavesPrimaryButton
                        isSaving = true
                        errorMessage = null
                        coroutineScope.launch {
                            var outputFile: File? = null
                            try {
                                val croppedFile = withContext(Dispatchers.IO) {
                                    cropToSquare(
                                        context = context,
                                        bitmap = bitmap,
                                        viewport = viewport,
                                        zoom = scale,
                                        panX = panX,
                                        panY = panY
                                    )
                                }
                                outputFile = croppedFile
                                onSaveCroppedImage(Uri.fromFile(croppedFile))
                            } catch (exception: CancellationException) {
                                throw exception
                            } catch (exception: Exception) {
                                errorMessage = exception.localizedMessage
                                    ?: "The cropped logo could not be uploaded."
                            } finally {
                                outputFile?.delete()
                                isSaving = false
                            }
                        }
                    }
                )
            }
        },
        containerColor = BackgroundColor
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Position your logo inside the square",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Drag to move · Pinch to zoom",
                color = TextSecondary,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isLoading -> CircularProgressIndicator(color = EmeraldInk)
                    imageBitmap != null -> {
                        val image = imageBitmap!!
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .onSizeChanged { viewport = it }
                                .pointerInput(image, viewport) {
                                    detectTransformGestures { _, gesturePan, gestureZoom, _ ->
                                        if (viewport.width == 0 || viewport.height == 0) return@detectTransformGestures
                                        val nextScale = (scale * gestureZoom).coerceIn(1f, 8f)
                                        val baseScale = max(
                                            viewport.width.toFloat() / image.width,
                                            viewport.height.toFloat() / image.height
                                        )
                                        val drawnWidth = image.width * baseScale * nextScale
                                        val drawnHeight = image.height * baseScale * nextScale
                                        val maxPanX = ((drawnWidth - viewport.width) / 2f).coerceAtLeast(0f)
                                        val maxPanY = ((drawnHeight - viewport.height) / 2f).coerceAtLeast(0f)
                                        scale = nextScale
                                        panX = (panX + gesturePan.x).coerceIn(-maxPanX, maxPanX)
                                        panY = (panY + gesturePan.y).coerceIn(-maxPanY, maxPanY)
                                    }
                                }
                        ) {
                            val baseScale = max(
                                size.width / image.width,
                                size.height / image.height
                            )
                            val drawnWidth = image.width * baseScale * scale
                            val drawnHeight = image.height * baseScale * scale
                            val left = (size.width - drawnWidth) / 2f + panX
                            val top = (size.height - drawnHeight) / 2f + panY
                            drawImage(
                                image = image,
                                dstOffset = IntOffset(left.roundToInt(), top.roundToInt()),
                                dstSize = IntSize(drawnWidth.roundToInt(), drawnHeight.roundToInt())
                            )
                            drawRect(
                                color = androidx.compose.ui.graphics.Color.White,
                                topLeft = Offset.Zero,
                                size = size,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = errorMessage ?: "Choose a different image to continue.",
                            color = DangerRed,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }

            if (errorMessage != null && imageBitmap != null) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = errorMessage.orEmpty(),
                    color = DangerRed,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            OutlinedButton(
                onClick = {
                    scale = 1f
                    panX = 0f
                    panY = 0f
                    errorMessage = null
                },
                enabled = imageBitmap != null && !isSaving,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = EmeraldInk,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Reset crop", color = EmeraldInk)
            }
        }
    }
}

private fun decodeOrientedBitmap(context: Context, uri: Uri): Bitmap {
    // Read the image payload ONCE into memory. Repeated openInputStream calls
    // fail on several providers (cloud-only gallery photos, chat app documents)
    // and were the usual cause of "Unable to open the selected image".
    val bytes = try {
        context.contentResolver.openInputStream(uri)?.use { stream -> stream.readBytes() }
            ?: throw IllegalStateException("Unable to open the selected image. Please pick it again.")
    } catch (exception: SecurityException) {
        throw IllegalStateException(
            "Access to the selected image was denied. Please pick it again.",
            exception
        )
    } catch (exception: FileNotFoundException) {
        throw IllegalStateException(
            "The selected image is no longer available. Please pick it again.",
            exception
        )
    }
    if (bytes.isEmpty()) {
        throw IllegalStateException("The selected image is empty. Please pick a different image.")
    }

    val orientation = try {
        ExifInterface(ByteArrayInputStream(bytes)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
    } catch (_: Exception) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) {
        "The selected file is not a supported image. Use a JPG, PNG or WebP image."
    }

    var sampleSize = 1
    while (max(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_DECODE_DIMENSION) {
        sampleSize *= 2
    }
    val decoded = BitmapFactory.decodeByteArray(
        bytes,
        0,
        bytes.size,
        BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
    ) ?: throw IllegalStateException(
        "Unable to decode the selected image. Please pick a different image."
    )

    val transform = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> transform.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> transform.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> transform.setScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            transform.setRotate(90f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> transform.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            transform.setRotate(-90f)
            transform.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> transform.setRotate(-90f)
    }
    return if (orientation == ExifInterface.ORIENTATION_NORMAL ||
        orientation == ExifInterface.ORIENTATION_UNDEFINED
    ) {
        decoded
    } else {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, transform, true)
            .also { if (it !== decoded) decoded.recycle() }
    }
}

private fun cropToSquare(
    context: Context,
    bitmap: Bitmap,
    viewport: IntSize,
    zoom: Float,
    panX: Float,
    panY: Float
): File {
    require(viewport.width > 0 && viewport.height > 0) { "The crop area is not ready yet." }
    require(viewport.width == viewport.height) { "The crop area must be square." }
    val baseScale = max(
        viewport.width.toFloat() / bitmap.width,
        viewport.height.toFloat() / bitmap.height
    )
    val totalScale = baseScale * zoom
    val renderedWidth = bitmap.width * totalScale
    val renderedHeight = bitmap.height * totalScale
    val left = (viewport.width - renderedWidth) / 2f + panX
    val top = (viewport.height - renderedHeight) / 2f + panY
    val sourceSide = minOf(viewport.width / totalScale, bitmap.width.toFloat(), bitmap.height.toFloat())
        .roundToInt()
        .coerceAtLeast(1)
    val sourceX = ((-left) / totalScale).roundToInt()
        .coerceIn(0, bitmap.width - sourceSide)
    val sourceY = ((-top) / totalScale).roundToInt()
        .coerceIn(0, bitmap.height - sourceSide)
    val square = Bitmap.createBitmap(bitmap, sourceX, sourceY, sourceSide, sourceSide)
    val outputBitmap = Bitmap.createScaledBitmap(
        square,
        LOGO_OUTPUT_DIMENSION,
        LOGO_OUTPUT_DIMENSION,
        true
    )

    val outputFile = File.createTempFile("waves-business-logo-", ".png", context.cacheDir)
    require(outputFile.setWritable(true, false) && outputFile.setReadable(true, false)) {
        "The cropped image could not be saved."
    }
    try {
        FileOutputStream(outputFile).use { stream ->
            check(outputBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                "The cropped image could not be saved."
            }
        }
        require(outputFile.length() < MAX_UPLOAD_BYTES) {
            "The cropped logo is too large to upload. Choose a simpler image."
        }
    } catch (exception: Exception) {
        outputFile.delete()
        throw exception
    } finally {
        if (outputBitmap !== square) outputBitmap.recycle()
        square.recycle()
    }
    return outputFile
}

private const val MAX_DECODE_DIMENSION = 2048
private const val MAX_UPLOAD_BYTES = 4L * 1024 * 1024
private const val LOGO_OUTPUT_DIMENSION = 500
