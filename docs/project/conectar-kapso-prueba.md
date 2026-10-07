# Conectar el Sandbox de Kapso con OdontoCare

07/10/2026. Este tramo recibe mensajes y permite responder texto escrito por un usuario del sistema. No ejecuta el agente, no crea pacientes ni citas. Se utiliza la instalación actual y PostgreSQL existente. El backend trabaja en la rama kapso y el frontend tiene también una rama kapso.

**Estado actual:** el usuario ya completó clave, ID, túnel y webhook. Se comprobó una entrada real, una respuesta personalizada desde OdontoCare con estado Leído y la respuesta posterior del teléfono. Los pasos 1 a 4 quedan como guía para repetir la configuración; ahora puedes comenzar por el paso 5. [Evidencia](conexion-kapso-fase-6.md).

## 1. Guardar la clave y el identificador

La cuenta y la sesión del Sandbox ya fueron creadas por el usuario, que confirmó recepción de texto personalizado en su teléfono. Mantén el proyecto Free actual; no contrates otro plan para seguir esta guía.

1. En Kapso abre el proyecto usado para tu Sandbox. Busca **API keys**, documentado dentro de **Integrations → API keys**, y crea una clave de ese proyecto.
2. Guarda únicamente su valor en **backend/config/kapso.local.properties**, en la línea **odontocare.kapso.api-key**. El archivo está preparado y excluido de Git. No envíes la clave por chat, capturas ni frontend.
3. Busca **Sandbox WhatsApp** en **Phone numbers / WhatsApp → Configurations** y copia su **phone_number_id**. Es un identificador numérico interno, no el número +56, el BSUID del contacto ni el código de activación.
4. Si la pantalla no muestra ese ID, abre una terminal en backend y ejecuta **./scripts/kapso-phone-numbers.ps1**. Consulta la API con la clave ya guardada y muestra solo los identificadores y nombres; elige el Sandbox de tu proyecto. También puede aparecer en el ejemplo de código de Quick setup, en la dirección que termina en /messages.
5. Coloca ese identificador en **odontocare.kapso.phone-number-id** del mismo archivo.

El número visible mostrado en tu captura es **+56920403095**. El participante es **+51956190217**. Esos valores ya se prepararon en tu archivo privado; verifica que coincidan con tu sesión actual. El ejemplo versionado utiliza un participante ficticio.

## 2. Revisar el archivo privado

~~~properties
odontocare.kapso.enabled=true
odontocare.kapso.worker-enabled=true
odontocare.kapso.api-key=TU_CLAVE_PRIVADA
odontocare.kapso.phone-number-id=ID_INTERNO_DEL_SANDBOX
odontocare.kapso.webhook-secret=SECRETO_PRIVADO_DEL_WEBHOOK
odontocare.kapso.sender=+56920403095
odontocare.kapso.public-base-url=https://TU_DOMINIO.ngrok-free.app
odontocare.kapso.allowed-participants=+51956190217
~~~

El **webhook-secret** ya se generó aleatoriamente en el archivo privado. Conserva ese valor y úsalo también al crear el webhook en Kapso; no es la API key. No copies los marcadores del ejemplo sobre valores reales que ya guardaste.

enabled=true selecciona la conexión manual de Kapso en la bandeja. Se pausan los trabajadores de Twilio y del agente, aunque sus archivos privados sigan configurados. Los mensajes de Kapso no se encolan para IA. No necesitas modificar ai.local.properties ni whatsapp.local.properties.

## 3. Publicar el receptor limitado

1. Desde backend inicia el servidor habitual con **./mvnw.cmd spring-boot:run**.
2. En otra terminal, también desde backend, ejecuta **node scripts/whatsapp-webhook-gateway.mjs**. El receptor escucha en **127.0.0.1:8082** y expone únicamente webhooks.
3. En otra terminal ejecuta **ngrok http 8082**. Usa la dirección HTTPS que aparezca.
4. Guarda esa dirección, sin ruta adicional ni barra final, en **odontocare.kapso.public-base-url** y reinicia el backend.
5. Ejecuta el frontend como habitualmente: **npm run dev** desde frontend.

No publiques el puerto 8080 completo. Si el receptor 8082 anterior sigue abierto, detenlo y vuelve a iniciarlo para cargar la ruta nueva. Si ngrok cambia su dominio, actualiza el archivo privado y el webhook.

## 4. Crear el webhook en Kapso

1. Abre **Sandbox WhatsApp → Manage Webhooks**. Según la vista puede estar dentro de Phone numbers o WhatsApp → Configurations.
2. Crea un webhook de tipo **Kapso**, con formato **v2**; no selecciones el formato Meta ni un webhook general del proyecto.
3. Como dirección usa **https://TU_DOMINIO.ngrok-free.app/api/v1/integrations/kapso/events**. La misma dirección aparecerá en la tarjeta de conexión del sistema.
4. En **Secret key / secret_key**, pega el valor de **odontocare.kapso.webhook-secret**. Si la pantalla genera un secreto en vez de permitir establecerlo, copia ese nuevo valor al archivo privado y reinicia el backend. Ambos deben coincidir.
5. Suscribe estos eventos: **whatsapp.message.received**, **whatsapp.message.sent**, **whatsapp.message.delivered**, **whatsapp.message.read** y **whatsapp.message.failed**.
6. Mantén el webhook activo. Deja **message buffering** desactivado para la primera prueba. El receptor también admite lotes acotados por si ya estaba activado.
7. No asignes un agente ni un workflow de Kapso a esta prueba. La respuesta se escribirá desde OdontoCare.

## 5. Comprobar recepción y respuesta

1. Inicia sesión en OdontoCare con administrador o recepción y abre **WhatsApp**.
2. Pulsa **Actualizar conexión**. Debe indicar **Kapso Sandbox**, modo texto y configuración lista. Ese estado valida campos locales; no demuestra todavía entrega externa.
3. Desde tu WhatsApp envía al Sandbox: **Hola, esta es una prueba de conexión con OdontoCare.**
4. Pulsa **Actualizar conversaciones**, abre tu contacto y comprueba el texto recibido y su referencia de Kapso.
5. En **Mensaje de respuesta** escribe: **Hola Willy. Este mensaje personalizado fue enviado desde OdontoCare mediante Kapso.** Pulsa **Enviar mensaje**.
6. Actualiza mensajes y verifica el estado. En cola indica guardado local; Aceptado indica aceptación de la API. Entregado o Leído y la recepción en tu teléfono verifican el resultado externo.
7. Responde desde el teléfono: **Confirmo que recibí el mensaje personalizado.** Debe aparecer en la conversación del sistema.

Comprueba que el sistema no muestra ni ejecuta el agente durante este modo. En la agenda no debe aparecer una cita por estos mensajes. Cambiar enabled a false y reiniciar selecciona otra vez el conector Twilio sin eliminar sus mensajes.

## Errores y límites

| Resultado | Revisión |
|---|---|
| Configuración pendiente | La tarjeta enumera valores faltantes; revisa ID interno, clave, secreto, dominio y participante. |
| No llegan entradas | Webhook activo del Sandbox correcto, URL exacta, receptor 8082 y túnel abiertos, secreto coincidente. Envía un mensaje nuevo después de configurar. |
| Firma rechazada | No uses la API key como secreto del webhook; copia el mismo valor en ambos sitios. |
| Rechazo por sesión o 24 horas | Renueva la sesión si hace falta y envía primero otro mensaje desde el teléfono registrado. |
| Clave o permisos rechazados | Usa la clave del proyecto donde activaste el Sandbox. |
| Saldo o facturación | Consulta Billing y la pestaña WhatsApp de Usage; el cupo del plan y los créditos son conceptos distintos. |
| Resultado por verificar | Consulta los registros de Kapso antes de volver a enviar; un fallo de conexión no demuestra que el mensaje no salió. |

El Sandbox admite texto, no plantillas. Solo se permite enviar a participantes autorizados con ventana de 24 horas abierta, validada al guardar y al despachar. Las repeticiones de eventos o de una solicitud de envío no crean otro registro. No se importa el historial anterior ni se descargan adjuntos en este tramo.

Fuentes oficiales: [Sandbox](https://docs.kapso.ai/docs/how-to/whatsapp/use-sandbox-for-testing), [texto](https://docs.kapso.ai/docs/whatsapp/send-messages/text), [webhooks](https://docs.kapso.ai/docs/platform/webhooks/overview), [firma](https://docs.kapso.ai/docs/platform/webhooks/security). La configuración por sí sola no cierra fase 6 ni A22.
