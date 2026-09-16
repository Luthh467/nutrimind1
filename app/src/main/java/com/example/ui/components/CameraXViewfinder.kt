package com.example.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.view.ViewGroup
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun CameraXViewfinder(
    onPhotoCaptured: (Bitmap) -> Unit,
    onPickGallery: () -> Unit,
    onSelectPreset: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }

    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableIntStateOf(ImageCapture.FLASH_MODE_OFF) }
    var isCapturing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPresetsDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            try {
                cameraProviderInstance?.unbindAll()
            } catch (e: Exception) {
                Log.e("CameraXViewfinder", "Error unbinding camera on dispose", e)
            }
        }
    }

    // Rebind camera whenever lensFacing or provider/previewView updates
    LaunchedEffect(lensFacing, previewViewInstance, cameraProviderInstance) {
        val provider = cameraProviderInstance ?: return@LaunchedEffect
        val pv = previewViewInstance ?: return@LaunchedEffect

        try {
            provider.unbindAll()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(pv.surfaceProvider)
            }

            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flashMode)
                .build()

            imageCapture = capture

            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()

            val camera = provider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
            cameraControl = camera.cameraControl
            errorMessage = null
        } catch (e: Exception) {
            Log.e("CameraXViewfinder", "Binding failed", e)
            errorMessage = "Kamera tidak dapat diakses di perangkat/emulator ini: ${e.localizedMessage}. Silakan gunakan tombol Galeri atau Simulasi Contoh Menu."
        }
    }

    // Update flash mode on existing imageCapture instance
    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    fun capturePhoto() {
        val capture = imageCapture
        if (capture == null) {
            errorMessage = "Kamera belum siap. Mohon tunggu sejenak atau gunakan pilihan Galeri."
            return
        }
        if (isCapturing) return

        isCapturing = true
        errorMessage = null

        capture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    try {
                        val rotationDegrees = image.imageInfo.rotationDegrees
                        val rawBitmap = image.toBitmap()
                        val finalBitmap = if (rotationDegrees != 0) {
                            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                            Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
                        } else {
                            rawBitmap
                        }
                        ContextCompat.getMainExecutor(context).execute {
                            isCapturing = false
                            onPhotoCaptured(finalBitmap)
                        }
                    } catch (e: Throwable) {
                        Log.e("CameraXViewfinder", "Image processing failed", e)
                        ContextCompat.getMainExecutor(context).execute {
                            isCapturing = false
                            errorMessage = "Gagal memproses foto: ${e.localizedMessage}"
                        }
                    } finally {
                        image.close()
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraXViewfinder", "Capture error", exception)
                    ContextCompat.getMainExecutor(context).execute {
                        isCapturing = false
                        errorMessage = "Pengambilan foto kamera gagal: ${exception.localizedMessage}. Gunakan tombol Galeri atau Contoh Menu di bawah."
                    }
                }
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview View
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val pv = previewViewInstance ?: return@detectTapGestures
                        val factory = pv.meteringPointFactory
                        val point = factory.createPoint(offset.x, offset.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraControl?.startFocusAndMetering(action)
                    }
                },
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                }
                previewViewInstance = previewView

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        cameraProviderInstance = cameraProviderFuture.get()
                    } catch (e: Exception) {
                        Log.e("CameraXViewfinder", "Failed to get camera provider", e)
                        errorMessage = "Gagal mengaktifkan modul kamera: ${e.localizedMessage}"
                    }
                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        // Overlay: Isi Piringku Targeting Guide Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                val strokeWidth = 3.dp.toPx()
                val cornerLength = 40.dp.toPx()
                val w = size.width
                val h = size.height
                val cornerRadius = 24.dp.toPx()

                // Semi-transparent center plate circle guide
                drawCircle(
                    color = Color.White.copy(alpha = 0.15f),
                    radius = (w / 2f) - 16.dp.toPx(),
                    center = Offset(w / 2f, h / 2f),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 16f), 0f)
                    )
                )

                // Outer targeting frame dashed
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    size = Size(w, h),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius),
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f), 0f)
                    )
                )

                // Vibrant Green Corners (Top-Left)
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(0f, cornerLength),
                    end = Offset(0f, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(0f, 0f),
                    end = Offset(cornerLength, 0f),
                    strokeWidth = strokeWidth
                )

                // Top-Right
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(w - cornerLength, 0f),
                    end = Offset(w, 0f),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(w, 0f),
                    end = Offset(w, cornerLength),
                    strokeWidth = strokeWidth
                )

                // Bottom-Left
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(0f, h - cornerLength),
                    end = Offset(0f, h),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(0f, h),
                    end = Offset(cornerLength, h),
                    strokeWidth = strokeWidth
                )

                // Bottom-Right
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(w - cornerLength, h),
                    end = Offset(w, h),
                    strokeWidth = strokeWidth
                )
                drawLine(
                    color = Color(0xFF10B981),
                    start = Offset(w, h - cornerLength),
                    end = Offset(w, h),
                    strokeWidth = strokeWidth
                )
            }

            // Plate Guidance Chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Panduan Piring / Bekal Makanan",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Top Controls Bar (Close, Flash, Flip)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.5f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .statusBarsPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Tutup Kamera",
                    tint = Color.White
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "CameraX AI Scanner",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "Arahkan ke makanan madrasah",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash Toggle Button
                IconButton(
                    onClick = {
                        flashMode = when (flashMode) {
                            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_ON
                            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_AUTO
                            else -> ImageCapture.FLASH_MODE_OFF
                        }
                    },
                    modifier = Modifier
                        .background(
                            if (flashMode != ImageCapture.FLASH_MODE_OFF) Color(0xFF10B981) else Color.White.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                            else -> Icons.Default.FlashOff
                        },
                        contentDescription = "Flash Kamera",
                        tint = Color.White
                    )
                }

                // Lens Facing Switch (Rear <-> Front)
                IconButton(
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    },
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        Icons.Default.FlipCameraAndroid,
                        contentDescription = "Ganti Lensa Kamera",
                        tint = Color.White
                    )
                }
            }
        }

        // Error message banner if any
        if (errorMessage != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 90.dp, start = 16.dp, end = 16.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFEF4444).copy(alpha = 0.9f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage!!,
                        color = Color.White,
                        fontSize = 11.sp,
                        modifier = Modifier.weight(1f),
                        lineHeight = 15.sp
                    )
                    IconButton(
                        onClick = { errorMessage = null },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Bottom Action Controls (Gallery, Shutter Button, Quick Presets)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pick from Gallery Button
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onPickGallery,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = "Pilih dari Galeri",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Galeri",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Camera Shutter Button (CameraX capture trigger)
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(if (isCapturing) Color.Gray else Color(0xFF10B981))
                        .clickable(enabled = !isCapturing) {
                            capturePhoto()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Ambil Foto Makanan",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                // Sample Meal Presets (great for emulator or quick demo)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = { showPresetsDialog = true },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(52.dp)
                    ) {
                        Icon(
                            Icons.Default.Fastfood,
                            contentDescription = "Contoh Menu Madrasah",
                            tint = Color(0xFFF59E0B),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Contoh",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Tekan tombol hijau untuk mengambil foto dan menganalisis gizi dengan Gemini Vision AI",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }

    // Presets Dialog
    if (showPresetsDialog) {
        AlertDialog(
            onDismissRequest = { showPresetsDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Fastfood, contentDescription = null, tint = NutriOrangeWarm)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pilih Contoh Menu Madrasah", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Gunakan contoh menu nyata untuk mensimulasikan identifikasi makanan dan estimasi keseimbangan gizi:",
                        fontSize = 12.sp,
                        color = NutriSlate700
                    )
                    val presets = listOf(
                        "Nasi Campur Madrasah" to "Nasi, ayam goreng, tempe, sayur bening bayam",
                        "Pecel Sayur & Tempe" to "Nasi pecel, sayuran hijau melimpah, tempe bacem & telur",
                        "Gado-Gado Madrasah" to "Lontong, tahu, telur, tauge, selada & bumbu kacang",
                        "Mie Instan & Nugget" to "Mie goreng instan dan nugget olahan (rendah serat)"
                    )
                    presets.forEach { (name, desc) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPresetsDialog = false
                                    onSelectPreset(name)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = NutriSlate100,
                            border = BorderStroke(1.dp, NutriSlate200)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NutriSlate900)
                                Text(desc, fontSize = 11.sp, color = NutriSlate600)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPresetsDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}
