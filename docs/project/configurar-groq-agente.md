# Crear la cuenta Groq y probar el agente

06/10/2026. El agente utiliza openai/gpt-oss-20b por API desde el backend normal y trabaja con los pacientes, servicios y agenda actuales, que el usuario confirmó ficticios. No necesitas otra instalación para estas pruebas manuales.

**Estado verificado:** el usuario ya creó y guardó la clave privada. Se comprobó conexión e inferencia real, una reserva confirmada y su repetición sin duplicación, consulta de precio, datos incompletos y negación. [Resultados](prototipo-agente-groq-fase-6.md). No necesitas generar otra clave; reinicia el backend que utilizas normalmente para cargarla.

## Cuenta y modelo

1. Abre [Groq Console](https://console.groq.com) y registra tu cuenta mediante correo o el acceso que ofrezca. Completa la verificación y acepta las condiciones.
2. Mantén el plan **Free**. No actives Developer ni añadas un medio de pago para esta primera evaluación si tu cuota gratuita está habilitada.
3. Revisa que **openai/gpt-oss-20b** esté disponible para tu organización. Puedes seleccionarlo en Playground, pero esa selección no cambia el modelo de nuestro backend: lo controla el archivo de configuración. No hay que descargar modelos ni instalar otra aplicación.
4. En [API Keys](https://console.groq.com/keys), crea una clave llamada **OdontoCare desarrollo** y copia su valor privado cuando aparezca. No la compartas por chat ni en capturas.
5. En **Data Controls**, activa **Zero Data Retention** según las opciones de tu organización. El contexto y las herramientas se conservan en nuestro backend. [Inicio oficial](https://console.groq.com/docs/quickstart), [datos](https://console.groq.com/docs/your-data).

## Archivo privado del backend

Abre **backend/config/ai.local.properties**, preparado con un marcador y excluido de Git. Si falta, copia **ai.example.properties** de esa misma carpeta con ese nombre. Coloca exactamente:

~~~properties
odontocare.ai.enabled=true
odontocare.ai.provider=GROQ
odontocare.ai.model=openai/gpt-oss-20b
odontocare.ai.api-key=REEMPLAZAR_POR_TU_CLAVE_REAL
odontocare.ai.worker-enabled=true
odontocare.ai.max-model-calls=6
odontocare.ai.max-completion-tokens=1500
odontocare.ai.request-timeout-seconds=12
odontocare.ai.run-timeout-seconds=75
odontocare.ai.context-messages=6
~~~

Sustituye api-key por tu clave real, sin comillas. No escribas la clave en el frontend. La configuración se importa automáticamente en el perfil local; las claves de Twilio y PostgreSQL no cambian. En este tramo el agente prepara respuestas **sin enviarlas a WhatsApp**.

Reinicia el backend desde su carpeta con **./mvnw.cmd spring-boot:run** y conserva la ejecución habitual del frontend. Se utiliza **sistema_odontologo**, backend 8080 y frontend habitual. Flyway incorpora las tablas nuevas sin vaciar los datos existentes. La opción **Probar agente** necesita el permiso Probar y reintentar el agente IA, concedido inicialmente al administrador.

En el módulo WhatsApp pulsa **Actualizar agente**. Debe mostrar GROQ, el modelo seleccionado y configuración lista. Ese estado comprueba los campos locales; una ejecución Completada y pasos del modelo en la bitácora acreditan que se obtuvo respuesta. Si falta configuración, se muestran los campos pendientes y no se invoca el modelo.

## Caso inicial en la aplicación

1. En Configuración → Servicios, verifica que **Limpieza dental** y su categoría estén activos, tenga duración positiva y **Permitir reserva automática**. Debe estar asignado a un odontólogo y usuario activos, con jornada en la fecha solicitada.
2. En Pacientes, elige una ficha actual y anota su teléfono. Si el contacto tiene varias fichas, utiliza el nombre completo de la persona que recibirá atención.
3. En WhatsApp pulsa **Probar agente**. Indica ese mismo teléfono, nombre de contacto opcional y este texto, adaptando el nombre a la ficha:

~~~text
Hola, soy Carlos Ruiz Vega. La cita es para mí. Quiero reservar una limpieza dental para mañana a las 9:00 am.
~~~

La entrada se identifica como **prueba desde la aplicación**, no como recepción de Twilio. Se procesa con el modelo configurado y aparece en Seguimiento del agente. Pulsa **Ver bitácora** para revisar mensajes, herramientas, resultados, consumo y respuesta preparada.

Ya quedó una demostración real en tu agenda: **Carlos Ruiz Vega, Limpieza dental, Julia Aracelly Huaytalla Alarcon, 07/10/2026 de 09:00 a 10:00**, Confirmada, Prueba IA. En WhatsApp busca **+51900001002** y abre la conversación para revisar la propuesta, confirmación y repetición. Para crear otra prueba, elige una hora disponible: las 09:00 de esa fecha con Julia ahora están ocupadas.

Si faltan datos, pulsa **Añadir mensaje de prueba** en esa conversación e indícalos. Si no hay horario, solicita una alternativa devuelta por la agenda. Para un contacto nuevo se puede preparar una ficha provisional con el nombre informado; se crea únicamente al confirmar la propuesta.

Cuando haya propuesta, lee paciente, servicio, odontólogo, fecha/hora y duración. Copia su código y envía otro mensaje desde la prueba con el texto exacto indicado:

~~~text
CONFIRMO A1B2C3D4
~~~

Este código es solo un ejemplo: utiliza el de tu propuesta real. La propuesta vence en 30 minutos y no retiene el horario. Confirmar vuelve a validar actividad, servicio reservable, duración y disponibilidad. La cita aparece **Confirmada**, con origen **Prueba IA** cuando la confirmación se hizo desde la aplicación. Repetir una confirmación completada devuelve la cita existente.

## Mensajes reales de WhatsApp

Mantén el receptor del puerto 8082 y ngrok, y conserva la configuración de Twilio que ya recibe mensajes. Desde el teléfono autorizado envía al número de prueba configurado la solicitud con los datos del paciente. El agente procesará las entradas nuevas automáticamente después de guardarlas.

Consulta la conversación en la aplicación para leer la respuesta o propuesta. Como todavía no puede enviar texto personalizado, puedes leer allí el resumen y enviar desde tu teléfono **CONFIRMO código**. La cita se registra con origen **WhatsApp** si la confirmación entró realmente por ese canal. Esta prueba no simula que la respuesta llegó al teléfono y no cierra A22 ni fase 6.

## Verificaciones y errores

Comprueba una reserva completa, una pregunta de precio y «no me reserves todavía». Consultas y negaciones no deben crear citas. Prueba un horario ocupado; registra otra cita o desactiva el servicio después de proponer y comprueba que la confirmación se rechaza. Revisa la agenda y el historial. Una conversación no puede confirmar el código de otra.

| Resultado | Qué hacer |
|---|---|
| Falta clave o agente desactivado | Completa el archivo privado, enabled=true y reinicia el backend. |
| Clave o modelo rechazados | Revisa que la clave sea de Groq, esté vigente y permita openai/gpt-oss-20b. |
| Cuota alcanzada | Espera al menos un minuto y consulta límites en Groq; no se habilita pago automáticamente. |
| Límite de llamadas o salida | Envía datos más concretos y revisa el límite configurado y la bitácora. |
| Horario cambiado o propuesta vencida | Pide una nueva propuesta y confirma su resumen actualizado. |
| Proceso fallido | Puedes reintentar hasta tres intentos; si llegó otro mensaje, utiliza una solicitud nueva. |

El agente no accede al expediente, registra pagos ni cambia finanzas. Los cambios y cancelaciones de citas se derivan a recepción en este primer tramo. La precisión real de Groq se acredita con tu clave y estos intercambios; las pruebas automáticas del código con respuestas controladas verifican infraestructura y reglas, sin sustituir esa evaluación.
