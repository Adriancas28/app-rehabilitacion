/**
 * Siembra datos FICTICIOS en los emuladores locales de Firebase (Auth + Firestore).
 *
 * SOLO funciona contra el emulador: se niega a ejecutarse si no estan definidas
 * FIRESTORE_EMULATOR_HOST y FIREBASE_AUTH_EMULATOR_HOST apuntando a localhost, y no
 * carga ninguna credencial (no usa service-account.json), asi que no puede alcanzar
 * el proyecto Firebase real.
 *
 * Uso (desde /backend, con los emuladores corriendo): ver backend/emuladores/README.md
 *
 * Cuentas ficticias (no existen en el proyecto real):
 *   fisio.prueba@local.test   / PruebaLocal123   (fisioterapeuta)
 *   elmer.prueba@local.test   / PruebaLocal123   (paciente asignado a ese fisio)
 */
const admin = require("firebase-admin");

const esLocal = (v) => typeof v === "string" && /^(localhost|127\.0\.0\.1)(:\d+)?$/.test(v);
if (!esLocal(process.env.FIRESTORE_EMULATOR_HOST) || !esLocal(process.env.FIREBASE_AUTH_EMULATOR_HOST)) {
  console.error("Abortado: define FIRESTORE_EMULATOR_HOST y FIREBASE_AUTH_EMULATOR_HOST con localhost/127.0.0.1.");
  process.exit(1);
}
if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.error("Abortado: hay GOOGLE_APPLICATION_CREDENTIALS definida; quitala para sembrar solo el emulador.");
  process.exit(1);
}

// Sin credenciales: el Admin SDK habla con el emulador.
admin.initializeApp({ projectId: "app-rehabilitacion-terapeutica" });
const db = admin.firestore();
const PASSWORD = "PruebaLocal123";

async function crearUsuarioAuth(nombre, email) {
  const u = await admin.auth().createUser({ email, password: PASSWORD, displayName: nombre });
  return u.uid;
}

(async () => {
  const ahora = admin.firestore.Timestamp.now();

  const fisioId = await crearUsuarioAuth("Fisio Prueba", "fisio.prueba@local.test");
  await db.collection("usuarios").doc(fisioId).set({
    nombre: "Fisio Prueba", correo: "fisio.prueba@local.test", rol: "fisioterapeuta", fechaCreacion: ahora, activo: true,
  });
  await db.collection("fisioterapeutas").doc(fisioId).set({
    edad: 35, genero: "Femenino", contacto: "000000000", especialidad: "Fisioterapia (ficticio)",
  });

  const elmerId = await crearUsuarioAuth("Elmer Prueba", "elmer.prueba@local.test");
  await db.collection("usuarios").doc(elmerId).set({
    nombre: "Elmer Prueba", correo: "elmer.prueba@local.test", rol: "paciente", fechaCreacion: ahora, activo: true,
  });
  await db.collection("pacientes").doc(elmerId).set({
    dni: "00000000", edad: 30, genero: "Masculino", contacto: "000000000", ladoAfectado: "Derecho", fisioterapeutaId: fisioId,
  });
  await db.collection("pacientes").doc(elmerId).collection("diagnosticos").add({
    diagnosticoId: "SINDROME_DOLOR_SUBACROMIAL", fecha: ahora,
  });

  const ejercicio = await db.collection("ejercicios").add({
    nombre: "Abducción de hombro",
    descripcion: "De pie, de frente a la cámara, eleva el brazo hacia el costado hasta la altura del hombro y regresa de forma controlada.",
    fisioterapeutaId: fisioId,
    categoria: "Movilidad",
    angulosReferencia: { HOMBRO_DERECHO: { min: 70, max: 110 } },
    repeticiones: { cantidad: 3, duracionSeg: 10 },
    diagnosticosAplicables: ["SINDROME_DOLOR_SUBACROMIAL"],
    activo: true,
    fechaCreacion: ahora,
    videoPath: "",
  });

  const acciones = (n) => [
    { ordenAccion: 1, anguloMaximo: 90 + n, completada: true, tiempoSeg: 4, desviacionAngular: 0 },
    { ordenAccion: 2, anguloMaximo: 15, completada: true, tiempoSeg: 2, desviacionAngular: 0 },
  ];
  const sesion = await db.collection("sesiones").add({
    pacienteId: elmerId,
    ejercicioId: ejercicio.id,
    fisioterapeutaId: fisioId,
    fechaAsignacion: ahora,
    fechaEjecucion: ahora,
    estado: "completada",
    nota: "Mantén el codo estirado (dato ficticio).",
    porcentajeEjecucion: 100,
    desviacionPromedio: 0,
    repeticionesAsignadas: 3,
    repeticionesCompletadas: 3,
    repeticionesCorrectas: 3,
    duracionMedidaSeg: 19,
    repeticiones: [1, 2, 3].map((n) => ({
      numero: n, porcentaje: 100, desviacionAngular: 0, error: null, articulacion: null, acciones: acciones(n), errores: [],
    })),
  });

  console.log("Datos ficticios sembrados en el emulador.");
  console.log("  fisioterapeuta:", fisioId, "| paciente:", elmerId, "| sesion:", sesion.id);
})().catch((e) => { console.error(e.message); process.exit(1); });
