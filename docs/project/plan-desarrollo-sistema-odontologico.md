# Plan de desarrollo de la primera entrega del sistema odontológico

Fecha: 5 de octubre de 2026. Versión del plan: 2.3. Alcance de referencia: versión 1.5 de [Alcance confirmado](propuesta-alcance-sistema-odontologico.md).

Estado: fases 0 a 4 revisadas y aprobadas por el usuario; fase 5 completada técnicamente y revisada inicialmente por el usuario; revisión manual general informada favorable de manera aproximada, pendiente de anotación individual. Fase 6 en desarrollo por partes: primero conexión y mensajes de WhatsApp, después agente y reservas. Las fases 7 a 9 permanecen pendientes. Evidencia y límites en [cierre de fase 0](cierre-fase-0.md), [cierre de fase 1](cierre-fase-1.md), [cierre de fase 2](cierre-fase-2.md), [cierre de fase 3](cierre-fase-3.md), [cierre de fase 4](cierre-fase-4.md) y [cierre de fase 5](cierre-fase-5.md).

Ajustes previos a la revisión general completados el 02/10/2026: consulta de detalle del servicio protegida, precio autocompletado y editable, terminología de tratamientos y avisos flotantes. 79 pruebas del servidor y 52 del navegador aprobadas, con revisión responsiva y recuperación de reprogramación. [Resultados y correcciones](ajustes-previos-fase-6.md).

Revisión general preparada el 02/10/2026, antes de fase 6: [guía secuencial de las fases 0 a 5](guia-pruebas-manuales-fases-0-a-5.md), [registro de resultados](registro-revision-manual.md) y archivos ficticios en datos-prueba. La guía contrasta las funciones actuales, reutiliza cinco pacientes e incluye variantes de clínica y finanzas, resultados esperados y comprobaciones por roles/tamaños. Su preparación no declara aprobada la ejecución manual. Las incidencias críticas que aparezcan se corregirán antes de avanzar con funciones dependientes.

Revisión del usuario el 05/10/2026: informa que realizó aproximadamente toda la guía y que las funciones probadas parecen correctas. No se asigna aprobación individual a los bloques pendientes de registrar. Se completan sus dos ajustes: resumen de tres servicios con consulta completa remota y mes con tres citas por día más lista paginada y detalle reutilizado. 18 pruebas del servidor y 17 del navegador aprobadas; compilación, lint, formato y revisión visual en tres tamaños aprobados. Validación y límites en [ajustes de servicios y calendario](ajustes-listas-agenda-odontologos.md). La integración completa de fase 6 permanece pendiente.

## 1. Objetivo y estrategia

Desarrollar toda la primera entrega mediante fases funcionales que se puedan utilizar, probar y ajustar antes de continuar con trabajo dependiente. Cada fase incluye interfaz, reglas del servidor, persistencia y validación: no se construye todo el backend para dejar la interfaz al final, ni se presentan pantallas sin comportamiento real como módulos terminados.

La secuencia prioriza configuración, pacientes y agenda; después atención clínica, tratamientos y gestión financiera; luego el agente de IA integrado a una base operativa estable; y finalmente reportes, recordatorios y preparación de la entrega. Se prepara temprano el acceso de prueba a WhatsApp para reducir el riesgo de descubrir problemas de conexión al implementar el agente.

La arquitectura vigente es React + Vite + TypeScript + TailwindCSS, Spring Boot con Java 21 y Maven, y PostgreSQL, con imágenes y PDF dentro de la misma base, instalación independiente por consultorio y una sola sede. Uno o varios odontólogos se configuran desde la aplicación. La disponibilidad depende de cada odontólogo y de la duración del servicio, sin gestión separada de sillones.

Aplicar el prompt maestro adoptado por el usuario y conservar commits de los avances en los repositorios independientes backend y frontend. El backend se organiza por módulos y capas (modelo, repositorio, servicio, DTO, controlador); el frontend separa composición, funciones, HTTP, estado y componentes reutilizables. Todas las tablas y listas de datos requieren paginación, filtros y búsqueda textual en el servidor, con tamaño máximo y ordenación autorizada; React solicita solo la página actual. La agenda consulta intervalos acotados y su alternativa de lista se pagina. Los menús y textos estáticos de navegación no son listados de datos de negocio.

El agente se desarrolla dentro del backend, con un modelo externo, contexto persistente, herramientas limitadas y bitácora. La conexión inicial es Twilio Sandbox, según la decisión técnica del alcance. La confirmación del paciente produce una reserva automática. No se añade n8n ni un editor visual de automatizaciones.

## 2. Método de trabajo y cierre de fases

Cada fase sigue este ciclo:

1. Revisar el alcance vigente y los resultados de las fases de las que depende.
2. Implementar un conjunto pequeño de funciones relacionadas de extremo a extremo.
3. Ejecutar las comprobaciones funcionales, de permisos e integridad que correspondan y revisar la interfaz en computadora, tablet y celular.
4. Corregir los errores encontrados y volver a comprobar las funciones afectadas.
5. Preparar una demostración reproducible y registrar el resultado, las limitaciones y los ajustes de la revisión.
6. Cerrar la fase cuando sus condiciones de salida se cumplan y actualizar el seguimiento. Los comentarios que cambien un resultado ya implementado se incorporan antes de dar por terminada la revisión correspondiente.

No se avanza con funciones dependientes de una regla crítica que sigue fallando. Una dependencia externa pendiente se registra con precisión y se pueden continuar tareas independientes, pero una simulación no permite cerrar un criterio que exige WhatsApp real.

Las comprobaciones serán proporcionales. Se automatizan principalmente reglas con riesgo de duplicación o pérdida de datos, dinero, concurrencia, permisos y recuperación. Los cambios visuales sencillos se revisan en el navegador; no requieren pruebas que solo repitan su implementación. No se repite una batería completa sin motivo después de pasar: se amplía cuando un cambio, fallo o duda lo justifique.

El reporte de cierre de cada fase debe conservar: funciones entregadas, escenarios comprobados, resultados, correcciones, pendientes no críticos y estado de los criterios de aceptación. Un criterio transversal puede estar parcialmente cubierto hasta que se conecten sus módulos; esa cobertura parcial no se presenta como aceptación final.

## 3. Diseño y calidad transversal

El diseño visual queda a criterio del agente y debe resultar elegante, profesional y atractivo, con muy buena experiencia de usuario. Se define desde la fase 0 y se mantiene durante el desarrollo, junto con la configuración de marca del consultorio.

Dirección inicial: interfaz clínica sobria, fondos claros, texto con buen contraste, acentos contenidos, separación visual por espacios y acciones principales fáciles de identificar. La paleta concreta y los componentes se eligen durante la fase 0, manteniendo los colores de estado diferenciados de la marca configurable. No se incorporan imágenes decorativas que dificulten la lectura de la información operativa.

Se construyen componentes consistentes para navegación, formularios, botones, tablas, filtros, paneles de detalle, avisos, estados vacíos, carga y error. Se preservan valores de formularios ante errores recuperables, se explica la validación junto al campo, se evita repetir información y se protegen las acciones sensibles de toques accidentales. Los estados de cita, tratamiento y pago se expresan con texto además de color.

Los resultados de acciones se muestran como notificaciones flotantes compartidas, también con un formulario abierto. Se cierran manualmente o automáticamente (5 segundos para éxito, 8 para error), pausando el tiempo al pasar el puntero por su botón de cierre o enfocarlo. Se mantienen las validaciones junto al campo y los fallos persistentes de carga o permisos. La interfaz utiliza «tratamiento» en presupuestos y «plan de tratamiento» al vincular una atención.

Prioridad de computadora: agenda amplia, tablas legibles y acceso cómodo a ficha, clínica y cuentas. En tablet se ajusta la distribución para interacción táctil; en celular se utilizan formularios y paneles apilados y vistas de agenda adaptadas. El odontograma puede utilizar navegación o desplazamiento contenido conservando su funcionalidad. Ninguna adaptación debe hacer inaccesibles los flujos incluidos.

Referencias iniciales de revisión: computadora de 1440 × 900 y 1280 × 800; tablet de 1024 × 768 y 768 × 1024; celular de 390 × 844 y 360 × 800. Se revisan también dimensiones intermedias y orientación. Estos tamaños son casos de prueba, no restricciones del producto. Se comprueban foco y navegación por teclado, etiquetas comprensibles, contraste, controles táctiles, texto largo y ausencia de desplazamiento horizontal de toda la página.

La seguridad, auditoría, fechas según zona horaria del consultorio, manejo de dinero, migraciones de datos y mensajes de error se incorporan en cada módulo. El servidor siempre valida las reglas y permisos; ocultar un botón en la interfaz no sustituye esa validación.

## 4. Secuencia de fases

| Fase | Resultado principal | Dependencia para comenzar |
|---|---|---|
| 0 | Base técnica y sistema visual | Alcance confirmado y este plan |
| 1 | Configuración, usuarios, odontólogos y servicios | Fase 0 |
| 2 | Pacientes y agenda manual confiable | Fase 1 |
| 3 | Atención clínica, odontograma y archivos | Fase 2 |
| 4 | Presupuestos, planes y cargos | Fase 3 |
| 5 | Pagos, cuotas, saldos, egresos y caja | Fase 4 |
| 6 | Reserva automática por WhatsApp con agente de IA | Fase 5 y acceso al entorno de mensajería y modelo |
| 7 | Reprogramación por IA, supervisión y recuperación | Fase 6 |
| 8 | Reportes, panel, exportaciones y recordatorios | Fase 7 |
| 9 | Validación integral y entrega reproducible | Fase 8 |

### Fase 0. Base técnica y sistema visual

**Objetivo.** Dejar una aplicación ejecutable y una base visual consistente para implementar los módulos siguientes.

**Trabajo incluido.**

- Organizar frontend React + Vite, backend Spring Boot y documentación; elegir versiones estables compatibles con el entorno durante esta fase.
- Preparar PostgreSQL, migraciones versionadas, configuración por entorno y conexión verificable entre interfaz y servidor. El usuario autorizó guardar las credenciales locales de demostración en application-local.properties; los secretos reales de producción, WhatsApp y modelo se mantienen fuera del código.
- Definir organización por módulos: configuración, seguridad, pacientes, agenda, clínica, documentos, tratamientos, finanzas, conversaciones, agente, reportes y auditoría, dentro de un backend modular.
- Preparar el acceso autenticado que se completará en la fase 1, estructura de respuestas, validaciones comunes, errores y registro técnico sin credenciales.
- Definir tipografía, paleta inicial, espaciado y componentes básicos; implementar la estructura responsiva de navegación y una pantalla base revisable.
- Documentar cómo iniciar y detener los servicios y preparar una instalación de demostración independiente.
- Identificar los accesos externos necesarios para fases posteriores: cuenta de mensajería, participantes de WhatsApp, credencial del modelo y punto de recepción HTTPS. Prepararlos cuando estén disponibles, sin condicionar los módulos locales a tener un cliente real.

**Validación.** Arranque desde una base vacía mediante migraciones; conexión real a PostgreSQL; comunicación frontend/backend; presentación de fallos de conexión; revisión de navegación, tipografía, carga y estados vacíos en los tres formatos de pantalla.

**Condición de salida.** Se puede iniciar el proyecto siguiendo instrucciones y revisar una estructura visual consistente. Los servicios se conectan realmente y la configuración sensible no se expone. Las pantallas base no se presentan como módulos de negocio completados.

**Criterios relacionados.** Preparación de A02 y A31; bases técnicas para A29 y A30.

### Fase 1. Configuración, usuarios, odontólogos y servicios

**Objetivo.** Convertir la base en una instalación configurable, con acceso por roles y reglas de operación editables.

**Trabajo incluido.**

- Inicio y cierre de sesión, usuarios activos/inactivos, roles de administrador, odontólogo, recepción y caja, y permisos en el servidor.
- Identidad de la única sede: nombre, logo almacenado en PostgreSQL, colores, dirección, contactos, moneda, zona horaria, textos y correlativos iniciales.
- Alta y configuración de uno o varios odontólogos, vinculados con los usuarios correspondientes y servicios que pueden realizar.
- Catálogo de servicios con categoría, precio, duración positiva en minutos y habilitación para reserva automática.
- Jornadas, descansos, días no laborables, ausencias, anticipación mínima y separación entre citas.
- Infraestructura de auditoría de operaciones sensibles, que cada módulo posterior utilizará desde su incorporación.
- Búsqueda, filtros y paginación en el servidor para los listados; formularios y navegación según permisos.

**Validación.** Cambiar identidad y horario desde la aplicación; configurar dos odontólogos con servicios distintos; rechazar duraciones y precios inválidos; verificar permisos solicitando directamente una operación restringida; comprobar que desactivar un servicio o profesional no borra su historial. Revisar formularios con teclado y pantallas táctiles.

**Condición de salida.** Un administrador configura el consultorio y los servicios sin modificar código. Cada rol accede a las funciones correspondientes y la auditoría registra los cambios relevantes.

**Criterios relacionados.** A01 y A03 para configuración básica; A29 para seguridad y auditoría iniciales; A31 en estas pantallas.

### Fase 2. Pacientes y agenda manual confiable

**Objetivo.** Permitir que recepción gestione pacientes y citas utilizando las reglas que después compartirá el agente.

**Trabajo incluido.**

- Ficha, código, búsqueda y prevención de duplicados de pacientes; contactos, responsables de menores y registros provisionales.
- Relación de un teléfono con varios pacientes y selección explícita del paciente de la cita.
- Calendario diario, semanal y mensual por odontólogo, con alternativa de lista utilizable en celular.
- Crear, consultar, confirmar, cambiar estado, reprogramar y cancelar citas, conservando su historial.
- Cálculo de fin a partir de inicio y duración, bloqueando el intervalo completo y respetando las reglas de disponibilidad.
- Conservar en cada cita la duración utilizada; modificar el catálogo no cambia silenciosamente reservas existentes.
- Impedir solapamientos por odontólogo en la base de datos y reservar de manera consistente ante solicitudes simultáneas.
- Crear funciones centrales de consulta de disponibilidad, creación, reprogramación y cancelación; serán las mismas que utilice el agente.
- Preparar una comprobación técnica temprana de recepción y envío de WhatsApp cuando los accesos externos estén disponibles. No incluye todavía la reserva mediante IA.

**Validación.** Reservar 60 minutos desde las 10:00; rechazar otra cita solapada para ese odontólogo y admitir una para otro; comprobar descansos, ausencias y servicios no habilitados; ejecutar reservas concurrentes; cambiar duración del catálogo conservando la cita anterior; reprogramar a un horario ocupado sin perder la reserva original; registrar dos hijos desde un contacto compartido. Revisar agenda y formularios en computadora, tablet y celular.

**Condición de salida.** Recepción puede completar el ciclo de una cita sin conflictos ni pérdida del historial. La disponibilidad se calcula con datos reales y queda lista para uso por herramientas del agente.

La comprobación externa temprana se documenta si puede ejecutarse. Si faltan accesos, se conserva como dependencia externa pendiente de la fase 6 y se pueden completar las fases clínicas y financieras. No se considera cumplida A22 con esta comprobación.

**Criterios relacionados.** A04 y A05 para registro y citas; A12, A14 y A25 para agenda manual; parte de A13 y A24; A31 en estas pantallas.

### Fase 3. Atención clínica, odontograma y archivos

**Objetivo.** Registrar atenciones y documentación del paciente conservando su evolución y protegiendo el acceso clínico.

**Trabajo incluido.**

- Antecedentes, alergias, medicamentos informados, anamnesis y plantillas clínicas básicas configurables.
- Atenciones vinculadas con paciente, cita y odontólogo; motivo, evolución, diagnósticos, procedimientos e indicaciones.
- Borradores, finalización y correcciones mediante versiones o anotaciones que preserven el original.
- Odontograma por pieza y superficie, dentición temporal/permanente e historial por fecha, con manejo táctil y representación legible.
- Imágenes JPG/JPEG, PNG, WebP y PDF, metadatos, categorías configurables y vínculos con paciente, atención, pieza o tratamiento cuando exista.
- Contenido binario en PostgreSQL separado de los metadatos, recuperación a demanda, validación de tipo/tamaño y límite inicial configurable de 20 MiB.
- Visualización, descarga autorizada y comparación de fotografías por fecha; conservación de originales.
- Consentimientos documentales con responsable, fecha y copia adjunta; sin firma electrónica.
- Auditoría de cambios clínicos y acceso a documentación sensible. Preparar el mecanismo de respaldo que se restaurará integralmente en fase 9.

**Validación.** Registrar y finalizar una atención, corregirla y consultar ambas versiones; consultar dos estados del odontograma; cargar y recuperar imágenes/PDF; rechazar archivos no admitidos o excesivos; denegar acceso no autorizado; verificar que una lista de documentos no trae todos los binarios. Revisar odontograma, visualizadores y formularios en los tres formatos.

**Condición de salida.** Se puede reconstruir la evolución clínica y recuperar los documentos desde PostgreSQL con permisos adecuados. La generación de cargos por procedimientos se conecta en la fase 4; no se presenta todavía el ciclo financiero como terminado.

**Criterios relacionados.** A06, A07, A08, A09 y A10; ampliación de A01 y A04; preparación de A15 y A30; A31 en estas pantallas.

### Fase 4. Presupuestos, planes y cargos

**Objetivo.** Conectar procedimientos y tratamientos con obligaciones de pago coherentes y explicables.

**Trabajo incluido.**

- Presupuestos y planes con procedimientos, piezas, sesiones, profesional, importes, condiciones y avance.
- Estados del plan: borrador, propuesto, aceptado, en curso, finalizado y cancelado; aceptación explícita y conservación de los valores acordados.
- Cargos de servicios individuales cuando se finaliza el servicio realizado.
- Cargos de planes al aceptarlos; aceptación y cargos guardados de manera consistente, sin duplicarse al repetir la operación.
- Asociación de las atenciones con conceptos ya incluidos en un plan para no generar una segunda deuda por sus sesiones.
- Procedimientos adicionales y ajustes de planes aceptados con motivo, responsable y conservación de movimientos previos.
- Historial de precios: cambiar el catálogo no modifica presupuestos aceptados ni cargos anteriores.
- Vista inicial de deuda por paciente, todavía separada del dinero cobrado, que se incorpora en fase 5.

**Validación.** Presentar un presupuesto sin generar deuda; aceptar un plan de S/ 1 200 y generar cargos por ese importe; completar sesiones sin aumentar la deuda; repetir la aceptación sin duplicarla; finalizar un servicio suelto y generar un único cargo; registrar un procedimiento adicional correctamente; modificar un precio del catálogo y conservar los importes aceptados.

**Condición de salida.** Toda deuda puede explicarse por un servicio realizado, un plan aceptado o un ajuste auditable. No hay cargos duplicados por sesiones, aceptación repetida o cambios de catálogo.

**Criterios relacionados.** A11, A15 y A16; completar precios de A13; ajustes de cargos de A18 y duplicaciones de A24; ampliación de A04; A31 en estas pantallas.

### Fase 5. Pagos, cuotas, saldos, egresos y caja

**Objetivo.** Completar la operación financiera del consultorio antes de incorporar la automatización de reservas.

**Trabajo incluido.**

- Registrar abonos con importe, fecha, medio, referencia, responsable y aplicación a uno o varios cargos.
- Anticipos pendientes de aplicar, cuotas con vencimiento y estado, saldos derivados de movimientos y estado de cuenta del paciente.
- Descuentos, anulaciones, devoluciones y reversión de pagos mediante operaciones auditables; cálculos monetarios exactos y conservación de referencias originales.
- Evitar doble registro por solicitudes repetidas y doble aplicación de un mismo anticipo o pago; las cuotas no generan nuevamente el total adeudado.
- Constancias internas de pago y estados de cuenta en PDF, con identidad, correlativo y detalle; datos históricos consistentes al cambiar la configuración.
- Adjuntar imágenes y PDF de sustento a ingresos y egresos usando el módulo de archivos.
- Egresos con categoría configurable, concepto, importe, medio y proveedor opcional.
- Apertura y cierre de caja, saldo esperado, saldo contado y diferencias; distinguir efectivo de otros medios.
- Conectar la ficha del paciente con cargos, pagos, cuotas y saldo. Aplicar permisos de caja y administración.

**Validación.** Sobre una deuda de S/ 1 200, registrar abonos de S/ 300 y S/ 200 y comprobar S/ 700 pendientes; aplicar un anticipo sin duplicar el ingreso; distribuir cuotas sin crear deuda adicional; rechazar operaciones incompatibles con el saldo disponible; revertir un pago conservando su historia; repetir una solicitud sin duplicarla; registrar un egreso con sustento, cerrar caja y descargar sus constancias. Revisar el flujo de cobro en los tres formatos de pantalla.

**Condición de salida.** Se puede cobrar, explicar y corregir una cuenta con trazabilidad. Los importes recibidos, las deudas y los egresos permanecen diferenciados. La base operativa clínica y financiera ya funciona mediante usuarios del sistema.

**Criterios relacionados.** A17, A19 y A20; completar ajustes financieros de A18 y evitar duplicaciones financieras de A24; completar la información financiera de A04; A31 en estas pantallas.

### Fase 6. Reserva automática por WhatsApp con agente de IA

**Secuencia acordada el 05/10/2026.** Primero implementar y comprobar recepción, persistencia, consulta y envío. El usuario ya creó la cuenta Twilio y confirmó mensaje real → sistema → plantilla real, con capturas de recepción y estado Leído. [Guía de conexión](conectar-whatsapp-prueba.md) y [estado del primer tramo](conexion-whatsapp-fase-6.md). Sigue [la habilitación y prueba de texto propio](probar-whatsapp-texto-personalizado.md): comprobar el Sandbox clásico y, si hace falta, PAYG con coste revisado por el usuario. No se ejecuta una compra ni se declara texto libre disponible por esta recomendación. Tras comprobar la respuesta personalizada real, se continúa con selección del modelo, herramientas y reserva. A22 sigue pendiente.

**Resultado de la alternativa gratuita.** El usuario recibió mensajes desde el Sandbox clásico, pero la salida TEXT fue rechazada con 21654 en el sistema; la petición Body sin ContentSid de la consola también dio 21654 según la corrección posterior del usuario; el 21655 corresponde a otra petición. La consulta de solo lectura a Twilio confirmó que la cuenta configurada sigue Trial y activa. Esta cuenta no tiene todavía texto propio habilitado; la siguiente dependencia es resolver esa habilitación, contemplando PAYG con coste aceptado por el usuario. No se considera resuelta cambiando el SID ni se repite como prueba gratuita aprobada.

**Plantilla clásica y pago.** El usuario aportó la petición de plantilla que falla con 21655. Su SID coincide con el Quickstart clásico y tiene formato válido; las consultas de Content API no permitieron verificar disponibilidad (401/20003), mientras Account API sigue autenticando. Primero comprobar el ejemplo permitido actual de Try out WhatsApp y resolver la compatibilidad con soporte antes de pagar. No atribuir todo error a ser Trial ni prometer que PAYG corrige una plantilla inválida. No se enviaron mensajes ni se modificó la cuenta durante estas consultas.

**Nueva prueba de control.** El usuario volvió a Try out WhatsApp y obtuvo aceptación de plantilla con su remitente temporal y un SID generado por ese entorno, sin variables: errorCode nulo, status=queued. Se confirma que el ejemplo permitido del trial funciona; esa respuesta no acredita entrega por sí sola ni habilita texto propio. La hipótesis de compatibilidad del ejemplo clásico se refuerza sin determinar propiedad o eliminación de su plantilla. [Datos y configuración para volver al entorno funcional](conexion-whatsapp-fase-6.md#retorno-exitoso-al-ejemplo-del-trial-nuevo). Continúan pendientes texto personalizado, agente y A22.

**Objetivo.** Completar una reserva real desde mensajes de texto de WhatsApp utilizando la agenda existente.

**Trabajo incluido.**

- Activar el entorno de WhatsApp de prueba, los participantes autorizados, las credenciales y el punto de recepción HTTPS; utilizar documentación oficial vigente al implementarlo.
- Elegir y conectar un modelo capaz de usar herramientas en español, con criterios de coste, tiempo de respuesta y tratamiento de datos; conservar configuración intercambiable.
- Conector de recepción y envío, validación de autenticidad, persistencia antes del procesamiento, diferenciación de mensajes y estados, y control de eventos repetidos.
- Contexto administrativo por conversación, solicitud en curso y agrupación ordenada de mensajes cuando sea necesario.
- Agente con herramientas de servicios, disponibilidad, contacto provisional y creación de cita, usando las reglas de fase 2.
- Interpretación de intención, preguntas por datos faltantes, fechas relativas según zona horaria y ofrecimiento de horarios devueltos por el sistema.
- Confirmación inequívoca de paciente, servicio, odontólogo, fecha y hora; validación final y creación automática de cita sin aprobación obligatoria de recepción.
- Registro consistente de la cita, referencia de solicitud y tarea de confirmación con reintentos; no volver a reservar si falla o se repite el envío.
- Bitácora inicial de mensajes, herramientas, resultados, errores y cita vinculada; vista básica de conversación en la aplicación.
- Límites del agente para no modificar finanzas ni acceder al expediente clínico; límites de tiempo e intentos para terminar de forma controlada si una herramienta falla.

**Validación.** Desde WhatsApp real de un participante, pedir una cita en español, proporcionar datos, elegir un horario y confirmar; verificar la cita en la agenda y su bitácora. Comprobar que una pregunta de precio o una negación no reserva, que un mensaje incompleto solicita información y que un horario ocupado produce alternativas. Comprobar en el servidor eventos repetidos y una reserva concurrente con recepción sin duplicaciones. Las comprobaciones de concurrencia y recuperación se ejecutan en la aplicación; no se realizan pruebas de carga sobre el Sandbox.

**Condición de salida.** Existe una demostración reproducible de mensaje real → agente → herramientas → confirmación del paciente → cita persistida → respuesta por WhatsApp. La bitácora permite verificar las acciones. No se cierra esta fase solo con respuestas generadas, una conversación simulada o una cita creada manualmente.

**Criterios relacionados.** A22 y A27; reserva básica de A23 y A24; verificación de A14 entre integración y recepción; A31 en estas pantallas.

### Fase 7. Gestión de conversaciones, cambios y recuperación del agente

**Objetivo.** Convertir la reserva inicial en un flujo supervisable que maneje cambios y fallos sin confundir al paciente.

**Trabajo incluido.**

- Consultar citas propias verificadas, reprogramar y cancelar con confirmación e historial, sin divulgar citas de otros pacientes.
- Confirmar para quién es una reserva cuando el contacto tiene varios pacientes o actúa como tutor.
- Completar la bandeja con estados de conversación, resumen administrativo, solicitud, paciente asociado y cita vinculada.
- Asumir y devolver el control a recepción, pausando acciones y respuestas de IA mientras una persona atiende. Volver a comprobar ese control antes de ejecutar una acción que estaba en curso.
- Derivar consultas clínicas, reclamos, identidad dudosa, excepciones o solicitudes no resolubles; utilizar mensajes administrativos configurables.
- Manejar confirmaciones tardías, solicitudes expiradas, cambios de intención, fallos del modelo, fallos de herramientas y de envío.
- Reanudar tareas persistidas tras reinicio, controlar orden por conversación y conservar las reservas aunque no se haya entregado la confirmación.
- Completar bitácora con versiones del modelo y flujo, consumo y resultado operacional, sin credenciales ni razonamientos internos del modelo.
- Configurar horarios del agente, textos y reglas administrativas de cambios dentro del alcance.

**Validación.** Reprogramar desde WhatsApp y conservar la cita original si el nuevo horario no puede reservarse; cancelar la cita correcta con confirmación; reservar para dos pacientes de un teléfono compartido; asumir una conversación mientras se procesa un mensaje y comprobar que no continúa una acción automática pendiente; provocar fallos controlados y reiniciar el servidor para comprobar recuperación sin duplicaciones. Intentar solicitar datos ajenos o cambiar las reglas mediante un mensaje y comprobar su rechazo. Revisar conversación y bitácora en celular y tablet.

**Condición de salida.** La automatización puede supervisarse e interrumpirse, los cambios conservan integridad y la recuperación de fallos no inventa resultados ni duplica reservas. El agente respeta sus límites clínicos y financieros.

**Criterios relacionados.** A05 y A25 en WhatsApp; A26; completar A23, A24 y A27 para el agente; ampliación de A01 con configuración de mensajes; A31 en estas pantallas.

### Fase 8. Reportes, panel, exportaciones y recordatorios

**Objetivo.** Completar las herramientas complementarias de consulta y seguimiento sobre módulos ya operativos.

**Trabajo incluido.**

- Panel por permisos: citas del día, espera, conversaciones pendientes, cuotas vencidas y cobros del periodo.
- Reportes por periodo de citas y ausencias, avance de tratamientos, servicios, cobros, cargos, egresos y saldos, con filtros por profesional cuando correspondan.
- Distinguir presupuestos, cargos emitidos, trabajo realizado y dinero recibido; mostrar cobros menos egresos como flujo neto de caja.
- Exportaciones autorizadas en formatos adecuados al reporte y acceso a las constancias PDF ya desarrolladas. Consultar, filtrar y paginar en el servidor, sin descargar toda la información para armar un listado.
- Completar la navegación integrada de la ficha del paciente entre clínica, documentos, tratamientos, agenda y cuenta, conservando los permisos.
- Programar recordatorios desde el servidor con zona horaria del consultorio, plantillas permitidas, autorización de contacto, estados de envío y recuperación de errores.
- Al cancelar o reprogramar una cita, invalidar el recordatorio anterior y preparar el correcto; evitar envíos duplicados y respetar la baja de mensajes.
- Revisar consistencia visual entre módulos y ajustar navegación y acceso a las tareas habituales usando resultados reales de las fases anteriores.

**Validación.** Comparar los reportes con un conjunto controlado de atenciones y movimientos; verificar totales y filtros, descargables y permisos. Enviar un recordatorio real admitido por el entorno de prueba; cancelar o reprogramar antes del envío y comprobar que no sale el aviso antiguo; registrar una baja y comprobar que se respeta. Completar los recorridos desde ficha, agenda y panel en los tres formatos de pantalla.

**Condición de salida.** Los reportes coinciden con los movimientos registrados, las vistas están integradas y los recordatorios funcionan con las condiciones del entorno autorizado. Todos los módulos del alcance están disponibles para validación integral.

**Criterios relacionados.** A21 y A28; completar integración de A01 y A04; confirmar coherencia de A19; A31 en estas pantallas.

### Fase 9. Validación integral y entrega reproducible

**Objetivo.** Comprobar el alcance completo y preparar una entrega que pueda instalarse y demostrarse con instrucciones claras.

**Trabajo incluido.**

- Recorrer los criterios A01–A32 y registrar para cada uno su evidencia, resultado y correcciones necesarias.
- Comprobar instalación desde base vacía y actualización mediante migraciones, configuración propia del consultorio y separación entre instalaciones independientes.
- Verificar permisos, auditoría y protección de archivos en el conjunto del sistema, incluida interacción entre roles.
- Respaldar y restaurar PostgreSQL con datos, imágenes, PDF y relaciones; comprobar archivos después de la restauración y conservar respaldos fuera de la base activa.
- Revisar estabilidad, consultas paginadas, cargas a demanda, tamaño de archivos y uso de recursos con datos representativos. Registrar mediciones y ajustar problemas observados, sin prometer capacidad para una infraestructura aún no definida.
- Revisar todas las pantallas y completar los flujos principales en computadora, tablet y celular; corregir los problemas visuales y de interacción restantes.
- Preparar configuración y datos de demostración, instrucciones técnicas, guía funcional y guion de demostración, sin incluir secretos ni datos reales de pacientes.
- Preparar los artefactos del frontend y backend y la configuración de ejecución; mantener el servidor accesible para demostrar WhatsApp real.

**Escenarios integrales obligatorios.**

1. Paciente nuevo contacta por WhatsApp, confirma cita, aparece en agenda, recibe atención, se registra un servicio individual, se genera un cargo, paga y recibe constancia.
2. Paciente acepta un plan, se genera la deuda, paga por cuotas y completa varias sesiones sin cargos duplicados; el estado de cuenta y los reportes coinciden.
3. Dos odontólogos atienden simultáneamente; dos solicitudes para el mismo odontólogo se resuelven sin solapamiento.
4. Un paciente reprograma o cancela por WhatsApp; se conserva el historial y se actualizan los recordatorios.
5. Falla un envío o se reinicia el servidor; la reserva y los movimientos sobreviven y los reintentos no duplican operaciones.
6. Se restaura una copia en un entorno de prueba y se recuperan la ficha, documentos, cuentas y relaciones verificables.

**Condición de salida.** Todos los criterios de aceptación están comprobados, las incidencias que impiden la entrega están resueltas, existe evidencia de agente con WhatsApp real y la instalación/demostración es reproducible. Los elementos excluidos no se presentan como faltantes de esta entrega.

**Criterios relacionados.** A30 y validación final de A01–A32.

## 5. Cobertura de los criterios del alcance

Las fases indicadas como cierre son las que completan la cobertura funcional del criterio; la fase 9 revisa todos nuevamente en los escenarios integrales. La calidad visual y la autorización se comprueban también en cada fase que agrega una pantalla o una operación.

| Criterio | Desarrollo principal | Cierre funcional antes de la revisión integral |
|---|---|---|
| A01 Personalización | 1, 3, 5 y 7 | 8: configuración e integración completas |
| A02 Instalación independiente | 0 y 1 | 9: instalación y separación verificadas |
| A03 Odontólogos configurables | 1 y 2 | 2: servicios y agenda por profesional |
| A04 Ficha integral | 2, 3, 4 y 5 | 8: navegación e información integradas |
| A05 Contactos compartidos y menores | 2 y 7 | 7: también desde WhatsApp |
| A06 Historia clínica versionada | 3 | 3 |
| A07 Odontograma e historial | 3 | 3 |
| A08 Consentimientos documentales | 3 | 3 |
| A09 Archivos dentro de PostgreSQL | 3 | 3 |
| A10 Protección y límites de archivos | 3 y 5 | 5: documentos clínicos y financieros |
| A11 Presupuestos y planes | 4 | 4 |
| A12 Duración y agenda | 2 | 2 |
| A13 Cambios de catálogo e historia | 2 y 4 | 4 |
| A14 Concurrencia de reservas | 2 y 6 | 6: interfaz y agente |
| A15 Servicio individual genera cargo | 4 | 4 |
| A16 Plan aceptado genera cargo | 4 | 4 |
| A17 Abonos, anticipos y cuotas | 5 | 5 |
| A18 Ajustes financieros | 4 y 5 | 5 |
| A19 Constancias internas | 5 | 5 |
| A20 Egresos y caja | 5 | 5 |
| A21 Panel y reportes | 8 | 8 |
| A22 Reserva real por IA | 6 | 6 |
| A23 Mensajes incompletos y negaciones | 6 y 7 | 7 |
| A24 Repeticiones y recuperación | 2, 4, 5, 6 y 7 | 7: todos los flujos críticos |
| A25 Reprogramación y cancelación | 2 y 7 | 7: manual y mediante IA |
| A26 Atención humana y límites | 1, 6 y 7 | 7 |
| A27 Bitácora verificable | 6 y 7 | 7 |
| A28 Recordatorios | 8 | 8 |
| A29 Usuarios, permisos y auditoría | 1 y todas las fases funcionales | 9: revisión conjunta |
| A30 Respaldo y restauración | Preparación en 0 y 3 | 9: restauración integral |
| A31 Diseño y adaptación responsiva | 0 y todas las fases funcionales | 9: todos los flujos y pantallas |
| A32 Paginación, filtros y búsqueda en el servidor | 1 y cada fase con listados de datos | 9: comprobar consultas y respuestas de todas las listas |

## 6. Seguimiento de implementación

Estados de trabajo: pendiente, en desarrollo, en validación, en ajustes y completada. Las dependencias externas se anotan aparte para no confundir una función implementada con una integración demostrada. Al actualizar una fase se registran fechas, resultados y evidencia en esta sección o en un documento de cierre enlazado.

| Fase | Estado inicial | Evidencia y observaciones |
|---|---|---|
| 0 | Completada | 30/09/2026: backend y frontend ejecutables; PostgreSQL real; 5 pruebas backend y 8 de navegador aprobadas; revisión visual en computadora, tablet y celular. [Evidencia](cierre-fase-0.md). Revisada y aprobada por el usuario |
| 1 | Completada | 30/09/2026: configuración, acceso, roles, equipo, catálogo, horarios y auditoría con PostgreSQL real; 21 pruebas backend y 17 de navegador aprobadas; revisión de formularios y listas en seis tamaños. [Evidencia](cierre-fase-1.md). Revisada y aprobada por el usuario |
| 2 | Completada | 01/10/2026: fichas, contactos compartidos, responsables y agenda manual con disponibilidad, concurrencia, duración conservada e historial; 32 pruebas backend y 22 escenarios de navegador aprobados. [Evidencia](cierre-fase-2.md). Revisada y aprobada por el usuario; WhatsApp externo pendiente de accesos de fase 6 |
| 3 | Completada | 01/10/2026: atenciones versionadas, antecedentes, odontograma, originales PostgreSQL, consentimientos, configuración, permisos y auditoría; 41 pruebas backend y 30 escenarios distintos de navegador aprobados. Revisión visual en computadora, tablet y celular; migración local conserva datos. [Evidencia](cierre-fase-3.md). Revisada y aprobada por el usuario; restauración integral pendiente de fase 9 |
| 4 | Completada | 01/10/2026: presupuestos, aceptación explícita, sesiones, adicionales y cargos/ajustes inmutables con deuda explicable; 54 pruebas backend y 37 escenarios de navegador aprobados. Migración local y respaldo conservan datos; revisión en computadora, tablet y celular. [Evidencia](cierre-fase-4.md). Revisada y aprobada por el usuario; pagos y caja en fase 5 |
| 5 | Completada | 02/10/2026: pagos, anticipos, cuotas, saldos, correcciones, egresos, sustentos y caja con PDF históricos y zoom. 77 pruebas backend y 48 escenarios de navegador aprobados, con 11 financieros también comprobados tras el ajuste del visor. Migraciones y respaldo conservan datos locales; revisión en computadora, tablet y celular. [Evidencia](cierre-fase-5.md). Revisada inicialmente por el usuario; revisión manual integral de fases 0 a 5 pendiente según la guía preparada. WhatsApp real pendiente de fase 6 y restauración integral de fase 9 |
| 6 | En desarrollo | 05/10/2026: conexión, persistencia, bandeja y envío implementados; 93 pruebas backend, 13 escenarios de navegador y una del receptor aprobados. El usuario confirmó recepción y envío de plantillas reales con capturas de Recibido/Leído. Sigue la prueba de texto propio; recomendación documentada de Sandbox clásico y PAYG si se requiere, sin compra ejecutada. Agente, reservas y A22 siguen pendientes. [Estado y evidencia](conexion-whatsapp-fase-6.md) |
| 7 | Pendiente | Depende de fase 6 |
| 8 | Pendiente | Depende de fase 7 |
| 9 | Pendiente | Depende de fase 8 |

## 7. Entregables y límites

Al finalizar habrá aplicación web funcional, backend, migraciones, configuración por entorno, instrucciones de instalación, guía de uso, datos de demostración, reporte de aceptación y evidencia verificable de WhatsApp con agente. Las cuentas y credenciales permanecen separadas de los archivos de entrega.

No se incorporan inventario, múltiples sedes, plataforma compartida, portal del paciente, notas de voz, lista de espera, citas recurrentes, facturación electrónica, firma electrónica, visores especializados, contabilidad formal ni ejecutable de escritorio. La implementación completa se evalúa contra el alcance acordado, no contra esas ampliaciones.

Este plan no fija fechas calendario ni costes sin conocer la dedicación y el entorno. El avance se determina por fases cerradas con evidencia. Si el usuario autoriza un cambio de requisitos, primero se actualizan el alcance, los criterios y su relación con las fases, conservando las decisiones vigentes como guía del proyecto.
