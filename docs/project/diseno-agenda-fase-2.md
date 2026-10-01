# Decisiones técnicas de pacientes y agenda — fase 2

Este documento complementa el alcance 1.2 y el plan, sin ampliarlos.

## Identidad y contactos
Una ficha tiene un código irrepetible, documento opcional y de uno a ocho contactos administrativos. Los teléfonos internacionales se guardan con prefijo + y pueden aparecer en varias fichas. Cada contacto indica nombre, relación, responsabilidad del menor y responsabilidad de pago. No se usa el teléfono como identificador del paciente.

La aplicación impide repetir un documento o la combinación de nombre, nacimiento y contacto; también revisa contactos secundarios. Una ficha provisional puede carecer de fecha de nacimiento. Un menor identificado por su nacimiento requiere responsable. Desactivar una ficha conserva sus citas. Las ediciones de contactos también actualizan la versión de la ficha para rechazar formularios desactualizados.

## Disponibilidad e integridad
Las operaciones viven en servicios separados de los controladores. BookingRules valida profesional y servicio; AvailabilityService calcula jornadas, descansos, excepciones, anticipación y separación. AppointmentService conserva estas reglas al crear y reprogramar. El calendario consulta hasta 42 días y 1.000 citas; una consulta que exceda ese límite debe reducir su intervalo o seleccionar profesional. La lista, las búsquedas, los selectores, los horarios sugeridos y el historial se paginan en el servidor; el historial permite además filtrar por tipo de movimiento.

Las escrituras de agenda adquieren un bloqueo compartido de la configuración y un bloqueo del odontólogo. Así, varios profesionales pueden reservar simultáneamente y las reglas no cambian a mitad de una transacción. La exclusión GiST de PostgreSQL impide que un odontólogo tenga intervalos bloqueados solapados incluso ante escritura directa. Los intervalos son semiabiertos: una cita puede comenzar al terminar la anterior si la separación es cero.

Cada cita guarda duración, nombre del servicio, nombre del profesional y separación usados. Cambiar el catálogo no cambia los horarios anteriores. Reprogramar conserva por defecto la duración; el usuario puede solicitar expresamente la duración actual y se vuelve a validar. Una operación rechazada revierte por completo, incluida su auditoría y su historial.

## Tiempo, estados e historial
El servidor interpreta el inicio local en la zona del consultorio y almacena instantes UTC. Se rechazan minutos ambiguos o inexistentes por cambios de hora. Las respuestas incluyen inicio y fin locales para mostrar la agenda sin depender de la zona del navegador.

Flujo habitual: reservada → confirmada → en espera → en atención → atendida. Reservada, confirmada o en espera pueden cancelarse o marcarse como no asistió, con motivo; los estados de asistencia requieren que haya llegado la hora. Atendida, cancelada y no asistió son terminales. Reprogramar una reserva o confirmación genera una nueva reserva pendiente de confirmar.

La cancelación libera el intervalo y conserva el historial. Los otros estados conservan el intervalo histórico. El historial es inmutable en PostgreSQL y cada movimiento registra el usuario, instante, motivo, estado e intervalo. La clave UUID de solicitud evita duplicar una reserva al reintentar; reutilizarla con otro contenido se rechaza.

## Agente y WhatsApp
No se implementa reserva mediante IA en esta fase. El agente utilizará los mismos servicios centrales, incorporando su identidad, origen, confirmación y restricciones de servicios habilitados en la fase 6. La recepción manual puede usar un servicio activo asignado aunque esté deshabilitado para reserva automática.

No se han proporcionado cuenta, número Sandbox, token de Twilio, participante autorizado ni dirección pública para recibir webhooks. La prueba real de envío/recepción queda pendiente de esos accesos. No se considera demostrado A22 ni se sustituye esta dependencia por mensajes simulados.
