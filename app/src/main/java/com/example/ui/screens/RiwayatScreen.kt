package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.components.RiskBadge
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun RiwayatScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val giziChecks by viewModel.studentGiziChecks.collectAsState()
    val dailyChecks by viewModel.studentDailyChecks.collectAsState()
    val foodLogs by viewModel.studentFoodLogs.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Cek Gizi, 1: Harian, 2: Foto Makanan
    var selectedRiskFilter by remember { mutableStateOf("Semua") }

    val filteredGiziChecks = giziChecks.filter { check ->
        selectedRiskFilter == "Semua" || check.riskCategory.equals(selectedRiskFilter, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Riwayat Pemantauan",
                subtitle = "Catatan Antropometri & Kebiasaan Sehat",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.RIWAYAT,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NutriSlate50)
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = NutriGreenDark
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Cek Gizi", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 0) NutriGreenDark else NutriSlate700) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Harian", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 1) NutriGreenDark else NutriSlate700) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Foto Makanan", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selectedTab == 2) NutriGreenDark else NutriSlate700) }
                )
            }

            // Body content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DisclaimerBanner()
                }

                // TAB 0: RIWAYAT CEK GIZI
                if (selectedTab == 0) {
                    item {
                        // Visual Trend Card
                        if (giziChecks.isNotEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.TrendingUp, contentDescription = null, tint = NutriGreenPrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Progres Antropometri & IMT",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NutriGreenDark
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    val latest = giziChecks.first()
                                    Text(
                                        text = "Saat ini: ${latest.weightKg} kg (TB: ${latest.heightCm} cm) • IMT: ${latest.bmi} kg/m²",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NutriSlate900
                                    )
                                    Text(
                                        text = "Status: ${latest.bmiCategory} • Kategori: ${latest.riskCategory}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NutriSlate800
                                    )
                                }
                            }
                        }
                    }

                    // Risk filter chips
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("Semua", "Risiko Rendah", "Perlu Perhatian", "Risiko Tinggi")) { rf ->
                                val isSel = selectedRiskFilter == rf
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedRiskFilter = rf },
                                    label = {
                                        Text(
                                            text = rf,
                                            fontSize = 11.sp,
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

                    if (filteredGiziChecks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada riwayat cek gizi tercatat.", color = NutriSlate700, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(filteredGiziChecks, key = { it.id }) { check ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("riwayat_gizi_${check.id}"),
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
                                        Text(
                                            text = check.date,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = NutriSlate900
                                        )
                                        RiskBadge(riskCategory = check.riskCategory)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "BB: ${check.weightKg} kg | TB: ${check.heightCm} cm", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NutriSlate800)
                                        Text(text = "IMT: ${check.bmi} (${check.bmiCategory})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NutriGreenDark)
                                    }

                                    Text(
                                        text = "Standar: ${check.standardReference} • Z-Score: ${if (check.zScore != 0f) "${check.zScore} SD" else "Normal"}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NutriTealDark
                                    )

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))

                                    Text(
                                        text = "Catatan: ${check.factorsToNote}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = NutriSlate800,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // TAB 1: RIWAYAT CEK HARIAN
                if (selectedTab == 1) {
                    if (dailyChecks.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada catatan cek kesehatan harian.", color = NutriSlate700, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(dailyChecks, key = { it.id }) { daily ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("riwayat_harian_${daily.id}"),
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
                                        Text(text = daily.date, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = NutriSlate900)
                                        Surface(shape = RoundedCornerShape(10.dp), color = NutriTealLight) {
                                            Text(
                                                text = daily.bodyCondition,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NutriTealDark,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Text(text = daily.summaryText, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)

                                    if (daily.bmr > 0f || daily.tdee > 0f) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = NutriSlate100,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriSlate200),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = "BMR: ${daily.bmr.toInt()} kkal",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NutriGreenDark
                                                )
                                                Text(
                                                    text = "TDEE: ${daily.tdee.toInt()} kkal",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NutriTealDark
                                                )
                                                Text(
                                                    text = daily.activityLevel.ifEmpty { "Aktivitas Harian" },
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = NutriSlate700
                                                )
                                            }
                                        }
                                    }

                                    if (daily.aiAdvice.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = NutriGreenSuperLight,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriGreenPrimary.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Saran AI: ${daily.aiAdvice}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = NutriGreenDark,
                                                modifier = Modifier.padding(8.dp),
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // TAB 2: RIWAYAT FOTO MAKANAN
                if (selectedTab == 2) {
                    if (foodLogs.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 30.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada foto makanan yang disimpan ke riwayat.", color = NutriSlate700, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            }
                        }
                    } else {
                        items(foodLogs, key = { it.id }) { log ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("riwayat_foto_${log.id}"),
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
                                        Text(text = log.foodName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = NutriGreenDark)
                                        Text(text = log.estimatedCalories, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = NutriAmberDark)
                                    }
                                    Text(text = log.date, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate700)
                                    Text(text = "Komponen: ${log.detectedItems}", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800)
                                    Text(text = "Piringku: ${log.foodGroups}", fontSize = 11.sp, color = NutriTealDark, fontWeight = FontWeight.SemiBold)
                                    Text(text = log.balanceEvaluation, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = NutriSlate800, lineHeight = 15.sp)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }
}
