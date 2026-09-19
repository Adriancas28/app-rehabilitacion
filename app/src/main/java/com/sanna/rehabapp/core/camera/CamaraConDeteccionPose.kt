package com.sanna.rehabapp.core.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.MirrorMode
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import com.sanna.rehabapp.core.posedetection.PoseLandmarkerHelper
import java.io.File
import java.util.concurrent.Executors

// HU07 — cámara frontal (modo espejo, para que el paciente se vea mientras
// ejecuta el ejercicio) + MediaPipe Pose Landmarker en vivo. Cada frame de
// CameraX se convierte a Bitmap, se rota y se espeja según la orientación
// real del sensor, y se envía al PoseLandmarkerHelper de forma asíncrona.
// Parte 3 (video): si se pasa `archivoVideo`, además del análisis se graba
// (sin audio, calidad SD para que pese poco) la misma cámara frontal mientras
// este composable esté en pantalla; al salir se detiene la grabación y, ya
// finalizado el archivo, se avisa por `onVideoGrabado`. Si el dispositivo no
// soporta Preview + ImageAnalysis + VideoCapture a la vez, se sigue con solo
// análisis (la sesión no se ve afectada, simplemente queda sin video).
@Composable
fun CamaraConDeteccionPose(
    modifier: Modifier = Modifier,
    onResultado: (PoseLandmarkerResult) -> Unit,
    onError: (Exception) -> Unit,
    archivoVideo: File? = null,
    onVideoGrabado: (File) -> Unit = {},
) {
    val contexto = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }

    // rememberUpdatedState evita que PoseLandmarkerHelper (creado una sola
    // vez) quede atado a una versión vieja de estos callbacks si el llamador
    // pasa una lambda nueva en cada recomposición.
    val onResultadoActualizado by rememberUpdatedState(onResultado)
    val onErrorActualizado by rememberUpdatedState(onError)

    val poseLandmarkerHelper = remember {
        PoseLandmarkerHelper(
            context = contexto,
            alEncontrarResultado = { onResultadoActualizado(it) },
            alFallar = { onErrorActualizado(it) },
        )
    }
    val onVideoGrabadoActualizado by rememberUpdatedState(onVideoGrabado)
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var grabacion by remember { mutableStateOf<Recording?>(null) }
    var liberado by remember { mutableStateOf(false) }
    var analisisVinculado by remember { mutableStateOf<ImageAnalysis?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            liberado = true
            val enCurso = grabacion
            if (enCurso != null) {
                // El análisis se desvincula ya (sigue entregando frames a
                // MediaPipe, que se cierra justo abajo: sería un crash nativo),
                // pero la cámara con el video se desvincula recién al recibir
                // el evento Finalize (abajo): cortarla de golpe dejaría el
                // archivo incompleto.
                // (clearAnalyzer, no unbind: reconfigurar los casos de uso
                // interrumpe la grabación en curso).
                analisisVinculado?.clearAnalyzer()
                enCurso.stop()
            } else {
                cameraProvider?.unbindAll()
            }
            // Se espera a que termine el frame que el analizador tenga en
            // proceso antes de liberar MediaPipe: cerrarlo con un frame a
            // medias es un crash nativo (SIGSEGV).
            executor.shutdown()
            executor.awaitTermination(1, java.util.concurrent.TimeUnit.SECONDS)
            poseLandmarkerHelper.cerrar()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val proveedor = cameraProviderFuture.get()
                cameraProvider = proveedor

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analisisImagen = ImageAnalysis.Builder()
                    .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()
                analisisImagen.setAnalyzer(executor) { imageProxy ->
                    procesarFrame(imageProxy, esCamaraFrontal = true, poseLandmarkerHelper)
                }

                var videoCapture: VideoCapture<Recorder>? = null
                if (archivoVideo != null) {
                    val grabador = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(Quality.SD, FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)),
                        )
                        .build()
                    videoCapture = VideoCapture.Builder(grabador)
                        .setMirrorMode(MirrorMode.MIRROR_MODE_ON_FRONT_ONLY)
                        .build()
                }

                fun vincular(conVideo: VideoCapture<Recorder>?) {
                    proveedor.unbindAll()
                    val casosDeUso = listOfNotNull(preview, analisisImagen, conVideo)
                    proveedor.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        *casosDeUso.toTypedArray(),
                    )
                }

                try {
                    try {
                        vincular(videoCapture)
                    } catch (e: Exception) {
                        if (videoCapture == null) throw e
                        videoCapture = null
                        vincular(null)
                    }
                    analisisVinculado = analisisImagen
                    val captura = videoCapture
                    if (captura != null && archivoVideo != null) {
                        val salida = FileOutputOptions.Builder(archivoVideo).build()
                        grabacion = captura.output
                            .prepareRecording(ctx, salida)
                            .start(ContextCompat.getMainExecutor(ctx)) { evento ->
                                if (evento is VideoRecordEvent.Finalize) {
                                    // Estos errores igual dejan un archivo válido y completo.
                                    val utilizable = !evento.hasError() ||
                                        evento.error == VideoRecordEvent.Finalize.ERROR_SOURCE_INACTIVE ||
                                        evento.error == VideoRecordEvent.Finalize.ERROR_DURATION_LIMIT_REACHED ||
                                        evento.error == VideoRecordEvent.Finalize.ERROR_FILE_SIZE_LIMIT_REACHED
                                    if (utilizable && archivoVideo.length() > 0L) {
                                        onVideoGrabadoActualizado(archivoVideo)
                                    } else {
                                        archivoVideo.delete()
                                    }
                                    if (liberado) proveedor.unbindAll()
                                }
                            }
                    }
                } catch (e: Exception) {
                    onErrorActualizado(e)
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
    )
}

private fun procesarFrame(
    imageProxy: ImageProxy,
    esCamaraFrontal: Boolean,
    poseLandmarkerHelper: PoseLandmarkerHelper,
) {
    val marcaDeTiempo = SystemClock.uptimeMillis()
    val ancho = imageProxy.width
    val alto = imageProxy.height
    val rotacion = imageProxy.imageInfo.rotationDegrees.toFloat()

    val bufferBitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
    bufferBitmap.copyPixelsFromBuffer(imageProxy.planes[0].buffer)
    imageProxy.close()

    val matriz = Matrix().apply {
        postRotate(rotacion)
        if (esCamaraFrontal) postScale(-1f, 1f, ancho.toFloat(), alto.toFloat())
    }
    val bitmapRotado = Bitmap.createBitmap(bufferBitmap, 0, 0, ancho, alto, matriz, true)
    val imagenMediaPipe = BitmapImageBuilder(bitmapRotado).build()
    poseLandmarkerHelper.detectarAsync(imagenMediaPipe, marcaDeTiempo)
}
