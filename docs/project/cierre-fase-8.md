# Cierre de fase 8: panel, reportes y exportaciones

Revisión del usuario del 10/10/2026: favorable. Sus ajustes posteriores de cargos, auditoría y atención rápida están implementados y verificados en [la guía de revisión](cargos-auditoria-atencion-rapida.md), alcance 1.12 y plan 3.14. Los resultados de este cierre se conservan como evidencia propia de fase 8.

10/10/2026. Alcance 1.11, plan 3.13. Fase completada técnicamente y verificada, pendiente de revisión del usuario. Los recordatorios automáticos/A28 permanecen aplazados a una implementación posterior por su instrucción expresa; no se implementaron ni se consideran cumplidos. Fase 9 no iniciada.

## Funciones incorporadas

- Panel como primera página después de iniciar sesión, con citas de la fecha elegida, pacientes en espera, conversaciones pendientes de recepción, cuotas vencidas, cobros del periodo y accesos habituales según permisos.
- Movimiento financiero por moneda: recibido, devoluciones/reversiones, cobros netos, egresos netos y flujo neto de caja. Gráfico por día, semana o mes, con datos del servidor y valores consultables con teclado o toque. Saldos y anticipos separados a fecha de corte.
- Nueve reportes: citas, inasistencias, presupuestos/avance, servicios realizados, cobros, cargos emitidos, egresos, saldos y cuotas. Periodo, profesional cuando corresponde, paciente, moneda, medio, estado, búsqueda, ordenación y paginación remotos.
- CSV y PDF del resultado filtrado completo, con límites de 10 000 y 500 filas respectivamente; consulta por grupos en el servidor y control de permisos/auditoría. PDF con identidad, filtros, totales, detalle y numeración de páginas; constancias internas existentes a demanda.
- Navegación compartida de paciente entre ficha, clínica, archivos, tratamientos, agenda y cuenta. Enlaces directos a citas, planes y atenciones desde reportes. Calendario y lista filtrables por paciente, incluidas las listas de un día.
- Anchos mínimos en reportes extensos y desplazamiento contenido en computadora/tablet; tarjetas en celular. Se mantiene la terminología y la interfaz de conversaciones anterior.

## Reglas verificadas

Los presupuestos no se presentan como ingresos. La aceptación genera cargos y las sesiones de plan no duplican deuda. El trabajo se obtiene de la última versión clínica finalizada, sin sumar varias correcciones de una atención. Los precios aceptados y cargos originales no cambian al editar el catálogo.

Cobros netos = pagos menos devoluciones y reversiones. Egresos netos = egresos menos reversiones. Flujo neto = cobros netos menos egresos netos, para todos los medios; no es utilidad ni saldo contado de caja. Aplicar un anticipo no genera otro ingreso. Los pagos que abarcan varios profesionales no se atribuyen completos a cada uno; los filtros profesionales se ofrecen en los reportes con origen inequívoco.

Saldos: deuda y aplicaciones registradas hasta el final elegido, incluyendo deuda anterior al comienzo del periodo. Aplicaciones/ajustes registrados posteriormente no alteran un corte anterior. Las cuotas se calculan sobre el cargo y los pagos ya existentes; al seleccionar un corte anterior se usa el cronograma que existía entonces, no su reemplazo actual. Las distribuciones que no coinciden con un cargo ajustado se identifican para revisión.

La zona horaria del consultorio define los límites del día. Los periodos de consulta se acotan a 366 días; en cuotas puede incluirse deuda con vencimiento anterior mediante una opción explícita. El aviso de cuotas vencidas incluye todos los vencimientos anteriores, conservando ese filtro al abrir el reporte.

## Permisos y persistencia

V23 incorpora REPORTS_READ y REPORTS_EXPORT a los roles iniciales e índices para las consultas. Cada reporte requiere también el permiso del módulo: recepción no puede abrir o exportar finanzas o trabajo clínico por cambiar la URL. El panel permanece disponible por áreas aunque un rol no tenga permiso de reportes. Exportar requiere consulta y exportación; la política de roles valida esa dependencia.

No se crea otro libro contable ni se alteran los movimientos, documentos o citas. Las consultas y exportaciones sensibles se auditan. CSV protege textos que puedan interpretarse como fórmulas, conservando los importes numéricos. Los binarios de constancias solo se recuperan cuando se solicita su descarga.

## Evidencia

Backend: **232 pruebas aprobadas en la regresión completa**, sin fallos, errores u omisiones; incluyen 19 casos de esta fase. Se comprobaron importes exactos, presupuesto/aceptación, servicio individual, sesiones y corrección clínica, pagos/anticipos, devolución, reversión completa de egreso y registro corregido, monedas, cuotas, cortes históricos, reemplazos de cronogramas, actividad de planes anteriores, límites, búsquedas literales, paginación, autorización y exportaciones. Empaquetado correcto.

Navegador: **76 escenarios distintos aprobados en bloques**: ocho de la fase y 68 de regresión de configuración, agenda, clínica, archivos, tratamientos, cobros, caja y supervisión del agente. Computadora 1440 px, tablet 768 px y celular 390 px, además de navegación inicial en 1280, 1024 y 360 px. Los nuevos recorridos compararon deuda de 1400, cobros de 500, egresos de 80 y flujo neto de 420 en PEN; una sesión incluida no generó un cargo nuevo. Se descargaron CSV/PDF y constancias, se aplicaron filtros, se revisaron los nueve reportes y se recorrieron las áreas del mismo paciente. Una cuenta de recepción entró primero al panel y tuvo tres reportes administrativos, con rechazo directo a finanzas y servicios clínicos.

Axe no detectó infracciones A/AA en las vistas comprobadas; no constituye una certificación del producto completo. TypeScript, build, lint y formato aprobados. Se inspeccionaron los PDF renderizados y las capturas de la interfaz. El build conserva el aviso de tamaño del paquete, sin afectar su compilación.

Las primeras ejecuciones detectaron un alias reservado en la consulta del gráfico y ajustes necesarios en los conjuntos de prueba (presentar antes de aceptar, endpoint de partidas, reversión completa de egreso y codificación Unicode de psql en Windows). Se corrigieron y se repitieron los casos. No se presentan los intentos iniciales como aprobados.

Las pruebas destructivas y de interfaz se ejecutaron exclusivamente sobre sistema_odontologo_test, con claves ficticias y trabajadores externos detenidos. No hubo envíos al Sandbox, llamadas al modelo, compras ni cambio de credenciales. Las capturas nuevas están en frontend/docs/verification/phase8; las anteriores se conservan como históricas. La instalación de demostración tenía 11 pacientes, 15 citas, nueve cargos y 11 movimientos antes de la actualización, y se comprueba su conservación al aplicar V23.

Actualización local comprobada: respaldo PostgreSQL con contenido clínico/financiero generado antes de iniciar el nuevo backend; V23 aplicada, servidor UP y frontend HTTP 200. Se conservan 11 pacientes, 15 citas, nueve cargos, 11 movimientos, cinco contenidos documentales clínicos y 14 financieros. No hay ejecuciones del agente pendientes. Estado observado: AUTO/generación 23 y política versión 3, habilitada y con jornadas vacías. La generación 21 corresponde al cierre histórico anterior y no se utiliza como estado actual. Las claves privadas siguen ignoradas por Git. Al aplicar V23 se renueva el acceso de los usuarios; si la sesión anterior vence, iniciar sesión nuevamente.

## Uso y revisión del usuario

1. Inicia sesión y verifica el panel. Consulta el día 20/10/2026 y Julia Huaytalla para revisar las citas familiares de la demostración, si conservas los datos anteriores.
2. Cambia el periodo financiero con Hoy, Este mes, Últimos 30 días o las fechas. Comprueba por separado cobros, egresos, flujo neto y saldo acumulado.
3. Abre Reportes. Selecciona cada tipo, ajusta periodo y filtros y consulta varias páginas. Los totales pertenecen al resultado completo, no solo a la página.
4. Exporta CSV y PDF con una búsqueda activa; compara filas y totales. Desde Cobros, descarga una constancia interna existente.
5. En Saldos, utiliza la fecha final como corte. En Cuotas, activa Incluir cuotas anteriores al periodo para revisar vencimientos previos; no sumes estos importes como deuda adicional.
6. Abre una ficha y recorre Clínica, Archivos, Tratamientos, Agenda y Cuenta. Debe conservarse el paciente y mostrarse solo lo permitido para el usuario.

No necesitas borrar la base ni repetir los flujos clínicos/financieros completos para esta revisión. Los datos actuales siguen siendo ficticios y pueden utilizarse para la demostración. Los recordatorios quedan para una implementación posterior y el respaldo/restauración integral, instalación reproducible y aceptación conjunta corresponden a fase 9.
