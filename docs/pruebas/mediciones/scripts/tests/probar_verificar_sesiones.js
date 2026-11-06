// Pruebas de evaluarSesion con sesiones SINTÉTICAS. Solo comprueban la lógica del script;
// no son resultados de medición. Ejecutar: node tests/probar_verificar_sesiones.js (desde scripts)
const assert = require("assert");
const { evaluarSesion, resumir } = require("../verificar_sesiones.js");

const accion = (orden) => ({ ordenAccion: orden, anguloMaximo: 90, completada: true, tiempoSeg: 3, desviacionAngular: 2 });
const rep = (n) => ({ numero: n, porcentaje: 100, desviacionAngular: 0, acciones: [accion(1), accion(2)] });
const completa = () => ({
  estado: "completada", fechaEjecucion: new Date(), porcentajeEjecucion: 90, desviacionPromedio: 3,
  repeticionesAsignadas: 3, repeticionesCompletadas: 2, repeticionesCorrectas: 2, repeticiones: [rep(1), rep(2)],
});

// 1. una sesión completa no tiene faltas
assert.deepStrictEqual(evaluarSesion(completa()), []);

// 2. sin fecha de ejecución falla CA01
let s = completa(); delete s.fechaEjecucion;
assert.deepStrictEqual(evaluarSesion(s), [{ ca: "CA01", campo: "fechaEjecucion" }]);

// 3. una acción sin tiempo falla CA05 con su ruta exacta
s = completa(); delete s.repeticiones[1].acciones[0].tiempoSeg;
assert.deepStrictEqual(evaluarSesion(s), [{ ca: "CA05", campo: "repeticiones[1].acciones[0].tiempoSeg" }]);

// 4. una sesión sin repeticiones registradas falla CA02 y CA05
s = completa(); s.repeticiones = []; s.repeticionesCompletadas = 0;
const faltas = evaluarSesion(s);
assert.ok(faltas.some((f) => f.ca === "CA02") && faltas.some((f) => f.ca === "CA05"));

// 5. repeticionesCompletadas distinto del detalle falla CA05
s = completa(); s.repeticionesCompletadas = 3;
assert.ok(evaluarSesion(s).some((f) => f.ca === "CA05" && f.campo.includes("hay 2")));

// 6. un campo numérico con texto falla
s = completa(); s.porcentajeEjecucion = "90";
assert.deepStrictEqual(evaluarSesion(s), [{ ca: "CA01", campo: "porcentajeEjecucion" }]);

// 7. el resumen cuenta finalizadas, completas y porcentaje
const mala = completa(); mala.repeticiones[0].acciones = [];
const r = resumir([{ id: "a", datos: completa() }, { id: "b", datos: completa() }, { id: "c", datos: mala }]);
assert.strictEqual(r.finalizadas, 3);
assert.strictEqual(r.completas, 2);
assert.ok(Math.abs(r.porcentaje - 66.6667) < 0.001);
assert.strictEqual(r.incompletas[0].id, "c");
assert.deepStrictEqual(r.conFallaPorCriterio, { CA01: 0, CA02: 0, CA05: 1 });

// 8. sin sesiones el porcentaje no se inventa
assert.strictEqual(resumir([]).porcentaje, null);

console.log("verificar_sesiones: 8 comprobaciones correctas");
