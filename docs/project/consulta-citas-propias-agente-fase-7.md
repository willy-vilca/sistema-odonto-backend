# Consulta de citas propias tras atención humana

08/10/2026, America/Lima. Corrección supervised-v7.10; Groq/openai/gpt-oss-20b, Kapso. Datos ficticios de la instalación actual. Sin cambios de claves, proveedor, esquema o frontend.

## Diagnóstico

El usuario devolvió la conversación desde la interfaz: AUTO/generación 12, auditado, sin ejecutar la solicitud anterior. La secuencia 76 pasó de PAUSED a GROUPED y conservó cero pasos/tokens y ninguna respuesta; reservas y finanzas intactas. La devolución funciona, pero la consulta nueva tuvo una incidencia distinta.

Secuencia 77, ejecución `8ca830e7-b56f-4e7a-820a-3d54b7c3c3fd`, COMPLETED/READ, un intento y dos inferencias. Preguntó cuál cita interesaba y pidió decir «una» si solo había una. 3 665 tokens de entrada, 103 de salida; 6,396 segundos hasta respuesta persistida. No hubo cuota ni pérdida de la reserva.

El modelo usó pacientes_contacto con nombre completo Mateo Prueba Familia y obtuvo una ficha asociada al teléfono. No ejecutó verificar_paciente ni consultar_mis_citas. Después de devolver el control, verified_at seguía vacío: buscar una ficha no sustituye verificar identidad/relación ni leer la agenda. La pregunta sobre cuántas citas había carecía de respaldo; prueba funcional fallida.

## Corrección

- Las consultas administrativas explícitas de citas propias tienen una etapa de verificación; buscar contactos no concede lectura de citas.
- Tras verificar, el flujo ejecuta consultar_mis_citas con las mismas reglas de teléfono/paciente/relación, página 0 y límite 5. Para la consulta informativa termina con los datos reales, sin otra inferencia que invente una cantidad o una ambigüedad.
- Se bloquean respuestas sobre citas sin evidencia de esa lectura. Se conserva la posibilidad de preguntar por nombre completo o relación si faltan; cambio, cancelación, disponibilidad y precio siguen sus flujos propios.
- Las lecturas no crean cargos ni modifican la agenda. Control/generación y aislamiento APP_TEST se conservan; no se revive una solicitud anterior por devolver el control.

## Verificación

La reproducción nueva falló antes de corregir en `phase7-own-query-before-fix.log`. El bloque posterior comprobó 93 casos: 91 pasaron y dos aserciones antiguas esperaban dos inferencias cuando ahora basta una; los datos/respuestas ya eran correctos. Se actualizaron esas dos aserciones y ambas repeticiones pasaron en `phase7-own-query-counts-final.log`. Son 93 casos distintos verificados, no una única ejecución de 93 sin fallos. Registro inicial `phase7-own-query-final-tests.log`; incluye 57 Kapso/agente, seis de definiciones y cliente/fechas/formato. Empaquetado aprobado.

Casos nuevos: consulta tras devolver control sin cambios de reserva; dato de identidad faltante; búsqueda de ficha seguida de falsa ambigüedad o falsa ausencia, con recuperación hacia lectura real; consulta obligatoria sin forzar catálogo/disponibilidad. Las regresiones anteriores de permisos, relaciones, reprogramación/cancelación, negaciones, recuperación y control se conservan.

Groq real APP_TEST `d5c000b8-7e8a-403f-a2f8-e8d71182095f` completó en un intento, una inferencia y 0,989 segundos de procesamiento; 2 009 tokens de entrada y 54 de salida. Verificó Mateo/GUARDIAN y leyó únicamente su cita actual. No mostró Lucía ni generó envío. Huellas de pacientes/contactos/citas/historiales/cargos/movimientos, contexto real, control y fila antigua de la secuencia 76 permanecieron iguales. Registro ignorado `phase7-own-query-groq-preview.log`.

## Repetición real

Backend actualizado y salud UP, AUTO/generación 12 conservado; receptor/ngrok y frontend intactos. El participante repitió su consulta y recibió solo Mateo/Limpieza/Julia/martes 20/10/2026 10:00/60 minutos, sin pregunta de cantidad ni cambio.

Secuencia 79, ejecución `c7310e9e-8e32-4706-8df0-ec6b9d2a9c2f`, COMPLETED/READ, un intento, una inferencia, 1 550 tokens de entrada y 42 de salida. Verificar_paciente → consultar_mis_citas → guardar_respuesta; 2,505 segundos hasta respuesta persistida. Consulta limitada al UUID de Mateo, página 0/límite 5 y un resultado, sin Lucía ni fichas ajenas. La secuencia 76 continúa GROUPED con cero pasos/tokens y sin respuesta; ambas reservas y cifras 11/15/9/11 conservadas.

La reanudación real con mensaje nuevo queda aprobada. Supervisión durante una inferencia en curso y recuperación tras interrupción siguen identificadas como pruebas internas. Se continúa con límites, derivaciones y horarios; fase 7 permanece abierta.
