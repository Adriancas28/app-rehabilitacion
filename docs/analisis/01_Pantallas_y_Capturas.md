# 1. Pantallas del sistema, capturas y funcionalidades implementadas

Fuente: código de `app/src/main/java/com/sanna/rehabapp` (rutas en `core/navigation/Rutas.kt`, pantallas en `feature/*`), rama `mejora-videos`, verificado además en emulador (Android API 30, Pixel 4a).
Las capturas se guardan en `docs/capturas/<rol>/` con el nombre indicado en la columna **Archivo**. Estado de captura al cierre de esta etapa: ver sección 1.5.

## 1.1 Pantallas comunes

| ID | Pantalla | Ruta | Módulo | Rol | Qué representa | Sección del manual | Archivo de captura |
|---|---|---|---|---|---|---|---|
| C01 | Inicio de sesión | `login` | Auth | Todos | Correo + contraseña (con ver/ocultar) y botón "Iniciar sesión" (deshabilitado hasta completar ambos campos). Sin registro ni recuperación de contraseña (por diseño, RNF02). | 5.1 (Fig. 1) | `comun/C01_login.png` |
| C02 | Login con error | `login` | Auth | Todos | Mensaje "No se pudo iniciar sesión. Verifica tus credenciales." | 5.2 | `comun/C02_login_error.png` |
| C03 | Consentimiento informado | `consentimiento` | Auth (RNF06-CA04) | Paciente y Fisioterapeuta (primer inicio de sesión) | Texto de tratamiento de datos (Ley 29733), incluye la grabación del video de la sesión; botón "Acepto y quiero continuar". No se muestra al Administrador. | 5.1.1 | `comun/C03_consentimiento.png` |

## 1.2 Rol Paciente

Navegación: barra lateral con 4 pestañas (Ejercicios · Resultados · Progreso · Perfil).

| ID | Pantalla | Ruta | HU | Qué representa | Sección del manual | Archivo |
|---|---|---|---|---|---|---|
| P01 | Ejercicios (inicio) | `paciente/inicio` | HU04, HU06-CA09 | Saludo, acceso "Mi progreso", tarjeta "Reanudar sesión" (si hay una incompleta), "Próxima sesión", lista "Todos mis ejercicios" | 7.1 (Fig. 3) | `paciente/P01_ejercicios.png` |
| P02 | Detalle del ejercicio asignado | `paciente/ejercicios/{sesionId}` | HU04, HU05, HU03-CA08 | Descripción, video (reproductor + botón "Ampliar video"), tarjetas Ángulo objetivo y Repeticiones, "Nota de tu fisioterapeuta", recomendaciones registradas, botón "Iniciar sesión" (solo si la sesión está pendiente) | 7.2 (Fig. 4) | `paciente/P02_detalle_ejercicio.png` |
| P03 | Video ampliado | (diálogo) | HU05 | Video a pantalla casi completa con botón cerrar | 7.2 | `paciente/P03_video_ampliado.png` |
| P04 | Ejecutar sesión – previa / permiso de cámara | `paciente/ejercicios/{sesionId}/ejecutar` | HU06 | Solicitud de permiso de cámara; vista previa de cámara con botón "Iniciar sesión"; mensaje si el dispositivo no tiene cámara | 7.3 (Fig. 5) | `paciente/P04_ejecutar_previa.png` |
| P05 | Ejecutar sesión – cuenta regresiva | idem | HU06-CA02 | "Prepárate" con cuenta regresiva de 10 s | 7.3 | `paciente/P05_cuenta_regresiva.png` |
| P06 | Ejecutar sesión – monitoreo | idem | HU06, HU07–HU10 | "Cámara en vivo", contador "Repetición X/Y", ícono de postura correcta/corregir, botón "Finalizar ejercicio", "Salir" | 7.3 | `paciente/P06_monitoreo.png` |
| P07 | Sesión completada | idem | HU06-CA04/05 | "Sesión completada" y botón "Ver resultado" | 7.3 | `paciente/P07_sesion_completada.png` |
| P08 | Resultado del ejercicio | `paciente/resultado/{sesionId}` | HU11 | "¡Ejercicio completado!", Repeticiones y Promedio, todas las repeticiones con %, "Ir a mi progreso" | 7.5 (Fig. 6) | `paciente/P08_resultado_final.png` |
| P09 | Mis resultados (lista) | `paciente/resultados` | HU13 | Tarjeta por sesión realizada (ejercicio, fecha, % en badge) | 7.6 (Fig. 7) | `paciente/P09_mis_resultados.png` |
| P10 | Detalle de resultado | `paciente/resultados/{sesionId}` | HU13, HU16 | Repeticiones/Promedio, detalle por repetición (lista/gráfico), recomendación del fisioterapeuta | 7.6 | `paciente/P10_detalle_resultado.png`, `paciente/P11_detalle_resultado_grafico.png` |
| P12 | Mi progreso | `paciente/progreso` | HU13 (progreso) | Sesiones realizadas, Promedio general, gráfico de línea y de barras por sesión | 7.7 (Fig. 8) | `paciente/P12_progreso.png` |
| P13 | Perfil (paciente) | `paciente/perfil` | HU22 | Nombre editable; correo, DNI, edad, género, contacto, diagnóstico(s) y fisioterapeuta en solo lectura; "Cerrar sesión" con diálogo | 7.8 (Fig. 9) | `paciente/P13_perfil.png` |
| P14 | Menú lateral (paciente) | — | Navegación | Ejercicios/Resultados/Progreso/Perfil | 6.1 (Fig. 2) | `paciente/P14_menu.png` |

## 1.3 Rol Fisioterapeuta

Navegación: barra lateral (Pacientes · Ejercicios · Resultados · Perfil).

| ID | Pantalla | Ruta | HU | Qué representa | Sección del manual | Archivo |
|---|---|---|---|---|---|---|
| F01 | Pacientes (lista) | `fisioterapeuta/pacientes` | HU01 | Tarjetas de resumen (pacientes activos, sesiones hoy, ejercicios), buscador y lista de pacientes con diagnóstico | 7.9 (Fig. 10) | `fisio/F01_pacientes.png` |
| F02 | Detalle de paciente | `fisioterapeuta/pacientes/{id}` | HU01, HU12, HU14 | Tarjeta de diagnóstico(s), Progreso (% completadas, promedio), gráficos de evolución (línea y barras), Progreso por ejercicio, filtro de período, lista de sesiones (estado, acceso a resultado y a recomendación) | 7.10 (Fig. 11) | `fisio/F02_paciente_detalle.png`, `F03_paciente_graficos.png`, `F04_paciente_sesiones.png` |
| F05 | Editar diagnóstico | (en F02) | HU01-CA06 | Checklist de 3 diagnósticos por región; Guardar/Cancelar | 7.11 | `fisio/F05_editar_diagnostico.png` |
| F06 | Asignar / editar sesión | `.../sesiones/formulario` | HU03 | Ejercicio (★ sugeridos por diagnóstico), fecha, repeticiones, duración, duración estimada, personalizar ángulo (mín/máx), nota | 7.14 (Fig. 13) | `fisio/F06_asignar_sesion.png`, `F07_asignar_personalizar.png` |
| F08 | Ejercicios (catálogo) | `fisioterapeuta/ejercicios` | HU02 | Grid de ejercicios con miniatura/video y menú "⋮" (Ver video, Editar, Desactivar/Activar, Eliminar) | 7.12 | `fisio/F08_ejercicios.png`, `F10_ejercicio_menu.png` |
| F09 | Formulario de ejercicio | `.../ejercicios/formulario` | HU02 | Nombre, descripción, categoría, duración, repeticiones, ángulos por articulación, diagnósticos aplicables, material (imagen/video) y "Calcular rango automáticamente" | 7.12–7.13 (Fig. 12) | `fisio/F09_ejercicio_formulario.png` |
| F11 | Resultados (pacientes) | `fisioterapeuta/resultados` | HU18 | Lista de pacientes asignados | 7.15 | `fisio/F11_resultados_pacientes.png` |
| F12 | Resultados de un paciente | `fisioterapeuta/resultados/{id}` | HU18 | Sesiones con estado Completada/Incompleta/Por hacer; filtros por período y ejercicio | 7.15 (Fig. 14) | `fisio/F12_resultados_paciente.png` |
| F13 | Resultado de sesión (vista fisio) | `.../sesiones/{id}/resultado` | HU18, HU15-CA05 | Repeticiones completas/Promedio, detalle por repetición (lista/gráfico, sólo las con error, segundo y ángulos), "Ver más repeticiones", "Ver video de la sesión", recomendación embebida | 7.16 (Fig. 15) | `fisio/F13_resultado_lista.png`, `F14_resultado_grafico.png`, `F15_ver_mas.png`, `F16_video_sesion.png`, `F17_recomendacion_embebida.png` |
| F18 | Recomendaciones de una sesión | `.../sesiones/{id}/recomendaciones` | HU15 | Lista, "Nueva recomendación", Editar/Eliminar en menú "⋮" | 7.17 (Fig. 16) | `fisio/F18_recomendaciones.png`, `F19_recomendacion_form.png` |
| F20 | Perfil (fisioterapeuta) | `fisioterapeuta/perfil` | HU23 | Nombre editable; correo, edad, género, contacto, especialidad, colegiatura solo lectura; "Cerrar sesión" | 7.18 | `fisio/F20_perfil.png` |
| F21 | Menú lateral (fisioterapeuta) | — | Navegación | 4 pestañas | 6.1 | `fisio/F21_menu.png` |

## 1.4 Rol Administrador

Navegación: barra lateral (Dashboard · Pacientes · Fisioterapeutas · Cerrar sesión). Sin pantalla de Perfil.

| ID | Pantalla | Ruta | HU | Qué representa | Sección del manual | Archivo |
|---|---|---|---|---|---|---|
| A01 | Dashboard (lista de pacientes) | `admin/dashboard` | (Etapa 2A, Épica 07) | Lista de pacientes; al tocar uno abre su dashboard | 7.19 (Fig. 17) | `admin/A01_dashboard_lista.png` |
| A02 | Dashboard de paciente | `admin/dashboard/{id}` | (Etapa 2A) | Sesiones ejecutadas, Precisión prom., detalle por sesión en lista o gráficos (tendencia y % completado) | 7.19 | `admin/A02_dashboard_paciente.png`, `A03_dashboard_grafico.png` |
| A04 | Pacientes (lista) | `admin/pacientes` | HU20 | Tarjetas con fisioterapeuta asignado o botón "Asignar fisioterapeuta"; menú "⋮" (Editar, Desactivar/Activar, Eliminar) | 7.20 (Fig. 18) | `admin/A04_pacientes.png`, `A07_menu_paciente.png`, `A08_dialogo_desactivar.png`, `A09_asignar_fisio.png` |
| A05 | Formulario de paciente | `admin/pacientes/formulario` | HU20 | Nombre, correo, contraseña (solo alta), DNI, edad, género, contacto, lado afectado, diagnósticos | 7.20 | `admin/A05_paciente_alta.png`, `A06_paciente_edicion.png` |
| A10 | Fisioterapeutas (lista) | `admin/fisioterapeutas` | HU21 | Tarjetas con nº de pacientes asignados y menú "⋮" | 7.21 (Fig. 19) | `admin/A10_fisioterapeutas.png` |
| A11 | Formulario de fisioterapeuta | `admin/fisioterapeutas/formulario` | HU21 | Nombre, correo, contraseña (solo alta), edad, género, contacto, especialidad, colegiatura | 7.21 | `admin/A11_fisio_formulario.png` |
| A12 | Menú lateral (admin) y confirmación de cierre de sesión | — | RNF02-CA04 | 4 opciones y diálogo "¿Seguro que deseas cerrar sesión?" | 6.1, 7.22 | `admin/A12_menu.png`, `A13_dialogo_cerrar_sesion.png` |

## 1.5 Estado de las capturas

Al iniciar el análisis no existía ninguna captura del sistema. Se generaron desde el emulador (2026-09-19/20) y están en `docs/capturas/`:

| Rol | Obtenidas | Faltantes / observaciones |
|---|---|---|
| Comunes | C01, C02, C03 | — |
| Administrador | A01–A13 (13) | — |
| Fisioterapeuta | F01–F21 (21) | F07 muestra valores de ejemplo no guardados; F17–F19 usan un texto de recomendación de prueba (ya eliminado de la base) |
| Paciente | P01–P14 (+ P10b) (15) | P04 es el diálogo de permiso del sistema (Android no repite "While using the app" tras dos denegaciones); P06 muestra la escena virtual del emulador, sin persona; P08 usa una sesión de prueba con valores de ejemplo |

**No se pudieron obtener** (limitación del emulador): pantalla de descanso entre repeticiones (dura 5 s), estado sin cámara (RNF03), monitoreo con persona real frente a la cámara y errores de validación de formularios para todos los campos (algunos están como evidencia en `docs/capturas/*/evidencia/`).

Resumen para armar el manual: `docs/Capturas_por_HU.docx` (una captura por HU). Detalle de pruebas: `05_Pruebas_y_Errores.md`.

## 1.6 Funcionalidades implementadas (verificadas en código) sin equivalente claro en la documentación previa

- Botón "Ampliar video" en el detalle del ejercicio del paciente (nuevo, 2026-09-19): no estaba en el manual ni en las HU.
- Grabación y subida a Storage del video de cada sesión, y "Ver video de la sesión" del fisioterapeuta (documentado en la enmienda de CLAUDE.md §11; falta reflejarlo en el manual, sección 7.3 lo menciona de forma general).
- Cuenta regresiva de 10 s, lectura en voz alta de la instrucción (TTS) y frases correctivas por voz (`core/tts`, `FrasesCorrectivas`).
- Error de coordinación "Movimiento simultáneo" para `ladoAfectado = AMBOS`.
- Cálculo automático de ROM a partir de un video (`AnalizadorVideoReferencia`).
- Reanudar sesión con fusión de resultados (`MergeResultadoSesion`).
- Cuenta desactivada: cierra sesión sola al entrar (`RaizViewModel`).

## 1.7 Cosas documentadas que NO están implementadas o no existen

- Pantalla de auto-registro, recuperación de contraseña y selector de "Tipo de usuario" del mockup de login: **no existen, por diseño** (RNF02).
- Perfil del administrador: no existe (el admin solo cierra sesión desde el menú).
- Chat/canal de comunicación fuera de recomendaciones por sesión (mencionado en HU10-CA07 como "más adelante"): **no implementado**.
- Esqueleto con checklist por articulación (mockup de monitoreo): **no implementado, decisión explícita**.
- Notificaciones push: no existen (Cloud Functions descartado).
- Eliminar el usuario de Firebase Auth al eliminar una cuenta: **no implementado** (la app borra solo los documentos de Firestore; documentado como limitación conocida).
