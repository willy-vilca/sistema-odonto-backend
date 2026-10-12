# OdontoCare para un odontólogo — versión sencilla

Decisión del usuario del 11/10/2026. Variante 1.0 en `version_sencilla` de backend y frontend. Parte del alcance 1.13 y plan 3.15; las diferencias de este documento prevalecen exclusivamente para esta variante. La versión de registro permanece en main.

## Alcance y decisiones

- Instalación local independiente en `sistema_odonto_mini`. Pruebas destructivas únicamente en `sistema_odonto_mini_test`, comprobando el nombre antes de limpiar. Nunca limpiar ni migrar la instalación original para este trabajo.
- Un odontólogo puede usar su misma cuenta administradora como cuenta profesional. No necesita crear una cuenta de recepción o caja. Con un único profesional activo, citas, historia clínica y presupuestos lo seleccionan automáticamente. Si la configuración contiene varios profesionales, conservar la selección explícita.
- Registrar pacientes desde citas, historia clínica o Pacientes con nombre y teléfono. El servidor crea ficha provisional activa y contacto del mismo nombre, relación Paciente y responsable de pago. Un teléfono compartido sigue admitiendo personas distintas; no crear una segunda ficha con el mismo nombre y teléfono, incluso si la primera tiene nacimiento registrado. Menores con nacimiento informado conservan la exigencia de responsable identificado.
- Una ficha provisional dispone de todos los módulos clínicos, documentales, de tratamientos y financieros. Sus datos adicionales se completan voluntariamente. La fecha de nacimiento no es obligatoria para guardar una ficha; si se informa, se validan fecha y tutor.
- Atención: servicios realizados como tarea principal; tres notas opcionales (evolución, diagnósticos, indicaciones). Anamnesis retirada del formulario de atención; conservar antecedentes y datos anteriores. Fecha de hoy y motivo derivado de procedimientos cuando se deja vacío, con opciones secundarias para editar fecha, motivo o cita vinculada. Finalizar o corregir admite las notas vacías.
- Una atención sin cita abre su detalle al guardarse, con finalizar, finalizar e ir a cobrar y finalizar/cobrar/finalizar cargos. Reutilizar la operación existente, sus permisos, bloqueo de paciente, idempotencia y transacción; un plan incluido sigue sin volver a generar deuda. No crear citas artificiales para este flujo.
- Abonos y aplicación de anticipos: proponer automáticamente el menor entre dinero disponible y saldo del cargo. Varios cargos reciben el dinero restante en orden de selección. Las modificaciones manuales se conservan; cambios del importe redistribuyen solo sugerencias automáticas. Dinero sobrante queda como anticipo. Las liberaciones y devoluciones conservan su selección explícita.
- Retirar auditoría de la configuración visible; conservar infraestructura interna, controles críticos, archivos y bitácora WhatsApp. Agrupar opciones poco habituales de configuración y pago en vistas secundarias.
- Conservar agenda y disponibilidad, historia/versiones, odontograma, documentos/PostgreSQL, presupuestos/planes, cargos/cuotas, abonos/anticipos, caja/egresos, PDF, reportes y WhatsApp/IA. No sustituir saldos derivados por campos libres ni reducir permisos de seguridad.
- Los proveedores externos necesitan configuración para la nueva instalación. Durante la revisión local no se envían mensajes externos ni se ejecutan inferencias reales; las credenciales privadas permanecen fuera de Git. Fase 9 y recordatorios siguen pendientes y no forman parte de esta adaptación.

## Plan de implementación y comprobación

1. Aislar ramas y bases; leer guías y registrar esta variante.
2. Habilitar fichas básicas en servidor y creación rápida desde los formularios, con protección de duplicados y permisos.
3. Reducir atención y reutilizar cobro desde ingresos sin cita, con selección del único profesional.
4. Sugerir aplicaciones exactas de abonos/anticipos y simplificar opciones visibles.
5. Comprobar clínica/finanzas, concurrencia, fallos y permisos; revisar flujos en computadora, tablet y celular. Registrar incidencias y resultados reales al finalizar.
6. Guardar commits en ambas ramas y verificar que las referencias main no cambien.

## Primer uso

1. Abrir la aplicación y crear una cuenta propia. La nueva base no contiene los usuarios de la versión anterior.
2. En Configuración completar Consultorio. En Opciones adicionales → Categorías crear una categoría; después crear Servicios con precio y duración. Registrar el perfil profesional y vincularlo a la misma cuenta administradora, sin crear otra cuenta. Asignar los servicios y definir Horarios. Las otras opciones siguen disponibles cuando se necesiten.
3. Nueva cita → Registrar paciente → nombre y teléfono → Guardar y seleccionar → servicio y horario → reservar.
4. Para un ingreso sin cita: Atender paciente → Registrar paciente o buscar uno existente → Nueva atención → Añadir procedimiento → elegir servicio → Guardar atención. Las notas son opcionales.
5. En el detalle elegir Finalizar e ir a cobrar para un pago parcial, o Finalizar y cobrar para el pago completo. En efectivo, abrir caja primero; no se inventa una apertura ni un cobro.
6. Registrar abono → importe y medio → seleccionar cargos; revisar las aplicaciones sugeridas o editarlas. El excedente se conserva como anticipo.
7. En la ficha del paciente, Editar ficha permite añadir datos cuando estén disponibles. Ser provisional no bloquea los demás módulos.

## Evidencia

Adaptación implementada y comprobada el 12/10/2026; pendiente de la revisión del usuario y de configurar los datos del consultorio real.

- **255 pruebas distintas de backend aprobadas**, sin errores ni omitidas en los resultados finales por clase. La primera regresión completa detectó dos expectativas antiguas (cuentas elegibles y búsqueda codificada en MockMvc); se corrigieron y se verificaron los 72 casos de configuración/pacientes/finanzas afectados, incluida V25. No se presenta el primer bloque como aprobado sin fallos.
- Casos nuevos: creación mínima con contacto/pagador, familiares de teléfono compartido, rechazo de duplicados incluso después de completar nacimiento, dos creaciones simultáneas, permisos/validaciones, cuenta única administradora-profesional, nacimiento opcional, atención sin cita y sin texto clínico, cobro completo repetido sin duplicación, corrección versionada, rechazo de importe incorrecto con reversión íntegra, aceptación de plan/abonos/anticipo/cuotas/sesiones y antecedentes/odontograma/PDF/consentimiento sobre ficha provisional.
- **90 escenarios distintos de navegador aprobados en bloques**: 59 de configuración, agenda, clínica, tratamientos y finanzas; 9 específicos de esta adaptación; 4 de documentos/formularios; 8 de panel/reportes/navegación; 10 de conversaciones y supervisión. Se adaptaron las comprobaciones antiguas al detalle que ahora se abre al guardar, notas opcionales, auditoría oculta y sugerencias monetarias con dos decimales. Los intentos que todavía esperaban el comportamiento anterior se conservan en los registros, sin contarlos como aprobados.
- El bloque final específico de 13 casos (9 mini + 4 documentos) pasó completo. Los 30 casos restantes del bloque operativo, los 8 de reportes y los 10 de conversaciones pasaron completos. Teclado, controles táctiles, Axe y ausencia de desbordamiento global revisados en computadora, tablet y celular; capturas propias en `frontend/evidence/mini`.
- Compilación y revisión estática del frontend y empaquetado del backend correctos. No se añadieron dependencias ni un segundo motor financiero o de agenda.
- Instalación local `sistema_odonto_mini` inicializada con las 25 migraciones, sin usuarios, pacientes ni citas de demostración insertados. Todos los datos de pruebas automatizadas se alojaron en `sistema_odonto_mini_test`.
- Main mantiene las referencias iniciales: backend `240962adb6c384dba9449ad152b5705d0e10ff1a`; frontend `d5a7ed357dc9d6bd854f0a090d0cce2184cdeb22`. La base original solo se consultó: lectura del 12/10/2026 con 12 pacientes, 19 citas, 13 entradas de cargos y 16 movimientos. Esa lectura no se presenta como comparación de contadores antes/después; no se ejecutaron escrituras o migraciones en esa base.
- Registros detallados locales: `backend/.runtime/mini-verify.log`, `mini-verify-final.log`, `mini-validation-summary.json`; `frontend/.runtime/mini-regression-e2e.log`, `mini-remaining-e2e.log`, `mini-documents-e2e-final.log`, `mini-phase8-e2e.log`, `mini-agent-e2e.log`. El resumen cuenta títulos distintos, sin sumar repeticiones.

## Ejecución y separación de versiones

Estado vigente del 12/10/2026, 11:19 Lima: el usuario configuró otra cuenta de Kapso; autenticación/webhook comprobados y backend reiniciado con esos valores. Prueba real recibida, ejecución Groq COMPLETED y respuesta READ en un intento, confirmada por el participante, sin cambios de pacientes/agenda/finanzas. WhatsApp/Groq activos; los apartados de pausa o saldo insuficiente siguientes conservan el recorrido anterior. [Evidencia de recuperación](diagnostico-kapso-saldo-insuficiente.md#recuperación-comprobada-con-la-configuración-nueva).

Actualización autorizada del 12/10/2026: WhatsApp, su trabajador y Groq activados para diagnosticar el rechazo de envío. Clave, número, webhook/túnel y recepción real comprobados. El proveedor devuelve HTTP 402 / `insufficient_credits`; recuperar la entrega depende de créditos del proyecto y una prueba entregada posterior. La pausa descrita en el párrafo siguiente corresponde a la revisión inicial y ya fue superada por esta autorización. [Diagnóstico y pasos de recuperación](diagnostico-kapso-saldo-insuficiente.md).

La aplicación de revisión está disponible en `http://127.0.0.1:5173`, con backend local en 8080. Como la instalación es nueva, solicita crear una cuenta propia; no se proporcionó una contraseña predeterminada ni se copiaron usuarios de la base original.

Kapso/Groq y la supervisión se conservan. En esta ejecución de revisión los conectores y trabajadores externos están pausados mediante argumentos de inicio para evitar que el Sandbox anterior atienda una instalación todavía vacía. Las claves privadas no se modificaron. Después de configurar profesional, servicios y horarios, detener esta ejecución e iniciar normalmente con `./mvnw.cmd spring-boot:run` para utilizar la configuración privada cuando se decida probar el canal de esta instalación. Para un cliente real deben configurarse sus propios participantes y número. Las comprobaciones de esta adaptación no enviaron WhatsApp ni hicieron inferencias reales.

Al cambiar entre `main` y `version_sencilla`, detener los servicios y hacer una compilación limpia del backend (`./mvnw.cmd clean package -DskipTests`) antes de iniciar. Así se eliminan clases y recursos de migración compilados de la otra rama. Iniciar siempre con las propiedades y la base correspondientes a esa rama; no reutilizar su archivo JAR para ejecutar la otra versión.

Fase 9, despliegue y recordatorios siguen pendientes. Esta adaptación no sustituye la validación de instalación y entrega de esa fase.
