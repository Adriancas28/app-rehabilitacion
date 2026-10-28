/**
 * Guía visual en vivo (HU02): siembra las acciones y la vista de cámara de los
 * 12 ejercicios del catálogo. Los ángulos son un BORRADOR clínico (convención
 * del sistema: ángulo en el vértice, no goniometría tradicional) que el
 * fisioterapeuta debe validar y puede editar. Solo agrega `acciones` y
 * `vistaCamara` (merge): no toca el resto del documento.
 *
 * Uso (desde /backend):
 *   GOOGLE_APPLICATION_CREDENTIALS=service-account.json npx ts-node seed/sembrar-acciones-ejercicios.ts
 * Guarda un respaldo previo en seed/_respaldo_ejercicios.json.
 */
import * as admin from "firebase-admin";
import * as fs from "fs";
import * as path from "path";

// Landmarks de MediaPipe Pose.
const HOMBRO_D = 12;
const CODO_D = 14;
const CADERA_D = 24;
const RODILLA_D = 26;
const TOBILLO_D = 28;
const HOMBRO_I = 11;

interface Paso {
  nombre: string;
  inicio: number;
  meta: number;
  tolerancia: number;
  mantenerSeg: number;
  tiempoMaxSeg: number;
}

interface Segmento {
  pivote: number;
  referencia: number;
  distal: number;
}

interface Especificacion {
  vista: "Lateral" | "Frontal" | "Oblicua";
  segmento: Segmento;
  pasos: Paso[];
}

const BRAZO: Segmento = { pivote: HOMBRO_D, referencia: CADERA_D, distal: CODO_D };
const RODILLA: Segmento = { pivote: RODILLA_D, referencia: CADERA_D, distal: TOBILLO_D };
const TRONCO_SOBRE_MUSLO: Segmento = { pivote: CADERA_D, referencia: RODILLA_D, distal: HOMBRO_D };
const MUSLO_SOBRE_TRONCO: Segmento = { pivote: CADERA_D, referencia: HOMBRO_D, distal: RODILLA_D };

const brazo = (subir: string, bajar: string, mantener: number, tiempo: number, topeSuperior = 90): Paso[] => [
  { nombre: subir, inicio: 15, meta: topeSuperior, tolerancia: 20, mantenerSeg: mantener, tiempoMaxSeg: tiempo },
  { nombre: bajar, inicio: topeSuperior, meta: 15, tolerancia: 15, mantenerSeg: 0, tiempoMaxSeg: tiempo },
];

const CATALOGO: Record<string, Especificacion> = {
  "Bisagra de cadera (Hip Hinge)": {
    vista: "Lateral",
    segmento: TRONCO_SOBRE_MUSLO,
    pasos: [
      { nombre: "Inclinar el tronco hacia adelante", inicio: 180, meta: 120, tolerancia: 30, mantenerSeg: 1, tiempoMaxSeg: 8 },
      { nombre: "Volver a erguirse", inicio: 120, meta: 170, tolerancia: 10, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Marcha estacionaria con control de tronco": {
    vista: "Frontal",
    segmento: MUSLO_SOBRE_TRONCO,
    pasos: [
      { nombre: "Elevar la rodilla", inicio: 175, meta: 120, tolerancia: 20, mantenerSeg: 1, tiempoMaxSeg: 6 },
      { nombre: "Bajar la pierna", inicio: 120, meta: 170, tolerancia: 10, mantenerSeg: 0, tiempoMaxSeg: 6 },
    ],
  },
  "Inclinación lateral de tronco": {
    vista: "Frontal",
    segmento: { pivote: CADERA_D, referencia: HOMBRO_I, distal: HOMBRO_D },
    pasos: [
      { nombre: "Inclinar el tronco hacia un lado", inicio: 50, meta: 90, tolerancia: 20, mantenerSeg: 1, tiempoMaxSeg: 8 },
      { nombre: "Volver al centro", inicio: 90, meta: 50, tolerancia: 15, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Puente de glúteo": {
    vista: "Lateral",
    segmento: TRONCO_SOBRE_MUSLO,
    pasos: [
      { nombre: "Elevar la cadera", inicio: 90, meta: 170, tolerancia: 10, mantenerSeg: 2, tiempoMaxSeg: 8 },
      { nombre: "Bajar con control", inicio: 170, meta: 100, tolerancia: 15, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Extensión de rodilla en sedestación": {
    vista: "Lateral",
    segmento: RODILLA,
    pasos: [
      { nombre: "Extender la rodilla", inicio: 90, meta: 160, tolerancia: 15, mantenerSeg: 2, tiempoMaxSeg: 8 },
      { nombre: "Flexionar de vuelta", inicio: 160, meta: 95, tolerancia: 15, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Flexión de rodilla en bipedestación": {
    vista: "Lateral",
    segmento: RODILLA,
    pasos: [
      { nombre: "Flexionar la rodilla", inicio: 175, meta: 115, tolerancia: 25, mantenerSeg: 1, tiempoMaxSeg: 8 },
      { nombre: "Volver a extender", inicio: 115, meta: 170, tolerancia: 10, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Mini-sentadilla": {
    vista: "Frontal",
    segmento: RODILLA,
    pasos: [
      { nombre: "Bajar flexionando las rodillas", inicio: 175, meta: 135, tolerancia: 15, mantenerSeg: 1, tiempoMaxSeg: 8 },
      { nombre: "Subir", inicio: 135, meta: 170, tolerancia: 10, mantenerSeg: 0, tiempoMaxSeg: 8 },
    ],
  },
  "Levantarse de una silla (Sit-to-Stand)": {
    vista: "Lateral",
    segmento: RODILLA,
    pasos: [
      { nombre: "Ponerse de pie", inicio: 90, meta: 170, tolerancia: 10, mantenerSeg: 1, tiempoMaxSeg: 10 },
      { nombre: "Sentarse con control", inicio: 170, meta: 95, tolerancia: 15, mantenerSeg: 0, tiempoMaxSeg: 10 },
    ],
  },
  "Abducción de hombro": { vista: "Frontal", segmento: BRAZO, pasos: brazo("Elevar el brazo hacia el costado", "Bajar con control", 2, 8) },
  "Flexión de hombro": { vista: "Lateral", segmento: BRAZO, pasos: brazo("Elevar el brazo hacia adelante", "Bajar con control", 2, 8) },
  "Trepado de dedos en pared": {
    vista: "Frontal",
    segmento: BRAZO,
    pasos: [
      { nombre: "Subir la mano por la pared", inicio: 20, meta: 105, tolerancia: 25, mantenerSeg: 2, tiempoMaxSeg: 10 },
      { nombre: "Bajar la mano", inicio: 105, meta: 20, tolerancia: 20, mantenerSeg: 0, tiempoMaxSeg: 10 },
    ],
  },
  "Flexión unilateral alternada de hombro": { vista: "Oblicua", segmento: BRAZO, pasos: brazo("Elevar el brazo", "Bajar", 1, 6) },
};

async function main(): Promise<void> {
  admin.initializeApp();
  const db = admin.firestore();
  const ejercicios = await db.collection("ejercicios").get();

  const respaldo: Record<string, unknown> = {};
  ejercicios.docs.forEach((d) => (respaldo[d.id] = d.data()));
  fs.writeFileSync(path.join(__dirname, "_respaldo_ejercicios.json"), JSON.stringify(respaldo, null, 1));

  let sembrados = 0;
  for (const doc of ejercicios.docs) {
    const nombre = doc.get("nombre") as string;
    const espec = CATALOGO[nombre];
    if (!espec) {
      console.log(`Sin especificación (se omite): ${nombre}`);
      continue;
    }
    const acciones = espec.pasos.map((p, i) => ({
      orden: i + 1,
      nombre: p.nombre,
      puntoReferencia: espec.segmento.referencia,
      pivote: espec.segmento.pivote,
      puntoDistal: espec.segmento.distal,
      lado: "Derecho",
      anguloInicio: p.inicio,
      anguloMeta: p.meta,
      tolerancia: p.tolerancia,
      mantenerSeg: p.mantenerSeg,
      tiempoMaxSeg: p.tiempoMaxSeg,
      sentido: "AUTOMATICO",
    }));
    await doc.ref.set({ acciones, vistaCamara: espec.vista }, { merge: true });
    sembrados += 1;
    console.log(`OK ${nombre}: ${acciones.length} acciones, vista ${espec.vista}`);
  }
  console.log(`Ejercicios sembrados: ${sembrados} de ${ejercicios.size}`);
}

main().then(() => process.exit(0)).catch((e) => {
  console.error(e);
  process.exit(1);
});
