package com.sanna.rehabapp.domain.repository

interface AuthRepository {
    val uidActual: String?

    suspend fun login(email: String, password: String): Result<Unit>

    fun logout()
}
