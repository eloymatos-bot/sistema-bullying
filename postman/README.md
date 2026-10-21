# Postman: solicitudes listas para estudiantes

## Importar una sola vez

1. En Postman pulsa **Import** (o Ctrl+O), selecciona **Files** y abre
   `Sistema-Bullying.postman_collection.json` de esta carpeta.
2. En **Collections**, abre **Sistema Bullying - Estudiantes**.
3. Haz clic en el nombre de la colección y abre **Variables**.
4. En `password`, escribe en el valor local tu contraseña del dashboard. En `email`
   indica tu correo (el ejemplo ya contiene admin@institucion.edu.pe). Guarda si
   aparece **Save**. La contraseña se deja vacía en el archivo compartido.
5. En el selector de entorno de la parte superior elige **No environment**. Las
   variables se guardan en la colección; otro entorno podría reemplazarlas.

La colección usa `http://127.0.0.1:8080`. Usa el mismo valor durante toda la sesión
y conserva activado el manejo normal de cookies de Postman. No copies tokens ni
cookies de tu navegador. Si usas Postman web, selecciona el Desktop Agent para
acceder al servidor de tu computadora.

## Iniciar sesión otro día

Con el backend encendido, abre cada solicitud y pulsa **Send** en este orden:

| Solicitud | Qué debes ver |
|---|---|
| 01 Obtener token | 200 OK. El token se guarda automáticamente. |
| 02 Iniciar sesión | 200 OK y Sesión iniciada. Se conserva la cookie de sesión. |
| 03 Renovar token después del login | 200 OK. Guarda el token nuevo automáticamente. |

No hay que modificar métodos, direcciones ni cabeceras. Las solicitudes ya los
incluyen. Si la sesión expira tras 30 minutos de inactividad o se reinicia el
backend, repite 01, 02 y 03. Un 401 indica que debes volver a iniciar sesión.
Un 403 puede indicar token antiguo o falta de permisos: prueba 03 y revisa tu rol.

## Probar estudiantes

| Solicitud | Uso |
|---|---|
| 04 Crear estudiante de prueba | Crea Prueba Postman Ficticio, 2.º A. Guarda id y versión. Cada envío crea otro registro. |
| 05 Listar estudiantes | Busca Prueba Postman; puedes modificar filtros en Params. No cambia el alumno seleccionado. |
| 06 Consultar estudiante | Lee el id guardado y actualiza automáticamente su versión y nombres. |
| 07 Editar estudiante | Conserva nombres; cambia al alumno seleccionado a 3.º B y guarda su nueva versión. |
| 08 Ver mi cuenta | Muestra el usuario autenticado y su rol. |
| 09 Cerrar sesión | Finaliza la sesión. Ejecutar solo al terminar. |

Primera prueba recomendada: **01 → 02 → 03 → 04 → 05 → 06 → 07 → 06**.
Ejecuta una solicitud a la vez. **Run collection** envía todas, incluida el alta;
para practicar no necesitas ese botón.

Para otro alumno, coloca su id en `studentId` dentro de Variables y ejecuta primero
**06**; revisa la respuesta antes de **07**. No edites datos ajenos para practicar.
Si 07 responde **409**, otra persona cambió el alumno: abre 06, revisa y luego
reintenta si corresponde. La colección no repite escrituras automáticamente.

Las respuestas correctas se comprueban en **Test Results**, y el cuerpo sigue
visible en **Body**. Si falta token/id/versión, la solicitud explica el paso previo.
El archivo entregado no contiene contraseñas, cookies ni tokens reales. Si exportas
tu copia tras utilizarla, vacía los secretos y valores de sesión antes de compartirla.

No hay solicitud DELETE porque el sistema conserva estudiantes e historial.
El contrato de los endpoints está en [API_ESTUDIANTES](../docs/API_ESTUDIANTES.md).
Scripts basados en [Postman Sandbox](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/overview/).
