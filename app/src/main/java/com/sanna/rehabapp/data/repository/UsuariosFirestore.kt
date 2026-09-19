package com.sanna.rehabapp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.sanna.rehabapp.domain.model.DiagnosticoRegistrado
import com.sanna.rehabapp.domain.model.Genero
import com.sanna.rehabapp.domain.model.LadoAfectado
import com.sanna.rehabapp.domain.model.Rol
import com.sanna.rehabapp.domain.model.TipoDiagnostico
import com.sanna.rehabapp.domain.model.Usuario
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

internal const val COL_USUARIOS = "usuarios"
internal const val COL_PACIENTES = "pacientes"
internal const val COL_FISIOTERAPEUTAS = "fisioterapeutas"
internal const val COL_DIAGNOSTICOS = "diagnosticos"
internal const val COL_EJERCICIOS = "ejercicios"
internal const val COL_SESIONES = "sesiones"
internal const val COL_OBSERVACIONES = "observaciones"

// Modelo de datos (DICCIONARIO_DE_DATOS_FIRESTORE, docs/modelo-datos): un
// usuario se guarda en tres lugares que comparten el MISMO id --
//   usuarios/{id}                          cuenta (correo, rol, nombre, fechaCreacion, activo)
//   pacientes/{id} | fisioterapeutas/{id}  datos propios del rol (herencia)
//   pacientes/{id}/diagnosticos/{x}        diagnósticos asignados (diagnosticoId, fecha)
// Esta clase los junta en el modelo de dominio `Usuario` y lo inverso, para
// que el resto de la app no sepa nada de esa separación.
@Singleton
class UsuariosFirestore @Inject constructor(
    private val firestore: FirebaseFirestore,
) {

    suspend fun leer(uid: String): Usuario? {
        val base = firestore.collection(COL_USUARIOS).document(uid).get().await()
        if (!base.exists()) return null
        val rol = Rol.desdeFirestore(base.getString("rol") ?: return null)
        return when (rol) {
            Rol.PACIENTE -> {
                val perfil = firestore.collection(COL_PACIENTES).document(uid).get().await()
                val diagnosticos = firestore.collection(COL_PACIENTES).document(uid)
                    .collection(COL_DIAGNOSTICOS).get().await().documents
                construir(base, perfil, diagnosticos)
            }
            Rol.FISIOTERAPEUTA -> {
                val perfil = firestore.collection(COL_FISIOTERAPEUTAS).document(uid).get().await()
                construir(base, perfil, emptyList())
            }
            Rol.ADMIN -> construir(base, null, emptyList())
        }
    }

    // Pacientes de un fisioterapeuta (o todos, si fisioterapeutaId es null,
    // para el administrador).
    fun observarPacientes(fisioterapeutaId: String? = null): Flow<List<Usuario>> {
        val perfiles = firestore.collection(COL_PACIENTES).let {
            if (fisioterapeutaId != null) it.whereEqualTo("fisioterapeutaId", fisioterapeutaId) else it
        }
        return combine(
            escuchar(firestore.collection(COL_USUARIOS).whereEqualTo("rol", Rol.PACIENTE.aFirestore())),
            escuchar(perfiles),
            escuchar(firestore.collectionGroup(COL_DIAGNOSTICOS)),
        ) { usuarios, perfilesSnap, diagnosticosSnap ->
            val basePorId = usuarios.documents.associateBy { it.id }
            val diagnosticosPorPaciente = diagnosticosSnap.documents.groupBy { it.reference.parent.parent?.id }
            perfilesSnap.documents.mapNotNull { perfil ->
                basePorId[perfil.id]?.let { base -> construir(base, perfil, diagnosticosPorPaciente[perfil.id].orEmpty()) }
            }
        }
    }

    fun observarFisioterapeutas(): Flow<List<Usuario>> = combine(
        escuchar(firestore.collection(COL_USUARIOS).whereEqualTo("rol", Rol.FISIOTERAPEUTA.aFirestore())),
        escuchar(firestore.collection(COL_FISIOTERAPEUTAS)),
    ) { usuarios, perfilesSnap ->
        val perfilPorId = perfilesSnap.documents.associateBy { it.id }
        usuarios.documents.mapNotNull { base -> construir(base, perfilPorId[base.id], emptyList()) }
    }

    // Deja los diagnósticos del paciente exactamente como `deseados`: conserva
    // (con su fecha original) los que ya estaban y siguen, quita los que ya no
    // están y agrega los nuevos. Un diagnóstico que llega con `fecha` (la UI
    // del fisioterapeuta la calcula) usa esa fecha; sin fecha, la de hoy.
    suspend fun reemplazarDiagnosticos(pacienteId: String, deseados: List<DiagnosticoRegistrado>) {
        val coleccion = firestore.collection(COL_PACIENTES).document(pacienteId).collection(COL_DIAGNOSTICOS)
        val existentes = coleccion.get().await().documents
        val existentePorTipo = existentes.associateBy { it.getString("diagnosticoId") }
        val batch = firestore.batch()
        val tiposDeseados = deseados.map { it.tipo.aFirestore() }.toSet()
        existentes.filter { it.getString("diagnosticoId") !in tiposDeseados }.forEach { batch.delete(it.reference) }
        deseados.forEach { deseado ->
            val actual = existentePorTipo[deseado.tipo.aFirestore()]
            if (actual != null && deseado.fecha == null) return@forEach
            val referencia = actual?.reference ?: coleccion.document()
            batch.set(
                referencia,
                mapOf(
                    "diagnosticoId" to deseado.tipo.aFirestore(),
                    "fecha" to Timestamp(deseado.fecha ?: java.util.Date()),
                ),
            )
        }
        batch.commit().await()
    }

    private fun construir(base: DocumentSnapshot, perfil: DocumentSnapshot?, diagnosticos: List<DocumentSnapshot>): Usuario? {
        val rol = Rol.desdeFirestore(base.getString("rol") ?: return null)
        val p = perfil?.takeIf { it.exists() }
        return Usuario(
            uid = base.id,
            nombre = base.getString("nombre") ?: "",
            email = base.getString("correo") ?: "",
            rol = rol,
            fisioterapeutaId = p?.getString("fisioterapeutaId"),
            diagnosticos = diagnosticos.mapNotNull { doc ->
                val tipo = TipoDiagnostico.desdeFirestoreOrNull(doc.getString("diagnosticoId")) ?: return@mapNotNull null
                DiagnosticoRegistrado(tipo = tipo, fecha = doc.getDate("fecha"))
            },
            dni = p?.getString("dni"),
            edad = (p?.get("edad") as? Number)?.toInt(),
            fechaRegistro = base.getDate("fechaCreacion"),
            ladoAfectado = LadoAfectado.desdeFirestoreOrNull(p?.getString("ladoAfectado")) ?: LadoAfectado.DERECHO,
            genero = Genero.desdeFirestoreOrNull(p?.getString("genero")),
            numeroContacto = p?.getString("contacto"),
            especialidad = p?.getString("especialidad"),
            numeroColegiatura = p?.getString("numeroColegiatura"),
            activo = base.getBoolean("activo") ?: true,
        )
    }
}

internal fun escuchar(consulta: Query): Flow<QuerySnapshot> = callbackFlow {
    val registro = consulta.addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
            return@addSnapshotListener
        }
        if (snapshot != null) trySend(snapshot)
    }
    awaitClose { registro.remove() }
}
