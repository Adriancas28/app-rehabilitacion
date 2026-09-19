# Casos de prueba QA – Rol Administrador y Autenticación

Proyecto SANNA Rehabilitación · rama `mejora-videos` · emulador Android (Pixel 4a, API 30) · build instalado · fecha 2026-09-19.
Verificado contra código real (`feature/admin/*`, `feature/auth/*`, `core/navigation/*`) y contra Firestore/Auth mediante scripts de solo lectura. Datos de prueba: cuentas QA creadas por el tester (`qa.paciente1@correo.com`, `qa.paciente2@correo.com`, `qa.fisio1@sanna.pe`, `qa.fisio2@sanna.pe`, clave `•••`).

> **INCIDENTE DURANTE LA PRUEBA (leer primero).** Al limpiar las cuentas QA desde la app, la lista de fisioterapeutas se reordenó entre eliminaciones y el tester **eliminó por error a "Dr. Carlos Mendoza"** (fisioterapeuta real, uid `BAAAohiPXvSzhvmWaDByacSAgu82`). Se borraron sus documentos `usuarios/{uid}` y `fisioterapeutas/{uid}`; su cuenta de Firebase Auth sigue existiendo y los pacientes Luis Ramírez y Rosa Vargas conservan su `fisioterapeutaId`. Se intentó restaurarlo con un script y el permiso fue **denegado** (modificaba datos fuera del alcance autorizado), por lo que **NO está restaurado**. Estado final: 5 pacientes, 1 fisioterapeuta (Ana Ruiz) y 1 admin. Hay que recrear `usuarios/BAAAoh…` (correo `carlos.mendoza@sanna.pe`, nombre `Dr. Carlos Mendoza`, rol `fisioterapeuta`, `activo: true`) y `fisioterapeutas/BAAAoh…` (posiblemente vacío: la cuenta se creó el 2026-09-19 04:28 GMT con `crear-usuario.ts`, que escribe `{}`), y volver a comprobar sus datos con el equipo. Ver también ERR-ADM-004 (causa raíz: orden inestable + eliminación sin salvaguarda).

## 1. Casos de prueba

| ID | HU relacionada | Funcionalidad | Precondiciones | Pasos | Datos utilizados | Resultado esperado | Resultado obtenido | Estado | Evidencia |
|---|---|---|---|---|---|---|---|---|---|
| CP-AUT-001 | RNF02-CA01 | Login con campos vacíos | App en login, sin sesión | Abrir app; mirar botón; escribir solo el correo; tocar el botón | correo `admin@sanna.pe`, clave vacía | Botón deshabilitado hasta completar ambos campos | Botón en gris; el toque no hace nada ni muestra error | PASS | comun/C01_login.png |
| CP-AUT-002 | RNF02-CA01 | Contraseña incorrecta | Login | Correo válido + clave errónea, Enter | `admin@sanna.pe` / `ClaveMala1` | "No se pudo iniciar sesión. Verifica tus credenciales." | Mensaje exacto mostrado | PASS | comun/C02_login_error.png |
| CP-AUT-003 | RNF02-CA01 | Correo inexistente | Login | Correo no registrado + clave | `noexiste@sanna.pe` / `Clave12345` | Mismo mensaje de credenciales | Mismo mensaje (no revela si el correo existe) | PASS | log (uiautomator) |
| CP-AUT-004 | RNF02-CA01 | Correo con formato inválido | Login | Escribir texto sin `@`, Enter | `adminsanna` / `Clave12345` | Mensaje de error | Mensaje genérico de credenciales (no indica formato inválido) | PASS (ver ERR-ADM-011) | log |
| CP-AUT-005 | RNF02-CA01 | Ver/ocultar contraseña | Login | Campo clave por defecto; activar el ojo | `•••` | Puntos por defecto; texto visible al activar el ojo | Puntos por defecto confirmados (C01/screens); el texto visible se observó una vez tras activar el control con Tab+Enter sin querer; no se hizo una prueba dirigida al toque del ojo | PASS (parcial) | log |
| CP-AUT-006 | RNF02-CA01 | Correo con espacios alrededor | Login | Escribir " admin@sanna.pe " + clave | `•••` | Se recorta y entra (código usa `trim`) | Ingresó al panel admin | PASS | log |
| CP-AUT-007 | RNF02-CA02 | Redirección por rol admin | Login | Iniciar sesión como admin | `admin@sanna.pe` / `•••` | Panel de administrador | Panel admin, pero aterriza en "Pacientes" y no en "Dashboard" (primera pestaña, A01) | FAIL (inconsistencia menor, ERR-ADM-010) | admin/A04_pacientes.png |
| CP-AUT-008 | RNF02-CA04 | Cerrar sesión – cancelar | Admin logueado | Menú lateral > Cerrar sesión > Cancelar | — | Diálogo "¿Seguro que deseas cerrar sesión?", permanece en la pantalla | Diálogo correcto; al cancelar sigue en el Dashboard | PASS | admin/A13_dialogo_cerrar_sesion.png |
| CP-AUT-009 | RNF02-CA04 | Cerrar sesión – confirmar | Admin logueado | Menú > Cerrar sesión > Cerrar sesión | — | Vuelve al login, campos vacíos | Vuelve al login sin datos | PASS | log |
| CP-AUT-010 | RNF02 / HU20-CA08 | Cuenta de paciente desactivada no entra | Paciente QA desactivado por el admin | Cerrar sesión; login con esa cuenta | `qa.paciente1@correo.com` | No permite el acceso | No accede: regresa al login | PASS | CP-AUT-011.png |
| CP-AUT-011 | RNF02 / HU20-CA08 | Aviso al usuario desactivado | Igual que arriba | Observar pantalla tras Enter | idem | Mensaje que explique que la cuenta está desactivada | Vuelve al login con campos vacíos y **sin ningún mensaje** | FAIL (ERR-ADM-009) | admin/evidencia/CP-AUT-011.png |
| CP-AUT-012 | HU20-CA08 | Reactivar cuenta y volver a entrar | Paciente QA desactivado | Admin > Activar > confirmar; login con la cuenta | idem | Entra a la app del paciente | Mensaje "Se activó a QA Paciente Uno."; entró ("Hola, QA") | PASS | log |
| CP-AUT-013 | HU21-CA05 / RNF02 | Fisio desactivado no entra | Fisio QA desactivado | Login con esa cuenta | `qa.fisio1@sanna.pe` | No accede | No accede, vuelve al login sin mensaje (mismo defecto ERR-ADM-009) | PASS (bloqueo) | log |
| CP-AUT-014 | HU21-CA05 | Reactivar fisio y entrar | Fisio QA desactivado | Admin > Activar > confirmar; login | idem | Entra al panel del fisio | Entró; panel del fisio mostró 2 pacientes activos | PASS | log |
| CP-AUT-015 | RNF02-CA01 | Persistencia de sesión al reabrir | Admin logueado | Cerrar el proceso de la app y volver a abrir | — | Sigue autenticado en el panel | Reabre directamente en "Pacientes" | PASS | log |
| CP-AUT-016 | Navegación | Girar la pantalla | Dashboard de paciente abierto | Rotar a horizontal y volver | — | No pierde sesión ni pantalla | Sin pérdida de estado | PASS | log |
| CP-AUT-017 | RNF06-CA04 | Consentimiento solo para paciente | App sin datos | Login paciente activo; login admin | paciente QA / admin | Paciente ve consentimiento; admin no | Paciente ve la pantalla; admin entra directo al panel | PASS | comun/C03_consentimiento.png |
| CP-ADM-001 | Etapa 2A | Dashboard: lista de pacientes | Admin logueado | Menú > Dashboard | 5 pacientes reales | Lista de 5 pacientes con diagnóstico y estado | 5 filas con "· Activo"; Elmer castro sin diagnóstico (solo "Activo") | PASS | admin/A01_dashboard_lista.png |
| CP-ADM-002 | Etapa 2A | Dashboard de Juan Pérez | Dashboard | Tocar Juan Pérez | — | 10 sesiones, ~76 % | 10 sesiones, 76 %, lista con % correctas y % completado | PASS | admin/A02_dashboard_paciente.png |
| CP-ADM-003 | Etapa 2A | Coherencia de cifras (resto) | Dashboard | Abrir cada paciente; comparar con Firestore (script solo lectura) | Firestore `sesiones` | María 3, Luis 2, Rosa 3, Elmer 4 (completadas) | App: Elmer 4/83 %, Luis 2/75 %, María 3/71 %, Rosa 3/72 %; Firestore: 4/82,8; 2/74,8; 3/70,7; 3/71,6. Coinciden (redondeo) | PASS | log |
| CP-ADM-004 | Etapa 2A | Dashboard: gráficos | Dashboard de Juan | "Mostrar gráfico" | — | Tendencia de precisión (línea) y % completado (barras) | Ambos gráficos con 10 sesiones; botón cambia a "Mostrar lista" | PASS | admin/A03_dashboard_grafico.png |
| CP-ADM-005 | Etapa 2A | Paciente sin sesiones | Paciente QA sin sesiones | Dashboard > QA Paciente Uno; probar "Mostrar gráfico" | — | "Este paciente todavía no ejecutó ninguna sesión." | Mensaje exacto; 0 y 0 %; el mensaje se mantiene en modo gráfico | PASS | admin/evidencia/CP-ADM-005.png |
| CP-ADM-006 | HU20-CA01 | Lista de pacientes | Admin | Menú > Pacientes | — | Lista con fisioterapeuta asignado o botón asignar | Lista correcta; el orden no es alfabético ni cronológico (ERR-ADM-004) | PASS | admin/A04_pacientes.png |
| CP-ADM-007 | HU20-CA02 | Alta de paciente con todos los campos | Formulario abierto | Completar nombre, correo, clave, DNI, edad, género, contacto, lado, diagnóstico; Crear | QA Paciente Uno / `70000001` / 45 / Femenino / `987654321` / Izquierdo / Rodilla | Cuenta creada y visible en la lista | Creada; Firestore confirmó dni, edad, género, contacto, lado y activo:true | PASS | admin/A05_paciente_alta.png |
| CP-ADM-008 | HU20-CA02 | Formulario vacío | Formulario nuevo | Tocar "Crear cuenta" sin datos | — | Error de campos requeridos | "Completa todos los campos requeridos." (no se probó cada campo vacío por separado; el código usa una sola condición para todos) | PASS | admin/evidencia/CP-ADM-009.png |
| CP-ADM-009 | HU20-CA02 | Sin diagnóstico | Formulario completo salvo diagnóstico | Crear cuenta | — | Error | "Completa todos los campos requeridos." | PASS | admin/evidencia/CP-ADM-010.png |
| CP-ADM-010 | HU20-CA02 | Edad 0 | Formulario completo | Edad `0`, Crear | 0 | Rechazo | Mensaje de campos requeridos | PASS | admin/evidencia/CP-ADM-011.png |
| CP-ADM-011 | HU20-CA02 | Edad negativa | idem | Edad `-5` | -5 | Rechazo | Mensaje de campos requeridos | PASS | log |
| CP-ADM-012 | HU20-CA02 | Edad con texto | idem | Edad `abc` (inyectado por adb; el teclado real es numérico) | abc | Rechazo | Mensaje de campos requeridos; el campo sí aceptó las letras (sin filtro de entrada) | PASS | log |
| CP-ADM-013 | HU20-CA02 | Edad fuera de rango (200) | Formulario completo | Edad `200`, Crear | 200 | Rechazo por rango no válido | Cuenta creada con edad 200 | FAIL (ERR-ADM-001) | admin/evidencia/CP-ADM-014.png |
| CP-ADM-014 | HU20-CA02 | DNI muy corto / con letras | idem | DNI `AB` | AB | Rechazo (DNI de 8 dígitos) | Cuenta creada con DNI "AB"; DNI muy largo no se probó por separado | FAIL (ERR-ADM-001) | admin/evidencia/CP-ADM-014.png |
| CP-ADM-015 | HU20-CA02 | DNI duplicado | Paciente con DNI 70000001 existente | Editar otro paciente y poner DNI `70000001` | 70000001 | Rechazo | Se guardó; Firestore mostró dos pacientes con el mismo DNI | FAIL (ERR-ADM-002) | log (Firestore) |
| CP-ADM-016 | HU20-CA02 | Contacto con letras | Formulario completo | Contacto `xyz`, Crear | xyz | Rechazo | Cuenta creada con contacto "xyz" | FAIL (ERR-ADM-001) | admin/evidencia/CP-ADM-014.png |
| CP-ADM-017 | HU20-CA02 | Contraseña corta | Formulario completo | Clave `123` | 123 | Rechazo con mensaje claro | Rechazo (lo hace Firebase) con mensaje genérico "No se pudo guardar. Verifica los datos e intenta de nuevo." | PASS (ver ERR-ADM-011) | admin/evidencia/CP-ADM-017.png |
| CP-ADM-018 | HU20-CA02 | Correo con formato inválido | Formulario completo | Correo `qa.paciente2` | — | Rechazo con mensaje claro | Rechazo genérico (Firebase) | PASS (ver ERR-ADM-011) | admin/evidencia/CP-ADM-018.png |
| CP-ADM-019 | HU20-CA02 | Correo duplicado | Correo ya registrado | Crear con `qa.paciente1@correo.com` | — | Rechazo indicando correo en uso | Rechazo genérico, sin decir que el correo ya existe | PASS (ver ERR-ADM-011) | admin/evidencia/CP-ADM-019.png |
| CP-ADM-020 | HU20-CA03 | Edición de paciente y persistencia | QA Paciente Uno | Editar; cambiar nombre y lado a "Ambos"; guardar; reabrir; restaurar | nombre +" Ed", lado Ambos | Cambios persisten | Persistieron; se restauró nombre y lado "Izquierdo" (Firestore lo confirmó). El correo aparece deshabilitado en edición | PASS | admin/A06_paciente_edicion.png |
| CP-ADM-021 | HU20-CA07 | Lado afectado guardado y recargado | Alta con Izquierdo | Reabrir edición; cambiar a Ambos | — | Valor se guarda y se recarga | Izquierdo y Ambos se recargaron bien | PASS | admin/A06_paciente_edicion.png |
| CP-ADM-022 | HU20-CA04 | Eliminar paciente – cancelar | Paciente QA | Menú > Eliminar > Cancelar | — | No se elimina | Sigue en la lista | PASS | admin/A07_menu_paciente.png |
| CP-ADM-023 | HU20-CA04 | Eliminar paciente – confirmar | idem | Eliminar > confirmar | QA Duplicado, QA Paciente Uno | Se elimina y se avisa | "Se eliminó a …" y desaparecen. La cuenta de Firebase Auth queda viva (comprobado por script) | PASS (ver ERR-ADM-006) | log |
| CP-ADM-024 | HU20-CA05 | Asignar fisioterapeuta | Paciente QA sin fisio | "Asignar fisioterapeuta" > elegir QA Fisio Uno | — | Se asigna y el botón desaparece | "Se asignó a QA Fisio Uno."; la tarjeta muestra el nombre y ya no hay botón | PASS | admin/A09_asignar_fisio.png |
| CP-ADM-025 | HU20-CA06 | Nombre del fisio y conteo | Asignaciones hechas | Ver tarjetas de pacientes y de fisios | — | Nombre del fisio en el paciente; nº de pacientes en el fisio | Carlos 2, Ana 3 (reales), QA Fisio 0 → 2; coincide | PASS | admin/A10_fisioterapeutas.png |
| CP-ADM-026 | HU20-CA08 | Desactivar – cancelar / confirmar | Paciente QA activo | Menú > Desactivar > Cancelar; otra vez > Desactivar | — | Solo desactiva al confirmar; badge "Inactivo" | Cancelar no cambió nada; al confirmar: "Se desactivó a …" e "Inactivo" | PASS | admin/A08_dialogo_desactivar.png |
| CP-ADM-027 | HU20-CA08 | Activar con confirmación | Paciente inactivo | Menú > Activar > confirmar | — | Vuelve a "Activo" | Correcto | PASS | log |
| CP-ADM-028 | HU20-CA01 | Menú del paciente | Lista | Tocar ⋮ | — | Editar, Desactivar/Activar, Eliminar | Las 3 opciones | PASS | admin/A07_menu_paciente.png |
| CP-ADM-029 | HU21-CA01 | Lista de fisioterapeutas | Admin | Menú > Fisioterapeutas | — | Lista con nº de pacientes | Correcto | PASS | admin/A10_fisioterapeutas.png |
| CP-ADM-030 | HU21-CA02 | Alta de fisio: opcionales vacíos | Formulario | Completar obligatorios; dejar especialidad y colegiatura vacías | QA Fisio Uno, `qa.fisio1@sanna.pe`, 38, Masculino, `911222333` | Se crea | Creada (Firestore: sin especialidad ni colegiatura) | PASS | admin/A11_fisio_formulario.png |
| CP-ADM-031 | HU21-CA02 | Fisio: formulario vacío | Formulario | Crear sin datos | — | Error | "Completa todos los campos requeridos." | PASS | log |
| CP-ADM-032 | HU21-CA02 | Fisio: validaciones de formato | Formulario | Mismo código que paciente (edad ≤ 0 rechazada; sin validar rango, contacto, colegiatura) | — | Validar edad y contacto | Solo se validó por lectura de código (`AdminFisioterapeutaFormViewModel`); no se creó ninguna cuenta extra para no ensuciar datos | BLOQUEADO (no ejecutado) | log |
| CP-ADM-033 | HU21-CA03 | Edición de fisio y persistencia | QA Fisio Uno | Editar; añadir colegiatura; guardar | `KinesiologiavCOL1234` | Persiste | Firestore mostró numeroColegiatura; especialidad quedó "". Correo deshabilitado | PASS | log |
| CP-ADM-034 | HU21-CA05 | Desactivar / activar fisio | Fisio QA | Desactivar (cancelar y confirmar), luego Activar | — | Cambia estado con confirmación | Correcto ("Inactivo" y vuelta a activo) | PASS | log |
| CP-ADM-035 | HU21-CA04 | Eliminar fisio – cancelar / confirmar (sin pacientes) | Fisio QA | Eliminar > cancelar; luego confirmar | QA Fisio Doble | No borra al cancelar; borra al confirmar | Correcto | PASS | admin/evidencia/CP-ADM-036a.png |
| CP-ADM-036 | HU21-CA04 / HU20-CA05 | Eliminar fisio con pacientes asignados | QA Fisio Uno con 2 pacientes QA | Eliminar > confirmar; ir a Pacientes | — | Impedir o avisar; los pacientes no quedan huérfanos | Se elimina sin advertencia. Los pacientes muestran "Fisioterapeuta: —", sin botón asignar (HU20-CA05 lo oculta si ya hay fisioterapeutaId), por lo que no se puede reasignar desde la app | FAIL (ERR-ADM-003) | admin/evidencia/CP-ADM-036.png |
| CP-ADM-037 | Navegación | Doble toque en "Crear cuenta" | Formulario de fisio completo | Dos toques rápidos en Crear cuenta | QA Fisio Doble | Una sola cuenta | Se crearon 2 filas `usuarios` (uids distintos) con el mismo correo; solo 1 tenía usuario en Firebase Auth (la otra es un fantasma sin login posible) | FAIL (ERR-ADM-005) | admin/evidencia/CP-ADM-037.png |
| CP-ADM-038 | RNF02-CA02 | Menú lateral admin | Admin | Abrir hamburguesa | — | Dashboard, Pacientes, Fisioterapeutas, Cerrar sesión | Exactamente esas 4 opciones | PASS | admin/A12_menu.png |
| CP-ADM-039 | RNF02-CA02 | Admin no ve pantallas de fisio/paciente | Admin | Recorrer todas las pestañas y accesos | — | Ninguna pantalla de fisio/paciente | Solo destinos admin (grafo `AdminNavGraph`) | PASS | log |
| CP-ADM-040 | RNF02 | Permisos de Firestore durante las acciones | Admin | `adb logcat -s Firestore:W` tras acciones | — | Sin PERMISSION_DENIED en operaciones admin | Solo aparecen `PERMISSION_DENIED` en los listeners de lista al cerrar sesión (12:56 y 12:58), atrapados por `.catch{}` en el ViewModel; ninguno en altas, ediciones, asignaciones o eliminaciones | PASS | log |
| CP-ADM-041 | Navegación | Botón atrás en formulario | Formulario con dato escrito | Atrás dos veces | "Borrador" | Vuelve a la lista sin guardar | Vuelve a Pacientes sin guardar y sin pedir confirmación | PASS | log |
| CP-ADM-042 | HU20-CA02 | Email reutilizado tras eliminar | Cuenta eliminada desde la app | Crear otra con el mismo correo | — | Debería poder reutilizarse | No ejecutado (la cuenta Auth sigue viva, ver ERR-ADM-006); se limpió por script | BLOQUEADO | log |

## 2. Errores detectados

| ID | HU / funcionalidad | Descripción | Pasos para reproducir | Resultado esperado | Resultado actual | Severidad | Prioridad | Estado |
|---|---|---|---|---|---|---|---|---|
| ERR-ADM-001 | HU20-CA02/CA03 – validación de formulario | El formulario de paciente solo exige campos no vacíos y edad > 0. No valida DNI (longitud/solo dígitos), rango de edad, formato del contacto. | Registrar paciente con DNI `AB`, edad `200`, contacto `xyz` | Rechazo con mensaje por campo | Cuenta creada con esos valores | Media | Alta | Abierto |
| ERR-ADM-002 | HU20-CA02 – unicidad de DNI | Se permite el mismo DNI en dos pacientes. | Editar un paciente y ponerle el DNI de otro | Rechazo por DNI duplicado | Guardado; dos docs `pacientes` con dni `70000001` | Media | Media | Abierto |
| ERR-ADM-003 | HU21-CA04 / HU20-CA05 – eliminar fisio con pacientes | Eliminar un fisioterapeuta con pacientes no advierte ni reasigna. Los pacientes quedan con `fisioterapeutaId` apuntando a un documento inexistente, muestran "Fisioterapeuta: —" y, como el botón de asignar solo aparece sin fisioterapeutaId, no se pueden reasignar desde la app. | Asignar 2 pacientes QA a un fisio QA; eliminar el fisio; abrir Pacientes | Bloquear la eliminación, advertir o liberar a los pacientes | Pacientes huérfanos sin reasignación posible | Alta | Alta | Abierto |
| ERR-ADM-004 | HU20/HU21 – orden de listas y eliminación | El orden de las tarjetas no es estable (ni alfabético ni cronológico; cambia tras cada alta/baja). Como el diálogo de eliminar no lista a quién se va a borrar de forma inequívoca, y la posición de la fila cambia entre operaciones, es fácil borrar una cuenta equivocada (ocurrió durante esta prueba: se eliminó a Dr. Carlos Mendoza). | Eliminar dos cuentas seguidas por posición | Orden determinista (p. ej. por nombre) y diálogo con nombre completo | Fila cambia de lugar; se eliminó la cuenta incorrecta | Alta | Alta | Abierto |
| ERR-ADM-005 | HU21-CA02 / HU20-CA02 – doble toque | Dos toques rápidos en "Crear cuenta" ejecutan dos altas. Resultado: dos documentos `usuarios` con el mismo correo y un usuario sin cuenta en Auth (fantasma que aparece en la lista pero nunca podrá iniciar sesión). El botón no se bloquea con `guardando`. | Rellenar el formulario de fisio; doble toque en Crear cuenta | Una sola cuenta | 2 filas duplicadas, una huérfana de Auth | Alta | Alta | Abierto |
| ERR-ADM-006 | HU20-CA04 / HU21-CA04 – eliminar cuenta | Eliminar solo borra documentos de Firestore; el usuario de Firebase Auth sigue existiendo (los 4 correos QA seguían en Auth). No se puede reutilizar el correo y la persona eliminada conserva credenciales válidas (la app la cierra al entrar por falta de documento). Está reconocido como limitación en `01_Pantallas_y_Capturas.md` (1.7). | Eliminar una cuenta desde la app; consultar Auth por script | Cuenta también eliminada de Auth | Auth intacto | Media | Media | Abierto |
| ERR-ADM-007 | HU20/HU21 – eliminar cuenta con historial | La eliminación de un paciente no borra sus `sesiones` ni `observaciones`; la de un fisio tampoco. | Revisar `AdminRepositoryImpl.eliminarUsuario` | Limpieza o conservación explícita | Solo borra `usuarios`, `pacientes`, `fisioterapeutas` y `diagnosticos` (revisado en código; no se comprobó con sesiones reales para no tocar datos) | Baja | Baja | Abierto |
| ERR-ADM-008 | HU20-CA02 – campo edad | El campo "Edad" no filtra la entrada (aceptó letras y signo menos inyectados por adb). En un teléfono real el teclado numérico lo limita, pero un pegado de texto llegaría hasta la validación. | Escribir `abc` en Edad por adb | Solo dígitos | Acepta cualquier texto | Baja | Baja | Abierto |
| ERR-ADM-009 | RNF02 / HU20-CA08 / HU21-CA05 – cuenta desactivada | Un usuario desactivado que inicia sesión es devuelto al login sin ningún mensaje ni explicación (`RaizViewModel` hace `logout` en silencio). Parece que el login falló y no que la cuenta está inactiva. | Desactivar una cuenta; iniciar sesión con ella | Mensaje "Tu cuenta está desactivada, contacta al administrador" | Vuelta al login con campos vacíos sin mensaje | Media | Media | Abierto |
| ERR-ADM-010 | Navegación admin | Al iniciar sesión el admin aterriza en "Pacientes", no en "Dashboard", que es la primera pestaña del menú (`01_Pantallas_y_Capturas.md`, A01 primera). La documentación no dice cuál es la pantalla inicial. | Iniciar sesión como admin | Dashboard (primera pestaña) o documentar la elección | Pacientes | Baja | Baja | Abierto |
| ERR-ADM-011 | HU20/HU21 – mensajes de error | Todos los errores de guardado (correo inválido, correo repetido, clave corta, problema de red) muestran el mismo "No se pudo guardar. Verifica los datos e intenta de nuevo."; el admin no sabe qué corregir. Lo mismo el login (correo con formato inválido devuelve mensaje de credenciales). | Crear con correo duplicado, correo sin `@` o clave `123` | Mensajes específicos por causa | Mensaje genérico único | Baja | Media | Abierto |
| ERR-ADM-012 | Etapa 2A – Dashboard | En "Detalle por sesión" el botón "Mostrar gráfico/lista" queda pegado al título y en pantallas estrechas lo desplaza visualmente (ver A03: el botón toca "Detalle por sesión"). Además el paciente Elmer castro no tiene diagnóstico y solo muestra "Activo". Cosmético. | Abrir dashboard de cualquier paciente | Título y botón separados | Botón junto al título | Baja | Baja | Abierto |

## 3. NO IMPLEMENTADO y FALTA DOCUMENTAR

**NO IMPLEMENTADO (documentado o esperable, pero ausente en el código):**
- Registro/auto-registro, recuperación de contraseña y selector de tipo de usuario en el login (por diseño, RNF02).
- Perfil del administrador (solo cierra sesión desde el menú).
- Eliminación del usuario de Firebase Auth al eliminar cuenta desde la app.
- Validación de DNI, rango de edad, formato de contacto, unicidad de DNI y de correo con mensaje propio.
- Reasignar o quitar fisioterapeuta de un paciente desde el panel (HU20-CA05 lo prohíbe por diseño una vez asignado; combinado con ERR-ADM-003 deja pacientes sin salida).
- Protección contra doble envío en los formularios de alta.
- Mensaje al usuario desactivado que intenta entrar (ERR-ADM-009).

**FALTA DOCUMENTAR (existe y no está en las HU/CLAUDE.md):**
- El campo Correo está deshabilitado (solo lectura) en edición de paciente y de fisioterapeuta; el código solo actualiza `usuarios.correo` en Firestore, no en Auth.
- El botón "Iniciar sesión" se deshabilita hasta completar ambos campos y el correo se recorta con `trim`.
- Pantalla inicial del admin tras el login (Pacientes).
- Los subtítulos del Dashboard muestran "Inactivo/Activo" además del diagnóstico.
- Badge "Inactivo" en las tarjetas de pacientes y fisioterapeutas.
- Al cerrar sesión con listeners abiertos aparecen `PERMISSION_DENIED` en logcat (silenciados con `.catch`).
- Pacientes sin diagnóstico (Elmer castro): el sistema lo permite pese a que el formulario de alta lo exige.

## 4. Capturas

| Captura | Estado | Nota |
|---|---|---|
| comun/C01_login.png | Obtenida | |
| comun/C02_login_error.png | Obtenida | |
| comun/C03_consentimiento.png | Obtenida | Pantalla de paciente (tras borrar datos de la app en el emulador) |
| admin/A01_dashboard_lista.png | Obtenida | |
| admin/A02_dashboard_paciente.png | Obtenida | |
| admin/A03_dashboard_grafico.png | Obtenida | |
| admin/A04_pacientes.png | Obtenida | Sin cuentas QA (pantalla de aterrizaje) |
| admin/A05_paciente_alta.png | Obtenida | Formulario vacío |
| admin/A06_paciente_edicion.png | Obtenida | Sobre QA Paciente Uno |
| admin/A07_menu_paciente.png | Obtenida | |
| admin/A08_dialogo_desactivar.png | Obtenida | |
| admin/A09_asignar_fisio.png | Obtenida | |
| admin/A10_fisioterapeutas.png | Obtenida | Antes de crear cuentas QA |
| admin/A11_fisio_formulario.png | Obtenida | |
| admin/A12_menu.png | Obtenida | |
| admin/A13_dialogo_cerrar_sesion.png | Obtenida | |
| admin/evidencia/ | Obtenidas | CP-ADM-005, 009, 010, 011, 014, 017, 018, 019, 036, 036a, 037 y CP-AUT-011 |
| Faltantes | — | Ninguna de la lista pedida. No se capturó: mensaje de éxito de alta, el ojo de la contraseña activado, ni el dashboard de fisios (no existe). |

**Estado final de datos (verificado por script):** cuentas QA eliminadas de Firestore y de Auth (4 correos). Quedan 1 admin, 5 pacientes y **1 fisioterapeuta** (Dra. Ana Ruiz); falta Dr. Carlos Mendoza por el incidente descrito arriba. Scripts temporales borrados. Ajuste de sistema en el emulador: `accelerometer_rotation=0`, `user_rotation=0` (rotación probada).
