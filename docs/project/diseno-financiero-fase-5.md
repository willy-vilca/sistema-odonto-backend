# Diseño financiero · Fase 5

Se extiende el módulo finance por capas con movimientos inmutables de dinero, aplicaciones firmadas a cargos y operaciones identificadas por clave y huella. Los cargos conservan su origen; el saldo pendiente es deuda menos aplicaciones vigentes. El dinero disponible es lo recibido menos devoluciones, reversiones y aplicaciones. Aplicar un anticipo no registra otro ingreso.

Los pagos y ajustes bloquean primero al paciente. Las operaciones de efectivo bloquean después el registro único de caja y finalmente el perfil cuando necesitan un correlativo. Ninguna operación toma esos bloqueos en orden inverso. La caja cerrada conserva su arqueo; correcciones posteriores se registran en una caja abierta. Los ajustes que reducirían deuda por debajo de dinero aplicado se rechazan: primero debe liberarse la aplicación o devolverse el dinero.

Las cuotas distribuyen el cargo neto y su avance se calcula desde aplicaciones, en orden de vencimiento. No crean cargos. Los cambios de deuda requieren reorganizar el calendario y se muestran por separado; el historial de calendarios se conserva.

Se reutilizan validadores y vistas previas del módulo documental para sustentos financieros, con metadatos y binarios separados y permisos financieros independientes del expediente clínico. Los comprobantes internos y estados de cuenta se generan como PDF conservados en PostgreSQL: identidad, logo, correlativo y detalle quedan congelados al emitirlos. Todas las listas se consultan por páginas con filtros y búsqueda.

Riesgos prioritarios: solicitudes repetidas, cobros simultáneos, aplicación superior a deuda o anticipo, reversión parcial, descuentos sobre cargos cobrados y modificación de cajas cerradas. Se cubren con bloqueos, referencias originales, restricciones de persistencia, auditoría transaccional y pruebas de integración.

## Reglas operativas adoptadas

- Medios iniciales: efectivo, transferencia, tarjeta y otro. Los importes se conservan por moneda; no se realiza conversión de divisas. Los movimientos en efectivo usan la moneda de la caja abierta y la fecha actual del consultorio. Otros medios admiten fechas anteriores, sin futuro.
- Una caja abierta por instalación. Su fondo inicial no es ingreso del paciente. Los otros medios se incluyen en el periodo, pero no aumentan el efectivo esperado. Los egresos y devoluciones en efectivo no superan lo disponible.
- Una aplicación positiva reduce deuda y anticipo disponible; una liberación hace lo contrario sin devolver dinero. Una devolución registra salida real y libera los cargos seleccionados cuando es necesario. La reversión es completa sobre el importe vigente y libera sus aplicaciones. Caja registra abonos y egresos; administración conserva los permisos iniciales de corrección. Los roles siguen siendo configurables.
- Las cuotas distribuyen el cargo neto completo, incluyendo importes ya pagados. Se ordenan por vencimiento y posición. El dinero aplicado cubre primero los primeros vencimientos; no se emiten intereses ni nueva deuda. Una variación de cargo marca su calendario para revisión. Programar otro conserva el anterior como histórico.
- Un descuento o anulación financiera conserva el servicio y el cargo original. La cancelación clínica de un plan sigue siendo una operación diferente. Ambas rechazan reducir deuda por debajo de pagos aplicados.
- Cada solicitud de cobro, aplicación, corrección, cuotas, gasto, apertura/cierre y estado de cuenta utiliza una clave estable y huella. Repetirla conserva el mismo resultado; otro contenido con la misma clave produce conflicto.
- Constancias y estados de cuenta tienen correlativo del perfil; su PDF se guarda al emitirlo, incorporando identidad y logo vigentes. Un arqueo también genera su PDF al cerrar. Las futuras configuraciones no reescriben documentos emitidos. Los archivos generados y sustentos tienen metadatos paginados y contenido separado. Se usa Noto Sans con licencia OFL para nombres y acentos; el PDF se divide en páginas y envuelve textos largos.
- Exportaciones autorizadas se consultan internamente por lotes de 100, con límite explícito de 5000 movimientos por clase. Los listados de pantalla siguen solicitando únicamente la página actual, de hasta 100 registros.

## Implementación y comprobación

Nuevas tablas y vista de saldo en migraciones V10 y V11; capas controller/dto/service/repository/model dentro de finance. DocumentValidator y el renderizador seguro de páginas PDF se comparten con documents. finance separa consulta de cuentas, pagos, egresos, caja, cuotas, operaciones y generación documental. La interfaz separa modelos, servicios HTTP y componentes de formularios/listas, reutilizando selectores, tablas, campos, modales y hooks.

Las consultas de resumen y movimientos utilizan una instantánea consistente; las escrituras financieras se serializan por paciente y/o registro de caja. Movimientos, aplicaciones, operaciones, filas de cuotas, metadatos y binarios son inmutables en PostgreSQL. Los calendarios solo permiten quedar históricos y las cajas cerradas bloquean cambios. Las pruebas comprueban estas reglas con PostgreSQL real, concurrencia, permisos directos, archivos y los tres formatos de pantalla.

El asignador de correlativos toma el bloqueo de escritura del perfil y comprueba todos los PDF generados, incluidos estados y arqueos. Reiniciar un número que ya se emitió se rechaza antes de guardar el movimiento; la transacción conserva el saldo y el contador. Esto también evita duplicación entre clases distintas de constancia.
