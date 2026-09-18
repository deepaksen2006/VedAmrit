package com.example.vedaahar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.vedaahar.i18n.AppLanguageProvider
import com.example.vedaahar.ui.theme.VedAaharTheme
import com.example.vedaahar.voice.VoiceAssistantProvider

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AppLanguageProvider(context = this) {
                VoiceAssistantProvider {
                    VedAaharTheme {
                        VedaAhaarNavHost(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}
