# Respaldo integral de PostgreSQL

Fase 3 prepara el respaldo; A30 exige restaurarlo y comprobarlo integralmente en fase 9.

Desde backend ejecutar: powershell -File scripts/backup-local.ps1. Por defecto respalda sistema_odontologo; la opción -Database sistema_odontologo_test respalda únicamente pruebas. Herramientas locales de PostgreSQL 18. La contraseña local de demostración se utiliza solo durante la ejecución y se restaura la variable anterior al terminar.

El script verifica el nombre real de la base, crea un archivo pg_dump en formato custom, comprueba que el catálogo incluye contenido binario y versiones clínicas, y muestra tamaño y SHA-256. No borra archivos ni restaura sobre la instalación. Destino: backend/.runtime/backups, excluido de Git.

El respaldo incluye esquema, migraciones, usuarios, permisos, auditoría, configuración, pacientes, agenda, atenciones, odontogramas, consentimientos y archivos BYTEA. Los originales pertenecen a la misma copia consistente de PostgreSQL. Ver [documentación de pg_dump](https://www.postgresql.org/docs/current/app-pgdump.html).

Guardar copias adicionales fuera del equipo es una responsabilidad operacional de la instalación. Los archivos contienen datos sensibles; no incorporarlos al repositorio ni compartirlos como evidencia pública. Este script es local: producción requiere sus propios accesos protegidos y una política de retención.

En fase 9: crear una base nueva y aislada, verificar que esté vacía, restaurar con pg_restore --exit-on-error --no-owner --no-acl, levantar la aplicación contra esa base y comprobar relaciones, auditoría, permisos, SHA-256 y recuperación visual de imágenes/PDF. Registrar resultados antes de declarar A30 cumplido. No utilizar --clean sobre la base principal.

Desde fase 4, el respaldo completo incluye acuerdos, conceptos, operaciones, sesiones y el registro inmutable de cargos y ajustes. El verificador también comprueba treatment_plan y charge_entry en el catálogo. La restauración integral de fase 9 debe recuperar esos orígenes, importes, monedas, claves de operación y vínculos con atenciones sin duplicar movimientos.
