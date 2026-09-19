package com.sanna.rehabapp.feature.admin

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

// HU20/HU21 — validaciones de los formularios de cuentas (ERR-ADM-001/008/011).
// Funciones puras: devuelven el mensaje de error de UN campo, o null si es válido.
internal object ValidacionesAdmin {

    private val REGEX_EMAIL = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Deja solo dígitos y recorta al máximo (filtra pegados con letras o signos). */
    fun soloDigitos(valor: String, max: Int): String = valor.filter { it.isDigit() }.take(max)

    fun nombre(valor: String): String? =
        if (valor.isBlank()) "Ingresa el nombre completo." else null

    fun email(valor: String): String? = when {
        valor.isBlank() -> "Ingresa el correo electrónico."
        !REGEX_EMAIL.matches(valor.trim()) -> "El correo no tiene un formato válido (ej. nombre@correo.com)."
        else -> null
    }

    fun password(valor: String): String? = when {
        valor.isBlank() -> "Ingresa una contraseña."
        valor.length < 6 -> "La contraseña debe tener al menos 6 caracteres."
        else -> null
    }

    fun dni(valor: String): String? = when {
        valor.isBlank() -> "Ingresa el DNI."
        valor.length != 8 || !valor.all { it.isDigit() } -> "El DNI debe tener exactamente 8 dígitos."
        else -> null
    }

    fun edad(valor: String): String? {
        if (valor.isBlank()) return "Ingresa la edad."
        val n = valor.takeIf { v -> v.all { it.isDigit() } }?.toIntOrNull()
            ?: return "La edad debe contener solo dígitos."
        return if (n !in 1..120) "La edad debe estar entre 1 y 120." else null
    }

    fun contacto(valor: String): String? = when {
        valor.isBlank() -> "Ingresa el número de contacto."
        !valor.all { it.isDigit() } || valor.length !in 7..15 ->
            "El contacto debe tener solo dígitos (entre 7 y 15)."
        else -> null
    }

    /** Mensaje específico según la causa del fallo al crear/editar una cuenta. */
    fun mensajeDeErrorGuardado(error: Throwable): String = when (error) {
        is FirebaseAuthUserCollisionException -> "Ese correo ya está registrado en otra cuenta."
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil: usa al menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException -> "El correo no tiene un formato válido."
        is FirebaseNetworkException -> "Sin conexión a internet. Revisa tu red e intenta de nuevo."
        else -> "No se pudo guardar. Verifica los datos e intenta de nuevo."
    }
}
