// Migración de la estructura antigua de Firestore al diccionario de datos
// (docs/modelo-datos). Uso: node seed/_migrar_modelo.js [--borrar-antiguo]
// 1) Respalda todo en seed/_respaldo_antes_migracion.json
// 2) Crea usuarios/pacientes/fisioterapeutas/diagnosticos/catalogo_diagnosticos/ejercicios/sesiones/observaciones nuevos
// 3) Con --borrar-antiguo, borra las sesiones/recomendaciones antiguas (subcolecciones de usuarios).
const admin = require("firebase-admin");
const fs = require("fs");
admin.initializeApp({ credential: admin.credential.cert(require("../service-account.json")) });
const db = admin.firestore();

const ROL = { admin: "administrador", administrador: "administrador", paciente: "paciente", fisioterapeuta: "fisioterapeuta" };
const ESTADO = { pendiente: "asignada", asignada: "asignada", completada: "completada", "en curso": "en curso" };
const DIAGNOSTICOS = {
  LUMBALGIA_INESPECIFICA: { nombre: "Lumbalgia inespecífica", regionCorporal: "Columna" },
  OSTEOARTROSIS_RODILLA: { nombre: "Osteoartritis de rodilla (gonartrosis)", regionCorporal: "Rodilla" },
  SINDROME_DOLOR_SUBACROMIAL: { nombre: "Síndrome de dolor subacromial / hombro doloroso", regionCorporal: "Hombro" },
};
const quitarNulos = (o) => Object.fromEntries(Object.entries(o).filter(([, v]) => v !== undefined && v !== null));
const cap = (s) => (s ? s.charAt(0) + s.slice(1).toLowerCase() : s);

async function main() {
  const borrar = process.argv.includes("--borrar-antiguo");
  const respaldo = { usuarios: {}, ejercicios: {} };
  const usuarios = await db.collection("usuarios").get();
  const ejercicios = await db.collection("ejercicios").get();
  for (const u of usuarios.docs) {
    const sesiones = await u.ref.collection("sesiones").get();
    respaldo.usuarios[u.id] = { ...u.data(), _sesiones: {} };
    for (const s of sesiones.docs) {
      const recs = await s.ref.collection("recomendaciones").get();
      respaldo.usuarios[u.id]._sesiones[s.id] = { ...s.data(), _recomendaciones: recs.docs.map((r) => ({ id: r.id, ...r.data() })) };
    }
  }
  ejercicios.docs.forEach((e) => (respaldo.ejercicios[e.id] = e.data()));
  fs.writeFileSync(__dirname + "/_respaldo_antes_migracion.json", JSON.stringify(respaldo, null, 1));
  console.log("Respaldo escrito");

  for (const [id, d] of Object.entries(DIAGNOSTICOS)) await db.collection("catalogo_diagnosticos").doc(id).set(d);

  // Usuarios (ya migrados si tienen "correo": se saltan)
  for (const u of usuarios.docs) {
    const d = u.data();
    if (d.correo !== undefined && d.email === undefined) continue;
    const rol = ROL[d.rol] || d.rol;
    await db.collection("usuarios").doc(u.id).set({
      correo: d.email, rol, nombre: d.nombre, fechaCreacion: d.fechaRegistro || admin.firestore.Timestamp.now(), activo: d.activo !== false,
    });
    const genero = d.genero ? cap(d.genero) : undefined;
    if (rol === "paciente") {
      await db.collection("pacientes").doc(u.id).set(quitarNulos({
        dni: d.dni, edad: d.edad, genero, contacto: d.numeroContacto, ladoAfectado: d.ladoAfectado ? cap(d.ladoAfectado) : undefined,
        fisioterapeutaId: d.fisioterapeutaId,
      }));
      for (const dg of d.diagnosticos || []) {
        await db.collection("pacientes").doc(u.id).collection("diagnosticos").add({ diagnosticoId: dg.codigo, fecha: dg.fecha || admin.firestore.Timestamp.now() });
      }
    } else if (rol === "fisioterapeuta") {
      await db.collection("fisioterapeutas").doc(u.id).set(quitarNulos({
        edad: d.edad, genero, contacto: d.numeroContacto, especialidad: d.especialidad, numeroColegiatura: d.numeroColegiatura,
      }));
    }
  }
  console.log("Usuarios migrados");

  // Ejercicios
  for (const e of ejercicios.docs) {
    const d = e.data();
    if (d.videoPath !== undefined || d.angulosReferencia !== undefined) continue;
    const angulos = {};
    (d.patronesReferencia || []).forEach((p) => (angulos[p.articulacion] = { min: p.anguloMin, max: p.anguloMax }));
    await db.collection("ejercicios").doc(e.id).set(quitarNulos({
      nombre: d.nombre, descripcion: d.descripcion, fisioterapeutaId: d.creadoPor, categoria: d.categoria === "CONTROL_MOTOR" ? "Control motor" : d.categoria === "MOVILIDAD" ? "Movilidad" : d.categoria,
      videoPath: d.materialUrl || "", angulosReferencia: angulos,
      repeticiones: { cantidad: d.repeticiones, duracionSeg: d.duracionSegundos },
      diagnosticosAplicables: d.diagnosticosAplicables || [], fechaCreacion: d.fechaCreacion, activo: d.activo !== false,
    }));
  }
  console.log("Ejercicios migrados");

  // Sesiones y observaciones
  const ejById = {};
  ejercicios.docs.forEach((e) => (ejById[e.id] = e.data()));
  let n = 0;
  for (const [pacienteId, u] of Object.entries(respaldo.usuarios)) {
    for (const [sesionId, s] of Object.entries(u._sesiones)) {
      const ej = ejById[s.ejercicioId] || {};
      const asignadas = s.repeticiones ?? ej.repeticiones ?? 1;
      const duracion = s.duracionSegundos ?? ej.duracionSegundos ?? 30;
      const r = s.resultado;
      const doc = quitarNulos({
        pacienteId, ejercicioId: s.ejercicioId, fisioterapeutaId: s.fisioterapeutaId, fechaAsignacion: s.fechaAsignacion, fechaEjecucion: s.fechaEjecucion,
        estado: ESTADO[s.estado] || s.estado, nota: s.notas, repeticionesAsignadas: asignadas, duracionEstimada: asignadas * duracion,
        duracionSegundos: s.duracionSegundos, anguloMinOverride: s.anguloMinOverride, anguloMaxOverride: s.anguloMaxOverride, videoUrl: s.videoUrl,
      });
      if (r) {
        Object.assign(doc, quitarNulos({
          porcentajeEjecucion: r.porcentajeEjecucion, desviacionPromedio: r.desviacionPromedio,
          repeticionesCompletadas: r.repeticionesCompletadas, repeticionesCorrectas: r.repeticionesCorrectas,
          repeticionesAsignadas: r.repeticionesAsignadas || asignadas,
          repeticiones: (r.detallePorRepeticion || []).map((d) => {
            const p = (d.errores || [])[0];
            const dev = (d.errores || []).filter((x) => x.anguloDetectado != null && x.anguloEsperado != null).map((x) => Math.abs(x.anguloDetectado - x.anguloEsperado));
            return quitarNulos({
              numero: d.numero, porcentaje: d.porcentajeEjecucion, desviacionAngular: dev.length ? dev.reduce((a, b) => a + b, 0) / dev.length : 0,
              error: p ? p.tipo : null, articulacion: p ? p.articulacion : null, errores: (d.errores || []).map(quitarNulos),
            });
          }),
        }));
      }
      await db.collection("sesiones").doc(sesionId).set(doc);
      for (const rec of s._recomendaciones || []) {
        const { id, ...datos } = rec;
        await db.collection("sesiones").doc(sesionId).collection("observaciones").doc(id).set(datos);
      }
      n++;
    }
  }
  console.log("Sesiones migradas:", n);

  if (borrar) {
    for (const u of usuarios.docs) {
      const sesiones = await u.ref.collection("sesiones").get();
      for (const s of sesiones.docs) {
        for (const r of (await s.ref.collection("recomendaciones").get()).docs) await r.ref.delete();
        await s.ref.delete();
      }
    }
    console.log("Sesiones antiguas borradas");
  }
}
main().then(() => process.exit(0)).catch((e) => { console.error(e); process.exit(1); });
