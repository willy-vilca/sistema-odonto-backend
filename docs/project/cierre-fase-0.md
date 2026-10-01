# Cierre técnico de la fase 0

Fecha: 30 de septiembre de 2026. Estado: completada técnicamente; disponible para revisión y ajustes del usuario. Alcance 1.2, plan 1.1. No se ha iniciado la fase 1.

## Resultado entregado

Se generaron las estructuras oficiales en los repositorios existentes backend y frontend, conservando sus carpetas Git. Producto provisional: OdontoCare; identidad de demostración: Mi consultorio. La marca y configuración editable se desarrollan en fase 1.

Backend Spring Boot 4.1.1, Java 21 y Maven Wrapper, conectado al PostgreSQL local del usuario. Primera migración Flyway aplicada sobre la base inicialmente vacía; Hibernate valida el esquema. Capas de instalación implementadas: modelo, repositorio, servicio, DTO y controlador. Seguridad deniega operaciones no habilitadas, mantiene CSRF y solo expone identidad pública y salud sin detalles. Errores seguros, referencias de solicitud y logs sin cuerpos ni credenciales.

Frontend React 19, Vite 8, TypeScript estricto y TailwindCSS 4. Estructura por app, features y shared. Sistema visual claro con acento verde, tipografías locales, iconos consistentes, navegación lateral y menú móvil accesible. Pantalla inicial con datos reales de instalación, carga, error y reintento. Vistas de módulos futuros identificadas expresamente como pendientes, sin operaciones ni datos de pacientes inventados.

Se documentaron estructura, decisiones, arranque, parada, compilación, comprobaciones, perfiles y accesos externos futuros. No se crearon cuentas ni se contrataron servicios. Twilio Sandbox, participantes autorizados, credencial de modelo y recepción HTTPS se prepararán antes de las fases de integración.

Tus instrucciones quedan incorporadas en la memoria del proyecto: TailwindCSS; interfaz elegante, profesional y adaptable; prompt maestro permanente; separación por capas y responsabilidades; paginación, filtros y búsqueda en el backend; archivos dentro de PostgreSQL; reglas financieras confirmadas y commits en ambos repositorios. Se añadió A32 para comprobar los listados en las próximas fases.

## Comprobaciones ejecutadas

| Comprobación | Resultado y evidencia |
|---|---|
| PostgreSQL real | PostgreSQL 18.3, base sistema_odontologo; identidad consultada desde la interfaz mediante /api y proxy Vite |
| Migración sobre esquema vacío | Flyway creó historial e installation_profile, versión 1, success=true; Hibernate valida su correspondencia |
| Reinicio y conservación | Arranques posteriores no repiten la migración ni el registro inicial; verificado un único perfil |
| Pruebas backend | 5 aprobadas, 0 fallos: contrato y persistencia real, acceso restringido, salud sin detalles, CSRF/escritura denegada, errores 503 y 500 sanitizados |
| Referencia de errores | 401, 403 y errores de aplicación comparten Problem Details; X-Request-Id generado por el servidor. Verificado también por HTTP real en la versión final |
| Compilación backend | Ejecutable Maven empaquetado correctamente; arrancado de nuevo y conectado a PostgreSQL |
| Separación de perfiles | Perfil prod sin sus variables rechaza el arranque; no utiliza los datos locales de demostración |
| Tipos, calidad y formato frontend | build, lint y format:check aprobados |
| Pruebas de navegador | 8 aprobadas: seis tamaños, conexión real, navegación, foco, ausencia de desbordamiento, carga, fallo controlado y recuperación, menú móvil y Escape |
| Accesibilidad automatizada | Sin infracciones detectadas por axe con reglas WCAG A/AA y WCAG 2.1 AA en las vistas y menú comprobados; no equivale a una certificación de accesibilidad del producto completo |
| Revisión visual | Capturas inspeccionadas de computadora, tablet y celular; jerarquía, contraste, adaptación y legibilidad correctos para esta base |
| Limpieza | Eliminados ejemplos y recursos de Vite; descartados archivos temporales. Dependencias de prueba innecesarias del generador retiradas |

Tamaños comprobados: 1440×900, 1280×800, 1024×768, 768×1024, 390×844 y 360×800. Las capturas aprobadas se conservan en frontend/docs/verification: inicio-1440.png, inicio-768.png e inicio-390.png. Las pruebas de fallo de conexión interceptan respuestas HTTP; la recuperación utiliza la API real. Las pruebas de fallo del backend sustituyen el repositorio dentro del contexto de prueba. No se detuvo el servicio PostgreSQL del equipo para provocar esos fallos.

Durante la revisión se corrigieron el mapeo JPA del tipo CHAR de la moneda y el ciclo de foco del menú móvil. En Windows, el ejecutable abierto bloqueó el reempaquetado; se detuvo únicamente el backend de esta sesión, se empaquetó correctamente y se reinició. Las cinco pruebas pasaron en la versión final; el empaquetado posterior se ejecutó sin repetirlas.

## Reproducir la revisión

1. Iniciar PostgreSQL. Leer backend/README.md para usar la base y configuración local ya proporcionadas.
2. En backend: ejecutar `mvnw.cmd spring-boot:run`.
3. En frontend: ejecutar `npm.cmd ci` y `npm.cmd run dev`.
4. Abrir http://127.0.0.1:5173; comprobar identidad disponible y navegar por los módulos previstos.
5. Ejecutar los comandos de validación indicados en ambos README. Detener un proceso java -jar antes de reconstruir su ejecutable en Windows.

Los servicios locales quedaron iniciados al cerrar esta implementación; permanecen disponibles mientras sus procesos estén activos. Los README permiten volver a iniciarlos sin depender de esta sesión.

## Trazabilidad y límites

Commits de implementación: backend fd9e76d y frontend 8b14357. Los commits de cierre agregan estas evidencias y las instantáneas de guías; consultar `git log --oneline` en cada repositorio para su identificación. No se realizó push ni despliegue externo.

Las guías maestras permanecen en la raíz compartida. Como esa raíz no es un repositorio Git, se conservan copias exactas identificadas como instantáneas en backend/docs/project, junto al prompt original del usuario. No se creó un tercer repositorio. Cuando cambien las fuentes, actualizar sus copias versionadas en el mismo avance.

A02 y A31 tienen bases preparadas; A29 y A30 tienen infraestructura inicial. A32 queda definido para los primeros listados de fase 1. Ninguno de estos criterios se declara aprobado para todo el producto con esta fase. Autenticación completa, roles, configuración editable, pacientes, agenda, clínica, documentos, dinero y agente siguen pendientes de sus fases.

No quedan fallos críticos abiertos de fase 0. La revisión del usuario puede ajustar esta base antes de comenzar fase 1.
