package com.sanna.rehabapp.core.navigation

import javax.inject.Inject
import javax.inject.Singleton

// ERR-ADM-009: aviso de una sola vez para la pantalla de Login. Cuando la
// app devuelve al usuario al login por una causa que no es un fallo de
// credenciales (p. ej. cuenta desactivada), lo explica en vez de volver en silencio.
@Singleton
class AvisoLogin @Inject constructor() {
    @Volatile
    private var mensaje: String? = null

    fun publicar(texto: String) {
        mensaje = texto
    }

    /** Devuelve el aviso pendiente y lo borra. */
    fun consumir(): String? = mensaje.also { mensaje = null }
}
