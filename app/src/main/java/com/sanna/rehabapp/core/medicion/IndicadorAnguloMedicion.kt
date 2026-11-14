package com.sanna.rehabapp.core.medicion

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sanna.rehabapp.BuildConfig
import com.sanna.rehabapp.core.posedetection.GuideFrame
import java.util.Locale

private const val VENTANA_PROMEDIO_MS = 2_000L

// Solo para la medición del error angular (EDT 1.8.6.1): muestra el ángulo que calcula la app
// (el mismo de la guía) y su promedio de los últimos 2 segundos, para anotarlo mientras se
// mantiene la postura. Si la compilación no activa la medición, no dibuja nada.
@Composable
fun IndicadorAnguloMedicion(guia: GuideFrame?, modifier: Modifier = Modifier) {
    if (!BuildConfig.MEDICION_ACTIVA) return
    val muestras = remember { ArrayDeque<Pair<Long, Float>>() }
    var texto by remember { mutableStateOf("Ángulo: sin pose") }
    LaunchedEffect(guia) {
        val ahora = SystemClock.elapsedRealtime()
        if (guia == null) {
            muestras.clear()
            texto = "Ángulo: sin pose"
        } else {
            muestras.addLast(ahora to guia.anguloActual)
            while (muestras.isNotEmpty() && ahora - muestras.first().first > VENTANA_PROMEDIO_MS) muestras.removeFirst()
            val promedio = muestras.map { it.second }.average()
            texto = String.format(
                Locale.getDefault(), "Ángulo: %.1f°   Prom. 2 s: %.1f°   (n=%d)",
                guia.anguloActual, promedio, muestras.size,
            )
        }
    }
    Text(
        text = texto,
        color = Color.White,
        fontSize = 15.sp,
        modifier = modifier
            .background(Color(0xCC000000))
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
