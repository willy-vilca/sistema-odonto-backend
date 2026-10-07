# Probar reservas autónomas con Kapso y Groq

07/10/2026. Modelo openai/gpt-oss-20b de GroqCloud. Se usa la instalación actual y su agenda con los datos ficticios autorizados. Credenciales privadas ya guardadas; no se requieren otras cuentas, un modelo local ni un servidor de IA adicional.

## Activación

En backend/config/kapso.local.properties se agregó **odontocare.kapso.agent-enabled=true**. Conservar enabled=true, worker-enabled=true, API key, phone_number_id, secreto, dominio y participante existentes. backend/config/ai.local.properties conserva enabled=true, worker-enabled=true y el modelo/clave configurados.

Reiniciar el backend actualizado. Mantener el receptor 8082 y ngrok hacia ese puerto; el webhook v2 y sus cinco eventos siguen siendo los mismos. Iniciar el frontend de la rama kapso. El agente escucha únicamente las entradas nuevas, no interpreta automáticamente el archivo histórico.

La tarjeta Agente IA muestra configuración lista y respuestas por WhatsApp. Ese estado comprueba campos; las ejecuciones Completadas y la entrega al teléfono comprueban el funcionamiento externo. Si falta el modelo, la conversación queda guardada sin invocar una clave inexistente.

## Demostración real de reserva

Desde el teléfono autorizado, escribir:

~~~text
Hola, quisiera reservar una limpieza dental para mañana a las 9:00 am.
~~~

Esperar la pregunta del agente y proporcionar la identidad:

~~~text
La cita es para mí, Willy Vilca Huaytalla.
~~~

El sistema busca esa ficha dentro de las vinculadas al teléfono. Si no se tiene ficha, solo prepara datos mínimos y la crea provisional al confirmar; una consulta de precio no crea pacientes. Si la hora no está libre, el agente ofrece alternativas calculadas por la agenda. Elegir una opción devuelta, por ejemplo «La opción de las 10:00 con Julia», adaptándola al resultado real.

Leer el resumen completo: paciente, servicio, odontólogo, fecha, hora y duración. La propuesta no retiene el horario y vence a los 30 minutos. Después responder:

~~~text
Sí, confirmo la cita.
~~~

También se puede enviar CONFIRMO más el código real de la propuesta. Un «sí» o «ok» aislado, una pregunta o una negación no son confirmaciones inequívocas. Para una reserva real, el resumen debe haber salido por WhatsApp antes de aceptar la confirmación.

Esperar la respuesta de cita confirmada en el teléfono. En OdontoCare → Agenda buscar la fecha indicada y comprobar paciente, servicio, profesional, intervalo completo y origen WhatsApp. En WhatsApp → conversación → Seguimiento del agente → Ver bitácora se consultan entrada, modelo, herramientas, resultados, cita y respuesta con estado de envío. En cola, Aceptado, Entregado y Leído son estados diferentes.

## Comprobaciones breves adicionales

| Mensaje | Resultado esperado |
|---|---|
| «¿Cuánto cuesta la limpieza dental? Solo quiero el precio.» | Precio vigente, sin paciente ni cita nuevos. |
| «Quiero una cita.» | Preguntas por los datos faltantes, sin reservar. |
| «No me reserves todavía.» | Respeta la negación; no crea una cita. |
| Pedir la misma hora ya ocupada | Ofrece horarios calculados libres y pide elegir. |
| Repetir la confirmación de una cita completada | Devuelve la cita existente, sin duplicación. |
| «¿Confirmo?» o «sí» | No reserva por una pregunta o respuesta ambigua. |

Las pruebas concurrentes de recepción contra agente y los fallos controlados se ejecutan en la base automática protegida, no como carga sobre el Sandbox. Cambiar servicio, duración o profesional después de la propuesta exige nueva validación; un conflicto conserva la reserva de recepción y ofrece alternativas cuando estén disponibles.

## Pruebas desde la aplicación

Probar agente permite enviar entradas ficticias sin pasar por WhatsApp. Quedan identificadas APP_TEST y sus respuestas como vistas previas: no consumen mensajes de WhatsApp. La confirmación de esas propuestas crea citas Prueba IA, no demuestra A22. La conversación real y las pruebas de aplicación no comparten su contexto de interpretación; no se confirma una propuesta de prueba mediante un mensaje real.

## Fallos y recuperación

La bitácora distingue errores de modelo, herramientas, validación y envío. La cuota del modelo tiene reintentos acotados; no se contrata otro plan automáticamente. Si un envío falla de forma confirmada antes de obtener referencia del proveedor, un usuario con permiso de WhatsApp puede pulsar Reintentar envío de respuesta. Eso reintenta solo la respuesta, conservando la cita y su referencia. Los resultados inciertos no se reenvían a ciegas: se comprueban en Kapso para evitar duplicar mensajes.

La cita, confirmación y tarea de respuesta se guardan en una transacción. Reiniciar no vuelve a reservar una cita ya guardada; los trabajos pendientes y sus referencias permanecen en PostgreSQL. Los mensajes consecutivos se agrupan brevemente para procesar los datos juntos, conservando cada entrada en la bitácora.

El agente no usa expedientes ni finanzas y no ejecuta cambios o cancelaciones de citas en esta fase; esos pedidos se derivan a recepción. Para mantener solo la conexión manual, cambiar agent-enabled a false y reiniciar. Las ramas kapso se conservan para esta integración.

El cierre de fase 6 exige ejecutar la demostración real y registrar sus resultados, además de las comprobaciones internas. Una vista previa o una cita creada por usuarios no sustituye ese recorrido.
