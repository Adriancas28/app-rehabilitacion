# Mediciones de validación (EDT 1.8.6)

Esta carpeta contiene las plantillas de las cuatro tablas, los scripts que las completan y la forma de obtener los datos.
Las tablas están vacías ("Por medir"): los números salen solo de ejecuciones reales.

| Paquete | Tabla | Script | Dato de entrada |
|---|---|---|---|
| 1.8.6.1 Error angular | `1.8.6.1_error_angular.md` | `scripts/calcular_error_angular.py` | `1.8.6.1_posturas.csv` (se llena a mano) |
| 1.8.6.2 Latencia | `1.8.6.2_latencia.md` | `scripts/analizar_logcat.py` | registro de logcat |
| 1.8.6.3 Fotogramas por segundo | `1.8.6.3_fps.md` | `scripts/analizar_logcat.py` | el mismo registro de logcat |
| 1.8.6.4 Sesiones registradas | `1.8.6.4_sesiones.md` | `scripts/verificar_sesiones.js` | sesiones finalizadas en Firestore |

Cada script reescribe su tabla. Las plantillas originales están en `plantillas/`.

## Qué se instrumentó

Rama `medicion-validacion`. Con la compilación normal no cambia nada. Solo con `-PmedirRendimiento=true` la app escribe en logcat,
etiqueta `MEDICION`, y muestra en pantalla el ángulo calculado. Los instantes usan el reloj monotónico del dispositivo.

| Evento | Dónde se registra |
|---|---|
| `llegada` del fotograma | `CamaraConDeteccionPose.procesarFrame`, al entrar el fotograma al analizador de CameraX |
| `resultado` | `PoseLandmarkerHelper`, cuando MediaPipe entrega el resultado de ese fotograma |
| `dibujo` | `PoseGuideOverlay`, cuando se dibuja la guía calculada con ese resultado |
| `fase`, `repeticion`, `sesion`, `angulo` | `EjecutarSesionViewModel` (preparación, monitoreo, descanso, fin) |

La latencia de un fotograma es `dibujo - llegada`. Mide hasta que la app emite el dibujo de la guía. No incluye el retardo del sensor
de la cámara ni el de la pantalla; para eso haría falta una cámara externa de alta velocidad.

## Compilar

```bash
# Compilación depurable (sirve con los emuladores de Firebase; añade sobrecarga de depuración)
gradle :app:assembleDebug -PmedirRendimiento=true

# Compilación no depurable, más representativa del rendimiento real (usa el Firebase real y se firma con la clave de debug)
gradle :app:assembleRelease -PmedirRendimiento=true
```

## Pasos exactos para cada medición

1. **Latencia (1.8.6.2) y fotogramas por segundo (1.8.6.3).** En un teléfono físico Android 10 o superior con la compilación `assembleRelease`, ejecutar la sesión de «Abducción de hombro» tres veces seguidas con 3 repeticiones cada una, de pie, de frente y a unos 2 m, con la cadera dentro del encuadre; antes de empezar, `adb logcat -c` y `adb logcat -v threadtime -s MEDICION:I > latencia_<modelo>.log`, y al terminar `python scripts/analizar_logcat.py latencia_<modelo>.log --umbral-lento-ms <valor>`.
2. **Sesiones registradas (1.8.6.4).** En el teléfono físico, ejecutar y finalizar al menos 10 sesiones reales de cualquier ejercicio, algunas con «Finalizar ejercicio» a mitad, y luego, desde `backend`, `node ../docs/pruebas/mediciones/scripts/verificar_sesiones.js --proyecto-real` (con `GOOGLE_APPLICATION_CREDENTIALS`) o `--emulador` si las sesiones se guardaron en los emuladores.
3. **Error angular (1.8.6.1).** En el teléfono físico con la compilación de medición, ejercicio «Abducción de hombro»: mantener el brazo quieto en 30°, 50°, 70°, 90°, 110° y 130° (5 veces cada uno, 30 observaciones), medir cada ángulo de referencia, anotar en `1.8.6.1_posturas.csv` el ángulo que muestra la pantalla (promedio de 2 s) y ejecutar `python scripts/calcular_error_angular.py 1.8.6.1_posturas.csv --actualizar-csv --referencia "<método>"`.

Prueba de humo antes de medir: tras una sesión corta, `grep -c "evento=dibujo" latencia_<modelo>.log` debe ser mayor que cero. Si es cero, la guía no se dibujó (la pose no fue confiable) y no hay latencia que medir.

## Umbral de «cuadro lento» (1.8.6.3)

El script no trae un valor por defecto. Un cuadro es lento cuando pasan más de X ms desde el cuadro procesado anterior.

| Umbral | Equivale a | Respaldo |
|---|---|---|
| 33,3 ms | 30 FPS | Ritmo habitual de la cámara de un teléfono, sin verificar en el dispositivo; casi todo cuadro irregular saldría lento |
| 50 ms | 20 FPS | Valor que ya figura en el proyecto: el plan de la guía visual y el caso 14 de `docs/guia_visual/Pruebas_manuales.md` piden 20 FPS |
| 66,7 ms | 15 FPS | Valor que propone el plan de pruebas del autor, marcado como «a confirmar» |
| 100 ms | 10 FPS | Umbral holgado; solo detecta pausas largas |

Criterio sugerido: 50 ms, porque es el único valor que aparece en la documentación del propio proyecto. La decisión es del autor y del asesor;
el informe muestra el porcentaje para los cuatro umbrales, de modo que el cambio no obliga a medir de nuevo.

## Cuántas posturas para que el error angular sea defendible

Mínimo propuesto: 30 observaciones, con al menos 6 ángulos de referencia distintos repartidos por el rango del ejercicio (por ejemplo 30° a 130°)
y al menos 5 repeticiones por ángulo.

- El error estándar del MAE es la desviación del error dividida por la raíz de n. Con n = 30 es cerca de 0,18 desviaciones; con n = 10 sube a 0,32.
- Con menos de 6 ángulos el resultado solo describe una parte del rango del ejercicio.
- Las 5 repeticiones por ángulo separan el error de la app del temblor de la persona al sostener la postura.
- Si el asesor pide límites de concordancia (Bland y Altman), conviene un tamaño mayor; definirlo con él.

Es un criterio práctico, no un valor normativo; confirmar con el asesor. La referencia debe medir el mismo ángulo que la app: el formado por la línea
hombro-cadera y la línea hombro-codo del lado medido.

## Pruebas de los scripts

Usan datos sintéticos y no son resultados de medición.

```bash
cd docs/pruebas/mediciones/scripts
python -m unittest discover -s tests -v
node tests/probar_verificar_sesiones.js
```
