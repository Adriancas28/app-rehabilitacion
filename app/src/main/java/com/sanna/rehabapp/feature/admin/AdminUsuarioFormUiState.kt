package com.sanna.rehabapp.feature.admin

// Compartido por el formulario de paciente y el de fisioterapeuta del
// panel de administrador (HU20/HU21) — misma forma, distinto repositorio
// detrás (AdminRepository.crearPaciente vs. crearFisioterapeuta).
data class AdminUsuarioFormUiState(
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val cargando: Boolean = false,
    val guardando: Boolean = false,
    val error: String? = null,
    val guardadoExitoso: Boolean = false,
)
