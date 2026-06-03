package com.example.vedaahar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.vedaahar.ui.theme.VedAaharTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VedAaharTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    VedaAhaarNavHost(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
