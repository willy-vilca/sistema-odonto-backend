# Guía de trabajo del proyecto

Antes de planificar, implementar o validar el sistema, leer `docs/propuesta-alcance-sistema-odontologico.md` y `docs/plan-desarrollo-sistema-odontologico.md`. El alcance contiene las decisiones vigentes y los criterios de aceptación; el plan organiza su implementación y validación por fases.

La primera entrega se orienta al registro y demostración del producto: instalación independiente por consultorio, una sede, uno o varios odontólogos y agenda por odontólogo sin gestión separada de sillones.

Usar React + Vite + TypeScript + TailwindCSS, Spring Boot con Java 21 y Maven, y PostgreSQL. Guardar imágenes y PDF en la misma base PostgreSQL según el documento. Incluir un agente propio conectado a WhatsApp con reservas automáticas tras confirmar el paciente y bitácora verificable.

Leer también `PROMPT MAESTRO — DESARROLLO PROFESONIAL DEL SOFTWARE.txt` antes de desarrollar. El usuario adoptó expresamente sus principios como guía permanente: código legible, mantenible, modular, seguro, con responsabilidades claras, revisión de calidad y sin sobreingeniería. Sus instrucciones directas tienen prioridad ante una excepción.

Backend organizado por módulos funcionales con arquitectura por capas: controller, dto, service, repository y model, según se necesiten. Los controladores no contienen reglas de negocio ni consultas; no devolver entidades JPA como contratos HTTP. Frontend organizado en app, features y shared, separando componentes, modelos, servicios HTTP y hooks. Reutilizar componentes cuando corresponda.

Todos los listados de datos en tablas o filas tienen paginación, filtros y búsqueda textual ejecutados en el backend. Consultar únicamente la página solicitada, limitar su tamaño y validar los campos de ordenación. No descargar la colección completa para filtrarla o paginarla en React. La agenda consulta intervalos acotados, además de paginar su alternativa de lista. Los binarios se recuperan solamente a demanda.

Excepción local autorizada: las credenciales de demostración de PostgreSQL proporcionadas por el usuario se guardan directamente en `application-local.properties`. Esta excepción no se extiende a producción ni a tokens de WhatsApp o del modelo. Mantener perfiles separados y no registrar credenciales en logs.

Trabajar en los repositorios Git independientes `backend` y `frontend`, conservar sus historiales y registrar commits descriptivos de cada avance. Las guías maestras están en la raíz; conservar copias versionadas identificadas como instantáneas en `backend/docs/project` para respaldar decisiones, y sincronizarlas cuando cambien.

El diseño visual queda a criterio del agente. Mantener una interfaz elegante, profesional y atractiva, con muy buena experiencia de usuario. Diseñar principalmente para computadora y adaptar todas las funciones a tablets y celulares. Comprobar navegación, formularios, agenda, odontograma, tablas, documentos y conversaciones en esos tamaños durante cada fase; la adaptación responsiva no se aplaza al final.

Desarrollar por fases completas del plan, incluyendo interfaz, servidor, persistencia y las validaciones correspondientes. Registrar resultados, correcciones y estado de cada fase en el plan. No considerar una fase terminada ni pasar a trabajo que dependa de ella mientras sus comprobaciones críticas sigan fallando. La auditoría, los permisos y la calidad visual se incorporan con cada función.

Mantener alineados el plan de desarrollo, la implementación y los criterios de aceptación con ese alcance. Las ampliaciones excluidas no se incorporan por iniciativa propia. Si el usuario cambia el alcance, actualizar el documento para reflejar la decisión vigente.
