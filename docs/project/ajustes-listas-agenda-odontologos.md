# Ajustes de servicios asociados y calendario mensual

05/10/2026 · Alcance 1.4 · Plan 1.9 · Fases 1 y 2, antes de fase 6.

El usuario informa una revisión aproximada de la guía con resultado favorable y solicita mejorar dos listas extensas. Ese comentario no sustituye el registro individual de aceptación manual. WhatsApp y el agente siguen pendientes de fase 6.

## Comportamiento

- Odontólogos: hasta tres servicios ordenados por nombre, indicador «Y N más» y botón «Ver servicios (total)». La ventana consulta los asociados mediante búsqueda, estado y paginación del servidor, incluidos los inactivos que sigan vinculados. Consultar no altera asignaciones ni historial.
- Mes: hasta tres citas por día y botón «+N citas más»; todas las celdas conservan la misma altura. Los nombres largos se abrevian visualmente, manteniendo su texto accesible. En celular cada día con citas muestra un contador que abre la misma lista.
- Lista del día: respeta fecha y odontólogo seleccionado, ofrece búsqueda, estado y paginación remotos. «Ver cita» abre el detalle existente con acciones e historial. Al modificar una cita se actualizan calendario y lista; se conservan búsqueda, estado y página. Cerrar el detalle devuelve a la lista; cerrar la lista conserva el mes.
- Teclado: Escape cierra primero el detalle y después la lista. Si una actualización reemplaza el botón que abrió el detalle, el foco vuelve a un control de la ventana inferior. Los controles táctiles mantienen un área mínima de 44 píxeles.

## Implementación

GET `/api/v1/dentists/{id}/services` exige DENTISTS_READ y devuelve únicamente identificador, nombre y estado. La búsqueda y el filtro se ejecutan en PostgreSQL; tamaño de página máximo 100 y ordenación autorizada por nombre. No expone precios ni requiere ampliar SERVICES_READ. Un profesional inexistente devuelve 404.

La agenda reutiliza su consulta acotada de calendario y GET `/api/v1/appointments` para la lista paginada del día. No incorpora otro conjunto de reglas de reserva. Se reutilizan Modal, PagedTable, AppointmentList y AppointmentDetail. El hook paginado admite actualización externa sin reiniciar filtros. No hay migraciones, dependencias nuevas ni cambios en los datos del consultorio.

## Verificación

Las pruebas utilizan exclusivamente `sistema_odontologo_test`, backend 8081 y frontend 5174. No se borra ni modifica la base de demostración del usuario.

- Backend: 18 pruebas de Phase1IntegrationTests aprobadas, incluido el nuevo caso de asociaciones, páginas, búsqueda, servicios inactivos, ordenación/tamaño inválidos, 404 y permisos independientes del catálogo. Empaquetado aprobado.
- Navegador: 17 escenarios aprobados de configuración, pacientes y agenda, incluidos tres nuevos a 1440×900, 768×1024 y 390×844. Los nuevos escenarios comprueban 21 servicios asociados, consulta de inactivos, búsqueda vacía, segunda página, límite de tres resúmenes, 21 citas en un día y otro profesional, filtro por profesional, segunda página de citas, detalle/historial, cambio a Confirmada y regreso conservando búsqueda y mes.
- Accesibilidad: comprobación Axe WCAG 2 A/AA y 2.1 AA, ausencia de desbordamiento horizontal global, Escape, recuperación de foco y ausencia de errores JavaScript en los escenarios nuevos.
- Frontend: compilación, lint y formato aprobados. Capturas de servicios, mes y lista del día en `frontend/docs/verification/compact-lists`, en los tres tamaños.

Se aisló la prueba anterior de paginación de servicios mediante búsqueda de sus propios registros: ya no depende de que el resto de las pruebas haya creado exactamente dos servicios en toda la base. No cambia el comportamiento de la aplicación.

La revisión manual del usuario sigue registrándose en su guía y hoja de resultados. Las pruebas de estos ajustes no representan una nueva aceptación completa de las fases 0 a 5 ni evidencia de integración con WhatsApp.
