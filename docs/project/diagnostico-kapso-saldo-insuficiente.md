# Diagnóstico de envíos de Kapso — 12/10/2026

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
