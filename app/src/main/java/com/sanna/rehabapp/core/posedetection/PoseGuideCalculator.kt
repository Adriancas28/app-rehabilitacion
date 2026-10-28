package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.SentidoGiro
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

// Guía visual en vivo, etapa 3: motor de cálculo GENÉRICO y puro (sin Android).
// Dada la acción actual y un cuadro de pose, calcula todo lo que la guía y la
// máquina de estados necesitan, sin asumir ninguna parte del cuerpo: el
// segmento se define por los landmarks de la propia acción.

enum class EstadoGuia { VERDE, AMBAR, ROJO }

// Umbrales del color de estado. Verde: dentro de la tolerancia. Ámbar: dentro de
// `factorAmbar` veces la tolerancia. Rojo: más lejos.
data class UmbralesGuia(val factorAmbar: Float = 3f)

data class GuideFrame(
    // Ángulo actual (0°–180°) entre el segmento pivote→distal y la línea pivote→referencia.
    val anguloActual: Float,
    // Largo del segmento a dibujar, en píxeles de la imagen analizada.
    val longitudSegmento: Float,
    // 0..1 entre anguloInicio y anguloMeta (en ambos sentidos).
    val progreso: Float,
    // |anguloActual − anguloMeta| ≤ tolerancia.
    val enZona: Boolean,
    val estado: EstadoGuia,
    // +1 si el segmento gira en sentido horario en pantalla al aumentar el ángulo, −1 si antihorario.
    val signoGiro: Int,
    // Landmarks de la acción, normalizados sobre la imagen analizada.
    val pivote: PuntoPose,
    val distal: PuntoPose,
    val intermedio: PuntoPose?,
    // Dónde queda el extremo del segmento al alcanzar la meta (segmento fantasma).
    val puntoMeta: PuntoPose,
    // Dirección de la línea pivote→referencia en radianes, en píxeles de imagen (y hacia abajo).
    val direccionReferencia: Float,
    val anchoImagen: Int,
    val altoImagen: Int,
) {
    // Punto (normalizado) sobre el arco de radio `longitudSegmento` centrado en el
    // pivote, a `anguloGrados` de la línea de referencia. Sirve para dibujar el
    // recorrido, la zona meta y el segmento fantasma.
    fun puntoEnArco(anguloGrados: Float, radio: Float = longitudSegmento): PuntoPose {
        val radianes = direccionReferencia + signoGiro * Math.toRadians(anguloGrados.toDouble()).toFloat()
        val x = pivote.x * anchoImagen + radio * cos(radianes)
        val y = pivote.y * altoImagen + radio * sin(radianes)
        return PuntoPose(x / anchoImagen, y / altoImagen)
    }
}

object PoseGuideCalculator {

    private const val ANGULO_MINIMO_PARA_DEDUCIR_SIGNO = 5f

    // Devuelve null si el cuadro no es confiable para esta acción (algún landmark
    // requerido no está visible): quien llama no debe avanzar ni penalizar.
    // `signoAnterior` conserva el sentido ya deducido cuando el segmento está
    // sobre la línea de referencia y no se puede deducir de este cuadro.
    fun calcular(
        frame: LandmarksFrame,
        accion: Accion,
        signoAnterior: Int? = null,
        umbrales: UmbralesGuia = UmbralesGuia(),
    ): GuideFrame? {
        if (!frame.esConfiable(listOf(accion.puntoReferencia, accion.pivote, accion.puntoDistal))) return null
        val referencia = frame[accion.puntoReferencia] ?: return null
        val pivote = frame[accion.pivote] ?: return null
        val distal = frame[accion.puntoDistal] ?: return null
        val intermedio = accion.puntoIntermedio?.let { frame[it] }?.takeIf { it.visibilidad >= VISIBILIDAD_MINIMA_CONFIABLE }

        val ancho = frame.anchoImagen.toFloat()
        val alto = frame.altoImagen.toFloat()
        // Todo el cálculo geométrico se hace en píxeles de la imagen, no en
        // coordenadas normalizadas: de lo contrario los ángulos se deforman.
        val ux = (referencia.x - pivote.x) * ancho
        val uy = (referencia.y - pivote.y) * alto
        val vx = (distal.x - pivote.x) * ancho
        val vy = (distal.y - pivote.y) * alto
        val magnitudU = hypot(ux, uy)
        val magnitudV = hypot(vx, vy)
        if (magnitudU == 0f || magnitudV == 0f) return null

        val angulo = Math.toDegrees(abs(atan2(ux * vy - uy * vx, ux * vx + uy * vy)).toDouble()).toFloat()
        val longitud = if (intermedio != null) {
            hypot((intermedio.x - pivote.x) * ancho, (intermedio.y - pivote.y) * alto) +
                hypot((distal.x - intermedio.x) * ancho, (distal.y - intermedio.y) * alto)
        } else {
            magnitudV
        }

        val signo = resolverSigno(accion.sentido, ux * vy - uy * vx, angulo, signoAnterior)
        val direccionReferencia = atan2(uy, ux)

        val distancia = abs(angulo - accion.anguloMeta)
        val enZona = distancia <= accion.tolerancia
        val estado = when {
            enZona -> EstadoGuia.VERDE
            distancia <= accion.tolerancia * umbrales.factorAmbar -> EstadoGuia.AMBAR
            else -> EstadoGuia.ROJO
        }
        val recorrido = accion.anguloMeta - accion.anguloInicio
        val progreso = if (recorrido == 0f) {
            if (enZona) 1f else 0f
        } else {
            ((angulo - accion.anguloInicio) / recorrido).coerceIn(0f, 1f)
        }

        val parcial = GuideFrame(
            anguloActual = angulo,
            longitudSegmento = longitud,
            progreso = progreso,
            enZona = enZona,
            estado = estado,
            signoGiro = signo,
            pivote = pivote,
            distal = distal,
            intermedio = intermedio,
            puntoMeta = pivote,
            direccionReferencia = direccionReferencia,
            anchoImagen = frame.anchoImagen,
            altoImagen = frame.altoImagen,
        )
        return parcial.copy(puntoMeta = parcial.puntoEnArco(accion.anguloMeta))
    }

    // En coordenadas de imagen (y hacia abajo), una rotación positiva se ve horaria.
    private fun resolverSigno(sentido: SentidoGiro, cruz: Float, angulo: Float, anterior: Int?): Int = when (sentido) {
        SentidoGiro.HORARIO -> 1
        SentidoGiro.ANTIHORARIO -> -1
        SentidoGiro.AUTOMATICO -> when {
            angulo > ANGULO_MINIMO_PARA_DEDUCIR_SIGNO && angulo < 180f - ANGULO_MINIMO_PARA_DEDUCIR_SIGNO ->
                if (cruz >= 0f) 1 else -1
            else -> anterior ?: 1
        }
    }
}
