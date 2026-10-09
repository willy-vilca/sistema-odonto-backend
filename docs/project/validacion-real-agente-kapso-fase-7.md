# Validación real del agente por WhatsApp: fase 7

Sesión iniciada el 07/10/2026, zona America/Lima. Backend y frontend en rama `kapso`; Groq `openai/gpt-oss-20b`, flujo `supervised-v7.0`, Kapso Sandbox. El participante envía los mensajes desde su propio WhatsApp y confirma aquí el contenido recibido. No se registran credenciales ni razonamientos internos.

**Estado vigente al 08/10/2026: fase 7 en validación real, todavía abierta.** Consulta propia, disponibilidad, negación, código descartado, reprogramación, repetición, conflicto entre propuesta y confirmación, cancelación expresa y cancelación repetida aprobados. Backend v7.9; control AUTO/generación 10. La cita de Willy está CANCELLED/versión 2, martes 20/10/2026 09:00–10:00, conservando tres eventos de historial. La ocupación manual temporal del ensayo de conflicto también está CANCELLED y conserva su historial. Lucía y Mateo Prueba Familia tienen fichas provisionales distintas y citas WHATSAPP/CONFIRMED el 20/10 de 09:00–10:00 y 10:00–11:00, con el mismo contacto responsable; 11 pacientes, 15 citas, 9 cargos y 11 movimientos. Contacto compartido aprobado; se continúa con supervisión y límites. Las comprobaciones internas se conservan en [implementación y resultados](gestion-conversaciones-agente-fase-7.md); esta evidencia no sustituye casos todavía pendientes. Los apartados anteriores conservan los estados observados en cada momento.

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

Se corrigieron consumo, prioridad de respuesta, continuación persistida y espera según el proveedor. La misma solicitud con Groq real en APP_TEST terminó en 2,331 segundos y tres llamadas; no envió WhatsApp ni modificó citas. Se devolvió exclusivamente esta derivación automática a AUTO/generación 2 para recibir una instrucción nueva. [Diagnóstico, cambios y resultados](optimizacion-agente-groq-fase-7.md). En ese punto quedó pendiente repetir por WhatsApp, sin elegir ni confirmar un horario.

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

## Prueba 3: preparar cambio y después rechazarlo — propuesta y negación reales aprobadas

Se solicita enviar:

> Elijo el martes 20/10/2026 a las 09:00 con Julia Huaytalla. Reprograma mi cita por el cambio de horario de trabajo.

Debe preparar el resumen con el paciente, fechas anterior y nueva, profesional, tratamiento, duración y motivo, conservando todavía la cita inicial. Se solicita copiar la propuesta sin confirmarla; después se probará su negación. No se declara aprobada mientras falta la respuesta y la verificación.

El participante envió esa elección y recibió nuevamente la lista de opciones, sin el resumen. Ejecución `9ae3e3a9-7026-47e5-ad54-d3f8512310b6`, secuencia 37, KAPSO/COMPLETED, dos intentos y respuesta READ. Aunque la ejecución terminó, **el comportamiento solicitado no pasó la prueba**. El modelo intentó proponer con un slot_id inválido, rechazado por el servidor, y después omitió preferred_time al consultar. El cierre rápido trató esa omisión como falta de elección, ignorando las 09:00 del mensaje.

Hubo además un límite temporal en el primer intento, registrado como fallo_controlado/RATE_LIMIT con retry-after de 14 segundos. La continuación persistida evitó repetir verificar_paciente y consultar_mis_citas; el registro completo tomó unos 22,488 segundos. Esta recuperación no convierte en correcta la respuesta funcional equivocada. La cita original permaneció CONFIRMED/versión 0 y con un solo evento de historial.

Flujo `supervised-v7.2`: hora explícita y unívoca aplicada en el servidor aun si el modelo la omite; disponibilidad con hora preferida aplicada; continuar hacia la propuesta cuando la hora elegida está libre; rechazar propuestas con una hora distinta; incluir el motivo en el resumen. Se conserva confirmación obligatoria e identidad/control. APP_TEST también continúa ante cuotas temporales con el mismo límite de intentos y espera, sin envíos.

Verificación final: 53 casos del agente, fechas y horas aprobados, cero fallos/omitidos y empaquetado correcto. Groq real en APP_TEST generó la propuesta PENDING correcta en `7490594d-3632-4d25-ab08-00aec7ca4ada`, 5,560 segundos, con cuatro llamadas completadas y una espera de unos dos segundos; 8 768 tokens de entrada y 306 de salida. Resumen: Willy, Limpieza dental, Julia, fecha actual 13/10/2026 09:00, destino 20/10/2026 09:00, 60 minutos, motivo Cambio de horario de trabajo. No se envió WhatsApp ni cambió la cita, su historial o finanzas. Se conserva también la vista previa anterior `5898b1a5-f7f7-42b8-bbd0-c7d3c91a45fe`, fallida por cuota sin cambios, que motivó extender la continuación a APP_TEST.

La repetición real de la elección sigue pendiente. El código de la propuesta APP_TEST no sirve para confirmar por WhatsApp; el participante debe recibir un resumen y código del canal real. [Detalle de la corrección](optimizacion-agente-groq-fase-7.md).

### Segunda repetición real: límite temporal y rechazo del proveedor

El participante repitió la elección y recibió «No pude completar la consulta. La dejo pendiente para recepción. Revisa la agenda antes de repetir una operación». Ejecución `61d42047-8785-4d07-a540-380bb0e29c43`, secuencia 40, KAPSO/FAILED, dos intentos, respuesta `baa6d825-11ac-48aa-a6e0-ee4865ee6c88` READ. Primer fallo RATE_LIMIT con espera de dos segundos; segundo PROVIDER_ERROR genérico. El estado HTTP y la categoría del segundo fallo no se conservaron entonces: no se atribuye sin evidencia a otra cuota ni a una herramienta. Aproximadamente 8,826 segundos hasta persistir la respuesta, 4 829 tokens de entrada y 112 de salida conocidos. Control HANDOFF/generación 3. Cita original CONFIRMED/versión 0, con un evento de historial y sin cambios financieros.

El usuario pidió corregir y guardar el estado, aplazando nuevos intercambios hasta regresar. Se reprodujo internamente HTTP 429/TOKENS_PER_MINUTE, se completó el diagnóstico seguro y la recuperación, y se presentaron herramientas por requisitos. Flujo final `supervised-v7.3`. La vista previa `3edcf13c-f11b-46ff-8f65-2b185dc6722e` preparó la propuesta correcta en 15,747 segundos, incluidos 12 de espera por cuota, sin rechazo de herramientas, envío ni cambio de cita. No se convierte esa prueba en demostración real de la propuesta o reprogramación. [Diagnóstico, cambios y punto para retomar](diagnostico-groq-reserva-fase-7.md).

Solo se devolvió a AUTO la derivación automática conocida de esta prueba, sin responsable humano; generación 4 y requiere instrucción nueva. Se mantienen 9 pacientes, 12 citas, 9 cargos y 11 movimientos. La siguiente comprobación real debe preparar otra propuesta correcta antes de probar su negación; no confirmar códigos APP_TEST ni antiguos.

### Reanudación del 08/10: filtro incorrecto de la consulta propia

Entrada nueva y completa: «Soy Willy Vilca Huaytalla. La cita es para mí. Elijo el martes 20/10/2026 a las 09:00 con Julia Huaytalla. Reprograma mi limpieza dental por el cambio de horario de trabajo». El participante recibió «No encontré próximas citas activas del paciente verificado». Ejecución `8a5acf3c-4018-4cc0-9129-f8d52bd799c4`, secuencia 44, KAPSO/COMPLETED, un intento, `supervised-v7.3`, 5 694 tokens de entrada y 153 de salida. La ejecución terminó, pero **la prueba funcional falló**.

La identidad se verificó correctamente como SELF y paciente `7e5ff960-c0b8-4fb2-941c-5a93af361d95`. El contexto administrativo conservaba al paciente y su solicitud. El modelo llamó consultar_mis_citas con search «Willy Vilca Huaytalla»; ese filtro buscaba servicios/profesionales y devolvió cero resultados. La cita seguía activa. No fue una pérdida de la cita o del contexto ni un error de cuota en esta ejecución.

Corrección `supervised-v7.4`: el nombre completo del paciente ya verificado se trata como identidad redundante, no como filtro de servicio. Solo se elimina ese filtro cuando coincide exactamente después de normalizar; otro texto continúa filtrando. La consulta permanece limitada al UUID del paciente verificado, en el servidor y con páginas de cinco. Devuelve search_applied y distingue una búsqueda vacía de una búsqueda sin coincidencias para no afirmar que no existen citas al fallar un filtro. Se aclararon herramienta e instrucciones del modelo.

72 regresiones del agente/cliente/fechas/herramientas aprobadas, cero fallos/errores/omitidos y empaquetado correcto. Incluyen 44 Kapso/agente, reproducción del filtro de nombre durante el flujo de propuesta, conservación de filtros legítimos y protección de identidad. Se conserva aparte la regresión completa anterior de 187 casos, sin presentarla como repetida aquí.

Groq real preparó la propuesta correcta en APP_TEST `1d657b85-1289-433b-8a9f-708200fb689c`, COMPLETED, dos intentos, cuatro inferencias aceptadas, 10,708 segundos incluidos unos siete de espera por cuota. 8 935 tokens de entrada y 303 de salida. Verificó al paciente, consultó sin filtro, recuperó la cita del 13/10, consultó el 20/10 a las 09:00 y preparó el resumen con motivo. No se confirmó, no se envió WhatsApp ni se modificó cita, historial o finanzas. Los códigos APP_TEST siguen sin servir en el canal real.

Backend actualizado para repetir la misma entrada por WhatsApp. Cita original CONFIRMED/versión 0, 13/10/2026 09:00–10:00, un evento de historial; control AUTO/generación 4. La propuesta real continúa pendiente de verificación, antes de probar su rechazo. No se declara fase 7 cerrada.

### Propuesta real correcta recibida — aprobada; todavía sin cambio de cita

El participante repitió la entrada completa y recibió el resumen correcto en aproximadamente diez segundos según su observación: Willy, Limpieza dental, Julia, fecha actual martes 13/10/2026 09:00, nuevo horario martes 20/10/2026 09:00, duración 60 minutos y motivo cambio de horario de trabajo. Código de propuesta real 233B4896; aún sin confirmación.

Ejecución `30b537e0-6e37-4aec-a1f6-a076b24873be`, secuencia 46, KAPSO/COMPLETED, `supervised-v7.4`, dos intentos, 8 899 tokens de entrada y 291 de salida. Entrada `29c5eeae-df00-40cd-b2e8-ba6da862b780`; respuesta `465d655c-7e37-4711-ab72-e72a195629e7`, READ y sin error. Herramientas correctas: verificar_paciente → consultar_mis_citas sin filtro → consultar_horarios con referencia propia y fecha/hora → proponer_reprogramacion → guardar_respuesta. Las consultas y propuesta no se duplicaron.

El primer intento registró HTTP 429/TOKENS_PER_MINUTE con retry-after de ocho segundos. Retomó la misma ejecución y sus herramientas completadas; la recuperación quedó demostrada también en este intercambio real.

Propuesta `233b4896-72b0-4ccf-bc98-0afa9119f5c9`, fuente KAPSO, RESCHEDULE/PENDING, vinculada a la cita original y su versión 0. Creada el 08/10/2026 a las 12:41:16 (America/Lima), vence a las 13:11:16. Desde registrar la ejecución hasta persistir la respuesta transcurrieron 13,865 segundos; esa medición del servidor se conserva separada de los aproximadamente diez segundos informados por el participante.

Cita original todavía CONFIRMED/versión 0, martes 13/10 09:00–10:00, con un evento de historial; sin cambios financieros. Permanecen 9 pacientes, 12 citas, 9 cargos, 11 movimientos y AUTO/generación 4. **La preparación y entrega real de la propuesta quedan aprobadas; no se declara aplicada una reprogramación.** El siguiente caso es rechazar esta propuesta y comprobar que el código no permite aplicar un cambio descartado.

## Prueba 4: rechazar el cambio — aprobada

El participante envió «No confirmo el cambio. No reprogrames mi cita; conserva la del martes 13/10/2026 a las 09:00» y recibió: «Entendido. Tu cita sigue programada para el martes 13/10/2026 a las 09:00 con Julia Huaytalla. Si necesitas algo más, avísame».

Ejecución `4194823b-7b16-4ffb-92b5-d2022aaffb48`, secuencia 47, KAPSO/COMPLETED, `supervised-v7.4`, un intento y sin errores. Entrada `02f1967e-57b8-405f-b60d-9c0dea1c0eaa`, respuesta `e6b11ad7-fc5e-45be-b396-a4b01ba7d309` READ. Herramienta descartar_propuesta OK; no creación ni cambio de cita. 3 946 tokens de entrada, 83 de salida; 4,053 segundos desde registrar la ejecución hasta persistir la respuesta.

La propuesta 233B4896 pasó a SUPERSEDED antes de vencer y conserva confirmation_message_id vacío. La cita original sigue CONFIRMED/versión 0, 13/10/2026 09:00–10:00, un evento de historial. Permanecen 9 pacientes, 12 citas, 9 cargos, 11 movimientos y AUTO/generación 4. La negación queda comprobada realmente, sin confundirse con una cancelación.

Al preparar el siguiente caso, revisión del servidor detectó que un rechazo de código descartado podía entrar en la recuperación genérica de conflictos y modificar el estado de una propuesta anterior de reserva confirmada. **No se había ejecutado ese intento en la instalación actual ni alterado su reserva**. Se corrige antes de pedir el mensaje: un cambio descartado devuelve su rechazo sin aplicarlo ni usar IA; los mecanismos de conflicto exigen una propuesta PENDING y la referencia correcta, y revalidan el control humano. Se conserva la reserva de fase 6 y se prepara la comprobación real del código 233B4896.

Corrección `supervised-v7.5` empaquetada. 75 casos distintos del agente verificados: el bloque inicial pasó 74 y falló únicamente una aserción del nuevo ensayo que contaba también inferencias de preparación de su reserva; al limitar el contador a la confirmación rechazada, ese caso pasó en una repetición aislada. No se presentan los dos bloques como una sola ejecución sin fallos. Los tres ensayos nuevos verifican que el código descartado no invalida una reserva ya confirmada, no marca otra propuesta pendiente como conflictiva y no usa el modelo para responder. Logs locales ignorados: `phase7-discarded-change-tests.log` y `phase7-discarded-change-final.log`.

La prueba real del código descartado aún está pendiente; no se ha enviado ese mensaje por cuenta del participante. La cita y su propuesta de reserva original de fase 6 permanecen CONFIRMED, y la propuesta de cambio real continúa SUPERSEDED.

## Prueba 5: confirmar un código descartado — aprobada

El participante envió «CONFIRMO 233B4896» y recibió: «Esa propuesta de cambio fue descartada. La cita original se conserva; solicita una nueva propuesta si deseas cambiarla».

Ejecución `a1ebed5a-d851-4304-807f-9b6bb7c81842`, secuencia 48, KAPSO/COMPLETED, `supervised-v7.5`, un intento, cero tokens de entrada/salida y ninguna llamada al modelo. Entrada `13c40e00-d92e-4075-a2d2-66fa334a44ec`, respuesta `5e28b4e8-f523-40b5-a6d7-fddfcd029947` READ, sin errores. 2,920 segundos desde registrar la ejecución hasta persistir la respuesta.

La propuesta de cambio sigue SUPERSEDED y la reserva original de fase 6 sigue CONFIRMED, vinculada a `fc272d39-566d-4b65-a610-0302e86d50b5`. La cita mantiene CONFIRMED/versión 0, martes 13/10/2026 09:00–10:00 y un evento de historial. 9 pacientes, 12 citas, 9 cargos, 11 movimientos; AUTO/generación 4. No se convirtió el código rechazado en un conflicto de otra reserva ni se reprogramó/canceló nada. El rechazo queda demostrado en WhatsApp real.

Se continúa con una propuesta nueva del martes 20/10 a las 09:00 con Julia, sin confirmarla todavía. Se comprobarán confirmación ambigua, confirmación expresa e idempotencia antes de los demás flujos.

## Prueba 6: preparar una propuesta nueva — aprobada tras corrección

El participante pidió de nuevo la reprogramación del 20/10 a las 09:00 con Julia y recibió la derivación a recepción. Secuencia 49, ejecución `a4bba789-ae9c-4815-810b-af7fbcf448c8`, FAILED/RATE_LIMIT, tres intentos, respuesta READ. Primero ocurrieron dos HTTP 400/tool_use_failed y después HTTP 429/TOKENS_PER_MINUTE. No se preparó otro cambio real; cita original, reserva, cambio descartado, historial y finanzas intactos.

Se corrigió el flujo v7.6: lectura propia obligatoria tras identidad verificada, ejecutada por el agente dentro del backend y persistida antes de otra inferencia; formatos opcionales alineados; presupuesto inicial 768. Se conservan la intervención del modelo para interpretar datos, consultar horarios y proponer, y la confirmación expresa para aplicar cambios. 79 casos aprobados. Groq real en APP_TEST `f5322cb5-d3d7-44ff-8ec8-d426b7881fd2` preparó el resumen correcto en 3,032 segundos, tres inferencias y un intento, sin envíos o cambios de cita. [Diagnóstico, intentos intermedios y límites](recuperacion-herramientas-groq-fase-7.md).

Se restableció únicamente la derivación automática identificada a AUTO/generación 6, sin responsable humano y sin ejecutar mensajes anteriores. La solicitud se debe repetir por WhatsApp y revisar antes de confirmar. No se declara aprobada la nueva propuesta real por esta vista previa.

### Repetición real v7.6 aprobada

El participante envió la solicitud completa y recibió el resumen correcto: Willy, Limpieza dental, Julia, del martes 13/10/2026 09:00 al martes 20/10/2026 09:00, 60 minutos y motivo cambio de horario de trabajo. Código nuevo F4160577, todavía sin confirmación.

Ejecución `69ab6ba1-9069-4541-8a40-3f275ef1008c`, secuencia 52, KAPSO/COMPLETED, `supervised-v7.6`, un intento, tres inferencias y sin errores/reintentos. 6 711 tokens de entrada y 283 de salida. Entrada `77f41dc8-6d87-4eab-a750-f7b1b0bf2d74`; respuesta `f68c3ec3-c2a7-4884-8c60-22570ac33c19`, READ y sin error. Desde registrar la ejecución hasta persistir la respuesta: 6,410 segundos; no equivale al tiempo de lectura en el teléfono.

La bitácora demuestra verificar_paciente mediante modelo → consultar_mis_citas como lectura obligatoria del flujo → consultar_horarios mediante modelo → proponer_reprogramacion mediante modelo → respuesta. Cada herramienta se ejecutó una sola vez. El cálculo respetó la referencia de la cita propia y la duración conservada.

Propuesta `f4160577-eed4-44e4-8f2c-331664012feb`, KAPSO/RESCHEDULE/PENDING, vinculada a la cita `fc272d39-566d-4b65-a610-0302e86d50b5`, versión 0; creada el 08/10 a las 18:26:12 Lima y vence a las 18:56:12. Cita original aún CONFIRMED/versión 0, 13/10 09:00–10:00, un evento de historial. Permanecen 9 pacientes, 12 citas, 9 cargos, 11 movimientos y AUTO/generación 6.

La nueva propuesta y su entrega real quedan aprobadas; **el cambio no se ha aplicado**. Se continúa con «sí» ambiguo, sin código ni otras palabras, antes de la confirmación expresa.

## Prueba 7: sí ambiguo — aprobada

El participante envió únicamente «sí» y recibió de nuevo el mismo resumen del 13/10 al 20/10 a las 09:00 con Julia y el código F4160577, solicitando confirmación expresa.

Ejecución `cc89563d-7984-4c94-a6cf-458272eb3285`, secuencia 53, KAPSO/COMPLETED, `supervised-v7.6`, un intento, cero inferencias y tokens. Entrada `d7306a4c-64d3-45d0-b169-b7a5b78bdc3a`; respuesta `9974620b-76ea-487a-9ae2-68a6e223b8b7` READ y sin error. No hubo herramientas de cambio; únicamente se guardó la respuesta. 1,921 segundos desde registrar la ejecución hasta persistirla.

La propuesta conserva PENDING, el mismo código y su vencimiento original de las 18:56:12; no se renovó su vigencia al repetir el resumen. confirmation_message_id sigue vacío. La cita del 13/10 09:00–10:00 continúa CONFIRMED/versión 0 y un historial; pacientes, citas y finanzas conservados (9/12/9/11), AUTO/generación 6. Se continúa con confirmación expresa del cambio real.

## Prueba 8: confirmación expresa y reprogramación real — aprobada

El participante envió «Sí, confirmo el cambio de mi cita» y recibió la confirmación de reprogramación con referencia `fc272d39-566d-4b65-a610-0302e86d50b5`.

Ejecución `19e93532-a219-4e26-a00a-02f7137b5dd8`, secuencia 54, KAPSO/COMPLETED, `supervised-v7.6`, un intento, cero inferencias/tokens. Entrada `e556e905-b303-4563-9d60-18231a7157cc`; respuesta `3e6e1587-6ede-4bf5-900a-e00721018164` READ, sin errores. Herramienta reprogramar_cita_confirmada OK; confirmación y tarea de respuesta guardadas de forma consistente. 4,653 segundos desde registrar la ejecución hasta persistir la respuesta.

La misma cita pasó a CONFIRMED/versión 1, martes 20/10/2026 09:00–10:00, conservando paciente, servicio, profesional y 60 minutos. La propuesta F4160577 quedó CONFIRMED y vinculada al mensaje de aceptación. El historial conserva CREATED del 13/10 y añade exactamente un RESCHEDULED del 13/10 al 20/10, actor Agente IA y motivo. Dos eventos totales; siguen 9 pacientes, 12 citas, 9 cargos y 11 movimientos, AUTO/generación 6. La reprogramación efectiva queda demostrada, sin crear otra cita ni alterar finanzas.

### Ajuste de presentación solicitado después de confirmar

El mensaje que realmente recibió el participante reutilizaba el resumen previo: «Fecha actual: 13/10» y «Nuevo horario: 20/10». El usuario pidió eliminar la fecha anterior de la confirmación y hacerla más legible. Se conserva esa respuesta original en la evidencia; no se sobrescribe como si hubiera tenido el formato nuevo.

Se separa el formato de confirmación del resumen de propuesta: datos de la cita resultante en líneas de paciente, servicio, profesional, fecha, horario, duración, estado y referencia, con hora del consultorio y sin código de zona técnico. Al repetir una confirmación se consultan los datos actuales, con identidad/contacto verificados, en lugar de reutilizar fechas antiguas de la propuesta. Propuesta e historial conservan las fechas necesarias para reconstruir el cambio. La siguiente prueba de repetición mostrará el formato actualizado.

Flujo `supervised-v7.7`, 83 pruebas aprobadas (48 Kapso/agente, tres de formato y las regresiones previas), sin fallos/errores/omitidas; empaquetado correcto. Incluye reprogramación posterior manual seguida de repetición: muestra el horario actual, conserva versión/historial y no vuelve a aplicar el cambio. Los mensajes históricos no se editan; el ajuste se aplica a futuras confirmaciones/repeticiones y también mantiene legible la confirmación de cancelación. No requiere inferencia ni cambios de frontend. La prueba real de repetición sigue pendiente.

## Prueba 9: confirmación repetida y formato nuevo — aprobada

El participante repitió «Sí, confirmo el cambio de mi cita». Recibió «La reprogramación ya estaba registrada» y los datos actuales en líneas: Willy, Limpieza dental, Julia, martes 20/10/2026, 09:00–10:00, 60 minutos, Confirmada y la referencia original. No apareció la fecha del 13/10 en el mensaje.

Ejecución `42862837-db35-4763-b896-39f345536e95`, secuencia 55, KAPSO/COMPLETED, `supervised-v7.7`, un intento, cero inferencias/tokens. Entrada `3b22f4db-6e6d-4a45-9b4c-7a7675fb646a`; respuesta `5181d0b0-9237-4eeb-a6be-cb793457b5fb` READ. Resultado repeated=true y misma cita; 3,006 segundos desde registro hasta respuesta persistida.

La cita sigue CONFIRMED/versión 1, 20/10 09:00–10:00. Conserva dos eventos de historial y exactamente un RESCHEDULED. La propuesta F4160577 mantiene CONFIRMED y el mensaje original de aceptación, sin reemplazarlo por la repetición. Pacientes/citas/cargos/movimientos 9/12/9/11 y AUTO/generación 6. Idempotencia y nuevo formato quedan demostrados realmente.

Siguiente bloque: proponer otro horario libre del 20/10, sin confirmar; ocuparlo después mediante una cita manual temporal para otro paciente ficticio, y confirmar para comprobar conflicto con conservación de la cita del 20/10 09:00. No se crea la ocupación antes de recibir la propuesta. Tras verificar, cancelar únicamente la cita temporal con motivo de prueba, conservando su historial. No hay pruebas de carga sobre Kapso.

## Prueba 10: conflicto entre propuesta y confirmación — aprobada

El participante pidió el 20/10/2026 a las 11:00 con Julia, recibió la propuesta A4BC2579 y todavía no la confirmó. Ejecución `4ee9ab6f-9ad2-411b-8dc0-a942f9482855`, secuencia 56, COMPLETED/READ, un intento y sin error; 7 460 tokens de entrada y 283 de salida. Entrada `5942b9cc-5772-403e-91a0-629b99d10d84`; respuesta `1a2ca61a-ac5e-4852-a514-7081d853acba`. Respuesta persistida a los 7,203 segundos del registro de ejecución.

Propuesta `a4bc2579-a454-47c4-aa3b-8b4d3af2f355`, RESCHEDULE/PENDING, sobre la misma cita y versión 1, de 20/10 09:00 a 20/10 11:00, 60 minutos. Creada el 08/10 a las 18:52:26 Lima, vence a las 19:22:26.

Después de recibir la propuesta se creó una reserva manual temporal mediante AppointmentService.create, las mismas reglas centrales del módulo manual. Actor auditado Sistema (ensayo local autorizado), no se simula un clic ni una cuenta de recepción. Paciente existente Carlos Ruiz Vega `38c845d1-9ea5-4ad0-8d74-de77b74aba0b`, Julia, Limpieza dental, 20/10 11:00–12:00, origen MANUAL/RESERVED. Cita temporal `ec4a37c6-b4a3-47a2-b5c3-698dc9733612`, clave idempotente `937de26f-ffc9-3fcd-997b-e243df1929d2`, creada el 08/10 después de las 18:54; nota de prueba vinculada a A4BC2579. No se enviaron mensajes ni se creó otro paciente.

El ensayo comprobó igualdad de la fila completa de Willy antes/después, historial y finanzas, y número de mensajes salientes. Cita de Willy permanece CONFIRMED/versión 1, 09:00–10:00 y dos historiales. Ahora hay 9 pacientes, 13 citas, 9 cargos y 11 movimientos; la cita adicional es la ocupación temporal. La propuesta sigue vigente/PENDING. Falta la confirmación real para demostrar el rechazo del conflicto; esta preparación por sí sola no lo declara aprobado.

Tras verificar el conflicto se cancelará exclusivamente la temporal, conservando su historial, con motivo «Fin de prueba de concurrencia fase 7». Helper y registro ignorados: `.runtime/Phase7ManualConflict.java`, `.runtime/phase7-manual-conflict-created.log`. No hay pruebas de carga sobre Kapso.

### Confirmación real rechazada y limpieza de la ocupación temporal

El participante envió «CONFIRMO A4BC2579» antes de vencer la propuesta y recibió: «No se aplicó el cambio; la cita original se conserva. El intervalo está ocupado o no respeta la separación entre citas. Podemos consultar otro horario o pedir ayuda a recepción». No se divulgaron datos del paciente que ocupaba el horario.

Ejecución `6276d0d5-f15a-4048-923b-9261e4f170d4`, secuencia 57, KAPSO/FAILED con BOOKING_VALIDATION, un intento y flujo supervised-v7.7. Es el rechazo esperado de una regla de agenda, no un fallo del modelo. Cero inferencias/tokens. Entrada `ce63672b-34fd-4a1f-84bf-99a03863435e`; respuesta `53f828e5-9c67-474c-862a-d5d755a0be9e`, READ y sin error de envío. Respuesta persistida a los 3,650 segundos del registro de ejecución. Propuesta A4BC2579 pasó a CONFLICT, sin aplicar el cambio.

La cita original conservó íntegra su fecha 20/10 09:00–10:00, CONFIRMED/versión 1 y dos historiales, con exactamente un RESCHEDULED. Las propuestas anteriores de reserva y reprogramación permanecen CONFIRMED. Control AUTO/generación 6; 9 pacientes, 13 citas, 9 cargos y 11 movimientos. El agente no canceló primero la reserva original ni inventó una reprogramación exitosa.

Después de comprobar el resultado se canceló exclusivamente la cita manual temporal `ec4a37c6-b4a3-47a2-b5c3-698dc9733612`, mediante AppointmentService.changeStatus y guardas de paciente, profesional, servicio, nota y clave de solicitud. CANCELLED/versión 1; conserva CREATED y CANCELLED, actor Sistema, motivo «Fin de prueba de concurrencia fase 7», a las 19:02:19 Lima. El ensayo verificó igualdad de la fila completa de Willy, su historial, pacientes, finanzas y mensajes salientes antes/después de la limpieza. Registro local ignorado `.runtime/phase7-manual-conflict-cancelled.log`. La fila temporal no se borra: permanecen 13 citas, una de ellas cancelada por este ensayo.

La integridad ante ocupación manual posterior queda demostrada con confirmación real de WhatsApp. Se solicita preparar la cancelación de la cita vigente de Willy, identificando paciente/relación, fecha, hora, servicio, profesional y motivo; todavía no se aplica ni se considera aprobada esa cancelación.

## Prueba 11: propuesta y cancelación expresa — aprobadas

El participante envió «Soy Willy Vilca Huaytalla. La cita es para mí. Quiero cancelar mi limpieza dental con Julia Huaytalla del martes 20/10/2026 a las 09:00 porque estaré de viaje. Confirma primero qué cita vas a cancelar». Recibió el resumen correcto de cancelación de Willy/Limpieza/Julia/20/10 09:00, motivo de viaje, código CBE82DEB y solicitud de confirmación.

Ejecución `19b9d1eb-7242-4cbd-ac13-d28a2ece2ba4`, secuencia 58, KAPSO/COMPLETED, un intento, supervised-v7.7, dos inferencias; 4 495 tokens de entrada y 122 de salida. Entrada `498c0af4-82bd-4839-8039-e79681a51245`; respuesta `77fd7245-eb84-46ce-a004-0b860b04ca55`, READ y sin error. Verificar_paciente SELF → consultar_mis_citas limitada al paciente → proponer_cancelacion → guardar_respuesta. 5,739 segundos desde registro de ejecución hasta respuesta persistida.

Propuesta `cbe82deb-0707-4446-8c07-6959eaa45d2b`, CANCEL/PENDING, cita original `fc272d39-566d-4b65-a610-0302e86d50b5`, versión 1, motivo «estoy de viaje»; creada 19:03:29, vence 19:33:29 Lima. No se canceló ni se cambió la cita: permanece CONFIRMED/versión 1, 09:00–10:00 y dos historiales. AUTO/generación 6 y pacientes/citas/cargos/movimientos 9/13/9/11. El sí ambiguo ya quedó comprobado en reprogramación, sin repetir innecesariamente ese intercambio. Próximo mensaje solicitado: «Sí, confirmo la cancelación»; la aplicación efectiva y su repetición aún no se consideran aprobadas.

### Cancelación real confirmada

El participante envió «Sí, confirmo la cancelación» y recibió «Tu cita quedó cancelada», seguido de los datos de Willy/Limpieza/Julia/20/10/2026 09:00–10:00, 60 minutos, estado Cancelada y la misma referencia. Formato en líneas, sin fechas antiguas ni datos de otra persona.

Ejecución `97e4b90f-257c-4ad3-93be-13bcd6d26617`, secuencia 59, KAPSO/COMPLETED, supervised-v7.7, un intento, cero inferencias/tokens y sin error. Entrada `cc50acdf-e7d0-43bb-a7bb-65ea6f678781`; respuesta `8a5556b2-0a17-4e87-b72a-c3330111d193`, READ. Cancelar_cita_confirmada OK y guardar_respuesta QUEUED; respuesta persistida a los 4,957 segundos del registro de ejecución. Propuesta CBE82DEB CONFIRMED, vinculada al mensaje expreso de aceptación; CONFIRMED aquí es el estado de la propuesta aplicada, distinto de CANCELLED en la cita.

La misma cita `fc272d39-566d-4b65-a610-0302e86d50b5` pasó a CANCELLED/versión 2 y conserva paciente, servicio, profesional, fecha, intervalo y duración. Historial: CREATED → RESCHEDULED → CANCELLED; exactamente una cancelación, actor Agente IA, motivo «estoy de viaje», a las 19:05:13 Lima. Se mantienen la reserva y reprogramación anteriores CONFIRMED y el cambio ocupado CONFLICT. Pacientes/citas/cargos/movimientos 9/13/9/11 y AUTO/generación 6. Se solicita repetir la misma confirmación para comprobar que no añada otra cancelación ni versión; esa repetición aún está pendiente.

## Prueba 12: cancelación repetida — aprobada

El participante repitió «Sí, confirmo la cancelación» y recibió «La cancelación ya estaba registrada. Estos son los datos actuales de tu cita», con Willy, Limpieza, Julia, 20/10 09:00–10:00, estado Cancelada y la misma referencia. No se creó una reserva nueva ni se presentó una operación pendiente como aplicada.

Ejecución `1f8fe3b0-dbbf-49cd-bb90-95d600cea810`, secuencia 60, KAPSO/COMPLETED, supervised-v7.7, un intento y cero inferencias/tokens. Entrada `40cf0b89-f0ae-4227-a3aa-db48e415bdee`; respuesta `3f1c0314-5b96-4316-baa1-f1cf3a21fa67`, READ, sin error. Resultado repeated=true para la misma cita; respuesta persistida a los 3,769 segundos del registro de ejecución.

La cita sigue CANCELLED/versión 2, tres historiales y un único CANCELLED. CBE82DEB conserva CONFIRMED y el mensaje original `cc50acdf-e7d0-43bb-a7bb-65ea6f678781` de aceptación, sin sustituirlo por la repetición. AUTO/generación 6; pacientes/citas/cargos/movimientos 9/13/9/11. Cancelación e idempotencia quedan comprobadas con mensajes reales.

Se verificó antes del siguiente bloque que no existen fichas con «Prueba Familia». Se pide una limpieza para la hija ficticia Lucía Prueba Familia con Julia el 20/10 por la mañana, identificando al padre y responsable Willy y solicitando primero horarios. Debe verificar GUARDIAN, diferenciar a la hija del contacto y no crear su ficha/cita hasta la confirmación. La prueba familiar aún no está aprobada.

## Prueba 13: horarios para una hija del mismo contacto — aprobada; reserva pendiente

El participante pidió una limpieza para su hija ficticia Lucía Prueba Familia con Julia el 20/10/2026 por la mañana, identificándose como padre y responsable y solicitando primero horarios. Recibió tres opciones reales: 09:00–10:00, 09:15–10:15 y 09:30–10:30, con pregunta de elección y aclaración de que aún no se creó ni cambió una cita.

Ejecución `84a22bbc-9311-4083-81d7-e6b3d5cb1f36`, secuencia 61, KAPSO/COMPLETED, supervised-v7.7, dos intentos, 6 181 tokens de entrada y 187 de salida. Entrada `f54fd1eb-42e7-45be-b094-1a7b35ef83cb`; respuesta `383179cb-0724-454a-8b74-3a1f09e7e64e`, READ y sin error de envío. Respuesta persistida a los 8,416 segundos del registro de ejecución.

Verificar_paciente identificó Lucía Prueba Familia, GUARDIAN, provisional=true y patient_id vacío; no reutilizó la ficha de Willy. Un HTTP 400/tool_use_failed intermedio se registró como TOOL_GENERATION y se recuperó en el segundo intento, conservando la verificación ya realizada. Consultar_servicios devolvió Limpieza dental, S/ 200 y 60 minutos; consultar_horarios calculó las tres opciones con Julia. No se ejecutó creación de ficha/cita ni se ocultó el rechazo intermedio.

Contexto KAPSO: patient_name Lucía Prueba Familia, relationship GUARDIAN y patient_id/appointment_id vacíos. No existen todavía fichas Prueba Familia; siguen 9 pacientes, 13 citas, 9 cargos y 11 movimientos, AUTO/generación 6. La cita anterior de Willy sigue CANCELLED/versión 2/tres historiales. Se solicita elegir 20/10 a las 09:00 para Lucía, sin confirmar todavía; debe preparar el resumen de la hija y volver a comprobar disponibilidad al confirmar. El bloque completo de dos hijos aún está pendiente.

## Prueba 14: elegir horario para Lucía — aprobada tras corrección

El participante eligió 20/10/2026 09:00 para la limpieza de su hija Lucía Prueba Familia con Julia, confirmando ser padre y responsable. Recibió «Voy a derivar tu consulta a recepción para que una persona pueda ayudarte. El asistente queda pausado».

Secuencia 62, ejecución `ab448654-6ff4-4d25-bfc2-3b8720effbb2`, KAPSO/COMPLETED y respuesta READ, un intento y dos inferencias aceptadas. Verificar_paciente fue REJECTED por permiso; después derivar_recepcion OK. No hubo error de cuota. Los argumentos rechazados están redactados y no permiten afirmar el nombre concreto seleccionado por el modelo. La prueba funcional falló; no se preparó propuesta, ficha ni cita real de Lucía. El contexto conservaba Lucía/GUARDIAN y el control pasó a HANDOFF/generación 7. Agenda, historial y finanzas intactos.

Corrección supervised-v7.8: continuidad explícita del mismo hijo en el mismo canal, revalidada mediante la herramienta central; selección nueva o ambigua no hereda el hijo anterior. Identidad y resultado persistidos antes de inferencia. Instrucciones separan nombre del hijo y responsable; respuestas que solicitan IDs internos se bloquean y se recuperan con herramientas. 86 regresiones aprobadas y empaquetado correcto. Una reproducción APP_TEST detectó primero la pregunta improcedente por slot_id; la vista previa final Groq real `a0c5e549-f79a-46c3-bb2c-6e226b4d6ad6` preparó el resumen correcto en 2,589 segundos, tres inferencias y sin errores/cambios/envíos. No se presenta esa preparación administrativa de prueba como conversación íntegra de WhatsApp.

Backend actualizado, AUTO/generación 8 tras devolver solo la derivación automática conocida con comprobaciones y auditoría. No tareas antiguas pendientes; requiere mensaje nuevo. Se solicita repetir la elección para Lucía, sin confirmar todavía. La repetición y la reserva real continúan pendientes. [Diagnóstico, regresiones y límites](identidad-responsables-agente-fase-7.md).

### Repetición real y propuesta correcta

El participante repitió la elección y recibió el resumen correcto de Lucía Prueba Familia/Limpieza dental/Julia/martes 20/10/2026 09:00/60 minutos, con código 414B310E y solicitud de confirmación. No pidió identificadores internos ni seleccionó al responsable como paciente.

Ejecución `a9be0c9e-78d0-4480-85a5-92e991da92ce`, secuencia 67, KAPSO/COMPLETED, supervised-v7.8, dos intentos y cuatro inferencias aceptadas; 9 043 tokens de entrada y 246 de salida. Entrada `8caa6346-4e74-4a98-b2de-3b681828d249`; respuesta `cbfffb8e-4f8b-4865-bdfe-2310579fda8a`, READ. Esta repetición sí tuvo un HTTP 429/TOKENS_PER_MINUTE con espera de cinco segundos antes de proponer; recuperó la ejecución conservando identidad, servicio y horario, sin volver a ejecutar las herramientas completadas. No confundir esa incidencia recuperada con la causa del fallo previo de la secuencia 62. Respuesta persistida a los 13,135 segundos desde el registro de ejecución.

Verificó Lucía/GUARDIAN como provisional; consultó servicio vigente, horario exacto 09:00 y preparó propuesta `414b310e-851a-42ab-87df-f96a36617880`, KAPSO/PENDING, sin patient_id ni appointment_id. Creada 19:25:11, vence 19:55:11 Lima. Cita todavía no creada; pacientes/citas/cargos/movimientos 9/13/9/11, AUTO/generación 8. Se solicita «CONFIRMO 414B310E» para verificar creación coherente de ficha provisional y cita de la hija, con contacto responsable y sin cargos. Esa aceptación y el segundo hijo todavía están pendientes.

## Prueba 15: confirmar la reserva de Lucía — aprobada

El participante envió «CONFIRMO 414B310E» y recibió confirmación de Lucía Prueba Familia/Limpieza/Julia/martes 20/10/2026 09:00/60 minutos, referencia `5d766bea-1d54-4c6f-bf92-c31651e41d0e`.

Ejecución `bbf32802-dcfe-4e47-8bc8-921ffe4240cd`, secuencia 68, KAPSO/COMPLETED, supervised-v7.8, un intento, cero inferencias/tokens. Entrada `e72f57f2-ea2e-42ed-9b9c-1edf4e926b08`; respuesta `ac4b2dec-f9d9-4839-94a5-faa1d039eb33`, READ y sin error. Crear_cita_confirmada OK; respuesta persistida a los 5,509 segundos del registro de ejecución.

Se creó exactamente una ficha Lucía Prueba Familia, `bf95319a-999e-486c-9823-8b2d0d126d22`, provisional=true. Contacto `02111c34-ef3d-4b3a-be3b-51726d5f7c08`, teléfono del participante, nombre administrativo Willy, relación Tutor WhatsApp, guardian=true y payer=true. El paciente de la atención es Lucía, no el padre. El nombre completo del responsable y demás datos de la ficha provisional pueden completarse desde Pacientes; el perfil de WhatsApp no sustituye la verificación paciente/relación.

Una cita WHATSAPP/CONFIRMED, versión 0, 20/10 09:00–10:00, 60 minutos y un historial CREATED; propuesta 414B310E CONFIRMED y vinculada a esa cita. Pacientes/citas/cargos/movimientos 10/14/9/11: aumentaron solo una ficha y una reserva, sin cargos ni dinero. AUTO/generación 8; Willy permanece cancelado con su historia conservada. No se repite otra confirmación de creación: esa idempotencia ya se probó en fase 6 y cambios/cancelación en fase 7.

Se solicita ahora reservar para «mi otro hijo», omitiendo deliberadamente el nombre, para comprobar que lo pregunte y no reutilice a Lucía. Después se identificará al segundo hijo ficticio Mateo Prueba Familia y se confirmará otro horario. El bloque completo de contacto compartido todavía no se considera terminado.

## Prueba 16: otro hijo sin nombre — aprobada

El participante pidió otra limpieza para «mi otro hijo», con Julia el 20/10 por la mañana y relación de padre/responsable, sin informar el nombre. Recibió «Para poder reservar la cita necesito saber el nombre completo de tu otro hijo. Por favor indícalo».

Ejecución `ecf735b8-d341-46f6-8c29-28c48bb62d41`, secuencia 69, KAPSO/COMPLETED, supervised-v7.8, un intento y dos inferencias aceptadas; 3 917 tokens de entrada, 92 de salida. Entrada `c242f0a0-5622-4c92-8304-f2b7e144754c`; respuesta `2e6a9e70-e46a-416d-8d75-5c219aadd865`, READ. Se conserva el intento intermedio verificar_paciente REJECTED; el agente terminó preguntando el dato faltante, sin reservar ni derivar. Respuesta persistida a los 3,993 segundos del registro de ejecución.

Lucía conserva CONFIRMED/versión 0 y un historial. Permanecen 10 pacientes, 14 citas, 9 cargos y 11 movimientos, AUTO/generación 8. La solicitud nueva no reutilizó a Lucía para crear otra cita. Se solicita nombre explícito Mateo Prueba Familia, relación de padre/responsable y consulta de horarios del 20/10 con Julia; todavía sin elegir ni confirmar. La segunda reserva sigue pendiente.

## Prueba 17: identificar a Mateo y consultar horarios — aprobada; reserva pendiente

El participante informó «Mi otro hijo se llama Mateo Prueba Familia. Soy su padre y responsable», pidió limpieza con Julia el 20/10 por la mañana y consultó horarios libres. Recibió 10:00–11:00, 10:15–11:15 y 10:30–11:30; se aclaró que todavía no se creó ni cambió una cita.

Ejecución `14c8d29a-858e-4164-95d4-4ee58050c711`, secuencia 70, KAPSO/COMPLETED, supervised-v7.8, un intento, tres inferencias, sin pasos rechazados; 5 977 tokens de entrada y 158 de salida. Entrada `39595079-9208-4648-a474-ea16187ccf8b`; respuesta `e5fefeb1-3956-408b-9b2d-46bc0078c039`, READ. Verificar_paciente → consultar_servicios → consultar_horarios → guardar_respuesta. Respuesta persistida a los 5,069 segundos desde registrar la ejecución.

La identidad se verificó de nuevo para Mateo Prueba Familia/GUARDIAN, provisional=true, patient_id vacío; no heredó a Lucía. Los horarios consultados respetan la cita existente de la hija, 09:00–10:00, y ofrecen opciones desde las 10:00. Lucía conserva CONFIRMED/versión 0 y un historial; todavía no existe ficha Mateo Prueba Familia. 10 pacientes, 14 citas, 9 cargos y 11 movimientos, AUTO/generación 8.

Se solicita elegir 20/10 10:00 para Mateo, con nombre y relación explícitos, para preparar el resumen sin confirmar todavía. El bloque completo de dos reservas desde el mismo responsable sigue pendiente de esa propuesta y su aceptación.

## Prueba 18: elegir horario de Mateo — aprobada tras corrección

El participante eligió el 20/10/2026 a las 10:00 con Julia para su hijo Mateo Prueba Familia y reiteró ser padre/responsable. Recibió la derivación a recepción y pausa del agente. La prueba funcional falló aunque la ejecución se guardara COMPLETED.

Ejecución `bf684c1a-b3ae-436c-9e3e-2ccb65f709b3`, secuencia 71, KAPSO/COMPLETED, supervised-v7.8, un intento. Entrada `a9e299d1-f90a-4da9-9529-cf0ac0096b03`; respuesta `4ee3a30d-9dac-4b7f-8e48-48ce2347a6d5`, READ. Verificar_paciente fue OK para Mateo/GUARDIAN antes de la inferencia: la continuidad de identidad sí funcionó. Una sola inferencia, 1 932 tokens de entrada y 183 de salida; el modelo llamó derivar_recepcion con motivo «No se puede obtener el slot_id para la reserva solicitada». No consultó catálogo ni horarios, ni hubo RATE_LIMIT/error de herramientas. Respuesta persistida a los 4,275 segundos desde registrar la ejecución. HANDOFF/generación 9.

Corrección supervised-v7.9: para paciente verificado y hora elegida, no aceptar una derivación por IDs internos faltantes antes de consultar disponibilidad, cuando no existe un fallo de lectura/propuesta registrado. Se devuelve al modelo la instrucción de obtener referencias con catálogo y agenda, dentro de los límites y punto de continuación existentes. Derivaciones por clínica, solicitud humana, identidad dudosa, disponibilidad ya consultada o falla real de herramientas se conservan; control revalidado antes de continuar.

88 regresiones del agente aprobadas, incluidas 53 Kapso/agente, empaquetado correcto. El caso nuevo falló antes de corregir y comprueba que se prepara la propuesta en vez de derivar por slot_id; otro caso permite la derivación después de un error real de consulta. Groq real en APP_TEST `eef297af-abf0-4580-8b62-1faa9360daee` preparó Mateo/Limpieza/Julia/20/10 10:00/60 minutos en 2,551 segundos, tres inferencias, un intento, sin pasos rechazados y sin cambios de negocio/control real/envíos. Esa vista previa no confirma una reserva real de Mateo.

Backend actualizado y salud UP, frontend/receptor/ngrok conservados. Se devolvió únicamente la derivación conocida de esta ejecución mediante servicio auditado, comprobando referencia, generación, motivo, pasos y ausencia de responsable humano o entrada real posterior. AUTO/generación 10, sin tareas antiguas pendientes; exige mensaje nuevo. Se solicita repetir la elección completa por WhatsApp, sin confirmar todavía. Lucía conserva su cita; no se creó ficha ni cita de Mateo. Pacientes/citas/cargos/movimientos 10/14/9/11. [Diagnóstico y límites](identidad-responsables-agente-fase-7.md).

### Repetición real con propuesta correcta

El participante repitió la elección y recibió el resumen correcto de Mateo Prueba Familia/Limpieza/Julia/martes 20/10/2026 10:00/60 minutos, código 78612830 y solicitud de confirmación. No volvió a derivar por la referencia de horario ni pidió identificadores internos.

Ejecución `8a293983-2b1d-4560-b473-4eba8d6a6a33`, secuencia 74, KAPSO/COMPLETED, supervised-v7.9, dos intentos y cuatro inferencias aceptadas; 8 708 tokens de entrada, 232 de salida. Entrada `a321405c-9bb3-4025-9fe8-b9992a5c99a7`; respuesta `7555385c-cf01-4c66-8d96-3872674d3641`, READ. Verificar_paciente Mateo/GUARDIAN → consultar_servicios → consultar_horarios con 10:00 → proponer_cita → guardar_respuesta. La devolución del control exigió una nueva verificación. Hubo HTTP 429/TOKENS_PER_MINUTE antes de proponer, con dos segundos de espera; retomó resultados anteriores sin duplicar herramientas. Esta cuota recuperada no es la causa del fallo funcional anterior de la secuencia 71. Respuesta persistida a los 10,022 segundos desde el registro de ejecución.

Propuesta `78612830-959e-418b-bfe5-ff67146ffc0d`, KAPSO/PENDING, Mateo, patient_id/appointment_id vacíos. Creada 19:57:11, vence 20:27:11 Lima. Intervalo 20/10 10:00–11:00, 60 minutos, profesional y servicio correctos. Lucía conserva 09:00–10:00 CONFIRMED/versión 0/un historial; no existe todavía ficha de Mateo. Cifras 10/14/9/11, AUTO/generación 10. Se solicita «CONFIRMO 78612830» para aplicar la segunda reserva y comprobar ambas fichas/contactos/citas; todavía no se considera aplicado el bloque completo de dos hijos.

## Prueba 19: confirmar Mateo y completar dos reservas de un responsable — aprobada

El participante envió «CONFIRMO 78612830» y recibió la confirmación de Mateo Prueba Familia/Limpieza/Julia/martes 20/10/2026 10:00/60 minutos, referencia `7b103374-8707-4b3c-bc01-dd5130361227`.

Ejecución `9a9a76d1-1032-44cb-be05-098caebc889a`, secuencia 75, KAPSO/COMPLETED, supervised-v7.9, un intento, cero inferencias/tokens. Entrada `557c6139-c3fa-42bd-89ae-8d4cfd41d6d8`; respuesta `2e6e607f-b541-41c2-a17e-45db060fe7d8`, READ. Crear_cita_confirmada OK y guardar_respuesta QUEUED. Respuesta persistida a los 4,494 segundos del registro de ejecución. Propuesta 78612830 CONFIRMED y vinculada al mensaje de aceptación y la nueva cita.

Mateo: una única ficha `cde35927-e18c-4564-96ed-ccf8601243bb`, provisional=true; cita `7b103374-8707-4b3c-bc01-dd5130361227`, WHATSAPP/CONFIRMED, versión 0, 20/10 10:00–11:00, 60 minutos y un historial CREATED. Lucía conserva su ficha `bf95319a-999e-486c-9823-8b2d0d126d22` y cita `5d766bea-1d54-4c6f-bf92-c31651e41d0e`, 09:00–10:00, CONFIRMED/versión 0 y un historial. Sus propuestas anteriores permanecen CONFIRMED.

Ambas fichas tienen el mismo teléfono del participante como contacto, nombre administrativo Willy, relación Tutor WhatsApp, guardian=true y payer=true. Son dos UUID de paciente y dos citas diferentes, sin solapamiento; no se convirtió al padre en paciente de las atenciones. Se comprobó una sola ficha por cada nombre. Pacientes/citas/cargos/movimientos 11/15/9/11: la aceptación de Mateo añadió solo una ficha y una cita. No se generaron cargos ni movimientos financieros por reservar. AUTO/generación 10.

El bloque de dos pacientes de un contacto responsable queda aprobado con mensajes reales, incluidos datos faltantes, cambio explícito de hijo, disponibilidad y confirmación. No se repite idempotencia de creación ya comprobada en fase 6. Se pide ahora al participante asumir la conversación desde la aplicación, motivo «Prueba de atención manual fase 7», y confirmar que vea Atención humana antes de enviar el siguiente mensaje real. Supervisión aún no aprobada externamente.

## Casos restantes

Atención humana y devolución; límites de acceso e instrucciones para cambiar reglas; derivaciones y horario configurable. Las comprobaciones internas de vencimiento, control durante una llamada, fallos y recuperación seguirán identificadas como internas salvo que se repitan y documenten realmente en esta sesión.

No se declara cierre ni cumplimiento completo de A25/A26 hasta resolver las comprobaciones críticas pendientes.
