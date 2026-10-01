# Cierre de fase 3 — atención clínica, odontograma y archivos

Fecha: 1 de octubre de 2026. Alcance de referencia: versión 1.2. Estado: completada técnicamente y disponible para revisión del usuario.

## Funciones entregadas

- Expediente clínico por paciente: antecedentes, alergias, medicamentos informados y anamnesis. Plantillas clínicas básicas configurables para antecedentes, atenciones y consentimientos.
- Atenciones por paciente y odontólogo, con cita opcional para ingresos directos, motivo, anamnesis, evolución, diagnósticos, procedimientos e indicaciones. Los procedimientos pueden vincularse al catálogo o describirse individualmente.
- Borradores editables con control de versiones; finalización y correcciones justificadas que agregan una revisión y conservan el original, su responsable y las identidades registradas. Finalizar una atención vinculada completa su cita mediante las reglas centrales de agenda.
- Odontograma FDI de 32 piezas permanentes y 20 temporales, con pieza completa y cinco superficies. Hallazgos expresados con texto, símbolos y color; estado sin registrar distinto de una pieza informada sin hallazgos. Cada actualización conserva fecha, profesional, autor y motivo; historial consultable y protección ante actualizaciones simultáneas.
- Archivos JPG/JPEG, PNG, WebP y PDF con originales en PostgreSQL. Metadatos y contenido bytea en tablas separadas, sin relación JPA que cargue binarios al listar. Categoría configurable, fecha, descripción y vínculos opcionales con atención y pieza.
- Validación real del contenido, extensión, tamaño y legibilidad; límite inicial de 20 MiB configurable entre 1 y 60 MiB. Imágenes de hasta 40 millones de píxeles; PDF de hasta 1.000 páginas, sin cifrado ni contenido activo admitido.
- Visualización y descarga autorizadas a demanda, comparación de dos fotografías con sus fechas, y visor PDF por página utilizable en celular. La vista PNG del PDF se genera a demanda, sin modificar el PDF original.
- Consentimientos documentales con nombre, responsable, relación, fecha y una copia adjunta del mismo paciente. Sin firma electrónica.
- Permisos separados para clínica, documentos y configuración clínica. El odontólogo escribe con su registro profesional activo; el administrador puede seleccionar el responsable. Recepción y caja no reciben acceso clínico o documental por defecto.
- Auditoría transaccional de modificaciones clínicas, consultas de historiales, acceso a archivos, descarga, consentimientos y configuración. Listados e historiales con filtros, búsqueda, ordenación autorizada y páginas ejecutadas en el servidor.
- Mecanismo local de respaldo PostgreSQL completo, incluidos originales y revisiones. Procedimiento de restauración integral preparado para ejecutarse en fase 9.

## Evidencia de validación

Servidor: 41 pruebas distintas aprobadas sobre PostgreSQL real en sistema_odontologo_test: 32 de regresión y 9 de fase 3. Se aprobó la batería completa y, después de los ajustes finales de fase 3, sus nueve pruebas específicas y la generación del ejecutable.

Navegador: 30 escenarios distintos aprobados. Una ejecución completa aprobó 29; posteriormente se incorporó la comprobación adicional de configuración clínica y se aprobaron nuevamente los ocho escenarios de fase 3, incluido el PDF de dos páginas. Los 22 escenarios anteriores permanecen aprobados. Compilación de producción, revisión estática y formato aprobados.

| Escenario | Resultado |
|---|---|
| Crear borrador, finalizar y corregir | Original y corrección recuperables; responsable y motivo conservados |
| Consultar una revisión después de cambiar identidades del catálogo | La revisión conserva las identidades y valores registrados |
| Editar una versión antigua o escribir estados simultáneamente | Conflicto sin sobrescribir el historial; una actualización concurrente aceptada |
| Intentar alterar directamente registros clínicos finalizados | PostgreSQL rechaza cambios o borrados de registros protegidos |
| Consultar estados de odontograma en dos fechas y ambas denticiones | Piezas y superficies conservadas; numeración inválida rechazada |
| Finalizar atención vinculada a una cita | Cita completada con historial; una cita futura no permite finalizar |
| Cargar JPG/JPEG, PNG, WebP y PDF | Contenido recuperado idéntico al original en la misma base |
| Listar documentos | Respuesta de metadatos sin contenido binario |
| Archivo vacío, falso, extensión incongruente o excesivo | Rechazado; límite configurable comprobado |
| PDF con acciones activas o página inexistente | Rechazado según validación del documento o de la página solicitada |
| Descargar y visualizar PDF | Permisos verificados; página PNG legible y navegación entre dos páginas |
| Consentimiento y categoría desactivada | Copia del mismo paciente obligatoria; originales anteriores conservados |
| Recepción, caja, sesión anónima y otro odontólogo | Operaciones restringidas denegadas por el servidor |
| Plantillas, categorías y límite desde la interfaz | Configuración persistida y plantilla aplicada al formulario |
| Computadora, tablet y celular | Formularios, odontograma táctil, archivos y visor PDF comprobados; teclado, Axe y ausencia de desborde horizontal |
| Respaldo completo | Archivo PostgreSQL generado y catálogo verificado; restauración integral reservada para fase 9 |

Los flujos nuevos se revisaron en 1440×900, 768×1024 y 390×844, además de la regresión de seis tamaños de fases anteriores. Quince capturas finales en frontend/docs/verification/phase3 registran odontograma, atención, imagen, PDF y comparación en los tres tamaños. Contienen exclusivamente datos sintéticos de la base de pruebas.

## Instalación local y conservación

Se aplicaron V6 y V7 a sistema_odontologo con Hibernate en modo validate, sin modificar migraciones anteriores. Se conservó una copia previa y se verificaron los mismos conteos antes y después: 2 pacientes, 2 citas, 2 usuarios, 1 odontólogo, 2 servicios y 1 logo. No se incorporaron las cuentas ni las fichas sintéticas de prueba a esta instalación.

El límite local inicial permanece en 20 MiB. El backend inicia en 8080 y el frontend en 5173. El mecanismo de respaldo produjo una copia posterior con las tablas clínicas y documentales; también se verificó sobre la base de pruebas que contiene originales y revisiones reales. Las copias, credenciales temporales e informes de ejecución se conservan en .runtime, excluido de Git. No se realizó ni se declara realizada la restauración integral.

## Correcciones comprobadas

Se reforzó el orden de los estados clínicos mediante una secuencia persistente por paciente y tipo, evitando ambigüedad cuando dos registros comparten instante. Se preservó la identificación del profesional en los estados y las revisiones finalizadas.

Se sustituyó la dependencia del visor PDF nativo por una vista autorizada de cada página; se comprobó que las páginas se decodifican y se ven en los tres tamaños. Se corrigieron la selección inicial del paciente desde su ficha y la versión inicial de formularios de plantillas y categorías. Los escenarios afectados se volvieron a ejecutar.

## Cobertura del alcance y límites

| Criterio | Cobertura de esta fase |
|---|---|
| A01 | Plantillas, categorías y política documental configurables; otras áreas en sus fases |
| A04 | Ficha administrativa y clínica integradas; tratamientos y cuentas pendientes |
| A06 | Historia clínica versionada y correcciones que preservan el original |
| A07 | Odontograma por pieza/superficie, dentición temporal e historial |
| A08 | Consentimiento documental y recuperación de su copia |
| A09 | Originales de imágenes/PDF almacenados y recuperados desde PostgreSQL |
| A10 | Tipos, tamaños, descarga autorizada y separación de metadatos; documentos financieros en fase 5 |
| A29 | Permisos y auditoría incorporados a clínica y documentación |
| A30 | Respaldo preparado y archivo verificado; restauración integral pendiente de fase 9 |
| A31 | Interfaz clínica, odontograma y documentos adaptados y verificados |
| A32 | Búsqueda, filtros, ordenación y paginación del servidor en los listados incorporados |

La fase 3 no genera cargos ni presenta el ciclo financiero como terminado. Los planes y tratamientos, y sus vínculos documentales, se incorporarán cuando existan en fase 4. Los archivos ya permiten paciente, atención y pieza. No incluye firma electrónica, visor radiológico especializado ni eliminación de originales.

La comprobación externa de WhatsApp sigue pendiente de los accesos de fase 6; A22 permanece pendiente. No se inició fase 4.

## Guía breve de revisión

1. Acceder como administrador; en Configuración revisar Plantillas clínicas, Categorías de archivos y Almacenamiento.
2. Abrir un paciente completo desde Pacientes mediante su expediente clínico, o seleccionarlo en Historia clínica. Elegir el odontólogo responsable.
3. Guardar antecedentes, crear una atención en borrador, finalizarla y añadir una corrección con motivo. Consultar sus versiones.
4. Registrar hallazgos de odontograma, guardar otro estado y alternar las fechas del historial.
5. Cargar una imagen y un PDF, visualizar sus páginas y descargar sus originales. Cargar otra fotografía y compararlas por fecha.
6. Registrar un consentimiento con su responsable y copia. Revisar Auditoría con los filtros de clínica y documentos.

Decisiones técnicas en [diseño clínico de fase 3](diseno-clinico-fase-3.md); respaldo y comprobación futura en [respaldo PostgreSQL](respaldo-postgresql.md).
