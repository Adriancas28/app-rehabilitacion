package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Articulacion

// HU10-CA06: frase plantilla para la corrección por voz, mapeada por tipo
// de error + articulación — nunca texto generado dinámicamente (RNF01/
// RNF06: nada sale del dispositivo, ni siquiera hay red de por medio).
// Pura y testeable, sin dependencias de Android/MediaPipe.
fun fraseCorrectiva(medicion: MedicionArticulacion): String {
    val articulo = articuloDe(medicion.articulacion)
    val nombre = medicion.articulacion.etiqueta.lowercase()
    return when (medicion.tipoDeError) {
        "Rango incompleto" -> "Flexiona más $articulo $nombre."
        "Desviación angular" -> "No fuerces tanto $articulo $nombre."
        else -> "Corrige $articulo $nombre."
    }
}

private fun articuloDe(articulacion: Articulacion): String = when (articulacion) {
    Articulacion.RODILLA_IZQUIERDA, Articulacion.RODILLA_DERECHA,
    Articulacion.CADERA_IZQUIERDA, Articulacion.CADERA_DERECHA,
    -> "la"
    Articulacion.CODO_IZQUIERDO, Articulacion.CODO_DERECHO,
    Articulacion.HOMBRO_IZQUIERDO, Articulacion.HOMBRO_DERECHO,
    -> "el"
}
