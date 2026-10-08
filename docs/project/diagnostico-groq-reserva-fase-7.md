# Diagnóstico de Groq y punto de continuación de fase 7

Continuación de la sesión iniciada el 07/10/2026. Rama `kapso`, Groq `openai/gpt-oss-20b`, flujo final `supervised-v7.3`. El usuario solicitó continuar las correcciones sin pedir ni ejecutar nuevos intercambios reales de WhatsApp hasta su regreso. Fase 7 en validación; fase 8 pendiente.

## Qué pasó en el último mensaje real

Al repetir la elección del martes 20/10/2026 a las 09:00, la ejecución `61d42047-8785-4d07-a540-380bb0e29c43`, secuencia 40, verificó al paciente y consultó su cita. El primer intento falló RATE_LIMIT y pidió esperar dos segundos. El segundo terminó PROVIDER_ERROR. La respuesta de derivación llegó como READ; se persistió aproximadamente 8,826 segundos después de registrar la ejecución.

Ese registro anterior no conservaba el estado HTTP ni la categoría del segundo rechazo. Por tanto, **no demuestra que el segundo fallo fuera otra cuota, una clave inválida o una llamada de herramienta mal generada**. No se debe inventar esa causa ni atribuir el fallo al transporte Kapso: entrada y respuesta fueron recibidas correctamente. Consumo conocido de las inferencias aceptadas: 4 829 tokens de entrada y 112 de salida; no incluye un coste de llamadas rechazadas que el proveedor no haya informado.

La cita `fc272d39-566d-4b65-a610-0302e86d50b5` quedó intacta: CONFIRMED, versión 0, martes 13/10/2026 09:00–10:00, un evento de historial. No se preparó ni confirmó una propuesta real. La conversación pasó a HANDOFF, generación 3, por el fallo automático.

## Diagnóstico adicional sin WhatsApp

La vista previa APP_TEST `3457fd17-a35a-4857-ae27-1ad5604a5004` reprodujo HTTP 429 con `rate_limit_exceeded`, tipo `tokens` y **TOKENS_PER_MINUTE**. Esperas indicadas: 13 y 25 segundos; el tercer fallo indicó tres segundos, pero ya se había alcanzado el límite de intentos. Terminó sin modificar registros ni enviar mensajes. Esta evidencia identifica una cuota por minuto en esa reproducción; no demuestra un agotamiento diario ni permite recuperar retrospectivamente la categoría del segundo rechazo de la secuencia 40.

La traza mostró llamadas evitables: intentar proponer antes de consultar un slot y corregir una consulta rechazada. Se ajustó la presentación de herramientas al modelo a los resultados disponibles en la ejecución. Se mantienen todas las validaciones de negocio y permisos en el servidor.

Después, la vista previa `3edcf13c-f11b-46ff-8f65-2b185dc6722e`, APP_TEST/COMPLETED, preparó correctamente la propuesta de reprogramación: Willy, Limpieza dental, Julia, del martes 13/10/2026 09:00 al martes 20/10/2026 09:00, 60 minutos y motivo Cambio de horario de trabajo. Duró **15,747 segundos**, incluidos unos 12 segundos de espera por una cuota por minuto; dos intentos, cuatro inferencias aceptadas, 8 646 tokens de entrada y 245 de salida. No hubo herramientas rechazadas en esta ejecución final. Es una medición de esa vista previa, no una promesa de latencia ni una prueba de entrega por WhatsApp.

El código y resumen APP_TEST permanecen aislados del canal real. No se confirmó esa propuesta, no se enviaron mensajes y se comprobó la conservación de la fila original de la cita, todo su historial, cargos, movimientos y número de mensajes salientes.

## Correcciones

- Bitácora con estado HTTP, código y tipo del proveedor de una lista permitida, y clase de cuota conocida cuando puede reconocerse. No conserva el cuerpo de error, `failed_generation`, claves, cuentas del proveedor ni razonamientos. Un código desconocido queda como OTHER.
- HTTP 429 respeta `retry-after`. Valores no numéricos, no finitos o no positivos usan el respaldo de 30 segundos. No se espera más que el presupuesto de la solicitud ni se exceden tres intentos.
- `tool_use_failed` en HTTP 400/422 se distingue como TOOL_GENERATION. Puede retomar el contexto con una instrucción de corrección, sin ejecutar el contenido rechazado por el proveedor. Los fallos HTTP 5xx permiten recuperación breve; otros rechazos o problemas de autenticación derivan sin reintentos automáticos indiscriminados.
- Se persiste contexto desde antes de la primera inferencia, además de los resultados posteriores: también puede recuperarse un rechazo inicial. Las herramientas que ya terminaron no se vuelven a ejecutar por un reintento.
- El modelo recibe únicamente herramientas cuyos requisitos de información ya se cumplieron: la propuesta de cambio se ofrece tras identidad, cita propia y horarios reales; la cancelación exige identidad y referencia propia. Se reduce el tamaño de las instrucciones de herramientas y se evita proponer con referencias inventadas. La consulta de catálogo y la derivación siguen disponibles.
- Antes de programar un reintento se bloquean y verifican el control y su generación. Si recepción asumió la conversación durante el fallo, se conserva su atención humana y no se reanuda ni responde automáticamente.

Estas correcciones no garantizan capacidad ilimitada de la cuenta Free. Una cuota que persista continúa produciendo una derivación controlada. No se cambió de modelo, cuenta, clave, plan ni proveedor, y no se compraron servicios.

La documentación oficial distingue [HTTP 429 y límites por organización](https://console.groq.com/docs/rate-limits), [errores de petición y fallos de servidor](https://console.groq.com/docs/errors) y [el ciclo local de herramientas](https://console.groq.com/docs/tool-use/local-tool-calling). La clasificación concreta por minuto procede de la respuesta observada, no de una suposición sobre el plan de la cuenta.

## Verificación y conservación

**Regresión final: 187 casos, cero fallos, cero errores y cero omitidos; empaquetado aprobado.** Incluye 43 casos Kapso/agente, 11 del cliente Groq y dos de etapas de herramientas. Verifica clasificación segura de errores, recuperación del primer rechazo y de rechazos posteriores, límite de tres intentos, conservación de citas y toma de control durante un fallo. Las pruebas automáticas que limpian datos usan exclusivamente `sistema_odontologo_test`; las vistas previas de Groq utilizan la instalación ficticia actual con comprobación de invariantes. Registro local ignorado: `backend/.runtime/phase7-provider-recovery-final.log`. Los fallos intermedios de preparación de pruebas no se presentan como aprobados; el resultado citado corresponde a la ejecución final.

Se mantienen 9 pacientes, 12 citas, 9 cargos y 11 movimientos. Se devolvió a AUTO **solo** la derivación automática identificada de la secuencia 40, sin responsable humano y generación 3, mediante servicio y auditoría; quedó generación 4. No se ejecutan instrucciones antiguas ni se sustituyó una conversación atendida por una persona. El intento de vista previa que había quedado PAUSED no se reprodujo al devolver el control.

Backend actualizado y comprobado UP con el artefacto final. Receptor 8082, ngrok, frontend y archivos privados conservados; no es necesario cambiar la API key ni pagar para retomar estas pruebas. No hubo cambios de interfaz: se conservan las catorce comprobaciones previas de navegador en computadora, tablet y celular, sin declararlas repetidas en este diagnóstico.

## Retomar cuando el participante vuelva

Comprobados realmente: consulta de la cita propia y disponibilidad del martes 20/10/2026. Sigue pendiente preparar una propuesta correcta **desde WhatsApp**, rechazarla, confirmar una nueva y verificar repetición, conflictos, cancelación, teléfono compartido, supervisión y demás casos de la guía. Las comprobaciones internas de fallos y reinicio siguen distinguidas de las reales.

Al regresar, verificar Agente activo y pedir una entrada nueva, sin confirmar códigos anteriores:

> Soy Willy Vilca Huaytalla. La cita es para mí. Elijo el martes 20/10/2026 a las 09:00 con Julia Huaytalla. Reprograma mi limpieza dental por el cambio de horario de trabajo.

Revisar propuesta completa y conservación de la cita original antes de continuar. El siguiente caso será **rechazar esa propuesta**, no aplicar todavía el cambio. No enviar este mensaje ni pedir al participante que lo envíe durante su ausencia. La fase 7 no se cierra con la vista previa.
