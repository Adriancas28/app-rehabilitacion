# Demo de 3 minutos — OE1 y OE2

**Objetivos que se muestran**
- **OE1.** Registro automatizado de ejercicios domiciliarios para almacenar información objetiva sobre la ejecución realizada por los pacientes.
- **OE2.** Análisis postural automatizado mediante visión computacional y estimación de pose para evaluar la ejecución de ejercicios domiciliarios.

**Datos de la demo**
- Paciente: **Rosa Vargas** (`rosa.vargas@correo.com`), hombro doloroso. Fisioterapeuta: **Dr. Carlos Mendoza** (`carlos.mendoza@sanna.pe`). Contraseñas: las de demostración que ya conoces.
- Ejercicio: **Abducción de hombro** (frontal, rango esperado 70°–110°, hombro derecho). Sesión pendiente ya preparada: **2 repeticiones de 8 s**, con nota del fisio.
- Las 3 sesiones anteriores de Rosa son **datos de demostración**; la nueva será una medición real. Dilo en la narración.

## Preparación (antes de grabar)
1. Laptop a la altura de los ojos, cámara apuntando a ti: torso y brazos completos en cuadro, a unos 2 m, de frente y con la luz delante (no a contraluz). Fondo liso.
2. Emulador abierto con la app en el login. Cierra el resto de ventanas y notificaciones.
3. **Ensayo sin grabar:** haz la sesión completa una vez para ver que detecta tu brazo derecho (si mide el izquierdo, levanta el otro brazo; en el ensayo se nota cuál mide). Esa sesión de ensayo se puede dejar; para la toma final pídeme reponer la sesión pendiente.
4. Graba con OBS (fuente: ventana del emulador) o con la barra de juegos de Windows (Win+G), con tu micrófono. Graba **por clips** y únelos después.

## Guion por clips (≈ 3:00)

| Clip | Tiempo | Pantalla | Narración (sugerida) |
|---|---|---|---|
| 1 | 0:00–0:12 | Título con OE1 y OE2 → login como Rosa | "Esta demo muestra los dos primeros objetivos: el registro automatizado de la ejecución y el análisis postural automatizado con estimación de pose." |
| 2 | 0:12–0:30 | Ejercicios → detalle de Abducción de hombro (video, ángulo objetivo 70°–110°, nota del fisio) | "La paciente ve su ejercicio asignado con el video de referencia, el ángulo objetivo y la indicación de su fisioterapeuta." |
| 3 | 0:30–1:05 | "Iniciar sesión" → cuenta regresiva → **tú haces la abducción**; la repetición 2, incompleta a propósito | "MediaPipe estima la pose en el propio dispositivo, calcula el ángulo del hombro en cada cuadro y avisa en tiempo real si la postura es correcta o hay que corregir." |
| 4 | 1:05–1:35 | "Sesión completada" → resultado (repeticiones y % de cada una) → Mis resultados → **Progreso** (línea y barras) | "Al terminar, el sistema registra la sesión sin intervención manual: repeticiones, porcentaje de ejecución y evolución sesión a sesión. Las tres primeras son de demostración; la última es esta medición real." |
| 5 | 1:35–1:45 | Firebase Console → colección `sesiones` → el documento nuevo (repeticiones con ángulos) | "Esta es la información objetiva almacenada: el resultado por repetición queda guardado en la base de datos." |
| 6 | 1:45–2:35 | Login del Dr. Mendoza → Resultados → Rosa → sesión nueva → **detalle por repetición** (ángulo detectado vs esperado, segundo del error) → gráfico → "Ver video de la sesión" | "El fisioterapeuta ve, repetición por repetición, el ángulo detectado frente al esperado, el tipo de error y el segundo en que ocurrió, y puede revisar el video de la sesión." |
| 7 | 2:35–2:55 | Extras (5–7 s cada uno): asignar sesión con ángulo personalizado · recomendación al paciente · dashboard del administrador | "Además: ajuste del ángulo objetivo por paciente, recomendaciones y panel del administrador." |
| 8 | 2:55–3:00 | Cierre | "Registro automatizado y análisis postural con IA, funcionando de extremo a extremo." |

## Consejos para que salga bien
- **Ritmo:** la cuenta regresiva de 10 s y las pausas se pueden recortar al editar; el tiempo neto de la sesión son ~30 s.
- **Errores visibles:** para que el detalle del fisio muestre un error, en la repetición 2 sube el brazo solo hasta la mitad (por debajo de 70°).
- **Edición:** CapCut o Clipchamp: une los clips, pon rótulos "OE1" y "OE2" en las partes correspondientes y música baja opcional.
- **Honestidad:** no presentes las sesiones sembradas como pacientes reales; la narración del clip 4 ya lo aclara.
- **Voz correctiva:** en el emulador puede no sonar (no se ha confirmado); no dependas de ella en la narración.
