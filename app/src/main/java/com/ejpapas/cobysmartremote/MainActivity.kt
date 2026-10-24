package com.ejpapas.cobysmartremote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ejpapas.cobysmartremote.ui.CobyApp
import com.ejpapas.cobysmartremote.ui.CobyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CobyTheme { CobyApp() }
        }
    }
}
