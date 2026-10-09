# Cierre de fase 7: gestión de conversaciones, cambios y recuperación

Fecha: 08/10/2026, America/Lima. Alcance funcional 1.9. Flujo final supervised-v7.10, Groq/openai/gpt-oss-20b y Kapso. Backend y frontend conservan la rama kapso.

**Estado: completada y validada.** Demostración real, restauración y regresión completa final aprobadas. Plan actualizado a 3.11; no se inicia fase 8 en este trabajo.

## Resultado funcional

El agente consulta citas del paciente verificado, propone y aplica reprogramación/cancelación tras confirmación expresa, conserva historial y rechaza conflictos sin perder la reserva original. Gestiona reservas para hijos distintos del mismo responsable, permite atención humana y texto manual, respeta cierre y horarios, y deriva las solicitudes que exceden sus permisos.

La demostración se realizó con mensajes reales enviados por el participante al Sandbox; sus respuestas se contrastaron con estados READ, herramientas, contexto, propuestas, citas, historial y política en PostgreSQL. [Evidencia de cada intercambio y corrección](validacion-real-agente-kapso-fase-7.md). Se conservaron los fallos detectados; no se presentan como aprobados antes de corregir y repetirlos.

| Flujo comprobado por WhatsApp o interfaz real | Resultado |
|---|---|
| Consulta propia y disponibilidad | Datos reales del paciente/profesional, consultas acotadas; sin cambios de reserva |
| Propuesta, negación y código descartado | La propuesta no altera agenda; negación y código descartado conservan la cita |
| Confirmación ambigua, expresa y repetición | «sí» no aplica; aceptación mueve la misma cita una vez; repetición muestra estado actual |
| Intervalo ocupado tras proponer | Ocupación manual temporal por servicio central; confirmación rechazada y original intacta; temporal cancelada con historial |
| Cancelación y repetición | La cita correcta se cancela una vez, sin borrar historial ni tocar dinero |
| Dos hijos de un teléfono | Lucía y Mateo, fichas/citas separadas, selección explícita y contacto guardian/payer compartido; pedir nombre del otro hijo no reutiliza al primero |
| Asumir y responder como recepción | Usuario auditado, mensaje entrante guardado PAUSED, cero inferencias; texto manual llegó READ |
| Devolver y recibir mensaje nuevo | Entradas antiguas retiradas sin respuesta/cambios; nueva verificación y lectura muestran solo Mateo |
| Datos ajenos e intento de cambiar reglas | Derivación sin consulta ajena ni cambio de política |
| Consulta clínica y reclamo | Mensajes configurados y pausa; sin receta, diagnóstico, expediente ni modificaciones |
| Cerrar conversación | Conserva entrada recibida y datos, detiene respuestas; devolución explícita para reabrir |
| Horario/texto del agente | Edición desde interfaz y mensaje personalizado exacto fuera de horario, sin modelo ni reserva |

## Correcciones incorporadas

- Lecturas y respuestas respaldadas por agenda; nombre del paciente no actúa como filtro de servicio. Fecha/hora elegidas se validan en el servidor.
- Continuación persistida para reintentos de Groq, instrucciones y consumo reducidos, categorías seguras de fallo y espera acotada. Cuotas por minuto se recuperaron también en intercambios reales; no se afirma agotamiento diario.
- Protección de código descartado y referencias: no convierte el rechazo en conflicto de otra reserva confirmada.
- Confirmaciones de cambios con datos resultantes claros; repeticiones consultan estado actual y preservan resumen histórico.
- Continuidad explícita del hijo verificado, con revalidación de relación/fuente/vigencia, sin confundirlo con su responsable.
- Identificadores internos se obtienen con herramientas, sin pedirlos al paciente ni derivar prematuramente por su ausencia.
- Consulta propia requiere verificar y leer citas: buscar una ficha no permite inventar cantidad/ambigüedad. Una consulta informativa termina con resultado real y una inferencia cuando basta.

Diagnósticos conservados: [cuota y latencia](optimizacion-agente-groq-fase-7.md), [errores y continuación](diagnostico-groq-reserva-fase-7.md), [orden de herramientas](recuperacion-herramientas-groq-fase-7.md), [responsables y referencias](identidad-responsables-agente-fase-7.md), [consulta propia](consulta-citas-propias-agente-fase-7.md).

## Verificaciones internas diferenciadas

Las comprobaciones de concurrencia, fallos de herramientas/modelo/envío, idempotencia, confirmaciones vencidas, permisos, referencias de otro contacto/canal y control cambiado durante una inferencia se ejecutaron en la aplicación/base protegida de pruebas. No se hicieron pruebas de carga sobre el Sandbox.

La recuperación tras interrupción se comprobó con dos procesos Java distintos, tarea persistida, intento posterior y una sola finalización, sin envío externo. Se adelantó únicamente la edad de la concesión en la base protegida; no se alteró el reloj de la instalación. Referencia de tarea `6ff26956-bd48-40e7-8c07-3d4ad2f5ff3f`. [Implementación y evidencia interna](gestion-conversaciones-agente-fase-7.md).

La toma de control con el modelo en curso tiene prueba interna determinista. La toma real del participante ocurrió con el agente en reposo; se comprobaron después recepción pausada, respuesta manual, devolución sin ejecutar entradas anteriores y reanudación. No se presentan los casos internos como mensajes reales de WhatsApp.

El último ajuste verificó 93 casos distintos del agente: 91 aprobados en bloque y dos repeticiones después de actualizar aserciones antiguas de dos inferencias a una. Después se ejecutó la regresión completa actual: **209 pruebas aprobadas en una única ejecución**, cero fallos, errores u omitidas. Incluye 57 Kapso/agente, cliente Groq/definiciones/confirmaciones, conector Kapso/Twilio, configuración, permisos, agenda, clínica, tratamientos y finanzas. Registro local ignorado `backend/.runtime/phase7-final-full-regression.log`. La base protegida de pruebas es independiente de los datos de la demostración; trabajadores/envíos externos desactivados en esas pruebas. Se conserva separada la regresión global anterior de 187 casos; no se suman conjuntos superpuestos.

Interfaz: catorce escenarios de navegador de supervisión/reglas, bitácora, pacientes/agenda y enlace de cita, en 1440, 768 y 390 px, con teclado/accesibilidad y sin desbordamiento global. Capturas en `frontend/docs/verification/phase7`. Los ajustes posteriores fueron del servidor; no se afirma que se repitió esa inspección visual sin haberlo hecho.

## Restauración y estado final de demostración

El usuario restauró desde la interfaz todos los campos funcionales originales: atención habilitada, jornadas vacías, anticipación de cambios 0, reprogramación/cancelación permitidas y los cuatro textos originales. Política versión 3, actualización auditada `349acf81-dcb3-486b-991d-720def6fc02c`, actor Willy Vilca Huaytalla, 23:16:26 Lima. Las versiones/auditorías nuevas son legítimas; no se fuerza la versión original.

Conversación AUTO/generación 21, sin usuario asignado; devolución auditada `58b4e130-87cd-4a98-8be6-51892ef6b156`, actor Willy, 23:16:54. Las secuencias 76 y 83 permanecen GROUPED con cero pasos/tokens y sin respuesta. No hay tareas QUEUED/PROCESSING pendientes. Backend salud UP, frontend HTTP 200, receptor/ngrok disponibles; credenciales privadas intactas, sin compras.

| Registro conservado | Estado final |
|---|---|
| Willy, cita de fase 6 `fc272d39-566d-4b65-a610-0302e86d50b5` | CANCELLED/versión 2; CREATED, RESCHEDULED y CANCELLED conservados |
| Temporal de conflicto `ec4a37c6-b4a3-47a2-b5c3-698dc9733612` | CANCELLED/versión 1; creada/cancelada por el servicio central, actor Sistema y motivos de prueba |
| Lucía, cita `5d766bea-1d54-4c6f-bf92-c31651e41d0e` | CONFIRMED/versión 0, martes 20/10/2026 09:00–10:00, Julia/Limpieza, un historial |
| Mateo, cita `7b103374-8707-4b3c-bc01-dd5130361227` | CONFIRMED/versión 0, martes 20/10/2026 10:00–11:00, Julia/Limpieza, un historial |

La instalación conserva 11 pacientes, 15 citas, 9 cargos y 11 movimientos financieros. Respecto al inicio de fase 7, se añadieron las dos fichas/citas de los hijos y la cita temporal ya cancelada. No se generó deuda por reservar ni se borraron datos para ocultar el ensayo. Ambos hijos comparten el teléfono autorizado como contacto responsable.

## Criterios y siguiente fase

A05, A23, A24, A25, A26 y A27 cuentan con cobertura funcional completada por los intercambios reales y las verificaciones internas descritas; A22 conserva el cierre real de fase 6. Se comprobó la ampliación de A01 en horarios/textos del agente y las funciones de permisos/auditoría, adaptación y consultas remotas de A29/A31/A32 de esta fase. La revisión integral del conjunto A01–A32 sigue en fase 9.

Groq Free puede limitar solicitudes; el sistema conserva avances y deriva de forma controlada al exceder sus límites. No se promete disponibilidad ilimitada ni que todo texto posible se haya ensayado. La interpretación y envío externo se comprobaron con este modelo/Sandbox; cambiar proveedor requiere sus pruebas correspondientes.

Fase 8 queda pendiente de instrucción del usuario: reportes/panel/exportaciones y recordatorios. No se incluyen ni se declaran terminadas funciones de esa fase. La restauración integral de respaldos permanece en fase 9.
