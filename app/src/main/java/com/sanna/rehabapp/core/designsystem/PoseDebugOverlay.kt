package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.sanna.rehabapp.core.posedetection.CONEXIONES_ESQUELETO
import com.sanna.rehabapp.core.posedetection.LandmarkMapper
import com.sanna.rehabapp.core.posedetection.LandmarksFrame
import com.sanna.rehabapp.core.posedetection.VISIBILIDAD_MINIMA_CONFIABLE

// Guía visual en vivo, etapa 2 — modo de depuración: dibuja los 33 landmarks y el
// esqueleto sobre la cámara para comprobar que el mapeo de coordenadas coincide
// con el cuerpo real. Verde = visible, rojo = visibilidad baja. Se superpone al
// PreviewView con el mismo recorte (FILL_CENTER) que usa por defecto.
@Composable
fun PoseDebugOverlay(frame: LandmarksFrame?, modifier: Modifier = Modifier) {
    if (frame == null) return
    Canvas(modifier = modifier.fillMaxSize()) {
        val mapper = LandmarkMapper(frame.anchoImagen, frame.altoImagen, size.width, size.height)
        val puntos = frame.puntos.map { mapper.mapear(it) }
        CONEXIONES_ESQUELETO.forEach { (a, b) ->
            val pa = puntos.getOrNull(a) ?: return@forEach
            val pb = puntos.getOrNull(b) ?: return@forEach
            drawLine(Color.White.copy(alpha = 0.8f), Offset(pa.x, pa.y), Offset(pb.x, pb.y), strokeWidth = 3f)
        }
        puntos.forEachIndexed { indice, p ->
            val visible = frame.puntos[indice].visibilidad >= VISIBILIDAD_MINIMA_CONFIABLE
            drawCircle(if (visible) Color(0xFF2ECC71) else Color(0xFFE74C3C), radius = 6f, center = Offset(p.x, p.y))
        }
    }
}
