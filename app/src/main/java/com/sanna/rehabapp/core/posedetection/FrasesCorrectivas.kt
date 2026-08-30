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
        // HU20 (ampliación): ejercicios con lado afectado "Ambos" pensados
        // para alternar un brazo a la vez (ej. HOM-01) -- no es un error
        // de rango, es de coordinación, así que no menciona la articulación.
        "Movimiento simultáneo" -> "Levanta un brazo a la vez, no los dos juntos."
        else -> "Corrige $articulo $nombre."
    }
}

private fun articuloDe(articulacion: Articulacion): String = when (articulacion) {
    Articulacion.RODILLA_IZQUIERDA, Articulacion.RODILLA_DERECHA,
    Articulacion.CADERA_IZQUIERDA, Articulacion.CADERA_DERECHA,
    -> "la"
    Articulacion.CODO_IZQUIERDO, Articulacion.CODO_DERECHO,
    Articulacion.HOMBRO_IZQUIERDO, Articulacion.HOMBRO_DERECHO,
    Articulacion.CUELLO, Articulacion.TOBILLO_IZQUIERDO, Articulacion.TOBILLO_DERECHO,
    Articulacion.TRONCO,
    -> "el"
}
