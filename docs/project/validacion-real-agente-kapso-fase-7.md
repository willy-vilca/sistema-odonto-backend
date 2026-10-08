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

## Prueba 3: preparar cambio y después rechazarlo — propuesta real aprobada; negación pendiente

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

## Casos restantes

Negación de la propuesta; confirmación expresa de una nueva propuesta e idempotencia; conflicto con agenda manual; cancelación correcta e historial; dos pacientes de un teléfono responsable; atención humana y devolución; límites de acceso e instrucciones para cambiar reglas; derivaciones y horario configurable. Las comprobaciones internas de vencimiento, control durante una llamada, fallos y recuperación seguirán identificadas como internas salvo que se repitan y documenten realmente en esta sesión.

No se declara cierre ni cumplimiento completo de A25/A26 hasta resolver las comprobaciones críticas pendientes.
