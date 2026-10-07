# Instantáneas de las guías maestras

Las fuentes vigentes están en la raíz compartida: AGENTS.md, docs/propuesta-alcance-sistema-odontologico.md, docs/plan-desarrollo-sistema-odontologico.md, los cierres de fase y el prompt maestro del usuario.

Esta carpeta conserva copias exactas en Git. PROJECT_GUIDE.md corresponde a AGENTS.md; PROMPT-MAESTRO.txt conserva el texto original. Son instantáneas de respaldo, no guías independientes. Sincronizarlas cuando cambien las fuentes. Actualización: agente Groq implementado, inferencia real verificada y pruebas autorizadas en la instalación ficticia actual, 06/10/2026; plan versión 2.8, alcance versión 1.8.

Se conservan los cierres de fases 0 a 5, las decisiones de agenda, clínica, tratamientos y finanzas, y el procedimiento de respaldo preparado. La restauración integral sigue pendiente de fase 9. La recepción y salida real de plantillas de WhatsApp fueron confirmadas por el usuario; texto propio y demostración completa por WhatsApp siguen pendientes; el prototipo del agente ya se verificó. A22 no se declara cumplida.

El frontend consulta las mismas fuentes en la raíz; sus commits conservan las instrucciones de ejecución y evidencias visuales, sin crear otra política.

Fase 6 por partes: [guía desde cero](conectar-whatsapp-prueba.md), [diseño y verificaciones del conector](conexion-whatsapp-fase-6.md) y [texto personalizado, coste y configuración](probar-whatsapp-texto-personalizado.md). Implementación y pruebas internas aprobadas; primer tramo externo confirmado por el usuario y A22 pendiente del agente y reserva real. La recomendación de PAYG no ejecuta una compra.

Prioridad nueva: [selección de IA y prototipo previo al upgrade](seleccion-ia-prototipo-fase-6.md). El usuario eligió Groq Free/openai/gpt-oss-20b por API y guardó la clave. El agente y las herramientas están implementados, con inferencia real, cita confirmada y repetición sin duplicación en la agenda actual ficticia. [Configuración](configurar-groq-agente.md), [implementación y evidencia](prototipo-agente-groq-fase-6.md) y [registro administrativo](groq-verificacion-real.json). La salida preparada en la aplicación no cierra A22.

## Revisión manual de fases 0 a 5

Guía secuencial: [guia-pruebas-manuales-fases-0-a-5.md](guia-pruebas-manuales-fases-0-a-5.md). Registro de resultados: [registro-revision-manual.md](registro-revision-manual.md). Archivos ficticios: [datos-prueba](datos-prueba/README.md). Son copias de las fuentes raíz; el usuario informó una ejecución aproximada favorable, pendiente de anotar por bloque. Sus dos ajustes se documentan en [servicios y calendario](ajustes-listas-agenda-odontologos.md).

Evaluación previa al pago de Twilio: [Kapso, oferta y adaptación](evaluacion-kapso-whatsapp-fase-6.md). El usuario confirmó el agente con entradas de aplicación y WhatsApp y conserva Groq. Kapso sigue como candidato; Free/Sandbox admite texto, pero el cupo no elimina cargos de entrega y no admite plantillas. No se migró el conector ni se compraron servicios.

Tramo manual autorizado el 07/10/2026: [configurar Kapso](conectar-kapso-prueba.md) y [resultados](conexion-kapso-fase-6.md). Conexión mínima en ramas kapso, tablas propias, intercambio real con lectura y respuesta del participante. El agente queda pausado; su conexión con Kapso y A22 siguen pendientes.
