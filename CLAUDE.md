# CLAUDE.md

Este es el ÚNICO archivo de contexto del proyecto para Claude Code. No hace
falta leer la tesis en Word: todo lo relevante para desarrollar la app ya
está resumido y corregido aquí (arquitectura, stack, modelo de datos,
historias de usuario y requisitos no funcionales, priorizados por sprint).

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
postura (MediaPipe Pose) corre **en el dispositivo** (Edge AI): no se procesa
ni se almacena video en la nube, solo métricas numéricas (ángulos, vectores).

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
| `BadgeEstado` (enum `TipoBadge`: EXITO/ADVERTENCIA/ERROR/NEUTRO) | `Badges.kt` | Pill de estado — reemplaza `EstadoPill` y equivalentes sueltos |
| `SelectorDropdown<T>` | `SelectorDropdown.kt` | Genérico — reemplaza el patrón repetido Box+OutlinedTextField+clickable+DropdownMenu |
| `FilaChipsFiltro<T>` | `FiltroChips.kt` | Fila de `FilterChip` de selección única — filtro por período/ejercicio, reemplaza el Row+FilterChip repetido a mano |
| `ChecklistAgrupado<T>` | `ChecklistAgrupado.kt` | Selección múltiple agrupada (ej. diagnósticos por región) — checkbox + texto por opción, con encabezado de grupo |
| `TarjetaEstadistica` | `TarjetaEstadistica.kt` | Ícono + cifra grande + etiqueta, para dashboards |
| `TarjetaPersona` | `TarjetaPersona.kt` | Avatar+nombre+subtítulo+slots — generaliza fila de paciente/fisioterapeuta/admin |
| `TarjetaConIcono` | `TarjetaConIcono.kt` | Avatar de ícono (círculo de color, no iniciales) + título/subtítulo + slots de contenido final e inferior — tarjetas de "acción" tipo fila: próxima sesión, sesión reanudable, acceso rápido, ejercicio asignado |
| `TarjetaEjercicio` | `TarjetaEjercicio.kt` | Tarjeta de grid 2 columnas con menú "⋮" opcional |
| `BarraSuperior` | `BarraSuperior.kt` | TopAppBar con el color/estilo del Design System |
| `BarraBusqueda` | `BarraBusqueda.kt` | Búsqueda + botón de filtro opcional |
| `ProgresoCircular` / `ProgresoLineal` | `ProgresoCircular.kt` | Anillo con cifra centrada / barra lineal — los dos indicadores de progreso |
| `EstadoCargando` / `EstadoVacio` / `EstadoError` | `EstadosPantalla.kt` | Los 3 estados que toda pantalla con datos remotos debe cubrir |
| `DialogoConfirmacion` | `Dialogos.kt` | Confirmar eliminar/descartar |
| `rememberSnackbarDeMensaje` | `SnackbarDS.kt` | Reemplaza el `SnackbarHostState`+`LaunchedEffect` repetido a mano en el panel admin |
| NavigationRail lateral | `core/navigation/ScaffoldConBarraLateral.kt` (ya existía) | Su `topBar` ya usa `BarraSuperior` (con `onAlternarMenu`) en todas las pantallas que lo consumen (fisio y admin) |
| `BotonSelectorFecha` | `SelectorFecha.kt` | Botón que abre un `DatePickerDialog`; resuelve internamente la conversión de zona horaria UTC↔local (antes vivía inline en `AsignarSesionScreen`) |
| Gráfico de línea (evolución semanal) | *pendiente* | No existe todavía — se construye cuando se aplique a la pantalla de Progreso |

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
                                fisioterapeuta de un catálogo cerrado de 13
                                — HU01-CA06, ampliado de un solo valor a
                                lista con catálogo clínico más específico.
                                Ver sección 9, Catálogo de diagnósticos)
  - dni, edad                 (solo si rol = paciente; capturados por el
                                administrador al registrarlo — HU20-CA02,
                                revisión de Sprint 5)
  - fechaRegistro

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
    - resultado: {
        angulosDetectados, desviacionPromedio, porcentajeEjecucion,
        erroresDetectados: [{ articulacion, tipo, repeticiones }],
        repeticionesCompletadas, repeticionesAsignadas, repeticionesCorrectas
                             (HU11-CA05, Sprint 3: completadas puede ser
                              menor a asignadas si se finalizó antes de
                              tiempo, HU06-CA07; correctas cuenta las
                              repeticiones sin ningún error detectado),
        detallePorRepeticion: [{ numero, dentroDeRango, errores }]
                             (HU18-CA04, Sprint 4: desglose por repetición
                              que ve el fisioterapeuta antes de
                              recomendar, HU15. `errores` es el mismo tipo
                              que `erroresDetectados` de arriba pero
                              acotado a esa repetición puntual, no agregado)
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
  - creadoPor, fechaCreacion, activo
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

Catálogo real (no de prueba) sembrado en Firestore vía
`backend/seed/crear-catalogo-ejercicios.ts`. Elegido con 3 criterios:
seguros y genéricos (no específicos de una sola patología), visibles desde
una sola cámara sin que el cuerpo salga del encuadre, y que cubran las
articulaciones ya soportadas por el enum `Articulacion`.

| # | Ejercicio | Categoría | Articulación | Repeticiones | Rango de referencia (seed)* |
|---|---|---|---|---|---|
| 1 | Flexión de hombro | Movilidad | Hombro derecho | 3 | 70°–110° |
| 2 | Abducción de hombro | Movilidad | Hombro derecho | 3 | 70°–110° |
| 3 | Flexión de codo | Movilidad | Codo derecho | 3 | 60°–90° |
| 4 | Marcha estacionaria | Movilidad | Cadera derecha | 6 | 100°–140° |
| 5 | Flexión de rodilla (sentado) | Movilidad | Rodilla derecha | 6 | 90°–160° |
| 6 | Flexión dorsal/plantar de tobillo | Movilidad | Tobillo derecho | 6 | 80°–100° |
| 7 | Rotación de tronco (sentado) | Movilidad | Tronco | 6 | 60°–100° |
| 8 | Mini sentadilla | Control motor | Rodilla derecha | 6 | 120°–150° |
| 9 | Puente de glúteos | Control motor | Cadera derecha | 6 | 160°–180° |

\* Estos ángulos están en la convención de este sistema (el ángulo crudo
calculado por `AnguloCalculator` en el vértice de la articulación, no la
goniometría clínica tradicional) — son un **punto de partida técnico**
razonable, no una dosis clínica. El fisioterapeuta puede y debe refinarlos
por paciente, ya sea editándolos a mano (HU02-CA05) o recalculándolos
analizando un video real con "Calcular ROM automáticamente" (HU02-CA07) una
vez que existan clips grabados para cada ejercicio — el seed deja
`materialUrl` vacío a propósito, pendiente de que se graben esos videos.

### Nota sobre el ejercicio "Equilibrio monopodal" (deliberadamente fuera del seed)

No se mide por ángulo articular sino por estabilidad/tiempo sostenido — con
el diseño actual de HU08/HU09 (comparación contra un ángulo esperado) no
encaja igual que los otros 9. Queda fuera del catálogo sembrado; se
agregaría en una segunda iteración con una métrica distinta (tiempo
balanceado, oscilación del tronco), no por defecto.

---

## 9. Catálogo de diagnósticos

Catálogo real del enum `TipoDiagnostico` (13 valores, ampliación acordada
sobre el catálogo genérico anterior de 7). Igual que antes: **la app no
genera diagnósticos** — el fisioterapeuta registra el que ya obtuvo de su
evaluación clínica; el sistema solo lo usa para **sugerir** ejercicios
relacionados (HU03-CA07), sin decidir ni bloquear nada por su cuenta.

### Lista de diagnósticos (13), agrupados por región corporal

| Región | Diagnóstico | Ejercicios sugeridos (del catálogo) |
|---|---|---|
| Hombro | Síndrome de pinzamiento subacromial | Flexión de hombro, Abducción de hombro |
| Hombro | Capsulitis adhesiva (hombro congelado) | Flexión de hombro, Abducción de hombro |
| Hombro | Rehabilitación post-quirúrgica de manguito rotador | Flexión de hombro, Abducción de hombro |
| Codo | Rigidez postraumática de codo | Flexión de codo |
| Cadera | Rehabilitación post-artroplastia de cadera | Marcha estacionaria, Puente de glúteos |
| Cadera | Osteoartrosis de cadera | Marcha estacionaria, Puente de glúteos |
| Rodilla | Post-reconstrucción de ligamento cruzado anterior | Flexión de rodilla, Mini sentadilla |
| Rodilla | Osteoartrosis de rodilla | Flexión de rodilla, Mini sentadilla |
| Rodilla | Síndrome de dolor femoropatelar | Flexión de rodilla, Mini sentadilla |
| Tobillo | Esguince de tobillo (fase funcional) | Flexión dorsal/plantar de tobillo |
| Columna | Lumbalgia mecánica / dolor lumbar inespecífico | Rotación de tronco, Puente de glúteos |
| General | Debilidad muscular / desacondicionamiento físico | Mini sentadilla, Puente de glúteos |
| General | Alteraciones del equilibrio / riesgo de caídas | Equilibrio monopodal (segunda iteración, sin ejercicio sembrado aún) |

### Estructura de datos (Firestore)

```
usuarios/{pacienteId}
  - diagnosticos: [
      { codigo: "OSTEOARTROSIS_RODILLA", fecha: <Timestamp> },
      { codigo: "DEBILIDAD_MUSCULAR", fecha: <Timestamp> }
    ]

ejercicios/{ejercicioId}
  - diagnosticosAplicables: ["OSTEOARTROSIS_RODILLA", "POST_RECONSTRUCCION_LCA", ...]
```

La sugerencia de HU03-CA07 se resuelve **client-side**: la pantalla de
asignar sesión ya tiene cargado el catálogo completo de ejercicios (~10
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
- CA06 *(ampliación acordada, Sprint 3, no en la versión original de la tesis;
  revisada durante el propio Sprint 3: pasó de texto libre a catálogo
  cerrado; ampliada de nuevo más adelante: de un solo valor a una lista)*:
  Dado que consulte el detalle de un paciente, cuando elija uno o más
  diagnósticos de un catálogo cerrado de 13 valores agrupados por región
  corporal (sección 9, Catálogo de diagnósticos), entonces el sistema los
  guarda (cada uno con su propia fecha) y los muestra junto al resto de su
  información terapéutica (incluida la lista de pacientes). No es texto
  libre: el fisioterapeuta selecciona de la lista, no redacta; un paciente
  puede tener más de un diagnóstico a la vez (ej. osteoartrosis de rodilla +
  desacondicionamiento general).

#### HU02 — Gestionar ejercicios terapéuticos
**Rol:** Fisioterapeuta
**Deseo:** Registrar y administrar ejercicios terapéuticos
**Propósito:** Asignar actividades terapéuticas a los pacientes.
- CA01: Dado que desea registrar un ejercicio, cuando seleccione "Registrar ejercicio", entonces el sistema muestra el formulario.
- CA02: Dado que completa los datos requeridos, cuando los envíe, entonces el sistema almacena la información.
- CA03: Dado que desea asociar material terapéutico, cuando seleccione contenido audiovisual, entonces el sistema lo almacena correctamente.
- CA04: Dado que existen ejercicios registrados, cuando acceda al módulo, entonces el sistema muestra la lista disponible.
- CA05: Dado que desea modificar un ejercicio, cuando actualice la información, entonces el sistema guarda los cambios.
- CA06: Dado que desea eliminar un ejercicio, cuando confirme la acción, entonces el sistema lo elimina.
- CA07 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que asocie un video como material terapéutico y haya seleccionado
  al menos una articulación de referencia, cuando solicite el cálculo
  automático de rango, entonces el sistema analiza el video en el propio
  dispositivo (MediaPipe, sin subirlo a ningún servicio externo — RNF06) y
  calcula el ángulo mínimo y máximo observado para cada articulación, sin
  que el fisioterapeuta deba escribirlos a mano. El fisioterapeuta puede
  seguir editando esos valores manualmente después si lo considera
  necesario.
- CA08 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que registre un ejercicio, cuando indique el número de
  repeticiones, entonces el sistema lo guarda junto con la duración de
  cada repetición (`duracionSegundos`, CA02). Este número es el que usa
  HU06 para repetir el ciclo de monitoreo esa cantidad de veces dentro
  de una misma sesión.
- CA09 *(ampliación acordada, no en la versión original de la tesis)*: Dado
  que registre o edite un ejercicio, cuando elija su categoría, entonces el
  sistema restringe la opción a un catálogo cerrado de 2 valores (Movilidad
  / Control motor — sección 7), no texto libre. Opcionalmente puede además
  marcar uno o más diagnósticos para los que este ejercicio se sugiere
  primero al asignar una sesión (`diagnosticosAplicables`, ver HU03-CA07).

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
  referencia antes de guardar.
- CA07 *(ampliación acordada, no en la versión original de la tesis)*: Dado
  que el paciente tenga uno o más diagnósticos registrados (HU01-CA06),
  cuando el fisioterapeuta abra el selector de ejercicios para asignar,
  entonces el sistema resalta primero (★) los ejercicios sugeridos para
  esos diagnósticos (según `ejercicios/{id}.diagnosticosAplicables`, sección
  9), sin impedir seleccionar cualquier otro ejercicio del catálogo.

#### HU04 — Visualizar ejercicios asignados
**Rol:** Paciente
**Deseo:** Visualizar los ejercicios terapéuticos asignados
**Propósito:** Consultar las actividades que debo realizar.
- CA01: Dado que accede al módulo de ejercicios, entonces el sistema muestra las actividades asignadas.
- CA02: Dado que existen ejercicios asignados, cuando consulte la información, entonces el sistema muestra el detalle de cada uno.
- CA03: Dado que desea consultar un ejercicio específico, cuando lo seleccione, entonces el sistema muestra su información.
- CA04: Dado que no tiene ejercicios asignados, entonces el sistema muestra un mensaje de ausencia.

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

#### HU06 — Ejecutar sesión terapéutica
**Rol:** Paciente
**Deseo:** Ejecutar una sesión terapéutica
**Propósito:** Realizar los ejercicios asignados por el fisioterapeuta.
- CA01: Dado que selecciona una sesión asignada, entonces el sistema muestra el ejercicio correspondiente.
- CA02: Dado que inicia la sesión, cuando seleccione "Iniciar sesión", entonces el sistema habilita la ejecución. *(Ampliación acordada, no en la versión original: antes de empezar a medir la primera repetición, el sistema muestra una cuenta regresiva de preparación de 10 segundos — durante ese lapso la cámara ya está encendida pero el movimiento no se mide, para que el paciente tenga tiempo de acomodarse frente a ella.)*
- CA03: Dado que ejecuta el ejercicio, cuando la cámara detecte movimiento, entonces el sistema inicia el monitoreo.
- CA04: Dado que finaliza el tiempo establecido, entonces el sistema concluye la sesión.
- CA05: Dado que completa la sesión, entonces el sistema la registra.
- CA06 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que el ejercicio tenga más de una repetición asignada (HU02-CA08)
  y termine el tiempo de la repetición actual, cuando todavía falten
  repeticiones por completar, entonces el sistema anuncia el inicio de
  la siguiente repetición tras una breve pausa, en vez de concluir la
  sesión — CA04/CA05 solo aplican una vez completadas todas las
  repeticiones asignadas. Todas las repeticiones de una misma sesión se
  registran juntas como un único resultado consolidado (HU08-CA04), no
  como sesiones separadas. *(Ampliación posterior: durante esa pausa, al
  quedar 3 segundos restantes el sistema avisa una sola vez por voz — "Prepárate,
  sigue la repetición N" — para que el paciente sepa que se acerca la
  siguiente repetición sin tener que mirar la pantalla; el número
  regresivo en sí se sigue mostrando visualmente, esto solo agrega el
  aviso hablado.)*
- CA07 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que la sesión esté en ejecución, cuando el paciente seleccione
  "Finalizar ejercicio" antes de completar el tiempo o las repeticiones
  asignadas, entonces el sistema concluye la sesión igualmente y registra
  el resultado con lo medido hasta ese momento (mejor un resultado
  parcial que perder por completo la ejecución ya realizada). Distinto
  de "Salir", que abandona la pantalla sin registrar nada.
- CA08 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que el paciente inicie el monitoreo de una sesión, entonces el
  sistema lee en voz alta (texto a voz nativo de Android, en el
  dispositivo, sin conexión) la descripción/instrucción del ejercicio una
  sola vez al empezar — no en cada repetición. Distinto de HU10-CA06
  (Sprint 4): esto lee la instrucción estática del ejercicio
  (`Ejercicio.descripcion`), no genera frases de corrección en tiempo real
  según el error detectado.
- CA09 *(ampliación acordada, Sprint 4, no en la versión original de la
  tesis)*: Dado que una sesión se haya finalizado antes de tiempo
  (CA07) sin completar todas las repeticiones asignadas, entonces el
  sistema la muestra en "Mis ejercicios" como **reanudable** (tarjeta
  "Reanudar sesión" con "X/Y repeticiones completadas"), distinta de una
  sesión pendiente nueva. Al reanudarla, el monitoreo continúa desde la
  repetición siguiente a la última completada — no repite lo ya medido —
  y el resultado final combina lo guardado antes con lo nuevo medido
  (repeticiones correctas se suman, el detalle por repetición se
  concatena con la numeración real, y los promedios/porcentajes globales
  se recalculan ponderados por cuántas repeticiones aportó cada tramo).
  Una sesión deja de ser reanudable en cuanto completa todas sus
  repeticiones asignadas (`repeticionesCompletadas == repeticionesAsignadas`).

#### HU07 — Monitorear movimiento corporal
**Rol:** Sistema
**Deseo:** Monitorear el movimiento corporal del paciente mediante la cámara
**Propósito:** Identificar los puntos anatómicos necesarios para el análisis de la ejecución terapéutica.
> Nota: se retiraron los CA de estabilidad/consistencia (duplicaban RNF05). Esta HU se enfoca en la detección; RNF05 concentra la estabilidad ante condiciones adversas.
- CA01: Dado que la cámara esté habilitada, entonces el sistema detecta el movimiento corporal mediante estimación de pose (MediaPipe).
- CA02: Dado que realiza el monitoreo, cuando el paciente ejecute movimientos, entonces el sistema identifica los landmarks correspondientes.
- CA03: Dado que detecte más de una persona u objetos en el encuadre, entonces el sistema prioriza el cuerpo del paciente.
- CA04: Dado que el monitoreo esté activo, entonces el sistema procesa correctamente la información detectada para su análisis posterior.

#### HU08 — Procesar movimiento corporal
**Rol:** Sistema
**Deseo:** Procesar los puntos anatómicos detectados y calcular los ángulos articulares
**Propósito:** Obtener métricas biomecánicas objetivas de la ejecución del paciente.
- CA01: Dado que detecte los landmarks, cuando el paciente ejecute el ejercicio, entonces el sistema calcula los ángulos articulares correspondientes.
- CA02: Dado que calcule ángulos articulares, cuando compare con el ROM esperado, entonces determina si la ejecución está dentro del margen aceptable.
- CA03: Dado que ocurra una oclusión temporal de un punto anatómico, entonces el sistema continúa el procesamiento sin interrumpirse.
- CA04: Dado que finalice la sesión, entonces el sistema genera un resumen de ángulos y desviaciones posturales.

#### HU09 — Analizar ejecución terapéutica
**Rol:** Sistema
**Deseo:** Analizar la ejecución terapéutica del paciente
**Propósito:** Generar un resultado referencial sobre la calidad del ejercicio realizado.
- CA01: Dado que reciba los ángulos calculados, entonces el sistema compara la ejecución con el patrón de referencia del ejercicio.
- CA02: Dado que detecte una desviación relevante, entonces el sistema clasifica el tipo de error (rango incompleto, desviación angular, etc.).
- CA03: Dado que el análisis concluya, entonces el sistema genera un resultado consolidado (% de acierto o desviación promedio).

> Nota (Sprint 4): esta HU **ya se cumple desde Sprint 3**, sin código
> nuevo — `MedicionArticulacion.tipoDeError` ya clasifica "Rango
> incompleto"/"Desviación angular" (CA02), `medirArticulacion` ya compara
> contra `patronReferencia` (CA01), y `porcentajeEjecucion`/
> `desviacionPromedio` ya son el resultado consolidado (CA03). Se deja
> constancia aquí para no repetir trabajo ya hecho.

---

### ÉPICA 03: Generar retroalimentación terapéutica

#### HU10 — Generar retroalimentación inmediata
**Rol:** Paciente
**Deseo:** Recibir retroalimentación visual inmediata durante los ejercicios
**Propósito:** Corregir mi ejecución terapéutica mientras la realizo.
- CA01: Dado que el sistema detecte una desviación, entonces muestra retroalimentación visual con la corrección sugerida.
- CA02: Dado que no se detecten desviaciones relevantes, entonces el sistema indica visualmente que la ejecución es correcta.
  > Nota (Sprint 4, confirmado con el usuario): el paciente no puede leer
  > texto en pantalla mientras se mueve, así que "retroalimentación
  > visual" se resuelve con un **ícono mínimo** (check verde / alerta
  > ámbar), sin texto de corrección en pantalla — la corrección en sí la
  > lleva la voz (CA06). No se construye el esqueleto con checks por
  > articulación del mockup de referencia (decisión explícita).
- CA03: Dado que continúe ejecutando el ejercicio, entonces la retroalimentación se actualiza de forma continua.
- CA04: Dado que el sistema procese el movimiento, entonces el tiempo de respuesta no debe superar los **500 ms**.
- CA05: Dado que finalice el ejercicio, entonces el sistema detiene la retroalimentación inmediata.
- CA06 *(ampliación acordada, no en la versión original de la tesis)*: Dado
  que el sistema detecte una desviación, entonces además de la señal visual
  reproduce una indicación por voz (texto a voz nativo de Android, en el
  dispositivo, sin conexión) describiendo la corrección — ej. "levanta más
  el brazo derecho" — usando frases plantilla mapeadas al tipo de error
  detectado, nunca un texto generado en el momento ni un servicio de voz en
  la nube (para no romper RNF01 y RNF06).
- CA07 *(ampliación acordada)*: Dado que un mismo tipo de error se repita
  varias veces durante la ejecución, entonces el sistema lo acumula como
  parte del resultado consolidado de la sesión (ver `erroresDetectados` en
  la sección 5), para que el fisioterapeuta lo vea como evidencia en HU01/
  HU18 y decida qué indicarle al paciente (por sesión guardada o, más
  adelante, por un canal de comunicación aparte — HU de chat aún sin
  definir, fuera del alcance de las 19 HU actuales).

#### HU11 — Visualizar resultados y porcentaje de ejecución
**Rol:** Paciente
**Deseo:** Visualizar los resultados y el porcentaje de ejecución de mi sesión
**Propósito:** Conocer mi desempeño referencial en los ejercicios realizados.
- CA01: Dado que finalice una sesión, entonces el sistema muestra los resultados obtenidos, incluyendo el % de ejecución.
- CA02: Dado que consulte el detalle, entonces el sistema muestra métricas comprensibles (desviación promedio, % de acierto).
- CA03: Dado que la sesión incluya más de un ejercicio, entonces el sistema diferencia el resultado por cada uno.
- CA04: Dado que consulte una sesión anterior, entonces el sistema muestra el mismo detalle de resultados obtenido en su momento.
- CA05 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que la sesión se haya completado o finalizado antes de tiempo
  (HU06-CA07), entonces el sistema muestra cuántas repeticiones se
  llegaron a completar sobre el total asignado (ej. "8/12"), y de esas
  cuántas no tuvieron ningún error ("Correctas") frente a las que sí
  ("Errores") — así se distingue una ejecución completa de una parcial.
- CA06 *(ampliación acordada, Sprint 3, no en la versión original de la tesis)*:
  Dado que consulte el resultado de una sesión, entonces el sistema
  ofrece acceso directo a "Ver mi progreso" (historial, HU13) y "Volver
  al inicio" (HU04), sin tener que navegar hacia atrás pantalla por
  pantalla.

---

### ÉPICA 04: Gestionar seguimiento terapéutico

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
**Rol:** Paciente
**Deseo:** Consultar mi historial terapéutico
**Propósito:** Visualizar las sesiones terapéuticas realizadas.
- CA01: Dado que consulte su historial, entonces el sistema muestra las sesiones realizadas.
- CA02: Dado que seleccione un registro, entonces el sistema muestra el detalle de esa sesión.
- CA03: Dado que existan múltiples sesiones, entonces el sistema las ordena de la más reciente a la más antigua.
- CA04: Dado que no haya completado ninguna sesión, entonces el sistema muestra un mensaje de ausencia de registros.

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

#### HU16 — Consultar recomendaciones terapéuticas
**Rol:** Paciente
**Deseo:** Consultar las recomendaciones terapéuticas registradas
**Propósito:** Visualizar las observaciones realizadas por el fisioterapeuta.
- CA01: Dado que el fisioterapeuta registre una recomendación, entonces el sistema la muestra al paciente en su sesión.
- CA02: Dado que seleccione el detalle de una sesión, entonces el sistema visualiza las recomendaciones registradas.
- CA03: Dado que se registren nuevas recomendaciones, entonces el sistema actualiza la información mostrada.
- CA04: Dado que no existan recomendaciones para una sesión, entonces el sistema indica que no hay disponibles.

---

### ÉPICA 06: Gestionar almacenamiento terapéutico

#### HU17 — Almacenar información terapéutica
**Rol:** Sistema
**Deseo:** Almacenar la información generada durante las sesiones terapéuticas
**Propósito:** Mantener registrados los resultados de forma segura y respetando la privacidad del paciente.
- CA01: Dado que finalice una sesión, entonces el sistema almacena resultados, métricas y fecha.
- CA02: Dado que procese el movimiento corporal, entonces **únicamente almacena datos numéricos** (ángulos, métricas) — **nunca video o imágenes**.
- CA03: Dado que se registre una nueva sesión, entonces el sistema actualiza los datos del paciente sin sobrescribir sesiones anteriores.

#### HU18 — Gestionar sesiones y resultados terapéuticos registrados
**Rol:** Fisioterapeuta
**Deseo:** Gestionar las sesiones y resultados terapéuticos registrados
**Propósito:** Consultar y administrar la información asociada a sesiones y resultados obtenidos.
- CA01: Dado que consulte sesiones y resultados registrados, entonces el sistema muestra la información correspondiente.
- CA02: Dado que acceda al detalle, entonces el sistema visualiza los resultados asociados a cada sesión.
- CA03: Dado que aplique un filtro por fecha o tipo de ejercicio, entonces el sistema muestra solo las sesiones que cumplen el criterio.
- CA04 *(ampliación acordada, no en la versión original de la tesis;
  construida en Sprint 4 junto con la versión mínima de CA02 — ver nota
  de dependencia con HU15 más abajo)*: Dado que acceda al detalle de una
  sesión completada, entonces el sistema
  desglosa el resultado **por repetición** (no solo agregado): para cada
  repetición muestra si estuvo dentro de rango y, si no, qué error se
  detectó y en qué articulación (ej. "Repetición 5: hombro derecho, rango
  incompleto"). Esto es más granular que `erroresDetectados` (HU08-CA04),
  que hoy agrupa por tipo de error con un conteo total, sin registrar en
  qué repetición ocurrió cada uno — requiere una estructura nueva
  (`detallePorRepeticion`, ver sección 5) para poder mostrarlo así. Es la
  base con la que el fisioterapeuta decide qué recomendación registrar
  (HU15).

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
- CA02 *(revisión acordada, no en la versión original)*: Dado que desee
  registrar un paciente, cuando complete nombre, correo, contraseña, DNI,
  edad y uno o más diagnósticos (del mismo catálogo cerrado de HU01-CA06),
  entonces el sistema crea la cuenta con esos datos y la muestra en la
  lista. La contraseña nunca se muestra en texto plano en el formulario —
  solo es legible temporalmente si el administrador presiona el ícono de
  ojo (mismo control en el formulario de fisioterapeuta, aunque ahí no
  aplican DNI/edad/diagnóstico por no ser datos clínicos del propio fisio).
- CA03: Dado que desee actualizar la información de un paciente, cuando modifique los datos correspondientes (incluidos DNI, edad y diagnóstico(s)), entonces el sistema guarda los cambios.
- CA04: Dado que desee eliminar la cuenta de un paciente, cuando confirme la eliminación, entonces el sistema la elimina.
- CA05: Dado que un paciente no tenga fisioterapeuta asignado, cuando el administrador seleccione uno desde la lista, entonces el sistema se lo asigna y la opción de asignar deja de estar disponible para ese paciente.
- CA06 *(ampliación acordada, no en la versión original)*: Dado que consulte
  la lista de pacientes, cuando un paciente ya tenga fisioterapeuta
  asignado, entonces el sistema muestra el nombre de ese fisioterapeuta en
  su tarjeta (en vez del botón de asignar). De forma simétrica (relación
  de uno a muchos: un fisioterapeuta puede tener varios pacientes), la
  lista de fisioterapeutas (HU21) muestra en la tarjeta de cada uno cuántos
  pacientes tiene asignados actualmente.

#### HU21 — Gestionar cuentas de fisioterapeutas
**Rol:** Administrador
**Deseo:** Registrar, editar y eliminar cuentas de fisioterapeutas
**Propósito:** Administrar al personal que atiende a los pacientes.
- CA01: Dado que el administrador acceda al sistema, cuando seleccione "Fisioterapeutas" en el panel de administración, entonces el sistema muestra la lista de fisioterapeutas registrados.
- CA02: Dado que desee registrar un fisioterapeuta, cuando complete nombre, correo y contraseña, entonces el sistema crea la cuenta y la muestra en la lista.
- CA03: Dado que desee actualizar la información de un fisioterapeuta, cuando modifique los datos correspondientes, entonces el sistema guarda los cambios.
- CA04: Dado que desee eliminar la cuenta de un fisioterapeuta, cuando confirme la eliminación, entonces el sistema la elimina.

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
> Nota: no existe una HU propia de "Iniciar/cerrar sesión" — la pantalla de
> Login y el botón de Logout que aparecen en toda la app implementan
> directamente los CA de este RNF (CA01 login, CA02 redirección por rol,
> CA04 logout), no una historia de usuario aparte. No hay pantalla de
> auto-registro ni recuperación de contraseña: las cuentas se crean por el
> Administrador (HU20/HU21) o el script `crear-usuario.ts`.
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
- CA02: Solo se deben almacenar/sincronizar datos numéricos (vectores, ángulos, métricas) — nunca imágenes ni video.
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

1. Todo procesamiento de cámara/video es **local al dispositivo**; nunca se
   sube video ni imágenes a Firebase — solo datos numéricos (RNF06).
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
