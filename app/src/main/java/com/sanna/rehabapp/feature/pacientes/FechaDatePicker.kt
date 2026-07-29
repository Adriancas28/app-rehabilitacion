package com.sanna.rehabapp.feature.pacientes

import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

// El DatePicker de Material3 (selectedDateMillis / initialSelectedDateMillis)
// siempre trabaja en UTC. Si se usa ese valor tal cual para construir un
// java.util.Date, cualquier zona horaria detrás de UTC (ej. Perú, UTC-5)
// lo interpreta como el día anterior al mostrarlo/guardarlo con la zona
// local del dispositivo. Estas funciones convierten entre ambos mundos
// conservando el día calendario que el usuario realmente seleccionó.

// Del picker (UTC) a un Date que representa la medianoche LOCAL de ese
// mismo día calendario — es lo que hay que guardar/mostrar.
fun utcMillisADiaLocal(utcMillis: Long): Date {
    val diaUtc = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return Date.from(diaUtc.atStartOfDay(ZoneId.systemDefault()).toInstant())
}

// De un Date ya guardado (medianoche local) a los millis en UTC que el
// picker necesita para preseleccionar ese mismo día calendario al editar.
fun diaLocalAUtcMillis(fecha: Date): Long {
    val diaLocal = fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    return diaLocal.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}
