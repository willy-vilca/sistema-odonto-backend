# Cierre de fase 4 — presupuestos, planes y cargos

Fecha: 1 de octubre de 2026. Alcance de referencia: versión 1.2. Estado: completada técnicamente y disponible para revisión del usuario.

## Funciones entregadas

- Presupuesto y plan en un documento con correlativo configurable, paciente, profesional, procedimientos, piezas, unidades facturadas, sesiones previstas, importes y condiciones.
- Estados borrador, propuesto, aceptado, en curso, finalizado y cancelado. Presentar un presupuesto no genera deuda. Su edición lo devuelve a borrador; la aceptación explícita conserva responsable, fecha, condiciones e importe original.
- Aceptación y cargos por concepto en una misma transacción, con claves de operación y huellas de contenido. Repetir la aceptación no duplica cargos; reutilizar una clave con otro contenido produce conflicto.
- Servicios individuales con cargos al finalizar la atención, utilizando precio acordado o vigente del catálogo. Una finalización repetida conserva un solo cargo por procedimiento. Los procedimientos libres sin honorarios se registran con importe cero.
- Sesiones vinculadas a conceptos aceptados del mismo paciente y profesional, con servicio y pieza coincidentes. Registran avance sin otra deuda y no pueden superar la capacidad pendiente, incluso ante finalizaciones simultáneas.
- Historial paginado de operaciones y sesiones, con vínculos hacia la atención. Terminar el plan exige completar todas las sesiones; un adicional conserva el estado aceptado hasta comenzar una sesión.
- Adicionales autorizados con motivo y cargo propio. Ajustes con motivo, responsable y referencia al original, sin borrar movimientos ni permitir importes netos negativos.
- Cancelación con motivo: conservar deuda o liberar proporcionalmente las sesiones pendientes, manteniendo el valor de lo realizado. La liberación exige permiso de ajustes; el historial clínico y financiero permanece disponible.
- Precios y moneda conservados en acuerdos y cargos. Cambiar el catálogo no reescribe lo aceptado. Las correcciones clínicas no modifican los movimientos financieros; una diferencia económica requiere un ajuste explícito.
- Vista inicial de deuda por paciente y moneda, con cargos, ajustes, motivos, responsables, fechas, unidades y precio. Ver origen consulta el cargo y sus movimientos relacionados.
- Permisos de planes, consulta financiera y ajustes separados, auditados en el servidor. Recepción gestiona presupuestos; odontólogo gestiona planes y consulta deuda; caja consulta planes y deuda. Solo el administrador recibe ajustes por defecto.
- Enlaces desde la ficha del paciente y asociación de documentos con tratamientos del mismo paciente. Metadatos, contenido binario y recuperación a demanda mantienen la separación de fase 3.
- Búsqueda, filtros, ordenación autorizada y paginación en PostgreSQL. Diseño para computadora y adaptación completa a tablet y celular.

## Validación

Se aprobaron 54 pruebas distintas del servidor: 41 de regresión y 13 de fase 4, con PostgreSQL real en sistema_odontologo_test. La batería completa pasó; después del ajuste final del estado de adicionales se repitieron y aprobaron las 13 comprobaciones de fase 4 y la generación del ejecutable.

La batería completa de navegador aprobó sus 37 escenarios: 30 anteriores y siete nuevos. Compilación de producción, revisión estática y formato aprobados. Los flujos nuevos se comprobaron en 1440×900, 768×1024 y 390×844; la regresión conserva los seis tamaños anteriores.

| Escenario | Resultado |
|---|---|
| Guardar y presentar presupuesto | Sin deuda |
| Aceptar plan de S/ 1 200 | Un cargo por ese importe, junto con su aceptación |
| Repetir aceptación, incluso concurrentemente | Un solo cargo; misma clave con otro contenido rechazada |
| Completar una sesión y después las dos restantes | Avance 3/3; deuda sigue en S/ 1 200 |
| Intentar finalizar plan con sesiones pendientes | Rechazado |
| Dos atenciones intentan consumir la última sesión | Una aceptada y otra rechazada; un solo avance |
| Finalizar un servicio individual y repetir | Un cargo; corrección clínica no lo duplica |
| Tres unidades a S/ 0,10 | Cargo exacto de S/ 0,30 |
| Agregar concepto adicional de S/ 200 | Deuda de S/ 1 400; repetición sin duplicarlo; acuerdo original conservado |
| Modificar precio del catálogo | Plan, conceptos y cargos previos conservados |
| Ajustar y repetir el ajuste | Una variación referida al original; reducción incompatible rechazada |
| Cancelar plan de S/ 1 200 con una de tres sesiones realizadas | Crédito de S/ 800; deuda conservada de S/ 400 |
| Alterar o borrar movimientos y conceptos aceptados directamente | PostgreSQL rechaza la operación |
| Enlazar concepto de otro paciente o un plan no aceptado | Finalización rechazada sin revisión clínica ni movimientos parciales |
| Editar solo conceptos desde un formulario antiguo | Conflicto de versión; edición vigente conservada |
| Vincular archivo al tratamiento | Admitido para el mismo paciente; rechazado para otro |
| Operaciones anónimas, aceptación por caja y ajuste sin permiso | Denegadas por el servidor |
| Páginas excesivas y ordenación no autorizada | Rechazadas |
| Formularios, tablas, detalles y deuda en tres tamaños | Teclado, interacción táctil, Axe y ausencia de desborde comprobados |

Doce capturas sintéticas en frontend/docs/verification/phase4 conservan presupuesto, detalle, formulario y deuda en los tres tamaños. Las evidencias visuales anteriores permanecen conservadas en sus carpetas.

## Instalación y respaldo

V8 incorpora planes, conceptos, operaciones, sesiones, registro inmutable de cargos, permisos y vínculos documentales; V9 protege el versionado de cambios del detalle. No se modificaron migraciones ya aplicadas.

Se guardó respaldo previo a la actualización de sistema_odontologo. Antes y después se verificaron los mismos conteos: 2 pacientes, 2 citas, 2 usuarios, 1 odontólogo, 2 servicios, 1 logo y ninguna atención, revisión o documento clínico. No se incorporaron las cuentas ni registros sintéticos de prueba a la instalación local.

El backend actualizado inicia en 8080 y el frontend en 5173. Se generaron y verificaron respaldos completos de la instalación y de la base de pruebas, incluyendo treatment_plan, charge_entry, documentos y revisiones. Los archivos de respaldo e informes quedan en .runtime, excluido de Git. La restauración integral permanece pendiente de fase 9; A30 no se declara cumplida.

## Correcciones y mejoras

Se incluyeron los cambios exclusivos del detalle y el avance en la versión del plan, evitando sobrescrituras desde formularios antiguos. Se corrigió el envío JSON de los nuevos formularios mediante el mecanismo HTTP compartido. Se ajustó la selección del elemento visible en la comprobación móvil del odontograma y se ejecutó la regresión completa con los archivos de interfaz estables.

Se añadió historial consultable de sesiones, acceso a la atención exacta desde los movimientos, vínculo documental con tratamiento y agrupación de deuda por moneda. Los adicionales conservan el estado aceptado hasta comenzar el trabajo clínico.

## Cobertura y límites

| Criterio | Cobertura |
|---|---|
| A04 | Ficha integrada con planes y deuda; pagos y saldo después de abonos en fase 5 |
| A11 | Presupuestos, aceptación, sesiones, avance y estados |
| A13 | Valores acordados y cargos conservados frente a cambios de catálogo |
| A15 | Cargo individual al finalizar el servicio realizado |
| A16 | Cargo al aceptar un plan; sesiones sin segunda deuda |
| A18 | Ajustes de cargos y cancelación auditables; pagos, descuentos y devoluciones en fase 5 |
| A24 | Reintentos de aceptación, adicionales, ajustes y finalización sin duplicaciones |
| A29 | Permisos y auditoría incorporados a tratamientos y deuda |
| A30 | Respaldo preparado; restauración integral pendiente |
| A31 | Pantallas y flujos de esta fase adaptados y revisados |
| A32 | Listados e historiales procesados en el servidor |

Esta fase muestra deuda generada y sus ajustes. Pagos, anticipos, cuotas, constancias de cobro, egresos y caja corresponden a fase 5. No se generan cargos retroactivos por atenciones finalizadas antes de fase 4. Las correcciones clínicas conservan el historial financiero y los cambios económicos se registran explícitamente.

WhatsApp real continúa pendiente de los accesos externos de fase 6; A22 permanece pendiente. No se inició fase 5.

## Guía de revisión

1. Abrir Presupuestos y planes, seleccionar un paciente completo y crear un presupuesto de S/ 1 200 con tres sesiones.
2. Presentarlo y verificar que todavía no genera deuda. Aceptarlo indicando responsable, motivo y aceptación explícita.
3. Consultar Finanzas: comprobar S/ 1 200 y su origen.
4. Crear una atención con un procedimiento vinculado al concepto aceptado; finalizarla y revisar avance 1/3 sin aumentar la deuda.
5. Añadir un concepto adicional autorizado, consultar sus cargos y registrar un ajuste con motivo.
6. Revisar historial, permisos de caja y recepción, vínculo documental y pantallas desde celular.

Consultar [diseño de tratamientos](diseno-tratamientos-fase-4.md) y [respaldo PostgreSQL](respaldo-postgresql.md).
