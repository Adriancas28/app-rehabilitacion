# Prompts para Claude Code — Guía visual en vivo (genérica para todos los ejercicios)

Úsalos en orden. Pega uno, revisa el resultado, prueba en el dispositivo y recién ahí pasa al siguiente.

---

## Etapa 0 — Contexto y análisis (sin escribir código)

```
Contexto del proyecto: app Android "SANNA — App Rehabilitación" (Kotlin, Jetpack Compose, MVVM + Repository, Hilt, Coroutines/Flow, CameraX, MediaPipe Pose Landmarker de 33 puntos, Firebase Firestore). El design system está cerrado en core/designsystem/ (ver CLAUDE.md): no dupliques componentes ni tokens, reutiliza los existentes.

Nueva funcionalidad: guía visual en vivo sobre la cámara durante la ejecución de CUALQUIER ejercicio de la app.

Requisito principal: la solución debe ser GENÉRICA. Debe funcionar para todos los ejercicios existentes y futuros, sin lógica específica por ejercicio, articulación o parte del cuerpo en el código. Todo lo que cambia entre ejercicios se define como datos.

Estructura: Ejercicio -> N repeticiones -> cada repetición contiene varias ACCIONES en orden.

Comportamiento esperado: desde que inicia cada acción se pinta sobre la cámara, anclado al cuerpo del paciente y en tiempo real:
- el recorrido que debe seguir el segmento corporal (arco desde la posición inicial hasta la meta),
- la parte del recorrido ya realizada (relleno de color),
- la zona meta hasta donde debe llegar (meta ± tolerancia),
- un segmento "fantasma" translúcido en la posición final,
- el segmento real del paciente con color según su desempeño (rojo / ámbar / verde).
La app sigue corrigiendo dentro de la misma acción hasta que el paciente la cumpla (o se agote su tiempo máximo); luego pasa automáticamente a la siguiente acción. Al terminar todas las acciones se cuenta la repetición y se reinicia la secuencia hasta completar el total.

Tarea de esta etapa: NO escribas código. Analiza el código actual y entrégame:
1. Dónde está hoy la pantalla de ejecución del ejercicio, su ViewModel, la integración CameraX + MediaPipe y cómo se calculan los ángulos.
2. Los modelos de datos actuales de ejercicio, sesión y repetición (Kotlin y Firestore).
3. Un plan de implementación por etapas con los archivos a crear y modificar.
4. Riesgos (mapeo de coordenadas, cámara frontal espejada, visibilidad de puntos, rendimiento).
Detente y espera mi confirmación.
```

---

## Etapa 1 — Modelo de datos genérico

```
Implementa el modelo de datos según el plan aprobado, sin asumir ninguna articulación concreta.

- Cada Accion define el segmento a medir con landmarks de MediaPipe (por índice o enum):
  puntoReferencia (fijo), pivote (articulación que gira), puntoIntermedio (opcional, solo para dibujar) y puntoDistal (extremo que se mueve).
- Accion: orden, nombre, puntoReferencia, pivote, puntoIntermedio?, puntoDistal, lado (izq/der), anguloInicio, anguloMeta, tolerancia, mantenerSeg, tiempoMaxSeg.
- Ejercicio: repeticiones: Int, acciones: List<Accion>, vistaCamara requerida (lateral/frontal/oblicua).
- Resultado por acción: ResultadoAccion(ordenAccion, anguloMaximo, completada, tiempoSeg, desviacionAngular), embebido dentro de cada repetición de la sesión.
- Actualiza mappers y repository de Firestore manteniendo compatibilidad con los ejercicios existentes: si un ejercicio no tiene acciones, genera una acción por defecto a partir de los datos de referencia que ya tenga.
- Los valores clínicos (ángulos, tolerancias, tiempos) NO se hardcodean; vienen de los datos del ejercicio.
No toques UI todavía. Resume cambios y espera mi confirmación.
```

---

## Etapa 2 — Mapeo de coordenadas y suavizado

```
Crea la capa que convierte los landmarks de MediaPipe a coordenadas de pantalla, válida para cualquier landmark.

- LandmarkMapper: recibe landmarks normalizados, tamaño de la imagen analizada, tamaño del PreviewView, rotación y si es cámara frontal. Debe replicar el ScaleType del PreviewView (escala + recorte) y espejar X con cámara frontal.
- OneEuroFilter (implementación propia) aplicado a los landmarks usados por la acción actual, con parámetros configurables.
- Si algún landmark requerido por la acción tiene visibility < 0.5, marcar el frame como "no confiable".
- Pruebas unitarias del mapper (cámara trasera, frontal, distintas relaciones de aspecto).
- Modo debug: dibujar todos los landmarks y el esqueleto sobre la cámara para validar que coinciden con el cuerpo.
Espera mi confirmación (lo probaré en dispositivo).
```

---

## Etapa 3 — Motor de cálculo genérico

```
Crea PoseGuideCalculator (lógica pura, sin dependencias de Android, testeable), sin asumir ninguna parte del cuerpo:

Entrada: landmarks mapeados y la Accion actual.
Salida (GuideFrame):
- anguloActual: ángulo del segmento pivote->distal respecto a la línea pivote->referencia.
- longitudSegmento: pivote->intermedio + intermedio->distal (o pivote->distal si no hay intermedio).
- puntoMeta: posición del segmento fantasma en anguloMeta, anclado al pivote y orientado según la línea de referencia.
- progreso (0..1) entre anguloInicio y anguloMeta; debe funcionar en ambos sentidos (meta mayor o menor que el inicio).
- enZona: |anguloActual - anguloMeta| <= tolerancia.
- estadoColor: VERDE / AMBAR / ROJO según la distancia a la meta (umbrales configurables).
Considera el lado y el espejo de cámara para el sentido de giro.
Pruebas unitarias con varias combinaciones de landmarks y sentidos. Espera mi confirmación.
```

---

## Etapa 4 — Máquina de estados (repetición → acción)

```
Implementa en el ViewModel de ejecución una máquina de estados genérica:

- Estado: repActual, indiceAccion, tiempoInicioAccion, tiempoEnZona, anguloMaximoAccion.
- En cada GuideFrame:
  - Si la acción tiene mantenerSeg > 0: debe permanecer enZona ese tiempo continuo (si sale, el contador se reinicia).
  - Si cumple: guardar ResultadoAccion(completada = true) y pasar a la siguiente acción.
  - Si supera tiempoMaxSeg: guardar ResultadoAccion(completada = false, anguloMaximo) y pasar a la siguiente.
  - Si no: seguir corrigiendo en la misma acción.
- Al terminar la última acción: cerrar la repetición, avanzar a la siguiente y volver a la primera acción. Al completar todas, finalizar la sesión.
- Frames no confiables no avanzan ni penalizan; se expone un estado para pedir al paciente reubicarse.
- Exponer StateFlow<GuideUiState>: acción actual, GuideFrame, repetición X/N, acción Y/M, segundos restantes de mantener, mensaje.
- Pruebas unitarias de transiciones con frames simulados.
Espera mi confirmación.
```

---

## Etapa 5 — Overlay visual en Compose (el recorrido pintado)

```
Crea el composable PoseGuideOverlay superpuesto al preview de CameraX (Box: preview abajo, Canvas encima). Dibuja SOLO la acción actual y funciona igual para cualquier segmento corporal:

1. Recorrido completo: arco punteado y translúcido de anguloInicio a anguloMeta, centrado en el pivote, radio = longitudSegmento + margen.
2. Recorrido realizado: el tramo del arco ya avanzado, relleno con el color de estado.
3. Zona meta: sector translúcido (meta ± tolerancia).
4. Segmento fantasma: línea gruesa translúcida desde el pivote hasta puntoMeta + marcador en el extremo ("aquí debes llegar").
5. Segmento real del paciente (pivote, intermedio, distal) con el color de estado.
6. HUD: "Repetición X/N · Acción Y/M · nombre de la acción", mensaje de corrección y contador de mantener.
7. Frame no confiable: ocultar la guía y mostrar un aviso para reubicarse según la vista de cámara del ejercicio.

Importante: el objetivo principal es que el paciente vea pintado sobre la cámara el recorrido que debe hacer en la acción actual y la zona hasta donde debe llegar, como una guía que sigue su cuerpo en tiempo real. Al completar una acción, la guía cambia automáticamente al recorrido de la siguiente.

Usa colores y tipografía de core/designsystem. Transiciones suaves entre acciones. Mantener ≥ 20 FPS evitando crear objetos innecesarios en cada frame.
Espera mi confirmación.
```

---

## Etapa 6 — Feedback háptico y voz

```
Agrega feedback multimodal genérico:
- Vibración corta al completar cada acción y más larga al completar una repetición.
- TextToSpeech en español con mensajes según el estado (sin repetir el mismo mensaje más de una vez cada 3 s). Los mensajes pueden usar el nombre de la acción definido en los datos.
- Opción en ajustes del paciente para activar/desactivar voz y vibración.
Servicios inyectados con Hilt detrás de interfaces. Espera mi confirmación.
```

---

## Etapa 7 — Persistencia, revisión y pruebas finales

```
- Guardar en Firestore los ResultadoAccion por repetición al cerrar cada repetición (respetando offline-first).
- En la vista del fisioterapeuta, mostrar por repetición qué acciones se completaron y el ángulo máximo alcanzado (reutiliza componentes del design system).
- Verificar que la guía funcione con TODOS los ejercicios registrados en la app.
- Revisar rendimiento (FPS, CPU) y documentar en CLAUDE.md la arquitectura (mapper, calculator, máquina de estados, overlay).
- Lista de pruebas manuales: cámara frontal y trasera, ambos lados del cuerpo, poca luz, paciente fuera de cuadro, acción no lograda.
Resume todo lo implementado.
```

---

## Consejos de uso

- Si una etapa queda grande, pídele "divide esta etapa en sub-pasos y haz solo el primero".
- Prueba en dispositivo real después de las etapas 2 y 5.
- Los ángulos, tolerancias y tiempos los define el fisioterapeuta en los datos de cada ejercicio.
