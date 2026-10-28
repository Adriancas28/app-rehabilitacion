package com.sanna.rehabapp.feature.ejercicios

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.LadoCuerpo
import com.sanna.rehabapp.domain.model.SentidoGiro
import java.util.UUID

// HU02-CA15: fila editable del formulario — una por acción de la repetición.
// Los números van como texto para poder editarlos libremente y validar al guardar.
data class AccionFila(
    val idFila: String = UUID.randomUUID().toString(),
    val nombre: String = "",
    val referencia: String = "",
    val pivote: String = "",
    val intermedio: String = "",
    val distal: String = "",
    val lado: LadoCuerpo = LadoCuerpo.DERECHO,
    val inicio: String = "",
    val meta: String = "",
    val tolerancia: String = "",
    val mantener: String = "0",
    val tiempoMax: String = "10",
    val sentido: SentidoGiro = SentidoGiro.AUTOMATICO,
)

private fun Float.sinCeroFinal(): String = if (this == toInt().toFloat()) toInt().toString() else toString()

fun Accion.aFila(): AccionFila = AccionFila(
    nombre = nombre,
    referencia = puntoReferencia.toString(),
    pivote = pivote.toString(),
    intermedio = puntoIntermedio?.toString().orEmpty(),
    distal = puntoDistal.toString(),
    lado = lado,
    inicio = anguloInicio.sinCeroFinal(),
    meta = anguloMeta.sinCeroFinal(),
    tolerancia = tolerancia.sinCeroFinal(),
    mantener = mantenerSeg.toString(),
    tiempoMax = tiempoMaxSeg.toString(),
    sentido = sentido,
)

// Rellena el segmento (referencia, pivote y distal) a partir de una articulación
// conocida, para no escribir los índices de landmarks a mano.
fun AccionFila.conSegmentoDe(articulacion: Articulacion): AccionFila {
    val roles = articulacion.roles()
    return copy(
        referencia = roles.referencia.toString(),
        pivote = articulacion.vertice.toString(),
        intermedio = "",
        distal = roles.distal.toString(),
        lado = articulacion.lado(),
        nombre = nombre.ifBlank { articulacion.etiqueta },
    )
}

// Convierte la fila a Accion; devuelve el mensaje de error si algún dato no es
// un número válido o si la acción incumple las reglas de HU02-CA18.
fun AccionFila.aAccion(orden: Int): Result<Accion> {
    fun entero(texto: String, nombreCampo: String): Int =
        texto.trim().toIntOrNull() ?: throw IllegalArgumentException("Acción $orden: ingresa un número válido en $nombreCampo.")
    fun decimal(texto: String, nombreCampo: String): Float =
        texto.trim().toFloatOrNull()?.takeIf { it.isFinite() }
            ?: throw IllegalArgumentException("Acción $orden: ingresa un número válido en $nombreCampo.")

    return runCatching {
        val accion = Accion(
            orden = orden,
            nombre = nombre.trim(),
            puntoReferencia = entero(referencia, "el punto de referencia"),
            pivote = entero(pivote, "el pivote"),
            puntoIntermedio = intermedio.trim().takeIf { it.isNotEmpty() }?.let { entero(it, "el punto intermedio") },
            puntoDistal = entero(distal, "el punto distal"),
            lado = lado,
            anguloInicio = decimal(inicio, "el ángulo de inicio"),
            anguloMeta = decimal(meta, "el ángulo meta"),
            tolerancia = decimal(tolerancia, "la tolerancia"),
            mantenerSeg = entero(mantener, "el mantenimiento"),
            tiempoMaxSeg = entero(tiempoMax, "el tiempo máximo"),
            sentido = sentido,
        )
        accion.errores().firstOrNull()?.let { throw IllegalArgumentException(it) }
        accion
    }
}
