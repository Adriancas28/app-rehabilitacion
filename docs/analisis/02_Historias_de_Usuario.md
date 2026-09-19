# 2. Historias de Usuario actualizadas (según la implementación real)

Base: HU/RNF de `CLAUDE.md` §10 contrastadas con el código (`feature/*`, `core/*`) de la rama `mejora-videos`. Solo se listan criterios que el sistema realmente cumple; lo documentado que no existe va en **Diferencias**. "Estado" es el estado según código; el resultado de las pruebas está en `05_Pruebas_y_Errores.md`.

Leyenda de estado: **Implementada** · **Implementada con diferencias** · **Implementada por plataforma** (la cumple Firestore/Android sin código propio) · **No verificable en emulador**.
Pantallas: ver códigos en `01_Pantallas_y_Capturas.md`.

## Épica 01 — Gestionar sesiones terapéuticas

### HU01 — Gestionar pacientes terapéuticos · Fisioterapeuta
Como fisioterapeuta quiero visualizar y gestionar mis pacientes asignados para hacer el seguimiento de sus sesiones.
- CA01 Al acceder, se muestra la lista de pacientes asignados (F01).
- CA02 Al seleccionar uno, se muestra su información terapéutica: diagnóstico(s) y progreso (F02).
- CA03 El detalle muestra progreso y sesiones registradas (F02).
- CA04 Un buscador filtra la lista por texto ("Buscar paciente…").
- CA05 Sin pacientes: "Aún no tienes pacientes asignados." Sin coincidencias: "No se encontraron pacientes con ese criterio."
- CA06 El fisioterapeuta elige uno o más diagnósticos de un catálogo cerrado de 3 (Lumbalgia inespecífica, Osteoartritis de rodilla, Síndrome de dolor subacromial), cada uno con su fecha; se muestran en el detalle y en la lista (F05).
- **Estado:** Implementada. **Pantallas:** F01, F02, F05.

### HU02 — Gestionar ejercicios terapéuticos · Fisioterapeuta
Como fisioterapeuta quiero registrar y administrar ejercicios para asignarlos a mis pacientes.
- CA01 "Registrar ejercicio" abre el formulario (F09). CA02 Guarda nombre, descripción, categoría, duración por repetición y repeticiones (la duración debe ser > 0).
- CA03 Permite asociar imagen o video como material. CA04 El catálogo lista los ejercicios (F08). CA05 Editar. CA06 Eliminar con confirmación.
- CA07 "Calcular rango automáticamente" analiza el video seleccionado en el dispositivo y completa mín./máx. por articulación (editable después).
- CA08 Guarda repeticiones y duración por repetición. CA09 Categoría restringida a Movilidad / Control motor; diagnósticos aplicables opcionales.
- CA10 Desactivar/Activar un ejercicio sin perder el historial (menú "⋮").
- **Estado:** Implementada. **Pantallas:** F08, F09, F10.

### HU03 — Asignar sesiones terapéuticas · Fisioterapeuta
Como fisioterapeuta quiero asignar sesiones a mis pacientes para indicar los ejercicios que deben realizar.
- CA01 Desde el detalle del paciente, ícono "+" abre el formulario (F06). CA02 Guarda ejercicio y fecha de asignación. CA03 Una sesión pendiente se edita (ejercicio, fecha, parámetros).
- CA04 El paciente ve las sesiones asignadas (P01). CA05 Nota opcional. CA06 Repeticiones y duración por repetición editables solo para esa sesión; se muestra la duración estimada total.
- CA07 Los ejercicios sugeridos para el diagnóstico del paciente aparecen primero con ★. CA08 "Personalizar ángulo objetivo" (mín./máx.) solo para esa sesión; el paciente lo ve en la tarjeta "Ángulo objetivo" (P02).
- **Diferencias:** el aviso al paciente "Este video es referencial. Tu fisioterapeuta ha ajustado este ejercicio…" descrito en CLAUDE.md **ya no existe**: el paciente ve el ángulo personalizado en la tarjeta "Ángulo objetivo" y la nota en "Nota de tu fisioterapeuta".
- **Estado:** Implementada con diferencias. **Pantallas:** F06, F07, P01, P02.

### HU04 — Visualizar ejercicios asignados · Paciente
Como paciente quiero ver los ejercicios asignados y el detalle de cada uno para saber qué hacer y cómo.
- CA01 "Ejercicios" muestra sesiones asignadas (próxima sesión, reanudable, lista). CA02 Sin ejercicios: mensaje de ausencia.
- CA03 Al seleccionar uno: video/imagen e instrucciones. CA04 Muestra el ángulo objetivo (personalizado o del ejercicio). CA05 Muestra las repeticiones. CA06 Muestra la nota del fisioterapeuta si existe (y las recomendaciones registradas para esa sesión).
- El botón "Iniciar sesión" del detalle solo aparece si la sesión está pendiente; el de "Próxima sesión" abre primero el detalle.
- **Estado:** Implementada. **Pantallas:** P01, P02.

### HU05 — Consultar material terapéutico · Paciente
Como paciente quiero consultar el material de apoyo para prepararme antes de la sesión.
- CA01 El detalle del ejercicio muestra el material asociado (si tiene). CA02 Reproduce video (controles, barra de progreso); imágenes se muestran fijas. CA03 Muestra las instrucciones. CA04 Se puede ver antes de iniciar. Añadido: botón "Ampliar video" a pantalla casi completa.
- Los 12 ejercicios del catálogo tienen video (subidos 2026-09-19).
- **Estado:** Implementada. **Pantallas:** P02, P03.

## Épica 02 — Monitorear la ejecución

### HU06 — Ejecutar sesión terapéutica · Paciente
Como paciente quiero ejecutar la sesión asignada frente a la cámara para realizar mis ejercicios con monitoreo.
- CA01 Muestra el ejercicio. CA02 "Iniciar sesión" activa la cámara y una cuenta regresiva de 10 s. CA03 Al terminar la cuenta regresiva inicia el monitoreo. CA04/CA05 Al completar todas las repeticiones se concluye y se registra el resultado.
- CA06 Con más de una repetición, pausa breve y aviso por voz 3 s antes de la siguiente. CA07 "Finalizar ejercicio" registra lo medido hasta ese momento; "Salir" no registra. CA08 Lee en voz alta la instrucción una sola vez. CA09 Una sesión finalizada antes de tiempo se ofrece como "Reanudar sesión" (X/Y) y continúa desde la repetición siguiente, combinando resultados.
- Pide permiso de cámara; sin cámara muestra "Este dispositivo no tiene cámara disponible…".
- **Estado:** Implementada. **No verificable en emulador:** el monitoreo con una persona real (la cámara virtual del emulador no tiene persona). **Pantallas:** P04–P07, P01.

### HU07 — Monitorear movimiento corporal · Sistema
- CA01 Detecta pose con MediaPipe en el dispositivo. CA02 Identifica landmarks. CA03 Con varias personas usa una sola pose (`numPoses = 1`). CA04 Procesa la información para el análisis.
- **Estado:** Implementada (código); **no verificable en emulador** con persona. Pruebas unitarias: `AnguloCalculatorTest`, `MedicionArticulacionTest`.

### HU08 — Procesar movimiento corporal · Sistema
- CA01 Calcula ángulos articulares. CA02 Compara con el ROM esperado (o el personalizado). CA03 Continúa ante oclusiones. CA04 Genera resumen de ángulos y desviaciones al finalizar.
- **Estado:** Implementada (código + pruebas unitarias); no verificable en emulador con persona.

### HU09 — Analizar ejecución terapéutica · Sistema
- CA01 Compara con el patrón de referencia. CA02 Clasifica el error (Rango incompleto, Desviación angular, Movimiento simultáneo para lado AMBOS). CA03 Genera % de ejecución y desviación promedio.
- **Estado:** Implementada (código + `MedicionArticulacionTest`).

## Épica 03 — Retroalimentación

### HU10 — Retroalimentación inmediata · Paciente
- CA01/CA02 Ícono visual mínimo: correcto (check) o corregir (alerta); sin texto de corrección en pantalla (decisión de diseño). CA03 Se actualiza de forma continua. CA05 Se detiene al finalizar.
- CA06 Indicación por voz (TextToSpeech nativo) con frases plantilla por tipo de error. CA07 Los errores repetidos se acumulan por repetición en el resultado (visible para el fisioterapeuta).
- CA04 (latencia ≤ 500 ms): **no medida** (no hay instrumentación ni prueba de latencia en el código).
- **Estado:** Implementada (código + `FrasesCorrectivasTest`); latencia no verificada.

### HU11 — Visualizar resultados y % de ejecución · Paciente
- CA01 Al terminar, "Ver resultado" abre "¡Ejercicio completado!". CA02 Repeticiones completadas/asignadas. CA03 Promedio. CA04 Lista de todas las repeticiones con su %. CA05/CA06 Verde ≥ 75 %, ámbar < 75 %. CA07 Sesión parcial: muestra X/Y reales. CA08 "Ir a mi progreso" → "Mis resultados". CA09 Aclaración de que el cálculo (MediaPipe) es referencial.
- **Diferencia:** ya no existe el modo "solo lectura" ni el desglose "Correctas/Errores" de la versión original.
- **Estado:** Implementada con diferencias. **Pantalla:** P08.

## Épica 04 — Seguimiento

### HU12 — Visualizar progreso y evolución · Fisioterapeuta
- CA01 En el detalle del paciente: progreso (completadas/total, promedio de ejecución), gráficos de evolución (línea) y precisión por sesión (barras), progreso por ejercicio (barras). CA02 Resultados comparativos por sesión. CA04 Se actualiza en vivo al completar sesiones.
- CA03 **Con diferencia:** el filtro es por **período predefinido** (Todos / Última semana / Último mes), no por rango de fechas libre.
- **Estado:** Implementada con diferencias. **Pantalla:** F02–F04.

### HU13 — Consultar historial · Paciente
- CA01 "Resultados" lista las sesiones realizadas (tarjetas). CA02 Orden de la más reciente a la más antigua. CA03 Cada tarjeta: ejercicio, fecha, % (verde/ámbar). CA04 Al seleccionar una se abre su detalle en otra pantalla (repeticiones, promedio, detalle por repetición lista/gráfico, recomendación). CA05 Sin sesiones: mensaje de ausencia. CA06 "Progreso": sesiones realizadas, promedio general, gráficos de línea y barras.
- **Estado:** Implementada. **Pantallas:** P09, P10, P12.

### HU14 — Monitorear cumplimiento · Fisioterapeuta
- CA01/CA04 El detalle del paciente distingue sesiones completadas y pendientes (badge). CA02 Se actualiza al finalizar una sesión (listener en vivo). CA03 "Progreso" muestra "X de Y sesiones completadas" y el % de adherencia.
- **Estado:** Implementada. **Pantalla:** F02, F04.

## Épica 05 — Comunicación

### HU15 — Registrar y gestionar recomendaciones · Fisioterapeuta
- CA01 Desde una sesión completada se registra una recomendación. CA02 Se almacena (con fecha y autor). CA03 Editar. CA04 Eliminar con confirmación. CA05 Campo embebido en el resultado de la sesión para crear rápido; "Ver todas las recomendaciones" abre la gestión completa.
- **Estado:** Implementada. **Pantallas:** F13, F17, F18, F19.

### HU16 — Consultar recomendaciones · Paciente
- CA01/CA02 La recomendación aparece con su fecha en el detalle de la sesión (Mis resultados) y en el detalle del ejercicio. CA03 Se actualiza en vivo. CA04 Sin recomendaciones no se muestra bloque.
- **Estado:** Implementada. **Pantallas:** P10, P02.

## Épica 06 — Almacenamiento

### HU17 — Almacenar información terapéutica · Sistema
- CA01 Al finalizar se guardan resultados, métricas y fecha. CA02 Se guardan datos numéricos y, además, el video de la sesión (sin audio) en Firebase Storage para revisión del fisioterapeuta (enmienda 2026-09-18). CA03 Cada sesión es un documento propio: no se sobrescriben sesiones anteriores.
- **Estado:** Implementada. Limitación conocida: al reanudar, el video del tramo nuevo reemplaza al anterior.

### HU18 — Gestionar sesiones y resultados registrados · Fisioterapeuta
- CA01 "Resultados" lista los pacientes asignados. CA02 Al seleccionar uno, todas sus sesiones con estado (Completada/Incompleta/Por hacer). CA03/CA04 Filtros por período y por ejercicio. CA05 Detalle: repeticiones completas y promedio. CA06 Detalle por repetición en lista o gráfico. CA07 Errores con segundo, articulación, ángulo detectado y esperado. CA08 "Ver más repeticiones". CA09 "Ver video de la sesión".
- **Estado:** Implementada. **Pantallas:** F11–F16.

### HU19 — Sincronizar información · Sistema
- Cumplida por **plataforma**: persistencia offline por defecto de Firestore Android (sin código propio); no hay lógica de resolución de conflictos ni indicador de sincronización en la interfaz.
- **Estado:** Implementada por plataforma; **no verificada** en pruebas (requiere modo avión).

## Épica 07 — Administración de cuentas

### HU20 — Gestionar cuentas de pacientes · Administrador
- CA01 Lista de pacientes. CA02 Alta: nombre, correo, contraseña, DNI, edad, género, contacto, lado afectado (Derecho/Izquierdo/Ambos) y diagnósticos; contraseña oculta con ver/ocultar. CA03 Edición. CA04 Eliminar con confirmación (borra los documentos de Firestore). CA05 Asignar fisioterapeuta (una sola vez; luego el botón desaparece). CA06 La tarjeta muestra el fisioterapeuta asignado. CA07 Lado afectado se guarda. CA08 Desactivar/Activar con confirmación; la cuenta inactiva no puede iniciar sesión.
- **Diferencia:** eliminar una cuenta **no** elimina su usuario de Firebase Auth (limitación conocida).
- **Estado:** Implementada con diferencias. **Pantallas:** A04–A09.

### HU21 — Gestionar cuentas de fisioterapeutas · Administrador
- CA01 Lista (con nº de pacientes asignados). CA02 Alta: nombre, correo, contraseña, edad, género, contacto; especialidad y colegiatura opcionales. CA03 Edición. CA04 Eliminar con confirmación. CA05 Desactivar/Activar con confirmación.
- **Estado:** Implementada (misma diferencia sobre Auth). **Pantallas:** A10, A11.

### Dashboard del administrador (refinamiento de la Épica 07, no es HU numerada)
- Lista de pacientes; al seleccionar uno, su dashboard: Sesiones ejecutadas, Precisión prom., detalle por sesión (lista) y gráficos (tendencia y % completado). Sin sesiones: "Este paciente todavía no ejecutó ninguna sesión."
- **Diferencia:** al iniciar sesión el administrador aterriza en "Pacientes", no en "Dashboard" (según la observación de la prueba manual; se confirma en QA).
- **Estado:** Implementada. **Pantallas:** A01–A03.

### HU22 / HU23 — Perfil (Paciente / Fisioterapeuta)
- CA01 Muestra datos de la cuenta. CA02 Solo el nombre es editable. CA03 Correo, DNI/edad/género/contacto, diagnóstico(s), fisioterapeuta asignado (paciente) o especialidad/colegiatura (fisio) en solo lectura. CA04/CA05 "Cerrar sesión" con diálogo de confirmación y regreso al login. CA06 "Perfil" es el único lugar de cierre de sesión para estos roles.
- **Estado:** Implementada. **Pantallas:** P13, F20.

## Requisitos no funcionales

| ID | Requisito | Estado según código | Observación |
|---|---|---|---|
| RNF01 | Disponibilidad y uso sin conexión | Implementada por plataforma (Firestore offline por defecto; análisis en dispositivo) | No verificada en emulador (falta prueba en modo avión) |
| RNF02 | Seguridad de acceso | Implementada: Firebase Auth, rol en Firestore, Security Rules, cierre de sesión con confirmación | Ver hallazgos de seguridad en `05_Pruebas_y_Errores.md` |
| RNF03 | Compatibilidad Android | Android 10+ (`minSdk 29`); "sin cámara" muestra mensaje | Un mensaje específico de "versión no compatible" no existe: el instalador de Android impide instalar en < 29 |
| RNF04 | Integridad | Escrituras atómicas por documento; lectura tolerante a campos ausentes | Ver pruebas CP-INT |
| RNF05 | Consistencia del monitoreo (iluminación, oclusión, distancia) | Implementada parcialmente en código (tolerancia a oclusión); **no verificable** con persona real en emulador | Requiere prueba en dispositivo físico |
| RNF06 | Privacidad / Edge AI | Análisis local; consentimiento informado antes de usar; video de sesión subido con consentimiento (enmienda 2026-09-18) | Los videos de ejercicios son públicos; los de sesión usan URL con token |

## Funcionalidades implementadas sin HU propia (faltan en la documentación de HU)

1. Botón "Ampliar video" (HU05).
2. Grabación y visualización del video de la sesión (HU17/HU18; solo en enmienda).
3. Gráficos de evolución en el detalle del paciente del fisio (HU12).
4. Dashboard del administrador por paciente (Épica 07).
5. Cuentas desactivadas cierran sesión automáticamente al entrar (HU20-CA08/HU21-CA05: implícito).
