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

## Prueba 2: consultar horarios de reprogramación — aprobada tras corrección

Se solicita al participante enviar:

> Soy Willy Vilca Huaytalla. Quiero reprogramar mi próxima limpieza dental con Julia Huaytalla al martes 20/10/2026. ¿Qué horarios tiene ese día? El motivo es un cambio de horario de trabajo.

El participante envió el mensaje y recibió después de más de un minuto: «No pude completar la consulta. La dejo pendiente para recepción. Revisa la agenda antes de repetir una operación».

Ejecución `a72d477c-10d0-4072-90af-66b9b1c351b6`, secuencia 34, FAILED/RATE_LIMIT, tres intentos. La agenda calculó opciones del 20/10/2026 en el primer intento, pero se agotó la cuota en llamadas posteriores al modelo. Los reintentos anteriores repetían el análisis y perdían las referencias; control HANDOFF/generación 1. La cita se conservó CONFIRMED/versión 0 y un evento de historial.

Se corrigieron consumo, prioridad de respuesta, continuación persistida y espera según el proveedor. La misma solicitud con Groq real en APP_TEST terminó en 2,331 segundos y tres llamadas; no envió WhatsApp ni modificó citas. Se devolvió exclusivamente esta derivación automática a AUTO/generación 2 para recibir una instrucción nueva. [Diagnóstico, cambios y resultados](optimizacion-agente-groq-fase-7.md). La repetición externa sigue pendiente: todavía no se elige ni confirma un horario.

**Repetición auténtica recibida y confirmada por el participante:**

> Estos son los horarios disponibles:
> • 2026-10-20 09:00–10:00 · Julia Aracelly Huaytalla Alarcon · Limpieza dental
> • 2026-10-20 09:15–10:15 · Julia Aracelly Huaytalla Alarcon · Limpieza dental
> • 2026-10-20 09:30–10:30 · Julia Aracelly Huaytalla Alarcon · Limpieza dental
>
> ¿Cuál de estos horarios prefieres? Todavía no se creó ni cambió ninguna cita.

Ejecución `fe017ec1-e329-41b7-ba8f-2d1341728fdb`, secuencia 36, fuente KAPSO, COMPLETED, un intento, flujo `supervised-v7.1`, sin error. Entrada `1bae9ba6-11c3-489e-9af4-6310da432777`; respuesta `1ce51456-6301-4b11-98e3-1f8223944829`, READ. Tres llamadas del modelo: verificar_paciente → consultar_mis_citas → consultar_horarios con la referencia verificada y los identificadores del servicio/profesional. Consumo: 6 922 tokens de entrada y 230 de salida.

Desde el registro de la ejecución (23:28:44,225) hasta persistir la respuesta (23:28:51,814): 7,589 segundos, incluyendo la espera del trabajador. Esta cifra no es el tiempo de lectura en el teléfono. El participante calificó la llegada como «casi instantánea» y confirmó el mismo contenido. La medición de 2,331 segundos anterior corresponde exclusivamente a APP_TEST y se conserva separada.

La cita original sigue CONFIRMED, versión 0, 13/10/2026 09:00–10:00 y un evento de historial; permanecen 9 pacientes, 12 citas, 9 cargos y 11 movimientos. No hubo propuesta ni reprogramación automática al consultar opciones. La incidencia queda resuelta y comprobada también en el canal real; siguen pendientes los cambios y demás casos.

La sesión agrupa primero el flujo de cambios y después los límites de acceso; cubre los mismos casos de [la guía preparada](probar-cambios-agente-kapso-fase-7.md), sin exigir su orden literal.

## Prueba 3: preparar cambio y después rechazarlo — en ejecución

Se solicita enviar:

> Elijo el martes 20/10/2026 a las 09:00 con Julia Huaytalla. Reprograma mi cita por el cambio de horario de trabajo.

Debe preparar el resumen con el paciente, fechas anterior y nueva, profesional, tratamiento, duración y motivo, conservando todavía la cita inicial. Se solicita copiar la propuesta sin confirmarla; después se probará su negación. No se declara aprobada mientras falta la respuesta y la verificación.

## Casos restantes

Propuesta de cambio y negación; confirmación expresa e idempotencia; conflicto con agenda manual; cancelación correcta e historial; dos pacientes de un teléfono responsable; atención humana y devolución; límites de acceso e instrucciones para cambiar reglas; derivaciones y horario configurable. Las comprobaciones internas de vencimiento, control durante una llamada, fallos y recuperación seguirán identificadas como internas salvo que se repitan y documenten realmente en esta sesión.

No se declara cierre ni cumplimiento completo de A25/A26 hasta resolver las comprobaciones críticas pendientes.
