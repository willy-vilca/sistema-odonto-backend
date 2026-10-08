# Validación real del agente por WhatsApp

07/10/2026, America/Lima. Intercambio real completado y verificado. Evidencia del criterio A22 y del [cierre de fase 6](cierre-fase-6.md).

El participante utiliza su WhatsApp autorizado y el Sandbox de Kapso. Se comprueban entradas auténticas, herramientas, cambios en PostgreSQL y estados reales de respuesta; el participante informa además el contenido que recibió. Se usa la instalación ficticia actual, en ramas kapso, con Groq/openai/gpt-oss-20b.

Datos de referencia: Limpieza dental, S/ 200.00, 60 minutos; Julia Aracelly Huaytalla Alarcon. Próximo lunes: 12/10/2026, bloqueado por «Cierre general». Próximo martes: 13/10/2026, jornada 09:00–17:00. No se modifica ese cierre para forzar una reserva.

Línea base: 9 pacientes, 11 citas, 9 cargos y 11 movimientos de dinero. Las preguntas informativas y los descartes deben conservar estos valores. La reserva solo se crea tras recibir confirmación expresa del resumen previamente enviado.

## Resultados

| Caso | Evidencia y resultado |
|---|---|
| Consulta inicial de servicio, precio y duración | Ejecución 0f27476e-122d-4f93-bcfb-e4e4ea481f18. Mensaje real y respuesta Leída, pero el modelo omitió herramientas e inventó S/ 120 y 50 minutos. Fallo identificado; sin pacientes ni citas nuevos. |
| Corrección y repetición de la consulta | Ejecución 66c6c1b4-3728-4087-a039-531dbcca7c8e. El control rechazó la afirmación sin evidencia; el agente consultó consultar_servicios y respondió S/ 200 y 60 minutos, sin iniciar reserva. Respuesta Leída y confirmada por el participante. Línea base conservada. |
| Solicitud incompleta «Quiero una cita» | Ejecución 79b31a99-be72-45f4-b712-03dd8180c151. Preguntó por el nombre completo; respuesta Leída y confirmada por el participante. No creó cita. |
| Primera consulta del lunes | Ejecución 9b569623-ed68-4cb7-9096-3c55cd482906. El modelo envió una propuesta para el viernes 09/10/2026, calculando mal el desplazamiento. Alcanzó la cuota gratuita y se recuperó en su tercer intento. Propuesta incorrecta sin confirmar; no creó cita. Se corrigió la resolución del día solicitado y la búsqueda de profesional por nombre. |
| Negación de la propuesta incorrecta | Ejecución 903a8579-0d46-44e1-805f-bda8d5906af6. Descartó la propuesta mediante descartar_propuesta; estado SUPERSEDED y respuesta Leída, confirmada por el participante. Agenda conservada con 11 citas. |
| Repetición de fecha relativa y lunes cerrado | Ejecución 1850d835-8545-46fd-a08e-c77632a6b1e4. consultar_horarios buscó el 12/10/2026 con dentist_name Julia Huaytalla: cero horarios. Respuesta Leída y confirmada; no propuso otro día ni creó cita. |
| Disponibilidad del próximo martes sin elegir | Ejecución 456fe3dd-6535-4de2-8ec4-0f62ebee5692. Ofreció 13/10/2026 09:00–10:00, 09:15–10:15 y 09:30–10:30 según agenda. Respuesta Leída y confirmada. No creó propuesta ni cita. Se identificó formato de tabla poco adecuado para WhatsApp y se indicó usar listas sencillas. |
| Elección y propuesta del martes | Ejecución 20153de8-ab4c-40b1-893f-0c9d3ffcb831. Resumen canónico: Willy Vilca Huaytalla, Limpieza dental, Julia Aracelly Huaytalla Alarcon, martes 13/10/2026 09:00, 60 minutos. Código 5F99CF1D. Respuesta Leída y confirmada; propuesta PENDING y aún 11 citas. |
| Confirmación ambigua «sí» | Ejecución 4003abbc-3f52-484b-9313-5c259025cff3. La cuota de Groq demoró la aclaración; se corrigió para resolver reconocimientos ambiguos directamente desde la propuesta pendiente, sin una nueva llamada al modelo. El trabajo original se recuperó en su tercer intento y envió el mismo resumen con solicitud de confirmación expresa. No creó cita ni otra propuesta. |
| Confirmación expresa, cita y respuesta | Ejecución cf83eb96-cc66-479c-8292-23815daf52b9. Entrada auténtica «Sí, confirmo la cita». Cita fc272d39-566d-4b65-a610-0302e86d50b5 persistida CONFIRMED/WHATSAPP, paciente existente PAC-000009, Julia, Limpieza dental, martes 13/10/2026 09:00–10:00. Bitácora crear_cita_confirmada y guardar_respuesta. Respuesta Leída y confirmada por el participante. No se creó otra ficha ni se modificaron finanzas. |
| Repetición sin duplicar | Ejecución 3d4661d2-1d7e-4a0d-9695-11a291cc845e. Nueva entrada «Sí, confirmo la cita». Devuelve la misma cita fc272d39-566d-4b65-a610-0302e86d50b5 y respuesta Leída, confirmada por el participante. Totales: 9 pacientes, 12 citas, 9 cargos y 11 movimientos. |
| Primera consulta de hora ocupada | Ejecución 38cd1492-91d8-4e07-a43f-9ceea6f112ee. consultar_horarios devolvió 10:00–11:00, 10:15–11:15 y 10:30–11:30, pero el control de afirmaciones de reserva sustituyó la respuesta por mencionar la cita confirmada existente. Sin cambios de agenda. Se corrigió la preservación de alternativas verificadas y se agregó regresión específica. |
| Repetición de hora ocupada y alternativas | Ejecución 298d7eb1-bf4a-45f2-935e-a1eed20d42d5. Consulta exacta de martes 13/10/2026 09:00 con Julia; no disponible y tres alternativas libres desde las 10:00. Respuesta en lista sencilla Leída y confirmada por el participante. Cita original intacta y ninguna nueva. |
| Límites de acceso del agente | Ejecución 6ad6518b-8dc1-4f1c-b095-5a797f78f1ef. Rechazó acceso a historia clínica, diagnósticos y saldo, derivando a recepción. No invocó herramientas clínicas ni financieras. Respuesta Leída y confirmada por el participante; pacientes, citas, cargos y dinero sin cambios. |

## Corrección detectada durante la demostración

Se reforzó la instrucción de consultar datos vigentes y de no convertir una pregunta informativa en reserva. AgentCatalogEvidence verifica las cifras de precio y duración de la respuesta frente a resultados de consultar_servicios en la misma ejecución. Una afirmación sin respaldo no se envía; se solicita la herramienta y se conserva el límite de llamadas. Si el modelo insiste, se termina con un error controlado. Los resultados rechazados permanecen en la bitácora, separados del texto enviado al paciente.

La inferencia mantiene reasoning oculto y utiliza temperature=0 y reasoning_effort=medium, admitidos por la [documentación oficial de Groq](https://console.groq.com/docs/reasoning). El modelo sigue siendo intercambiable por la configuración existente; no se contrataron servicios.

31 comprobaciones específicas y una regresión completa de 142 casos aprobadas tras el ajuste del catálogo. Incluyen una respuesta inicialmente inventada que se corrige antes del único envío y una insistencia del modelo que termina sin enviar importes falsos.

Después de la propuesta de fecha incorrecta se agregó AgentRequestedDate, que resuelve un único día de semana expresado en español según fecha de recepción y zona del consultorio, respetando «próximo». La consulta no utiliza un desplazamiento aritmético contradictorio del modelo. La propuesta y la confirmación verifican también ese día; una propuesta anterior para otro día no se puede confirmar. Fechas con varios días posibles se mantienen como datos que requieren aclaración. El resumen agrega el día de semana. Se agregó búsqueda por dentist_name en el repositorio para evitar usar nombres como UUID; mantiene filtros y límite de cinco profesionales. Una propuesta válida termina con su resumen del servidor inmediatamente, sin otra llamada al modelo para redactarlo.

34 casos específicos aprobados tras el ajuste de días y búsqueda de profesional. La regresión completa posterior aprobó 146 casos, incluyendo el rechazo de un horario de otro día al proponer y una propuesta antigua incorrecta incluso ante confirmación expresa. Se ampliaron los tres casos unitarios de calendario, todos aprobados, para conservar fechas explícitas, resolver «próxima semana» y rechazar fecha/día contradictorios. Los días de semana con un mes escrito se dejan al análisis de la fecha absoluta, evitando sustituirlos por el día más cercano. Las pruebas automáticas que limpian datos se ejecutan únicamente en sistema_odontologo_test.

La concurrencia con recepción, eventos repetidos, atomicidad y recuperación de respuestas ya cuentan con pruebas de aplicación; no se genera carga sobre el Sandbox para repetirlas.

Los reconocimientos breves «sí», «ok» y preguntas «¿Confirmo?» se distinguen de consentimiento explícito. Con propuesta pendiente de la misma fuente, el servidor pide confirmación y muestra el resumen existente sin reejecutar IA ni crear una nueva propuesta. No se muestra una propuesta APP_TEST como contexto administrativo de una entrada real; se reforzó también este aislamiento al seleccionar propuestas. La vigencia se refiere a los 30 minutos desde la creación original y no se prolonga al mostrar otra vez el resumen.

AgentAvailabilityReply conserva una respuesta basada exclusivamente en los horarios devueltos por la herramienta cuando el control de afirmaciones de reserva necesita sustituir texto del modelo. Evita perder las alternativas por una referencia a una cita anterior. Tras ese ajuste, los 18 escenarios de integración Kapso/agente volvieron a aprobarse. La regresión completa final aprobó 150 casos, incluidos 19 escenarios Kapso/agente y el aislamiento de propuestas reales/pruebas en un mismo teléfono. V19 proyecta la fuente y mantiene una propuesta pendiente por conversación/fuente; referencias de horarios, descarte y selección del contexto respetan esa separación. Esta comprobación adicional es interna, no otro intercambio real.

## Resultado final

Los catorce intercambios de esta sesión conservaron entradas, ejecuciones y respuestas en la conversación real; todos sus envíos terminaron READ. Incluyen los fallos detectados, sus correcciones y repeticiones; no se borran para presentar una demostración artificialmente limpia.

Se creó exactamente una cita adicional: fc272d39-566d-4b65-a610-0302e86d50b5, para Willy Vilca Huaytalla, Limpieza dental con Julia Aracelly Huaytalla Alarcon, martes 13/10/2026 de 09:00 a 10:00 America/Lima, CONFIRMED/WHATSAPP. Totales finales: 9 pacientes, 12 citas, 9 cargos y 11 movimientos de dinero. La ficha existente fue reutilizada y las finanzas conservaron su línea base.

La consulta del lunes comprobó el día no laborable; no se quitó el bloqueo para conseguir la demostración. La del martes comprobó fecha relativa, disponibilidad, elección, propuesta, consentimiento y persistencia. La repetición y la hora ocupada preservaron la cita. La bitácora relaciona el resultado con la entrada y la tarea de respuesta.

No se envió carga al Sandbox ni se contrataron servicios. Se conserva Kapso para desarrollo/pruebas y Groq/openai/gpt-oss-20b. Los límites gratuitos pueden provocar esperas; el agente tiene reintentos acotados y estados visibles. Las verificaciones externas no acreditan rendimiento sostenido ni una instalación de producción.
