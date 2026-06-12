package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.screen.collectAsState()

                BackHandler(enabled = currentScreen != AppScreen.SPLASH && currentScreen != AppScreen.HOME) {
                    viewModel.navigateBack()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            fadeIn(tween(250)) togetherWith fadeOut(tween(250))
                        },
                        label = "nav"
                    ) { screen ->
                        when (screen) {
                            AppScreen.SPLASH      -> SplashScreen()
                            AppScreen.HOME        -> HomeScreen(viewModel)
                            AppScreen.UPLOAD      -> UploadScreen(viewModel)
                            AppScreen.LIVE_RECORD -> LiveRecordScreen(viewModel)
                            AppScreen.PROCESSING  -> ProcessingScreen(viewModel)
                            AppScreen.RESULT      -> ResultScreen(viewModel)
                            AppScreen.SETTINGS    -> SettingsScreen(viewModel)
                            AppScreen.NOTES       -> NotesScreen(viewModel)
                        }
                    }
                }
            }
        }
    }
}
