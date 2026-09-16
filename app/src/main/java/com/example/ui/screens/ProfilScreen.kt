package com.example.ui.screens

import androidx.compose.foundation.background
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
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun ProfilScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Profil Siswa",
                subtitle = "Identitas Pemantauan Gizi Madrasah",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onLogout = { viewModel.logout() },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.PROFIL,
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
            // Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(NutriGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = currentStudent?.name?.take(1) ?: "S",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = NutriGreenDark
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentStudent?.name ?: "Siswa Madrasah",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NutriSlate900
                    )

                    Text(
                        text = currentStudent?.email ?: "email@madrasah.sch.id",
                        fontSize = 12.sp,
                        color = NutriSlate600
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    HorizontalDivider()

                    Spacer(modifier = Modifier.height(12.dp))

                    // Detail rows
                    ProfileInfoRow(label = "Nomor NISN", value = currentStudent?.studentIdNumber ?: "-")
                    ProfileInfoRow(label = "Kelas Madrasah", value = currentStudent?.grade ?: "Belum diatur")
                    ProfileInfoRow(label = "Umur", value = "${currentStudent?.age ?: 16} Tahun")
                    ProfileInfoRow(label = "Jenis Kelamin", value = currentStudent?.gender ?: "Laki-laki")
                    ProfileInfoRow(label = "ID Pemantauan", value = currentStudent?.id ?: "-")

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedButton(
                        onClick = { viewModel.navigateTo(AppScreen.COMPLETE_PROFILE) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("edit_profile_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NutriGreenPrimary),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NutriGreenDark)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Perbarui Data Profil", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // Confidentiality Notice Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = NutriTealLight.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, NutriTealPrimary.copy(alpha = 0.3f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = NutriTealDark, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Kerahasiaan Data Terjamin: Data skrining dan antropometri hanya dapat dilihat oleh kamu dan pembina/petugas UKS madrasah yang berwenang.",
                        fontSize = 12.sp,
                        color = NutriTealDark,
                        lineHeight = 16.sp
                    )
                }
            }

            // Logout Button
            Button(
                onClick = { viewModel.logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("logout_profile_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NutriRedRisk)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar dari Akun", fontWeight = FontWeight.Bold, color = Color.White)
            }

            DisclaimerBanner()

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 12.sp, color = NutriSlate600)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = NutriSlate900)
    }
}
