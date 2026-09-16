package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GiziCheckEntity
import com.example.data.model.UserEntity
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriTopAppBar
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import com.example.viewmodel.NutriMindViewModel

@Composable
fun UksDashboardScreen(
    viewModel: NutriMindViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val officer by viewModel.currentOfficer.collectAsState()
    val allStudents by viewModel.allStudents.collectAsState()
    val allChecks by viewModel.allGiziChecks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("Semua") }
    var selectedGenderFilter by remember { mutableStateOf("Semua") }
    var selectedRiskFilter by remember { mutableStateOf("Semua") }

    var selectedStudentDetail by remember { mutableStateOf<UserEntity?>(null) }
    var followUpNoteText by remember { mutableStateOf("") }
    var showExportDialog by remember { mutableStateOf(false) }

    // Map each student to their latest check
    val studentWithChecks = allStudents.map { student ->
        val studentChecks = allChecks.filter { it.studentId == student.id }.sortedByDescending { it.timestamp }
        val latest = studentChecks.firstOrNull()
        Triple(student, latest, studentChecks)
    }

    // Filter logic
    val filteredStudents = studentWithChecks.filter { (student, latest, _) ->
        val matchesSearch = searchQuery.isBlank() ||
                student.name.contains(searchQuery, ignoreCase = true) ||
                student.id.contains(searchQuery, ignoreCase = true) ||
                student.studentIdNumber.contains(searchQuery, ignoreCase = true)

        val matchesClass = selectedClassFilter == "Semua" || student.grade.contains(selectedClassFilter, ignoreCase = true)
        val matchesGender = selectedGenderFilter == "Semua" || student.gender.equals(selectedGenderFilter, ignoreCase = true)
        val matchesRisk = selectedRiskFilter == "Semua" || (latest?.riskCategory.equals(selectedRiskFilter, ignoreCase = true))

        matchesSearch && matchesClass && matchesGender && matchesRisk
    }

    // Metrics
    val totalStudents = allStudents.size
    val lowRiskCount = studentWithChecks.count { it.second?.riskCategory == "Risiko Rendah" }
    val attentionCount = studentWithChecks.count { it.second?.riskCategory == "Perlu Perhatian" }
    val highRiskCount = studentWithChecks.count { it.second?.riskCategory == "Risiko Tinggi" }

    // Students needing reminder (>7 days or never checked)
    val now = System.currentTimeMillis()
    val needingReminder = studentWithChecks.filter { (_, latest, _) ->
        latest == null || (now - latest.timestamp) > (7L * 24 * 60 * 60 * 1000)
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Dashboard UKS Madrasah",
                subtitle = "${officer?.name ?: "Petugas UKS"} • ${officer?.madrasahName ?: "MAN 1"}",
                onLogout = { viewModel.logout() }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NutriSlate50)
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Confidentiality Banner
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("uks_confidentiality_banner"),
                        shape = RoundedCornerShape(12.dp),
                        color = NutriAmberLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriAmber.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = NutriAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pemberitahuan Kerahasiaan: Data ini bersifat rahasia dan hanya digunakan untuk pemantauan serta tindak lanjut kesehatan siswa.",
                                fontSize = 11.sp,
                                color = NutriSlate900,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Overview Metrics Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Ringkasan Pemantauan Gizi Siswa",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NutriSlate900
                            )

                            Button(
                                onClick = { showExportDialog = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("export_data_button")
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ekspor Data", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            KpiMetricCard(
                                title = "Total Siswa",
                                value = "$totalStudents",
                                bgColor = NutriSlate100,
                                textColor = NutriSlate900,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Risiko Rendah",
                                value = "$lowRiskCount",
                                bgColor = NutriGreenLight,
                                textColor = NutriGreenDark,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Perlu Perhatian",
                                value = "$attentionCount",
                                bgColor = NutriYellowLight,
                                textColor = Color(0xFF854D0E),
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Risiko Tinggi",
                                value = "$highRiskCount",
                                bgColor = NutriRedLight,
                                textColor = NutriRedRisk,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Siswa Perlu Diingatkan Callout
                if (needingReminder.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriAmber.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = NutriAmber, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Siswa Perlu Diingatkan (> 7 hari belum cek)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NutriSlate900
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Terdapat ${needingReminder.size} siswa yang belum melakukan pemantauan berkala minggu ini.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NutriSlate800
                                )
                            }
                        }
                    }
                }

                // Search & Filters
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("uks_search_student_input"),
                            placeholder = { Text("Cari ID Siswa atau Nama...", fontSize = 12.sp, color = NutriSlate700) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NutriSlate700) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NutriGreenPrimary,
                                unfocusedBorderColor = NutriSlate300
                            )
                        )

                        // Filters row: Class & Risk
                        Text(text = "Filter Kategori Risiko:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NutriSlate800)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Semua", "Risiko Rendah", "Perlu Perhatian", "Risiko Tinggi")) { rf ->
                                val isSel = selectedRiskFilter == rf
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedRiskFilter = rf },
                                    label = {
                                        Text(
                                            text = rf,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSel) NutriTealDark else NutriSlate800
                                        )
                                    },
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

                        Text(text = "Filter Kelas:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NutriSlate800)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Semua", "X", "XI", "XII")) { cl ->
                                val isSel = selectedClassFilter == cl
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedClassFilter = cl },
                                    label = {
                                        Text(
                                            text = cl,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.SemiBold,
                                            color = if (isSel) NutriGreenDark else NutriSlate800
                                        )
                                    },
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

                // Student List / Table Cards
                items(filteredStudents, key = { it.first.id }) { (student, latest, allStudentChecks) ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedStudentDetail = student }
                            .testTag("uks_student_card_${student.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(NutriGreenLight),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = student.name.take(1),
                                            fontWeight = FontWeight.Bold,
                                            color = NutriGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = student.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NutriSlate900)
                                        Text(text = "${student.id} • ${student.grade} • ${student.age} th", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                                    }
                                }

                                if (latest != null) {
                                    RiskBadge(riskCategory = latest.riskCategory)
                                } else {
                                    Surface(shape = RoundedCornerShape(12.dp), color = NutriSlate100, border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200)) {
                                        Text(
                                            text = "Belum Ada Data",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = NutriSlate800,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    if (latest != null) {
                                        Text(
                                            text = "Status: ${latest.bmiCategory} (IMT: ${latest.bmi}${if (latest.zScore != 0f) ", Z: ${latest.zScore} SD" else ""})",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NutriGreenDark
                                        )
                                        Text(
                                            text = "Terakhir dicek: ${latest.date} • Kemenkes RI IMT/U",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = NutriSlate700
                                        )
                                    } else {
                                        Text(text = "Perlu diingatkan untuk cek gizi", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NutriAmberDark)
                                    }
                                }

                                Button(
                                    onClick = { selectedStudentDetail = student },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("Detail & Tindak Lanjut", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    DisclaimerBanner()
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    // Student Detail & Follow-up Dialog
    if (selectedStudentDetail != null) {
        val student = selectedStudentDetail!!
        val studentChecks = allChecks.filter { it.studentId == student.id }.sortedByDescending { it.timestamp }
        val latest = studentChecks.firstOrNull()

        AlertDialog(
            onDismissRequest = { selectedStudentDetail = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = NutriTealDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = student.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NutriSlate900)
                        Text(text = "${student.id} • ${student.grade} • NISN: ${student.studentIdNumber}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (latest != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NutriSlate100,
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(text = "Status Gizi Terkini (Standar Kemenkes RI):", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NutriSlate900)
                                Text(text = "BB: ${latest.weightKg} kg | TB: ${latest.heightCm} cm | IMT: ${latest.bmi} kg/m²", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate900)
                                Text(text = "Z-Score IMT/U: ${if (latest.zScore != 0f) "${latest.zScore} SD" else "Normal"} • ${latest.standardReference}", fontSize = 11.sp, color = NutriTealDark, fontWeight = FontWeight.Bold)
                                Text(text = "Kategori: ${latest.bmiCategory} • ${latest.riskCategory}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NutriGreenDark)
                                Text(text = "Kebiasaan: Sarapan: ${latest.breakfastHabit} • Sayur: ${latest.vegetableIntake} • Buah: ${latest.fruitIntake}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                                Text(text = "Faktor Risiko: ${latest.factorsToNote}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                            }
                        }
                    } else {
                        Text("Siswa ini belum pernah melakukan cek gizi.", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                    }

                    // Form Catat Tindak Lanjut UKS
                    Text(
                        text = "Catat Tindak Lanjut Petugas UKS:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = NutriSlate900
                    )

                    OutlinedTextField(
                        value = followUpNoteText,
                        onValueChange = { followUpNoteText = it },
                        placeholder = { Text("misal: Diberikan edukasi sarapan bergizi seimbang & tablet tambah darah (TTD)", fontSize = 11.sp, color = NutriSlate700) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("follow_up_note_input"),
                        maxLines = 3
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val quickNotes = listOf("Edukasi Sarapan", "Berikan Tablet Tambah Darah (TTD)", "Anjuran Bekal Sayur/Buah")
                        quickNotes.forEach { qn ->
                            Surface(
                                modifier = Modifier.clickable { followUpNoteText = qn },
                                shape = RoundedCornerShape(8.dp),
                                color = NutriTealLight
                            ) {
                                Text(
                                    text = qn,
                                    fontSize = 9.sp,
                                    color = NutriTealDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (followUpNoteText.isNotBlank()) {
                            viewModel.addUksFollowUp(student.id, followUpNoteText) {
                                followUpNoteText = ""
                                selectedStudentDetail = null
                            }
                        } else {
                            selectedStudentDetail = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark)
                ) {
                    Text("Simpan Tindak Lanjut")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedStudentDetail = null }) {
                    Text("Tutup")
                }
            }
        )
    }

    // Export Data Dialog
    if (showExportDialog) {
        val exportSummary = buildString {
            appendLine("=== LAPORAN PEMANTAUAN GIZI SISWA MADRASAH - NUTRIMIND AI ===")
            appendLine("Madrasah: ${officer?.madrasahName ?: "MAN 1 Model"}")
            appendLine("Petugas UKS: ${officer?.name ?: "Petugas UKS"}")
            appendLine("Total Siswa Terdaftar: $totalStudents")
            appendLine("Risiko Rendah: $lowRiskCount")
            appendLine("Perlu Perhatian: $attentionCount")
            appendLine("Risiko Tinggi: $highRiskCount")
            appendLine("Format: ID | Nama | Kelas | BB | TB | IMT | Z-Score (Permenkes No. 2/2020) | Status Gizi | Kategori Risiko")
            appendLine("-----------------------------------------------------------")
            filteredStudents.forEach { (s, latest, _) ->
                appendLine("${s.id} | ${s.name} | ${s.grade} | BB: ${latest?.weightKg ?: "-"}kg | TB: ${latest?.heightCm ?: "-"}cm | IMT: ${latest?.bmi ?: "-"} | Z: ${if (latest?.zScore != null && latest.zScore != 0f) "${latest.zScore} SD" else "Normal"} | Status: ${latest?.bmiCategory ?: "-"} | Risiko: ${latest?.riskCategory ?: "-"}")
            }
            appendLine("===========================================================")
            appendLine("Catatan: NutriMind AI merupakan sistem skrining awal, bukan diagnosis medis.")
        }

        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, tint = NutriTealDark)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ekspor Rekapitulasi Data UKS", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Salin data teks rekap berikut untuk arsip laporan madrasah atau Puskesmas:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NutriSlate100,
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = exportSummary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = NutriSlate900,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Rekap Gizi UKS NutriMind", exportSummary)
                        clipboard.setPrimaryClip(clip)
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark)
                ) {
                    Text("Salin ke Clipboard")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun KpiMetricCard(
    title: String,
    value: String,
    bgColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = textColor)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, fontSize = 9.sp, color = textColor, fontWeight = FontWeight.Medium, maxLines = 1)
        }
    }
}
