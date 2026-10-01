# Desarrollo del backend

Antes de implementar, leer `../AGENTS.md`, el alcance y plan de `../docs` y el prompt maestro de la raíz. Si este repositorio se obtiene por separado, las instantáneas de `docs/project` conservan esas guías; revisar su fecha antes de usarlas.

Arquitectura por módulos funcionales y capas en `com.odontocare`: modelo JPA, repositorio, servicio transaccional, DTO y controlador. `shared` aloja únicamente necesidades transversales reales. SQL versionado en Flyway; Hibernate valida, nunca crea o actualiza tablas. Listados paginados, filtrados y buscados en PostgreSQL, con tamaño máximo y ordenación permitida explícitos. No devolver entidades ni contenido binario en listados.

Leer README y docs/architecture.md. La fase 0 prepara la base; autenticación completa y configuración editable pertenecen a fase 1. Registrar commits y evidencia de validación sin declarar funciones futuras como terminadas.
