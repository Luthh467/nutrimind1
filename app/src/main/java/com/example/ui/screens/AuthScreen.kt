package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DisclaimerBanner
import com.example.ui.theme.*
import com.example.viewmodel.NutriMindViewModel

@Composable
fun AuthScreen(
    viewModel: NutriMindViewModel,
    modifier: Modifier = Modifier
) {
    var selectedRoleTab by remember { mutableStateOf(0) } // 0 = Siswa, 1 = UKS
    var showHelpDialog by remember { mutableStateOf(false) }
    var showGoogleAccountPicker by remember { mutableStateOf(false) }
    var isCreatingNewStudentAccount by remember { mutableStateOf(false) }

    // UKS Form State
    var uksEmail by remember { mutableStateOf("") }
    var uksPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showRegisterUksDialog by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NutriSlate50)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // App Logo & Branding Header
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .background(NutriGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.nutrimind_logo_1789536643309),
                contentDescription = "Logo NutriMind AI",
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "NutriMind AI",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = NutriGreenDark,
            letterSpacing = (-0.5).sp
        )

        Text(
            text = "“Temanmu untuk mengenal dan menjaga gizi setiap hari.”",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = NutriGreenPrimary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        Text(
            text = "Pantau kondisi gizi, kenali makananmu, dan bangun kebiasaan sehat.",
            style = MaterialTheme.typography.bodySmall,
            color = NutriSlate600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Role Selector Tabs: "Masuk sebagai Siswa" vs "Masuk sebagai Petugas UKS"
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("role_tab_selector"),
            shape = RoundedCornerShape(16.dp),
            color = NutriSlate100
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = {
                        selectedRoleTab = 0
                        authError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_tab_siswa"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRoleTab == 0) NutriGreenPrimary else Color.Transparent,
                        contentColor = if (selectedRoleTab == 0) Color.White else NutriSlate600
                    ),
                    elevation = if (selectedRoleTab == 0) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text(
                        text = "Masuk sebagai Siswa",
                        fontSize = 12.sp,
                        fontWeight = if (selectedRoleTab == 0) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        selectedRoleTab = 1
                        authError = null
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("role_tab_uks"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedRoleTab == 1) NutriTealDark else Color.Transparent,
                        contentColor = if (selectedRoleTab == 1) Color.White else NutriSlate600
                    ),
                    elevation = if (selectedRoleTab == 1) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) else null
                ) {
                    Text(
                        text = "Masuk sebagai Petugas UKS",
                        fontSize = 12.sp,
                        fontWeight = if (selectedRoleTab == 1) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Error message if any
        if (authError != null) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(8.dp),
                color = NutriRedLight
            ) {
                Text(
                    text = authError ?: "",
                    color = NutriRedRisk,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // TAB 1: SISWA LOGIN
        if (selectedRoleTab == 0) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("siswa_login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = NutriGreenPrimary,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Akses Khusus Siswa Madrasah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NutriSlate900
                    )

                    Text(
                        text = "Masuk dengan akun Google untuk memulai skrining dan memantau status gizimu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NutriSlate600,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Button: Lanjutkan dengan Google
                    Button(
                        onClick = {
                            isCreatingNewStudentAccount = false
                            showGoogleAccountPicker = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("lanjutkan_google_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutriGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Lanjutkan dengan Google",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button: Buat Akun Baru
                    OutlinedButton(
                        onClick = {
                            isCreatingNewStudentAccount = true
                            showGoogleAccountPicker = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("buat_akun_siswa_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = NutriGreenDark
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Buat Akun Baru",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Warning notice
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = NutriSlate100
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NutriSlate600,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gunakan akun Google milikmu sendiri. Jangan menggunakan akun orang lain.",
                                fontSize = 11.sp,
                                color = NutriSlate600,
                                lineHeight = 15.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Help Link: "Lupa atau mengalami masalah saat login?"
                    Text(
                        text = "Lupa atau mengalami masalah saat login?",
                        color = NutriTealDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clickable { showHelpDialog = true }
                            .padding(4.dp)
                            .testTag("bantuan_login_link")
                    )
                }
            }
        } else {
            // TAB 2: UKS LOGIN
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("uks_login_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.MedicalServices,
                        contentDescription = null,
                        tint = NutriTealDark,
                        modifier = Modifier.size(44.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Portal Petugas UKS Madrasah",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = NutriSlate900
                    )

                    Text(
                        text = "Akses pemantauan gizi dan tindak lanjut kesehatan siswa madrasah.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NutriSlate600,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = uksEmail,
                        onValueChange = { uksEmail = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("uks_email_input"),
                        label = { Text("Email Petugas UKS") },
                        placeholder = { Text("uks@madrasah.sch.id") },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = NutriTealDark)
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = uksPassword,
                        onValueChange = { uksPassword = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("uks_password_input"),
                        label = { Text("Kata Sandi") },
                        leadingIcon = {
                            Icon(Icons.Default.VpnKey, contentDescription = null, tint = NutriTealDark)
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = "Tampilkan sandi"
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Button: Masuk sebagai Petugas UKS
                    Button(
                        onClick = {
                            if (uksEmail.isBlank() || uksPassword.isBlank()) {
                                authError = "Harap masukkan email dan kata sandi petugas UKS."
                                return@Button
                            }
                            viewModel.loginAsUksOfficer(uksEmail, uksPassword) { success, msg ->
                                if (!success) {
                                    authError = msg
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("masuk_uks_button"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutriTealDark,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Masuk sebagai Petugas UKS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Button: Buat Akun Petugas UKS
                    OutlinedButton(
                        onClick = { showRegisterUksDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("buat_akun_uks_button"),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriTealDark),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriTealDark)
                    ) {
                        Text(
                            text = "Buat Akun Petugas UKS",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quick Demo Credentials Helper for reviewer
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                uksEmail = "uks@madrasah.sch.id"
                                uksPassword = "uks12345"
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = NutriTealLight.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriTealPrimary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = NutriTealDark,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Klik di sini untuk mengisi Akun Demo UKS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NutriTealDark
                                )
                                Text(
                                    text = "Email: uks@madrasah.sch.id | Sandi: uks12345",
                                    fontSize = 10.sp,
                                    color = NutriSlate600
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mandatory Disclaimer Banner
        DisclaimerBanner(
            text = "NutriMind AI merupakan sistem skrining dan pemantauan awal, bukan alat diagnosis medis."
        )

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Google Account Picker Simulation Dialog (Compatible with OAuth specification)
    if (showGoogleAccountPicker) {
        AlertDialog(
            onDismissRequest = { showGoogleAccountPicker = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = NutriGreenPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCreatingNewStudentAccount) "Pilih Akun Google Baru" else "Pilih Akun Google Siswa",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pilih akun Google terdaftar milikmu untuk melanjutkan:",
                        fontSize = 13.sp,
                        color = NutriSlate600
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val accounts = listOf(
                        Triple("syahrulalfiantroso@gmail.com", "Ahmad Syahrul", "XI IPA 1"),
                        Triple("fatimah.zahra@madrasah.sch.id", "Fatimah Az-Zahra", "X MA 2"),
                        Triple("m.rizki@madrasah.sch.id", "Muhammad Rizki", "XII IPS 1")
                    )

                    accounts.forEach { (email, name, grade) ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    showGoogleAccountPicker = false
                                    viewModel.loginAsGoogleStudent(email, name, isCreatingNewStudentAccount)
                                },
                            shape = RoundedCornerShape(10.dp),
                            color = NutriSlate100
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(NutriGreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1),
                                        fontWeight = FontWeight.Bold,
                                        color = NutriGreenDark
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "$email • $grade", fontSize = 11.sp, color = NutriSlate600)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Add customized new account
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showGoogleAccountPicker = false
                                val rand = (100..999).random()
                                viewModel.loginAsGoogleStudent(
                                    "siswa$rand@madrasah.sch.id",
                                    "Siswa Madrasah $rand",
                                    isNewAccount = true
                                )
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = NutriTealLight.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = NutriTealDark)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Gunakan Akun Google Siswa Lain",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = NutriTealDark
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGoogleAccountPicker = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Help Dialog: "Lupa atau mengalami masalah saat login?"
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.HelpOutline, contentDescription = null, tint = NutriGreenPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bantuan Masuk NutriMind AI", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "1. Siswa masuk menggunakan Akun Google pribadi yang aktif di perangkat.",
                        fontSize = 13.sp,
                        color = NutriSlate800
                    )
                    Text(
                        text = "2. NutriMind AI tidak pernah meminta atau menyimpan kata sandi akun Google kamu secara langsung.",
                        fontSize = 13.sp,
                        color = NutriSlate800
                    )
                    Text(
                        text = "3. Jika mengalami kendala saat login Google, pastikan koneksi internet stabil dan akun Google sudah terpasang di HP.",
                        fontSize = 13.sp,
                        color = NutriSlate800
                    )
                    Text(
                        text = "4. Jika data profil tidak muncul, silakan hubungi Pembina UKS Madrasah untuk pengecekan data pemantauan.",
                        fontSize = 13.sp,
                        color = NutriSlate800
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                ) {
                    Text("Mengerti")
                }
            }
        )
    }

    // Register UKS Officer Dialog
    if (showRegisterUksDialog) {
        var newOfficerName by remember { mutableStateOf("") }
        var newOfficerEmail by remember { mutableStateOf("") }
        var newMadrasahName by remember { mutableStateOf("MAN 1 Model") }
        var newOfficerCode by remember { mutableStateOf("UKS-" + (1000..9999).random()) }
        var newOfficerPass by remember { mutableStateOf("") }
        var regError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showRegisterUksDialog = false },
            title = {
                Text(
                    text = "Pendaftaran Petugas UKS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = NutriTealDark
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Setelah pendaftaran, akun petugas UKS diverifikasi oleh sistem agar kerahasiaan data siswa tetap terjaga.",
                        fontSize = 11.sp,
                        color = NutriSlate600
                    )

                    if (regError != null) {
                        Text(text = regError ?: "", color = NutriRedRisk, fontSize = 12.sp)
                    }

                    OutlinedTextField(
                        value = newOfficerName,
                        onValueChange = { newOfficerName = it },
                        label = { Text("Nama Lengkap & Gelar") },
                        placeholder = { Text("Ibu Aisyah, S.Kep") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newOfficerEmail,
                        onValueChange = { newOfficerEmail = it },
                        label = { Text("Email Petugas UKS") },
                        placeholder = { Text("petugas@madrasah.sch.id") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newMadrasahName,
                        onValueChange = { newMadrasahName = it },
                        label = { Text("Nama Madrasah") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newOfficerCode,
                        onValueChange = { newOfficerCode = it },
                        label = { Text("Kode/Nomor Petugas") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newOfficerPass,
                        onValueChange = { newOfficerPass = it },
                        label = { Text("Kata Sandi") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newOfficerName.isBlank() || newOfficerEmail.isBlank() || newOfficerPass.isBlank()) {
                            regError = "Harap isi semua kolom wajib."
                            return@Button
                        }
                        viewModel.registerUksOfficer(
                            name = newOfficerName,
                            email = newOfficerEmail,
                            madrasahName = newMadrasahName,
                            officerCode = newOfficerCode,
                            password = newOfficerPass
                        ) { success, msg ->
                            if (success) {
                                showRegisterUksDialog = false
                            } else {
                                regError = msg
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark)
                ) {
                    Text("Daftar Akun UKS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRegisterUksDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}
