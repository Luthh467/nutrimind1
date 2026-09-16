package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.BookmarkAdded
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
import com.example.data.model.EducationArticleEntity
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.NutriBottomNavigation
import com.example.ui.components.NutriTopAppBar
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

@Composable
fun EdukasiScreen(
    viewModel: NutriMindViewModel,
    onOpenChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val articles by viewModel.allArticles.collectAsState()
    val latestCheck by viewModel.latestGiziCheck.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Semua") }
    var viewingArticle by remember { mutableStateOf<EducationArticleEntity?>(null) }

    val categories = listOf(
        "Semua",
        "Gizi Seimbang",
        "Sarapan",
        "Buah dan Sayur",
        "Makanan dan Minuman",
        "Aktivitas Fisik",
        "Tidur dan Kebiasaan Sehat"
    )

    val filteredArticles = articles.filter { art ->
        val matchesCategory = (selectedCategory == "Semua" || art.category.equals(selectedCategory, ignoreCase = true))
        val matchesQuery = searchQuery.isBlank() || art.title.contains(searchQuery, ignoreCase = true) || art.summary.contains(searchQuery, ignoreCase = true)
        matchesCategory && matchesQuery
    }

    Scaffold(
        topBar = {
            NutriTopAppBar(
                title = "Belajar Gizi Madrasah",
                subtitle = "Edukasi Gizi Seimbang & Pola Hidup Sehat",
                onBack = { viewModel.navigateTo(AppScreen.BERANDA) },
                onChatClick = onOpenChat
            )
        },
        bottomBar = {
            NutriBottomNavigation(
                currentScreen = AppScreen.EDUKASI,
                onScreenSelected = { viewModel.navigateTo(it) }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenChat,
                icon = { Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = NutriAmber) },
                text = { Text("Tanya AI", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                containerColor = NutriGreenDark,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(NutriSlate50)
                .padding(padding)
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_articles_input"),
                    placeholder = { Text("Cari topik gizi, sarapan, buah...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NutriSlate400) },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = null, tint = NutriSlate400)
                            }
                        }
                    } else null,
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NutriGreenPrimary,
                        unfocusedBorderColor = NutriSlate400
                    )
                )
            }

            // Category Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { cat ->
                    val isSel = selectedCategory == cat
                    FilterChip(
                        selected = isSel,
                        onClick = { selectedCategory = cat },
                        label = { Text(cat, fontSize = 11.sp, fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NutriGreenLight,
                            selectedLabelColor = NutriGreenDark
                        )
                    )
                }
            }

            // Article List
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    DisclaimerBanner(modifier = Modifier.padding(vertical = 4.dp))
                }

                // High-risk personalized recommendation callout
                if (latestCheck != null && latestCheck!!.riskCategory != "Risiko Rendah") {
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("rekomendasi_edukasi_pribadi"),
                            shape = RoundedCornerShape(12.dp),
                            color = NutriAmberLight.copy(alpha = 0.7f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, NutriAmber.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Recommend, contentDescription = null, tint = NutriAmber)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Rekomendasi Berdasarkan Skriningmu",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = NutriSlate900
                                    )
                                    Text(
                                        text = "Prioritaskan membaca panduan Sarapan Pagi, Konsumsi Buah & Sayur, serta Pembatasan Gula Garam Lemak (GGL).",
                                        fontSize = 11.sp,
                                        color = NutriSlate800
                                    )
                                }
                            }
                        }
                    }
                }

                items(filteredArticles, key = { it.id }) { art ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewingArticle = art }
                            .testTag("article_card_${art.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = NutriTealLight
                                ) {
                                    Text(
                                        text = art.category,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NutriTealDark,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = art.readTime, fontSize = 10.sp, color = NutriSlate400)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    IconButton(
                                        onClick = { viewModel.toggleArticleFavorite(art.id, !art.isFavorite) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (art.isFavorite) Icons.Outlined.BookmarkAdded else Icons.Outlined.BookmarkAdd,
                                            contentDescription = "Simpan",
                                            tint = if (art.isFavorite) NutriAmber else NutriSlate400,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = art.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = NutriSlate900
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = art.summary,
                                fontSize = 12.sp,
                                color = NutriSlate600,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    // Article Detail Modal Dialog
    if (viewingArticle != null) {
        val art = viewingArticle!!
        AlertDialog(
            onDismissRequest = { viewingArticle = null },
            title = {
                Column {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NutriTealLight
                    ) {
                        Text(
                            text = art.category,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NutriTealDark,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = art.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NutriGreenDark)
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(text = art.content, fontSize = 13.sp, color = NutriSlate800, lineHeight = 19.sp)
                    HorizontalDivider()
                    Text(
                        text = "Sumber: Panduan Gizi Remaja Kemenkes RI & Konsep NutriMind AI Madrasah.",
                        fontSize = 11.sp,
                        color = NutriSlate400,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewingArticle = null },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriGreenPrimary)
                ) {
                    Text("Tutup")
                }
            }
        )
    }
}
