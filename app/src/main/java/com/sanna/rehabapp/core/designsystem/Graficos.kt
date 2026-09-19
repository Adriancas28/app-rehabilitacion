package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.theme.AmbarAlertaTexto
import com.sanna.rehabapp.core.theme.VerdeExitoTexto

// Design System — gráficos simples dibujados a mano (el proyecto no tiene
// ninguna librería de gráficos como Vico/MPAndroidChart como dependencia,
// y agregar una solo para 2-3 pantallas sería sobre-ingeniería). Ambos
// reciben valores 0-100 (porcentajes), no listas arbitrarias.
// `etiquetas` (eje X, ej. "Sesión 1") es opcional -- si no se pasa, el
// gráfico se ve igual que antes de esta ampliación (pantallas existentes
// como HistorialSesionesScreen/ResultadoSesionScreen no la usan).

// Gráfico de barras — precisión por repetición (HU11 ampliación) o
// dashboard de paciente en la vista Admin (Etapa 2A): una barra por valor,
// verde si >= umbral, ámbar si no, con su porcentaje encima y la etiqueta
// del eje X debajo.
@Composable
fun GraficoBarras(
    valores: List<Float>,
    modifier: Modifier = Modifier,
    umbral: Float = 75f,
    colorSobreUmbral: Color = VerdeExitoTexto,
    colorBajoUmbral: Color = AmbarAlertaTexto,
    etiquetas: List<String> = emptyList(),
    // Con muchas barras (ej. 12 repeticiones) el porcentaje sobre cada una no
    // cabe: se oculta y se usa el eje Y (0/25/50/75/100) en su lugar.
    mostrarValores: Boolean = true,
    mostrarEjeY: Boolean = false,
) {
    val colorTrack = MaterialTheme.colorScheme.surfaceVariant
    val anchoEjeY = if (mostrarEjeY) 34.dp else 0.dp
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(if (mostrarValores) 110.dp else 130.dp)) {
            if (mostrarEjeY) {
                Column(
                    modifier = Modifier.width(anchoEjeY).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(100, 75, 50, 25, 0).forEach { valor ->
                        Text(
                            text = "$valor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalArrangement = Arrangement.spacedBy(if (valores.size > 8) 4.dp else 6.dp),
            ) {
                valores.forEach { valor ->
                    val color = if (valor >= umbral) colorSobreUmbral else colorBajoUmbral
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (mostrarValores) {
                            Text(
                                text = "${valor.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = color,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f).background(colorTrack, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(valor.coerceIn(0f, 100f) / 100f)
                                    .background(color, RoundedCornerShape(4.dp)),
                            )
                        }
                    }
                }
            }
        }
        if (etiquetas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = anchoEjeY),
                horizontalArrangement = Arrangement.spacedBy(if (valores.size > 8) 4.dp else 6.dp),
            ) {
                etiquetas.forEach { etiqueta ->
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

// Gráfico de línea con relleno — evolución de precisión entre sesiones
// (HU12 ampliación) o dashboard de paciente en la vista Admin (Etapa 2A),
// en orden cronológico (más antigua primero). Con `etiquetas` dibuja el
// eje Y (0/25/50/75/100) a la izquierda y el eje X debajo.
@Composable
fun GraficoLinea(
    valores: List<Float>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    etiquetas: List<String> = emptyList(),
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            if (etiquetas.isNotEmpty()) {
                Column(
                    modifier = Modifier.width(28.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    listOf(100, 75, 50, 25, 0).forEach { valor ->
                        Text(
                            text = "$valor",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    if (valores.isEmpty()) return@Canvas
                    val maxValor = 100f
                    // Cada punto en el centro de su "casilla" (igual que las
                    // etiquetas del eje X, que reparten el ancho en partes iguales).
                    val casilla = size.width / valores.size
                    val puntos = valores.mapIndexed { indice, valor ->
                        Offset(
                            x = (indice + 0.5f) * casilla,
                            y = size.height * (1f - (valor.coerceIn(0f, maxValor) / maxValor)),
                        )
                    }
                    if (puntos.size > 1) {
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
                    }
                    puntos.forEach { punto -> drawCircle(color = color, radius = 4.5.dp.toPx(), center = punto) }
                }
            }
        }
        if (etiquetas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(start = 34.dp)) {
                etiquetas.forEach { etiqueta ->
                    Text(
                        text = etiqueta,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
