package com.sanna.rehabapp.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sanna.rehabapp.domain.model.DiagnosticoRegistrado
import com.sanna.rehabapp.domain.model.Genero
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val APP_TEMPORAL_ADMIN = "app_admin_temporal"

// Cuentas según el diccionario de datos: usuarios/{uid} (cuenta) +
// pacientes/{uid} | fisioterapeutas/{uid} (datos del rol, mismo id) +
// pacientes/{uid}/diagnosticos. Ver UsuariosFirestore.
class AdminRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
    private val usuarios: UsuariosFirestore,
) : AdminRepository {

    override fun observarPacientes(): Flow<List<Usuario>> = usuarios.observarPacientes(null)

    override fun observarFisioterapeutas(): Flow<List<Usuario>> = usuarios.observarFisioterapeutas()

    override suspend fun crearPaciente(
        nombre: String,
        email: String,
        password: String,
        dni: String,
        edad: Int,
        diagnosticos: List<TipoDiagnostico>,
        ladoAfectado: LadoAfectado,
        genero: Genero,
        numeroContacto: String,
    ): Result<Unit> = crearCuenta(nombre, email, password, Rol.PACIENTE) { uid, batch ->
        batch.set(
            firestore.collection(COL_PACIENTES).document(uid),
            mapOf(
                "dni" to dni,
                "edad" to edad,
                "genero" to genero.aFirestore(),
                "contacto" to numeroContacto,
                "ladoAfectado" to ladoAfectado.aFirestore(),
            ),
        )
        diagnosticos.forEach { diagnostico ->
            batch.set(
                firestore.collection(COL_PACIENTES).document(uid).collection(COL_DIAGNOSTICOS).document(),
                mapOf("diagnosticoId" to diagnostico.aFirestore(), "fecha" to Timestamp.now()),
            )
        }
    }

    override suspend fun crearFisioterapeuta(
        nombre: String,
        email: String,
        password: String,
        edad: Int,
        genero: Genero,
        numeroContacto: String,
        especialidad: String?,
        numeroColegiatura: String?,
    ): Result<Unit> = crearCuenta(nombre, email, password, Rol.FISIOTERAPEUTA) { uid, batch ->
        batch.set(
            firestore.collection(COL_FISIOTERAPEUTAS).document(uid),
            buildMap {
                put("edad", edad)
                put("genero", genero.aFirestore())
                put("contacto", numeroContacto)
                if (!especialidad.isNullOrBlank()) put("especialidad", especialidad)
                if (!numeroColegiatura.isNullOrBlank()) put("numeroColegiatura", numeroColegiatura)
            },
        )
    }

    // Instancia secundaria de FirebaseApp: createUserWithEmailAndPassword
    // inicia sesión automáticamente en la instancia donde se ejecuta, así
    // que usar la instancia por defecto cerraría la sesión del
    // administrador. Se crea la cuenta en una instancia aparte, se limpia,
    // y los documentos en Firestore se escriben (en un solo batch atómico)
    // con el Firestore normal, bajo la sesión real del admin.
    private suspend fun crearCuenta(
        nombre: String,
        email: String,
        password: String,
        rol: Rol,
        escribirPerfil: (uid: String, batch: com.google.firebase.firestore.WriteBatch) -> Unit,
    ): Result<Unit> = runCatching {
        val appTemporal = obtenerAppTemporal()
        val authTemporal = FirebaseAuth.getInstance(appTemporal)
        try {
            val resultado = authTemporal.createUserWithEmailAndPassword(email, password).await()
            val uid = checkNotNull(resultado.user?.uid) { "No se pudo crear el usuario" }

            val batch = firestore.batch()
            batch.set(
                firestore.collection(COL_USUARIOS).document(uid),
                mapOf(
                    "nombre" to nombre,
                    "correo" to email,
                    "rol" to rol.aFirestore(),
                    "fechaCreacion" to FieldValue.serverTimestamp(),
                    "activo" to true,
                ),
            )
            escribirPerfil(uid, batch)
            batch.commit().await()
            Unit
        } finally {
            authTemporal.signOut()
            appTemporal.delete()
        }
    }

    private fun obtenerAppTemporal(): FirebaseApp = try {
        FirebaseApp.getInstance(APP_TEMPORAL_ADMIN)
    } catch (e: IllegalStateException) {
        FirebaseApp.initializeApp(context, FirebaseApp.getInstance().options, APP_TEMPORAL_ADMIN)
    }

    override suspend fun actualizarUsuario(uid: String, nombre: String, email: String): Result<Unit> = runCatching {
        firestore.collection(COL_USUARIOS)
            .document(uid)
            .update(mapOf("nombre" to nombre, "correo" to email))
            .await()
        Unit
    }

    override suspend fun actualizarPaciente(
        uid: String,
        nombre: String,
        email: String,
        dni: String,
        edad: Int,
        diagnosticos: List<TipoDiagnostico>,
        ladoAfectado: LadoAfectado,
        genero: Genero,
        numeroContacto: String,
    ): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.update(firestore.collection(COL_USUARIOS).document(uid), mapOf("nombre" to nombre, "correo" to email))
        batch.set(
            firestore.collection(COL_PACIENTES).document(uid),
            mapOf(
                "dni" to dni,
                "edad" to edad,
                "genero" to genero.aFirestore(),
                "contacto" to numeroContacto,
                "ladoAfectado" to ladoAfectado.aFirestore(),
            ),
            SetOptions.merge(),
        )
        batch.commit().await()
        usuarios.reemplazarDiagnosticos(uid, diagnosticos.map { DiagnosticoRegistrado(tipo = it, fecha = null) })
    }

    override suspend fun actualizarFisioterapeuta(
        uid: String,
        nombre: String,
        email: String,
        edad: Int,
        genero: Genero,
        numeroContacto: String,
        especialidad: String?,
        numeroColegiatura: String?,
    ): Result<Unit> = runCatching {
        val batch = firestore.batch()
        batch.update(firestore.collection(COL_USUARIOS).document(uid), mapOf("nombre" to nombre, "correo" to email))
        batch.set(
            firestore.collection(COL_FISIOTERAPEUTAS).document(uid),
            mapOf(
                "edad" to edad,
                "genero" to genero.aFirestore(),
                "contacto" to numeroContacto,
                "especialidad" to (especialidad ?: ""),
                "numeroColegiatura" to (numeroColegiatura ?: ""),
            ),
            SetOptions.merge(),
        )
        batch.commit().await()
        Unit
    }

    override suspend fun cambiarEstadoActivoUsuario(uid: String, activo: Boolean): Result<Unit> = runCatching {
        firestore.collection(COL_USUARIOS).document(uid).update("activo", activo).await()
        Unit
    }

    override suspend fun eliminarUsuario(uid: String): Result<Unit> = runCatching {
        val diagnosticos = firestore.collection(COL_PACIENTES).document(uid)
            .collection(COL_DIAGNOSTICOS).get().await().documents
        val batch = firestore.batch()
        diagnosticos.forEach { batch.delete(it.reference) }
        batch.delete(firestore.collection(COL_PACIENTES).document(uid))
        batch.delete(firestore.collection(COL_FISIOTERAPEUTAS).document(uid))
        batch.delete(firestore.collection(COL_USUARIOS).document(uid))
        batch.commit().await()
        Unit
    }

    override suspend fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String): Result<Unit> =
        runCatching {
            firestore.collection(COL_PACIENTES)
                .document(pacienteId)
                .update("fisioterapeutaId", fisioterapeutaId)
                .await()
            Unit
        }
}
