package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ui.MainEditorScreen
import com.example.ui.theme.CineCutTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory App Check configuration with intent as required by Firebase AI Logic
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            CineCutTheme {
                MainEditorScreen()
            }
        }
    }
}
