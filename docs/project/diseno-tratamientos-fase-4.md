# Decisiones de tratamientos y cargos — fase 4

Presupuesto y plan forman un único documento con correlativo configurable. Proponerlo no crea deuda; la aceptación explícita registra responsable y condiciones y genera un cargo por concepto en la misma transacción. Los conceptos aceptados conservan sus precios y sesiones; un adicional se agrega con motivo y un nuevo cargo, y los ajustes son movimientos separados.

Las sesiones se enlazan desde el procedimiento clínico al concepto aceptado. Su cantidad representa sesiones realizadas; la cantidad del presupuesto representa unidades facturadas. El servidor comprueba paciente, profesional, servicio, pieza, capacidad pendiente y estado. Finalizar la atención registra avance y cargos individuales en la misma transacción. Las revisiones clínicas posteriores no reescriben movimientos financieros; una diferencia económica requiere un ajuste explícito. No se generan cargos retroactivos de atenciones finalizadas antes de esta fase.

Los cargos y ajustes son inmutables y tienen una clave de origen única. Las operaciones del plan usan una clave UUID y huella del contenido; repetir una solicitud idéntica recupera el resultado y reutilizar su clave con otro contenido se rechaza. Se serializan escrituras por paciente, antes de bloquear el plan o la atención. La finalización clínica no adquiere primero otro bloqueo incompatible. La base conserva claves foráneas, unicidad e integridad monetaria.

Cancelar un plan no borra movimientos. Se puede conservar la deuda vigente o liberar el importe de sesiones pendientes mediante créditos proporcionales con dos decimales; se conserva el valor de las sesiones realizadas. Un ajuste nunca deja un concepto con valor negativo o inferior a la parte ya realizada. Terminar un plan requiere completar todas sus sesiones.

La vista inicial muestra cargos netos por moneda, con sus orígenes e historial; aún no registra dinero recibido, cuotas ni saldos después de pagos. Los importes se calculan con BigDecimal, sin usar coma flotante. Los presupuestos, conceptos, movimientos e historiales se filtran y paginan en PostgreSQL.

Se incorporan permisos de planes, consulta financiera y ajustes separados; clínica conserva su acceso independiente. Los documentos pueden vincularse al tratamiento del mismo paciente. Se reutilizan componentes, selectores y consultas existentes. No se añaden dependencias ni ampliaciones fuera del alcance.
