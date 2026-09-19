package com.sanna.rehabapp.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sanna.rehabapp.core.theme.Spacing
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

// Design System — reproductor de video de material terapéutico (HU05-CA01/
// CA02: reproducir el contenido audiovisual asociado a un ejercicio). Usa
// Media3/ExoPlayer en vez del VideoView legacy: controles con auto-ocultado,
// mejor manejo de formatos/buffering. El ExoPlayer se crea y libera junto
// con el ciclo de vida del composable (DisposableEffect), no del proceso.
@Composable
fun ReproductorVideo(
    url: String,
    modifier: Modifier = Modifier,
    alto: Dp? = 220.dp,
    esquinas: Dp = 16.dp,
    // Con true se agrega debajo del video el botón "Ampliar video", que lo abre a
    // pantalla completa. El botón va fuera del PlayerView porque este captura
    // los toques antes de que lleguen a un clickable puesto encima.
    permitirAmpliar: Boolean = false,
) {
    var ampliado by remember { mutableStateOf(false) }
    Column(modifier = modifier) {
        PlayerVista(url = url, alto = alto, esquinas = esquinas)
        if (permitirAmpliar) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            BotonOutline(
                texto = "Ampliar video",
                onClick = { ampliado = true },
                icono = Icons.Rounded.Fullscreen,
            )
        }
    }
    if (ampliado) {
        Dialog(
            onDismissRequest = { ampliado = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
                PlayerVista(url = url, alto = null, esquinas = 0.dp, modifier = Modifier.fillMaxSize())
                IconButton(
                    onClick = { ampliado = false },
                    modifier = Modifier.align(Alignment.TopStart).padding(Spacing.sm),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cerrar video ampliado", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PlayerVista(
    url: String,
    modifier: Modifier = Modifier,
    alto: Dp? = 220.dp,
    esquinas: Dp = 16.dp,
) {
    val contexto = LocalContext.current
    val forma = RoundedCornerShape(esquinas)

    val exoPlayer = remember(url) {
        ExoPlayer.Builder(contexto).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .then(if (alto != null) Modifier.height(alto) else Modifier.fillMaxHeight())
            .clip(forma),
    )
}
