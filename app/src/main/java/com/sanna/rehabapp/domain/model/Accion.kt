package com.sanna.rehabapp.domain.model

// Guía visual en vivo: hacia qué lado gira el segmento al ir del ángulo de
// inicio al de meta. El ángulo medido no tiene signo (0°–180°), así que para
// dibujar el recorrido hay que saber el sentido; AUTOMATICO lo deduce del
// primer cuadro estable de la acción.
enum class SentidoGiro {
    AUTOMATICO,
    HORARIO,
    ANTIHORARIO,
    ;

    companion object {
        fun desdeFirestoreOrNull(valor: String?): SentidoGiro? = entries.find { it.name.equals(valor, true) }
    }
}

// HU02/HU06: una acción es un movimiento parcial dentro de una repetición
// (ej. "subir el brazo" y luego "bajarlo"). Se define como dato del ejercicio:
// el segmento a medir se expresa con landmarks de MediaPipe, sin lógica por
// articulación en el código. El ángulo es el que forma el segmento
// pivote→distal con la línea pivote→referencia.
data class Accion(
    val orden: Int,
    val nombre: String,
    val puntoReferencia: Int,
    val pivote: Int,
    // Solo para dibujar (ej. el codo en un brazo extendido); no entra al cálculo.
    val puntoIntermedio: Int? = null,
    val puntoDistal: Int,
    // Lado con el que está definida la acción (convención de autoría).
    val lado: LadoCuerpo = LadoCuerpo.DERECHO,
    val anguloInicio: Float,
    val anguloMeta: Float,
    val tolerancia: Float,
    // Tiempo continuo que debe permanecer en la zona meta; 0 = basta con llegar.
    val mantenerSeg: Int = 0,
    val tiempoMaxSeg: Int,
    val sentido: SentidoGiro = SentidoGiro.AUTOMATICO,
) {
    // HU02-CA18: devuelve los datos incorrectos, vacío si la acción es válida.
    fun errores(): List<String> = buildList {
        if (nombre.isBlank()) add("La acción $orden necesita un nombre.")
        if (tolerancia <= 0f) add("La tolerancia de la acción $orden debe ser mayor a cero.")
        if (tiempoMaxSeg <= 0) add("El tiempo máximo de la acción $orden debe ser mayor a cero.")
        if (mantenerSeg < 0) add("El tiempo de mantenimiento de la acción $orden no puede ser negativo.")
        if (mantenerSeg > tiempoMaxSeg) add("El mantenimiento de la acción $orden no puede superar su tiempo máximo.")
        if (anguloInicio !in 0f..180f) add("El ángulo de inicio de la acción $orden debe estar entre 0° y 180°.")
        if (anguloMeta !in 0f..180f) add("El ángulo meta de la acción $orden debe estar entre 0° y 180°.")
        val puntos = listOfNotNull(puntoReferencia, pivote, puntoDistal, puntoIntermedio)
        if (puntos.any { it !in PoseLandmark.MINIMO..PoseLandmark.MAXIMO }) {
            add("Los puntos de la acción $orden deben ser landmarks válidos (0 a 32).")
        }
        if (setOf(puntoReferencia, pivote, puntoDistal).size < 3) {
            add("Referencia, pivote y punto distal de la acción $orden deben ser distintos.")
        }
    }

    // Misma acción medida sobre el lado opuesto del cuerpo.
    fun espejo(): Accion = copy(
        puntoReferencia = PoseLandmark.espejo(puntoReferencia),
        pivote = PoseLandmark.espejo(pivote),
        puntoIntermedio = puntoIntermedio?.let(PoseLandmark::espejo),
        puntoDistal = PoseLandmark.espejo(puntoDistal),
        lado = lado.opuesto(),
        sentido = when (sentido) {
            SentidoGiro.HORARIO -> SentidoGiro.ANTIHORARIO
            SentidoGiro.ANTIHORARIO -> SentidoGiro.HORARIO
            SentidoGiro.AUTOMATICO -> SentidoGiro.AUTOMATICO
        },
    )

    // HU20: la acción se define sobre un lado; si el lado afectado del
    // paciente es el opuesto se mide su espejo. Con AMBOS no se cambia (cómo
    // se miden los dos lados lo decide la máquina de estados, Etapa 4).
    fun paraLado(ladoAfectado: LadoAfectado): Accion = when (ladoAfectado) {
        LadoAfectado.DERECHO -> if (lado == LadoCuerpo.DERECHO) this else espejo()
        LadoAfectado.IZQUIERDO -> if (lado == LadoCuerpo.IZQUIERDO) this else espejo()
        LadoAfectado.AMBOS -> this
    }
}
