#!/usr/bin/env node
/**
 * Mide el porcentaje de sesiones registradas correctamente (EDT 1.8.6.4).
 *
 * Lee las sesiones finalizadas (estado "completada") de Firestore y comprueba cuáles contienen todo lo
 * exigido por HU17-CA01, CA02 y CA05. Solo LEE: no escribe nada en Firestore.
 *
 * Uso (desde la carpeta /backend, que es donde está instalado firebase-admin):
 *   # Emuladores locales (Auth y Firestore en marcha):
 *   FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 node ../docs/pruebas/mediciones/scripts/verificar_sesiones.js --emulador
 *   # Proyecto real (credencial de servicio propia; el script muestra el proyecto antes de leer):
 *   GOOGLE_APPLICATION_CREDENTIALS=service-account.json node ../docs/pruebas/mediciones/scripts/verificar_sesiones.js --proyecto-real
 *
 * Opciones:
 *   --desde AAAA-MM-DD   solo sesiones ejecutadas desde esa fecha (inclusive)
 *   --paciente UID       solo las sesiones de ese paciente
 *   --salida DIR         carpeta de salida (por defecto docs/pruebas/mediciones)
 *
 * Criterios aplicados a cada sesión finalizada (cada falta se informa con su campo):
 *   CA01 (resultados, métricas y fecha): estado, fechaEjecucion, porcentajeEjecucion, desviacionPromedio,
 *        repeticionesAsignadas, repeticionesCompletadas, repeticionesCorrectas.
 *   CA02 (datos numéricos, ángulos y métricas): al menos una repetición, y en cada una numero, porcentaje
 *        y desviacionAngular numéricos.
 *   CA05 (resultado de cada acción de cada repetición): tantas repeticiones como repeticionesCompletadas y,
 *        en cada una, acciones con ordenAccion, anguloMaximo, tiempoSeg, desviacionAngular (numéricos) y completada (booleano).
 */
const fs = require("fs");
const path = require("path");

const admin = require(path.resolve(__dirname, "../../../../backend/node_modules/firebase-admin"));

// ------------------------------------------------------------------ evaluación (exportada para pruebas)
const esNumero = (v) => typeof v === "number" && Number.isFinite(v);

function evaluarSesion(s) {
  const faltas = [];
  const falta = (ca, campo) => faltas.push({ ca, campo });

  // CA01
  if (s.estado !== "completada") falta("CA01", "estado");
  const fecha = s.fechaEjecucion;
  if (!(fecha && (typeof fecha.toDate === "function" || fecha instanceof Date))) falta("CA01", "fechaEjecucion");
  for (const c of ["porcentajeEjecucion", "desviacionPromedio", "repeticionesAsignadas", "repeticionesCompletadas", "repeticionesCorrectas"]) {
    if (!esNumero(s[c])) falta("CA01", c);
  }

  // CA02 y CA05 trabajan sobre el detalle por repetición
  const reps = Array.isArray(s.repeticiones) ? s.repeticiones : null;
  if (!reps || reps.length === 0) {
    falta("CA02", "repeticiones (sin repeticiones registradas)");
    falta("CA05", "repeticiones[].acciones (sin repeticiones registradas)");
  } else {
    reps.forEach((r, i) => {
      for (const c of ["numero", "porcentaje", "desviacionAngular"]) {
        if (!esNumero(r && r[c])) falta("CA02", `repeticiones[${i}].${c}`);
      }
      const acciones = r && Array.isArray(r.acciones) ? r.acciones : null;
      if (!acciones || acciones.length === 0) {
        falta("CA05", `repeticiones[${i}].acciones`);
      } else {
        acciones.forEach((a, j) => {
          for (const c of ["ordenAccion", "anguloMaximo", "tiempoSeg", "desviacionAngular"]) {
            if (!esNumero(a && a[c])) falta("CA05", `repeticiones[${i}].acciones[${j}].${c}`);
          }
          if (typeof (a && a.completada) !== "boolean") falta("CA05", `repeticiones[${i}].acciones[${j}].completada`);
        });
      }
    });
    if (esNumero(s.repeticionesCompletadas) && reps.length !== s.repeticionesCompletadas) {
      falta("CA05", `repeticiones (hay ${reps.length} y repeticionesCompletadas = ${s.repeticionesCompletadas})`);
    }
  }
  return faltas;
}

function resumir(sesiones) {
  const filas = sesiones.map((s) => ({ id: s.id, ...s.datos, faltas: evaluarSesion(s.datos) }));
  const completas = filas.filter((f) => f.faltas.length === 0);
  const porCriterio = { CA01: 0, CA02: 0, CA05: 0 };
  for (const f of filas) for (const ca of new Set(f.faltas.map((x) => x.ca))) porCriterio[ca] += 1;
  return {
    finalizadas: filas.length,
    completas: completas.length,
    porcentaje: filas.length ? (100 * completas.length) / filas.length : null,
    incompletas: filas.filter((f) => f.faltas.length > 0),
    conFallaPorCriterio: porCriterio,
  };
}

// ------------------------------------------------------------------ ejecución
function argumento(nombre) {
  const i = process.argv.indexOf(nombre);
  return i >= 0 ? process.argv[i + 1] : undefined;
}

function aFecha(v) {
  return v && typeof v.toDate === "function" ? v.toDate() : v instanceof Date ? v : null;
}

async function main() {
  const emulador = process.argv.includes("--emulador");
  const real = process.argv.includes("--proyecto-real");
  if (emulador === real) {
    console.error("Indique exactamente una opción: --emulador o --proyecto-real.");
    process.exit(1);
  }
  const salida = path.resolve(argumento("--salida") || path.join(__dirname, ".."));
  const proyectoId = argumento("--proyecto-id") || "app-rehabilitacion-terapeutica";

  if (emulador) {
    if (!/^(localhost|127\.0\.0\.1)(:\d+)?$/.test(process.env.FIRESTORE_EMULATOR_HOST || "")) {
      console.error("Con --emulador defina FIRESTORE_EMULATOR_HOST apuntando a localhost (por ejemplo 127.0.0.1:8080).");
      process.exit(1);
    }
    if (process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      console.error("Hay GOOGLE_APPLICATION_CREDENTIALS definida; quítela para leer solo el emulador.");
      process.exit(1);
    }
    admin.initializeApp({ projectId: proyectoId });
  } else {
    if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) {
      console.error("Con --proyecto-real defina GOOGLE_APPLICATION_CREDENTIALS con su credencial de servicio.");
      process.exit(1);
    }
    admin.initializeApp({ credential: admin.credential.applicationDefault() });
  }
  const db = admin.firestore();
  const origen = emulador ? `emulador local (${process.env.FIRESTORE_EMULATOR_HOST})` : `proyecto real (${admin.app().options.projectId || "ver credencial"})`;
  console.log("Leyendo sesiones finalizadas de:", origen);

  let consulta = db.collection("sesiones").where("estado", "==", "completada");
  const paciente = argumento("--paciente");
  if (paciente) consulta = consulta.where("pacienteId", "==", paciente);
  const instantaneas = await consulta.get();
  const desde = argumento("--desde") ? new Date(argumento("--desde") + "T00:00:00") : null;
  const sesiones = instantaneas.docs
    .map((d) => ({ id: d.id, datos: d.data() }))
    .filter((s) => !desde || (aFecha(s.datos.fechaEjecucion) && aFecha(s.datos.fechaEjecucion) >= desde));

  const r = resumir(sesiones);
  const pct = r.porcentaje === null ? "—" : r.porcentaje.toFixed(1) + " %";
  const md = [
    "# 1.8.6.4 Sesiones registradas correctamente", "",
    `Generado por \`scripts/verificar_sesiones.js\` el ${new Date().toISOString()}.`, "",
    "| Campo | Valor |", "|---|---|",
    "| Método | Lectura de las sesiones con estado `completada` en Firestore y verificación campo por campo de lo exigido por HU17-CA01, CA02 y CA05 |",
    `| Origen de los datos | ${origen} |`,
    `| Filtros | ${[desde ? "desde " + argumento("--desde") : null, paciente ? "paciente " + paciente : null].filter(Boolean).join("; ") || "ninguno"} |`,
    `| Sesiones finalizadas | ${r.finalizadas} |`,
    `| Sesiones con registro completo | ${r.completas} |`,
    `| Porcentaje con registro completo | ${pct} |`,
    `| Sesiones con faltas por criterio | CA01: ${r.conFallaPorCriterio.CA01}; CA02: ${r.conFallaPorCriterio.CA02}; CA05: ${r.conFallaPorCriterio.CA05} |`,
    "",
  ];
  if (r.incompletas.length) {
    md.push("Sesiones que no cumplen y qué les falta:", "", "| Sesión | Paciente | Fecha | Faltas |", "|---|---|---|---|");
    for (const f of r.incompletas) {
      const fecha = aFecha(f.fechaEjecucion);
      const faltas = f.faltas.map((x) => `${x.ca}: ${x.campo}`).join("; ");
      md.push(`| ${f.id} | ${f.pacienteId || "—"} | ${fecha ? fecha.toISOString().slice(0, 10) : "—"} | ${faltas} |`);
    }
    md.push("");
  } else if (r.finalizadas > 0) {
    md.push("Todas las sesiones finalizadas contienen lo exigido.", "");
  }
  fs.mkdirSync(salida, { recursive: true });
  fs.writeFileSync(path.join(salida, "1.8.6.4_sesiones.md"), md.join("\n"), "utf8");
  console.log(md.join("\n"));
  console.log("escrito:", path.join(salida, "1.8.6.4_sesiones.md"));
}

if (require.main === module) {
  main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
}

module.exports = { evaluarSesion, resumir };
