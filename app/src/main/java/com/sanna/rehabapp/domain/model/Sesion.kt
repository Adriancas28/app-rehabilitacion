package com.sanna.rehabapp.domain.model

import java.util.Date

data class Sesion(
    val id: String = "",
    // HU18-CA01 (Sprint 5): solo se completa cuando la sesión viene de una
    // consulta agregada entre pacientes (collection group) — las consultas
    // de un paciente puntual ya lo conocen por fuera, no lo necesitan aquí.
    val pacienteId: String? = null,
    val ejercicioId: String,
    val fisioterapeutaId: String,
    val fechaAsignacion: Date? = null,
    val fechaEjecucion: Date? = null,
    val estado: EstadoSesion = EstadoSesion.PENDIENTE,
    // HU03-CA05: nota opcional del fisioterapeuta sobre esta sesión puntual.
    val notas: String? = null,
    // HU03-CA06: override opcional de las repeticiones del ejercicio, solo
    // para esta sesión. Si es null, se usa Ejercicio.repeticiones.
    val repeticiones: Int? = null,
    // HU03-CA06 (ampliacion): override opcional de la duracion por
    // repeticion (segundos), solo para esta sesion. Si es null, se usa
    // Ejercicio.duracionSegundos.
    val duracionSegundos: Int? = null,
    // HU03 (ampliación, Etapa 4): override opcional del ángulo objetivo
    // (min/max), solo para esta sesión/paciente -- ej. 120° en vez de 80°
    // para un paciente en primera sesión. Aplica sobre la PRIMERA
    // articulación de Ejercicio.patronesReferencia (simplificación: el
    // catálogo actual define un solo patrón por ejercicio); si cualquiera
    // de los dos es null, se usa el rango por defecto del ejercicio.
    val anguloMinOverride: Float? = null,
    val anguloMaxOverride: Float? = null,
    val resultado: ResultadoSesion? = null,
    // Parte 3 (video): URL en Firebase Storage del video grabado durante la
    // sesión, para que el fisioterapeuta lo revise. Null si no se grabó.
    val videoUrl: String? = null,
    val sincronizado: Boolean = true,
) {
    // ERR-FIS-006: finalizada antes de tiempo (menos repeticiones completadas
    // que asignadas, HU06-CA07). No cuenta como completada en la adherencia (HU14).
    val estaIncompleta: Boolean
        get() = resultado != null && resultado.repeticionesAsignadas > 0 &&
            resultado.repeticionesCompletadas < resultado.repeticionesAsignadas

    // Sesión realizada por completo: todas las repeticiones asignadas.
    val estaCompletada: Boolean
        get() = estado == EstadoSesion.COMPLETADA && !estaIncompleta
}
