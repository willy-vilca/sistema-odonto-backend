# Integración del agente con Kapso

07/10/2026. Desarrollo autorizado para completar fase 6, en ramas kapso de backend y frontend. GroqCloud/openai/gpt-oss-20b se conserva como modelo. El conector manual ya fue aprobado por el usuario; esta ampliación agrega respuestas autónomas, consulta de datos faltantes y reservas tras confirmación.

## Implementación

V17 agrega referencias de conversaciones y mensajes del agente con claves foráneas a cada canal. Los textos originales permanecen en las tablas de Twilio o Kapso, sin duplicarlos. Las ejecuciones anteriores conservan sus identificadores. V18 mantiene esas referencias mediante triggers para todos los escritores, incluidos los de la versión Twilio preservada.

El webhook guarda el texto auténtico antes de registrar su solicitud. Un período breve configurable agrupa mensajes consecutivos; cada entrada permanece registrada y las anteriores se identifican como agrupadas. El procesamiento se serializa por conversación y respeta el proveedor activo. Las respuestas obsoletas no se envían cuando llegó otro mensaje de su contexto.

El agente interpreta español y usa exclusivamente herramientas administrativas: catálogo, pacientes del teléfono, disponibilidad, propuesta y descarte. La identidad clínica no se deduce del nombre de perfil. Las fechas relativas utilizan recepción y zona del consultorio; las horas y profesionales se obtienen de la agenda, no del modelo. Una hora ocupada produce alternativas y una pregunta para elegir.

La propuesta contiene paciente, servicio, odontólogo, fecha/hora y duración, vence a los 30 minutos y no retiene el intervalo. Se acepta «Sí, confirmo» y otras frases expresas acotadas, o CONFIRMO con su código. Preguntas, «sí» aislado, citas textuales y negaciones no se aceptan como consentimiento. En el canal real se requiere que el resumen haya salido por WhatsApp. Las propuestas de prueba no se confirman mediante un mensaje real.

La confirmación vuelve a validar ficha, profesional, servicio, duración y disponibilidad. Cita, historial, propuesta confirmada y tarea de respuesta comparten transacción. Si falla la tarea al guardarse, se revierte la cita. El envío ocurre después y conserva su referencia; rechazar un envío no vuelve a reservar. Los conflictos con recepción conservan la cita ganadora y permiten consultar alternativas.

La cola de salida distingue En cola, Aceptado, Enviado, Entregado, Leído, Fallido y resultado incierto. Las referencias y recibos conservan los estados anticipados o repetidos. Un reintento permitido de respuesta usa el mismo mensaje y no reejecuta el modelo ni la reserva. Un resultado incierto no se reenvía automáticamente.

Groq tiene límites de llamadas, salida, tiempo y tres intentos de análisis. La cuota tiene espera automática acotada para entradas reales. Una propuesta válida ya preparada puede terminar con su resumen canónico aunque falle la llamada final al modelo. Los procesos interrumpidos se recuperan sin duplicar la operación. Los fallos terminales quedan en bitácora y producen una respuesta controlada cuando corresponde.

La aplicación muestra modelo, modo de respuesta, propuesta, herramientas, cita y estado real del envío. La bitácora y las conversaciones se buscan, filtran y paginan en PostgreSQL. El diálogo real se actualiza mientras está abierto y conserva navegación de teclado al cerrar una bitácora que actualizó sus datos. Pruebas APP_TEST son vistas previas sin envíos, con contexto separado de las entradas reales.

## Verificaciones realizadas

- Regresión completa del servidor: 138 casos aprobados, incluyendo trece nuevos escenarios de agente/Kapso y dos de confirmación. Se comprobó creación y respuesta atómicas ante un fallo inducido, concurrencia real de hilos entre recepción y agente con un solo ganador, agrupación, repetición de eventos, reintento solo del envío, recuperación, cuota acotada, resultado incierto, permisos y límites de herramientas.
- Navegador: seis escenarios de prueba y bitácora aprobados a 1440, 768 y 390 píxeles. Incluye respuesta Leída, estado Fallido, reintento a En cola, filtros, páginas, foco y accesibilidad automática. Los resultados de interfaz son controlados y no sustituyen WhatsApp real.
- Lint, TypeScript, compilación y formato aprobados. El receptor conserva las validaciones anteriores de bytes originales y encabezados.
- Modelo real, vista previa de precio: respondió PEN 200.00 y 60 minutos para Limpieza dental usando el catálogo. No creó cita ni salida WhatsApp. Ejecución 9fd8baf6-09cd-48c0-a265-444538bd4f49, 2318 tokens de entrada y 92 de salida.
- Respaldo previo de la instalación actual conservado en backend/.runtime/backups. Las pruebas que limpian datos se ejecutaron solo en sistema_odontologo_test. Migraciones 17 y 18 se aplicaron a la instalación ficticia actual preservando sus datos.

## Demostración externa y estado

El backend habitual y el agente están activos, con el receptor y túnel del usuario. **A22 y fase 6 completados el 07/10/2026** después de la conversación real del participante, con propuesta recibida, confirmación expresa, cita WHATSAPP/CONFIRMED del martes 13/10/2026 09:00–10:00 con Julia para limpieza y respuesta READ. Los catorce intercambios reales incluyen consultas, datos faltantes, negación, fecha relativa, día cerrado, disponibilidad, confirmación ambigua, repetición, hora ocupada y límites. Se corrigieron y repitieron los fallos de catálogo, cálculo del lunes y preservación de alternativas. [Evidencia individual](validacion-real-agente-kapso-fase-6.md) y [cierre de fase](cierre-fase-6.md).

[Guía de conversación y comprobaciones](probar-agente-kapso-fase-6.md). La reprogramación, cancelación por IA y toma de control humano completa se conservan para fase 7; no se agregan dentro de este desarrollo.

Documentación oficial usada: [herramientas locales Groq](https://console.groq.com/docs/tool-use/local-tool-calling), [texto Kapso](https://docs.kapso.ai/docs/whatsapp/send-messages/text), [firma y duplicados](https://docs.kapso.ai/docs/platform/webhooks/security).
