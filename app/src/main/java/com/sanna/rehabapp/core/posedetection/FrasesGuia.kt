package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.VistaCamara

// Guía visual en vivo (HU10): frases genéricas para voz y pantalla, armadas solo con
// datos de la acción y del ejercicio (nombre de la acción, vista de cámara); no
// hay frases por articulación. Siguen siendo plantillas fijas (RNF01/RNF06).
fun fraseDeGuia(mensaje: MensajeGuia, accion: Accion?, vista: VistaCamara): String = when (mensaje) {
    MensajeGuia.REUBICARSE -> "Colócate ${posicionDeVista(vista)} a la cámara."
    MensajeGuia.AVANZAR_MAS -> accion?.nombre?.let { "Sigue: ${it.replaceFirstChar { c -> c.lowercase() }}." } ?: "Sigue el recorrido."
    MensajeGuia.REGRESAR_UN_POCO -> "Te pasaste, regresa un poco."
    MensajeGuia.MANTENER -> "Mantén la posición."
    MensajeGuia.ACCION_CUMPLIDA -> "Muy bien."
}

private fun posicionDeVista(vista: VistaCamara): String = when (vista) {
    VistaCamara.FRONTAL -> "de frente"
    VistaCamara.LATERAL -> "de lado"
    VistaCamara.OBLICUA -> "en diagonal"
}
