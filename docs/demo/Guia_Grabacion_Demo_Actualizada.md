# Guía para grabar la demo actualizada (`Demo_SANNA_Actualizada.mp4`)

> Estado: **la demo NO se pudo grabar en el entorno cloud** donde se preparó (sin Android SDK, sin
> `/dev/kvm`, sin `google-services.json`, y la red bloquea `dl.google.com`, `maven.google.com` y
> `firestore.googleapis.com`). Nada de lo siguiente fue ejecutado allí; es el procedimiento para
> hacerlo en tu PC. El video antiguo `Demo_SANNA_3min.mp4` corresponde a una versión previa a la
> guía visual y se conserva sin cambios.

## 1. Preparación (PC con Android Studio, Hyper-V/HAXM/KVM activado)
1. Abrir el proyecto en Android Studio y dejar que sincronice (usa tu `app/google-services.json`).
2. Emulador Pixel_4a (API 34). En `~/.android/avd/Pixel_4a.avd/config.ini`: `hw.camera.front=webcam0`.
3. Cámara: tu webcam real (de pie, a ~2 m, de frente, torso y brazos completos) **o** un video propio
   como cámara virtual (OBS Virtual Camera en Windows; `v4l2loopback` + `ffmpeg -re -stream_loop -1
   -i video.mp4 -f v4l2 /dev/video0` en Linux). Debe ser una persona real moviéndose: la detección es en vivo.
4. Cuentas: paciente Rosa Vargas (ejercicio Abducción de hombro) y fisio Dr. Mendoza, con una sesión
   pendiente (2 repeticiones). Las sesiones anteriores sembradas son datos de demostración: decirlo.
5. Grabar el emulador: `adb shell screenrecord --size 720x1560 /sdcard/c1.mp4` (máx. 3 min por archivo)
   o OBS. Para unir clips sin recodificar: `ffmpeg -f concat -safe 0 -i lista.txt -c copy docs/demo/Demo_SANNA_Actualizada.mp4`.

## 2. Secuencia sugerida (2–4 min), mismo flujo de principio a fin
| # | Escena | Objetivo que evidencia |
|---|---|---|
| 1 | Login paciente → Ejercicios → detalle de Abducción de hombro (ángulo objetivo, nota del fisio) | contexto |
| 2 | "Iniciar sesión": cuenta regresiva de 10 s; colocarse frente a la cámara | OE2 (cámara + pose) |
| 3 | Ejecutar: arco punteado, zona meta, segmento real que cambia rojo→ámbar→verde, HUD (repetición, acción, "Mantén N s"). Segunda repetición **incompleta a propósito** para ver mensaje de corrección | OE2 y OE3 |
| 4 | (Opcional, build debug) mantener presionada la insignia "Cámara en vivo" para mostrar los 33 puntos del esqueleto | OE2 |
| 5 | "Ver resultado": repeticiones, promedio, **Duración medida**, % por repetición | OE1 |
| 6 | Mis resultados → misma sesión | OE1 |
| 7 | Login Dr. Mendoza → Resultados → Rosa → **esa misma sesión**: acciones completadas/incompletas, ángulo detectado vs meta, segundo del error, duración medida, "Ver video de la sesión" | OE1 |
| 8 | (Opcional) Firebase Console → `sesiones/{id}`: `repeticiones[].acciones[]` con `tiempoSeg` | OE1 |

## 3. Qué anunciar solo si lo compruebas
- **Voz**: confirmar que suena en el emulador antes de mencionarla (puede no sonar).
- **Vibración**: el emulador no vibra visiblemente; solo afirmarla con un teléfono físico.
- Latencia ≤ 500 ms y ≥ 20 FPS: casos 14 de `docs/guia_visual/Pruebas_manuales.md`, aún sin medir.

## 4. Verificación previa recomendada
`./gradlew :app:testDebugUnitTest` (incluye la prueba nueva de `duracionMedidaSegundos`) y
`./gradlew :app:installDebug`. Luego recorrer los casos 1–13 de `Pruebas_manuales.md`.
