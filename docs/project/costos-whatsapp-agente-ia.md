# Costos referenciales de WhatsApp y agente de OdontoCare

Preparado el 12/10/2026 para la presentación de la versión sencilla. Servicios actuales: Kapso y GroqCloud con `openai/gpt-oss-20b`. Investigación de precios y lectura agregada de bitácora; no se compraron servicios, cambiaron planes, enviaron mensajes ni modificaron configuraciones durante esta estimación.

## Qué hay que pagar para activar o ampliar cada servicio

| Servicio | Modalidad | Activación o pago mínimo | Alcance |
|---|---|---|---|
| Kapso Free | Plataforma gratuita, créditos de envío separados | En la cuenta examinada por el usuario: US$5 de saldo, total US$5,55 con comisión | La recarga conserva Free: 2.000 mensajes mensuales y un número conectado, además del Sandbox. No convierte la cuenta en Pro. |
| Kapso Pro | Suscripción | US$25 al mes; los créditos/tarifas de Meta siguen separados | 100.000 mensajes mensuales y tres números conectados. No es obligatorio contratarlo para un solo número mientras se respeten los límites Free. |
| Groq Developer | Consumo por tokens | Sin recarga ni cargo inmediato para subir de Free: registrar un medio de pago válido | Mayor capacidad; consumo facturado por ciclo mensual o umbrales progresivos. La documentación indica cobro cuando el uso acumulado facturable alcanza al menos US$0,50 y un primer umbral progresivo de US$1. No se interpreta US$1 como recarga obligatoria. |

Fuentes: [planes de Kapso](https://kapso.com/pricing), [facturación de Groq](https://console.groq.com/docs/billing-faqs). El mínimo US$5/US$5,55 corresponde a la pantalla que verificó el usuario, no a un mínimo universal confirmado en la página pública de Kapso. Revisar el checkout antes de pagar.

## Supuestos para las tablas

- Todos los destinatarios son números peruanos; mensajes de texto de servicio dentro de 24 horas desde el último mensaje del paciente. No se incluyen marketing, plantillas fuera de ventana, audios, llamadas o recordatorios.
- Tarifa de servicio a Perú: **US$0,03 por respuesta entregada**, comprobada en los datos públicos de la [calculadora oficial de Kapso](https://kapso.com/whatsapp-api-pricing-calculator) el 12/10/2026. El número del remitente no determina la tarifa; lo hace el destino.
- Una reserva se modela con **cinco respuestas del asistente y cinco mensajes del paciente**, aproximadamente diez mensajes de plataforma. No es un consumo fijo: puede ser de cuatro a seis respuestas, o mayor si hay aclaraciones. Consultas sin reserva, cancelaciones, reprogramaciones y respuestas humanas también consumen mensajes.
- Groq: presupuesto prudente de **US$0,001 por respuesta** (US$1 por 1.000). La tarifa real se basa en tokens, no en mensajes: US$0,075 por millón de entrada y US$0,30 por millón de salida. Un turno puede contener varias llamadas al modelo; este presupuesto cuenta su suma y deja margen. No se incorpora ahorro por caché. [Precio del modelo](https://console.groq.com/docs/model/openai/gpt-oss-20b).
- Conversión: **US$1 = S/3,457**, venta referencial publicada por [SBS al 09/10/2026](https://www.sbs.gob.pe/app/pp/SISTIP_PORTAL/Paginas/Publicacion/TipoCambioPromedio.aspx), último dato mostrado al consultar. Es una equivalencia informativa; la tarjeta o banco puede usar otra cotización.
- Importes redondeados a centavos. Se excluyen impuestos, otros cargos del banco y márgenes cambiarios del proveedor. La facturación de una cuenta WhatsApp en otra moneda puede incorporar el margen indicado por Kapso; no se garantiza que sea USD. El alojamiento, dominio, implementación, mantenimiento y precio de venta del software se cotizan aparte.

### Fundamento de la estimación de IA

La lectura de `sistema_odontologo` se limitó a cantidades agregadas de tokens de mensajes reales KAPSO, excluyendo APP_TEST. Se encontraron 55 ejecuciones completadas: medias redondeadas de 4.239 tokens de entrada y 231 de salida, aproximadamente US$0,000387 por respuesta con las tarifas anteriores. En 17 ejecuciones completadas posteriores al 08/10, 22:00 Lima, las medias fueron 2.805/89, aproximadamente US$0,000237 por respuesta; percentiles 90 de 8.348/290. Una preparación de propuesta registrada consumió 8.348/290, aproximadamente US$0,000713.

Esto es una muestra de desarrollo, no una muestra representativa de pacientes reales ni una conciliación de factura. Algunas respuestas se resuelven sin modelo y otras usan herramientas o reintentos. Los máximos de la muestra general fueron 18.049/2.383 tokens; no se garantiza que cada respuesta cueste menos de US$0,001. El presupuesto unitario elegido supera las medias observadas, pero las conversaciones prolongadas pueden aumentar el costo. No se deduce que los errores o llamadas sin uso registrado sean siempre gratuitos.

### Capacidad publicada de Groq Developer frente a Free

Comparación verificada el 12/10/2026 para `openai/gpt-oss-20b` en las páginas oficiales de [límites Free](https://console.groq.com/docs/rate-limits) y [modelos y límites Developer](https://console.groq.com/docs/models):

| Límite | Free | Developer | Capacidad relativa | Incremento |
|---|---:|---:|---:|---:|
| Tokens por minuto | 8.000 | 250.000 | 31,25 veces | 3.025 % |
| Llamadas al modelo por minuto | 30 | 1.000 | 33,33 veces | Aproximadamente 3.233 % |

No son límites por conversación: se comparten a nivel de organización. La cuota de tokens contempla el trabajo del modelo, incluidos el contexto y las llamadas a herramientas. Una preparación observada sumó 8.638 tokens, frente a los 250.000 por minuto publicados para Developer; esto permite inferir un margen amplio para una conversación normal de reserva y el volumen inicial del odontólogo. No demuestra ausencia universal de esperas: conversaciones largas, concurrencia, reintentos y límites particulares de la cuenta pueden afectar la operación. Tras actualizar, revisar los valores efectivos en Settings → Limits y repetir una reserva real. No se realizó una actualización de plan ni una prueba de carga en este análisis.

Resumen para presentación: `docs/presentacion/costos-whatsapp-agente-ia.docx`, dos páginas con Arial y texto negro; conserva ambas tablas de costos y sus condiciones. Se verificaron importes y equivalencias, las cuatro tablas y las dos páginas mediante exportación local de Word y el rasterizador del paquete de documentos con Poppler, ya que el paquete no incluye LibreOffice para Windows. La copia versionada se conserva en `backend/docs/project/presentacion/`.

## Tabla 1 — capacidad de una recarga en el Sandbox actual

En el Sandbox se cobran respuestas entregadas sin la franquicia mensual del número compartido. [Regla oficial](https://docs.kapso.ai/docs/whatsapp/meta-message-billing#sandbox-service-messages). La capacidad puede utilizarse durante varios días o meses; no se exige consumir el saldo en un mes. Esta tabla no convierte los mensajes disponibles en reservas garantizadas.

| Saldo añadido a Kapso (US$) | Respuestas disponibles aprox. | Reservas equivalentes (5 respuestas) | Pago de recarga Kapso, con comisión (US$) | Consumo Groq estimado (US$) | Desembolso referencial total (US$) | Equivalente total (S/) |
|---:|---:|---:|---:|---:|---:|---:|
| 5 | 166 | 33 | 5,55 | 0,17 | 5,72 | 19,77 |
| 10 | 333 | 66 | 10,78 | 0,33 | 11,11 | 38,41 |
| 15 | 500 | 100 | 16,01 | 0,50 | 16,51 | 57,08 |
| 20 | 666 | 133 | 21,24 | 0,67 | 21,91 | 75,74 |
| 30 | 1.000 | 200 | 31,70 | 1,00 | 32,70 | 113,04 |

Kapso cobra por entrega: una recarga de US$5 permite 166 respuestas a US$0,03, consumo US$4,98 y residuo US$0,02. En las otras filas, el consumo de envío sería US$9,99/15/19,98/30. El cuadro muestra el pago de la recarga, no confunde su saldo residual con dinero gastado en mensajes.

US$5,55 fue verificado por el usuario, y US$10,78 aparecía en su captura. Para otros montos se estima la misma comisión mediante `(saldo + 0,30) / (1 − 0,044)`, redondeando hacia arriba; el checkout puede variar. Groq muestra consumo presupuestado, no una recarga, y su cobro puede acumularse. Mantener Free exige sumar mensajes entrantes y salientes de todas las conversaciones del mes, no solo reservas.

## Tabla 2 — uso mensual con el número propio del consultorio

Esta es la referencia adecuada para el odontólogo. Según [Kapso](https://docs.kapso.ai/docs/whatsapp/pricing-faq), en números de producción Meta otorga 1.000 mensajes de servicio entregados por número al mes, sin acumulación. El Sandbox no usa esa franquicia. Además, la plataforma Free tiene un límite separado de 2.000 mensajes totales, incluyendo entradas y salidas.

Supuesto: un número propio conectado, facturación base USD sin márgenes adicionales, cinco mensajes entrantes y cinco respuestas por reserva, ninguna otra conversación, respuesta dentro de la ventana de atención y uso limitado a texto. La franquicia corresponde a mensajes que Meta clasifique dentro del beneficio. Costos mensuales estimados **antes de comisiones de recarga e impuestos**:

| Reservas al mes | Respuestas del agente | Mensajes totales aprox. | Plan Kapso | Kapso plataforma + tarifas de envío (US$) | Groq estimado (US$) | Total mensual (US$) | Equivalente (S/) |
|---:|---:|---:|---|---:|---:|---:|---:|
| 30 | 150 | 300 | Free | 0,00 | 0,15 | 0,15 | 0,52 |
| 50 | 250 | 500 | Free | 0,00 | 0,25 | 0,25 | 0,86 |
| 100 | 500 | 1.000 | Free | 0,00 | 0,50 | 0,50 | 1,73 |
| 150 | 750 | 1.500 | Free | 0,00 | 0,75 | 0,75 | 2,59 |
| 200 | 1.000 | 2.000 | Free, al límite | 0,00 | 1,00 | 1,00 | 3,46 |
| 250 | 1.250 | 2.500 | Pro | 32,50 | 1,25 | 33,75 | 116,67 |
| 300 | 1.500 | 3.000 | Pro | 40,00 | 1,50 | 41,50 | 143,47 |

A 250 reservas: US$25 de Pro + 250 respuestas que superan la franquicia × US$0,03 = US$32,50; agregar US$1,25 presupuestados de Groq. A 300: US$25 + 500 × US$0,03 = US$40, más US$1,50 de Groq. Las filas no son cotizaciones garantizadas ni un compromiso de gratuidad: otras consultas pueden hacer que Pro sea necesario con menos reservas. La fila 200 no deja margen para una consulta adicional. La plataforma no cuenta cada llamada a herramientas como un mensaje de WhatsApp.

Aunque el consumo estimado de envío sea cero, puede ser conveniente conservar saldo operativo positivo; no se presupone que todas las cuentas puedan despachar con saldo cero. La configuración y sincronización de facturación deben estar correctas. Una recarga inicial puede permanecer disponible para futuros cargos y no debe presentarse como cuota mensual obligatoria.

## Propuesta para presentar hoy

Para un odontólogo con volumen inicial bajo: mantener Kapso Free mientras el tráfico real deje margen dentro de 2.000 mensajes mensuales; conectar su propio WhatsApp Business y utilizar Groq Developer por capacidad, con límite de gasto opcional de US$5 al mes para IA. Ese límite es un control, no una cuota fija. [Límites de gasto de Groq](https://console.groq.com/docs/spend-limits).

Para presupuestar a 100–150 reservas mensuales: consumo referencial Groq US$0,50–0,75 y envío potencialmente dentro de la franquicia, sujeto a consultas adicionales y clasificación real. Para dar una oferta con mayor margen de operación y sin depender del pequeño cupo de plataforma Free: Kapso Pro US$25/mes y presupuesto Groq US$5/mes, aproximadamente US$30 (S/103,71) reservados, más cualquier tarifa de Meta que exceda la franquicia, recargas/comisiones e impuestos. Los US$5 de Groq son margen de presupuesto, no necesariamente consumo efectivo.

La conexión profesional todavía requiere preparar el número propio y verificar las condiciones de Meta/Kapso. El conector actual identifica Sandbox y limita participantes; no se presenta como habilitado para cualquier paciente solo por pagar. Es una preparación posterior; esta investigación no implementa fase 9 ni recordatorios. [Conexión y coexistencia con WhatsApp Business app](https://docs.kapso.ai/docs/platform/setup-links/create-and-configure#connection-types).

El costo del agente no incluye el alojamiento del sistema, compra de número si se necesitara, instalación, soporte ni la licencia comercial que acuerde el desarrollador con el odontólogo. No se promete disponibilidad garantizada por el solo hecho de pagar los servicios.
