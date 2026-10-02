# Ajustes antes de la revisión general y de la fase 6

Fecha: 02/10/2026. Referencias: alcance 1.3 y plan 1.7. La fase 6 continúa pendiente.

## Cambios solicitados

1. Seleccionar un servicio en un presupuesto ya puede consultar su detalle. La causa era la ausencia de GET /api/v1/services/{id}; se añadió un controlador que delega en un servicio de lectura transaccional y devuelve el DTO existente, protegido por SERVICES_READ. También se consultan registros inactivos para conservar su referencia histórica; el selector de nuevos tratamientos sigue filtrando servicios activos.
2. La selección completa la descripción y el precio vigente del catálogo. El precio unitario permanece editable. Mientras se recupera el detalle, el tratamiento y las acciones de guardar, quitar, añadir o cerrar quedan protegidos. Las actualizaciones se aplican sobre el estado actual de las partidas, conservando otros cambios. Si falla la consulta, se conserva el formulario y se puede reintentar.
3. La interfaz utiliza «tratamiento» en la edición, detalle, avance y movimientos del presupuesto, y «plan de tratamiento» en la vinculación de procedimientos clínicos. También se actualizaron los mensajes del servidor que describen esas reglas. Los contratos y nombres internos permanecen compatibles. «Concepto» conserva su significado administrativo en abonos y egresos.
4. Los resultados de acciones utilizan notificaciones flotantes compartidas: éxitos de guardado, validaciones generales, errores recuperables, descarga y emisión de documentos. No se incorporaron dependencias nuevas ni se reescribieron las reglas de cada módulo.

## Comportamiento de las notificaciones

Se sitúan en la esquina inferior derecha en computadora y tablet, y ocupan el ancho disponible con márgenes en celular. Un éxito desaparece después de 5 segundos y un error después de 8 segundos. Pasar el puntero por su botón de cierre o enfocarlo pausa el tiempo; puede cerrarse mediante su botón, con teclado o mediante interacción táctil. Se conservan como máximo cuatro avisos y se agrupan mensajes idénticos. Un guardado exitoso retira los avisos de error previos. El cuerpo de la notificación permite continuar interactuando con los controles que quedan debajo; solo su botón de cierre recibe clics propios.

El componente usa anuncios accesibles de estado o alerta, texto e iconos además del color, y un botón de cierre de 44 píxeles. Se ubica dentro del diálogo activo para conservar visibilidad e interacción frente a la capa modal del navegador; al cerrar un formulario vuelve a la página. Se conservan las validaciones junto a los campos y los errores persistentes de permisos, carga y conexión, con sus opciones de recuperación.

## Validación

- Servidor: 79 pruebas aprobadas en la verificación completa. La nueva comprobación cubre precio y duración del detalle, servicio inactivo, identificador inexistente, ausencia de sesión, lectura por recepción y revocación del permiso. Se conservan las comprobaciones financieras y clínicas existentes.
- Comprobación dirigida en navegador: carga del precio S/ 185,50, cambio a S/ 160,75, conservación al guardar, segunda partida conservada, bloqueo durante la consulta, aviso de error dentro del diálogo, cierre táctil, conservación de valores, repetición del mismo aviso, desaparición automática y recuperación de un error de consulta. Computadora 1440 × 900, tablet 768 × 1024 y celular 390 × 844; sin desbordamiento de página y sin infracciones de Axe en el formulario con aviso.
- Regresión completa del navegador: 52 escenarios aprobados (2,7 minutos), que abarcan configuración, pacientes, agenda, clínica, documentos, planes y finanzas. En la primera ejecución, la reprogramación detectó que un aviso podía bloquear su botón de reintento. Se corrigió la interacción del aviso; la ejecución final completa verifica también reprogramación fallida seguida de un nuevo horario válido, pausa del temporizador mediante puntero/foco, desaparición automática y acceso mediante teclado.
- Compilación y análisis de la interfaz aprobados, sin advertencias; comprobación de formato aprobada. Se revisaron las nueve capturas específicas de precio y avisos de esta corrección.

Las pruebas de escritura usan únicamente sistema_odontologo_test y comprueban su nombre antes de preparar datos. No se añadieron migraciones ni se requiere modificar el esquema o borrar datos del consultorio. Las capturas se conservan en frontend/docs/verification/adjustments; los registros locales de ejecución están en las carpetas .runtime de cada repositorio.

## Límites y continuidad

Estos ajustes preparan la revisión del sistema existente. No incorporan WhatsApp, herramientas del agente ni nuevas funciones de la fase 6. A22 continúa pendiente de la integración real y A30 de la restauración integral prevista en fase 9. Las decisiones de terminología, precio y avisos se registran en AGENTS.md, alcance y plan, con copias versionadas en backend/docs/project.
