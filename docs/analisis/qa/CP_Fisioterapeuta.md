# Casos de prueba QA – Rol Fisioterapeuta

Proyecto SANNA Rehabilitación · rama `mejora-videos` · emulador Android (Pixel 4a, API 30, 1080x2340) · build instalado · fecha 2026-09-19.
Cuenta usada: Dra. Ana Ruiz (`ana.ruiz@sanna.pe`), que atiende a Juan Pérez, Elmer castro y María Torres. Verificado contra el código real (`feature/pacientes/*`, `feature/ejercicios/*`, `feature/comunicacion/*`, `feature/perfil/*`, `core/navigation/*`, `backend/firestore.rules`) y contra Firestore con scripts (los scripts se borraron).

Las capturas están en `docs/capturas/fisio/` (F01–F21) y la evidencia de casos/defectos en `docs/capturas/fisio/evidencia/`.

## 0. Datos y limpieza

- Estado inicial (script): 8 usuarios, 5 pacientes, 2 fisioterapeutas, 12 ejercicios, 28 sesiones, 7 observaciones.
- Estado final (script): 8 / 5 / 2 / 12 / 28 / 7. **Sin diferencias.**
- Datos QA creados y eliminados: ejercicio "QA Ejercicio Prueba" (eliminado desde la app); sesión `6g7PXnoBlSYNeYKHpAEe` (nota "QA-nota-…", eliminada con script tras imprimir el id); recomendaciones `n6ZaD2EFG8ha3j1dqSIk` (borrada desde la app), `SzCqQo05JoC7BqBNfEb9` (texto de 2009 caracteres, eliminada con script porque su menú ⋮ quedó fuera de pantalla, ver ERR-FIS-007) y `lvY7g3QdCFUl067eokFE` (creada solo para recapturar F17–F19, eliminada con script por id exacto).
- Datos reales tocados y restaurados: diagnósticos de Juan Pérez (se añadió y quitó "Lumbalgia"; queda igual, mismo id de subdocumento y fecha original) y nombre de la Dra. Ana Ruiz (restaurado a "Dra. Ana Ruiz"; verificado en Firestore). No se eliminó ni modificó ninguna cuenta, ejercicio real, sesión real ni recomendación real.
- Antes de cada eliminación se verificó el nombre exacto en el diálogo de confirmación (captura/`uiautomator`).
- Nota de método: `uiautomator dump` a veces devuelve la pantalla anterior unos segundos; los resultados se confirmaron con capturas o con Firestore.

## 1. Casos de prueba

| ID | HU relacionada | Funcionalidad | Precondiciones | Pasos | Datos utilizados | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|---|---|---|---|
| CP-FIS-001 | RNF02 / HU01-CA01 | Login fisio y lista de pacientes asignados | App en login | Correo + Tab + clave + Enter | ana.ruiz@sanna.pe / ••• | Entra al panel del fisio en "Pacientes" con solo sus 3 pacientes | Muestra Elmer castro, María Torres y Juan Pérez | PASS | fisio/F01_pacientes.png |
| CP-FIS-002 | HU01-CA01 / RNF02-CA02 | No ve pacientes de otro fisio | Sesión de Ana | Buscar "luis" y "rosa"; revisar lista | Luis Ramírez, Rosa Vargas (de Carlos Mendoza) | Ninguno aparece | "No se encontraron pacientes con ese criterio." en ambos; la lista solo tiene 3 | PASS | log |
| CP-FIS-003 | HU01 | Tarjetas de resumen | Pacientes | Observar tarjetas | — | Pacientes activos 3, sesiones hoy, ejercicios 12 | 3 / 1 / 12 (ver ERR-FIS-012 sobre la semántica de "Sesiones hoy") | PASS | fisio/F01_pacientes.png |
| CP-FIS-004 | HU01-CA04 | Buscador: nombre parcial | Pacientes | Escribir "jua" | jua | Solo Juan Pérez | Solo Juan Pérez | PASS | log |
| CP-FIS-005 | HU01-CA04 | Buscador: mayúsculas | Pacientes | Escribir "MAR" | MAR | Solo María Torres (no distingue mayúsculas) | Solo María Torres | PASS | log |
| CP-FIS-006 | HU01-CA04 | Buscador por correo | Pacientes | Escribir "gmail" | gmail | Elmer castro (correo gmail) | Solo Elmer castro | PASS | log |
| CP-FIS-007 | HU01-CA04 | Buscador sin resultados | Pacientes | Escribir "zzz" | zzz | Mensaje de sin resultados | "No se encontraron pacientes con ese criterio." | PASS | fisio/evidencia/CP-FIS-busqueda_sin_resultados.png |
| CP-FIS-008 | HU01-CA04 | Limpiar la búsqueda | Búsqueda activa | Tocar la X del buscador | — | Vuelve la lista completa | Vuelve la lista de 3 pacientes | PASS | log |
| CP-FIS-009 | HU01-CA02/CA03 | Detalle de paciente | Pacientes | Tocar Juan Pérez | Juan Pérez | Correo, diagnóstico, progreso y sesiones | Muestra correo, diagnóstico "Osteoartritis de rodilla", progreso 83 % (10 de 12), gráficos y sesiones | PASS | fisio/F02_paciente_detalle.png |
| CP-FIS-010 | HU01-CA02 | Paciente sin diagnóstico | Pacientes | Abrir Elmer castro | Elmer castro | Mensaje de ausencia de diagnóstico | "Sin diagnóstico registrado." y progreso 4 de 5 | PASS | fisio/evidencia/CP-FIS-paciente_sin_diagnostico.png |
| CP-FIS-011 | HU01-CA06 | Abrir edición de diagnósticos | Detalle de Juan | Tocar el lápiz de "Diagnóstico(s)" | — | Checklist de 3 diagnósticos por región con Guardar/Cancelar | Checklist Columna/Rodilla/Hombro; marcado "Osteoartritis de rodilla" | PASS | fisio/F05_editar_diagnostico.png |
| CP-FIS-012 | HU01-CA06 | Desmarcar todos los diagnósticos | Edición abierta | Desmarcar Osteoartritis | — | Impide guardar o permite quedar sin diagnóstico | "Guardar" se deshabilita; no se puede dejar sin diagnóstico (ver ERR-FIS-010) | PASS | fisio/evidencia/CP-FIS-diag_sin_seleccion.png |
| CP-FIS-013 | HU01-CA06 | Cancelar no persiste cambios | Edición abierta | Desmarcar rodilla, marcar Hombro, tocar Cancelar | — | Vuelve al valor original | Muestra "Osteoartritis de rodilla (gonartrosis)" | PASS | log |
| CP-FIS-014 | HU01-CA06 | Guardar diagnóstico y persistencia | Edición abierta | Marcar además "Lumbalgia inespecífica", Guardar; leer Firestore | Lumbalgia | Se guarda con fecha y se muestra junto al resto | Muestra ambos; subcolección `diagnosticos` con OSTEOARTROSIS_RODILLA y LUMBALGIA_INESPECIFICA (fecha nueva) | PASS | log |
| CP-FIS-015 | HU01-CA06 | Restaurar diagnóstico original | Juan con 2 diagnósticos | Quitar Lumbalgia, Guardar; leer Firestore | — | Queda solo el original, con su fecha original | Solo OSTEOARTROSIS_RODILLA, mismo id `HQQxd4…` y fecha | PASS | log |
| CP-FIS-016 | HU01-CA05 | Módulo sin pacientes asignados | Fisio sin pacientes | Iniciar sesión con un fisio sin pacientes | — | Mensaje de ausencia | No se pudo probar: Ana y Carlos tienen pacientes y no hay credenciales de un fisio vacío (no se crearon cuentas). Texto verificado solo en código | BLOQUEADO | log |
| CP-FIS-017 | HU02-CA04 | Catálogo de ejercicios | Menú > Ejercicios | Abrir Ejercicios | 12 ejercicios | Grid de 12 con video | 12 tarjetas con miniatura de video (verificado 12 activos en Firestore) | PASS | fisio/F08_ejercicios.png |
| CP-FIS-018 | HU02 | Menú ⋮ de ejercicio | Catálogo | Tocar ⋮ de Mini-sentadilla | — | Ver video, Editar, Desactivar, Eliminar | Las 4 opciones | PASS | fisio/F10_ejercicio_menu.png |
| CP-FIS-019 | HU02 / HU05 | "Ver video" del menú | Menú abierto | Tocar "Ver video" | Mini-sentadilla | Reproductor a pantalla completa con cerrar | Diálogo con reproductor (00:10) y X para cerrar | PASS | fisio/evidencia/CP-FIS-ver_video_ejercicio.png |
| CP-FIS-020 | HU02-CA01 | Abrir formulario de alta | Catálogo | Tocar "+" | — | Formulario con nombre, descripción, categoría, duración, repeticiones, ángulos, diagnósticos y material | Presente todo; duración 30 y repeticiones 1 por defecto | PASS | fisio/F09_ejercicio_formulario.png |
| CP-FIS-021 | HU02-CA02 | Alta con formulario vacío | Formulario nuevo | Guardar sin datos | — | Error de campos obligatorios | "Completa nombre, descripción y categoría." | PASS | fisio/evidencia/CP-FIS-ejercicio_vacio.png |
| CP-FIS-022 | HU02-CA09 | Alta sin categoría | Nombre y descripción llenos | Guardar | QA Ejercicio Prueba | Error (categoría obligatoria) | Mismo mensaje de campos obligatorios | PASS | log |
| CP-FIS-023 | HU02 | Duración 0 | Categoría elegida | Duración 0, Guardar | 0 | Rechazo | "La duración debe ser un número de segundos mayor a cero." | PASS | fisio/evidencia/CP-FIS-ejercicio_duracion0.png |
| CP-FIS-024 | HU02 | Duración negativa | idem | Duración -10, Guardar | -10 | Rechazo | Mismo mensaje | PASS | log |
| CP-FIS-025 | HU02 | Duración con texto | idem | Duración "abc" (inyectado por adb), Guardar | abc | Rechazo | Mismo mensaje | PASS | log |
| CP-FIS-026 | HU02-CA08 | Repeticiones 0 | idem | Repeticiones 0, Guardar | 0 | Rechazo | "Las repeticiones deben ser un número mayor a cero." | PASS | log |
| CP-FIS-027 | HU02-CA08 | Repeticiones negativas | idem | Repeticiones -3, Guardar | -3 | Rechazo | Mismo mensaje | PASS | log |
| CP-FIS-028 | HU02-CA05 | Ángulo mínimo mayor que máximo | Formulario válido + articulación | Rodilla derecha, mín 150, máx 100, Guardar | 150 / 100 | Rechazo con mensaje | Se guarda (`angulosReferencia.RODILLA_DERECHA = {min:150, max:100}`) sin aviso (ERR-FIS-004) | FAIL | fisio/evidencia/ERR-FIS-004.png |
| CP-FIS-029 | HU02-CA02 | Alta válida | Formulario válido | Guardar | QA Ejercicio Prueba / QA descripcion de prueba / Movilidad / 10 s / 5 rep | Ejercicio creado y visible | Creado (Firestore: 13 ejercicios, activo true); la pantalla tarda unos segundos en volver al catálogo | PASS | log |
| CP-FIS-030 | HU02-CA05 | Editar ejercicio | QA creado | ⋮ > Editar, añadir " Editado" al nombre, Guardar | — | Cambio persistido | Nombre "QA Ejercicio Prueba Editado" en Firestore | PASS | log |
| CP-FIS-031 | HU02-CA10 | Desactivar ejercicio | QA activo | ⋮ > Desactivar | — | Badge "Inactivo" | Muestra badge "Inactivo" | PASS | fisio/evidencia/CP-FIS-desactivar.png |
| CP-FIS-032 | HU02-CA10 / HU03 | Inactivo no aparece al asignar | QA inactivo | Juan > "+" > abrir selector | — | El QA no está en el selector | Selector con 12 opciones, ninguna QA | PASS | fisio/evidencia/CP-FIS-selector_sin_inactivo.png |
| CP-FIS-033 | HU02-CA10 | Reactivar ejercicio | QA inactivo | ⋮ > Activar | — | Vuelve a estar activo | Menú pasa a "Desactivar" | PASS | log |
| CP-FIS-034 | HU02-CA06 | Eliminar: cancelar | QA activo | ⋮ > Eliminar > Cancelar | — | No se elimina | El ejercicio sigue en la lista | PASS | fisio/evidencia/CP-FIS-eliminar_confirmacion.png |
| CP-FIS-035 | HU02-CA06 | Eliminar: confirmar | QA activo (nombre verificado en el diálogo "QA Ejercicio Prueba Editado") | ⋮ > Eliminar > Eliminar | — | Se elimina solo ese | Firestore vuelve a 12 ejercicios, ninguno QA | PASS | log |
| CP-FIS-036 | HU02-CA07 | Calcular ROM automáticamente desde video | Editar/alta con video elegido y una articulación | Seleccionar un video y tocar "Calcular rango automáticamente desde el video" | — | Calcula mín/máx por articulación | El selector de archivos se abre, pero el emulador no tiene videos indexados (ni tras copiar uno a /sdcard/Movies) y el picker no respondió a los toques sobre las carpetas; el botón solo aparece con un video recién elegido (código) | BLOQUEADO | log |
| CP-FIS-037 | HU03-CA01 | Abrir asignar sesión | Detalle de Juan | Tocar "+" | — | Formulario de asignación | Ejercicio, fecha, repeticiones, duración, ángulo y notas | PASS | fisio/F06_asignar_sesion.png |
| CP-FIS-038 | HU03-CA02 | Guardar sin ejercicio ni fecha | Formulario nuevo | Tocar "Asignar sesión" | — | Error | "Selecciona un ejercicio y una fecha." | PASS | fisio/evidencia/CP-FIS-asignar_vacio.png |
| CP-FIS-039 | HU03-CA07 | Ejercicios sugeridos ★ | Juan (rodilla) | Abrir el selector | Juan Pérez | Los 4 de rodilla primero con ★ | ★ Mini-sentadilla, Flexión de rodilla, Levantarse de una silla, Extensión de rodilla; luego el resto | PASS | log |
| CP-FIS-040 | HU03-CA07 | Sin diagnóstico no hay ★ | Elmer | Abrir el selector | Elmer castro | Sin ★ | Sin ninguna ★ (12 opciones) | PASS | log |
| CP-FIS-041 | HU03-CA06 | Precarga de repeticiones y duración | Ejercicio elegido | Elegir Mini-sentadilla | — | 6 rep, 10 s, duración estimada 1 min | 6 repeticiones, 10, "1 min" | PASS | fisio/F06_asignar_sesion.png |
| CP-FIS-042 | HU03-CA06 | Override de repeticiones y duración | idem | 9 repeticiones + 30 s | 9 / 30 | Estimada recalculada | "5 min" (270 s redondeado hacia arriba) | PASS | log |
| CP-FIS-043 | HU03-CA06 | Duración 0 | idem | Duración 0 | 0 | Rechazo o corrección | El campo la acepta y la estimada muestra "0 s"; no se intentó guardar con 0 (ERR-FIS-001) | FAIL | log |
| CP-FIS-044 | HU03-CA06 | Duración negativa y guardado | idem | Duración -54, 9 rep, Asignar | -54 | Rechazo con mensaje | Estimada "-486 s"; la sesión se guardó (`duracionSegundos:-54`, `duracionEstimada:-486`) (ERR-FIS-001) | FAIL | fisio/evidencia/ERR-FIS-001.png |
| CP-FIS-045 | HU03-CA06 | Duración con texto | idem | Duración "abc" | abc | Rechazo o aviso | Se ignora en silencio (la estimada usa la duración por defecto); no se ejecutó el guardado con texto (ERR-FIS-003) | FAIL | fisio/evidencia/CP-FIS-duracion_texto.png |
| CP-FIS-046 | HU03-CA08 | Personalizar ángulo: precarga | Ejercicio elegido | Marcar "Personalizar ángulo objetivo" | Mini-sentadilla | Precarga el rango del ejercicio | Precarga 120 / 150 | PASS | fisio/F07_asignar_personalizar.png |
| CP-FIS-047 | HU03-CA08 | Ángulo mín > máx | Personalizar marcado | Mín 150, máx 100, Asignar | 150 / 100 | Rechazo con mensaje | Se guarda (`anguloMinOverride:150`, `anguloMaxOverride:100`) (ERR-FIS-002) | FAIL | fisio/evidencia/ERR-FIS-002.png |
| CP-FIS-048 | HU03-CA08 | Personalizar con campos vacíos | Sesión editada | Marcar, vaciar mín y máx, Guardar cambios | vacío | Aviso o rechazo | Guarda en silencio ambos overrides como `null` (ERR-FIS-003) | FAIL | log |
| CP-FIS-049 | HU03-CA05 | Nota larga | Formulario | Nota de 314 caracteres | QA-nota-larga_… | Se guarda | Guardada completa (314 caracteres) | PASS | log |
| CP-FIS-050 | HU03-CA02 | Sesión creada | Formulario lleno | Asignar | QA-… / Juan | Sesión visible como "Pendiente" en el detalle del paciente | Aparece primero con badge "Pendiente" (fecha 20/09/2026) | PASS | fisio/F04_paciente_sesiones.png |
| CP-FIS-051 | HU03-CA03 | Editar sesión pendiente (precarga) | Sesión QA | Tocarla en el detalle | — | "Editar sesión" con los datos guardados | Precarga duración, ángulo y nota | PASS | log |
| CP-FIS-052 | HU03-CA03 | Editar y guardar | idem | Duración 30, ángulo 100–130, nota "QA-nota-editada…" | — | Cambios persistidos | Firestore: duracionSegundos 30, override 100/130, duracionEstimada 270 | PASS | fisio/F07_asignar_personalizar.png |
| CP-FIS-053 | HU03-CA04 | La sesión llega al paciente | Sesión QA creada | Leer `sesiones/{id}` | — | Doc con pacienteId de Juan y estado "asignada" | Confirmado por Firestore (no se cambió de rol para verlo en la app del paciente) | PASS | log |
| CP-FIS-054 | HU03-CA03 | Sesión completada no editable | Detalle de Juan | Tocar una sesión completada | — | Abre el resultado, no el formulario | Abre el resultado de la sesión | PASS | log |
| CP-FIS-055 | HU14-CA03 / HU12 | Cumplimiento y promedio | Detalle de Juan | Leer tarjeta Progreso | Juan (10 completadas / 12) | 83 % y promedio 76 % | 83 % "10 de 12"; "Progreso general: 76 %" = media de los 10 % de Firestore | PASS | fisio/F02_paciente_detalle.png |
| CP-FIS-056 | HU12-CA01 | Gráfico de evolución | idem | Ver "Evolución por sesión" | — | Línea con las 10 sesiones | Línea con 10 puntos, eje X = sesión, eje Y = % | PASS | fisio/F02_paciente_detalle.png |
| CP-FIS-057 | HU12-CA02 | Gráfico de barras por sesión | idem | Ver "Precisión por sesión" | — | Barras verde (≥75) / ámbar | 10 barras (5 ámbar, 5 verdes) | PASS | fisio/F03_paciente_graficos.png |
| CP-FIS-058 | HU12-CA02 | Progreso por ejercicio | idem | Ver la tarjeta | — | Promedio por ejercicio | Flexión de rodilla 75 % (4), Levantarse 80 % (2), Mini-sentadilla 76 % (2), Extensión 73 % (2); coinciden con Firestore | PASS | fisio/F03_paciente_graficos.png |
| CP-FIS-059 | HU12-CA03 | Filtro "Última semana" | Fecha 2026-09-19 | Tocar el chip | — | Solo sesiones de los últimos 7 días | "2 de 4 sesiones completadas", promedio 85 % (=(81+90)/2) | PASS | log |
| CP-FIS-060 | HU12-CA03 | Filtro "Último mes" | idem | Tocar el chip | — | Últimos 30 días | "8 de 10", 80 %, promedio 79 % (excluye las de 15/08 y 18/08); coincide con Firestore | PASS | log |
| CP-FIS-061 | HU12-CA03 | "Todos" restaura | Filtro activo | Tocar "Todos" | — | Vuelven las cifras completas | 10 de 12, 83 % | PASS | log |
| CP-FIS-062 | HU12-CA03 | Rango de fechas libre | — | Buscar un selector de fecha desde/hasta | — | Selección de un rango de fechas (texto del CA) | Solo existen los chips Todos/Última semana/Último mes (ERR-FIS-011) | FAIL | log |
| CP-FIS-063 | HU14-CA04 | Pendiente vs completada | Detalle de Juan | Ver la lista | — | Se distinguen | Badge ámbar "Pendiente" / verde "Completada" | PASS | fisio/F04_paciente_sesiones.png |
| CP-FIS-064 | HU14-CA01 | Sesión incompleta | Detalle de Juan (sesión 8/12) | Ver su badge | Sesión del 17/09 (8 de 12 rep.) | "Incompleta" (como en Resultados) o distinguible | Aparece como "Completada" y suma a "completadas" (ERR-FIS-006) | FAIL | fisio/F04_paciente_sesiones.png |
| CP-FIS-065 | HU15 (atajo) | Ícono de recomendación en sesión completada | Detalle de Juan | Ver las tarjetas completadas | — | Ícono "Registrar recomendación" | Presente en las completadas, ausente en las pendientes (no se ejecutó el toque) | PASS | fisio/F04_paciente_sesiones.png |
| CP-FIS-066 | HU18-CA01 | Resultados: lista de pacientes | Menú > Resultados | Abrir | — | Pacientes de Ana | Elmer, Juan, María | PASS | fisio/F11_resultados_pacientes.png |
| CP-FIS-067 | HU18-CA02 | Sesiones con su estado | Resultados > Juan | Abrir | — | Completada / Incompleta / Por hacer | 3 "Por hacer", 1 "Incompleta" (8/12, 81 %), resto "Completada" | PASS | fisio/F12_resultados_paciente.png |
| CP-FIS-068 | HU18-CA03 | Filtro Última semana | Resultados de Juan | Chip "Última semana" | — | Solo sesiones recientes | 5 sesiones (3 "Por hacer" + 15/09 + 17/09) | PASS | log |
| CP-FIS-069 | HU18-CA03 | Filtro Último mes | idem | Chip "Último mes" | — | Solo últimos 30 días | La última visible es la del 22/08 (excluye 19/08 y 15/08) | PASS | log |
| CP-FIS-070 | HU18-CA04 | Filtro por ejercicio | idem | Elegir "Flexión de rodilla en bipedestación" | — | Solo esas sesiones | 5 sesiones (18/09 pendiente, 17/09, 15/09, 29/08, 15/08) | PASS | log |
| CP-FIS-071 | HU18-CA04 | "Todos los ejercicios" quita el filtro | Filtro por ejercicio activo | Elegir "Todos los ejercicios" | — | Vuelven todas | Reaparecen todos los ejercicios (Levantarse, Mini-sentadilla…) | PASS | log |
| CP-FIS-072 | HU18-CA05 | Detalle de sesión | Resultados de Juan | Tocar la sesión del 15/09 | — | Repeticiones completas y promedio | "12 / 12" y "90 %" | PASS | fisio/F13_resultado_lista.png |
| CP-FIS-073 | HU18-CA06 | Detalle en gráfico | Detalle abierto | "Mostrar gráfico" | — | Barras por repetición | 12 barras (rojas con error, verdes sin error) y leyenda; "Mostrar lista" vuelve | PASS | fisio/F14_resultado_grafico.png |
| CP-FIS-074 | HU18-CA07 | Segundo, articulación, ángulo detectado y esperado | Detalle en lista | Leer un error | Sesión 15/09 | Segundo + articulación + ángulos | "Segundo 8 — ángulo incorrecto: 81° (esperado 90°)": falta el nombre de la articulación (código: solo se muestra si no hay segundo) (ERR-FIS-005) | FAIL | fisio/F13_resultado_lista.png |
| CP-FIS-075 | HU18-CA08 | Ver más repeticiones | Detalle en lista | "Ver más repeticiones" | — | Listado completo | Diálogo con las repeticiones con error (1, 2, 3, 5, 6…) y "Cerrar" | PASS | fisio/F15_ver_mas.png |
| CP-FIS-076 | HU18-CA09 | Ver video de la sesión | Sesión con video (15/09) | "Ver video de la sesión" | — | Reproduce | Diálogo con el video (00:17) | PASS | fisio/F16_video_sesion.png |
| CP-FIS-077 | HU18-CA09 | Sesión sin video | Sesión 17/09 (sin video) | "Ver video de la sesión" | — | Aviso | "Video no disponible aún." con "Entendido" | PASS | fisio/evidencia/CP-FIS-video_no_disponible.png |
| CP-FIS-078 | HU15-CA01 | Recomendación rápida vacía | Detalle de sesión | Observar el botón sin texto | — | No permite guardar | "Guardar recomendación" deshabilitado | PASS | log |
| CP-FIS-079 | HU15-CA02 | Solo espacios | idem | Escribir 3 espacios | — | No permite guardar | Botón sigue deshabilitado | PASS | log |
| CP-FIS-080 | HU15-CA02 / HU15-CA05 | Recomendación rápida normal | idem | Escribir y guardar | "QA-rec-rapida Revisa la flexion de rodilla." | Se guarda y se avisa | "Recomendación guardada. El paciente la verá en Mis resultados."; el campo se vacía | PASS | fisio/F17_recomendacion_embebida.png |
| CP-FIS-081 | HU15 / modelo §5 | Se guarda en `observaciones` | Recomendación guardada | Leer Firestore | — | `sesiones/{id}/observaciones/{id}` con fisioterapeutaId, texto, fecha | Confirmado | PASS | log |
| CP-FIS-082 | HU15-CA02 | Texto muy largo | Detalle | Escribir 2009 caracteres sin espacios y guardar | QA-larga_… | Límite razonable | Se guarda completo (sin límite); en "Recomendaciones" el ⋮ de esa tarjeta desaparece de pantalla (ERR-FIS-007) | FAIL | log |
| CP-FIS-083 | HU15-CA01 | "Ver todas las recomendaciones" | Detalle | Tocar el enlace | — | Lista de recomendaciones de la sesión con formulario | "Nueva recomendación" + "Registradas" con fecha | PASS | fisio/F18_recomendaciones.png |
| CP-FIS-084 | HU15-CA02 | Registrar desde la pantalla completa | Pantalla de recomendaciones | Escribir y "Registrar" | — | Se registra | No ejecutado (solo se probó el campo embebido, que llama al mismo repositorio) | BLOQUEADO | log |
| CP-FIS-085 | HU15-CA03 | Editar recomendación | Recomendación QA | ⋮ > Editar | — | Formulario "Editar recomendación" con el texto | Precarga el texto con Cancelar / Guardar cambios | PASS | fisio/F19_recomendacion_form.png |
| CP-FIS-086 | HU15-CA03 | Guardar edición | idem | Añadir " EDIT", Guardar cambios | — | Texto actualizado | Firestore: "…rodilla. EDIT" | PASS | log |
| CP-FIS-087 | HU15-CA03 | Editar dejando vacío | idem | Vaciar el texto, Guardar cambios | — | Error | "Escribe una recomendación." | PASS | fisio/evidencia/CP-FIS-rec_vacia.png |
| CP-FIS-088 | HU15-CA03 | Cancelar edición | idem | Tocar Cancelar | — | Vuelve a "Nueva recomendación" limpio | Vuelve, pero el mensaje de error anterior queda visible (ERR-FIS-008) | FAIL | log |
| CP-FIS-089 | HU15-CA04 | Eliminar: cancelar | Recomendación QA | ⋮ > Eliminar > Cancelar | — | No se elimina | Sigue en Firestore (8 observaciones) | PASS | fisio/evidencia/CP-FIS-rec_eliminar_dialogo.png |
| CP-FIS-090 | HU15-CA04 | Eliminar: confirmar | idem | ⋮ > Eliminar > Eliminar | — | Se elimina solo esa | Firestore vuelve a 7; la real "Excelente sesión…" (`QCVbMu8…`) sigue | PASS | log |
| CP-FIS-091 | HU15-CA04 | El diálogo identifica qué se elimina | Diálogo abierto | Leer el texto | — | Muestra a cuál recomendación se refiere | "¿Seguro que deseas eliminarla?" sin citar el texto (ERR-FIS-009) | FAIL | fisio/evidencia/CP-FIS-rec_eliminar_dialogo.png |
| CP-FIS-092 | HU23-CA01 | Datos del perfil | Menú > Perfil | Abrir | — | Nombre, correo, edad, género, contacto, especialidad, colegiatura | Todos presentes (34, Femenino, 912345678, Fisioterapia musculoesquelética, CTMP-10234) | PASS | fisio/F20_perfil.png |
| CP-FIS-093 | HU23-CA03 | Solo lectura | Perfil | Intentar editar correo/edad/etc. | — | Sin control de edición | Solo el nombre es editable | PASS | fisio/F20_perfil.png |
| CP-FIS-094 | HU23-CA02 | Nombre vacío | Perfil | Borrar el nombre | — | No permite guardar | "Guardar cambios" deshabilitado; Firestore sin cambios | PASS | fisio/evidencia/CP-FIS-perfil_nombre_vacio.png |
| CP-FIS-095 | HU23-CA02 | Editar nombre | Perfil | Añadir " QA", Guardar cambios | "Dra. Ana Ruiz QA" | Se guarda | Firestore: "Dra. Ana Ruiz QA" | PASS | fisio/evidencia/CP-FIS-perfil_nombre_editado.png |
| CP-FIS-096 | HU23-CA02 | Restaurar nombre | idem | Restaurar "Dra. Ana Ruiz" y guardar | — | Nombre original | Firestore: "Dra. Ana Ruiz" | PASS | log |
| CP-FIS-097 | HU23-CA04 | Cerrar sesión: cancelar | Perfil | "Cerrar sesión" > Cancelar | — | Diálogo; sigue en Perfil | "¿Seguro que deseas cerrar sesión?"; permanece | PASS | fisio/evidencia/CP-FIS-dialogo_cerrar_sesion.png |
| CP-FIS-098 | HU23-CA05 | Cerrar sesión: confirmar | idem | "Cerrar sesión" > Cerrar sesión | — | Vuelve al login | Pantalla de login vacía | PASS | log |
| CP-FIS-099 | HU23-CA06 | Cerrar sesión solo en Perfil | Sesión de fisio | Revisar el menú lateral y las pantallas | — | Sin otra opción de cerrar sesión | Menú con 4 opciones (Pacientes, Ejercicios, Resultados, Perfil); ninguna otra pantalla lo ofrece | PASS | fisio/F21_menu.png |
| CP-FIS-100 | Navegación | Menú lateral | Panel del fisio | Tocar la hamburguesa | — | 4 opciones | Pacientes / Ejercicios / Resultados / Perfil | PASS | fisio/F21_menu.png |
| CP-FIS-101 | Navegación | Botón atrás en detalles y formularios | Varias pantallas | Atrás desde detalle, formulario y diálogos | — | Vuelve a la pantalla previa | Correcto (el diálogo del picker de archivos también se cierra) | PASS | log |
| CP-FIS-102 | Navegación | Atrás entre pestañas | Visitadas Pacientes > Resultados > Perfil | Atrás desde Perfil | — | Sale hacia la pantalla principal | Recorre las pestañas visitadas (Perfil → Resultados → Pacientes), con un salto extra en Resultados (ERR-FIS-014) | FAIL | log |
| CP-FIS-103 | Permisos | El fisio no accede a pantallas de admin/paciente | Sesión de Ana | Revisar el menú y el grafo | — | Solo rutas de fisio | Login aterriza en el grafo de fisio; sus rutas no incluyen las de admin/paciente (`RehabNavHost`/`FisioterapeutaNavGraph`); no hay enlaces a ellas | PASS | log |
| CP-FIS-104 | Rotación | La rotación no pierde estado | — | Rotar la pantalla | — | No pierde el estado | No probado: la indicación era no cambiar la rotación; el manifest no bloquea la orientación (ERR-FIS-015) | BLOQUEADO | log |
| CP-FIS-105 | RNF02 | Sin PERMISSION_DENIED durante las acciones | logcat | `adb logcat -d -s Firestore:W` tras las pruebas | — | Sin errores de permisos | 0 entradas desde el login de Ana (las 15 anteriores son de las escuchas del admin al cerrar su sesión, 13:15) | PASS | log |

## 2. Errores encontrados

| ID | HU/funcionalidad | Descripción | Pasos para reproducir | Resultado esperado | Resultado actual | Severidad | Prioridad | Estado |
|---|---|---|---|---|---|---|---|---|
| ERR-FIS-001 | HU03-CA06 Asignar sesión (duración por repetición) | Sin validación de la duración: acepta 0 y negativos; la duración estimada muestra valores negativos y la sesión se guarda con ellos. | Juan > "+" > elegir ejercicio > duración -54, 9 repeticiones > "Asignar sesión". Evidencia: fisio/evidencia/ERR-FIS-001.png | Rechazar duración ≤ 0 con mensaje (como ya hace el formulario de ejercicio) | Estimada "-486 s"; Firestore guarda `duracionSegundos:-54`, `duracionEstimada:-486`. Con 0 la estimada es "0 s" | Media | Media | Abierto |
| ERR-FIS-002 | HU03-CA08 Ángulo personalizado | Acepta mínimo mayor que máximo. | Asignar sesión > marcar "Personalizar" > mín 150, máx 100 > Asignar. Evidencia: fisio/evidencia/ERR-FIS-002.png | Rechazar con mensaje | Guarda `anguloMinOverride:150`, `anguloMaxOverride:100` sin aviso | Media | Media | Abierto |
| ERR-FIS-003 | HU03-CA06/CA08 Asignar/editar sesión | Valores inválidos o vacíos se descartan en silencio: duración con texto, y ángulos vacíos con "Personalizar" marcado se guardan como `null`. | Editar sesión > "Personalizar" marcado > vaciar mín y máx > Guardar cambios | Aviso al fisio de que faltan valores | Guarda `anguloMin/MaxOverride = null` y vuelve, sin mensaje | Baja | Baja | Abierto |
| ERR-FIS-004 | HU02-CA05 Formulario de ejercicio | Acepta rango de referencia con mínimo mayor que máximo. | "+" en Ejercicios > datos válidos > "Agregar articulación" > Rodilla derecha, mín 150, máx 100 > Guardar. Evidencia: fisio/evidencia/ERR-FIS-004.png | Rechazar con mensaje | Guarda `RODILLA_DERECHA {min:150, max:100}` | Media | Media | Abierto |
| ERR-FIS-005 | HU18-CA07 Detalle de repetición (fisio) | Al haber segundo del error, no se muestra la articulación (solo "ángulo incorrecto"). El código solo usa la articulación cuando falta el segundo. | Resultados > Juan > sesión 15/09 > ver "Repetición 1/12" | "Segundo 8 — Rodilla derecha — ángulo 81° (esperado 90°)" | "Segundo 8 — ángulo incorrecto: 81° (esperado 90°)" (`FisioResultadoSesionScreen.kt`, línea ~318) | Media | Media | Abierto |
| ERR-FIS-006 | HU14-CA01/CA04, HU12 Detalle de paciente | Una sesión finalizada antes de tiempo (8/12 repeticiones) aparece como "Completada" y cuenta como completada en la adherencia; en "Resultados" sí aparece "Incompleta". Criterio inconsistente. | Detalle de Juan > sesión 17/09 (8/12); comparar con Resultados > Juan | Distinguirla como incompleta en ambos lugares | Detalle: badge "Completada" (cuenta en 10 de 12); Resultados: "Incompleta". Evidencia: fisio/F04_paciente_sesiones.png | Media | Media | Abierto |
| ERR-FIS-007 | HU15 Recomendaciones (texto largo) | Sin límite de longitud; un texto largo sin espacios empuja el menú ⋮ fuera de la tarjeta y no se puede editar ni eliminar desde la app. | Escribir 2000 caracteres sin espacios en la recomendación rápida > Guardar > "Ver todas las recomendaciones" | Límite de caracteres y ajuste de línea; menú ⋮ siempre accesible | Se guardó (2009 caracteres); la tarjeta no muestra el ⋮ (se eliminó con script) | Baja | Baja | Abierto |
| ERR-FIS-008 | HU15 Recomendaciones | El mensaje de error "Escribe una recomendación." permanece tras "Cancelar" la edición. | Editar una recomendación > vaciar > Guardar cambios (error) > Cancelar | El error desaparece al cancelar | Vuelve a "Nueva recomendación" con el error anterior visible | Baja | Baja | Abierto |
| ERR-FIS-009 | HU15-CA04 Eliminar recomendación | El diálogo no identifica cuál se elimina ("¿Seguro que deseas eliminarla?"), a diferencia del de ejercicios que cita el nombre. Riesgo de borrar la equivocada. Evidencia: fisio/evidencia/CP-FIS-rec_eliminar_dialogo.png | ⋮ > Eliminar en cualquier recomendación | Mostrar un extracto del texto | Mensaje genérico | Baja | Media | Abierto |
| ERR-FIS-010 | HU01-CA06 Diagnósticos | No se puede dejar a un paciente sin diagnóstico una vez asignado alguno (Guardar se deshabilita sin selección). Posible decisión de diseño no documentada. Evidencia: fisio/evidencia/CP-FIS-diag_sin_seleccion.png | Detalle de Juan > lápiz > desmarcar todo | Permitir vaciar o documentar la restricción | Botón "Guardar" deshabilitado; para quitarlo hace falta editar en Firestore | Baja | Baja | Abierto |
| ERR-FIS-011 | HU12-CA03 Filtro de fechas | Solo hay tres chips (Todos, Última semana, Último mes); el criterio habla de "seleccionar un rango de fechas". | Detalle de paciente / Resultados de paciente: buscar un selector de rango | Selector de fecha desde/hasta | Solo períodos predefinidos | Baja | Baja | Abierto |
| ERR-FIS-012 | HU01 Tarjetas de resumen | "Sesiones hoy" cuenta las sesiones ASIGNADAS hoy (`fechaAsignacion`), no las ejecutadas ni las programadas para hoy; "Pacientes activos" cuenta todos los asignados sin filtrar `activo`. Etiquetas ambiguas (por código, `PacientesViewModel.kt`). | Pacientes > tarjetas | Etiquetas coherentes con lo que cuentan | Muestra 1 "Sesiones hoy" según fecha de asignación | Baja | Baja | Abierto |
| ERR-FIS-013 | HU02 Catálogo | El catálogo no tiene orden lógico (sin ordenar por nombre ni fecha): el ejercicio nuevo se insertó en la 4.ª posición. | Crear un ejercicio y ver su posición | Orden estable (nombre/fecha) | Orden por id de documento | Baja | Baja | Abierto |
| ERR-FIS-014 | Navegación | Cada cambio de pestaña se apila en la pila de atrás; "Atrás" recorre las pestañas visitadas (Perfil → Resultados → Pacientes). | Pacientes > Resultados > Perfil > Atrás varias veces | Atrás vuelve a la pestaña inicial y sale | Recorre las pestañas en orden inverso | Baja | Baja | Abierto |
| ERR-FIS-015 | Rotación / RNF03 | El manifest no fija la orientación (`screenOrientation` ausente) y el emulador tiene auto-rotación activada, pese a que se dio por fijada en vertical. No se probó la rotación (se indicó no cambiarla). | Revisar `AndroidManifest.xml` y `settings get system accelerometer_rotation` (=1) | Orientación bloqueada o estado conservado al rotar | Sin bloqueo; comportamiento al rotar sin verificar | Baja | Media | Abierto |
| ERR-FIS-016 | Seguridad HU03 (por revisión de código, no probado en ejecución) | `firestore.rules`: `create` en `sesiones` solo exige `fisioterapeutaId == auth.uid`; no comprueba que el paciente esté asignado a ese fisio. Un fisio podría asignar sesiones a pacientes ajenos con la API. | Revisar `backend/firestore.rules` líneas 91–95 | Validar que `pacienteId` pertenezca al fisio | Sin esa validación | Media | Media | Abierto |
| ERR-FIS-017 | HU02-CA07 (por código, no probado) | "Calcular rango automáticamente" solo aparece con un video recién elegido en el formulario; no se puede calcular con el video ya guardado del ejercicio. | Editar un ejercicio con video (`materialUrl`) y buscar el botón | Ofrecer el cálculo con el video existente | Botón oculto si no hay archivo elegido (`EjercicioFormScreen.kt` línea 138) | Baja | Baja | Abierto |
| ERR-FIS-018 | HU03-CA06 (por código, no reproducido) | El selector de repeticiones es fijo (1, 3, 6, 9, 12): un ejercicio con 5 repeticiones (OAR-04) se precarga con 6. | Asignar "Levantarse de una silla" y leer las repeticiones | Precargar 5 | `valorMasCercano` la redondea a 6 (`AsignarSesionViewModel.kt`) | Baja | Baja | Abierto |

## 3. NO IMPLEMENTADO / FALTA DOCUMENTAR

### NO IMPLEMENTADO
- Selector de rango de fechas libre en HU12-CA03 (solo hay Todos / Última semana / Último mes).
- Eliminar o cancelar una sesión ya asignada (no hay opción en la app y las reglas prohíben `delete`); solo se puede editar mientras esté pendiente.
- Articulación en el detalle de error de HU18-CA07 (sí se muestra segundo y ángulos).
- Calcular el ROM automáticamente sobre el video ya guardado de un ejercicio (HU02-CA07 solo funciona con un archivo recién elegido).
- Filtros de pacientes por diagnóstico y orden alfabético en la lista (HU01-CA04 solo pide búsqueda; no se reporta como defecto).
- Navegar a editar una sesión "Por hacer" desde Resultados (esas tarjetas no son pulsables; solo desde el detalle del paciente).
- Confirmación al abandonar un formulario con cambios sin guardar (Atrás descarta sin aviso).

### FALTA DOCUMENTAR (existe en la app y no está en CLAUDE.md / 02_Historias)
- Tres estados de sesión en Resultados (Completada / Incompleta / Por hacer) y el detalle "8/12 repeticiones · Ejecución 81 %" en cada tarjeta.
- Reproductor de "Video de la sesión" en diálogo y su aviso "Video no disponible aún.".
- Diálogo "Ver más repeticiones" y la regla de que solo se listan repeticiones con error; leyenda del gráfico (rojo = tuvo algún ángulo incorrecto).
- Filtro por ejercicio con "Todos los ejercicios" y chips de período en Resultados del paciente.
- Badge "Inactivo" en las tarjetas de ejercicio y la opción Activar/Desactivar (documentada como HU02-CA10, pero no su presentación).
- Mensaje "Recomendación guardada. El paciente la verá en 'Mis resultados'." y el botón "Ver todas las recomendaciones" (HU15-CA05 sí lo menciona).
- Tarjetas "Sesiones hoy" / "Pacientes activos" y su semántica real (ERR-FIS-012).
- "Progreso general" (promedio de precisión) distinto del anillo de porcentaje (adherencia).
- Precarga del ángulo objetivo (120/150 en Mini-sentadilla) y redondeo de la duración estimada hacia arriba en minutos.
- Selector de repeticiones limitado a 1, 3, 6, 9 y 12.

## 4. Capturas

Todas se tomaron con `adb exec-out screencap -p` y se revisaron una por una. `C03_consentimiento.png` ya existía en `comun/` y no se rehizo (el consentimiento no apareció con esta cuenta: ya estaba aceptado).

| Captura | Estado | Nota |
|---|---|---|
| F01_pacientes.png | Obtenida | Ana con sus 3 pacientes |
| F02_paciente_detalle.png | Obtenida | Juan Pérez, diagnóstico, progreso y evolución |
| F03_paciente_graficos.png | Obtenida | Barras por sesión, progreso por ejercicio, chips de período y primeras sesiones |
| F04_paciente_sesiones.png | Obtenida | Pendientes y completadas (con "Incompleta" mostrada como Completada, ERR-FIS-006). Solo sesiones reales (se tomó antes de crear la sesión QA) |
| F05_editar_diagnostico.png | Obtenida | Checklist con Guardar/Cancelar |
| F06_asignar_sesion.png | Obtenida | Ejercicio ★ Mini-sentadilla, fecha 20/09/2026, 6 repeticiones, 10 s, "1 min" |
| F07_asignar_personalizar.png | Obtenida | Ángulo personalizado 110–140 y nota "Hazlo con apoyo y sin dolor."; no se guardó |
| F08_ejercicios.png | Obtenida | Grid con videos |
| F09_ejercicio_formulario.png | Obtenida | Formulario vacío de alta |
| F10_ejercicio_menu.png | Obtenida | Menú ⋮ con 4 opciones |
| F11_resultados_pacientes.png | Obtenida | Lista de Resultados |
| F12_resultados_paciente.png | Obtenida | Estados y filtros de Juan |
| F13_resultado_lista.png | Obtenida | Detalle en lista (sesión 15/09) |
| F14_resultado_grafico.png | Obtenida | Detalle en gráfico |
| F15_ver_mas.png | Obtenida | Diálogo "Detalle por repetición" |
| F16_video_sesion.png | Obtenida | Video real de la sesión del 15/09 (la sesión con video existe). El caso sin video se documenta en fisio/evidencia/CP-FIS-video_no_disponible.png |
| F17_recomendacion_embebida.png | Obtenida | Con el texto "Buen trabajo. Sigue con la espalda recta al bajar." (recomendación de prueba ya eliminada) |
| F18_recomendaciones.png | Obtenida | Lista con la recomendación de prueba y la real del 17/09 (la de prueba ya no existe en Firestore) |
| F19_recomendacion_form.png | Obtenida | "Editar recomendación" |
| F20_perfil.png | Obtenida | Perfil de la Dra. Ana Ruiz |
| F21_menu.png | Obtenida | Barra lateral de 4 opciones (se ve sobre Pacientes) |
| C03_consentimiento.png | Ya existía | No se rehízo |
| Evidencias en `fisio/evidencia/` | Obtenidas | ERR-FIS-001, 002, 004 y 17 capturas `CP-FIS-*` referidas en la tabla |
| Faltantes | — | Ninguna de F01–F21. No se capturó el cálculo de ROM (CP-FIS-036, BLOQUEADO) ni la rotación (CP-FIS-104, BLOQUEADO) |

Observación para el manual: F04 y F03 se tomaron con el filtro "Todos"; F12 muestra la sesión "Mini-sentadilla" del 20/09/2026 "Por hacer", que corresponde a la sesión QA creada durante la prueba (eliminada de Firestore al terminar). Si el manual necesita datos sin rastro de las pruebas, hay que recapturar F04 y F12 después de la limpieza.

## 5. Resumen

- 105 casos: 88 PASS, 13 FAIL, 4 BLOQUEADO (CP-FIS-016, 036, 084, 104; el 084 quedó sin ejecutar, no bloqueado por el entorno).
- 18 errores abiertos (0 críticos, 0 altos, 6 medios, 12 bajos); 3 de ellos por revisión de código (ERR-FIS-016, 017, 018).
- Sin PERMISSION_DENIED ni cierres inesperados durante las pruebas del fisio.
