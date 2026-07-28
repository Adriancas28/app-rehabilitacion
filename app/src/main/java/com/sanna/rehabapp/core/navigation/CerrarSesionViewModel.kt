package com.sanna.rehabapp.core.navigation

import androidx.lifecycle.ViewModel
import com.sanna.rehabapp.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// RNF02-CA04: cierre de sesión, compartido por ambos grafos de rol.
@HiltViewModel
class CerrarSesionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun cerrarSesion() = authRepository.logout()
}
