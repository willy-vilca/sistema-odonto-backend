# Pruebas de WhatsApp con texto personalizado

05/10/2026 · Plan 2.1 · Alcance 1.5. La recepción real y el envío de plantillas ya fueron confirmados por el usuario. Falta comprobar texto libre antes de integrar el agente. Esta guía no realiza pagos ni cambia las credenciales del archivo privado.

## Opción recomendada y comprobación gratuita previa

Para pocas pruebas, la opción de pago más conveniente es **Pay-as-you-go con el Sandbox clásico de WhatsApp**. Se usa el conector existente; no hace falta contratar un paquete mensual, alquilar un número dedicado ni registrar todavía un WhatsApp empresarial para el Sandbox. PAYG descuenta el consumo del saldo. [Modelo de pago oficial](https://help.twilio.com/articles/49507358452635), [Sandbox](https://www.twilio.com/docs/whatsapp/sandbox).

Tu entorno actual **Try out WhatsApp** de la consola nueva restringe los envíos a plantillas del proveedor. Cambiar a TEXT en el sistema no elimina esa restricción. [Trial de WhatsApp](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

Antes de pagar, comprueba si puedes abrir el [Sandbox de la consola anterior](https://www.twilio.com/console/sms/whatsapp/sandbox) con la misma cuenta. Twilio documenta pruebas en ese Sandbox con cuentas trial, pero distingue ese recorrido del trial nuevo que estás usando. No puedo asegurar acceso ni texto libre gratuito en tu cuenta concreta. Si permite activarlo y entregar una respuesta de texto dentro de la ventana válida, puedes continuar con las unidades de prueba sin actualizar. Si la consola o API exige actualización, utiliza PAYG después de comprobar la disponibilidad del Sandbox. [Pruebas oficiales en trial y consola anterior](https://help.twilio.com/articles/46617762649371-How-to-Use-WhatsApp-With-Twilio-Studio-Flows-on-a-Twilio-Trial-Account).

Si solo ves **Try out WhatsApp**, consulta a Twilio sobre acceso al Sandbox clásico para esta cuenta antes de pagar. La actualización no garantiza que el número temporal actual siga disponible como remitente. Un número propio con alta empresarial es otro recorrido y añade configuración que todavía no necesitamos. No contrates ese alta por defecto para estas pruebas.

## Coste y control del presupuesto

Twilio publica **US$0.005 por mensaje recibido o enviado**. Estos ejemplos calculan únicamente su tarifa base, antes de cargos de Meta, impuestos, unidades gratuitas y errores:

| Uso | Total de mensajes | Subtotal Twilio |
|---|---:|---:|
| 10 recibidos y 10 respuestas | 20 | US$0.10 |
| 50 recibidos y 50 respuestas | 100 | US$0.50 |
| 100 recibidos y 100 respuestas | 200 | US$1.00 |

La página de precios vigente también contempla cargos de Meta para mensajes de servicio, que incluyen respuestas sin plantilla. No se debe asumir que el total es siempre solo US$0.005 ni utilizar como garantía artículos antiguos que los describen como gratuitos. Revisa **Peru** en el calculador oficial y contrasta el consumo real tras el primer intercambio. No pude verificar en la documentación accesible la aplicación exacta de una franquicia de Meta al número compartido del Sandbox. [Tarifa y calculador actuales](https://www.twilio.com/en-us/whatsapp/pricing).

La recarga inicial es distinta del consumo: Twilio exige método de pago y saldo mínimo para actualizar. Una publicación oficial indica **US$20** como referencia, pero debes confirmar el mínimo, moneda e impuestos que muestre tu consola hoy. Esa cifra es saldo para consumir, no una cuota mensual de US$20. Recarga solo el mínimo que decidas aceptar. [Requisito actual de saldo](https://www.twilio.com/docs/messaging/onboarding/sms-foundations), [referencia publicada de US$20](https://www.twilio.com/en-us/blog/getting-the-most-out-of-your-twilio-messaging-project).

La documentación anuncia 100 mensajes de WhatsApp y 30 mensajes de plantilla después de actualizar. Revisa el **Free Units Tracker** de tu cuenta; no contamos esa promoción como cobertura asegurada de todos los cargos de Meta o impuestos. [Unidades después del upgrade](https://www.twilio.com/docs/usage/trials#post-upgrade-free-unit-allocations).

Para controlar gastos, deja desactivado **Auto Recharge**, revisa **Billing / Usage** y activa avisos de consumo si aparecen. Un aviso no es un límite rígido: Twilio indica que PAYG no ofrece un ajuste general de gasto máximo. Esta guía utiliza únicamente mensajes manuales y el conector propio; no añade productos opcionales. [Pago por uso y recargas](https://help.twilio.com/articles/49507358452635).

## Pasos para habilitar la prueba

### 1. Confirmar el acceso al Sandbox

Abre el enlace de la consola anterior indicado arriba. Debe mostrar un número de Sandbox y un código de incorporación. No cambies todavía la configuración que funciona si la cuenta no te permite acceder. Si puedes activarlo sin actualizar, realiza los pasos 3 a 7 una vez para comprobar texto gratuito. Si la consola o ese envío exige quitar la restricción trial, realiza el paso 2 y repite la comprobación. Si el texto llega correctamente en trial, no hace falta pagar todavía.

### 2. Actualizar a PAYG si es necesario

En tu cuenta actual de Twilio, pulsa **Upgrade** o busca esa palabra en la consola. Completa tus datos reales de facturación y dirección; elige **Pay-as-you-go** si aparecen varias modalidades. Añade el método de pago y revisa el importe final. Desactiva Auto Recharge y confirma únicamente cuando estés conforme con el coste mostrado. El pago lo realizas tú en Twilio.

Comprueba que la cuenta ya figura como actualizada y abre de nuevo el Sandbox clásico. Si no aparece, resuelve su acceso con soporte antes de comprar un número propio. [Ubicación de Upgrade](https://www.twilio.com/docs/usage/trials#upgrade-your-account).

### 3. Incorporar tu teléfono al Sandbox clásico

Desde tu WhatsApp, envía al número que muestre ese Sandbox el mensaje **join** con su código exacto, o utiliza su QR. Espera la confirmación. Haber unido el teléfono al entorno anterior no sustituye esta incorporación. Copia el número del Sandbox nuevo: la documentación muestra **+14155238886**, distinto del número temporal de tu trial actual.

### 4. Configurar la recepción HTTPS

Mantén encendidos el backend, el receptor del puerto 8082 y ngrok según [la guía de conexión](conectar-whatsapp-prueba.md). En **Sandbox settings / Sandbox configuration**, configura:

| Campo | Valor |
|---|---|
| When a Message Comes in | https://TU_DOMINIO_ACTUAL/api/v1/integrations/whatsapp/inbound |
| Método | POST |
| Status callback URL, si aparece | https://TU_DOMINIO_ACTUAL/api/v1/integrations/whatsapp/status |

Sustituye TU_DOMINIO_ACTUAL por tu dirección HTTPS real. La API del sistema también indica la dirección de estados en cada envío. Guarda los cambios.

### 5. Cambiar la configuración local

Abre **backend/config/whatsapp.local.properties** y cambia estas dos líneas, usando el número que realmente muestre tu Sandbox:

~~~properties
odontocare.whatsapp.send-mode=TEXT
odontocare.whatsapp.sender=whatsapp:+14155238886
~~~

Conserva **enabled=true**, **worker-enabled=true**, la URL pública actual y tu teléfono en allowed-participants. Usa Account SID y Auth Token de la misma cuenta que contiene ese Sandbox; no los compartas por chat. Si sigues en la misma cuenta y las credenciales no cambiaron, puedes conservarlas. test-template-sid deja de utilizarse en modo TEXT y puede permanecer guardado.

Reinicia el backend y pulsa **Actualizar conexión** en WhatsApp, o recarga la página. La respuesta de prueba debe mostrar **Mensaje de respuesta** y **Enviar mensaje**. Las conversaciones existentes se conservan; cambiar el número de prueba no borra sus mensajes.

### 6. Enviar un texto entrante nuevo

Desde el teléfono incorporado, envía al número del Sandbox clásico:

~~~text
Hola, soy Willy. Estoy probando los mensajes personalizados del sistema odontológico.
~~~

Pulsa **Actualizar conversaciones**, abre **Ver conversación** y pulsa **Actualizar mensajes**. Comprueba que el texto aparece como Recibido. Esto confirma además que el Sandbox nuevo entrega eventos a nuestro sistema.

### 7. Responder desde el sistema

En **Mensaje de respuesta**, escribe:

~~~text
Hola, Willy. Recibimos tu mensaje y esta respuesta fue escrita directamente desde OdontoCare. La prueba de texto personalizado está funcionando.
~~~

Pulsa **Enviar mensaje** una vez. Actualiza después los mensajes para revisar En cola, Enviado y Entregado; Leído aparece cuando WhatsApp proporciona ese aviso. Comprueba el mismo texto en tu teléfono. Revisa también el mensaje y su coste en los registros de Twilio. Esta prueba no crea una cita: la integración del agente se desarrolla después.

## Ventana de respuesta y problemas habituales

Las respuestas de texto requieren un mensaje del participante en las últimas **24 horas**; pagar no elimina esa regla. Envía un texto nuevo al inicio de cada sesión de pruebas. En el Sandbox, la incorporación vence a los tres días y puede renovarse con join. Se conserva el ritmo limitado de pruebas y no se realizan pruebas de carga. [Regla de conversación](https://www.twilio.com/docs/whatsapp/api#conversational-messaging-on-whatsapp).

| Resultado | Qué revisar |
|---|---|
| Sigue apareciendo Enviar plantilla de prueba | Confirma send-mode=TEXT, reinicio del backend y Actualizar conexión. |
| La API sigue exigiendo ContentSid | Puede continuar la restricción de trial o estar usándose el remitente temporal anterior. Comprueba la cuenta y el Sandbox activo; revisa el código exacto del error antes de repetir. |
| Error 63015 | Tu teléfono debe incorporarse a ese Sandbox; puede haber vencido su sesión. |
| Error 63016 o el sistema pide un mensaje nuevo | Envía desde el teléfono otro texto al Sandbox y confirma que se recibe antes de responder. |
| Entrada rechazada tras cambiar remitente | Revisa sender, Account SID, token vigente y URL pública exacta; la firma sigue siendo obligatoria. |
| Resultado incierto | Consulta Twilio antes de enviar otra respuesta; el primer intento podría haber sido aceptado. |

La prueba termina cuando un texto real entra, una respuesta propia sale y se confirma en el teléfono con su estado y referencia. Entonces se registra ese resultado en el plan y se continúa con modelo y agente. **A22 y la fase 6 completa siguen pendientes** hasta demostrar la reserva automática confirmada por el paciente.
