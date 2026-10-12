# Diagnóstico de envíos de Kapso — 12/10/2026

**Actualización vigente, 11:19 Lima:** el usuario eligió y configuró otra cuenta de Kapso y pidió reactivar la versión sencilla. La autenticación y el nuevo webhook se comprobaron; se reinició el backend para cargar sus valores privados. La entrada real, ejecución Groq y respuesta enviada quedaron verificadas con estado READ, un intento y confirmación del participante. Los envíos funcionan con la configuración nueva; el diagnóstico de saldo insuficiente siguiente corresponde a la cuenta anterior. No hubo recarga ni compra durante estas comprobaciones.

Trabajar en `version_sencilla` y `sistema_odonto_mini`. Main y la instalación original se conservan. El usuario autorizó activar backend/frontend, WhatsApp/Groq y realizar un envío diagnóstico; no autorizó compras.

## Causa comprobada

La respuesta del conector en la instalación original del 11/10, 20:58:50 Lima, quedó FAILED / `insufficient_credits`, con un intento. La propuesta previa fue READ. Una respuesta posterior quedó suprimida por atención humana (`LOCAL_POLICY`); esa supresión es distinta del rechazo de saldo.

El 12/10 se realizó una sola petición de texto a la API del Sandbox con la configuración privada existente y el formato del conector. Kapso devolvió HTTP 402 y `insufficient_credits`, indicando que el saldo del proyecto no cubre la tarifa del mensaje. No devolvió referencia de mensaje aceptado. No se repitió automáticamente esta petición.

Se verificaron por API de solo lectura: autenticación HTTP 200, número configurado, webhook activo, cinco eventos y formato v2. La dirección registrada coincide con el túnel HTTPS actual, que apunta al receptor limitado de 8082. No se modificaron claves, secreto, participante ni webhook; los archivos privados permanecen ignorados por Git.

El usuario mostró Billing: plan Free, 65/2.000 mensajes y saldo disponible US$0,04, con sincronización indicada hace 16 minutos. Este saldo visible es una observación de la pantalla; no se afirma que sea el saldo exacto al instante de enviar. El rechazo actual de la API es la evidencia de insuficiencia para el mensaje. Según su comprobación del importe personalizado, la recarga mínima aceptada es US$5 y el total presentado con comisión es US$5,55. No se compraron créditos ni se cambió el plan.

Kapso distingue cupo del plan y tarifas del Sandbox: los mensajes entregados consumen créditos del proyecto por la tarifa publicada del mercado del destinatario; el cupo de Meta del número compartido no elimina estos cargos. [Facturación oficial del Sandbox](https://docs.kapso.ai/docs/whatsapp/meta-message-billing#sandbox-service-messages), [plan y cupos](https://docs.kapso.ai/docs/whatsapp/pricing-faq).

## Prueba real en la versión sencilla

Backend en 8080 e interfaz en 5173, con Kapso, su trabajador y Groq/`openai/gpt-oss-20b` activos. Se conserva ngrok y el receptor que el usuario inició. Esta autorización supera la pausa temporal de la ejecución de revisión de la adaptación.

El participante envió «Hola, esta es una prueba de conexión. No quiero reservar ni cambiar ninguna cita». La entrada quedó RECEIVED el 12/10, 10:50:02 Lima. El agente finalizó su ejecución y preparó una respuesta a las 10:50:06. El trabajador la intentó enviar una vez y quedó FAILED / `insufficient_credits`. La única cita de mini era MANUAL / RESERVED y fue creada a las 01:23:15, antes de esta prueba; el mensaje no creó una reserva.

La propuesta original `AFD58F88` ya estaba CONFIRMED, vinculada a la cita `7537014d-bc3c-46c4-93b4-8e2aa21ef514`: lunes 12/10/2026, 11:00–12:00 Lima, WHATSAPP / CONFIRMED. Se comprobó solo por lectura. El fallo de confirmación saliente no borró la reserva; no volver a confirmarla para diagnosticar el envío.

## Ajuste y comprobación

El conector muestra ahora un aviso específico y seguro para `insufficient_credits` y `service_credits_exhausted` con HTTP 402: saldo insuficiente, revisar Billing y separación entre cupo y saldo. Conserva el código, no copia el cuerpo externo ni expone credenciales. Otros rechazos de facturación conservan su explicación general; no se confunden con falta de saldo.

Pasaron 4 pruebas del emisor y 17 de integración de Kapso (21 en total), con servidor HTTP simulado y la base protegida `sistema_odonto_mini_test`. Se mantienen autenticidad, persistencia, idempotencia, incertidumbre y reintentos. El primer empaquetado posterior encontró el JAR bloqueado por el proceso Java activo en Windows; tras detener únicamente ese backend propio, se empaquetó correctamente y se reinició con el cambio. Receptor/ngrok y datos se conservaron. Las respuestas históricas no se reescribieron.

## Qué hacer para recuperar el envío

1. Mantener el plan Free; el problema verificado requiere créditos de envío, no cambiar la suscripción ni la API key.
2. Si se decide continuar pagando, en Billing → Enter custom amount elegir US$5 y revisar el total de US$5,55 que mostró la cuenta antes de confirmar. La compra la realiza el usuario; comprobar nuevamente el total en el checkout.
3. Esperar a que Billing refleje el saldo actualizado y volver a enviar desde WhatsApp «Hola, esta es una prueba de conexión. No quiero reservar ni cambiar ninguna cita».
4. Comprobar entrada, respuesta del agente y estado de entrega/lectura en la conversación. No dar la entrega por recuperada solo por registrar la recarga.
5. Si vuelve a fallar, conservar el código nuevo y contrastarlo con saldo, sesión y ventana de atención. No generar otra cita ni repetir automáticamente mensajes con resultado incierto.

Estado: causa identificada, recepción/modelo comprobados y sistema activo. Recuperación de entrega pendiente de la decisión de recarga del usuario y de una respuesta real entregada. No se hicieron compras ni se confirmó una nueva reserva en esta revisión.

## Recuperación comprobada con la configuración nueva

Este resultado supera el estado pendiente anterior para la conexión actualmente utilizada; no declara repuesto el saldo de la cuenta anterior.

- Backend/frontend continúan en `version_sencilla`, con base `sistema_odonto_mini`, disponibles en 8080/5173. Receptor/ngrok conservados. Autenticación de Kapso HTTP 200; webhook `d59ebc43-912f-43c0-a71d-b0430758d985`, activo, v2, los cinco eventos y la dirección HTTPS correcta. El identificador visible de la configuración Sandbox sigue siendo `597907523413541`; no se infiere que cada cuenta deba mostrar otro identificador.
- Entrada del participante: «Hola, prueba de conexión con la cuenta nueva. No quiero reservar ni cambiar ninguna cita». Mensaje `138f587f-d02c-4c73-98c6-1268e762ae6c`, RECEIVED, 12/10/2026 11:19:29 Lima.
- Ejecución del agente `4f690a71-f6b3-4c86-93a2-18f586fb3228`, COMPLETED sin error, con Groq/`openai/gpt-oss-20b`.
- Respuesta `26302426-4d83-4e48-ab63-bfe1f989a5cb`, persistida a las 11:19:32: «¡Hola! Todo funcionando correctamente. Si necesitas algo más, avísame.». READ, un intento, sin código de error. El participante confirmó haberla recibido.
- Control AUTO/generación 0; pacientes/citas/entradas de cargos/movimientos conservaron 1/1/0/0 antes y después. La prueba no creó ni modificó reservas o finanzas y no reejecutó la confirmación de la cita original.
- Registros anteriores FAILED se conservaron. Credenciales y secreto de la nueva cuenta fueron configurados por el usuario; no se muestran ni se versionan. Main mantiene sus referencias y no se cambió la base original.

No se hizo una reserva adicional para esta comprobación. El resultado acredita recepción → agente → respuesta persistida → envío → lectura en la conexión nueva; las reglas de agenda y cambios mantienen las comprobaciones previamente documentadas. Conservar los registros de ambos resultados para diferenciar fallo por saldo y recuperación posterior.
