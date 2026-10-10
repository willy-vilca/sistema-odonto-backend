# Panel, reportes y navegación del paciente

10/10/2026. Fase 8 autorizada. Desarrollo en main, manteniendo Kapso/Groq y los cierres anteriores. El usuario aplaza expresamente los recordatorios automáticos: no se implementan ni se consideran una condición de salida de este tramo.

## Enfoque

La ruta inicial muestra el panel operativo por permisos: citas del día, pacientes en espera, conversaciones pendientes de recepción, cuotas vencidas y cobros del periodo. Permite consultar otra fecha, revisar el periodo financiero y acceder a las tareas habituales. Los valores proceden del servidor; ausencia de registros produce ceros reales y falta de permiso omite el bloque.

Reportes separados de citas, inasistencias, presupuestos/avance de planes, servicios realizados, cargos, cobros, egresos, saldos y cuotas. Por profesional cuando los datos tienen atribución inequívoca: agenda, planes, procedimientos y cargos. Un pago puede aplicarse a cargos de varios profesionales o quedar como anticipo; los cobros y egresos se presentan por consultorio, sin atribuir el pago completo a cada profesional.

El backend incorpora un módulo de consultas de reportes y un panel, con controladores, contratos, servicios y repositorios. Reutiliza las tablas y movimientos existentes, sin crear un segundo libro financiero ni mutar citas/atenciones. Se añaden permisos de consulta/exportación y los índices pertinentes. React incorpora páginas, filtros y componentes con los tokens visuales actuales. La navegación del paciente comparte enlaces autorizados y conserva su identificación al pasar entre módulos.

## Reglas de cifras

- Presupuesto: importe ofrecido o aceptado de un plan; no es dinero recibido. Los valores aceptados se conservan y el estado actual del plan se identifica como tal.
- Trabajo realizado: procedimientos de atenciones finalizadas según su última versión clínica. Las sesiones de plan y los servicios individuales se distinguen. No se suman varias revisiones de una misma atención ni se valoran sesiones como cobros nuevos.
- Cargos emitidos: movimientos de deuda del periodo; cargos originales, ajustes y anulaciones separados, con importe neto. La vinculación profesional usa el origen del cargo.
- Cobros netos: pagos recibidos menos devoluciones y reversiones de pago del periodo. Los anticipos son ingreso al recibirse; sus aplicaciones no son dinero nuevo.
- Egresos netos: egresos menos sus reversiones del periodo. El flujo neto de caja es cobros netos menos egresos netos, para todos los medios; no se presenta como utilidad ni como saldo contado de caja.
- Saldos al cierre del periodo: cargos y aplicaciones registrados hasta la fecha de corte, incluyendo deuda anterior al inicio del periodo. Dinero pendiente de aplicar separado de deuda pendiente. Las monedas no se mezclan.
- Cuotas: vencimientos y distribución de pagos sobre cargos existentes, sin duplicar deuda. Se identifican cuotas que requieren revisión por ajustes del cargo.

Las fechas se interpretan en la zona horaria del consultorio. Los reportes acotan el periodo hasta un año (366 días); los saldos utilizan el final como corte. Los listados se paginan, filtran y buscan en el servidor. Los agregados se calculan sobre todo el resultado filtrado, no solo sobre la página visible.

## Exportación y permisos

CSV para análisis y PDF para compartir el reporte, generados desde el servidor con los mismos filtros y totales. Exportaciones acotadas y consultadas por grupos; si un resultado supera el límite se pide reducir periodo o filtros. Se neutralizan fórmulas en texto CSV. Las constancias internas PDF existentes se recuperan a demanda por sus referencias; no se cargan binarios con el listado.

Consultar o exportar un tipo de reporte requiere tanto su permiso de reportes como el del módulo correspondiente. Cambiar la URL no amplía derechos. El panel consulta únicamente las áreas autorizadas; las exportaciones y consultas sensibles se auditan sin credenciales ni información del modelo.

## Comprobaciones previstas

Conjunto controlado: presupuesto no aceptado, plan aceptado con sesiones, servicio individual finalizado y corregido, abonos, anticipo aplicado, devolución/reversión, egreso corregido y cuotas. Comparar cifras, cortes históricos, profesional, monedas, búsquedas literales, paginación y exportaciones. Denegar consulta/exportación directa sin derechos. Recorrer ficha, clínica, archivos, planes, agenda y cuenta en computadora/tablet/celular. Las regresiones destructivas usan solo sistema_odontologo_test; la instalación actual de demostración se conserva.

Diseño implementado y verificado el 10/10/2026: [cierre, resultados y límites](cierre-fase-8.md). Esta guía describe las decisiones; la evidencia y el estado final pertenecen al cierre.
