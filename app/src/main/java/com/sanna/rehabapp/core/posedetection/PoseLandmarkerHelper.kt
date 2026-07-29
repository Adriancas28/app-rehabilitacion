package com.sanna.rehabapp.core.posedetection

import android.content.Context
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

private const val MODELO_ASSET = "pose_landmarker_lite.task"

// HU07 — envuelve MediaPipe Pose Landmarker en modo LIVE_STREAM: cada frame
// de la cámara se entrega de forma asíncrona (detectarAsync) y el resultado
// llega después por el callback, sin bloquear el hilo de análisis de
// CameraX. numPoses = 1 resuelve HU07-CA03 (si hay más de una persona en
// el encuadre, el modelo prioriza la más prominente) sin lógica adicional.
class PoseLandmarkerHelper(
    context: Context,
    private val alEncontrarResultado: (PoseLandmarkerResult) -> Unit,
    private val alFallar: (Exception) -> Unit,
) {
    private var poseLandmarker: PoseLandmarker? = null

    init {
        val opcionesBase = BaseOptions.builder()
            .setModelAssetPath(MODELO_ASSET)
            .build()
        val opciones = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(opcionesBase)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setNumPoses(1)
            .setMinPoseDetectionConfidence(0.5f)
            .setMinTrackingConfidence(0.5f)
            .setMinPosePresenceConfidence(0.5f)
            .setResultListener { resultado, _ -> alEncontrarResultado(resultado) }
            .setErrorListener { error -> alFallar(error) }
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, opciones)
    }

    fun detectarAsync(imagen: MPImage, marcaDeTiempoMs: Long) {
        poseLandmarker?.detectAsync(imagen, marcaDeTiempoMs)
    }

    fun cerrar() {
        poseLandmarker?.close()
        poseLandmarker = null
    }
}
