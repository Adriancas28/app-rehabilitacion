package com.sanna.rehabapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.sanna.rehabapp.core.navigation.RehabNavHost
import com.sanna.rehabapp.core.theme.AppRehabilitacionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppRehabilitacionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RehabNavHost()
                }
            }
        }
    }
}
