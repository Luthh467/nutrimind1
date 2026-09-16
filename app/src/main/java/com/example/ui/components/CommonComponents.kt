package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen

@Composable
fun DisclaimerBanner(
    modifier: Modifier = Modifier,
    text: String = "NutriMind AI merupakan sistem skrining dan pemantauan awal, bukan alat diagnosis medis."
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("disclaimer_banner"),
        shape = RoundedCornerShape(12.dp),
        color = NutriAmberLight.copy(alpha = 0.85f),
        border = androidx.compose.foundation.BorderStroke(1.dp, NutriAmber.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Pemberitahuan",
                tint = NutriAmber,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = NutriSlate800,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun RiskBadge(riskCategory: String, modifier: Modifier = Modifier) {
    val (bgColor, textColor, icon) = when (riskCategory) {
        "Risiko Rendah" -> Triple(NutriGreenLight, NutriGreenDark, Icons.Default.CheckCircle)
        "Perlu Perhatian" -> Triple(NutriYellowLight, Color(0xFF854D0E), Icons.Default.Warning)
        else -> Triple(NutriRedLight, NutriRedRisk, Icons.Default.Error)
    }

    Surface(
        modifier = modifier.testTag("risk_badge_${riskCategory.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = riskCategory,
                color = textColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriTopAppBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    onLogout: (() -> Unit)? = null,
    onChatClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier.testTag("top_app_bar"),
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NutriGreenDark
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = NutriSlate600,
                        fontSize = 11.sp
                    )
                }
            }
        },
        navigationIcon = {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Kembali",
                        tint = NutriGreenDark
                    )
                }
            }
        },
        actions = {
            if (onChatClick != null) {
                IconButton(
                    onClick = onChatClick,
                    modifier = Modifier.testTag("chat_ai_top_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Tanya NutriMind AI",
                        tint = NutriAmber
                    )
                }
            }
            if (onLogout != null) {
                IconButton(
                    onClick = onLogout,
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Keluar",
                        tint = NutriSlate600
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.White
        )
    )
}

@Composable
fun NutriBottomNavigation(
    currentScreen: AppScreen,
    onScreenSelected: (AppScreen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
            .navigationBarsPadding()
            .testTag("bottom_nav_bar"),
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            NavigationItem(AppScreen.BERANDA, "Beranda", Icons.Filled.Home, Icons.Outlined.Home),
            NavigationItem(AppScreen.CEK_GIZI, "Cek Gizi", Icons.Filled.MonitorWeight, Icons.Outlined.MonitorWeight),
            NavigationItem(AppScreen.FOTO_MAKANAN, "Foto", Icons.Filled.CameraAlt, Icons.Outlined.CameraAlt),
            NavigationItem(AppScreen.CEK_HARIAN, "Harian", Icons.Filled.FactCheck, Icons.Outlined.FactCheck),
            NavigationItem(AppScreen.EDUKASI, "Edukasi", Icons.Filled.School, Icons.Outlined.School),
            NavigationItem(AppScreen.RIWAYAT, "Riwayat", Icons.Filled.History, Icons.Outlined.History),
            NavigationItem(AppScreen.PROFIL, "Profil", Icons.Filled.Person, Icons.Outlined.Person)
        )

        items.forEach { item ->
            val isSelected = currentScreen == item.screen
            NavigationBarItem(
                modifier = Modifier.testTag("nav_item_${item.label.lowercase()}"),
                selected = isSelected,
                onClick = { onScreenSelected(item.screen) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NutriGreenDark,
                    selectedTextColor = NutriGreenDark,
                    indicatorColor = NutriGreenLight,
                    unselectedIconColor = NutriSlate600,
                    unselectedTextColor = NutriSlate600
                )
            )
        }
    }
}

private data class NavigationItem(
    val screen: AppScreen,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)
