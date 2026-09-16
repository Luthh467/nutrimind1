package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.data.model.DailyCheckEntity
import com.example.data.model.GiziCalculator
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel
import java.util.Locale

@Composable
fun CekHarianScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()
    val latestGiziCheck by viewModel.latestGiziCheck.collectAsState()
    val todayCheck by viewModel.todayDailyCheck.collectAsState()
    val isSubmitting by viewModel.isSubmittingDaily.collectAsState()

    val isMale = remember(currentStudent) {
        val g = currentStudent?.gender ?: "Laki-laki"
        g.contains("Laki", ignoreCase = true) || g.contains("Pria", ignoreCase = true)
    }
    val age = currentStudent?.age ?: 16

    var weightText by remember {
        mutableStateOf(
            latestGiziCheck?.weightKg?.toString() ?: "55.0"
        )
    }
    var heightText by remember {
        mutableStateOf(
            latestGiziCheck?.heightCm?.toString() ?: "165.0"
        )
    }

    // Pilihan Aktivitas Rumus Harris-Benedict (1,55 jika aktif 3-5x/minggu, 1,2 jika jarang olahraga)
    var isActiveExercise by remember {
        mutableStateOf(
            todayCheck?.activityLevel?.contains("1.55") ?: true
        )
    }

    var sarapan by remember { mutableStateOf(todayCheck?.breakfast ?: "Ya") }
    var mealsCount by remember { mutableStateOf(todayCheck?.mealsCount ?: "3 kali") }
    var eatVeg by remember { mutableStateOf(todayCheck?.eatVeg ?: "Ya") }
    var eatFruit by remember { mutableStateOf(todayCheck?.eatFruit ?: "Belum") }
    var sweetDrinks by remember { mutableStateOf(todayCheck?.sweetDrinks ?: "1 kali") }
    var activityDone by remember { mutableStateOf(todayCheck?.activityDone ?: "Ya") }
    var activityDuration by remember { mutableStateOf(todayCheck?.activityDuration ?: "30–60 menit") }
    var sedentaryHours by remember { mutableStateOf(todayCheck?.sedentaryHours ?: "3-6 jam") }
    var sleepHours by remember { mutableStateOf(todayCheck?.sleepHours ?: "6–8 jam") }
    var bodyCondition by remember { mutableStateOf(todayCheck?.bodyCondition ?: "Cukup Baik") }

    var latestSavedDaily by remember { mutableStateOf<DailyCheckEntity?>(todayCheck) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    // Hitung BMR dan TDEE secara real-time berdasarkan Rumus Harris-Benedict yang diperbarui
    val currentWeight = weightText.toFloatOrNull() ?: 55f
    val currentHeight = heightText.toFloatOrNull() ?: 165f
    val bmrValue = remember(currentWeight, currentHeight, age, isMale) {
        GiziCalculator.calculateBmr(
            weightKg = currentWeight,
            heightCm = currentHeight,
            age = age,
            isMale = isMale
        )
    }
    val tdeeResult = remember(bmrValue, isActiveExercise) {
        GiziCalculator.calculateTdee(
            bmr = bmrValue,
            isActive3to5Times = isActiveExercise
        )
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Cek Gizi & Energi Harian",
                subtitle = "Rumus BMR Harris-Benedict & TDEE",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.CEK_HARIAN,
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

            // Student identity & physical info pill
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
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(NutriTealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = NutriTealDark)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${currentStudent?.name ?: "Siswa Madrasah"} (${if (isMale) "Pria" else "Wanita"}, $age th)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = NutriSlate900
                        )
                        Text(
                            text = "Gunakan data fisik harian untuk menghitung kebutuhan kalori tubuh secara akurat.",
                            fontSize = 11.sp,
                            color = NutriSlate600
                        )
                    }
                }
            }

            // ==========================================
            // BAGIAN 1: KALKULATOR BMR & TDEE HARRIS-BENEDICT
            // ==========================================
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bmr_tdee_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = NutriOrangeWarm)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "1. Kebutuhan Energi (BMR & TDEE)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = NutriGreenDark
                                )
                                Text(
                                    text = "Rumus Harris-Benedict yang Diperbarui",
                                    fontSize = 11.sp,
                                    color = NutriSlate600
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NutriGreenLight
                        ) {
                            Text(
                                text = if (isMale) "Formula Pria" else "Formula Wanita",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NutriGreenDark,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Berat Badan & Tinggi Badan input
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = weightText,
                            onValueChange = { weightText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bmr_weight_input"),
                            label = { Text("Berat (kg)") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = heightText,
                            onValueChange = { heightText = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bmr_height_input"),
                            label = { Text("Tinggi (cm)") },
                            singleLine = true
                        )
                    }

                    // Pilihan Aktivitas Harian (Olahraga 3-5x vs Jarang Olahraga)
                    Text(
                        text = "Tingkat Aktivitas Fisik Minggu Ini:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NutriSlate800
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Option 1: Aktif 3-5 kali seminggu (x1.55)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActiveExercise) NutriGreenLight else NutriSlate100,
                            border = BorderStroke(
                                1.dp,
                                if (isActiveExercise) NutriGreenPrimary else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isActiveExercise = true }
                                .testTag("activity_active_button")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Aktif Olahraga",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (isActiveExercise) NutriGreenDark else NutriSlate800
                                    )
                                    if (isActiveExercise) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NutriGreenDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "3–5 kali / minggu",
                                    fontSize = 11.sp,
                                    color = NutriSlate600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pengali: 1,55",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = NutriTealDark
                                )
                            }
                        }

                        // Option 2: Jarang Olahraga (x1.2)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (!isActiveExercise) NutriGreenLight else NutriSlate100,
                            border = BorderStroke(
                                1.dp,
                                if (!isActiveExercise) NutriGreenPrimary else Color.Transparent
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { isActiveExercise = false }
                                .testTag("activity_sedentary_button")
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Jarang Olahraga",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (!isActiveExercise) NutriGreenDark else NutriSlate800
                                    )
                                    if (!isActiveExercise) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = NutriGreenDark,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "Ringan / santai",
                                    fontSize = 11.sp,
                                    color = NutriSlate600
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pengali: 1,20",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    color = NutriTealDark
                                )
                            }
                        }
                    }

                    // Kotak Hasil Perhitungan BMR & TDEE
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NutriTealLight.copy(alpha = 0.35f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // BMR Box
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "BMR (Energi Basal)",
                                        fontSize = 11.sp,
                                        color = NutriSlate600
                                    )
                                    Text(
                                        text = "${bmrValue.toInt()} kkal/hari",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = NutriGreenDark
                                    )
                                    Text(
                                        text = "Energi minimal saat istirahat",
                                        fontSize = 10.sp,
                                        color = NutriSlate600
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // TDEE Box
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "TDEE (Kebutuhan Total)",
                                        fontSize = 11.sp,
                                        color = NutriSlate600
                                    )
                                    Text(
                                        text = "${tdeeResult.tdee.toInt()} kkal/hari",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = NutriTealDark
                                    )
                                    Text(
                                        text = "BMR × ${tdeeResult.activityMultiplier}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NutriTealDark
                                    )
                                }
                            }

                            HorizontalDivider(color = NutriSlate200)

                            // Detail Rumus yang Digunakan
                            Text(
                                text = if (isMale) {
                                    "Rumus Pria: BMR = 88,362 + (13,397×${currentWeight}) + (4,799×${currentHeight}) - (5,677×${age})"
                                } else {
                                    "Rumus Wanita: BMR = 447,593 + (9,247×${currentWeight}) + (3,098×${currentHeight}) - (4,330×${age})"
                                },
                                fontSize = 10.sp,
                                color = NutriSlate600
                            )

                            // Pembagian Porsi Kalori Rekomendasi Gizi Remaja
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Distribusi Energi Harian (Isi Piringku):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NutriSlate800
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sarapan (~25%): ${tdeeResult.mealDistribution.breakfastCalories} kkal", fontSize = 10.sp, color = NutriSlate700)
                                    Text("Siang (~35%): ${tdeeResult.mealDistribution.lunchCalories} kkal", fontSize = 10.sp, color = NutriSlate700)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Malam (~30%): ${tdeeResult.mealDistribution.dinnerCalories} kkal", fontSize = 10.sp, color = NutriSlate700)
                                    Text("Selingan (~10%): ${tdeeResult.mealDistribution.snackCalories} kkal", fontSize = 10.sp, color = NutriSlate700)
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // BAGIAN 2: 10 PERTANYAAN PEMANTAUAN HARIAN
            // ==========================================
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
                    Text(
                        text = "2. 10 Pertanyaan Pemantauan Harian",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = NutriGreenDark
                    )

                    // 1. Sarapan
                    DailyQuestionItem(
                        number = 1,
                        question = "Apakah kamu sudah sarapan hari ini?",
                        options = listOf("Ya", "Tidak"),
                        selected = sarapan,
                        onSelect = { sarapan = it }
                    )

                    // 2. Makan berapa kali
                    DailyQuestionItem(
                        number = 2,
                        question = "Berapa kali kamu makan hari ini?",
                        options = listOf("1 kali", "2 kali", "3 kali", ">3 kali"),
                        selected = mealsCount,
                        onSelect = { mealsCount = it }
                    )

                    // 3. Makan sayur
                    DailyQuestionItem(
                        number = 3,
                        question = "Apakah hari ini kamu makan sayur?",
                        options = listOf("Ya", "Belum"),
                        selected = eatVeg,
                        onSelect = { eatVeg = it }
                    )

                    // 4. Makan buah
                    DailyQuestionItem(
                        number = 4,
                        question = "Apakah hari ini kamu makan buah?",
                        options = listOf("Ya", "Belum"),
                        selected = eatFruit,
                        onSelect = { eatFruit = it }
                    )

                    // 5. Minuman manis
                    DailyQuestionItem(
                        number = 5,
                        question = "Berapa banyak minuman manis yang kamu konsumsi?",
                        options = listOf("Tidak ada", "1 kali", "2 kali", ">=3 kali"),
                        selected = sweetDrinks,
                        onSelect = { sweetDrinks = it }
                    )

                    // 6. Aktivitas fisik
                    DailyQuestionItem(
                        number = 6,
                        question = "Apakah kamu melakukan aktivitas fisik hari ini?",
                        options = listOf("Ya", "Tidak"),
                        selected = activityDone,
                        onSelect = { activityDone = it }
                    )

                    // 7. Durasi aktivitas fisik
                    DailyQuestionItem(
                        number = 7,
                        question = "Berapa lama kamu beraktivitas fisik?",
                        options = listOf("<30 menit", "30–60 menit", ">60 menit"),
                        selected = activityDuration,
                        onSelect = { activityDuration = it }
                    )

                    // 8. Lama duduk / gadget
                    DailyQuestionItem(
                        number = 8,
                        question = "Berapa lama kamu duduk / menggunakan perangkat?",
                        options = listOf("<3 jam", "3-6 jam", ">6 jam"),
                        selected = sedentaryHours,
                        onSelect = { sedentaryHours = it }
                    )

                    // 9. Lama tidur tadi malam
                    DailyQuestionItem(
                        number = 9,
                        question = "Berapa lama kamu tidur tadi malam?",
                        options = listOf("<6 jam", "6–8 jam", ">8 jam"),
                        selected = sleepHours,
                        onSelect = { sleepHours = it }
                    )

                    // 10. Kondisi tubuh
                    DailyQuestionItem(
                        number = 10,
                        question = "Bagaimana kondisi tubuhmu hari ini?",
                        options = listOf("Sangat Bugar", "Cukup Baik", "Kurang Berenergi", "Mudah Lelah", "Pusing / Sakit"),
                        selected = bodyCondition,
                        onSelect = { bodyCondition = it }
                    )
                }
            }

            // Save Button with BMR & TDEE Submission
            Button(
                onClick = {
                    val activityLabel = if (isActiveExercise) {
                        "Aktif 3-5x/minggu (1,55)"
                    } else {
                        "Jarang olahraga (1,20)"
                    }

                    viewModel.submitDailyCheck(
                        sarapan = sarapan,
                        makanBerapaKali = mealsCount,
                        sayur = eatVeg,
                        buah = eatFruit,
                        minumanManis = sweetDrinks,
                        aktivitasFisik = activityDone,
                        durasiAktivitas = activityDuration,
                        durasiDuduk = sedentaryHours,
                        tidur = sleepHours,
                        kondisiTubuh = bodyCondition,
                        bmr = bmrValue,
                        tdee = tdeeResult.tdee,
                        activityLevel = activityLabel
                    ) { saved ->
                        latestSavedDaily = saved
                        showSuccessDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("submit_cek_harian_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark),
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Gemini AI Sedang Menganalisis...", fontSize = 13.sp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simpan & Analisis Kebutuhan Energi Hari Ini", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // ==========================================
            // HASIL PENILAIAN HARIAN
            // ==========================================
            if (latestSavedDaily != null) {
                val daily = latestSavedDaily!!
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("hasil_cek_harian_card"),
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
                            Text(
                                text = "Ringkasan Energi & Kebiasaan Hari Ini",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = NutriGreenDark
                            )
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NutriGreenLight
                            ) {
                                Text(
                                    text = daily.date,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = NutriGreenDark,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        HorizontalDivider()

                        // Energi Bar jika tersimpan BMR & TDEE
                        if (daily.bmr > 0f || daily.tdee > 0f) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = NutriSlate100,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(10.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("BMR (Basal)", fontSize = 11.sp, color = NutriSlate600)
                                        Text("${daily.bmr.toInt()} kkal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NutriGreenDark)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("TDEE (Kebutuhan)", fontSize = 11.sp, color = NutriSlate600)
                                        Text("${daily.tdee.toInt()} kkal", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NutriTealDark)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Aktivitas", fontSize = 11.sp, color = NutriSlate600)
                                        Text(daily.activityLevel.ifEmpty { "1,55" }, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = NutriSlate800)
                                    }
                                }
                            }
                        }

                        Text(
                            text = daily.summaryText,
                            fontSize = 12.sp,
                            color = NutriSlate800,
                            lineHeight = 17.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = NutriTealLight.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = NutriTealDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Evaluasi & Saran NutriMind AI:",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NutriTealDark
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = daily.aiAdvice,
                                    fontSize = 12.sp,
                                    color = NutriSlate900,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NutriGreenRisk)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pencatatan Berhasil", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Kebutuhan energimu hari ini berhasil dihitung:",
                        fontSize = 12.sp,
                        color = NutriSlate700
                    )
                    Text(
                        text = "• BMR: ${bmrValue.toInt()} kkal/hari\n• TDEE: ${tdeeResult.tdee.toInt()} kkal/hari (${if (isActiveExercise) "Aktif 3-5x/minggu [×1,55]" else "Jarang olahraga [×1,20]"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = NutriTealDark
                    )
                    Text(
                        text = "Evaluasi gizi harian dan anjuran porsi telah diperbarui oleh NutriMind AI.",
                        fontSize = 12.sp,
                        color = NutriSlate800
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark)
                ) {
                    Text("Selesai")
                }
            }
        )
    }
}

@Composable
private fun DailyQuestionItem(
    number: Int,
    question: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "$number. $question",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = NutriSlate900
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { opt ->
                val isSel = selected == opt
                FilterChip(
                    selected = isSel,
                    onClick = { onSelect(opt) },
                    label = { Text(opt, fontSize = 10.sp, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = NutriGreenLight,
                        selectedLabelColor = NutriGreenDark
                    )
                )
            }
        }
    }
}
