package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.ReaderViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        val viewModel: ReaderViewModel = viewModel()
        val currentScreen by viewModel.currentScreen.collectAsState()

        Scaffold(
          modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            // Smooth beautiful transitions between library shelf and dark fullscreen reading canvas
            Crossfade(
              targetState = currentScreen,
              animationSpec = tween(durationMillis = 300),
              label = "ScreenTransition"
            ) { screen ->
              when (screen) {
                "LIBRARY" -> LibraryScreen(viewModel = viewModel)
                "READER_TXT" -> ReaderTxtScreen(viewModel = viewModel)
                "READER_PDF" -> ReaderPdfScreen(viewModel = viewModel)
                "READER_COMIC" -> ReaderComicScreen(viewModel = viewModel)
                "SETTINGS" -> SettingsScreen(viewModel = viewModel)
                "LOADING" -> LoadingScreen()
                else -> LibraryScreen(viewModel = viewModel)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun LoadingScreen() {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background),
    contentAlignment = Alignment.Center
  ) {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
  }
}

