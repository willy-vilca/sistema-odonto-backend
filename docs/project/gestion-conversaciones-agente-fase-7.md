# Fase 7: conversaciones, cambios y recuperación del agente

Actualización: 08/10/2026. Plan 3.7; alcance funcional 1.9. Backend y frontend: rama `kapso`.

**Estado: implementación y validación interna completas; demostración real reanudada el 08/10.** Consulta propia y disponibilidad reales aprobadas. Se respetó la ausencia del participante, que ahora autorizó continuar. Tras las incidencias de cuota/rechazo se detectó un filtro incorrecto del nombre del paciente, corregido en v7.4 con Groq y cita conservada. La propuesta correcta, los cambios y demás casos todavía necesitan demostración real. Fase 6 conserva su cierre; fase 7 en validación y fase 8 pendiente. [Intercambios reales](validacion-real-agente-kapso-fase-7.md), [corrección de cuota y latencia](optimizacion-agente-groq-fase-7.md), [diagnóstico acumulado](diagnostico-groq-reserva-fase-7.md).

## Uso desde la aplicación

Verificación final de la corrección v7.3: 187 casos backend aprobados, cero fallos/errores/omitidos, empaquetado correcto. Incluye 43 casos Kapso/agente, 11 del cliente Groq y dos de etapas de herramientas. Groq real preparó la propuesta correcta en APP_TEST con conservación de cita, historial, finanzas y mensajes salientes. No se repitieron pruebas de interfaz porque no hubo cambios visuales; se conserva la evidencia previa de los tres tamaños. [Mediciones, límites y casos para retomar](diagnostico-groq-reserva-fase-7.md).

Corrección posterior v7.4: el nombre completo verificado no se utiliza como filtro de servicios en las citas propias; se informa la búsqueda aplicada y se diferencia una consulta sin coincidencias. 72 pruebas del agente aprobadas, con 44 Kapso/agente; Groq real preparó la propuesta correcta sin aplicarla. La consulta sigue paginada y limitada al paciente autorizado. [Incidencia real y nueva comprobación pendiente](validacion-real-agente-kapso-fase-7.md).

Seguimiento v7.5: propuesta y negación reales aprobadas, con cita original conservada. Se protegió el rechazo del código descartado para no tratarlo como conflicto de otra reserva; requiere propuesta pendiente y referencia correcta al recuperar un conflicto. Un cambio descartado responde sin inferencia. 75 casos distintos comprobados en el bloque de regresión y la repetición aislada del contador de preparación corregido; la prueba real del código continúa pendiente. [Evidencia y límites](validacion-real-agente-kapso-fase-7.md).

Seguimiento v7.6: el código descartado ya pasó la prueba real, pero la nueva propuesta falló por herramientas y cuota. La lectura propia obligatoria se ejecuta directamente en el flujo con la herramienta autorizada y bitácora, y queda persistida antes de otra inferencia. Se alinearon campos opcionales y presupuesto de salida 768. 79 pruebas aprobadas; Groq real preparó el resumen correcto en APP_TEST en 3,032 segundos y tres inferencias, sin reintentos o cambios/envíos. AUTO/generación 6, requiere mensaje nuevo. [Diagnóstico final y límites](recuperacion-herramientas-groq-fase-7.md); nueva propuesta real y demás casos aún pendientes.

Seguimiento v7.7: propuesta nueva, sí ambiguo y reprogramación efectiva comprobados realmente. A petición del usuario, las confirmaciones muestran solo los datos resultantes en líneas; las repeticiones leen el estado/horario actual con permisos, conservando resumen histórico y referencias. 83 pruebas del agente/formato aprobadas; idempotencia real y demás flujos pendientes. [Evidencia actualizada](validacion-real-agente-kapso-fase-7.md).

En **WhatsApp**, la bandeja permite buscar contacto, mensaje, paciente y resumen. Los filtros **Control** y **Solicitud** y la paginación se ejecutan en el servidor. Cada conversación muestra su modo, solicitud administrativa, responsable, paciente identificado y enlace al detalle e historial de la cita cuando existe, recuperados a demanda y con permisos.

En **Ver conversación → Control y solicitud**:

1. **Asumir conversación** pasa a atención humana y registra al usuario responsable. Indicar el motivo ayuda a recepción a entender la solicitud. Mientras permanece asumida, el agente no procesa ni responde a entradas nuevas. Los mensajes sí se almacenan y recepción puede enviar texto manual.
2. **Devolver al agente** permite atender mensajes nuevos. Las confirmaciones y respuestas anteriores suspendidas no se ejecutan al devolver el control. Pedir al paciente una instrucción nueva y comprobar un resumen actualizado.
3. **Cerrar conversación** detiene la automatización sin borrar mensajes ni citas. Para reabrirla, devolver el control al agente.
4. **Actualizar control**, **Actualizar mensajes** y **Actualizar agente** recuperan el estado vigente. Los cambios concurrentes de control rechazan un formulario desactualizado.

Las derivaciones automáticas aparecen como **Derivada a recepción**. Recepción debe asumir la conversación para continuar atendiendo al contacto. Un aviso de derivación puede salir una sola vez, pero la IA queda detenida después. Se conserva el mensaje administrativo, el motivo y la bitácora.

Antes de responder manualmente una gestión atendida por IA, usar **Asumir conversación**. El envío manual existente sigue disponible; escribir texto manual por sí solo no toma el control.

En **WhatsApp → Reglas del agente**, administración puede editar:

- Activación de la atención automática y jornadas por día, con inicio, fin y opción hasta medianoche. Sin jornadas, atiende todos los días a cualquier hora; la zona es la del consultorio. No se aceptan jornadas solapadas. Los turnos que cruzan medianoche se distribuyen en dos días.
- Anticipación mínima para reprogramación o cancelación, en minutos; inicialmente cero. Las citas pasadas, canceladas o en atención requieren gestión humana.
- Permitir reprogramación y permitir cancelación automáticas.
- Textos de derivación a recepción, consulta clínica, fuera de horario y fallo controlado.

Estos horarios determinan cuándo atiende el **agente**, no amplían las jornadas ni disponibilidad de los odontólogos. Una entrada fuera de horario deriva a recepción; no vuelve a procesarse silenciosamente al comenzar otra jornada.

## Identidad y teléfono compartido

El teléfono lo obtiene el servidor desde el webhook autenticado; el modelo no puede sustituirlo. Para consultar citas se solicita nombre completo y relación explícita: «soy / para mí» o «mi hijo / soy su responsable». La coincidencia ignora tildes y diferencias de mayúsculas. Solo permite la ficha activa asociada a ese contacto y, para un tutor, exige que la relación de responsable esté registrada.

Un teléfono puede tener varias fichas. No se devuelve automáticamente su lista de nombres: se pregunta **para quién** es la gestión. Una coincidencia ambigua requiere recepción. Una referencia temporal de cita solo sirve en su conversación, canal y paciente verificado; expira a los treinta minutos. La verificación administrativa dura como máximo veinticuatro horas y se invalida al devolver el control.

Una persona que no coincide con ninguna ficha del contacto puede preparar una reserva para un paciente provisional. La ficha se crea después de confirmar la reserva; para un hijo se registra el contacto como responsable. No se muestra ni se vincula una ficha ajena por compartir nombre. Cada nuevo paciente se elige explícitamente.

Esta verificación administrativa confirma la asociación de contacto y ficha para gestionar agenda. No acredita identidad clínica ni habilita diagnósticos, documentos, cuentas, pagos u otros datos sensibles. No se añadieron documentos de identidad, códigos externos ni un portal del paciente al alcance.

## Flujo de consulta y cambios

1. Interpretar la solicitud y verificar paciente/relación.
2. **consultar_mis_citas** consulta hasta cinco próximas citas activas por página, con búsqueda; devuelve referencias temporales y datos administrativos. Ante varias citas, pedir cuál corresponde.
3. Para reprogramar, consultar disponibilidad con la referencia de la cita. Se conserva su servicio y duración original y se excluye únicamente esa misma cita al calcular el nuevo intervalo.
4. Preparar la propuesta de reprogramación con paciente, servicio, profesional actual, fecha anterior, nuevo profesional/horario, duración y motivo; para cancelar, presentar la cita identificada y motivo. Preparar no modifica ni retiene horarios.
5. Esperar la entrega del resumen y una confirmación expresa: «Sí, confirmo el cambio», «Sí, confirmo la cancelación» o `CONFIRMO código`. Un «sí» aislado vuelve a pedir confirmación; una negación descarta propuestas pendientes. No se usa una confirmación de cancelación para crear una reserva.
6. Volver a comprobar control humano, horario/reglas del agente, identidad, vigencia, canal, versión de cita, disponibilidad y datos del profesional/servicio. Confirmar guarda el cambio, historial, auditoría y tarea de respuesta en una transacción.
7. Repetir la confirmación devuelve el resultado registrado. Si el nuevo horario se ocupó o recepción modificó la cita, no se aplica el cambio y se informa que la cita original se conserva. Cambiar el precio o duración del catálogo no altera la duración anterior de una cita reprogramada.

Las consultas clínicas, posibles urgencias, solicitudes financieras, reclamos y peticiones de una persona derivan a recepción; no se diagnostica ni se ejecutan operaciones de dinero. Las herramientas no aceptan órdenes para cambiar permisos, SQL, configuración ni fichas de otros contactos. Los problemas de identidad y excepciones no resolubles requieren intervención humana.

## Control, persistencia y recuperación

- El control incorpora una generación creciente. Se compara antes/después de llamar al modelo, dentro de cada herramienta, antes de confirmar y antes del envío. Asumir y devolver durante una llamada del modelo no revive esa acción anterior.
- Las citas confirmadas se conservan aunque falle el envío. **Reintentar envío** opera únicamente sobre la respuesta existente y no repite la reserva o el cambio. Los resultados de envío inciertos se revisan en Kapso antes de intentar otro.
- Las tareas pendientes permanecen en PostgreSQL. Una ejecución interrumpida pierde su concesión después de cinco minutos y puede retomarse, con hasta tres intentos; el orden por conversación y las claves de solicitud impiden duplicaciones.
- Los límites de tiempo, llamadas, salida y herramientas detienen una ejecución que no puede completarse. La limitación temporal de Groq puede reintentarse de forma acotada; un fallo definitivo registra el error y deriva. Las propuestas vencidas no se ejecutan.
- Una cita y la tarea de su respuesta se guardan juntas. Un fallo al guardar la tarea revierte la operación; un fallo posterior del proveedor deja la cita persistida y el envío verificable aparte.
- La intervención humana no puede retirar un mensaje que el proveedor ya aceptó ni deshacer una operación comprometida antes de asumir. La protección impide continuar las acciones pendientes después de registrar el cambio de control.
- Las entradas `APP_TEST` conservan su contexto administrativo separado y no envían WhatsApp. Una derivación de vista previa no pausa una conversación real.

## Bitácora y permisos

La bitácora muestra entrada, propuesta de creación/cambio, estado y respuesta, pasos de herramientas y resultados, errores, modelo/proveedor, versión del flujo (inicial `supervised-v7.0`, continuación optimizada `supervised-v7.1`, hora elegida protegida `supervised-v7.2`, diagnóstico y recuperación `supervised-v7.3`), tokens y resultado operacional. Los fallos nuevos registran cada intento, la espera solicitada y el estado HTTP/categorías permitidas cuando provienen del modelo. Las confirmaciones vinculan la misma cita y el historial conserva el actor **Agente IA**. No se guardan claves, cuerpos de errores del proveedor ni razonamientos internos del modelo.

Lectura: `WHATSAPP_READ`. Control humano: `AGENT_CONTROL_WRITE`, otorgado inicialmente a administración y recepción. Reglas: `SETTINGS_WRITE`. Texto manual y reintento de envío: `WHATSAPP_WRITE`. Pruebas/reintento de análisis: `AGENT_TEST_WRITE`. Toda escritura HTTP conserva autenticación, validación y CSRF; la agenda sigue usando sus reglas centrales.

La estructura permanece por capas. `AgentSupervisionController` delega; los repositorios consultan y bloquean PostgreSQL; servicios separados manejan identidad, cambios, control, respuestas y bandeja. `AppointmentService` reutiliza las reglas manuales para cambios internos del agente. No hay acceso HTTP que permita escoger actor u omitir confirmación.

## Verificaciones del 07/10/2026

| Comprobación | Resultado |
|---|---|
| Regresión completa de servidor | 163 pruebas, cero fallos, cero omitidas; incluye 30 casos Kapso/agente |
| Reprogramación y cancelación | Una aplicación por confirmación, duración anterior conservada, historial y referencias correctos; finanzas intactas |
| Conflictos, vencimiento y versión de cita | Horario ocupado y cambio manual conservan la cita vigente; propuesta expirada no se ejecuta |
| Contacto compartido y referencias | Dos hijos seleccionados explícitamente; rechazo de contacto/ref ajenos y consulta sin verificar |
| Control en curso | Asumir durante el modelo y devolver no permite continuar la acción ni enviar su respuesta; nuevas entradas pausadas |
| Permisos, CSRF, versiones y filtros | Rechazo desde servidor y paginación remota; reglas y control desde aplicación |
| Reinicio real de proceso local | Proceso temporal interrumpido con tarea PROCESSING; un proceso nuevo recuperó la misma tarea en intento 2, una finalización, cero duplicaciones/envíos |
| Interfaz | Catorce escenarios de navegador: seis nuevos de supervisión/reglas, tres de bitácora existente y cinco de pacientes/agenda/enlace al detalle; 1440, 768 y 390 px, teclado, accesibilidad y sin desbordamiento global |
| Calidad | Backend empaquetado, frontend compilado, lint y formato aprobados; capturas revisadas en `frontend/docs/verification/phase7` |
| Modelo real, vista previa | Groq consultó catálogo y verificó un contacto ficticio nuevo sin citas; una consulta de citas propias terminó correctamente, sin cita ni WhatsApp |
| Cuota del modelo | Dos consultas de vista previa recibieron RATE_LIMIT; errores persistidos sin mutaciones y posterior consulta completada |
| Instalación de demostración | Migraciones V20/V21 aplicadas; conservados 9 pacientes, 12 citas, 9 cargos y 11 movimientos de dinero |

La regresión destructiva y el reinicio utilizan **solo `sistema_odontologo_test`**, claves ficticias, modelo controlado y trabajadores externos desactivados. El reinicio usó dos procesos Java distintos y adelantó únicamente la edad de la concesión para comprobar recuperación sin esperar cinco minutos. Tarea: `6ff26956-bd48-40e7-8c07-3d4ad2f5ff3f`. Regresión automatizada equivalente: `staleProcessingLeaseRecoversWithoutDuplicateBookingAndKeepsAuditVersion`.

La vista previa real usa `APP_TEST`, teléfono/contacto ficticios creados para esta comprobación. Consulta completada: `9d23fcd4-2b31-46ec-8c03-9476b235462f`; resultado «No encontré próximas citas activas del paciente verificado». No acredita todavía interpretación real de reprogramación/cancelación ni entrega de sus respuestas por WhatsApp.

Antes de migrar se guardó respaldo PostgreSQL local: `backend/.runtime/backups/sistema_odontologo-phase7-20261007-201251.dump`; inventario legible verificado. SHA256: `ADCBE921CD16111C1750615E476862DC822AD5D0766C1FA670F2747499BA7B44`. No se restauró la instalación; la restauración integral permanece en fase 9.

Pendiente: [demostración real de fase 7](probar-cambios-agente-kapso-fase-7.md), confirmar con el participante respuestas, cambios y supervisión. A25/A26 permanecen en validación para su uso por WhatsApp; A23/A24/A27 cuentan con cobertura interna ampliada. La reserva real de fase 6/A22 sigue cumplida. No se alteraron credenciales, túnel ni webhook y no se compraron servicios.
