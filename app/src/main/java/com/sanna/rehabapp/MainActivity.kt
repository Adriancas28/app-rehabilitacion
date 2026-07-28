package com.sanna.rehabapp

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sanna.rehabapp.core.navigation.RehabNavHost
import com.sanna.rehabapp.core.theme.AppRehabilitacionTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Animación de salida propia: el logo crece un poco y se desvanece,
        // en vez del corte seco por defecto de la API de splash screen.
        splashScreen.setOnExitAnimationListener { vistaSplash ->
            val icono = vistaSplash.iconView
            val escalaX = ObjectAnimator.ofFloat(icono, View.SCALE_X, 1f, 1.15f)
            val escalaY = ObjectAnimator.ofFloat(icono, View.SCALE_Y, 1f, 1.15f)
            val opacidad = ObjectAnimator.ofFloat(icono, View.ALPHA, 1f, 0f)

            AnimatorSet().apply {
                playTogether(escalaX, escalaY, opacidad)
                duration = 350
                interpolator = AccelerateInterpolator()
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        vistaSplash.remove()
                    }
                })
                start()
            }
        }

        setContent {
            AppRehabilitacionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RehabNavHost()
                }
            }
        }
    }
}
