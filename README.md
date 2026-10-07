# OdontoCare — backend

Fases 0 a 5: configuración, acceso, pacientes, agenda, clínica, archivos, presupuestos, planes, cargos, cobros, cuotas, egresos y caja con historial. Fase 6 en desarrollo: conexión inicial de WhatsApp. Java 21, Spring Boot 4.1.1, Maven y PostgreSQL.

## Inicio local

Requisitos: Java 21, PostgreSQL y la base sistema_odontologo. El usuario local autorizado es postgres/admin; application-local.properties contiene esa configuración.

Desde backend:

~~~powershell
./mvnw.cmd spring-boot:run
~~~

Desde frontend, en otra terminal:

~~~powershell
npm.cmd ci
npm.cmd run dev
~~~

Abrir http://127.0.0.1:5173. Backend: http://127.0.0.1:8080. Detener con Ctrl+C. Flyway conserva los datos existentes; Hibernate valida el esquema. No editar migraciones aplicadas ni usar ddl-auto=update.

La instalación sin usuarios solicita crear una cuenta administradora propia. No se distribuye una contraseña administrativa predeterminada. En Configuración, completar identidad, categorías, servicios y usuarios con rol odontólogo; después vincular profesionales, asignar sus servicios y configurar jornadas.

## Acceso y permisos

Sesiones de servidor con cookie HttpOnly, SameSite Strict, CSRF y rotación de sesión al acceder. El perfil prod exige además cookie Secure y HTTPS. Las contraseñas se almacenan mediante BCrypt y nunca se devuelven por HTTP.

Administrador, odontólogo, recepción y caja son roles fijos con nombres y permisos configurables. Los roles operativos consultan configuración, profesionales, servicios y horarios. Recepción registra pacientes y gestiona citas; odontólogo y caja consultan fichas administrativas y agenda. El administrador conserva todos los permisos. El odontólogo dispone también de clínica y documentos; caja y recepción no tienen acceso clínico por defecto. Caja gestiona cobros y recepción consulta WhatsApp con los permisos correspondientes.

Administrar necesita consultar la función correspondiente. Administrar odontólogos necesita consultar servicios; administrar horarios necesita consultar profesionales. El administrador conserva acceso completo. Se impide desactivar la propia cuenta o retirar el último administrador activo. Cambiar contraseña, usuario, estado o roles invalida sesiones; los permisos de un rol se verifican en cada solicitud.

Para clientes HTTP: GET /api/v1/auth/csrf devuelve headerName/token y establece la cookie. POST /api/v1/auth/login recibe username/password como formulario, con cabecera CSRF. Recuperar otro token tras acceder o salir. Enviar cookie y cabecera en todas las escrituras. La autorización protege también las peticiones directas.

## API de fase 1

| Ruta | Funciones |
|---|---|
| /api/v1/auth/session, /csrf | Estado de acceso y protección CSRF |
| /api/v1/auth/setup | Creación exclusiva del primer administrador |
| /api/v1/auth/login, /logout | Inicio y cierre de sesión |
| /api/v1/system/installation, /logo | Identidad pública mínima y logo a demanda |
| /api/v1/settings, /settings/logo | Configuración por permisos |
| /api/v1/users, /roles | Usuarios y política de roles |
| /api/v1/categories, /services | Catálogo |
| /api/v1/dentists, /dentists/eligible-users | Profesionales y selector de cuentas |
| /api/v1/schedules/periods, /exceptions | Jornadas, descansos y bloqueos |
| /api/v1/audit-events | Auditoría por permiso |

Los listados aceptan page desde 0, size por defecto 20 y máximo 100, search, sort autorizado y direction asc/desc. Filtros adicionales tipados según recurso; ver controladores. Respuesta: items, page, size, totalElements y totalPages. PostgreSQL ejecuta búsqueda, filtros y paginación.

POST crea; PUT /{id} actualiza con version para detectar ediciones obsoletas. Usuarios, profesionales y catálogos se desactivan, sin rutas de borrado. El logo admite PNG/JPG verificado, 2 MiB y 4096 píxeles por lado; bytea en tabla separada y descarga explícita.

Precios no negativos con dos decimales; duración entre 1 y 1440 minutos. Descansos dentro de una jornada. Intervalos activos del mismo tipo no se solapan por profesional y día, incluso ante escrituras simultáneas. Ausencias vinculadas a un profesional; bloqueos parciales dentro de una misma fecha.

X-Request-ID identifica solicitudes. Los errores usan Problem Details y errors con field/message cuando corresponde, sin SQL, contraseñas ni trazas. La auditoría participa en la transacción del cambio y su tabla rechaza UPDATE/DELETE. El administrador de PostgreSQL conserva sus facultades propias de administración.

## Verificación aislada

Crear una base exclusiva sin datos reales:

~~~powershell
& 'C:/Program Files/PostgreSQL/18/bin/createdb.exe' -h localhost -U postgres sistema_odontologo_test
./mvnw.cmd -B -ntp verify
~~~

Las pruebas usan application-test.properties y comprueban current_database() antes de limpiar exclusivamente sistema_odontologo_test. No apuntar este perfil a datos reales. Informe: target/surefire-reports.

Para navegador, después de verify, desde frontend:

~~~powershell
./scripts/prepare-e2e.ps1
java -jar ../backend/target/odontocare-0.0.1-SNAPSHOT.jar --spring.profiles.active=test --spring.config.additional-location=file:../backend/src/test/resources/application-test.properties,file:../backend/src/test/resources/whatsapp-e2e.properties
~~~

En otra terminal de frontend: npm.cmd run test:e2e. Puertos 8081 y 5174. Contraseñas aleatorias únicamente en .runtime, ignorado por Git. La preparación verifica el nombre de la base antes de vaciarla. No ejecutar verify mientras se pruebe el navegador: comparten la base de pruebas.

En Windows, detener java -jar antes de reconstruir el ejecutable. El arranque spring-boot:run evita depender del archivo JAR durante desarrollo.

## Producción y siguientes fases

prod exige DB_URL, DB_USERNAME y DB_PASSWORD, con PORT y SERVER_ADDRESS opcionales; no hereda credenciales locales. Flyway V2 necesita autorización para instalar btree_gist. Preparar esa extensión con el administrador de PostgreSQL para una instalación productiva.

La fase 2 aplica las reglas a las citas, genera códigos de pacientes y conserva duración e historial. Presupuestos y constancias consumirán sus textos y correlativos en fases posteriores. La clínica está disponible desde fase 3, presupuestos y cargos desde fase 4 y pagos desde fase 5. La conexión inicial de WhatsApp se incorpora en fase 6; el agente y la reserva automática siguen pendientes. Despliegue final, respaldos y restauración: fase 9.

Consultar [arquitectura](docs/architecture.md) y [cierre de fase 4](docs/project/cierre-fase-4.md). Las guías maestras están en la raíz; docs/project conserva sus instantáneas exactas.

## Pacientes y agenda

Pacientes: GET/POST /api/v1/patients, GET/PUT /api/v1/patients/{id}. Citas: GET/POST /api/v1/appointments, GET /{id}, PUT /{id}/status y PUT /{id}/reschedule. El historial GET /{id}/history se pagina. /appointments/calendar limita el intervalo a 42 días y 1.000 citas; /appointments/availability consulta un día, devuelve horarios paginados y acepta appointmentId al reprogramar. Todos los endpoints requieren sus permisos y las escrituras requieren CSRF.

POST /appointments recibe paciente explícito, odontólogo, servicio (o motivo administrativo y duración), inicio local del consultorio, notas y requestKey UUID. Reintentar el mismo contenido con la misma clave devuelve la cita existente; cambiar el contenido con esa clave produce conflicto. Reprogramación y estado requieren version. Los estados terminales se conservan, no se eliminan citas.

AvailabilityService y AppointmentService concentran las reglas que utilizará el agente. La reserva manual no necesita bookableByAgent; sí exige servicio activo asignado. La prueba real de WhatsApp está pendiente de accesos externos, no se ha simulado como evidencia. Diseño detallado en [decisiones de fase 2](docs/project/diseno-agenda-fase-2.md).

## Atención clínica y documentos

CLINICAL_READ/WRITE protegen atenciones, antecedentes y odontograma. DOCUMENTS_READ/WRITE protegen archivos y consentimientos. CLINICAL_CONFIG_READ/WRITE protegen plantillas, categorías y política. Administrador recibe todos; odontólogo recibe clínica, documentos y consulta de configuración. Recepción/caja no acceden por defecto. Escritura clínica exige su profesional vinculado activo; el administrador puede actuar para el profesional seleccionado.

Atenciones: GET/POST /api/v1/clinical/encounters; GET/PUT /{id}; POST /{id}/finalize; POST /{id}/corrections con motivo obligatorio; GET /{id}/versions paginado. Borradores y correcciones requieren version; no se reescribe un registro finalizado. Cita opcional validada por paciente y profesional; finalizar la atención completa el ciclo de estados de su cita dentro de la misma transacción, una vez alcanzado su horario. La ficha provisional debe completarse para finalizar.

Estados: GET/POST /api/v1/clinical/states/BACKGROUND y ODONTOGRAM. Se envía previousId del último estado; bloqueo del paciente y secuencia explícita previenen pérdida de cambios y ordenan estados incluso si comparten fecha/hora. Se conservan actor y profesional. La numeración FDI admite 32 piezas permanentes y 20 temporales.

Archivos: GET/POST multipart /api/v1/documents. Partes metadata (JSON) y file. Listado por paciente con categoría, tipo, pieza, atención, fechas, búsqueda y paginación; no trae binarios. GET /{id}/content para abrir, o ?download=true para descargar. PDF: GET /{id}/preview?page=0 devuelve únicamente la página PNG solicitada y X-Document-Pages. Tanto el original como su visualización requieren permiso y registran acceso; no-store evita caché del expediente. Los originales BYTEA se conservan con SHA-256 y no se reemplazan por las vistas.

Consentimientos: GET/POST /documents/consents, copia del mismo paciente y responsable/relación/fecha. Configuración: /clinical/templates, /documents/categories y /documents/policy. Política de 20 MiB inicialmente, ajustable entre 1 y 60 MiB. Transporte limitado a 64 MiB. Formatos verificados por contenido: JPEG, PNG, WebP y PDF; imágenes hasta 40 millones de píxeles, PDF sin contraseña y hasta 1.000 páginas, sin acciones activas ni archivos incrustados.

Respaldo local: scripts/backup-local.ps1. Copias en .runtime/backups, ignoradas por Git. [Procedimiento](docs/project/respaldo-postgresql.md). La restauración integral y A30 se validan en fase 9. Los vínculos con planes/tratamientos y los cargos se incorporan en fase 4.

## Presupuestos, planes y cargos

Abrir Presupuestos y planes, seleccionar paciente y guardar un presupuesto con profesional, conceptos, precio, unidades, sesiones, piezas y condiciones. Presentarlo no genera deuda. Aceptarlo exige registrar la aceptación explícita y genera cargos en la misma transacción. El correlativo utiliza budget_prefix/budget_next_number de Configuración.

API: GET/POST /api/v1/plans; GET/PUT /plans/{id}; POST /plans/{id}/actions/{propose|accept|finish|cancel}; POST /plans/{id}/additional; GET /plans/items, /plans/items/{id}, /plans/{id}/history y /plans/{id}/sessions. Las escrituras usan requestKey UUID; edición y acciones requieren version. Misma clave con otro contenido: HTTP 409.

GET /api/v1/charges y /charges/summary requieren patientId. Los movimientos se filtran por planId, originalId, kind y currency; se buscan y paginan. POST /charges/{id}/adjustments agrega una variación con requestKey, amount y reason. Un importe negativo reduce deuda y uno positivo la aumenta, conservando el cargo original. No se admiten cargos netos negativos ni reducción de la parte ya realizada.

PLANS_READ/WRITE protegen acuerdos; FINANCES_READ protege deuda y FINANCES_ADJUST permite ajustes y liberación de deuda al cancelar. Administrador dispone de todos; recepción gestiona planes, odontólogo gestiona planes y consulta deuda, caja consulta planes y deuda. Solo el administrador recibe ajustes por defecto. Se pueden configurar desde Roles.

El procedimiento clínico admite planItemId y unitPrice opcionales. planItemId enlaza el concepto del mismo paciente y profesional, con servicio/pieza coincidentes; quantity representa sesiones realizadas. El presupuesto separa unidades facturadas de sesiones previstas. Los servicios individuales se cobran al finalizar, con precio acordado o vigente del catálogo; un procedimiento libre sin importe es sin honorarios. Las correcciones clínicas conservan cargos; los cambios económicos requieren ajustes explícitos.

Cancelar puede conservar deuda o, con permiso de ajustes, liberar proporcionalmente lo pendiente. Finalizar exige completar sesiones. Las tablas de cargos, operaciones y sesiones rechazan cambios o borrados; los acuerdos y conceptos aceptados conservan sus datos. La moneda permanece en cada documento/movimiento; los totales se muestran por moneda.

Las atenciones finalizadas antes de fase 4 no generan cargos retroactivos. La fase 5 incorpora pagos, cuotas, anticipos, egresos, devoluciones y caja, separados del registro de deuda. Evidencia en [cierre de fase 4](docs/project/cierre-fase-4.md) y [diseño de tratamientos](docs/project/diseno-tratamientos-fase-4.md).


## Cobros y caja · Fase 5

Finanzas reúne Cuenta del paciente, Egresos, Caja y Categorías. Seleccionar paciente permite consultar deuda generada, dinero recibido neto, dinero aplicado, saldo pendiente y anticipo disponible por moneda. Registrar abono conserva fecha, medio, referencia y responsable; las aplicaciones pueden cubrir uno o varios cargos. Sin aplicaciones, el dinero queda como anticipo. Aplicarlo no crea otro ingreso.

API bajo /api/v1/finance:

- GET /summary?patientId; GET /charges?patientId y /charges/{id}: saldos y cargos netos paginados. GET /movements con patientId, originalId, cashSessionId, kind, method, currency, from/to, búsqueda, página y orden validado.
- POST /payments; POST /payments/{id}/{apply|release|refund|reverse}. GET /applications?paymentId o chargeId conserva aplicaciones firmadas y motivo.
- POST /charges/{id}/{discount|void}. Descuento/anulación preservan el cargo original; los ajustes y cancelaciones también rechazan dejar deuda por debajo del dinero aplicado.
- POST /installments con chargeId e installments de amount/dueOn; GET /installments?patientId, chargeId, active, from/to, búsqueda y página. Cuotas distribuyen el cargo neto completo y no generan deuda. Un nuevo calendario deja histórico el anterior.
- GET /expense-categories; POST/PUT /expense-categories[/{id}]; POST /expenses; POST /expenses/{id}/reverse. Categorías tienen búsqueda, filtro activo y paginación; sus nombres históricos quedan en el movimiento.
- GET /cash/current, GET /cash; POST /cash para apertura, POST /cash/{id}/close; GET /cash/{id}/report descarga el arqueo PDF conservado.
- GET /documents con patientId, movementId, generated, búsqueda y página; POST multipart con metadata (movementId/description) y file; GET /documents/receipt/{paymentId}; POST /documents/statement; GET /documents/{id}/content y /preview?page=0. Metadatos sin BYTEA; binarios recuperados a demanda.

Todos los comandos financieros usan requestKey UUID estable. Correcciones exigen motivo. Reversión completa del pago vigente libera sus aplicaciones; devolución parcial exige liberar el dinero aplicado necesario. No se sobrescriben pagos/cargos. Efectivo requiere caja abierta, moneda coincidente y fecha actual; fondos insuficientes rechazan egresos/devoluciones. Otros medios admiten fecha anterior y se separan del arqueo de efectivo. No hay conversión de monedas.

Permisos: FINANCES_READ para cuentas y sustentos; PAYMENTS_WRITE para cobros/aplicaciones/cuotas; EXPENSES_WRITE para egresos; CASH_READ/WRITE para caja; FINANCE_CONFIG_WRITE para categorías; FINANCES_ADJUST para descuentos, liberaciones y correcciones. Administrador conserva todos; caja recibe consultas, pagos, egresos y caja, con correcciones reservadas inicialmente a administración. Roles editables. Sustentos financieros reutilizan la validación documental sin dar acceso clínico a caja.

PDF históricos incluyen identidad, logo, correlativo y detalle al emitirlos; se guardan en financial_content (BYTEA). El arqueo cerrado conserva también expected/counted/difference. La tipografía Noto Sans se distribuye con su licencia OFL en src/main/resources/fonts; origen: https://github.com/notofonts/noto-fonts.

Validación: 24 pruebas financieras en Phase5IntegrationTests, además de la regresión previa. Guía y resultados en [diseño financiero](docs/project/diseno-financiero-fase-5.md) y [cierre de fase 5](docs/project/cierre-fase-5.md). La integración real de WhatsApp sigue en fase 6; la restauración integral del respaldo, en fase 9.

## Ajustes previos a la revisión general

02/10/2026: GET /api/v1/services/{id} devuelve ServiceResponse con precio vigente y permiso SERVICES_READ; incluye consulta de referencias inactivas sin borrado de historial. La selección en presupuesto puede completar precio y descripción; el importe acordado sigue siendo editable y se conserva en el plan. Los mensajes de reglas de tratamientos utilizan la terminología de la interfaz. No hay cambios de esquema. Verificación completa: 79 pruebas aprobadas. Guías sincronizadas: alcance 1.3, plan 1.7 y docs/project/ajustes-previos-fase-6.md; WhatsApp continúa pendiente de fase 6.

## Revisión manual antes de fase 6

[Guía secuencial con datos de prueba](docs/project/guia-pruebas-manuales-fases-0-a-5.md) y [registro de resultados](docs/project/registro-revision-manual.md). Reutiliza cinco pacientes para recorrer las funciones implementadas de las fases 0 a 5, con saldos esperados y controles por rol/dispositivo. Archivos ficticios en docs/project/datos-prueba. El usuario informó el 05/10/2026 una ejecución aproximada favorable, pendiente de registrar por bloque; no se marca la integración de WhatsApp como terminada. Instantáneas vigentes: alcance 1.4 y plan 1.9.

## Consulta paginada de servicios asociados

05/10/2026: GET /api/v1/dentists/{id}/services permite consultar servicios asociados con page, size, search y active. Requiere DENTISTS_READ y devuelve solo id, name y active; no expone precios. Incluye referencias inactivas conservadas y valida tamaño y ordenación por nombre. Sin cambios de esquema. Phase1IntegrationTests: 18 pruebas aprobadas, incluido el permiso de lectura de odontólogos sin acceso al catálogo. [Detalles y revisión visual](docs/project/ajustes-listas-agenda-odontologos.md).

## WhatsApp · Fase 6, conexión inicial

Recepción autenticada con firma Twilio, conversaciones paginadas, mensajes de texto y multimedia marcada como no compatible, salida persistida e idempotente y estados de entrega. Administración y recepción reciben WHATSAPP_READ/WRITE. El primer tramo del conector se amplía con el prototipo del agente descrito abajo; la salida personalizada real sigue pendiente.

La conexión está deshabilitada por defecto. Seguir [la guía desde cero](docs/project/conectar-whatsapp-prueba.md): crear cuenta, autorizar teléfono, copiar config/whatsapp.example.properties a config/whatsapp.local.properties (ignorado), completar credenciales y publicar únicamente los webhooks mediante el receptor local en 8082. El trial nuevo usa TEMPLATE; TEXT requiere habilitación real de la cuenta. La configuración lista en pantalla no certifica la prueba externa.

Prueba del receptor: node --test --test-isolation=none scripts/whatsapp-webhook-gateway.test.mjs. La regresión del servidor incluye WhatsAppIntegrationTests en la base exclusiva de pruebas, sin contactar al proveedor. [Diseño y resultados](docs/project/conexion-whatsapp-fase-6.md). El usuario confirmó recepción y plantillas; texto propio y A22 siguen pendientes.

### Prueba externa y texto personalizado

05/10/2026: el usuario confirmó recepción de mensajes y envío de plantillas reales; sus capturas muestran Recibido/Leído y un intento de envío. Texto propio, agente y reserva continúan pendientes. El modo TEXT ya utiliza Body y mantiene ventana de 24 horas, cola y control de duplicados. No se cambió la conexión privada para darlo por habilitado. [Guía de Sandbox, pago por uso y prueba de texto](docs/project/probar-whatsapp-texto-personalizado.md). Plan vigente 2.1 y alcance 1.5.

## Agente Groq · prototipo de fase 6

Modelo openai/gpt-oss-20b mediante API; agente, cola, herramientas, propuestas y confirmación dentro de Spring Boot. Archivo privado config/ai.local.properties, ignorado por Git, importado por el perfil local. Ejemplo sin secretos: config/ai.example.properties. [Guía de configuración](docs/project/configurar-groq-agente.md) y [implementación y resultados](docs/project/prototipo-agente-groq-fase-6.md).

API bajo /api/v1/whatsapp: GET /agent/configuration; POST /agent/test-messages con teléfono E.164, contactName, body y requestKey UUID; GET /conversations/{id}/agent/runs y /agent/proposal; GET /agent/runs/{id} y /steps; POST /agent/runs/{id}/retry. Lecturas requieren WHATSAPP_READ; pruebas y reintentos requieren AGENT_TEST_WRITE, inicialmente para administración. Listados con filtros, búsqueda, paginación y orden permitidos; escrituras con sesión/CSRF.

El usuario autorizó probar con sistema_odontologo y los datos ficticios existentes. Confirmar CONFIRMO código crea una cita transaccional; APP_TEST produce origen AI_TEST. Las respuestas permanecen PREVIEW, sin enviarse por WhatsApp. Se verificó Groq real, una cita, repetición sin duplicación, precio, datos incompletos y negación. A22 y fase 6 permanecen abiertos.

Verificación interna: 107 casos backend aprobados; AgentIntegrationTests y GroqLanguageModelClientTests usan proveedores controlados y la base automática protegida existente. Para el E2E del agente: añadir --odontocare.ai.enabled=true --odontocare.ai.api-key=gsk_test_key --odontocare.ai.worker-enabled=false al backend test, conservar WhatsApp worker-enabled=false y ejecutar los escenarios indicados en frontend/README.md. Nunca usar claves reales en esa regresión. Instantáneas vigentes: alcance 1.7, plan 2.6.

## Kapso · conexión manual experimental

Trabajar en la rama kapso. El módulo kapso recibe JSON v2 firmado y envía texto manual usando la bandeja existente. Configuración privada en config/kapso.local.properties, importada por local e ignorada por Git; ejemplo en config/kapso.example.properties. enabled=true selecciona Kapso y pausa Twilio y el agente; enabled=false restaura Twilio. Se conservan las tablas anteriores y V16 agrega tres tablas propias.

Webhook limitado: POST /api/v1/integrations/kapso/events a través del receptor 8082. HMAC SHA256 del cuerpo original, número/participante autorizados, eventos persistidos y respuesta HTTP rápida. El Sandbox real puede indicar delivered para una entrada; su dirección y evento determinan la clasificación. No conecta IA ni crea pacientes o citas.

[Guía de cuenta, clave, ID, túnel, webhook y prueba](docs/project/conectar-kapso-prueba.md). [Resultados y compatibilidad](docs/project/conexion-kapso-fase-6.md): 123 casos backend verificados, dos del receptor, diez del navegador y un intercambio real Leído con respuesta del participante. Se conserva A22 pendiente.

Para E2E de Kapso, iniciar el JAR con perfil test y spring.config.additional-location=file:src/test/resources/application-test.properties,file:src/test/resources/kapso-e2e.properties. Ese archivo solo contiene credenciales ficticias, worker-enabled=false y agente detenido. Preparar la base protegida desde frontend/scripts/prepare-e2e.ps1 y ejecutar npx playwright test tests/kapso.spec.ts. Las claves reales no se utilizan en la regresión. La prueba manual habitual usa sistema_odontologo con los datos ficticios actuales. Instantáneas vigentes: alcance 1.8, plan 2.8.
