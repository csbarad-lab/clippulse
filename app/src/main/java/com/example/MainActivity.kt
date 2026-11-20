package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.firebase.configureAppCheck
import com.example.ui.AppNavigation
import com.example.ui.theme.ClipPulseTheme
import com.example.ui.theme.StudioObsidian

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch intent BEFORE any UI or ViewModels.
        configureAppCheck(this, intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )

        setContent {
            ClipPulseTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = StudioObsidian
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
