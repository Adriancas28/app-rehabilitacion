const admin = require("firebase-admin");
admin.initializeApp({ credential: admin.credential.cert(require("../service-account.json")) });
const db = admin.firestore();

async function main() {
  const pacienteId = "oSBlO03R6OVDCJxhCt9NV2VFKWn1"; // Juan Pérez (prueba)
  const ejercicioId = "mKsrvGjkRNcAq6zc7mcS"; // Flexión de hombro (SDS-02)
  const fisioterapeutaId = "zqUil6LgYggtidJPfJ8epgD6hAw2";

  const ref = await db.collection("usuarios").doc(pacienteId).collection("sesiones").add({
    ejercicioId,
    fisioterapeutaId,
    fechaAsignacion: admin.firestore.Timestamp.now(),
    fechaEjecucion: admin.firestore.Timestamp.now(),
    estado: "completada",
    sincronizado: true,
    resultado: {
      porcentajeEjecucion: 86.5,
      desviacionPromedio: 3.2,
      angulosDetectados: [
        { articulacion: "Hombro derecho", anguloDetectado: 104.3, anguloEsperado: 110, desviacion: 5.7 },
      ],
      erroresDetectados: [
        { tipo: "Rango incompleto", articulacion: "Hombro derecho", repeticiones: 1 },
      ],
      repeticionesAsignadas: 3,
      repeticionesCompletadas: 3,
      repeticionesCorrectas: 2,
      detallePorRepeticion: [
        { numero: 1, porcentajeEjecucion: 79.0, errores: [{ tipo: "Rango incompleto", articulacion: "Hombro derecho", repeticiones: 1 }] },
        { numero: 2, porcentajeEjecucion: 91.0, errores: [] },
        { numero: 3, porcentajeEjecucion: 89.5, errores: [] },
      ],
    },
  });
  console.log("Sesion demo creada:", ref.id);
}

main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
