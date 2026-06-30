package com.localai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.localai.presentation.chat.ChatScreen
import com.localai.presentation.model.ModelDownloaderScreen
import com.localai.presentation.theme.LocalAiTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LocalAiTheme {
                var selectedModelPath by remember { mutableStateOf<String?>(null) }

                if (selectedModelPath == null) {
                    ModelDownloaderScreen(
                        onNavigateToChat = { path -> selectedModelPath = path }
                    )
                } else {
                    ChatScreen(
                        modelPath = selectedModelPath!!,
                        onNavigateBack = { selectedModelPath = null }
                    )
                }
            }
        }
    }
}
