# Instantáneas de las guías maestras

Las fuentes vigentes están en la raíz compartida: AGENTS.md, docs/propuesta-alcance-sistema-odontologico.md, docs/plan-desarrollo-sistema-odontologico.md, los cierres de fase y el prompt maestro del usuario.

Esta carpeta conserva copias exactas en Git. PROJECT_GUIDE.md corresponde a AGENTS.md; PROMPT-MAESTRO.txt conserva el texto original. Son instantáneas de respaldo, no guías independientes. Sincronizarlas cuando cambien las fuentes. Actualización: 08/10/2026; plan 3.8 y alcance 1.9. Fase 6 y A22 completados mediante reserva real con Kapso/Groq y respuesta READ. [Cierre](cierre-fase-6.md) y [trazas individuales](validacion-real-agente-kapso-fase-6.md). Los apuntes siguientes conservan el recorrido histórico de los tramos anteriores.

Se conservan los cierres de fases 0 a 5, las decisiones de agenda, clínica, tratamientos y finanzas, y el procedimiento de respaldo preparado. La restauración integral sigue pendiente de fase 9. La recepción y salida real de plantillas de WhatsApp fueron confirmadas por el usuario; texto propio y demostración completa por WhatsApp siguen pendientes; el prototipo del agente ya se verificó. A22 no se declara cumplida.

El frontend consulta las mismas fuentes en la raíz; sus commits conservan las instrucciones de ejecución y evidencias visuales, sin crear otra política.

Fase 6 por partes: [guía desde cero](conectar-whatsapp-prueba.md), [diseño y verificaciones del conector](conexion-whatsapp-fase-6.md) y [texto personalizado, coste y configuración](probar-whatsapp-texto-personalizado.md). Implementación y pruebas internas aprobadas; primer tramo externo confirmado por el usuario y A22 pendiente del agente y reserva real. La recomendación de PAYG no ejecuta una compra.

Prioridad nueva: [selección de IA y prototipo previo al upgrade](seleccion-ia-prototipo-fase-6.md). El usuario eligió Groq Free/openai/gpt-oss-20b por API y guardó la clave. El agente y las herramientas están implementados, con inferencia real, cita confirmada y repetición sin duplicación en la agenda actual ficticia. [Configuración](configurar-groq-agente.md), [implementación y evidencia](prototipo-agente-groq-fase-6.md) y [registro administrativo](groq-verificacion-real.json). La salida preparada en la aplicación no cierra A22.

## Revisión manual de fases 0 a 5

Guía secuencial: [guia-pruebas-manuales-fases-0-a-5.md](guia-pruebas-manuales-fases-0-a-5.md). Registro de resultados: [registro-revision-manual.md](registro-revision-manual.md). Archivos ficticios: [datos-prueba](datos-prueba/README.md). Son copias de las fuentes raíz; el usuario informó una ejecución aproximada favorable, pendiente de anotar por bloque. Sus dos ajustes se documentan en [servicios y calendario](ajustes-listas-agenda-odontologos.md).

Evaluación previa al pago de Twilio: [Kapso, oferta y adaptación](evaluacion-kapso-whatsapp-fase-6.md). El usuario confirmó el agente con entradas de aplicación y WhatsApp y conserva Groq. Kapso sigue como candidato; Free/Sandbox admite texto, pero el cupo no elimina cargos de entrega y no admite plantillas. No se migró el conector ni se compraron servicios.

Tramo manual autorizado el 07/10/2026: [configurar Kapso](conectar-kapso-prueba.md) y [resultados](conexion-kapso-fase-6.md). Conexión mínima en ramas kapso, tablas propias, intercambio real con lectura y respuesta del participante. El agente queda pausado; su conexión con Kapso y A22 siguen pendientes.

Agente autónomo por Kapso: [guía](probar-agente-kapso-fase-6.md), [implementación y estado](integracion-agente-kapso-fase-6.md). Se implementaron respuesta y reserva consistentes, confirmación natural y bitácora; 138 casos backend y seis de interfaz verificados. A22 y fase 6 en validación a la espera de la conversación real del participante.


Fase 7 implementada y validada internamente: [conversaciones, cambios y recuperación](gestion-conversaciones-agente-fase-7.md) y [demostración real preparada](probar-cambios-agente-kapso-fase-7.md). Datos actuales y cierre de fase 6 conservados. La validación real de cambios por WhatsApp se realizará cuando el usuario esté listo; fase 7 en validación y fase 8 pendiente. Las entradas anteriores son apuntes históricos.

Estado vigente: consulta propia y disponibilidad reales aprobadas; la propuesta real sigue pendiente tras fallos de cuota/rechazo. Corrección v7.3: 187 pruebas backend y vista previa con Groq aprobadas, sin cambiar la cita ni enviar WhatsApp. El usuario se ausenta y pidió no solicitar nuevos mensajes hasta regresar. [Diagnóstico y punto para continuar](diagnostico-groq-reserva-fase-7.md); plan 3.3, fase 7 abierta y fase 8 pendiente.

Actualización del 08/10: el participante regresó y se reanudó la demostración. v7.4 corrige el filtro de nombre del paciente, con 72 regresiones del agente aprobadas. Propuesta auténtica del 13/10 al 20/10 09:00 recibida READ y PENDING, cita original intacta; recuperación de cuota demostrada sin duplicar consultas. Continuar con negación y después confirmación de una propuesta nueva. [Evidencia](validacion-real-agente-kapso-fase-7.md); fase 7 abierta.

Último ajuste v7.6: negación y código descartado ya comprobados realmente; la nueva propuesta se repite tras fallo de herramientas/cuota. Lectura propia obligatoria ejecutada por el flujo con permisos, contexto persistido y 79 pruebas aprobadas; Groq real preparó el resumen en tres inferencias y 3,032 segundos sin cambios ni WhatsApp. [Diagnóstico y continuación](recuperacion-herramientas-groq-fase-7.md). Fase 7 abierta.
