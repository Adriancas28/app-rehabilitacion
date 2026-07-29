package com.sanna.rehabapp.core.posedetection

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.sanna.rehabapp.domain.model.Articulacion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MODELO_ASSET = "pose_landmarker_lite.task"
private const val INTERVALO_MUESTREO_MS = 200L

// HU02-CA02: pequeño margen sobre el mínimo/máximo observado en el video —
// el video muestra una ejecución de referencia, pero exigir el ángulo
// exacto sin ningún margen sería clínicamente demasiado estricto para un
// paciente real.
private const val MARGEN_TOLERANCIA_GRADOS = 10f

data class RangoAngulo(val min: Float, val max: Float)

// HU02-CA07 — analiza un video de referencia ya grabado (no en vivo, a
// diferencia de HU07) para calcular automáticamente el ROM de cada
// articulación seleccionada, en vez de que el fisioterapeuta lo escriba a
// mano. Corre 100% en el dispositivo con MediaPipe en modo VIDEO (RNF06:
// el video nunca sale del dispositivo).
class AnalizadorVideoReferencia(private val context: Context) {

    suspend fun calcularRangosDeAngulo(
        video: Uri,
        articulaciones: List<Articulacion>,
    ): Map<Articulacion, RangoAngulo> = withContext(Dispatchers.Default) {
        if (articulaciones.isEmpty()) return@withContext emptyMap()

        val retriever = MediaMetadataRetriever()
        val poseLandmarker = crearPoseLandmarkerDeVideo()
        val angulosPorArticulacion = articulaciones.associateWith { mutableListOf<Float>() }

        try {
            retriever.setDataSource(context, video)
            val duracionMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            var marcaDeTiempo = 0L
            while (marcaDeTiempo < duracionMs) {
                val bitmap = retriever.getFrameAtTime(
                    marcaDeTiempo * 1_000,
                    MediaMetadataRetriever.OPTION_CLOSEST,
                )
                if (bitmap != null) {
                    val imagenMediaPipe = BitmapImageBuilder(bitmap).build()
                    val resultado = poseLandmarker.detectForVideo(imagenMediaPipe, marcaDeTiempo)
                    val landmarks = resultado.landmarks().firstOrNull()
                    if (landmarks != null) {
                        articulaciones.forEach { articulacion ->
                            medirAnguloDeArticulacion(articulacion, landmarks)?.let { angulo ->
                                angulosPorArticulacion.getValue(articulacion).add(angulo)
                            }
                        }
                    }
                }
                marcaDeTiempo += INTERVALO_MUESTREO_MS
            }
        } finally {
            retriever.release()
            poseLandmarker.close()
        }

        angulosPorArticulacion
            .filterValues { it.isNotEmpty() }
            .mapValues { (_, angulos) ->
                RangoAngulo(
                    min = (angulos.min() - MARGEN_TOLERANCIA_GRADOS).coerceAtLeast(0f),
                    max = angulos.max() + MARGEN_TOLERANCIA_GRADOS,
                )
            }
    }

    private fun crearPoseLandmarkerDeVideo(): PoseLandmarker {
        val opcionesBase = BaseOptions.builder().setModelAssetPath(MODELO_ASSET).build()
        val opciones = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(opcionesBase)
            .setRunningMode(RunningMode.VIDEO)
            .setNumPoses(1)
            .build()
        return PoseLandmarker.createFromOptions(context, opciones)
    }
}
