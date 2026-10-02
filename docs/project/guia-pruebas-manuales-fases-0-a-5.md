# Guía secuencial de pruebas y demostración: fases 0 a 5

Versión 1.0 · 02/10/2026 · Alcance 1.3 · Plan 1.8.

Recorrido de todas las funciones implementadas hasta fase 5, reutilizando cinco pacientes. Incluye datos para copiar, acciones y resultados esperados. **Las pruebas manuales están pendientes hasta que las ejecutes.** Marca cada bloque en [el registro de resultados](registro-revision-manual.md). Si un saldo no coincide, detente y registra el primer paso que falla; los siguientes dependen de él. Un rechazo previsto es correcto si conserva los datos y no deja operaciones parciales.

## Preparación

### 01. Acceso y condiciones iniciales

Abre http://localhost: 5173 con backend y PostgreSQL ejecutándose. Entra con **tu administrador actual**, comprueba conexión y navegación. Cierra sesión, intenta contraseña incorrecta y vuelve a entrar con la correcta. Después de salir y recargar una página protegida no deben entregarse datos privados.

Los datos de esta guía son ficticios. Si existen de un intento previo, continúa desde tus notas. Para repetir el recorrido completo utiliza una instalación de demostración independiente o un conjunto nuevo identificado DEMO 2, con las mismas cantidades. Repetir abonos como operaciones nuevas sobre las mismas cuentas alteraría los resultados.

Anota la configuración original antes de modificarla. No elimines datos anteriores. La caja debe estar sin movimientos ajenos a este recorrido: si ya hay una caja abierta con datos, utiliza una instalación de prueba independiente o contabiliza sus valores por separado; no cierres la caja existente solo para seguir esta guía.

Trabaja como administrador salvo cuando se indique otro rol. En el paso 40 hay comprobaciones de roles que puedes incorporar al recorrido, sin repetir registros. En los catálogos, Añadir abre creación; Editar abre modificación y Guardar cambios confirma. En planes, el botón Presentar presupuesto deja el estado Propuesto. Cada motivo indicado se introduce en Motivo de la operación, salvo que el formulario tenga una etiqueta específica.

| Símbolo | Valor que debes usar | Ejemplo si comienzas el 02/10/2026 |
|---|---|---|
| H | Fecha real de hoy en America/Lima; suele aparecer en el formulario | 2026-10-02 |
| H−1 | Ayer | 2026-10-01 |
| H+15 | Quince días después de H | 2026-10-17 |
| H+30 | Treinta días después de H | 2026-11-01 |
| F | Próximo lunes todavía futuro | 2026-10-05 |
| F+7 | Lunes siguiente a F | 2026-10-12 |
| T | Hora disponible de hoy, al menos 15 minutos después de ahora | Si son las 10:05, usa 10:30 |

**Solo H, F y T dependen del momento de prueba.** No copies las fechas de ejemplo si ya pasaron. El navegador puede mostrar fechas día/mes/año; aquí se expresan año-mes-día. Importes sin separador de miles: 1200.00.

La atención vinculada a una cita y la inasistencia solo se registran una vez alcanzado su inicio. Mientras llega T avanza con presupuestos y antecedentes. Realiza estos casos durante el día, con margen para una cita de 60 minutos antes de acabar la jornada. Si continúas otro día, actualiza H en las operaciones nuevas, sin cambiar las ya registradas.

### Archivos incluidos

Están en **docs/datos-prueba**; PDF en **docs/datos-prueba/output/pdf**. Las imágenes son diagramas ficticios, no fotografías ni evidencia clínica.

| Archivo | Uso |
|---|---|
| logo-demo.png | Logo |
| registro-inicial.jpg | Primera imagen del expediente de María, JPEG |
| registro-posterior.png | Segunda imagen, PNG y comparación |
| registro-posterior.webp | Tercer formato admitido |
| informe-maria-demo.pdf | PDF de dos páginas de María |
| consentimiento-lucia-demo.pdf | Copia ficticia del consentimiento de Lucía |
| sustento-egreso-demo.pdf | Sustento ficticio de egreso 50.00 |
| imagen-limite.png | Imagen válida de aproximadamente 1.76 MiB para un límite temporal de 1 MiB |
| formato-no-admitido.txt | Rechazo de formato |
| archivo-disfrazado.jpg | Texto con extensión JPG: rechazo por contenido |

Los PDF están marcados como ficticios; no constituyen autorización real, factura ni diagnóstico. Las fechas del caso se registran en los formularios.

## Configuración y usuarios

### 02. Identidad del consultorio

En **Configuración → Consultorio** (los colores se eligen con su control de color; puedes introducir el valor hexadecimal):

| Campo | Valor |
|---|---|
| Nombre del consultorio | Clínica Sonrisa Integral |
| Nombre legal o del profesional | Consultorio Odontológico Sonrisa Integral |
| Dirección | Av. Los Jardines 245, oficina 302, Lima |
| Teléfono | +51900001000 |
| Correo electrónico | contacto@sonrisa.example |
| Color principal | #215e4d |
| Color de fondo destacado | #edf2e9 |
| Moneda | PEN |
| Zona horaria | America/Lima |
| Formato de fecha | Opción día/mes/año |
| Anticipación mínima (minutos) | 0 |
| Separación entre citas (minutos) | 0 |
| Encabezado de documentos | Atención odontológica integral |
| Pie de documentos | Constancia interna de demostración. Gracias por confiar en Sonrisa Integral. |
| Indicaciones para las citas | Llegar 10 minutos antes y traer documento de identidad. |

Conserva los **siguientes números** actuales de pacientes, constancias y presupuestos. Puedes usar prefijos D-PAC, D-REC y D-PRE; no reinicies números utilizados. Si el control de moneda o fecha está configurado de otra manera, selecciona PEN y la opción día/mes/año. Guarda y carga **logo-demo.png**. Comprueba identidad/logo al recargar. Puedes retirar y volver a cargar el logo ahora, antes de emitir documentos históricos.

Prueba correo incorrecto y zona horaria inexistente: no deben guardarse. Restablece valores válidos. Nombre, colores y textos deben poder configurarse sin modificar código.

### 03. Cuentas y roles

En **Configuración → Usuarios**, crea cuentas activas con contraseña **SonrisaDemo2026!**:

| Nombre completo | Usuario | Correo electrónico | Rol |
|---|---|---|---|
| Ana Mendoza Ríos | ana.demo | ana@sonrisa.example | Odontólogo |
| Diego Salazar León | diego.demo | diego@sonrisa.example | Odontólogo |
| Elena Paredes Gómez | recepcion.demo | recepcion@sonrisa.example | Recepción |
| Rosa Vargas Soto | caja.demo | caja@sonrisa.example | Caja |

Antes de guardar la primera, intenta contraseña menor de 10 caracteres; corrige. Intenta otro usuario **ana.demo**: debe rechazarse; cierra sin crear duplicado. Edita Rosa a **Rosa Vargas Soto · Caja**, dejando contraseña vacía: conserva la anterior. Para probar cambio de contraseña, cambia únicamente la de Rosa a **SonrisaCaja2026!**; usa esa al entrar como caja.

En **Roles**, revisa nombres y permisos de los cuatro roles. Son configurables; no necesitas crear otros. No quites la administración al último administrador activo. Modificarás y restaurarás un permiso en el paso 40.

### 04. Categorías y servicios

Crea categorías activas **General DEMO**, **Preventiva DEMO**, **Estética DEMO**. Después servicios activos:

| Nombre | Categoría | Precio | Minutos | Reserva automática | Descripción |
|---|---|---:|---:|---|---|
| Evaluación integral DEMO | General DEMO | 100.00 | 30 | Sí | Evaluación inicial y orientación del paciente. |
| Limpieza dental DEMO | Preventiva DEMO | 200.00 | 60 | Sí | Limpieza dental del caso de demostración. |
| Consulta de control DEMO | General DEMO | 80.00 | 30 | Sí | Revisión posterior a una atención. |
| Rehabilitación estética DEMO | Estética DEMO | 1200.00 | 60 | No | Plan de varias sesiones de demostración. |
| Protección localizada DEMO | Preventiva DEMO | 150.00 | 30 | No | Procedimiento adicional de demostración. |
| Orientación sin honorarios DEMO | General DEMO | 0.00 | 15 | No | Orientación gratuita de demostración. |

En el primer formulario intenta duración 0, duración −15 y precio −1.00: deben impedir guardar. Corrige y crea un único servicio válido. **Precio cero sí se permite; duración cero no.** Busca Limpieza y verifica sus valores en edición.

**Reserva automática desmarcada no impide reserva manual**; controla la futura integración del agente. Servicio inactivo o no asignado al profesional sí impide una nueva reserva.

### 05. Profesionales y servicios asociados

En **Configuración → Odontólogos**:

| Campo | Ana | Diego |
|---|---|---|
| Nombre del profesional | Ana Mendoza Ríos | Diego Salazar León |
| Registro profesional | DEMO-ANA-001 | DEMO-DIE-002 |
| Especialidad | Odontología integral | Odontología preventiva |
| Cuenta del odontólogo | ana.demo | diego.demo |
| Servicios habilitados | Los seis servicios DEMO | Evaluación integral, Limpieza dental, Consulta de control DEMO |
| Profesional activo | Sí | Sí |

La misma cuenta no puede vincularse a dos profesionales; recepción no debe ser cuenta elegible de odontólogo. Añade y quita una selección de servicio antes de guardar para comprobar el selector múltiple sin registros extra.

### 06. Jornadas, descansos y bloqueos

Para **cada** profesional crea periodos activos en **Horarios**:

| Día | Tipo | Inicio | Fin |
|---|---|---|---|
| Lunes | Jornada de trabajo | 09:00 | 18:00 |
| Lunes | Descanso | 13:00 | 14:00 |

Fin anterior al inicio, descanso fuera de jornada y jornada superpuesta deben rechazarse. Conserva solo periodos válidos.

Para atender hoy añade una **jornada temporal del día de la semana de H**, 00:00–23:59, para ambos. Si H es lunes, conserva la jornada normal 09:00–18:00 y elige T dentro de ella, fuera del descanso y con margen para 60 minutos; no la amplíes, porque también cambiarías las reglas del lunes F. Si hoy ya no queda horario, realiza las dos citas de atención en una jornada posterior y déjalas pendientes en tu registro. Al terminar desactiva las jornadas temporales creadas en otros días.

En **Bloqueos** crea:

| Tipo | Profesional | Fechas | Horas | Motivo |
|---|---|---|---|---|
| Día no laborable | Sin selección | F+7 a F+7 | Vacías: día completo | Cierre general de demostración |
| Ausencia de un odontólogo | Ana | F a F | 16:00–17:00 | Capacitación profesional de demostración |

Ausencia sin profesional y una sola hora sin su pareja deben rechazarse. Comprobarás disponibilidad real en el paso 10.

### 07. Plantillas y categorías documentales/financieras

En **Plantillas clínicas** crea activas:

| Nombre | Uso | Contenido |
|---|---|---|
| Evaluación inicial DEMO | Atención | Paciente orientado en tiempo y espacio. Se explican los hallazgos del caso ficticio y se registran las indicaciones entregadas. |
| Antecedentes básicos DEMO | Antecedentes | Información referida por el paciente. Registro ficticio para comprobar la evolución del expediente. |
| Consentimiento básico DEMO | Consentimiento | Material de demostración: verificar paciente, responsable, fecha y copia adjunta antes de registrar consentimiento. |

En **Categorías documentales**, crea activas **Registro de imagen DEMO**, **Informe DEMO**, **Consentimiento DEMO**. En **Configuración → Archivos**, conserva límite 20 MiB. En **Finanzas → Categorías** crea activas **Insumos DEMO**, **Gastos administrativos DEMO**. Abre una edición de cada tipo de catálogo y verifica valores, sin crear copias.

## Pacientes y agenda

### 08. Cinco pacientes, contactos y responsables

Todos activos. Deja vacíos los campos opcionales no indicados.

| Campo | María | Carlos | Lucía | Mateo | Valeria |
|---|---|---|---|---|---|
| Nombre completo | María Torres Castillo - DEMO | Carlos Ruiz Vega - DEMO | Lucía Paredes Gómez - DEMO | Mateo Paredes Gómez - DEMO | Valeria Soto Ramos - DEMO |
| Nacimiento | 1990-04-18 | 1985-08-12 | 2016-03-14 | 2019-07-22 | Vacío inicialmente |
| Documento | DNI 90000001 | DNI 90000002 | DNI 90000003 | DNI 90000004 | Sin documento; número vacío |
| Correo | maria@pacientes.example | carlos@pacientes.example | elena@pacientes.example | elena@pacientes.example | valeria@pacientes.example |
| Dirección | Calle Las Flores 110, Lima | Av. El Parque 208, Lima | Jr. Los Olivos 350, Lima | Jr. Los Olivos 350, Lima | Calle Las Palmeras 410, Lima |
| Provisional | No | No | No | No | Sí |
| Notas administrativas | Paciente principal para plan, cuotas y seguimiento. | Caso de servicio individual, anticipo y correcciones. | Menor con teléfono familiar compartido. | Hermano de Lucía; contacto compartido. | Completar ficha antes de finalizar atención o aceptar plan. |

Selecciona DNI y copia solo los ocho dígitos en Número de documento. Añade contactos:

| Paciente | Nombre del contacto | Teléfono | Relación | Responsable del menor | Responsable de pago |
|---|---|---|---|---|---|
| María | María Torres Castillo | +51900001001 | Paciente | No | Sí |
| Carlos | Carlos Ruiz Vega | +51900001002 | Paciente | No | Sí |
| Lucía | Elena Paredes Gómez | +51900001003 | Madre | Sí | Sí |
| Mateo | Elena Paredes Gómez | +51900001003 | Madre | Sí | Sí |
| Valeria | Valeria Soto Ramos | +51900001004 | Paciente | No | Sí |

En María añade segundo contacto **Jorge Torres Castillo**, teléfono **+51900001005**, relación **Hermano**, sin responsabilidades; también úsalo en **Contacto de emergencia / Teléfono de emergencia**. Añade y quita un tercer contacto vacío antes de guardar.

Nacimiento futuro debe rechazarse; Lucía sin responsable del menor debe rechazarse. Corrige antes de guardar una sola ficha. Valeria sí se guarda sin fecha como provisional. Anota los códigos asignados: no presupongas que comienzan en 001.

### 09. Edición, duplicados y búsquedas

Busca María por nombre, DNI 90000001 y teléfono; abre ficha y cambia notas a **Paciente principal para plan, cuotas y seguimiento. Prefiere contacto telefónico por la tarde.** Guarda y recarga.

Intenta nuevo paciente con DNI 90000001 y luego mismo nombre/nacimiento de María sin documento: ambas variantes deben prevenir duplicado. Cierra sin crear otra ficha. Buscar **+51900001003** permite encontrar Lucía/Mateo, dos fichas distintas. Filtra provisional para Valeria; busca **Paciente inexistente DEMO** para comprobar estado vacío; limpia filtros.

### 10. Intervalos completos y disponibilidad

En **Agenda**, reserva:

| Ref. | Paciente | Profesional | Servicio | Inicio | Notas |
|---|---|---|---|---|---|
| A1 | María | Ana | Limpieza dental DEMO | F 10:00 | Prueba de duración y reprogramación. |
| A2 | Carlos | Diego | Limpieza dental DEMO | F 10:00 | Misma hora con otro odontólogo. |
| A3 | Carlos | Ana | Evaluación integral DEMO | F 14:00 | Referencia de horario ocupado. |
| A4 | Lucía | Diego | Evaluación integral DEMO | F 11:15 | Cita de Lucía con contacto familiar. |
| A5 | Mateo | Diego | Evaluación integral DEMO | F 12:00 | Cita de Mateo con el mismo contacto. |

A1/A2 terminan 11:00. Carlos/Ana a10:30 debe rechazarse por solapamiento; A2 sí demuestra simultaneidad por profesional. Ana 13:00 debe rechazarse por descanso. En F 16:00–17:00 Ana no está disponible; Diego sí. F+7 bloquea ambos todo el día. Desactiva cada bloqueo, consulta que reaparezca disponibilidad y reactívalo.

Con Diego, Rehabilitación/Protección no aparecen como servicios asignados. Con Ana, Rehabilitación sí permite reserva manual aunque tenga reserva automática desmarcada. Selecciona explícitamente Lucía/Mateo: compartir teléfono no determina cuál recibirá la cita.

### 11. Cita administrativa y cancelación

Valeria/Ana, F 09:00; marca **Cita por motivo administrativo**. Motivo **Entrega de documentación pendiente**, duración 15, notas **Registro administrativo sin procedimiento ni deuda.** Prueba duración 0 y corrige antes de guardar. Termina 09:15, sin cargo.

Cancela con **Documentación recibida por otro medio en la demostración**. Se conserva historial y se libera horario, sin crear movimiento financiero.

### 12. Anticipación y separación

Cambia anticipación a30 min; intenta inicio de hoy 15 min después de ahora dentro de la jornada temporal. Debe rechazarlo. Restaura 0.

Cambia separación a15 min y reserva Carlos/Ana/Consulta de control, F 17:00, notas **Prueba de separación entre citas**. Fin 17:30. Otra cita 17:30 debe rechazarse por separación. Consulta disponibilidad:17:45 ya respeta separación, pero una evaluación 30 min termina 18:15 fuera de jornada y tampoco cabe. Cancela la cita creada con **Fin de comprobación de separación** y restaura separación 0.

### 13. Solicitudes simultáneas

En dos pestañas/ventanas autorizadas prepara Ana/Consulta de control/F 17:00, una para María y otra para Carlos, notas **Prueba simultánea de reserva**. Guarda casi al mismo tiempo. Debe crearse exactamente una cita y rechazarse la otra; sin solapamiento. Cancela la creada con **Fin de prueba de concurrencia**.

Si no logras enviarlas casi simultáneamente, anota “conflicto secuencial comprobado”; no declares concurrencia manual. La concurrencia real también tiene prueba técnica automática.

### 14. Catálogo e historial de reprogramación

1. Cambia Limpieza de 60 a45 min y precio 200 a220.00. A1/A2 siguen 60 min y fin 11:00.
2. Confirma A1. Reprograma a F 14:00/Ana, motivo **Prueba de horario ocupado**: debe rechazar por A3 y conservar 10:00–11:00 y estado Confirmada.
3. Reprograma A1 a F 15:00 sin marcar **Usar la duración actual del catálogo**, motivo **Cambio solicitado por la paciente**: fin 16:00, duración 60.
4. Reprograma al mismo inicio marcando esa casilla, motivo **Aplicar explícitamente duración actual de 45 minutos**: fin 15:45.
5. Cancela con **Paciente cancela reserva de prueba**. Conserva confirmación y ambas reprogramaciones; no produce devolución ni cargo.

Mantén catálogo Limpieza en 220.00/45 min. A2 conserva 60 min. Sobre C1 del siguiente paso, antes de T, intenta reprogramar a Diego: debe rechazar porque no tiene Rehabilitación asignada; no debe perderse C1.

### 15. Citas de hoy y estados

Hoy H, a T, reserva **C1 María/Ana/Rehabilitación 60 min**, notas **Primera sesión del plan**; y **C2 Mateo/Diego/Evaluación 30 min**, notas **Prueba de inasistencia**. Confirma C1. Antes de T pasarla a En espera debe rechazarse. Mientras llega T continúa con presupuestos/antecedentes.

Al llegar T: C1→**En espera**, motivo **Paciente llegó al consultorio**; luego→**En atención**, motivo **Inicio de atención de demostración**. Déjala así: la atención clínica la completará en el paso 19. C2→**No asistió**, motivo **Paciente no llegó a la cita de demostración**, sin cargo.

Atendida/Cancelada/No asistió son terminales; no deben permitir reprogramación normal. Reservada y Confirmada sí admiten cambios permitidos con motivo e historial.

### 16. Día, semana, mes y lista

En F revisa vistas día/semana/mes, primero todos y después Diego. Comprueba A2/A4/A5 y detalles/historial. En lista busca Carlos y filtra estado; limpia filtros. En celular la lista debe permitir gestionar lo mismo. La revisión de tamaños se concentra en el paso 41, sin volver a reservar todo.

## Presupuestos, clínica y documentos

### 17. Presupuesto de María en borrador/propuesto

En **Presupuestos y planes**, María:

| Campo | Valor |
|---|---|
| Título del presupuesto | Rehabilitación estética integral - María DEMO |
| Profesional del plan | Ana Mendoza Ríos |
| Condiciones acordadas | Total S/ 1200.00. Tres sesiones previstas. Abonos aplicados al plan y adicionales con aprobación independiente. |
| Servicio 1 | Rehabilitación estética DEMO |
| Descripción del tratamiento 1 | Rehabilitación estética de la pieza 16 |
| Precio unitario 1 | 600.00 |
| Unidades facturadas 1 | 2 |
| Sesiones previstas 1 | 3 |
| Pieza del tratamiento 1 | 16 |

Al seleccionar servicio carga 1200.00 sin error GET; sustitúyelo por 600.00. **2×600=1200; tres sesiones son avance, no tres cargos.** Prueba precio negativo/sesiones 0 y corrige. Añade y quita segundo tratamiento vacío. Guarda borrador, edita condiciones añadiendo **Caso ficticio para demostración.** No hay deuda.

Para comprobar versiones de edición abre este borrador en dos pestañas. Guarda el texto anterior en la primera; intenta guardar otra modificación desde la versión antigua de la segunda. Debe rechazar sobrescritura y pedir recargar. Cierra la antigua.

**Presentar presupuesto**, motivo **Presupuesto presentado a la paciente**. Estado Propuesto, deuda 0.00.

### 18. Aceptación única y precio acordado

**Aceptar**: nombre **María Torres Castillo**, motivo **Paciente acepta importe, sesiones y condiciones del caso de demostración.** Sin casilla de aceptación debe impedirlo; márcala y guarda. Estado Aceptado, un cargo 1200.00, pendiente 1200, recibido 0.

Recarga y consulta historial. Un doble clic rápido en el mismo envío no debe duplicar cargo; después no se ofrece nueva aceptación ni edición silenciosa del acuerdo. Una repetición exacta de solicitud también se prueba técnicamente; abrir otro formulario de pago sería una nueva operación legítima.

Cambia catálogo Rehabilitación a1300.00: acuerdo sigue 600×2=1200. Restaura 1200.00, sin alterar cargos.

### 19. Antecedentes y primera sesión vinculada a C1

En Historia clínica, María, **Odontólogo responsable Ana**, Antecedentes→Actualizar:

| Campo | Primer estado |
|---|---|
| Fecha del registro | H−1 |
| Antecedentes personales y familiares | No refiere antecedentes relevantes para este caso ficticio. |
| Alergias informadas | Refiere alergia a penicilina. Dato ficticio. |
| Medicamentos informados | No refiere medicación habitual. |
| Anamnesis | Solicita evaluación por molestia localizada en pieza 16. |
| Motivo del registro o actualización | Antecedentes iniciales de demostración. |

Aplica plantilla Antecedentes básicos y comprueba texto añadido. Guarda. Nuevo estado H: mismos datos salvo medicamentos **Refiere suplemento vitamínico diario. Dato ficticio.**, motivo **Actualización de información referida**. Consultar ambos conserva sus diferencias.

Nueva atención:

| Campo | Valor |
|---|---|
| Fecha de atención | H |
| Motivo de consulta | Primera sesión de rehabilitación estética de pieza 16. |
| Cita | C1: María/Ana/H/T |
| Anamnesis | Se revisan los antecedentes del expediente ficticio. |
| Evolución | Se registra primera sesión del tratamiento acordado; paciente tolera el procedimiento ficticio. |
| Diagnósticos | Hallazgo ficticio de pieza 16 para probar seguimiento clínico. |
| Indicaciones | Se registran indicaciones del caso y continuidad del plan. |
| Plantilla | Evaluación inicial DEMO; aplicar texto |
| Plan de tratamiento 1 | Rehabilitación estética de la pieza 16, del plan aceptado |
| Sesiones realizadas 1 | 1 |
| Pieza FDI 1 | 16 |

El selector enlaza el **tratamiento concreto del plan**, aunque el texto diga Plan de tratamiento. Completa servicio/descripcion asociados; no cobres un precio individual por esa sesión. Guarda borrador, reabre y añade evolución **Sesión 1 de 3 del plan aceptado.**, guarda edición.

Antes de T, finalizar debe rechazar y conservar borrador. Después de T finaliza: atención Finalizada, C1 Atendida, plan En curso, avance 1/3, deuda 1200. Si no pasaste manualmente por En espera/En atención, la finalización clínica puede completar el ciclo de la cita de forma consistente.

Intenta Finalizar plan con **Comprobación de sesiones pendientes**: debe rechazar por sesiones faltantes sin cambiar estado.

### 20. Corrección sin reescribir ni volver a cobrar

En atención finalizada, Registrar corrección. Solo cambia indicaciones a **Se aclara el seguimiento ficticio: revisar evolución en la próxima sesión; conservar las indicaciones anteriores como versión original.** Motivo **Aclaración de indicaciones sin modificar procedimientos ni importes.** Guarda y consulta original/corrección con responsable y motivo. Avance 1/3 y deuda 1200 permanecen; no se factura otra vez.

### 21. Odontograma permanente/temporal e historia

María/Ana→Actualizar odontograma: Permanente 32 piezas, pieza 16/Oclusal-incisal/Caries, nota **Hallazgo ficticio inicial de pieza 16.** Añade pieza 11/Vestibular/Restauración, nota **Restauración previa del caso ficticio.** Fecha H−1, observaciones **Estado inicial ficticio para comparación.**, motivo **Registro odontológico inicial de demostración.** Guarda.

Nuevo estado H:16/Oclusal-incisal cambia a Restauración, nota **Cambio para comprobar evolución.**, observaciones **Segundo estado; se conserva evaluación inicial.**, motivo **Actualización después de primera sesión**. Ver estado de ambos debe mostrar diferencias sin reemplazar original.

Mateo/Diego: Temporal 20 piezas,55/Oclusal-incisal/**Sin hallazgo informado**, nota **Control ficticio de dentición temporal.**, fecha H, motivo **Comprobación de piezas temporales**. Guarda; alterna denticiones. No necesita atención finalizada para registrar odontograma.

En un formulario con pieza FDI intenta 99 y corrige 16: debe rechazar 99. Revisa opciones de **Pieza completa** sin guardar un tercer estado solo para recorrer hallazgos.

### 22. Carga, vínculos, comparación y PDF

En María→Archivos→Adjuntar archivo:

| Archivo | Categoría | Fecha | Descripción | Vínculos |
|---|---|---|---|---|
| registro-inicial.jpg | Registro de imagen DEMO | H−1 | Registro ilustrativo inicial de pieza 16. | Pieza 16; tratamiento de María |
| registro-posterior.png | Registro de imagen DEMO | H | Registro ilustrativo posterior de pieza 16. | Pieza 16; tratamiento; primera atención |
| registro-posterior.webp | Registro de imagen DEMO | H | Comprobación del formato WebP. | Pieza 16 |
| informe-maria-demo.pdf | Informe DEMO | H | Informe ficticio de dos páginas asociado al plan. | Tratamiento; primera atención |

En Tratamiento (opcional) elige el plan por su título Rehabilitación estética integral - María DEMO; Atención vinculada identifica la atención por su motivo. Deja categoría vacía antes del primer envío: debe impedirlo; corrige y carga una sola vez. Abre las tres imágenes y descarga JPG/WebP para verificar originales. PDF: páginas 1/2, zoom, descarga original de dos páginas. Selecciona imágenes inicial/posterior con Elegir fotografía→Comparar fotografías; verifica fecha/descripción y limpia selección.

Busca pieza 16, filtra categoría/tipo/fechas y limpia filtros. Lista debe traer metadatos, no descargar todas las imágenes. Guarda el enlace de **Descargar original** del PDF para probar permisos en el paso 40.

### 23. Formatos, contenido y límite configurable

Intenta subir formato-no-admitido.txt; si no aparece en diálogo usa Todos los archivos. Debe rechazarse. Intenta archivo-disfrazado.jpg: debe rechazar texto con extensión JPG. Ninguno aparece en lista.

Configuración→Archivos, Tamaño máximo por archivo(MiB)→1. Intenta imagen-limite.png de 1.76 MiB: debe rechazarla sin registrar documento. Restaura 20; originales anteriores siguen abriendo. Valores de política 0/61 deben impedirse. No necesitas un archivo 21 MiB: el límite temporal prueba la misma validación.

### 24. Consentimiento de una menor

En Lucía/Diego adjunta consentimiento-lucia-demo.pdf, categoría Consentimiento DEMO, fecha H, descripción **Copia ficticia de consentimiento de Lucía otorgado por su madre.** La copia debe pertenecer a Lucía.

Consentimientos→Registrar consentimiento:

| Campo | Valor |
|---|---|
| Nombre del consentimiento | Consentimiento de evaluación odontológica - Lucía DEMO |
| Responsable que otorgó el consentimiento | Elena Paredes Gómez |
| Relación con el paciente | Madre |
| Fecha del consentimiento | H |
| Copia adjunta | consentimiento-lucia-demo.pdf, del expediente de Lucía |
| Plantilla | Consentimiento básico DEMO |

Guardar sin copia debe rechazarse; selecciona copia, guarda y descarga desde consentimiento. Plantilla es texto de apoyo mostrado en pantalla, no reemplaza el archivo ni constituye firma. No hay firma electrónica.

### 25. Atención individual de Carlos: deuda al finalizar

Con Carlos y **Diego**, sin cita vinculada; puedes entrar como diego.demo para ejecutar escritura con su profesional.

| Campo | Valor |
|---|---|
| Fecha | H |
| Motivo | Limpieza y control preventivo del caso de demostración. |
| Anamnesis | Solicita limpieza y revisión; no se utiliza un plan aceptado. |
| Evolución | Inicialmente vacía, para probar finalización incompleta |
| Diagnósticos | Inicialmente vacío, para probar finalización incompleta |
| Indicaciones | Indicaciones ficticias de revisión registradas para demostración. |

Añade tres procedimientos **sin Plan de tratamiento**:

| Procedimiento | Servicio | Descripción | Cantidad | Pieza | Precio acordado |
|---|---|---|---:|---|---:|
| 1 | Limpieza dental DEMO | Limpieza individual con precio acordado | 1 | Vacía | 180.00 |
| 2 | Consulta de control DEMO | Control individual del paciente | 1 | Vacía | Vacío: catálogo 80.00 |
| 3 | Sin servicio | Orientación informativa sin honorarios | 1 | Vacía | Vacío |

Guarda borrador e intenta finalizar: exige evolución/diagnóstico y no genera deuda. Edita **Evolución: Se registran limpieza y control del caso ficticio, con orientación sin honorarios.** y **Diagnósticos: Evaluación preventiva ficticia registrada para prueba funcional.** Guarda y finaliza.

Resultado: exactamente dos cargos, **180.00 y 80.00**, total 260.00. El procedimiento libre sin importe no genera cargo. Catálogo Limpieza 220, atención 180 acordados. Recarga: no debe duplicarlos. Nombra en tus notas esos cargos **C-Limpieza** y **C-Control**.

### 26. Provisional y dos políticas de cancelación

Con Valeria provisional, crea:

| Campo | Valor |
|---|---|
| Título | Evaluación por sesiones - Valeria DEMO |
| Profesional | Ana |
| Condiciones | Dos sesiones por un importe acordado de S/ 200.00. |
| Servicio | Evaluación integral DEMO |
| Descripción del tratamiento | Evaluación en dos sesiones de Valeria |
| Precio unitario | 200.00, después de comprobar automático 100.00 |
| Unidades / Sesiones | 1 / 2 |

Presenta el presupuesto, motivo **Propuesta registrada mientras se completa la ficha**. Aceptar con nombre Valeria Soto Ramos, casilla marcada y motivo **Prueba con datos pendientes** debe rechazarse por provisional; deuda 0.

Crea borrador clínico de Valeria/Ana/H, motivo **Evaluación inicial de ficha provisional**, evolución **Registro ficticio pendiente de completar datos administrativos.**, diagnóstico **Evaluación ficticia inicial.**, indicaciones **Completar ficha antes de finalizar.**, sin procedimientos. Finalizar debe rechazar por provisional y conservar borrador.

Completa ficha: nacimiento **1998-11-09**, DNI **90000005**, desmarca provisional, notas **Ficha completada durante revisión funcional.** Acepta plan con su nombre, casilla marcada y motivo **Ficha completada y presupuesto aceptado**: deuda 200.

Edita borrador anterior, añade tratamiento aceptado **Evaluación en dos sesiones de Valeria**, sesiones realizadas 1, y finaliza: avance 1/2, deuda 200. Cancela plan marcando **Liberar el importe de sesiones pendientes; conservar lo realizado**, motivo **Paciente suspende después de primera sesión de prueba**. Cargo original 200, liberación −100, deuda 100 por lo realizado.

Crea segundo plan **Evaluación reservada - Valeria DEMO**, Ana, mismo servicio, descripción **Evaluación acordada con deuda conservada**, precio 200, unidades 1, sesiones 2, condiciones **Cancelación sin liberación de deuda.** Presenta/acepta con nombre y confirmación; cancela sin liberar, motivo **Cancelación conservando compromiso económico**. No registres sesiones ni pagos de ese segundo plan.

Resultado: dos planes cancelados, historial completo y deuda Valeria **300.00**: 100 del primero+200 del segundo. Cancelar no significa automáticamente devolver dinero.

### 27. Propuesto/cancelado sin aceptación

Lucía: **Evaluación inicial - Lucía DEMO**, Diego, condiciones **Evaluación pendiente de aceptación por la responsable.**, servicio Evaluación, descripción **Evaluación inicial de Lucía**, precio 100, unidades 1, sesiones 1. Presenta el presupuesto con **Se entrega presupuesto a la madre**; cancela con **La responsable no acepta el presupuesto de demostración**. Historial conservado y deuda 0. No repitas edición de borrador ya probada con María.

## Pagos, cuotas, anticipos y correcciones

### 28. Apertura de caja

En **Finanzas → Caja**, puedes usar caja.demo, con contraseña SonrisaCaja2026! si la cambiaste en 03. **Fondo inicial de efectivo 100.00**, **Motivo y observaciones: Apertura de caja para revisión integral de demostración.** Confirma apertura. Esperado 100.00; una segunda apertura mientras está abierta debe rechazarse.

### 29. Cuotas y abonos 300+200 de María

En Finanzas→Cuenta del paciente→María→Cargos, sobre cargo 1200, **Programar cuotas**:

| Cuota | Vencimiento | Importe |
|---|---|---:|
| 1 | H−1 | 400.00 |
| 2 | H+15 | 400.00 |
| 3 | H+30 | 400.00 |

Motivo **Calendario inicial de tres cuotas de María**. Primero última 399.00: suma 1199, debe rechazar. Corrige 400 y guarda. Deuda sigue 1200, sin deuda nueva; primera cuota Vencida antes de pagos.

Antes del primer abono intenta importe 0, importe −1 y fecha futura: deben impedir guardar. Corrige los datos antes de crear el abono válido. Dos abonos, fecha H; selecciona el cargo en **Aplicación a cargos → cargos** y completa importe aplicado:

| Campo | Abono 1 | Abono 2 |
|---|---|---|
| Importe del abono | 300.00 | 200.00 |
| Medio de pago | Efectivo | Transferencia |
| Referencia del pago | DEMO-MARIA-001 | DEMO-MARIA-002 |
| Concepto del abono | Primera cuota del plan de María | Segundo abono del plan de María |
| Importe aplicado al plan | 300.00 | 200.00 |

Tras primero: recibido/aplicado 300, pendiente 900. Tras segundo: **deuda 1200, recibido 500, aplicado 500, pendiente 700, anticipo 0**. Cuotas: primera Pagada 400, segunda Parcial 100 con 300 pendientes, tercera Pendiente 400. Caja efectivo esperado 400; transferencia 200 se distingue del efectivo.

Descarga Constancia PDF de cada uno: identidad/logo, correlativos distintos, paciente, fecha, medio, importes, aplicación y responsable. Adjunta al abono 300 **registro-inicial.jpg**, descripción **Sustento ficticio del primer abono de María**. Abre/descarga sustento.

### 30. Reprogramación de cuotas

Mismo cargo 1200: nuevo calendario dos cuotas 600.00, vencimientos H+15/H+30, motivo **Reprogramación con conservación del calendario anterior**. Distribuye **1200, no 700**: lo pagado cubre parte de la primera. Primera Parcial 500/pendiente 100; segunda Pendiente 600. Calendario anterior Histórico al consultar Anteriores/Todos. Cuenta sigue 1200/500/700.

### 31. Anticipo y aplicación a varios cargos

Vuelve a administrador para las correcciones. En Carlos:

| Campo | Valor |
|---|---|
| Importe | 300.00 |
| Fecha | H−1: se permite fecha anterior para un medio distinto de efectivo |
| Medio | Tarjeta |
| Referencia | DEMO-CARLOS-ANT-001 |
| Concepto | Anticipo para limpieza y control de Carlos |
| Aplicación a cargos | Ninguna selección |

Deuda 260, recibido 300, aplicado 0, pendiente 260, anticipo 300. Descarga constancia y anota correlativo como **P-Carlos-Tarjeta**.

Sobre este movimiento **Aplicar anticipo**, C-Limpieza 100 y C-Control 50, motivo **Primera distribución del anticipo**. Recibido sigue 300; aplicado 150, pendiente 110, anticipo 150.

Intenta aplicar 200 a C-Limpieza, motivo **Prueba de exceso de aplicación**: debe rechazar sin cambios. Corrige a80 en C-Limpieza y 30 en C-Control, motivo **Completar cargos con anticipo existente**. Aplicado 260, pendiente 0, anticipo 40, recibido 300. **Aplicar no crea otro ingreso.**

### 32. Devolución parcial, liberación y reversión

Todas sobre **P-Carlos-Tarjeta**, con fecha H cuando corresponda:

1. Devolver 50 sin liberar, motivo **Prueba de devolución superior al anticipo libre**: rechaza, solo 40 libres.
2. Devolver 50 liberando 10 de C-Control en el mismo formulario, motivo **Devolución parcial con liberación de control**. Recibido 250, aplicado 250, pendiente 10, anticipo 0.
3. Liberar aplicación 40 de C-Limpieza, motivo **Liberación sin devolver dinero**. Recibido 250, aplicado 210, pendiente 50, anticipo 40.
4. Aplicar anticipo 40 a C-Limpieza, motivo **Reaplicación al cargo original**. Vuelve recibido 250/aplicado 250/pendiente 10/anticipo 0.
5. Revertir pago, motivo **Corrección de tarjeta: sustituir por medio correcto**. Revierte **vigente 250**, no original 300, porque 50 ya se devolvieron. Libera aplicaciones automáticamente. Recibido 0, aplicado 0, pendiente 260, anticipo 0.
6. Consulta originales/correcciones y descarga la constancia original. Segundo intento de reversión del pago agotado no genera otra salida.

### 33. Pago correcto y anulación de cargo

Nuevo abono Carlos:

| Campo | Valor |
|---|---|
| Importe / Fecha | 260.00 / H |
| Medio | Transferencia |
| Referencia | DEMO-CARLOS-TRANS-002 |
| Concepto | Cobro correcto de limpieza y control |
| Aplicación C-Limpieza / C-Control | 180.00 / 80.00 |

Nómbralo **P-Carlos-Transferencia**. Deuda 260, recibido 260, aplicado 260, pendiente 0, anticipo 0.

Anular C-Control con **Prueba de anulación con pago aplicado** debe rechazar por 80 aplicados. Sobre P-Carlos-Transferencia libera 80 de C-Control, motivo **Liberación previa a anular control**. Después anula cargo con **Anulación acordada del control de demostración**. Conserva original 80 y anulación −80. Cuenta: deuda 180, recibido 260, aplicado 180, pendiente 0, anticipo 80.

### 34. Descuento, cuotas en revisión y ajustes

1. Descuento 20 a C-Limpieza con 180 aplicados debe impedirse: no puede reducir debajo de aplicado. Motivo **Prueba de descuento incompatible con aplicación**. Puede bloquear el importe antes de enviar; cierra sin cambios.
2. Libera 20 de C-Limpieza desde P-Carlos-Transferencia, motivo **Liberación para descuento autorizado**. Deuda 180, aplicado 160, pendiente 20, anticipo 100, recibido 260.
3. Programa dos cuotas 90, fechas H+15/H+30, motivo **Calendario previo al descuento de Carlos**. Total 180, incluyendo 160 pagados.
4. Descuento 20, motivo **Descuento de cortesía autorizado en demostración**. Deuda 160, aplicado 160, pendiente 0, anticipo 100. Calendario 180 requiere Revisión.
5. Nuevo calendario dos cuotas 80, H+15/H+30, motivo **Actualizar cuotas al importe descontado**. Ambas Pagadas; calendario 90 conservado como Histórico.
6. Ajustar cargo C-Limpieza: **Variación del importe 10.00**, **Motivo del ajuste: Adicional administrativo de prueba autorizado**. Deuda 170, pendiente 10, mismo recibido 260/aplicado 160/anticipo 100.
7. Ajuste −10.00, motivo **Reversión del adicional administrativo de prueba**. Deuda 160, pendiente 0; conserva los dos movimientos. Cuotas activas vuelven a coincidir con 160.
8. Ajuste −200 debe rechazar cargo neto negativo o inferior al dinero aplicado. No sustituyas por otro ajuste válido en este intento.

### 35. Devolver el anticipo restante

P-Carlos-Transferencia: devolver 80 sin liberar, motivo **Devolución por control anulado**. Recibido 180, aplicado 160, anticipo 20. Aplicar 20 a C-Limpieza, pendiente 0, debe rechazarse. Devuelve 20, motivo **Devolución de anticipo liberado por descuento**.

**Final Carlos: deuda 160, recibido 160, aplicado 160, pendiente 0, anticipo 0.** Emite Estado de cuenta PDF y comprueba trazabilidad.

| Después de | Deuda | Recibido neto | Aplicado | Pendiente | Anticipo |
|---|---:|---:|---:|---:|---:|
| Atención individual | 260 | 0 | 0 | 260 | 0 |
| Anticipo 300 | 260 | 300 | 0 | 260 | 300 |
| Aplicar 100+50 | 260 | 300 | 150 | 110 | 150 |
| Aplicar 80+30 | 260 | 300 | 260 | 0 | 40 |
| Devolver 50/liberar 10 | 260 | 250 | 250 | 10 | 0 |
| Liberar 40 | 260 | 250 | 210 | 50 | 40 |
| Reaplicar 40 | 260 | 250 | 250 | 10 | 0 |
| Revertir 250 | 260 | 0 | 0 | 260 | 0 |
| Transferencia 260 aplicada | 260 | 260 | 260 | 0 | 0 |
| Liberar 80/anular control | 180 | 260 | 180 | 0 | 80 |
| Liberar 20/descontar 20 | 160 | 260 | 160 | 0 | 100 |
| Ajuste+10 | 170 | 260 | 160 | 10 | 100 |
| Ajuste −10 | 160 | 260 | 160 | 0 | 100 |
| Devolver 80 | 160 | 180 | 160 | 0 | 20 |
| Devolver 20 | 160 | 160 | 160 | 0 | 0 |

La tabla es de comprobación, no nuevos formularios. Si una cifra difiere, registra el primer paso que no coincide.

## Completar tratamientos, egresos y PDF históricos

### 36. Sesiones restantes y adicional de María

María/Ana, segunda atención sin cita vinculada:

| Campo | Valor |
|---|---|
| Fecha | H |
| Motivo | Segunda sesión del plan de rehabilitación de María. |
| Anamnesis | Paciente retorna para continuidad del tratamiento ficticio. |
| Evolución | Sesión 2 de 3 sin cambios en el acuerdo económico. |
| Diagnósticos | Seguimiento del hallazgo ficticio de pieza 16. |
| Indicaciones | Continuar seguimiento del plan de demostración. |
| Plan de tratamiento / Sesiones / Pieza | Tratamiento original de María / 1 / 16 |

Finaliza: avance 2/3, deuda 1200, pendiente 700. En plan **Añadir adicional**:

| Campo | Valor |
|---|---|
| Servicio | Protección localizada DEMO |
| Descripción del tratamiento | Protección adicional de pieza 16 |
| Precio unitario / Unidades / Sesiones | 150.00 / 1 / 1 |
| Pieza | 16 |
| Motivo del adicional | Procedimiento adicional acordado expresamente durante seguimiento de demostración. |

Guarda adicional y cargo: deuda 1350, pendiente 850. Acuerdo original 1200 conservado, cargo adicional 150 independiente. Cuotas originales siguen distribuyendo 1200; no cambian silenciosamente por el cargo nuevo.

Tercera atención H, motivo **Cierre clínico del plan y adicional**, anamnesis **Paciente retorna para completar sesiones acordadas.**, evolución **Se completa tercera sesión y adicional autorizado del caso ficticio.**, diagnóstico **Seguimiento final ficticio de pieza 16.**, indicaciones **Se registra cierre clínico del plan de demostración.** Añade dos procedimientos vinculados: original 1 sesión, adicional 1 sesión.

Antes de finalizar intenta 2 sesiones en original, cuando solo queda 1: debe rechazar sin avanzar ni cobrar. Corrige 1 y finaliza. Avances 3/3 y 1/1, deuda 1350, recibido 500, pendiente 850. **Finalizar plan**, motivo **Todas las sesiones y el adicional fueron registrados**. Queda Finalizado. Revisa Tratamientos y avance, Historial, Sesiones realizadas: cada atención explica avance; ninguna sesión genera segunda deuda.

### 37. Egresos, sustentos y reversión

Caja abierta, fecha H:

| Campo | Egreso válido | Egreso erróneo |
|---|---|---|
| Categoría | Insumos DEMO | Gastos administrativos DEMO |
| Importe | 50.00 | 20.00 |
| Medio | Efectivo | Otro |
| Proveedor | Dental Demo SAC | Vacío: opcional |
| Referencia | DEMO-EGR-001 | DEMO-EGR-ERROR-002 |
| Concepto | Compra de guantes y material descartable de demostración. | Gasto por error para prueba de reversión. |

Adjunta al 50 **sustento-egreso-demo.pdf**, descripción **Sustento ficticio de insumos**; abre/zoom/descarga. Al 20 **registro-posterior.png**, descripción **Imagen ficticia de sustento del registro erróneo**; abre imagen.

Como administrador revierte 20, motivo **Egreso por error: conservar historial**. Original 20 y reversión 20 conservados, neto Otro 0. Segunda reversión no debe producir otro movimiento.

Intenta egreso Efectivo 500, fecha H, categoría Insumos, referencia **DEMO-EGR-RECHAZADO**, concepto **Prueba de fondos insuficientes**: rechaza porque efectivo esperado 350. Cierra sin crear otro gasto.

### 38. Identidad histórica y correlativos

Guarda constancias de María y estado Carlos. Emite Estado de cuenta PDF de María: deuda 1350/recibido 500/pendiente 850. Cambia nombre temporalmente a **Clínica Sonrisa Integral - Revisión**, pie **Texto actualizado de prueba documental.** Emite nuevo estado María: refleja configuración nueva. Vuelve a descargar constancia/estado anteriores: conservan identidad, logo, pie y cifras al emitirlos. Restaura identidad/pie del paso 02.

En Archivos financieros consulta documentos generados y sustentos, busca un correlativo y descarga desde lista. En configuración intenta disminuir el siguiente número de constancia del prefijo actual a un correlativo ya utilizado: debe impedirlo. Cancela o restablece número actual; no reinicies contadores.

### 39. Cierre de caja

Sin movimientos ajenos:

| Concepto | Efectivo |
|---|---:|
| Fondo inicial | +100.00 |
| Abono María | +300.00 |
| Egreso Insumos | −50.00 |
| Esperado | 350.00 |

Otros medios netos **360.00**: María Transferencia 200+Carlos Transferencia 160. Tarjeta Carlos termina 0; egreso Otro 20/reversión 20 termina 0. No se añaden 360 al efectivo 350.

Cierra con **Efectivo contado 345.00**, **Motivo y observaciones: Arqueo ficticio: diferencia de cinco soles identificada para revisión.** Esperado 350/contado 345/**diferencia −5.00**, caja Cerrada. Descarga Arqueo PDF y verifica responsable, movimientos y cifras; se conserva al recargar.

Caja cerrada: intenta abono Efectivo 1 a María, sin aplicaciones, referencia **DEMO-EFECTIVO-SIN-CAJA**, concepto **Prueba de caja cerrada**. Debe rechazar y dejar cuenta 1350/500/850 intacta. No abras otra caja para completar el intento; una caja cerrada no se edita ni pierde movimientos.

## Permisos, historia, calidad visual y listas

### 40. Acceso por roles y desactivaciones

Usa registros existentes, sin repetir atenciones/cobros:

| Cuenta | Comprobación |
|---|---|
| recepcion.demo | Pacientes/agenda/presupuestos según permisos. Reprograma A2 Carlos/Diego a F 14:30, motivo **Cambio solicitado a recepción**, sin usar duración actual: conserva 60 min, fin 15:30. Sin clínica ni ajustes financieros por defecto. |
| ana.demo | Clínica/planes y escritura con Ana. No puede actuar como Diego aunque manipule selección/solicitud; no administra usuarios ni correcciones financieras por defecto. |
| diego.demo | Puede realizar paso 25 con su profesional. No puede registrar atención a nombre de Ana. El permiso clínico puede permitir consultar historia compartida del paciente: restricción del profesional se aplica a escritura, no supone aislamiento de todos los antecedentes entre odontólogos. |
| caja.demo | Cuentas/abonos/egresos/caja y sustentos financieros. Sin documentos clínicos ni correcciones administrativas por defecto. |
| Administrador | Configuración/permisos/auditoría y correcciones clínicas/financieras motivadas. |

**Servidor, no solo menú:** copia enlace Descargar original del PDF clínico de María mientras eres administrador. Sal, entra como caja o recepción en el mismo navegador y abre ese enlace. Debe denegar, normalmente 403, sin entregar PDF. Si entrega el archivo falla aunque menú esté oculto. Usa mismo servidor (localhost) para compartir sesión. Tras salir y recargar tampoco debe entregar archivo.

Configura un permiso: anota los originales de Recepción, retira únicamente **Gestionar presupuestos y planes (PLANS_WRITE)** y guarda. Vuelve a entrar como recepción: consulta si conserva PLANS_READ, pero no crea/acepta. Restituye PLANS_WRITE y reinicia sesión. No reduzcas permisos obligatorios del administrador.

Como administrador comprueba y restaura inmediatamente:

| Elemento | Acción y resultado |
|---|---|
| Usuario caja | Desactiva: login/operaciones protegidas rechazadas; pagos previos mantienen responsable. Reactiva y entra. |
| Profesional Diego | Desactiva: no disponible para nueva cita; A2 y atención Carlos consultables. Reactiva. |
| Servicio Limpieza | Desactiva: no disponible para reservas; citas/cargo Carlos conservados. Reactiva. |
| Paciente Carlos | Desactiva: no elegible activo para cita; historia y cuenta 160 conservadas. Reactiva. |
| Categoría Estética | Desactivar con servicio activo debe rechazar. Desactiva Rehabilitación y después categoría: historia María sigue. Reactiva primero categoría y después servicio. |
| Plantilla Antecedentes | Desactiva: no elegible para nueva aplicación; texto guardado conservado. Reactiva. |
| Categoría Registro de imagen | Desactiva: no elegible para nueva carga; documentos guardados abren. Reactiva. |
| Categoría Insumos | Renombra **Insumos revisados DEMO**: egreso previo conserva nombre histórico. Desactiva: no elegible para egreso nuevo. Reactiva/restaura nombre. |

Usuarios con varios roles reúnen permisos. Si cambiaste los iniciales, compara comportamiento con tu configuración; la tabla describe la base de demostración. Durante estas comprobaciones no guardes operaciones válidas adicionales que alteren los saldos finales.

### 41. Computadora, tablet, celular, teclado y avisos

En computadora recorre todo. En tablet/celular reutiliza los registros:

| Vista | Comprobar sin nuevas operaciones financieras |
|---|---|
| Navegación/login | Menú, inicio/cierre de sesión, textos y botones legibles |
| Pacientes | Abrir edición de María; contactos/emergencia/fecha; cerrar sin cambios |
| Agenda | Calendario/lista, filtros, A2/historial, abrir formulario y cerrar |
| Presupuestos | Tratamientos/sesiones de María; abrir nuevo borrador y elegir servicio para ver precio; cerrar sin guardar |
| Clínica | Versiones/antecedentes; tocar piezas/superficies sin guardar nuevo estado |
| Archivos | Imágenes/PDF dos páginas/zoom/comparación |
| Finanzas | Cuenta/cuotas/pago/PDF; abrir abono y cerrar sin guardar |
| Egresos/caja | Sustento/arqueo y acciones de tablas o tarjetas |

Tamaños orientativos: 1440×900,768×1024,390×844. Dispositivo real o vista adaptable del navegador; revisa orientación si una tabla/modal queda estrecho. Desplazamiento interno de tabla puede ser necesario; no debe ocultar acciones ni desbordar toda la página.

Teclado: modal con Tab/Shift+Tab, foco visible dentro del modal, Escape cierra cuando no guarda y foco vuelve al disparador. Selector permite buscar/seleccionar. Prueba aviso de error sin guardar y éxito mediante modificación reversible de notas: flotante, legible sobre modal, cierre manual y desaparición automática. No se duplica por mismo error; puntero sobre aviso pausa tiempo. Errores persistentes de carga de listas pueden permanecer en su contexto.

### 42. Búsquedas, filtros, páginas y archivos a demanda

En cada lista siguiente: busca registro conocido, combina un filtro disponible, limpia, prueba sin resultados y vuelve. Usa solo controles que ofrece la vista; los filtros no son idénticos en todas. Al cambiar paciente no debe conservarse una selección incompatible.

| Área | Listas/casos |
|---|---|
| Configuración | Usuarios, profesionales, servicios, categorías, horarios, bloqueos, plantillas, categorías documentales; DEMO/nombre. Roles: cuatro registros fijos. |
| Pacientes | Nombre/documento/teléfono; activos/provisionales |
| Agenda | Lista/historial; profesional/estado/fecha; calendario por intervalo |
| Clínica | Atenciones/versiones/antecedentes/odontogramas; fechas/textos |
| Documentos | Categoría/tipo/fechas; consentimientos |
| Planes | Título/estado; selectores/tratamientos/historial/sesiones |
| Finanzas | Cargos/movimientos/medio/tipo/fecha/moneda; cuotas vigentes/anteriores; documentos generados/sustentos |
| Egresos/caja | Medio/fecha/estado; categorías |
| Auditoría | Módulo/fecha/usuario o texto |

**Paginación sin crear veinte pacientes:** Auditoría tendrá suficientes eventos. Usa tamaño pequeño y Siguiente/Anterior, comprueba filas/recuento; filtrar debe volver a página válida. Las listas pequeñas pueden tener una página: no requieren registros extra. En selectores prueba búsqueda y páginas cuando haya varias.

Comprobación técnica opcional: navegador→Red/Network→Fetch/XHR. Cambiar página solicita page/size/filtros; no se baja colección entera para recortar en React. Lista documental devuelve metadatos sin todos los binarios; content/preview se pide al abrir archivo/página. Las pruebas técnicas cubren estos contratos también.

### 43. Auditoría y permanencia

Configuración→Auditoría: revisa identidad, usuarios/roles, catálogo, pacientes/citas, planes, clínica/documentos, cargos/pagos/cuotas, egresos/caja. Deben conservar actor, operación y momento; motivos de operaciones sensibles cuando corresponda.

Busca reprogramaciones/cancelación A1, corrección María, aceptación/adicional, devolución/reversión Carlos, anulación/descuento, reversión del egreso y cierre. Abrir/descargar PDF clínico autorizado deja evidencia de acceso documental.

Sal, vuelve a entrar, recarga y consulta cuentas, versiones, originales y caja Cerrada: deben persistir. No borres base para comprobar permanencia. Reinicio integral y restauración con binarios se tratan técnicamente en fase 9.

### 44. Restaurar temporales y comparar resultados

Identidad/pie originales del paso 02, límite 20 MiB, anticipación 0, separación 0; jornada temporal según 06. Usuarios, profesionales, servicios, plantillas y categorías DEMO activos. Mantén Limpieza 220.00/45 min para mostrar valores históricos conservados. Bloqueos F/F+7 pueden quedar activos para demostración o desactivarse, conservando historia.

| Paciente/caso final | Deuda | Recibido neto | Aplicado | Pendiente | Anticipo |
|---|---:|---:|---:|---:|---:|
| María: plan finalizado+adicional | 1350.00 | 500.00 | 500.00 | 850.00 | 0.00 |
| Carlos: cuenta corregida/saldada | 160.00 | 160.00 | 160.00 | 0.00 | 0.00 |
| Valeria: dos cancelaciones | 300.00 | 0.00 | 0.00 | 300.00 | 0.00 |
| Lucía: sin aceptación | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |
| Mateo: inasistencia | 0.00 | 0.00 | 0.00 | 0.00 | 0.00 |

Para estos cinco pacientes: deuda 1810, recibido 660, aplicado 660, pendiente 1150, anticipo 0. Es control aritmético, no un reporte global que deba existir ya. Caja esperado 350/contado 345/diferencia −5, otros medios 360; egreso Efectivo 50 y egreso Otro 20 revertido.

## Qué sigue pendiente

- **Fase 6:** WhatsApp real, agente, reserva automática y bitácora verificable. Configurar “reserva automática” en catálogo no demuestra integración ni cumple A22.
- **Fase 7:** variantes conversacionales, reprogramación/cancelación por agente, atención humana. Aquí sí se prueba agenda manual.
- **Fase 8:** panel completo, reportes consolidados y recordatorios. Cuentas individuales/PDF financieros ya existen.
- **Fase 9:** entrega reproducible y respaldo/restauración integral con binarios. Existe preparación de respaldo, sin declarar restauración final aquí.

Regresión previa registrada: **79 pruebas de servidor y 52 de navegador**. Cubre además transacciones, repeticiones exactas, concurrencia, reglas monetarias y contratos de datos. No equivale a que tú ya hayas realizado este recorrido. Incidencias críticas de agenda/clínica/archivos/permisos/finanzas deben corregirse antes de pasar a integración dependiente.

## Guion breve para presentar después de aprobar

1. Identidad configurable y dos profesionales con servicios distintos.
2. Teléfono compartido de Lucía/Mateo y responsable de menores.
3. Agenda por profesional, intervalos/descansos/ausencia e historial de reprogramación/cancelación.
4. María: antecedentes/versiones, odontograma inicial/posterior, imágenes comparadas y PDF dos páginas.
5. Su plan: 1200 aceptados, tres sesiones sin duplicar deuda, adicional 150, cargos 1350, abonos 500, saldo 850.
6. Carlos: anticipo, aplicaciones, constancia original y correcciones explican cuenta final 160 saldada.
7. Egreso con sustento y arqueo 350/345/−5; efectivo separado de otros medios.
8. Acceso como recepción/caja y auditoría como administrador.
9. Señala con claridad que WhatsApp/agente/reportes complementarios se incorporan después.

Conserva resultados e incidencias. La revisión se cierra cuando los valores críticos coinciden y no quedan fallos pendientes que impidan los flujos.
