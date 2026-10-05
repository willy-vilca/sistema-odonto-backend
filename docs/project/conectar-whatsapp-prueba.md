# Conectar WhatsApp de prueba con OdontoCare

Fecha: 05/10/2026. Primera parte de la fase 6: recibir mensajes reales, guardarlos, consultarlos y comprobar el envío. La interpretación con IA y la creación automática de citas se incorporan después de verificar esta conexión.

Resultado comunicado el 05/10/2026: el usuario confirmó recepción real y envío de plantillas; sus capturas muestran mensajes Recibido y respuestas Leído. Para continuar con respuestas propias, seguir [prueba de texto personalizado y costes](probar-whatsapp-texto-personalizado.md). El texto libre y la reserva mediante agente aún no están comprobados.

## Qué vas a preparar

Necesitas tu computadora con el sistema, un teléfono con WhatsApp, una cuenta de Twilio y una cuenta de ngrok. Utiliza tu propio teléfono o el de alguien que haya aceptado participar en la prueba. Los mensajes serán de demostración y no incluirán información clínica.

El recorrido de entrada será: tu WhatsApp → número de prueba de Twilio → dirección HTTPS de ngrok → receptor local limitado a WhatsApp → backend → PostgreSQL → pantalla WhatsApp del sistema. La salida usa la API de Twilio y sus avisos de entrega regresan por ese mismo receptor.

Twilio proporciona el número de prueba. No se conecta el historial de tu WhatsApp personal ni hay que registrar todavía un número de consultorio. La disponibilidad de la consola y las restricciones de la cuenta se comprueban al crearla; las pantallas pueden cambiar con el proveedor.

## 1. Crear tu cuenta de Twilio

1. Abre [el registro de Twilio](https://www.twilio.com/try-twilio).
2. Registra tus datos y confirma el correo y el teléfono cuando lo solicite.
3. En la información del proyecto puedes indicar: **Sistema odontológico de demostración con reservas por WhatsApp**.
4. Completa el acceso a la consola. La documentación actual permite comenzar la prueba sin tarjeta; comprueba los límites que aparecen en tu propia cuenta.
5. Mantén esta sesión abierta para los siguientes pasos. [Registro y credenciales oficiales](https://www.twilio.com/docs/usage/tutorials/how-to-use-your-free-trial-account).

**Antes de continuar:** el nuevo trial de Twilio dura 30 días e incluye 100 mensajes de WhatsApp. Admite hasta cinco teléfonos verificados y restringe el contenido a ejemplos o plantillas del proveedor. Por eso esta primera conexión incluye un envío de prueba con plantilla; no presupone que una cuenta recién creada pueda responder con cualquier texto. La conversación libre del agente requerirá una cuenta o un Sandbox que permitan esa función. No tienes que actualizar ni pagar la cuenta para comenzar a comprobar la recepción. [Restricciones oficiales del trial](https://www.twilio.com/docs/usage/trials).

## 2. Activar WhatsApp y unir tu teléfono

La documentación actual distingue dos recorridos. Usa el que aparezca en tu cuenta:

| Si tienes… | Qué debes hacer |
|---|---|
| Consola nueva y trial | Entra en **Messaging**, busca WhatsApp entre los canales y abre **Try out WhatsApp**. Sigue la verificación del teléfono, acepta las condiciones si las solicita y conecta el dispositivo con el QR o mensaje de incorporación mostrado. |
| Consola anterior con Sandbox | Abre [Try WhatsApp en la consola anterior](https://www.twilio.com/console/sms/whatsapp/sandbox), acepta las condiciones y envía desde WhatsApp el mensaje `join ...` que muestra tu Sandbox. |

En cualquiera de los casos:

1. Usa exactamente el número y el mensaje de incorporación que aparezcan en tu consola.
2. Espera la confirmación de que el teléfono se incorporó.
3. Si la cuenta exige verificar el destinatario, completa también esa verificación. Incorporarse al Sandbox y verificar un teléfono en el trial pueden ser dos pasos distintos.
4. Conserva la conversación de WhatsApp con el número de prueba. Allí enviarás los mensajes de esta guía.
5. Si quieres usar otro teléfono, repite la incorporación y la verificación correspondientes con ese participante.

La consola nueva dispone de **Inbound** y de la opción **Custom** en la configuración de respuesta; allí se asignará el enlace de recepción más adelante. El Sandbox anterior muestra **Sandbox settings / Sandbox configuration**. [Guía del trial de WhatsApp](https://www.twilio.com/docs/usage/trials/try-out-whatsapp), [Sandbox oficial](https://www.twilio.com/docs/whatsapp/sandbox).

## 3. Guardar los datos de conexión en tu computadora

Desde la carpeta `backend`, copia `config/whatsapp.example.properties` como `config/whatsapp.local.properties`. Si este último ya existe, ábrelo y actualízalo; no lo reemplaces sin revisar sus valores.

~~~powershell
if (-not (Test-Path -LiteralPath config/whatsapp.local.properties)) {
  Copy-Item -LiteralPath config/whatsapp.example.properties -Destination config/whatsapp.local.properties
}
notepad config/whatsapp.local.properties
~~~

Este archivo local está excluido de Git y el perfil local del backend lo carga automáticamente. **No envíes el Auth Token ni las claves de ngrok por el chat, ni los pegues en capturas.** El ejemplo versionado contiene solamente campos de referencia.

En la página principal de Twilio localiza **Account SID** y **Auth Token**. Si no aparecen, consulta la sección **API Keys & Tokens** de esa cuenta. Deben pertenecer a la misma cuenta que recibe tus mensajes. Usa las credenciales reales de la cuenta de prueba, no unas credenciales ficticias para simular solicitudes. [Ubicación oficial de las credenciales](https://www.twilio.com/docs/usage/tutorials/how-to-use-your-free-trial-account), [Alcance de las credenciales de simulación](https://www.twilio.com/docs/iam/test-credentials).

Completa los campos así:

| Campo | Valor que debes colocar |
|---|---|
| `odontocare.whatsapp.enabled` | `true` |
| `odontocare.whatsapp.account-sid` | Tu Account SID real, que comienza por `AC`. |
| `odontocare.whatsapp.auth-token` | El Auth Token de esa misma cuenta. |
| `odontocare.whatsapp.sender` | El número habilitado para WhatsApp que muestra Twilio, con formato `whatsapp:+...`. Copia el remitente de la prueba, no tu teléfono personal ni otro número de Twilio. |
| `odontocare.whatsapp.public-base-url` | Se completa en el paso 5 con la dirección HTTPS de ngrok, sin añadir las rutas de recepción o estados. |
| `odontocare.whatsapp.allowed-participants` | Tu teléfono con código internacional, sin espacios y sin `whatsapp:`. Ejemplo de formato: `+51987654321`. Para varios participantes autorizados, separa los números con comas. |
| `odontocare.whatsapp.send-mode` | Conserva `TEMPLATE` para el nuevo trial. |
| `odontocare.whatsapp.test-template-sid` | El `ContentSid` que comienza por `HX` del ejemplo de envío permitido en **Try out WhatsApp**. Utiliza una plantilla del trial que no requiera variables adicionales. |
| `odontocare.whatsapp.worker-enabled` | `true` para procesar la tarea de envío. |

El número `+51987654321` es solo una referencia de formato; debes sustituirlo por el teléfono que incorporaste. Puedes guardar los campos que ya conoces y completar la URL después.

Para obtener el `ContentSid`, realiza el primer envío guiado de la consola de Twilio a tu propio teléfono y consulta el ejemplo de petición que muestra esa pantalla. Copia solo el identificador de su plantilla al archivo. Esta comprobación confirma además que Twilio puede entregar al participante antes de probar el sistema.

Si tu cuenta no ofrece una plantilla que sirva para este envío, la recepción puede probarse igualmente. Conservaremos el envío como pendiente de habilitación del proveedor; no cambiamos a texto libre para saltar esa limitación.

## 4. Preparar el acceso HTTPS con ngrok

ngrok permite que Twilio entregue los mensajes a tu computadora mientras haces la prueba. El sistema incluye un receptor que admite únicamente las dos rutas de WhatsApp; el túnel apunta a ese receptor, no a toda la aplicación.

1. Abre [el registro de ngrok](https://dashboard.ngrok.com/signup) y crea tu cuenta.
2. Instala la versión de Windows siguiendo [la descarga oficial](https://ngrok.com/download/windows). Puedes usar Microsoft Store o ejecutar en PowerShell:

~~~powershell
winget install ngrok -s msstore
~~~

3. Si la terminal no reconoce `ngrok` después de instalarlo, ciérrala y abre una nueva.
4. Obtén tu authtoken en la cuenta de ngrok. Para guardarlo sin escribirlo en una instrucción de terminal, abre su configuración:

~~~powershell
ngrok config edit
~~~

5. En el archivo que se abre, conserva las opciones existentes y completa `agent.authtoken` con tu token si utiliza la configuración versión 3. Si todavía no existe una configuración, puedes usar:

~~~yaml
version: 3
agent:
  authtoken: PEGA_AQUI_TU_TOKEN_DE_NGROK
~~~

6. Guarda el archivo y comprueba su formato:

~~~powershell
ngrok config check
~~~

Si la configuración que te proporciona ngrok utiliza otro formato, sigue el ejemplo de su panel en lugar de mezclar versiones. El token de ngrok se guarda en la configuración de esa herramienta, separado del archivo de Twilio. [Formato oficial de la configuración versión 3](https://ngrok.com/docs/gateway/agent/config/v3), [Comandos oficiales de ngrok](https://ngrok.com/docs/gateway/agent/cli).

## 5. Encender el receptor y obtener tu dirección pública

Necesitarás mantener abiertas estas terminales durante la prueba. Los comandos de esta guía se ejecutan desde las carpetas indicadas.

**Terminal del receptor, desde `backend`:**

~~~powershell
node scripts/whatsapp-webhook-gateway.mjs
~~~

El receptor escucha en `127.0.0.1:8082` y reenvía exclusivamente los avisos de entrada y entrega al backend local del puerto 8080.

**Terminal del túnel, desde cualquier carpeta:**

~~~powershell
ngrok http http://127.0.0.1:8082 --inspect=false
~~~

Copia la dirección que comienza por `https://` que muestre ngrok. Como ejemplo de forma: `https://tu-direccion.ngrok-free.app`. Usa tu dirección real.

Vuelve a `backend/config/whatsapp.local.properties`, completa:

~~~properties
odontocare.whatsapp.public-base-url=https://TU_DIRECCION_REAL_DE_NGROK
~~~

Guarda y **reinicia el backend** para que cargue la configuración. Desde `backend`:

~~~powershell
./mvnw.cmd spring-boot:run
~~~

Si ya está encendido en otra terminal, detén esa ejecución con `Ctrl+C` y vuelve a iniciarlo allí. No abras dos backend en el mismo puerto.

Mantén también el frontend abierto como en las fases anteriores. Si hace falta iniciarlo, desde `frontend`:

~~~powershell
npm.cmd run dev
~~~

El uso de `--inspect=false` desactiva la inspección local de las peticiones del túnel. Los mensajes se consultan desde la pantalla protegida del sistema. [Opciones oficiales del túnel HTTPS](https://ngrok.com/docs/gateway/agent/cli).

## 6. Indicar a Twilio dónde entregar los mensajes

Con tu dirección real de ngrok, forma estas dos URLs:

| Uso | URL |
|---|---|
| Mensajes entrantes | `https://TU_DIRECCION_REAL_DE_NGROK/api/v1/integrations/whatsapp/inbound` |
| Avisos de entrega | `https://TU_DIRECCION_REAL_DE_NGROK/api/v1/integrations/whatsapp/status` |

**Consola nueva:** vuelve a **Try out WhatsApp → Inbound** y, en **Auto-Reply**, elige **Custom**. Configura el webhook de entrada con la primera URL. Si la pantalla permite elegir el método, selecciona **POST**. Guarda los cambios.

**Sandbox anterior:** en **Sandbox settings / Sandbox configuration**, completa **When a Message Comes in** con la primera URL y método **POST**. Si está disponible, completa **Status callback URL** con la segunda URL y guarda.

El envío desde OdontoCare ya indica la segunda URL a Twilio para recibir los estados; no hace falta que la consola nueva muestre una casilla equivalente. No configures respuestas automáticas adicionales de Twilio para esta prueba, pues dificultan distinguir qué mensaje envió el sistema. [Recepción del trial](https://www.twilio.com/docs/usage/trials/try-out-whatsapp), [Configuración del Sandbox](https://www.twilio.com/docs/whatsapp/sandbox).

No pegues `localhost`, `127.0.0.1` ni la dirección del frontend en Twilio: el proveedor necesita la URL pública HTTPS. Cuando cambie la dirección del túnel, actualiza el archivo local, las URLs de Twilio y reinicia el backend antes de enviar otra prueba.

## 7. Comprobar que un mensaje llega y se guarda

1. Entra en el sistema con un administrador y abre **WhatsApp**.
2. Revisa el estado de la conexión. Si indica que faltan campos, completa el archivo local y reinicia el backend. La pantalla no muestra las claves.
3. Desde el teléfono autorizado, abre el chat del número de prueba de Twilio y envía:

~~~text
Hola, soy Carlos Ruiz. Quisiera una cita para una limpieza dental la próxima semana. Este es un mensaje de prueba.
~~~

4. Pulsa **Actualizar conversaciones** desde la aplicación y busca tu número o el nombre de perfil.
5. Pulsa **Ver conversación**. Comprueba que aparecen el texto completo, el remitente y la hora. Despliega **Referencia de Twilio** para consultar el identificador del mensaje.
6. Envía un segundo texto:

~~~text
Preferiría por la mañana. Todavía no confirmo ninguna reserva.
~~~

7. Pulsa **Actualizar mensajes** y verifica que se conserva el primer mensaje junto al segundo. La agenda debe seguir sin una cita nueva originada por esta prueba: en esta primera parte todavía no está funcionando el agente de reserva.

La recepción valida la firma de Twilio antes de guardar. Los avisos de entrega no se presentan como si fueran mensajes nuevos del paciente. El identificador del proveedor permite reconocer una repetición de la misma entrega. [Formato oficial de mensajes entrantes](https://www.twilio.com/docs/messaging/guides/webhook-request), [Firmas de los webhooks](https://www.twilio.com/docs/usage/webhooks/webhooks-security).

## 8. Comprobar que el sistema puede enviar y seguir la entrega

**Con `send-mode=TEMPLATE`:** en la conversación pulsa **Enviar plantilla de prueba**. Se enviará la plantilla configurada en el paso 3, no el texto libre que escriba un operador. Comprueba en el teléfono que llegó el mensaje del ejemplo y, al pulsar **Actualizar mensajes**, que existe un mensaje saliente con su estado.

Si la plantilla no está habilitada, Twilio rechaza el envío o caducó el trial, anota el resultado y el código de error. La prueba de recepción sigue siendo útil; no declaramos completado el envío solo porque se creó una tarea local.

**Solo con texto libre habilitado por la cuenta:** cambia `odontocare.whatsapp.send-mode=TEXT` en el archivo local y reinicia el backend. Desde WhatsApp envía primero un mensaje nuevo al número de prueba; después, desde la conversación del sistema, responde:

~~~text
Hola, Carlos. Recibimos tu mensaje de prueba correctamente. La conexión de WhatsApp con OdontoCare está funcionando.
~~~

Escribe la respuesta en **Mensaje de respuesta** y pulsa **Enviar mensaje**. Comprueba la recepción en el teléfono y el estado en la aplicación. En el Sandbox clásico, el texto libre se admite dentro de las 24 horas del último mensaje del participante. El nuevo trial mantiene además su restricción de contenido; abrir esa ventana no elimina la restricción del trial. Si la cuenta necesita actualizarse para conversar libremente, esa decisión y cualquier pago los realizas tú desde Twilio después de revisar las condiciones. [Ventana y límites del Sandbox](https://www.twilio.com/docs/whatsapp/sandbox), [Límites específicos del trial de WhatsApp](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

El estado **En cola** indica una tarea guardada; **Enviado** no equivale todavía a entrega confirmada. Busca **Entregado** y, cuando WhatsApp lo proporcione, **Leído**. La ausencia de leído por sí sola no indica un fallo. Conserva la referencia del mensaje para comparar con los registros de Twilio. [Estados oficiales de entrega](https://www.twilio.com/docs/messaging/guides/outbound-message-status-in-status-callbacks).

## 9. Guardar el resultado de la prueba y cerrar el túnel

Anota únicamente información que se pueda compartir, sin claves:

| Comprobación | Qué debes registrar |
|---|---|
| Recepción real | Fecha, texto de prueba y referencia del mensaje entrante. |
| Conservación | Ambos textos aparecen en la misma conversación tras actualizar. |
| Envío real | Referencia del mensaje saliente y confirmación de llegada al teléfono. |
| Entrega | Estado recibido por el sistema, o código del error si falló. |
| Cuenta | Trial con plantilla, Sandbox con texto libre o cuenta actualizada; sin credenciales. |

Puedes guardar capturas de la conversación del sistema y del teléfono ocultando datos que no quieras mostrar. No captures la pantalla del archivo de configuración.

Al terminar, cierra el túnel y el receptor con `Ctrl+C` en sus respectivas terminales. El resto del sistema puede continuar funcionando localmente. Los mensajes ya guardados siguen en PostgreSQL aunque se cierre el túnel.

La conexión externa queda comprobada cuando un mensaje de tu teléfono aparece en el sistema y una salida real llega al teléfono. La fase 6 completa permanece en desarrollo hasta demostrar también agente → herramientas → confirmación → cita persistida → respuesta por WhatsApp; esta guía no sustituye esa validación.

## Si algo no funciona

| Lo que ocurre | Qué revisar |
|---|---|
| La pantalla indica integración desactivada o campos pendientes | Revisa `enabled`, los valores del archivo local y que reiniciaste el backend después de guardarlos. |
| Twilio recibe el texto pero no aparece en OdontoCare | Revisa que la URL de **Inbound** sea la vigente, use `/inbound` y POST; mantén encendidos backend, receptor y ngrok. Consulta el error del mensaje en los registros de Twilio. |
| Al abrir el webhook en el navegador aparece 403, 404 o 405 | Esa URL recibe llamadas firmadas de Twilio mediante POST; no es una página para navegar. Usa la prueba desde WhatsApp. |
| Twilio registra 403 al entregar un mensaje real | Comprueba que Account SID y Auth Token sean de la misma cuenta, que remitente y destinatario correspondan a la prueba y que el teléfono esté en `allowed-participants`. La URL pública debe coincidir exactamente con la configurada en Twilio. Conserva la firma activada. |
| Twilio registra 404/405 | Comprueba las rutas completas y el método POST. El receptor bloquea intencionadamente cualquier otra ruta o método. |
| Twilio registra 11200 o el túnel indica que no conecta | Twilio no obtuvo una respuesta correcta. Revisa la URL vigente y las tres ejecuciones locales; no lo soluciones desactivando autenticidad ni publicando toda la aplicación. [Diagnóstico oficial de 11200](https://www.twilio.com/docs/api/errors/11200). |
| El envío falla con 63015 | El destinatario no está incorporado al Sandbox o caducó su incorporación. Repite el `join` que muestre tu consola. [Error oficial 63015](https://www.twilio.com/docs/api/errors/63015). |
| El envío de texto falla con 63016 | Está fuera de la ventana de conversación o requiere plantilla. Envía un mensaje nuevo desde el teléfono para reabrir la ventana; si es el trial nuevo, conserva también su modo de plantilla. [Diagnóstico oficial de 63016](https://help.twilio.com/articles/360038232293-Error-63016-Failed-to-send-freeform-message-because-you-are-outside-the-allowed-window-Please-use-a-Template). |
| No sale la plantilla o Twilio rechaza `ContentSid` | Copia el identificador `HX...` del ejemplo permitido por tu trial, de esa misma cuenta. Comprueba que no requiere variables que no se hayan configurado. |
| El mensaje quedó con resultado incierto | Consulta su referencia en Twilio antes de enviarlo nuevamente; evita provocar dos entregas distintas por un mismo intento. |

En el Sandbox clásico la incorporación de un participante vence a los tres días y el remitente permite como máximo un mensaje cada tres segundos. No hagas pruebas de carga sobre este entorno. Las comprobaciones de repetición, recuperación y concurrencia se realizan con las pruebas aisladas de la aplicación. [Restricciones del Sandbox](https://www.twilio.com/docs/whatsapp/sandbox).
