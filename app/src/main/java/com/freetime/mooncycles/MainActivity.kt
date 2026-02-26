package com.freetime.mooncycles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.freetime.mooncycles.ui.screens.MoonCyclesScreen
import com.freetime.mooncycles.ui.theme.MoonCyclesTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MoonCyclesTheme {
                MoonCyclesScreen()
            }
        }
    }
}