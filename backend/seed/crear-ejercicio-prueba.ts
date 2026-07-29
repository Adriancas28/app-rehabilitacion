/**
 * Crea (o reemplaza) un ejercicio de prueba con un video de 10s como
 * material terapéutico, para probar HU05/HU06 sin tener que subir un
 * archivo manualmente desde la app.
 *
 * Requiere: GOOGLE_APPLICATION_CREDENTIALS apuntando a la service account.
 *
 * Uso:
 *   npx ts-node seed/crear-ejercicio-prueba.ts --video "C:/ruta/video.mp4" \
 *     --creadoPorEmail ana.ruiz@sanna.pe
 */
import * as admin from "firebase-admin";
import * as crypto from "crypto";

const BUCKET = "app-rehabilitacion-terapeutica.firebasestorage.app";

interface Argumentos {
  video: string;
  creadoPorEmail: string;
}

function parsearArgumentos(argv: string[]): Argumentos {
  const valores: Record<string, string> = {};
  for (let i = 0; i < argv.length; i += 1) {
    const actual = argv[i];
    if (actual.startsWith("--")) {
      valores[actual.slice(2)] = argv[i + 1];
      i += 1;
    }
  }
  const { video, creadoPorEmail } = valores;
  if (!video) {
    throw new Error('Uso: ts-node crear-ejercicio-prueba.ts --video "ruta.mp4" [--creadoPorEmail correo]');
  }
  return { video, creadoPorEmail: creadoPorEmail ?? "ana.ruiz@sanna.pe" };
}

async function crearEjercicioPrueba(argumentos: Argumentos): Promise<void> {
  admin.initializeApp({ storageBucket: BUCKET });

  const fisio = await admin.auth().getUserByEmail(argumentos.creadoPorEmail);

  const firestore = admin.firestore();
  const ejercicioRef = firestore.collection("ejercicios").doc();

  const token = crypto.randomUUID();
  const rutaEnStorage = `ejercicios/${ejercicioRef.id}/video-prueba.mp4`;
  await admin.storage().bucket().upload(argumentos.video, {
    destination: rutaEnStorage,
    metadata: {
      contentType: "video/mp4",
      metadata: { firebaseStorageDownloadTokens: token },
    },
  });
  const materialUrl =
    `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/` +
    `${encodeURIComponent(rutaEnStorage)}?alt=media&token=${token}`;

  await ejercicioRef.set({
    nombre: "Flexión de rodilla (prueba)",
    descripcion: "Ejercicio de prueba generado para probar el video de material terapéutico (HU05) y la ejecución con cámara (HU06/07/08). Flexiona y extiende la rodilla derecha durante la sesión.",
    categoria: "Rodilla",
    materialUrl,
    duracionSegundos: 10,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 90, anguloMax: 160 }],
    creadoPor: fisio.uid,
    fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
    activo: true,
  });

  console.log("Ejercicio de prueba creado:");
  console.log(`  id:          ${ejercicioRef.id}`);
  console.log(`  materialUrl: ${materialUrl}`);
  console.log(`  creadoPor:   ${argumentos.creadoPorEmail} (${fisio.uid})`);
}

crearEjercicioPrueba(parsearArgumentos(process.argv.slice(2)))
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Error al crear el ejercicio de prueba:", error?.message ?? error);
    process.exit(1);
  });
