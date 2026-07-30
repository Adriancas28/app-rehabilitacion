/**
 * Crea (o reemplaza, por nombre) el catalogo de 9 ejercicios predeterminados
 * de movilidad y control motor -- ver CLAUDE.md, "Catalogo de ejercicios
 * predeterminados". Deja fuera el ejercicio #10 (Equilibrio monopodal): no
 * es angular, no encaja con el diseno actual de HU08/HU09 (comparacion
 * contra un angulo esperado).
 *
 * Los rangos de angulo (patronesReferencia) son un punto de partida
 * tecnico basado en goniometria general -- no una dosis clinica. El
 * fisioterapeuta puede ajustarlos por paciente (HU02-CA05), o recalcularlos
 * analizando un video real con "Calcular ROM automaticamente" (HU02-CA07)
 * una vez que existan clips grabados para cada ejercicio.
 *
 * Requiere: GOOGLE_APPLICATION_CREDENTIALS apuntando a la service account.
 *
 * Uso:
 *   npx ts-node seed/crear-catalogo-ejercicios.ts --creadoPorEmail ana.ruiz@sanna.pe
 */
import * as admin from "firebase-admin";

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

interface EjercicioSeed {
  nombre: string;
  descripcion: string;
  categoria: "MOVILIDAD" | "CONTROL_MOTOR";
  duracionSegundos: number;
  repeticiones: number;
  patronesReferencia: PatronReferencia[];
  diagnosticosAplicables: string[];
}

const CATALOGO: EjercicioSeed[] = [
  {
    nombre: "Flexión de hombro",
    descripcion: "Sentado o de pie, eleva el brazo derecho hacia adelante hasta la altura del hombro y sostenlo.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: [
      "PINZAMIENTO_SUBACROMIAL",
      "CAPSULITIS_ADHESIVA",
      "POST_QUIRURGICO_MANGUITO_ROTADOR",
    ],
  },
  {
    nombre: "Abducción de hombro",
    descripcion: "De pie, de frente a la cámara, eleva el brazo derecho hacia el costado hasta la altura del hombro y sostenlo.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: [
      "PINZAMIENTO_SUBACROMIAL",
      "CAPSULITIS_ADHESIVA",
      "POST_QUIRURGICO_MANGUITO_ROTADOR",
    ],
  },
  {
    nombre: "Flexión de codo",
    descripcion: "Sentado, flexiona el codo derecho llevando la mano hacia el hombro y sostenlo.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "CODO_DERECHO", anguloMin: 60, anguloMax: 90 }],
    diagnosticosAplicables: ["RIGIDEZ_POSTRAUMATICA_CODO"],
  },
  {
    nombre: "Marcha estacionaria",
    descripcion: "De pie, de perfil a la cámara, levanta la rodilla derecha hacia el pecho y sostenla.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "CADERA_DERECHA", anguloMin: 100, anguloMax: 140 }],
    diagnosticosAplicables: ["POST_ARTROPLASTIA_CADERA", "OSTEOARTROSIS_CADERA"],
  },
  {
    nombre: "Flexión de rodilla (sentado)",
    descripcion: "Sentado, flexiona y extiende la rodilla derecha.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 90, anguloMax: 160 }],
    diagnosticosAplicables: [
      "POST_RECONSTRUCCION_LCA",
      "OSTEOARTROSIS_RODILLA",
      "SINDROME_DOLOR_FEMOROPATELAR",
    ],
  },
  {
    nombre: "Flexión dorsal/plantar de tobillo",
    descripcion: "Sentado, con la pierna extendida, sube y baja la punta del pie derecho.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "TOBILLO_DERECHO", anguloMin: 80, anguloMax: 100 }],
    diagnosticosAplicables: ["ESGUINCE_TOBILLO"],
  },
  {
    nombre: "Rotación de tronco (sentado)",
    descripcion: "Sentado, de frente a la cámara, gira el torso hacia la derecha y sostenlo.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "TRONCO", anguloMin: 60, anguloMax: 100 }],
    diagnosticosAplicables: ["LUMBALGIA_MECANICA"],
  },
  {
    nombre: "Mini sentadilla",
    descripcion: "De pie, de frente o de perfil a la cámara, flexiona ligeramente las rodillas y sostén la posición.",
    categoria: "CONTROL_MOTOR",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 120, anguloMax: 150 }],
    diagnosticosAplicables: [
      "POST_RECONSTRUCCION_LCA",
      "OSTEOARTROSIS_RODILLA",
      "SINDROME_DOLOR_FEMOROPATELAR",
      "DEBILIDAD_MUSCULAR",
    ],
  },
  {
    nombre: "Puente de glúteos",
    descripcion: "Acostado boca arriba, con las rodillas flexionadas, eleva la cadera y sostenla.",
    categoria: "CONTROL_MOTOR",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "CADERA_DERECHA", anguloMin: 160, anguloMax: 180 }],
    diagnosticosAplicables: [
      "POST_ARTROPLASTIA_CADERA",
      "OSTEOARTROSIS_CADERA",
      "LUMBALGIA_MECANICA",
      "DEBILIDAD_MUSCULAR",
    ],
  },
];

async function crearCatalogo(argumentos: Argumentos): Promise<void> {
  admin.initializeApp();

  const fisio = await admin.auth().getUserByEmail(argumentos.creadoPorEmail);
  const firestore = admin.firestore();

  for (const ejercicio of CATALOGO) {
    const existente = await firestore
      .collection("ejercicios")
      .where("nombre", "==", ejercicio.nombre)
      .limit(1)
      .get();

    const datos = {
      nombre: ejercicio.nombre,
      descripcion: ejercicio.descripcion,
      categoria: ejercicio.categoria,
      materialUrl: "",
      duracionSegundos: ejercicio.duracionSegundos,
      repeticiones: ejercicio.repeticiones,
      patronesReferencia: ejercicio.patronesReferencia,
      diagnosticosAplicables: ejercicio.diagnosticosAplicables,
      creadoPor: fisio.uid,
      fechaCreacion: admin.firestore.FieldValue.serverTimestamp(),
      activo: true,
    };

    if (existente.empty) {
      await firestore.collection("ejercicios").add(datos);
      console.log(`Creado: ${ejercicio.nombre}`);
    } else {
      await existente.docs[0].ref.set(datos, { merge: true });
      console.log(`Actualizado: ${ejercicio.nombre}`);
    }
  }

  console.log(`Catálogo listo (${CATALOGO.length} ejercicios), creadoPor: ${argumentos.creadoPorEmail}`);
}

crearCatalogo(parsearArgumentos(process.argv.slice(2)))
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Error al crear el catálogo de ejercicios:", error?.message ?? error);
    process.exit(1);
  });
