package com.sanna.rehabapp.feature.paciente

import java.util.Calendar
import java.util.Date

private const val MILISEGUNDOS_POR_DIA = 24 * 60 * 60 * 1000L

// HU12-CA01: cuántos días consecutivos (hasta hoy o ayer) el paciente
// completó al menos una sesión. Pura y testeable — sin dependencias de
// Firestore/Android más allá de java.util.Date/Calendar.
fun calcularRacha(fechasDeEjecucion: List<Date>, ahora: Date = Date()): Int {
    if (fechasDeEjecucion.isEmpty()) return 0

    val dias = fechasDeEjecucion.map(::inicioDelDia).distinct().sortedDescending()
    val hoy = inicioDelDia(ahora)

    // Si la sesión más reciente no fue hoy ni ayer, la racha ya se rompió.
    if (dias.first() != hoy && dias.first() != hoy - MILISEGUNDOS_POR_DIA) return 0

    var racha = 1
    for (i in 1 until dias.size) {
        if (dias[i] == dias[i - 1] - MILISEGUNDOS_POR_DIA) {
            racha++
        } else {
            break
        }
    }
    return racha
}

private fun inicioDelDia(fecha: Date): Long {
    val calendario = Calendar.getInstance()
    calendario.time = fecha
    calendario.set(Calendar.HOUR_OF_DAY, 0)
    calendario.set(Calendar.MINUTE, 0)
    calendario.set(Calendar.SECOND, 0)
    calendario.set(Calendar.MILLISECOND, 0)
    return calendario.timeInMillis
}
