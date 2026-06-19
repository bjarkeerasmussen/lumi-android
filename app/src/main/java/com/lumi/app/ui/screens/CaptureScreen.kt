package com.lumi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.lumi.app.data.DayEntry
import com.lumi.app.data.LumiStore

/**
 * In-app camera with an ID-style oval face guide. The user lines their face up
 * inside the oval, which keeps daily photos framed consistently for a clean
 * before/after. Front camera by default.
 */
@Composable
fun CaptureScreen(store: LumiStore, onCaptured: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val todayKey = store.todayKey()

    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val askPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted = it }

    LaunchedEffect(Unit) {
        if (!granted) askPermission.launch(Manifest.permission.CAMERA)
    }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Camera permission is needed to take your skin photo.", textAlign = TextAlign.Center)
            Button(onClick = { askPermission.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 12.dp)) {
                Text("Allow camera")
            }
            TextButton(onClick = onCancel) { Text("Cancel") }
        }
        return
    }

    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var usingFront by remember { mutableStateOf(true) }

    DisposableEffect(usingFront, granted) {
        val provider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        val selector = if (usingFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        try {
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
        } catch (e: Exception) {
            try {
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (_: Exception) { }
        }
        onDispose { runCatching { provider.unbindAll() } }
    }

    fun capture() {
        val file = store.photoFileFor(todayKey)
        val meta = ImageCapture.Metadata().apply { isReversedHorizontal = usingFront }
        val opts = ImageCapture.OutputFileOptions.Builder(file).setMetadata(meta).build()
        imageCapture.takePicture(
            opts,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val updated = (store.entryFor(todayKey) ?: DayEntry(todayKey))
                        .copy(photoFile = "$todayKey.jpg")
                    store.upsert(updated)
                    onCaptured()
                }
                override fun onError(exc: ImageCaptureException) { /* stay on screen to retry */ }
            }
        )
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        // Oval face-guide overlay: dim scrim with a clear oval cut-out + outline.
        Canvas(Modifier.fillMaxSize()) {
            val ovalW = size.width * 0.72f
            val ovalH = ovalW * 1.32f
            val left = (size.width - ovalW) / 2f
            val top = size.height * 0.16f
            val rect = Rect(Offset(left, top), Size(ovalW, ovalH))
            val scrim = Path().apply {
                addRect(Rect(Offset.Zero, size))
                addOval(rect)
                fillType = PathFillType.EvenOdd
            }
            drawPath(scrim, Color.Black.copy(alpha = 0.55f))
            drawOval(
                color = Color.White,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = 4.dp.toPx())
            )
        }

        Text(
            "Line your face up inside the oval.\nSame spot and light each day works best.",
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 24.dp, start = 24.dp, end = 24.dp)
        )

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Shutter button.
            Box(
                Modifier.size(74.dp).clip(CircleShape).background(Color.White).padding(6.dp)
            ) {
                Box(
                    Modifier.fillMaxSize().clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable { capture() }
                )
            }
            TextButton(onClick = { usingFront = !usingFront }) {
                Text(if (usingFront) "Switch to back camera" else "Switch to front camera", color = Color.White)
            }
            TextButton(onClick = onCancel) { Text("Cancel", color = Color.White) }
        }
    }
}
