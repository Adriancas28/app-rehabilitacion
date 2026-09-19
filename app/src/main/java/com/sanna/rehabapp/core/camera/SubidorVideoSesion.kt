package com.sanna.rehabapp.core.camera

import com.sanna.rehabapp.domain.repository.SesionRepository
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

// Parte 3 (video) — la subida vive en un alcance de aplicación, no en el
// del ViewModel de la sesión: al terminar la sesión el paciente navega de
// inmediato a su resultado y ese ViewModel se destruye; una subida atada a
// él se cancelaría a medias.
@Singleton
class SubidorVideoSesion @Inject constructor(
    private val sesionRepository: SesionRepository,
) {
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun subir(pacienteId: String, sesionId: String, archivo: File) {
        alcance.launch {
            val resultado = sesionRepository.subirVideoSesion(pacienteId, sesionId, archivo)
            if (resultado.isSuccess) archivo.delete()
        }
    }
}
