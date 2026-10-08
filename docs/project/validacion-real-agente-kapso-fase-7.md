# Validación real del agente por WhatsApp: fase 7

Sesión iniciada el 07/10/2026, zona America/Lima. Backend y frontend en rama `kapso`; Groq `openai/gpt-oss-20b`, flujo `supervised-v7.0`, Kapso Sandbox. El participante envía los mensajes desde su propio WhatsApp y confirma aquí el contenido recibido. No se registran credenciales ni razonamientos internos.

**Estado: en ejecución. La fase 7 permanece abierta.** Las comprobaciones internas se conservan en [implementación y resultados](gestion-conversaciones-agente-fase-7.md); esta evidencia registra los intercambios reales y no sustituye casos todavía pendientes.

## Estado inicial

- Conversación: `7dd48687-7e15-4ae9-a7d8-fefdc5cc6c2c`; control AUTO, generación 0.
- Cita: `fc272d39-566d-4b65-a610-0302e86d50b5`; Willy Vilca Huaytalla, Limpieza dental con Julia Aracelly Huaytalla Alarcon, martes 13/10/2026 09:00–10:00, CONFIRMED, versión 0, un evento de historial.
- Instalación: 9 pacientes, 12 citas, 9 cargos, 11 movimientos financieros. Datos ficticios autorizados para esta demostración.
- Atención automática habilitada, sin restricción de jornadas; cambios y cancelación habilitados, anticipación mínima cero.

La instantánea local de las 22:53:51 contiene ya la entrada de la primera prueba, secuencia 33. Por eso la primera ejecución se verifica por su identificador explícito, y las siguientes se buscan desde la secuencia 34; no se omite el primer intercambio.

## Prueba 1: consultar la cita propia — aprobada

**Entrada del participante:**

> Soy Willy Vilca Huaytalla. Quiero consultar mis próximas citas. La consulta es para mí; por ahora no quiero cambiar ni reservar nada.

**Respuesta recibida y confirmada por el participante:**

> Próximas citas del paciente verificado:
> • Willy Vilca Huaytalla: Limpieza dental con Julia Aracelly Huaytalla Alarcon, martes 13/10/2026 09:00. Duración: 60 minutos.
> Si deseas un cambio, indica cuál cita y el motivo.

**Evidencia del servidor:** entrada `5719d847-d761-44b0-a33e-39dcc14b4abe`; ejecución `60e0440d-a22d-4118-ad83-42e1476fb2ed`, secuencia 33, COMPLETED/RESPONDED. Las herramientas verificaron SELF y el paciente asociado al teléfono, y consultaron únicamente su cita. Las consultas devuelven referencias temporales ligadas a conversación/canal/paciente; no se ejecutó una herramienta de creación o cambio.

Respuesta `e3072656-3104-4557-a713-36e71e9ee396`, estado READ, sin error. Entrada a las 22:53:30 y respuesta persistida a las 22:55:17: aproximadamente 107 segundos, con tres intentos. Se conserva como observación de latencia; el registro final no identifica la causa de los reintentos y no se atribuye sin evidencia a Groq o Kapso. Consumo registrado: 18 049 tokens de entrada y 633 de salida, incluyendo las llamadas repetidas.

Tras la consulta, la cita conserva CONFIRMED, versión 0, 13/10/2026 09:00–10:00 y un evento de historial; el control sigue AUTO/generación 0. El participante confirmó la misma respuesta que consta en el servidor.

## Prueba 2: consultar horarios de reprogramación — incidencia y repetición pendiente

Se solicita al participante enviar:

> Soy Willy Vilca Huaytalla. Quiero reprogramar mi próxima limpieza dental con Julia Huaytalla al martes 20/10/2026. ¿Qué horarios tiene ese día? El motivo es un cambio de horario de trabajo.

El participante envió el mensaje y recibió después de más de un minuto: «No pude completar la consulta. La dejo pendiente para recepción. Revisa la agenda antes de repetir una operación».

Ejecución `a72d477c-10d0-4072-90af-66b9b1c351b6`, secuencia 34, FAILED/RATE_LIMIT, tres intentos. La agenda calculó opciones del 20/10/2026 en el primer intento, pero se agotó la cuota en llamadas posteriores al modelo. Los reintentos anteriores repetían el análisis y perdían las referencias; control HANDOFF/generación 1. La cita se conservó CONFIRMED/versión 0 y un evento de historial.

Se corrigieron consumo, prioridad de respuesta, continuación persistida y espera según el proveedor. La misma solicitud con Groq real en APP_TEST terminó en 2,331 segundos y tres llamadas; no envió WhatsApp ni modificó citas. Se devolvió exclusivamente esta derivación automática a AUTO/generación 2 para recibir una instrucción nueva. [Diagnóstico, cambios y resultados](optimizacion-agente-groq-fase-7.md). La repetición externa sigue pendiente: todavía no se elige ni confirma un horario.

La sesión agrupa primero el flujo de cambios y después los límites de acceso; cubre los mismos casos de [la guía preparada](probar-cambios-agente-kapso-fase-7.md), sin exigir su orden literal.

## Casos restantes

Propuesta de cambio y negación; confirmación expresa e idempotencia; conflicto con agenda manual; cancelación correcta e historial; dos pacientes de un teléfono responsable; atención humana y devolución; límites de acceso e instrucciones para cambiar reglas; derivaciones y horario configurable. Las comprobaciones internas de vencimiento, control durante una llamada, fallos y recuperación seguirán identificadas como internas salvo que se repitan y documenten realmente en esta sesión.

No se declara cierre ni cumplimiento completo de A25/A26 hasta resolver las comprobaciones críticas pendientes.
