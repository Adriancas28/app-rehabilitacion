package com.sanna.rehabapp.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.Usuario
import com.sanna.rehabapp.domain.repository.AdminRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private const val COLECCION_USUARIOS = "usuarios"
private const val APP_TEMPORAL_ADMIN = "app_admin_temporal"

class AdminRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestore: FirebaseFirestore,
) : AdminRepository {

    override fun observarPacientes(): Flow<List<Usuario>> = observarPorRol(Rol.PACIENTE)

    override fun observarFisioterapeutas(): Flow<List<Usuario>> = observarPorRol(Rol.FISIOTERAPEUTA)

    private fun observarPorRol(rol: Rol): Flow<List<Usuario>> = callbackFlow {
        val registro = firestore.collection(COLECCION_USUARIOS)
            .whereEqualTo("rol", rol.aFirestore())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.documents?.mapNotNull { it.toUsuario() } ?: emptyList())
            }
        awaitClose { registro.remove() }
    }

    override suspend fun crearPaciente(nombre: String, email: String, password: String): Result<Unit> =
        crearCuenta(nombre, email, password, Rol.PACIENTE)

    override suspend fun crearFisioterapeuta(nombre: String, email: String, password: String): Result<Unit> =
        crearCuenta(nombre, email, password, Rol.FISIOTERAPEUTA)

    // Instancia secundaria de FirebaseApp: createUserWithEmailAndPassword
    // inicia sesión automáticamente en la instancia donde se ejecuta, así
    // que usar la instancia por defecto cerraría la sesión del
    // administrador. Se crea la cuenta en una instancia aparte, se limpia,
    // y el documento en Firestore se escribe con el Firestore normal (bajo
    // la sesión real del admin, validada por las Security Rules).
    private suspend fun crearCuenta(
        nombre: String,
        email: String,
        password: String,
        rol: Rol,
    ): Result<Unit> = runCatching {
        val appTemporal = obtenerAppTemporal()
        val authTemporal = FirebaseAuth.getInstance(appTemporal)
        try {
            val resultado = authTemporal.createUserWithEmailAndPassword(email, password).await()
            val uid = checkNotNull(resultado.user?.uid) { "No se pudo crear el usuario" }

            val datos = mapOf(
                "nombre" to nombre,
                "email" to email,
                "rol" to rol.aFirestore(),
                "fechaRegistro" to FieldValue.serverTimestamp(),
            )
            firestore.collection(COLECCION_USUARIOS).document(uid).set(datos).await()
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
        firestore.collection(COLECCION_USUARIOS)
            .document(uid)
            .update(mapOf("nombre" to nombre, "email" to email))
            .await()
        Unit
    }

    override suspend fun eliminarUsuario(uid: String): Result<Unit> = runCatching {
        firestore.collection(COLECCION_USUARIOS).document(uid).delete().await()
        Unit
    }

    override suspend fun asignarFisioterapeuta(pacienteId: String, fisioterapeutaId: String): Result<Unit> =
        runCatching {
            firestore.collection(COLECCION_USUARIOS)
                .document(pacienteId)
                .update("fisioterapeutaId", fisioterapeutaId)
                .await()
            Unit
        }
}

private fun DocumentSnapshot.toUsuario(): Usuario? {
    if (!exists()) return null
    val rolStr = getString("rol") ?: return null
    return Usuario(
        uid = id,
        nombre = getString("nombre") ?: "",
        email = getString("email") ?: "",
        rol = Rol.desdeFirestore(rolStr),
        fisioterapeutaId = getString("fisioterapeutaId"),
        diagnostico = getString("diagnostico"),
        fechaRegistro = getDate("fechaRegistro"),
    )
}
