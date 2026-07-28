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
6. Historias de Usuario y Requisitos No Funcionales
7. Reglas del proyecto (no negociables)

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
  - nombre, email, rol ("paciente" | "fisioterapeuta")
  - fisioterapeutaId          (solo si rol = paciente; a quién está asignado)
  - fechaRegistro

  usuarios/{pacienteId}/sesiones/{sesionId}
    - ejercicioId
    - fisioterapeutaId
    - fechaAsignacion, fechaEjecucion
    - estado                  ("pendiente" | "completada")
    - resultado: {
        angulosDetectados, desviacionPromedio, porcentajeEjecucion,
        erroresDetectados: [{ articulacion, tipo, repeticiones }]
      }
    - sincronizado: bool      (para el manejo offline de RNF01 / HU19)

    usuarios/{pacienteId}/sesiones/{sesionId}/recomendaciones/{recomendacionId}
      - fisioterapeutaId, texto, fecha

ejercicios/{ejercicioId}
  - nombre, descripcion, categoria
  - materialUrl              (video/imagen en Firebase Storage)
  - patronReferencia         (ángulos ideales / ROM esperado, usado por HU08-09)
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

### Pendiente de validar con el equipo

- ¿Un paciente puede tener más de un fisioterapeuta a la vez? (el modelo
  actual asume uno solo, campo `fisioterapeutaId` singular).
- ¿Se necesita versionar `patronReferencia` de un ejercicio si cambia con el
  tiempo, o basta con el valor vigente?
-e 
---

## 6. Historias de Usuario y Requisitos No Funcionales

Fuente de verdad del producto. 19 Historias de Usuario + 6 Requisitos No
Funcionales, ya depurados (sin criterios de aceptación genéricos/duplicados)
y priorizados en 6 sprints.

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

#### HU03 — Asignar sesiones terapéuticas
**Rol:** Fisioterapeuta
**Deseo:** Asignar sesiones terapéuticas a los pacientes
**Propósito:** Indicar los ejercicios que deberán realizar.
- CA01: Dado que accede al módulo terapéutico, cuando seleccione un paciente, entonces el sistema muestra opciones de asignación.
- CA02: Dado que selecciona ejercicios y confirma, entonces el sistema almacena la sesión con la fecha de asignación.
- CA03: Dado que desea modificar una sesión ya asignada, cuando actualice ejercicios o fecha, entonces el sistema guarda los cambios.
- CA04: Dado que el paciente consulta sus actividades, entonces el sistema muestra las sesiones asignadas.

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
- CA02: Dado que inicia la sesión, cuando seleccione "Iniciar sesión", entonces el sistema habilita la ejecución.
- CA03: Dado que ejecuta el ejercicio, cuando la cámara detecte movimiento, entonces el sistema inicia el monitoreo.
- CA04: Dado que finaliza el tiempo establecido, entonces el sistema concluye la sesión.
- CA05: Dado que completa la sesión, entonces el sistema la registra.

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

---

### ÉPICA 03: Generar retroalimentación terapéutica

#### HU10 — Generar retroalimentación inmediata
**Rol:** Paciente
**Deseo:** Recibir retroalimentación visual inmediata durante los ejercicios
**Propósito:** Corregir mi ejecución terapéutica mientras la realizo.
- CA01: Dado que el sistema detecte una desviación, entonces muestra retroalimentación visual con la corrección sugerida.
- CA02: Dado que no se detecten desviaciones relevantes, entonces el sistema indica visualmente que la ejecución es correcta.
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

#### HU19 — Sincronizar información terapéutica
**Rol:** Sistema
**Deseo:** Sincronizar la información terapéutica generada de forma local
**Propósito:** Mantener actualizados los datos entre el dispositivo del paciente y el fisioterapeuta.
- CA01: Dado que el paciente realice una sesión sin conexión, cuando la conexión se restablezca, entonces el sistema sincroniza automáticamente la información.
- CA02: Dado que existan datos sincronizados recientemente, entonces el sistema muestra la información actualizada al fisioterapeuta.
- CA03: Dado que ocurra un conflicto entre datos locales y ya sincronizados, entonces el sistema prioriza la información más reciente sin duplicar.

---

### Requisitos No Funcionales

#### RNF01 — Disponibilidad operativa del sistema
El sistema deberá garantizar disponibilidad operativa, incluyendo funcionamiento sin conexión a internet para las funciones de procesamiento local.
- CA01: Los usuarios deben poder consultar funcionalidades terapéuticas con disponibilidad correcta.
- CA02: Sin conexión a internet, el sistema debe permitir ejecutar y monitorear una sesión localmente, sincronizando al reconectar.
- CA03: La disponibilidad debe mantenerse durante la operación del fisioterapeuta.

#### RNF02 — Seguridad de acceso a la información terapéutica
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
| 2 | E01 | HU03 | Asignar sesiones terapéuticas | Alta |
| 2 | E01 | HU04 | Visualizar ejercicios asignados | Crítica |
| 2 | E01 | HU05 | Consultar material terapéutico | Crítica |
| 3 | E02 | HU06 | Ejecutar sesión terapéutica | Crítica |
| 3 | E02 | HU07 | Monitorear movimiento corporal | Crítica |
| 3 | E02 | HU08 | Procesar movimiento corporal | Crítica |
| 3 | RNF | RNF05 | Consistencia del monitoreo corporal | Alta |
| 3 | RNF | RNF03 | Compatibilidad con dispositivos móviles | Alta |
| 4 | E02 | HU09 | Analizar ejecución terapéutica | Alta |
| 4 | E03 | HU10 | Generar retroalimentación inmediata | Crítica |
| 4 | E03 | HU11 | Visualizar resultados y % de ejecución | Crítica |
| 5 | E05 | HU15 | Registrar y gestionar recomendaciones | Alta |
| 5 | E05 | HU16 | Consultar recomendaciones terapéuticas | Alta |
| 5 | E04 | HU12 | Visualizar progreso y evolución | Media |
| 5 | E04 | HU13 | Consultar historial terapéutico | Media |
| 5 | E04 | HU14 | Monitorear cumplimiento terapéutico | Media |
| 6 | E06 | HU18 | Gestionar sesiones y resultados registrados | Alta |
| 6 | E06 | HU19 | Sincronizar información terapéutica | Media |
| 6 | RNF | RNF01 | Disponibilidad operativa del sistema | Crítica |
| 6 | RNF | RNF04 | Integridad de la información terapéutica | Alta |

---

## 7. Reglas del proyecto (no negociables)

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
