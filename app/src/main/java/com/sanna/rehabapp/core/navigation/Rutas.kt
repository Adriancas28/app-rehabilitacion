package com.sanna.rehabapp.core.navigation

object Rutas {
    const val RAIZ = "raiz"
    const val LOGIN = "login"
    const val CONSENTIMIENTO = "consentimiento"
    const val PACIENTES = "fisioterapeuta/pacientes"
    const val PACIENTE_DETALLE = "fisioterapeuta/pacientes/{pacienteId}"
    const val ARG_PACIENTE_ID = "pacienteId"

    // HU18-CA01/CA03 (Sprint 5) — vista agregada de sesiones/resultados de
    // todos los pacientes del fisioterapeuta, con filtros.
    const val RESULTADOS = "fisioterapeuta/resultados"

    const val EJERCICIOS = "fisioterapeuta/ejercicios"
    const val EJERCICIO_FORMULARIO = "fisioterapeuta/ejercicios/formulario?ejercicioId={ejercicioId}"
    const val ARG_EJERCICIO_ID = "ejercicioId"

    const val SESION_FORMULARIO = "fisioterapeuta/pacientes/{pacienteId}/sesiones/formulario?sesionId={sesionId}"
    const val ARG_SESION_ID = "sesionId"

    // HU18-CA02/CA04 — el fisioterapeuta ve el detalle de una sesión ya
    // completada (a diferencia de la ruta del paciente, necesita el
    // pacienteId explícito: no hay un "dueño autenticado" implícito).
    const val FISIO_RESULTADO_SESION = "fisioterapeuta/pacientes/{pacienteId}/sesiones/{sesionId}/resultado"

    // HU15 — registrar/editar/eliminar recomendaciones sobre una sesión.
    const val RECOMENDACIONES = "fisioterapeuta/pacientes/{pacienteId}/sesiones/{sesionId}/recomendaciones"

    const val INICIO_PACIENTE = "paciente/inicio"
    const val DETALLE_EJERCICIO_ASIGNADO = "paciente/ejercicios/{sesionId}"
    const val EJECUTAR_SESION = "paciente/ejercicios/{sesionId}/ejecutar"
    const val HISTORIAL_SESIONES = "paciente/historial"
    const val RESULTADO_SESION = "paciente/historial/{sesionId}"

    // HU22/HU23 — Perfil de cuenta propia; "Cerrar sesión" vive únicamente
    // aquí (una ruta por rol porque cada pantalla de Perfil recibe
    // parámetros de navegación distintos: el fisioterapeuta la ve como una
    // pestaña más de su barra lateral, el paciente como una pantalla de
    // detalle con botón atrás).
    const val PERFIL_PACIENTE = "paciente/perfil"
    const val PERFIL_FISIOTERAPEUTA = "fisioterapeuta/perfil"

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

    fun fisioResultadoSesion(pacienteId: String, sesionId: String): String =
        "fisioterapeuta/pacientes/$pacienteId/sesiones/$sesionId/resultado"

    fun recomendaciones(pacienteId: String, sesionId: String): String =
        "fisioterapeuta/pacientes/$pacienteId/sesiones/$sesionId/recomendaciones"

    fun detalleEjercicioAsignado(sesionId: String): String = "paciente/ejercicios/$sesionId"

    fun ejecutarSesion(sesionId: String): String = "paciente/ejercicios/$sesionId/ejecutar"

    fun resultadoSesion(sesionId: String): String = "paciente/historial/$sesionId"

    fun adminPacienteFormulario(usuarioId: String? = null): String =
        "admin/pacientes/formulario" + if (usuarioId != null) "?usuarioId=$usuarioId" else ""

    fun adminFisioterapeutaFormulario(usuarioId: String? = null): String =
        "admin/fisioterapeutas/formulario" + if (usuarioId != null) "?usuarioId=$usuarioId" else ""
}
