# Fase 6, primer tramo: conexión de WhatsApp

05/10/2026 · Alcance 1.5 · Plan 2.2 · Estado: recepción y envío real de plantillas confirmados por el usuario; texto personalizado pendiente de habilitación de cuenta.

El usuario autorizó comprobar primero la conexión de mensajes, creó su cuenta Twilio y confirmó su funcionamiento real mediante la guía. Este tramo implementa entrada, consulta y salida; modelo, interpretación, herramientas, pacientes provisionales y reservas se desarrollarán después. A22 permanece pendiente.

## Entrada, consulta y salida

El módulo valida firma con Twilio SDK 13.0.1 usando la URL pública exacta y todos los parámetros; no confía en Host ni cabeceras reenviadas. Comprueba AccountSid, número receptor y participante autorizado. Persiste mensaje y auditoría antes de responder. El SID único evita duplicados concurrentes; una reentrega no amplía la ventana de respuesta. Se admiten SID SM/MM. Multimedia se marca no compatible sin descargar archivos ni enlaces.

La pantalla presenta conversaciones y mensajes con búsqueda, filtros y paginación en PostgreSQL: contacto, texto, fecha, sentido, referencia del proveedor, estado y errores. Administración y recepción reciben WHATSAPP_READ/WHATSAPP_WRITE, configurables y comprobados por el servidor. No se devuelve ninguna clave ni se importa el historial del teléfono.

La salida se registra como tarea antes de contactar al proveedor. Repetir la clave UUID con el mismo contenido devuelve el original; usarla para otro contenido o conversación produce conflicto. La plantilla elegida se conserva en la tarea. TEMPLATE utiliza el ContentSid permitido por el trial; TEXT admite texto propio cuando la cuenta lo permita. El sistema limita esta prueba a participantes autorizados que escribieron en las últimas 24 horas; recordatorios fuera de esa ventana se incorporan después.

Un trabajador toma una tarea con bloqueo de fila y procesa como máximo una cada cuatro segundos en esta instalación. No mantiene una transacción durante la petición HTTP, que tiene límites de conexión y respuesta y no sigue redirecciones. Una respuesta del sistema no implica por sí sola entrega al teléfono.

## Recuperación y estados

QUEUED = pendiente, SENDING = petición en curso y ACCEPTED = aceptación del proveedor. SENT, DELIVERED y READ se actualizan por avisos firmados. La URL de estado lleva el UUID de la tarea para enlazar incluso avisos que lleguen antes de la respuesta de API. Se conservan eventos y auditoría; una repetición no crea otra operación, y un aviso tardío no retrocede el estado ni borra el error previo.

Solo 429 admite reintento automático, hasta tres intentos con espera. Timeouts, 5xx o SENDING abandonado tras una caída pasan a UNKNOWN sin reenvío ciego, porque el proveedor podría haber aceptado el primero. Un aviso posterior puede resolverlo. Los rechazos definitivos son FAILED. No se vuelcan tokens, contenido, números ni respuestas de API en logs operativos.

## Persistencia, seguridad y API

V12 añade whatsapp_conversation, whatsapp_message y whatsapp_delivery_event, claves únicas, índices y permisos. No modifica registros previos de pacientes, citas, clínica ni finanzas. Controller, dto, service, repository y model tienen responsabilidades separadas. El repositorio JDBC concentra consultas paginadas y bloqueos.

| Ruta | Acceso y propósito |
|---|---|
| POST /api/v1/integrations/whatsapp/inbound | Evento firmado; persistir entrada |
| POST /api/v1/integrations/whatsapp/status | Evento firmado; estados de mensajes propios |
| GET /api/v1/whatsapp/connection | WHATSAPP_READ; configuración sin secretos, no certifica conexión externa |
| GET /api/v1/whatsapp/conversations | WHATSAPP_READ; página de conversaciones |
| GET /api/v1/whatsapp/conversations/{id} | WHATSAPP_READ; contacto |
| GET /api/v1/whatsapp/conversations/{id}/messages | WHATSAPP_READ; página, búsqueda, messageDirection y status |
| POST /api/v1/whatsapp/conversations/{id}/messages | WHATSAPP_WRITE y CSRF; registrar texto |
| POST /api/v1/whatsapp/conversations/{id}/test-reply | WHATSAPP_WRITE y CSRF; registrar plantilla |

Solo los dos webhooks POST quedan exentos de sesión/CSRF y exigen autenticidad Twilio. La recepción TEMPLATE responde 200 vacío: el nuevo trial no admite XML directo. TEXT devuelve Response XML vacío para el entorno clásico, sin contestación automática. La salida usa exclusivamente la API y la cola. [Respuesta asíncrona oficial](https://www.twilio.com/docs/api/errors/11200), [webhooks clásicos](https://www.twilio.com/docs/usage/webhooks/messaging-webhooks), [restricciones actuales](https://www.twilio.com/docs/usage/trials/try-out-whatsapp).

## Configuración y habilitación de texto propio

Deshabilitado por defecto. Las claves se guardan en config/whatsapp.local.properties ignorado, con ejemplo sin secretos e importación solo local; producción usa configuración externa. El receptor Node de pruebas escucha en 127.0.0.1:8082, admite únicamente las dos rutas, limita el cuerpo a 32 KiB y no reenvía cookies, autorización ni cabeceras de proxy. ngrok apunta a ese receptor con inspección desactivada.

El nuevo trial restringe envíos a plantillas. No se promete conversación libre ni se actualiza o paga una cuenta por iniciativa del sistema. [Guía paso a paso desde cero](conectar-whatsapp-prueba.md).

El usuario confirmó externamente teléfono autorizado → mensaje real → registro visible en el sistema → plantilla enviada desde la aplicación → estado de entrega y mensaje en el teléfono. Ahora se resuelve la habilitación de texto propio y después se continúa con agente y reserva. No se cierran A22 ni fase 6 con esta comprobación parcial.

## Verificación técnica

Pruebas exclusivamente en sistema_odontologo_test y puertos 8081/5174, con trabajador de envío deshabilitado y entrega simulada sin contactar al Sandbox. WhatsAppIntegrationTests comprueba firma/URL, cuenta/participantes, duplicados concurrentes, permisos/CSRF, paginación/filtros, claves, ventana, estados fuera de orden y recuperación. El receptor tiene una prueba Node de exposición de rutas, eliminación de credenciales y límite de cuerpo. La interfaz tiene escenarios de computadora, tablet, celular y denegación de acceso.

Resultados: 93 pruebas del servidor aprobadas, incluidas las 13 nuevas de WhatsApp; 13 escenarios de navegador aprobados (9 de configuración/permisos y 4 de WhatsApp); una prueba del receptor aprobada. Compilación frontend, lint y formato aprobados. Ninguna prueba contactó al Sandbox. La base local del consultorio no se vació ni recibió datos ficticios de estas verificaciones.

Se comprobaron en Tomcat real mensajes firmados con porcentajes, guiones bajos, tildes y emoji, búsquedas literales y recuperación íntegra del texto. Las pruebas del navegador seleccionan la representación visible de cada fila y esperan la búsqueda remota antes de verificar su página; se corrigieron selectores y esperas de pruebas, sin cambiar reglas del producto. Los cuatro escenarios de WhatsApp volvieron a aprobarse al actualizar las capturas.

La revisión visual y Axe no detectaron desbordamientos horizontales ni infracciones de WCAG 2 A/AA y 2.1 AA en las pantallas comprobadas, con teclado y controles táctiles. Capturas de datos ficticios: frontend/docs/verification/whatsapp/inbox-1440.png, inbox-768.png, inbox-390.png, conversation-1440.png, conversation-768.png y conversation-390.png. Las conversaciones se capturan en el viewport para conservar la posición real del diálogo.

Para reproducir E2E, cargar además src/test/resources/whatsapp-e2e.properties según el README backend: referencias y token ficticios, dominio example.test y worker-enabled=false. El receptor se comprueba con node --test --test-isolation=none scripts/whatsapp-webhook-gateway.test.mjs. Los informes locales están en backend/target/surefire-reports y las trazas del navegador en frontend/test-results, sin versionar credenciales.

## Prueba externa comunicada por el usuario

El 05/10/2026 el usuario informó que completó la configuración y comprobó recepción y salida por WhatsApp. Sus dos capturas muestran los textos entrantes de la guía con estado Recibido y referencias SM, y respuestas de plantilla con referencias MM, estado Leído e Intentos de envío: 1. En las capturas se observan entradas a las 15:19 y 15:21 y salidas a las 15:29 y 15:31. La llegada al teléfono fue confirmada por el usuario; no se atribuye una ejecución propia del agente de desarrollo ni una reserva a esa evidencia.

Resultado del primer tramo: recepción, persistencia, consulta, envío de plantilla y avisos de entrega comprobados externamente según esa evidencia. No se guardan claves ni se reproducen tokens en este registro. Las capturas del navegador versionadas arriba siguen siendo pruebas internas con datos ficticios, distintas de las aportadas por el usuario.

Siguiente comprobación: [texto personalizado y presupuesto de Twilio](probar-whatsapp-texto-personalizado.md). Se propone comprobar primero disponibilidad del Sandbox clásico y, si la restricción trial lo exige, PAYG con recarga mínima revisada por el usuario. El conector ya soporta TEXT con Body y la interfaz su formulario; no se cambiaron código, credenciales, cuenta ni modalidad de envío en este turno. La habilitación en la cuenta concreta y el texto real siguen pendientes. Agente, reserva, A22 y la fase 6 completa permanecen pendientes.

## Diagnóstico del intento de texto gratuito

El usuario probó el Sandbox clásico y confirmó recepción, pero informó rechazo de salida. La consulta de solo lectura a PostgreSQL encontró una salida TEXT con FAILED/21654, un intento y template_sid nulo. La configuración privada se inspeccionó únicamente para modo, remitente y habilitación, sin imprimir claves. TwilioSender envía From, To, StatusCallback y Body para TEXT; no añade ContentSid ni ContentVariables. El usuario corrigió la referencia: la petición nueva de la consola usa Body sin ContentSid y devuelve 21654, igual que el sistema; el 21655 inicial pertenece a otra petición no aportada.

Un GET autenticado de solo lectura a la cuenta configurada confirmó type=Trial y status=active; no se envió ningún mensaje ni se realizó un pago. La restricción publicada del trial nuevo requiere ContentSid de plantilla del proveedor, por lo que este acceso al Sandbox clásico no demuestra ni habilita texto libre en esa cuenta. Se mantiene como dependencia la habilitación real de la cuenta y se evita atribuir el rechazo de texto a un fallo de copia del SID de nuestro archivo. [Trial](https://www.twilio.com/docs/usage/trials/try-out-whatsapp), [21654](https://www.twilio.com/docs/api/errors/21654), [21655](https://www.twilio.com/docs/api/errors/21655).

No se cambió código ni configuración privada, no se vació la base del consultorio y no se repitieron pruebas de envío. La actualización PAYG continúa siendo una decisión del usuario. El 21655 de la otra petición necesitaría revisar su plantilla y cuenta; no se considera diagnosticado ni se afirma que PAYG lo corrija. El resultado de texto propio debe verificarse después de la habilitación y antes del agente.
