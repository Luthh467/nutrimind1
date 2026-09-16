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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun BerandaScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentStudent by viewModel.currentStudent.collectAsState()
    val latestCheck by viewModel.latestGiziCheck.collectAsState()
    val todayDailyCheck by viewModel.todayDailyCheck.collectAsState()

    val studentName = currentStudent?.name ?: "Siswa Madrasah"
    val studentGrade = currentStudent?.grade ?: "Kelas Belum Diatur"
    val isProfileComplete = currentStudent?.isProfileComplete ?: false

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "NutriMind AI",
                subtitle = "Sistem Skrining & Pemantauan Gizi Siswa",
                onLogout = { viewModel.logout() },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.BERANDA,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenChat,
                icon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NutriAmber
                    )
                },
                text = {
                    Text(
                        text = "Tanya NutriMind AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                },
                containerColor = NutriGreenDark,
                contentColor = Color.White,
                modifier = Modifier.testTag("floating_tanya_ai_button")
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
            // Profile Incomplete Notice Banner (if applicable)
            if (!isProfileComplete) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_profile_incomplete"),
                    shape = RoundedCornerShape(12.dp),
                    color = NutriAmberLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NutriAmber.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = NutriAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Profil Belum Lengkap",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NutriSlate900
                            )
                            Text(
                                text = "Lengkapi profilmu terlebih dahulu agar hasil pemantauan lebih sesuai.",
                                fontSize = 11.sp,
                                color = NutriSlate800
                            )
                        }
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.COMPLETE_PROFILE) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NutriAmber),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("lengkapi_profil_banner_button")
                        ) {
                            Text("Lengkapi", fontSize = 11.sp, color = NutriSlate900, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Hero Greeting Card with Madrasah Illustration
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("beranda_hero_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(NutriGreenDark, NutriGreenPrimary)
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Assalamu'alaikum,",
                                    color = NutriGreenLight,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = studentName,
                                    color = Color.White,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "$studentGrade • NISN: ${currentStudent?.studentIdNumber ?: "-"}",
                                    color = NutriGreenLight.copy(alpha = 0.9f),
                                    fontSize = 11.sp
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(NutriGreenLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(id = R.drawable.nutrimind_logo_1789536643309),
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp).clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.White.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "Mari pantau asupan gizi harian dan kebiasaan sehatmu untuk mendukung prestasi belajar di madrasah.",
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Quick Status Overview Grid
            Text(
                text = "Ringkasan Kondisi Terkini",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NutriSlate900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Status Gizi & Risiko
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_status_gizi"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonitorWeight,
                                contentDescription = null,
                                tint = NutriGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Status Gizi",
                                fontSize = 12.sp,
                                color = NutriSlate600,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (latestCheck != null) {
                            Text(
                                text = latestCheck!!.bmiCategory,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NutriGreenDark
                            )
                            Text(
                                text = "IMT: ${latestCheck!!.bmi} kg/m² ${if (latestCheck!!.zScore != 0f) "• ${latestCheck!!.zScore} SD" else ""}",
                                fontSize = 11.sp,
                                color = NutriSlate600
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            RiskBadge(riskCategory = latestCheck!!.riskCategory)
                            Text(
                                text = "Standar Kemenkes RI IMT/U",
                                fontSize = 9.sp,
                                color = NutriTealDark,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Dicek: ${latestCheck!!.date}",
                                fontSize = 10.sp,
                                color = NutriSlate400
                            )
                        } else {
                            Text(
                                text = "Belum Ada Data",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = NutriSlate600
                            )
                            Text(
                                text = "Lakukan cek gizi pertama kalimu",
                                fontSize = 11.sp,
                                color = NutriSlate400
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.CEK_GIZI) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Mulai Cek", fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Card 2: Cek Kesehatan Hari Ini
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("card_cek_harian"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = NutriTealPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Cek Hari Ini",
                                fontSize = 12.sp,
                                color = NutriSlate600,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (todayDailyCheck != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NutriGreenLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = NutriGreenRisk,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Sudah Selesai",
                                        color = NutriGreenDark,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Kondisi: ${todayDailyCheck!!.bodyCondition}",
                                fontSize = 11.sp,
                                color = NutriSlate800,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Sarapan: ${todayDailyCheck!!.breakfast} • Tidur: ${todayDailyCheck!!.sleepHours}",
                                fontSize = 10.sp,
                                color = NutriSlate600
                            )
                            if (todayDailyCheck!!.tdee > 0f) {
                                Text(
                                    text = "TDEE: ${todayDailyCheck!!.tdee.toInt()} kkal (BMR: ${todayDailyCheck!!.bmr.toInt()})",
                                    fontSize = 10.sp,
                                    color = NutriTealDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = NutriAmberLight
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = NutriAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Belum Dicek",
                                        color = NutriSlate900,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Hanya butuh 1 menit untuk mencatat kebiasaan sehatmu.",
                                fontSize = 10.sp,
                                color = NutriSlate600
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.CEK_HARIAN) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NutriTealDark),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("Isi Sekarang", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }

            // Quick Action Buttons
            Text(
                text = "Aksi Cepat",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NutriSlate900
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Cek Gizi",
                    subtitle = "Antropometri & IMT",
                    icon = Icons.Default.FitnessCenter,
                    accentColor = NutriGreenPrimary,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CEK_GIZI) }
                )
                QuickActionCard(
                    title = "Foto Makanan",
                    subtitle = "Analisis AI Piringku",
                    icon = Icons.Default.PhotoCamera,
                    accentColor = NutriTealDark,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.FOTO_MAKANAN) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionCard(
                    title = "Cek Harian",
                    subtitle = "Kebiasaan & Tidur",
                    icon = Icons.Default.AssignmentTurnedIn,
                    accentColor = Color(0xFFD97706),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.CEK_HARIAN) }
                )
                QuickActionCard(
                    title = "Belajar Gizi",
                    subtitle = "Edukasi & Tips",
                    icon = Icons.Default.MenuBook,
                    accentColor = Color(0xFF2563EB),
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.navigateTo(AppScreen.EDUKASI) }
                )
            }

            // Daily Madrasah Nutrition Tip
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(NutriTealLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = NutriTealDark,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Tips Gizi Madrasah Hari Ini",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = NutriSlate900
                            )
                            Text(
                                text = "Pedoman Gizi Seimbang Kemenkes RI",
                                fontSize = 10.sp,
                                color = NutriSlate600
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "“Jangan lewatkan sarapan sebelum berangkat ke madrasah. Sarapan bergizi dengan kombinasi karbohidrat kompleks dan protein membantu otak berkonsentrasi optimal selama jam pelajaran berlangsung!”",
                        fontSize = 12.sp,
                        color = NutriSlate800,
                        lineHeight = 17.sp
                    )
                }
            }

            // Disclaimer Banner
            DisclaimerBanner()

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag("quick_action_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = NutriSlate900
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = NutriSlate600
                )
            }
        }
    }
}
