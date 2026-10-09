# Continuidad del paciente y responsable: diagnóstico de fase 7

08/10/2026, America/Lima. Groq/openai/gpt-oss-20b, Kapso; corrección supervised-v7.8. Datos ficticios de la instalación actual. No se compraron servicios ni se cambiaron credenciales.

## Incidencia real

La consulta para la hija Lucía Prueba Familia verificó GUARDIAN y ofreció horarios correctamente en la secuencia 61. Al elegir el 20/10/2026 a las 09:00, el participante recibió el aviso de derivación a recepción y pausa del asistente.

Ejecución `ab448654-6ff4-4d25-bfc2-3b8720effbb2`, secuencia 62, KAPSO/COMPLETED, un intento, supervised-v7.7; respuesta READ. Entrada `0fa712ac-7dfd-47a4-8ed0-df59eaa09688`; respuesta `fe6832da-e0d4-46b1-8753-cdffae8ac821`. Dos inferencias aceptadas, 3 906 tokens de entrada y 98 de salida; 5,525 segundos hasta persistir la respuesta.

El modelo llamó verificar_paciente; el servidor lo rechazó con «No tienes permiso para esta operación». A continuación llamó derivar_recepcion con motivo «Solicitud de reserva para menor sin identificación de paciente». El control pasó a HANDOFF/generación 7, sin responsable humano asignado. No hubo RATE_LIMIT, HTTP 429 ni fallo de autenticación del proveedor en esta ejecución: no atribuirla a un límite de cuota.

Los argumentos rechazados se conservaron redactados, no el nombre/relación concretos elegidos por el modelo. Por ello no se afirma retrospectivamente qué persona seleccionó. Sí se comprobó una reselección innecesaria: antes del nuevo mensaje el contexto ya tenía Lucía Prueba Familia/GUARDIAN, mientras la inferencia recibía principalmente mensajes y debía volver a escoger nombres y relación. La ficha del responsable no debe sustituir a la hija al continuar la misma solicitud.

Resultado funcional fallido aunque la ejecución terminara COMPLETED con derivación. No se creó propuesta real para Lucía, ficha ni cita. Permanecen 9 pacientes, 13 citas, 9 cargos y 11 movimientos; Willy CANCELLED/versión 2 y tres historiales.

## Corrección

- AgentIdentityService proporciona argumentos de continuidad solo para una verificación GUARDIAN vigente del mismo canal y conversación, cuando el mensaje actual nombra expresamente al mismo hijo. No hereda el paciente si falta su nombre, cambia el hijo, se nombran dos hijos, cambia el nombre completo, vence la verificación o se usa APP_TEST con otra fuente.
- El flujo vuelve a ejecutar verificar_paciente mediante AgentToolService, comprobando control, teléfono, relación de responsable y permisos actuales. Conserva el resultado en bitácora y punto de continuación antes de inferir. No concede permisos desde un recuerdo del modelo ni crea la ficha durante esta verificación.
- El modelo recibe los datos administrativos verificados y continúa con catálogo, horarios y propuesta. Se aclara que patient_name corresponde al hijo y que el nombre del padre identifica al responsable. Las nuevas selecciones siguen requiriendo identificar al paciente.
- Se rechazan respuestas que soliciten slot_id, patient_id, dentist_id, service_id, appointment_ref o UUID al paciente. El agente debe obtenerlos mediante herramientas; la corrección y su punto de continuación quedan persistidos bajo el límite de llamadas/tiempo existente. El paciente solo proporciona datos comprensibles de la reserva.
- Confirmación, disponibilidad final, integridad de agenda, aislamiento de APP_TEST y control humano se conservan. No se cambiaron esquema, modelo, claves o frontend.

## Verificación

La regresión nueva falló antes de corregir: faltaba la identidad verificada en la continuación. Registro local ignorado `phase7-guardian-before-fix-tests.log`. Después pasaron 86 pruebas del agente, cero fallos/errores/omitidos, incluidas 51 Kapso/agente y tres casos nuevos de continuidad, selección/canal/vigencia y revocación del responsable. El caso de elección también comprueba que una respuesta que solicita slot_id se bloquee y se sustituya por una propuesta obtenida con herramientas. Registro final `phase7-guardian-continuation-final-tests.log`; empaquetado aprobado en `phase7-guardian-package.log`.

Se hizo una vista previa Groq real, fuente APP_TEST y contacto ficticio de prueba, en la misma instalación. La preparación administrativa verificó a Lucía mediante la herramienta, sin modelo, ficha o cita; la elección del horario sí pasó por Groq real. La primera reproducción `b68cf4f9-b62d-47b4-b333-f92459ada2fa` mantuvo a Lucía, pero pidió un slot_id y no preparó propuesta: fallo funcional adicional, sin envío. Se corrigió el control de respuestas antes de repetir.

Vista previa final `a0c5e549-f79a-46c3-bb2c-6e226b4d6ad6`, COMPLETED, un intento, tres inferencias, sin pasos rechazados, 5 545 tokens de entrada y 221 de salida. Preparó Lucía/Limpieza/Julia/20/10/2026 09:00/60 minutos, propuesta APP_TEST/PENDING, sin aceptar. Tiempo medido del procesamiento: 2,589 segundos. No se declara ese tiempo como entrega en un teléfono ni esta prueba como demostración completa de WhatsApp.

Se compararon huellas de las filas completas de pacientes, contactos, citas, historiales, cargos y movimientos antes/después: todos iguales. También se conservaron íntegros el control de la conversación real y el número de mensajes salientes. No se enviaron mensajes, crearon pacientes/citas ni modificaron finanzas. Registros ignorados `phase7-guardian-groq-preview.log` y `phase7-guardian-groq-final-preview.log`.

## Reanudación real

Backend actualizado y salud UP, frontend y receptor/ngrok conservados. Mediante el servicio de supervisión y auditoría se devolvió únicamente la derivación automática de la ejecución 62, comprobando HANDOFF/generación 7, motivo, ausencia de responsable humano, pasos conocidos y ausencia de otra ejecución real posterior. Control AUTO/generación 8; sin tareas antiguas pendientes. La devolución invalida verificaciones anteriores y exige un mensaje nuevo; no se revive la solicitud fallida. Registro ignorado `phase7-guardian-resume.log`.

El participante repitió la elección completa por WhatsApp: secuencia 67/`a9be0c9e-78d0-4480-85a5-92e991da92ce`, COMPLETED/READ, dos intentos y cuatro inferencias. Preparó el resumen correcto de Lucía, 20/10 09:00, y la propuesta real 414B310E/PENDING. Esta ejecución recuperó un HTTP 429/TOKENS_PER_MINUTE con cinco segundos de espera antes de proponer, conservando resultados anteriores. 9 043 tokens de entrada, 246 de salida; 13,135 segundos hasta respuesta persistida. La cuota recuperada de esta repetición no es la causa del fallo anterior de identidad.

La propuesta se confirmó realmente antes de vencer: secuencia 68/`bbf32802-dcfe-4e47-8bc8-921ffe4240cd`, COMPLETED/READ y cero inferencias. Se creó una única ficha provisional Lucía (`bf95319a-999e-486c-9823-8b2d0d126d22`) y una única cita WHATSAPP/CONFIRMED `5d766bea-1d54-4c6f-bf92-c31651e41d0e`, 20/10 09:00–10:00, vinculando al teléfono del participante como contacto guardian/payer. 10 pacientes, 14 citas, 9 cargos y 11 movimientos: sin deuda por reservar.

Se pide una solicitud para el otro hijo sin nombre, para comprobar que lo pregunte antes de identificar a Mateo. Segundo hijo, supervisión y límites todavía pendientes. Fase 7 permanece abierta; no iniciar fase 8. [Evidencia acumulada](validacion-real-agente-kapso-fase-7.md).
