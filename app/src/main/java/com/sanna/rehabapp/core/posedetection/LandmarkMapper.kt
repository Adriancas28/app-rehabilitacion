package com.sanna.rehabapp.core.posedetection

// Cómo recorta/ajusta el PreviewView la imagen de la cámara dentro de su área.
// FILL_CENTER (el valor por defecto de PreviewView) llena el área y recorta.
enum class EscalaVista { FILL_CENTER, FIT_CENTER }

data class PuntoPantalla(val x: Float, val y: Float)

// Guía visual en vivo, etapa 2: convierte un landmark normalizado de MediaPipe
// a coordenadas de pantalla (píxeles dentro del PreviewView), válido para
// cualquier landmark.
//
// - anchoImagen/altoImagen: tamaño de la imagen analizada TAL COMO se entregó a
//   MediaPipe (antes de rotar). CamaraConDeteccionPose entrega el bitmap ya
//   rotado y espejado, así que ahí se usa rotacionGrados = 0 y espejarX = false;
//   los demás valores existen para otros flujos de cámara y para las pruebas.
// - rotacionGrados: 0, 90, 180 o 270 en sentido horario (CameraX `rotationDegrees`).
// - espejarX: cámara frontal cuya imagen aún no se espejó.
class LandmarkMapper(
    anchoImagen: Int,
    altoImagen: Int,
    private val anchoVista: Float,
    private val altoVista: Float,
    private val rotacionGrados: Int = 0,
    private val espejarX: Boolean = false,
    escala: EscalaVista = EscalaVista.FILL_CENTER,
) {
    // Tamaño de la imagen una vez rotada.
    private val anchoRotado: Float
    private val altoRotado: Float
    private val factor: Float
    private val desplazamientoX: Float
    private val desplazamientoY: Float

    init {
        require(rotacionGrados % 90 == 0) { "La rotación debe ser múltiplo de 90°" }
        val girada = (rotacionGrados / 90) % 2 != 0
        anchoRotado = (if (girada) altoImagen else anchoImagen).toFloat()
        altoRotado = (if (girada) anchoImagen else altoImagen).toFloat()
        val escalaAncho = anchoVista / anchoRotado
        val escalaAlto = altoVista / altoRotado
        factor = if (escala == EscalaVista.FILL_CENTER) maxOf(escalaAncho, escalaAlto) else minOf(escalaAncho, escalaAlto)
        desplazamientoX = (anchoVista - anchoRotado * factor) / 2f
        desplazamientoY = (altoVista - altoRotado * factor) / 2f
    }

    fun mapear(xNormalizado: Float, yNormalizado: Float): PuntoPantalla {
        // Rotación sobre coordenadas normalizadas (sentido horario).
        var nx = xNormalizado
        var ny = yNormalizado
        repeat(((rotacionGrados / 90) % 4 + 4) % 4) {
            val giradoX = 1f - ny
            val giradoY = nx
            nx = giradoX
            ny = giradoY
        }
        if (espejarX) nx = 1f - nx
        return PuntoPantalla(
            x = nx * anchoRotado * factor + desplazamientoX,
            y = ny * altoRotado * factor + desplazamientoY,
        )
    }

    fun mapear(punto: PuntoPose): PuntoPantalla = mapear(punto.x, punto.y)
}
