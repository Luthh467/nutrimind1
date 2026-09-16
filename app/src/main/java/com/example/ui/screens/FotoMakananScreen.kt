package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.ui.components.CameraXViewfinder
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun FotoMakananScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isAnalyzing by viewModel.isAnalyzingFood.collectAsState()
    val currentAnalysis by viewModel.currentFoodAnalysis.collectAsState()

    var isCameraXOpen by remember { mutableStateOf(false) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedPresetName by remember { mutableStateOf<String?>("Nasi Campur Madrasah") }
    var showPermissionDeniedDialog by remember { mutableStateOf(false) }
    var cameraNoticeMessage by remember { mutableStateOf<String?>(null) }
    var isPhotoFromCameraX by remember { mutableStateOf(false) }

    // Android Photo Picker Launcher (Modern, zero-permission fallback)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT < 28) {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                } else {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source)
                }
                selectedBitmap = bitmap
                selectedPresetName = null
                cameraNoticeMessage = null
                isPhotoFromCameraX = false
                isCameraXOpen = false
                viewModel.analyzeFoodImage(bitmap)
            } catch (e: Exception) {
                cameraNoticeMessage = "Gagal memproses gambar dari galeri: ${e.localizedMessage}"
            }
        }
    }

    // Runtime Permission Launcher for CameraX
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraNoticeMessage = null
            isCameraXOpen = true
        } else {
            showPermissionDeniedDialog = true
        }
    }

    fun openCameraXSafely() {
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            cameraNoticeMessage = null
            isCameraXOpen = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera Permission Dialog
    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = NutriRedRisk) },
            title = { Text("Izin Kamera Diperlukan", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Text(
                    "NutriMind membutuhkan izin kamera untuk mengaktifkan pemindai makanan langsung (CameraX). Anda juga tetap dapat mengunggah foto makanan dari Galeri perangkat.",
                    fontSize = 13.sp,
                    color = NutriSlate700
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                ) {
                    Text("Beri Izin")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDeniedDialog = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                ) {
                    Text("Pilih Galeri")
                }
            }
        )
    }

    // Full-screen Live CameraX Viewfinder when active
    if (isCameraXOpen) {
        CameraXViewfinder(
            onPhotoCaptured = { bitmap ->
                selectedBitmap = bitmap
                selectedPresetName = null
                isPhotoFromCameraX = true
                isCameraXOpen = false
                cameraNoticeMessage = null
                viewModel.analyzeFoodImage(bitmap)
            },
            onPickGallery = {
                isCameraXOpen = false
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onSelectPreset = { presetName ->
                selectedPresetName = presetName
                selectedBitmap = null
                isPhotoFromCameraX = false
                isCameraXOpen = false
                cameraNoticeMessage = null
                viewModel.analyzeFoodImage(null, presetName)
            },
            onClose = {
                isCameraXOpen = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Foto Makanan & Analisis AI",
                subtitle = "CameraX & Gemini Vision Analisis Gizi",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.FOTO_MAKANAN,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NutriSlate50)
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DisclaimerBanner()

            // Info Card with CameraX & Gemini Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NutriTealLight.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, NutriTealPrimary.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = NutriTealDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CameraX API & Gemini Vision AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NutriTealDark
                        )
                        Text(
                            text = "Ambil foto piring bekal/kantin dengan kamera langsung. Gemini Vision AI akan mengidentifikasi makanan dan mengevaluasi keseimbangan gizi sesuai panduan Isi Piringku Kemenkes RI.",
                            fontSize = 11.sp,
                            color = NutriSlate700,
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Image Preview & Capture Controls Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NutriSlate100)
                            .border(1.dp, NutriSlate400.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .testTag("food_image_preview_box"),
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedBitmap != null) {
                            Image(
                                bitmap = selectedBitmap!!.asImageBitmap(),
                                contentDescription = "Foto Makanan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Image(
                                painter = painterResource(id = R.drawable.nutrimind_hero_1789536664993),
                                contentDescription = "Ilustrasi Makanan",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Badge Tag: CameraX or Gallery or Preset
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isPhotoFromCameraX) Icons.Default.CameraAlt else Icons.Default.Image,
                                    contentDescription = null,
                                    tint = if (isPhotoFromCameraX) Color(0xFF10B981) else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = when {
                                        isPhotoFromCameraX -> "Ditangkap via CameraX"
                                        selectedBitmap != null -> "Dari Galeri HP"
                                        else -> "Contoh Menu Madrasah"
                                    },
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Analyzing overlay
                        if (isAnalyzing) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                color = Color.Black.copy(alpha = 0.5f)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(color = Color(0xFF10B981))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Gemini Vision AI Sedang Menganalisis...",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Mengidentifikasi makanan & porsi Isi Piringku",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }

                    if (cameraNoticeMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = NutriAmberLight,
                            border = BorderStroke(1.dp, NutriAmber.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = NutriAmber, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = cameraNoticeMessage!!,
                                    fontSize = 11.sp,
                                    color = NutriSlate800,
                                    modifier = Modifier.weight(1f),
                                    lineHeight = 15.sp
                                )
                                IconButton(
                                    onClick = { cameraNoticeMessage = null },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Tutup", modifier = Modifier.size(14.dp), tint = NutriSlate600)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons: Live CameraX & Galeri
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                openCameraXSafely()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("take_photo_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Buka Kamera CameraX", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = {
                                cameraNoticeMessage = null
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("pick_gallery_button"),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NutriGreenPrimary),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriGreenDark)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Pilih Galeri", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Quick Sample Presets
                    Text(
                        text = "Atau coba contoh menu madrasah untuk simulasi analisis AI:",
                        fontSize = 11.sp,
                        color = NutriSlate800,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val presets = listOf(
                        "Nasi Campur Madrasah",
                        "Pecel Sayur & Tempe",
                        "Gado-Gado Madrasah",
                        "Mie Instan & Nugget"
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.take(2).forEach { preset ->
                                val isSel = selectedPresetName == preset
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedPresetName = preset
                                        selectedBitmap = null
                                        isPhotoFromCameraX = false
                                        cameraNoticeMessage = null
                                        viewModel.analyzeFoodImage(null, preset)
                                    },
                                    label = {
                                        Text(
                                            text = preset,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSel) NutriTealDark else NutriSlate800,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NutriTealLight,
                                        selectedLabelColor = NutriTealDark,
                                        containerColor = NutriSlate50,
                                        labelColor = NutriSlate800
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = if (isSel) NutriTealPrimary else NutriSlate200
                                    )
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            presets.drop(2).forEach { preset ->
                                val isSel = selectedPresetName == preset
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        selectedPresetName = preset
                                        selectedBitmap = null
                                        isPhotoFromCameraX = false
                                        cameraNoticeMessage = null
                                        viewModel.analyzeFoodImage(null, preset)
                                    },
                                    label = {
                                        Text(
                                            text = preset,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSel) NutriTealDark else NutriSlate800,
                                            maxLines = 1
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NutriTealLight,
                                        selectedLabelColor = NutriTealDark,
                                        containerColor = NutriSlate50,
                                        labelColor = NutriSlate800
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSel,
                                        borderColor = if (isSel) NutriTealPrimary else NutriSlate200
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Food Identification & Nutritional Balance Estimation Result
            if (currentAnalysis != null) {
                val analysis = currentAnalysis!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_analysis_result_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Section Header: Food Identification
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = analysis.foodName,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp,
                                    color = NutriGreenDark
                                )
                                Text(
                                    text = "Estimasi Kalori: ${analysis.estimatedCalories}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = NutriAmberDark
                                )
                            }

                            // Balance Status Badge
                            val badgeColor = when {
                                analysis.balanceStatus.contains("Seimbang", ignoreCase = true) && !analysis.balanceStatus.contains("Kurang", ignoreCase = true) -> NutriGreenPrimary
                                analysis.balanceStatus.contains("Penyesuaian", ignoreCase = true) || analysis.balanceStatus.contains("Perlu", ignoreCase = true) -> NutriOrangeWarm
                                else -> NutriTealPrimary
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = badgeColor.copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = badgeColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = analysis.balanceStatus,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }
                            }
                        }

                        // Macronutrient Distribution Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NutriSlate100,
                            border = BorderStroke(1.dp, NutriSlate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BarChart, contentDescription = null, tint = NutriTealDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Estimasi Proporsi Makronutrien:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NutriSlate800
                                    )
                                    Text(
                                        text = analysis.macronutrients,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NutriSlate900
                                    )
                                }
                            }
                        }

                        HorizontalDivider(color = NutriSlate200)

                        // 1. Food Identification Details
                        Text(
                            text = "1. Hasil Identifikasi Makanan (Gemini Vision)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NutriSlate900
                        )

                        AnalysisRow(
                            icon = Icons.Default.Restaurant,
                            label = "Bahan / Lauk Teridentifikasi",
                            value = analysis.detectedItems,
                            tint = NutriTealDark
                        )
                        AnalysisRow(
                            icon = Icons.Default.PieChart,
                            label = "Kelompok Pangan (Isi Piringku)",
                            value = analysis.foodGroups,
                            tint = NutriGreenDark
                        )

                        HorizontalDivider(color = NutriSlate200)

                        // 2. Nutritional Balance Estimation
                        Text(
                            text = "2. Estimasi Keseimbangan Komponen Gizi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NutriSlate900
                        )

                        AnalysisRow(
                            icon = Icons.Default.Grain,
                            label = "Sumber Karbohidrat",
                            value = analysis.carbSource,
                            tint = NutriAmberDark
                        )
                        AnalysisRow(
                            icon = Icons.Default.Egg,
                            label = "Sumber Protein (Hewani/Nabati)",
                            value = analysis.proteinSource,
                            tint = NutriOrangeWarm
                        )
                        AnalysisRow(
                            icon = Icons.Default.Eco,
                            label = "Kandungan Sayur & Buah (Serat & Vitamin)",
                            value = analysis.vegFruitSource,
                            tint = NutriGreenPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Penilaian & Rekomendasi Evaluasi Gizi
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NutriSlate100,
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, NutriSlate200)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.HealthAndSafety,
                                        contentDescription = null,
                                        tint = NutriGreenDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Evaluasi Keseimbangan Gizi Madrasah:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NutriSlate900
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = analysis.balanceEvaluation,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriSlate900,
                                    lineHeight = 17.sp
                                )

                                if (analysis.improvementTips.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.Lightbulb,
                                            contentDescription = null,
                                            tint = NutriAmberDark,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tips: ${analysis.improvementTips}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NutriSlate800,
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Mandatory Note
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NutriAmberLight,
                            border = BorderStroke(1.dp, NutriAmberDark.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = NutriAmberDark,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "“${analysis.educationalNote}”",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriSlate900,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons: Simpan & Tanya Chatbot
                        Button(
                            onClick = { viewModel.saveFoodAnalysisToHistory() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("save_food_analysis_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                        ) {
                            Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Simpan ke Riwayat Makanan", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        OutlinedButton(
                            onClick = onOpenChat,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, NutriTealPrimary)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, tint = NutriTealDark, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tanya NutriBot tentang Menu Ini", color = NutriTealDark, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AnalysisRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            color = tint.copy(alpha = 0.12f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NutriSlate800)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NutriSlate900, lineHeight = 16.sp)
        }
    }
}

