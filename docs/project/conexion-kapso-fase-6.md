# Conexión manual de Kapso: implementación y verificación

07/10/2026 · Alcance 1.8 · Plan 2.8. Tramo autorizado: recibir mensajes y enviar texto personalizado desde OdontoCare. **Conexión manual comprobada con WhatsApp real; el agente no se conecta a Kapso en este tramo y A22 sigue pendiente.** Backend y frontend trabajan en ramas kapso; main conserva la implementación anterior.

## Funciones entregadas

Módulo kapso por capas: configuración privada, repositorio SQL, servicios de mensajes y recepción, cliente HTTP y controlador de webhook. Reutiliza la bandeja, permisos, contratos administrativos y paginación de WhatsApp. La selección local ocurre con odontocare.kapso.enabled=true; los trabajadores de Twilio y del agente se pausan mientras está activo, aunque sus archivos privados sigan configurados. Las pruebas y reintentos de IA se rechazan durante este modo.

Recepción POST /api/v1/integrations/kapso/events, JSON v2, firma HMAC SHA256 sobre los bytes originales, número interno configurado y participante autorizado. Se aceptan textos; otros formatos se registran como no admitidos sin descargar archivos. Contactos sin teléfono verificable se omiten con trazabilidad, sin asociar fichas por suposición. El receptor 8082 solo expone webhooks y preserva bytes y encabezados de firma, sin reenviar cookies o autorización de sesión.

Eventos y solicitudes repetidos se controlan con referencia estable y registro persistente. La respuesta se guarda antes de enviar, se vuelve a comprobar la ventana de 24 horas y se conservan estados separados de aceptación, envío, entrega y lectura. Autenticación, saldo o sesión producen errores seguros; 429 tiene hasta tres intentos y un resultado incierto no se repite automáticamente.

Los recibos de entrega se conservan si llegan antes de la respuesta HTTP del envío. Un bloqueo por referencia serializa la vinculación; Leído no retrocede a Enviado. No se incluyen plantillas, workflows, pacientes, reservas, herramientas de IA ni importación del historial anterior.

## Persistencia y regreso a Twilio

Flyway V16 agrega únicamente kapso_conversation, kapso_message y kapso_webhook_event. Conserva las tablas, índices y restricciones de Twilio. Los registros de Kapso admiten referencias wamid de hasta 512 caracteres, separadas de los SID anteriores.

Cambiar odontocare.kapso.enabled a false y reiniciar selecciona de nuevo Twilio. También se puede volver a main en ambos repositorios: main no importa kapso.local.properties y las tablas anteriores no se alteraron. Su exclusión local de Git conserva la protección del archivo privado al cambiar de rama. No se eliminan registros ni se reenvían pendientes de otro proveedor.

Credenciales y dominio se guardan en backend/config/kapso.local.properties; el ejemplo versionado no contiene claves. [Guía completa](conectar-kapso-prueba.md).

## Comprobación externa

El usuario creó la cuenta Free y mostró recepción de texto personalizado desde Kapso. Durante la implementación guardó API key y phone_number_id, configuró ngrok y creó el webhook. Las consultas de solo lectura verificaron clave válida, Sandbox correcto, webhook activo de tipo kapso, dirección y cinco eventos requeridos. Se comprobó la coincidencia del secreto sin mostrarlo.

El usuario envió desde su teléfono **Hola, esta es una prueba de conexión con odontocare**. El evento auténtico atravesó Kapso y el túnel; inicialmente fue rechazado porque el Sandbox usa estado delivered para una entrada, mientras el ejemplo documental mostraba received. Se corrigió la clasificación de entradas conservando firma, dirección e identidad, y se reprocesó el mismo evento con sus bytes, firma y referencia originales. No se creó un mensaje simulado.

| Comprobación real | Evidencia |
|---|---|
| Entrada inicial persistida | 6556cddd-86d6-486b-9225-27b9f78c7fbc, texto del usuario, Recibido. |
| Respuesta desde el conector | 8bfc31a2-0e03-434e-ad84-f0a59f01bb20, texto personalizado, un intento, Leído. |
| Texto enviado | Hola Willy. Este mensaje personalizado fue enviado desde OdontoCare mediante Kapso. Es una prueba de conexión manual, sin agente IA. |
| Estados autenticados | Sent, Delivered y Read, con HTTP 200 en el receptor. |
| Confirmación del usuario | Informó «Sí, lo recibí y responderé». |
| Entrada nueva tras corregir | b34340f3-987e-48ab-aefd-ed6a2b6f31ec, «Confirmo que recibí el mensaje personalizado», recibida por el webhook real sin reprocesamiento local. |
| Sin efectos de IA o negocio | Se conservaron 12 ejecuciones previas del agente, nueve pacientes, once citas y once movimientos financieros. |

La salida se encoló mediante el servicio de la instalación actual, con referencia estable, y el trabajador habitual la envió por API real. No se modificaron usuarios ni se utilizaron modelos. No se recargó saldo ni se contrató otro plan; el uso puede consumir créditos del proveedor y no se afirma gratuidad ilimitada.

## Verificación interna y visual

**123 casos distintos del servidor verificados sin fallos:** regresión completa de 122 y actualización posterior de los 16 casos de Kapso, incluyendo la nueva variante de entrada delivered. Cubren firma exacta, identidad, límites, Unicode, duplicados concurrentes, idempotencia, ventana al guardar/despachar, lotes acotados, orden, permisos/CSRF, referencias largas, estados anticipados, fallos seguros y aislamiento de Twilio/IA.

**Dos pruebas del receptor aprobadas**, incluyendo JSON Unicode exacto y exclusión de encabezados de sesión. **Diez escenarios de navegador aprobados:** tres nuevos de Kapso a 1440, 768 y 390 píxeles y siete de regresión de Twilio/agente con Kapso desactivado. Formularios conservan texto y clave al reintentar; listas consultan páginas y filtros remotos. Se revisaron teclado, foco, tacto, ausencia de desbordamiento y accesibilidad automática. Capturas en frontend/docs/verification/kapso con resultados controlados de interfaz; no se presentan como inferencia ni mensajes externos.

Se guardó un respaldo antes de aplicar V16 a sistema_odontologo, conservando los registros actuales. Las pruebas que limpian datos usaron únicamente sistema_odontologo_test y claves ficticias con envíos externos desactivados.

## Estado y siguiente tramo

Conexión manual implementada y comprobada: entrada real → persistencia → respuesta personalizada → entrega/lectura → respuesta nueva del participante. Queda la revisión manual del recorrido de la interfaz por el usuario. Conectar Groq y sus respuestas persistidas requiere una autorización posterior; no se desarrolla dentro de este tramo mínimo. Fase 6 y A22 permanecen abiertos.

Fuentes: [texto](https://docs.kapso.ai/docs/whatsapp/send-messages/text), [eventos](https://docs.kapso.ai/docs/platform/webhooks/message-events), [seguridad](https://docs.kapso.ai/docs/platform/webhooks/security), [webhooks](https://docs.kapso.ai/docs/platform/webhooks/overview), [Sandbox](https://docs.kapso.ai/docs/how-to/whatsapp/use-sandbox-for-testing).
