# Propuesta de ajustes después de la presentación al odontólogo

**Estado: análisis para revisión del usuario; no implementado ni incorporado como alcance confirmado.** Se mantienen `version_sencilla`, la base `sistema_odonto_mini` y la versión de registro en main. No se han cambiado código del sistema, migraciones, datos, proveedores ni configuraciones durante este análisis.

## Fuente y contraste

Se analizó el audio completo del archivo `presentacion del sistema al odontologo.mp4` (47:06,45), con transcripción local desde el inicio hasta 47:06,36 y revisión de pantallas a lo largo de toda la grabación. Las recomendaciones se identifican por el tiempo del video, no por la hora del equipo. La transcripción automática y las capturas de apoyo quedan en `.work/video-review`; no se incorporan al repositorio los datos de las conversaciones ni la grabación.

| Comentario | Referencia en la grabación | Interpretación |
|---|---|---|
| Próxima sesión visible para recordar contactar al paciente | 15:13–15:31, 17:38–18:14 | Aviso visual para el odontólogo; no equivale por sí solo a autorizar mensajes automáticos de recordatorio. |
| Limpiezas/controles periódicos con o sin límite de sesiones | 17:49–18:57 | Necesidad adicional omitida en el resumen inicial: distinguir seguimiento preventivo continuo de un plan con sesiones e importe acordados. |
| Fechas de sesiones en agenda y hora por definir | 19:01–20:33; se reitera en 44:52–45:06 | Coincide con lo solicitado: fecha explícita o periodicidad, visible en agenda sin reservar todavía una hora. |
| Anotar lo realizado y lo que queda para la siguiente visita | 28:09–30:14 | Las notas clínicas ya lo permiten; mejorar su acceso desde el flujo de la siguiente atención. |
| Cómo contactar al paciente después de una reserva nueva | 33:20–36:10 | El teléfono debe estar conservado y ser fácil de consultar; no debería ser necesario copiar el nombre y buscar otra vez la ficha. |
| Evitar entregar todos los precios a quien pregunta reiteradamente | 39:13–40:53 | Se propone un límite de tres servicios y se menciona que sea configurable. El resumen del usuario precisa que se aplique a una secuencia, no a todo el historial. |
| Imprimir el presupuesto y entregar una copia para que el paciente lo evalúe | 45:06–46:34; archivo firmado se comenta en 06:21–06:52 | Proforma antes de aceptar; aceptación posterior registrada por el odontólogo y posibilidad de conservar una copia firmada. Descargar un archivo no significa aceptar el plan. |

Crear una cita manual con servicio propio y seleccionar allí la sesión de un tratamiento son ampliaciones expresamente propuestas por el usuario en su mensaje. Son coherentes con la demostración, pero no se atribuyen como peticiones textuales del odontólogo.

## 1. Programación opcional de sesiones

En cada tratamiento del presupuesto, conservar número de sesiones e importes, y añadir una sección opcional **Programar sesiones** con tres opciones:

- Sin programación: funcionamiento actual, sin crear entradas futuras de agenda.
- Fechas individuales: una fecha para cada sesión, con posibilidad de dejar sesiones sin fecha para completarlas después.
- Frecuencia: primera fecha, intervalo positivo y unidad días/semanas/meses. Previsualizar las fechas y permitir corregir una fecha concreta antes de aceptar.

Ejemplo: tres sesiones, primera fecha 03/11/2026 y cada dos semanas → 03/11, 17/11 y 01/12. Los meses se calculan como meses calendario; una fecha como el día 31 se ajusta al último día cuando el mes no tiene ese día, conservando la referencia original para evitar desplazamientos acumulados.

Guardar o presentar el presupuesto conserva la planificación como propuesta. Al aceptar explícitamente el plan se generan, una sola vez, las sesiones fechadas pendientes de horario y los cargos acordados existentes. Repetir la aceptación no duplica sesiones ni deuda. Las sesiones sin fecha siguen pendientes dentro del plan.

## 2. Agenda y atención de las sesiones

- Mostrar las sesiones fechadas con una etiqueta textual **Sesión de tratamiento · Hora pendiente**, un estilo distinguible y ubicación separada de los intervalos ocupados. Mostrar paciente, tratamiento, número de sesión, profesional, servicio o descripción propia y duración prevista.
- Mostrar estas entradas en día/semana/mes/lista y en el panel; un apartado **Pendientes de horario** permite encontrarlas rápidamente, incluso si su fecha ya pasó. No contarlas como pacientes en espera o inasistencias.
- No inventar una hora —por ejemplo 00:00— ni bloquear disponibilidad hasta que el odontólogo la asigne.
- **Asignar hora** reutiliza disponibilidad y reserva manual existentes, con intervalo completo, jornadas, descansos, ausencias y separación. Si la hora está ocupada, la sesión sigue pendiente y se ofrecen opciones; no se pierde su planificación.
- Para servicios propios, proponer una duración editable desde la planificación; debe quedar una duración positiva para reservar una hora. En servicios de catálogo se precarga su duración.
- Si una fecha prevista cae en un día cerrado, mostrar la advertencia y permitir cambiarla; no trasladarla silenciosamente a otro día.
- Al atender, completar paciente, profesional, cita, procedimiento y vínculo con la sesión correspondiente. Finalizar la atención aumenta el avance una sola vez y no vuelve a generar el cargo de una sesión ya incluida en el plan aceptado.
- Permitir cambiar fecha/hora de una sesión individual con historial. Cambiar una frecuencia no altera sesiones realizadas ni reservas confirmadas sin una acción expresa. Al cancelar un plan, revisar sus citas pendientes/futuras mediante confirmación y conservar lo realizado y el historial.

Para una cita creada manualmente, añadir las opciones **Servicio del catálogo**, **Servicio propio** y **Sesión de un tratamiento activo**. El servicio propio guarda su descripción/duración y precio acordado cuando corresponda, sin obligar a crear un servicio en el catálogo. El tratamiento se selecciona únicamente entre los activos del paciente y precarga sus datos; no se permite usar una sesión ya realizada ni generar dos reservas para la misma sesión.

El flujo rápido desde esa cita mantiene los datos precargados. Un servicio individual genera su cargo al finalizar; una sesión de plan registra avance y permite ir al saldo del plan para cobrar un abono, sin una deuda adicional. Las notas clínicas continúan opcionales en esta variante. El agente sigue ofreciendo servicios habilitados de catálogo: esta mejora manual no lo autoriza a inventar servicios, precios o planes.

## 3. Proforma y acuerdo de tratamiento en PDF

Añadir **Descargar presupuesto PDF** desde el presupuesto presentado, sin necesidad de aceptarlo. El documento institucional incluirá:

- Identidad/logo/contactos del consultorio, profesional, paciente, código y fecha del presupuesto.
- Título, tratamientos, piezas cuando correspondan, importes por partida y total; diferenciar unidades cobradas de sesiones clínicas.
- Número de sesiones, fechas o frecuencia previstas y texto **hora por coordinar** cuando corresponda.
- Acuerdo de pago: precio total, anticipo acordado y detalle de cuotas/importes/vencimientos cuando se pacten. Si todavía no se definieron, indicarlo expresamente. No presentar un anticipo pactado como dinero ya recibido.
- Condiciones y observaciones acordadas, más espacios para nombre, fecha y firmas manuscritas.

La propuesta es incorporar condiciones de pago opcionales y estructuradas al presupuesto, necesarias para mostrar una tabla coherente en la proforma. El calendario debe incluir el pago inicial pactado y las cuotas posteriores, cuadrar con el total acordado y reutilizar el módulo financiero al aceptarse, sin emitir deuda por segunda vez. Un pago inicial que todavía no se recibió sigue siendo un vencimiento pendiente: no se descuenta ficticiamente de la deuda. Registrar dinero recibido y aplicar anticipos conserva sus operaciones y confirmaciones actuales. Fechas de cuotas y de sesiones son calendarios independientes; no se exige que coincidan.

Flujo: presupuesto → PDF para revisar → paciente acepta o solicita cambios → odontólogo registra la aceptación → cargos y sesiones planificadas. El paciente puede aceptar posteriormente por el medio que acuerde con el profesional; el agente de reservas no acepta planes financieros. Una copia firmada puede adjuntarse con el módulo de archivos existente, vinculada al paciente y al plan.

Conservar la versión de importes, identidad, condiciones, calendario y PDF que se entregó y aceptó. Cambios posteriores no reescriben el documento anterior. La proforma/acuerdo y la constancia de un pago son documentos diferentes; no se incorpora firma electrónica ni se sustituye el consentimiento clínico existente.

## 4. Consultas consecutivas de precios por WhatsApp

Propuesta concreta para revisar:

- Máximo inicial de **tres servicios distintos con precio** en una secuencia de consultas. Repetir el precio de uno de esos mismos servicios no revela un cuarto precio ni consume otro servicio.
- Contar servicios, no solo mensajes: pedir varios precios o todo el catálogo en un mensaje sigue limitado a tres.
- Consultar servicios/duración o usar internamente catálogo para reservar no cuenta como comunicar precios. La restricción protege las respuestas automáticas, no la configuración del odontólogo ni las respuestas humanas.
- Al solicitar un cuarto servicio, responder con cortesía: **Para consultar precios de otros tratamientos, te atenderá personalmente el consultorio. Puedo ayudarte a coordinar una cita.** Todavía puede continuar una reserva sin recibir más cotizaciones automáticas.
- Si insiste después de ese aviso en obtener más precios, derivar la conversación al odontólogo/atención humana y pausar la IA mediante los controles existentes.
- Definir una secuencia por contacto/canal: conservarla entre mensajes y reinicios; los eventos repetidos y reintentos no la incrementan. Las pruebas APP_TEST no consumen el límite de WhatsApp real.
- **Regla de reinicio propuesta, pendiente de confirmación:** cuando avance realmente a un flujo de reserva o transcurran treinta minutos sin consultas de precio. Un saludo aislado no reinicia el contador. No es un máximo de tres precios para toda la vida del contacto.
- Mostrar el máximo en ajustes del asistente, con valor inicial tres. Aplicar la política en el servidor, tanto al proporcionar información al modelo como al guardar su respuesta; no depender exclusivamente de instrucciones del prompt.

El sistema no identifica con certeza si una persona es un competidor. La regla se aplica objetivamente a las consultas reiteradas; la derivación permite al profesional decidir cómo continuar.

## 5. Mejoras adicionales identificadas en el video

### Seguimiento periódico sin número fijo de sesiones

El video plantea una limpieza cada cuatro o seis meses sin un límite obligatorio. Proponer una modalidad opcional **Control periódico** con servicio, paciente, profesional, primera fecha y frecuencia, con finalización opcional. Mostrar próximas ocurrencias de un periodo acotado y renovar la planificación conforme avance el calendario, evitando crear una cantidad infinita de registros.

Separarlo del plan con importe cerrado: no generar una deuda infinita al activar un seguimiento. Si es un servicio individual preventivo, su cargo nace al realizarlo; si forma parte de un plan contratado, conservar las condiciones de ese plan. Esta ampliación necesita confirmación del usuario, porque la base original excluía citas recurrentes.

### Avisos visuales y continuidad de la atención

- En el panel mostrar sesiones próximas por coordinar y pendientes cuya fecha ya pasó; usar texto/fecha además de color para que el odontólogo recuerde revisar y contactar al paciente. Un aviso visual no envía WhatsApp.
- En la nueva atención mostrar, a demanda y con permisos clínicos, el resumen de la última atención del paciente/plan y sus notas de evolución/indicaciones. Reutilizar esas notas para consultar qué se completó y qué dejó anotado para la siguiente visita, sin obligar a rellenar otro formulario ni duplicar historia clínica.
- En el detalle de la cita mostrar contacto/teléfono y responsable cuando corresponda, junto con acceso a ficha/conversación. La ficha rápida y el contacto creado desde WhatsApp ya conservan teléfono; evitar el recorrido de copiar nombre y volver a buscar Pacientes. No imponer completar toda la ficha.

Las radiografías/fotos intraorales, notas por sesión, abonos parciales, cuotas, reportes/PDF y atención humana que se comentan en el video ya tienen base implementada. Se reutilizarán; no se propone un conector de cámara ni una nueva contabilidad.

En 41:00–44:32 se discute contratar WhatsApp y separar número personal/de consultorio. Es una decisión de instalación pendiente; no se cambiarán número, plan o claves en estos ajustes ni se establece como requisito adquirir dos teléfonos físicos. La modalidad concreta se verificará al activar el número propio.

## Integración y comprobaciones previstas

Extender presupuestos/planes con planificación y acuerdo de pago; reutilizar agenda/disponibilidad, atención y progreso, finanzas/cuotas, composición institucional PDF y archivos PostgreSQL. Mantener capas/DTO/permisos, paginación y consultas acotadas, importes exactos e historial. La política de precios será un servicio específico del agente con persistencia y bitácora, independiente de las reglas clínicas/financieras.

Orden propuesto: (1) sesiones/agenda/atención y controles periódicos aprobados; (2) proforma, aceptación y acuerdo de pago; (3) límite de precios y revisión de conversación; (4) comprobación conjunta de los flujos y de la interfaz en computadora, tablet y celular.

Casos críticos: aceptación repetida; sesión sin hora que no bloquea; asignación simultánea con recepción/agente; fecha no laborable; reprogramación fallida conservando lo anterior; servicio propio; tratamiento correcto del paciente; sesión ya atendida; finalización/cobro sin cargo duplicado; cuotas sin repetir deuda; PDF antes/después de aceptar y documentos históricos; tres precios permitidos, cuarto restringido, petición masiva, insistencia, reinicio de secuencia y duplicados; IA pausada por atención humana. También se comprobarán los módulos existentes afectados.

La propuesta no inicia desarrollo, fase 9 ni recordatorios automáticos. Las ampliaciones y las reglas sugeridas quedan pendientes de revisión del usuario.
