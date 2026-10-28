package com.sanna.rehabapp.feature.ejercicios

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sanna.rehabapp.core.designsystem.BotonOutline
import com.sanna.rehabapp.core.designsystem.CampoTexto
import com.sanna.rehabapp.core.designsystem.SeccionFormulario
import com.sanna.rehabapp.core.designsystem.SelectorDropdown
import com.sanna.rehabapp.core.designsystem.TarjetaBase
import com.sanna.rehabapp.core.theme.Spacing
import com.sanna.rehabapp.domain.model.Articulacion
import com.sanna.rehabapp.domain.model.LadoCuerpo
import com.sanna.rehabapp.domain.model.SentidoGiro
import com.sanna.rehabapp.domain.model.VistaCamara

// HU02-CA15 a CA17: define las acciones de cada repetición (en orden), el
// segmento corporal que mide cada una y la vista de cámara del ejercicio.
@Composable
fun SeccionAcciones(
    acciones: List<AccionFila>,
    vistaCamara: VistaCamara,
    onVistaCamaraCambiada: (VistaCamara) -> Unit,
    onAgregar: () -> Unit,
    onEliminar: (String) -> Unit,
    onMover: (String, Int) -> Unit,
    onCambiar: (String, (AccionFila) -> AccionFila) -> Unit,
) {
    SeccionFormulario(titulo = "Acciones de cada repetición (opcional)") {
        Text(
            text = "Cada repetición se compone de acciones que el paciente cumple en orden. Sin acciones, el ejercicio usa una acción generada a partir de su rango de referencia.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
        SelectorDropdown(
            valorSeleccionado = vistaCamara,
            opciones = VistaCamara.entries,
            etiquetaDeOpcion = { it.etiqueta },
            onSeleccionar = onVistaCamaraCambiada,
            etiqueta = "Vista de cámara requerida",
            placeholder = "Selecciona la vista",
        )
        acciones.forEachIndexed { indice, fila ->
            Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
            TarjetaAccion(
                numero = indice + 1,
                fila = fila,
                puedeSubir = indice > 0,
                puedeBajar = indice < acciones.lastIndex,
                onEliminar = { onEliminar(fila.idFila) },
                onMover = { delta -> onMover(fila.idFila, delta) },
                onCambiar = { transformar -> onCambiar(fila.idFila, transformar) },
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm + 4.dp))
        BotonOutline(texto = "Agregar acción", onClick = onAgregar, icono = Icons.Rounded.Add)
    }
}

@Composable
private fun TarjetaAccion(
    numero: Int,
    fila: AccionFila,
    puedeSubir: Boolean,
    puedeBajar: Boolean,
    onEliminar: () -> Unit,
    onMover: (Int) -> Unit,
    onCambiar: ((AccionFila) -> AccionFila) -> Unit,
) {
    TarjetaBase {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Acción $numero",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onMover(-1) }, enabled = puedeSubir) {
                Icon(Icons.Rounded.ArrowUpward, contentDescription = "Subir acción")
            }
            IconButton(onClick = { onMover(1) }, enabled = puedeBajar) {
                Icon(Icons.Rounded.ArrowDownward, contentDescription = "Bajar acción")
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    Icons.Rounded.RemoveCircleOutline,
                    contentDescription = "Quitar acción",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
        CampoTexto(
            valor = fila.nombre,
            onValorCambiado = { v -> onCambiar { it.copy(nombre = v) } },
            etiqueta = "Nombre de la acción",
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        SelectorDropdown(
            valorSeleccionado = null as Articulacion?,
            opciones = Articulacion.entries,
            etiquetaDeOpcion = { it.etiqueta },
            onSeleccionar = { articulacion -> onCambiar { it.conSegmentoDe(articulacion) } },
            etiqueta = "Rellenar segmento desde una articulación",
            placeholder = "Elegir articulación (opcional)",
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        FilaNumeros(
            "Referencia" to fila.referencia, "Pivote" to fila.pivote, "Distal" to fila.distal,
            onCambio = { campo, v ->
                onCambiar {
                    when (campo) {
                        0 -> it.copy(referencia = v)
                        1 -> it.copy(pivote = v)
                        else -> it.copy(distal = v)
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 4.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                CampoTexto(
                    valor = fila.intermedio,
                    onValorCambiado = { v -> onCambiar { it.copy(intermedio = v) } },
                    etiqueta = "Intermedio (opc.)",
                    tipoTeclado = KeyboardType.Number,
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                SelectorDropdown(
                    valorSeleccionado = fila.lado,
                    opciones = LadoCuerpo.entries,
                    etiquetaDeOpcion = { it.etiqueta },
                    onSeleccionar = { lado -> onCambiar { it.copy(lado = lado) } },
                    etiqueta = "Lado",
                    placeholder = "Lado",
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        FilaNumeros(
            "Inicio (°)" to fila.inicio, "Meta (°)" to fila.meta, "Tolerancia (°)" to fila.tolerancia,
            onCambio = { campo, v ->
                onCambiar {
                    when (campo) {
                        0 -> it.copy(inicio = v)
                        1 -> it.copy(meta = v)
                        else -> it.copy(tolerancia = v)
                    }
                }
            },
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Row(modifier = Modifier.fillMaxWidth()) {
            CampoTexto(
                valor = fila.mantener,
                onValorCambiado = { v -> onCambiar { it.copy(mantener = v) } },
                etiqueta = "Mantener (s)",
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(Spacing.sm + 4.dp))
            CampoTexto(
                valor = fila.tiempoMax,
                onValorCambiado = { v -> onCambiar { it.copy(tiempoMax = v) } },
                etiqueta = "Tiempo máx. (s)",
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        SelectorDropdown(
            valorSeleccionado = fila.sentido,
            opciones = SentidoGiro.entries,
            etiquetaDeOpcion = { it.nombreVisible() },
            onSeleccionar = { sentido -> onCambiar { it.copy(sentido = sentido) } },
            etiqueta = "Sentido de giro",
            placeholder = "Sentido",
        )
    }
}

private fun SentidoGiro.nombreVisible(): String = when (this) {
    SentidoGiro.AUTOMATICO -> "Automático"
    SentidoGiro.HORARIO -> "Horario"
    SentidoGiro.ANTIHORARIO -> "Antihorario"
}

@Composable
private fun FilaNumeros(
    a: Pair<String, String>,
    b: Pair<String, String>,
    c: Pair<String, String>,
    onCambio: (Int, String) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Spacing.sm + 4.dp)) {
        listOf(a, b, c).forEachIndexed { indice, (etiqueta, valor) ->
            CampoTexto(
                valor = valor,
                onValorCambiado = { v -> onCambio(indice, v) },
                etiqueta = etiqueta,
                tipoTeclado = KeyboardType.Number,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
