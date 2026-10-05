package com.sanna.rehabapp.core.posedetection

import com.sanna.rehabapp.domain.model.Accion
import com.sanna.rehabapp.domain.model.ResultadoAccion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GuideStateMachineTest {

    private fun accion(
        orden: Int,
        inicio: Float,
        meta: Float,
        tolerancia: Float = 10f,
        mantener: Int = 0,
        tiempoMax: Int = 8,
    ) = Accion(
        orden = orden, nombre = "Acción $orden", puntoReferencia = 24, pivote = 12, puntoDistal = 14,
        anguloInicio = inicio, anguloMeta = meta, tolerancia = tolerancia, mantenerSeg = mantener, tiempoMaxSeg = tiempoMax,
    )

    // Cuadro de guía simulado: lo que importa a la máquina es el ángulo y si está en zona.
    private fun guia(angulo: Float, accion: Accion) = GuideFrame(
        anguloActual = angulo, longitudSegmento = 10f, progreso = 0f,
        enZona = kotlin.math.abs(angulo - accion.anguloMeta) <= accion.tolerancia,
        estado = EstadoGuia.ROJO, signoGiro = 1,
        pivote = PuntoPose(0.5f, 0.5f), distal = PuntoPose(0.6f, 0.5f), intermedio = null, puntoMeta = PuntoPose(0.6f, 0.5f),
        direccionReferencia = 0f, anchoImagen = 100, altoImagen = 100,
    )

    private val subir = accion(1, inicio = 15f, meta = 90f, mantener = 2)
    private val bajar = accion(2, inicio = 90f, meta = 15f, tolerancia = 15f)

    private fun maquina(totalRepeticiones: Int = 2, repeticionInicial: Int = 1) =
        GuideStateMachine(listOf(subir, bajar), totalRepeticiones, repeticionInicial)

    @Test
    fun `empieza en la primera accion de la primera repeticion`() {
        val e = maquina().estado
        assertEquals(1, e.repeticion)
        assertEquals(1, e.numeroAccion)
        assertEquals(2, e.totalAcciones)
        assertEquals("Acción 1", e.accion?.nombre)
    }

    @Test
    fun `una accion con mantenimiento exige permanecer en zona ese tiempo continuo`() {
        val m = maquina()
        assertTrue(m.procesar(guia(90f, subir), 0).isEmpty())
        assertEquals(2, m.estado.segundosMantener)
        assertTrue(m.procesar(guia(90f, subir), 1000).isEmpty())
        val eventos = m.procesar(guia(90f, subir), 2000)
        val terminada = eventos.filterIsInstance<EventoGuia.AccionTerminada>().single()
        assertTrue(terminada.resultado.completada)
        assertEquals(2, m.estado.numeroAccion)
    }

    @Test
    fun `salir de la zona reinicia el contador de mantenimiento`() {
        val m = maquina()
        m.procesar(guia(90f, subir), 0)
        m.procesar(guia(90f, subir), 1500)
        m.procesar(guia(40f, subir), 1800) // sale de la zona
        assertNull(m.estado.segundosMantener)
        m.procesar(guia(90f, subir), 2000)
        assertTrue(m.procesar(guia(90f, subir), 3500).isEmpty())
        assertTrue(m.procesar(guia(90f, subir), 4000).any { it is EventoGuia.AccionTerminada })
    }

    @Test
    fun `una accion sin mantenimiento se cumple al llegar a la zona`() {
        val m = maquina()
        m.procesar(guia(90f, subir), 0); m.procesar(guia(90f, subir), 2000) // completa la primera
        val eventos = m.procesar(guia(15f, bajar), 2100)
        assertTrue(eventos.filterIsInstance<EventoGuia.AccionTerminada>().single().resultado.completada)
    }

    @Test
    fun `al agotarse el tiempo maximo se registra no completada con el mejor angulo alcanzado`() {
        val m = maquina()
        m.procesar(guia(40f, subir), 0)
        m.procesar(guia(60f, subir), 400)
        m.procesar(guia(50f, subir), 800)
        var eventos: List<EventoGuia> = emptyList()
        var t = 800L
        while (eventos.isEmpty()) {
            t += 400
            eventos = m.procesar(guia(50f, subir), t)
        }
        val r = eventos.filterIsInstance<EventoGuia.AccionTerminada>().single().resultado
        assertFalse(r.completada)
        assertEquals(60f, r.anguloMaximo, 0.001f)
        // Faltaron 30° para la meta de 90° y la tolerancia es 10°.
        assertEquals(20f, r.desviacionAngular, 0.001f)
        assertEquals(2, m.estado.numeroAccion)
    }

    @Test
    fun `en una accion descendente el mejor angulo es el minimo`() {
        val m = maquina()
        m.procesar(guia(90f, subir), 0); m.procesar(guia(90f, subir), 2000)
        m.procesar(guia(80f, bajar), 2100)
        m.procesar(guia(50f, bajar), 2500)
        var eventos: List<EventoGuia> = emptyList()
        var t = 2500L
        while (eventos.isEmpty()) {
            t += 400
            eventos = m.procesar(guia(70f, bajar), t)
        }
        assertEquals(50f, eventos.filterIsInstance<EventoGuia.AccionTerminada>().single().resultado.anguloMaximo, 0.001f)
    }

    @Test
    fun `los cuadros no confiables no avanzan ni gastan el tiempo de la accion`() {
        val m = maquina()
        m.procesar(guia(40f, subir), 0)
        repeat(30) { m.procesar(null, 400L * (it + 1)) } // 12 s sin pose, menos que el limite de 20 s
        assertEquals(1, m.estado.numeroAccion)
        assertFalse(m.estado.confiable)
        assertEquals(MensajeGuia.REUBICARSE, m.estado.mensaje)
        // Al volver, todavía queda casi todo el tiempo de la acción.
        m.procesar(guia(40f, subir), 12_500)
        assertTrue(m.estado.segundosRestantesAccion >= 7)
    }

    @Test
    fun `tras 20 segundos sin pose la accion se da por no completada y se sigue`() {
        val m = maquina()
        var cerrada = false
        var t = 0L
        while (t <= 20_000 && !cerrada) {
            cerrada = m.procesar(null, t).any { it is EventoGuia.AccionTerminada }
            t += 500
        }
        assertTrue(cerrada)
        assertEquals(2, m.estado.numeroAccion)
    }

    @Test
    fun `tras 60 segundos sin pose se avisa que la sesion debe finalizar`() {
        val m = maquina()
        var aviso = false
        var t = 0L
        while (t <= 60_000 && !aviso) {
            aviso = m.procesar(null, t).any { it is EventoGuia.AusenciaProlongada }
            t += 500
        }
        assertTrue(aviso)
        assertTrue(m.estado.terminada || aviso)
    }

    @Test
    fun `al terminar la ultima accion se cuenta la repeticion y se reinicia la secuencia`() {
        val m = maquina(totalRepeticiones = 2)
        m.procesar(guia(90f, subir), 0); m.procesar(guia(90f, subir), 2000)
        val eventos = m.procesar(guia(15f, bajar), 2200)
        val rep = eventos.filterIsInstance<EventoGuia.RepeticionCompletada>().single()
        assertEquals(1, rep.numero)
        assertEquals(2, rep.resultados.size)
        assertEquals(2, m.estado.repeticion)
        assertEquals(1, m.estado.numeroAccion)
        assertEquals(1, m.repeticionesCerradas.size)
    }

    @Test
    fun `al completar todas las repeticiones la sesion termina`() {
        val m = maquina(totalRepeticiones = 1)
        m.procesar(guia(90f, subir), 0); m.procesar(guia(90f, subir), 2000)
        val eventos = m.procesar(guia(15f, bajar), 2200)
        assertTrue(eventos.contains(EventoGuia.SesionTerminada))
        assertTrue(m.estado.terminada)
        assertTrue(m.procesar(guia(90f, subir), 3000).isEmpty())
    }

    @Test
    fun `reanudar una sesion continua desde la repeticion indicada`() {
        val m = maquina(totalRepeticiones = 3, repeticionInicial = 3)
        assertEquals(3, m.estado.repeticion)
        m.procesar(guia(90f, subir), 0); m.procesar(guia(90f, subir), 2000)
        val eventos = m.procesar(guia(15f, bajar), 2200)
        assertEquals(3, eventos.filterIsInstance<EventoGuia.RepeticionCompletada>().single().numero)
        assertTrue(eventos.contains(EventoGuia.SesionTerminada))
    }

    @Test
    fun `una sesion ya completa arranca terminada`() {
        assertTrue(maquina(totalRepeticiones = 2, repeticionInicial = 3).estado.terminada)
    }

    @Test
    fun `en pausa no se procesa y al reanudar el reloj de la accion arranca de cero`() {
        val m = maquina()
        m.procesar(guia(40f, subir), 0)
        m.procesar(guia(40f, subir), 400)
        m.pausar()
        assertTrue(m.procesar(guia(90f, subir), 5000).isEmpty())
        m.reanudar(6000)
        m.procesar(guia(40f, subir), 6100)
        assertTrue(m.estado.segundosRestantesAccion >= 7)
    }

    @Test
    fun `el mensaje indica si falta recorrer o hay que regresar`() {
        val m = maquina()
        m.procesar(guia(40f, subir), 0)
        assertEquals(MensajeGuia.AVANZAR_MAS, m.estado.mensaje)
        m.procesar(guia(130f, subir), 100)
        assertEquals(MensajeGuia.REGRESAR_UN_POCO, m.estado.mensaje)
        m.procesar(guia(90f, subir), 200)
        assertEquals(MensajeGuia.MANTENER, m.estado.mensaje)
    }

    @Test
    fun `sin acciones no se puede crear la maquina`() {
        val resultado = runCatching { GuideStateMachine(emptyList(), 1) }
        assertTrue(resultado.exceptionOrNull() is IllegalArgumentException)
    }

    @Test
    fun `el puntaje de una accion no completada depende de cuanto se acerco`() {
        val r = ResultadoAccion(ordenAccion = 1, anguloMaximo = 52.5f, completada = false, tiempoSeg = 8f, desviacionAngular = 27.5f)
        // Recorrido 75° (15→90); faltaron 37,5° → 50 %.
        assertEquals(50f, puntajeDeAccion(subir, r), 0.01f)
        assertEquals(100f, puntajeDeAccion(subir, r.copy(completada = true)), 0.01f)
    }
}
