# WhatsApp: interfaz de atención y supervisión

09/10/2026. Ajuste solicitado por el usuario después de cerrar las fases 6 y 7. Se trabaja en main en ambos repositorios. Se conservan Kapso, Groq/openai/gpt-oss-20b y el flujo supervised-v7.10. Este trabajo mejora la presentación y la atención manual; no inicia la fase 8.

## Uso cotidiano

1. Abre **WhatsApp**. Busca un contacto o filtra por **Atención** y **Solicitud**. La bandeja muestra el contacto, un resumen del último mensaje, el estado y la actividad; las consultas siguen paginadas en el servidor.
2. Pulsa **Ver conversación**. Los mensajes del paciente aparecen a la izquierda y los del consultorio a la derecha. Las respuestas automáticas se identifican como **Asistente** y las manuales como **Recepción**. Los más recientes están abajo; el encabezado y el cuadro de respuesta permanecen visibles.
3. Sube por el chat para consultar mensajes antiguos. Se recuperan por grupos de hasta 30, conservando la posición de lectura. Si llegan mensajes mientras lees arriba, aparece **Nuevos mensajes** para ir al final. Los estados de los envíos recientes se actualizan automáticamente.
4. Para responder personalmente, pulsa **Asumir conversación**. Se pausa el asistente y se habilita **Mensaje de respuesta**. En computadora puedes enviar con Enter y crear una nueva línea con Mayús + Enter; en pantalla táctil utiliza el botón de envío. Se admiten hasta 1600 caracteres.
5. Al terminar, pulsa **Devolver al asistente** para que atienda el próximo mensaje del paciente, o **Cerrar conversación** para finalizar la atención. Devolver el control no ejecuta mensajes antiguos ni propuestas pendientes. El borrador se conserva al cambiar de control, consultar información o fallar una solicitud de envío.

Asumir, devolver y cerrar registran el usuario y un motivo administrativo de la acción sin exigir un formulario adicional. Las operaciones siguen validando la generación del control en el servidor. Con atención automática habilitada, una respuesta manual nueva requiere control humano también en el backend; una pantalla desactualizada no puede saltarse esta regla. Repetir una solicitud ya aceptada conserva su referencia y no crea otro mensaje.

## Información y opciones secundarias

| Opción | Contenido |
|---|---|
| **Buscar mensajes**, icono de lupa en el chat | Búsqueda textual, recibidos/enviados y estado. Filtros y cursores ejecutados en PostgreSQL. Los resultados son una consulta estable; **Actualizar mensajes** vuelve a consultarlos. Cerrar la búsqueda recupera el chat habitual. |
| **Información de la conversación**, icono de información | Contacto, responsable, paciente, solicitud, motivo de atención, resumen administrativo y enlace a la cita cuando existe. |
| **Historial del asistente**, icono de historial | Ejecuciones, propuestas, resultados y acceso a la bitácora. Conserva paginación y filtros, consulta de herramientas y reintento de la respuesta existente según permisos. No se muestra dentro del chat habitual. |
| **Información del mensaje**, icono en cada burbuja | Estado, fecha, intentos y detalle de un fallo. La referencia del proveedor queda en un apartado desplegable. |
| **Información del servicio**, botón de la bandeja | Configuración de WhatsApp y del modelo. Las direcciones técnicas se reservan a quienes administran la configuración; las claves no se devuelven al navegador. El simulador aparece únicamente con permiso de prueba. |
| **Ajustes del asistente**, botón de la bandeja | Horarios, mensajes administrativos y reglas de cambios. Disponible según el permiso de configuración. |

El reloj señala un mensaje pendiente o en envío; una marca señala enviado; dos marcas, entregado o leído (azules al leerse). Un fallo o resultado incierto conserva un aviso visible y su información. Guardar un mensaje no se presenta como entrega confirmada. Se mantiene la ventana de respuesta de WhatsApp y la comprobación de participantes autorizados.

El simulador continúa separado del canal real: sus entradas APP_TEST están excluidas del chat de WhatsApp y se consultan desde el historial del asistente. La bitácora y sus referencias no se borran.

## Consulta del historial

Se añade `GET /api/v1/whatsapp/conversations/{id}/timeline`, protegido por WHATSAPP_READ. Admite `before` o `after`, tamaño de 1 a 50 (30 por defecto), búsqueda literal, dirección y estado validados. Reutiliza las referencias persistidas y su secuencia de recepción, con el índice existente por conversación; no requiere migración. Devuelve mensajes en orden ascendente y un cursor para recuperar anteriores o nuevos.

La conversación debe pertenecer al número y al proveedor activo. La consulta sirve para Kapso y para la implementación anterior de Twilio. No descarga todas las filas: React conserva solamente los grupos que se consultaron al leer el chat. La búsqueda no se ejecuta sobre esa memoria. Las llegadas y los cambios de estado se combinan por identificador, evitando duplicados y reinicios de la pantalla.

## Presentación general

Se retiraron los avisos de fases, primera entrega, desarrollo y conexión de las pantallas habituales, la navegación y los pies de página. Inicio presenta herramientas y datos del consultorio según permisos, sin métricas inventadas ni la antigua tarjeta técnica. Se mantienen errores reales de carga y opciones para reintentar.

Reportes permanece pendiente de fase 8 y no aparece en la navegación hasta disponer de sus funciones. Su URL conserva una pantalla de opción no habilitada. Este ajuste no declara implementado un módulo futuro.

Los diálogos bloquean el desplazamiento de la página de fondo, conservan el foco y admiten Escape. Las notificaciones del chat aparecen arriba para no tapar el cuadro de respuesta. Computadora usa un diálogo amplio con altura acotada; celular ocupa la pantalla. Los controles táctiles conservan un área de al menos 44 px.

## Verificación

Regresión completa del servidor: **213 pruebas aprobadas**, cero fallos y errores, con empaquetado correcto. Incluye los cuatro casos nuevos de cursores, filtros literales/estados, límites y pertenencia al número, y envío humano con repetición idempotente. La cola, el agente, las reservas y las finanzas conservan sus comprobaciones anteriores.

Interfaz: **36 escenarios distintos aprobados en bloques**, con datos controlados: 29 de agente, navegación, agenda y atención humana; tres de chat manual/cursor y cuatro de compatibilidad con Twilio/permisos. Se revisaron 1440, 768 y 390 px, además de Inicio/navegación en 1280, 1024 y 360 px. Axe no detectó infracciones A/AA en las vistas revisadas; esta comprobación no certifica todo el producto. TypeScript, build, lint y formato aprobados. El build conserva el aviso de tamaño del paquete; la división de rutas no forma parte de este ajuste.

Se comprobaron: mensajes a ambos lados, carga anterior con conservación de posición, llegada nueva sin salto, búsqueda literal Unicode, actualización de entrega, borrador tras error y cambio de control, clave de envío estable, Enter/Mayús + Enter y composición de teclado, foco de diálogos anidados, pantalla completa móvil, controles táctiles, roles y ausencia de frases de desarrollo en las páginas habituales. Las capturas actuales están en `frontend/docs/verification/whatsapp-ui`; se conservan aparte las de fases anteriores.

Incidencias corregidas durante la revisión: el estado de entrega necesitaba un rol accesible para su icono; los ensayos de búsqueda debían esperar la respuesta remota antes de contar filas o abrir contactos; un selector de la compatibilidad anterior coincidía también con una opción de filtro. La última revisión de 29 escenarios aprobó 26; los tres restantes se repitieron junto con la comprobación de páginas (cuatro aprobados), después de sincronizar el ensayo con el filtro del servidor. No se presenta ese primer bloque como libre de fallos. Se verificó también la actualización de la información de un mensaje mientras su diálogo está abierto.

Las comprobaciones de interfaz usan únicamente sistema_odontologo_test, claves ficticias y trabajadores externos desactivados. No requieren mensajes nuevos del participante ni envían pruebas al Sandbox o al modelo.

Lectura de la instalación de demostración: 11 pacientes, 15 citas, nueve cargos, 11 movimientos; cero ejecuciones pendientes, conversación AUTO/generación 21 y política versión 3, habilitada y con jornadas vacías. Credenciales privadas conservadas e ignoradas por Git. La evidencia de cierre de las fases 6 y 7 se mantiene; esta revisión no repite ni sustituye las demostraciones reales por WhatsApp.
