package com.sanna.rehabapp.domain.model

import java.util.Date

data class Usuario(
    val uid: String,
    val nombre: String,
    val email: String,
    val rol: Rol,
    val fisioterapeutaId: String? = null,
    // HU01-CA06 (ampliación): solo aplica si rol == PACIENTE; puede tener
    // varios a la vez, cada uno de un catálogo cerrado (TipoDiagnostico),
    // no texto libre.
    val diagnosticos: List<DiagnosticoRegistrado> = emptyList(),
    // HU20-CA02 (revisión): datos adicionales del paciente que el
    // administrador captura al registrarlo. dni solo aplica si rol ==
    // PACIENTE; edad aplica a ambos roles desde la actualización del
    // modelo de datos (HU21/HU23 — el fisioterapeuta también registra su
    // edad).
    val dni: String? = null,
    val edad: Int? = null,
    val fechaRegistro: Date? = null,
    // HU20 (ampliación): lado del cuerpo afectado, indicado por el
    // administrador al registrar/editar al paciente. Solo aplica si
    // rol == PACIENTE; el monitoreo (HU07/HU08) lo usa para saber qué lado
    // medir en vez de asumir siempre el derecho.
    val ladoAfectado: LadoAfectado = LadoAfectado.DERECHO,
    // Actualización del modelo de datos (HU20/HU21/HU22/HU23): datos de
    // contacto, aplican a ambos roles.
    val genero: Genero? = null,
    val numeroContacto: String? = null,
    // Solo aplican si rol == FISIOTERAPEUTA; especialidad y colegiatura son
    // opcionales incluso para ese rol (HU21-CA02).
    val especialidad: String? = null,
    val numeroColegiatura: String? = null,
    // Recomendación del modelo E-R-SANNA: desactivar una cuenta en vez de
    // (o además de) eliminarla — reversible, y evita el problema conocido
    // de que eliminar solo borra el documento de Firestore, dejando el
    // registro de Firebase Auth huérfano (ver HU20/HU21, "limitación
    // conocida"). Una cuenta con activo = false se desloguea sola al
    // entrar (RaizViewModel), igual que si no tuviera documento.
    val activo: Boolean = true,
)
