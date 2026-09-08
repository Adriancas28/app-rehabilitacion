package com.sanna.rehabapp.core.designsystem

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
    alto: Dp = 220.dp,
) {
    val contexto = LocalContext.current
    val forma = RoundedCornerShape(16.dp)

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
            .height(alto)
            .clip(forma),
    )
}
