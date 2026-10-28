package com.sanna.rehabapp.domain.model

// Índices oficiales de los 33 landmarks de MediaPipe Pose (BlazePose) usados
// por las acciones. Se guardan como enteros en los datos del ejercicio.
object PoseLandmark {
    const val NARIZ = 0
    const val HOMBRO_IZQUIERDO = 11
    const val HOMBRO_DERECHO = 12
    const val CODO_IZQUIERDO = 13
    const val CODO_DERECHO = 14
    const val MUNECA_IZQUIERDA = 15
    const val MUNECA_DERECHA = 16
    const val CADERA_IZQUIERDA = 23
    const val CADERA_DERECHA = 24
    const val RODILLA_IZQUIERDA = 25
    const val RODILLA_DERECHA = 26
    const val TOBILLO_IZQUIERDO = 27
    const val TOBILLO_DERECHO = 28
    const val PUNTA_PIE_IZQUIERDO = 31
    const val PUNTA_PIE_DERECHO = 32

    const val MINIMO = 0
    const val MAXIMO = 32

    // Landmark equivalente del lado opuesto. Del 11 al 32 los impares son del
    // lado izquierdo y los pares del derecho; en el rostro hay pares propios.
    fun espejo(indice: Int): Int = when (indice) {
        in 11..32 -> if (indice % 2 == 1) indice + 1 else indice - 1
        1 -> 4
        4 -> 1
        2 -> 5
        5 -> 2
        3 -> 6
        6 -> 3
        7 -> 8
        8 -> 7
        9 -> 10
        10 -> 9
        else -> indice
    }
}
