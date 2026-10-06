# Pruebas de WhatsApp con texto personalizado

05/10/2026 · Plan 2.4 · Alcance 1.5. La recepción real y el envío de plantillas ya fueron confirmados por el usuario. Falta comprobar texto libre antes de integrar el agente. Esta guía no realiza pagos ni cambia las credenciales del archivo privado.

## Recorrido vigente después de las comprobaciones

No se ha demostrado que el Sandbox clásico completo sea incompatible con la cuenta: se rechazaron un envío Body durante Trial y una plantilla concreta. No hay otro Sandbox gratuito documentado que elimine las restricciones del trial nuevo. El entorno gratuito que ya funciona es Try out WhatsApp con sus plantillas permitidas. Para comprobar respuestas propias dentro de Twilio, el recorrido recomendado es actualizar **la misma cuenta** a PAYG y utilizar el Sandbox clásico de la consola anterior, al que el usuario ya pudo acceder y desde el que recibió mensajes. [Trial](https://www.twilio.com/docs/usage/trials/try-out-whatsapp), [Sandbox y respuestas libres](https://www.twilio.com/docs/whatsapp/sandbox#user-initiated-messages-and-replies).

La prueba posterior a la actualización será Body sin ContentSid ni ContentVariables, dentro de las 24 horas del último mensaje del participante. No depende de corregir el SID del ejemplo de plantilla que dio 21655. El pago no se presenta como reparación de esa plantilla; sirve para retirar las limitaciones Trial. La disponibilidad efectiva se comprobará con un intercambio real después del upgrade. El número temporal de Try out WhatsApp no se supone convertido en remitente propio al pagar; utilizar el número mostrado por el Sandbox clásico, documentado como +14155238886.

Para esta cuenta, continuar con los pasos 2 a 7 de abajo: el intento gratuito ya se realizó y fue rechazado. No repetirlo buscando otro enlace. El Sandbox no exige registrar un WABA o alquilar un número propio; si la consola deriva únicamente a un alta empresarial, resolver el acceso al Sandbox antes de contratar productos adicionales. [Requisitos del Sandbox](https://www.twilio.com/docs/whatsapp/sandbox).

**Resultado posterior de la comprobación gratuita en esta cuenta.** El usuario consiguió recibir mensajes desde el Sandbox clásico, pero el envío TEXT falló. La lectura local confirma modo TEXT y remitente del Sandbox clásico; la base registra un mensaje TEXT, FAILED, código 21654, un intento y sin plantilla asociada. La API de Twilio confirmó que la cuenta configurada sigue activa y es Trial mediante un GET de solo lectura. El usuario corrigió la asociación de la prueba de consola: la nueva petición Body sin ContentSid devuelve también 21654; el 21655 anterior corresponde a otra petición no aportada. Por tanto, acceder a la consola anterior no habilitó texto libre en esta cuenta. No repetir ese intento como alternativa gratuita ya verificada. Para avanzar con TEXT se requiere resolver la habilitación de la cuenta con Twilio, contemplando PAYG; el usuario decide y realiza la actualización. [Restricción vigente del trial](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

## Opción recomendada y comprobación gratuita previa

Para pocas pruebas, la opción de pago más conveniente es **Pay-as-you-go con el Sandbox clásico de WhatsApp**. Se usa el conector existente; no hace falta contratar un paquete mensual, alquilar un número dedicado ni registrar todavía un WhatsApp empresarial para el Sandbox. PAYG descuenta el consumo del saldo. [Modelo de pago oficial](https://help.twilio.com/articles/49507358452635), [Sandbox](https://www.twilio.com/docs/whatsapp/sandbox).

Tu entorno actual **Try out WhatsApp** de la consola nueva restringe los envíos a plantillas del proveedor. Cambiar a TEXT en el sistema no elimina esa restricción. [Trial de WhatsApp](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

Antes de pagar, comprueba si puedes abrir el [Sandbox de la consola anterior](https://www.twilio.com/console/sms/whatsapp/sandbox) con la misma cuenta. Twilio documenta pruebas en ese Sandbox con cuentas trial, pero distingue ese recorrido del trial nuevo que estás usando. No puedo asegurar acceso ni texto libre gratuito en tu cuenta concreta. Si permite activarlo y entregar una respuesta de texto dentro de la ventana válida, puedes continuar con las unidades de prueba sin actualizar. Si la consola o API exige actualización, utiliza PAYG después de comprobar la disponibilidad del Sandbox. [Pruebas oficiales en trial y consola anterior](https://help.twilio.com/articles/46617762649371-How-to-Use-WhatsApp-With-Twilio-Studio-Flows-on-a-Twilio-Trial-Account).

Si solo ves **Try out WhatsApp**, consulta a Twilio sobre acceso al Sandbox clásico para esta cuenta antes de pagar. La actualización no garantiza que el número temporal actual siga disponible como remitente. Un número propio con alta empresarial es otro recorrido y añade configuración que todavía no necesitamos. No contrates ese alta por defecto para estas pruebas.

**Comprobación posterior de la plantilla del Sandbox.** El usuario aportó la petición que devuelve 21655: remitente del Sandbox clásico, ContentSid HXb5b62575e6e4ff6129ad7c8efe1f983e y ContentVariables con claves 1 y 2. El SID coincide con [el ejemplo oficial del Quickstart](https://www.twilio.com/docs/whatsapp/quickstart) y su formato es válido. No se ha verificado que esa plantilla esté disponible para esta cuenta. Las consultas GET a la API de contenido dieron 401/20003 tanto para ese SID como para el SID del trial que había funcionado; la consulta a la cuenta dio 200, Trial y activa. Ese rechazo de acceso no demuestra que la plantilla esté borrada o pertenezca a otra cuenta. El 21655 no permite concluir que pagar lo arregle.

Antes de recargar, volver a Try out WhatsApp en la consola nueva y utilizar juntos el From y ContentSid que genere su ejemplo actual permitido, sin añadir ContentVariables al ejemplo del trial. No intercambiar el SID fijo del Quickstart clásico con el remitente temporal del trial ni dar por universal la disponibilidad de aquel SID. Si el ejemplo permitido también falla, consultar soporte de Twilio con la petición sin claves y su error; confirmar además la habilitación del Sandbox/texto tras el upgrade antes de asumir ese coste. La restricción documentada de texto libre del trial sigue vigente, pero no equivale a diagnosticar una plantilla inválida.

**Resultado de esa comprobación de control.** El usuario volvió a Try out WhatsApp y obtuvo éxito con la plantilla actual del entorno: referencia de mensaje, errorCode nulo y queued. La aceptación funciona en el trial nuevo; el texto de esa respuesta proviene de la plantilla y no demuestra salida personalizada. Para restablecer esa modalidad en el sistema, utilizar [la configuración de retorno documentada](conexion-whatsapp-fase-6.md#retorno-exitoso-al-ejemplo-del-trial-nuevo). El siguiente paso para el agente continúa siendo resolver la habilitación de Body y verificarlo con un intercambio real. No se presupone que actualizar la cuenta habilite la plantilla clásica rechazada.

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

En esta cuenta el acceso ya se comprobó y el envío gratuito TEXT fue rechazado. El entorno es el Sandbox de la consola anterior, no otro Sandbox nuevo. No repetir los envíos Body mientras siga Trial. Revisar los datos de facturación del paso 2 y, después de la actualización, reabrir ese mismo Sandbox con la misma cuenta.

### 2. Actualizar a PAYG si es necesario

En tu cuenta actual de Twilio, pulsa **Upgrade** o busca esa palabra en la consola. Completa tus datos reales de facturación y dirección; elige **Pay-as-you-go** si aparecen varias modalidades. Añade el método de pago y revisa el importe final. Desactiva Auto Recharge y confirma únicamente cuando estés conforme con el coste mostrado. El pago lo realizas tú en Twilio.

Comprueba que esa misma cuenta ya figura como actualizada y abre de nuevo el Sandbox clásico. En la consola anterior, el recorrido habitual es **Messaging → Try it out → Send a WhatsApp message → Sandbox settings**; también puedes usar el enlace del Sandbox de esta guía. Si no aparece, resuelve su acceso con soporte antes de comprar un número propio. [Ubicación de Upgrade](https://www.twilio.com/docs/usage/trials#upgrade-your-account), [enlace oficial del Sandbox](https://www.twilio.com/console/sms/whatsapp/sandbox).

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
| Error 21654 — ContentSid requerido | En general, revisar si se enviaron ContentVariables sin ContentSid. Nuestro modo TEXT no envía ninguno de esos campos: en la cuenta Trial comprobada, contrastar la restricción actual que exige plantilla; no añadir un ContentSid a TEXT para convertirlo en otra prueba de plantilla. [21654](https://www.twilio.com/docs/api/errors/21654). |
| Error 21655 — ContentSid inválido | En un envío de plantilla, revisar formato HX, existencia y cuenta propietaria. El usuario aportó el SID del Quickstart clásico; el formato es válido, pero la disponibilidad para su cuenta no está comprobada. Contrastar el ejemplo permitido de Try out WhatsApp y resolver la plantilla antes de asumir que una recarga la habilita. [21655](https://www.twilio.com/docs/api/errors/21655). |
| La API sigue exigiendo ContentSid | Puede continuar la restricción de trial o estar usándose el remitente temporal anterior. Comprueba la cuenta y el Sandbox activo; revisa el código exacto del error antes de repetir. |
| Error 63015 | Tu teléfono debe incorporarse a ese Sandbox; puede haber vencido su sesión. |
| Error 63016 o el sistema pide un mensaje nuevo | Envía desde el teléfono otro texto al Sandbox y confirma que se recibe antes de responder. |
| Entrada rechazada tras cambiar remitente | Revisa sender, Account SID, token vigente y URL pública exacta; la firma sigue siendo obligatoria. |
| Resultado incierto | Consulta Twilio antes de enviar otra respuesta; el primer intento podría haber sido aceptado. |

La prueba termina cuando un texto real entra, una respuesta propia sale y se confirma en el teléfono con su estado y referencia. Entonces se registra ese resultado en el plan y se continúa con modelo y agente. **A22 y la fase 6 completa siguen pendientes** hasta demostrar la reserva automática confirmada por el paciente.
