package com.sanna.rehabapp.core.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
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
import java.util.concurrent.Executors

// HU07 — cámara frontal (modo espejo, para que el paciente se vea mientras
// ejecuta el ejercicio) + MediaPipe Pose Landmarker en vivo. Cada frame de
// CameraX se convierte a Bitmap, se rota y se espeja según la orientación
// real del sensor, y se envía al PoseLandmarkerHelper de forma asíncrona.
@Composable
fun CamaraConDeteccionPose(
    modifier: Modifier = Modifier,
    onResultado: (PoseLandmarkerResult) -> Unit,
    onError: (Exception) -> Unit,
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
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            poseLandmarkerHelper.cerrar()
            executor.shutdown()
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

                try {
                    proveedor.unbindAll()
                    proveedor.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview,
                        analisisImagen,
                    )
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
