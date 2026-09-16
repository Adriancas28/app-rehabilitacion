/**
 * Crea (o reemplaza, por ID) los ejercicios de la nueva Biblioteca de
 * Ejercicios (92 ejercicios / 35 MVP, ver Diseno_Biblioteca_Ejercicios_Tecnico.docx
 * y Biblioteca_Ejercicios_Rehabilitacion.xlsx) -- catalogo SEPARADO del de
 * 9 ejercicios de crear-catalogo-ejercicios.ts, para no mezclar ambos.
 *
 * Diferencia clave respecto a los scripts anteriores: el documento de
 * Firestore usa como ID el codigo del catalogo maestro (ej. "HOM-01"), no
 * un ID autogenerado -- es el mismo identificador que conecta Blender,
 * Firebase Storage y este seed (ver Guia_Oficial_Produccion_Multimedia.docx,
 * "Consideraciones finales").
 *
 * El ROM (patronesReferencia) de cada ejercicio NO lo escribe a mano el
 * fisioterapeuta ni lo calcula desde la app (HU02-CA07 sigue existiendo,
 * pero es para ejercicios que el fisio registre el mismo) -- para este
 * catalogo predeterminado, el ROM ya viene calculado de antemano corriendo
 * calcular_rom_video.py (mismo algoritmo que AnalizadorVideoReferencia.kt)
 * sobre el video ya producido en Blender, y se pega directo aqui.
 *
 * Requiere: GOOGLE_APPLICATION_CREDENTIALS apuntando a la service account.
 *
 * Uso:
 *   npx ts-node seed/crear-biblioteca-ejercicios.ts --creadoPorEmail ana.ruiz@sanna.pe
 */
import * as admin from "firebase-admin";
import * as crypto from "crypto";
import * as fs from "fs";

const BUCKET = "app-rehabilitacion-terapeutica.firebasestorage.app";

interface Argumentos {
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
  return { creadoPorEmail: valores.creadoPorEmail ?? "ana.ruiz@sanna.pe" };
}

interface PatronReferencia {
  articulacion: string;
  anguloMin: number;
  anguloMax: number;
}

// Campos que YA existen en el modelo Ejercicio de la app (CLAUDE.md seccion 5).
// categoriaSlug/rutaVideoLocal/rutaThumbLocal son solo para este script (no
// se guardan en Firestore tal cual) -- resuelven donde subir el material.
interface EjercicioBiblioteca {
  id: string; // codigo del catalogo maestro, ej. "HOM-01" -- es el ID del documento
  nombre: string;
  descripcion: string;
  categoria: "MOVILIDAD" | "CONTROL_MOTOR";
  duracionSegundos: number;
  repeticiones: number;
  patronesReferencia: PatronReferencia[];
  diagnosticosAplicables: string[];
  categoriaSlug: string; // biblioteca_ejercicios/{categoriaSlug}/{id}/... en Storage
  rutaVideoLocal: string;
  rutaThumbLocal: string;
}

// HOM-01: unico ejercicio producido hasta el momento. Se agrega uno mas a
// este arreglo por cada ejercicio que se termine de producir (Blender +
// ffmpeg + calcular_rom_video.py), sin tocar el resto del script.
const BIBLIOTECA: EjercicioBiblioteca[] = [
  {
    id: "HOM-01",
    nombre: "Flexión de hombro",
    descripcion: "De pie, de frente a la cámara, eleva cada brazo (uno a la vez) hacia adelante hasta la altura del hombro y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 4, // ~1 ciclo de 3.5s del video 3D (ambos brazos, uno por uno), redondeado
    repeticiones: 3,
    // Calculado con calcular_rom_video.py sobre HOM-01_flexion_hombro_3d_v1.mp4
    // (mismo algoritmo/modelo/margen que usa la app, no escrito a mano).
    // Version 2: camara frontal (antes perfil) -- el angulo hombro-codo-cadera
    // se ve distinto de frente que de perfil, por eso el rango cambio.
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 8.2, anguloMax: 136.0 }],
    diagnosticosAplicables: [
      "PINZAMIENTO_SUBACROMIAL",
      "CAPSULITIS_ADHESIVA",
      "POST_QUIRURGICO_MANGUITO_ROTADOR",
    ],
    categoriaSlug: "hombro",
    rutaVideoLocal: "D:\\biblioteca_ejercicios\\hombro\\HOM-01\\HOM-01_flexion_hombro_3d_v1.mp4",
    rutaThumbLocal: "D:\\biblioteca_ejercicios\\hombro\\HOM-01\\HOM-01_flexion_hombro_3d_v1_thumb.webp",
  },
];

async function subirArchivo(rutaLocal: string, rutaEnStorage: string, contentType: string): Promise<string> {
  const token = crypto.randomUUID();
  await admin.storage().bucket().upload(rutaLocal, {
    destination: rutaEnStorage,
    metadata: {
      contentType,
      metadata: { firebaseStorageDownloadTokens: token },
    },
  });
  return (
    `https://firebasestorage.googleapis.com/v0/b/${BUCKET}/o/` +
    `${encodeURIComponent(rutaEnStorage)}?alt=media&token=${token}`
  );
}

async function crearBiblioteca(argumentos: Argumentos): Promise<void> {
  admin.initializeApp({ storageBucket: BUCKET });

  const fisio = await admin.auth().getUserByEmail(argumentos.creadoPorEmail);
  const firestore = admin.firestore();

  for (const ejercicio of BIBLIOTECA) {
    if (!fs.existsSync(ejercicio.rutaVideoLocal)) {
      console.warn(`[AVISO] Video no encontrado, se omite ${ejercicio.id}: ${ejercicio.rutaVideoLocal}`);
      continue;
    }

    const nombreArchivoVideo = ejercicio.rutaVideoLocal.split("\\").pop();
    const nombreArchivoThumb = ejercicio.rutaThumbLocal.split("\\").pop();

    const materialUrl = await subirArchivo(
      ejercicio.rutaVideoLocal,
      `biblioteca_ejercicios/${ejercicio.categoriaSlug}/${ejercicio.id}/${nombreArchivoVideo}`,
      "video/mp4",
    );

    // La miniatura se sube a Storage (misma estructura de carpetas que la
    // Guia Oficial, seccion 11) pero todavia NO se escribe como campo en
    // Firestore -- el modelo Ejercicio de CLAUDE.md no tiene un campo de
    // miniatura hoy; agregarlo es un cambio de esquema a documentar ahi
    // primero, no algo para decidir dentro de este script.
    if (fs.existsSync(ejercicio.rutaThumbLocal)) {
      await subirArchivo(
        ejercicio.rutaThumbLocal,
        `biblioteca_ejercicios/${ejercicio.categoriaSlug}/${ejercicio.id}/${nombreArchivoThumb}`,
        "image/webp",
      );
    }

    const datos = {
      nombre: ejercicio.nombre,
      descripcion: ejercicio.descripcion,
      categoria: ejercicio.categoria,
      materialUrl,
      duracionSegundos: ejercicio.duracionSegundos,
      repeticiones: ejercicio.repeticiones,
      patronesReferencia: ejercicio.patronesReferencia,
      diagnosticosAplicables: ejercicio.diagnosticosAplicables,
      creadoPor: fisio.uid,
      fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
      activo: true,
    };

    await firestore.collection("ejercicios").doc(ejercicio.id).set(datos, { merge: true });
    console.log(`Listo: ${ejercicio.id} (${ejercicio.nombre}) -> ${materialUrl}`);
  }

  console.log(`Biblioteca actualizada (${BIBLIOTECA.length} ejercicio(s)), creadoPor: ${argumentos.creadoPorEmail}`);
}

crearBiblioteca(parsearArgumentos(process.argv.slice(2)))
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Error al crear la biblioteca de ejercicios:", error?.message ?? error);
    process.exit(1);
  });
