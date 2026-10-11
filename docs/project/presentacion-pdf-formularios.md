# Presentación institucional de documentos y formularios

Solicitud confirmada del 11/10/2026. Ajustes posteriores a la revisión favorable de cargos, auditoría y atención rápida. Trabajo en main, alcance 1.13 y plan 3.15. No inicia fase 9 ni implementa recordatorios.

## Plantillas fuera de la interfaz

Se retiran el catálogo de plantillas clínicas de Configuración y los selectores/aplicadores de plantillas de antecedentes, atenciones y consentimientos. Los permisos mantienen sus códigos y las categorías/documentos siguen configurándose. La API de plantillas, su almacenamiento, sus registros y sus validaciones permanecen sin cambios: este retiro es de interfaz, según la instrucción expresa del usuario. La navegación directa al catálogo retirado ya no habilita esa pantalla.

No se modifica el contenido clínico que una plantilla haya aportado a registros existentes. Los antecedentes, atenciones, versiones y consentimientos conservan su funcionamiento y sus originales.

## Documentos PDF

Una composición compartida presenta identidad del consultorio, logo, datos de contacto, encabezado configurable, título y correlativo cuando corresponde, información del documento, resumen y detalle tabulado. Tipografía incrustada con caracteres españoles, márgenes consistentes, números alineados a la derecha, dos decimales, separación de miles, filas alternas y encabezados repetidos al continuar tablas. Todas las páginas indican su número y total. Los mensajes institucionales cortos se integran al pie de la última página, evitando páginas vacías; los mensajes extensos conservan su espacio como sección. Se adapta la orientación y se separan las tablas de reportes con muchas columnas, conservando el número de registro para relacionarlas.

- Constancias de abono, devolución y reversión: paciente/código, fecha, responsable, medio, referencia, importe, aplicación o liberación y anticipo al emitir cuando corresponde. Se conserva su carácter de documento interno.
- Estado de cuenta: resumen histórico y tablas separadas de cargos/ajustes, pagos/devoluciones/reversiones, aplicaciones/liberaciones y cuotas. Identifica expresamente que incluye los cargos finalizados; los ajustes mantienen su signo. Las aplicaciones y cuotas no se presentan como ingresos o deudas adicionales.
- Arqueo de caja: apertura/cierre, responsables, fondo, efectivo esperado/contado, diferencia y otros medios. Detalle con columnas de entrada y salida y medio, separado del cálculo de efectivo.
- Los nueve reportes usan el mismo estilo institucional, sus filtros, período, responsable y totales. Se conservan permisos, resultados del servidor, límites de exportación y CSV.

Los cálculos monetarios, movimientos, correlativos y referencias no cambian por el rediseño. La lectura para estados y exportaciones continúa por páginas acotadas en el servidor. La preparación de datos financieros se separa de su almacenamiento y del trazado del PDF.

**Aplicación del diseño:** los documentos generados desde esta actualización usan el formato nuevo. Los PDF emitidos y almacenados anteriormente conservan sus bytes, identidad y contenido histórico. Descargar otra vez una constancia antigua devuelve ese original; repetir una solicitud no reemite su constancia ni duplica el movimiento. Los reportes descargados nuevamente, un estado nuevo y las constancias de operaciones nuevas permiten ver el nuevo formato. Para ver un arqueo nuevo, abrir/cerrar un turno nuevo.

## Campos obligatorios

Los campos obligatorios muestran un asterisco rojo junto a su etiqueta. Se reutiliza el indicador en entradas, áreas de texto, selectores y catálogos; los campos opcionales no se marcan por defecto. Las etiquetas accesibles y la navegación por teclado conservan sus nombres. No se añaden obligaciones al servidor para justificar un marcador.

La atención distingue las reglas: fecha y motivo son obligatorios para guardar; evolución y diagnósticos se marcan como necesarios **para finalizar** y se explica que el borrador puede guardarse sin ellos. Anamnesis e indicaciones siguen siendo opcionales. En pacientes, nacimiento depende de si la ficha es provisional; tipo y número de documento se completan juntos si se utiliza documento. Selecciones necesarias en citas, planes, copias de consentimiento y categoría de archivos/egresos también se marcan. Se conservan las validaciones del servidor, permisos, restricciones de fechas, importes y campos inmutables.

## Revisión y evidencia

Las comprobaciones usan la base protegida de pruebas, sin mensajes reales de WhatsApp ni inferencias del modelo. Se verifica conservación de constancias al cambiar identidad, importes exactos, documentos largos, cabeceras y márgenes en todas las páginas, ausencia de recortes y palabras finales preservadas en celdas que continúan. Las muestras incluyen abono, devolución, estado de cuenta, caja y reporte de tratamientos, con datos ficticios coherentes. La interfaz se revisa en computadora, tablet y celular con teclado, accesibilidad y guardado de borradores incompletos.

Resultado final del 11/10/2026: 248 comprobaciones backend aprobadas en la regresión completa y bloque final de tres pruebas de presentación tras los ajustes visuales; cero fallos/errores en los informes vigentes. 81 escenarios de navegador distintos aprobados en bloques: 59 de acceso/configuración/agenda/clínica/tratamientos/finanzas, cuatro nuevos de presentación, ocho de panel/reportes y diez de supervisión/chat. Las dos comprobaciones de auditoría del bloque común se repitieron después de ajustar sus selectores a acciones legibles y referencias únicas. Se corrigieron también las etiquetas accesibles con número de jornada y se volvió a verificar la política y conversación en los tres tamaños. Las ejecuciones aisladas de política sin conversación de prueba no se presentan como aprobadas.

Compilación, lint, formato, revisión visual de las cinco muestras y de páginas de continuación aprobados. Las muestras de abono, devolución y arqueo caben en una página; estado de cuenta y reporte de tratamientos tienen dos páginas, con sus cifras, encabezados y pie correctos. Fechas, correlativos e importes permanecen completos dentro de las celdas. Capturas en `frontend/docs/verification/presentation`; muestras intermedias de verificación en `frontend/.runtime/pdf-presentation`.

Respaldo previo a actualizar el ejecutable: `backend/.runtime/backups/sistema_odontologo-20261011-143730-978.dump`, SHA256 `3E69A4E9033F35ACB4C1073C34A0B800C39CABD53B2346CBBB9E8B82D5D9DAD7`. La instalación conserva 11 pacientes, 15 citas, 9 movimientos de deuda, 11 movimientos de dinero y 14 documentos financieros. Se compararon los contadores y la huella conjunta de las constancias antes/después: iguales. No hay migración de esquema ni modificación de las claves privadas. Backend local y frontend disponibles; el servidor auxiliar de pruebas se detiene al terminar. Esta guía sustituye la disponibilidad de plantillas en las guías históricas de interfaz; sus APIs conservadas y las evidencias anteriores no se borran.
