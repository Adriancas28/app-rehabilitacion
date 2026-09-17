package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.VerdeExitoTexto

// Design System — gráficos simples dibujados a mano con Canvas (el proyecto
// no tiene ninguna librería de gráficos como Vico/MPAndroidChart como
// dependencia, y agregar una solo para 2-3 pantallas sería sobre-ingeniería).
// Ambos reciben valores 0-100 (porcentajes), no listas arbitrarias.

// Gráfico de barras — precisión por repetición (HU11 ampliación): una barra
// por repetición, verde si >= umbral, ámbar si no.
@Composable
fun GraficoBarras(
    valores: List<Float>,
    modifier: Modifier = Modifier,
    umbral: Float = 75f,
    colorSobreUmbral: Color = VerdeExitoTexto,
    colorBajoUmbral: Color = AmbarAlertaTexto,
) {
    val colorTrack = MaterialTheme.colorScheme.surfaceVariant
    Box(modifier = modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (valores.isEmpty()) return@Canvas
            val espacio = size.width * 0.02f
            val anchoBarra = (size.width - espacio * (valores.size - 1)) / valores.size
            valores.forEachIndexed { indice, valor ->
                val alturaTrack = size.height
                val alturaBarra = alturaTrack * (valor.coerceIn(0f, 100f) / 100f)
                val x = indice * (anchoBarra + espacio)
                // Track de fondo (0-100%), para que se note la escala.
                drawRoundRect(
                    color = colorTrack,
                    topLeft = Offset(x, 0f),
                    size = androidx.compose.ui.geometry.Size(anchoBarra, alturaTrack),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
                drawRoundRect(
                    color = if (valor >= umbral) colorSobreUmbral else colorBajoUmbral,
                    topLeft = Offset(x, alturaTrack - alturaBarra),
                    size = androidx.compose.ui.geometry.Size(anchoBarra, alturaBarra),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                )
            }
        }
    }
}

// Gráfico de línea con relleno — evolución de precisión entre sesiones
// (HU12 ampliación), en orden cronológico (más antigua primero).
@Composable
fun GraficoLinea(
    valores: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    Box(modifier = modifier.fillMaxWidth().height(120.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (valores.size < 2) return@Canvas
            val maxValor = 100f
            val pasoX = size.width / (valores.size - 1)
            val puntos = valores.mapIndexed { indice, valor ->
                Offset(
                    x = indice * pasoX,
                    y = size.height * (1f - (valor.coerceIn(0f, maxValor) / maxValor)),
                )
            }
            val lineaPath = androidx.compose.ui.graphics.Path().apply {
                moveTo(puntos.first().x, puntos.first().y)
                puntos.drop(1).forEach { lineTo(it.x, it.y) }
            }
            val relleno = androidx.compose.ui.graphics.Path().apply {
                addPath(lineaPath)
                lineTo(puntos.last().x, size.height)
                lineTo(puntos.first().x, size.height)
                close()
            }
            drawPath(relleno, color = color.copy(alpha = 0.12f), style = Fill)
            drawPath(lineaPath, color = color, style = Stroke(width = 2.5.dp.toPx()))
            puntos.forEach { punto -> drawCircle(color = color, radius = 3.5.dp.toPx(), center = punto) }
        }
    }
}

// Gráfico de dona — "Correctas vs errores (acumulado)" del dashboard de
// paciente en la vista Admin. Recibe los dos conteos crudos (no
// porcentajes) porque el hueco central no muestra cifra propia; la
// pantalla que lo usa arma la leyenda al lado con esos mismos valores.
@Composable
fun GraficoDona(
    correctas: Int,
    errores: Int,
    modifier: Modifier = Modifier,
    colorCorrectas: Color = VerdeExitoTexto,
    colorErrores: Color = com.sanna.rehabapp.core.theme.RojoErrorTexto,
    tamano: androidx.compose.ui.unit.Dp = 120.dp,
    grosor: androidx.compose.ui.unit.Dp = 16.dp,
) {
    val total = (correctas + errores).coerceAtLeast(1)
    Box(modifier = modifier.size(tamano)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val grosorPx = grosor.toPx()
            val diametro = size.minDimension - grosorPx
            val topLeft = Offset((size.width - diametro) / 2f, (size.height - diametro) / 2f)
            val arcSize = androidx.compose.ui.geometry.Size(diametro, diametro)
            val barridoCorrectas = 360f * correctas / total
            drawArc(
                color = colorErrores,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = grosorPx, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
            )
            if (correctas > 0) {
                drawArc(
                    color = colorCorrectas,
                    startAngle = -90f,
                    sweepAngle = barridoCorrectas,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = grosorPx, cap = androidx.compose.ui.graphics.StrokeCap.Butt),
                )
            }
        }
    }
}
