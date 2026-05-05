package com.yeromich.pokecollect.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
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
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

@Composable
fun ScannerScreen(
    onClose: () -> Unit,
    onCardSelected: (Card) -> Unit,
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

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

                LaunchedEffect(previewView) {
                    val provider = suspendCancellableCoroutine<ProcessCameraProvider> { cont ->
                        val future = ProcessCameraProvider.getInstance(context)
                        future.addListener({ cont.resume(future.get()) }, ContextCompat.getMainExecutor(context))
                    }
                    cameraProvider = provider

                    val preview = Preview.Builder().build()
                    preview.surfaceProvider = previewView.surfaceProvider

                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetRotation(previewView.display?.rotation ?: Surface.ROTATION_0)
                        .build()
                    imageCapture = capture

                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, capture)
                }

                DisposableEffect(Unit) {
                    onDispose { cameraProvider?.unbindAll() }
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
                        val cr = 16.dp.toPx()

                        drawRect(Color.Black.copy(alpha = 0.55f))

                        drawRoundRect(
                            color = Color.Transparent,
                            topLeft = Offset(fl, ft),
                            size = Size(fw, fh),
                            cornerRadius = CornerRadius(cr),
                            blendMode = BlendMode.Clear
                        )

                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(fl, ft),
                            size = Size(fw, fh),
                            cornerRadius = CornerRadius(cr),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }

                    Text(
                        text = stringResource(R.string.scanner_hint),
                        color = Color.White,
                        style = TextStyle(
                            shadow = Shadow(
                                color = Color.Black.copy(alpha = 0.7f),
                                offset = Offset(0f, 1f),
                                blurRadius = 4f
                            )
                        ),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = frameBottom + 16.dp)
                    )
                }

                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())

                FloatingActionButton(
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
                        .padding(bottom = 32.dp)
                        .size(72.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = stringResource(R.string.scanner_capture))
                }
            }

            is ScannerUiState.Captured -> {
                val bitmap = remember(s.file) {
                    val raw = BitmapFactory.decodeFile(s.file.absolutePath) ?: return@remember null
                    val orientation = ExifInterface(s.file.absolutePath).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                    val degrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                        else -> 0f
                    }
                    if (degrees == 0f) raw
                    else android.graphics.Bitmap.createBitmap(
                        raw, 0, 0, raw.width, raw.height,
                        Matrix().apply { postRotate(degrees) }, true
                    )
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
                    val bitmap = remember(s.file) { BitmapFactory.decodeFile(s.file.absolutePath) }
                    bitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)))
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
                    onCardSelected = onCardSelected
                )
            }

            is ScannerUiState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White, textAlign = TextAlign.Center)
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
    onCardSelected: (Card) -> Unit
) {
    val bitmap = remember(file) { BitmapFactory.decodeFile(file.absolutePath) }
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
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))

        when (result) {
            is RecognitionResult.Success -> {
                if (result.candidates.size == 1) {
                    LaunchedEffect(Unit) { onCardSelected(result.candidates.first()) }
                } else if (showSheet) {
                    ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                        Text(
                            "Найдено ${result.candidates.size} вариантов",
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
                                            modifier = Modifier.size(48.dp)
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
                    result.extractedName?.let { Text("Имя: $it", color = Color.White.copy(alpha = 0.7f)) }
                    result.extractedNumber?.let { Text("Номер: $it", color = Color.White.copy(alpha = 0.7f)) }
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onRetake) { Text(stringResource(R.string.scanner_retake)) }
                }
            }

            is RecognitionResult.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(result.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onRetake) { Text(stringResource(R.string.scanner_retake)) }
                }
            }
        }
    }
}

@Composable
private fun CloseButton(onClose: () -> Unit, modifier: Modifier) {
    IconButton(onClick = onClose, modifier = modifier) {
        Icon(Icons.Filled.Close, contentDescription = null, tint = Color.White)
    }
}
