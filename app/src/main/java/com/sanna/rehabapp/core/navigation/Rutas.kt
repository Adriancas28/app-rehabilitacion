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

    const val SESION_FORMULARIO = "fisioterapeuta/pacientes/{pacienteId}/sesiones/formulario?sesionId={sesionId}"
    const val ARG_SESION_ID = "sesionId"

    const val INICIO_PACIENTE = "paciente/inicio"
    const val DETALLE_EJERCICIO_ASIGNADO = "paciente/ejercicios/{sesionId}"
    const val EJECUTAR_SESION = "paciente/ejercicios/{sesionId}/ejecutar"
    const val HISTORIAL_SESIONES = "paciente/historial"
    const val RESULTADO_SESION = "paciente/historial/{sesionId}"

    const val ADMIN_PACIENTES = "admin/pacientes"
    const val ADMIN_PACIENTE_FORMULARIO = "admin/pacientes/formulario?usuarioId={usuarioId}"
    const val ADMIN_FISIOTERAPEUTAS = "admin/fisioterapeutas"
    const val ADMIN_FISIOTERAPEUTA_FORMULARIO = "admin/fisioterapeutas/formulario?usuarioId={usuarioId}"
    const val ARG_ADMIN_USUARIO_ID = "usuarioId"

    fun pacienteDetalle(pacienteId: String): String = "fisioterapeuta/pacientes/$pacienteId"

    fun ejercicioFormulario(ejercicioId: String? = null): String =
        "fisioterapeuta/ejercicios/formulario" + if (ejercicioId != null) "?ejercicioId=$ejercicioId" else ""

    fun sesionFormulario(pacienteId: String, sesionId: String? = null): String =
        "fisioterapeuta/pacientes/$pacienteId/sesiones/formulario" +
            if (sesionId != null) "?sesionId=$sesionId" else ""

    fun detalleEjercicioAsignado(sesionId: String): String = "paciente/ejercicios/$sesionId"

    fun ejecutarSesion(sesionId: String): String = "paciente/ejercicios/$sesionId/ejecutar"

    fun resultadoSesion(sesionId: String): String = "paciente/historial/$sesionId"

    fun adminPacienteFormulario(usuarioId: String? = null): String =
        "admin/pacientes/formulario" + if (usuarioId != null) "?usuarioId=$usuarioId" else ""

    fun adminFisioterapeutaFormulario(usuarioId: String? = null): String =
        "admin/fisioterapeutas/formulario" + if (usuarioId != null) "?usuarioId=$usuarioId" else ""
}
