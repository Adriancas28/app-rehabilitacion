# 4. Manual de usuario: contenido y capturas requeridas

Documento vigente: `docs/Manual_Usuario_SANNA.docx` (v1.1). Este análisis lo contrasta con el comportamiento real del sistema. Regla: el manual describe solo lo que existe; los mensajes deben coincidir textualmente con los de la app.

## 4.1 Revisión del manual actual frente al sistema real

Se comprobó automáticamente cada frase entre comillas del manual contra el código de la app.

| Hallazgo | Ubicación en el manual | Acción |
|---|---|---|
| Mensaje "Tu fisioterapeuta aún no registró recomendaciones para esta sesión." **no existe** (la app simplemente no muestra el bloque de recomendación) | 7.6, "Mensajes del sistema" | **Corregido** (se eliminó y se aclaró el comportamiento) |
| Mensaje "No hay sesiones que cumplan el filtro." era inexacto; el real es "Este paciente no tiene sesiones que cumplan el filtro." | 7.15 | **Corregido** |
| "Descansa, viene la repetición siguiente…": el texto real es "Descansa, viene la repetición N…" (con el número) | 7.3 paso 4 | Pendiente de retocar (menor) |
| Las capturas siguen siendo marcadores "[INSERTAR CAPTURA DE PANTALLA]" (19 figuras) | Todo el manual | Se completan con las capturas de `docs/capturas/` (ver 4.3) |
| No se describen: video de la sesión grabado durante el ejercicio (visible al fisio), botón "Ampliar video" | 7.2, 7.3, 7.16 | 7.2 y 7.16 ya lo mencionan en parte; "Ampliar video" se agrega al pie de 7.2 |
| El manual no explica que la duración estimada se calcula (repeticiones × duración) | 7.14 | Ya está en el paso 4 |

## 4.2 Contenido del manual (índice y qué debe explicar cada procedimiento)

Cada procedimiento sigue la plantilla: Descripción · Acceso · Procedimiento (pasos) · Captura · Resultado · Validaciones · Mensajes del sistema · Consideraciones.

| Sección | Procedimiento (comportamiento real) | Capturas necesarias |
|---|---|---|
| 5.1 Inicio de sesión | Correo + contraseña (ojo para ver/ocultar); botón deshabilitado hasta completar ambos campos; redirección por rol | C01 |
| 5.1.1 Consentimiento informado | Solo paciente y fisioterapeuta, en el primer inicio; "Acepto y quiero continuar" | C03 |
| 5.2 Errores de inicio de sesión | "No se pudo iniciar sesión. Verifica tus credenciales."; cuenta desactivada no entra | C02 |
| 6.1 Menú lateral | Paciente: Ejercicios · Resultados · Progreso · Perfil. Fisio: Pacientes · Ejercicios · Resultados · Perfil. Admin: Dashboard · Pacientes · Fisioterapeutas · Cerrar sesión | P14, F21, A12 |
| 7.1 Ejercicios asignados | Próxima sesión, sesión reanudable, lista | P01 |
| 7.2 Detalle del ejercicio | Descripción, video (+ "Ampliar video"), ángulo objetivo, repeticiones, nota, recomendaciones, "Iniciar sesión" | P02, P03 |
| 7.3 Ejecutar sesión | Permiso de cámara, vista previa, cuenta regresiva de 10 s, monitoreo, pausas, "Finalizar ejercicio"/"Salir", "Sesión completada" | P04, P05, P06, P07 |
| 7.4 Reanudar sesión | Tarjeta "Reanudar sesión" X/Y | P01 (con tarjeta) |
| 7.5 Resultado al terminar | "¡Ejercicio completado!", Repeticiones/Promedio, lista de repeticiones, "Ir a mi progreso" | P08 |
| 7.6 Mis resultados | Lista de tarjetas y detalle en otra pantalla, gráfico, recomendación | P09, P10, P11 |
| 7.7 Mi progreso | Sesiones realizadas, promedio general, línea y barras | P12 |
| 7.8 / 7.18 Perfil | Nombre editable; datos solo lectura; "Cerrar sesión" con confirmación | P13, F20 |
| 7.9 Pacientes (fisio) | Tarjetas de resumen, buscador, lista | F01 |
| 7.10 Detalle del paciente | Diagnóstico, progreso, gráficos, progreso por ejercicio, filtro de período, sesiones | F02, F03, F04 |
| 7.11 Diagnóstico | Checklist de 3 diagnósticos, guardar/cancelar | F05 |
| 7.12 Catálogo de ejercicios | Grid, alta/edición/desactivar/eliminar | F08, F09, F10 |
| 7.13 ROM automático | Botón "Calcular rango automáticamente desde el video" | F09 (sección de ángulos) |
| 7.14 Asignar sesión | Formulario con sugeridos ★, parámetros propios, ángulo personalizado, nota | F06, F07 |
| 7.15 Resultados (fisio) | Pacientes → sesiones con estado → filtros | F11, F12 |
| 7.16 Detalle de resultado (fisio) | Lista/gráfico, "Ver más repeticiones", video | F13, F14, F15, F16 |
| 7.17 Recomendaciones | Registro rápido y gestión completa | F17, F18, F19 |
| 7.19 Dashboard (admin) | Lista de pacientes → dashboard por paciente | A01, A02, A03 |
| 7.20 / 7.21 Cuentas | Alta, edición, asignación, activar/desactivar, eliminar | A04–A09, A10, A11 |
| 7.22 Cerrar sesión (admin) | Menú → diálogo → confirmar | A13 |
| 8–15 | Tablas de funciones por rol, mensajes, solución de problemas, glosario, versiones | — |

## 4.3 Estado de las capturas y cómo se insertan

- Las capturas se generan durante el testing (ver `docs/capturas/`); el manual tiene 19 marcadores. Con las capturas disponibles se reemplazan los marcadores por imágenes (mismo orden de figuras). El estado de cada captura (obtenida / faltante) se informa en `05_Pruebas_y_Errores.md`.
- Capturas que **no se pueden obtener en el emulador**: monitoreo con persona real (P06 solo mostrará la cámara virtual), dispositivo sin cámara (P04 alterna), cualquier prueba de RNF05.
