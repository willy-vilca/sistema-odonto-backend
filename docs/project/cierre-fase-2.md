# Cierre de fase 2 — pacientes y agenda manual confiable

Fecha: 1 de octubre de 2026. Alcance de referencia: versión 1.2. Estado: completada técnicamente y disponible para revisión del usuario.

## Funciones entregadas
- Fichas con código correlativo único, documento opcional, nacimiento, dirección, correo, contactos, emergencia y notas administrativas. Fichas provisionales, responsables de menores y responsables de pago; estado activo/inactivo sin borrar citas.
- Búsqueda por nombre, código, documento o contacto; filtros y páginas del servidor. Un teléfono puede pertenecer a varios pacientes y cada reserva selecciona expresamente su paciente.
- Prevención de documentos repetidos y de fichas con nombre, nacimiento y contacto coincidentes. Los contactos secundarios también se comprueban. Las versiones protegen incluso una edición que solo cambia contactos.
- Agenda por odontólogo en día, semana y mes; lista inicial para celulares con fechas, estado, búsqueda y paginación. Los calendarios consultan intervalos acotados de hasta 42 días y 1.000 citas.
- Reserva con servicio activo asignado o motivo administrativo con duración. Horarios disponibles calculados con jornadas, descansos, excepciones, anticipación y separación; sugerencias cada 15 minutos y escritura manual de otra hora.
- Confirmación, espera, atención, atención completada, cancelación, ausencia y reprogramación según transiciones permitidas. Cambios con motivo, responsable e historial inmutable; historial paginado, búsqueda y filtro por movimiento.
- Duración e intervalo conservados en cada cita. El catálogo no modifica reservas previas; reprogramar puede conservar la duración o usar explícitamente la actual, con nueva comprobación.
- Bloqueo por profesional y exclusión GiST en PostgreSQL para impedir solapamientos. Una solicitud fallida no cambia la reserva, la auditoría ni el historial. Una clave UUID evita reservas duplicadas al reintentar.
- Permisos PATIENTS_READ/WRITE y APPOINTMENTS_READ/WRITE, validados en el servidor. Recepción opera fichas y citas; odontólogo y caja tienen consulta administrativa por defecto. La política permanece editable por el administrador.
- Auditoría de creación/edición de pacientes y reserva, reprogramación y estado de citas. El registro técnico no guarda contactos, notas personales ni credenciales.

## Evidencia de validación
Backend: 32 pruebas aprobadas (21 de regresión y 11 de fase 2), sin fallos, con PostgreSQL real en sistema_odontologo_test. Interfaz: 22 pruebas de navegador aprobadas (17 de regresión y 5 de fase 2). Compilación de frontend, revisión estática y formato aprobados.

| Escenario | Resultado |
|---|---|
| Cita de 60 minutos desde las 10:00 | Fin calculado a las 11:00 |
| Otra cita solapada del mismo profesional | Rechazada; otra para un profesional distinto admitida |
| Escritura concurrente del mismo intervalo | Una reserva aceptada y una rechazada; un solo movimiento creado |
| Escritura directa solapada en PostgreSQL | Rechazada por la exclusión de intervalos |
| Descanso, ausencia, día no laborable, anticipación y separación | Rechazos comprobados con datos reales |
| Servicio inactivo o sin asignación | Rechazado; deshabilitar reserva automática no impide reserva manual |
| Cambiar servicio de 60 a 30 minutos | La cita anterior conserva 60 minutos |
| Reprogramar hacia un intervalo ocupado | La cita y su historial originales permanecen sin cambios |
| Reprogramar hacia un intervalo libre | Nuevo horario e historial; duración anterior por defecto |
| Confirmar, atender, cancelar y conservar estados terminales | Transiciones y motivos verificados; cancelación libera el intervalo |
| Reintentar una reserva y reutilizar una clave con otro contenido | Sin duplicación; contenido diferente rechazado |
| Registrar dos hijos con teléfono de su madre | Dos códigos y fichas; responsable obligatorio en menores |
| Documento o ficha duplicados | Rechazo; los datos del formulario se conservan |
| Completar ficha provisional y cambiar solo contactos | Versiones y rechazo de formularios antiguos comprobados |
| Consultar historial con filtro de movimiento | Consulta en el servidor y filtro inválido rechazado |
| Solicitar operaciones restringidas directamente | HTTP 403 con una sesión y CSRF válidos |
| Zona del navegador distinta a la del consultorio | Agenda consistente; pruebas con navegador en Asia/Tokyo y sede en America/Lima |
| Horarios ambiguos o inexistentes por cambio de hora | Rechazados en la zona configurada |
| Computadora, tablet y celular | Día, semana, mes, lista y formularios comprobados; teclado, Axe y ausencia de desborde horizontal |

La regresión revisa seis tamaños: 1440×900, 1280×800, 1024×768, 768×1024, 390×844 y 360×800. Los flujos nuevos completos se comprobaron además en 1440×900, 768×1024 y 390×844. Las capturas en frontend/docs/verification/phase2 contienen únicamente datos de prueba; no se sembraron esas fichas o cuentas en la instalación local.

La migración local conservó la cuenta y el logo existentes. Se aplicaron V4 (pacientes y citas) y V5 (versionado de ediciones de contactos) sin modificar migraciones anteriores.

## Correcciones de la revisión
Se ajustó la sincronización de las pruebas con las búsquedas remotas de 250 ms. Se reforzó el contraste y se evitó interpolar texto y fondo al cambiar de vista. Se incluyeron las ediciones de contactos en la versión de la ficha para evitar sobrescrituras. Las comprobaciones correspondientes se volvieron a ejecutar.

## Comprobación externa de WhatsApp
Pendiente de accesos externos. No se ha proporcionado cuenta/token de Twilio, número Sandbox, participante autorizado ni dirección HTTPS pública para recibir webhooks. No se ejecutaron envíos ni recepción reales. Esta dependencia se conserva para la fase 6 y no impide continuar las fases clínicas y financieras. A22 sigue pendiente; esta fase no implementa ni demuestra reserva mediante IA.

El agente se conectará a los servicios centrales de disponibilidad y operaciones de agenda, incorporando su confirmación, origen y restricciones en la fase 6. No se añade un chatbot, n8n ni una simulación como evidencia.

## Cobertura del alcance
| Criterio | Cobertura de esta fase |
|---|---|
| A03 | Configuración de profesionales y agenda comprobadas |
| A04 | Ficha administrativa y citas; clínica y finanzas siguen pendientes |
| A05 | Contactos compartidos y menores en el flujo manual; integración por WhatsApp en fase 7 |
| A12 | Duración y disponibilidad de agenda manual cubiertas |
| A13 | Duración anterior conservada; presupuestos y cargos se completan en fase 4 |
| A14 | Concurrencia manual y protección en base de datos; agente en fase 6 |
| A24 | Reintentos de reserva manual sin duplicación; otros flujos críticos en sus fases |
| A25 | Reprogramación y cancelación manuales; flujo de IA en fase 7 |
| A29 | Permisos y auditoría incorporados a pacientes y citas |
| A31 | Diseño, teclado y adaptación de las funciones de esta fase |
| A32 | Búsqueda, filtros y páginas remotos; calendario acotado y binarios a demanda |

La aceptación integral continúa en las fases previstas. Este cierre no presenta como completos los criterios que dependen de clínica, finanzas o WhatsApp.

## Recorrido de revisión
1. Acceder con la cuenta administradora existente. En Configuración, crear categoría, servicios (uno de 60 minutos), usuarios odontólogos, profesionales con servicios asignados y jornadas para el día de prueba.
2. Registrar un adulto y dos menores con el mismo teléfono familiar. Identificar el contacto responsable de los menores; comprobar que buscar el teléfono devuelve sus fichas.
3. Elegir una fecha futura dentro de la jornada y reservar 60 minutos desde las 10:00. Comprobar el fin a las 11:00, el rechazo de un solapamiento y la admisión para otro profesional.
4. Consultar la cita, confirmar y revisar el historial. Los estados de asistencia requieren que haya llegado la hora de la cita.
5. Cambiar el catálogo a 30 minutos y comprobar que la cita previa mantiene 60. Reprogramar hacia una hora ocupada y comprobar que no cambia; después usar una hora libre.
6. Cancelar con motivo, revisar su movimiento y comprobar que el intervalo queda libre. Filtrar el historial por movimiento y consultar la auditoría como administrador.
7. Repetir la navegación desde celular usando Lista y los formularios adaptados.

La aplicación local queda disponible en http://127.0.0.1:5173, con backend en 8080. Las pruebas utilizan 5174 y 8081 y la base separada de pruebas. No se inicia trabajo de fase 3 hasta la revisión correspondiente.
