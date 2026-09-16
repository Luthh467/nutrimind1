package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriTopAppBar
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun CompleteProfileScreen(
    viewModel: NutriMindViewModel,
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()

    var name by remember(currentStudent) { mutableStateOf(currentStudent?.name ?: "") }
    var grade by remember(currentStudent) { mutableStateOf(if (!currentStudent?.grade.isNullOrBlank()) currentStudent!!.grade else "XI IPA 1") }
    var ageText by remember(currentStudent) { mutableStateOf(currentStudent?.age?.toString() ?: "16") }
    var gender by remember(currentStudent) { mutableStateOf(currentStudent?.gender ?: "Laki-laki") }
    var nisn by remember(currentStudent) { mutableStateOf(currentStudent?.studentIdNumber ?: "") }
    var agreementChecked by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val gradeOptions = listOf("X MA 1", "X MA 2", "XI IPA 1", "XI IPA 2", "XI IPS 1", "XII MA 1", "XII MA 2")

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Lengkapi Profil Siswa",
                subtitle = "NutriMind AI Madrasah",
                onLogout = { viewModel.logout() }
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
            // Informational Notice Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = NutriGreenLight
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.School,
                        contentDescription = null,
                        tint = NutriGreenDark,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Lengkapi profilmu terlebih dahulu agar hasil pemantauan gizi dan rekomendasi nutrisi sesuai dengan kelompok umurmu.",
                        fontSize = 12.sp,
                        color = NutriGreenDark,
                        lineHeight = 16.sp
                    )
                }
            }

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NutriRedLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = NutriRedRisk,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Nama atau Inisial
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_name_input"),
                        label = { Text("Nama atau Inisial Siswa", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("misal: Ahmad S. atau Ahmad Syahrul", color = NutriSlate700) },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = NutriSlate700) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriGreenPrimary,
                            unfocusedBorderColor = NutriSlate300,
                            focusedLabelColor = NutriGreenDark,
                            unfocusedLabelColor = NutriSlate800
                        ),
                        singleLine = true
                    )

                    // NISN
                    OutlinedTextField(
                        value = nisn,
                        onValueChange = { nisn = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_nisn_input"),
                        label = { Text("Nomor Identitas Siswa / NISN", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("misal: 0078129841", color = NutriSlate700) },
                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = NutriSlate700) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriGreenPrimary,
                            unfocusedBorderColor = NutriSlate300,
                            focusedLabelColor = NutriGreenDark,
                            unfocusedLabelColor = NutriSlate800
                        ),
                        singleLine = true
                    )

                    // Umur
                    OutlinedTextField(
                        value = ageText,
                        onValueChange = { ageText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_age_input"),
                        label = { Text("Umur (Tahun)", fontWeight = FontWeight.SemiBold) },
                        placeholder = { Text("misal: 16", color = NutriSlate700) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriGreenPrimary,
                            unfocusedBorderColor = NutriSlate300,
                            focusedLabelColor = NutriGreenDark,
                            unfocusedLabelColor = NutriSlate800
                        ),
                        singleLine = true
                    )

                    // Jenis Kelamin
                    Text(
                        text = "Jenis Kelamin",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NutriSlate900
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf("Laki-laki", "Perempuan").forEach { g ->
                            val isSelected = gender == g
                            FilterChip(
                                selected = isSelected,
                                onClick = { gender = g },
                                label = {
                                    Text(
                                        text = g,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) NutriGreenDark else NutriSlate800
                                    )
                                },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, tint = NutriGreenDark, modifier = Modifier.size(16.dp)) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NutriGreenLight,
                                    selectedLabelColor = NutriGreenDark,
                                    containerColor = NutriSlate50,
                                    labelColor = NutriSlate800
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) NutriGreenPrimary else NutriSlate200
                                )
                            )
                        }
                    }

                    // Kelas Madrasah
                    Text(
                        text = "Kelas di Madrasah",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = NutriSlate900
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Quick selection chips
                        gradeOptions.take(4).forEach { opt ->
                            val isSelected = grade == opt
                            FilterChip(
                                selected = isSelected,
                                onClick = { grade = opt },
                                label = {
                                    Text(
                                        text = opt,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) NutriGreenDark else NutriSlate800
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
                                    selected = isSelected,
                                    borderColor = if (isSelected) NutriGreenPrimary else NutriSlate200
                                )
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        gradeOptions.drop(4).forEach { opt ->
                            val isSelected = grade == opt
                            FilterChip(
                                selected = isSelected,
                                onClick = { grade = opt },
                                label = {
                                    Text(
                                        text = opt,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                        color = if (isSelected) NutriGreenDark else NutriSlate800
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
                                    selected = isSelected,
                                    borderColor = if (isSelected) NutriGreenPrimary else NutriSlate200
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Persetujuan Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_agreement_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = agreementChecked,
                            onCheckedChange = { agreementChecked = it },
                            colors = CheckboxDefaults.colors(checkedColor = NutriGreenPrimary)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Saya menyetujui penggunaan aplikasi NutriMind AI untuk pemantauan gizi siswa madrasah.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = NutriSlate900,
                            lineHeight = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Simpan Profil Button
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Harap masukkan nama atau inisial siswa."
                                return@Button
                            }
                            val age = ageText.toIntOrNull()
                            if (age == null || age < 10 || age > 25) {
                                errorMessage = "Harap masukkan umur siswa yang valid (antara 10-25 tahun)."
                                return@Button
                            }
                            if (!agreementChecked) {
                                errorMessage = "Harap centang persetujuan penggunaan aplikasi."
                                return@Button
                            }

                            errorMessage = null
                            viewModel.saveStudentProfile(
                                name = name.trim(),
                                grade = grade,
                                age = age,
                                gender = gender,
                                nisn = nisn.trim()
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutriGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Simpan Profil & Lanjut ke Beranda",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            DisclaimerBanner()
        }
    }
}
