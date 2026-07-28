package com.sanna.rehabapp.domain.repository

interface AuthRepository {
    val uidActual: String?

    suspend fun login(email: String, password: String): Result<Unit>

    fun logout()

    // RNF02: recuperación de acceso. No hay auto-registro, pero sí se
    // permite reestablecer la contraseña de una cuenta ya existente.
    suspend fun enviarCorreoRecuperacion(email: String): Result<Unit>
}
