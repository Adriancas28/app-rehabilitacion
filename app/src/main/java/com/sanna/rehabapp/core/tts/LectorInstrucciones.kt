package com.sanna.rehabapp.core.tts

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

// HU06 — lee en voz alta las instrucciones del ejercicio al iniciar el
// monitoreo: texto a voz nativo de Android (TextToSpeech), en el propio
// dispositivo y sin conexión — mismo criterio de privacidad que HU10-CA06
// (RNF01/RNF06): nunca un servicio de voz en la nube.
@Composable
fun rememberLectorInstrucciones(): (String) -> Unit {
    val contexto = LocalContext.current
    var motor by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        lateinit var instancia: TextToSpeech
        instancia = TextToSpeech(contexto) { estado ->
            if (estado == TextToSpeech.SUCCESS) {
                instancia.language = Locale("es", "ES")
            }
        }
        motor = instancia
        onDispose {
            instancia.stop()
            instancia.shutdown()
        }
    }

    return { texto -> motor?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, null) }
}
