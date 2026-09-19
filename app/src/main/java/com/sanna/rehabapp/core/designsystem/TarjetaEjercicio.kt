package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sanna.rehabapp.core.theme.Spacing

// Design System — tarjeta de ejercicio para grillas de 2 columnas: el
// video ya subido (si existe) en vez de un icono generico, con menu "⋮"
// encima, y solo el nombre debajo -- sin repeticiones/duracion/categoria
// (esta pantalla es de gestion del catalogo, no de ejecucion). "Ver
// video" (dentro del menu "⋮", NO tocando la miniatura -- el reproductor
// (PlayerView/ExoPlayer) capturaba el toque antes de que llegara a un
// clickable puesto sobre el, asi que nunca abria el dialogo) muestra el
// video a pantalla completa.
@Composable
fun TarjetaEjercicio(
    nombre: String,
    materialUrl: String,
    modifier: Modifier = Modifier,
    etiquetaEstado: (@Composable () -> Unit)? = null,
    menu: (@Composable (cerrar: () -> Unit) -> Unit)? = null,
) {
    var menuAbierto by remember { mutableStateOf(false) }
    var videoAmpliado by remember { mutableStateOf(false) }
    val elevacion = CardDefaults.cardElevation(defaultElevation = 2.dp)
    val forma = MaterialTheme.shapes.large

    Card(
        elevation = elevacion,
        shape = forma,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer),
            ) {
                if (materialUrl.isNotBlank()) {
                    ReproductorVideo(url = materialUrl, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(
                        Icons.Rounded.FitnessCenter,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(40.dp),
                    )
                }
                if (etiquetaEstado != null) {
                    Box(modifier = Modifier.align(Alignment.BottomStart).padding(6.dp)) {
                        etiquetaEstado()
                    }
                }
                if (menu != null) {
                    Box(modifier = Modifier.align(Alignment.TopEnd)) {
                        IconButton(onClick = { menuAbierto = true }) {
                            Icon(
                                Icons.Rounded.MoreVert,
                                contentDescription = "Más opciones",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                            if (materialUrl.isNotBlank()) {
                                DropdownMenuItem(
                                    text = { Text("Ver video") },
                                    leadingIcon = { Icon(Icons.Rounded.PlayCircle, contentDescription = null) },
                                    onClick = {
                                        menuAbierto = false
                                        videoAmpliado = true
                                    },
                                )
                            }
                            menu { menuAbierto = false }
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = nombre,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }

    if (videoAmpliado && materialUrl.isNotBlank()) {
        Dialog(
            onDismissRequest = { videoAmpliado = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                ReproductorVideo(
                    url = materialUrl,
                    alto = null,
                    esquinas = 0.dp,
                    modifier = Modifier.fillMaxSize(),
                )
                // Salir del video a pantalla completa y volver a la lista
                // de ejercicios -- ademas de tocar fuera/atras (ya
                // cubierto por onDismissRequest), un boton visible es mas
                // descubrible dentro de un reproductor a pantalla completa.
                IconButton(
                    onClick = { videoAmpliado = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(Spacing.sm),
                ) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Salir del video",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}
