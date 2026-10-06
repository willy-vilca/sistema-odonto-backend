# Selección de IA y prototipo previo al upgrade de WhatsApp

06/10/2026. Investigación inicial del alcance 1.6 y plan 2.5, conservada como referencia de la selección. En esa investigación no se hicieron compras ni inferencias. Decisión posterior vigente: el usuario eligió GroqCloud/openai/gpt-oss-20b, autorizó la instalación actual con datos ficticios y guardó la clave. El agente está implementado y verificado mediante inferencia real; consultar [evidencia actual](prototipo-agente-groq-fase-6.md) y [configuración](configurar-groq-agente.md), alcance 1.7 y plan 2.6.

## Recomendación

Comenzar con **GroqCloud Free y el modelo openai/gpt-oss-20b mediante API**. Es el candidato inicial que mejor combina prueba gratuita, herramientas, bajo coste y despliegue sencillo para nuestro alcance. Es un modelo de OpenAI alojado por Groq; se usa la cuenta y clave de Groq, no una suscripción de ChatGPT ni la API comercial directa de OpenAI.

Groq lo clasifica como modelo de producción y publica tarifa estándar de US$0.075 por millón de tokens de entrada y US$0.30 por millón de salida. [Modelo y precios](https://console.groq.com/docs/models). La disponibilidad documental no demuestra precisión en nuestras reservas: se evaluarán español, negaciones, datos faltantes y confirmaciones antes de habilitar escrituras.

## Comparación verificada al 06/10/2026

Precios estándar en USD por un millón de tokens, fuera de franquicias gratuitas, sin descuentos de caché/batch ni cargos adicionales. La salida y el razonamiento facturable deben medirse según proveedor; no equivalen a un precio fijo por cita.

| API / modelo candidato | Prueba API gratuita | Entrada / salida | Valor para este proyecto |
|---|---|---:|---|
| Groq / openai/gpt-oss-20b | Free con límites de solicitudes y tokens | 0.075 / 0.30 | Primera opción; herramientas y acceso posterior por consumo sin recarga inicial obligatoria documentada |
| Mistral / ministral-8b-2512 | Free mode sin tarjeta; créditos y cuotas de la cuenta | 0.15 / 0.15 | Segunda opción para comparar comprensión y herramientas |
| Gemini / gemini-3.5-flash-lite | Free Tier con cuotas del proyecto | 0.30 / 2.50 | Alternativa estable; revisar tratamiento de datos y límites de usuarios en modalidad gratuita |
| OpenAI / gpt-6-luna | Su ficha indica Free no soportado | 0.10 / 0.50 | Alternativa económica de pago; no es la primera elección para comenzar sin inversión |

Fuentes: [Groq](https://console.groq.com/docs/models), [Ministral 8B](https://docs.mistral.ai/models/ministral-3-8b-25-12), [precio Gemini](https://ai.google.dev/gemini-api/docs/pricing), [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna). No se afirma que todos tengan la misma calidad ni que una prueba en un chat web permita integrar gratis su API.

Mistral permite activar la API en Free mode por defecto sin tarjeta. Su página actual anuncia US$10/mes en créditos API; revisar la asignación efectiva y su presupuesto compartido en la cuenta. No se encontró un depósito mínimo público general para activar PAYG, por lo que no se inventa una cifra. [Activación](https://docs.mistral.ai/getting-started/quickstarts/studio/activate-and-generate-api-key), [plan Free](https://mistral.ai/pricing/), [facturación](https://docs.mistral.ai/admin/billing-usage/billing).

Google recomienda modelos 3.5 Flash-Lite o 3.8 Flash para proyectos nuevos; no seleccionar versiones 2.5 por memoria de guías antiguas. El 3.5 Flash-Lite es estable, con herramientas y salida estructurada. Las cuotas gratuitas se consultan en AI Studio; no hay un número universal garantizado de solicitudes por día. Para nuevas cuentas de pago, la documentación describe prepago con compra mínima de US$5, sujeto al flujo mostrado en la cuenta. [Catálogo](https://ai.google.dev/gemini-api/docs/models), [cuotas](https://ai.google.dev/gemini-api/docs/rate-limits), [facturación](https://ai.google.dev/gemini-api/docs/billing).

## Cuotas y pago de Groq

La tabla Free publicada para gpt-oss-20b indica **30 solicitudes/minuto, 1.000 solicitudes/día, 8.000 tokens/minuto y 200.000 tokens/día**. Se aplican a la organización y deben comprobarse en la consola. Una conversación usa varias llamadas y el límite de tokens puede alcanzarse antes que el de solicitudes. No son 1.000 citas gratuitas por día ni una capacidad garantizada. [Límites](https://console.groq.com/docs/rate-limits).

Para Developer se añade un medio de pago, sin cargo inmediato al actualizar. Groq factura consumo al terminar el ciclo o alcanzar umbrales progresivos que comienzan en US$1; no describe una compra inicial de saldo como la de Twilio. El cambio a pago será decisión del usuario y no se realizará automáticamente al agotar Free. [Facturación oficial](https://console.groq.com/docs/billing-faqs).

Ejemplo calculado: **1.000 llamadas** con 2.000 tokens de entrada y 500 de salida facturable cada una producen US$0.15 de entrada más US$0.15 de salida: **US$0.30 de consumo base**. Es un ejemplo, no una factura garantizada ni 1.000 reservas; una reserva requiere varias llamadas, contexto repetido y posibles reintentos. Incluir razonamiento facturable, medir uso y consultar condiciones de facturación. Durante Free no se consume saldo pagado dentro de la cuota habilitada.

## Herramientas y datos

gpt-oss-20b admite llamadas a herramientas y JSON, pero no llamadas paralelas; utilizaremos un ciclo secuencial corto y acotado. Groq no admite actualmente combinar Structured Outputs con tool use: no prometer argumentos garantizados por un esquema estricto en esa combinación. Validaremos nombres de herramientas, argumentos, resultados y condiciones en Spring Boot. [Herramientas](https://console.groq.com/docs/tool-use/overview), [limitación de salida estructurada](https://console.groq.com/docs/structured-outputs).

Groq documenta que no entrena con entradas/salidas sin permiso del cliente, y permite activar Zero Data Retention en Data Controls. Esto no elimina los metadatos de uso ni sustituye revisar las condiciones al usar datos reales. Activar ZDR para el prototipo; pruebas con datos ficticios, no enviar expedientes, documentos clínicos ni teléfonos/identificadores reales al modelo. La asociación de contacto y paciente se resuelve localmente. [Contrato](https://console.groq.com/docs/legal/services-agreement), [controles](https://console.groq.com/docs/your-data).

Gemini gratuito puede utilizar entradas y salidas para mejora y revisión humana; sus términos limitan usuarios y aplicaciones accesibles a menores y excluyen práctica clínica/consejo médico. Mistral Free también puede utilizar contenido para entrenamiento, sujeto a sus controles aplicables. Estas condiciones hacen importante revisar datos/usuarios además del precio y refuerzan elegir Groq para esta primera evaluación. Ninguna opción convierte al agente en profesional clínico. [Condiciones Gemini](https://ai.google.dev/gemini-api/terms), [uso de datos Mistral](https://help.mistral.ai/en/articles/347617-do-you-use-my-user-data-to-train-your-artificial-intelligence-models).

## Despliegue propuesto

El **agente es código propio del backend Spring Boot**: conserva solicitud, contexto administrativo, límites, herramientas y bitácora. El **modelo vive en la infraestructura del proveedor** y se consulta por HTTPS. No se añade un servidor de modelos ni un agente gestionado del proveedor.

~~~mermaid
flowchart LR
    W[WhatsApp y receptor] --> P[Mensaje persistido]
    P --> A[Agente en Spring Boot]
    A --> M[Modelo por API Groq]
    M --> A
    A --> T[Herramientas validadas]
    T --> D[Agenda en PostgreSQL]
    T --> A
    A --> B[Bitácora y respuesta preparada en la aplicación]
~~~

El backend envía el contexto mínimo y las declaraciones de herramientas. El modelo propone llamadas; nuestro servidor las valida y ejecuta mediante los servicios existentes. El modelo no recibe acceso directo a PostgreSQL, SQL libre, credenciales, historia clínica ni finanzas. [REST compatible de Groq](https://console.groq.com/docs/openai).

Integración mínima prevista: cliente HTTP intercambiable del modelo, orquestador dentro del módulo del agente y adaptadores de herramientas de catálogo, disponibilidad y reserva. Mantener proveedor/modelo y clave configurables, sin crear adaptadores vacíos de proveedores no utilizados. No hace falta Python, GPU, n8n, embeddings, base vectorial ni un servicio independiente para este flujo. La clave quedará solo en configuración privada del backend; no en VITE_, Git o bitácora.

Alojar el modelo en el mismo servidor exigiría dimensionar memoria, cómputo, tiempos de respuesta y operación de otro proceso. El coste de hardware no se elimina porque los pesos sean gratuitos. Para pocas pruebas y un despliegue sencillo recomiendo API; el sistema sigue desplegándose como React, Spring Boot y PostgreSQL, con salida HTTPS hacia el proveedor. La latencia y disponibilidad externas se manejarán con tiempos máximos, reintentos limitados y solicitudes pendientes, no con reservas inventadas.

## Prototipo antes del upgrade de Twilio

El usuario pidió desarrollar y comprobar IA antes de pagar Twilio. No se espera a habilitar texto saliente para elegir/probar el modelo. Se mantienen los criterios finales del alcance.

1. Recibir un texto real de un participante de demostración por el conector ya validado, persistirlo y procesarlo de forma asíncrona con control de duplicados.
2. Interpretar intención y preferencias con el modelo, sin inferir autorización de cualquier mención de cita.
3. Invocar catálogo y disponibilidad real del backend, con búsqueda/paginación acotadas y horarios de los odontólogos.
4. Mostrar en la aplicación interpretación, herramientas, resultados y respuesta preparada, etiquetada **sin enviar a WhatsApp**.
5. Completar los datos y la confirmación en el escenario de demostración: el participante puede leer la propuesta en la aplicación y enviar otro mensaje de WhatsApp con los datos o confirmación. Las pruebas automatizadas pueden usar confirmaciones ficticias, marcadas como tales.
6. Crear la cita por la herramienta central cuando el paciente, servicio, profesional, fecha/hora y confirmación estén completos. Comprobar registro, idempotencia, historial, origen y conflictos en la agenda actual de desarrollo con datos ficticios, autorizada posteriormente por el usuario.

Ejemplo: «quiero reservar una cita para mañana a las 9:00 am para hacerme una limpieza dental» permite detectar reserva, mañana, 09:00 y servicio. El backend debe resolver mañana con su reloj y America/Lima, buscar el servicio activo/reservable y consultar el intervalo según su duración. Todavía no identifica inequívocamente paciente/profesional ni confirma el resumen completo: inicialmente genera una solicitud/propuesta. No inventar paciente, escoger arbitrariamente entre familiares, ocupar un horario inexistente ni crear una cita definitiva en la instalación real por esa frase.

Las reservas de prueba deben conservar esa misma regla: paciente de demostración, datos completos y confirmación de prueba explícita antes de ejecutar creación. El botón de un administrador puede simular confirmación desde la aplicación, identificada como prueba; no se presenta como confirmación real de un paciente ni como aprobación obligatoria de recepción. La disponibilidad y creación serán servicios reales, no respuestas simuladas del modelo.

Para aislamiento: sistema_odontologo_test, datos ficticios y perfil/archivo de configuración verificado; worker de salida WhatsApp deshabilitado. El receptor actual apunta al 8080: para pruebas directas al backend 8081 se debe configurar explícitamente ese destino, manteniendo exposición de solo los webhooks. Nunca activar dos destinos para el mismo evento ni copiar credenciales a registros. No limpiar la base local del consultorio para preparar el prototipo.

La creación central actual requiere adaptar origen/estado/auditoría para WhatsApp y validar bookableByAgent también al guardar. Añadir solicitud persistida y una creación interna que reutilice BookingRules/AppointmentService y conserve la confirmación, cita y tarea de respuesta de manera consistente. Hoy no existe procesamiento IA; agentEnabled sigue siendo false hasta implementarlo.

## Comprobación y elección definitiva

La primera evaluación cubrirá intención clara, consulta de precio sin reserva, negación, falta de datos, mañana y otros días relativos, varios odontólogos, contacto compartido, horario ocupado, repetición y fallo de herramienta/modelo. Medir éxito de herramientas, fechas correctas, latencia, número de llamadas y tokens. Un modelo barato que falla condiciones críticas no se da por suficiente; comparar el siguiente candidato con los mismos casos.

El prototipo verifica modelo → herramientas → persistencia en el sistema, pero **no cierra A22 ni fase 6**. La demostración completa sigue necesitando presentar el resumen y responder por WhatsApp, recibir confirmación inequívoca del paciente y persistir la cita con una respuesta real. El envío preparado en la aplicación no se registra como entregado al teléfono.

## Preparación manual para el siguiente tramo

Crear una cuenta en [Groq Console](https://console.groq.com), mantener el plan Free, crear una clave API en [API Keys](https://console.groq.com/keys) y revisar los límites del modelo. Activar Zero Data Retention en Data Controls. No habilitar Developer ni introducir una tarjeta para esta primera prueba si el Free disponible cubre los escenarios. No compartir la clave por chat; al implementar se preparará el archivo privado y su importación segura. [Inicio oficial](https://console.groq.com/docs/quickstart).

Esta preparación ya se ejecutó: config/ai.local.properties está ignorado por Git, el usuario guardó la clave y se comprobó el modelo real con herramientas y reserva confirmada. La prueba utilizó pacientes y agenda actuales ficticios, sin otra instalación. La salida personalizada por WhatsApp continúa pendiente; la implementación y evidencia no cierran A22.
