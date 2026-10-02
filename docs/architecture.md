# Arquitectura y decisiones iniciales

Fecha: 30/09/2026. Fase 0.

El sistema es un monolito modular, distribuido en dos repositorios: una API Spring Boot y una interfaz React. Cada consultorio tendrá instalación y base independientes; no hay multiempresa ni gestión de sillones.

## Backend

Paquete raíz `com.odontocare`. Cada módulo funcional conserva sus propias capas:

```text
installation/
  controller/    Contrato HTTP y códigos de respuesta.
  dto/           Datos de entrada/salida; no entidades JPA.
  service/       Casos de uso y límites transaccionales.
  repository/    Acceso a persistencia.
  model/         Entidades y relaciones persistidas.
security/config/ Políticas de acceso.
shared/web/      Errores y correlación de solicitudes.
```

Los módulos previstos se incorporan cuando se desarrolla su fase: configuración, usuarios/seguridad, odontólogos y servicios, pacientes, agenda, clínica, documentos, tratamientos, finanzas, conversaciones/agente, reportes y auditoría. No se agregan clases vacías ni interfaces de servicio sin necesidad. Los controladores delegan; los repositorios no deciden reglas clínicas o financieras. Un mapeo de tres campos se realiza en el servicio; se extraerá un mapper cuando su complejidad justifique una clase propia.

Flyway controla el esquema y su evolución. Las migraciones aplicadas no se modifican: se agregan nuevas versiones. Hibernate usa `validate` y `open-in-view=false`; el servicio resuelve los datos dentro de su transacción antes de convertirlos a DTO. Fechas persistidas en UTC; interpretación de horarios según la zona del consultorio. Dinero futuro en `BigDecimal` y PostgreSQL `numeric`, nunca punto flotante.

`installation_profile` contiene un único registro, protegido por una clave y una restricción `id=1`. La fase 0 expone únicamente nombre, zona horaria y moneda. No permite edición, usuarios ni datos clínicos; su finalidad es verificar la conexión de extremo a extremo y preparar la identidad configurable.

Errores con `application/problem+json` (Problem Details): estado, título, detalle seguro y referencia. `X-Request-Id` es generado por el servidor y acompaña los logs; no se aceptan identificadores arbitrarios para el contexto de registro. Las excepciones no devuelven SQL, credenciales ni trazas al navegador. No se registran cuerpos de solicitudes. Los fallos del acceso a datos y de creación de transacción producen 503; los inesperados, 500.

Spring Security permite solo GET de la identidad pública y salud sin detalles. Las demás rutas se deniegan. Se conserva CSRF; no se habilita CORS abierto ni autenticación ficticia. Se desactiva el usuario aleatorio de Spring Boot. Login, contraseñas y roles se implementan en fase 1 antes de cualquier operación de negocio.

## Frontend

```text
src/app/                         Composición, rutas, navegación y tokens Tailwind.
src/features/home/               Inicio y vistas informativas de módulos futuros.
src/features/installation/
  model/                         Tipos y estados del contrato.
  services/                      Solicitudes y validación de la respuesta.
  hooks/                         Estado asíncrono, cancelación y reintento.
  components/                    Presentación de la conexión.
src/shared/api/                  Transporte HTTP y errores comunes.
src/shared/ui/                   Componentes reutilizables.
```

React + TypeScript estricto. React Router gestiona navegación y direcciones; HTTP usa `fetch`, con cancelación al desmontar y un límite de espera. No se añade una librería de estado o consultas para un solo recurso. Los tipos HTTP no sustituyen validación en ejecución.

TailwindCSS 4 se integra con su plugin de Vite. Tokens para marca, contraste, bordes, tipografía y espaciado. Fuentes DM Sans y Manrope alojadas en el proyecto; no se consulta un servicio de fuentes externo. Iconos Lucide y marca provisional dibujada en SVG. No se usan imágenes decorativas generadas ni gráficos con datos inventados.

Navegación lateral en computadora y diálogo modal en pantallas menores. Diálogo con cierre mediante Escape, ciclo de foco y restauración al botón; cambio de ruta lleva el foco al contenido principal. Carga, conexión correcta, error y reintento visibles con texto. Se respeta reducción de movimiento y hay enlace para saltar al contenido.

## Reglas transversales para las próximas fases

- Listados: `page` desde 0, tamaño por defecto 20 y máximo 100; filtros tipados y `search` normalizado. El repositorio ejecuta filtros, búsqueda, orden estable y paginación en PostgreSQL. El servicio valida campos permitidos de ordenación. El DTO contiene `items`, `page`, `size`, `totalElements` y `totalPages`; React no pagina ni filtra colecciones completas. Los valores concretos se implementan con el primer listado en fase 1.
- Agenda: consultas con intervalo limitado y filtro de odontólogo; las listas asociadas también se paginan. La base impedirá solapamientos aunque haya reservas concurrentes. Una sola capa de reglas para usuario y agente.
- Adjuntos: metadatos separados de contenido `bytea` dentro de PostgreSQL. Consultar solo metadatos paginados; descargar binarios a demanda con permisos.
- Deuda: servicios individuales al finalizar su realización y planes al aceptarlos. Sesiones incluidas y reintentos no generan un segundo cargo. Auditoría y transacciones desde cada módulo.
- Seguridad y accesibilidad se verifican durante cada fase. No se aplazan a la entrega final.

## Entornos e integraciones futuras

El perfil por defecto es `local`, con los datos de PostgreSQL autorizados explícitamente por el usuario. El perfil `prod` exige `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`; no hereda credenciales de demostración. El backend local escucha solo en 127.0.0.1. Vite utiliza un proxy `/api`, evitando un CORS de desarrollo innecesario y manteniendo secretos fuera del frontend.

La fase 0 no es un despliegue para pacientes reales. El despliegue definitivo, TLS, respaldos y restauración se cierran en fase 9; autenticación, en fase 1.

Para las fases 6–7 se requerirán: cuenta Twilio con Sandbox de WhatsApp, participantes autorizados inscritos, credenciales del proveedor/modelo y un webhook HTTPS accesible. Las credenciales externas permanecen fuera de Git. No se contrataron servicios ni se crearon cuentas. Preparar estos accesos no condiciona las fases locales. El agente propio utilizará herramientas de los servicios de negocio y bitácora persistente, sin SQL libre ni n8n.

Referencias de integración: [Spring Boot](https://docs.spring.io/spring-boot/system-requirements.html), [Vite](https://vite.dev/guide/), [TailwindCSS con Vite](https://tailwindcss.com/docs/installation/using-vite).

## Decisiones incorporadas en fase 1

Se incorporan users, catalog, dentists, schedules y audit con las mismas capas. security separa política, DTO, sesión, contraseña y filtros. Autenticación de formularios y sesiones de Spring Security, CSRF y BCrypt; el navegador no guarda tokens de acceso. [CSRF en Spring Security](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html).

Las modificaciones administrativas bloquean la única fila de instalación dentro de su transacción. Esto serializa comprobaciones de primer administrador, último administrador, referencias y horarios. version protege ediciones obsoletas. La agenda futura utilizará servicios y restricciones propios; el bloqueo administrativo no se impone a las reservas.

btree_gist y una restricción de exclusión protegen los intervalos semanales activos del mismo tipo por odontólogo y día. El validador comprueba descansos dentro de jornadas y limita a 48 intervalos activos por día. Auditoría con propagación transaccional obligatoria: un cambio revertido no deja un evento de éxito.

Listados con Specifications y Pageable. La búsqueda escapa porcentajes y guiones bajos para interpretar texto literal; orden estable por identificador. Solo DTO de la página solicitada. Las asociaciones seleccionadas se limitan a 100 servicios por profesional. El selector de cuentas habilitadas devuelve únicamente identificador y nombre, con DENTISTS_WRITE.

installation_logo separa bytes del perfil. La identidad pública expone nombre, marca, zona, moneda, formato de fecha y revisión del logo; excluye contactos privados y cuentas. El binario se consulta explícitamente.

Frontend con definiciones de formularios por módulo y componentes reutilizables de editor, selector remoto, tabla y diálogo. Los selectores consultan al abrir su búsqueda. La autorización definitiva permanece en el servidor.

test usa una base separada y comprueba su nombre antes de limpiar datos. Las pruebas de navegador crean una contraseña aleatoria y una sesión por caso. No utilizan cuentas predeterminadas ni preparan datos en la instalación local del usuario.

## Fase 2: pacientes y agenda

Los módulos patients y appointments tienen sus propios modelos, repositorios, DTO, servicios y controladores. PatientValidator separa identidad y responsables; BookingRules y AppointmentStateRules separan elegibilidad, tiempo y transiciones. AvailabilityService calcula con horarios reales; AppointmentService ejecuta transacciones y AppointmentQueryService limita y pagina consultas. AppointmentHistoryService agrega movimientos inmutables; AuditService registra las operaciones sin contactos ni notas personales.

La configuración compartida se bloquea para lectura durante las reservas, mientras los cambios administrativos usan su bloqueo exclusivo. Los bloqueos de odontólogos se toman en orden UUID al cambiar de profesional. La exclusión GiST protege el intervalo ocupado en PostgreSQL y la clave UUID de solicitud evita duplicación por reintento. La duración es una instantánea y los cambios de catálogo no alteran citas existentes.

Ver [decisiones de la fase](project/diseno-agenda-fase-2.md) para límites de consultas, separación, estados y zona horaria. La integración real de WhatsApp continúa como dependencia externa de fase 6.

## Fase 3: expediente clínico y originales

clinical separa configuración, control de acceso profesional, estados clínicos y atenciones. Un borrador usa version optimista; finalizar agrega encounter_revision inmutable; corregir agrega otra revisión con motivo y la identidad histórica del paciente/profesional. El controlador no ejecuta reglas. Las revisiones y estados se protegen también con triggers PostgreSQL.

Los estados BACKGROUND y ODONTOGRAM usan contratos tipados y JSON textual acotado; el estado previo y la secuencia se validan dentro de un bloqueo de paciente. Índices únicos previenen cadenas bifurcadas. El gráfico es un conjunto fijo de 52 piezas, no un listado de pacientes descargado; el historial se pagina en PostgreSQL. No se presupone salud para superficies sin registrar.

documents almacena metadatos inmutables y DocumentContent BYTEA en entidades/tablas separadas, sin asociación de carga automática. Las listas nunca consultan contents. Validación por contenido, extensión, tamaño, estructura PDF y dimensiones de imagen; SHA-256 para recuperación y conservación de originales. Consentimientos referencian una copia del mismo paciente.

DocumentPreviewService utiliza PDFBox existente para renderizar solo la página PDF solicitada. La imagen es una vista, no reemplaza el archivo; el acceso autorizado a la vista y al original se audita. Se limita el render a 1600 píxeles por lado con subsampling; el navegador administra URLs de objeto y las libera al cerrar/cambiar. JPG/PNG/WebP se recuperan mediante URL protegida a demanda.

Permisos de clínica, documentación y configuración independientes. Escribir clínica exige vinculación con odontólogo activo o rol administrador; consultar historias conserva acceso a registros de profesionales inactivos. AuditService registra consultas y cambios sin volcar antecedentes, diagnósticos, archivos ni motivos clínicos en logs.

El cierre de fase 3 prepara pg_dump de toda la base; [procedimiento](project/respaldo-postgresql.md). El catálogo y la huella del respaldo se verifican, pero la restauración integral/A30 se valida en fase 9. Referencias y decisiones en [diseño clínico](project/diseno-clinico-fase-3.md).

## Tratamientos y cargos: fase 4

Treatments contiene el acuerdo, conceptos, operaciones inmutables y sesiones. Finance contiene cargos y ajustes inmutables y su agregado por moneda. ClinicalBillingService conecta la finalización clínica con ambos servicios; la corrección clínica conserva los movimientos anteriores. No hay pagos, cuotas ni caja en esta fase.

La escritura se serializa por paciente antes del plan o atención. Las operaciones del plan y ajustes usan claves UUID con huella de contenido; los cargos tienen claves únicas de origen. La aceptación, cargos y auditoría comparten transacción. Cada edición de conceptos o avance incrementa mutation y por tanto la versión del plan, incluso cuando su cabecera permanece igual.

El presupuesto tiene un máximo inicial de 50 conceptos y admite hasta 100 con adicionales. Esta colección acotada se procesa dentro de la transacción de aceptación; los listados HTTP son paginados. Las sumas y el avance se consultan mediante agregados de PostgreSQL. BigDecimal y numeric conservan cálculos monetarios de dos decimales. Cada acuerdo y cargo conserva moneda; no se suman monedas distintas.

V8 incorpora tablas, permisos, claves, índices, protección de acuerdos y vínculo documental con tratamiento. V9 incorpora la revisión de cambios del detalle. Se conservan migraciones anteriores. Las atenciones finalizadas anteriormente no se cobran retroactivamente. Decisiones y comprobaciones en las instantáneas de diseño y cierre de fase 4.

## Fase 5: dinero y trazabilidad

finance incorpora modelos y repositorios separados para movimientos de dinero, aplicaciones, operaciones, cuotas/calendarios, categorías, caja y documentación financiera. CashRegister es un mutex de persistencia del registro único; ChargeBalance consulta una vista agregada de cargos y aplicaciones. No existe un saldo editable en patient.

AccountQueryService consulta cuentas con instantánea consistente y listas acotadas; PaymentService registra, aplica, libera y corrige; ExpenseService gestiona categorías y egresos; CashService conserva el arqueo; InstallmentService distribuye vencimientos; FinanceOperationService comprueba clave y huella; FinancialDocumentService conserva documentos y FinancialPdfService solo renderiza. Controllers validan contratos y permisos; no calculan saldos ni devuelven JPA.

Orden de bloqueos: paciente → registro de caja → perfil cuando corresponde un correlativo. Egresos/caja solo toman registro → perfil; planes toman paciente → perfil. Consultas clínicas no adquieren registro de caja. Cada cambio de deuda y cada aplicación se serializa por paciente; efectivo se serializa con apertura/cierre. La transacción incluye movimiento, aplicaciones, operación, PDF y auditoría; un fallo revierte todo.

V10 añade el registro monetario, aplicaciones, cuotas, categorías, documentos/contenido, registro/cajas y vista de saldo. V11 protege calendarios históricos, vincula documentos con arqueos y añade unicidad de constancias y nombres de categorías. Los movimientos originales y sus binarios no permiten UPDATE/DELETE; caja cerrada queda congelada. Las consultas de archivo no incluyen bytea.

El frontend agrupa finanzas en cuenta, gastos, caja y categorías. Dentro de la cuenta, cargos/pagos/cuotas/archivos se montan según la sección activa. Formularios conservan una clave por operación mientras se reintentan y los selectores consultan páginas del servidor. Las constancias tienen visor de páginas con zoom y descarga autorizada; fechas respetan configuración.
