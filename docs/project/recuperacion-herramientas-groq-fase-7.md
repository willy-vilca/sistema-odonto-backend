# Orden de herramientas y consumo de Groq: fase 7

08/10/2026. Rama `kapso`, flujo final `supervised-v7.6`, modelo conservado `openai/gpt-oss-20b`. Alcance 1.9; fase 7 en validación real.

## Fallo real

Al solicitar una propuesta nueva, la ejecución `a4bba789-ae9c-4815-810b-af7fbcf448c8`, secuencia 49, falló después de verificar al paciente y consultar el catálogo. Los intentos 1 y 2 recibieron HTTP 400/tool_use_failed, clasificados TOOL_GENERATION, con esperas de dos segundos. El intento 3 recibió HTTP 429/rate_limit_exceeded, TOKENS_PER_MINUTE, con retry-after de siete segundos; ya había alcanzado el máximo de intentos y derivó a recepción.

Respuesta `617cf08e-eb1c-4be6-971c-1cce8b1f1709`, READ. Duración hasta persistir la respuesta: 13,487 segundos; uso informado de inferencias aceptadas: 3 540 tokens de entrada y 104 de salida. No se deduce un agotamiento diario ni se atribuyen los dos HTTP 400 a la cuota. El proveedor no informó aquí un uso detallado de esas llamadas rechazadas.

La conversación pasó a HANDOFF/generación 5. La cita original permaneció CONFIRMED/versión 0, 13/10/2026 09:00–10:00; el cambio anterior seguía SUPERSEDED y la reserva de fase 6 CONFIRMED. No se creó otra propuesta real ni se modificó el historial o dinero.

## Diagnóstico estructural

Se hicieron inferencias de reproducción sin ejecutar herramientas, sin guardar cuerpos de error ni razonamientos: solo se inspeccionaron nombres de funciones de una lista permitida y tipos de campos. Una reproducción devolvió tool_use_failed mencionando consultar_horarios; otra devolvió una llamada a horarios todavía sin haber consultado la cita propia. No se reconstruye retrospectivamente cada argumento del fallo original.

La elección de una función concreta mediante tool_choice y una instrucción adicional tampoco evitaron que el modelo intentara horarios en esa reproducción parcial. Esos ensayos no se presentan como éxito. La solución final conserva tool_choice=auto y ejecuta la lectura administrativa obligatoria en el flujo, en lugar de depender del modelo para seleccionar ese paso.

## Solución final

- Tras verificar al paciente, una solicitud de reprogramación/cancelación consulta sus próximas citas mediante la misma herramienta autorizada. Usa búsqueda vacía, página 0 y límite de cinco, con identidad, relación, control y reglas del servidor. La IA continúa usando las referencias devueltas; no se elige automáticamente entre varias citas.
- La lectura queda registrada en bitácora y en el contexto del modelo, con un identificador de llamada generado por el flujo. Se persiste el punto de continuación antes de la siguiente inferencia, por lo que una cuota no repite esa lectura completada. No registra ni modifica una cita.
- Solo después de obtener citas propias se ofrece disponibilidad para un cambio. Una solicitud de cambio no ofrece la herramienta de crear otra reserva. Las negaciones y consultas informativas no activan el paso obligatorio ni ofrecen herramientas de propuesta.
- Los campos opcionales del esquema admiten null igual que su omisión, como ya permitían las validaciones del servidor. Los identificadores requeridos siguen siendo cadenas obligatorias; se mantiene additionalProperties=false y la validación del proveedor y del backend. Esto alinea formatos, sin afirmar que un null específico causara el fallo original.
- El presupuesto inicial de salida se reduce de 1 500 a 768 tokens, con esfuerzo low. Sigue siendo configurable; el máximo no es el consumo real. La configuración local conserva modelo, clave y demás valores. No se contrataron servicios ni se cambió de proveedor.

La [documentación de herramientas](https://console.groq.com/docs/tool-use/local-tool-calling) contempla la ejecución local y el control del ciclo en la aplicación. La [referencia API](https://console.groq.com/docs/api-reference) y [errores](https://console.groq.com/docs/errors) se consultaron para diferenciar selección de funciones, rechazos y cuota. La aplicación conserva los controles de consentimiento y permisos independientemente del resultado del modelo.

## Verificación

79 casos del agente aprobados, cero fallos/errores/omitidos y empaquetado correcto: 47 Kapso/agente, 10 del agente base, cinco de etapas/esquemas, 12 del cliente Groq y cinco de fechas/horas. Registro final local ignorado: `backend/.runtime/phase7-required-read-tests.log`. La regresión completa anterior de 187 casos permanece separada.

Groq real: APP_TEST `f5322cb5-d3d7-44ff-8ec8-d426b7881fd2`, COMPLETED, un intento, **3,032 segundos**, tres inferencias, 7 445 tokens de entrada y 234 de salida. Verificación del paciente → lectura propia obligatoria → disponibilidad → propuesta correcta del 13/10 al 20/10 a las 09:00 con Julia, 60 minutos y motivo. No hubo errores ni reintentos en ese ensayo, envío de WhatsApp o modificación de cita/historial/finanzas.

Se conserva la vista previa intermedia `377e6ea1-7f54-4727-bf90-4dc7fb86a68a`, correcta en 10,625 segundos con cuatro inferencias y una espera por cuota; la solución final elimina una inferencia dedicada a seleccionar la lectura. Son mediciones de sus respectivos contextos, no una promesa general de latencia o ahorro.

Se devolvió a AUTO únicamente la derivación automática conocida de la secuencia 49, sin responsable humano y generación 5, mediante servicio y auditoría; quedó generación 6. Requiere un mensaje nuevo y no reanuda entradas antiguas. 9 pacientes, 12 citas, 9 cargos, 11 movimientos; cita original versión 0/un historial. Receptor, frontend, ngrok y archivos privados se conservan.

## Continuación

Repetir la solicitud completa por WhatsApp para comprobar la propuesta del canal real. Copiar el resumen sin confirmar; después probar sí ambiguo, confirmación expresa y repetición. Los códigos APP_TEST no se usan por WhatsApp. Consulta propia, disponibilidad, propuesta anterior, negación y rechazo de código descartado ya tienen evidencia real; la nueva propuesta y los demás casos siguen pendientes. No se cierra fase 7 con esta vista previa.
