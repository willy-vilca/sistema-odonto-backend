# OdontoCare — backend

Fases 0 y 1: identidad, acceso, permisos, usuarios, odontólogos, servicios, horarios, bloqueos y auditoría. Java 21, Spring Boot 4.1.1, Maven y PostgreSQL.

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

Administrador, odontólogo, recepción y caja son roles fijos con nombres y permisos configurables. En esta fase los roles operativos consultan configuración, profesionales, servicios y horarios. El administrador gestiona usuarios, política y auditoría. Las funciones de pacientes, agenda, clínica y cobros llegarán con sus módulos.

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
java -jar ../backend/target/odontocare-0.0.1-SNAPSHOT.jar --spring.profiles.active=test --spring.config.additional-location=file:../backend/src/test/resources/application-test.properties
~~~

En otra terminal de frontend: npm.cmd run test:e2e. Puertos 8081 y 5174. Contraseñas aleatorias únicamente en .runtime, ignorado por Git. La preparación verifica el nombre de la base antes de vaciarla. No ejecutar verify mientras se pruebe el navegador: comparten la base de pruebas.

En Windows, detener java -jar antes de reconstruir el ejecutable. El arranque spring-boot:run evita depender del archivo JAR durante desarrollo.

## Producción y siguientes fases

prod exige DB_URL, DB_USERNAME y DB_PASSWORD, con PORT y SERVER_ADDRESS opcionales; no hereda credenciales locales. Flyway V2 necesita autorización para instalar btree_gist. Preparar esa extensión con el administrador de PostgreSQL para una instalación productiva.

La fase 2 aplicará las reglas a las citas. Textos y correlativos se consumirán al incorporar pacientes, presupuestos y constancias. Clínica, pagos y WhatsApp siguen pendientes. Despliegue final, respaldos y restauración: fase 9.

Consultar [arquitectura](docs/architecture.md) y [cierre de fase 1](docs/project/cierre-fase-1.md). Las guías maestras están en la raíz; docs/project conserva sus instantáneas exactas.
