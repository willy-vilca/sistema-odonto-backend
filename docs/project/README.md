# Instantáneas de las guías maestras

Las fuentes vigentes están en la raíz compartida: AGENTS.md, docs/propuesta-alcance-sistema-odontologico.md, docs/plan-desarrollo-sistema-odontologico.md, los cierres de fase y el prompt maestro del usuario.

Esta carpeta conserva copias exactas en Git. PROJECT_GUIDE.md corresponde a AGENTS.md; PROMPT-MAESTRO.txt conserva el texto original. Son instantáneas de respaldo, no guías independientes. Sincronizarlas cuando cambien las fuentes. Actualización: selección documental de IA y prioridad del prototipo antes de pagar Twilio, 06/10/2026; plan versión 2.5, alcance versión 1.6.

Se conservan los cierres de fases 0 a 5, las decisiones de agenda, clínica, tratamientos y finanzas, y el procedimiento de respaldo preparado. La restauración integral sigue pendiente de fase 9. La recepción y salida real de plantillas de WhatsApp fueron confirmadas por el usuario; texto propio, agente y reserva siguen pendientes. A22 no se declara cumplida.

El frontend consulta las mismas fuentes en la raíz; sus commits conservan las instrucciones de ejecución y evidencias visuales, sin crear otra política.

Fase 6 por partes: [guía desde cero](conectar-whatsapp-prueba.md), [diseño y verificaciones del conector](conexion-whatsapp-fase-6.md) y [texto personalizado, coste y configuración](probar-whatsapp-texto-personalizado.md). Implementación y pruebas internas aprobadas; primer tramo externo confirmado por el usuario y A22 pendiente del agente y reserva real. La recomendación de PAYG no ejecuta una compra.

Prioridad nueva: [selección de IA y prototipo previo al upgrade](seleccion-ia-prototipo-fase-6.md). Groq Free/gpt-oss-20b es el candidato inicial por API; no se ejecutó inferencia y falta implementar y evaluar el agente. La agenda de pruebas y la confirmación se conservan; la salida preparada en la aplicación no cierra A22.

## Revisión manual de fases 0 a 5

Guía secuencial: [guia-pruebas-manuales-fases-0-a-5.md](guia-pruebas-manuales-fases-0-a-5.md). Registro de resultados: [registro-revision-manual.md](registro-revision-manual.md). Archivos ficticios: [datos-prueba](datos-prueba/README.md). Son copias de las fuentes raíz; el usuario informó una ejecución aproximada favorable, pendiente de anotar por bloque. Sus dos ajustes se documentan en [servicios y calendario](ajustes-listas-agenda-odontologos.md).
