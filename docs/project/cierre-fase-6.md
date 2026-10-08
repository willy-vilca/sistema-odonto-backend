# Cierre de fase 6 · reserva automática por WhatsApp

07/10/2026. Estado: completada y validada para la primera entrega en desarrollo. Alcance 1.9 y plan 3.0. Backend y frontend permanecen en ramas kapso; Twilio se conserva en su implementación anterior. No se inició fase 7.

## Resultado verificable

El participante envió mensajes desde su WhatsApp al Sandbox de Kapso. El webhook auténtico guardó las entradas, Groq interpretó la solicitud y usó herramientas administrativas de Spring Boot para consultar catálogo y disponibilidad. El paciente eligió un horario, recibió el resumen completo y confirmó «Sí, confirmo la cita». El sistema creó la cita automáticamente, conservó el historial y guardó su tarea de respuesta. Kapso confirmó lectura y el participante informó el texto que recibió.

- Paciente existente: Willy Vilca Huaytalla, PAC-000009.
- Servicio: Limpieza dental, duración 60 minutos.
- Profesional: Julia Aracelly Huaytalla Alarcon.
- Horario: martes 13/10/2026, 09:00–10:00, America/Lima.
- Cita: fc272d39-566d-4b65-a610-0302e86d50b5, CONFIRMED, origen WHATSAPP.
- Propuesta: código 5F99CF1D, confirmada después de su resumen enviado.
- Ejecución de creación: cf83eb96-cc66-479c-8292-23815daf52b9; pasos crear_cita_confirmada y guardar_respuesta.

En OdontoCare, Agenda permite consultar esa fecha y abrir su detalle/historial. WhatsApp → conversación de Willy → Seguimiento del agente → Ver bitácora permite reconstruir mensajes, herramientas, resultado, cita y estado de envío. Las entradas APP_TEST permanecen diferenciadas y no se usan como evidencia externa.

## Cobertura

| Comprobación | Resultado |
|---|---|
| Servicio, precio y duración | Catálogo real: S/ 200 y 60 minutos; respuesta Leída; sin paciente/cita nuevos. |
| Datos faltantes | «Quiero una cita» pide nombre; no reserva. |
| Fecha relativa y cierre | «Próximo lunes» consulta 12/10/2026, bloqueado por Cierre general; no sustituye la fecha ni crea cita. |
| Disponibilidad sin hora elegida | «Próximo martes» consulta 13/10/2026 y ofrece tres intervalos; no decide por el paciente. |
| Propuesta y negación | Descarta propuesta sin generar ni cancelar una cita. |
| «Sí» ambiguo | Solicita confirmación expresa; conserva propuesta; no reserva ni consume nueva inferencia para esa aclaración. |
| Confirmación expresa | Reserva real y respuesta automática Leída, sin aprobación de recepción. |
| Confirmación repetida | Misma referencia; no duplica la cita. |
| Horario ocupado | Detecta 09:00–10:00 ocupado; ofrece 10:00, 10:15 y 10:30; conserva la reserva original. |
| Límites administrativos | Niega acceso a expediente, diagnósticos y saldo; no modifica clínica ni finanzas. |
| Eventos, concurrencia y recuperación | Regresiones de aplicación: deduplicación de webhook, un solo ganador recepción/agente, respuesta y cita atómicas, agrupación, reintento de envío sin reservar otra vez, resultado incierto y recuperación limitada. |
| Interfaz | Seis escenarios de conversación/bitácora previamente aprobados en 1440, 768 y 390 píxeles, navegación de teclado y accesibilidad. Capturas controladas de interfaz, separadas de la conversación real. |

Trazas individuales, contenido, fallos y correcciones: [validación real](validacion-real-agente-kapso-fase-6.md). Línea base 9 pacientes, 11 citas, 9 cargos y 11 movimientos; final 9 pacientes, 12 citas, 9 cargos y 11 movimientos. Los catorce intercambios reales finalizaron con respuesta READ. Se creó una sola cita, sin otra ficha ni cargos nuevos.

## Correcciones y verificaciones

La demostración detectó un precio/duración inventados, un desplazamiento equivocado del lunes y la pérdida de alternativas al mencionar una cita ya confirmada. Se corrigieron y se repitieron las comprobaciones reales satisfactoriamente. El servidor comprueba cifras del catálogo, resuelve días relativos según recepción/zona y valida la fecha antes de proponer y confirmar. Las alternativas verificadas se conservan frente al control de afirmaciones de reserva. Los fallos originales permanecen en bitácora.

Se redujeron llamadas innecesarias: el resumen de propuesta se construye directamente desde datos validados y los reconocimientos ambiguos se aclaran desde la propuesta persistida. Se reforzó el aislamiento de propuestas entre pruebas de aplicación y mensajes reales. Los reintentos por cuota se observaron realmente; no se ocultan como respuestas instantáneas.

Regresión completa final: **150 casos aprobados, sin fallos ni errores**, terminada el 07/10/2026 a las 19:15 America/Lima. Incluye 19 escenarios Kapso/agente. El caso adicional crea y descarta una propuesta APP_TEST mientras conserva y confirma la propuesta real del mismo teléfono. V19 conserva el origen de cada propuesta, proyectado desde su entrada por trigger, y exige como máximo una pendiente por conversación/fuente. La consulta, descarte y referencias de horarios usan esa misma fuente. No hay cambios de interfaz que requieran repetir las capturas anteriores.

Se conservó un respaldo de la instalación actual antes de V19, incluyendo la cita comprobada, documentos clínicos y contenido financiero: backend/.runtime/backups/sistema_odontologo-20261007-191410-364.dump, SHA256 5464AD2E443072B8A48C30E1DF8E43FD422CD53C30A18B72848C207F2CF86DB9. El inventario del archivo fue validado; la restauración integral permanece en fase 9.

## Criterios y límites

**A22 cumplido:** mensaje real → agente → herramientas → resumen enviado → confirmación del paciente → cita persistida → respuesta WhatsApp Leída. A23 se demostró con datos faltantes, consultas y negación; A24 se verificó para deduplicación y recuperación de la reserva/respuesta. A27 cuenta con bitácora paginada y registros del intercambio. Las comprobaciones de concurrencia de A14 se realizan en la aplicación y no mediante carga sobre el Sandbox. El límite clínico/financiero de A26 está probado; su toma de control humana completa y los cambios por IA de A25 siguen en fase 7.

El proveedor vigente para desarrollo/pruebas es Kapso Sandbox Free y el modelo es GroqCloud/openai/gpt-oss-20b. Sus cuotas pueden causar esperas controladas; las pruebas no certifican rendimiento de producción ni mensajería fuera de la ventana de servicio. No se compraron servicios ni se cambiaron claves. El receptor limitado de 8082 y el túnel HTTPS continúan activos; los secretos siguen en archivos privados ignorados por Git. La preparación de despliegue y restauración integral del respaldo conserva su fase prevista.

La fase 6 ya permite el ciclo inicial de reserva. Reprogramar o cancelar por IA y supervisar una conversación con toma de control humana completa se implementarán cuando se autorice fase 7.
