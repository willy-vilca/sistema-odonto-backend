# OdontoCare — backend

Base del sistema odontológico configurable. Fase 0: instalación ejecutable, PostgreSQL, migraciones, arquitectura por capas, política inicial de acceso, errores seguros e identidad pública del consultorio. Los módulos clínicos, agenda, pagos y agente aún no están implementados.

Tecnologías: Java 21, Spring Boot 4.1.1, Maven Wrapper 3.9.16, Spring MVC, Data JPA, Validation, Security, Actuator, Flyway y PostgreSQL. La instalación local verificada utiliza PostgreSQL 18.3. Las versiones transitivas se administran con Spring Boot; no se requiere Maven global.

## Inicio local en Windows

Requisitos: JDK 21 en PATH y PostgreSQL activo. Crear la base `sistema_odontologo` si aún no existe, mediante pgAdmin o `CREATE DATABASE sistema_odontologo;`. No reinicializar una base existente.

El archivo `src/main/resources/application-local.properties` ya contiene los datos locales autorizados:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/sistema_odontologo
spring.datasource.username=postgres
spring.datasource.password=admin
```

Desde la carpeta backend:

```powershell
.\mvnw.cmd spring-boot:run
```

Se inicia en http://127.0.0.1:8080. El primer arranque descarga dependencias y aplica las migraciones. Los siguientes validan el esquema y conservan los datos. Detener con Ctrl+C en la misma terminal. No hace falta definir variables de entorno para este perfil local.

En otra terminal, desde la carpeta frontend:

```powershell
npm.cmd ci
npm.cmd run dev
```

Abrir http://127.0.0.1:5173. Vite comunica `/api` con este backend. Si un puerto está ocupado, cerrar únicamente el proceso correspondiente o ajustar ambos extremos del proxy; no terminar procesos desconocidos.

## Compilación y validación

Con PostgreSQL activo:

```powershell
.\mvnw.cmd -B -ntp verify
java -jar target/odontocare-0.0.1-SNAPSHOT.jar
```

Las pruebas consultan la identidad real de la base y comprueban restricciones de acceso y errores sanitizados; no alteran la identidad ni datos del negocio. El contexto aplica nuevas migraciones pendientes, igual que un arranque normal. Los escenarios de fallo usan un repositorio sustituido en el contexto de prueba. El informe queda en `target/surefire-reports`.

En Windows, detener el proceso iniciado con `java -jar` antes de reconstruir el ejecutable: el archivo permanece bloqueado mientras está en uso. El arranque mediante `spring-boot:run` usa las clases de desarrollo y evita depender de ese archivo para trabajar.

En macOS/Linux: utilizar `./mvnw` y `npm`; los comandos restantes son equivalentes.

## API inicial

| Método y ruta | Respuesta |
|---|---|
| GET /api/v1/system/installation | Nombre, zona horaria y moneda leídos de PostgreSQL |
| GET /actuator/health | Estado de salud sin detalles internos |
| Otras rutas | Acceso denegado; los permisos completos se incorporan en fase 1 |

```json
{"displayName":"Mi consultorio","timeZone":"America/Lima","currency":"PEN"}
```

Cada respuesta incorpora `X-Request-Id`. Los fallos del acceso a datos utilizan estado 503 y Problem Details sin SQL o trazas. Las solicitudes de escritura no están habilitadas.

## Arquitectura y configuración

Consultar [decisiones y estructura](docs/architecture.md). Los módulos se organizan por responsabilidad: model, repository, service, dto y controller. Flyway en `src/main/resources/db/migration`; Hibernate solo valida. No editar una migración aplicada ni activar `ddl-auto=update`.

Para comprobar la configuración separada de producción:

```powershell
java -jar target/odontocare-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Este perfil exige `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, con `PORT` y `SERVER_ADDRESS` opcionales. Sin esos valores falla el arranque; nunca sustituye silenciosamente credenciales de producción por las locales. No es todavía una instalación productiva: login y roles corresponden a fase 1, y despliegue final a fase 9. Tokens Twilio y modelo se incorporarán como secretos externos cuando se implementen sus fases.

## Guías permanentes

Las guías vigentes están en la raíz de trabajo: AGENTS.md, alcance y plan dentro de docs, y prompt maestro. [docs/project](docs/project) conserva instantáneas versionadas para respaldarlas junto al código. No mantener requisitos diferentes entre ambas ubicaciones. Imágenes/PDF futuros irán dentro de PostgreSQL; todos los listados se paginan, filtran y buscan en el backend. Conservar commits descriptivos y validar cada fase antes de continuar.
