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
en Firebase Auth + Firestore antes de que alguien pueda iniciar sesión.

- **Pacientes y fisioterapeutas:** se crean directamente desde el panel de
  Administrador dentro de la propia app (rol `admin`, ver HU20/HU21 en
  [`CLAUDE.md`](./CLAUDE.md)) — ya no depende del script para el uso diario.
- **La primera cuenta de administrador** (bootstrap, sin UI propia) se crea
  con un script administrativo en `/backend`:

```bash
cd backend
npm install
GOOGLE_APPLICATION_CREDENTIALS=./service-account.json \
  npx ts-node seed/crear-usuario.ts \
  --nombre "Admin SANNA" --email admin@sanna.pe --rol admin
```

El mismo script sigue funcionando para `--rol paciente` (requiere
`--fisioterapeutaId <uid>`) y `--rol fisioterapeuta`, por si se prefiere
la vía de línea de comandos. Imprime la contraseña generada por consola;
se entrega al usuario por correo o WhatsApp.

## Roles

- **Paciente:** ejecuta ejercicios asignados y recibe retroalimentación en tiempo real.
- **Fisioterapeuta:** asigna rutinas y hace seguimiento del progreso de sus pacientes.
- **Administrador:** gestiona las cuentas de pacientes y fisioterapeutas
  (alta, edición, baja) y asigna el fisioterapeuta responsable de cada
  paciente, sin depender de herramientas de línea de comandos.

## Design System

Todas las pantallas siguen un Design System único (paleta, tipografía,
espaciado e iconografía) con componentes reutilizables en
`app/core/designsystem/`. Antes de tocar una pantalla o crear un
componente nuevo, revisar el inventario en la sección 3 de
[`CLAUDE.md`](./CLAUDE.md).
