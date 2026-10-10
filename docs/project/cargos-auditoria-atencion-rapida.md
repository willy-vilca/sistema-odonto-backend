# Cargos activos, auditoría esencial y atención rápida

Solicitud del usuario: 10/10/2026. Trabajo en las ramas main de ambos repositorios, posterior a la revisión favorable de fase 8. Recordatorios y fase 9 siguen fuera de este tramo.

## Cuenta activa y cierre de cargos

La cuenta del paciente muestra primero los cargos abiertos. Se conserva el botón **Ver resumen histórico** para consultar todos los cargos y pagos anteriores. El estado de cuenta PDF y los reportes financieros conservan el historial completo: finalizar no es una anulación, devolución ni movimiento de caja.

En cada moneda, la cuenta activa presenta:

- Cargos activos: importe original más sus ajustes, solo de cargos abiertos.
- Dinero aplicado: aplicaciones netas a esos cargos abiertos.
- Saldo pendiente: cargos activos menos dinero aplicado.
- Anticipo disponible: dinero recibido neto histórico menos todas sus aplicaciones, incluidas las de cargos cerrados.
- Dinero en cuenta activa: aplicaciones a cargos abiertos más el anticipo disponible. El dinero aplicado a cargos finalizados se excluye una sola vez, aunque un mismo abono cubra varios cargos.

**Finalizar cargo** requiere saldo pendiente cero. Puede hacerlo quien registra pagos. Retira el cargo de la cuenta activa y de los selectores de pagos y anticipos. El selector también excluye los cargos abiertos ya pagados completamente para nuevas aplicaciones. La liberación de aplicaciones utiliza cargos abiertos aunque estén pagados.

Mientras está finalizado, el servidor impide ajustes, descuentos, anulaciones, nuevas cuotas, aplicaciones, liberaciones y devoluciones de su dinero aplicado. Una reversión completa que afecte un cargo finalizado también se rechaza. Se puede devolver un anticipo sin aplicar del mismo pago, conservando el dinero del cargo cerrado. **Reabrir cargo** requiere permiso de ajustes financieros y motivo; devuelve el mismo cargo y sus valores a la cuenta activa. No crea otra deuda ni otro ingreso.

El detalle del pago identifica el importe aplicado a cargos finalizados y explica cómo reabrirlos. Deshabilita reversión, devolución completa o liberación cuando afectan solo dinero protegido; una devolución parcial puede utilizar únicamente el dinero libre o aplicado a cargos abiertos.

El cierre/reapertura usa una tabla de estado separada. Los cargos, pagos, aplicaciones y constancias originales siguen inmutables. Las acciones son auditadas e idempotentes; repetir un cierre antiguo después de reabrir no vuelve a cerrarlo. Cierre, pagos, ajustes y correcciones se serializan por paciente; PostgreSQL también protege los cambios sobre cargos finalizados.

Después de pagar completamente uno o varios cargos, una notificación indica que pueden finalizarse desde **Cargos**. El cobro rápido de una atención sí los finaliza dentro de la misma operación confirmada.

## Auditoría esencial

La auditoría general registra cambios de configuración y permisos, pacientes, agenda, registros clínicos, documentos sensibles, tratamientos, movimientos de dinero, cierres/reaperturas, caja, decisiones de supervisión e incidencias relevantes. Se conservan descargas y visualizaciones de documentos sensibles y exportaciones autorizadas.

Las consultas de listas, saldos, panel y reportes, actualizaciones de borradores, entradas y estados rutinarios de mensajes, análisis concluidos y propuestas pendientes no generan nuevos registros generales. Mensajes, entrega, herramientas y propuestas mantienen su historial propio de WhatsApp/agente. Los registros generales antiguos no se borran; la consulta paginada aplica el mismo criterio esencial y oculta ese ruido histórico. Se muestran módulos comprensibles y referencias técnicas bajo **Ver referencia**.

## Atención rápida

1. En **Agenda**, abrir una cita y pulsar **Atender esta cita**. Se seleccionan paciente, odontólogo y cita, y se prepara su servicio con el precio vigente editable. Si la cita ya tiene atención, se abre esa atención; no se crea otra. Las citas canceladas o con ausencia no admiten este acceso de creación.
2. Completar anamnesis, evolución, diagnóstico, indicaciones y procedimientos. El profesional revisa el procedimiento preparado; puede cambiarlo o vincularlo a un plan aceptado. Guardar el borrador abre su detalle para continuar.
3. Elegir el recorrido:
   - **Finalizar atención** conserva el funcionamiento independiente habitual.
   - **Finalizar e ir a cobrar** finaliza y abre la cuenta del mismo paciente. **Registrar abono** se prepara con los cargos pendientes individuales de esa atención, su moneda y los importes. Permite un pago parcial, aplicar otro importe o conservar anticipos como en el módulo habitual.
   - **Finalizar y registrar cobro** pide fecha, medio y referencia, muestra el importe total pendiente de los cargos individuales y requiere confirmación. Finaliza la atención, emite los cargos, registra/aplica el pago, genera su constancia y cierra esos cargos en una sola transacción. Efectivo requiere caja abierta y fecha actual. También puede cobrarse una atención ya finalizada; sus cargos no se vuelven a emitir.
4. Una sesión incluida en un plan no genera deuda adicional. El cobro rápido la identifica y no vuelve a cobrar el total del plan: el saldo del plan se gestiona desde la cuenta del paciente. Una atención con solo sesiones incluidas utiliza **Finalizar e ir a cobrar**. Los cargos sin importe pendiente pueden cerrarse sin inventar un ingreso.

El cobro combinado exige permisos de escritura clínica, cobro y lectura financiera, y conserva la comprobación del profesional responsable. Un odontólogo sin permiso de cobro sigue finalizando clínicamente; caja continúa desde la cuenta. No se amplían los permisos del agente IA.

Si falla cualquier validación de cobro, no se aplican partes del flujo: el borrador, agenda, cargo, pago y constancia quedan en su estado anterior. Repetir la misma solicitud conserva una sola finalización y un único pago. Un nuevo intento con datos distintos exige revisar el saldo y la versión actual.

## Comprobación y uso

Validación automatizada en la base protegida `sistema_odontologo_test`, sin envíos a WhatsApp ni solicitudes al modelo. Pruebas de cierres, reaperturas, anticipos, bloqueos, concurrencia, atomicidad, permisos y ausencia de doble cargo por sesiones; recorrido de agenda a cobro en computadora, tablet y celular. Resultados finales: 245 comprobaciones backend distintas aprobadas en bloques (regresión completa de 241 y verificación final de 56 de finanzas/reportes, incorporando cuatro escenarios adicionales); cero fallos o errores en los informes actuales. 49 escenarios distintos de navegador aprobados: seis del flujo financiero, uno de auditoría en tres tamaños, treinta de clínica/tratamientos/finanzas, ocho de panel/reportes y cuatro de conversación. Compilación, lint, formato y revisión visual aprobados. Capturas nuevas en `frontend/docs/verification/finance-workflow`; se conservan las capturas históricas originales de los módulos.

Se corrigieron las expectativas de pruebas que utilizaban el rótulo anterior «Deuda generada». Los bloques de reportes y conversación se ejecutan sobre conjuntos protegidos independientes: mezclar sus datos de prueba altera los agregados o el estado de supervisión esperado. Las verificaciones finales anteriores ya usan esa preparación; no se presentan los intentos iniciales como aprobados.

Migración V24 aplicada en la instalación actual después del respaldo `.runtime/backups/sistema_odontologo-20261010-132440-034.dump`, SHA256 `7EA982395BD0026A6A471EDBF626AEA383F2823C10355A4AF8E778C033B8FA8D`. Conserva 11 pacientes, 15 citas, 9 movimientos de deuda, 11 movimientos de dinero, 5 contenidos clínicos y 14 contenidos financieros. Ningún cargo de la instalación se cerró automáticamente durante la migración: la nueva tabla empieza vacía. Cola de agente pendiente/en proceso: cero. Backend local y frontend disponibles; no se usaron envíos reales ni inferencias del modelo para estos ajustes.

Para revisar manualmente en la instalación actual, usa una cita pasada de un paciente con ficha completa. Abre **Atender esta cita**, guarda un borrador con evolución y diagnóstico y prueba uno de los dos recorridos de cobro. Para efectivo, abre antes la caja. Desde su cuenta consulta **Activos**, **Finalizados**, **Ver resumen histórico** y **Reabrir cargo**. No necesitas crear servicios ni volver a escoger paciente, odontólogo o cita en cada módulo.
