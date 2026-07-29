# App Rehabilitación (SANNA)

Aplicación móvil con Inteligencia Artificial para análisis postural en el
monitoreo de ejercicios domiciliarios de rehabilitación musculoesquelética
(Clínica SANNA). El análisis de postura (MediaPipe Pose) corre **en el
dispositivo** (Edge AI): no se procesa ni se almacena video en la nube, solo
métricas numéricas (ángulos, vectores).

> El contexto completo del proyecto (arquitectura, stack, modelo de datos,
> historias de usuario, RNF y reglas del proyecto) vive en [`CLAUDE.md`](./CLAUDE.md).

## Estructura del repositorio

Monorepo con dos carpetas raíz independientes:

- **`/backend`** — configuración de Firebase (BaaS): Firestore, Storage,
  Security Rules. No existe un servidor propio.
- **`/app`** — proyecto Android nativo (Kotlin + Jetpack Compose + Hilt).

## Requisitos

- Android Studio (SDK mínimo API 29 / Android 10) para `/app`.
- [Firebase CLI](https://firebase.google.com/docs/cli) (`npm install -g firebase-tools`) para `/backend`.
- Cuenta de Firebase con un proyecto creado y enlazado en `backend/.firebaserc`.

## Cómo empezar

1. Abrir la carpeta `/app` en Android Studio y esperar el sync de Gradle.
2. Configurar Firebase: `firebase login` y `firebase use --add` dentro de `/backend`.
3. Descargar `google-services.json` desde la consola de Firebase y colocarlo
   en `/app` (no se versiona, ver `.gitignore`).

## Alta de usuarios (pacientes y fisioterapeutas)

La app **no tiene pantalla de auto-registro**: las cuentas ya deben existir
en Firebase Auth + Firestore antes de que alguien pueda iniciar sesión. Se
crean con un script administrativo en `/backend`, nunca desde el propio
dispositivo del paciente o fisioterapeuta:

```bash
cd backend
npm install
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json \
  npx ts-node seed/crear-usuario.ts \
  --nombre "Juan Pérez" --email juan.perez@correo.com \
  --rol paciente --fisioterapeutaId <uid-del-fisioterapeuta>
```

El script imprime la contraseña generada por consola; se entrega al
usuario por correo o WhatsApp. Para un fisioterapeuta se omite
`--fisioterapeutaId` y se usa `--rol fisioterapeuta`.

## Roles

- **Paciente:** ejecuta ejercicios asignados y recibe retroalimentación en tiempo real.
- **Fisioterapeuta:** asigna rutinas y hace seguimiento del progreso de sus pacientes.
