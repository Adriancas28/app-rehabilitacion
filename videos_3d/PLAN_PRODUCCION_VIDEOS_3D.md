# Plan de Producción de Videos 3D — Ejercicios SANNA

> Documento de trabajo, no forma parte de `CLAUDE.md` (que sigue siendo el
> único archivo de contexto del proyecto). Este archivo persiste el plan
> completo y el avance del piloto para no perderlo entre sesiones — ver
> puntero corto en `CLAUDE.md`.

## Estado actual

- **Fase**: piloto en curso — ejercicio **SDS-02 (Flexión de hombro derecho)**.
- **Blender detectado en la máquina**: `C:\Program Files\Blender Foundation\Blender 5.2\blender.exe` — se usa headless (`--background --python script.py`) para automatizar todo lo posible.
- **Estructura de carpetas creada**:
  ```
  D:\APP-REHABILITACION\videos_3d\
    SDS-02\
      blender\      ← .blend de trabajo
      renders\      ← secuencia PNG del render
      exports\      ← .mp4 final
      preview\      ← vista previa rápida
  ```
- **Próximo paso pendiente**: script headless que verifica versión de Blender, intenta activar el add-on Rigify, y guarda `blender/SDS-02_base.blend` como punto de partida.

## Reglas de trabajo acordadas para el piloto

- Interactivo, paso a paso: un paso → comprobación → validación del usuario → recién el siguiente paso.
- Cuando sea posible, Claude ejecuta scripts/Python/Blender headless directamente (no solo instrucciones para clic manual).
- Si algo no se puede automatizar de forma fiable, se marca explícitamente **"ESTO DEBES HACERLO MANUALMENTE EN BLENDER"** con instrucciones exactas de dónde hacer clic.
- No se toca código Android/Firebase hasta que el usuario escriba **"VIDEO APROBADO"**.

## Datos verificados del ejercicio piloto (sin inconsistencias con el proyecto)

| Campo | Valor |
|---|---|
| Ejercicio | SDS-02 — Flexión de hombro |
| Categoría | Movilidad |
| Articulación | Hombro derecho (`Articulacion.HOMBRO_DERECHO`, landmarks MediaPipe 14-12-24) |
| Vista de cámara | Lateral |
| Repeticiones (app real) | 3 |
| Rango de referencia (seed) | 70°–110° |
| Duración objetivo del video piloto | 10–15 s |
| Salida final | `SDS-02_Flexion_Hombro_Derecho.mp4` |

## 1. Resumen

El proyecto tiene un catálogo cerrado de 12 ejercicios MVP (sección 8 de
`CLAUDE.md`), todos con `materialUrl` vacío a propósito. Este plan cubre
qué videos hacen falta, con qué estilo visual y flujo en Blender, cómo
deben diseñarse para no contradecir lo que MediaPipe mide después, y un
piloto único (SDS-02) para validar el pipeline completo antes de producir
los 11 restantes.

## 2. Ejercicios identificados

Los 12 ejercicios existen en código (`backend/seed/crear-catalogo-ejercicios.ts`)
y documentación (`CLAUDE.md` sección 8) — no hay ejercicios "solo en
documentación", el catálogo anterior de 9 y HOM-01 fueron descartados.

| ID | Ejercicio | Zona corporal | Movimiento principal | Complejidad de animación | Video requerido |
|---|---|---|---|---|---|
| SDS-01 | Abducción de hombro | Hombro derecho | Elevar brazo lateralmente | Baja | Sí |
| SDS-02 | Flexión de hombro | Hombro derecho | Elevar brazo al frente | Baja | Sí |
| SDS-04 | Flexión unilateral alternada de hombro | Hombro derecho | Elevar/bajar un brazo, alternando | Baja | Sí |
| OAR-01 | Extensión de rodilla en sedestación | Rodilla derecha | Extender pierna sentado | Baja-Media (silla) | Sí |
| OAR-02 | Flexión de rodilla en bipedestación | Rodilla derecha | Flexionar rodilla de pie | Baja | Sí |
| LUM-01 | Bisagra de cadera (Hip Hinge) | Cadera derecha | Inclinar tronco desde cadera | Media | Sí |
| LUM-03 | Inclinación lateral de tronco | Tronco | Inclinar tronco lateralmente | Media | Sí |
| LUM-02 | Marcha estacionaria con control de tronco | Cadera derecha | Marchar en el sitio | Media | Sí |
| SDS-03 | Trepado de dedos en pared | Hombro derecho | Subir dedos por una pared | Media-Alta (prop pared) | Sí |
| OAR-03 | Mini-sentadilla | Rodilla derecha | Sentadilla parcial | Media-Alta (equilibrio, 2 vistas) | Sí |
| LUM-04 | Puente de glúteo | Cadera derecha | Elevar cadera acostado | Alta (supino, cámara baja) | Sí |
| OAR-04 | Levantarse de silla (Sit-to-Stand) | Rodilla derecha | Transición sentado→de pie | Alta (prop silla, oclusión) | Sí |

Recursos compartibles: mismo modelo/rig base y escenario neutro para los
12. OAR-01/OAR-04 comparten prop silla; SDS-03 necesita prop pared;
SDS-01/02/04 comparten pose base de pie.

Riesgo ya documentado en el proyecto: LUM-04, OAR-04 y SDS-03 son los 3 de
mayor riesgo técnico para la estimación de pose real — relevante para
priorizar validación de la app, no para elegir el piloto de *producción de
video* (por eso el piloto es SDS-02, no uno de estos 3).

## 3. Estilo visual — decisión

Evaluadas 4 opciones (anatómico simplificado / avatar médico / minimalista
tipo guía de ejercicios / low-poly neutro). **Elegida: low-poly/"paper
doll" neutro, sin género marcado, sin rasgos faciales, colores planos,
silueta clara** — dirige la atención al ángulo articular, no a la
superficie del cuerpo; reduce tiempo de rig/skinning; es el estilo que ya
usan apps de referencia del mismo dominio (Kaia Health, Physitrack, Hinge
Health); un solo asset base sirve para los 12 ejercicios.

## 4. Flujo Blender (decisiones fijas para todos los videos)

1. Modelo: generado/rigueado con **Rigify** (add-on incluido en Blender),
   no modelado desde cero.
2. Rig: Armature Rigify humanoide completo (IK en brazos/piernas).
3. Motor de render: **Eevee** (no Cycles) — suficiente para el estilo
   plano, mucho más rápido.
4. Cámara fija por toma (sin animación de cámara), encuadre según la
   `Vista de cámara` del ejercicio (Lateral/Frontal, ya definida en el
   catálogo).
5. Iluminación: 3 puntos (key/fill/rim), luces de área, sombras suaves.
6. Fondo: color plano sólido (coherente con `GrisFondo #F5F6F8` del Design
   System de la app).
7. Exportación: secuencia PNG desde Blender → ensamblado a video con
   FFmpeg (evita perder el render si el encoder de video de Blender falla
   a mitad de proceso).
8. Formato final: **MP4 (H.264)**, sin audio.
9. Resolución: **720×1280** (vertical, 9:16 — coincide con la orientación
   real de la app).
10. FPS: **30**.
11. Optimización: CRF ~26-28, bitrate objetivo ~1-1.5 Mbps.

## 5. Plantilla estándar de video (2 variantes)

**Plantilla A — cíclico/repetitivo** (SDS-01/02/04, OAR-01/02/03,
LUM-01/02/03), 10-12s: posición inicial (0-1.5s) → ciclo 1 (1.5-4s) →
pausa (4-4.5s) → ciclo 2 (4.5-7s) → pausa final (7-8.5s).

**Plantilla B — transición única** (LUM-04, OAR-04), 12-15s: posición
inicial (0-2s) → movimiento principal lento (2-6s) → sostener posición
final (6-8s) → regreso (8-11s) → pausa final (11-13s).

SDS-02 usa **Plantilla A**.

## 6. Información visual — qué SÍ y qué NO va en el video

**Sí**: modelo 3D animado, flechas de dirección sutiles, indicador sutil
en la articulación activa, timing que resalta posición inicial/final.

**No** (ya lo resuelve la UI/voz de la app, duplicar lo satura y no se
puede editar sin re-renderizar): nombre del ejercicio, ángulos numéricos
superpuestos, texto de instrucciones quemado, contador de repeticiones.

## 7. Relación con la estimación de pose (crítico)

El video debe ser geométricamente consistente con lo que MediaPipe medirá
después, no una coreografía libre:

- Vista de cámara = la ya definida por ejercicio (Lateral/Frontal).
- Rango de movimiento del modelo = `anguloMin`/`anguloMax` real del
  ejercicio (para SDS-02: 70°-110°).
- Los 3 landmarks de la tripleta (`puntoInicial`-`vertice`-`puntoFinal`
  del enum `Articulacion`) visibles y sin oclusión durante todo el clip.
- Un solo plano de movimiento dominante (MediaPipe Pose es 2D con
  profundidad limitada).
- Mostrar siempre el **lado derecho** del modelo (coherente con que el
  catálogo está definido con ese lado por defecto; `Articulacion.espejo()`
  ya resuelve el lado real del paciente en la app, no hace falta un video
  espejado).

## 8. Ejercicio piloto — por qué SDS-02

Elegido explícitamente por ser el **más simple** de animar (de pie, un
solo eje, sin props, vista lateral, landmarks de hombro muy estables) —
correcto para un piloto de *pipeline*: si algo falla aquí (formato,
tamaño, reproducción en Android), es un problema del pipeline, no del
ejercicio. Los ejercicios de mayor riesgo real para pose estimation
(LUM-04, OAR-04, SDS-03) se dejan para después de validar el pipeline.

## 9. Plan de fases del piloto (resumen — detalle paso a paso se ejecuta interactivo)

1. Preparación (estructura de carpetas, verificación de Blender/Rigify).
2. Modelo 3D (Rigify meta-rig humano).
3. Rig (verificar brazo derecho, hombro, codo, muñeca, sin deformaciones).
4. Animación (keyframes 70°→110°→70°, Plantilla A, ease in/out).
5. Cámara (lateral fija, encuadre cabeza a pies con margen).
6. Iluminación/estilo (3 puntos, fondo plano).
7. Render (Eevee, 720x1280, 30fps, secuencia PNG).
8. Optimización (FFmpeg → MP4 H.264, CRF ~27, sin audio).
9. Integración Android (pendiente de "VIDEO APROBADO" — ver sección 12).
10. Pruebas en celular real.
11. Validación contra checklist (sección 11).

## 10. Integración Android (diseño, NO implementar hasta "VIDEO APROBADO")

- Almacenamiento: **Firebase Storage** (ya es la tecnología elegida y
  documentada, HU02-CA03/CA07).
- Identificación: el modelo `Ejercicio` **ya tiene el campo exacto**
  (`materialUrl` en `ejercicios/{id}`) — no se cambia el modelo de datos,
  solo se llena ese campo.
- Reproducción: `ExoPlayer`/Media3 apuntando a la URL de descarga de
  Storage (streaming, no descarga completa antes de reproducir).
- Remoto con caché (`SimpleCache` de Media3), no empaquetar videos en el
  APK.
- **Gap detectado**: hoy no existe pantalla/reproductor de video en la
  app (HU05 no tiene UI de reproducción todavía) — es una tarea de código
  pendiente para después del video aprobado, no parte de la producción
  del video en sí.

## 11. Criterios de calidad (checklist previa a aprobar el MP4)

**Animación**: movimiento dentro de 70°-110°, interpolación suave sin
saltos, posición inicial/final distinguibles, sin rotación de tronco no
intencional.

**Visualización**: se entiende sin texto/audio, vista lateral correcta,
las 3 articulaciones del patrón visibles sin oclusión.

**Técnica**: 720x1280 @ 30fps, MP4/H.264 sin audio, 1-3 MB para 10-15s,
reproduce fluido en Android real.

**Integración** (una vez exista el reproductor): `materialUrl` correcto,
reproduce dentro del flujo real, sin errores/ANR, falla controlada sin
conexión.

## 12. Roadmap de producción completa (después del piloto aprobado)

| Complejidad | Ejercicios | Cantidad |
|---|---|---|
| Fácil | SDS-01, SDS-02, SDS-04, OAR-02 | 4 |
| Media | LUM-01, LUM-02, LUM-03, OAR-01 | 4 |
| Difícil | SDS-03, OAR-03, LUM-04, OAR-04 | 4 |

Estimaciones (una vez el .blend plantilla base esté validado por el
piloto): fácil ~30-45min animación + 15-30min render; media ~45-90min +
30-45min; difícil ~1.5-3h + 30-60min. Variables que alteran esto:
experiencia en Blender, si hay que crear props nuevos, potencia de GPU,
iteraciones de corrección por video.

## 13. Riesgos

- No existe reproductor de video en la app todavía (ver sección 10).
- Curva de aprendizaje de Blender/Rigify si no hay experiencia previa.
- Licencias de assets externos si se usa un modelo descargado (no aplica
  al piloto: se genera con Rigify, sin assets externos).
- Consistencia visual entre los 12 videos si no se reutiliza el mismo
  `.blend` plantilla como fuente única de verdad.

## Bitácora de avance

- **2026-09-06**: plan completo escrito y guardado aquí. Estructura de
  carpetas `videos_3d/SDS-02/{blender,renders,exports,preview}` creada.
  Blender 5.2 detectado en la máquina. Próximo paso: script headless de
  verificación + activación de Rigify + `.blend` base.
- **2026-09-06 (cont.)**: `00_setup.py` — Rigify activado, `SDS-02_base.blend`
  guardado (escena vacía).
- **2026-09-06 (cont.)**: `01_generar_rig.py` — meta-rig humano Rigify
  generado (159 huesos) → rig de control final `rig_SDS02` (706 huesos),
  huesos FK del brazo derecho confirmados (`upper_arm_fk.R`,
  `forearm_fk.R`, `hand_fk.R`). Guardado en `SDS-02_rig.blend`.
- **2026-09-06 (cont.)**: `02_verificar_rig.py` — verificado que la
  cadena FK hombro→codo→muñeca se mueve correctamente y de forma aislada
  (brazo izquierdo no se ve afectado). Guardado en
  `SDS-02_rig_verificado.blend`.
- **2026-09-06 (cont.)**: `03_calibrar_angulo.py` — **calibración
  angular** entre la rotación del hueso `upper_arm_fk.R` (eje X local) y
  el ángulo real que mide la app (vértice 12-hombro, entre landmarks
  14-elbow y 24-hip, igual convención que `Articulacion.HOMBRO_DERECHO`).
  Hallazgo: la A-pose de reposo del metarig de Rigify ya equivale a
  **72.8°** en esta convención (no 0°, el brazo de reposo no cuelga
  recto). Calibración exacta encontrada por barrido:
  - **rot_x = +10°** → ángulo real 69.94° ≈ **70°** (posición inicial)
  - **rot_x = -99°** → ángulo real 109.95° ≈ **110°** (posición máxima)

  Estos son los valores de rotación del hueso `upper_arm_fk.R` a usar como
  keyframes en la Fase 4 (animación), en vez de los -80°/0° usados solo
  como prueba genérica en la Fase 3.
- **2026-09-06 (cont.)**: `04_animar.py` — animación creada con Plantilla
  A estirada a 10.0s exactos (30fps, frames 1-301): pausa inicial (0-2s) →
  ciclo 1 (2-4.5s, pico en 3.25s) → pausa (4.5-5s) → ciclo 2 (5-7.5s, pico
  en 6.25s) → pausa final (7.5-10s). Interpolación Bezier/Ease-In-Out en
  los 8 keyframes. **Nota técnica para scripts futuros**: Blender 5.x usa
  Actions "en capas" (slots/layers/channelbags) — ya no existe
  `action.fcurves` directo, hay que navegar
  `action.layers[0].strips[0].channelbag(action.slots[0]).fcurves`.
  Guardado en `SDS-02_animado.blend`.
- **2026-09-06 (cont.)**: `04b_verificar_animacion.py` — verificado por
  muestreo (14 frames a lo largo de los 10s) que el ángulo real oscila
  exactamente entre **69.94° y 109.95°** en todo momento, sin overshoot
  fuera del rango 70-110 en ningún punto de la curva Bezier.
- **2026-09-06 (cont.)**: `05_construir_cuerpo.py` — cuerpo low-poly
  neutro (14 segmentos: cabeza esfera, torso caja, brazos/piernas
  cilindros) parentado rígidamente vía BONE parenting
  (`keep_transform=True`) a los huesos FK correspondientes — sin
  skinning/pesos, cada segmento sigue su hueso como pieza rígida (estilo
  "paper doll"). **Bug encontrado y corregido**: el torso se calculó mal
  al asumir que el hueso `torso`/`chest` de Rigify abarca cadera→pecho —
  en realidad `torso` es solo un pivote de control corto. Se recalculó
  usando la cadera real (promedio de `thigh_fk.L/.R.head`) hasta la base
  del cuello (`head.head`). Guardado en `SDS-02_con_cuerpo.blend`.
- **2026-09-06 (cont.)**: `06_camara_luces_fondo.py` — cámara lateral
  fija, 3 luces de área (key/fill/rim), fondo plano (`#F5F6F8` del Design
  System), motor **Eevee** (`BLENDER_EEVEE`, Blender 5.x ya no permitió
  `BLENDER_EEVEE_NEXT` en este build), resolución 720×1280 @ 30fps.
  **2 bugs encontrados y corregidos**:
  1. `sensor_fit` por defecto no cubría la altura completa del cuerpo en
     formato retrato (piernas fuera de cuadro) → se fijó
     `sensor_fit='VERTICAL'` y lente 24mm.
  2. La A-pose del metarig separa las piernas en el eje X, el mismo eje
     de una vista lateral pura → quedaban casi invisibles/de canto. Se
     giró la cámara ~25° (lateral-oblicua) para revelarlas sin perder
     claridad del movimiento de hombro (que ocurre en el plano Y-Z).
- **2026-09-06 (cont.)**: `07_render_prueba.py` — renders de prueba
  (frames 1, 60, 99) para validar visualmente antes del render completo.
- **2026-09-06 (cont.)**: `08_render_final.py` — render completo de los
  301 frames a PNG (Eevee, 32 samples), ~570s totales (~1.9s/frame).
- **2026-09-06 (cont.)**: `09_exportar_video.py` — intento de ensamblar
  el MP4 vía el Video Sequence Editor de Blender (Image Strip + salida
  FFmpeg). **API renombrada en Blender 5.x** (notas para scripts
  futuros): `se.sequences`→`se.strips`, y
  `image_settings.file_format='FFMPEG'` requiere primero
  `image_settings.media_type='VIDEO'` (no `'MOVIE'`). El MP4 se generó
  pero con un **bug de color** (salida lavada/sin contraste — probable
  doble aplicación de la gestión de color del VSE sobre PNGs ya
  transformados). **Solución aplicada**: se descartó el VSE y se
  ensambló el video directo desde los 301 PNG ya renderizados (correctos)
  usando OpenCV (`cv2.VideoWriter`, códec `mp4v`) — evita el pipeline de
  color del VSE por completo. Resultado verificado con `cv2.VideoCapture`:
  **720×1280, 30.0 fps, 301 frames, 10.03s**, contraste correcto
  (confirmado visualmente en frames 0 y 98).
- **2026-09-06 (cont.) — cambio de modelo base**: el cuerpo low-poly
  armado a mano (v1 rígido, v2 con Automatic Weights) se veía "raro"
  (costuras visibles / v2 directamente no se animaba — ver hallazgo de
  la sesión: el Automatic Weights de Blender falla con geometría de
  primitivas superpuestas, la difusión de calor no encuentra una
  superficie limpia). El usuario descargó manualmente un personaje real
  de **Mixamo** ("Y Bot", T-pose, FBX Binary) — no se manejaron
  credenciales de la cuenta Adobe del usuario, el login lo hizo él mismo.
  - `10_importar_ybot.py`: importado `Y Bot.fbx` (add-on `io_scene_fbx`,
    ya incluido en Blender). Trae 2 mallas ya skinneadas de fábrica
    (`Alpha_Surface` 17336 verts, `Alpha_Joints` 10514 verts — ambas son
    parte del diseño visual normal del personaje, no assets de debug) +
    armature `Armature` (65 huesos, prefijo `mixamorig:`).
  - `12_calibrar_ybot.py` / `13_calibrar_fino_ybot.py`: recalibración del
    ángulo igual que antes pero en el hueso `mixamorig:RightArm`
    (equivalente a `upper_arm_fk.R`), usando `mixamorig:RightForeArm`
    (codo) y `mixamorig:RightUpLeg` (cadera) como proxies de los
    landmarks 14/24. Mismo eje (X local) que en el rig anterior.
    Calibración: **rot_x=31° → 69.97° (~70°)**, **rot_x=-9° → 109.68°
    (~110°)**.
  - `14_animar_ybot.py`: misma Plantilla A (10s, 301 frames) sobre
    `mixamorig:RightArm`.
  - `15_camara_luces_ybot.py` / `16_render_prueba_ybot.py`: mismo criterio
    de cámara lateral-oblicua (25°), 3 luces, fondo plano. Verificado
    visualmente: brazo baja/sube correctamente (70°→110°), **sin
    costuras** (malla continua con skinning real de Mixamo).
  - `17_render_final_ybot.py`: render completo 301 frames a
    `renders_ybot/` (Eevee, ~606s).
  - Ensamblado a MP4 con el mismo método OpenCV (`cv2.VideoWriter`,
    códec `mp4v`) que ya se usó para evitar el bug de color del VSE de
    Blender.
- **Archivo final (versión con personaje Mixamo)**:
  `videos_3d/SDS-02/exports/SDS-02_Flexion_Hombro_Derecho.mp4` (479 KB,
  720×1280, 30fps, 301 frames, 10.03s). Verificado visualmente en los
  frames 0 (70°) y 98 (110°): movimiento correcto, sin costuras, contraste
  normal. **Pendiente de aprobación del usuario** ("VIDEO APROBADO")
  antes de iniciar la Fase 12 (integración Android).
