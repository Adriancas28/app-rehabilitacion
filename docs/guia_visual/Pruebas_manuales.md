# Guía visual en vivo — lista de pruebas manuales

Estado al cierre de las etapas 0–7: la lógica (cálculo, máquina de estados, resultado por
acciones, mapeo, filtro) tiene pruebas unitarias automáticas (124+, todas verdes). La
interfaz (overlay, HUD, vibración, voz, Perfil) **no** se ha probado aún con una persona
frente a la cámara; esta lista es para hacerlo.

## Preparación
1. Emulador Pixel_4a con la webcam como cámara frontal (`hw.camera.front=webcam0`) o un
   teléfono real.
2. Una cuenta de paciente con fisioterapeuta asignado y una sesión asignada de un ejercicio
   con acciones (los 12 del catálogo ya las tienen sembradas).

## Casos
| # | Caso | Resultado esperado |
|---|---|---|
| 1 | Abrir la sesión | Cámara grande, descripción y botón "Iniciar sesión" debajo (el botón responde al toque). |
| 2 | Iniciar | Cuenta regresiva de 10 s; no se dibuja la guía ni se mide. |
| 3 | Colocarse según la vista (frontal/lateral/oblicua) | Arco punteado, zona meta verde, segmento fantasma con marcador y segmento real sobre el cuerpo; el segmento sigue la extremidad sin desfase visible. |
| 4 | Mover la extremidad hacia la meta | Color rojo → ámbar → verde; el tramo recorrido se pinta. |
| 5 | Acción con "mantener" | HUD muestra "Mantén N s" y baja solo mientras se está en zona; salir de la zona lo reinicia. |
| 6 | Pasarse de la meta | Mensaje "Te pasaste, regresa un poco." |
| 7 | Salir de cuadro / tapar la cámara | Mensaje de reubicarse; el tiempo de la acción no corre; tras 20 s la acción se cierra incompleta; tras 60 s la sesión termina con aviso. |
| 8 | Completar una acción | Vibración corta; pasa a la siguiente. |
| 9 | Completar una repetición | Vibración larga; descanso de 5 s con aviso por voz a los 3 s. |
| 10 | Perfil → "Guía durante el ejercicio" | Desactivar voz: no hay voz ni lectura de instrucciones. Desactivar vibración: no vibra. Los valores persisten al reabrir la app. |
| 11 | "Finalizar ejercicio" a mitad | Se guarda resultado parcial; la sesión queda reanudable. |
| 12 | Reanudar | Continúa en la repetición siguiente y combina el resultado anterior con el nuevo. |
| 13 | Fisioterapeuta → resultado de la sesión | Cada repetición muestra sus acciones (completada/incompleta, ángulo máximo) y los errores con ángulo detectado vs meta. |
| 14 | Rendimiento | Overlay fluido (≥ 20 FPS percibidos) sin que la latencia de la guía supere los 500 ms. |
| 15 | Los 12 ejercicios | Abrir cada uno y confirmar que la guía aparece sobre la articulación correcta del lado afectado. |

## Depuración
En builds debug, mantener presionada la insignia "Cámara en vivo" activa el esqueleto de 33
puntos para comprobar el mapeo de coordenadas.
