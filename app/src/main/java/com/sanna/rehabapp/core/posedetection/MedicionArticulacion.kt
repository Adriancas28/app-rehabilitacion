package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.AnguloDetectado
import com.sanna.rehabapp.domain.model.DetalleRepeticion
import com.sanna.rehabapp.domain.model.ErrorDetectado
import com.sanna.rehabapp.domain.model.ResultadoSesion

// HU08-CA01/CA02: una medición puntual (de un frame) de una articulación,
// ya comparada contra su ROM esperado. Sin dependencias de MediaPipe —
// testeable con JUnit normal.
data class MedicionArticulacion(
    val articulacion: Articulacion,
    val angulo: Float,
    val anguloMin: Float,
    val anguloMax: Float,
    // HU20 (ampliación, ejercicios bilaterales con lado afectado "Ambos"):
    // cuando no es null, fuerza que esta medición cuente como fuera de
    // rango con este motivo específico (ej. "Movimiento simultáneo" si el
    // paciente levanta los dos brazos a la vez en un ejercicio pensado
    // para alternar) — no lo produce la comparación ángulo/rango normal,
    // lo decide ProcesadorMovimiento al detectar el patrón de movimiento.
    val motivoErrorForzado: String? = null,
) {
    val dentroDeRango: Boolean get() = motivoErrorForzado == null && angulo in anguloMin..anguloMax

    val desviacion: Float
        get() = when {
            motivoErrorForzado != null -> 0f
            angulo < anguloMin -> anguloMin - angulo
            angulo > anguloMax -> angulo - anguloMax
            else -> 0f
        }

    // HU09-CA02: clasifica el tipo de error (ya satisface la CA tal cual
    // está redactada — "rango incompleto, desviación angular, etc.").
    val tipoDeError: String
        get() = motivoErrorForzado ?: when {
            angulo < anguloMin -> "Rango incompleto"
            angulo > anguloMax -> "Desviación angular"
            else -> "Desviación"
        }
}

// HU08-CA04: al finalizar la sesión, agrega todas las mediciones (agrupadas
// por repetición, y dentro de cada una por frame procesado) en el resumen
// que se guarda como ResultadoSesion.
fun construirResultadoSesion(
    medicionesPorRepeticion: List<List<List<MedicionArticulacion>>>,
    repeticionesCompletadas: Int,
    repeticionesAsignadas: Int,
    numeroRepeticionInicial: Int = 1,
    // Segundo (desde el inicio de su repetición) en que se midió cada frame;
    // paralelo a medicionesPorRepeticion. Null si no se registró.
    segundosPorFrame: List<List<Int>>? = null,
): ResultadoSesion {
    val medicionesPorFrame = medicionesPorRepeticion.flatten()
    val todasLasMediciones = medicionesPorFrame.flatten()
    if (todasLasMediciones.isEmpty()) {
        return ResultadoSesion(
            repeticionesCompletadas = repeticionesCompletadas,
            repeticionesAsignadas = repeticionesAsignadas,
        )
    }

    val angulosDetectados = todasLasMediciones
        .groupBy { it.articulacion }
        .map { (articulacion, mediciones) ->
            AnguloDetectado(
                articulacion = articulacion.etiqueta,
                anguloDetectado = mediciones.map { it.angulo }.average().toFloat(),
                anguloEsperado = (mediciones.first().anguloMin + mediciones.first().anguloMax) / 2f,
                desviacion = mediciones.map { it.desviacion }.average().toFloat(),
            )
        }

    val erroresDetectados = todasLasMediciones
        .filterNot { it.dentroDeRango }
        .groupBy { it.articulacion to it.tipoDeError }
        .map { (clave, mediciones) ->
            ErrorDetectado(
                articulacion = clave.first.etiqueta,
                tipo = clave.second,
                repeticiones = mediciones.size,
            )
        }

    val framesDentroDeRango = medicionesPorFrame.count { frame -> frame.isNotEmpty() && frame.all { it.dentroDeRango } }
    val porcentajeEjecucion = framesDentroDeRango.toFloat() / medicionesPorFrame.size * 100f
    val desviacionPromedio = todasLasMediciones.map { it.desviacion }.average().toFloat()

    // HU11: una repetición cuenta como "correcta" si tuvo frames medidos y
    // todos ellos estuvieron dentro de rango en todas las articulaciones.
    val repeticionesCorrectas = medicionesPorRepeticion.count { frames ->
        frames.isNotEmpty() && frames.all { frame -> frame.isNotEmpty() && frame.all { it.dentroDeRango } }
    }

    // HU18-CA04 (actualización del modelo de datos): el mismo agrupamiento
    // de erroresDetectados, pero acotado a cada repetición en vez de
    // global — lo que ve el fisioterapeuta al revisar el detalle de la
    // sesión. porcentajeEjecucion se calcula igual que el % global de la
    // sesión (framesDentroDeRango / totalFrames), pero acotado a los
    // frames de esta repetición puntual — no un simple sí/no.
    val detallePorRepeticion = medicionesPorRepeticion.mapIndexed { indice, frames ->
        // Un error por (articulación, tipo) en esta repetición, descrito por su
        // peor instante: el segundo y el ángulo medidos ahí, contra el límite
        // del rango que se incumplió (ej. "Segundo 15 — 120° (esperado 90°)").
        val erroresRepeticion = frames
            .flatMapIndexed { i, frame ->
                frame.filterNot { it.dentroDeRango }.map { medicion ->
                    medicion to segundosPorFrame?.getOrNull(indice)?.getOrNull(i)
                }
            }
            .groupBy { (medicion, _) -> medicion.articulacion to medicion.tipoDeError }
            .map { (clave, pares) ->
                val (peor, segundo) = pares.maxBy { (medicion, _) -> medicion.desviacion }
                ErrorDetectado(
                    articulacion = clave.first.etiqueta,
                    tipo = clave.second,
                    repeticiones = pares.size,
                    anguloDetectado = peor.angulo,
                    anguloEsperado = when {
                        peor.motivoErrorForzado != null -> (peor.anguloMin + peor.anguloMax) / 2f
                        peor.angulo < peor.anguloMin -> peor.anguloMin
                        else -> peor.anguloMax
                    },
                    segundo = segundo,
                )
            }
        val framesRepeticionDentroDeRango = frames.count { frame -> frame.isNotEmpty() && frame.all { it.dentroDeRango } }
        val porcentajeRepeticion = if (frames.isNotEmpty()) {
            framesRepeticionDentroDeRango.toFloat() / frames.size * 100f
        } else {
            0f
        }
        DetalleRepeticion(
            // HU06-CA09: numeroRepeticionInicial > 1 cuando se reanuda una
            // sesión — la numeración refleja la repetición real, no el
            // índice dentro de esta ejecución puntual.
            numero = indice + numeroRepeticionInicial,
            porcentajeEjecucion = porcentajeRepeticion,
            errores = erroresRepeticion,
        )
    }

    return ResultadoSesion(
        angulosDetectados = angulosDetectados,
        desviacionPromedio = desviacionPromedio,
        porcentajeEjecucion = porcentajeEjecucion,
        erroresDetectados = erroresDetectados,
        repeticionesCompletadas = repeticionesCompletadas,
        repeticionesAsignadas = repeticionesAsignadas,
        repeticionesCorrectas = repeticionesCorrectas,
        detallePorRepeticion = detallePorRepeticion,
    )
}
