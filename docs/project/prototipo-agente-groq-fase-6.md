# Agente Groq: implementación y verificación del prototipo

06/10/2026 · Alcance 1.7 · Plan 2.6. **Prototipo implementado y conectado al modelo real; fase 6 y A22 siguen abiertos.** Las respuestas se preparan en la aplicación. El envío personalizado por WhatsApp y la demostración completa con entrega al teléfono se completarán en el siguiente tramo.

## Funcionamiento entregado

El backend usa GroqCloud con openai/gpt-oss-20b mediante HTTPS. El modelo interpreta y elige herramientas; los servicios del sistema validan y ejecutan las operaciones. No se instala un modelo, GPU, n8n ni servidor de IA adicional. La clave se carga desde config/ai.local.properties, ignorado por Git; la interfaz solo recibe estado de configuración y campos públicos.

1. El conector valida y guarda un mensaje nuevo antes de encolarlo. Los estados de entrega, adjuntos no admitidos y eventos repetidos no vuelven a ejecutar el agente.
2. Una cola persistente procesa cada conversación en orden. Los números secuenciales conservan el orden aunque dos entradas tengan la misma fecha. El contexto tiene límite y comprende mensajes administrativos y respuestas anteriores.
3. El modelo usa consultar_servicios, pacientes_contacto, consultar_horarios, proponer_cita y descartar_propuesta. Los pacientes se limitan al teléfono de la conversación; no hay herramientas de clínica, finanzas, SQL o ejecución de código. Catálogos y pacientes se consultan en páginas de cinco; disponibilidad y resultados son acotados.
4. Consultar horarios usa AvailabilityService de fase 2. Fechas relativas se calculan desde la recepción del mensaje en la zona del consultorio. Las referencias devueltas contienen profesional, servicio, duración y horario real.
5. La propuesta contiene paciente, servicio, odontólogo, fecha/hora y duración, con un código de ocho caracteres. Vence en 30 minutos y no retiene el horario. Consultar precios, negar o enviar datos incompletos no debe reservar.
6. Solo el mensaje exacto **CONFIRMO código** activa la reserva. El servidor valida de nuevo paciente, servicio, profesional, duración, identidad del resumen, zona y disponibilidad. No necesita aprobación de recepción. La creación usa AppointmentService y las restricciones de solapamiento existentes.
7. Cita, historial, confirmación y propuesta se guardan en la misma transacción. La referencia estable de la propuesta evita duplicación. Si recepción ocupa el horario, falla la confirmación sin borrar esa cita ni dejar un paciente provisional incompleto.
8. La bitácora conserva entradas, herramientas, argumentos, resultados, errores, modelo, consumo y cita. No guarda claves ni razonamiento interno. Las respuestas se rotulan **preparadas, sin envío a WhatsApp**.

Configuración inicial: seis llamadas por mensaje, 1 500 tokens máximos de salida, 12 segundos por llamada, 75 por ejecución, seis mensajes de contexto y hasta tres intentos manuales tras fallo. Autenticación rechazada, cuota, salida truncada, herramienta inválida y tiempo agotado terminan de forma controlada. No se habilita facturación ni un proveedor alternativo automáticamente.

La interfaz incluye estado del agente, **Probar agente**, mensajes de prueba, propuesta con código y bitácora con búsqueda, filtro y paginación del servidor. El permiso AGENT_TEST_WRITE se concede inicialmente a administración. Las entradas desde la aplicación son APP_TEST y sus reservas se muestran como **Prueba IA**, origen AI_TEST. Una confirmación recibida realmente por WhatsApp utiliza ese origen; no convierte la respuesta preparada en mensaje entregado.

## Verificación con Groq real y los datos actuales

El usuario confirmó que la instalación, pacientes y agenda actuales son ficticios y autorizó utilizarlos. Se guardó un respaldo local antes de aplicar Flyway V13–V15 a **sistema_odontologo**, conservando los registros existentes. La clave privada proporcionada por el usuario se leyó internamente, sin mostrarse. Los trabajadores de envío de WhatsApp se desactivaron durante la verificación; no se enviaron mensajes ni se modificaron las credenciales de Twilio.

Solicitud evaluada:

~~~text
Hola, soy Carlos Ruiz Vega. La cita es para mí. Quiero reservar una Limpieza dental para mañana a las 09:00 con Julia Aracelly Huaytalla Alarcon.
~~~

| Caso real | Resultado observado |
|---|---|
| Interpretación y propuesta | Groq utilizó servicios, pacientes del contacto, horarios y propuesta. Interpretó mañana como 07/10/2026 en America/Lima; limpieza de 60 minutos con Julia a las 09:00. Cinco llamadas al modelo, 5 788 tokens de entrada y 405 de salida acumulados; aproximadamente 3.1 s en esta ejecución. Todavía no creó la cita. |
| Confirmación exacta | Cita 9ee560e4-2d48-4322-84a2-7a8a8795f0bb, Carlos Ruiz Vega, 07/10/2026 09:00–10:00, Confirmada, Prueba IA. La confirmación la ejecuta el servidor sin otra llamada al modelo. |
| Repetir confirmación | Devuelve la misma cita. El total aumenta solo una vez: ocho citas iniciales, nueve después. |
| Preguntar precio sin reservar | Responde el precio real del catálogo, S/. 200.00, usando consultar_servicios. Sin cita ni propuesta nueva. |
| «Quiero una cita. Todavía no sé qué servicio ni qué día.» | Pide identificar al paciente y no crea una cita. |
| «No me reserves ninguna cita. No confirmo ninguna propuesta.» | Respeta la negación, sin cita ni propuesta nueva. |

Los tres casos de consulta se midieron aproximadamente en 1.4, 1.1 y 0.5 segundos, respectivamente. Son observaciones de esta muestra, no una garantía de latencia o de comprensión universal. Hubo diez llamadas reales al modelo entre estos cuatro casos de interpretación; las dos confirmaciones no consumieron llamadas. Las seis ejecuciones quedaron Completadas en la bitácora de la instalación actual.

Los totales conservados fueron ocho pacientes, nueve cargos, once movimientos financieros, cinco atenciones, cinco contenidos documentales y ocho mensajes salientes. La única cita nueva fue la confirmada de demostración. La evidencia administrativa se conserva en [groq-verificacion-real.json](groq-verificacion-real.json); los registros de la aplicación permiten consultar la misma bitácora.

## Validaciones automáticas y revisión visual

Se verificaron **107 casos del servidor sin fallos**: batería completa de 105 y luego los dos escenarios adicionales de conflicto y código de otra conversación. El proveedor controlado de estas pruebas valida infraestructura y reglas; se distingue de la inferencia real descrita arriba. Los casos nuevos cubren API, formato de herramientas, ocultación de razonamiento, errores de autenticación/cuota/truncamiento, intención, fechas, permisos, CSRF, paginación, ficha provisional, expiración, cambio de duración/servicio, repetición, confirmación cruzada y ocupación por recepción. La regresión de concurrencia de fase 2 conserva la restricción de solapamientos; no se ejecutaron pruebas de carga sobre el Sandbox.

El navegador verificó nueve escenarios de configuración, siete de agente/WhatsApp y ocho adicionales de pacientes, agenda y listas compactas: **24 escenarios distintos aprobados**. Formularios y bitácora se probaron a 1 440, 768 y 390 píxeles. Se revisaron contraste, accesibilidad automática, teclado, regreso del foco, búsqueda/paginación y ausencia de desbordamiento, incluyendo listas anidadas y regreso al mes. Lint, TypeScript, compilación y formato aprobados. Capturas en frontend/docs/verification/agent y whatsapp; las capturas con resultados controlados son evidencia de interfaz, no de inferencia.

Correcciones realizadas: orden de llegada persistente ante fechas iguales; conservación de nombres, duración y zona de la oferta; cierre de la bitácora con Escape sin cerrar también la conversación, y aislamiento del recorrido de Tab dentro del diálogo superior.

Las pruebas automáticas destructivas conservan la base de regresión protegida **sistema_odontologo_test** que ya utilizaba el proyecto. Esa protección no exige otra instalación, agenda o pacientes para las pruebas manuales del usuario.

## Próxima prueba y pendientes

Seguir [configurar-groq-agente.md](configurar-groq-agente.md). Reiniciar el backend normal para cargar la clave y probar desde WhatsApp → Probar agente. También pueden procesarse entradas nuevas del participante autorizado de WhatsApp; leer la propuesta en la aplicación y confirmar desde el teléfono permite comprobar recepción → agente → cita, pero la respuesta todavía se lee en la aplicación.

Falta habilitar y comprobar texto saliente real según la decisión del usuario sobre Twilio, conectar las respuestas a la tarea persistida de envío con recuperación y realizar la demostración reproducible completa de fase 6. Reprogramación, cancelación por IA y toma de control humano siguen en fase 7. Este informe no declara cumplido A22.

Documentación oficial consultada: [herramientas locales](https://console.groq.com/docs/tool-use/local-tool-calling), [API](https://console.groq.com/docs/api-reference), [GPT-OSS y razonamiento](https://console.groq.com/docs/reasoning), [inicio](https://console.groq.com/docs/quickstart), [tratamiento de datos](https://console.groq.com/docs/your-data).
