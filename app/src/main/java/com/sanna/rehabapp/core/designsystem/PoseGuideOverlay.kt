package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sanna.rehabapp.core.posedetection.EstadoGuia
import com.sanna.rehabapp.core.posedetection.GuideFrame
import com.sanna.rehabapp.core.posedetection.GuideUiState
import com.sanna.rehabapp.core.posedetection.LandmarkMapper
import com.sanna.rehabapp.core.posedetection.PuntoPose
import com.sanna.rehabapp.domain.model.Accion



private val RojoGuia = Color(0xFFFF5A5F)
private val VerdeExitoTexto = Color(0xFF2ECC71)
private val AmbarAlertaTexto = Color(0xFFFFB020)
private const val PASOS_ARCO = 28

// Guía visual en vivo (HU07/HU09): dibuja sobre la cámara el recorrido de la acción
// actual — arco punteado de inicio a meta, tramo ya recorrido, zona meta, segmento
// fantasma con su marcador y el segmento real del paciente coloreado según qué tan
// cerca está de la meta. Todo se calcula en píxeles de la imagen analizada y se
// mapea a la vista con el mismo recorte que PreviewView (FILL_CENTER).
@Composable
fun PoseGuideOverlay(estado: GuideUiState, modifier: Modifier = Modifier) {
    val guia = estado.guia
    val accion = estado.accion
    if (guia == null || accion == null || !estado.confiable) return
    Canvas(modifier = modifier.fillMaxSize()) {
        dibujarGuia(guia, accion)
    }
}

private fun DrawScope.dibujarGuia(guia: GuideFrame, accion: Accion) {
    val mapper = LandmarkMapper(guia.anchoImagen, guia.altoImagen, size.width, size.height)
    fun a(p: PuntoPose): Offset = mapper.mapear(p).let { Offset(it.x, it.y) }
    fun enArco(angulo: Float): Offset = a(guia.puntoEnArco(angulo))

    val grosor = (size.minDimension * 0.012f).coerceIn(4f, 10f)
    val colorEstado = when (guia.estado) {
        EstadoGuia.VERDE -> VerdeExitoTexto
        EstadoGuia.AMBAR -> AmbarAlertaTexto
        EstadoGuia.ROJO -> RojoGuia
    }
    val pivote = a(guia.pivote)

    // Zona meta: sector de ± tolerancia alrededor de la meta.
    val zona = Path().apply {
        moveTo(pivote.x, pivote.y)
        val desde = accion.anguloMeta - accion.tolerancia
        val hasta = accion.anguloMeta + accion.tolerancia
        for (i in 0..PASOS_ARCO) {
            val p = enArco(desde + (hasta - desde) * i / PASOS_ARCO)
            lineTo(p.x, p.y)
        }
        close()
    }
    drawPath(zona, VerdeExitoTexto.copy(alpha = 0.28f))

    // Recorrido completo (punteado) de inicio a meta.
    val recorrido = trazadoArco(accion.anguloInicio, accion.anguloMeta, ::enArco)
    drawPath(
        recorrido,
        Color.White.copy(alpha = 0.85f),
        style = Stroke(width = grosor * 0.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 12f))),
    )

    // Parte ya realizada: desde el inicio hasta el ángulo actual.
    val tramo = trazadoArco(accion.anguloInicio, guia.anguloActual, ::enArco)
    drawPath(tramo, colorEstado.copy(alpha = 0.9f), style = Stroke(width = grosor * 1.2f, cap = StrokeCap.Round))

    // Segmento fantasma hasta la meta, con marcador.
    val meta = a(guia.puntoMeta)
    drawLine(
        Color.White.copy(alpha = 0.9f), pivote, meta, strokeWidth = grosor * 0.7f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f)),
    )
    drawCircle(Color.White, radius = grosor * 1.8f, center = meta, style = Stroke(width = grosor * 0.7f))
    drawCircle(VerdeExitoTexto, radius = grosor * 0.9f, center = meta)

    // Segmento real del paciente (pasa por el punto intermedio si la acción lo define).
    val distal = a(guia.distal)
    val intermedio = guia.intermedio?.let { a(it) }
    if (intermedio != null) {
        drawLine(colorEstado, pivote, intermedio, strokeWidth = grosor * 1.6f, cap = StrokeCap.Round)
        drawLine(colorEstado, intermedio, distal, strokeWidth = grosor * 1.6f, cap = StrokeCap.Round)
    } else {
        drawLine(colorEstado, pivote, distal, strokeWidth = grosor * 1.6f, cap = StrokeCap.Round)
    }
    drawCircle(Color.White, radius = grosor * 1.3f, center = pivote)
    drawCircle(colorEstado, radius = grosor * 1.1f, center = distal)
}

private fun trazadoArco(desde: Float, hasta: Float, punto: (Float) -> Offset): Path = Path().apply {
    val inicio = punto(desde)
    moveTo(inicio.x, inicio.y)
    for (i in 1..PASOS_ARCO) {
        val p = punto(desde + (hasta - desde) * i / PASOS_ARCO)
        lineTo(p.x, p.y)
    }
}
