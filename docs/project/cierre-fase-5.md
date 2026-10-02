# Cierre de fase 5 — pagos, cuotas, saldos, egresos y caja

Fecha: 2 de octubre de 2026. Alcance de referencia: versión 1.2. Estado: completada técnicamente y disponible para revisión del usuario.

## Funciones entregadas

- Abonos con importe exacto, fecha, medio, referencia, responsable y constancia. Aplicación a varios cargos; anticipos pendientes de aplicar sin duplicar ingresos.
- Cuenta por paciente y moneda: deuda generada, recibido neto, aplicado, pendiente y anticipo, calculados desde movimientos. Ficha del paciente enlazada a la cuenta.
- Calendarios de cuotas por cargo, vencimiento, pagado y estado. Distribuyen el cargo neto completo; no generan deuda. El avance se calcula desde aplicaciones, por vencimiento. Reprogramar conserva el calendario anterior; un cambio económico marca el calendario para revisión.
- Descuentos, anulación de cargo, liberación de aplicaciones, devolución parcial/completa y reversión de pago vigente, con motivo y auditoría. Los originales y sus referencias se conservan.
- Claves y huellas de solicitudes. Repeticiones no duplican movimientos, aplicaciones, cuotas, arqueos ni constancias; otro contenido con una clave usada produce conflicto.
- Egresos con categorías configurables, concepto, fecha, importe, medio, proveedor opcional y sustentos. Reversión de egresos conserva historia y categoría acordada.
- Una caja abierta por instalación; fondo inicial, esperado, contado y diferencia. El efectivo se separa de otros medios. Egresos/devoluciones no superan efectivo disponible. Una caja cerrada queda inmutable; correcciones posteriores usan una caja abierta.
- Constancias, estados de cuenta y arqueos PDF guardados al emitir, con identidad, logo, correlativo y detalle histórico. Los estados incluyen cargos, dinero, aplicaciones y cuotas. Cambiar identidad o configuración no reescribe los originales.
- Sustentos JPG/JPEG, PNG, WebP y PDF en PostgreSQL, con validación del módulo documental y límite configurado. Metadatos y contenido separados; vista por página, zoom y descarga a demanda con permisos y auditoría.
- Permisos independientes de consulta, cobros, egresos, caja, categorías y ajustes. Caja no recibe acceso clínico por consultar un sustento financiero. Listas con búsqueda, filtros, ordenación validada y paginación del servidor.

## Validación

77 pruebas del backend aprobadas en la regresión completa; 24 escenarios financieros en la comprobación final. 48 escenarios de navegador aprobados en la regresión conjunta, incluidos 11 de fase 5. La comprobación final de los 24 escenarios backend financieros cubre también el reinicio de correlativos. La comprobación final de los 11 escenarios financieros también pasó tras añadir zoom. Compilación, análisis estático y formato del frontend verificados. Las pruebas usan exclusivamente sistema_odontologo_test; nunca limpian la base principal.

| Escenario | Resultado |
|---|---|
| Deuda S/ 1 200, abonos S/ 300 + S/ 200 | S/ 500 recibidos/aplicados y S/ 700 pendientes |
| Aplicar S/ 300 de anticipo y repetir | Recibido sigue S/ 300; una aplicación |
| Dos solicitudes consumen el mismo anticipo | Una aceptada; otra rechazada |
| Dos cobros de S/ 800 sobre cargo S/ 1 200 | Uno aceptado; otro rechazado; S/ 400 pendientes |
| Aplicar un abono a varios cargos | Distribución validada por saldo, paciente y moneda |
| Reiniciar correlativo de una constancia/estado ya emitido | Rechazado sin cobro parcial; exige nuevo prefijo o número |
| Repetir pago o corrección | Mismo resultado; otro contenido con la clave produce conflicto |
| Devolver dinero aplicado sin liberar | Rechazado; liberar lo necesario permite devolver |
| Revertir pago S/ 300 | Original conservado, aplicaciones liberadas, cuenta recalculada |
| Distribuir cuotas y reemplazar calendario | Sin cargos adicionales; calendario anterior consultable |
| Descuento/anulación o cancelación de plan pagado | No permite deuda inferior a aplicaciones; operación original conservada |
| S/ 0.10 + S/ 0.20 | S/ 0.30 exactos |
| Caja: fondo S/ 100 + efectivo S/ 300 − egreso S/ 50 | Esperado S/ 350; transferencia S/ 200 separada |
| Contado S/ 345 | Diferencia −S/ 5; cierre y PDF conservados |
| Cambiar identidad después de emitir PDF | Binario histórico idéntico |
| Sustento PDF válido; archivo no admitido o excesivo | Recuperación autorizada; rechazos 400/413 |
| Listas documentales | Metadatos sin contenido binario |
| Caja intenta descuento o documento clínico por URL | Acceso denegado por el servidor |
| PDF con logo, acentos y nombres largos | Varias páginas, texto legible y sin recortes |

Navegación, cobro, cuotas, correcciones, egresos, cierre y PDF comprobados en 1440×900, 768×1024 y 390×844, con controles táctiles, teclado y comprobaciones de accesibilidad. Se conservan 21 capturas de fase 5 en frontend/docs/verification/phase5; también un render de exportación extensa. Revisión visual de cuenta, formularios, caja y documentos.

## Actualización local y respaldo

Migraciones V10 y V11 aplicadas con PostgreSQL real. Los conteos anteriores y posteriores permanecen iguales: 2 pacientes, 2 citas, 2 usuarios, 1 atención, 1 documento, 0 planes y 1 cargo. Las nuevas tablas monetarias/caja quedan vacías en la base principal; las demostraciones no introducen pacientes ni cobros ficticios allí. Salud del servidor local: UP.

Respaldo previo: 500 239 bytes. Respaldo posterior de la instalación: 532 639 bytes. Archivo de demostración aislada: 445 775 bytes. Catálogos verificados con versiones clínicas, BYTEA, cargos, dinero, aplicaciones y caja; SHA-256 registrados en la ejecución local. Copias excluidas de Git. La restauración integral sigue pendiente de fase 9 y A30 no se considera cumplido todavía.

## Decisiones y límites

Medios iniciales: efectivo, transferencia, tarjeta y otro. Sin conversión de monedas ni intereses. Efectivo usa la moneda de caja y fecha actual; otros medios admiten fechas anteriores sin futuro. Las cuotas distribuyen el cargo neto completo, incluyendo lo ya pagado; son un calendario de vencimientos, no un segundo cargo. Las correcciones monetarias están inicialmente reservadas a administración y los permisos son configurables.

Los documentos son constancias internas, sin facturación electrónica ni contabilidad formal. Exportaciones consultadas por lotes de 100 con límite de 5000 movimientos por clase. La IA no registra ni verifica pagos y su integración real continúa pendiente de accesos externos de fase 6; A22 permanece pendiente.

Criterios cubiertos en este módulo: A17, A18, A19, A20, duplicación financiera de A24 e información financiera de A04. Permisos/auditoría y adaptación de pantallas aportan a A29/A31; la revisión integral final se conserva en fase 9. Decisiones en [diseño financiero](diseno-financiero-fase-5.md).
