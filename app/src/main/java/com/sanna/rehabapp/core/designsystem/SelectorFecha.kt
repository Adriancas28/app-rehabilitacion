package com.sanna.rehabapp.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Date

// Design System — botón que abre un selector de fecha (Material3
// DatePickerDialog). Toda pantalla que necesite elegir una fecha usa este
// componente en vez de manejar rememberDatePickerState/DatePickerDialog a
// mano, para no repetir la conversión de zona horaria en cada pantalla.
@Composable
fun BotonSelectorFecha(
    fecha: Date?,
    onFechaSeleccionada: (Date) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Seleccionar fecha",
    formatear: (Date) -> String,
    habilitado: Boolean = true,
) {
    var abierto by remember { mutableStateOf(false) }

    BotonOutline(
        texto = fecha?.let(formatear) ?: placeholder,
        onClick = { abierto = true },
        icono = Icons.Filled.CalendarMonth,
        habilitado = habilitado,
        modifier = modifier,
    )

    if (abierto) {
        DialogoSelectorFecha(
            fechaInicial = fecha ?: Date(),
            onFechaSeleccionada = {
                onFechaSeleccionada(it)
                abierto = false
            },
            onDescartar = { abierto = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoSelectorFecha(
    fechaInicial: Date,
    onFechaSeleccionada: (Date) -> Unit,
    onDescartar: () -> Unit,
) {
    val estadoDatePicker = rememberDatePickerState(
        initialSelectedDateMillis = diaLocalAUtcMillis(fechaInicial),
    )
    DatePickerDialog(
        onDismissRequest = onDescartar,
        confirmButton = {
            TextButton(onClick = {
                estadoDatePicker.selectedDateMillis?.let { onFechaSeleccionada(utcMillisADiaLocal(it)) }
            }) { Text("Aceptar") }
        },
        dismissButton = {
            TextButton(onClick = onDescartar) { Text("Cancelar") }
        },
    ) {
        DatePicker(state = estadoDatePicker)
    }
}

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
