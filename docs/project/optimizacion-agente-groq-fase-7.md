# Corrección de cuota y latencia del agente: fase 7

07/10/2026. Rama `kapso`. Flujo nuevo `supervised-v7.1`; se conserva Groq `openai/gpt-oss-20b` y el alcance funcional 1.9.

## Incidencia real

La consulta de reprogramación del participante, ejecución `a72d477c-10d0-4072-90af-66b9b1c351b6`, secuencia 34, terminó FAILED/RATE_LIMIT y derivó a recepción. Duró aproximadamente 101 segundos, con tres intentos, 18 648 tokens de entrada y 1 129 de salida. La primera consulta sí verificó al paciente, encontró su cita y calculó opciones para el 20/10/2026 antes de las 23:00:02. Falló después una llamada al modelo, que devolvió HTTP 429.

El reintento anterior reconstruía la conversación desde el principio, perdía las referencias obtenidas en esa ejecución y repetía consultas. Las esperas eran fijas de 30 y 60 segundos. El tercer intento además envió una referencia de disponibilidad que el servidor rechazó; no se aplicó una operación. Se conservan todos esos pasos, no se presentan como aprobados.

La cita `fc272d39-566d-4b65-a610-0302e86d50b5` permaneció CONFIRMED, versión 0, 13/10/2026 09:00–10:00 y un único evento de historial. No hubo cambios en pacientes, cargos o dinero.

Groq documenta límites por organización, HTTP 429 y la cabecera `retry-after`; el límite concreto de cada cuenta se consulta en su consola. La documentación permite esfuerzo `low` en GPT-OSS. No se infiere una cuota diaria o por minuto concreta del error genérico almacenado en esta incidencia. Fuentes oficiales consultadas: [límites](https://console.groq.com/docs/rate-limits), [esfuerzo y exclusión de razonamientos](https://console.groq.com/docs/reasoning).

## Corrección

- La consulta de citas propias termina con los datos autorizados de la herramienta cuando no se pide otro trámite. La disponibilidad termina con los horarios devueltos cuando falta elegir una hora, está ocupada o solo se consulta. No necesita otra inferencia para redactar esos resultados.
- Se priorizan las opciones nuevas frente al resumen de la cita anterior. La consulta de citas devuelve los identificadores de su servicio y profesional, evitando buscar otra vez el catálogo para reprogramar. Las visitas manuales sin servicio de catálogo siguen siendo consultables.
- Se compactan las instrucciones manteniendo límites, identidad, negaciones, referencias y confirmación. `odontocare.ai.reasoning-effort=low` es el valor inicial configurable; no se cambia la clave privada ni se compra un plan.
- V22 añade un punto de continuación interno `model_checkpoint` en `agent_run`: mensajes administrativos, resultados autorizados y número de llamadas completadas. Tras un límite temporal se retoma el intercambio sin repetir las herramientas completadas. Se reconstruye la evidencia de catálogo/disponibilidad y se conserva el presupuesto de llamadas entre intentos. Se limpia al terminar o al pedir un reanálisis manual.
- `retry-after` reemplaza la espera fija; hay un respaldo acotado si falta o es inválido. No se programa una espera que, sumada al tiempo de la próxima llamada, exceda la ventana de reintento configurada. Un restablecimiento largo deriva sin esperar tres ciclos. Cada fallo queda en bitácora con intento, código y demora, sin cuerpos privados del proveedor.
- Se comprueba control/identidad al retomar y al ejecutar herramientas o enviar. La continuación no ejecuta confirmaciones suspendidas ni sustituye una confirmación expresa. Una propuesta ya guardada en esa misma ejecución se recupera sin pedir al modelo que la cree otra vez.

## Verificación

La regresión completa aprobó 169 casos, sin fallos ni omitidos. Después se añadió la prueba de consultas de visitas sin catálogo y se repitieron 51 casos de agente/cliente Groq con la corrección final: cero fallos y cero omitidos; empaquetado aprobado. Son 170 casos distintos comprobados entre ambos bloques, sin sumar las repeticiones. Las regresiones usan exclusivamente `sistema_odontologo_test`.

Se probó la misma solicitud con Groq real en la instalación actual, fuente APP_TEST: ejecución `83636ab0-21de-41f2-a345-d94fed2df44c`, COMPLETED, un intento, **2,331 segundos**, 5 131 tokens de entrada y 202 de salida. Herramientas: verificar_paciente → consultar_mis_citas → consultar_horarios. Ofreció el 20/10/2026 a las 09:00, 09:15 y 09:30 con Julia, duración 60 minutos. La entrada registrada se redujo aproximadamente 72,5 % frente a la ejecución fallida con repeticiones; no es una medición de ahorro universal.

No se generó propuesta, cita ni respuesta saliente de WhatsApp en esa vista previa. Se conservan 9 pacientes, 12 citas, 9 cargos y 11 movimientos. No es una prueba de entrega real ni se promete que todo mensaje futuro tarde 2,3 segundos: el historial/canal y la cuota disponible pueden variar.

Se devolvió a AUTO únicamente la derivación automática RATE_LIMIT, generación 1, producida por esta incidencia, usando el servicio de supervisión y auditoría. La generación pasó a 2, sin atender mensajes antiguos. No se sustituye atención humana asumida; la repetición necesita un mensaje nuevo del participante. Receptor 8082 y ngrok se conservan.

Antes de V22 se guardó un respaldo local de la instalación: `.runtime/backups/sistema_odontologo-before-agent-optimization-20261007.dump`, SHA-256 `D94405358466E566A18BEDA37CE8A565C350C1E040FF173ACCFBA0BAAABB4686`. El archivo queda ignorado y no contiene claves de proveedores. No se hizo restauración; esa comprobación integral pertenece a fase 9.

**Repetición real aprobada:** el participante envió de nuevo la consulta y confirmó llegada «casi instantánea» de los tres horarios correctos. Ejecución `fe017ec1-e329-41b7-ba8f-2d1341728fdb`, KAPSO/COMPLETED, un intento, respuesta READ, 6 922 tokens de entrada, 230 de salida. La respuesta se persistió 7,589 segundos después de registrar la ejecución; esta medida incluye al trabajador y no equivale al tiempo de lectura en el teléfono. No cambió la cita ni su historial/finanzas. La incidencia queda corregida también mediante WhatsApp real. Siguen pendientes propuesta, rechazo, confirmación y demás casos de fase 7 en [el registro real](validacion-real-agente-kapso-fase-7.md).
