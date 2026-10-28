# Etapa 0 — Análisis de la guía visual en vivo (sin código)

Fecha: 2026-10-05 · Rama `guia-visual-en-vivo` · Fuente: código actual de `app/src/main/java/com/sanna/rehabapp`.
Esta etapa no modifica código. Termina con propuestas para las decisiones abiertas y espera confirmación.

## 1. Cómo funciona hoy la ejecución de una sesión

| Pieza | Archivo | Qué hace hoy |
|---|---|---|
| Pantalla | `feature/sesiones/EjecutarSesionScreen.kt` (467 líneas) | Cinco estados: cargando/error, sin cámara, sin permiso, sesión completada, y dos de ejecución: previa (cámara al 80 % de alto + botón "Iniciar sesión") y en curso (cámara a la izquierda con 1,3 de peso, instrucciones a la derecha, `PanelProgreso` debajo). Sobre la cámara solo hay el badge "Cámara en vivo" y un ícono mínimo ✓/! (`IconoEstadoCorreccion`). La cámara se monta **dos veces** (previa y en curso), por lo que se reconecta al iniciar. |
| ViewModel | `feature/sesiones/EjecutarSesionViewModel.kt` (310 líneas) | Orquesta con una corrutina: cuenta regresiva de 10 s → por cada repetición, cuenta atrás de `duracionSegundos` → descanso fijo de 5 s con aviso por voz a 3 s → `finalizarSesion()`. **La repetición termina por tiempo, no por cumplir un movimiento.** Cada resultado de MediaPipe llega a `procesarResultadoPose()`, que mide y decide ícono y voz (reintento de voz cada 4 s). Soporta reanudar (`numeroRepeticionInicial`, `mergearResultados`) y subir el video. |
| Cámara + IA | `core/camera/CamaraConDeteccionPose.kt` (211 líneas), `core/posedetection/PoseLandmarkerHelper.kt` | CameraX con tres casos de uso: `Preview`, `ImageAnalysis` (4:3, RGBA, solo el último cuadro) y `VideoCapture` (SD, sin audio). Cada cuadro se convierte a `Bitmap`, se **rota y se espeja a mano** (cámara frontal) y se envía a MediaPipe `PoseLandmarker` modo `LIVE_STREAM`, `numPoses = 1`, modelo `pose_landmarker_lite`, confianzas 0,5. El `PreviewView` usa el `ScaleType` por defecto (`FILL_CENTER`, recorta). El resultado trae landmarks normalizados sobre la imagen ya rotada y espejada. |
| Medición | `core/posedetection/ProcesadorMovimiento.kt`, `MedicionLandmarks.kt`, `AnguloCalculator.kt`, `MedicionArticulacion.kt` | Para cada `PatronReferencia` mide el ángulo en el **vértice** de una tripleta de landmarks (`Articulacion.puntoInicial–vertice–puntoFinal`), en **3D** (usa x, y, z). Solo exige visibilidad ≥ 0,5 en el vértice. Compara contra `anguloMin..anguloMax`: dentro = correcto; fuera = "Rango incompleto" o "Desviación angular"; con lado "Ambos", "Movimiento simultáneo". El porcentaje de la sesión es **cuadros dentro de rango / cuadros medidos** durante todo el tiempo de la repetición. Ajusta el lado (`espejo()`) y el ángulo personalizado por sesión. |
| Resultado | `MedicionArticulacion.construirResultadoSesion`, `MergeResultadoSesion.kt` | Agrega por repetición: porcentaje, errores con segundo y ángulo detectado/esperado, repeticiones correctas. `mergearResultados` combina el tramo anterior con el reanudado. |
| Voz | `core/tts/LectorInstrucciones.kt`, `core/posedetection/FrasesCorrectivas.kt` | Frases plantilla por tipo de error. No existe vibración (`Vibrator`) ni pantalla de ajustes. |
| Pruebas | `src/test/.../posedetection` | Hay pruebas de `AnguloCalculator`, `MedicionArticulacion`, `FrasesCorrectivas` y `MergeResultadoSesion`. `ProcesadorMovimiento` y el ViewModel no tienen pruebas. |

Coincidencia importante: el ángulo actual en el vértice entre `puntoInicial` y `puntoFinal` es matemáticamente el mismo ángulo "segmento pivote–distal respecto a la línea pivote–referencia" que pide la guía. **Los rangos de los 12 ejercicios siguen siendo válidos.**

## 2. Modelos de datos actuales

Kotlin (`domain/model`):
- `Ejercicio`: nombre, descripción, categoría, `materialUrl`, `duracionSegundos`, `repeticiones`, `patronesReferencia: List<PatronReferencia>`, `diagnosticosAplicables`, `creadoPor`, `activo`.
- `PatronReferencia(articulacion, anguloMin, anguloMax)`; `Articulacion` (12 valores) con la tripleta de landmarks.
- `Sesion`: ejercicio, fisio, fechas, estado, nota, overrides (`repeticiones`, `duracionSegundos`, `anguloMinOverride/MaxOverride`), `resultado`, `videoUrl`.
- `ResultadoSesion`: promedios, errores agregados, repeticiones completadas/asignadas/correctas, `detallePorRepeticion: List<DetalleRepeticion(numero, porcentajeEjecucion, errores)>`.

Firestore (modelo oficial, ver `CLAUDE.md` §5): `ejercicios/{id}` con `angulosReferencia` (mapa articulación → {min,max}) y `repeticiones {cantidad, duracionSeg}`; `sesiones/{id}` con `repeticiones[]` ({numero, porcentaje, desviacionAngular, error, articulacion, errores[]}). Las reglas permiten al paciente escribir `repeticiones` y `videoUrl` en su sesión, así que **meter el resultado por acción dentro de cada elemento de `repeticiones[]` no exige cambiar reglas**.

## 3. Hallazgos que cambian el plan

1. **El formulario del fisioterapeuta no está en las etapas de la guía.** HU02-CA15 a CA18 (definir acciones, orden, vista de cámara, validaciones) necesitan UI en `EjercicioFormScreen/ViewModel`. Propongo agregarlo como etapa **1b**.
2. **Los 12 ejercicios no tienen datos de acciones.** Hay que sembrar, para cada uno, ángulo de inicio, meta, tolerancia, mantenimiento y tiempo máximo. Son valores clínicos: los propongo yo en la Etapa 1 y los valida el usuario o su fisioterapeuta.
3. **`Articulacion` no dice cuál punto es referencia y cuál es distal.** Para rodilla, el muslo es la referencia y el tobillo el distal; para hombro, la cadera es la referencia y el codo el distal; el orden `puntoInicial/puntoFinal` no es consistente. El ángulo no se ve afectado, pero la guía necesita el rol. Propongo guardar el rol en los datos de la acción y, para el respaldo, una tabla de roles dentro de `Articulacion`.
4. **El ángulo no tiene signo (0°–180°).** Para dibujar el arco y el segmento fantasma hay que saber hacia qué lado gira. Propongo un campo `sentido` en la acción o deducirlo del primer cuadro estable.
5. **El ángulo se calcula en 3D y se dibuja en 2D.** Con vista frontal coinciden; con vista lateral u oblicua puede haber diferencia entre la meta pintada y el ángulo medido. Propongo que la guía use el ángulo en el plano de la pantalla para el progreso y la zona, y conservar el 3D solo para el resultado guardado, o decidir uno solo.
6. **Cambia la lógica de porcentaje.** Hoy es "% de cuadros dentro del rango". Con acciones el resultado es "acción cumplida o no, ángulo máximo, tiempo". Hay que definir cómo se obtiene el `porcentajeEjecucion` por repetición para que los gráficos y promedios actuales no se rompan (propuesta: % de acciones cumplidas ponderado por cercanía a la meta).
7. **La cámara es pequeña y se monta dos veces.** En ejecución ocupa poco más de la mitad del ancho y comparte pantalla con las instrucciones. La guía pide cámara grande con HUD. Propongo cámara a todo el ancho y una sola instancia para toda la ejecución (evita el parpadeo del cambio de estado).
8. **El video grabado no incluirá la guía** (se graba el flujo de la cámara, no el overlay). Para el fisio sigue siendo útil, pero no se verá el recorrido pintado.
9. **La voz repite cada 4 s; la HU pide 3 s.** Hay que ajustar la constante.
10. **El ViewModel tiene lógica de tiempo mezclada con UI.** Conviene extraer la máquina de estados como clase pura, testeable, y que el ViewModel solo la conecte.

## 4. Plan por etapas (archivos a crear y modificar)

| Etapa | Crear | Modificar |
|---|---|---|
| **1 Modelo** | `domain/model/Accion.kt`, `ResultadoAccion.kt`, `VistaCamara.kt`; `AccionPorDefecto` (acción derivada del rango); tabla de roles en `Articulacion`; script `backend/seed/` para sembrar acciones de los 12 ejercicios; pruebas del respaldo | `Ejercicio.kt`, `DetalleRepeticion` (acciones), `EjercicioRepositoryImpl` (leer/escribir `acciones`), `SesionRepositoryImpl` (`acciones` dentro de `repeticiones[]`), `MergeResultadoSesion`, `CLAUDE.md` §5 |
| **1b Formulario del fisio** | — | `EjercicioFormScreen/ViewModel` (lista de acciones, orden, vista de cámara, validaciones de HU02) |
| **2 Mapeo y suavizado** | `core/posedetection/LandmarkMapper.kt`, `OneEuroFilter.kt`, composable de depuración con esqueleto; pruebas del mapeo (frontal/trasera, varias relaciones de aspecto) | `PoseLandmarkerHelper` (entregar el tamaño de la imagen analizada, hoy se ignora), `CamaraConDeteccionPose` (exponer tamaño y rotación), `ProcesadorMovimiento` (visibilidad por landmark de la acción) |
| **3 Motor de cálculo** | `core/posedetection/PoseGuideCalculator.kt`, `GuideFrame.kt`; pruebas con varios landmarks y sentidos | Reutiliza `calcularAngulo` |
| **4 Máquina de estados** | `feature/sesiones/GuideStateMachine.kt` (pura) y sus pruebas | `EjecutarSesionViewModel` (reemplaza el bucle por tiempo; conserva cuenta regresiva, descanso, finalizar, reanudar y subida de video), `ProcesadorMovimiento` (resultados por acción) |
| **5 Overlay** | `core/designsystem/PoseGuideOverlay.kt` (Canvas sobre el `PreviewView`) | `EjecutarSesionScreen` (cámara grande, una sola instancia, HUD con tokens del Design System), `CamaraConDeteccionPose` |
| **6 Voz y vibración** | `core/feedback/Vibrador` (interfaz + implementación + módulo Hilt), almacén de preferencias | `AndroidManifest` (permiso `VIBRATE`), `LectorInstrucciones`/ViewModel (3 s entre repeticiones del mismo mensaje), `PerfilScreen` (interruptores, ver decisión 6) |
| **7 Persistencia y revisión** | Pruebas manuales documentadas | `FisioResultadoSesionScreen/ViewModel` (acciones por repetición), verificación con los 12 ejercicios, rendimiento, `CLAUDE.md` (arquitectura) |

## 5. Riesgos

| Riesgo | Mitigación propuesta |
|---|---|
| **Mapeo de coordenadas.** Los landmarks son normalizados sobre la imagen ya rotada y espejada, y el `PreviewView` recorta con `FILL_CENTER`. | `LandmarkMapper` replica escala + recorte con el tamaño real de imagen y del `PreviewView`; modo de depuración con esqueleto para validar en dispositivo antes de la Etapa 3. |
| **Cámara frontal espejada.** El bitmap ya se espeja a mano; duplicar el espejo desalinea. | Un único punto de espejado, con pruebas unitarias del mapper. |
| **Visibilidad de puntos.** Hoy solo se mira el vértice. | Visibilidad por cada landmark de la acción; cuadro no confiable no avanza ni penaliza. |
| **Sesión que no avanza si no hay persona.** Los cuadros no confiables no avanzan ni penalizan. | Tiempo límite global (decisión 4). Relacionado con ERR-PAC-004 y PB-50. |
| **Rendimiento.** Hoy cada cuadro crea 2 `Bitmap`. Con filtro y overlay se suma carga en el hilo principal. | Dibujar solo el `Canvas` en cada cuadro; el resto del estado de la UI se actualiza solo al cambiar; medir FPS en dispositivo real (el emulador no sirve para esto). |
| **Desfase entre imagen y landmarks.** El resultado llega con 1–3 cuadros de retraso. | Aceptable; documentarlo y evaluarlo en dispositivo. |
| **Arco con ángulo sin signo.** | Campo `sentido` o deducción del primer cuadro estable. |
| **Compatibilidad.** Ejercicios y sesiones existentes sin acciones. | Acción por defecto derivada del rango; todo lo nuevo es opcional en Firestore. |
| **Pruebas.** El emulador solo tiene webcam sin profundidad real ni segundo lado. | Webcam frontal para pruebas básicas; guía de pruebas manuales en teléfono real (cámara frontal y trasera, ambos lados, poca luz, fuera de cuadro, acción no lograda). |

## 6. Propuesta para las decisiones abiertas

1. **Diseño de HU10:** reemplazar el ícono mínimo por la guía (ya redactado). Conservar el ícono ✓/! como indicador adicional pequeño solo si el usuario lo pide.
2. **Duración y ángulo personalizado (HU03):** la duración de la repetición pasa a ser la suma de los tiempos máximos de sus acciones (solo para estimarla); el override de duración deja de aplicarse al avance. El ángulo personalizado reemplaza la **meta de la primera acción** y ajusta su tolerancia al ancho del rango personalizado.
3. **Acción por defecto:** una sola acción con meta = punto medio de `[min,max]`, tolerancia = mitad del ancho, mantenimiento 2 s, tiempo máximo 10 s, ángulo de inicio = a definir por ejercicio (dato clínico). Para los 12 ejercicios del catálogo se siembran acciones explícitas, no derivadas.
4. **Fuera de cuadro:** tras 20 s seguidos sin cuadros confiables, la acción se registra como no completada y la sesión sigue; tras 60 s la sesión se finaliza y se avisa.
5. **Pausa entre repeticiones:** conservar los 5 s y el aviso por voz a 3 s.
6. **Ajustes de voz y vibración:** dos interruptores en el Perfil del paciente, guardados en el dispositivo.
7. **Modelo de datos:** documentar `acciones`, `vistaCamara` y `acciones` dentro de `repeticiones[]` como ampliación fuera del diccionario oficial (igual que `videoUrl`).

## 7. Qué necesito para empezar la Etapa 1

- Confirmación de las 7 propuestas anteriores (o tus cambios).
- Valores clínicos de cada ejercicio (ángulo de inicio, meta, tolerancia, mantenimiento y tiempo máximo): los redacto como borrador y los revisas.
- Si el ejercicio de la demo (Abducción de hombro) debe ser el piloto de la guía.
