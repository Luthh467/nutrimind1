package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GiziCalculator
import com.example.data.model.GiziCheckEntity
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun CekGiziScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()
    val latestCheck by viewModel.latestGiziCheck.collectAsState()

    var weightText by remember { mutableStateOf(latestCheck?.weightKg?.toString() ?: "55.0") }
    var heightText by remember { mutableStateOf(latestCheck?.heightCm?.toString() ?: "165.0") }

    var breakfastHabit by remember { mutableStateOf(latestCheck?.breakfastHabit ?: "Setiap hari") }
    var vegetableIntake by remember { mutableStateOf(latestCheck?.vegetableIntake ?: ">= 2 porsi/hari") }
    var fruitIntake by remember { mutableStateOf(latestCheck?.fruitIntake ?: "1 porsi/hari") }
    var junkFoodIntake by remember { mutableStateOf(latestCheck?.junkFoodIntake ?: "1-2x/minggu") }
    var sweetDrinkIntake by remember { mutableStateOf(latestCheck?.sweetDrinkIntake ?: "1 kali/hari") }
    var physicalActivity by remember { mutableStateOf(latestCheck?.physicalActivity ?: "30-60 menit") }
    var sedentaryTime by remember { mutableStateOf(latestCheck?.sedentaryTime ?: "3-6 jam") }

    var currentResult by remember { mutableStateOf<GiziCheckEntity?>(latestCheck) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showResultDialog by remember { mutableStateOf(false) }

    // Live BMI and Kemenkes IMT/U calculation preview
    val w = weightText.toFloatOrNull() ?: 0f
    val h = heightText.toFloatOrNull() ?: 0f
    val age = currentStudent?.age ?: 16
    val gender = currentStudent?.gender ?: "Laki-laki"
    val liveImtu = remember(w, h, age, gender) {
        GiziCalculator.calculateImtu(
            weightKg = w,
            heightCm = h,
            age = age,
            gender = gender
        )
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Cek Status Gizi Siswa",
                subtitle = "Antropometri & Kebiasaan Sehari-hari",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.CEK_GIZI,
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

            // Student Identity Info Pill
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NutriGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = NutriGreenDark)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = currentStudent?.name ?: "Siswa Madrasah",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NutriSlate900
                        )
                        Text(
                            text = "Umur: ${currentStudent?.age ?: 16} th • Jenis Kelamin: ${currentStudent?.gender ?: "Laki-laki"} • ${currentStudent?.grade ?: "MA"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriSlate700
                        )
                    }
                }
            }

            // 1. DATA ANTROPOMETRI
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Straighten, contentDescription = null, tint = NutriGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Data Antropometri",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NutriGreenDark
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("weight_input_field"),
                            label = { Text("Berat Badan (kg)", color = NutriSlate800, fontWeight = FontWeight.SemiBold) },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("height_input_field"),
                            label = { Text("Tinggi Badan (cm)", color = NutriSlate800, fontWeight = FontWeight.SemiBold) },
                            singleLine = true
                        )
                    }

                    // Live BMI & IMT/U Kemenkes RI Preview bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = NutriSlate100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Perkiraan Indeks Massa Tubuh (IMT):",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NutriSlate800
                                    )
                                    Text(
                                        text = "${liveImtu.bmi} kg/m²",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = NutriGreenDark
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (liveImtu.statusColorType) {
                                        "DANGER" -> NutriRedLight
                                        "WARNING" -> NutriAmberLight
                                        else -> NutriGreenLight
                                    }
                                ) {
                                    Text(
                                        text = "${liveImtu.category} (${liveImtu.zScoreFormatted})",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (liveImtu.statusColorType) {
                                            "DANGER" -> NutriRedRisk
                                            "WARNING" -> NutriAmberDark
                                            else -> NutriGreenDark
                                        },
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Standar Kemenkes RI (Permenkes No. 2/2020) IMT/U usia ${age} th. Rentang Normal: ${liveImtu.normalRangeText}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = NutriSlate700
                            )
                        }
                    }
                }
            }

            // 2. KUESIONER KEBIASAAN MAKAN & GAYA HIDUP
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = NutriTealDark)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "2. Kebiasaan Makan & Gaya Hidup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NutriGreenDark
                        )
                    }

                    // Question 1
                    QuestionOptionGroup(
                        title = "a. Kebiasaan Sarapan Pagi",
                        options = listOf("Setiap hari", "Sering (3-5x)", "Jarang (1-2x)", "Tidak pernah"),
                        selected = breakfastHabit,
                        onSelect = { breakfastHabit = it }
                    )

                    // Question 2
                    QuestionOptionGroup(
                        title = "b. Konsumsi Sayur Harian",
                        options = listOf(">= 2 porsi/hari", "1 porsi/hari", "Jarang", "Tidak pernah"),
                        selected = vegetableIntake,
                        onSelect = { vegetableIntake = it }
                    )

                    // Question 3
                    QuestionOptionGroup(
                        title = "c. Konsumsi Buah Segar",
                        options = listOf(">= 2 porsi/hari", "1 porsi/hari", "Jarang", "Tidak pernah"),
                        selected = fruitIntake,
                        onSelect = { fruitIntake = it }
                    )

                    // Question 4
                    QuestionOptionGroup(
                        title = "d. Makanan Cepat Saji / Gorengan",
                        options = listOf("Jarang/tidak pernah", "1-2x/minggu", ">=3x/minggu"),
                        selected = junkFoodIntake,
                        onSelect = { junkFoodIntake = it }
                    )

                    // Question 5
                    QuestionOptionGroup(
                        title = "e. Minuman Manis Kemasan / Boba",
                        options = listOf("Tidak pernah/jarang", "1 kali/hari", ">=2x/hari"),
                        selected = sweetDrinkIntake,
                        onSelect = { sweetDrinkIntake = it }
                    )

                    // Question 6
                    QuestionOptionGroup(
                        title = "f. Aktivitas Fisik / Olahraga",
                        options = listOf(">= 60 menit/hari", "30-60 menit", "<30 menit"),
                        selected = physicalActivity,
                        onSelect = { physicalActivity = it }
                    )

                    // Question 7
                    QuestionOptionGroup(
                        title = "g. Lama Waktu Duduk / Layar Gadget",
                        options = listOf("<3 jam", "3-6 jam", ">6 jam"),
                        selected = sedentaryTime,
                        onSelect = { sedentaryTime = it }
                    )
                }
            }

            if (validationError != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NutriRedLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = validationError ?: "",
                        color = NutriRedRisk,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // Save and Evaluate Button
            Button(
                onClick = {
                    val weight = weightText.toFloatOrNull()
                    val height = heightText.toFloatOrNull()
                    if (weight == null || weight < 20f || weight > 200f) {
                        validationError = "Harap masukkan berat badan yang valid (20-200 kg)."
                        return@Button
                    }
                    if (height == null || height < 100f || height > 220f) {
                        validationError = "Harap masukkan tinggi badan yang valid (100-220 cm)."
                        return@Button
                    }
                    validationError = null

                    viewModel.calculateAndSaveGiziCheck(
                        weightKg = weight,
                        heightCm = height,
                        breakfast = breakfastHabit,
                        vegetable = vegetableIntake,
                        fruit = fruitIntake,
                        junkFood = junkFoodIntake,
                        sweetDrink = sweetDrinkIntake,
                        physicalAct = physicalActivity,
                        sedentary = sedentaryTime
                    ) { check ->
                        currentResult = check
                        showResultDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_cek_gizi_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NutriGreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.Assessment, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan & Analisis Hasil Cek Gizi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            // Result Display Card (if latest or calculated)
            if (currentResult != null) {
                val res = currentResult!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hasil_cek_gizi_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Hasil Skrining Gizi Siswa",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NutriGreenDark
                                )
                                Text(
                                    text = "Pemeriksaan: ${res.date}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriSlate700
                                )
                            }
                            RiskBadge(riskCategory = res.riskCategory)
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem(label = "Berat Badan", value = "${res.weightKg} kg")
                            MetricItem(label = "Tinggi Badan", value = "${res.heightCm} cm")
                            MetricItem(label = "Nilai IMT", value = "${res.bmi} kg/m²")
                            MetricItem(
                                label = "Z-Score IMT/U",
                                value = if (res.zScore != 0f) "${res.zScore} SD" else "Normal"
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NutriSlate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(
                                    text = "Status Gizi: ${res.bmiCategory}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = NutriGreenDark
                                )
                                Text(
                                    text = "Standar: ${res.standardReference}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriSlate700
                                )
                                Text(
                                    text = "Kategori Pemantauan: ${res.riskCategory}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NutriSlate900
                                )
                            }
                        }

                        Text(
                            text = "Faktor yang Perlu Diperhatikan:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NutriSlate900
                        )
                        Text(
                            text = res.factorsToNote,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriSlate800,
                            lineHeight = 16.sp
                        )

                        Text(
                            text = "Saran Edukasi NutriMind:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = NutriGreenDark
                        )
                        Text(
                            text = res.generalAdvice,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriSlate900,
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // Modal Sheet or Dialog when submitted successfully
    if (showResultDialog && currentResult != null) {
        val res = currentResult!!
        AlertDialog(
            onDismissRequest = { showResultDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NutriGreenRisk)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Hasil Skrining Berhasil", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Hasil IMT: ${res.bmi} kg/m² • Status: ${res.bmiCategory}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NutriGreenDark
                    )
                    Text(
                        text = "Rujukan: Standar Antropometri Kemenkes RI (Permenkes No. 2/2020) IMT/U",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = NutriSlate700
                    )
                    Text(
                        text = "Tingkat Risiko: ${res.riskCategory}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (res.riskCategory == "Risiko Rendah") NutriGreenRisk else NutriAmberDark
                    )
                    Text(
                        text = res.generalAdvice,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = NutriSlate900
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showResultDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                ) {
                    Text("Selesai")
                }
            }
        )
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NutriSlate700)
        Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = NutriSlate900)
    }
}

@Composable
private fun QuestionOptionGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = NutriSlate900
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.take(2).forEach { opt ->
                val isSel = selected == opt
                FilterChip(
                    selected = isSel,
                    onClick = { onSelect(opt) },
                    label = {
                        Text(
                            text = opt,
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                            color = if (isSel) NutriGreenDark else NutriSlate800
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NutriGreenLight,
                        selectedLabelColor = NutriGreenDark,
                        containerColor = NutriSlate50,
                        labelColor = NutriSlate800
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSel,
                        borderColor = if (isSel) NutriGreenPrimary else NutriSlate200
                    )
                )
            }
        }
        if (options.size > 2) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                options.drop(2).forEach { opt ->
                    val isSel = selected == opt
                    FilterChip(
                        selected = isSel,
                        onClick = { onSelect(opt) },
                        label = {
                            Text(
                                text = opt,
                                fontSize = 11.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSel) NutriGreenDark else NutriSlate800
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NutriGreenLight,
                            selectedLabelColor = NutriGreenDark,
                            containerColor = NutriSlate50,
                            labelColor = NutriSlate800
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSel,
                            borderColor = if (isSel) NutriGreenPrimary else NutriSlate200
                        )
                    )
                }
            }
        }
    }
}
