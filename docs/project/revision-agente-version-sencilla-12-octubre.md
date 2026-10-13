# Revisión del agente de WhatsApp en la versión sencilla

Fecha: 12/10/2026. Rama: `version_sencilla`. Instalación: `sistema_odonto_mini`. Flujo corregido: `supervised-v7.11`.

## Hallazgos en los diez intercambios anteriores a la corrección

Se contrastaron entradas, argumentos de herramientas, resultados de la agenda, respuestas y control de la conversación. Las ejecuciones que dieron respuestas incorrectas terminaron `COMPLETED`, sin errores de cuota: estas incidencias no se atribuyen a límites de Groq ni al saldo de Kapso.

| Hora de recepción en Lima | Solicitud o resultado | Evaluación |
|---|---|---|
| 19:07 | «mañana a las 10:00» | Incorrecto: después de un argumento de fecha rechazado, el modelo consultó miércoles 14 en vez de martes 13, cambió Blanqueamiento por Limpieza y envió el nombre del profesional con espacios no separables. La búsqueda no encontró al profesional y la respuesta anunció falta de disponibilidad. Ejecución `a421e6cb-b399-4b71-b343-9995dc0ea756`. |
| 19:06 | Nombre del paciente y próximo sábado | La agenda consultó correctamente sábado 17 y no encontró horarios, pero el texto presentó un encabezado de horarios disponibles junto a la ausencia de opciones. Ejecución `af683c01-812b-4065-b1d3-57750adf06a3`. |
| 19:05 | Disponibilidad del próximo sábado | Solicitó nombre completo. No reservó; se aclara que una consulta de disponibilidad puede preceder a la identificación requerida para reservar. |
| 19:04 | Entrada durante atención humana | Se agrupó/omitió por control humano. No fue un fallo del modelo. |
| 19:00 | Reserva el próximo sábado | Derivación injustificada: declaró no disponer de herramienta de horarios sin completar primero la consulta del catálogo que habilita esa herramienta. Ejecución `72760c5d-e651-40a1-b3e3-45cc4ce2c837`. |
| 18:58 | «quiero reservar una cita» | Arrastró 20:00 de una reserva anterior sin que el paciente indicara una hora nueva. No creó cita, pero la respuesta podía confundir. Ejecución `d2794c75-ea0e-40db-9182-01eaa2d4bcb2`. |
| 18:57 | Precio de Blanqueamiento | Correcto: catálogo real de esta instalación, PEN 200 y 60 minutos. |
| 18:57 | Saludo | Respuesta de saludo, sin acciones de reserva. |
| 12:57 | Confirmación expresa | Correcto: una cita confirmada de Javier para Limpieza, lunes 12/10 de 20:00 a 20:30. Se conserva. |
| 12:56 | Propuesta anterior de Limpieza | Resumen de la cita posteriormente confirmada. No se reutiliza como una nueva elección del paciente. |

La única agenda activa es la de Willy Vilca Huaytalla: lunes y martes a viernes configurados, sin jornadas de sábado/domingo. El martes 13 a las 10:00 está libre; el sábado 17 realmente está cerrado. No se añadieron horarios para modificar esos resultados.

## Correcciones

- Resolver hoy, mañana, pasado mañana, días de semana y fechas explícitas con la fecha local de recepción. Una fecha inequívoca del paciente prevalece sobre argumentos contradictorios del modelo. «Por la mañana» no significa «mañana» como fecha; fechas incompatibles requieren aclaración.
- Normalizar espacios Unicode del profesional y de los argumentos textuales antes de buscar, sin relajar la selección del profesional ni sus servicios habilitados.
- Usar horas explícitas del paciente, incluidas formas abreviadas como «a las 10» y «mañana 9». Recuperar una elección reciente durante el mismo flujo cuando el siguiente mensaje aporta solo el nombre; no reutilizar la hora de una operación ya completada.
- Recuperar únicamente un servicio que el paciente nombró en una consulta reciente respaldada por catálogo. Consultarlo nuevamente para obtener precio, duración e identificador vigentes. No tratar como elección del paciente una suposición anterior del modelo. Búsquedas acotadas a doce registros y treinta minutos, con separación por canal y generación de control, después de la última reserva/cambio/cancelación completados.
- Aclarar las instrucciones de datos faltantes y orden de herramientas. Rechazar la falsa derivación por herramienta inexistente cuando aún debe completarse una consulta disponible; conservar derivaciones reales, límites de tiempo y protección ante fallos.
- Distinguir profesional no encontrado de un día sin intervalos libres. Mostrar una sola respuesta clara de falta de horarios con fecha legible, sin encabezados contradictorios.
- Corregir el contexto de continuidad para respetar la relación verificada SELF/GUARDIAN, sin describir a todo paciente como un hijo.

Se conservan las reglas de disponibilidad, duración, confirmación expresa, idempotencia, control humano, identidad y permisos. No se añaden herramientas clínicas o financieras ni se cambia el proveedor/modelo. Los registros anteriores permanecen en la bitácora.

## Comprobaciones

La regresión específica del agente aprobó 86 pruebas, incluidos fecha incorrecta del modelo, nombre con espacio no separable, hora antigua, continuidad de servicio y elección de hora, derivación falsa, disponibilidad, confirmaciones, contactos compartidos, aislamiento APP_TEST/WhatsApp, concurrencia y recuperación. La regresión integral del backend aprobó 269 pruebas, sin fallos, errores ni omitidos, incluyendo clínica, agenda, tratamientos, finanzas, reportes, PDF y ambos conectores. Los proveedores están simulados en las pruebas y solo se limpia `sistema_odonto_mini_test`. Empaquetado aprobado; frontend sin cambios y disponible, backend actualizado con salud UP.

También se efectuaron tres consultas APP_TEST con Groq real, sin confirmar reservas ni generar envíos de WhatsApp:

| Consulta | Resultado | Tiempo de procesamiento | Ejecución |
|---|---|---:|---|
| Precio/duración de Blanqueamiento | PEN 200 y 60 minutos | 2,034 s | `fc1dbcf3-e143-4cda-9dde-1e3d8a12ffac` |
| Ese servicio mañana a las 10:00 con Willy | Martes 13/10/2026, 10:00–11:00, Blanqueamiento | 1,201 s | `38fbb867-da1e-474f-86a5-71bddb31d3ff` |
| Próximo sábado | Sin horarios para sábado 17/10/2026, sin contradicción | 0,934 s | `0aa5f1e2-5f11-44bd-a2df-c4cacea174a9` |

Estas medidas son del procesamiento de la vista previa; no garantizan latencia de red, entrega WhatsApp ni ausencia de límites futuros del proveedor. Huellas de pacientes, citas, historiales, cargos, movimientos, reglas y control real, además del contador de envíos, permanecieron iguales antes/después de las tres consultas. La evidencia local está en `.runtime/agent-live-review-groq-evidence.json`, sin credenciales ni razonamientos internos.

## Confirmación real del usuario

Después de actualizar el backend, el participante envió:

> Solo quiero consultar disponibilidad de blanqueamiento con Willy Vilca Huaytalla para mañana a las 10:00. No reserves ninguna cita

Recibió los horarios del martes 13/10/2026 y el intervalo 10:00–11:00 para Blanqueamiento con Willy. La ejecución `6fdd576b-3605-40a6-99da-d91096b47f15` está `COMPLETED`, un intento, sin error, flujo v7.11. Catálogo y disponibilidad son `OK`, fecha 13/10, hora 10:00, profesional encontrado y preferencia disponible. La respuesta está `READ` con un solo intento de entrega y el usuario confirmó su recepción.

Se mantienen 2 pacientes, 5 citas, 8 cargos y 7 movimientos monetarios. La cita original `1608b785-04c6-4894-a280-6329b5e6c8b7` conserva estado `CONFIRMED`, versión 0 y horario lunes 12/10 de 20:00–20:30. Control real `AUTO`, generación 5. No se repitieron confirmaciones anteriores ni se creó una cita para comprobar esta consulta.

Backend actualizado en 8080; frontend conservado en 5173. Se mantienen receptor/ngrok, configuraciones privadas y main. No hubo compras ni cambio de claves. Fase 9 y recordatorios siguen pendientes.
