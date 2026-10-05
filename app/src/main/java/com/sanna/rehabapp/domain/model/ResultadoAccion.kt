package com.sanna.rehabapp.domain.model

// HU09/HU17: resultado de una acción dentro de una repetición. Va embebido en
// DetalleRepeticion (un elemento por acción, en el orden en que se ejecutaron).
data class ResultadoAccion(
    val ordenAccion: Int,
    val anguloMaximo: Float,
    val completada: Boolean,
    val tiempoSeg: Float,
    // Diferencia entre el ángulo meta y el máximo alcanzado (0 si llegó a la zona).
    val desviacionAngular: Float = 0f,
)
