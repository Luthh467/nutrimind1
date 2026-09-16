package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.ui.components.AiChatbotDialog
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppScreen
import com.example.viewmodel.NutriMindViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: NutriMindViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        NutriMindApp(viewModel = viewModel)
      }
    }
  }
}

@Composable
fun NutriMindApp(viewModel: NutriMindViewModel) {
  val context = LocalContext.current
  val currentScreen by viewModel.currentScreen.collectAsState()
  val toastMessage by viewModel.toastMessage.collectAsState()

  var showChatDialog by remember { mutableStateOf(false) }
  val chatMessages by viewModel.chatMessages.collectAsState()
  val isChatLoading by viewModel.isChatLoading.collectAsState()

  LaunchedEffect(toastMessage) {
    if (toastMessage != null) {
      Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
      viewModel.clearToast()
    }
  }

  Surface(
    modifier = Modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    when (currentScreen) {
      AppScreen.AUTH -> {
        AuthScreen(viewModel = viewModel)
      }
      AppScreen.COMPLETE_PROFILE -> {
        CompleteProfileScreen(viewModel = viewModel)
      }
      AppScreen.BERANDA -> {
        BerandaScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.CEK_GIZI -> {
        CekGiziScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.FOTO_MAKANAN -> {
        FotoMakananScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.CEK_HARIAN -> {
        CekHarianScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.EDUKASI -> {
        EdukasiScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.RIWAYAT -> {
        RiwayatScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.PROFIL -> {
        ProfilScreen(
          viewModel = viewModel,
          onOpenChat = { showChatDialog = true }
        )
      }
      AppScreen.UKS_DASHBOARD -> {
        UksDashboardScreen(viewModel = viewModel)
      }
    }

    if (showChatDialog) {
      AiChatbotDialog(
        messages = chatMessages,
        isLoading = isChatLoading,
        onSendMessage = { viewModel.sendChatMessage(it) },
        onDismiss = { showChatDialog = false }
      )
    }
  }
}

