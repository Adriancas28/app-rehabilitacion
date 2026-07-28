/**
 * Crea un usuario (paciente o fisioterapeuta) en Firebase Auth + su
 * documento en Firestore, con una contraseña generada aleatoriamente.
 *
 * No hay pantalla de auto-registro en la app: las cuentas se dan de alta
 * con este script y la contraseña se entrega manualmente (correo o
 * WhatsApp) a quien corresponda.
 *
 * Requiere: `npm install` (en /backend) y la variable de entorno
 * GOOGLE_APPLICATION_CREDENTIALS apuntando a una service account con
 * permisos de Firebase Auth + Firestore.
 *
 * Uso:
 *   npx ts-node seed/crear-usuario.ts --nombre "Juan Perez" \
 *     --email juan.perez@correo.com --rol paciente --fisioterapeutaId <uid>
 *
 *   npx ts-node seed/crear-usuario.ts --nombre "Dra. Ana Ruiz" \
 *     --email ana.ruiz@sanna.pe --rol fisioterapeuta
 */
import * as admin from "firebase-admin";
import * as crypto from "crypto";

interface Argumentos {
  nombre: string;
  email: string;
  rol: "paciente" | "fisioterapeuta";
  fisioterapeutaId?: string;
}

function parsearArgumentos(argv: string[]): Argumentos {
  const valores: Record<string, string> = {};
  for (let i = 0; i < argv.length; i += 1) {
    const actual = argv[i];
    if (actual.startsWith("--")) {
      valores[actual.slice(2)] = argv[i + 1];
      i += 1;
    }
  }

  const { nombre, email, rol, fisioterapeutaId } = valores;

  if (!nombre || !email || !rol) {
    throw new Error(
      'Uso: ts-node crear-usuario.ts --nombre "..." --email ... ' +
        "--rol paciente|fisioterapeuta [--fisioterapeutaId <uid>]",
    );
  }
  if (rol !== "paciente" && rol !== "fisioterapeuta") {
    throw new Error('--rol debe ser "paciente" o "fisioterapeuta"');
  }
  if (rol === "paciente" && !fisioterapeutaId) {
    throw new Error("--fisioterapeutaId es obligatorio cuando --rol es paciente");
  }

  return { nombre, email, rol, fisioterapeutaId };
}

function generarContrasena(): string {
  return crypto.randomBytes(9).toString("base64").replace(/[/+=]/g, "x");
}

async function crearUsuario(argumentos: Argumentos): Promise<void> {
  admin.initializeApp();

  const contrasena = generarContrasena();

  const usuarioAuth = await admin.auth().createUser({
    email: argumentos.email,
    password: contrasena,
    displayName: argumentos.nombre,
  });

  const datosFirestore: Record<string, unknown> = {
    nombre: argumentos.nombre,
    email: argumentos.email,
    rol: argumentos.rol,
    fechaRegistro: admin.firestore.FieldValue.serverTimestamp(),
  };
  if (argumentos.rol === "paciente") {
    datosFirestore.fisioterapeutaId = argumentos.fisioterapeutaId;
  }

  await admin.firestore().collection("usuarios").doc(usuarioAuth.uid).set(datosFirestore);

  console.log("Usuario creado correctamente:");
  console.log(`  uid:        ${usuarioAuth.uid}`);
  console.log(`  nombre:     ${argumentos.nombre}`);
  console.log(`  email:      ${argumentos.email}`);
  console.log(`  rol:        ${argumentos.rol}`);
  console.log(`  contraseña: ${contrasena}`);
  console.log("");
  console.log("Entrega estas credenciales por un canal seguro (correo o WhatsApp).");
  console.log("No quedan guardadas en ningún otro lugar: si se pierden, hay que resetear la contraseña.");
}

crearUsuario(parsearArgumentos(process.argv.slice(2)))
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("Error al crear el usuario:", error?.message ?? error);
    process.exit(1);
  });
