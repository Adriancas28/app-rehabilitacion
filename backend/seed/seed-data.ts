/**
 * Datos de prueba para desarrollo local.
 * Requiere: npm install firebase-admin
 * Uso: GOOGLE_APPLICATION_CREDENTIALS=./service-account.json ts-node seed-data.ts
 */
import * as admin from "firebase-admin";

admin.initializeApp();
const db = admin.firestore();

async function seed() {
  const fisioterapeutaId = "fisio-demo-01";
  const pacienteId = "paciente-demo-01";

  await db.collection("usuarios").doc(fisioterapeutaId).set({
    nombre: "Dra. Demo Fisioterapeuta",
    email: "fisio.demo@sanna.pe",
    rol: "fisioterapeuta",
    fechaRegistro: admin.firestore.FieldValue.serverTimestamp(),
  });

  await db.collection("usuarios").doc(pacienteId).set({
    nombre: "Paciente Demo",
    email: "paciente.demo@sanna.pe",
    rol: "paciente",
    fisioterapeutaId,
    fechaRegistro: admin.firestore.FieldValue.serverTimestamp(),
  });

  const ejercicioRef = db.collection("ejercicios").doc();
  await ejercicioRef.set({
    nombre: "Flexión de hombro",
    descripcion: "Elevar el brazo hacia adelante hasta 90 grados.",
    categoria: "MOVILIDAD",
    materialUrl: "",
    patronReferencia: { anguloMin: 80, anguloMax: 100 },
    creadoPor: fisioterapeutaId,
    fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
    activo: true,
  });

  await db
    .collection("usuarios")
    .doc(pacienteId)
    .collection("sesiones")
    .doc()
    .set({
      ejercicioId: ejercicioRef.id,
      fisioterapeutaId,
      fechaAsignacion: admin.firestore.FieldValue.serverTimestamp(),
      fechaEjecucion: null,
      estado: "pendiente",
      resultado: null,
      sincronizado: true,
    });

  console.log("Seed completado.");
}

seed().then(() => process.exit(0));
