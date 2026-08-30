/**
 * Crea (o reemplaza, por nombre) el catalogo de 12 ejercicios del MVP --
 * ver CLAUDE.md, "Catalogo de ejercicios predeterminados", y el documento
 * fuente "Catalogo_Ejercicios_MVP_SANNA_Especificaciones.docx" (3
 * condiciones, 12 ejercicios, con respaldo bibliografico: OMS, Guia de
 * Practica Clinica de EsSalud 2025, estudio de 366 pacientes en un centro
 * de rehabilitacion de Lima).
 *
 * Reemplaza por completo al catalogo anterior de 9 ejercicios (Sprint 3) --
 * decision del usuario, no una ampliacion.
 *
 * Los rangos de angulo (patronesReferencia) son un punto de partida
 * tecnico basado en goniometria general -- no una dosis clinica. Los 4
 * ejercicios cuyo movimiento coincide con el catalogo anterior (LUM-04,
 * OAR-03, SDS-01, SDS-02) reusan el rango ya validado; el resto son
 * estimaciones provisionales, pendientes de refinar por el fisioterapeuta
 * (HU02-CA05) o de recalcular con "Calcular ROM automaticamente" (HU02-CA07)
 * una vez que existan clips grabados. El propio documento fuente marca
 * LUM-04, OAR-04 y SDS-03 como "validacion piloto prioritaria" -- sus
 * metricas definitivas dependen de esa validacion.
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
  codigo: string; // codigo del catalogo fuente (LUM-01, OAR-02, ...) -- no se guarda en Firestore, solo trazabilidad de este script
  nombre: string;
  descripcion: string;
  categoria: "MOVILIDAD" | "CONTROL_MOTOR";
  duracionSegundos: number;
  repeticiones: number;
  patronesReferencia: PatronReferencia[];
  diagnosticosAplicables: string[];
}

const CATALOGO: EjercicioSeed[] = [
  // ===== Lumbalgia inespecífica (LUM) =====
  {
    codigo: "LUM-01",
    nombre: "Bisagra de cadera (Hip Hinge)",
    descripcion: "De pie, de perfil a la cámara, con los pies apoyados, lleva la pelvis hacia atrás flexionando la cadera (sin redondear la espalda) y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "CADERA_DERECHA", anguloMin: 90, anguloMax: 150 }],
    diagnosticosAplicables: ["LUMBALGIA_INESPECIFICA"],
  },
  {
    codigo: "LUM-02",
    nombre: "Marcha estacionaria con control de tronco",
    descripcion: "De pie, de frente a la cámara, levanta alternadamente una rodilla y luego la otra, manteniendo el tronco estable.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "CADERA_DERECHA", anguloMin: 100, anguloMax: 140 }],
    diagnosticosAplicables: ["LUMBALGIA_INESPECIFICA"],
  },
  {
    codigo: "LUM-03",
    nombre: "Inclinación lateral de tronco",
    descripcion: "De pie, de frente a la cámara, con la pelvis estable, inclina el tronco lateralmente y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "TRONCO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: ["LUMBALGIA_INESPECIFICA"],
  },
  {
    codigo: "LUM-04",
    nombre: "Puente de glúteo",
    descripcion: "Acostado boca arriba, con las rodillas flexionadas y los pies apoyados, eleva la cadera y regresa de forma controlada.",
    categoria: "CONTROL_MOTOR",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "CADERA_DERECHA", anguloMin: 160, anguloMax: 180 }],
    diagnosticosAplicables: ["LUMBALGIA_INESPECIFICA"],
  },
  // ===== Osteoartritis de rodilla (OAR) =====
  {
    codigo: "OAR-01",
    nombre: "Extensión de rodilla en sedestación",
    descripcion: "Sentado, de perfil a la cámara, extiende la pierna y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 140, anguloMax: 175 }],
    diagnosticosAplicables: ["OSTEOARTROSIS_RODILLA"],
  },
  {
    codigo: "OAR-02",
    nombre: "Flexión de rodilla en bipedestación",
    descripcion: "De pie, de perfil a la cámara, flexiona la rodilla llevando el talón hacia atrás y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 90, anguloMax: 140 }],
    diagnosticosAplicables: ["OSTEOARTROSIS_RODILLA"],
  },
  {
    codigo: "OAR-03",
    nombre: "Mini-sentadilla",
    descripcion: "De pie, de frente a la cámara, desciende parcialmente flexionando cadera y rodillas, y regresa de forma controlada.",
    categoria: "CONTROL_MOTOR",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 120, anguloMax: 150 }],
    diagnosticosAplicables: ["OSTEOARTROSIS_RODILLA"],
  },
  {
    codigo: "OAR-04",
    nombre: "Levantarse de una silla (Sit-to-Stand)",
    descripcion: "Sentado en una silla estable, de perfil a la cámara, incorpórate hasta quedar de pie y regresa a sentarte de forma controlada.",
    categoria: "CONTROL_MOTOR",
    duracionSegundos: 10,
    repeticiones: 5,
    patronesReferencia: [{ articulacion: "RODILLA_DERECHA", anguloMin: 150, anguloMax: 180 }],
    diagnosticosAplicables: ["OSTEOARTROSIS_RODILLA"],
  },
  // ===== Síndrome de dolor subacromial / hombro doloroso (SDS) =====
  {
    codigo: "SDS-01",
    nombre: "Abducción de hombro",
    descripcion: "De pie, de frente a la cámara, eleva el brazo hacia el costado hasta la altura del hombro y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: ["SINDROME_DOLOR_SUBACROMIAL"],
  },
  {
    codigo: "SDS-02",
    nombre: "Flexión de hombro",
    descripcion: "De pie o sentado, de perfil a la cámara, eleva el brazo hacia adelante hasta la altura del hombro y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: ["SINDROME_DOLOR_SUBACROMIAL"],
  },
  {
    codigo: "SDS-03",
    nombre: "Trepado de dedos en pared",
    descripcion: "De pie, de frente a una pared, desplaza progresivamente la mano hacia arriba apoyada en la pared y regresa de forma controlada.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 3,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 80, anguloMax: 130 }],
    diagnosticosAplicables: ["SINDROME_DOLOR_SUBACROMIAL"],
  },
  {
    codigo: "SDS-04",
    nombre: "Flexión unilateral alternada de hombro",
    descripcion: "De pie, de perfil u oblicuo a la cámara, eleva un brazo hacia adelante, regresa y repite con el otro. Este ejercicio está pensado para pacientes con lado afectado \"Ambos\" (usuarios/{uid}.ladoAfectado), para comparar el desempeño entre ambos lados.",
    categoria: "MOVILIDAD",
    duracionSegundos: 10,
    repeticiones: 6,
    patronesReferencia: [{ articulacion: "HOMBRO_DERECHO", anguloMin: 70, anguloMax: 110 }],
    diagnosticosAplicables: ["SINDROME_DOLOR_SUBACROMIAL"],
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
      console.log(`Creado: ${ejercicio.codigo} - ${ejercicio.nombre}`);
    } else {
      await existente.docs[0].ref.set(datos, { merge: true });
      console.log(`Actualizado: ${ejercicio.codigo} - ${ejercicio.nombre}`);
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
