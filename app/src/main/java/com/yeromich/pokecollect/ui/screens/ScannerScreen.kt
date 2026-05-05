package com.yeromich.pokecollect.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import android.graphics.Matrix
import android.media.ExifInterface
import android.view.Surface
import androidx.core.content.ContextCompat
import com.yeromich.pokecollect.R
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

sealed class ScannerState {
    object NoPermission : ScannerState()
    object PermissionDenied : ScannerState()
    object Ready : ScannerState()
    object Capturing : ScannerState()
    data class Captured(val file: File) : ScannerState()
    data class Error(val message: String) : ScannerState()
}

@Composable
fun ScannerScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var state by remember { mutableStateOf<ScannerState>(ScannerState.NoPermission) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        state = if (granted) ScannerState.Ready else ScannerState.PermissionDenied
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            state = ScannerState.Ready
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
            is ScannerState.NoPermission -> Unit

            is ScannerState.PermissionDenied -> {
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

            is ScannerState.Ready, is ScannerState.Capturing -> {
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
                        if (state is ScannerState.Ready) {
                            state = ScannerState.Capturing
                            val file = File(context.cacheDir, "card_${System.currentTimeMillis()}.jpg")
                            imageCapture?.takePicture(
                                ImageCapture.OutputFileOptions.Builder(file).build(),
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(results: ImageCapture.OutputFileResults) {
                                        state = ScannerState.Captured(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        state = ScannerState.Error(exception.message ?: "Error")
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

            is ScannerState.Captured -> {
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
                    OutlinedButton(onClick = { state = ScannerState.Ready }) {
                        Text(stringResource(R.string.scanner_retake))
                    }
                    Button(onClick = {
                        Toast.makeText(context, s.file.absolutePath, Toast.LENGTH_LONG).show()
                    }) {
                        Text(stringResource(R.string.scanner_use))
                    }
                }

                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())
            }

            is ScannerState.Error -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(s.message, color = Color.White, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { state = ScannerState.Ready }) {
                        Text(stringResource(R.string.scanner_error_back))
                    }
                }
                CloseButton(onClose, Modifier.align(Alignment.TopEnd).statusBarsPadding())
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
