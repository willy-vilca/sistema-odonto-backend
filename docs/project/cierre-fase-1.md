# Cierre técnico de fase 1

Fecha: 30 de septiembre de 2026. Alcance 1.2. Fase 0 revisada y aprobada por el usuario antes de iniciar esta fase.

## Resultado

La instalación se configura desde la aplicación: una sede, uno o varios odontólogos, cuentas y roles, catálogo y reglas de trabajo. No se necesita modificar código para personalizar identidad, servicios o jornadas. Las funciones se implementan en interfaz, servidor y PostgreSQL.

Entregado:

- Primera cuenta administradora creada por el usuario, inicio/cierre de sesión, usuarios activos/inactivos y roles administrador, odontólogo, recepción y caja.
- Permisos configurables y comprobados en el servidor. Protección del último administrador, de la propia cuenta y de sesiones con credenciales o roles revocados.
- Identidad: nombre, datos legales, dirección, contacto, logo PNG/JPG en bytea de PostgreSQL, colores con contraste validado, moneda, zona, formato de fechas, textos, prefijos y correlativos iniciales.
- Categorías y servicios: precio no negativo con dos decimales, duración positiva, estado y disponibilidad para futura reserva automática.
- Odontólogos vinculados con cuentas y servicios seleccionados; activación/desactivación conserva registros y asociaciones.
- Jornadas semanales, descansos dentro de jornadas, feriados, ausencias completas o parciales, anticipación mínima y separación entre citas.
- Auditoría transaccional con responsable, fecha, acción, referencia y solicitud. Consulta paginada; la tabla rechaza modificación/borrado.
- Tablas y selectores con consultas de páginas, búsqueda textual y filtros en PostgreSQL; sin colecciones completas procesadas en React.
- Interfaz con pestañas por permiso, edición o detalle de solo lectura, errores junto a los campos, estados vacíos/carga y formularios reutilizables.

## Evidencia de validación

Servidor: 21 pruebas aprobadas, incluyendo las 5 de fase 0 y 16 de fase 1. PostgreSQL real, base exclusiva sistema_odontologo_test.

Navegador: 17 pruebas aprobadas en Chrome. Incluyen la batería base y el recorrido completo de configuración, alta de dos odontólogos con servicios distintos, jornadas/descansos y bloqueos, desactivación conservando asociaciones, acceso de recepción y rechazo de petición restringida.

Se revisan configuración, formularios de usuarios, servicios, categorías, odontólogos, horarios, bloqueos y permisos, además de auditoría, en 1440×900, 1280×800, 1024×768, 768×1024, 390×844 y 360×800. Axe sin infracciones detectadas en los casos examinados; comprobación de teclado, Escape, restauración de foco, controles táctiles y ausencia de desbordamiento de página. Esto no sustituye una evaluación humana integral de accesibilidad para la entrega final.

Comprobaciones relevantes:

| Escenario | Resultado |
|---|---|
| Crear el administrador y acceder desde la interfaz | Correcto; contraseña BCrypt y sesión rotada |
| Solicitar edición sin CSRF o sin permiso | Rechazada |
| Revocar permiso de un rol o desactivar una cuenta con sesión abierta | Restricción efectiva en la siguiente solicitud |
| Retirar el último administrador o desactivar la propia cuenta | Rechazado |
| Cambiar identidad, marca, moneda, zona, logo y reglas | Persistencia real; navegación refleja identidad y color |
| Editar una versión anterior de la configuración | Conflicto, conservando valores del formulario |
| Precio negativo, más de dos decimales, duración cero | Rechazados sin registros |
| Dos profesionales con servicios distintos | Asociaciones verificadas |
| Desactivar servicio o profesional | Registro y asignaciones conservados |
| Descanso fuera de jornada, jornada solapada o retiro que deja un descanso huérfano | Rechazado |
| Dos solicitudes simultáneas de jornadas solapadas | Un éxito y un conflicto; un único registro y evento |
| Bloqueo parcial sin pareja completa de horas | Rechazo también en PostgreSQL |
| Subir imagen válida o archivo incompatible | PNG guardado/descargado exactamente; archivo incompatible rechazado |
| Modificar directamente un evento de auditoría | Rechazado por la base |
| Buscar, filtrar y cambiar de página | Respuestas solo de la página; caso de 10 registros y luego 1 |
| Fallo de conexión y recuperación | Mensaje y reintento; navegación utilizable |

Informes técnicos: backend/target/surefire-reports y frontend/.runtime/phase1-e2e.log, ignorados por Git. Pruebas reproducibles versionadas en ambos repositorios. Capturas revisadas: frontend/docs/verification/phase1. Compilación de ambos proyectos, lint y formato comprobados.

Se corrigieron durante la validación: tratamiento de respuestas exitosas sin contenido, etiquetas accesibles, rutas de pestañas, restauración del foco, ancho del campo de logo en celular, aislamiento de sesiones de prueba y preparación reproducible de roles en Windows. La migración V3 refuerza la pareja de horas sin alterar V2 ya aplicada.

## Estado de aceptación y límites

A01 y A03: configuración básica de esta fase cubierta. A29: acceso, permisos y auditoría inicial cubiertos; se ampliarán con cada módulo. A31 y A32: cubiertos para las pantallas y listas implementadas; la aceptación integral continúa hasta fase 9.

La fase 1 configura disponibilidad y reglas. La fase 2 incorporará pacientes y citas que las consuman. Activar reserva automática en un servicio configura su elegibilidad; todavía no crea citas ni conecta WhatsApp. Textos y correlativos se utilizarán al incorporar sus módulos.

Los roles operativos tienen inicialmente consulta de consultorio, catálogo, profesionales y horarios. Su capacidad para gestionar agenda, clínica o caja se incorporará junto con esas funciones, sin presentar módulos futuros como terminados.

Los datos y cuentas de pruebas permanecen en una base separada. La instalación local del usuario conserva sus datos y solicita su primera cuenta administradora; no contiene una contraseña administrativa predeterminada. Producción, HTTPS, respaldo/restauración y demostración de WhatsApp corresponden a sus fases.

Estado: fase 1 completada técnicamente y disponible para revisión del usuario. No se comenzó la fase 2.
