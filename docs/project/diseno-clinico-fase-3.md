# Diseño de la fase 3

Fecha: 1 de octubre de 2026. Implementación dentro del alcance confirmado.

- Módulos por capas: clinical (antecedentes, atenciones y estados del odontograma) y documents (metadatos, originales, categorías y consentimientos). Se reutilizan permisos, auditoría, paginación y control de versiones.
- Atenciones: borrador editable con versión optimista; finalizar guarda una copia inmutable con identidad del paciente y profesional. Corregir exige motivo y agrega una versión completa, sin modificar el original. Una cita puede enlazarse a una sola atención; la relación se valida por paciente y odontólogo. La atención sin cita permite atender un ingreso directo.
- Antecedentes y odontograma: estados fechados inmutables. Actualizar exige indicar el estado anterior para prevenir pérdida de cambios simultáneos. No se interpreta ausencia de hallazgos como salud: las superficies sin registrar se muestran como tales. Numeración FDI, 32 piezas permanentes y 20 temporales. La secuencia propia y un índice único ordenan estados aun si la fecha/hora coincide; se conserva el profesional seleccionado.
- Documentación: tablas distintas para metadatos y BYTEA, sin asociación JPA que cargue el binario en consultas de listas. El contenido se recupera solo al abrir o descargar un archivo, previa autorización y auditoría. Conservación de originales y huella SHA-256. Consentimiento vinculado a archivo del mismo paciente con responsable y fecha.
- Configuración: plantillas clínicas, categorías documentales y límite de archivo editable (20 MiB inicial). Límite de transporte de 64 MiB; política admite 1–60 MiB. Validación del contenido real con ImageIO/WebP y PDFBox, sin confiar en MIME o extensión.
- Acceso: permisos separados para consultar/escribir clínica, consultar/cargar documentos y consultar/editar configuración clínica. Administrador conserva todos; odontólogo recibe clínica y documentos. Recepción y caja no los reciben automáticamente. Escritura clínica requiere profesional activo y cuenta vinculada, o administrador con selección explícita.
- Visualización PDF: páginas renderizadas a PNG por el servidor, únicamente a demanda, con paginación y permisos. No depende de plugins del dispositivo; el PDF original continúa intacto para descarga.
- Riesgos principales: correcciones y cargas concurrentes; acceso por ID directo; documentos con formato falso; consultas que carguen bytes; accesibilidad del gráfico táctil; backups incompletos. Se verifican con PostgreSQL real y navegador.
- Fase 4 agregará cargos y tratamientos. No se crean identificadores de tratamientos sin una relación real todavía.

Referencias técnicas: [PDFBox 3](https://pdfbox.apache.org/3.0/migration.html), [TwelveMonkeys](https://github.com/haraldk/TwelveMonkeys), [ISO 3950](https://www.iso.org/standard/68292.html).
