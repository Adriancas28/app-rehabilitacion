package com.sanna.rehabapp.core.navigation

object Rutas {
    const val RAIZ = "raiz"
    const val LOGIN = "login"
    const val CONSENTIMIENTO = "consentimiento"
    const val PACIENTES = "fisioterapeuta/pacientes"
    const val PACIENTE_DETALLE = "fisioterapeuta/pacientes/{pacienteId}"
    const val ARG_PACIENTE_ID = "pacienteId"

    const val EJERCICIOS = "fisioterapeuta/ejercicios"
    const val EJERCICIO_FORMULARIO = "fisioterapeuta/ejercicios/formulario?ejercicioId={ejercicioId}"
    const val ARG_EJERCICIO_ID = "ejercicioId"

    const val INICIO_PACIENTE = "paciente/inicio"

    fun pacienteDetalle(pacienteId: String): String = "fisioterapeuta/pacientes/$pacienteId"

    fun ejercicioFormulario(ejercicioId: String? = null): String =
        "fisioterapeuta/ejercicios/formulario" + if (ejercicioId != null) "?ejercicioId=$ejercicioId" else ""
}
