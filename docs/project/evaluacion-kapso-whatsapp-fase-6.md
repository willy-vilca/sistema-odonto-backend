# Evaluación de Kapso para completar las pruebas de WhatsApp

06/10/2026 · Plan 2.7 · Alcance 1.7. Investigación de documentación pública y revisión del código local. **Kapso es un candidato técnicamente viable para pruebas; su integración y gratuidad efectiva todavía no se han comprobado con una cuenta.** No se crearon cuentas, cambiaron credenciales, compraron créditos ni enviaron mensajes durante esta evaluación. Twilio sigue siendo el conector implementado.

El usuario informó que probó el agente con entradas de la aplicación y de WhatsApp y que funcionó favorablemente; confirma conservar GroqCloud/openai/gpt-oss-20b. No informó resultados individuales para cada criterio ni entrega de respuestas personalizadas por WhatsApp. Fase 6 y A22 permanecen abiertos.

## Oferta comprobada y correcciones

| Afirmación investigada | Resultado documental |
|---|---|
| Plan Free de US$0 al mes y 2 000 mensajes mensuales | Confirmado. El cupo cuenta entradas y salidas; no son 2 000 conversaciones. |
| US$2 de bienvenida y registro sin tarjeta | Anunciado en la versión indexada de la página oficial de precios y la web de Kapso. Hay que verificar que el nuevo proyecto recibe el saldo: el anuncio no sustituye la cuenta. |
| Texto personalizado en el Sandbox | Admitido, con sesión activa del teléfono autorizado y ventana de atención vigente. Permite webhooks propios. |
| Plantillas desde el Sandbox | No admitidas. Requieren un número de producción conectado; el Sandbox sirve para texto y mensajes interactivos. |
| Número estadounidense dedicado gratis | Instant setup documenta una asignación gratuita para usuarios Free: primer proyecto predeterminado, una vez por usuario y sujeta a disponibilidad de números pre-verificados. Se libera tras 30 días sin mensajes de producción; eliminarlo no reinicia el beneficio. |
| Pruebas gratuitas indefinidamente dentro de las 24 horas | No se puede confirmar. La documentación de facturación dice que los mensajes de servicio entregados del Sandbox consumen créditos del proyecto; el cupo del plan no elimina ese cargo. |
| Una suscripción inferior a US$20 | Pro cuesta US$25 mensuales. Para este volumen se recomienda comenzar en Free, no contratar Pro. No se encontró un mínimo público fiable para recargas posteriores. |

Fuentes: [precios](https://kapso.com/pricing), [FAQ](https://docs.kapso.ai/docs/whatsapp/pricing-faq), [Sandbox](https://docs.kapso.ai/docs/how-to/whatsapp/use-sandbox-for-testing), [número dedicado](https://docs.kapso.ai/docs/platform/phone-numbers/instant-setup).

## Ventana de respuesta y presupuesto de pruebas

El cliente debe escribir primero para abrir o renovar la ventana de 24 horas. Dentro de ella se admite texto libre; fuera se requiere plantilla aprobada. Esta es una condición para enviar, independiente de que la entrega tenga coste. El Sandbox no permite usar una plantilla para reabrir el chat: pedir al participante un mensaje nuevo. [Envío de texto](https://docs.kapso.ai/docs/whatsapp/send-messages/text).

Kapso documenta un cargo propio por mensajes de servicio entregados desde el Sandbox, descontado de créditos según el mercado del destinatario, sin aplicar el cupo gratuito del número compartido. Sin saldo suficiente hay que recargar. [Facturación del Sandbox](https://docs.kapso.ai/docs/whatsapp/meta-message-billing#sandbox-service-messages).

Estimación condicionada: Kapso publica US$0.0300 para utilidad en Perú y dice que la tarifa de servicio de octubre coincide con utilidad. Si esa tarifa se aplica al Sandbox del proyecto y los US$2 de bienvenida están disponibles, alcanzarían para aproximadamente 66 respuestas entregadas de ese importe. Es una estimación sobre las cifras del proveedor, no un presupuesto garantizado. Cada respuesta del agente consume un mensaje; una reserva puede necesitar varias. [Tarifa publicada por país](https://kapso.com/guides/whatsapp-pricing/how-pricing-works/pricing-by-country).

Hay una discrepancia que no se debe ocultar: Kapso afirma nuevos cargos de servicio desde el 01/10/2026, mientras la [página pública de precios de Meta](https://whatsappbusiness.com/products/platform-pricing/) consultada aún describe respuestas de servicio gratuitas. Los enlaces técnicos de tarifas de Meta no pudieron recuperarse. No se declara verificado el cambio general de Meta ni se promete gratuidad; la condición explícita de cobro del Sandbox de Kapso basta para exigir observar saldo y uso reales.

## Viabilidad en OdontoCare

La API de Kapso usa HTTPS y JSON; Spring Boot puede consumirla con el cliente HTTP de Java existente, sin SDK de TypeScript ni otro servidor. El flujo conserva el agente propio Groq, herramientas administrativas, AvailabilityService, AppointmentService, PostgreSQL y bitácora. No se necesitan los agentes, modelos ni workflows alojados por Kapso. [API de texto](https://docs.kapso.ai/docs/whatsapp/send-messages/text).

La adaptación tiene complejidad moderada, concentrada en el módulo de WhatsApp. No consiste en cambiar solamente una dirección:

| Área local | Adaptación propuesta |
|---|---|
| WhatsAppProperties y conexión | Selección de proveedor activo, API key y phone_number_id de Kapso, secreto de webhook y participantes. Configuración privada; no copiar SID/token de Twilio. |
| TwilioSender y WhatsAppWorker | Separar contrato común de envío y cliente Kapso. Reutilizar cola, reintentos y estado incierto; enrutar cada mensaje con su proveedor guardado. |
| WhatsAppWebhookController, SignatureService y gateway 8082 | Receptor JSON para Kapso, validación HMAC SHA256 del cuerpo original mediante X-Webhook-Signature, comprobación del número configurado y límite de tamaño. El receptor sigue exponiendo únicamente webhooks. |
| WhatsAppConversationService y Repository | Traducir entradas y estados a contratos internos, guardar antes de encolar al agente y deduplicar por proveedor/referencia. |
| Persistencia | Las referencias actuales admiten solo 34 caracteres y las validaciones exigen SM/MM. Los identificadores wamid de WhatsApp requieren otro formato y longitud. Migración nueva para proveedor y referencias sin alterar las migraciones aplicadas ni perder historial Twilio. |
| Conversaciones del frontend | Marca de proveedor, referencias, estados y errores comprensibles; preservar permisos, búsqueda, filtros y paginación remotos. |
| Respuestas del agente | Actualmente AgentQueueService.finish guarda PREVIEW y no encola el envío. Conectar esa salida a WhatsAppOutbox, y guardar cita y tarea de confirmación de forma consistente. Este trabajo también estaba pendiente si se continuaba con Twilio. |

Documentación técnica: [seguridad de webhooks](https://docs.kapso.ai/docs/platform/webhooks/security), [eventos](https://docs.kapso.ai/docs/platform/webhooks/message-events), [guía desde Twilio](https://docs.kapso.ai/docs/migrate/from-twilio).

No trasladar el número compartido de Twilio ni seguir los pasos de eliminación de un WABA de producción de la guía de migración: nuestro caso empieza con el Sandbox propio de Kapso. Conservar registros existentes y distinguir el proveedor evita enviar pendientes de Twilio por otro canal o perder sus referencias. Si un evento no aporta un teléfono resoluble, no vincularlo por suposición a una ficha.

## Prueba inicial recomendada antes de una migración definitiva

1. Crear una cuenta Free y su primer proyecto; comprobar saldo de bienvenida, límite y opción de desactivar uso del contenido para mejora de modelos.
2. Activar el Sandbox con el teléfono de prueba y el código de seis caracteres; el código vence a los 15 minutos. No se exige número empresarial propio para este paso.
3. Enviar un texto desde el teléfono y responder desde el mecanismo de prueba disponible en Kapso dentro de la ventana. Comprobar entrega efectiva, créditos y uso. Esto aún no prueba el agente integrado.
4. Confirmados acceso y presupuesto, incorporar el adaptador, webhook y persistencia. Probar recepción real, respuesta personalizada, estados, firma incorrecta y eventos repetidos.
5. Conectar las respuestas de Groq y completar solicitud → alternativas/datos → propuesta → confirmación → cita → respuesta entregada. Comprobar conflicto con recepción, reintentos y recuperación en pruebas internas, sin carga sobre el Sandbox.
6. Para recordatorios fuera de ventana, evaluar después el número conectado y las plantillas; el Sandbox por sí solo no completa ese requisito de fase 8.

No hay una prueba externa de Kapso realizada en este turno. El resultado documental justifica probar el plan Free antes de pagar Twilio, pero no autoriza una compra ni declara terminada la integración.

## Condiciones para datos reales posteriores

La política de Kapso habilita por defecto el uso de Customer Content para mejora de modelos/producto en proyectos Free y permite desactivarlo. Sus términos restringen determinados datos regulados, incluidos PHI sujetos a HIPAA, salvo acuerdo escrito. Esto no cambia las pruebas ficticias autorizadas; antes de elegirlo para un consultorio real deben revisarse configuración y contrato aplicables. No se afirma que HIPAA rija automáticamente en Perú. [Privacidad](https://kapso.com/privacy), [términos](https://kapso.com/terms).
