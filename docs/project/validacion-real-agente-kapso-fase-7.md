# Validación real del agente por WhatsApp: fase 7

Sesión iniciada el 07/10/2026, zona America/Lima. Backend y frontend en rama `kapso`; Groq `openai/gpt-oss-20b`, flujo `supervised-v7.0`, Kapso Sandbox. El participante envía los mensajes desde su propio WhatsApp y confirma aquí el contenido recibido. No se registran credenciales ni razonamientos internos.

**Estado: validación real reanudada por el participante el 08/10/2026. La fase 7 permanece abierta.** Se respetó el aplazamiento durante su ausencia. El usuario regresó y autorizó continuar los mensajes; backend v7.3 y frontend iniciados y comprobados, con receptor/ngrok ya activos. Control AUTO/generación 4, sin tareas en cola ni respuestas salientes pendientes. Cita original CONFIRMED/versión 0 y un evento de historial; 9 pacientes, 12 citas, 9 cargos y 11 movimientos. Se solicita una propuesta nueva completa antes de probar su rechazo; no confirmar códigos anteriores. Las comprobaciones internas se conservan en [implementación y resultados](gestion-conversaciones-agente-fase-7.md); esta evidencia no sustituye casos todavía pendientes.

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

## Casos restantes

Conflicto con agenda manual; cancelación correcta e historial; dos pacientes de un teléfono responsable; atención humana y devolución; límites de acceso e instrucciones para cambiar reglas; derivaciones y horario configurable. Las comprobaciones internas de vencimiento, control durante una llamada, fallos y recuperación seguirán identificadas como internas salvo que se repitan y documenten realmente en esta sesión.

No se declara cierre ni cumplimiento completo de A25/A26 hasta resolver las comprobaciones críticas pendientes.
