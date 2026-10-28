# Pruebas locales con los emuladores de Firebase

Permite probar la app (por ejemplo la pantalla del fisioterapeuta) sin tocar el proyecto
Firebase real. Todo corre en tu PC: Authentication (9099) y Firestore (8080). Las cuentas y los
datos son ficticios y viven solo en memoria mientras los emuladores estén encendidos.

## 1. Iniciar los emuladores (terminal 1, desde `/backend`)

```bash
npx -y firebase-tools@latest emulators:start --only auth,firestore --project app-rehabilitacion-terapeutica
```

Usa `backend/firebase.json` (bloque `emulators`) y las reglas de `firestore.rules`. No despliega nada.
Necesita Java. `firebase-tools` se descarga con `npx` y no se instala en el repositorio.

## 2. Sembrar datos ficticios (terminal 2, desde `/backend`)

```bash
FIRESTORE_EMULATOR_HOST=127.0.0.1:8080 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9099 node emuladores/sembrar-emulador.js
```

El script se niega a ejecutarse si esas variables no apuntan a localhost, y no carga
`service-account.json`, así que no puede alcanzar el proyecto real. Crea:

| Rol | Correo | Contraseña |
|---|---|---|
| Fisioterapeuta | `fisio.prueba@local.test` | `PruebaLocal123` |
| Paciente (Elmer Prueba) | `elmer.prueba@local.test` | `PruebaLocal123` |

Más el ejercicio "Abducción de hombro" y una sesión completada al 100 %. Son cuentas locales
que no existen en el proyecto real.

## 3. Compilar e instalar la app de depuración apuntando al emulador

```bash
cd app   # o desde la raíz del repo
gradlew :app:assembleDebug -PusarEmuladorFirebase=true
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb reverse tcp:9099 tcp:9099
adb reverse tcp:8080 tcp:8080
```

- `-PusarEmuladorFirebase=true` pone `BuildConfig.USAR_EMULADOR_FIREBASE = true` solo en el build
  **debug**. Sin la propiedad vale `false`. En **release** es siempre `false` (constante en
  `app/build.gradle.kts`), así que no hay forma de activarlo en producción.
- `RehabApplication` conecta Auth y Firestore a `localhost` cuando el campo es `true`.
- El permiso para HTTP en claro hacia `localhost` está en `app/src/debug/` (solo debug).
- Si la app ya tenía una sesión del Firebase real, bórrala (`adb shell pm clear com.sanna.rehabapp`).
- El emulador de Auth devuelve "No se pudo iniciar sesión" si el `adb reverse` no está activo.

## 4. Flujo probado

1. Entrar como `fisio.prueba@local.test` → Resultados → Elmer Prueba → sesión completada.
2. Escribir y guardar una recomendación.
3. Cerrar sesión, entrar como `elmer.prueba@local.test` → Mis resultados → la sesión: se ve la
   recomendación.
4. Volver al fisioterapeuta → "Ver todas las recomendaciones" → eliminarla.

## 5. Volver a la configuración normal

```bash
gradlew :app:assembleDebug        # sin -PusarEmuladorFirebase
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell pm clear com.sanna.rehabapp   # quita la sesión local; luego inicia sesión en el Firebase real
adb reverse --remove-all
```

Detén los emuladores con Ctrl+C en la terminal 1.
