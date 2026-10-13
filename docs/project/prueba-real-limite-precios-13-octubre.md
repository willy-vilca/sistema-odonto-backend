# Prueba real del límite de precios — 13/10/2026

Estado: prueba real en curso; dos consultas aprobadas. Solo comprobar tres precios permitidos y rechazo del cuarto, sin reservar citas, pruebas masivas, insistencias adicionales ni compras. Usar `version_sencilla`, `sistema_odonto_mini` y configuración privada existente.

El usuario informa que completó correctamente los demás recorridos de la guía de ajustes. No se repiten ni se les atribuye una nueva ejecución automática.

## Preparación comprobada

- Backend 8080 y frontend 5173 disponibles; Kapso/IA configurados y sus trabajadores reactivados. Twilio queda detenido mientras se usa Kapso. Esta autorización supera la pausa temporal del cierre anterior. No se modificaron claves, archivos privados, reglas administrativas ni main.
- Conversación del participante AUTO, generación 7. Sin tareas de IA ni envíos antiguos en cola; no se reintenta el envío fallido histórico.
- Límite configurable vigente: tres servicios; sin secuencia de precios previa. Horario del agente sin jornadas restrictivas.
- Catálogo actual: Limpieza dental S/100, 30 minutos; Blanqueamiento S/200, 60 minutos; Evaluación general S/50, 30 minutos.
- Solo había tres servicios. Se creó mediante DentalServiceService, validación y auditoría un cuarto ficticio **Consulta de prueba de precios**, S/80 y 30 minutos, sin asignación a profesionales. ID `6975618a-8df2-4efe-8ef3-6377c0cc66bd`. Desactivarlo mediante el servicio existente cuando se compruebe el cuarto intercambio; conservar su registro y la bitácora.
- Referencia inicial del negocio: tres pacientes, trece citas, catorce entradas de cargo y once movimientos de dinero. Ninguna reserva, deuda o pago se creó durante la preparación.

## Secuencia prevista

Enviar un mensaje por vez, esperar respuesta y comprobar entrada, herramientas, precios persistidos y entrega antes de continuar. No espaciar las consultas treinta minutos: el plazo forma parte del reinicio deliberado de la secuencia.

1. «¿Cuánto cuesta una limpieza dental? Solo quiero información, no quiero reservar ninguna cita.» → precio vigente 100, una exposición registrada, sin reserva.
2. «¿Cuánto cuesta el blanqueamiento? Solo quiero consultar su precio, sin reservar.» → 200, dos servicios registrados.
3. «¿Cuánto cuesta la evaluación general? Solo quiero consultar su precio, sin reservar.» → 50, tres servicios registrados.
4. «¿Cuánto cuesta la consulta de prueba de precios? Solo quiero consultar su precio, sin reservar.» → aviso de límite sin revelar 80, sin nueva reserva ni modificación financiera.

No declarar esta secuencia aprobada antes de contrastar los cuatro intercambios reales. Las pruebas de reinicio, consulta masiva e insistencia ya tienen evidencia simulada y no se repetirán aquí para ahorrar crédito. La recepción del usuario y el estado de entrega del proveedor son evidencias distintas.

## Resultados

| Paso | Resultado contrastado | Evidencia |
|---|---|---|
| 1. Limpieza dental | Respuesta con PEN 100.00 y 30 minutos; recepción confirmada por el usuario. Un servicio contado, cero avisos de límite. | Entrada 13/10/2026 12:41:32 Lima; ejecución `6072b955-1b1b-48c3-af76-53bf9480bd3b` COMPLETED en un intento. Una inferencia (1617/43 tokens), catálogo filtrado por «limpieza», respuesta READ en un envío. AUTO/generación 7 y referencia de negocio 3/13/14/11 conservados. |
| 2. Blanqueamiento | PEN 200.00 y 60 minutos, recepción confirmada por el usuario. Dos servicios distintos contados, cero avisos. | Entrada 12:43:52 Lima; ejecución `a2c9913a-7cdb-4767-9f77-472b8397c6ce` COMPLETED, una inferencia (1873/42 tokens), catálogo actual de blanqueamiento verificado. READ en un envío; AUTO/generación 7 y negocio 3/13/14/11 conservados. La consulta de apoyo del servicio previo no consumió otra exposición. |

Pasos 3–4 pendientes. No se enviaron mensajes adicionales ni se hicieron llamadas nuevas al modelo durante esta comprobación por lectura.
