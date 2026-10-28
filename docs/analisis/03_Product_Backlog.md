# 3. Product Backlog priorizado (estado real del producto)

Prioridad y sprint: los del backlog de `CLAUDE.md` §10 (plan de 5 sprints). Estado: según código y verificación en emulador. "Hecho*" = hecho con diferencias respecto a lo planificado (ver Observaciones). Los ítems **PB-30 en adelante** son trabajo pendiente o deuda detectada al analizar el sistema; su prioridad es una propuesta.

## 3.1 Historias de usuario y requisitos planificados

| ID | HU / RNF | Descripción | Prioridad | Estado | Sprint | Observaciones |
|---|---|---|---|---|---|---|
| PB-01 | RNF02 | Seguridad de acceso (login, roles, reglas, cierre de sesión) | Crítica | Hecho* | 1 | Hallazgos de reglas en `05_Pruebas_y_Errores.md` |
| PB-02 | RNF06 | Privacidad y procesamiento local (Edge AI, consentimiento) | Crítica | Hecho* | 1 | El video de la sesión ahora se sube a la nube (enmienda 2026-09-18); consentimiento actualizado a v2 |
| PB-03 | HU17 | Almacenar información terapéutica | Crítica | Hecho* | 1 | Guarda además el video; al reanudar, el video nuevo reemplaza al anterior |
| PB-04 | HU01 | Gestionar pacientes terapéuticos | Crítica | Hecho | 1 | Diagnósticos: catálogo de 3 (antes 13) |
| PB-05 | HU02 | Gestionar ejercicios terapéuticos | Crítica | Hecho | 1 | Incluye ROM automático, desactivar/activar |
| PB-06 | HU20 | Gestionar cuentas de pacientes (Admin) | Crítica | Hecho* | 1 | Eliminar no borra el usuario de Firebase Auth |
| PB-07 | HU21 | Gestionar cuentas de fisioterapeutas (Admin) | Crítica | Hecho* | 1 | Igual que PB-06 |
| PB-08 | HU22 | Perfil del paciente | Media | Hecho | 1 | — |
| PB-09 | HU23 | Perfil del fisioterapeuta | Media | Hecho | 1 | — |
| PB-10 | HU03 | Asignar sesiones terapéuticas | Alta | Hecho* | 2 | Aviso "video referencial" ya no existe (lo reemplaza la nota) |
| PB-11 | HU04 | Visualizar ejercicios asignados | Crítica | Hecho | 2 | Rediseño 2026-09-19 (detalle con nota y recomendaciones) |
| PB-12 | HU05 | Consultar material terapéutico | Crítica | Hecho | 2 | + "Ampliar video"; 12/12 videos cargados |
| PB-13 | HU06 | Ejecutar sesión terapéutica | Crítica | Hecho | 3 | Monitoreo con persona real no verificado en emulador |
| PB-14 | HU07 | Monitorear movimiento corporal | Crítica | Hecho | 3 | Idem |
| PB-15 | HU08 | Procesar movimiento corporal | Crítica | Hecho | 3 | Pruebas unitarias existentes |
| PB-16 | RNF05 | Consistencia del monitoreo | Alta | Parcial | 3 | Sin prueba en dispositivo físico |
| PB-17 | RNF03 | Compatibilidad con dispositivos | Alta | Hecho* | 3 | Solo detección de "sin cámara"; Android < 10 lo impide el instalador |
| PB-18 | HU11 | Resultados y % de ejecución (paciente) | Crítica | Hecho* | 3 | Rediseñada; sin modo solo lectura |
| PB-19 | HU13 | Historial terapéutico (paciente) | Media | Hecho* | 3 | Ahora "Mis resultados" (lista + detalle en pantalla aparte) y "Progreso" |
| PB-20 | HU09 | Analizar ejecución terapéutica | Alta | Hecho | 4 | Cumplida desde Sprint 3 |
| PB-21 | HU10 | Retroalimentación inmediata | Crítica | Hecho* | 4 | CA04 (≤ 500 ms) no medido |
| PB-22 | HU15 | Registrar y gestionar recomendaciones | Alta | Hecho | 4 | Incluye registro rápido embebido |
| PB-23 | HU16 | Consultar recomendaciones | Alta | Hecho | 4 | Se ven en "Mis resultados" y en el detalle del ejercicio |
| PB-24 | HU12 | Progreso y evolución (fisio) | Media | Hecho* | 5 | Filtro por período predefinido, no rango de fechas |
| PB-25 | HU14 | Cumplimiento terapéutico (fisio) | Media | Hecho | 5 | — |
| PB-26 | HU18 | Gestionar sesiones y resultados (fisio) | Alta | Hecho | 5 | Flujo paciente → sesiones → detalle |
| PB-27 | HU19 | Sincronizar información | Media | Hecho (plataforma) | 5 | Sin verificación en modo avión |
| PB-28 | RNF01 | Disponibilidad operativa | Crítica | Hecho (plataforma) | 5 | Sin verificación en modo avión |
| PB-29 | RNF04 | Integridad de la información | Alta | Hecho | 5 | Ver CP-INT |

## 3.2 Ítems añadidos durante el desarrollo (no estaban en el backlog original)

| ID | Ítem | Descripción | Prioridad | Estado | Sprint | Observaciones |
|---|---|---|---|---|---|---|
| PB-30 | Dashboard del administrador | Lista de pacientes + dashboard por paciente (lista y gráficos) | Media | Hecho | 5 | Sin HU numerada |
| PB-31 | Video de sesión | Grabar (sin audio), subir y reproducir el video de cada sesión | Alta | Hecho | 5 | Solo en enmienda de `CLAUDE.md` §11 |
| PB-32 | Personalización de ángulo | Rango mín./máx. por sesión | Media | Hecho | 5 | Parte de HU03-CA08 |
| PB-33 | Botón "Ampliar video" | Video a pantalla casi completa (paciente) | Baja | Hecho | 5 | Nuevo, sin HU |
| PB-34 | Modelo de datos oficial | Migración a `usuarios/pacientes/fisioterapeutas/sesiones/observaciones` | Alta | Hecho | 5 | Reglas y app actualizadas |
| PB-35 | Videos 3D de ejercicios | Videos de los 12 ejercicios subidos a Storage | Media | Hecho | — | Plan en `videos_3d/PLAN_PRODUCCION_VIDEOS_3D.md`; los videos son públicos |

## 3.3 Pendientes y deuda detectada (propuestos)

| ID | Ítem | Descripción | Prioridad propuesta | Estado | Origen |
|---|---|---|---|---|---|
| PB-40 | Eliminar usuario de Auth al eliminar cuenta | Hoy solo se borran documentos de Firestore; queda el usuario en Firebase Auth | Media | Pendiente | HU20-CA04 / HU21-CA04 |
| PB-41 | Aterrizaje del administrador en Dashboard | Abría "Pacientes" aunque Dashboard es la primera pestaña | Baja | Hecho (2026-09-20) | ERR-ADM-010 |
| PB-42 | Medir latencia de retroalimentación (≤ 500 ms) | No hay instrumentación ni prueba | Media | Pendiente | HU10-CA04 |
| PB-43 | Verificación de modo sin conexión | Probar ejecución de sesión y sincronización en modo avión | Media | Pendiente | RNF01 / HU19 |
| PB-44 | Prueba del monitoreo con persona real en dispositivo físico | Iluminación, oclusión, distancia | Alta | Pendiente | RNF05 / HU07–HU09 |
| PB-45 | Reglas de seguridad: restringir campos actualizables | Escalada de rol, `activo`, reescritura de sesiones, sesiones a pacientes ajenos, Storage de ejercicios | Alta | Hecho (2026-09-20, desplegado) | ERR-SEG-003/004/005/008, ERR-FIS-016 |
| PB-46 | Pruebas unitarias | `FechaDatePickerTest` no compilaba y `RolTest` estaba desactualizado | Media | Hecho (2026-09-20, suite en verde: 35 pruebas) | ERR-SEG-001/002 |
| PB-47 | Documentar HU nuevas | Ampliar/actualizar las HU con lo listado en `02_Historias_de_Usuario.md` | Media | Hecho parcialmente (este documento) | Documentación |
| PB-48 | Modelo financiero | Inconsistencia de Tabla 4 vs. Tabla 8 (TIR) señalada en CLAUDE.md §4 | Baja | Pendiente | Tesis (fuera de la app) |
| PB-49 | Videos públicos de sesión | Evaluar acceso restringido al fisioterapeuta asignado (limitación de Storage Rules) | Media | Pendiente | RNF06 |

## 3.4 Pendientes tras el QA (requieren decisión)

| ID | Ítem | Descripción | Prioridad propuesta | Estado | Origen |
|---|---|---|---|---|---|
| PB-50 | Sesión sin persona detectada | Hoy se guarda como completada con 0 % y baja el promedio | Media | Decisión pendiente | ERR-PAC-004 |
| PB-51 | Reanudar con detalle vacío | El resultado no avanza si el tramo nuevo no genera repeticiones | Media | Decisión pendiente | ERR-PAC-003 |
| PB-52 | Aislamiento de lectura entre fisioterapeutas | Cualquier fisio lee `pacientes/*` y `usuarios/*` de pacientes ajenos; requiere rehacer las consultas de la app | Media | Pendiente | ERR-SEG-006 |
| PB-53 | Storage con custom claims | Restringir escritura de material a fisioterapeutas y lectura del video de sesión al fisio asignado | Media | Pendiente | ERR-SEG-007/008 |
| PB-54 | Borrado completo de cuentas | Eliminar usuario de Auth y, si corresponde, sesiones/observaciones | Media | Pendiente | ERR-ADM-006/007 |
| PB-55 | Confirmar la voz en teléfono real | El emulador no tiene motor TTS; se declaró `<queries>` en el manifiesto | Media | Por verificar | ERR-PAC-005 |

## 3.5 Guía visual en vivo (refinamiento de HU existentes, redactado el 2026-10-05)

Funcionalidad: cada ejercicio se compone de N repeticiones y cada repetición de varias acciones definidas como datos; la app pinta sobre la cámara el recorrido de la acción actual, anclado al cuerpo, y avanza sola de acción en acción. Es un refinamiento técnico de las HU indicadas, **sin sprint nuevo** (el plan de 5 sprints no cambia). Plan por etapas: `docs/guia_visual/prompts_guia_visual_en_vivo.md`. Las HU afectadas ya están reescritas en `CLAUDE.md` §10 (formato Gherkin) y esperan aprobación antes de implementar.

| ID | HU base | Etapa | Descripción | Prioridad | Estado | Observaciones |
|---|---|---|---|---|---|---|
| PB-60 | HU06, HU07, HU10 | 0 | Análisis del código actual (pantalla de ejecución, CameraX + MediaPipe, cálculo de ángulos, modelos de ejercicio/sesión) y plan de implementación | Alta | Hecho (2026-10-05); espera confirmación de las decisiones | `docs/guia_visual/Etapa0_Analisis.md` |
| PB-61 | HU02, HU06, HU09, HU17 | 1 | Modelo de datos genérico: `Accion`, `ResultadoAccion`, vista de cámara; compatibilidad con ejercicios sin acciones | Alta | Redactado, pendiente de aprobación | Ampliación fuera del diccionario de datos oficial; se documenta como las demás |
| PB-62 | HU07 | 2 | Mapeo de coordenadas (landmark → pantalla, cámara frontal espejada) y suavizado (filtro One Euro); cuadros no confiables | Alta | Redactado, pendiente de aprobación | Probar en dispositivo real |
| PB-63 | HU08, HU09 | 3 | Motor de cálculo genérico `PoseGuideCalculator` (ángulo, progreso, zona, estado) | Alta | Redactado, pendiente de aprobación | El ángulo actual (vértice de tres puntos) coincide con pivote–referencia–distal |
| PB-64 | HU06 | 4 | Máquina de estados repetición → acción | Alta | Redactado, pendiente de aprobación | Reemplaza el avance por tiempo fijo de cada repetición |
| PB-65 | HU10 | 5 | Overlay visual en Compose (recorrido, parte realizada, zona meta, segmento fantasma, segmento real, HUD) | Alta | Redactado, pendiente de aprobación | Cambia la decisión de Sprint 4 (ícono mínimo sin texto): requiere confirmación |
| PB-66 | HU10 | 6 | Feedback háptico y voz con opción de activar o desactivar | Media | Redactado, pendiente de aprobación | No existe pantalla de ajustes: definir dónde va la opción |
| PB-67b | HU02 | 1b | Formulario del fisioterapeuta para definir las acciones de cada ejercicio, su orden, la vista de cámara y las validaciones (HU02-CA15 a CA18) | Alta | Detectado en la Etapa 0, pendiente | No estaba en el plan original de etapas |
| PB-67 | HU17, HU18 | 7 | Persistencia de `ResultadoAccion` por repetición y detalle por acciones en la vista del fisioterapeuta; prueba con los 12 ejercicios; documentación | Alta | Redactado, pendiente de aprobación | Incluye pruebas manuales: cámara frontal y trasera, ambos lados, poca luz, fuera de cuadro |

Relación con pendientes ya registrados: PB-50 (sesión sin persona detectada) se resuelve en parte con los cuadros no confiables, pero queda abierto cómo evitar que la sesión se quede esperando indefinidamente.

### Puntos por decidir antes de implementar
1. Confirmar el cambio de la decisión de Sprint 4 sobre HU10 (monitoreo minimalista con ícono, sin texto ni esqueleto).
2. Qué pasa con la duración por repetición, la duración estimada y el ángulo personalizado de HU03, ahora que la repetición termina por acciones y cada acción tiene su tiempo máximo.
3. Regla para generar la acción por defecto de los 12 ejercicios actuales: tienen mínimo y máximo, pero no ángulo de inicio, tolerancia ni tiempo de mantenimiento.
4. Tiempo límite global cuando el paciente no está en cuadro (los cuadros no confiables no avanzan ni penalizan).
5. Si se conserva la pausa de 5 s entre repeticiones y el aviso por voz de la siguiente repetición.
6. Dónde se activan o desactivan la voz y la vibración.
