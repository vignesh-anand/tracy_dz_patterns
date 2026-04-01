package com.tracydz.patterns

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tracydz.patterns.ui.screen.MapScreen
import com.tracydz.patterns.ui.theme.DZPatternsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DZPatternsTheme {
                MapScreen()
            }
        }
    }
}
