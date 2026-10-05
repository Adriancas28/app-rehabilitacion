# CLAUDE.md

Este es el ÚNICO archivo de contexto del proyecto para Claude Code. No hace
falta leer la tesis en Word: todo lo relevante para desarrollar la app ya
está resumido y corregido aquí (arquitectura, stack, modelo de datos,
historias de usuario y requisitos no funcionales, priorizados por sprint).

> Trabajo en curso, fuera del alcance de este archivo (no son HU/RNF del
> backlog): el plan de producción de videos 3D para el material
> audiovisual de los ejercicios (`Ejercicio.materialUrl`, hoy vacío) vive
> en `videos_3d/PLAN_PRODUCCION_VIDEOS_3D.md`, con bitácora de avance del
> piloto SDS-02.

## Índice
1. Contexto del proyecto
2. Estructura del repositorio
3. Arquitectura de la app Android
4. Stack tecnológico
5. Modelo de datos (Firestore)
6. Modelo de datos relacional (lógico)
7. Catálogo de tipos de ejercicio
8. Catálogo de ejercicios predeterminados
9. Catálogo de diagnósticos
10. Historias de Usuario y Requisitos No Funcionales
11. Reglas del proyecto (no negociables)

---

## 1. Contexto del proyecto

Aplicación móvil con Inteligencia Artificial para análisis postural en el
monitoreo de ejercicios domiciliarios de rehabilitación musculoesquelética
(Clínica SANNA). Dos roles: **Paciente** y **Fisioterapeuta**. El análisis de
postura (MediaPipe Pose) corre **en el dispositivo** (Edge AI): el video no
se procesa en la nube. Se guardan métricas numéricas (ángulos, vectores) y,
desde 2026-09-18 el video de cada sesión en Firebase Storage, para que el
fisioterapeuta vea lo que el paciente realiza y pueda dar una mejor respuesta
— ver la "Enmienda 2026-09-18" en la sección 11.

## 2. Estructura del repositorio (monorepo)

Este proyecto es un **monorepo**: un único repositorio Git con dos carpetas
raíz independientes, `/backend` y `/app`, que se versionan juntas pero se
despliegan por separado. No existe una carpeta "server" con código de
backend propio — todo lo que normalmente sería un servidor lo resuelve
Firebase (BaaS).

**Estado actual: el repositorio está vacío.** Crear esta estructura es la
primera tarea, no asumir que ya existe nada de lo siguiente:

```
/ (raíz del repositorio)
├── CLAUDE.md                    ← este archivo
├── README.md
├── .gitignore
│
├── backend/                     ← configuración de Firebase (BaaS, sin servidor propio)
│   ├── firebase.json
│   ├── .firebaserc
│   ├── firestore.rules
│   ├── firestore.indexes.json
│   ├── storage.rules
│   └── seed/
│       └── seed-data.ts         ← datos de prueba (pacientes, ejercicios de ejemplo)
│
└── app/                         ← proyecto Android Studio (Kotlin)
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   └── java/com/sanna/rehabapp/
        │       ├── RehabApplication.kt      (Application, @HiltAndroidApp)
        │       ├── MainActivity.kt
        │       ├── core/
        │       │   ├── camera/              (CameraX)
        │       │   ├── posedetection/       (MediaPipe — HU07/HU08/HU09)
        │       │   ├── di/                  (módulos Hilt)
        │       │   └── navigation/          (Navigation Compose: grafo paciente / fisioterapeuta)
        │       ├── data/
        │       │   ├── repository/          (implementaciones Repository)
        │       │   └── remote/firestore/    (data sources, mappers)
        │       ├── domain/
        │       │   └── model/               (modelos de dominio)
        │       └── feature/                 (una carpeta por épica)
        │           ├── auth/
        │           ├── pacientes/           (Épica 01)
        │           ├── ejercicios/          (Épica 01)
        │           ├── sesiones/            (Épica 02 y 03)
        │           ├── seguimiento/         (Épica 04)
        │           └── comunicacion/        (Épica 05)
        ├── test/                            (unit tests)
        └── androidTest/                     (UI / instrumented tests)
```

**Convención de nombres:** package base sugerido `com.sanna.rehabapp`
(ajustar solo si ya existe un nombre definitivo acordado con el equipo).

> Nota (limpieza final pre-entrega): este árbol es el plan inicial de
> esqueleto, no un espejo 1:1 del árbol actual (que además ganó
> `feature/admin/`, `feature/perfil/`, `core/designsystem/`, `core/theme/`,
> `core/tts/`, `core/consent/`, entre otros, sobre la marcha). Dos carpetas
> planeadas aquí nunca se usaron y se eliminaron en la limpieza final:
> `data/remote/firestore/` (el acceso a Firestore terminó viviendo directo
> en `data/repository/*Impl.kt`) y `feature/seguimiento/` (HU12/HU14 se
> implementaron dentro de `feature/pacientes/`/`feature/paciente/` — ver su
> nota de implementación en la sección 10).

**Primer paso sugerido para Claude Code:** generar este esqueleto (proyecto
Android con Gradle + Hilt + Compose configurado, y proyecto Firebase con
`firebase init` sobre `/backend`) antes de empezar a implementar historias
de usuario.

## 3. Arquitectura de la app Android

- Patrón: **MVVM + Repository Pattern**, capas presentación / dominio / datos.
- Inyección de dependencias: **Hilt**.
- UI: **Jetpack Compose** + Navigation Compose (dos grafos: paciente / fisioterapeuta).
- Concurrencia y tiempo real: **Kotlin Coroutines + Flow**.
- Cámara + IA: **CameraX + MediaPipe Tasks Vision API**, en un módulo aislado
  y testeable aparte (ahí viven las historias de "Sistema": detección,
  procesamiento y análisis de movimiento).
- SDK mínimo: **Android 10 (API 29)**, cámara funcional obligatoria (RNF03).
- Empezar en un solo módulo Gradle organizado por capas; no sobre-modularizar
  para un equipo de 2 personas.

### Design System (rediseño visual completo, ampliación acordada)

A partir de este punto del proyecto, **una imagen de referencia de 10
pantallas (mockup) es el Design System oficial y obligatorio** de toda la
app. Regla permanente: ninguna pantalla nueva o existente puede inventar un
estilo propio — todas reutilizan estos mismos tokens y componentes, de
forma que la app entera se vea diseñada por un solo equipo. Antes de crear
un componente nuevo, verificar si ya existe uno reutilizable aquí. Este
rediseño es **solo visual**: no cambia lógica de negocio ni las HU
existentes, salvo los ajustes puntuales señalados abajo.

**Paleta:**
| Rol | Color aprox. | Uso |
|---|---|---|
| Primario | Verde-teal medio (`~#12A79B`) | TopAppBar, botones primarios, ícono activo del nav, logo, anillos de progreso, checks de éxito |
| Éxito | Verde (variante clara del primario) | Pills "Activo"/"Correcta", check verde |
| Advertencia | Ámbar | Pill/ícono "Corrige" |
| Error | Rojo/coral | Botón outline "Finalizar ejercicio", contador "Errores", alertas |
| Texto primario | Gris carbón (~#1F2937) | Títulos, nombres, cifras |
| Texto secundario | Gris medio (~#6B7280) | Subtítulos, fechas |
| Fondo de pantalla | Gris muy claro (~#F5F6F8) | Detrás de las tarjetas |
| Superficie | Blanco | Tarjetas, inputs |

**Tipografía:** sans-serif estándar (tipo Inter/Roboto). Título de pantalla
bold ~18-20px; cifras destacadas en tarjetas de stats bold ~24px; cuerpo
regular ~14px; etiquetas secundarias regular ~12px gris.

**Forma/espaciado:** esquinas muy redondeadas en todo (tarjetas ~16px,
botones pill completo, inputs ~12px, badges de estado pill completo);
padding interno ~16px en tarjetas; separación vertical uniforme entre
ítems de lista.

**Inventario de componentes reutilizables** — viven en
`app/core/designsystem/` (paquete nuevo). Antes de crear un componente
nuevo, o de tocar una pantalla existente, revisar primero si ya hay uno
aquí:

| Componente | Archivo | Notas |
|---|---|---|
| `BotonPrimario` / `BotonSecundario` / `BotonOutline` | `Botones.kt` | Forma pill fija, no depende de `MaterialTheme.shapes` |
| `TarjetaBase` | `Tarjetas.kt` | Card base — toda tarjeta nueva la envuelve, en vez de un `Card` suelto |
| `SeccionFormulario` | `Tarjetas.kt` | Título + `TarjetaBase` — agrupar campos relacionados en un formulario largo |
| `CampoTexto` | `Inputs.kt` | Input genérico; `esPassword = true` resuelve el toggle de ver/ocultar contraseña; `soloUnaLinea = false` + `lineasMinimas` para texto largo |
| `CampoSoloLectura` | `Inputs.kt` | Contraparte de solo lectura de `CampoTexto` — etiqueta + valor, sin control de edición (Perfil, HU22/HU23) |
| `BadgeEstado` (enum `TipoBadge`: EXITO/ADVERTENCIA/ERROR/NEUTRO) | `Badges.kt` | Pill de estado — reemplaza `EstadoPill` y equivalentes sueltos. `mostrarPunto = true` agrega un punto de color antes del texto (ej. "● Cámara en vivo" en HU06) |
| `SelectorDropdown<T>` | `SelectorDropdown.kt` | Genérico — reemplaza el patrón repetido Box+OutlinedTextField+clickable+DropdownMenu |
| `FilaChipsFiltro<T>` | `FiltroChips.kt` | Fila de `FilterChip` de selección única — filtro por período/ejercicio, reemplaza el Row+FilterChip repetido a mano |
| `ChecklistAgrupado<T>` | `ChecklistAgrupado.kt` | Selección múltiple agrupada (ej. diagnósticos por región) — checkbox + texto por opción, con encabezado de grupo |
| `TarjetaEstadistica` | `TarjetaEstadistica.kt` | Ícono + cifra grande + etiqueta, para dashboards |
| `TarjetaPersona` | `TarjetaPersona.kt` | Avatar+nombre+subtítulo+slots — generaliza fila de paciente/fisioterapeuta/admin |
| `TarjetaConIcono` | `TarjetaConIcono.kt` | Avatar de ícono (círculo de color, no iniciales) + título/subtítulo + slots de contenido final e inferior — tarjetas de "acción" tipo fila: próxima sesión, sesión reanudable, acceso rápido, ejercicio asignado |
| `TarjetaEjercicio` | `TarjetaEjercicio.kt` | Tarjeta de grid 2 columnas con menú "⋮" opcional |
| `BarraSuperior` | `BarraSuperior.kt` | TopAppBar con el color/estilo del Design System |
| `BarraBusqueda` | `BarraBusqueda.kt` | Búsqueda + botón de filtro opcional |
| `ProgresoCircular` / `ProgresoLineal` | `ProgresoCircular.kt` | Anillo con cifra centrada / barra lineal — los dos indicadores de progreso. `ProgresoCircular` acepta `texto`/`estiloTexto` opcionales para mostrar otra cifra además del porcentaje (ej. segundos restantes en HU06) |
| `EstadoCargando` / `EstadoVacio` / `EstadoError` | `EstadosPantalla.kt` | Los 3 estados que toda pantalla con datos remotos debe cubrir |
| `DialogoConfirmacion` | `Dialogos.kt` | Confirmar eliminar/descartar |
| `rememberSnackbarDeMensaje` | `SnackbarDS.kt` | Reemplaza el `SnackbarHostState`+`LaunchedEffect` repetido a mano en el panel admin |
| NavigationRail lateral | `core/navigation/ScaffoldConBarraLateral.kt` | Su `topBar` ya usa `BarraSuperior` (con `onAlternarMenu`) en todas las pantallas que lo consumen — **fisio, admin y (ampliación acordada, 2026-09-10) también paciente**: `EjerciciosAsignadosScreen`/`HistorialSesionesScreen`/`PerfilPacienteScreen` ahora comparten la misma barra lateral (Ejercicios/Progreso/Perfil) en vez de navegar solo con "Accesos rápidos" sueltos — decisión explícita del usuario para dar consistencia de navegación entre los 3 roles |
| `TarjetaEjercicio` (rediseño) | `TarjetaEjercicio.kt` | (Ampliación acordada, 2026-09-10) La miniatura ya no es un ícono genérico: si el ejercicio tiene `materialUrl`, se reproduce el video ahí mismo (`ReproductorVideo`); ícono de mancuerna solo si aún no tiene video. Se quitaron las líneas de categoría/repeticiones/duración (esta pantalla es de gestión del catálogo, no de ejecución). El menú "⋮" gana la opción "Ver video" (abre el video a pantalla completa en un `Dialog`) — no se usa tocar la miniatura directamente porque `PlayerView`/ExoPlayer capturaba el toque antes de que llegara a un `clickable` puesto encima |
| `BotonSelectorFecha` | `SelectorFecha.kt` | Botón que abre un `DatePickerDialog`; resuelve internamente la conversión de zona horaria UTC↔local (antes vivía inline en `AsignarSesionScreen`) |
| `GraficoBarras` / `GraficoLinea` | `Graficos.kt` | (Ampliación acordada, 2026-09-15) Gráficos simples dibujados a mano con `Canvas` — el proyecto no tiene ninguna librería de gráficos (Vico/MPAndroidChart) como dependencia. `GraficoBarras` recibe una lista de porcentajes 0-100 y colorea cada barra verde/ámbar según un umbral (precisión por repetición, HU11-CA07); `GraficoLinea` dibuja una línea con relleno para una serie en orden cronológico (evolución de precisión, HU12) |

Componentes visuales que **ya existen dentro de una pantalla concreta y no
se han extraído todavía** (se migran cuando se rediseñe esa pantalla, no
antes): tarjeta de mensaje del fisioterapeuta, ícono mínimo de corrección
de HU10, tabs de Resumen/Evolución/Historial.

**Iconografía:** íconos de línea/outline (no rellenos) — casa, personas,
pesa, calendario, gráfico, engranaje, campana, lupa, embudo/filtro, flecha
atrás, más, ojo, check-circle, alerta-circle, cámara.

**Confirmado explícitamente al adoptar este Design System (para no
reabrir estas decisiones sin querer durante el rediseño):**
- El login del mockup muestra selector "Tipo de usuario", "Registrarse" y
  "¿Olvidaste tu contraseña?" — **se copia solo el estilo visual**, sin
  construir esas 3 funciones (RNF02 sigue igual: cuentas creadas por el
  Admin, sin auto-registro ni recuperación de contraseña).
- La pantalla de monitoreo del mockup muestra un esqueleto con checklist
  de texto por articulación — **se mantiene el diseño minimalista actual**
  de HU10 (ícono mínimo + voz), sin reincorporar ese checklist visual.
- "Ejecutar sesión" y "Monitoreo corporal" del mockup son la misma
  pantalla ya construida (cámara + panel lateral simultáneos), no dos
  pantallas separadas — no se cambia el flujo de navegación de HU06/HU07.

---

## 4. Stack tecnológico

### Tecnologías

| Categoría | Tecnología | Uso en el proyecto | Justificación técnica |
|---|---|---|---|
| Desarrollo móvil | Kotlin (Android nativo) | Construcción de la aplicación móvil | Alto rendimiento y compatibilidad con procesamiento en tiempo real |
| Arquitectura de la app | MVVM + Repository Pattern | Organización en capas: presentación, dominio y datos | Separa el pipeline cámara→IA→UI de la lógica de negocio y de Firestore; facilita pruebas y mantenimiento |
| Inyección de dependencias | Hilt | Gestión de dependencias entre módulos | Estándar oficial de Android; reduce el acoplamiento entre capas |
| Interfaz de usuario | Jetpack Compose | Construcción de pantallas y overlays en tiempo real | Permite dibujar el esqueleto detectado y la retroalimentación visual sobre el preview de cámara de forma más simple que XML/Views |
| Acceso a cámara | CameraX | Captura de video para el análisis postural | Librería oficial de Android con integración directa con MediaPipe Tasks Vision API |
| Concurrencia y tiempo real | Kotlin Coroutines + Flow | Manejo del pipeline cámara → MediaPipe → UI | Necesario para cumplir el límite de latencia de 500 ms (HU10) sin bloquear la interfaz |
| Inteligencia Artificial | MediaPipe (BlazePose) | Estimación de pose corporal | Detecta puntos clave del cuerpo en tiempo real; procesado en el dispositivo (Edge AI) |
| Visión computacional | Pose Estimation (vía MediaPipe) | Análisis de postura | Permite calcular ángulos articulares en ejercicios domiciliarios |
| Base de datos | Firebase Firestore | Almacenamiento de pacientes, ejercicios, sesiones y resultados | Base de datos en tiempo real con caché offline nativo, alineada con el funcionamiento sin conexión (RNF01) |
| Almacenamiento multimedia | Firebase Storage | Material audiovisual de ejercicios (HU02, HU05) | Almacenamiento escalable integrado con el resto del ecosistema Firebase |
| Autenticación | Firebase Authentication | Gestión de usuarios (paciente / fisioterapeuta) | Seguridad integrada y fácil implementación de roles |
| Seguridad de datos | Firestore Security Rules | Control de acceso a la información por rol | Reemplaza la necesidad de un backend intermedio (Cloud Functions) para las reglas de acceso |
| Control de versiones | GitHub | Gestión del código fuente | Control y seguimiento del desarrollo |
| Diseño UI/UX | Figma | Prototipado de interfaces | Mejora la experiencia del usuario antes del desarrollo |

**Descartado deliberadamente:** Firebase Cloud Functions. Ninguna historia de
usuario requiere lógica de servidor de confianza — el control de acceso se
resuelve con Firestore Security Rules y todo el análisis es Edge AI (en el
dispositivo). Si en el futuro aparece un caso de uso real (p. ej. notificaciones
push o reportes agregados), se agrega como una decisión nueva y justificada,
no por defecto.

### Costo de servicios tecnológicos (mensual)

| Servicio | Costo mensual (S/.) |
|---|---|
| Integración y mantenimiento de IA | 150 |
| Entorno de desarrollo (incluye librerías Jetpack: CameraX, Compose, Hilt, Coroutines — uso libre) | 80 |
| Firebase Firestore | 150 |
| Firebase Storage | 200 |
| Firebase Authentication | 10 |
| CDN / transferencia de datos | 140 |
| **Total** | **730** (S/ 8,760/año) |

### Pendiente (no resuelto aún, ver conversación de arquitectura)

Se detectó una inconsistencia en las tablas financieras originales de la
tesis: la inversión inicial declarada (Tabla 4: S/ 9,600) no coincide con la
usada en el flujo de caja (Tabla 8: S/ 19,800), y la TIR reportada (~143%) no
es matemáticamente consistente con esos flujos (da ~68%). Pendiente de
resolver como tarea aparte antes de dar por cerrado el modelo financiero.
-e 
---

## 5. Modelo de datos (Firestore)

> **ESTRUCTURA VIGENTE (migración 2026-09-18 al diccionario oficial del equipo,
> `docs/modelo-datos/DICCIONARIO_DE_DATOS_FIRESTORE.docx` + diagramas E-R).**
> El esquema detallado que sigue más abajo es el histórico anterior; donde
> difieran, manda esta nota:
>
> - `usuarios/{id}`: correo, rol (`paciente`|`fisioterapeuta`|`administrador`), nombre, fechaCreacion, activo.
> - `pacientes/{id}` (mismo id, herencia): dni, edad, genero, contacto, ladoAfectado, fisioterapeutaId; subcolección `diagnosticos/{x}`: diagnosticoId, fecha.
> - `fisioterapeutas/{id}`: edad, genero, contacto, especialidad, numeroColegiatura.
> - `catalogo_diagnosticos/{id}`: nombre, regionCorporal (ids = enum `TipoDiagnostico`).
> - `ejercicios/{id}`: nombre, descripcion, fisioterapeutaId, categoria ("Movilidad"/"Control motor"), videoPath, angulosReferencia (mapa articulación → {min,max}), repeticiones ({cantidad, duracionSeg}), diagnosticosAplicables, activo, fechaCreacion.
> - `sesiones/{id}` (colección de NIVEL SUPERIOR, ya no subcolección del paciente): pacienteId, ejercicioId, fisioterapeutaId, fechaAsignacion, fechaEjecucion, estado (`asignada`|`en curso`|`completada`), nota, repeticionesAsignadas, duracionEstimada, porcentajeEjecucion, desviacionPromedio, repeticiones[] ({numero, porcentaje, desviacionAngular, error, articulacion}), repeticionesCompletadas, repeticionesCorrectas; subcolección `observaciones/{id}` (fisioterapeutaId, texto, fecha) = las recomendaciones (HU15/HU16).
> - **Atributos añadidos por la app (no están en el diccionario):** en `sesiones`: `duracionSegundos`, `anguloMinOverride`, `anguloMaxOverride` (HU03-CA06/CA08), `videoUrl` (enmienda 2026-09-18); en cada elemento de `repeticiones[]`: `errores` (lista con articulacion, tipo, anguloDetectado, anguloEsperado, segundo — HU18-CA05).
> - **Guía visual en vivo (2026-10-05):** `ejercicios/{id}.acciones`: [{orden, nombre, puntoReferencia, pivote, puntoIntermedio?, puntoDistal, lado, anguloInicio, anguloMeta, tolerancia, mantenerSeg, tiempoMaxSeg, sentido}] (landmarks de MediaPipe como enteros) y `vistaCamara` ("Lateral"|"Frontal"|"Oblicua"); en cada elemento de `sesiones/{id}.repeticiones[]`, `acciones`: [{ordenAccion, anguloMaximo, completada, tiempoSeg, desviacionAngular}]. Ampliaciones fuera del diccionario oficial. Un ejercicio sin acciones se trata como de una sola acción derivada de su rango (`Ejercicio.accionesEfectivas()`). Los 12 ejercicios del catálogo se siembran con `backend/seed/sembrar-acciones-ejercicios.ts` (valores de borrador a validar por el fisioterapeuta).
> - **Desviaciones:** `ejercicios.videoPath` guarda la URL de descarga de Storage (no una ruta); los agregados `angulosDetectados`/`erroresDetectados` ya no se guardan (se derivan del detalle por repetición).
> - Consultas de sesiones por `whereEqualTo("pacienteId"|"fisioterapeutaId")` ordenadas en memoria (sin índices compuestos). Reglas en `backend/firestore.rules`; migración desde el modelo anterior en `backend/seed/_migrar_modelo.js`.

Contrato entre `/backend` (Security Rules) y `/app` (Repository pattern).
Cualquier cambio de campos se actualiza aquí primero, antes de tocar código.

```
usuarios/{uid}
  - nombre, email, rol ("paciente" | "fisioterapeuta" | "admin")
  - fisioterapeutaId          (solo si rol = paciente; a quién está asignado)
  - diagnosticos: [{ codigo, fecha }]
                              (solo si rol = paciente; puede tener más de uno
                                a la vez — ej. osteoartrosis de rodilla +
                                desacondicionamiento general. `codigo` es un
                                valor del enum TipoDiagnostico que elige el
                                fisioterapeuta de un catálogo cerrado de 3
                                — HU01-CA06. Ver sección 9, Catálogo de
                                diagnósticos)
  - dni                       (solo si rol = paciente)
  - edad                      (ambos roles — capturados por el administrador
                                al registrarlo, HU20-CA02/HU21-CA02)
  - genero: "MASCULINO" | "FEMENINO" | "OTRO"
                              (ambos roles; actualización del modelo de
                                datos, no en la versión original —
                                HU20-CA02/HU21-CA02)
  - numeroContacto            (ambos roles; misma actualización)
  - especialidad, numeroColegiatura
                              (solo si rol = fisioterapeuta; opcionales
                                incluso para ese rol — HU21-CA02)
  - ladoAfectado: "DERECHO" | "IZQUIERDO" | "AMBOS"
                              (solo si rol = paciente; ampliación acordada,
                                no en la versión original. Un único valor
                                por paciente, no por diagnóstico — lo elige
                                el administrador al registrarlo/editarlo
                                (HU20-CA02/CA03, mismo formulario que el
                                checklist de diagnósticos), nunca lo infiere
                                el sistema. El motoreo (HU07/HU08) lo usa
                                para medir la articulación del lado real del
                                paciente en vez de asumir siempre el derecho
                                con el que se define `patronesReferencia`
                                — ver `Articulacion.espejo()`. Con "AMBOS"
                                (ejercicios pensados para alternar un lado a
                                la vez, ej. HOM-01), el sistema mide los dos
                                lados en cada frame y reporta el que tenga
                                mayor ángulo (el que se está levantando en
                                ese instante); si los dos superan el
                                `anguloMin` del patrón al mismo tiempo, se
                                detecta que el paciente mueve ambos lados
                                juntos en vez de alternar, y se marca como
                                error de coordinación ("Movimiento
                                simultáneo") en vez de una comparación de
                                rango normal. Sin dato, se mide el lado con
                                el que se definió el ejercicio (derecho))
  - fechaRegistro
  - activo: bool               (ambos roles, default true; recomendación
                                del modelo E-R-SANNA — desactivación
                                reversible de una cuenta, distinta de
                                eliminarla. Una cuenta con activo = false se
                                desloguea sola al entrar (`RaizViewModel`),
                                igual que si no tuviera documento. No
                                reemplaza a "Eliminar" — el admin dispone de
                                ambas acciones, ver HU20-CA08/HU21-CA05)

  usuarios/{pacienteId}/sesiones/{sesionId}
    - ejercicioId
    - fisioterapeutaId
    - fechaAsignacion, fechaEjecucion
    - estado                  ("pendiente" | "completada")
    - notas                   (texto libre opcional del fisioterapeuta al
                                asignar la sesión — HU03-CA05, Sprint 3)
    - repeticiones             (opcional; override de las repeticiones del
                                ejercicio SOLO para esta sesión puntual —
                                HU03-CA06, Sprint 3. Si es null, se usa el
                                valor por defecto de `ejercicios.repeticiones`)
    - duracionSegundos         (opcional; override de la duración por
                                repetición SOLO para esta sesión puntual —
                                HU03-CA06, ampliación 2026-09-10. Si es
                                null, se usa el valor por defecto de
                                `ejercicios.duracionSegundos`)
    - anguloMinOverride, anguloMaxOverride
                               (opcionales; override del ángulo objetivo
                                (mín/máx) SOLO para esta sesión/paciente —
                                HU03-CA08, ampliación 2026-09-16. Aplica
                                sobre la PRIMERA articulación de
                                `ejercicios.patronesReferencia`
                                (simplificación: el catálogo actual define
                                un solo patrón por ejercicio). Si
                                cualquiera de los dos es null, se usa el
                                rango por defecto del ejercicio)
    - resultado: {
        angulosDetectados, desviacionPromedio, porcentajeEjecucion,
        erroresDetectados: [{ articulacion, tipo, repeticiones }]
                             (anguloDetectado/anguloEsperado opcionales,
                              ampliación 2026-09-16 — HU18-CA05: solo se
                              completan dentro de detallePorRepeticion,
                              nunca aquí en el agregado de sesión),
        repeticionesCompletadas, repeticionesAsignadas, repeticionesCorrectas
                             (HU11-CA05, Sprint 3: completadas puede ser
                              menor a asignadas si se finalizó antes de
                              tiempo, HU06-CA07; correctas cuenta las
                              repeticiones sin ningún error detectado),
        detallePorRepeticion: [{ numero, porcentajeEjecucion, errores }]
                             (HU18-CA04: desglose por repetición que ve el
                              fisioterapeuta antes de recomendar, HU15.
                              `porcentajeEjecucion` reemplaza al booleano
                              `dentroDeRango` original — actualización del
                              modelo de datos: en vez de "sí/no cumplió",
                              muestra qué porcentaje de esa repetición
                              estuvo dentro del rango esperado, calculado
                              con el mismo criterio que el % global de la
                              sesión pero acotado a esa repetición.
                              `errores` es el mismo tipo que
                              `erroresDetectados` de arriba pero acotado a
                              esa repetición puntual, no agregado)
      }
    - sincronizado: bool      (para el manejo offline de RNF01 / HU19)

    usuarios/{pacienteId}/sesiones/{sesionId}/recomendaciones/{recomendacionId}
      - fisioterapeutaId, texto, fecha

ejercicios/{ejercicioId}
  - nombre, descripcion
  - categoria                (valor del enum CategoriaEjercicio —
                              "MOVILIDAD" | "CONTROL_MOTOR", catálogo cerrado
                              de 2 valores, ampliación acordada — antes texto
                              libre. Ver sección 7, Catálogo de tipos de
                              ejercicio)
  - materialUrl              (video/imagen en Firebase Storage)
  - duracionSegundos         (duración de CADA repetición, HU06-CA04)
  - repeticiones             (cuántas veces se repite el ciclo de
                              monitoreo dentro de una misma sesión —
                              HU02-CA08 / HU06-CA06, Sprint 3)
  - patronesReferencia: [{ articulacion, anguloMin, anguloMax }]
                             (ángulos ideales / ROM esperado, usado por HU08-09;
                              `articulacion` es un valor del enum Articulacion
                              — Sprint 3 — no texto libre, porque el sistema
                              necesita mapear cada patrón a una tripleta de
                              landmarks concreta de MediaPipe para calcular el
                              ángulo automáticamente)
  - diagnosticosAplicables: [codigo1, codigo2, ...]
                             (ampliación acordada: diagnósticos para los que
                              este ejercicio se sugiere primero al asignar
                              una sesión — HU03-CA07. No restringe la
                              selección a cualquier otro ejercicio del
                              catálogo, solo lo resalta. Ver sección 9)
  - creadoPor, fechaCreacion
  - activo                     (HU02-CA10, actualización del modelo de
                                datos: el fisioterapeuta desactiva un
                                ejercicio en vez de eliminarlo cuando ya
                                tiene sesiones históricas asociadas — se
                                oculta del selector de asignación, HU03,
                                pero conserva su historial; reactivable en
                                cualquier momento)
```

### Decisiones de diseño

- **`sesiones` es subcolección de cada paciente**, no colección aparte: el
  paciente siempre consulta sus propias sesiones (HU04, HU06, HU13), lo que
  encaja natural con subcolecciones y simplifica las Security Rules (RNF02:
  "solo el dueño del documento padre puede leer/escribir").
- El **fisioterapeuta**, que necesita ver sesiones de varios pacientes a la
  vez (HU12, HU14, HU18), usa una **collection group query** sobre
  `sesiones` filtrando por `fisioterapeutaId` — patrón estándar de Firestore
  para este caso, evita duplicar datos.
- **`recomendaciones` cuelga de la sesión**, no es independiente: en HU15/HU16
  siempre se consulta en el contexto de una sesión puntual.
- **`resultado` va embebido** dentro del documento de sesión, no en una
  colección separada, porque es 1:1 y siempre se lee junto con la sesión
  (evita una lectura extra a Firestore).
- Nombres de campos en español, consistentes con el lenguaje de las HU.
- **`diagnosticos` y `diagnosticosAplicables` van como arrays embebidos**,
  no como colecciones aparte: el catálogo de diagnósticos es fijo y pequeño
  (13 valores, sección 9), así que crear una colección `diagnosticos/`
  sería sobre-ingeniería. La sugerencia de ejercicios (HU03-CA07) se
  resuelve client-side (el catálogo de ejercicios ya está cargado en la
  pantalla de asignar sesión, son ~10 documentos) comparando ambos arrays,
  sin necesidad de una query `array-contains-any` dedicada.

### Pendiente de validar con el equipo

- ~~¿Un paciente puede tener más de un fisioterapeuta a la vez?~~ Resuelto
  (HU20-CA05): no, el modelo asume uno solo — una vez asignado, la opción
  de asignar deja de estar disponible para ese paciente en el panel de
  administrador.
- ¿Se necesita versionar `patronReferencia` de un ejercicio si cambia con el
  tiempo, o basta con el valor vigente?

---

## 6. Modelo de datos relacional (lógico)

> Este es el modelo relacional lógico (normalizado) del sistema, pensado para
> razonar sobre entidades, integridad referencial y relaciones — es un nivel
> de diseño distinto al modelo físico de Firestore (NoSQL, desnormalizado a
> propósito). Ver la sección "Traducción a Firestore" al final para la
> equivalencia entre ambos. El modelo de Firestore (el que realmente se
> implementa en `/app`) está documentado en la sección 5.

### Entidades

Derivadas de las 21 Historias de Usuario del backlog (incluida la Épica 07,
Administrar cuentas del sistema, agregada durante el desarrollo). Se
agregaron entidades que no eran explícitas en el modelo Firestore pero que
un modelo relacional normalizado requiere: **CategoriaEjercicio** y
**TipoError** (catálogos, para no repetir texto libre), y se separaron
**AnguloReferencia**/**DetalleAngulo** como tablas propias en vez de campos
sueltos, porque un ejercicio tiene *varios* ángulos de referencia (uno por
articulación), no uno solo.

#### 1. Usuario
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| usuario_id | VARCHAR(28) | PK | Coincide con el UID de Firebase Auth |
| nombre | VARCHAR(150) | | |
| email | VARCHAR(150) | UNIQUE | |
| rol | VARCHAR(20) | | CHECK: 'paciente' \| 'fisioterapeuta' \| 'admin' |
| fecha_registro | TIMESTAMP | | |

#### 2. Paciente
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| paciente_id | VARCHAR(28) | PK, FK → Usuario | Subtipo 1:1 de Usuario (rol='paciente') |
| fisioterapeuta_id | VARCHAR(28) | FK → Usuario, NULL | El fisioterapeuta asignado (HU20-CA05: uno solo, hasta que se asigna no puede cambiarse desde el panel de admin) |
| dni | VARCHAR(15) | UNIQUE | Capturado por el administrador al registrarlo (HU20-CA02) |
| edad | INT | | |

#### 3. CategoriaEjercicio
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| categoria_id | INT | PK | |
| nombre | VARCHAR(50) | UNIQUE | Catálogo cerrado de 2 valores: "Movilidad", "Control motor" — sección 7 |

#### 4. Ejercicio
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| ejercicio_id | VARCHAR(28) | PK | |
| nombre | VARCHAR(150) | | |
| descripcion | TEXT | | |
| categoria_id | INT | FK → CategoriaEjercicio | |
| material_url | VARCHAR(300) | | Referencia a Firebase Storage |
| duracion_segundos | INT | | Duración de cada repetición (HU06-CA04) |
| repeticiones | INT | | Veces que se repite el ciclo de monitoreo por sesión (HU02-CA08) |
| creado_por | VARCHAR(28) | FK → Usuario | Debe ser un fisioterapeuta |
| fecha_creacion | TIMESTAMP | | |
| activo | BOOLEAN | | |

#### 5. AnguloReferencia
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| angulo_ref_id | INT | PK | |
| ejercicio_id | VARCHAR(28) | FK → Ejercicio | |
| articulacion | VARCHAR(50) | | Valor del enum Articulacion (landmarks de MediaPipe) |
| angulo_min | DECIMAL(5,2) | | ROM esperado (usado en HU08) |
| angulo_max | DECIMAL(5,2) | | |
| | | UNIQUE(ejercicio_id, articulacion) | |

#### 6. Sesion
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| sesion_id | VARCHAR(28) | PK | |
| paciente_id | VARCHAR(28) | FK → Paciente | |
| ejercicio_id | VARCHAR(28) | FK → Ejercicio | |
| fisioterapeuta_id | VARCHAR(28) | FK → Usuario | Quién la asignó |
| fecha_asignacion | TIMESTAMP | | |
| fecha_ejecucion | TIMESTAMP | NULL | Nula hasta completarse |
| estado | VARCHAR(20) | | 'pendiente' \| 'completada' |
| notas | TEXT | NULL | Indicación puntual del fisio al asignar (HU03-CA05) |
| repeticiones | INT | NULL | Override puntual de `Ejercicio.repeticiones` (HU03-CA06) |
| sincronizado | BOOLEAN | | Para HU19 |

#### 7. TipoError
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| tipo_error_id | INT | PK | |
| nombre | VARCHAR(100) | UNIQUE | "Rango incompleto", "Desviación angular" |

#### 8. ResultadoSesion
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| resultado_id | VARCHAR(28) | PK | |
| sesion_id | VARCHAR(28) | FK → Sesion, UNIQUE | Relación 1:1 |
| porcentaje_ejecucion | DECIMAL(5,2) | | |
| desviacion_promedio | DECIMAL(5,2) | | |
| repeticiones_completadas | INT | | Puede ser < asignadas si se finalizó antes (HU06-CA07) |
| repeticiones_asignadas | INT | | |
| repeticiones_correctas | INT | | Repeticiones sin ningún error detectado |

#### 9. DetalleAngulo
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| detalle_id | INT | PK | |
| resultado_id | VARCHAR(28) | FK → ResultadoSesion | |
| articulacion | VARCHAR(50) | | |
| angulo_detectado | DECIMAL(5,2) | | Desglose por articulación (HU08-CA04) |
| angulo_esperado | DECIMAL(5,2) | | |
| desviacion | DECIMAL(5,2) | | |

#### 10. ErrorSesion
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| error_sesion_id | INT | PK | |
| resultado_id | VARCHAR(28) | FK → ResultadoSesion | |
| tipo_error_id | INT | FK → TipoError | |
| articulacion | VARCHAR(50) | | |
| repeticiones | INT | | Cuántas veces ocurrió este error en la sesión (HU08-CA04) |

#### 11. DetalleRepeticion
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| detalle_repeticion_id | INT | PK | |
| resultado_id | VARCHAR(28) | FK → ResultadoSesion | |
| numero | INT | | Número real de la repetición (considera reanudaciones, HU06-CA09) |
| dentro_de_rango | BOOLEAN | | |

#### 12. Recomendacion
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| recomendacion_id | VARCHAR(28) | PK | |
| sesion_id | VARCHAR(28) | FK → Sesion | |
| fisioterapeuta_id | VARCHAR(28) | FK → Usuario | Autor |
| texto | TEXT | | |
| fecha | TIMESTAMP | | |

### Diagnóstico clínico y sugerencia de ejercicios

Un paciente puede tener **más de un diagnóstico a la vez** (ej. osteoartrosis
de rodilla + desacondicionamiento general), así que no se modela como un
campo único en Paciente, sino como una relación N:M contra un catálogo.

#### 13. TipoDiagnostico
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| diagnostico_id | INT | PK | |
| nombre | VARCHAR(150) | UNIQUE | Catálogo completo en la sección 9 |
| region_corporal | VARCHAR(50) | | Ej. "Rodilla", "Hombro", "Columna", "General" |

#### 14. PacienteDiagnostico
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| paciente_id | VARCHAR(28) | PK compuesta, FK → Paciente | |
| diagnostico_id | INT | PK compuesta, FK → TipoDiagnostico | |
| fecha_diagnostico | DATE | | |

#### 15. DiagnosticoEjercicioRecomendado
| Campo | Tipo | Llave | Notas |
|---|---|---|---|
| diagnostico_id | INT | PK compuesta, FK → TipoDiagnostico | |
| ejercicio_id | VARCHAR(28) | PK compuesta, FK → Ejercicio | |

Con estas dos tablas N:M, al asignar una sesión (HU03-CA07) el sistema puede
resolver "¿qué ejercicios sugiero primero para este paciente?" cruzando
`PacienteDiagnostico` → `DiagnosticoEjercicioRecomendado` → `Ejercicio`, sin
bloquear que el fisio elija cualquier otro ejercicio del catálogo.

### Relaciones (cardinalidad)

- Usuario (1) — (1) Paciente *(subtipo)*
- Usuario/fisioterapeuta (1) — (N) Paciente *(asignación, HU20-CA05)*
- Usuario/fisioterapeuta (1) — (N) Ejercicio *(creado_por)*
- CategoriaEjercicio (1) — (N) Ejercicio
- Ejercicio (1) — (N) AnguloReferencia
- Paciente (1) — (N) Sesion
- Ejercicio (1) — (N) Sesion
- Usuario/fisioterapeuta (1) — (N) Sesion
- Sesion (1) — (1) ResultadoSesion
- ResultadoSesion (1) — (N) DetalleAngulo
- ResultadoSesion (1) — (N) ErrorSesion *(TipoError (1) — (N) ErrorSesion)*
- ResultadoSesion (1) — (N) DetalleRepeticion
- Sesion (1) — (N) Recomendacion
- Usuario/fisioterapeuta (1) — (N) Recomendacion
- Paciente (N) — (M) TipoDiagnostico *(vía PacienteDiagnostico)*
- TipoDiagnostico (N) — (M) Ejercicio *(vía DiagnosticoEjercicioRecomendado)*

### Traducción a Firestore (lo que realmente se implementa)

No son dos esquemas contradictorios: es el mismo diseño en dos niveles. Este
es el mapeo que debe seguir el código en `/app` y las reglas en `/backend`.

| Tabla relacional | En Firestore |
|---|---|
| Usuario, Paciente | `usuarios/{uid}` — fusionadas en un solo documento; `fisioterapeutaId`/`dni`/`edad` reemplazan a la tabla Paciente |
| Ejercicio + AnguloReferencia | `ejercicios/{id}`, con `patronesReferencia` como lista embebida (no como tabla aparte) |
| Sesion + ResultadoSesion + DetalleAngulo + ErrorSesion + DetalleRepeticion | `usuarios/{pacienteId}/sesiones/{id}`, con `resultado` embebido (relación 1:1, siempre se lee junto con la sesión) |
| Recomendacion | Subcolección `usuarios/{pacienteId}/sesiones/{id}/recomendaciones/{id}` |
| CategoriaEjercicio, TipoError | Se guardan como **string** dentro del documento, no como colección aparte — catálogos fijos de 2-5 valores, una colección separada sería sobre-ingeniería en NoSQL |
| TipoDiagnostico, PacienteDiagnostico | `usuarios/{pacienteId}.diagnosticos: [{ codigo, fecha }]` — array embebido, mismo criterio |
| DiagnosticoEjercicioRecomendado | `ejercicios/{id}.diagnosticosAplicables: [codigo1, codigo2, ...]` |

**Por qué mantener ambos modelos:** en Firestore se gana velocidad de
lectura (un solo `get()` trae la sesión completa con su resultado); en el
modelo relacional se gana integridad referencial explícita y una forma más
fácil de verificar que no falte ni sobre ninguna entidad. Este documento es
la referencia para razonar sobre el dominio; la sección 5 es la referencia
para lo que efectivamente se implementa.

---

## 7. Catálogo de tipos de ejercicio (categorías)

La tesis define explícitamente el alcance de la app como ejercicios de
**movilidad y control motor** (no fortalecimiento con resistencia, no
cardio, no motricidad fina de manos/dedos — MediaPipe Pose trackea
articulaciones grandes del cuerpo, no manos con precisión). Por eso el
catálogo de ejercicios (sección 8) se organiza en **exactamente 2 tipos**.

### Tipo 1: Movilidad

Ejercicios orientados a mejorar o mantener el rango de movimiento articular
(ROM) de una articulación específica. Se miden por el ángulo alcanzado en
el movimiento, comparado contra un rango de referencia.

### Tipo 2: Control motor

Ejercicios orientados a la estabilidad, coordinación y control del
movimiento corporal, generalmente involucrando más de una articulación a
la vez.

### Por qué solo estos 2 tipos (guardrail para el catálogo)

Frontera deliberada del alcance, no un olvido. Si en el futuro se quiere
agregar un ejercicio que no encaje claramente en "Movilidad" ni en "Control
motor" (fortalecimiento con banda elástica, motricidad fina de mano),
**eso es una señal para revisar el alcance con el asesor de tesis antes de
crear un tercer tipo por conveniencia**.

### Estructura de datos

`ejercicios/{id}.categoria: "MOVILIDAD" | "CONTROL_MOTOR"` (enum
`CategoriaEjercicio`, ampliación acordada — antes era texto libre).

---

## 8. Catálogo de ejercicios predeterminados

Catálogo del MVP (reemplaza por completo al catálogo anterior de 9
ejercicios, no es una ampliación) sembrado en Firestore vía
`backend/seed/crear-catalogo-ejercicios.ts`. Fuente:
`Catalogo_Ejercicios_MVP_SANNA_Especificaciones.docx` — 12 ejercicios para
las 3 condiciones de la sección 9, elegidos combinando frecuencia clínica
documentada (ver sección 9) con viabilidad técnica real: qué movimientos
se pueden representar de forma confiable con landmarks y métricas
geométricas de MediaPipe Pose/BlazePose, según literatura de pose
estimation citada en el documento fuente. El código (LUM-01, OAR-02, ...)
es solo de trazabilidad con el documento fuente — no se guarda en
Firestore.

| Código | Ejercicio | Categoría | Articulación | Vista de cámara | Repeticiones | Rango de referencia (seed)* |
|---|---|---|---|---|---|---|
| LUM-01 | Bisagra de cadera (Hip Hinge) | Movilidad | Cadera derecha | Lateral | 6 | 90°–150° |
| LUM-02 | Marcha estacionaria con control de tronco | Movilidad | Cadera derecha | Frontal | 6 | 100°–140° |
| LUM-03 | Inclinación lateral de tronco | Movilidad | Tronco | Frontal | 6 | 70°–110° |
| LUM-04 | Puente de glúteo | Control motor | Cadera derecha | Lateral, cámara baja | 6 | 160°–180° |
| OAR-01 | Extensión de rodilla en sedestación | Movilidad | Rodilla derecha | Lateral | 6 | 140°–175° |
| OAR-02 | Flexión de rodilla en bipedestación | Movilidad | Rodilla derecha | Lateral | 6 | 90°–140° |
| OAR-03 | Mini-sentadilla | Control motor | Rodilla derecha | Frontal (+ lateral complementaria) | 6 | 120°–150° |
| OAR-04 | Levantarse de una silla (Sit-to-Stand) | Control motor | Rodilla derecha | Lateral | 5 | 150°–180° |
| SDS-01 | Abducción de hombro | Movilidad | Hombro derecho | Frontal | 3 | 70°–110° |
| SDS-02 | Flexión de hombro | Movilidad | Hombro derecho | Lateral | 3 | 70°–110° |
| SDS-03 | Trepado de dedos en pared | Movilidad | Hombro derecho | Frontal | 3 | 80°–130° |
| SDS-04 | Flexión unilateral alternada de hombro | Movilidad | Hombro derecho | Lateral/oblicua | 6 | 70°–110° |

\* Estos ángulos están en la convención de este sistema (el ángulo crudo
calculado por `AnguloCalculator` en el vértice de la articulación, no la
goniometría clínica tradicional). Los 4 ejercicios cuyo movimiento
coincide con el catálogo anterior (LUM-04, OAR-03, SDS-01, SDS-02) reusan
el rango ya usado antes; el resto son un **punto de partida técnico**
provisional, no una dosis clínica. El fisioterapeuta puede y debe
refinarlos por paciente, ya sea editándolos a mano (HU02-CA05) o
recalculándolos analizando un video real con "Calcular ROM automáticamente"
(HU02-CA07) una vez que existan clips grabados — el seed deja
`materialUrl` vacío a propósito, pendiente de que se graben esos videos.

### Validación piloto prioritaria

El documento fuente marca explícitamente **LUM-04 (puente de glúteo),
OAR-04 (sit-to-stand) y SDS-03 (trepado de dedos en pared)** como los 3
ejercicios a validar primero con un fisioterapeuta antes de fijar sus
métricas definitivas — son los que presentan mayor riesgo técnico para
pose estimation monocular: LUM-04 por la cámara cercana al suelo, OAR-04
por las oclusiones de la silla durante la transición sentado↔de pie, y
SDS-03 porque MediaPipe no puede verificar directamente el contacto de
los dedos con la pared (el análisis debe centrarse en trayectoria de
muñeca y ROM de hombro, no en el contacto en sí).

### Límites explícitos del catálogo (documento fuente, sección 5)

La aplicación no debe diagnosticar, determinar gravedad clínica, sustituir
al fisioterapeuta, ni afirmar una precisión equivalente a un goniómetro o
a un sistema de captura de movimiento de laboratorio — MediaPipe Pose es
una estimación computacional del movimiento, no una medición clínica de
referencia (mismo espíritu ya establecido en RNF06).

---

## 9. Catálogo de diagnósticos

Catálogo real del enum `TipoDiagnostico` (**3 valores** — catálogo del
MVP, reemplaza por completo al catálogo anterior de 13 diagnósticos, no
es una ampliación). Elegido con respaldo bibliográfico real: la OMS
(carga de discapacidad de la lumbalgia), la Guía de Práctica Clínica de
EsSalud 2025 (osteoartritis) y un estudio de 366 pacientes en un centro
de rehabilitación de Villa El Salvador, Lima (frecuencia de lumbalgia,
gonalgia y hombro doloroso) — ver
`Catalogo_Ejercicios_MVP_SANNA_Especificaciones.docx`. El documento
fuente aclara explícitamente que esta selección **no afirma ser la más
frecuente dentro de SANNA** (no hay datos internos públicos de su
servicio) y que la selección final debe validarse con sus
fisioterapeutas. Igual que antes: **la app no genera diagnósticos** — el
fisioterapeuta registra el que ya obtuvo de su evaluación clínica; el
sistema solo lo usa para **sugerir** ejercicios relacionados (HU03-CA07),
sin decidir ni bloquear nada por su cuenta.

### Lista de diagnósticos (3), agrupados por región corporal

| Región | Diagnóstico | Ejercicios sugeridos (del catálogo) |
|---|---|---|
| Columna | Lumbalgia inespecífica | Bisagra de cadera, Marcha estacionaria con control de tronco, Inclinación lateral de tronco, Puente de glúteo |
| Rodilla | Osteoartritis de rodilla (gonartrosis) | Extensión de rodilla en sedestación, Flexión de rodilla en bipedestación, Mini-sentadilla, Levantarse de una silla |
| Hombro | Síndrome de dolor subacromial / hombro doloroso | Abducción de hombro, Flexión de hombro, Trepado de dedos en pared, Flexión unilateral alternada de hombro |

### Estructura de datos (Firestore)

```
usuarios/{pacienteId}
  - diagnosticos: [
      { codigo: "OSTEOARTROSIS_RODILLA", fecha: <Timestamp> }
    ]

ejercicios/{ejercicioId}
  - diagnosticosAplicables: ["LUMBALGIA_INESPECIFICA"]
```

La sugerencia de HU03-CA07 se resuelve **client-side**: la pantalla de
asignar sesión ya tiene cargado el catálogo completo de ejercicios (12
documentos), así que compara `diagnosticosAplicables` de cada ejercicio
contra los códigos en `usuarios/{pacienteId}.diagnosticos` sin necesitar
una query aparte — los sugeridos se muestran primero en el selector
(marcados con ★), sin impedir elegir cualquier otro.

### Historias de usuario afectadas

- **HU01-CA06** — el fisioterapeuta registra/actualiza uno o más
  diagnósticos de un paciente.
- **HU03-CA07** — el sistema resalta primero los ejercicios sugeridos
  según el/los diagnóstico(s) del paciente al asignar una sesión.

---

## 10. Historias de Usuario y Requisitos No Funcionales

Fuente de verdad del producto. 19 Historias de Usuario + 6 Requisitos No
Funcionales, ya depurados (sin criterios de aceptación genéricos/duplicados)
y priorizados en 5 sprints.

---

### ÉPICA 01: Gestionar sesiones terapéuticas

#### HU01 — Gestionar pacientes terapéuticos
**Rol:** Fisioterapeuta
**Deseo:** Visualizar y gestionar los pacientes asignados
**Propósito:** Realizar el seguimiento de sus sesiones terapéuticas.
- CA01: Dado que el fisioterapeuta acceda al sistema, cuando seleccione "Pacientes", entonces el sistema muestra el panel con la lista de pacientes asignados.
- CA02: Dado que visualiza pacientes registrados, cuando seleccione uno específico, entonces el sistema muestra su información terapéutica.
- CA03: Dado que consulta un paciente, cuando acceda a su detalle, entonces el sistema muestra progreso y sesiones registradas.
- CA04: Dado que desee ubicar un paciente, cuando ingrese un criterio de búsqueda, entonces el sistema filtra la lista mostrada.
- CA05: Dado que no tenga pacientes asignados, cuando acceda al módulo, entonces el sistema muestra un mensaje de ausencia de pacientes.
- CA06 *(ampliación acordada, no en la versión original de la tesis; pasó
  de texto libre a catálogo cerrado, luego de un solo valor a una lista, y
  finalmente el catálogo se redujo de 13 a 3 diagnósticos del MVP con
  respaldo bibliográfico — ver sección 9, Catálogo de diagnósticos)*:
  Dado que consulte el detalle de un paciente, cuando elija uno o más
  diagnósticos de un catálogo cerrado de 3 valores agrupados por región
  corporal (sección 9, Catálogo de diagnósticos), entonces el sistema los
  guarda (cada uno con su propia fecha) y los muestra junto al resto de su
  información terapéutica (incluida la lista de pacientes). No es texto
  libre: el fisioterapeuta selecciona de la lista, no redacta; un paciente
  puede tener más de un diagnóstico a la vez (ej. osteoartrosis de rodilla +
  desacondicionamiento general).

#### HU02 — Gestionar ejercicios terapéuticos

**Declaración de la Historia de Usuario**

**Rol**
YO COMO FISIOTERAPEUTA

**Deseo**
QUIERO REGISTRAR Y ADMINISTRAR EJERCICIOS TERAPÉUTICOS, DEFINIENDO LAS ACCIONES QUE COMPONEN CADA REPETICIÓN

**Propósito**
CON EL OBJETIVO DE ASIGNAR ACTIVIDADES TERAPÉUTICAS A LOS PACIENTES CON UNA GUÍA DE MOVIMIENTO ADECUADA A CADA EJERCICIO

**Criterios de Aceptación**

CA01
Dado que desee registrar un ejercicio,
Cuando seleccione "Registrar ejercicio",
Entonces el sistema muestra el formulario.

CA02
Dado que complete los datos requeridos,
Cuando los envíe,
Entonces el sistema almacena la información.

CA03
Dado que desee asociar material terapéutico,
Cuando seleccione contenido audiovisual,
Entonces el sistema lo almacena correctamente.

CA04
Dado que existan ejercicios registrados,
Cuando acceda al módulo,
Entonces el sistema muestra la lista disponible.

CA05
Dado que desee modificar un ejercicio,
Cuando actualice la información,
Entonces el sistema guarda los cambios.

CA06
Dado que desee eliminar un ejercicio,
Cuando confirme la acción,
Entonces el sistema lo elimina.

CA07
Dado que asocie un video como material terapéutico y haya seleccionado al menos una articulación de referencia,
Cuando solicite el cálculo automático de rango,
Entonces el sistema analiza el video en el propio dispositivo y completa el ángulo mínimo y máximo observado para cada articulación.

CA08
Dado que el sistema haya calculado el rango automáticamente,
Cuando el fisioterapeuta edite esos valores,
Entonces el sistema guarda los valores editados.

CA09
Dado que registre un ejercicio,
Cuando indique el número de repeticiones,
Entonces el sistema lo guarda como las veces que se repite la secuencia de acciones dentro de una sesión.

CA10
Dado que registre un ejercicio,
Cuando indique la duración,
Entonces el sistema la guarda junto con el número de repeticiones.

CA11
Dado que registre o edite un ejercicio,
Cuando elija su categoría,
Entonces el sistema restringe la opción a un catálogo cerrado de 2 valores: Movilidad o Control motor.

CA12
Dado que registre o edite un ejercicio,
Cuando marque uno o más diagnósticos,
Entonces el sistema los guarda para sugerir ese ejercicio primero al asignar sesiones.

CA13
Dado que un ejercicio ya no deba usarse para nuevas asignaciones pero tenga sesiones históricas,
Cuando el fisioterapeuta lo desactive,
Entonces el sistema lo oculta del listado de asignación y conserva su historial.

CA14
Dado que un ejercicio esté desactivado,
Cuando el fisioterapeuta lo reactive,
Entonces el sistema lo muestra nuevamente en el listado de asignación.

CA15
Dado que registre o edite un ejercicio,
Cuando agregue una acción,
Entonces el sistema permite definir su nombre, su orden, el segmento corporal a medir (punto de referencia, pivote, punto intermedio opcional y punto distal), el lado, el ángulo de inicio, el ángulo meta, la tolerancia, el tiempo de mantenimiento y el tiempo máximo.

CA16
Dado que un ejercicio tenga varias acciones,
Cuando el fisioterapeuta defina su orden,
Entonces el sistema las guarda y las presenta al paciente en ese orden.

CA17
Dado que registre o edite un ejercicio,
Cuando seleccione la vista de cámara requerida (lateral, frontal u oblicua),
Entonces el sistema la guarda como la posición en que el paciente debe ubicarse frente a la cámara.

CA18
Dado que defina una acción,
Cuando ingrese una tolerancia o un tiempo máximo menor o igual a cero, o un ángulo fuera del rango de 0° a 180°,
Entonces el sistema rechaza el dato e indica cuál es incorrecto.

CA19
Dado que exista un ejercicio sin acciones definidas,
Cuando el sistema lo use en una sesión,
Entonces lo trata como un ejercicio de una única acción generada a partir de su rango de referencia.

#### HU03 — Asignar sesiones terapéuticas
**Rol:** Fisioterapeuta
**Deseo:** Asignar sesiones terapéuticas a los pacientes
**Propósito:** Indicar los ejercicios que deberán realizar.
- CA01: Dado que accede al módulo terapéutico, cuando seleccione un paciente, entonces el sistema muestra opciones de asignación.
- CA02: Dado que selecciona ejercicios y confirma, entonces el sistema almacena la sesión con la fecha de asignación.
- CA03: Dado que desea modificar una sesión ya asignada, cuando actualice ejercicios o fecha, entonces el sistema guarda los cambios.
- CA04: Dado que el paciente consulta sus actividades, entonces el sistema muestra las sesiones asignadas.
- CA05 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que esté asignando o editando una sesión, cuando agregue una nota
  (texto libre, opcional), entonces el sistema la guarda junto con la
  sesión. Sirve para indicaciones puntuales del fisioterapeuta sobre esa
  sesión en particular (ej. "hacerlo con apoyo"); no reemplaza a las
  recomendaciones de HU15/HU16, que se registran sobre una sesión ya
  realizada, no al momento de asignarla.
- CA06 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que esté asignando o editando una sesión, el sistema precarga el
  número de repeticiones configurado por defecto en el ejercicio
  (HU02-CA08), pero permite que el fisioterapeuta lo cambie solo para
  esa sesión puntual (ej. reducirlo para un paciente que recién empieza),
  sin alterar el valor por defecto del ejercicio ni el de otras sesiones
  ya asignadas o futuras. El sistema también muestra la duración total
  estimada de la sesión (repeticiones × duración por repetición) como
  referencia antes de guardar. *(Ampliación posterior, 2026-09-10: el
  mismo criterio de override aplica tambien a la duración por
  repetición — el sistema precarga el valor por defecto del ejercicio
  (ej. 10s) pero el fisioterapeuta puede cambiarlo solo para esa sesión
  puntual (ej. 30s), sin alterar el ejercicio ni otras sesiones. La
  duración total estimada se recalcula con el valor que el fisio haya
  puesto, y la propia ejecución de la sesión (HU06) usa ese valor
  override, no el default del ejercicio, para el cronómetro de cada
  repetición.)*
- CA07 *(ampliación acordada, no en la versión original de la tesis)*: Dado
  que el paciente tenga uno o más diagnósticos registrados (HU01-CA06),
  cuando el fisioterapeuta abra el selector de ejercicios para asignar,
  entonces el sistema resalta primero (★) los ejercicios sugeridos para
  esos diagnósticos (según `ejercicios/{id}.diagnosticosAplicables`, sección
  9), sin impedir seleccionar cualquier otro ejercicio del catálogo.
- CA08 *(ampliación acordada, 2026-09-16, Etapa 4)*: Dado que esté
  asignando o editando una sesión, cuando marque "Personalizar ángulo
  objetivo para este paciente" e ingrese un mínimo/máximo, entonces el
  sistema guarda ese rango SOLO para esta sesión/paciente (campos
  `anguloMinOverride`/`anguloMaxOverride` en `Sesion`, sección 5) — el
  monitoreo (HU07/HU08) usa ese rango en vez del `patronesReferencia` por
  defecto del ejercicio para la primera articulación medida
  (simplificación: el catálogo actual define un solo patrón por
  ejercicio). El campo se precarga con el rango por defecto del ejercicio
  al seleccionarlo. Si el paciente tiene una nota (HU03-CA05) y hay
  personalización activa, el paciente ve un aviso antes de iniciar la
  sesión ("Este video es referencial. Tu fisioterapeuta ha ajustado este
  ejercicio para tu etapa actual...") en `DetalleEjercicioAsignadoScreen`
  — no se agregó un campo "nota clínica" nuevo, se reutiliza `notas`.

#### HU04 — Visualizar ejercicios asignados

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO VISUALIZAR LOS EJERCICIOS TERAPÉUTICOS ASIGNADOS Y EL DETALLE DE CADA UNO

**Propósito**
CON EL OBJETIVO DE CONOCER LAS ACTIVIDADES QUE DEBO REALIZAR Y CÓMO EJECUTARLAS

**Criterios de Aceptación**

CA01
Dado que el paciente acceda al módulo de ejercicios,
Cuando seleccione "Ejercicios",
Entonces el sistema muestra las actividades asignadas.

CA02
Dado que no tenga ejercicios asignados,
Cuando acceda al módulo de ejercicios,
Entonces el sistema muestra un mensaje de ausencia.

CA03
Dado que existan ejercicios asignados,
Cuando seleccione uno de ellos,
Entonces el sistema muestra su detalle con el material audiovisual y las instrucciones de ejecución.

CA04
Dado que consulte el detalle de un ejercicio,
Cuando visualice la información del ejercicio,
Entonces el sistema muestra el ángulo objetivo, personalizado por el fisioterapeuta o el predeterminado del ejercicio.

CA05
Dado que consulte el detalle de un ejercicio,
Cuando visualice la información del ejercicio,
Entonces el sistema muestra el número de repeticiones de la sesión.

CA06
Dado que el fisioterapeuta haya registrado una nota para la sesión,
Cuando consulte el detalle del ejercicio,
Entonces el sistema muestra la nota del fisioterapeuta.


#### HU05 — Consultar material terapéutico
**Rol:** Paciente
**Deseo:** Consultar material terapéutico
**Propósito:** Visualizar contenido de apoyo para la ejecución de los ejercicios.
- CA01: Dado que accede a sus ejercicios, cuando seleccione uno asignado, entonces el sistema muestra el material asociado.
- CA02: Dado que existe contenido audiovisual, cuando lo consulte, entonces el sistema lo reproduce correctamente.
- CA03: Dado que visualiza contenido, cuando acceda al material, entonces el sistema muestra las instrucciones asociadas.
- CA04: Dado que desea prepararse antes de una sesión, cuando consulte el material, entonces el sistema permite visualizarlo previamente.

---

### ÉPICA 02: Monitorear ejecución de ejercicios terapéuticos

> **Ampliación acordada, 2026-10-05 (guía visual en vivo — HU redactadas, pendientes de aprobación):** cada ejercicio se
> compone de N repeticiones y cada repetición de varias *acciones* definidas como datos por el fisioterapeuta (segmento a
> medir, ángulo de inicio, ángulo meta, tolerancia, tiempo de mantenimiento y tiempo máximo). La app pinta sobre la cámara el
> recorrido de la acción actual, anclado al cuerpo del paciente, y avanza sola de acción en acción. Es un refinamiento técnico
> de HU02, HU06, HU07, HU08, HU09, HU10, HU17 y HU18, sin sprint nuevo. Plan por etapas en
> `docs/guia_visual/prompts_guia_visual_en_vivo.md`; backlog en `docs/analisis/03_Product_Backlog.md` (sección 3.5).

#### HU06 — Ejecutar sesión terapéutica

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO EJECUTAR UNA SESIÓN TERAPÉUTICA SIGUIENDO, ACCIÓN POR ACCIÓN, EL MOVIMIENTO DE CADA REPETICIÓN

**Propósito**
CON EL OBJETIVO DE REALIZAR LOS EJERCICIOS ASIGNADOS POR EL FISIOTERAPEUTA CORRIGIENDO MI MOVIMIENTO HASTA CUMPLIR CADA ACCIÓN

**Criterios de Aceptación**

CA01
Dado que seleccione una sesión asignada,
Cuando acceda a ella,
Entonces el sistema muestra el ejercicio correspondiente.

CA02
Dado que inicie la sesión,
Cuando seleccione "Iniciar sesión",
Entonces el sistema enciende la cámara y muestra una cuenta regresiva de preparación de 10 segundos sin medir el movimiento.

CA03
Dado que termine la cuenta regresiva,
Cuando el paciente esté frente a la cámara,
Entonces el sistema inicia el monitoreo de la primera acción de la primera repetición.

CA04
Dado que el ejercicio tenga acciones definidas,
Cuando inicie una repetición,
Entonces el sistema presenta la primera acción en el orden definido.

CA05
Dado que el paciente mantenga el segmento corporal dentro de la zona meta durante el tiempo de mantenimiento de la acción,
Cuando complete ese tiempo de forma continua,
Entonces el sistema registra la acción como completada y presenta la siguiente.

CA06
Dado que el paciente salga de la zona meta antes de completar el tiempo de mantenimiento,
Cuando abandone la zona,
Entonces el sistema reinicia el contador de mantenimiento.

CA07
Dado que la acción actual no se haya cumplido y no se haya agotado su tiempo máximo,
Cuando el paciente continúe ejecutando,
Entonces el sistema sigue corrigiendo dentro de la misma acción.

CA08
Dado que el paciente no cumpla la acción actual,
Cuando se agote su tiempo máximo,
Entonces el sistema la registra como no completada con el ángulo máximo alcanzado y presenta la siguiente acción.

CA09
Dado que el paciente complete o agote la última acción de una repetición,
Cuando esa acción concluya,
Entonces el sistema cuenta la repetición y reinicia la secuencia desde la primera acción.

CA10
Dado que el ejercicio tenga más repeticiones asignadas por completar,
Cuando termine una repetición,
Entonces el sistema anuncia la siguiente tras una breve pausa.

CA11
Dado que falten 3 segundos para iniciar la siguiente repetición,
Cuando el sistema esté en pausa,
Entonces el sistema avisa una sola vez por voz que se acerca la siguiente repetición.

CA12
Dado que el paciente complete todas las repeticiones asignadas,
Cuando termine la última,
Entonces el sistema concluye la sesión y la registra.

CA13
Dado que el sistema concluya la sesión,
Cuando la registre,
Entonces el resultado de todas las repeticiones queda consolidado en un único registro.

CA14
Dado que la sesión esté en ejecución,
Cuando el paciente seleccione "Finalizar ejercicio" antes de completar las repeticiones asignadas,
Entonces el sistema concluye la sesión y registra el resultado con lo medido hasta ese momento.

CA15
Dado que la sesión esté en ejecución,
Cuando el paciente seleccione "Salir",
Entonces el sistema abandona la pantalla sin registrar resultado.

CA16
Dado que el paciente inicie el monitoreo de una sesión,
Cuando comience la primera acción,
Entonces el sistema lee en voz alta la instrucción del ejercicio una sola vez.

CA17
Dado que una sesión se haya finalizado antes de completar todas las repeticiones asignadas,
Cuando el paciente consulte "Mis ejercicios",
Entonces el sistema la muestra como reanudable con las repeticiones completadas sobre las asignadas.

CA18
Dado que el paciente reanude una sesión,
Cuando inicie el monitoreo,
Entonces el sistema continúa desde la repetición siguiente a la última completada sin repetir lo ya medido.

CA19
Dado que el paciente complete una sesión reanudada,
Cuando el sistema registre el resultado,
Entonces combina lo guardado antes con lo nuevo medido, sumando las repeticiones correctas, concatenando el detalle con su numeración real y ponderando los promedios por las repeticiones de cada tramo.

CA20
Dado que una sesión complete todas sus repeticiones asignadas,
Cuando el paciente consulte "Mis ejercicios",
Entonces el sistema deja de mostrarla como reanudable.

#### HU07 — Monitorear movimiento corporal

**Declaración de la Historia de Usuario**

**Rol**
YO COMO SISTEMA

**Deseo**
QUIERO MONITOREAR EL MOVIMIENTO CORPORAL DEL PACIENTE MEDIANTE LA CÁMARA

**Propósito**
CON EL OBJETIVO DE IDENTIFICAR, DE FORMA CONFIABLE Y ESTABLE, LOS PUNTOS ANATÓMICOS NECESARIOS PARA EL ANÁLISIS DE LA EJECUCIÓN TERAPÉUTICA

**Criterios de Aceptación**

CA01
Dado que la cámara esté habilitada,
Cuando el paciente se ubique frente a ella,
Entonces el sistema detecta el movimiento corporal mediante estimación de pose.

CA02
Dado que realice el monitoreo,
Cuando el paciente ejecute movimientos,
Entonces el sistema identifica los landmarks correspondientes.

CA03
Dado que detecte más de una persona u objetos en el encuadre,
Cuando procese la imagen,
Entonces el sistema prioriza el cuerpo del paciente.

CA04
Dado que el monitoreo esté activo,
Cuando el sistema reciba los landmarks,
Entonces los procesa para su análisis posterior.

CA05
Dado que el sistema reciba los landmarks de la acción actual,
Cuando los prepare para el análisis,
Entonces aplica un suavizado que estabiliza su posición entre cuadros.

CA06
Dado que el sistema obtenga los landmarks sobre la imagen analizada,
Cuando los presente sobre la cámara en pantalla,
Entonces los ubica sobre el cuerpo del paciente aun con la cámara frontal espejada.

CA07
Dado que algún landmark requerido por la acción actual tenga una visibilidad menor a 0,5,
Cuando el sistema evalúe el cuadro,
Entonces lo marca como no confiable.

CA08
Dado que el sistema marque un cuadro como no confiable,
Cuando procese la acción actual,
Entonces no avanza ni penaliza la acción y solicita al paciente reubicarse.

> Nota: los CA de estabilidad/consistencia ante condiciones adversas del entorno se concentran en RNF05; esta HU se enfoca en la detección.

#### HU08 — Procesar movimiento corporal

**Declaración de la Historia de Usuario**

**Rol**
YO COMO SISTEMA

**Deseo**
QUIERO PROCESAR LOS PUNTOS ANATÓMICOS DETECTADOS Y CALCULAR LOS ÁNGULOS ARTICULARES DE CADA ACCIÓN

**Propósito**
CON EL OBJETIVO DE OBTENER MÉTRICAS BIOMECÁNICAS OBJETIVAS DE LA EJECUCIÓN DEL PACIENTE

**Criterios de Aceptación**

CA01
Dado que detecte los landmarks,
Cuando el paciente ejecute el ejercicio,
Entonces el sistema calcula los ángulos articulares correspondientes.

CA02
Dado que calcule ángulos articulares,
Cuando compare con el rango esperado,
Entonces determina si la ejecución está dentro del margen aceptable.

CA03
Dado que ocurra una oclusión temporal de un punto anatómico,
Cuando el sistema procese el movimiento,
Entonces continúa el procesamiento sin interrumpirse.

CA04
Dado que finalice la sesión,
Cuando el sistema consolide el procesamiento,
Entonces genera un resumen de ángulos y desviaciones posturales.

CA05
Dado que el sistema procese la acción actual,
Cuando calcule el ángulo,
Entonces lo obtiene entre el segmento pivote–distal y la línea pivote–referencia.

CA06
Dado que el sistema conozca el ángulo actual, el ángulo de inicio y el ángulo meta de la acción,
Cuando calcule el progreso,
Entonces lo expresa entre 0 y 1, tanto si la meta es mayor como si es menor que el inicio.

CA07
Dado que el sistema conozca el ángulo actual y el ángulo meta,
Cuando evalúe la acción,
Entonces determina que el paciente está en zona cuando la diferencia es menor o igual a la tolerancia.

CA08
Dado que el sistema conozca la distancia a la meta,
Cuando evalúe el desempeño,
Entonces lo clasifica en tres niveles (verde, ámbar y rojo) según umbrales configurables.

CA09
Dado que la acción se defina para un lado del cuerpo,
Cuando el sistema calcule el ángulo,
Entonces considera el lado medido y el espejo de la cámara frontal para el sentido de giro.

CA10
Dado que el sistema calcule la posición de la meta,
Cuando prepare la guía de la acción,
Entonces obtiene el punto donde debe ubicarse el extremo del segmento al alcanzar el ángulo meta, anclado al pivote.

#### HU09 — Analizar ejecución terapéutica

**Declaración de la Historia de Usuario**

**Rol**
YO COMO SISTEMA

**Deseo**
QUIERO ANALIZAR LA EJECUCIÓN TERAPÉUTICA DEL PACIENTE ACCIÓN POR ACCIÓN

**Propósito**
CON EL OBJETIVO DE GENERAR UN RESULTADO REFERENCIAL SOBRE LA CALIDAD DEL EJERCICIO REALIZADO

**Criterios de Aceptación**

CA01
Dado que reciba los ángulos calculados,
Cuando analice la ejecución,
Entonces compara la ejecución con el patrón de referencia del ejercicio.

CA02
Dado que detecte una desviación relevante,
Cuando analice la ejecución,
Entonces clasifica el tipo de error entre rango incompleto, desviación angular y movimiento simultáneo.

CA03
Dado que el análisis concluya,
Cuando el sistema consolide el resultado,
Entonces genera un resultado consolidado con el porcentaje de acierto y la desviación promedio.

CA04
Dado que concluya una repetición,
Cuando el sistema consolide su análisis,
Entonces genera por cada acción si fue completada, el ángulo máximo alcanzado, el tiempo empleado y la desviación angular.

> Nota: CA01 a CA03 se cumplen desde Sprint 3 (`MedicionArticulacion.tipoDeError`, `medirArticulacion`, `porcentajeEjecucion`/`desviacionPromedio`). CA04 es la ampliación de la guía visual en vivo y está pendiente de implementación.

---

### ÉPICA 03: Generar retroalimentación terapéutica

#### HU10 — Generar retroalimentación inmediata

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO RECIBIR RETROALIMENTACIÓN VISUAL, SONORA Y HÁPTICA INMEDIATA, CON UNA GUÍA PINTADA SOBRE LA CÁMARA, DURANTE LOS EJERCICIOS

**Propósito**
CON EL OBJETIVO DE CORREGIR MI EJECUCIÓN TERAPÉUTICA MIENTRAS LA REALIZO

**Criterios de Aceptación**

CA01
Dado que el paciente ejecute una acción,
Cuando inicie la acción,
Entonces el sistema dibuja sobre la cámara, anclado a su cuerpo, el recorrido que debe seguir el segmento corporal desde la posición inicial hasta la meta.

CA02
Dado que el paciente avance en la acción,
Cuando el segmento recorra parte del trayecto,
Entonces el sistema rellena con el color de estado la parte del recorrido ya realizada.

CA03
Dado que la acción tenga una meta y una tolerancia,
Cuando el sistema muestre la guía,
Entonces resalta la zona meta como un sector translúcido de meta más o menos tolerancia.

CA04
Dado que la acción tenga una meta,
Cuando el sistema muestre la guía,
Entonces dibuja un segmento fantasma translúcido en la posición final con un marcador en el extremo.

CA05
Dado que el sistema detecte el segmento corporal del paciente,
Cuando el paciente se mueva,
Entonces lo dibuja con el color del desempeño: rojo, ámbar o verde.

CA06
Dado que el paciente ejecute el ejercicio,
Cuando visualice la pantalla,
Entonces el sistema muestra la repetición actual sobre el total, la acción actual sobre el total y su nombre.

CA07
Dado que el sistema detecte una desviación,
Cuando el paciente continúe la acción,
Entonces muestra un mensaje de corrección.

CA08
Dado que la acción exija mantener la posición en la zona meta,
Cuando el paciente permanezca en ella,
Entonces el sistema muestra los segundos restantes de mantenimiento.

CA09
Dado que el paciente complete una acción,
Cuando el sistema pase a la siguiente,
Entonces cambia automáticamente la guía al recorrido de la nueva acción.

CA10
Dado que el sistema marque un cuadro como no confiable,
Cuando el paciente esté fuera de posición,
Entonces oculta la guía y muestra un aviso para reubicarse según la vista de cámara del ejercicio.

CA11
Dado que el sistema procese el movimiento,
Cuando actualice la retroalimentación,
Entonces su tiempo de respuesta no supera los 500 ms.

CA12
Dado que el paciente continúe ejecutando el ejercicio,
Cuando se modifique su movimiento,
Entonces la retroalimentación se actualiza de forma continua.

CA13
Dado que finalice el ejercicio,
Cuando el paciente concluya o finalice la sesión,
Entonces el sistema detiene la retroalimentación inmediata.

CA14
Dado que el sistema detecte una desviación,
Cuando genere la indicación sonora,
Entonces reproduce por voz una frase plantilla según el tipo de error, en el dispositivo y sin conexión.

CA15
Dado que el sistema ya haya reproducido un mensaje por voz,
Cuando deba repetir el mismo mensaje,
Entonces no lo reproduce otra vez antes de 3 segundos.

CA16
Dado que el paciente complete una acción,
Cuando el sistema la registre,
Entonces emite una vibración corta.

CA17
Dado que el paciente complete una repetición,
Cuando el sistema la cuente,
Entonces emite una vibración más larga.

CA18
Dado que el paciente defina sus preferencias,
Cuando active o desactive la voz o la vibración,
Entonces el sistema respeta esa elección durante la ejecución.

CA19
Dado que un mismo tipo de error se repita varias veces durante la ejecución,
Cuando el sistema consolide el resultado,
Entonces lo acumula como parte del resultado de la sesión para que el fisioterapeuta lo vea como evidencia.

> **Decisión de diseño que cambia (pendiente de confirmación explícita del usuario):** en Sprint 4 se acordó que la retroalimentación visual fuera un ícono mínimo (check/alerta) sin texto ni esqueleto sobre la cámara, y el Design System confirmó mantener el monitoreo minimalista. La guía visual en vivo (CA01 a CA10) reemplaza esa decisión: dibuja el recorrido sobre la cámara y muestra un HUD con texto. Hasta confirmarlo, se mantiene vigente el diseño anterior.

#### HU11 — Visualizar resultados y porcentaje de ejecución

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO VISUALIZAR LOS RESULTADOS Y EL PORCENTAJE DE EJECUCIÓN DE MI SESIÓN

**Propósito**
CON EL OBJETIVO DE CONOCER MI DESEMPEÑO REFERENCIAL EN LOS EJERCICIOS REALIZADOS

**Criterios de Aceptación**

CA01
Dado que el paciente finalice una sesión,
Cuando seleccione "Ver resultado",
Entonces el sistema muestra la pantalla de resultado del ejercicio.

CA02
Dado que consulte el resultado de una sesión,
Cuando visualice el resumen,
Entonces el sistema muestra las repeticiones completadas sobre las asignadas.

CA03
Dado que consulte el resultado de una sesión,
Cuando visualice el resumen,
Entonces el sistema muestra el porcentaje promedio de ejecución.

CA04
Dado que consulte el resultado de una sesión,
Cuando visualice el detalle,
Entonces el sistema lista todas las repeticiones con su porcentaje de ejecución.

CA05
Dado que una repetición alcance el 75 % de ejecución o más,
Cuando visualice el detalle,
Entonces el sistema la destaca en verde.

CA06
Dado que una repetición no alcance el 75 % de ejecución,
Cuando visualice el detalle,
Entonces el sistema la destaca en ámbar.

CA07
Dado que la sesión se haya finalizado antes de completar las repeticiones asignadas,
Cuando consulte su resultado,
Entonces el sistema muestra las repeticiones realmente completadas sobre el total asignado.

CA08
Dado que consulte el resultado de una sesión,
Cuando seleccione "Ir a mi progreso",
Entonces el sistema lo lleva a la pantalla "Mis resultados".

CA09
Dado que consulte el resultado de una sesión,
Cuando lea la aclaración del cálculo,
Entonces el sistema indica que el porcentaje se calcula según las correcciones detectadas por MediaPipe y es referencial.

> **Bug/brecha real encontrada y corregida (2026-09-15):** al completar
> una sesión, `EjecutarSesionScreen` solo mostraba un mensaje genérico
> "Sesión completada" + botón "Volver" — el paciente nunca veía el % de
> ejecución ni nada del resultado ahí mismo (CA01 no se cumplía en la
> práctica); para verlo tenía que ir manualmente a "Mi progreso" y
> volver a abrir esa sesión desde el historial. Se agregó
> `onSesionCompletada(sesionId)` (`EjecutarSesionScreen.kt`) que navega
> de inmediato a `ResultadoSesionScreen` con `soloLectura=false` en
> cuanto la sesión termina. La misma pantalla, cuando se abre desde el
> historial (HU13, siempre `soloLectura=true` por defecto en la ruta),
> muestra en cambio un aviso "Sesión finalizada — vista de solo lectura"
> — nueva ruta `Rutas.RESULTADO_SESION` con query param opcional
> `soloLectura` (`ARG_SOLO_LECTURA`, default `true`).
>
> **Segundo bug encontrado y corregido en el mismo cambio (2026-09-15):**
> el botón "Iniciar sesión" de `EjecutarSesionScreen` (pantalla previa al
> monitoreo) no respondía al toque en el emulador — confirmado con
> `uiautomator`: el botón se dibujaba superpuesto (`Box` + `align(BottomCenter)`)
> encima de `CamaraConDeteccionPose`, cuyo `PreviewView` (AndroidView/CameraX)
> intercepta el toque antes de que llegue a un `clickable` de Compose dibujado
> encima — mismo problema ya visto antes con `PlayerView` en `TarjetaEjercicio`
> (ver el rediseño de esa tarjeta más arriba). Se resolvió reestructurando ese
> estado a un `Column` con la cámara y el botón en zonas separadas sin
> superposición (`Box.fillMaxHeight(0.8f)` para la cámara + un `Box` propio
> debajo para el botón, en vez de `weight(1f)` — con `weight(1f)` la cámara
> igual reclamaba casi toda la altura y el botón quedaba comprimido a unos
> pocos píxeles al fondo de la pantalla, fuera del área táctil visible).
> Confirmado con una ejecución completa end-to-end en el emulador (iniciar →
> cuenta regresiva → repeticiones → finalizar → "Ver resultado").

---

### ÉPICA 04: Gestionar seguimiento terapéutico

> **Rediseño del flujo del paciente (2026-09-19, mockups `paciente_mockups.html`).**
> Barra lateral del paciente con 4 pestañas (Ejercicios / Resultados /
> Progreso / Perfil, `BarraLateralPaciente.kt`).
> - *Detalle del ejercicio* (HU04/HU05, `DetalleEjercicioAsignadoScreen`):
>   video, "Cómo realizar el ejercicio", tarjetas "Ángulo objetivo" (rango
>   personalizado de la sesión, HU03-CA08, o el del ejercicio) y
>   "Repeticiones", y bloque "Nota de tu fisioterapeuta" siempre que la
>   sesión tenga `notas` (antes solo con ángulo personalizado).
> - *Resultado al terminar* (HU11, `ResultadoSesionScreen`): "¡Ejercicio
>   completado!", tarjetas Repeticiones/Promedio, TODAS las repeticiones con
>   su % (verde >= 75%, ámbar si no) y "Ir a mi progreso" -> "Mis
>   resultados". Se eliminó el modo "solo lectura" y la ruta con
>   `soloLectura`.
> - *Mis resultados* (HU13/HU16, `MisResultadosScreen`): solo la lista de
>   sesiones realizadas, una tarjeta por sesión (`TarjetaConIcono` con
>   badge de %). Al tocar una se abre `DetalleResultadoScreen` (ruta
>   `paciente/resultados/{sesionId}`, otra pantalla): tarjetas
>   Repeticiones/Promedio, detalle por repetición con toggle lista/gráfico
>   y la recomendación del fisioterapeuta con su fecha (subcolección
>   `observaciones` de la sesión; solo aparece lo ya guardado).
> - *Mi progreso* (HU12, `MiProgresoScreen`): "Sesiones realizadas",
>   "Promedio general" y gráfico de puntos por sesión. Reemplaza al antiguo
>   historial (`HistorialSesionesScreen`, filtros y racha eliminados).
> - Nuevos componentes de Design System: `TarjetaCifra`; `GraficoLinea`
>   ahora alinea sus puntos con las etiquetas del eje X y dibuja también una
>   sola sesión.

#### HU12 — Visualizar progreso y evolución terapéutica
**Rol:** Fisioterapeuta
**Deseo:** Visualizar el progreso y la evolución terapéutica del paciente
**Propósito:** Analizar su evolución a lo largo de las sesiones realizadas.
- CA01: Dado que consulte el progreso de un paciente, entonces el sistema muestra la evolución de sus resultados a lo largo de las sesiones.
- CA02: Dado que existan sesiones registradas, entonces el sistema presenta resultados históricos de forma comparativa.
- CA03: Dado que seleccione un rango de fechas, entonces el sistema filtra los resultados mostrados.
- CA04: Dado que el paciente realice nuevas sesiones, entonces el sistema incorpora los resultados recientes a la evolución.

> **Implementado en Sprint 5** en `PacienteDetalleScreen` (fisio, no en
> una pantalla nueva): la tarjeta "Progreso" ahora muestra, además del %
> de sesiones completadas ya existente, el **promedio de calidad de
> ejecución** (`porcentajeEjecucion` promedio de las sesiones
> completadas — distinto del % de cumplimiento, que solo cuenta
> completadas/asignadas). "Evolución/comparativa" (CA02) se cubre con la
> lista de sesiones de abajo, cada una con su propio % — no se construyó
> un gráfico de líneas nuevo, dado el alcance del sprint. Filtro por
> período (Todo/Última semana/Último mes, CA03) afecta tanto el resumen
> como la lista. CA04 es gratis por ser un listener en vivo, igual que el
> resto de la app. De regalo, el paciente también ve un resumen análogo
> de su propio progreso (% general, sesiones completadas, racha de días
> consecutivos) en su propia pantalla de historial (`HistorialSesionesScreen`)
> — no es parte de esta HU (que es del fisioterapeuta), pero es una
> mejora razonable sobre HU13 que ya existía, no le hace daño a nadie.
>
> **Ampliación posterior (2026-09-15):** ese regalo del paciente ganó su
> propio gráfico de líneas — "Evolución de precisión" en
> `HistorialSesionesScreen`, con el `%` de cada sesión completada en
> orden cronológico (respeta el filtro de período vigente), vía el nuevo
> componente `GraficoLinea` (`core/designsystem/Graficos.kt`). Desde 2026-09-19 el
> componente compartido `EvolucionPrecisionPorSesion` (línea + barras de
> precisión por sesión, `core/designsystem/Graficos.kt`) se usa tanto en
> "Mi progreso" (paciente) como en el detalle del paciente del
> fisioterapeuta (`PacienteDetalleScreen`).
>
> **Ampliación posterior (corrección, no en la versión original):**
> "Progreso por ejercicio" — una tarjeta nueva debajo del resumen general
> que agrupa las sesiones completadas por ejercicio y muestra, para cada
> uno, una barra de progreso (`LinearProgressIndicator`) con el promedio
> de `porcentajeEjecucion` de ese ejercicio y cuántas sesiones lo
> completaron. Esto sí es el equivalente con barras de la pantalla 10 del
> mockup (antes solo se cubría con la lista de sesiones individuales, sin
> agrupar por ejercicio). Respeta el mismo filtro de período que el resto
> de la pantalla. También se agregó un acceso directo: cada tarjeta de
> sesión completada tiene un ícono "Registrar recomendación" que navega
> directo a `RegistrarRecomendacionScreen` (HU15) sin pasar primero por
> el detalle de la sesión (HU18-CA02) — solo un atajo de navegación, la
> lógica de HU15 no cambió.

#### HU13 — Consultar historial terapéutico

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO CONSULTAR MIS RESULTADOS Y MI PROGRESO

**Propósito**
CON EL OBJETIVO DE VISUALIZAR LAS SESIONES TERAPÉUTICAS REALIZADAS Y MI EVOLUCIÓN

**Criterios de Aceptación**

CA01
Dado que el paciente haya realizado sesiones,
Cuando seleccione "Resultados",
Entonces el sistema muestra las sesiones realizadas.

CA02
Dado que existan múltiples sesiones,
Cuando consulte sus resultados,
Entonces el sistema las ordena de la más reciente a la más antigua.

CA03
Dado que consulte sus resultados,
Cuando visualice la lista de sesiones,
Entonces el sistema muestra cada sesión en una tarjeta con el ejercicio, la fecha y el porcentaje de ejecución.

CA04
Dado que consulte sus resultados,
Cuando seleccione una sesión,
Entonces el sistema abre en otra pantalla el detalle de esa sesión con sus repeticiones y su promedio.

CA05
Dado que visualice el detalle de una sesión,
Cuando seleccione "Mostrar gráfico",
Entonces el sistema muestra el porcentaje de cada repetición en un gráfico de barras.

CA06
Dado que no haya completado ninguna sesión,
Cuando consulte sus resultados,
Entonces el sistema muestra un mensaje de ausencia de registros.

CA07
Dado que el paciente haya realizado sesiones,
Cuando seleccione "Progreso",
Entonces el sistema muestra el número de sesiones realizadas y el promedio general.

CA08
Dado que consulte su progreso,
Cuando visualice la evolución,
Entonces el sistema muestra un gráfico con el porcentaje de precisión de cada sesión según su número.


#### HU14 — Monitorear cumplimiento terapéutico
**Rol:** Fisioterapeuta
**Deseo:** Monitorear el cumplimiento terapéutico del paciente
**Propósito:** Verificar la realización de las sesiones asignadas.
- CA01: Dado que consulte el cumplimiento de un paciente, entonces el sistema muestra sesiones realizadas y pendientes.
- CA02: Dado que el paciente finalice una sesión, entonces el sistema actualiza el estado de cumplimiento.
- CA03: Dado que el sistema calcule sesiones completadas vs. asignadas, entonces muestra el **% de adherencia terapéutica**.
- CA04: Dado que existan sesiones pendientes, entonces el sistema distingue visualmente pendientes de completadas.

> Nota (Sprint 5): esta HU **ya se cumple desde Sprint 2/3**, sin código
> nuevo — `PacienteDetalleScreen` (detalle de un paciente puntual) ya
> muestra "TarjetaProgreso" con el % de sesiones completadas sobre el
> total (CA03), la lista completa de sesiones con su `EstadoPill`
> verde/ámbar (CA01/CA04), y todo vía un listener de Firestore en vivo
> que se actualiza solo al completarse una sesión (CA02). Se deja
> constancia aquí para no repetir trabajo ya hecho.

---

### ÉPICA 05: Gestionar comunicación terapéutica

#### HU15 — Registrar y gestionar recomendaciones terapéuticas
**Rol:** Fisioterapeuta
**Deseo:** Registrar y gestionar recomendaciones terapéuticas
**Propósito:** Brindar y administrar observaciones relacionadas con las sesiones realizadas.
- CA01: Dado que seleccione una sesión realizada, entonces el sistema permite registrar una recomendación.
- CA02: Dado que complete la información requerida, entonces el sistema almacena la recomendación.
- CA03: Dado que desee actualizar una recomendación, entonces el sistema guarda los cambios.
- CA04: Dado que desee eliminar una recomendación, cuando confirme, entonces el sistema la elimina.
- CA05 *(ampliación acordada, 2026-09-16, Etapa 4)*: Dado que esté viendo
  el resultado de una sesión (`FisioResultadoSesionScreen`), entonces el
  sistema ofrece un campo de recomendación embebido ahí mismo (mockup
  Idea 9) para registrar una nueva sin navegar a otra pantalla — llama al
  mismo `RecomendacionRepository.crear()` que ya usaba
  `RegistrarRecomendacionScreen`. Ese campo es solo para CREAR rápido;
  editar, eliminar o ver el historial completo de recomendaciones de la
  sesión sigue en `RegistrarRecomendacionScreen` (botón "Ver todas las
  recomendaciones"), que no se eliminó ni se duplicó.

#### HU16 — Consultar recomendaciones terapéuticas

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO CONSULTAR LAS RECOMENDACIONES TERAPÉUTICAS REGISTRADAS

**Propósito**
CON EL OBJETIVO DE VISUALIZAR LAS OBSERVACIONES REALIZADAS POR EL FISIOTERAPEUTA

**Criterios de Aceptación**

CA01
Dado que el fisioterapeuta haya registrado una recomendación para una sesión,
Cuando el paciente seleccione esa sesión en "Resultados",
Entonces el sistema muestra la recomendación con su fecha.

CA02
Dado que existan varias recomendaciones para una sesión,
Cuando el paciente consulte el detalle de la sesión,
Entonces el sistema las muestra todas.

CA03
Dado que el fisioterapeuta registre una nueva recomendación,
Cuando el paciente consulte la sesión,
Entonces el sistema muestra la información actualizada.

CA04
Dado que no existan recomendaciones para una sesión,
Cuando el paciente consulte su detalle,
Entonces el sistema indica que no hay recomendaciones disponibles.


---

### ÉPICA 06: Gestionar almacenamiento terapéutico

#### HU17 — Almacenar información terapéutica

**Declaración de la Historia de Usuario**

**Rol**
YO COMO SISTEMA

**Deseo**
QUIERO ALMACENAR LA INFORMACIÓN GENERADA DURANTE LAS SESIONES TERAPÉUTICAS

**Propósito**
CON EL OBJETIVO DE MANTENER REGISTRADOS LOS RESULTADOS DE FORMA SEGURA Y RESPETANDO LA PRIVACIDAD DEL PACIENTE

**Criterios de Aceptación**

CA01
Dado que finalice una sesión,
Cuando el sistema la registre,
Entonces almacena resultados, métricas y fecha.

CA02
Dado que el sistema procese el movimiento corporal,
Cuando registre la sesión,
Entonces almacena datos numéricos como ángulos y métricas.

CA03
Dado que finalice una sesión,
Cuando el sistema la registre,
Entonces almacena el video de la sesión, sin audio, para que el fisioterapeuta lo revise.

CA04
Dado que se registre una nueva sesión,
Cuando el sistema actualice los datos del paciente,
Entonces no sobrescribe las sesiones anteriores.

CA05
Dado que el paciente cierre una repetición,
Cuando el sistema la registre,
Entonces almacena el resultado de cada acción de esa repetición con su ángulo máximo, tiempo y desviación.

#### HU18 — Gestionar sesiones y resultados terapéuticos registrados

**Declaración de la Historia de Usuario**

**Rol**
YO COMO FISIOTERAPEUTA

**Deseo**
QUIERO GESTIONAR LAS SESIONES Y RESULTADOS TERAPÉUTICOS REGISTRADOS

**Propósito**
CON EL OBJETIVO DE CONSULTAR LA INFORMACIÓN ASOCIADA A LAS SESIONES Y RESULTADOS OBTENIDOS POR CADA PACIENTE

**Criterios de Aceptación**

CA01
Dado que el fisioterapeuta acceda al sistema,
Cuando seleccione "Resultados",
Entonces el sistema muestra la lista de sus pacientes asignados.

CA02
Dado que consulte la lista de pacientes,
Cuando seleccione un paciente,
Entonces el sistema muestra todas sus sesiones con su estado: completada, incompleta o por hacer.

CA03
Dado que consulte las sesiones de un paciente,
Cuando aplique un filtro por período,
Entonces el sistema muestra solo las sesiones que cumplen el criterio.

CA04
Dado que consulte las sesiones de un paciente,
Cuando aplique un filtro por ejercicio,
Entonces el sistema muestra solo las sesiones de ese ejercicio.

CA05
Dado que consulte las sesiones de un paciente,
Cuando seleccione una sesión,
Entonces el sistema muestra las repeticiones completas y el promedio de ejecución.

CA06
Dado que acceda al detalle de una sesión,
Cuando visualice el detalle por repetición,
Entonces el sistema muestra el porcentaje de ejecución de cada repetición en lista o en gráfico.

CA07
Dado que una repetición tenga un error,
Cuando visualice el detalle por repetición,
Entonces el sistema muestra el segundo del error, la articulación, el ángulo detectado y el esperado.

CA08
Dado que existan más repeticiones que las mostradas,
Cuando seleccione "Ver más repeticiones",
Entonces el sistema muestra el listado completo de repeticiones.

CA09
Dado que el paciente haya grabado la sesión,
Cuando seleccione "Ver video de la sesión",
Entonces el sistema reproduce la grabación del ejercicio realizado.

CA10
Dado que acceda al detalle de una repetición,
Cuando visualice el desglose,
Entonces el sistema muestra qué acciones se completaron y el ángulo máximo alcanzado en cada una.

CA11
Dado que una acción no se haya completado,
Cuando el fisioterapeuta visualice el desglose de la repetición,
Entonces el sistema la distingue visualmente de las acciones completadas.

> **Nota de dependencia (resuelta en Sprint 4):** HU15 (registrar
> recomendaciones) requiere que el fisioterapeuta pueda ver el resultado de
> una sesión completada antes de recomendar algo — esa vista era HU18-CA02,
> planeada para Sprint 5. Igual que se hizo con HU11/HU13 (adelantadas a
> Sprint 3 por la misma razón de dependencia), en Sprint 4 se construyó una
> versión mínima de HU18-CA02 + CA04 junto con HU15: en
> `PacienteDetalleScreen` las tarjetas de sesión ahora son clickeables
> también cuando están completadas (antes solo las pendientes), y abren
> `FisioResultadoSesionScreen` (porcentaje, ángulos, detalle por
> repetición). Sigue pendiente para Sprint 5 el resto de HU18 (CA01/CA03:
> vista agregada de todas las sesiones con filtro por fecha/tipo de
> ejercicio) — esto solo cubre el detalle de una sesión puntual.

> **CA01/CA03 completados en Sprint 5:** nueva pantalla "Resultados"
> (`ResultadosScreen`, tercera pestaña de la barra lateral del
> fisioterapeuta) con la lista agregada de sesiones completadas de
> **todos** sus pacientes — via `collectionGroup("sesiones")` filtrado
> por `fisioterapeutaId` (el índice compuesto ya estaba declarado en
> `firestore.indexes.json` desde etapas tempranas del proyecto, solo
> hubo que desplegarlo). Filtro por ejercicio (dropdown) y por período
> (Todos/Última semana/Último mes) — CA03 pide "fecha o tipo de
> ejercicio", se cubre con ambos.
>
> **Bug real encontrado y corregido (2026-09-10):** el filtro de período
> comparaba contra `fechaAsignacion` en vez de `fechaEjecucion` —
> sesiones asignadas hace tiempo pero completadas recientemente quedaban
> excluidas de "Última semana"/"Último mes" aunque sí correspondían. Y el
> dropdown de ejercicio no incluía "Todos los ejercicios" como opción
> seleccionable, así que una vez elegido un ejercicio específico no había
> forma de volver a quitar ese filtro desde la UI (quedaba "atascado").
> Corregido en `ResultadosViewModel.kt`/`ResultadosScreen.kt`.

#### HU19 — Sincronizar información terapéutica
**Rol:** Sistema
**Deseo:** Sincronizar la información terapéutica generada de forma local
**Propósito:** Mantener actualizados los datos entre el dispositivo del paciente y el fisioterapeuta.
- CA01: Dado que el paciente realice una sesión sin conexión, cuando la conexión se restablezca, entonces el sistema sincroniza automáticamente la información.
- CA02: Dado que existan datos sincronizados recientemente, entonces el sistema muestra la información actualizada al fisioterapeuta.
- CA03: Dado que ocurra un conflicto entre datos locales y ya sincronizados, entonces el sistema prioriza la información más reciente sin duplicar.

> Nota (Sprint 5): esta HU **ya se cumple desde Sprint 1**, sin código
> nuevo — Firestore para Android trae persistencia offline activada por
> defecto (`FirebaseModule.kt` usa `FirebaseFirestore.getInstance()` sin
> deshabilitarla): las escrituras hechas sin conexión (ej.
> `guardarResultado` al finalizar una sesión) quedan en una cola local y
> se sincronizan solas al reconectar (CA01), sin intervención de la app.
> Como todas las pantallas usan listeners en vivo (`addSnapshotListener`/
> `Flow`), el fisioterapeuta ve los datos actualizados apenas sincronizan
> (CA02). No hay ediciones concurrentes reales sobre un mismo documento
> en este modelo (una sesión la escribe o el fisio al asignarla o el
> paciente al ejecutarla, nunca ambos a la vez), así que el
> "last write wins" por defecto de Firestore ya cubre CA03 sin lógica de
> resolución de conflictos a medida.

---

### ÉPICA 07: Administrar cuentas del sistema

*(Ampliación acordada, no en la versión original de la tesis: no existía un
rol Administrador ni una forma de crear cuentas dentro de la propia app —
antes solo era posible mediante el script `crear-usuario.ts`.)*

#### HU20 — Gestionar cuentas de pacientes
**Rol:** Administrador
**Deseo:** Registrar, editar y eliminar cuentas de pacientes, y asignarles su fisioterapeuta correspondiente
**Propósito:** Administrar a los pacientes atendidos por la clínica sin depender de herramientas de línea de comandos.
- CA01: Dado que el administrador acceda al sistema, cuando seleccione "Pacientes" en el panel de administración, entonces el sistema muestra la lista de pacientes registrados.
- CA02 *(actualizado — revisión del modelo de datos: se agregaron género y
  número de contacto)*: Dado que desee registrar un paciente, cuando
  complete nombre, correo, contraseña, DNI, edad, género, número de
  contacto y uno o más diagnósticos (del mismo catálogo cerrado de
  HU01-CA06), entonces el sistema crea la cuenta con esos datos y la
  muestra en la lista. La contraseña nunca se muestra en texto plano en el
  formulario — solo es legible temporalmente si el administrador presiona
  el ícono de ojo (mismo control en el formulario de fisioterapeuta).
- CA03 *(actualizado)*: Dado que desee actualizar la información de un paciente, cuando modifique los datos correspondientes (incluidos DNI, edad, género, contacto y diagnóstico(s)), entonces el sistema guarda los cambios.
- CA04: Dado que desee eliminar la cuenta de un paciente, cuando confirme la eliminación, entonces el sistema la elimina.
- CA05: Dado que un paciente no tenga fisioterapeuta asignado, cuando el administrador seleccione uno desde la lista, entonces el sistema se lo asigna y la opción de asignar deja de estar disponible para ese paciente.
- CA06 *(ampliación acordada, no en la versión original)*: Dado que consulte
  la lista de pacientes, cuando un paciente ya tenga fisioterapeuta
  asignado, entonces el sistema muestra el nombre de ese fisioterapeuta en
  su tarjeta (en vez del botón de asignar). De forma simétrica (relación
  de uno a muchos: un fisioterapeuta puede tener varios pacientes), la
  lista de fisioterapeutas (HU21) muestra en la tarjeta de cada uno cuántos
  pacientes tiene asignados actualmente.
- CA07 *(ampliación acordada, no en la versión original)*: Dado que registre
  o edite un paciente, cuando indique el lado del cuerpo afectado (Derecho /
  Izquierdo / Ambos, un único valor para todo el paciente, no por
  diagnóstico), entonces el sistema lo guarda junto al resto de su
  información clínica. Este dato lo define siempre el administrador — el
  sistema nunca lo infiere ni lo decide por su cuenta (coherente con que la
  app es de seguimiento, no de diagnóstico) — y lo usa el monitoreo (HU07/
  HU08) para medir la articulación del lado real del paciente en vez de
  asumir siempre el derecho con el que se definió el ejercicio. Con "Ambos"
  (ejercicios pensados para alternar un lado a la vez, ej. HOM-01), el
  sistema mide los dos lados en cada frame y evalúa el que esté activo (el
  de mayor ángulo); si el paciente mueve los dos a la vez en vez de
  alternar, se detecta como error de coordinación ("Movimiento
  simultáneo"). Sin dato, se mide el lado derecho por defecto.
- CA08 *(ampliación acordada, no en la versión original — recomendación
  del modelo E-R-SANNA)*: Dado que consulte la lista de pacientes, cuando
  seleccione "Desactivar" en el menú de un paciente y confirme en el
  diálogo (mismo `DialogoConfirmacion` que "Eliminar"), entonces el
  sistema marca su cuenta como inactiva (`activo = false`) sin
  eliminarla — el paciente no puede volver a iniciar sesión mientras esté
  inactiva, pero su historial de sesiones y resultados se conserva. El
  administrador puede revertirlo seleccionando "Activar" (con el mismo
  paso de confirmación). Esta acción convive con "Eliminar" (CA04): son
  dos opciones distintas, no un reemplazo — eliminar borra el registro;
  desactivar solo bloquea el acceso de forma reversible. Toda cuenta
  nueva se crea activa por defecto (`crearCuenta`, HU20-CA02/HU21-CA02).

#### HU21 — Gestionar cuentas de fisioterapeutas
**Rol:** Administrador
**Deseo:** Registrar, editar y eliminar cuentas de fisioterapeutas
**Propósito:** Administrar al personal que atiende a los pacientes.
- CA01: Dado que el administrador acceda al sistema, cuando seleccione "Fisioterapeutas" en el panel de administración, entonces el sistema muestra la lista de fisioterapeutas registrados.
- CA02 *(actualizado — revisión del modelo de datos: se agregaron edad,
  género, número de contacto, especialidad y número de colegiatura)*: Dado
  que desee registrar un fisioterapeuta, cuando complete nombre, correo,
  contraseña, edad, género y número de contacto, y opcionalmente
  especialidad y número de colegiatura, entonces el sistema crea la cuenta
  y la muestra en la lista.
- CA03 *(actualizado)*: Dado que desee actualizar la información de un
  fisioterapeuta, cuando modifique los datos correspondientes (incluidos
  edad, género, contacto, especialidad y número de colegiatura), entonces
  el sistema guarda los cambios.
- CA04: Dado que desee eliminar la cuenta de un fisioterapeuta, cuando confirme la eliminación, entonces el sistema la elimina.
- CA05 *(ampliación acordada, no en la versión original — recomendación
  del modelo E-R-SANNA, simétrica a HU20-CA08)*: Dado que consulte la
  lista de fisioterapeutas, cuando seleccione "Desactivar" en el menú de
  uno de ellos y confirme en el diálogo (mismo `DialogoConfirmacion` que
  "Eliminar"), entonces el sistema marca su cuenta como inactiva
  (`activo = false`) sin eliminarla — no puede volver a iniciar sesión
  mientras esté inactiva, pero sus datos (ejercicios creados, sesiones
  asignadas, recomendaciones) se conservan. El administrador puede
  revertirlo seleccionando "Activar". Convive con "Eliminar" (CA04), no
  la reemplaza.

> **Etapa 2A — Dashboard del Admin (ampliación acordada, 2026-09-16, no
> es una HU del backlog original, es refinamiento técnico sobre esta
> Épica 07; rediseñado el 2026-09-18).** Pestaña "Dashboard" en la barra
> lateral del administrador (`AdminDashboardScreen.kt`, primera pestaña).
> Muestra la lista de pacientes (mismo estilo que "Pacientes"); al
> seleccionar uno abre `AdminPacienteDashboardScreen`: tarjetas "Sesiones
> ejecutadas" y "Precisión prom." (promedio de los `porcentajeEjecucion`
> de sus sesiones completadas) y "Detalle por sesión" en lista (ejercicio,
> fecha, % correctas, % completado) o, con "Mostrar gráfico", tendencia de
> precisión (línea, eje X = número de sesión, eje Y = %) y % completado por
> sesión (barras). Todo sale de las sesiones reales del paciente en
> Firestore (`sesiones`, filtradas por `pacienteId`); las sesiones aún por
> hacer no entran en el promedio. Reutiliza `AdminRepository`/
> `SesionRepository` y requiere que el administrador pueda leer `sesiones`
> (`esAdmin()` en `firestore.rules`). Reemplaza al dashboard global de
> estadísticas (adherencia por fisioterapeuta) de la primera versión.

*(Ampliación acordada — HU22/HU23: hasta ahora esta épica solo cubría al
Administrador gestionando cuentas ajenas; se extiende con el mismo
espíritu ("gestionar cuentas") a que cada paciente/fisioterapeuta
gestione su propia cuenta — expone en una pantalla propia los atributos
que existen en `Usuario` (sección 5/6), respetando exactamente quién
puede editar cada uno según las reglas ya vigentes. Nota: al momento de
esta ampliación el modelo `Usuario` no tenía género, contacto,
especialidad ni colegiatura — esos campos se agregaron después, en la
actualización del modelo de datos documentada en HU20/HU21/HU22/HU23.)*

*Decisión de navegación asociada (confirmada): "Cerrar sesión" deja de
mostrarse como ícono suelto en la barra superior de cada pantalla.
- Paciente y fisioterapeuta: se centraliza únicamente en la nueva
  pantalla de Perfil (HU22-CA04/CA05/CA06, HU23-CA04/CA05/CA06) — se
  retira el ícono de cerrar sesión de `PacientesListScreen` (HU01) y de
  `EjerciciosAsignadosScreen` (HU04).
- Administrador (sin HU de Perfil propia — no está en el alcance de
  HU22/HU23): en vez de un ícono en la barra superior, "Cerrar sesión"
  pasa a ser una opción más dentro de la barra de navegación lateral
  (`ScaffoldConBarraLateral`/`ItemBarraLateral`), junto a "Pacientes" y
  "Fisioterapeutas". Al seleccionarla, el sistema muestra un diálogo de
  confirmación ("¿Seguro que deseas cerrar sesión?") antes de finalizar
  la sesión — mismo componente `DialogoConfirmacion` del Design System
  que ya se usa para confirmar eliminaciones.
- Por consistencia de interacción, HU22-CA04/CA05 y HU23-CA04/CA05
  (paciente y fisioterapeuta) aplican ese mismo diálogo de confirmación
  antes de cerrar sesión.*

#### HU22 — Consultar y editar mi perfil (Paciente)

**Declaración de la Historia de Usuario**

**Rol**
YO COMO PACIENTE

**Deseo**
QUIERO CONSULTAR LA INFORMACIÓN DE MI PROPIA CUENTA Y EDITAR LOS DATOS QUE ME CORRESPONDAN

**Propósito**
CON EL OBJETIVO DE MANTENER MI INFORMACIÓN DE CONTACTO ACTUALIZADA Y TENER UN ÚNICO LUGAR PARA GESTIONAR MI CUENTA, INCLUIDO EL CIERRE DE SESIÓN

**Criterios de Aceptación**

CA01 *(actualizado — revisión del modelo de datos: se agregaron género y número de contacto)*
Dado que el paciente acceda al sistema,
Cuando seleccione "Perfil",
Entonces el sistema muestra su nombre, correo electrónico, DNI, edad, género, número de contacto, diagnóstico(s) registrado(s) y el fisioterapeuta asignado.

CA02
Dado que el paciente consulte su perfil,
Cuando edite su nombre y confirme el cambio,
Entonces el sistema guarda el nuevo nombre.

CA03 *(actualizado)*
Dado que el paciente consulte su perfil,
Cuando visualice su correo electrónico, DNI, edad, género, número de contacto, diagnóstico(s) y fisioterapeuta asignado,
Entonces el sistema los muestra en modo solo lectura, sin controles de edición.

CA04
Dado que el paciente esté en la pantalla "Perfil",
Cuando seleccione "Cerrar sesión",
Entonces el sistema muestra un diálogo de confirmación.

CA05
Dado que el sistema muestre el diálogo de confirmación de cierre de sesión,
Cuando el paciente confirme la acción,
Entonces el sistema finaliza su sesión y lo redirige a la pantalla de Login.

CA06
Dado que el paciente navegue por cualquier otra pantalla de la aplicación,
Cuando busque la opción de cerrar sesión,
Entonces no la encuentra — "Perfil" es la única pantalla desde la que el paciente puede cerrar sesión.

#### HU23 — Consultar y editar mi perfil (Fisioterapeuta)

**Declaración de la Historia de Usuario**

**Rol**
YO COMO FISIOTERAPEUTA

**Deseo**
QUIERO CONSULTAR LA INFORMACIÓN DE MI PROPIA CUENTA Y EDITAR LOS DATOS QUE ME CORRESPONDAN

**Propósito**
CON EL OBJETIVO DE MANTENER MI INFORMACIÓN DE CONTACTO ACTUALIZADA Y TENER UN ÚNICO LUGAR PARA GESTIONAR MI CUENTA, INCLUIDO EL CIERRE DE SESIÓN

**Criterios de Aceptación**

CA01 *(actualizado — revisión del modelo de datos: se agregaron edad, género, número de contacto, especialidad y número de colegiatura)*
Dado que el fisioterapeuta acceda al sistema,
Cuando seleccione "Perfil",
Entonces el sistema muestra su nombre, correo electrónico, edad, género, número de contacto, especialidad y número de colegiatura.

CA02
Dado que el fisioterapeuta consulte su perfil,
Cuando edite su nombre y confirme el cambio,
Entonces el sistema guarda el nuevo nombre.

CA03 *(actualizado)*
Dado que el fisioterapeuta consulte su perfil,
Cuando visualice su correo electrónico, edad, género, número de contacto, especialidad y número de colegiatura,
Entonces el sistema los muestra en modo solo lectura, sin controles de edición.

CA04
Dado que el fisioterapeuta esté en la pantalla "Perfil",
Cuando seleccione "Cerrar sesión",
Entonces el sistema muestra un diálogo de confirmación.

CA05
Dado que el sistema muestre el diálogo de confirmación de cierre de sesión,
Cuando el fisioterapeuta confirme la acción,
Entonces el sistema finaliza su sesión y lo redirige a la pantalla de Login.

CA06
Dado que el fisioterapeuta navegue por cualquier otra pantalla de la aplicación,
Cuando busque la opción de cerrar sesión,
Entonces no la encuentra — "Perfil" es la única pantalla desde la que el fisioterapeuta puede cerrar sesión.

> **HU22/HU23 implementadas.** `feature/perfil/PerfilViewModel.kt` +
> `PerfilScreen.kt` (`PerfilPacienteScreen`/`PerfilFisioterapeutaScreen`,
> comparten el mismo `ContenidoPerfil` privado). `UsuarioRepository`
> ganó `actualizarNombre(uid, nombre)` — mismo patrón que
> `actualizarDiagnosticos`, sin cambios en Firestore Security Rules
> (`esDueno(uid)` ya permitía el `update`). Nuevo componente de Design
> System `CampoSoloLectura` (contraparte de solo lectura de
> `CampoTexto`). Rutas `paciente/perfil` y `fisioterapeuta/perfil`
> (`Rutas.PERFIL_PACIENTE`/`PERFIL_FISIOTERAPEUTA`) — una por rol, ya que
> cada pantalla de Perfil recibe parámetros de navegación distintos
> (el fisioterapeuta la ve como una pestaña más de su barra lateral; el
> paciente, como pantalla de detalle -- ver ampliación posterior abajo).
> El ícono de
> cerrar sesión se retiró de `PacientesListScreen` (HU01) y
> `EjerciciosAsignadosScreen` (HU04); el administrador conserva su
> logout, ahora como opción "Cerrar sesión" dentro de la barra de
> navegación lateral (`AdminPacientesScreen`/`AdminFisioterapeutasScreen`),
> con el mismo `DialogoConfirmacion` antes de cerrar sesión.
>
> **Ampliacion posterior (2026-09-10):** el paciente ahora tambien ve
> Perfil como una pestana mas de su propia barra lateral (Ejercicios/
> Progreso/Perfil), igual que fisio/admin -- antes era una pantalla de
> detalle con solo boton atras, sin barra persistente. Ver la nota de
> `ScaffoldConBarraLateral` en la seccion 3 (Design System).
>
> **Bug real encontrado y corregido (2026-09-10):** al navegar a Perfil,
> la app crasheaba con `PERMISSION_DENIED` -- las Firestore Security
> Rules no dejaban a un paciente leer el documento de su propio
> fisioterapeuta asignado (necesario para mostrar "Fisioterapeuta
> asignado" en su perfil, HU22-CA01). Se agrego la funcion
> `esPacienteDe(fisioUid)` en `firestore.rules` (compara
> `fisioterapeutaId` del documento del solicitante) y se desplego a
> produccion.

---

### Requisitos No Funcionales

#### RNF01 — Disponibilidad operativa del sistema
El sistema deberá garantizar disponibilidad operativa, incluyendo funcionamiento sin conexión a internet para las funciones de procesamiento local.
- CA01: Los usuarios deben poder consultar funcionalidades terapéuticas con disponibilidad correcta.
- CA02: Sin conexión a internet, el sistema debe permitir ejecutar y monitorear una sesión localmente, sincronizando al reconectar.
- CA03: La disponibilidad debe mantenerse durante la operación del fisioterapeuta.

> Nota (Sprint 5): al igual que HU19, esta RNF **ya se cumple desde
> Sprint 1** gracias a la persistencia offline por defecto de Firestore
> (lecturas desde caché local + cola de escritura offline) — el
> procesamiento de cámara/MediaPipe (HU06/07/08) de por sí ya corre 100%
> en el dispositivo, sin red. No se agrega código nuevo.

#### RNF02 — Seguridad de acceso a la información terapéutica
> Nota: no existe una HU propia de "Iniciar sesión" — la pantalla de Login
> implementa directamente CA01 (login) y CA02 (redirección por rol) de
> este RNF. **Cerrar sesión (CA04) ya no es un botón repetido en cada
> pantalla** (ver ÉPICA 07, HU22/HU23): para paciente y fisioterapeuta se
> centralizó en la pantalla de Perfil, y para el administrador pasó a ser
> una opción dentro de la barra de navegación lateral — en los tres casos,
> con un diálogo de confirmación antes de finalizar la sesión. No hay
> pantalla de auto-registro ni recuperación de contraseña: las cuentas se
> crean por el Administrador (HU20/HU21) o el script `crear-usuario.ts`.
- CA01: El sistema valida correctamente la autenticación.
- CA02: El sistema restringe el acceso según el tipo de usuario (rol).
- CA03: La información almacenada debe mantenerse protegida.
- CA04: Al cerrar sesión, el sistema finaliza correctamente el acceso autenticado.

#### RNF03 — Compatibilidad con dispositivos móviles
El sistema debe garantizar compatibilidad con dispositivos Android que cumplan los requisitos mínimos definidos.
- CA01: Debe instalarse y ejecutarse correctamente en Android 10 o superior.
- CA02: Debe permitir el monitoreo corporal si el dispositivo cuenta con cámara funcional.
- CA03: Si el dispositivo no cumple los requisitos mínimos, el sistema debe informar la incompatibilidad.

#### RNF04 — Integridad de la información terapéutica
- CA01: Los datos almacenados deben mantenerse completos.
- CA02: El sistema debe recuperar correctamente los datos almacenados al consultarlos.
- CA03: La información debe mantenerse consistente entre usuarios tras sincronizar.
- CA04: Ante una interrupción durante el guardado, el sistema debe evitar pérdida o duplicación de datos.

> Nota (Sprint 5): esta RNF **ya se cumple** con lo construido en sprints
> anteriores, sin código nuevo — cada escritura (`guardarResultado`,
> `asignarSesion`, `crear` de recomendación, etc.) es un único
> `.set()`/`.add()` de Firestore, atómico por documento (no hay
> transacciones multi-documento que puedan quedar a medias), y todos los
> mappers (`toSesion()`, `toUsuario()`, etc.) usan valores por defecto
> ante campos ausentes en vez de lanzar excepciones (CA01/CA02). CA03 se
> cubre igual que HU19 (listeners en vivo + Firestore offline).

#### RNF05 — Consistencia del monitoreo corporal
El sistema debe garantizar consistencia del monitoreo corporal ante condiciones adversas del entorno domiciliario.
- CA01: Ante variaciones de iluminación, la detección de landmarks debe mantenerse sin interrupciones significativas.
- CA02: Ante una oclusión parcial de un punto anatómico, el sistema debe continuar el seguimiento del resto del cuerpo.
- CA03: Ante cambios de distancia o posición del paciente frente a la cámara, el sistema debe mantener la consistencia del monitoreo.

#### RNF06 — Privacidad y procesamiento local de datos biométricos (Edge AI)
El sistema debe garantizar el procesamiento local de la información biométrica capturada por la cámara, para proteger la privacidad del paciente y cumplir la Ley N.° 29733.
- CA01: El procesamiento del video debe realizarse localmente en el dispositivo, sin enviarlo a servidores externos.
- CA02: Se almacenan/sincronizan datos numéricos (vectores, ángulos, métricas) y, por la enmienda del 2026-09-18 (sección 11), el video de la sesión para revisión del fisioterapeuta; nunca imágenes sueltas.
- CA03: La app debe solicitar únicamente los permisos estrictamente necesarios (cámara y, cuando corresponda, internet).
- CA04: En el primer uso, el sistema debe presentar el consentimiento informado sobre el tratamiento de datos personales.

---

### Backlog priorizado por sprint

| Sprint | Épica | Código | Historia / Requisito | Prioridad |
|---|---|---|---|---|
| 1 | RNF | RNF02 | Seguridad de acceso | Crítica |
| 1 | RNF | RNF06 | Privacidad y procesamiento local | Crítica |
| 1 | E06 | HU17 | Almacenar información terapéutica | Crítica |
| 1 | E01 | HU01 | Gestionar pacientes terapéuticos | Crítica |
| 1 | E01 | HU02 | Gestionar ejercicios terapéuticos | Crítica |
| 1 | E07 | HU20 | Gestionar cuentas de pacientes | Crítica |
| 1 | E07 | HU21 | Gestionar cuentas de fisioterapeutas | Crítica |
| 1 | E07 | HU22 | Consultar y editar mi perfil (Paciente) | Media |
| 1 | E07 | HU23 | Consultar y editar mi perfil (Fisioterapeuta) | Media |
| 2 | E01 | HU03 | Asignar sesiones terapéuticas | Alta |
| 2 | E01 | HU04 | Visualizar ejercicios asignados | Crítica |
| 2 | E01 | HU05 | Consultar material terapéutico | Crítica |
| 3 | E02 | HU06 | Ejecutar sesión terapéutica | Crítica |
| 3 | E02 | HU07 | Monitorear movimiento corporal | Crítica |
| 3 | E02 | HU08 | Procesar movimiento corporal | Crítica |
| 3 | RNF | RNF05 | Consistencia del monitoreo corporal | Alta |
| 3 | RNF | RNF03 | Compatibilidad con dispositivos móviles | Alta |
| 3 | E03 | HU11 | Visualizar resultados y % de ejecución | Crítica |
| 3 | E04 | HU13 | Consultar historial terapéutico | Media |
| 4 | E02 | HU09 | Analizar ejecución terapéutica | Alta |
| 4 | E03 | HU10 | Generar retroalimentación inmediata | Crítica |
| 4 | E05 | HU15 | Registrar y gestionar recomendaciones | Alta |
| 4 | E05 | HU16 | Consultar recomendaciones terapéuticas | Alta |
| 5 | E04 | HU12 | Visualizar progreso y evolución | Media |
| 5 | E04 | HU14 | Monitorear cumplimiento terapéutico | Media |
| 5 | E06 | HU18 | Gestionar sesiones y resultados registrados | Alta |
| 5 | E06 | HU19 | Sincronizar información terapéutica | Media |
| 5 | RNF | RNF01 | Disponibilidad operativa del sistema | Crítica |
| 5 | RNF | RNF04 | Integridad de la información terapéutica | Alta |

> Nota (Sprint 1, mismo criterio que HU20/HU21): HU22/HU23 no estaban en
> el backlog original de 19 HU — se agregan como ampliación acordada,
> junto a HU20/HU21, para dar autoservicio de perfil a paciente y
> fisioterapeuta, reutilizando el modelo de datos y el Design System
> existentes sin modificarlos. Traen consigo un ajuste de UI (no de HU)
> en las pantallas de HU01 y HU04, que pierden su ícono de cerrar sesión
> en favor de la nueva pantalla de Perfil — ver el detalle en ÉPICA 07 y
> en la nota actualizada de RNF02.
>
> Nota: el plan original repartía esto en 6 sprints; se comprimió a 5.
> HU15/HU16 (recomendaciones) se adelantaron al sprint 4 junto con
> HU09-10 porque ya dependen de que exista una sesión con resultado real
> (HU15-CA01: "seleccione una sesión realizada"), disponible desde
> HU08/HU09 — no había motivo para esperar un sprint más. El resto de la
> Épica 04 (seguimiento) y el cierre de la Épica 06 (HU18/HU19, RNF01,
> RNF04) se fusionaron en el sprint 5 final, ya que son en su mayoría
> pantallas de consulta/lectura sobre datos que otras historias ya
> generan, más el endurecimiento (offline, integridad) de cierre de
> proyecto.
>
> Ajuste posterior (durante el propio sprint 3): HU11 y HU13 se
> adelantaron de los sprints 4/5 al sprint 3, por decisión explícita del
> usuario al probar HU06/07/08 — no tenía sentido ejecutar sesiones y
> guardar resultados (HU08 ya los calcula completos: % de ejecución,
> desviación promedio, ángulos por articulación) sin que el paciente
> pudiera ver ni su historial ni el detalle de lo que hizo. HU09
> (clasificación fina del tipo de error) y HU10 (retroalimentación en
> vivo) siguen en el sprint 4 sin cambios — HU11 solo necesita los datos
> que HU08 ya deja listos, no depende de HU09/HU10.

---

## 11. Reglas del proyecto (no negociables)

1. Todo **procesamiento** de cámara/video es **local al dispositivo** (Edge
   AI, MediaPipe). Enmienda 2026-09-18 (antes
   la regla prohibía subir cualquier video): el video de cada sesión SÍ se
   graba (sin audio, SD) y se sube a Firebase Storage
   (`sesiones/{pacienteId}/{sesionId}.mp4`, URL en `Sesion.videoUrl`) para
   que el fisioterapeuta lo vea y pueda dar una mejor respuesta (botón "Ver video de la sesión" en
   `FisioResultadoSesionScreen`). Como consecuencia: el consentimiento
   informado (`ConsentimientoScreen`) lo informa explícitamente y su clave
   pasó a `aceptado_v2`, así que todos los usuarios lo vuelven a aceptar; las
   Storage Rules permiten al paciente escribir solo en su carpeta. Si una
   sesión se abandona con "Salir", el video se descarta. Limitaciones
   conocidas: al reanudar una sesión (HU06-CA09) el video del tramo nuevo
   reemplaza al anterior; cualquier usuario autenticado con la URL puede leer
   el video (Storage no puede cruzar a Firestore para validar al
   fisioterapeuta asignado).
2. El control de acceso por rol se implementa con **Firestore Security
   Rules**, no con un backend propio (se evaluó y se descartó Cloud
   Functions por falta de un caso de uso real).
3. La app debe permitir ejecutar y monitorear una sesión **sin conexión**,
   sincronizando la información al reconectar (RNF01).
4. La retroalimentación visual durante el ejercicio no debe superar
   **500 ms** de latencia desde la detección del movimiento (HU10).
5. Roles: **Paciente** ejecuta ejercicios y recibe retroalimentación;
   **Fisioterapeuta** asigna rutinas y hace seguimiento — no mezclar
   funcionalidades entre roles sin revisar la historia correspondiente.
6. El backlog de la sección 6 es la fuente de verdad de qué construir y en
   qué orden (por sprint) — no reinterpretar ni ampliar el alcance sin
   señalarlo explícitamente.
