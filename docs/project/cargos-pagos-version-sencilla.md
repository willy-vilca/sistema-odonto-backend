# Estado de pago y acceso a abonos desde cargos

Ajuste autorizado el 12/10/2026 exclusivamente en `version_sencilla` de ambos repositorios. Instalación `sistema_odonto_mini`; regresiones destructivas únicamente en `sistema_odonto_mini_test`. Main y la instalación original permanecen separados.

## Funcionamiento

En Finanzas → Cuenta del paciente → Cargos, cada cargo original muestra total vigente, pagado y falta pagar. El total incorpora descuentos y ajustes; pagado es la suma neta de aplicaciones confirmadas, descontando liberaciones y devoluciones. Estados explícitos Sin pagar, Pago parcial, Pagado por completo y Sin deuda. Activo/Finalizado conserva un significado administrativo independiente. Ajustes y anulaciones siguen visibles como movimientos vinculados al origen, sin presentarlos como otra deuda ni otro pago.

Ver pagos abre una lista de los abonos que se vincularon a ese cargo, con búsqueda, medio, fechas y paginación en el servidor. Un abono repartido entre varios cargos conserva su importe y constancia original; la lista indica cuánto está aplicado actualmente al cargo consultado. Los abonos liberados o devueltos se conservan con aplicación neta cero para consultar su historia. El detalle reutiliza los datos, archivos, operaciones autorizadas y constancia PDF existentes; cerrar los diálogos devuelve al cargo sin perder filtros.

## Implementación

`GET /api/v1/charges` incorpora debt, applied y paymentStatus sobre la vista PostgreSQL financial_charge_balance. Consulta los saldos de los orígenes de la página en un único lote y mantiene una lectura consistente; no añade columnas monetarias editables ni migra tablas. Las filas de ajuste conservan importes propios y valores de resumen nulos.

`GET /api/v1/finance/movements` admite chargeId, valida su correspondencia con patientId cuando se suministran ambos y selecciona pagos mediante una subconsulta, sin duplicar pagos por varias aplicaciones. chargeApplied es la aplicación neta al cargo; se consulta por lote únicamente para los pagos de la página. El endpoint conserva FINANCES_READ y filtros/ordenación acotados. Consultar no modifica dinero, cargos ni constancias históricas.

La interfaz reutiliza la tabla, los diálogos y los datos financieros existentes. Los importes proceden del servidor y los estados incluyen texto, sin depender exclusivamente del color. Constancia PDF está disponible también desde el detalle del movimiento.

## Validación

Completada el 12/10/2026.

- 45 casos financieros focalizados aprobados, incluidos cinco nuevos de estado/importes, descuentos y deuda cero, reparto y paginación, devolución con historia y permisos/paciente incorrecto. Regresión completa posterior: **261 pruebas backend aprobadas**, sin fallos, errores ni omisiones. Compilación, formato y empaquetado correctos.
- **112 escenarios distintos de navegador aprobados** en bloques: 62 operativos y de navegación, 13 de versión sencilla/documentos, 8 de panel/reportes, 6 nuevos de cargos, 16 del agente/conversaciones, 3 del conector Kapso conservado y 4 del conector Twilio conservado. Los modos de conexión se probaron por separado, con claves ficticias y trabajadores externos desactivados. No se enviaron mensajes externos ni se llamó al modelo real durante las pruebas.
- Los seis casos nuevos verifican Sin pagar → Pago parcial con S/500 aplicados y S/700 pendientes sobre S/1 200 → Pagado → Finalizado; pagos asociados, parte exacta de un pago repartido, constancia y detalle en 1440/768/390 px; búsqueda y páginas en servidor; devolución con aplicación neta cero; cargo gratuito sin dinero inventado; fallo de carga y reintento. Axe, ausencia de desbordamiento global, teclado, Escape y restauración de foco comprobados. Las capturas propias se conservan en `frontend/evidence/charge-payments/`; las evidencias históricas anteriores se preservan.
- Los primeros intentos del archivo nuevo no se cuentan como aprobados: se corrigieron selectores de etiquetas y la inspección de una respuesta cancelada por React StrictMode. Una repetición coincidió con una ejecución todavía activa y produjo errores de preparación; se repitió de forma aislada. Los bloques finales se ejecutaron secuencialmente y pasaron completos. El resumen no suma reintentos como casos adicionales.
- Frontend compilado y revisión estática aprobada. No se añadieron dependencias ni migraciones. Los contadores de `sistema_odonto_mini` conservaron 2 pacientes, 5 citas, 3 cargos, 3 movimientos, 3 aplicaciones y 3 documentos financieros antes/después. Main conserva backend `240962adb6c384dba9449ad152b5705d0e10ff1a` y frontend `d5a7ed357dc9d6bd854f0a090d0cce2184cdeb22`; no se ejecutaron escrituras ni migraciones en la base original.
- Servidor de pruebas detenido al finalizar. Backend actualizado en 8080 con perfil local y `sistema_odonto_mini`; frontend en 5173. Archivos privados y configuración externa conservados. Las comprobaciones de conectores de este ajuste son simuladas y no se presentan como nueva evidencia de WhatsApp real.

Registro detallado: `backend/.runtime/charge-payments-focused.log`, `charge-payments-backend-regression.log`, `charge-payments-package.log`, `charge-payments-validation-summary.json`; `frontend/.runtime/charge-payments-{core,mini-presentation,reports,new-final,agent,kapso,twilio}-e2e.log`. Fase 9 y recordatorios siguen pendientes.
