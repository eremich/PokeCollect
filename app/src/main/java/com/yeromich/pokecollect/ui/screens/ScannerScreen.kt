package com.yeromich.pokecollect.ui.screens

import com.yeromich.pokecollect.ui.theme.Paper
import com.yeromich.pokecollect.ui.theme.Ink
import com.yeromich.pokecollect.ui.theme.Holo
import com.yeromich.pokecollect.ui.components.CardShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.clip
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.view.Surface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.yeromich.pokecollect.R
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.usecase.RecognitionResult
import java.io.File


private const val CARD_ASPECT = 0.72f
private const val CARD_CORNER_FRACTION = 0.045f
private const val CORNER_ARM_FRACTION = 0.16f
private const val SCRIM_ALPHA = 0.65f
private const val SHUTTER_PRESSED_SCALE = 0.86f
private const val SHUTTER_PRESS_MS = 120

@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    onCardSelected: (Card) -> Unit,
    onSearchManual: (String) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted() else viewModel.onPermissionDenied()
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when (val s = state) {
            is ScannerUiState.NoPermission -> Unit

            is ScannerUiState.PermissionDenied -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.scanner_permission_title),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }) {
                        Text(stringResource(R.string.scanner_open_settings))
                    }
                }
                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())
            }

            is ScannerUiState.Ready, is ScannerUiState.Capturing -> {
                val previewView = remember {
                    PreviewView(context).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    }
                }

                // Each viewfinder binds and later unbinds only its own use cases. A blanket unbindAll()
                // from a screen that is leaving could otherwise switch off the camera that the
                // returning screen just started, leaving a black preview.
                DisposableEffect(previewView, lifecycleOwner) {
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                        .build()
                    var provider: ProcessCameraProvider? = null
                    var disposed = false
                    val future = ProcessCameraProvider.getInstance(context)
                    future.addListener({
                        if (disposed) return@addListener
                        provider = future.get().also {
                            it.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                        }
                        imageCapture = capture
                    }, ContextCompat.getMainExecutor(context))

                    onDispose {
                        disposed = true
                        provider?.unbind(preview, capture)
                        if (imageCapture === capture) imageCapture = null
                    }
                }

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val frameWidth = maxWidth * 0.85f
                    val frameHeight = frameWidth / 0.72f
                    val frameTop = (maxHeight - frameHeight) / 2
                    val frameBottom = frameTop + frameHeight

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    ) {
                        val fw = size.width * 0.85f
                        val fh = fw / 0.72f
                        val fl = (size.width - fw) / 2f
                        val ft = (size.height - fh) / 2f
                        val cr = fw * CARD_CORNER_FRACTION

                        drawRect(Ink.copy(alpha = SCRIM_ALPHA))

                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(fl, ft),
                            size = Size(fw, fh),
                            cornerRadius = CornerRadius(cr),
                            blendMode = BlendMode.Clear
                        )

                        drawViewfinderCorners(fl, ft, fw, fh, cr, Holo, 3.dp.toPx())
                    }

                    Text(
                        text = stringResource(R.string.scanner_hint),
                        style = MaterialTheme.typography.labelLarge,
                        color = Paper,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = frameBottom + 20.dp)
                            .background(Ink.copy(alpha = 0.6f), CircleShape)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())

                ShutterButton(
                    onClick = {
                        if (state is ScannerUiState.Ready) {
                            viewModel.onCapturing()
                            val file = File(context.cacheDir, "card_${System.currentTimeMillis()}.jpg")
                            imageCapture?.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                        viewModel.onCaptured(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        viewModel.onRetake()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 40.dp)
                )
            }

            is ScannerUiState.Captured -> {
                val bitmap = remember(s.file) {
                    val raw = BitmapFactory.decodeFile(s.file.absolutePath) ?: return@remember null
                    val exif = androidx.exifinterface.media.ExifInterface(s.file.absolutePath)
                    val orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                    )
                    val matrix = android.graphics.Matrix()
                    when (orientation) {
                        6 -> matrix.postRotate(90f)
                        3 -> matrix.postRotate(180f)
                        8 -> matrix.postRotate(270f)
                    }
                    android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                }

                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 32.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(onClick = { viewModel.onRetake() }) {
                        Text(stringResource(R.string.scanner_retake))
                    }
                    Button(onClick = { viewModel.onCaptured(s.file) }) {
                        Text(stringResource(R.string.scanner_use))
                    }
                }

                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())
            }

            is ScannerUiState.Recognizing -> {
                Box(Modifier.fillMaxSize()) {
                    val bitmap = remember(s.file) {
                        val raw = BitmapFactory.decodeFile(s.file.absolutePath) ?: return@remember null
                        val exif = androidx.exifinterface.media.ExifInterface(s.file.absolutePath)
                        val orientation = exif.getAttributeInt(
                            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                        )
                        val matrix = android.graphics.Matrix()
                        when (orientation) {
                            6 -> matrix.postRotate(90f)
                            3 -> matrix.postRotate(180f)
                            8 -> matrix.postRotate(270f)
                        }
                        android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
                    }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.7f)))
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.White
                    )
                    Text(
                        stringResource(R.string.scanner_recognizing),
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(top = 80.dp)
                    )
                    CloseButton(
                        { viewModel.onRetake() },
                        Modifier.align(Alignment.TopEnd).statusBarsPadding()
                    )
                }
            }

            is ScannerUiState.Recognized -> {
                RecognizedContent(
                    file = s.file,
                    result = s.result,
                    onRetake = { viewModel.onRetake() },
                    // Reset first, so coming back from the card shows a live camera, not this result.
                    onCardSelected = { card ->
                        viewModel.onRetake()
                        onCardSelected(card)
                    },
                    onSearchManual = { query ->
                        viewModel.onRetake()
                        onSearchManual(query)
                    }
                )
            }

            is ScannerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.scanner_error_generic), color = Paper, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.onRetake() }) {
                        Text(stringResource(R.string.scanner_error_back))
                    }
                }
                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecognizedContent(
    file: File,
    result: RecognitionResult,
    onRetake: () -> Unit,
    onCardSelected: (Card) -> Unit,
    onSearchManual: (String) -> Unit
) {
    val bitmap = remember(file) {
        val raw = BitmapFactory.decodeFile(file.absolutePath) ?: return@remember null
        val exif = androidx.exifinterface.media.ExifInterface(file.absolutePath)
        val orientation = exif.getAttributeInt(
            androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
            androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
        )
        val matrix = android.graphics.Matrix()
        when (orientation) {
            6 -> matrix.postRotate(90f)
            3 -> matrix.postRotate(180f)
            8 -> matrix.postRotate(270f)
        }
        android.graphics.Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }
    var showSheet by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize()) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.55f)))

        when (result) {
            is RecognitionResult.Success -> {
                if (result.candidates.size == 1) {
                    LaunchedEffect(Unit) { onCardSelected(result.candidates.first()) }
                } else if (showSheet) {
                    ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                        Text(
                            stringResource(R.string.scanner_found_candidates, result.candidates.size),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        LazyColumn {
                            items(result.candidates) { card ->
                                ListItem(
                                    headlineContent = { Text(card.name) },
                                    supportingContent = { Text("${card.setName} · ${card.number}") },
                                    leadingContent = {
                                        AsyncImage(
                                            model = card.imageSmall,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .width(48.dp)
                                                .aspectRatio(CARD_ASPECT)
                                                .clip(CardShape)
                                        )
                                    },
                                    modifier = Modifier.clickable { onCardSelected(card) }
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }

            is RecognitionResult.NotFound -> {
                var searchText by remember { mutableStateOf(result.extractedName ?: "") }
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        stringResource(R.string.scanner_not_found),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        label = { Text(stringResource(R.string.scanner_card_name_label)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedLabelColor = Color.White,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                            focusedBorderColor = Color.White,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.5f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Row {
                        OutlinedButton(
                            onClick = onRetake,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = BorderStroke(1.dp, Color.White)
                        ) { Text(stringResource(R.string.scanner_retake)) }
                        Spacer(Modifier.width(12.dp))
                        Button(
                            onClick = { onSearchManual(searchText) },
                            enabled = searchText.isNotBlank()
                        ) { Text(stringResource(R.string.scanner_search)) }
                    }
                }
            }

            is RecognitionResult.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.scanner_error_generic), color = Paper, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetake) { Text(stringResource(R.string.scanner_retake)) }
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier) {
    FilledIconButton(
        onClick = onClose,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Ink.copy(alpha = 0.6f),
            contentColor = Paper
        ),
        modifier = modifier.padding(12.dp)
    ) {
        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.scanner_close))
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) SHUTTER_PRESSED_SCALE else 1f, tween(SHUTTER_PRESS_MS), label = "shutter")
    val label = stringResource(R.string.scanner_capture)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(80.dp)
            .border(4.dp, Paper, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick
            )
            .semantics { contentDescription = label }
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .background(Paper, CircleShape)
        )
    }
}

private fun DrawScope.drawViewfinderCorners(
    left: Float, top: Float, width: Float, height: Float, radius: Float, color: Color, stroke: Float
) {
    val arm = width * CORNER_ARM_FRACTION
    val right = left + width
    val bottom = top + height
    val d = radius * 2
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    listOf(
        Triple(Offset(left, top), 180f, listOf(Offset(left, top + radius) to Offset(left, top + arm), Offset(left + radius, top) to Offset(left + arm, top))),
        Triple(Offset(right - d, top), 270f, listOf(Offset(right - radius, top) to Offset(right - arm, top), Offset(right, top + radius) to Offset(right, top + arm))),
        Triple(Offset(right - d, bottom - d), 0f, listOf(Offset(right, bottom - radius) to Offset(right, bottom - arm), Offset(right - radius, bottom) to Offset(right - arm, bottom))),
        Triple(Offset(left, bottom - d), 90f, listOf(Offset(left + radius, bottom) to Offset(left + arm, bottom), Offset(left, bottom - radius) to Offset(left, bottom - arm)))
    ).forEach { (arcTopLeft, startAngle, lines) ->
        drawArc(color, startAngle, 90f, useCenter = false, topLeft = arcTopLeft, size = Size(d, d), style = style)
        lines.forEach { (from, to) -> drawLine(color, from, to, stroke, StrokeCap.Round) }
    }
}
