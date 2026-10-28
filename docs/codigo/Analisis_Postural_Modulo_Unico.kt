// =============================================================================================
// COPIA DE DOCUMENTACIÓN (no forma parte del build): reúne en un solo archivo el código real del
// análisis postural de SANNA. Cada sección conserva el código tal cual está en el proyecto; el
// archivo de origen se indica en cada encabezado. Si se edita el código fuente, regenerar esta copia.
//
// FLUJO:
//  Cámara → bitmap rotado/espejado → MediaPipe (33 landmarks) → LandmarksFrame → suavizado One Euro
//  → ¿visibilidad ≥ 0,5? → PoseGuideCalculator (ÁNGULO vs meta ± tolerancia → color/progreso)
//  → GuideStateMachine (acción → repetición → sesión) → ResultadoAccion → ResultadoSesion → Firestore
// =============================================================================================
package com.sanna.rehabapp.core.posedetection


// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/LandmarksFrame.kt ═══════════════════════════


// Guía visual en vivo: un landmark reducido a lo que hace falta para geometría
// y dibujo, sin depender del tipo real de MediaPipe (así la lógica es pura y
// se prueba con JUnit normal). x e y están normalizados (0..1) sobre la imagen
// analizada; z conserva la escala de MediaPipe.
data class PuntoPose(val x: Float, val y: Float, val z: Float = 0f, val visibilidad: Float = 1f)

// Un cuadro de pose: los 33 landmarks de la persona detectada y el tamaño de
// la imagen sobre la que están normalizados (ya rotada y espejada).
data class LandmarksFrame(
    val puntos: List<PuntoPose>,
    val anchoImagen: Int,
    val altoImagen: Int,
    val marcaMs: Long,
) {
    operator fun get(indice: Int): PuntoPose? = puntos.getOrNull(indice)
}

// Adaptador desde MediaPipe; devuelve null si no hay persona en el cuadro.
fun PoseLandmarkerResult.aFrame(anchoImagen: Int, altoImagen: Int): LandmarksFrame? {
    val landmarks = landmarks().firstOrNull() ?: return null
    return LandmarksFrame(
        puntos = landmarks.map { PuntoPose(it.x(), it.y(), it.z(), it.visibility().orElse(0f)) },
        anchoImagen = anchoImagen,
        altoImagen = altoImagen,
        marcaMs = timestampMs(),
    )
}

// Conexiones del esqueleto de BlazePose, para el modo de depuración.
val CONEXIONES_ESQUELETO: List<Pair<Int, Int>> = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 27 to 29, 29 to 31, 27 to 31,
    24 to 26, 26 to 28, 28 to 30, 30 to 32, 28 to 32,
    15 to 17, 15 to 19, 15 to 21, 16 to 18, 16 to 20, 16 to 22,
    0 to 11, 0 to 12,
)

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/ConfiabilidadPose.kt ═══════════════════════════

// HU07 (guía visual): un cuadro es confiable para una acción solo si TODOS los
// landmarks que esa acción usa están presentes y con visibilidad suficiente.
// Un cuadro no confiable no hace avanzar ni penaliza la acción: se le pide al
// paciente reubicarse.
const val VISIBILIDAD_MINIMA_CONFIABLE = 0.5f

fun LandmarksFrame.esConfiable(
    requeridos: Collection<Int>,
    visibilidadMinima: Float = VISIBILIDAD_MINIMA_CONFIABLE,
): Boolean = requeridos.all { indice ->
    val punto = this[indice]
    punto != null && punto.visibilidad >= visibilidadMinima
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/OneEuroFilter.kt ═══════════════════════════


// Filtro One Euro (Casiez et al., 2012): suaviza mucho cuando la señal está casi
// quieta (quita el temblor) y poco cuando se mueve rápido (sin retraso visible).
// - minCutoff: frecuencia de corte base en Hz; menor = más suavizado en reposo.
// - beta: cuánto sube la frecuencia de corte con la velocidad; mayor = menos retraso al moverse.
// - dCutoff: frecuencia de corte del filtro de la derivada.
class OneEuroFilter(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f,
    private val dCutoff: Float = 1.0f,
) {
    private var valorPrevio: Float? = null
    private var derivadaPrevia = 0f
    private var instantePrevioSeg = 0.0

    fun filtrar(valor: Float, instanteSeg: Double): Float {
        val previo = valorPrevio
        if (previo == null) {
            valorPrevio = valor
            instantePrevioSeg = instanteSeg
            return valor
        }
        val dt = (instanteSeg - instantePrevioSeg).toFloat()
        if (dt <= 0f) return previo
        val derivada = (valor - previo) / dt
        val derivadaSuave = suavizar(derivada, derivadaPrevia, alfa(dCutoff, dt))
        val corte = minCutoff + beta * abs(derivadaSuave)
        val filtrado = suavizar(valor, previo, alfa(corte, dt))
        valorPrevio = filtrado
        derivadaPrevia = derivadaSuave
        instantePrevioSeg = instanteSeg
        return filtrado
    }

    fun reiniciar() {
        valorPrevio = null
        derivadaPrevia = 0f
    }

    private fun alfa(corte: Float, dt: Float): Float {
        val tau = 1f / (2f * PI.toFloat() * corte)
        return 1f / (1f + tau / dt)
    }

    private fun suavizar(actual: Float, previo: Float, alfa: Float): Float = alfa * actual + (1f - alfa) * previo
}

// Suaviza los landmarks de un cuadro de pose, un filtro por coordenada y por
// landmark. `indices` limita qué landmarks se filtran (los usados por la acción
// actual); el resto pasa sin cambios. La visibilidad no se filtra: se usa tal
// cual para decidir si el cuadro es confiable.
class FiltroLandmarks(
    private val minCutoff: Float = 1.0f,
    private val beta: Float = 0.05f,
    private val dCutoff: Float = 1.0f,
) {
    private class Canales(minCutoff: Float, beta: Float, dCutoff: Float) {
        val x = OneEuroFilter(minCutoff, beta, dCutoff)
        val y = OneEuroFilter(minCutoff, beta, dCutoff)
        val z = OneEuroFilter(minCutoff, beta, dCutoff)
    }

    private val porLandmark = mutableMapOf<Int, Canales>()

    fun aplicar(frame: LandmarksFrame, indices: Collection<Int>? = null): LandmarksFrame {
        val instanteSeg = frame.marcaMs / 1000.0
        val nuevos = frame.puntos.mapIndexed { indice, punto ->
            if (indices != null && indice !in indices) {
                punto
            } else {
                val canales = porLandmark.getOrPut(indice) { Canales(minCutoff, beta, dCutoff) }
                punto.copy(
                    x = canales.x.filtrar(punto.x, instanteSeg),
                    y = canales.y.filtrar(punto.y, instanteSeg),
                    z = canales.z.filtrar(punto.z, instanteSeg),
                )
            }
        }
        return frame.copy(puntos = nuevos)
    }

    fun reiniciar() {
        porLandmark.clear()
    }
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/AnguloCalculator.kt ═══════════════════════════


// Un landmark de MediaPipe reducido a lo que hace falta para geometría —
// sin depender del tipo real de MediaPipe, para que esta función sea pura
// y testeable con JUnit normal (sin Android ni el SDK de MediaPipe).
data class Punto3D(val x: Float, val y: Float, val z: Float = 0f)

// HU08-CA01: ángulo (en grados, 0-180) formado en `vertice` por los
// segmentos vertice→inicio y vertice→fin, vía el producto punto entre
// ambos vectores. Usa x/y/z (no solo el plano de la cámara) para que el
// ángulo sea razonable aunque el paciente no esté perfectamente de frente.
fun calcularAngulo(inicio: Punto3D, vertice: Punto3D, fin: Punto3D): Float {
    val v1x = inicio.x - vertice.x
    val v1y = inicio.y - vertice.y
    val v1z = inicio.z - vertice.z
    val v2x = fin.x - vertice.x
    val v2y = fin.y - vertice.y
    val v2z = fin.z - vertice.z

    val magnitud1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z)
    val magnitud2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z)
    if (magnitud1 == 0f || magnitud2 == 0f) return 0f

    val productoPunto = v1x * v2x + v1y * v2y + v1z * v2z
    val coseno = (productoPunto / (magnitud1 * magnitud2)).coerceIn(-1f, 1f)
    return Math.toDegrees(acos(coseno).toDouble()).toFloat()
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/PoseGuideCalculator.kt ═══════════════════════════


// Guía visual en vivo, etapa 3: motor de cálculo GENÉRICO y puro (sin Android).
// Dada la acción actual y un cuadro de pose, calcula todo lo que la guía y la
// máquina de estados necesitan, sin asumir ninguna parte del cuerpo: el
// segmento se define por los landmarks de la propia acción.

enum class EstadoGuia { VERDE, AMBAR, ROJO }

// Umbrales del color de estado. Verde: dentro de la tolerancia. Ámbar: dentro de
// `factorAmbar` veces la tolerancia. Rojo: más lejos.
data class UmbralesGuia(val factorAmbar: Float = 3f)

data class GuideFrame(
    // Ángulo actual (0°–180°) entre el segmento pivote→distal y la línea pivote→referencia.
    val anguloActual: Float,
    // Largo del segmento a dibujar, en píxeles de la imagen analizada.
    val longitudSegmento: Float,
    // 0..1 entre anguloInicio y anguloMeta (en ambos sentidos).
    val progreso: Float,
    // |anguloActual − anguloMeta| ≤ tolerancia.
    val enZona: Boolean,
    val estado: EstadoGuia,
    // +1 si el segmento gira en sentido horario en pantalla al aumentar el ángulo, −1 si antihorario.
    val signoGiro: Int,
    // Landmarks de la acción, normalizados sobre la imagen analizada.
    val pivote: PuntoPose,
    val distal: PuntoPose,
    val intermedio: PuntoPose?,
    // Dónde queda el extremo del segmento al alcanzar la meta (segmento fantasma).
    val puntoMeta: PuntoPose,
    // Dirección de la línea pivote→referencia en radianes, en píxeles de imagen (y hacia abajo).
    val direccionReferencia: Float,
    val anchoImagen: Int,
    val altoImagen: Int,
) {
    // Punto (normalizado) sobre el arco de radio `longitudSegmento` centrado en el
    // pivote, a `anguloGrados` de la línea de referencia. Sirve para dibujar el
    // recorrido, la zona meta y el segmento fantasma.
    fun puntoEnArco(anguloGrados: Float, radio: Float = longitudSegmento): PuntoPose {
        val radianes = direccionReferencia + signoGiro * Math.toRadians(anguloGrados.toDouble()).toFloat()
        val x = pivote.x * anchoImagen + radio * cos(radianes)
        val y = pivote.y * altoImagen + radio * sin(radianes)
        return PuntoPose(x / anchoImagen, y / altoImagen)
    }
}

object PoseGuideCalculator {

    private const val ANGULO_MINIMO_PARA_DEDUCIR_SIGNO = 5f

    // Devuelve null si el cuadro no es confiable para esta acción (algún landmark
    // requerido no está visible): quien llama no debe avanzar ni penalizar.
    // `signoAnterior` conserva el sentido ya deducido cuando el segmento está
    // sobre la línea de referencia y no se puede deducir de este cuadro.
    fun calcular(
        frame: LandmarksFrame,
        accion: Accion,
        signoAnterior: Int? = null,
        umbrales: UmbralesGuia = UmbralesGuia(),
    ): GuideFrame? {
        if (!frame.esConfiable(listOf(accion.puntoReferencia, accion.pivote, accion.puntoDistal))) return null
        val referencia = frame[accion.puntoReferencia] ?: return null
        val pivote = frame[accion.pivote] ?: return null
        val distal = frame[accion.puntoDistal] ?: return null
        val intermedio = accion.puntoIntermedio?.let { frame[it] }?.takeIf { it.visibilidad >= VISIBILIDAD_MINIMA_CONFIABLE }

        val ancho = frame.anchoImagen.toFloat()
        val alto = frame.altoImagen.toFloat()
        // Todo el cálculo geométrico se hace en píxeles de la imagen, no en
        // coordenadas normalizadas: de lo contrario los ángulos se deforman.
        val ux = (referencia.x - pivote.x) * ancho
        val uy = (referencia.y - pivote.y) * alto
        val vx = (distal.x - pivote.x) * ancho
        val vy = (distal.y - pivote.y) * alto
        val magnitudU = hypot(ux, uy)
        val magnitudV = hypot(vx, vy)
        if (magnitudU == 0f || magnitudV == 0f) return null

        val angulo = Math.toDegrees(abs(atan2(ux * vy - uy * vx, ux * vx + uy * vy)).toDouble()).toFloat()
        val longitud = if (intermedio != null) {
            hypot((intermedio.x - pivote.x) * ancho, (intermedio.y - pivote.y) * alto) +
                hypot((distal.x - intermedio.x) * ancho, (distal.y - intermedio.y) * alto)
        } else {
            magnitudV
        }

        val signo = resolverSigno(accion.sentido, ux * vy - uy * vx, angulo, signoAnterior)
        val direccionReferencia = atan2(uy, ux)

        val distancia = abs(angulo - accion.anguloMeta)
        val enZona = distancia <= accion.tolerancia
        val estado = when {
            enZona -> EstadoGuia.VERDE
            distancia <= accion.tolerancia * umbrales.factorAmbar -> EstadoGuia.AMBAR
            else -> EstadoGuia.ROJO
        }
        val recorrido = accion.anguloMeta - accion.anguloInicio
        val progreso = if (recorrido == 0f) {
            if (enZona) 1f else 0f
        } else {
            ((angulo - accion.anguloInicio) / recorrido).coerceIn(0f, 1f)
        }

        val parcial = GuideFrame(
            anguloActual = angulo,
            longitudSegmento = longitud,
            progreso = progreso,
            enZona = enZona,
            estado = estado,
            signoGiro = signo,
            pivote = pivote,
            distal = distal,
            intermedio = intermedio,
            puntoMeta = pivote,
            direccionReferencia = direccionReferencia,
            anchoImagen = frame.anchoImagen,
            altoImagen = frame.altoImagen,
        )
        return parcial.copy(puntoMeta = parcial.puntoEnArco(accion.anguloMeta))
    }

    // En coordenadas de imagen (y hacia abajo), una rotación positiva se ve horaria.
    private fun resolverSigno(sentido: SentidoGiro, cruz: Float, angulo: Float, anterior: Int?): Int = when (sentido) {
        SentidoGiro.HORARIO -> 1
        SentidoGiro.ANTIHORARIO -> -1
        SentidoGiro.AUTOMATICO -> when {
            angulo > ANGULO_MINIMO_PARA_DEDUCIR_SIGNO && angulo < 180f - ANGULO_MINIMO_PARA_DEDUCIR_SIGNO ->
                if (cruz >= 0f) 1 else -1
            else -> anterior ?: 1
        }
    }
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/GuideStateMachine.kt ═══════════════════════════


// Guía visual en vivo, etapa 4: máquina de estados GENÉRICA repetición → acción.
// Pura (sin Android): el tiempo llega como parámetro, así que se prueba con
// cuadros simulados. No conoce ningún ejercicio: solo recorre la lista de
// acciones que se le entrega.

// Qué mensaje corresponde mostrar o decir; la interfaz y la voz lo traducen a texto.
enum class MensajeGuia { REUBICARSE, AVANZAR_MAS, REGRESAR_UN_POCO, MANTENER, ACCION_CUMPLIDA }

sealed interface EventoGuia {
    data class AccionTerminada(val repeticion: Int, val resultado: ResultadoAccion) : EventoGuia
    data class RepeticionCompletada(val numero: Int, val resultados: List<ResultadoAccion>) : EventoGuia
    data object SesionTerminada : EventoGuia

    // Más de `limiteSesionSinPoseMs` sin cuadros confiables: la sesión debe finalizar.
    data object AusenciaProlongada : EventoGuia
}

data class GuideUiState(
    val accion: Accion? = null,
    val guia: GuideFrame? = null,
    val repeticion: Int = 1,
    val totalRepeticiones: Int = 1,
    // Posición (desde 1) de la acción actual y total de acciones por repetición.
    val numeroAccion: Int = 1,
    val totalAcciones: Int = 1,
    // Segundos que faltan para completar el mantenimiento; null si la acción no lo pide o aún no está en zona.
    val segundosMantener: Int? = null,
    val segundosRestantesAccion: Int = 0,
    // false si el último cuadro no sirvió (hay que pedir al paciente reubicarse).
    val confiable: Boolean = true,
    val mensaje: MensajeGuia? = null,
    val terminada: Boolean = false,
    val enPausa: Boolean = false,
)

class GuideStateMachine(
    private val acciones: List<Accion>,
    private val totalRepeticiones: Int,
    repeticionInicial: Int = 1,
    // Sin cuadros confiables durante este tiempo, la acción actual se da por no completada.
    private val limiteAccionSinPoseMs: Long = 20_000L,
    // Sin cuadros confiables durante este tiempo, se avisa que la sesión debe finalizar.
    private val limiteSesionSinPoseMs: Long = 60_000L,
) {
    private var repeticionActual = repeticionInicial
    private var indiceAccion = 0
    private var tiempoAccionMs = 0L
    private var enZonaDesdeMs: Long? = null
    private var mejorAngulo: Float? = null
    private var ultimoInstanteMs: Long? = null
    private var ausenciaTotalDesdeMs: Long? = null
    private var ausenciaAccionDesdeMs: Long? = null
    private var terminada = false
    private var enPausa = false
    private var ultimoEstado = GuideUiState(totalRepeticiones = totalRepeticiones)

    private val resultadosRepeticionActual = mutableListOf<ResultadoAccion>()
    private val cerradas = mutableListOf<List<ResultadoAccion>>()

    init {
        require(acciones.isNotEmpty()) { "El ejercicio necesita al menos una acción" }
        terminada = repeticionActual > totalRepeticiones
        ultimoEstado = construirEstado(null, confiable = true, mensaje = null, held = 0L)
    }

    val estado: GuideUiState get() = ultimoEstado

    // Resultados de las repeticiones ya cerradas, en orden.
    val repeticionesCerradas: List<List<ResultadoAccion>> get() = cerradas.toList()

    // Acciones de la repetición en curso que ya terminaron (para guardar un resultado parcial).
    val accionesDeRepeticionAbierta: List<ResultadoAccion> get() = resultadosRepeticionActual.toList()

    fun pausar() {
        enPausa = true
        ultimoEstado = ultimoEstado.copy(enPausa = true)
    }

    // Al reanudar (ej. tras la pausa entre repeticiones) el reloj de la acción arranca de cero.
    fun reanudar(ahoraMs: Long) {
        enPausa = false
        ultimoInstanteMs = ahoraMs
        tiempoAccionMs = 0L
        enZonaDesdeMs = null
        ausenciaTotalDesdeMs = null
        ausenciaAccionDesdeMs = null
        ultimoEstado = ultimoEstado.copy(enPausa = false)
    }

    // `guia` es null cuando el cuadro no es confiable (sin persona o landmarks ocultos).
    fun procesar(guia: GuideFrame?, ahoraMs: Long): List<EventoGuia> {
        val anterior = ultimoInstanteMs
        ultimoInstanteMs = ahoraMs
        if (terminada || enPausa) return emptyList()
        // Un salto grande entre cuadros (cámara detenida) no debe gastar el tiempo de la acción.
        val dt = if (anterior == null) 0L else (ahoraMs - anterior).coerceIn(0L, DT_MAXIMO_MS)
        val eventos = mutableListOf<EventoGuia>()
        val accion = acciones[indiceAccion]

        if (guia == null) {
            val desdeTotal = ausenciaTotalDesdeMs ?: ahoraMs.also { ausenciaTotalDesdeMs = it }
            val desdeAccion = ausenciaAccionDesdeMs ?: ahoraMs.also { ausenciaAccionDesdeMs = it }
            // El mantenimiento queda congelado mientras no hay pose confiable.
            enZonaDesdeMs = enZonaDesdeMs?.plus(dt)
            when {
                ahoraMs - desdeTotal >= limiteSesionSinPoseMs -> {
                    terminada = true
                    eventos += EventoGuia.AusenciaProlongada
                }
                ahoraMs - desdeAccion >= limiteAccionSinPoseMs -> {
                    cerrarAccion(accion, completada = false, eventos = eventos)
                    ausenciaAccionDesdeMs = ahoraMs
                }
            }
            ultimoEstado = construirEstado(null, confiable = false, mensaje = MensajeGuia.REUBICARSE, held = 0L)
            return eventos
        }

        ausenciaTotalDesdeMs = null
        ausenciaAccionDesdeMs = null
        tiempoAccionMs += dt
        mejorAngulo = mejorEntre(mejorAngulo, guia.anguloActual, accion)

        val heldMs: Long
        if (guia.enZona) {
            val desde = enZonaDesdeMs ?: ahoraMs.also { enZonaDesdeMs = it }
            heldMs = ahoraMs - desde
        } else {
            enZonaDesdeMs = null
            heldMs = 0L
        }

        val cumplida = guia.enZona && heldMs >= accion.mantenerSeg * 1000L
        val sinTiempo = tiempoAccionMs >= accion.tiempoMaxSeg * 1000L
        val mensaje = when {
            cumplida -> MensajeGuia.ACCION_CUMPLIDA
            guia.enZona -> MensajeGuia.MANTENER
            noLlegoAun(guia.anguloActual, accion) -> MensajeGuia.AVANZAR_MAS
            else -> MensajeGuia.REGRESAR_UN_POCO
        }
        val estadoAntesDeCerrar = construirEstado(guia, confiable = true, mensaje = mensaje, held = heldMs)

        when {
            cumplida -> cerrarAccion(accion, completada = true, eventos = eventos)
            sinTiempo -> cerrarAccion(accion, completada = false, eventos = eventos)
        }
        ultimoEstado = if (eventos.isEmpty()) estadoAntesDeCerrar else construirEstado(null, confiable = true, mensaje = mensaje.takeIf { cumplida }, held = 0L)
        return eventos
    }

    private fun cerrarAccion(accion: Accion, completada: Boolean, eventos: MutableList<EventoGuia>) {
        val mejor = mejorAngulo ?: accion.anguloInicio
        val resultado = ResultadoAccion(
            ordenAccion = accion.orden,
            anguloMaximo = mejor,
            completada = completada,
            tiempoSeg = tiempoAccionMs / 1000f,
            desviacionAngular = if (completada) 0f else (abs(mejor - accion.anguloMeta) - accion.tolerancia).coerceAtLeast(0f),
        )
        resultadosRepeticionActual += resultado
        eventos += EventoGuia.AccionTerminada(repeticionActual, resultado)
        tiempoAccionMs = 0L
        enZonaDesdeMs = null
        mejorAngulo = null
        indiceAccion += 1
        if (indiceAccion >= acciones.size) {
            val copia = resultadosRepeticionActual.toList()
            cerradas += copia
            resultadosRepeticionActual.clear()
            eventos += EventoGuia.RepeticionCompletada(repeticionActual, copia)
            indiceAccion = 0
            repeticionActual += 1
            if (repeticionActual > totalRepeticiones) {
                terminada = true
                eventos += EventoGuia.SesionTerminada
            }
        }
    }

    // Para metas mayores que el inicio el "mejor" ángulo es el máximo; para metas menores, el mínimo.
    private fun mejorEntre(actual: Float?, nuevo: Float, accion: Accion): Float =
        when {
            actual == null -> nuevo
            accion.anguloMeta >= accion.anguloInicio -> maxOf(actual, nuevo)
            else -> minOf(actual, nuevo)
        }

    private fun noLlegoAun(angulo: Float, accion: Accion): Boolean =
        if (accion.anguloMeta >= accion.anguloInicio) angulo < accion.anguloMeta else angulo > accion.anguloMeta

    private fun construirEstado(guia: GuideFrame?, confiable: Boolean, mensaje: MensajeGuia?, held: Long): GuideUiState {
        val accion = acciones[indiceAccion.coerceIn(0, acciones.lastIndex)]
        val restanteMs = (accion.tiempoMaxSeg * 1000L - tiempoAccionMs).coerceAtLeast(0L)
        val mantener = if (accion.mantenerSeg > 0 && guia?.enZona == true) {
            ceil((accion.mantenerSeg * 1000L - held).coerceAtLeast(0L) / 1000.0).toInt()
        } else {
            null
        }
        return GuideUiState(
            accion = accion,
            guia = guia,
            repeticion = repeticionActual.coerceAtMost(totalRepeticiones),
            totalRepeticiones = totalRepeticiones,
            numeroAccion = indiceAccion + 1,
            totalAcciones = acciones.size,
            segundosMantener = mantener,
            segundosRestantesAccion = ceil(restanteMs / 1000.0).toInt(),
            confiable = confiable,
            mensaje = mensaje,
            terminada = terminada,
            enPausa = enPausa,
        )
    }

    private companion object {
        const val DT_MAXIMO_MS = 500L
    }
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/core/posedetection/ResultadoPorAcciones.kt ═══════════════════════════


private const val TIPO_RANGO_INCOMPLETO = "Rango incompleto"
private const val TIPO_MANTENIMIENTO_INCOMPLETO = "Mantenimiento incompleto"

// Puntaje de una acción (0–100): completada = 100; si no, qué tan cerca de la meta
// llegó respecto del recorrido total, para que un casi-logro no valga igual que no moverse.
fun puntajeDeAccion(accion: Accion, resultado: ResultadoAccion): Float {
    if (resultado.completada) return 100f
    val recorrido = abs(accion.anguloMeta - accion.anguloInicio)
    if (recorrido == 0f) return 0f
    val faltante = abs(resultado.anguloMaximo - accion.anguloMeta)
    return ((1f - faltante / recorrido).coerceIn(0f, 1f)) * 100f
}

// HU08-CA04/HU09-CA03/HU09-CA04: arma el ResultadoSesion a partir de los resultados
// por acción de cada repetición cerrada. El porcentaje de cada repetición es el
// promedio de los puntajes de sus acciones y el de la sesión, el de sus repeticiones.
fun construirResultadoPorAcciones(
    acciones: List<Accion>,
    repeticiones: List<List<ResultadoAccion>>,
    repeticionesCompletadas: Int,
    repeticionesAsignadas: Int,
    numeroRepeticionInicial: Int = 1,
): ResultadoSesion {
    val porOrden = acciones.associateBy { it.orden }
    if (repeticiones.isEmpty()) {
        return ResultadoSesion(repeticionesCompletadas = repeticionesCompletadas, repeticionesAsignadas = repeticionesAsignadas)
    }

    val detalle = repeticiones.mapIndexed { indice, resultados ->
        val puntajes = resultados.mapNotNull { r -> porOrden[r.ordenAccion]?.let { puntajeDeAccion(it, r) } }
        val errores = resultados.filterNot { it.completada }.mapNotNull { r ->
            val accion = porOrden[r.ordenAccion] ?: return@mapNotNull null
            ErrorDetectado(
                articulacion = accion.nombre,
                tipo = if (r.desviacionAngular == 0f) TIPO_MANTENIMIENTO_INCOMPLETO else TIPO_RANGO_INCOMPLETO,
                repeticiones = 1,
                anguloDetectado = r.anguloMaximo,
                anguloEsperado = accion.anguloMeta,
                segundo = r.tiempoSeg.toInt(),
            )
        }
        DetalleRepeticion(
            numero = indice + numeroRepeticionInicial,
            porcentajeEjecucion = if (puntajes.isEmpty()) 0f else puntajes.average().toFloat(),
            errores = errores,
            acciones = resultados,
        )
    }

    val todos = repeticiones.flatten()
    val angulos = todos.groupBy { it.ordenAccion }.mapNotNull { (orden, lista) ->
        val accion = porOrden[orden] ?: return@mapNotNull null
        AnguloDetectado(
            articulacion = accion.nombre,
            anguloDetectado = lista.map { it.anguloMaximo }.average().toFloat(),
            anguloEsperado = accion.anguloMeta,
            desviacion = lista.map { it.desviacionAngular }.average().toFloat(),
        )
    }
    val erroresAgregados = detalle.flatMap { it.errores }
        .groupBy { it.articulacion to it.tipo }
        .map { (clave, lista) -> ErrorDetectado(articulacion = clave.first, tipo = clave.second, repeticiones = lista.size) }

    return ResultadoSesion(
        angulosDetectados = angulos,
        desviacionPromedio = todos.map { it.desviacionAngular }.average().toFloat(),
        porcentajeEjecucion = detalle.map { it.porcentajeEjecucion }.average().toFloat(),
        erroresDetectados = erroresAgregados,
        repeticionesCompletadas = repeticionesCompletadas,
        repeticionesAsignadas = repeticionesAsignadas,
        repeticionesCorrectas = repeticiones.count { rep -> rep.isNotEmpty() && rep.all { it.completada } },
        detallePorRepeticion = detalle,
    )
}

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/domain/model/Accion.kt ═══════════════════════════

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

// ═══════════════════════════ app/src/main/java/com/sanna/rehabapp/domain/model/ResultadoAccion.kt ═══════════════════════════

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
