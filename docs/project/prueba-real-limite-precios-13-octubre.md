# Prueba real del límite de precios — 13/10/2026

Estado: tres consultas aprobadas; cuarta detectó una omisión del modelo, corregida en v7.13 y pendiente de repetir por WhatsApp. Solo comprobar tres precios permitidos y rechazo del cuarto, sin reservar citas, pruebas masivas, insistencias adicionales ni compras. Usar `version_sencilla`, `sistema_odonto_mini` y configuración privada existente.

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
| 3. Evaluación general | PEN 50.00 y 30 minutos, recepción confirmada por el usuario. Tres servicios contados, cero avisos: límite completo antes de la cuarta consulta. | Entrada 12:45:54 Lima; ejecución `251ac671-be47-4c1d-b6f8-c5e4c4dbaf33` COMPLETED, una inferencia (1813/37 tokens), catálogo de evaluación verificado. READ en un envío; AUTO/generación 7 y negocio 3/13/14/11 conservados. |
| 4. Primer intento | No mostró 80, pero respondió incorrectamente «no dispongo de información sobre ese servicio». **No se aprueba como aplicación del límite.** | Entrada 12:48:36 Lima; ejecución `9349f222-8dd1-4ce6-88a2-289fcc518a36`. Servicio temporal activo y habilitado. Solo consultó como apoyo Evaluación general, el modelo no buscó el servicio solicitado y respondió sin herramientas. READ en un intento; contador tres, avisos cero. Se conserva la incidencia original. |

## Corrección de la cuarta consulta

Flujo **supervised-v7.13**: una pregunta directa de precio con un único nombre completo verificado en el catálogo se resuelve en el servidor mediante la herramienta existente, sin inferencia. Coincidencia normalizada de mayúsculas/acentos, consulta acotada y exclusión de servicios/categorías inactivos o no habilitados. El precio y la existencia se verifican antes de responder; una búsqueda de nombre exacto no cotiza por accidente servicios de nombre parecido. Consultas ambiguas y otras intenciones mantienen el flujo del modelo, sin atribuir falsamente un límite a un servicio inexistente.

El aviso explícito es: «Ya alcanzamos el límite de consultas de precios seguidas. Por ahora no puedo darte más precios. Si deseas, puedo ayudarte a reservar una cita; para más información te atenderá personalmente el consultorio.» La validación de control/generación, persistencia, confirmación, vigencia, reinicio e idempotencia se conservan.

**116 casos distintos del agente aprobados en bloques**, incluidos cuatro nuevos: tres precios y cuarto rechazo sin dependencia del modelo, acentos/nombres similares/repetición, configuración/reinicio a treinta minutos/insistencia y separación APP_TEST/servicio inexistente. La primera regresión aprobó 115; el único fallo restante era la referencia esperada de versión v7.12 en la prueba de recuperación, actualizada a v7.13 y repetida con resultado aprobado. Compilación/empaquetado y diferencias Git comprobados. Pruebas únicamente con proveedores simulados en mini_test, sin envíos ni inferencias externos adicionales. No hubo cambios frontend.

Backend corregido reactivado en 8080 con el contador real aún en tres y sin avisos ni tareas antiguas en cola. No se reejecutó ni se sustituyó el mensaje fallido anterior. **La repetición real del paso 4 sigue pendiente**; mantener activo el servicio temporal hasta contrastarla y desactivarlo al cerrar la prueba.
